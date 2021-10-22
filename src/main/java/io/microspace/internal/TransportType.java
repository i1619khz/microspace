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
package io.microspace.internal;

import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableSet;

import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * Native transport types.
 */
public enum TransportType {

    NIO(NioServerSocketChannel.class, NioSocketChannel.class, NioDatagramChannel.class,
        NioEventLoopGroup::new, NioEventLoopGroup.class, NioEventLoop.class),

    EPOLL(EpollServerSocketChannel.class, EpollSocketChannel.class, EpollDatagramChannel.class,
          EpollEventLoopGroup::new, EpollEventLoopGroup.class, Epolls.epollEventLoopClass());

    /**
     * Returns the available {@link TransportType}.
     */
    public static TransportType detectTransportType() {
        if (Epolls.epollIsAvailable()) {
            return EPOLL;
        } else {
            return NIO;
        }
    }

    /**
     * Returns the {@link ServerChannel} class for {@code eventLoopGroup}.
     */
    public static Class<? extends ServerChannel> serverChannelType(EventLoopGroup eventLoopGroup) {
        return find(eventLoopGroup).serverChannelType;
    }

    /**
     * Returns the available {@link SocketChannel} class for {@code eventLoopGroup}.
     */
    public static Class<? extends SocketChannel> socketChannelType(EventLoopGroup eventLoopGroup) {
        return find(eventLoopGroup).socketChannelType;
    }

    /**
     * Returns the available {@link DatagramChannel} class for {@code eventLoopGroup}.
     */
    public static Class<? extends DatagramChannel> datagramChannelType(EventLoopGroup eventLoopGroup) {
        return find(eventLoopGroup).datagramChannelType;
    }

    /**
     * Returns whether the specified {@link EventLoop} supports any {@link TransportType}.
     */
    public static boolean isSupported(EventLoop eventLoop) {
        final EventLoopGroup parent = eventLoop.parent();
        if (parent == null) {
            return false;
        }
        return isSupported(parent);
    }

    /**
     * Returns whether the specified {@link EventLoopGroup} supports any {@link TransportType}.
     */
    public static boolean isSupported(EventLoopGroup eventLoopGroup) {
        return findOrNull(eventLoopGroup) != null;
    }

    private final Class<? extends ServerChannel> serverChannelType;
    private final Class<? extends SocketChannel> socketChannelType;
    private final Class<? extends DatagramChannel> datagramChannelType;
    private final Set<Class<? extends EventLoopGroup>> eventLoopGroupClasses;
    private final BiFunction<Integer, ThreadFactory, ? extends EventLoopGroup> eventLoopGroupConstructor;

    @SafeVarargs
    TransportType(Class<? extends ServerChannel> serverChannelType,
                  Class<? extends SocketChannel> socketChannelType,
                  Class<? extends DatagramChannel> datagramChannelType,
                  BiFunction<Integer, ThreadFactory, ? extends EventLoopGroup> eventLoopGroupConstructor,
                  Class<? extends EventLoopGroup>... eventLoopGroupClasses) {
        this.serverChannelType = serverChannelType;
        this.socketChannelType = socketChannelType;
        this.datagramChannelType = datagramChannelType;
        this.eventLoopGroupClasses = ImmutableSet.copyOf(eventLoopGroupClasses);
        this.eventLoopGroupConstructor = eventLoopGroupConstructor;
    }

    private static TransportType find(EventLoopGroup eventLoopGroup) {
        final TransportType found = findOrNull(eventLoopGroup);
        if (found == null) {
            throw unsupportedEventLoopType(eventLoopGroup);
        }
        return found;
    }

    @Nullable
    private static TransportType findOrNull(EventLoopGroup eventLoopGroup) {
        for (TransportType type : values()) {
            for (Class<? extends EventLoopGroup> eventLoopGroupClass : type.eventLoopGroupClasses) {
                if (eventLoopGroupClass.isAssignableFrom(eventLoopGroup.getClass())) {
                    return type;
                }
            }
        }
        return null;
    }

    private static IllegalStateException unsupportedEventLoopType(EventLoopGroup eventLoopGroup) {
        return new IllegalStateException("unsupported event loop type: " +
                                         eventLoopGroup.getClass().getName());
    }

    /**
     * Returns the {@link ServerChannel} class that is available for this transport type.
     */
    public Class<? extends ServerChannel> serverChannelType() {
        return serverChannelType;
    }

    /**
     * Returns lowercase name of {@link TransportType}.
     * This method is a shortcut for:
     * <pre>{@code
     * Ascii.toLowerCase(name());
     * }</pre>
     */
    public String lowerCasedName() {
        return Ascii.toLowerCase(name());
    }

    /**
     * Creates the available {@link EventLoopGroup}.
     */
    public EventLoopGroup newEventLoopGroup(int nThreads,
                                            Function<TransportType, ThreadFactory> threadFactoryFactory) {
        final ThreadFactory threadFactory = threadFactoryFactory.apply(this);
        return eventLoopGroupConstructor.apply(nThreads, threadFactory);
    }
}
