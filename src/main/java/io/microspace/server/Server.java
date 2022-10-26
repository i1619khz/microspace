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

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.microspace.internal.FreePortFinder;
import io.microspace.internal.ServerThreadNamer;
import io.microspace.internal.TransportType;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ImmediateEventExecutor;

/**
 * @author i1619kHz
 */
public final class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    public static ServerBuilder builder() {
        return new ServerBuilder();
    }

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final Set<ServerChannel> serverChannels = new CopyOnWriteArraySet<>();
    private final Map<InetSocketAddress, ServerPort> activePorts = new LinkedHashMap<>();
    private final Stopwatch startupWatch = Stopwatch.createUnstarted();
    private final ServerBootstrap serverBootstrap = new ServerBootstrap();
    private final ServerConfig config;
    private final SslContext sslContext;
    private EventLoopGroup workerGroup;
    private ConnectionLimitHandler connectionLimitHandler;

    Server(ServerConfig config, SslContext sslContext) {
        this.config = config;
        this.sslContext = sslContext;

        setupMetrics();
    }

    private static boolean isLocalPort(ServerPort serverPort) {
        final InetAddress address = serverPort.localAddress().getAddress();
        return address.isAnyLocalAddress() || address.isLoopbackAddress();
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
    private void setupMetrics() {
        final MeterRegistry meterRegistry = config.meterRegistry();
        final List<Tag> tags = ImmutableList.of(Tag.of("version", "1.0"),
                                                Tag.of("commit", "2000"),
                                                Tag.of("repo.status", "final"));
        Gauge.builder("microspace.build.info", () -> 1)
             .tags(tags)
             .description("A metric with a constant '1' value labeled by version and commit hash" +
                          " from which Microspace was built.")
             .register(meterRegistry);
    }

    public CompletableFuture<Void> start() {
        startupWatch.start();
        return start(false);
    }

    /**
     * Start http server
     * @return CompletableFuture
     */
    public CompletableFuture<Void> start(boolean registerShutdownHook) {
        if (!startupWatch.isRunning()) {
            startupWatch.start();
        }

        final EventLoopGroup parentGroup = createParentEventLoopGroup();
        workerGroup = createWorkerEventLoopGroup();

        connectionLimitHandler = new ConnectionLimitHandler(config.maxNumConnections());
        final HttpServerConfigurator initializer = new HttpServerConfigurator(config, sslContext);
        serverBootstrap.group(parentGroup, workerGroup).handler(connectionLimitHandler)
                       .channel(transportChannel()).childHandler(initializer);

        config.banner().printBanner(config.bannerText(), config.bannerFont());
        processOptions(config.channelOptions(), serverBootstrap::option);
        processOptions(config.childChannelOptions(), serverBootstrap::option);
        registerShutdownHook(registerShutdownHook);

        // Initialize the server sockets asynchronously.
        final List<ServerPort> ports = config.ports();
        final Iterator<ServerPort> it = ports.iterator();
        assert it.hasNext();

        final ServerPort primary = it.next();
        final AtomicInteger attempts = new AtomicInteger(0);
        final CompletableFuture<Void> future = new CompletableFuture<>();

        start(primary, attempts)
                .addListener(new ServerPortStartListener(primary))
                .addListener(new ChannelFutureListener() {
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
                        start(next, attempts).addListener(new ServerPortStartListener(next))
                                             .addListener(this);
                        startupWatch.stop();
                        if (logger.isInfoEnabled()) {
                            logger.info("Serving startup time {}{}",
                                        startupWatch.elapsed().toMillis(), "ms");
                        }
                    }
                });

        return future;
    }

    private void registerShutdownHook(boolean registerShutdownHook) {
        if (registerShutdownHook) {
            final Runtime runtime = Runtime.getRuntime();
            runtime.addShutdownHook(new Thread(this::stop));

            if (logger.isDebugEnabled()) {
                logger.debug("The shutdown hook has been registered, the " +
                             "service will call the stop method when the system is shut down");
            }
        }
    }

    private ChannelFuture start(ServerPort serverPort, AtomicInteger attempts) {
        final String host = serverPort.host();
        int port = serverPort.port();

        try {
            if (!isRunning.get()) {
                isRunning.compareAndSet(false, true);
            }
            if (host != null) {
                return serverBootstrap.bind(host, port).sync();
            } else {
                return serverBootstrap.bind(port).sync();
            }
        } catch (Throwable e) {
            final boolean isBindException = isBindException(e);

            if (logger.isErrorEnabled()) {
                if (isBindException) {
                    logger.error("Unable to start server. Port already {} in use.", port);
                } else {
                    logger.error("Error starting Microspace server: " + e.getMessage(), e);
                }
            }

            final int attemptCount = attempts.getAndIncrement();
            final int restartCount = config.serverRestartCount();

            if (attemptCount < restartCount) {
                port = FreePortFinder.findFreeLocalPort(port);
                return start(new ServerPort(port, serverPort.protocols()), attempts);
            } else {
                throw new ServerStartupException("Unable to start Microspace server on port: " + port, e);
            }
        }
    }

    private CompletionStage<Void> close(Iterable<? extends Channel> channels) {
        final List<Channel> channelsCopy = ImmutableList.copyOf(channels);
        if (channelsCopy.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        final AtomicInteger numChannelsToClose = new AtomicInteger(channelsCopy.size());
        final CompletableFuture<Void> future = new CompletableFuture<>();
        final ChannelFutureListener listener = unused -> {
            if (numChannelsToClose.decrementAndGet() == 0) {
                future.complete(null);
            }
        };
        for (Channel ch : channelsCopy) {
            ch.close().addListener(listener);
        }
        return future;
    }

    public CompletableFuture<Void> stop() {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        // Create a single-use thread dedicated for monitoring graceful shutdown status.
        final ScheduledExecutorService gracefulShutdownExecutor =
                Executors.newSingleThreadScheduledExecutor(
                        r -> new Thread(r, "microspace-shutdown-0x" + Integer.toHexString(hashCode())));

        // Check every 100 ms for the server to have completed processing requests.
        final ScheduledFuture<?> quietPeriodFuture = gracefulShutdownExecutor.scheduleAtFixedRate(() -> {
            stop(future, config.gracefulShutdownQuietPeriod().toMillis(),
                 config.gracefulShutdownTimeout().toMillis(), gracefulShutdownExecutor);
        }, 0, 100, TimeUnit.MILLISECONDS);

        // Make sure the event loop stops after the timeout, regardless of what
        // the GracefulShutdownSupport says.
        try {
            gracefulShutdownExecutor.schedule(() -> {
                quietPeriodFuture.cancel(false);
                stop(future, config.gracefulShutdownQuietPeriod().toMillis(),
                     config.gracefulShutdownTimeout().toMillis(), gracefulShutdownExecutor);
            }, config.gracefulShutdownTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            // Can be rejected if quiet period is complete already.
        }

        return future;
    }

    public CompletableFuture<Void> stop(CompletableFuture<Void> future,
                                        long stopQuietPeriod,
                                        long stopTimeout,
                                        ExecutorService gracefulShutdownExecutor) {
        // Graceful shutdown is over. Terminate the temporary executor we created at stop0(future).
        if (gracefulShutdownExecutor != null) {
            gracefulShutdownExecutor.shutdownNow();
        }

        final Stopwatch stopwatch = Stopwatch.createStarted();
        if (isRunning() && workerGroup != null) {
            if (isRunning.compareAndSet(true, false)) {
                final Set<Channel> serverChannels = ImmutableSet.copyOf(Server.this.serverChannels);
                close(serverChannels).handleAsync((unused, throwable) -> {
                    // All server ports have been closed.
                    synchronized (activePorts) {
                        activePorts.clear();
                    }
                    close(connectionLimitHandler.childChannels()).handleAsync((unused3, unused4) -> {
                        final Future<?> workerShutdownFuture;
                        if (config.shutdownWorkerGroupOnStop()) {
                            workerShutdownFuture = workerGroup.shutdownGracefully(
                                    stopQuietPeriod, stopTimeout, TimeUnit.MILLISECONDS).addListener(
                                    this::logShutdownErrorIfNecessary);
                        } else {
                            workerShutdownFuture = ImmediateEventExecutor.INSTANCE.newSucceededFuture(null);
                        }
                        workerShutdownFuture.addListener(unused5 -> {
                            final Set<EventLoopGroup> bossGroups =
                                    Server.this.serverChannels
                                            .stream()
                                            .map(ch -> ch.eventLoop().parent())
                                            .collect(toImmutableSet());
                            if (bossGroups.isEmpty()) {
                                future.complete(null);
                                return;
                            }
                            final AtomicInteger remainingBossGroups = new AtomicInteger(bossGroups.size());
                            bossGroups.forEach(bossGroup -> {
                                bossGroup.shutdownGracefully(stopQuietPeriod, stopTimeout,
                                                             TimeUnit.MILLISECONDS)
                                         .addListener(this::logShutdownErrorIfNecessary);
                                bossGroup.terminationFuture().addListener(unused6 -> {
                                    if (remainingBossGroups.decrementAndGet() != 0) {
                                        // There are more boss groups to terminate.
                                        return;
                                    }
                                    future.complete(null);
                                });
                            });
                        });
                        return null;
                    });
                    return null;
                });
            }
        }
        stopwatch.stop();
        logger.info("Serving stop time {}{}", stopwatch.elapsed().toMillis(), "ms");
        return future;
    }

    private void logShutdownErrorIfNecessary(Future<?> future) {
        if (!future.isSuccess()) {
            if (logger.isWarnEnabled()) {
                Throwable e = future.cause();
                logger.warn("Error stopping Microspace server: " + e.getMessage(), e);
            }
        }
    }

    private boolean isBindException(Throwable e) {
        return e.getClass().getName().equals(BindException.class.getName());
    }

    @SuppressWarnings("rawtypes")
    private void processOptions(Map<ChannelOption<?>, Object> options,
                                BiConsumer<ChannelOption, Object> biConsumer) {
        options.forEach(biConsumer);
    }

    private EventLoopGroup createWorkerEventLoopGroup() {
        return createEventLoopGroup("worker" + config.serverThreadName());
    }

    private EventLoopGroup createParentEventLoopGroup() {
        return createEventLoopGroup("parent" + config.serverThreadName());
    }

    private ServerThreadNamer withThreadName(String prefix) {
        return ServerThreadNamer.withPrefix(eventLoopGroupName(config.ports().get(0), prefix));
    }

    private Class<? extends ServerChannel> transportChannel() {
        return TransportType.detectTransportType().serverChannelType();
    }

    private String eventLoopGroupName(ServerPort port, String prefix) {
        final InetSocketAddress localAddress = port.localAddress();
        final String localHostName =
                localAddress.getAddress().isAnyLocalAddress() ? "*" : localAddress.getHostString();

        // e.g. 'microspace-boss-http-*:8080'
        //      'microspace-boss-http-127.0.0.1:8443'
        //      'microspace-boss-proxy+http+https-127.0.0.1:8443'
        final String protocolNames = port.protocols().stream()
                                         .map(SessionProtocol::uriText)
                                         .collect(Collectors.joining("+"));
        return "microspace-" + prefix + "-" + protocolNames + '-' + localHostName + ':'
               + localAddress.getPort();
    }

    private EventLoopGroup createEventLoopGroup(String threadName) {
        return TransportType.detectTransportType()
                            .newEventLoopGroup(config.ioThreadCount(),
                                               transportType -> withThreadName(threadName));
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    public ServerConfig serverConfig() {
        return config;
    }

    public Set<ServerChannel> serverChannels() {
        return serverChannels;
    }

    /**
     * Returns all {@link ServerPort}s that this {@link Server} is listening to.
     *
     * @return a {@link Map} whose key is the bind address and value is {@link ServerPort}.
     *         an empty {@link Map} if this {@link Server} did not start.
     *
     * @see Server#activeLocalPort()
     */
    public Map<InetSocketAddress, ServerPort> activePorts() {
        synchronized (activePorts) {
            return Collections.unmodifiableMap(new LinkedHashMap<>(activePorts));
        }
    }

    /**
     * Returns the local {@link ServerPort} that this {@link Server} is listening to.
     *
     * @throws IllegalStateException if there is no active local port available or the server is not
     *                               started yet
     */
    public int activeLocalPort() {
        return activeLocalPort(null);
    }

    public int activeLocalPort(SessionProtocol protocol) {
        synchronized (activePorts) {
            return activePorts.values().stream()
                              .filter(activePort -> (protocol == null || activePort.hasProtocol(protocol)) &&
                                                    isLocalPort(activePort))
                              .findFirst()
                              .orElseThrow(() -> new IllegalStateException(
                                      (protocol == null ? "no active local ports: "
                                                        : ("no active local ports for " + protocol + ": ")) +
                                      activePorts.values()))
                              .localAddress()
                              .getPort();
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

                if (config.bootCls() != null) {
                    String applicationName = config.bootCls().getName();
                    if (isLocalPort(actualPort)) {
                        port.protocols().forEach(p -> logger.info(
                                "Binding {} Serving {} at {} - {}://127.0.0.1:{}/", applicationName,
                                p.name(), localAddress, p.uriText(), localAddress.getPort()));
                    }
                } else {
                    if (logger.isInfoEnabled()) {
                        logger.info("Serving {} at {}", Joiner.on('+').join(port.protocols()), localAddress);
                    }
                }
            }
        }
    }
}
