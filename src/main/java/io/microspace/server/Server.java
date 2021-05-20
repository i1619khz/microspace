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

import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.microspace.utils.FreePortFinder;
import io.microspace.utils.ServerThreadNamer;
import io.microspace.utils.TransportType;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static io.netty.handler.ssl.ApplicationProtocolConfig.Protocol.ALPN;
import static io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT;
import static io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE;
import static io.netty.handler.ssl.ApplicationProtocolNames.HTTP_1_1;
import static io.netty.handler.ssl.ApplicationProtocolNames.HTTP_2;
import static java.util.Objects.requireNonNull;

/**
 * @author i1619kHz
 */
public final class Server {
    private static final Logger log = LoggerFactory.getLogger(Server.class);

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final Set<ServerChannel> serverChannels = new CopyOnWriteArraySet<>();
    private final Map<InetSocketAddress, ServerPort> activePorts = new LinkedHashMap<>();
    private final Stopwatch startupWatch = Stopwatch.createUnstarted();
    private final ServerConfig config;
    private SslContext sslContext;
    private EventLoopGroup parentGroup;
    private EventLoopGroup workerGroup;
    private ConnectionLimitHandler connectionLimitHandler;

    Server(ServerConfig config, SslContext sslContext) {
        this.config = config;
        this.sslContext = sslContext;

        setupMetrics();
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
        return null != keyPath ? Paths.get(keyPath).toFile() : defaultFilePath;
    }

    /**
     * Sets up the version metrics.
     */
    void setupMetrics() {
        final MeterRegistry meterRegistry = config().meterRegistry();
        final List<Tag> tags = ImmutableList.of(Tag.of("version", "1.0"),
                Tag.of("commit", "2000"),
                Tag.of("repo.status", "final"));
        Gauge.builder("microspace.build.info", () -> 1)
                .tags(tags)
                .description("A metric with a constant '1' value labeled by version and commit hash" +
                        " from which Microspace was built.")
                .register(meterRegistry);
    }

    private SslProvider createSslProvider() {
        return SslProvider.isAlpnSupported(SslProvider.OPENSSL) ? SslProvider.OPENSSL : SslProvider.JDK;
    }

    private ApplicationProtocolConfig createApplicationProtocolConfig() {
        return new ApplicationProtocolConfig(ALPN, NO_ADVERTISE, ACCEPT, HTTP_2, HTTP_1_1);
    }

    /**
     * Start http server
     */
    public void start() {
        startupWatch.start();
        try {
            if (config().useSsl()) {
                final SelfSignedCertificate ssc = new SelfSignedCertificate();
                final ApplicationProtocolConfig config = createApplicationProtocolConfig();

                final File sslCertFile = setKeyCertFileAndPriKey(config().sslCert(), ssc.certificate());
                final File sslPrivateKeyFile = setKeyCertFileAndPriKey(config().sslPrivateKey(), ssc.privateKey());
                final String privateKeyPass = config().sslPrivateKeyPass();

                final SupportedCipherSuiteFilter suiteFilter = SupportedCipherSuiteFilter.INSTANCE;
                final List<String> ciphers = Http2SecurityUtil.CIPHERS;
                final SslProvider provider = createSslProvider();

                sslContext = SslContextBuilder.forServer(sslCertFile, sslPrivateKeyFile, privateKeyPass)
                        .applicationProtocolConfig(config).ciphers(ciphers, suiteFilter).sslProvider(provider).build();
            }
        } catch (Exception e) {
            log.error("Build SslContext exception", e);
        }

        if (!isRunning()) {
            final ServerBootstrap serverBootstrap = new ServerBootstrap();
            this.parentGroup = createParentEventLoopGroup();
            this.workerGroup = createWorkerEventLoopGroup();

            connectionLimitHandler = new ConnectionLimitHandler(config().maxNumConnections());
            final HttpServerInitializer initializer = new HttpServerInitializer(config, sslContext);
            serverBootstrap.group(parentGroup, workerGroup).handler(connectionLimitHandler)
                    .channel(transportChannel()).childHandler(initializer);

            config.banner().printBanner(config.bannerText(), config.bannerFont());
            processOptions(config().channelOptions(), serverBootstrap::option);
            processOptions(config().childChannelOptions(), serverBootstrap::option);

            // Initialize the server sockets asynchronously.
            final CompletableFuture<Void> future = new CompletableFuture<>();
            final CompletableFuture<Void> startupWatchFuture = new CompletableFuture<>();
            List<ServerPort> ports = config().ports();
            final Iterator<ServerPort> it = ports.iterator();
            assert it.hasNext();

            final ServerPort primary = it.next();
            final AtomicInteger attempts = new AtomicInteger(0);
            config().startStopExecutor().execute(() -> bindServerToHost(serverBootstrap, primary, attempts)
                    .addListener(new ServerPortStartListener(primary))
                    .addListener(new BindNextPortListener(future, it, serverBootstrap))
                    .addListener((ChannelFutureListener) f -> {
                        if (!f.isSuccess()) {
                            startupWatchFuture.completeExceptionally(f.cause());
                            return;
                        }
                        startupWatch.stop();
                        if (log.isInfoEnabled()) {
                            log.info("Serving startup time {}{}", startupWatch.elapsed().toMillis(), "ms");
                        }
                    }));
        }
    }

