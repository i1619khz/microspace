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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.microspace.internal.Flags.defaultMaxRequestLength;
import static io.microspace.internal.Flags.defaultRequestTimeoutMillis;
import static io.microspace.server.ServerConfig.validateGreaterThanOrEqual;
import static io.microspace.server.ServerConfig.validateNonNegative;
import static io.microspace.server.SessionProtocol.HTTP;
import static io.microspace.server.SessionProtocol.HTTPS;
import static io.microspace.server.SessionProtocol.PROXY;
import static java.util.Objects.requireNonNull;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.microspace.context.banner.BannerPrinter;
import io.microspace.context.banner.DefaultBannerPrinter;
import io.microspace.internal.Flags;
import io.microspace.internal.UncheckedFnKit;
import io.microspace.server.annotation.ExceptionHandlerFunction;
import io.microspace.server.annotation.RequestConverterFunction;
import io.microspace.server.annotation.ResponseConverterFunction;
import io.netty.channel.ChannelOption;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * @author i1619kHz
 */
public final class ServerBuilder {
    private static final long MIN_PING_INTERVAL_MILLIS = 1000L;
    private static final long MIN_MAX_CONNECTION_AGE_MILLIS = 1_000L;

    private final Map<Class<? extends Throwable>,
            ExceptionHandlerFunction> exceptionServices = new ConcurrentHashMap<>();
    private final List<ServiceConfigSetters> serviceConfigSetters = new ArrayList<>();
    private final Map<ChannelOption<?>, Object> channelOptions = new HashMap<>();
    private final Map<ChannelOption<?>, Object> childChannelOptions = new HashMap<>();
    private MeterRegistry meterRegistry = new CompositeMeterRegistry();
    private int maxNumConnections = Flags.defaultMaxNumConnections();
    private long idleTimeoutMillis = Flags.defaultIdleTimeoutMillis();
    private long pingIntervalMillis = Flags.defaultPingIntervalMillis();
    private long maxConnectionAgeMillis = Flags.defaultMaxConnectionAgeMillis();
    private int http1MaxInitialLineLength = Flags.defaultHttp1MaxInitialLineLength();
    private int http1MaxHeaderSize = Flags.defaultHttp1MaxHeaderSize();
    private int http1MaxChunkSize = Flags.defaultHttp1MaxChunkSize();
    private int http2InitialConnectionWindowSize = Flags.defaultHttp2InitialConnectionWindowSize();
    private int http2InitialStreamWindowSize = Flags.defaultHttp2InitialStreamWindowSize();
    private long http2MaxHeaderListSize = Flags.defaultHttp2MaxHeaderListSize();
    private long http2MaxStreamsPerConnection = Flags.defaultHttp2MaxStreamsPerConnection();
    private int http2MaxFrameSize = Flags.defaultHttp2MaxFrameSize();
    private Duration gracefulShutdownQuietPeriod = Duration.ofMillis(Flags.defaultShutdownQuietPeriod());
    private Duration gracefulShutdownTimeout = Duration.ofMillis(Flags.defaultShutdownTimeout());
    private int acceptThreadCount = Flags.defaultAcceptThreadCount();
    private int ioThreadCount = Flags.defaultIoThreadCount();
    private int serverRestartCount = Flags.defaultServerRestartCount();
    private boolean shutdownWorkerGroupOnStop = Flags.defaultShutdownWorkerGroupOnStop();
    private ExecutorService startStopExecutor = GlobalEventExecutor.INSTANCE;
    private BannerPrinter bannerPrinter = new DefaultBannerPrinter();
    private List<ServerPort> ports = new ArrayList<>();
    private boolean useSsl = Flags.useSsl();
    private boolean useEpoll = Flags.useEpoll();
    private boolean useSession = Flags.useSession();
    private boolean useIoUsing = Flags.useIoUsing();
    private String bannerText = Flags.defaultBannerText();
    private String bannerFont = Flags.defaultBannerFont();
    private String sessionKey = Flags.defaultSessionKey();
    private String viewSuffix = Flags.defaultViewSuffix();
    private String templateFolder = Flags.defaultTemplateFolder();
    private String serverThreadName = Flags.defaultServerThreadName();
    private String profiles = Flags.profiles();
    @Nullable
    private Long requestTimeoutMillis;
    @Nullable
    private Long maxRequestLength;
    @Nullable
    private Boolean verboseResponses;

