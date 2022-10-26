/*
 * MIT License
 *
 * Copyright (c) 2021 1619kHz
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.microspace.server;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.common.base.Strings;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * @author i1619kHz
 */
@Sharable
final class HttpWebSocketHandler extends SimpleChannelInboundHandler<Object> {
    private final ChannelGroup channelGroup;
    private final ConcurrentMap<String, ChannelId> channelMap;
    private WebSocketServerHandshaker webSocketServerHandshaker;

    public HttpWebSocketHandler() {
        this.channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        this.channelMap = new ConcurrentHashMap<>();
    }

    private void addChannel(Channel channel) {
        channelGroup.add(channel);
        channelMap.put(channel.id().asShortText(), channel.id());
    }

    private void removeChannel(Channel channel) {
        channelGroup.remove(channel);
        channelMap.remove(channel.id().asShortText());
    }

    private Channel findChannel(String channelId) {
        return channelGroup.find(channelMap.get(channelId));
    }

    private Channel getChannel(ChannelHandlerContext ctx) {
        return ctx.channel();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object message) {
        if (message instanceof HttpRequest) {
            handleHttpRequest(channelHandlerContext, (HttpRequest) message);
        } else if (message instanceof WebSocketFrame) {
            handleWebSocketFrame(channelHandlerContext, (WebSocketFrame) message);
        } else {
            ReferenceCountUtil.retain(message);
            channelHandlerContext.fireChannelRead(message);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        addChannel(getChannel(ctx));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        removeChannel(getChannel(ctx));
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }

    private void handleWebSocketFrame(ChannelHandlerContext channelHandlerContext, WebSocketFrame frame) {
        final Channel channel = getChannel(channelHandlerContext);
        if (frame instanceof CloseWebSocketFrame) {
            webSocketServerHandshaker.close(channel, (CloseWebSocketFrame) frame.retain());
            return;
        }
        if (frame instanceof PingWebSocketFrame) {
            channel.write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        if (frame instanceof BinaryWebSocketFrame) {
            channel.write(new BinaryWebSocketFrame(frame.content().retain()));
            return;
        }
        if (frame instanceof ContinuationWebSocketFrame) {
            channel.write(new ContinuationWebSocketFrame(frame.content().retain()));
            return;
        }
        if (!(frame instanceof TextWebSocketFrame)) {
            webSocketServerHandshaker.close(channelHandlerContext.channel(), new CloseWebSocketFrame());
            return;
        }
        final String date = new Date().toString();
        final ChannelId channelId = channel.id();
        final String request = ((TextWebSocketFrame) frame).text();
        final String text = date + channelId + "：" + request;
        respond(channelHandlerContext, new TextWebSocketFrame(text));
    }

    /**
     * Handler http request
     *
     * @param ctx         Netty channel context
     * @param httpRequest An HTTP request.
     */
    private void handleHttpRequest(ChannelHandlerContext ctx,
                                   io.netty.handler.codec.http.HttpRequest httpRequest) {
        final DefaultFullHttpRequest fullHttpRequest = new DefaultFullHttpRequest(
                httpRequest.protocolVersion(), httpRequest.method(), httpRequest.uri());
        if (isWebSocketRequest(httpRequest)) {
            final WebSocketServerHandshakerFactory websocketFactory =
                    new WebSocketServerHandshakerFactory(httpRequest.uri(),
                                                         null, true);
            webSocketServerHandshaker = websocketFactory.newHandshaker(httpRequest);
            if (webSocketServerHandshaker == null) {
                WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
            } else {
                webSocketServerHandshaker.handshake(ctx.channel(), httpRequest);
            }
        } else {
            ReferenceCountUtil.retain(httpRequest);
            ctx.fireChannelRead(httpRequest);
        }
    }

    private boolean isWebSocketRequest(HttpRequest httpRequest) {
        String uri = Strings.commonPrefix(httpRequest.uri(), "?");
        return httpRequest.decoderResult().isSuccess() && "websocket".equals(
                httpRequest.headers().get("Upgrade"));
    }

    private void respond(ChannelHandlerContext context, WebSocketFrame webSocketFrame) {
        context.writeAndFlush(webSocketFrame);
    }

    private void respond(ChannelHandlerContext context, boolean isKeepAlive, DefaultFullHttpResponse response) {
        if (response.status().code() != HttpStatus.OK.code()) {
            ByteBuf byteBuf = Unpooled.copiedBuffer(response.status().toString(), CharsetUtil.UTF_8);
            response.content().writeBytes(byteBuf);
            byteBuf.release();
        }
        final ChannelFuture future = getChannel(context).writeAndFlush(response);
        if (!isKeepAlive || response.status().code() != 200) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }
}
