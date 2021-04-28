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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.microspace.context.banner.Banner;
import io.microspace.context.banner.DefaultApplicationBanner;
import io.microspace.core.Flags;
import io.microspace.core.UncheckedFnKit;
import io.microspace.server.http.HttpMethod;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.microspace.server.SessionProtocol.HTTP;
import static io.microspace.server.SessionProtocol.HTTPS;
import static io.microspace.server.SessionProtocol.PROXY;
import static java.util.Objects.requireNonNull;

/**
 * @author i1619kHz
 */
public final class ServerBuilder {

    // Prohibit deprecated options
    @SuppressWarnings("deprecation")
    private static final Set<ChannelOption<?>> PROHIBITED_SOCKET_OPTIONS = ImmutableSet.of(
            ChannelOption.ALLOW_HALF_CLOSURE, ChannelOption.AUTO_READ,
            ChannelOption.AUTO_CLOSE, ChannelOption.MAX_MESSAGES_PER_READ,
            ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, ChannelOption.WRITE_BUFFER_LOW_WATER_MARK,
            EpollChannelOption.EPOLL_MODE);

    static final long MIN_PING_INTERVAL_MILLIS = 1000L;
    private static final long MIN_MAX_CONNECTION_AGE_MILLIS = 1_000L;

    private final List<Interceptor> interceptors = new ArrayList<>();
    private final List<Filter> filters = new ArrayList<>();
    private final List<View> views = new ArrayList<>();
    private final List<ViewResolver> viewResolvers = new ArrayList<>();
    private final List<ViewAdapter> viewAdapters = new ArrayList<>();
    private final List<TypeConverter> typeConverters = new ArrayList<>();
    private final List<HttpService> httpServices = new ArrayList<>();
    private final List<ArgumentBinder> argumentBinders = new ArrayList<>();
    private final List<HandlerMapping> handlerMappings = new ArrayList<>();
    private final List<HandlerAdapter> handlerAdapters = new ArrayList<>();
    private final List<HandlerInterceptor> handlerInterceptors = new ArrayList<>();
    private final List<HandlerExceptionResolver> handlerExceptionResolvers = new ArrayList<>();
    private final Map<String, WebSocketChannel> websSocketSessions = new HashMap<>();
    private final Map<String, Object> httpHandlerService = new HashMap<>();
    private final Map<ChannelOption<?>, Object> channelOptions = new HashMap<>();
    private final Map<ChannelOption<?>, Object> childChannelOptions = new HashMap<>();
    private HandlerExecutionChain handlerExecutionChain = new DefaultHandlerExecutionChain();
    private Executor startStopExecutor = GlobalEventExecutor.INSTANCE;
    private List<ServerPort> ports = new ArrayList<>();
    private Banner banner = new DefaultApplicationBanner();
    private String bannerText = Flags.bannerText();
    private String bannerFont = Flags.bannerFont();
    private String sessionKey = Flags.sessionKey();
    private String viewSuffix = Flags.viewSuffix();
    private String templateFolder = Flags.templateFolder();
    private String serverThreadName = Flags.serverThreadName();
    private String profiles = Flags.profiles();
    private PropertyEnvType propertyEnvType;

    private String sslCert;
    private String sslPrivateKey;
    private String sslPrivateKeyPass;

    private int maxNumConnections = Flags.maxNumConnections();
    private int http2InitialConnectionWindowSize = Flags.http2InitialConnectionWindowSize();
    private int http2InitialStreamWindowSize = Flags.http2InitialStreamWindowSize();
    private int http2MaxFrameSize = Flags.http2MaxFrameSize();
    private int http1MaxInitialLineLength = Flags.http1MaxInitialLineLength();
    private int http1MaxHeaderSize = Flags.http1MaxHeaderSize();
    private int http1MaxChunkSize = Flags.http1MaxChunkSize();
    private int acceptThreadCount = Flags.acceptThreadCount();
    private int ioThreadCount = Flags.ioThreadCount();
    private final int serverRestartCount = Flags.serverRestartCount();

