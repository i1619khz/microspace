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

import static java.util.Objects.requireNonNull;

import io.microspace.server.websocket.WebSocketHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.ssl.SslContext;

/**
 * @author i1619kHz
 */
final class HttpServerInitializer extends ChannelInitializer<SocketChannel> {
    private static final WriteBufferWaterMark DISABLED_WRITE_BUFFER_WATERMARK =
            new WriteBufferWaterMark(0, Integer.MAX_VALUE);

    private final SslContext sslContext;
    private final ServerConfig serverConfig;

    HttpServerInitializer(ServerConfig serverConfig, SslContext sslContext) {
        requireNonNull(serverConfig, "serverConfig");
        this.sslContext = sslContext;
        this.serverConfig = serverConfig;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.config().setWriteBufferWaterMark(DISABLED_WRITE_BUFFER_WATERMARK);

        final ChannelPipeline pipeline = ch.pipeline();
        if (null != sslContext && serverConfig.useSsl()) {
            pipeline.addLast(sslContext.newHandler(ch.alloc()));
        }

        pipeline.addLast(new FlushConsolidationHandler());
        pipeline.addLast(new ReadSuppressingHandler());
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(512 * 1024));
        pipeline.addLast(new HttpServerExpectContinueHandler());
        pipeline.addLast(new WebSocketHandler());
        pipeline.addLast(new HttpServerHandler(serverConfig));
    }
}
