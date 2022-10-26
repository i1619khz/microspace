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

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.ssl.SslContext;

/**
 * @author i1619kHz
 */
final class HttpServerConfigurator extends ChannelInitializer<Channel> {
    private static final WriteBufferWaterMark DISABLED_WRITE_BUFFER_WATERMARK =
            new WriteBufferWaterMark(0, Integer.MAX_VALUE);

    private final SslContext sslContext;
    private final ServerConfig serverConfig;

    HttpServerConfigurator(ServerConfig serverConfig, SslContext sslContext) {
        requireNonNull(serverConfig, "serverConfig");
        this.sslContext = sslContext;
        this.serverConfig = serverConfig;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        // The remote Address method is called at least once to cache the remote address
        ch.remoteAddress();

        // Disable the write buffer watermark notification because we manage backpressure by ourselves.
        ch.config().setWriteBufferWaterMark(DISABLED_WRITE_BUFFER_WATERMARK);

        final ChannelPipeline pipeline = ch.pipeline();
        if (null != sslContext && serverConfig.useSsl()) {
            pipeline.addLast(sslContext.newHandler(ch.alloc()));
        }

        pipeline.addLast(new FlushConsolidationHandler());
        pipeline.addLast(ReadSuppressingHandler.INSTANCE);
        configurePipeline(pipeline);
    }

    private void configurePipeline(ChannelPipeline pipeline) {
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpServerExpectContinueHandler());
        pipeline.addLast(new HttpObjectAggregator(512 * 1024));
        pipeline.addLast(new HttpServerExpectContinueHandler());
        pipeline.addLast(TrafficLoggingHandler.SERVER);
        pipeline.addLast(new HttpWebSocketHandler());
        pipeline.addLast(new HttpServerHandler(serverConfig));
    }
}
