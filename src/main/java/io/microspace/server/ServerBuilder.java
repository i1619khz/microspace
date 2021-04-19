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
import io.microspace.core.Flags;
import io.netty.channel.ChannelOption;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

/**
 * @author i1619kHz
 */
public final class ServerBuilder {
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
  private ServerPort serverPort = new ServerPort(Flags.defaultPort(), SessionProtocol.HTTP);
  private String bannerText = Flags.bannerText();
  private String bannerFont = Flags.bannerFont();
  private String sessionKey = Flags.sessionKey();
  private String viewSuffix = Flags.viewSuffix();
  private String templateFolder = Flags.templateFolder();
  private String serverThreadName = Flags.serverThreadName();
  private String profiles = Flags.profiles();

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

  private long idleTimeoutMillis = Flags.idleTimeoutMillis();
  private long pingIntervalMillis = Flags.pingIntervalMillis();
  private long maxConnectionAgeMillis = Flags.maxConnectionAgeMillis();
  private long http2MaxHeaderListSize = Flags.http2MaxHeaderListSize();
  private long http2MaxStreamsPerConnection = Flags.http2MaxStreamsPerConnection();

  private boolean useSsl = Flags.useSsl();
  private boolean useEpoll = Flags.useEpoll();
  private boolean useSession = Flags.useSession();

  public ServerBuilder http(int serverPort) {
    checkState(serverPort > 0 && serverPort <= 65533, "Port number must be available");
    return this.port(new ServerPort(serverPort, SessionProtocol.HTTP));
  }

  public ServerBuilder https(int serverPort) {
    checkState(serverPort > 0 && serverPort <= 65533, "Port number must be available");
    return this.port(new ServerPort(serverPort, SessionProtocol.HTTPS));
  }

  public ServerBuilder port(InetSocketAddress localAddress) {
    checkNotNull(localAddress, "InetSocketAddress can't be null");
    return this.port(new ServerPort(requireNonNull(localAddress)));
  }