    private long idleTimeoutMillis = Flags.idleTimeoutMillis();
    private long pingIntervalMillis = Flags.pingIntervalMillis();
    private long maxConnectionAgeMillis = Flags.maxConnectionAgeMillis();
    private long http2MaxHeaderListSize = Flags.http2MaxHeaderListSize();
    private long http2MaxStreamsPerConnection = Flags.http2MaxStreamsPerConnection();

    private boolean useSsl = Flags.useSsl();
    private boolean useEpoll = Flags.useEpoll();
    private boolean useSession = Flags.useSession();

    public ServerBuilder http(int serverPort) {
        checkArgument(serverPort > 0 && serverPort <= 65533, "Port number must be available");
        return this.port(new ServerPort(serverPort, HTTP));
    }

    public ServerBuilder https(int serverPort) {
        checkArgument(serverPort > 0 && serverPort <= 65533, "Port number must be available");
        return this.port(new ServerPort(serverPort, HTTPS));
    }

    public ServerBuilder port(InetSocketAddress localAddress) {
        checkNotNull(localAddress, "InetSocketAddress can't be null");
        return this.port(new ServerPort(requireNonNull(localAddress)));
    }

    public ServerBuilder banner(Banner banner) {
        checkNotNull(banner, "Banner can't be null");
        this.banner = requireNonNull(banner);
        return this;
    }

    /**
     * Set the listening port number.
     */
    public ServerBuilder port(ServerPort port) {
        checkNotNull(port, "ServerPort can't be null");
        this.ports.add(requireNonNull(port));
        return this;
    }

    /**
     * Set the print banner text
     */
    public ServerBuilder bannerText(String bannerText) {
        checkState(Strings.isNullOrEmpty(this.bannerText), "bannerText was already set to %s", this.bannerText);
        this.bannerText = requireNonNull(bannerText);
        return this;
    }

    /**
     * Set the print banner font
     */
    public ServerBuilder bannerFont(String bannerFont) {
        checkState(Strings.isNullOrEmpty(this.bannerFont), "bannerFont was already set to %s", this.bannerFont);
        this.bannerFont = requireNonNull(bannerFont);
        return this;
    }

    /**
     * Set the print banner name
     */
    public ServerBuilder profiles(String profiles) {
        checkState(Strings.isNullOrEmpty(this.profiles), "bootConfName was already set to %s", this.profiles);
        this.profiles = requireNonNull(profiles);
        return this;
    }

    /**
     * Set the print banner name
     */
    public ServerBuilder serverThreadName(String serverThreadName) {
        checkState(Strings.isNullOrEmpty(this.serverThreadName), "bootConfName was already set to %s", this.serverThreadName);
        this.serverThreadName = requireNonNull(serverThreadName);
        return this;
    }

    /**
     * Set render view suffix
     *
     * @param viewSuffix view suffix
     */
    public ServerBuilder viewSuffix(String viewSuffix) {
        checkState(Strings.isNullOrEmpty(this.viewSuffix), "viewSuffix was already set to %s", this.viewSuffix);
        this.viewSuffix = requireNonNull(viewSuffix);
        return this;
    }

    /**
     * Set server session key
     *
     * @param sessionKey session key
     * @return this
     */
    public ServerBuilder sessionKey(String sessionKey) {
        checkState(Strings.isNullOrEmpty(this.sessionKey), "sessionKey was already set to %s", this.sessionKey);
        this.sessionKey = requireNonNull(sessionKey);
        return this;
    }

