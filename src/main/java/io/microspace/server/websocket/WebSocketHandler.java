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
package io.microspace.server.websocket;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.microspace.server.HttpStatus;
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
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
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
public final class WebSocketHandler extends SimpleChannelInboundHandler<Object> {
    private final ChannelGroup channelGroup;
    private final ConcurrentMap<String, ChannelId> channelMap;
    private WebSocketServerHandshaker handShaker;

    public WebSocketHandler() {
        this.channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        this.channelMap = new ConcurrentHashMap();
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
        if (message instanceof FullHttpRequest) {
            handleHttpRequest(channelHandlerContext, (FullHttpRequest) message);
        } else if (message instanceof WebSocketFrame) {
            handlerWebSocketFrame(channelHandlerContext, (WebSocketFrame) message);
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

    private void handlerWebSocketFrame(ChannelHandlerContext context, WebSocketFrame frame) {
        final Channel channel = getChannel(context);
        if (frame instanceof CloseWebSocketFrame) {
            handShaker.close(channel, (CloseWebSocketFrame) frame.retain());
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
            throw new UnsupportedOperationException(String.format(
                    "%s frame types not supported", frame.getClass().getName()));
        }
        final String date = new Date().toString();
        final ChannelId channelId = getChannel(context).id();
        final String request = ((TextWebSocketFrame) frame).text();
        final String text = date + channelId + "：" + request;
        respond(context, new TextWebSocketFrame(text));
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest httpRequest) {
        if (isWebSocketRequest(httpRequest)) {
            final DefaultFullHttpResponse defaultFullHttpResponse = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
            respond(ctx, HttpUtil.isKeepAlive(httpRequest), defaultFullHttpResponse);
            return;
        }
        final WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                "ws://localhost:8081/websocket", null, false);
        handShaker = wsFactory.newHandshaker(httpRequest);
        if (handShaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(getChannel(ctx));
        } else {
            handShaker.handshake(getChannel(ctx), httpRequest);
        }
    }

    private boolean isWebSocketRequest(FullHttpRequest httpRequest) {
        return (!httpRequest.decoderResult().isSuccess() ||
                (!"websocket".equals(httpRequest.headers().get("Upgrade"))));
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