    ServerBuilder() {/* nothing*/}

    public ServerBuilder port(int port) {
        return port(new ServerPort(port, HTTP));
    }

    public ServerBuilder port(int port, SessionProtocol... protocols) {
        return port(new ServerPort(port, protocols));
    }

    public ServerBuilder port(int port, Iterable<SessionProtocol> protocols) {
        return port(new ServerPort(port, protocols));
    }

    public ServerBuilder port(InetSocketAddress localAddress, SessionProtocol... protocols) {
        return port(new ServerPort(localAddress, protocols));
    }

    public ServerBuilder port(InetSocketAddress localAddress, Iterable<SessionProtocol> protocols) {
        return port(new ServerPort(localAddress, protocols));
    }

    public ServerBuilder port(InetSocketAddress localAddress) {
        checkArgument(null != localAddress, "localAddress");
        return this.port(new ServerPort(requireNonNull(localAddress)));
    }

    public ServerBuilder port(ServerPort... serverPorts) {
        ImmutableSet.copyOf(serverPorts).forEach(this::port);
        return this;
    }

    public ServerBuilder port(Iterable<ServerPort> serverPorts) {
        ImmutableSet.copyOf(serverPorts).forEach(this::port);
        return this;
    }

    public ServerBuilder port(Integer... serverPorts) {
        ImmutableSet.copyOf(serverPorts).forEach((port -> this.port(new ServerPort(port, HTTP))));
        return this;
    }

    public ServerBuilder port(ServerPort serverPort) {
        requireNonNull(serverPort, "serverPort");
        checkArgument(Flags.checkMinPort(serverPort.port()),
                      "The minimum server currentMinPort number for IPv4. " +
                      "Set at 1100 to avoid returning privileged currentMinPort numbers.");
        ports.add(requireNonNull(serverPort, "serverPot"));
        return this;
    }

    public ServerBuilder http() {
        return http(Flags.defaultPort());
    }

    public ServerBuilder http(int serverPort) {
        checkArgument(Flags.checkPort(serverPort), "port number must be available");
        return port(new ServerPort(serverPort, HTTP));
    }

    public ServerBuilder https() {
        return https(Flags.defaultPort());
    }

    public ServerBuilder https(int serverPort) {
        checkArgument(Flags.checkPort(serverPort), "port number must be available");
        return port(new ServerPort(serverPort, HTTPS));
    }

    /**
     * Set the print banner text
     */
    public ServerBuilder bannerText(String bannerText) {
        checkState(Strings.isNullOrEmpty(this.bannerText),
                   "bannerText was already set to %s", this.bannerText);
        this.bannerText = requireNonNull(bannerText);
        return this;
    }

    /**
     * Set the print banner font
     */
    public ServerBuilder bannerFont(String bannerFont) {
        checkState(Strings.isNullOrEmpty(this.bannerFont),
                   "bannerFont was already set to %s", this.bannerFont);
        this.bannerFont = requireNonNull(bannerFont);
        return this;
    }

    /**
     * Set server session key
     *
     * @param sessionKey session key
     * @return this
     */
    public ServerBuilder sessionKey(String sessionKey) {
        checkState(Strings.isNullOrEmpty(this.sessionKey),
                   "sessionKey was already set to %s", this.sessionKey);
        this.sessionKey = requireNonNull(sessionKey);
        return this;
    }

    /**
     * Set template folder
     *
     * @param templateFolder template folder
     */
    public ServerBuilder templateFolder(String templateFolder) {
        checkState(Strings.isNullOrEmpty(this.templateFolder),
                   "templateFolder was already set to %s", this.templateFolder);
        this.templateFolder = requireNonNull(templateFolder);
        return this;
    }

