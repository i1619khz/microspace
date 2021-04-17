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
package io.microspace.core;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.AbstractEventLoop;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Future;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Provides methods that are useful for creating an {@link EventLoopGroup}.
 */
public final class EventLoopGroups {

  private static final EventLoop directEventLoop = new DirectEventLoop();

  private EventLoopGroups() {}

  /**
   * Returns a newly-created {@link EventLoopGroup}.
   *
   * @param numThreads the number of event loop threads
   */
  public static EventLoopGroup newEventLoopGroup(int numThreads) {
    return newEventLoopGroup(numThreads, false);
  }

  /**
   * Returns a newly-created {@link EventLoopGroup}.
   *
   * @param numThreads       the number of event loop threads
   * @param useDaemonThreads whether to create daemon threads or not
   */
  public static EventLoopGroup newEventLoopGroup(int numThreads, boolean useDaemonThreads) {
    return newEventLoopGroup(numThreads, "armeria-eventloop", useDaemonThreads);
  }

  /**
   * Returns a newly-created {@link EventLoopGroup}.
   *
   * @param numThreads       the number of event loop threads
   * @param threadNamePrefix the prefix of thread names
   */
  public static EventLoopGroup newEventLoopGroup(int numThreads, String threadNamePrefix) {
    return newEventLoopGroup(numThreads, threadNamePrefix, false);
  }

  /**
   * Returns a newly-created {@link EventLoopGroup}.
   *
   * @param numThreads       the number of event loop threads
   * @param threadNamePrefix the prefix of thread names
   * @param useDaemonThreads whether to create daemon threads or not
   */
  public static EventLoopGroup newEventLoopGroup(int numThreads, String threadNamePrefix,
                                                 boolean useDaemonThreads) {

    checkArgument(numThreads > 0, "numThreads: %s (expected: > 0)", numThreads);
    requireNonNull(threadNamePrefix, "threadNamePrefix");

    final TransportType type = TransportType.detectTransportType();
    final String prefix = threadNamePrefix + '-' + type.lowerCasedName();
    return newEventLoopGroup(numThreads, ThreadFactories.newEventLoopThreadFactory(prefix,
        useDaemonThreads));
  }

  /**
   * Returns a newly-created {@link EventLoopGroup}.
   *
   * @param numThreads    the number of event loop threads
   * @param threadFactory the factory of event loop threads
   */
  public static EventLoopGroup newEventLoopGroup(int numThreads, ThreadFactory threadFactory) {

    checkArgument(numThreads > 0, "numThreads: %s (expected: > 0)", numThreads);
    requireNonNull(threadFactory, "threadFactory");

    final TransportType type = TransportType.detectTransportType();
    return type.newEventLoopGroup(numThreads, unused -> threadFactory);
  }

  /**
   * Returns a special {@link EventLoop} which executes submitted tasks in the caller thread.
   * Note that this {@link EventLoop} will raise an {@link UnsupportedOperationException} for any operations
   * related with {@link EventLoop} shutdown or {@link Channel} registration.
   */
  public static EventLoop directEventLoop() {
    return directEventLoop;
  }

  /**
   * Returns the {@link ServerChannel} class that is available for this {@code eventLoopGroup}, for use in
   * configuring a custom {@link Bootstrap}.
   */
  public static Class<? extends ServerChannel> serverChannelType(EventLoopGroup eventLoopGroup) {
    return TransportType.serverChannelType(requireNonNull(eventLoopGroup, "eventLoopGroup"));
  }

  /**
   * Returns the available {@link SocketChannel} class for {@code eventLoopGroup}, for use in configuring a
   * custom {@link Bootstrap}.
   */
  public static Class<? extends SocketChannel> socketChannelType(EventLoopGroup eventLoopGroup) {
    return TransportType.socketChannelType(requireNonNull(eventLoopGroup, "eventLoopGroup"));
  }

  /**
   * Returns the available {@link DatagramChannel} class for {@code eventLoopGroup}, for use in configuring a
   * custom {@link Bootstrap}.
   */
  public static Class<? extends DatagramChannel> datagramChannelType(EventLoopGroup eventLoopGroup) {
    return TransportType.datagramChannelType(requireNonNull(eventLoopGroup, "eventLoopGroup"));
  }

  private static final class DirectEventLoop extends AbstractEventLoop {
    @Override
    public ChannelFuture register(Channel channel) {
      throw new UnsupportedOperationException();
    }

    @Override
    public ChannelFuture register(ChannelPromise promise) {
      throw new UnsupportedOperationException();
    }

    @Override
    public ChannelFuture register(Channel channel, ChannelPromise promise) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void shutdown() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean inEventLoop(Thread thread) {
      return true;
    }

    @Override
    public boolean isShuttingDown() {
      return false;
    }

    @Override
    public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Future<?> terminationFuture() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isShutdown() {
      return false;
    }

    @Override
    public boolean isTerminated() {
      return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
      return false;
    }

    @Override
    public void execute(Runnable command) {
      command.run();
    }

    @Override
    public String toString() {
      return EventLoopGroups.class.getSimpleName() + ".directEventLoop()";
    }
  }
}
