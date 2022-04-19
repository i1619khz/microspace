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

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static java.util.Objects.requireNonNull;

import io.microspace.server.annotation.ExceptionHandlerFunction;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.CharsetUtil;

/**
 * @author i1619kHz
 */
@Sharable
final class HttpServerHandler extends ChannelInboundHandlerAdapter {
    private final ServerConfig serverConfig;

    public HttpServerHandler(ServerConfig serverConfig) {
        this.serverConfig = requireNonNull(serverConfig, "config");
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        final Routers routers = Routers.ofServer(serverConfig);
        final ServiceConfig serviceConfig = routers.findServiceConfig("/");
        if (null == serviceConfig) {
            writeResponse(ctx, "404");
        } else {
            final Route route = serviceConfig.route();
            final HttpService service = serviceConfig.service();
            final Class<? extends HttpService> ref = service.getClass();
            service.serve(null);
            writeResponse(ctx, "");
        }
    }

    private void writeResponse(ChannelHandlerContext ctx, String resp) {
        final FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, OK, Unpooled.copiedBuffer(resp, CharsetUtil.UTF_8));
        ctx.write(response).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        final ExceptionHandlerFunction function = serverConfig.exceptionServices().get(cause.getClass());
        ctx.close();
    }
}
