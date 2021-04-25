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

import io.microspace.core.FreePortFinder;
import io.microspace.core.ServerThreadNamer;
import io.microspace.core.TransportType;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * @author i1619kHz
 */
public final class Server {
    private static final Logger log = LoggerFactory.getLogger(Server.class);

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final ServerConfig config;
    private SslContext sslContext;
    private EventLoopGroup parentGroup;
    private EventLoopGroup workerGroup;

    Server(ServerConfig config, SslContext sslContext) {
        this.config = config;
        this.sslContext = sslContext;
    }

    public static ServerBuilder builder() {
        return new ServerBuilder();
    }

    /**
     * Use the configured path if the certificate and private key are
     * configured, otherwise use the default configuration
     *
     * @param keyPath         private keystore path
     * @param defaultFilePath default private keystore path
     * @return keystore file
     */
    private File setKeyCertFileAndPriKey(String keyPath, File defaultFilePath) {
        return keyPath != null ? Paths.get(keyPath).toFile() : defaultFilePath;
    }

    public void start() {
        try {
            final boolean ssl = config.useSsl();
            final SelfSignedCertificate ssc = new SelfSignedCertificate();

            if (!ssl) {
                sslContext = null;
            }

            final File sslCertFile = setKeyCertFileAndPriKey(config.sslCert(), ssc.certificate());
            final File sslPrivateKeyFile = setKeyCertFileAndPriKey(config.sslPrivateKey(), ssc.privateKey());

            sslContext = SslContextBuilder.forServer(sslCertFile,
                    sslPrivateKeyFile, config.sslPrivateKeyPass()).build();
        } catch (Exception e) {
            log.error("Build SslContext exception", e);
        }

        if (!isRunning()) {
            final ServerBootstrap serverBootstrap = new ServerBootstrap();
            this.parentGroup = createParentEventLoopGroup();
            this.workerGroup = createWorkerEventLoopGroup();

            final HttpServerInitializer initializer = new HttpServerInitializer(sslContext);
            serverBootstrap.group(parentGroup, workerGroup).handler(createAcceptLimiter())
                    .channel(transportChannel()).childHandler(initializer);

            processOptions(config.channelOptions(), serverBootstrap::option);
            processOptions(config.childChannelOptions(), serverBootstrap::option);

            bindServerToHost(serverBootstrap, config.serverPort().host(),
                    config.serverPort().port(), new AtomicInteger(0));
        }
    }

    private void bindServerToHost(ServerBootstrap serverBootstrap, String host, int port, AtomicInteger attempts) {
        final boolean isRandomPort = port == -1;

        if (config().mainType() != null) {
            String applicationName = config.mainType().getName();
            if (log.isInfoEnabled()) {
                log.info("Binding {} server to {}:{}", applicationName, host != null ? host : "*", port);
            }
        } else {
            if (log.isInfoEnabled()) {
                log.info("Binding server to {}:{}", host != null ? host : "*", port);
            }
        }

        try {
            if (host != null) {
                serverBootstrap.bind(host, port).sync();
            } else {
                serverBootstrap.bind(port).sync();
            }
        } catch (Throwable e) {
            final boolean isBindError = isBindError(e);

            if (log.isErrorEnabled()) {
                if (isBindError) {
                    log.error("Unable to start server. Port already {} in use.", port);
                } else {
                    log.error("Error starting Microspace server: " + e.getMessage(), e);
                }
            }

            final int attemptCount = attempts.getAndIncrement();
            final int restartCount = config().serverRestartCount();

            if (isRandomPort && attemptCount < restartCount) {
                port = FreePortFinder.findFreeLocalPort();
                bindServerToHost(serverBootstrap, host, port, attempts);
            } else {
                throw new ServerStartupException("Unable to start Microspace server on port: " + port, e);
            }
        }
    }

    public void stop() {
        if (isRunning() && workerGroup != null) {
            if (isRunning.compareAndSet(true, false)) {
                stopServerAndGroup();
            }
        }
    }

    private void stopServerAndGroup() {
        final long quietPeriod = Duration.ofSeconds(2).toMillis();
        final long timeout = Duration.ofSeconds(15).toMillis();
        if (parentGroup != null) {
            parentGroup.shutdownGracefully(quietPeriod, timeout, TimeUnit.MILLISECONDS)
                    .addListener(this::logShutdownErrorIfNecessary);
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully(quietPeriod, timeout, TimeUnit.MILLISECONDS)
                    .addListener(this::logShutdownErrorIfNecessary);
        }
    }

    private void logShutdownErrorIfNecessary(Future<?> future) {
        if (!future.isSuccess()) {
            if (log.isWarnEnabled()) {
                Throwable e = future.cause();
                log.warn("Error stopping Microspace server: " + e.getMessage(), e);
            }
        }
    }

    private boolean isBindError(Throwable e) {
        return e.getClass().getName().equals(BindException.class.getName());
    }

    @SuppressWarnings("rawtypes")
    private void processOptions(Map<ChannelOption<?>, Object> options, BiConsumer<ChannelOption, Object> biConsumer) {
        options.forEach(biConsumer);
    }

    private ConnectionLimitHandler createAcceptLimiter() {
        return new ConnectionLimitHandler(config().maxNumConnections());
    }

    private EventLoopGroup createWorkerEventLoopGroup() {
        return createEventLoopGroup("worker" + config.serverThreadName());
    }

    private EventLoopGroup createParentEventLoopGroup() {
        return createEventLoopGroup("parent" + config.serverThreadName());
    }

    private ServerThreadNamer withThreadName(String prefix) {
        return new ServerThreadNamer(eventLoopGroupName(config().serverPort(), prefix));
    }

    private Class<? extends ServerChannel> transportChannel() {
        return TransportType.detectTransportType().serverChannelType();
    }

    private static String eventLoopGroupName(ServerPort port, String prefix) {
        final InetSocketAddress localAddr = port.localAddress();
        final String localHostName =
                localAddr.getAddress().isAnyLocalAddress() ? "*" : localAddr.getHostString();

        // e.g. 'armeria-boss-http-*:8080'
        //      'armeria-boss-http-127.0.0.1:8443'
        //      'armeria-boss-proxy+http+https-127.0.0.1:8443'
        final String protocolNames = port.protocols().stream()
                .map(SessionProtocol::uriText)
                .collect(Collectors.joining("+"));
        return "microspace-" + prefix + "-" + protocolNames + '-' + localHostName + ':' + localAddr.getPort();
    }

    private EventLoopGroup createEventLoopGroup(String threadName) {
        return TransportType.detectTransportType()
                .newEventLoopGroup(config.ioThreadCount(),
                        transportType -> withThreadName(threadName));
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    public ServerConfig config() {
        return config;
    }
}