    /**
     * Set session open state, the default is open
     *
     * @param enable Whether to open the session
     * @return this
     */
    public ServerBuilder useSession(boolean enable) {
        this.useSession = enable;
        return this;
    }

    /**
     * Set io_using open state, the default is close
     *
     * @param enable Whether to open the io_using
     * @return this
     */
    public ServerBuilder useIoUsing(boolean enable) {
        this.useIoUsing = enable;
        return this;
    }

    /**
     * Set epoll open state, the default is close
     *
     * @param enable Whether to open the session
     * @return this
     */
    public ServerBuilder useEpoll(boolean enable) {
        this.useEpoll = enable;
        return this;
    }

    /**
     * Set ssl open state, the default is close
     *
     * @param enable Whether to open the session
     * @return this
     */
    public ServerBuilder useSsl(boolean enable) {
        this.useSsl = enable;
        return this;
    }

    /**
     * Sets the {@link MeterRegistry} that collects various stats.
     */
    public ServerBuilder meterRegistry(MeterRegistry meterRegistry) {
        this.meterRegistry = requireNonNull(meterRegistry, "meterRegistry");
        return this;
    }

    /**
     * Sets the timeout of a request.
     *
     * @param requestTimeout the timeout. {@code 0} disables the timeout.
     */
    public ServerBuilder requestTimeout(Duration requestTimeout) {
        return requestTimeoutMillis(requireNonNull(requestTimeout, "requestTimeout").toMillis());
    }

    /**
     * Sets the timeout of a request in milliseconds.
     *
     * @param requestTimeoutMillis the timeout in milliseconds. {@code 0} disables the timeout.
     */
    public ServerBuilder requestTimeoutMillis(long requestTimeoutMillis) {
        this.requestTimeoutMillis = requestTimeoutMillis;
        return this;
    }

    /**
     * Sets the maximum allowed length of the content decoded at the session layer.
     * e.g. the content length of an HTTP request.
     *
     * @param maxRequestLength the maximum allowed length. {@code 0} disables the length limit.
     */
    public ServerBuilder maxRequestLength(long maxRequestLength) {
        this.maxRequestLength = maxRequestLength;
        return this;
    }

    /**
     * Sets whether the verbose response mode is enabled. When enabled, the server responses will contain
     * the exception type and its full stack trace, which may be useful for debugging while potentially
     * insecure. When disabled, the server responses will not expose such server-side details to the client.
     * The default value of this property is retrieved from {@link Flags#verboseResponses()}.
     */
    public ServerBuilder verboseResponses(boolean verboseResponses) {
        this.verboseResponses = verboseResponses;
        return this;
    }

    /**
     * Sets the {@link ChannelOption} of the server socket bound by {@link Server}.
     * Note that the previously added option will be overridden if the same option is set again.
     *
     * <pre>{@code
     * ServerBuilder sb = Server.builder();
     * sb.channelOption(ChannelOption.BACKLOG, 1024);
     * }</pre>
     */
    public <T> ServerBuilder channelOption(ChannelOption<T> option, T value) {
        requireNonNull(option, "option");
        option.validate(value);
        channelOptions.put(option, value);
        return this;
    }

    /**
     * Sets the {@link ChannelOption} of sockets accepted by {@link Server}.
     * Note that the previously added option will be overridden if the same option is set again.
     *
     * <pre>{@code
     * ServerBuilder sb = Server.builder();
     * sb.childChannelOption(ChannelOption.SO_REUSEADDR, true)
     *   .childChannelOption(ChannelOption.SO_KEEPALIVE, true);
     * }</pre>
     */
    public <T> ServerBuilder childChannelOption(ChannelOption<T> option, T value) {
        requireNonNull(option, "option");
        option.validate(value);
        childChannelOptions.put(option, value);
        return this;
    }

