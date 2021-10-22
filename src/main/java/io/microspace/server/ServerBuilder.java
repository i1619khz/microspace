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
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static io.microspace.internal.AnnotationUtil.FindOption.LOOKUP_SUPER_CLASSES;
import static io.microspace.internal.Flags.HTTP_METHOD_MAP;
import static io.microspace.server.SessionProtocol.HTTP;
import static io.microspace.server.SessionProtocol.HTTPS;
import static io.microspace.server.SessionProtocol.PROXY;
import static java.util.Objects.requireNonNull;
import static org.reflections8.ReflectionUtils.getAllMethods;
import static org.reflections8.ReflectionUtils.getConstructors;
import static org.reflections8.ReflectionUtils.getMethods;
import static org.reflections8.ReflectionUtils.withModifier;
import static org.reflections8.ReflectionUtils.withName;
import static org.reflections8.ReflectionUtils.withParametersCount;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.microspace.context.banner.Banner;
import io.microspace.context.banner.MicrospaceBanner;
import io.microspace.internal.AnnotationUtil;
import io.microspace.internal.DefaultValues;
import io.microspace.internal.Flags;
import io.microspace.internal.UncheckedFnKit;
import io.microspace.server.annotation.AdditionalHeader;
import io.microspace.server.annotation.AdditionalTrailer;
import io.microspace.server.annotation.Consumes;
import io.microspace.server.annotation.Delete;
import io.microspace.server.annotation.ExceptionHandler;
import io.microspace.server.annotation.ExceptionHandlerFunction;
import io.microspace.server.annotation.Get;
import io.microspace.server.annotation.Head;
import io.microspace.server.annotation.MatchesHeader;
import io.microspace.server.annotation.MatchesParam;
import io.microspace.server.annotation.Options;
import io.microspace.server.annotation.Order;
import io.microspace.server.annotation.Patch;
import io.microspace.server.annotation.Path;
import io.microspace.server.annotation.PathPrefix;
import io.microspace.server.annotation.Post;
import io.microspace.server.annotation.Produces;
import io.microspace.server.annotation.Put;
import io.microspace.server.annotation.RequestConverter;
import io.microspace.server.annotation.RequestConverterFunction;
import io.microspace.server.annotation.ResponseConverter;
import io.microspace.server.annotation.ResponseConverterFunction;
import io.microspace.server.annotation.RouteExceptionHandler;
import io.microspace.server.annotation.StatusCode;
import io.microspace.server.annotation.Trace;
import io.netty.channel.ChannelOption;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * @author i1619kHz
 */
public final class ServerBuilder {
    private static final Logger log = LoggerFactory.getLogger(ServerBuilder.class);

    private static final long MIN_PING_INTERVAL_MILLIS = 1000L;
    private static final long MIN_MAX_CONNECTION_AGE_MILLIS = 1_000L;

    private final Map<Class<? extends Throwable>,
            ExceptionHandlerFunction> exceptionServices = new ConcurrentHashMap<>();
    private final List<ServiceConfigSetters> serviceConfigSetters = new ArrayList<>();
    private final Map<ChannelOption<?>, Object> channelOptions = new HashMap<>();
    private final Map<ChannelOption<?>, Object> childChannelOptions = new HashMap<>();
    private final Map<String, Set<String>> addedHeaders = new ConcurrentHashMap<>();
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
    private long stopQuietPeriod = Flags.defaultStopQuietPeriod();
    private long stopTimeout = Flags.defaultStopTimeout();
    private int acceptThreadCount = Flags.defaultAcceptThreadCount();
    private int ioThreadCount = Flags.defaultIoThreadCount();
    private int serverRestartCount = Flags.defaultServerRestartCount();
    private boolean shutdownWorkerGroupOnStop = Flags.defaultShutdownWorkerGroupOnStop();
    private ExecutorService startStopExecutor = GlobalEventExecutor.INSTANCE;
    private List<ServerPort> ports = new ArrayList<>();
    private boolean useSsl = Flags.useSsl();
    private boolean useEpoll = Flags.useEpoll();
    private boolean useSession = Flags.useSession();
    private Banner banner = new MicrospaceBanner();
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