  /**
   * Set the listening port number.
   */
  public ServerBuilder port(ServerPort port) {
    checkNotNull(port, "ServerPort can't be null");
    this.serverPort = requireNonNull(port);
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
    checkState(Strings.isNullOrEmpty(this.templateFolder), "templateFolder was already set to %s",
        this.templateFolder);
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
    checkNotNull(prefix, "prefix");
    checkNotNull(wsChannel, "webSocketChannel");
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
  public ServerBuilder get(String path, RequestHandler requestHandler) {
    return this.route(path, HttpMethod.GET, requestHandler);
  }

  /**
   * Register post route
   *
   * @param path           Route path
   * @param requestHandler Request handler
   * @return this
   */
  public ServerBuilder post(String path, RequestHandler requestHandler) {
    return this.route(path, HttpMethod.POST, requestHandler);
  }

  /**
   * Register head route
   *
   * @param path           Route path
   * @param requestHandler Request handler
   * @return this
   */
  public ServerBuilder head(String path, RequestHandler requestHandler) {
    return this.route(path, HttpMethod.HEAD, requestHandler);
  }

  /**
   * Register put route
   *
   * @param path           Route path
   * @param requestHandler Request handler
   * @return this
   */
  public ServerBuilder put(String path, RequestHandler requestHandler) {
    return this.route(path, HttpMethod.PUT, requestHandler);
  }

  /**
   * Register patch route
   *
   * @param path           Route path
   * @param requestHandler Request handler
   * @return this
   */
  public ServerBuilder patch(String path, RequestHandler requestHandler) {
    return this.route(path, HttpMethod.PATCH, requestHandler);
  }

  /**
   * Register delete route
   *
   * @param path           Route path
   * @param requestHandler Request handler
   * @return this
   */
  public ServerBuilder delete(String path, RequestHandler requestHandler) {
    return this.route(path, HttpMethod.DELETE, requestHandler);
  }

  /**
   * Register options route
   *
   * @param path           Route path
   * @param requestHandler Request handler
   * @return this
   */
  public ServerBuilder options(String path, RequestHandler requestHandler) {
    return this.route(path, HttpMethod.OPTIONS, requestHandler);
  }

  /**
   * Register trace route
   *
   * @param path           Route path
   * @param requestHandler Request handler
   * @return this
   */
  public ServerBuilder trace(String path, RequestHandler requestHandler) {
    return this.route(path, HttpMethod.TRACE, requestHandler);
  }

  /**
   * Register route
   *
   * @param path           Route path
   * @param requestHandler Request handler
   * @return this
   */
  public ServerBuilder route(String path, HttpMethod httpMethod, RequestHandler requestHandler) {
    checkNotNull(path, "Path can't be null");
    checkNotNull(httpMethod, "Need to specify of http request method registered");
    checkNotNull(requestHandler, "RequestHandler object can't be null");
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

  public ServerBuilder maxNumConnections(int maxNumConnections) {
    this.maxNumConnections = maxNumConnections;
    return this;
  }

  public ServerBuilder http2InitialConnectionWindowSize(int http2InitialConnectionWindowSize) {
    this.http2InitialConnectionWindowSize = http2InitialConnectionWindowSize;
    return this;
  }

  public ServerBuilder http2InitialStreamWindowSize(int http2InitialStreamWindowSize) {
    this.http2InitialStreamWindowSize = http2InitialStreamWindowSize;
    return this;
  }

  public ServerBuilder http2MaxFrameSize(int http2MaxFrameSize) {
    this.http2MaxFrameSize = http2MaxFrameSize;
    return this;
  }

  public ServerBuilder http1MaxInitialLineLength(int http1MaxInitialLineLength) {
    this.http1MaxInitialLineLength = http1MaxInitialLineLength;
    return this;
  }

  public ServerBuilder http1MaxHeaderSize(int http1MaxHeaderSize) {
    this.http1MaxHeaderSize = http1MaxHeaderSize;
    return this;
  }

  public ServerBuilder http1MaxChunkSize(int http1MaxChunkSize) {
    this.http1MaxChunkSize = http1MaxChunkSize;
    return this;
  }

  public ServerBuilder idleTimeoutMillis(long idleTimeoutMillis) {
    this.idleTimeoutMillis = idleTimeoutMillis;
    return this;
  }

  public ServerBuilder pingIntervalMillis(long pingIntervalMillis) {
    this.pingIntervalMillis = pingIntervalMillis;
    return this;
  }

  public ServerBuilder maxConnectionAgeMillis(long maxConnectionAgeMillis) {
    this.maxConnectionAgeMillis = maxConnectionAgeMillis;
    return this;
  }

  public ServerBuilder http2MaxHeaderListSize(long http2MaxHeaderListSize) {
    this.http2MaxHeaderListSize = http2MaxHeaderListSize;
    return this;
  }

  public ServerBuilder http2MaxStreamsPerConnection(long http2MaxStreamsPerConnection) {
    this.http2MaxStreamsPerConnection = http2MaxStreamsPerConnection;
    return this;
  }

  public ServerBuilder tls(String sslCert, String sslPrivateKey, String sslPrivateKeyPass){
    this.sslCert = sslCert;
    this.sslPrivateKey = sslPrivateKey;
    this.sslPrivateKeyPass = sslPrivateKeyPass;
    return this;
  }

  public ServerBuilder acceptThreadCount(int acceptThreadCount) {
    this.acceptThreadCount = acceptThreadCount;
    return this;
  }

  public ServerBuilder ioThreadCount(int ioThreadCount) {
    this.ioThreadCount = ioThreadCount;
    return this;
  }

  /**
   * Build {@link Server}
   *
   * @return Http Server
   */
  public Server build() {
    return new Server(new ServerSpec(this.interceptors, this.filters,
        this.views, this.viewResolvers, this.viewAdapters,
        this.typeConverters, this.httpServices, this.argumentBinders,
        this.handlerMappings, this.handlerAdapters, this.handlerInterceptors,
        this.handlerExceptionResolvers, this.websSocketSessions, this.httpHandlerService,
        this.channelOptions, this.childChannelOptions, this.useSsl, this.useEpoll,
        this.handlerExecutionChain, this.bannerText, this.bannerFont, this.sessionKey,
        this.viewSuffix, this.templateFolder, this.serverThreadName, this.profiles, this.useSession,
        this.serverPort, this.maxNumConnections, this.http2InitialConnectionWindowSize,
        this.http2InitialStreamWindowSize, this.http2MaxFrameSize, this.http1MaxInitialLineLength,
        this.http1MaxHeaderSize, this.http1MaxChunkSize, this.idleTimeoutMillis, this.pingIntervalMillis,
        this.maxConnectionAgeMillis, this.http2MaxHeaderListSize, this.http2MaxStreamsPerConnection,
        this.acceptThreadCount, this.ioThreadCount, this.sslCert, this.sslPrivateKey, this.sslPrivateKeyPass));
  }
}