    /**
     * Register get route
     *
     * @param pathPrefix Route path
     * @param service    HttpService
     * @return this
     */
    public ServerBuilder get(String pathPrefix, HttpService service) {
        return route().get(pathPrefix).build(service);
    }

    /**
     * Register post route
     *
     * @param pathPrefix Route path
     * @param service    HttpService
     * @return this
     */
    public ServerBuilder post(String pathPrefix, HttpService service) {
        return route().post(pathPrefix).build(service);
    }

    /**
     * Register head route
     *
     * @param pathPrefix Route path
     * @param service    HttpService
     * @return this
     */
    public ServerBuilder head(String pathPrefix, HttpService service) {
        return route().head(pathPrefix).build(service);
    }

    /**
     * Register put route
     *
     * @param pathPrefix Route path
     * @param service    HttpService
     * @return this
     */
    public ServerBuilder put(String pathPrefix, HttpService service) {
        return route().put(pathPrefix).build(service);
    }

    /**
     * Register patch route
     *
     * @param pathPrefix Route path
     * @param service    HttpService
     * @return this
     */
    public ServerBuilder patch(String pathPrefix, HttpService service) {
        return route().patch(pathPrefix).build(service);
    }

    /**
     * Register delete route
     *
     * @param pathPrefix Route path
     * @param service    HttpService
     * @return this
     */
    public ServerBuilder delete(String pathPrefix, HttpService service) {
        return route().delete(pathPrefix).build(service);
    }

    /**
     * Register options route
     *
     * @param prefix  Route path
     * @param service HttpService
     * @return this
     */
    public ServerBuilder options(String pathPrefix, HttpService service) {
        return route().options(pathPrefix).build(service);
    }

    /**
     * Register trace route
     *
     * @param pathPrefix Route path
     * @param service    HttpService
     * @return this
     */
    public ServerBuilder trace(String pathPrefix, HttpService service) {
        return route().trace(pathPrefix).build(service);
    }

    public ServerBuilder service(String pathPattern, HttpService service) {
        return route().pathPrefix(pathPattern).methods(HttpMethod.knownMethods()).build(service);
    }

    public ServerBuilder service(String pathPattern, HttpService service, HttpMethod... httpMethods) {
        return route().pathPrefix(pathPattern).methods(httpMethods).build(service);
    }

    public ServerBuilder service(String pathPattern, HttpService service, Iterable<HttpMethod> httpMethods) {
        return route().pathPrefix(pathPattern).methods(httpMethods).build(service);
    }

    public ServerBuilder service(Route route, HttpService service) {
        return route().route(route).build(service);
    }

    public ServerBuilder annotatedService(Object service) {
        checkArgument(null != service, "service");
        return annotatedService("/", service);
    }

    public ServerBuilder annotatedService(String pathPrefix, Object service) {
        return annotatedService(pathPrefix, service, ImmutableList.of());
    }

    public ServerBuilder annotatedService(String pathPrefix, Object service,
                                          Object... exceptionHandlersAndConverters) {
        return annotatedService(pathPrefix, service, ImmutableList.copyOf(
                requireNonNull(exceptionHandlersAndConverters, "exceptionHandlersAndConverters")));
    }

    public ServerBuilder annotatedService(Object service,
                                          Object... exceptionHandlersAndConverters) {
        return annotatedService(service, Function.identity(), exceptionHandlersAndConverters);
    }

    public ServerBuilder annotatedService(String pathPrefix, Object service,
                                          Iterable<?> exceptionHandlersAndConverters) {
        return annotatedService(pathPrefix, service, Function.identity(), ImmutableList.copyOf(
                requireNonNull(exceptionHandlersAndConverters, "exceptionHandlersAndConverters")));
    }

    public ServerBuilder annotatedService(Object service,
                                          Function<? super HttpService, ? extends HttpService> decorator,
                                          Iterable<?> exceptionHandlersAndConverters) {
        return annotatedService("/", service, decorator, exceptionHandlersAndConverters);
    }

