package io.microspace.server;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static io.microspace.internal.AnnotationUtil.FindOption.LOOKUP_SUPER_CLASSES;
import static io.microspace.internal.AnnotationUtil.findFirst;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;

import io.microspace.internal.AnnotationUtil;
import io.microspace.internal.DefaultValues;
import io.microspace.server.annotation.AdditionalHeader;
import io.microspace.server.annotation.AdditionalTrailer;
import io.microspace.server.annotation.Blocking;
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

/**
 * @author i1619kHz
 */
public class AnnotatedServiceFactory {
    private static final Logger log = LoggerFactory.getLogger(ServerBuilder.class);

    private static final Map<Class<?>, HttpMethod> HTTP_METHOD_MAP;
    private static final Map<String, Set<String>> addedHeaders = new ConcurrentHashMap<>();

    static {
        Builder<Class<?>, HttpMethod> builder = ImmutableMap.builder();
        builder.put(Options.class, HttpMethod.OPTIONS);
        builder.put(Get.class, HttpMethod.GET);
        builder.put(Head.class, HttpMethod.HEAD);
        builder.put(Post.class, HttpMethod.POST);
        builder.put(Put.class, HttpMethod.PUT);
        builder.put(Patch.class, HttpMethod.PATCH);
        builder.put(Delete.class, HttpMethod.DELETE);
        builder.put(Trace.class, HttpMethod.TRACE);
        HTTP_METHOD_MAP = builder.build();
    }

    /**
     * Returns the list of {@link AnnotatedService} defined by {@link Path} and HTTP method annotations
     * from the specified {@code object}, {@link RequestConverterFunction}s, {@link ResponseConverterFunction}s,
     * {@link ExceptionHandlerFunction}s and {@link AnnotatedServiceExtensions}.
     */
    public static List<AnnotatedServiceElement> find(String pathPrefix, Object object,
                                                     boolean useBlockingTaskExecutor,
                                                     List<RequestConverterFunction> requestConverterFunctions,
                                                     List<ResponseConverterFunction> responseConverterFunctions,
                                                     List<ExceptionHandlerFunction> exceptionHandlerFunctions) {
        final List<Method> requestMappingMethods = requestMappingMethods(object);
        return requestMappingMethods.stream().flatMap((method) -> requireNonNull(
                                            create(pathPrefix, object, method,
                                                   useBlockingTaskExecutor,
                                                   requestConverterFunctions,
                                                   responseConverterFunctions,
                                                   exceptionHandlerFunctions))
                                            .stream())
                                    .collect(ImmutableList.toImmutableList());

    }

    public static List<AnnotatedServiceElement> create(String prefix, Object service, Method method,
                                                       boolean useBlockingTaskExecutor,
                                                       List<RequestConverterFunction> baseRequestConverterFunctions,
                                                       List<ResponseConverterFunction> baseResponseConverterFunctions,
                                                       List<ExceptionHandlerFunction> baseExceptionHandlerFunctions) {
        final Set<Annotation> methodAnnotations = httpMethodAnnotations(method);
        if (methodAnnotations.isEmpty()) {
            throw new IllegalArgumentException("HTTP Method specification is missing: " + method.getName());
        }

        final Class<?> clazz = service.getClass();
        final Map<HttpMethod, List<String>> methodListMap = getHttpMethodPatternsMap(method, methodAnnotations);
        final HttpStatus statusCode = statusCode(method);
        final String computedPathPrefix = computePathPrefix(clazz, prefix);
        final Set<MediaType> consumableMediaTypes = consumableMediaTypes(method, clazz);
        final Set<MediaType> producibleMediaTypes = producibleMediaTypes(method, clazz);
        final List<Route> routes = new ArrayList<>();

        methodListMap.forEach((httpMethod, pathMappings) -> {
            for (String pathMapping : pathMappings) {
                List<String> matchesParamPredicates =
                        predicates(method, clazz, MatchesParam.class, MatchesParam::value);

                List<String> matchesHeaderPredicates =
                        predicates(method, clazz, MatchesHeader.class, MatchesHeader::value);
                routes.add(Route.builder().pathPattern(pathMapping)
                                .pathPrefix(computedPathPrefix)
                                .methods(httpMethod).consumes(consumableMediaTypes)
                                .produces(producibleMediaTypes).matchesParams(matchesParamPredicates)
                                .matchesHeaders(matchesHeaderPredicates).statusCode(statusCode)
                                .build());
            }
        });

        final List<RequestConverterFunction> requestConverterFunctions =
                getAnnotatedInstances(method, clazz, RequestConverter.class,
                                      RequestConverterFunction.class)
                        .addAll(baseRequestConverterFunctions)
                        .build();

        final List<ResponseConverterFunction> responseConverterFunctions =
                getAnnotatedInstances(method, clazz, ResponseConverter.class,
                                      ResponseConverterFunction.class)
                        .addAll(baseResponseConverterFunctions)
                        .build();

        final List<ExceptionHandlerFunction> exceptionHandlerFunctions =
                getAnnotatedInstances(method, clazz, RouteExceptionHandler.class,
                                      ExceptionHandlerFunction.class)
                        .addAll(baseExceptionHandlerFunctions)
                        .build();

        final String classAlias = clazz.getName();
        final String methodAlias = String.format("%s.%s()", classAlias, method.getName());
        setAdditionalHeader(clazz, "header", classAlias, "class",
                            AdditionalHeader.class, AdditionalHeader::name, AdditionalHeader::value);

        setAdditionalHeader(method, "header", methodAlias, "method",
                            AdditionalHeader.class, AdditionalHeader::name, AdditionalHeader::value);

        setAdditionalHeader(clazz, "trailer", classAlias, "class",
                            AdditionalTrailer.class, AdditionalTrailer::name, AdditionalTrailer::value);

        setAdditionalHeader(method, "trailer", methodAlias, "method",
                            AdditionalTrailer.class, AdditionalTrailer::name, AdditionalTrailer::value);

        final boolean needToUseBlockingTaskExecutor = useBlockingTaskExecutor || findFirst(
                method, Blocking.class) != null || findFirst(service.getClass(), Blocking.class) != null;

        return routes.stream().map(route -> new AnnotatedServiceElement(
                             route, new AnnotatedService(
                             service, method, needToUseBlockingTaskExecutor,
                             addedHeaders, requestConverterFunctions,
                             responseConverterFunctions, exceptionHandlerFunctions)))
                     .collect(toImmutableList());
    }

