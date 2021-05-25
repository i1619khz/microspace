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

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;

/**
 * @author i1619kHz
 */
public final class Epolls {
    private static final Class<? extends EventLoopGroup> EPOLL_EVENT_LOOP_CLASS;

    static {
        try {
            //noinspection unchecked
            EPOLL_EVENT_LOOP_CLASS = (Class<? extends EventLoopGroup>)
                    Class.forName("io.netty.channel.epoll.EpollEventLoop", false,
                            EpollEventLoopGroup.class.getClassLoader());
        } catch (Exception e) {
            throw new IllegalStateException("failed to locate EpollEventLoop class", e);
        }
    }

    private Epolls() {/* no thing */}

    public static boolean epollIsAvailable() {
        try {
            Object obj = Class.forName("io.netty.channel.epoll.Epoll")
                    .getMethod("isAvailable").invoke(null);
            return null != obj && Boolean.parseBoolean(obj.toString())
                    && System.getProperty("os.name").toLowerCase().contains("linux");
        } catch (Exception e) {
            return false;
        }
    }

    public static Class<? extends EventLoopGroup> epollEventLoopClass() {
        return EPOLL_EVENT_LOOP_CLASS;
    }
}
