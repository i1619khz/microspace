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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.microspace.context.banner.Banner;
import io.microspace.context.banner.DefaultApplicationBanner;
import io.microspace.internal.AnnotationUtil;
import io.microspace.internal.DefaultValues;
import io.microspace.internal.Flags;
import io.microspace.internal.UncheckedFnKit;
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
import io.microspace.server.annotation.StatusCode;
import io.microspace.server.annotation.Trace;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.util.concurrent.GlobalEventExecutor;

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
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static io.microspace.internal.AnnotationUtil.FindOption;
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

    private static final long MIN_PING_INTERVAL_MILLIS = 1000L;
    private static final long MIN_MAX_CONNECTION_AGE_MILLIS = 1_000L;

    private final Map<String, ServiceWrap> serviceWraps = new ConcurrentHashMap<>();
    private final MeterRegistry meterRegistry = new CompositeMeterRegistry();
    private final Map<ChannelOption<?>, Object> channelOptions = new HashMap<>();
    private final Map<ChannelOption<?>, Object> childChannelOptions = new HashMap<>();
    private final Banner banner = new DefaultApplicationBanner();
    private Executor startStopExecutor = GlobalEventExecutor.INSTANCE;
    private List<ServerPort> ports = new ArrayList<>();
    private String bannerText = Flags.bannerText();
    private String bannerFont = Flags.bannerFont();
    private String sessionKey = Flags.sessionKey();
    private String viewSuffix = Flags.viewSuffix();
    private String templateFolder = Flags.templateFolder();
    private String serverThreadName = Flags.serverThreadName();
    private String profiles = Flags.profiles();

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

    private Duration stopQuietPeriod = Flags.stopQuietPeriod();
    private Duration stopTimeout = Flags.stopTimeout();

    private String sslCert;
    private String sslPrivateKey;
    private String sslPrivateKeyPass;

    public ServerBuilder http(int serverPort) {
        checkArgument(serverPort > 0 && serverPort <= 65533, "port number must be available");
        return this.port(new ServerPort(serverPort, HTTP));
    }

    public ServerBuilder https(int serverPort) {
        checkArgument(serverPort > 0 && serverPort <= 65533, "port number must be available");
        return this.port(new ServerPort(serverPort, HTTPS));
    }

    public ServerBuilder port(InetSocketAddress localAddress) {
        checkArgument(null != localAddress, "inetSocketAddress can't be null");
        return this.port(new ServerPort(requireNonNull(localAddress)));
    }

    public ServerBuilder port(ServerPort serverPort) {
        checkArgument(null != serverPort, "serverPort can't be null");
        this.ports.add(requireNonNull(serverPort));
        return this;
    }

    public ServerBuilder port(ServerPort... serverPorts) {
        ImmutableSet.copyOf(serverPorts).forEach(this::port);
        return this;
    }

    public ServerBuilder port(Integer... serverPorts) {
        ImmutableSet.copyOf(serverPorts).forEach((port -> this.port(new ServerPort(port, HTTP))));
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
     * Register get route
     *
     * @param prefix         Route path
     * @param requestHandler Request handler
     * @return this
     */
    public ServerBuilder get(String prefix, HttpService service) {
        return service(prefix, service, HttpMethod.GET);
    }

    /**
     * Register post route
     *
     * @param prefix         Route path
     * @param requestHandler Request handler
     * @return this
     */
    public ServerBuilder post(String prefix, HttpService service) {
        return service(prefix, service, HttpMethod.POST);
    }

    /**
     * Register head route
     *
     * @param prefix         Route path
     * @param requestHandler Request handler
     * @return this
     */
    public ServerBuilder head(String prefix, HttpService service) {
        return service(prefix, service, HttpMethod.HEAD);
    }

    /**
     * Register put route
     *
     * @param prefix         Route path
     * @param requestHandler Request handler
     * @return this
     */
    public ServerBuilder put(String prefix, HttpService service) {
        return service(prefix, service, HttpMethod.PUT);
    }

    /**
     * Register patch route
     *
     * @param prefix         Route path
     * @param requestHandler Request handler
     * @return this
     */
    public ServerBuilder patch(String prefix, HttpService service) {
        return service(prefix, service, HttpMethod.PATCH);
    }

    /**
     * Register delete route
     *
     * @param prefix         Route path
     * @param requestHandler Request handler
     * @return this
     */
    public ServerBuilder delete(String prefix, HttpService service) {
        return service(prefix, service, HttpMethod.DELETE);
    }

    /**
     * Register options route
     *
     * @param prefix         Route path
     * @param requestHandler Request handler
     * @return this
     */
    public ServerBuilder options(String prefix, HttpService service) {
        return service(prefix, service, HttpMethod.OPTIONS);
    }

    /**
     * Register trace route
     *
     * @param prefix         Route path
     * @param requestHandler Request handler
     * @return this
     */
    public ServerBuilder trace(String prefix, HttpService service) {
        return service(prefix, service, HttpMethod.TRACE);
    }

    /**
     * Register exception advice
     *
     * @param throwableCls exception
     * @param errorHandler exception handler
     * @return this
     */
    public ServerBuilder exceptionHandler(Class<? extends Throwable> throwableCls, ExceptionHandlerFunction function) {
        checkNotNull(throwableCls, "Throwable type can't be null");
        checkNotNull(function, "ExceptionHandler can't be null");
        return this;
    }

    public ServerBuilder service(String prefix, HttpService service) {
        return service(prefix, service, HttpMethod.knownMethods());
    }

    public ServerBuilder service(String prefix, HttpService service, HttpMethod... httpMethods) {
        return service(prefix, service, ImmutableSet.copyOf(requireNonNull(httpMethods, "httpMethods")));
    }

    public ServerBuilder service(String prefix, HttpService service, Iterable<HttpMethod> httpMethods) {
        checkArgument(!Strings.isNullOrEmpty(prefix), "path can't be null");
        checkArgument(null != httpMethods, "need to specify of http request method registered");
        checkArgument(null != service, "httpService object can't be null");
        serviceWraps.put(prefix, ServiceWrap.of(Route.builder()
                .path(prefix, prefix)
                .methods(httpMethods)
                .consumes(ImmutableSet.of())
                .produces(ImmutableSet.of())
                .matchesParams(ImmutableList.of())
                .matchesHeaders(ImmutableList.of())
                .statusCode(HttpStatus.OK)
                .build(), service));
        return this;
    }

    public ServerBuilder serviceUnder(String prefix, HttpService service) {
        checkArgument(!Strings.isNullOrEmpty(prefix), "prefix");
        checkArgument(null != service, "service");
        return get(prefix, service);
    }

    public ServerBuilder annotatedService(Object service) {
        checkArgument(null != service, "service");
        return annotatedService("/", service);
    }

    public ServerBuilder annotatedService(String prefix, Object service) {
        checkArgument(!Strings.isNullOrEmpty(prefix), "prefix can't be null");
        checkArgument(null != service, "object can't be null");

        final List<Method> methods = requestMappingMethods(service);
        final List<ServiceWrap> serviceWraps = methods.stream()
                .flatMap(method -> buildService(prefix, service, method).stream())
                .collect(toImmutableList());

        serviceWraps.forEach(serviceWrap -> this.serviceWraps.put(serviceWrap.route().fullPath(), serviceWrap));
        return this;
    }

    private List<ServiceWrap> buildService(String prefix, Object service, Method method) {
        final Set<Annotation> methodAnnotations = httpMethodAnnotations(method);
        if (methodAnnotations.isEmpty()) {
            throw new IllegalArgumentException("HTTP Method specification is missing: " + method.getName());
        }
        final Class<?> clazz = service.getClass();
        final Map<HttpMethod, List<String>> httpMethodPatternsMap = getHttpMethodPatternsMap(method, methodAnnotations);

        final HttpStatus statusCode = statusCode(method);
        final String computedPathPrefix = computePathPrefix(clazz, prefix);
        final Set<MediaType> consumableMediaTypes = consumableMediaTypes(method, clazz);
        final Set<MediaType> producibleMediaTypes = producibleMediaTypes(method, clazz);
        final List<Route> routes = httpMethodPatternsMap.entrySet().stream().flatMap(
                pattern -> {
                    final HttpMethod httpMethod = pattern.getKey();
                    final List<String> pathMappings = pattern.getValue();
                    return pathMappings.stream().map(
                            pathMapping -> Route.builder()
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
                                    .build());
                }).collect(toImmutableList());

        final Set<RequestConverterFunction> requestConverterFunctions =
                getAnnotatedInstances(method, clazz, RequestConverter.class, RequestConverterFunction.class);
        final Set<ResponseConverterFunction> responseConverterFunctions =
                getAnnotatedInstances(method, clazz, ResponseConverter.class, ResponseConverterFunction.class);
        final Set<ExceptionHandlerFunction> exceptionHandlerFunctions =
                getAnnotatedInstances(method, clazz, ExceptionHandler.class, ExceptionHandlerFunction.class);

        return routes.stream().map(route -> ServiceWrap.of(route, new AnnotatedService(service, method,
                requestConverterFunctions, responseConverterFunctions, exceptionHandlerFunctions)))
                .collect(toImmutableList());
    }

    /**
     * Mapping from HTTP method annotation to {@link HttpMethod}, like following.
     * <ul>
     *   <li>{@link Options} -> {@link HttpMethod#OPTIONS}
     *   <li>{@link Get} -> {@link HttpMethod#GET}
     *   <li>{@link Head} -> {@link HttpMethod#HEAD}
     *   <li>{@link Post} -> {@link HttpMethod#POST}
     *   <li>{@link Put} -> {@link HttpMethod#PUT}
     *   <li>{@link Patch} -> {@link HttpMethod#PATCH}
     *   <li>{@link Delete} -> {@link HttpMethod#DELETE}
     *   <li>{@link Trace} -> {@link HttpMethod#TRACE}
     * </ul>
     */
    private static final Map<Class<?>, HttpMethod> HTTP_METHOD_MAP =
            ImmutableMap.<Class<?>, HttpMethod>builder()
                    .put(Options.class, HttpMethod.OPTIONS)
                    .put(Get.class, HttpMethod.GET)
                    .put(Head.class, HttpMethod.HEAD)
                    .put(Post.class, HttpMethod.POST)
                    .put(Put.class, HttpMethod.PUT)
                    .put(Patch.class, HttpMethod.PATCH)
                    .put(Delete.class, HttpMethod.DELETE)
                    .put(Trace.class, HttpMethod.TRACE)
                    .build();

    /**
     * Returns the list of {@link Path} annotated methods.
     */
    @SuppressWarnings("unchecked")
    private static List<Method> requestMappingMethods(Object object) {
        return getAllMethods(object.getClass(), withModifier(Modifier.PUBLIC))
                .stream()
                // Lookup super classes just in case if the object is a proxy.
                .filter(m -> AnnotationUtil.getAnnotations(m, FindOption.LOOKUP_SUPER_CLASSES)
                        .stream()
                        .map(Annotation::annotationType)
                        .anyMatch(annotationType -> annotationType.isAssignableFrom(Path.class) ||
                                HTTP_METHOD_MAP.containsKey(annotationType)))
                .sorted(Comparator.comparingInt(ServerBuilder::order))
                .collect(toImmutableList());
    }

    /**
     * Returns the value of the order of the {@link Method}. The order could be retrieved from {@link Order}
     * annotation. 0 would be returned if there is no specified {@link Order} annotation.
     */
    private static int order(Method method) {
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
        return AnnotationUtil.getAnnotations(method, FindOption.LOOKUP_SUPER_CLASSES)
                .stream()
                .filter(annotation -> HTTP_METHOD_MAP.containsKey(annotation.annotationType()) ||
                        annotation.annotationType().isAssignableFrom(Path.class))
                .collect(Collectors.toSet());
    }

    private <T extends Annotation, R> ImmutableSet<R> getAnnotatedInstances(
            AnnotatedElement method, AnnotatedElement clazz, Class<T> annotationType, Class<R> resultType) {
        final ImmutableSet.Builder<R> builder = ImmutableSet.builder();
        Stream.concat(AnnotationUtil.findAll(method, annotationType).stream(),
                AnnotationUtil.findAll(clazz, annotationType).stream())
                .forEach(annotation -> builder.add(getInstance(annotation, resultType)));
        return builder.build();
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
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    /**
     * Returns a list of predicates which will be used to evaluate whether a request can be accepted
     * by a service method.
     */
    private static <T extends Annotation> List<String> predicates(Method method, Class<?> clazz,
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
                ImmutableMap.toImmutableMap(
                        Map.Entry::getKey,
                        entry -> {
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
        methodAnnotations.stream()
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

    private static Object invokeAnnotationMethod(Annotation a, String invokeName) {
        try {
            @SuppressWarnings("unchecked") final Method method = Iterables.getFirst(
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
    private static Object invokeValueMethod(Annotation a) {
        return invokeAnnotationMethod(a, "value");
    }

    public ServerBuilder stopQuietPeriod(Duration stopQuietPeriod) {
        this.stopQuietPeriod = stopQuietPeriod;
        return this;
    }

    public ServerBuilder stopTimeout(Duration stopTimeout) {
        this.stopTimeout = stopTimeout;
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

    public <T> ServerBuilder channelOption(ChannelOption<T> option, T value) {
        requireNonNull(option, "option");
        checkArgument(!PROHIBITED_SOCKET_OPTIONS.contains(option),
                "prohibited socket option: %s", option);

        option.validate(value);
        channelOptions.put(option, value);
        return this;
    }

    public <T> ServerBuilder childChannelOption(ChannelOption<T> option, T value) {
        requireNonNull(option, "option");
        checkArgument(!PROHIBITED_SOCKET_OPTIONS.contains(option),
                "prohibited socket option: %s", option);

        option.validate(value);
        childChannelOptions.put(option, value);
        return this;
    }

    public ServerBuilder tls(String sslCert, String sslPrivateKey, String sslPrivateKeyPass) {
        checkArgument(!Strings.isNullOrEmpty(sslCert), "sslCert can't be null");
        checkArgument(!Strings.isNullOrEmpty(sslPrivateKey), "sslPrivateKey can't be null");
        checkArgument(!Strings.isNullOrEmpty(sslPrivateKeyPass), "sslPrivateKeyPass can't be null");
        this.sslCert = requireNonNull(sslCert);
        this.sslPrivateKey = requireNonNull(sslPrivateKey);
        this.sslPrivateKeyPass = requireNonNull(sslPrivateKeyPass);
        return this;
    }

    public Server build() {
        return build(new String[0]);
    }

    public Server build(String[] args) {
        return build(findMainCaller(), args);
    }

    private Class<?> findMainCaller() {
        final Thread currentThread = Thread.currentThread();
        final Optional<? extends Class<?>> classOptional = Arrays.stream(currentThread
                .getStackTrace())
                .filter(st -> "main".equals(st.getMethodName()))
                .findFirst()
                .map(StackTraceElement::getClassName)
                .map(UncheckedFnKit.function(Class::forName));
        return classOptional.orElse(null);
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
    public Server build(Class<?> bootCls, String[] args) {
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

        return new Server(new ServerConfig(this.serviceWraps, this.meterRegistry, bootCls, args, this.banner,
                this.channelOptions, this.childChannelOptions, this.useSsl, this.useEpoll, this.startStopExecutor,
                this.bannerText, this.bannerFont, this.sessionKey,
                this.viewSuffix, this.templateFolder, this.serverThreadName, this.profiles, this.useSession,
                this.ports, this.maxNumConnections, this.http2InitialConnectionWindowSize,
                this.http2InitialStreamWindowSize, this.http2MaxFrameSize, this.http1MaxInitialLineLength,
                this.http1MaxHeaderSize, this.http1MaxChunkSize, this.idleTimeoutMillis, this.pingIntervalMillis,
                this.maxConnectionAgeMillis, this.http2MaxHeaderListSize, this.http2MaxStreamsPerConnection,
                this.acceptThreadCount, this.ioThreadCount, this.serverRestartCount, this.sslCert, this.sslPrivateKey,
                this.sslPrivateKeyPass, stopQuietPeriod, stopTimeout), null);
    }
}