    private static List<Method> getMethodsByPredicate(Object service,
                                                      Predicate<? super Class<? extends Annotation>> predicate) {
        // Lookup super classes just in case if the object is a proxy.
        return getAllMethods(service.getClass(), withModifier(Modifier.PUBLIC))
                .stream()
                .filter(m -> AnnotationUtil.getAnnotations(m, LOOKUP_SUPER_CLASSES)
                                           .stream()
                                           .map(Annotation::annotationType)
                                           .anyMatch(predicate))
                .sorted(Comparator.comparingInt(AnnotatedServiceFactory::order))
                .collect(toImmutableList());
    }

    /**
     * Returns the list of {@link ExceptionHandler} annotated methods.
     */
    private static List<Method> exceptionHandlerMethods(Object service) {
        return getMethodsByPredicate(service, type -> type.isAssignableFrom(ExceptionHandler.class));
    }

    /**
     * Returns the list of {@link Path} annotated methods.
     */
    private static List<Method> requestMappingMethods(Object object) {
        return getMethodsByPredicate(object, type -> type.isAssignableFrom(Path.class) ||
                                                     HTTP_METHOD_MAP.containsKey(type));
    }

    /**
     * Returns the value of the order of the {@link Method}. The order could be retrieved from {@link Order}
     * annotation. 0 would be returned if there is no specified {@link Order} annotation.
     */
    private static int order(Method method) {
        final Order order = findFirst(method, Order.class);
        return order != null ? order.value() : 0;
    }

    private static Set<Annotation> httpMethodAnnotations(Method method) {
        return AnnotationUtil.getAnnotations(method, LOOKUP_SUPER_CLASSES)
                             .stream()
                             .filter(AnnotatedServiceFactory::containsAnnotationType)
                             .collect(Collectors.toSet());
    }

    private static boolean containsAnnotationType(Annotation annotation) {
        return HTTP_METHOD_MAP.containsKey(annotation.annotationType()) ||
               annotation.annotationType().isAssignableFrom(Path.class);
    }

    private static <T extends Annotation> void setAdditionalHeader(AnnotatedElement element,
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

    private static <T extends Annotation, R> ImmutableList.Builder<R> getAnnotatedInstances(
            AnnotatedElement method, AnnotatedElement clazz, Class<T> annotationType, Class<R> resultType) {
        final ImmutableList.Builder<R> builder = ImmutableList.builder();
        Stream.concat(AnnotationUtil.findAll(method, annotationType).stream(),
                      AnnotationUtil.findAll(clazz, annotationType).stream())
              .forEach(annotation -> builder.add(getInstance(annotation, resultType)));
        return builder;
    }

    private static <T> T getInstance(Constructor<?> constructor, Class<T> expectedType) {
        try {
            constructor.setAccessible(true);
            return expectedType.cast(constructor.newInstance());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "A class specified in " + constructor.getName() +
                    " annotation cannot be cast to " + expectedType, e);
        }
    }

