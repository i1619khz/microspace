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
module microspace {
    requires static com.google.common;
    requires static micrometer.core;
    requires static reflections8;
    requires static jsr305;
    requires static io.netty.transport;
    requires static io.netty.transport.epoll;
    requires static io.netty.common;
    requires static io.netty.handler;
    requires static io.netty.codec;
    requires static io.netty.buffer;
    requires static io.netty.codec.http;
    requires static org.checkerframework.checker.qual;
    requires static com.google.errorprone.annotations;
    requires static com.fasterxml.jackson.core;
    requires static banana;
    requires static juel.api;
    requires static juel.impl;
    requires org.slf4j;

    exports io.microspace.context;
    exports io.microspace.expression;
    exports io.microspace.inject;
    exports io.microspace.internal;
    exports io.microspace.server;
    exports io.microspace.aop;
}