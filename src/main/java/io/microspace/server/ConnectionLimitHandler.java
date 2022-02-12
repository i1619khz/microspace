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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;

/**
 * @author i1619kHz
 */
@Sharable
final class ConnectionLimitHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionLimitHandler.class);

    private final int maxConnectionNum;
    private final AtomicLong numConnections = new AtomicLong(0);
    private final LongAdder numDroppedConnections = new LongAdder();
    private final AtomicBoolean loggingScheduled = new AtomicBoolean(false);
    private final Set<Channel> childChannels = Collections.newSetFromMap(new ConcurrentHashMap<>());

    ConnectionLimitHandler(int maxConnectionNums) {
        this.maxConnectionNum = maxConnectionNums;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel channel = (Channel) msg;
        long conn = numConnections.incrementAndGet();

        if (conn > 0 && conn <= maxConnectionNum) {
            this.childChannels.add(channel);

            channel.closeFuture().addListener(future -> {
                childChannels.remove(channel);
                numConnections.decrementAndGet();
            });
            super.channelRead(ctx, msg);
        } else {
            numConnections.decrementAndGet();

            // Set linger option to 0 so that the server doesn't get too many TIME_WAIT states.
            channel.config().setOption(ChannelOption.SO_LINGER, 0);
            channel.unsafe().closeForcibly();

            numDroppedConnections.increment();

            if (loggingScheduled.compareAndSet(false, true)) {
                ctx.executor().schedule(this::writeNumDroppedConnectionsLog, 1, TimeUnit.SECONDS);
            }
        }
    }

    private void writeNumDroppedConnectionsLog() {
        loggingScheduled.set(false);

        final long dropped = numDroppedConnections.sumThenReset();
        if (dropped > 0) {
            logger.warn("Dropped {} connection(s) to limit the number of open connections to {}",
                     dropped, maxConnectionNum);
        }
    }

    public int maxConnectionNum() {
        return maxConnectionNum;
    }

    public AtomicLong connectionsNum() {
        return numConnections;
    }

    public LongAdder numDroppedConnections() {
        return numDroppedConnections;
    }

    public Set<Channel> childChannels() {
        return childChannels;
    }
}