    private ChannelFuture bindServerToHost(ServerBootstrap serverBootstrap, ServerPort serverPort, AtomicInteger attempts) {
        final String host = serverPort.host();
        int port = serverPort.port();
        final boolean isRandomPort = port == -1;

        try {
            isRunning.compareAndSet(false, true);
            if (host != null) {
                return serverBootstrap.bind(host, port).sync();
            } else {
                return serverBootstrap.bind(port).sync();
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
                port = FreePortFinder.findFreeLocalPort(port);
                return bindServerToHost(serverBootstrap,
                        new ServerPort(port, serverPort.protocols()), attempts);
            } else {
                throw new ServerStartupException("Unable to start Microspace server on port: " + port, e);
            }
        }
    }

    private static boolean isLocalPort(ServerPort serverPort) {
        final InetAddress address = serverPort.localAddress().getAddress();
        return address.isAnyLocalAddress() || address.isLoopbackAddress();
    }

    private final class BindNextPortListener implements ChannelFutureListener {
        private final CompletableFuture<Void> future;
        private final Iterator<ServerPort> it;
        private final ServerBootstrap serverBootstrap;

        private BindNextPortListener(CompletableFuture<Void> future, Iterator<ServerPort> it,
                                     ServerBootstrap serverBootstrap) {
            this.future = future;
            this.it = it;
            this.serverBootstrap = serverBootstrap;
        }

        @Override
        public void operationComplete(ChannelFuture f) {
            if (!f.isSuccess()) {
                future.completeExceptionally(f.cause());
                return;
            }
            if (!it.hasNext()) {
                future.complete(null);
                return;
            }

            final ServerPort next = it.next();
            AtomicInteger attempts = new AtomicInteger(0);
            bindServerToHost(serverBootstrap, next, attempts)
                    .addListener(new ServerPortStartListener(next)).addListener(this);
        }
    }

    private final class ServerPortStartListener implements ChannelFutureListener {

        private final ServerPort port;

        ServerPortStartListener(ServerPort port) {
            this.port = requireNonNull(port, "port");
        }