    private static <T> T getInstance(Annotation annotation, Class<T> expectedType) {
        try {
            final Object instance = getInstance(annotation);
            return expectedType.cast(instance);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "A class specified in @" + annotation.annotationType().getSimpleName() +
                    " annotation cannot be cast to " + expectedType, e);
        }
    }

    private static <T> T getInstance(Annotation annotation) throws Exception {
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
    private static <T extends Annotation> List<String> predicates(Method method, Class<?> clazz,
                                                                  Class<T> annotationType,
                                                                  Function<T, String> toStringPredicate) {
        final List<T> classLevel = AnnotationUtil.findAll(clazz, annotationType);
        final List<T> methodLevel = AnnotationUtil.findAll(method, annotationType);
        return Streams.concat(classLevel.stream(), methodLevel.stream())
                      .map(toStringPredicate).collect(toImmutableList());
    }

    private static HttpStatus statusCode(Method method) {
        final StatusCode statusCodeAnnotation = findFirst(method, StatusCode.class);
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

    private static String computePathPrefix(Class<?> clazz, String pathPrefix) {
        ensureAbsolutePath(pathPrefix, "pathPrefix");
        final PathPrefix pathPrefixAnnotation = findFirst(clazz, PathPrefix.class);
        if (null == pathPrefixAnnotation) {
            return pathPrefix;
        }

        final String pathPrefixValue = pathPrefixAnnotation.value();
        ensureAbsolutePath(pathPrefixValue, "pathPrefixValue");
        if (pathPrefix.equals("/") && pathPrefixValue.equals("/")) {
            return pathPrefix;
        }
        return (pathPrefix + pathPrefixValue);
    }

    private static void ensureAbsolutePath(String path, String paramName) {
        checkArgument(!Strings.isNullOrEmpty(paramName), "paramName");
        if (Strings.isNullOrEmpty(path) || path.charAt(0) != '/') {
            throw new IllegalArgumentException(paramName + ": " + path +
                                               " (expected: an absolute path starting with '/')");
        }
    }

    private static Set<MediaType> consumableMediaTypes(Method method, Class<?> clazz) {
        List<Consumes> consumes = AnnotationUtil.findAll(method, Consumes.class);
        if (consumes.isEmpty()) {
            consumes = AnnotationUtil.findAll(clazz, Consumes.class);
        }
        return consumes.stream()
                       .map(Consumes::value)
                       .map(MediaType::parse)
                       .collect(toImmutableSet());
    }

    private static Set<MediaType> producibleMediaTypes(Method method, Class<?> clazz) {
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
    private static Map<HttpMethod, List<String>> getHttpMethodPatternsMap(Method method,
                                                                          Set<Annotation> methodAnnotations) {
        final Map<HttpMethod, List<String>> patternMap = getHttpMethodAnnotatedPatternMap(methodAnnotations);
        if (patternMap.isEmpty()) {
            final String declaringClassName = method.getDeclaringClass().getName();
            throw new IllegalArgumentException(declaringClassName + '#' +
                                               method.getName() + " must have an HTTP method annotation.");
        }
        patternMap.entrySet().forEach(entry -> {
            final List<String> httpMethodPaths = entry.getValue();
            // Add an empty value if HTTP method annotation value is empty or not specified.
            if (httpMethodPaths.isEmpty()) {
                httpMethodPaths.add("");
                entry.setValue(ImmutableList.copyOf(httpMethodPaths));
            }
        });
        return patternMap;
    }

    private static Map<HttpMethod, List<String>> getHttpMethodAnnotatedPatternMap(
            Set<Annotation> methodAnnotations) {
        final Map<HttpMethod, List<String>> httpMethodPatternMap = new EnumMap<>(HttpMethod.class);
        for (Annotation annotation : methodAnnotations) {
            if (!containsAnnotationType(annotation)) {
                continue;
            }
            HttpMethod httpMethod = HTTP_METHOD_MAP.get(annotation.annotationType());
            if (null == httpMethod) {
                if (annotation.annotationType().isAssignableFrom(Path.class)) {
                    final Path path = ((Path) annotation);
                    httpMethod = path.method();
                }
            }
            final String value = (String) invokeValueMethod(annotation);
            final List<String> patterns = httpMethodPatternMap
                    .computeIfAbsent(httpMethod, ignored -> new ArrayList<>());
            if (DefaultValues.isSpecified(value)) {
                patterns.add(value);
            }
        }
        return httpMethodPatternMap;
    }

    private static Object invokeAnnotationMethod(Annotation a, String invokeName) {
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
    private static Object invokeValueMethod(Annotation a) {
        return invokeAnnotationMethod(a, "value");
    }

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
}