    public ServerBuilder annotatedService(Object service,
                                          Function<? super HttpService, ? extends HttpService> decorator,
                                          Object... exceptionHandlersAndConverters) {
        return annotatedService("/", service, decorator, exceptionHandlersAndConverters);
    }

    public ServerBuilder annotatedService(String pathPrefix, Object service,
                                          Function<? super HttpService, ? extends HttpService> decorator,
                                          Object... exceptionHandlersAndConverters) {
        return annotatedService(pathPrefix, service, decorator, ImmutableList.copyOf(
                requireNonNull(exceptionHandlersAndConverters, "exceptionHandlersAndConverters")));
    }

    public ServerBuilder annotatedService(String pathPrefix, Object service,
                                          Function<? super HttpService, ? extends HttpService> decorator,
                                          Iterable<?> exceptionHandlersAndConverters) {
        requireNonNull(pathPrefix, "pathPrefix");
        requireNonNull(service, "service");
        requireNonNull(exceptionHandlersAndConverters, "exceptionHandlersAndConverters");
        final Builder<RequestConverterFunction> requestConverters = ImmutableList.builder();
        final Builder<ResponseConverterFunction> responseConverters = ImmutableList.builder();
        final Builder<ExceptionHandlerFunction> exceptionHandlers = ImmutableList.builder();
        for (final Object object : exceptionHandlersAndConverters) {
            if (null == object) {
                continue;
            }
            switch (object) {
                case RequestConverterFunction reqConverter -> requestConverters.add(reqConverter);
                case ResponseConverterFunction respConverter -> responseConverters.add(respConverter);
                case ExceptionHandlerFunction excHandler -> exceptionHandlers.add(excHandler);
                default -> throw new IllegalArgumentException(
                        object.getClass().getName() + " is neither an exception handler nor a converter.");
            }
        }
        return annotatedService(pathPrefix, service, decorator, exceptionHandlers.build(),
                                requestConverters.build(), responseConverters.build());
    }

    public ServerBuilder annotatedService(
            String pathPrefix, Object service,
            Function<? super HttpService, ? extends HttpService> decorator,
            Iterable<? extends ExceptionHandlerFunction> exceptionHandlerFunctions,
            Iterable<? extends RequestConverterFunction> requestConverters,
            Iterable<? extends ResponseConverterFunction> responseConverters) {
        requireNonNull(pathPrefix, "pathPrefix");
        requireNonNull(service, "service");
        requireNonNull(decorator, "decorator");
        requireNonNull(exceptionHandlerFunctions, "exceptionHandlerFunctions");
        requireNonNull(requestConverters, "requestConverters");
        requireNonNull(responseConverters, "responseConverters");
        return annotatedService().pathPrefix(pathPrefix)
                                 .decorator(decorator)
                                 .exceptionHandlers(exceptionHandlerFunctions)
                                 .requestConverters(requestConverters)
                                 .responseConverters(responseConverters)
                                 .build(service);
    }

    ServerBuilder serviceConfigBuilder(ServiceConfigSetters ServiceConfigSetters) {
        serviceConfigSetters.add(ServiceConfigSetters);
        return this;
    }

    private ServiceBindingBuilder route() {
        return new ServiceBindingBuilder(this);
    }

    private AnnotatedServiceBindingBuilder annotatedService() {
        return new AnnotatedServiceBindingBuilder(this);
    }

    private List<ServiceConfigSetters> serviceConfigSetters() {
        return serviceConfigSetters;
    }

    public ServerBuilder profiles(String profiles) {
        checkArgument(Strings.isNullOrEmpty(this.profiles),
                      "bootConfName was already set to %s", this.profiles);
        this.profiles = requireNonNull(profiles);
        return this;
    }

    public ServerBuilder serverThreadName(String serverThreadName) {
        checkArgument(Strings.isNullOrEmpty(this.serverThreadName),
                      "bootConfName was already set to %s", this.serverThreadName);
        this.serverThreadName = requireNonNull(serverThreadName);
        return this;
    }