        @Override
        public void operationComplete(ChannelFuture f) {
            final ServerChannel ch = (ServerChannel) f.channel();
            assert ch.eventLoop().inEventLoop();
            serverChannels.add(ch);

            if (f.isSuccess()) {
                final InetSocketAddress localAddress = (InetSocketAddress) ch.localAddress();
                final ServerPort actualPort = new ServerPort(localAddress, port.protocols());

                // Update the boss thread so its name contains the actual port.
                Thread.currentThread().setName(eventLoopGroupName(actualPort, "parent"));

                synchronized (activePorts) {
                    // Update the map of active ports.
                    activePorts.put(localAddress, actualPort);
                }

                if (config().mainType() != null) {
                    String applicationName = config().mainType().getName();
                    if (isLocalPort(actualPort)) {
                        port.protocols().forEach(p -> log.info(
                                "Binding {} Serving {} at {} - {}://127.0.0.1:{}/", applicationName,
                                p.name(), localAddress, p.uriText(), localAddress.getPort()));
                    }
                } else {
                    if (log.isInfoEnabled()) {
                        log.info("Serving {} at {}", Joiner.on('+').join(port.protocols()), localAddress);
                    }
                }
            }
        }
    }

    public void stop() {
        config().startStopExecutor().execute(() -> {
            final Stopwatch stopwatch = Stopwatch.createStarted();
            if (isRunning() && workerGroup != null) {
                if (isRunning.compareAndSet(true, false)) {
                    stopServerAndGroup();
                }

                stopwatch.stop();
                if (log.isInfoEnabled()) {
                    log.info("Serving stop time {}{}", stopwatch.elapsed().toMillis(), "ms");
                }
            }
        });
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

        try {
            if (parentGroup.awaitTermination(3000, TimeUnit.MILLISECONDS)) {
                log.info("Parent EventLoopGroup is closed");
            }
            if (workerGroup.awaitTermination(3000, TimeUnit.MILLISECONDS)) {
                log.info("Worker EventLoopGroup is closed");
            }
        } catch (InterruptedException e) {
            log.error("Interrupt EventLoopGroup terminate", e);
            Thread.currentThread().interrupt();
        }

        final Set<Channel> channels = connectionLimitHandler.childChannels();
        if (!channels.isEmpty()) {
            final AtomicInteger numChannelsToClose = new AtomicInteger(channels.size());
            final CompletableFuture<Void> future = new CompletableFuture<>();
            final ChannelFutureListener listener = unused -> {
                if (numChannelsToClose.decrementAndGet() == 0) {
                    future.complete(null);
                }
            };
            for (Channel ch : channels) {
                ch.close().addListener(listener);
            }
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

    private EventLoopGroup createWorkerEventLoopGroup() {
        return createEventLoopGroup("worker" + config().serverThreadName());
    }

    private EventLoopGroup createParentEventLoopGroup() {
        return createEventLoopGroup("parent" + config().serverThreadName());
    }

    private ServerThreadNamer withThreadName(String prefix) {
        return new ServerThreadNamer(eventLoopGroupName(config().ports().get(0), prefix));
    }

    private Class<? extends ServerChannel> transportChannel() {
        return TransportType.detectTransportType().serverChannelType();
    }

    private static String eventLoopGroupName(ServerPort port, String prefix) {
        final InetSocketAddress localAddr = port.localAddress();
        final String localHostName =
                localAddr.getAddress().isAnyLocalAddress() ? "*" : localAddr.getHostString();

        // e.g. 'microspace-boss-http-*:8080'
        //      'microspace-boss-http-127.0.0.1:8443'
        //      'microspace-boss-proxy+http+https-127.0.0.1:8443'
        final String protocolNames = port.protocols().stream()
                .map(SessionProtocol::uriText)
                .collect(Collectors.joining("+"));
        return "microspace-" + prefix + "-" + protocolNames + '-' + localHostName + ':' + localAddr.getPort();
    }

    private EventLoopGroup createEventLoopGroup(String threadName) {
        return TransportType.detectTransportType()
                .newEventLoopGroup(config().ioThreadCount(),
                        transportType -> withThreadName(threadName));
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    public ServerConfig config() {
        return config;
    }

    public Set<ServerChannel> serverChannels() {
        return serverChannels;
    }

    public Map<InetSocketAddress, ServerPort> activePorts() {
        return activePorts;
    }
}
