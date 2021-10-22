module microspace {
    requires static com.google.common;
    requires static micrometer.core;
    requires static jsr305;
    requires static io.netty.transport;
    requires static io.netty.transport.epoll;
    requires static io.netty.common;
    requires static io.netty.handler;
    requires static org.checkerframework.checker.qual;
    requires static com.google.errorprone.annotations;
    requires static com.fasterxml.jackson.core;
    requires static banana;
    requires static reflections8;
    requires static juel.api;
    requires static juel.impl;
    requires static io.netty.codec;
    requires static io.netty.buffer;
    requires static io.netty.codec.http;
    requires org.slf4j;

    exports io.microspace.context;
    exports io.microspace.expression;
    exports io.microspace.inject;
    exports io.microspace.internal;
    exports io.microspace.server;
}