    public ServerBuilder viewSuffix(String viewSuffix) {
        checkArgument(Strings.isNullOrEmpty(this.viewSuffix),
                      "viewSuffix was already set to %s", this.viewSuffix);
        this.viewSuffix = requireNonNull(viewSuffix);
        return this;
    }

    /**
     * Sets the amount of time to wait after calling {@link Server#stop()} for
     * requests to go away before actually shutting down.
     *
     * @param quietPeriodMillis the number of milliseconds to wait for active
     *                          requests to go end before shutting down. 0 means the server will
     *                          stop right away without waiting.
     * @param timeoutMillis the number of milliseconds to wait before shutting down the server regardless of
     *                      active requests. This should be set to a time greater than {@code quietPeriodMillis}
     *                      to ensure the server shuts down even if there is a stuck request.
     */
    public ServerBuilder gracefulShutdownTimeoutMillis(long quietPeriodMillis, long timeoutMillis) {
        return gracefulShutdownTimeout(Duration.ofMillis(quietPeriodMillis), Duration.ofMillis(timeoutMillis));
    }

    /**
     * Sets the amount of time to wait after calling {@link Server#stop()} for
     * requests to go away before actually shutting down.
     *
     * @param quietPeriod the number of milliseconds to wait for active
     *                    requests to go end before shutting down. {@link Duration#ZERO} means
     *                    the server will stop right away without waiting.
     * @param timeout the amount of time to wait before shutting down the server regardless of active requests.
     *                This should be set to a time greater than {@code quietPeriod} to ensure the server
     *                shuts down even if there is a stuck request.
     */
    public ServerBuilder gracefulShutdownTimeout(Duration quietPeriod, Duration timeout) {
        requireNonNull(quietPeriod, "quietPeriod");
        requireNonNull(timeout, "timeout");
        gracefulShutdownQuietPeriod = validateNonNegative(quietPeriod, "quietPeriod");
        gracefulShutdownTimeout = validateNonNegative(timeout, "timeout");
        validateGreaterThanOrEqual(gracefulShutdownTimeout, "quietPeriod",
                                   gracefulShutdownQuietPeriod, "timeout");
        return this;
    }

    public ServerBuilder startStopExecutor(ExecutorService startStopExecutor) {
        this.startStopExecutor = requireNonNull(startStopExecutor, "startStopExecutor");
        return this;
    }

    public ServerBuilder maxNumConnections(int maxNumConnections) {
        checkArgument(maxNumConnections > 0, "maxNumConnections must > 0");
        this.maxNumConnections = maxNumConnections;
        return this;
    }

    public ServerBuilder http2InitialConnectionWindowSize(int http2InitialConnectionWindowSize) {
        checkArgument(http2InitialConnectionWindowSize > 0, "http2InitialConnectionWindowSize must > 0");
        this.http2InitialConnectionWindowSize = http2InitialConnectionWindowSize;
        return this;
    }

    public ServerBuilder http2InitialStreamWindowSize(int http2InitialStreamWindowSize) {
        checkArgument(http2InitialStreamWindowSize > 0, "http2InitialStreamWindowSize must > 0");
        this.http2InitialStreamWindowSize = http2InitialStreamWindowSize;
        return this;
    }

    public ServerBuilder http2MaxFrameSize(int http2MaxFrameSize) {
        checkArgument(http2MaxFrameSize > 0, "http2MaxFrameSize must > 0");
        this.http2MaxFrameSize = http2MaxFrameSize;
        return this;
    }

    public ServerBuilder http1MaxInitialLineLength(int http1MaxInitialLineLength) {
        checkArgument(http1MaxInitialLineLength > 0, "http1MaxInitialLineLength must > 0");
        this.http1MaxInitialLineLength = http1MaxInitialLineLength;
        return this;
    }

    public ServerBuilder http1MaxHeaderSize(int http1MaxHeaderSize) {
        checkArgument(http1MaxHeaderSize > 0, "http1MaxHeaderSize must > 0");
        this.http1MaxHeaderSize = http1MaxHeaderSize;
        return this;
    }