    /**
     * Set template folder
     *
     * @param folder template folder
     */
    public ServerBuilder templateFolder(String templateFolder) {
        checkState(Strings.isNullOrEmpty(this.templateFolder), "templateFolder was already set to %s", this.templateFolder);
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
     * Register websocket route
     *
     * @param path             Websocket route path
     * @param webSocketChannel WebSocket abstract interface
     * @return this
     */
    public ServerBuilder websocket(String prefix, WebSocketChannel wsChannel) {
        checkNotNull(prefix, "prefix can't be null");
        checkNotNull(wsChannel, "webSocketChannel can't be null");
        this.websSocketSessions.put(prefix, wsChannel);
        return this;
    }

    /**
     * Register get route
     *
     * @param path           Route path
     * @param requestHandler Request handler
     * @return this
     */
    public ServerBuilder get(String path, RequestHandler handler) {
        return this.route(path, HttpMethod.GET, handler);
    }

    /**
     * Register post route
     *
     * @param path           Route path
     * @param requestHandler Request handler
     * @return this
     */
    public ServerBuilder post(String path, RequestHandler handler) {
        return this.route(path, HttpMethod.POST, handler);
    }

    /**
     * Register head route
     *
     * @param path           Route path
     * @param requestHandler Request handler
     * @return this
     */
    public ServerBuilder head(String path, RequestHandler handler) {
        return this.route(path, HttpMethod.HEAD, handler);
    }

    /**
     * Register put route
     *
     * @param path           Route path
     * @param requestHandler Request handler
     * @return this
     */
    public ServerBuilder put(String path, RequestHandler handler) {
        return this.route(path, HttpMethod.PUT, handler);
    }

    /**
     * Register patch route
     *
     * @param path           Route path
     * @param requestHandler Request handler
     * @return this
     */
    public ServerBuilder patch(String path, RequestHandler handler) {
        return this.route(path, HttpMethod.PATCH, handler);
    }

    /**
     * Register delete route
     *
     * @param path           Route path
     * @param requestHandler Request handler
     * @return this
     */
    public ServerBuilder delete(String path, RequestHandler handler) {
        return this.route(path, HttpMethod.DELETE, handler);
    }

    /**
     * Register options route
     *
     * @param path           Route path
     * @param requestHandler Request handler
     * @return this
     */
    public ServerBuilder options(String path, RequestHandler handler) {
        return this.route(path, HttpMethod.OPTIONS, handler);
    }

    /**
     * Register trace route
     *
     * @param path           Route path
     * @param requestHandler Request handler
     * @return this
     */
    public ServerBuilder trace(String path, RequestHandler handler) {
        return this.route(path, HttpMethod.TRACE, handler);
    }

    /**
     * Register route
     *
     * @param path           Route path
     * @param requestHandler Request handler
     * @return this
     */
    private ServerBuilder route(String path, HttpMethod httpMethod, RequestHandler handler) {
        checkNotNull(path, "Path can't be null");
        checkNotNull(httpMethod, "Need to specify of http request method registered");
        checkNotNull(handler, "RequestHandler object can't be null");
        return this;
    }

    /**
     * Register exception advice
     *
     * @param throwableCls exception
     * @param errorHandler exception handler
     * @return this
     */
    public ServerBuilder exceptionHandler(Class<? extends Throwable> throwableCls, ExceptionHandler exceptionHandler) {
        checkNotNull(throwableCls, "Throwable type can't be null");
        checkNotNull(exceptionHandler, "ExceptionHandler can't be null");
        return this;
    }

    public ServerBuilder annotatedService(String path, Object service) {
        checkNotNull(path, "Path can't be null");
        checkNotNull(service, "Service can't be null");
        this.httpHandlerService.put(path, service);
        return this;
    }

    public ServerBuilder handlerExecutionChain(HandlerExecutionChain handlerExecutionChain) {
        checkNotNull(handlerExecutionChain, "HandlerExecutionChain can't be null");
        this.handlerExecutionChain = handlerExecutionChain;
        return this;
    }

    public ServerBuilder startStopExecutor(Executor startStopExecutor) {
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

    public ServerBuilder tls(String sslCert, String sslPrivateKey, String sslPrivateKeyPass) {
        checkNotNull(sslCert, "sslCert can't be null");
        checkNotNull(sslPrivateKey, "sslPrivateKey can't be null");
        checkNotNull(sslPrivateKeyPass, "sslPrivateKeyPass can't be null");
        this.sslCert = requireNonNull(sslCert);
        this.sslPrivateKey = requireNonNull(sslPrivateKey);
        this.sslPrivateKeyPass = requireNonNull(sslPrivateKeyPass);
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

    public ServerBuilder propertyEnvType(PropertyEnvType propertyEnvType) {
        checkNotNull(propertyEnvType, "propertyEnvType can't be null");
        this.propertyEnvType = propertyEnvType;
        return this;
    }

    public ServerBuilder channelOption(ChannelOption<?> channelOption, Object value) {
        checkNotNull(channelOption, "channelOption can't be null");
        checkNotNull(value, "value can't be null");
        checkState(!PROHIBITED_SOCKET_OPTIONS.contains(channelOption), "prohibited socket options");
        this.channelOptions.put(channelOption, value);
        return this;
    }

    public ServerBuilder childChannelOption(ChannelOption<?> channelOption, Object value) {
        checkNotNull(channelOption, "channelOption can't be null");
        checkNotNull(value, "value can't be null");
        checkState(!PROHIBITED_SOCKET_OPTIONS.contains(channelOption), "prohibited socket options");
        this.childChannelOptions.put(channelOption, value);
        return this;
    }

    public Server build() {
        return build(new String[0]);
    }

    public Server build(String[] args) {
        Class<?> caller = findMainCaller();
        return build(caller, args);
    }

    private Class<?> findMainCaller() {
        return Arrays.stream(Thread.currentThread()
                .getStackTrace())
                .filter(st -> "main".equals(st.getMethodName()))
                .findFirst()
                .map(StackTraceElement::getClassName)
                .map(UncheckedFnKit.function(Class::forName))
                .orElse(null);
    }

    /**
     * Returns a list of {@link ServerPort}s which consists of distinct port numbers except for the port
     * {@code 0}. If there are the same port numbers with different {@link SessionProtocol}s,
     * their {@link SessionProtocol}s will be merged into a single {@link ServerPort} instance.
     * The returned list is sorted as the same order of the specified {@code ports}.
     */
    private static List<ServerPort> resolveDistinctPorts(List<ServerPort> ports) {
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
    public Server build(Class<?> mainType, String[] args) {
        Flags.propertyEnvType(propertyEnvType == null ? PropertyEnvType.SYS_PROPERTY : propertyEnvType);

        this.ports.forEach(
                port -> checkState(port.protocols().stream().anyMatch(p -> p != PROXY),
                        "protocols: %s (expected: at least one %s or %s)",
                        port.protocols(), HTTP, HTTPS));

        if (!this.ports.isEmpty()) {
            ports = resolveDistinctPorts(this.ports);
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

        return new Server(new ServerConfig(mainType, args, this.banner, this.interceptors, this.filters,
                this.views, this.viewResolvers, this.viewAdapters,
                this.typeConverters, this.httpServices, this.argumentBinders,
                this.handlerMappings, this.handlerAdapters, this.handlerInterceptors,
                this.handlerExceptionResolvers, this.websSocketSessions, this.httpHandlerService,
                this.channelOptions, this.childChannelOptions, this.useSsl, this.useEpoll,this.startStopExecutor,
                this.handlerExecutionChain, this.bannerText, this.bannerFont, this.sessionKey,
                this.viewSuffix, this.templateFolder, this.serverThreadName, this.profiles, this.useSession,
                this.ports, this.maxNumConnections, this.http2InitialConnectionWindowSize,
                this.http2InitialStreamWindowSize, this.http2MaxFrameSize, this.http1MaxInitialLineLength,
                this.http1MaxHeaderSize, this.http1MaxChunkSize, this.idleTimeoutMillis, this.pingIntervalMillis,
                this.maxConnectionAgeMillis, this.http2MaxHeaderListSize, this.http2MaxStreamsPerConnection,
                this.acceptThreadCount, this.ioThreadCount, this.serverRestartCount, this.sslCert, this.sslPrivateKey,
                this.sslPrivateKeyPass), null);
    }
}