    void serviceConfigBuilder(ServiceConfigBuilder serviceConfigBuilder) {
        serviceConfigSetters.add(serviceConfigBuilder);
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
        checkArgument(null != serverPort, "serverPort");
        this.ports.add(requireNonNull(serverPort));
        return this;
    }

    public ServerBuilder http(int serverPort) {
        checkArgument(Flags.checkPort(serverPort), "port number must be available");
        return this.port(new ServerPort(serverPort, HTTP));
    }

    public ServerBuilder https(int serverPort) {
        checkArgument(Flags.checkPort(serverPort), "port number must be available");
        return this.port(new ServerPort(serverPort, HTTPS));
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

    private List<ServiceConfig> create(String prefix, Object service, Method method) {
        final Set<Annotation> methodAnnotations = httpMethodAnnotations(method);
        if (methodAnnotations.isEmpty()) {
            throw new IllegalArgumentException("HTTP Method specification is missing: " + method.getName());
        }
        final Class<?> clazz = service.getClass();
        final Map<HttpMethod, List<String>> httpMethodPatternsMap = getHttpMethodPatternsMap(method,
                                                                                             methodAnnotations);
        final HttpStatus statusCode = statusCode(method);
        final String computedPathPrefix = computePathPrefix(clazz, prefix);
        final Set<MediaType> consumableMediaTypes = consumableMediaTypes(method, clazz);
        final Set<MediaType> producibleMediaTypes = producibleMediaTypes(method, clazz);
        final List<Route> routes = httpMethodPatternsMap.entrySet().stream().flatMap(
                pattern -> {
                    final HttpMethod httpMethod = pattern.getKey();
                    final List<String> pathMappings = pattern.getValue();
                    return pathMappings.stream().map(
                            pathMapping -> buildRoute(method, clazz, statusCode,
                                                      computedPathPrefix, consumableMediaTypes,
                                                      producibleMediaTypes, httpMethod, pathMapping));
                }).collect(toImmutableList());

        final Set<RequestConverterFunction> requestConverterFunctions =
                getAnnotatedInstances(method, clazz, RequestConverter.class, RequestConverterFunction.class);
        final Set<ResponseConverterFunction> responseConverterFunctions =
                getAnnotatedInstances(method, clazz, ResponseConverter.class, ResponseConverterFunction.class);
        final Set<ExceptionHandlerFunction> exceptionHandlerFunctions =
                getAnnotatedInstances(method, clazz, RouteExceptionHandler.class,
                                      ExceptionHandlerFunction.class);

        final String classAlias = clazz.getName();
        final String methodAlias = String.format("%s.%s()", classAlias, method.getName());
        setAdditionalHeader(clazz, "header", classAlias, "class", AdditionalHeader.class,
                            AdditionalHeader::name, AdditionalHeader::value);
        setAdditionalHeader(method, "header", methodAlias, "method", AdditionalHeader.class,
                            AdditionalHeader::name, AdditionalHeader::value);
        setAdditionalHeader(clazz, "trailer", classAlias, "class",
                            AdditionalTrailer.class, AdditionalTrailer::name, AdditionalTrailer::value);
        setAdditionalHeader(method, "trailer", methodAlias, "method",
                            AdditionalTrailer.class, AdditionalTrailer::name, AdditionalTrailer::value);
        return null;
    }

    private Route buildRoute(Method method, Class<?> clazz,
                             HttpStatus statusCode,
                             String computedPathPrefix,
                             Set<MediaType> consumableMediaTypes,
                             Set<MediaType> producibleMediaTypes,
                             HttpMethod httpMethod,
                             String pathMapping) {
        return Route.builder()
                    .path(computedPathPrefix, pathMapping)
                    .methods(httpMethod)
                    .consumes(consumableMediaTypes)
                    .produces(producibleMediaTypes)
                    .matchesParams(
                            predicates(method, clazz, MatchesParam.class,
                                       MatchesParam::value))
                    .matchesHeaders(
                            predicates(method, clazz, MatchesHeader.class,
                                       MatchesHeader::value))
                    .statusCode(statusCode)
                    .build();
    }

    @SuppressWarnings("unchecked")
    private List<Method> getMethodsByPredicate(Object service,
                                               Predicate<? super Class<? extends Annotation>> predicate) {
        return getAllMethods(service.getClass(), withModifier(Modifier.PUBLIC))
                .stream()
                // Lookup super classes just in case if the object is a proxy.
                .filter(m -> AnnotationUtil.getAnnotations(m, LOOKUP_SUPER_CLASSES)
                                           .stream()
                                           .map(Annotation::annotationType)
                                           .anyMatch(predicate))
                .sorted(Comparator.comparingInt(this::order))
                .collect(toImmutableList());
    }

    /**
     * Returns the list of {@link ExceptionHandler} annotated methods.
     */
    private List<Method> exceptionHandlerMethods(Object service) {
        return getMethodsByPredicate(service, type -> type.isAssignableFrom(ExceptionHandler.class));
    }

    /**
     * Returns the list of {@link Path} annotated methods.
     */
    private List<Method> requestMappingMethods(Object object) {
        return getMethodsByPredicate(object, type -> type.isAssignableFrom(Path.class) ||
                                                     HTTP_METHOD_MAP.containsKey(type));
    }

    /**
     * Returns the value of the order of the {@link Method}. The order could be retrieved from {@link Order}
     * annotation. 0 would be returned if there is no specified {@link Order} annotation.
     */
    private int order(Method method) {
        final Order order = AnnotationUtil.findFirst(method, Order.class);
        return order != null ? order.value() : 0;
    }

    /**
     * Returns {@link Set} of HTTP method annotations of a given method.
     * The annotations are as follows.
     *
     * @see Options
     * @see Get
     * @see Head
     * @see Post
     * @see Put
     * @see Patch
     * @see Delete
     * @see Trace
     */
    private Set<Annotation> httpMethodAnnotations(Method method) {
        return AnnotationUtil.getAnnotations(method, LOOKUP_SUPER_CLASSES)
                             .stream()
                             .filter(annotation -> HTTP_METHOD_MAP.containsKey(annotation.annotationType())
                                                   || annotation.annotationType().isAssignableFrom(Path.class))
                             .collect(Collectors.toSet());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <A extends Annotation, K1 extends K, V1 extends V, K, V>
    Map<K, V> getAnnotatedInstanceMap(AnnotatedElement method,
                                      AnnotatedElement clazz,
                                      Class<K> keyType,
                                      Class<V> valueType,
                                      Class<A> annotation,
                                      Function<A, Class<K1>> keyGetter,
                                      Function<A, Class<V1>> valueGetter) {
        final ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
        Streams.concat(AnnotationUtil.findAll(clazz, annotation).stream(),
                       AnnotationUtil.findAll(method, annotation).stream()
        ).forEach(header -> {
            final Class<K1> key = keyGetter.apply(header);
            final Class<V1> value = valueGetter.apply(header);
            final @Nullable Constructor keyConstructor = Iterables.getFirst(
                    getConstructors(key, withParametersCount(0)), null);
            final @Nullable Constructor valueConstructor = Iterables.getFirst(
                    getConstructors(value, withParametersCount(0)), null);
            if (null != keyConstructor && null != valueConstructor) {
                builder.put(getInstance(keyConstructor, keyType), getInstance(valueConstructor, valueType));
            }
        });
        return builder.build();
    }

    private <T extends Annotation> void setAdditionalHeader(AnnotatedElement element,
                                                            String clsAlias,
                                                            String elementAlias,
                                                            String level,
                                                            Class<T> annotation,
                                                            Function<T, String> nameGetter,
                                                            Function<T, String[]> valueGetter) {
        requireNonNull(element, "element");
        requireNonNull(level, "level");
        AnnotationUtil.findAll(element, annotation).forEach(header -> {
            final String name = nameGetter.apply(header);
            final String[] value = valueGetter.apply(header);
            if (addedHeaders.containsKey(name)) {
                log.warn("The additional {} named '{}' at '{}' is set at the same {} level already;" +
                         "ignoring.",
                         clsAlias, name, elementAlias, level);
                return;
            }
            addedHeaders.put(name, ImmutableSet.copyOf(value));
        });
    }

    private <T extends Annotation, R> ImmutableSet<R> getAnnotatedInstances(AnnotatedElement method,
                                                                            AnnotatedElement clazz,
                                                                            Class<T> annotationType,
                                                                            Class<R> resultType) {
        final ImmutableSet.Builder<R> builder = ImmutableSet.builder();
        Stream.concat(AnnotationUtil.findAll(method, annotationType).stream(),
                      AnnotationUtil.findAll(clazz, annotationType).stream())
              .forEach(annotation -> builder.add(getInstance(annotation, resultType)));
        return builder.build();
    }

    private <T> T getInstance(Constructor<?> constructor, Class<T> expectedType) {
        try {
            constructor.setAccessible(true);
            return expectedType.cast(constructor.newInstance());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "A class specified in " + constructor.getName() +
                    " annotation cannot be cast to " + expectedType, e);
        }
    }

    private <T> T getInstance(Annotation annotation, Class<T> expectedType) {
        try {
            final Object instance = getInstance(annotation);
            return expectedType.cast(instance);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "A class specified in @" + annotation.annotationType().getSimpleName() +
                    " annotation cannot be cast to " + expectedType, e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getInstance(Annotation annotation) throws Exception {
        Class<? extends T> clazz = (Class<? extends T>) invokeValueMethod(annotation).getClass();
        final Constructor<? extends T> constructor =
                Iterables.getFirst(getConstructors(clazz, withParametersCount(0)), null);
        assert constructor != null : "constructor can't be null";
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    /**
     * Returns a list of predicates which will be used to evaluate whether a request can be accepted
     * by a service method.
     */
    private <T extends Annotation> List<String> predicates(Method method, Class<?> clazz,
                                                           Class<T> annotationType,
                                                           Function<T, String> toStringPredicate) {
        final List<T> classLevel = AnnotationUtil.findAll(clazz, annotationType);
        final List<T> methodLevel = AnnotationUtil.findAll(method, annotationType);
        return Streams.concat(classLevel.stream(), methodLevel.stream())
                      .map(toStringPredicate).collect(toImmutableList());
    }

    private HttpStatus statusCode(Method method) {
        final StatusCode statusCodeAnnotation = AnnotationUtil.findFirst(method, StatusCode.class);
        if (statusCodeAnnotation == null) {
            // Set a default HTTP status code for a response depending on the return type of the method.
            final Class<?> returnType = method.getReturnType();
            return returnType == Void.class ||
                   returnType == void.class ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        }
        final int statusCode = statusCodeAnnotation.value();
        checkArgument(statusCode >= 0,
                      "invalid HTTP status code: %s (expected: >= 0)", statusCode);
        return HttpStatus.valueOf(statusCode);
    }

    private String computePathPrefix(Class<?> clazz, String pathPrefix) {
        ensureAbsolutePath(pathPrefix, "pathPrefix");
        final PathPrefix pathPrefixAnnotation = AnnotationUtil.findFirst(clazz, PathPrefix.class);
        if (null == pathPrefixAnnotation) {
            return pathPrefix;
        }

        final String pathPrefixValue = pathPrefixAnnotation.value();
        ensureAbsolutePath(pathPrefixValue, "pathPrefixValue");
        if (pathPrefix.equals("/") && pathPrefixValue.equals("/")) {
            return pathPrefix;
        }
        return pathPrefix + pathPrefixValue;
    }

    private void ensureAbsolutePath(String path, String paramName) {
        checkArgument(!Strings.isNullOrEmpty(paramName), "paramName");
        if (Strings.isNullOrEmpty(path) || path.charAt(0) != '/') {
            throw new IllegalArgumentException(paramName + ": " + path +
                                               " (expected: an absolute path starting with '/')");
        }
    }

    private Set<MediaType> consumableMediaTypes(Method method, Class<?> clazz) {
        List<Consumes> consumes = AnnotationUtil.findAll(method, Consumes.class);
        if (consumes.isEmpty()) {
            consumes = AnnotationUtil.findAll(clazz, Consumes.class);
        }
        return consumes.stream()
                       .map(Consumes::value)
                       .map(MediaType::parse)
                       .collect(toImmutableSet());
    }

    private Set<MediaType> producibleMediaTypes(Method method, Class<?> clazz) {
        List<Produces> produces = AnnotationUtil.findAll(method, Produces.class);
        if (produces.isEmpty()) {
            produces = AnnotationUtil.findAll(clazz, Produces.class);
        }
        return produces.stream()
                       .map(Produces::value)
                       .map(MediaType::parse)
                       .collect(toImmutableSet());
    }

    /**
     * Returns path patterns for each {@link HttpMethod}. The path pattern might be specified by
     * {@link Path} or HTTP method annotations such as {@link Get} and {@link Post}. Path patterns
     * may be specified by either HTTP method annotations, or {@link Path} annotations but not both
     * simultaneously.
     */
    private Map<HttpMethod, List<String>> getHttpMethodPatternsMap(Method method,
                                                                   Set<Annotation> methodAnnotations) {
        final Map<HttpMethod, List<String>> httpMethodAnnotatedPatternMap =
                getHttpMethodAnnotatedPatternMap(methodAnnotations);

        if (httpMethodAnnotatedPatternMap.isEmpty()) {
            throw new IllegalArgumentException(method.getDeclaringClass().getName() + '#' + method.getName() +
                                               " must have an HTTP method annotation.");
        }

        return httpMethodAnnotatedPatternMap.entrySet().stream().collect(
                ImmutableMap.toImmutableMap(Map.Entry::getKey, entry -> {
                    final List<String> httpMethodPaths = entry.getValue();
                    if (httpMethodPaths.isEmpty()) {
                        // Add an empty value if HTTP method annotation value is empty or not specified.
                        httpMethodPaths.add("");
                    }
                    return ImmutableList.copyOf(httpMethodPaths);
                }));
    }

    private Map<HttpMethod, List<String>> getHttpMethodAnnotatedPatternMap(
            Set<Annotation> methodAnnotations) {
        final Map<HttpMethod, List<String>> httpMethodPatternMap = new EnumMap<>(HttpMethod.class);
        methodAnnotations
                .stream()
                .filter(annotation -> HTTP_METHOD_MAP.containsKey(annotation.annotationType())
                                      || annotation.annotationType().isAssignableFrom(Path.class))
                .forEach(annotation -> {
                    HttpMethod httpMethod = HTTP_METHOD_MAP.get(annotation.annotationType());
                    if (null == httpMethod) {
                        final Path path = ((Path) annotation);
                        httpMethod = path.method();
                    }
                    final String value = (String) invokeValueMethod(annotation);
                    final List<String> patterns = httpMethodPatternMap
                            .computeIfAbsent(httpMethod, ignored -> new ArrayList<>());
                    if (DefaultValues.isSpecified(value)) {
                        patterns.add(value);
                    }
                });
        return httpMethodPatternMap;
    }

    @SuppressWarnings("unchecked")
    private Object invokeAnnotationMethod(Annotation a, String invokeName) {
        try {
            final Method method = Iterables.getFirst(
                    getMethods(a.annotationType(), withName(invokeName)), null);
            assert method != null : "No 'value' method is found from " + a;
            return method.invoke(a);
        } catch (Exception e) {
            throw new IllegalStateException("An annotation @" + a.annotationType().getSimpleName() +
                                            " must have a 'value' method", e);
        }
    }

    /**
     * Returns an object which is returned by {@code value()} method of the specified annotation {@code a}.
     */
    private Object invokeValueMethod(Annotation a) {
        return invokeAnnotationMethod(a, "value");
    }

    public ServerBuilder service(String pathPrefix, HttpService service) {
        return route().pathPrefix(pathPrefix).methods(HttpMethod.knownMethods()).build(service);
    }

    public ServerBuilder service(String pathPrefix, HttpService service, HttpMethod... httpMethods) {
        return route().pathPrefix(pathPrefix).methods(httpMethods).build(service);
    }

    public ServerBuilder service(String pathPrefix, HttpService service, Iterable<HttpMethod> httpMethods) {
        return route().pathPrefix(pathPrefix).methods(httpMethods).build(service);
    }

    public ServerBuilder service(Route route, HttpService service) {
        return route().addRoute(route).build(service);
    }

    public ServerBuilder annotatedService(Object service) {
        checkArgument(null != service, "service");
        return annotatedService("/", service);
    }

    public ServerBuilder annotatedService(String pathPrefix, Object service) {
        return annotatedService().pathPrefix(pathPrefix)
                                 .exceptionHandlers(ImmutableList.of())
                                 .requestConverters(ImmutableList.of())
                                 .responseConverters(ImmutableList.of())
                                 .build(service);
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

    public ServerBuilder stopQuietPeriod(long stopQuietPeriod) {
        this.stopQuietPeriod = stopQuietPeriod;
        return this;
    }

    public ServerBuilder stopTimeout(long stopTimeout) {
        this.stopTimeout = stopTimeout;
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

    public ServerBuilder banner(Banner banner) {
        this.banner = requireNonNull(banner, "banner");
        return this;
    }

    public Server build() {
        return build(new String[0]);
    }

    public Server build(String[] args) {
        return build(callerClass(), args);
    }

    private Class<?> callerClass() {
        final Thread currentThread = Thread.currentThread();
        final Optional<? extends Class<?>> classOptional = Arrays
                .stream(currentThread.getStackTrace())
                .filter(st -> "main".equals(st.getMethodName()))
                .findFirst()
                .map(StackTraceElement::getClassName)
                .map(UncheckedFnKit.wrap(Class::forName));
        return classOptional.orElse(null);
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
                this.requestTimeoutMillis != null ? this.requestTimeoutMillis : 10000;
        final long maxRequestLength =
                this.maxRequestLength != null ? this.maxRequestLength : 10 * 1024 * 1024;
        final boolean verboseResponses =
                this.verboseResponses != null ? this.verboseResponses : false;

        final List<ServiceConfig> serviceConfigs = serviceConfigSetters()
                .stream()
                .flatMap(cfgSetters -> {
                    if (cfgSetters instanceof VirtualHostBuilder) {
                        return ((VirtualHostBuilder) cfgSetters)
                                .buildServiceConfigBuilder(null, null).stream();
                    } else if (cfgSetters instanceof AnnotatedServiceBindingBuilder) {
                        return ((AnnotatedServiceBindingBuilder) cfgSetters)
                                .buildServiceConfigBuilder(null, null).stream();
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

        return new Server(new ServerConfig(serviceConfigs, exceptionServices, meterRegistry,
                                           bootCls, args, banner, channelOptions, childChannelOptions, useSsl,
                                           useEpoll, shutdownWorkerGroupOnStop, startStopExecutor, bannerText,
                                           bannerFont, sessionKey, viewSuffix, templateFolder, serverThreadName,
                                           profiles, useSession, ports, maxNumConnections,
                                           http2InitialConnectionWindowSize, http2InitialStreamWindowSize,
                                           http2MaxFrameSize, http1MaxInitialLineLength, http1MaxHeaderSize,
                                           http1MaxChunkSize, idleTimeoutMillis, pingIntervalMillis,
                                           maxConnectionAgeMillis, http2MaxHeaderListSize,
                                           http2MaxStreamsPerConnection, acceptThreadCount, ioThreadCount,
                                           serverRestartCount, stopQuietPeriod, stopTimeout), null);
    }
}