    public ServerBuilder http1MaxChunkSize(int http1MaxChunkSize) {
        checkArgument(http1MaxChunkSize > 0, "http1MaxChunkSize must > 0");
        this.http1MaxChunkSize = http1MaxChunkSize;
        return this;
    }

    public ServerBuilder idleTimeoutMillis(long idleTimeoutMillis) {
        checkArgument(idleTimeoutMillis > 0, "idleTimeoutMillis must > 0");
        this.idleTimeoutMillis = idleTimeoutMillis;
        return this;
    }

    public ServerBuilder pingIntervalMillis(long pingIntervalMillis) {
        checkArgument(pingIntervalMillis > 0, "pingIntervalMillis must > 0");
        this.pingIntervalMillis = pingIntervalMillis;
        return this;
    }

    public ServerBuilder maxConnectionAgeMillis(long maxConnectionAgeMillis) {
        checkArgument(maxConnectionAgeMillis > 0, "maxConnectionAgeMillis must > 0");
        this.maxConnectionAgeMillis = maxConnectionAgeMillis;
        return this;
    }

    public ServerBuilder http2MaxHeaderListSize(long http2MaxHeaderListSize) {
        checkArgument(http2MaxHeaderListSize > 0, "http2MaxHeaderListSize must > 0");
        this.http2MaxHeaderListSize = http2MaxHeaderListSize;
        return this;
    }

    public ServerBuilder http2MaxStreamsPerConnection(long http2MaxStreamsPerConnection) {
        checkArgument(http2MaxStreamsPerConnection > 0, "http2MaxStreamsPerConnection must > 0");
        this.http2MaxStreamsPerConnection = http2MaxStreamsPerConnection;
        return this;
    }

    public ServerBuilder acceptThreadCount(int acceptThreadCount) {
        checkArgument(acceptThreadCount > 0, "acceptThreadCount must > 0");
        this.acceptThreadCount = acceptThreadCount;
        return this;
    }

    public ServerBuilder ioThreadCount(int ioThreadCount) {
        checkArgument(ioThreadCount > 0, "ioThreadCount must > 0");
        this.ioThreadCount = ioThreadCount;
        return this;
    }

    public ServerBuilder serverRestartCount(int serverRestartCount) {
        checkArgument(serverRestartCount > 0, "serverRestartCount must > 0");
        this.serverRestartCount = serverRestartCount;
        return this;
    }

    public ServerBuilder shutdownWorkerGroupOnStop(boolean shutdownWorkerGroupOnStop) {
        this.shutdownWorkerGroupOnStop = shutdownWorkerGroupOnStop;
        return this;
    }

    public ServerBuilder bannerPrinter(BannerPrinter bannerPrinter) {
        this.bannerPrinter = requireNonNull(bannerPrinter, "bannerPrinter");
        return this;
    }

    public Server build() {
        return build(new String[0]);
    }

    public Server build(String[] args) {
        final Thread currentThread = Thread.currentThread();
        final Optional<? extends Class<?>> classOptional = Arrays
                .stream(currentThread.getStackTrace())
                .filter(st -> "main".equals(st.getMethodName()))
                .findFirst()
                .map(StackTraceElement::getClassName)
                .map(UncheckedFnKit.wrap(Class::forName));
        return build(classOptional.orElse(null), args);
    }

    /**
     * Returns a list of {@link ServerPort}s which consists of distinct port numbers except for the port
     * {@code 0}. If there are the same port numbers with different {@link SessionProtocol}s,
     * their {@link SessionProtocol}s will be merged into a single {@link ServerPort} instance.
     * The returned list is sorted as the same order of the specified {@code ports}.
     */
    private List<ServerPort> resolveDistinctPorts(List<ServerPort> ports) {
        final List<ServerPort> distinctPorts = new ArrayList<>();
        for (final ServerPort p : ports) {
            boolean found = false;
            // Do not check the port number 0 because a user may want his or her server to be bound
            // on multiple arbitrary ports.
            if (p.localAddress().getPort() > 0) {
                for (int i = 0; i < distinctPorts.size(); i++) {
                    final ServerPort port = distinctPorts.get(i);
                    if (port.localAddress().equals(p.localAddress())) {
                        final ServerPort merged =
                                new ServerPort(port.localAddress(),
                                               Sets.union(port.protocols(), p.protocols()));
                        distinctPorts.set(i, merged);
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                distinctPorts.add(p);
            }
        }
        return Collections.unmodifiableList(distinctPorts);
    }

    /**
     * Build {@link Server}
     *
     * @return Http Server
     */
    public Server build(Class<?> bootCls, String[] args) {
        this.ports.forEach(
                port -> checkState(port.protocols().stream().anyMatch(p -> p != PROXY),
                                   "protocols: %s (expected: at least one %s or %s)",
                                   port.protocols(), HTTP, HTTPS));

        // Retrieve all settings as a local copy. Use default builder's properties if not set.
        final long requestTimeoutMillis =
                this.requestTimeoutMillis != null ? this.requestTimeoutMillis : defaultRequestTimeoutMillis();
        final long maxRequestLength =
                this.maxRequestLength != null ? this.maxRequestLength : defaultMaxRequestLength();
        final boolean verboseResponses =
                this.verboseResponses != null ? this.verboseResponses : false;

        final List<ServiceConfig> serviceConfigs = serviceConfigSetters()
                .stream()
                .flatMap(cfgSetters -> {
                    if (cfgSetters instanceof AnnotatedServiceBindingBuilder) {
                        return ((AnnotatedServiceBindingBuilder) cfgSetters)
                                .buildServiceConfigBuilder().stream();
                    } else if (cfgSetters instanceof ServiceConfigBuilder) {
                        return Stream.of((ServiceConfigBuilder) cfgSetters);
                    } else {
                        throw new Error("Unexpected service config setters type: " +
                                        cfgSetters.getClass().getSimpleName());
                    }
                }).map(cfgBuilder -> cfgBuilder.build(
                        requestTimeoutMillis, maxRequestLength, verboseResponses))
                .collect(toImmutableList());

        if (!ports.isEmpty()) {
            ports = resolveDistinctPorts(ports);
        } else {
            ports = ImmutableList.of(Flags.defaultServerPort());
        }

        if (pingIntervalMillis > 0) {
            pingIntervalMillis = Math.max(pingIntervalMillis, MIN_PING_INTERVAL_MILLIS);
            if (idleTimeoutMillis > 0 && pingIntervalMillis >= idleTimeoutMillis) {
                pingIntervalMillis = 0;
            }
        }

        if (maxConnectionAgeMillis > 0) {
            maxConnectionAgeMillis = Math.max(maxConnectionAgeMillis, MIN_MAX_CONNECTION_AGE_MILLIS);
            if (idleTimeoutMillis == 0 || idleTimeoutMillis > maxConnectionAgeMillis) {
                idleTimeoutMillis = maxConnectionAgeMillis;
            }
        }

        return new Server(new ServerConfig(serviceConfigs, exceptionServices, meterRegistry, bootCls,
                                           args, bannerPrinter, channelOptions, childChannelOptions, useSsl,
                                           useEpoll, shutdownWorkerGroupOnStop, startStopExecutor, bannerText,
                                           bannerFont, sessionKey, viewSuffix, templateFolder, serverThreadName,
                                           profiles, useSession, useIoUsing, ports, maxNumConnections,
                                           http2InitialConnectionWindowSize, http2InitialStreamWindowSize,
                                           http2MaxFrameSize, http1MaxInitialLineLength, http1MaxHeaderSize,
                                           http1MaxChunkSize, idleTimeoutMillis, pingIntervalMillis,
                                           maxConnectionAgeMillis, http2MaxHeaderListSize,
                                           http2MaxStreamsPerConnection, acceptThreadCount, ioThreadCount,
                                           serverRestartCount, gracefulShutdownQuietPeriod,
                                           gracefulShutdownTimeout), null);
    }
}
