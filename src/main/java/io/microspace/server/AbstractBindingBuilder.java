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
import static io.microspace.server.HttpMethod.CONNECT;
import static io.microspace.server.HttpMethod.DELETE;
import static io.microspace.server.HttpMethod.GET;
import static io.microspace.server.HttpMethod.HEAD;
import static io.microspace.server.HttpMethod.OPTIONS;
import static io.microspace.server.HttpMethod.PATCH;
import static io.microspace.server.HttpMethod.POST;
import static io.microspace.server.HttpMethod.PUT;
import static io.microspace.server.HttpMethod.TRACE;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 * An abstract builder class for binding something to a {@link Route} fluently.
 */
abstract class AbstractBindingBuilder extends AbstractServiceConfigSetters {

    private final List<RoutingPredicate<QueryParams>> paramPredicates = new ArrayList<>();
    private final List<RoutingPredicate<HttpHeaders>> headerPredicates = new ArrayList<>();
    private final Map<RouteBuilder, Set<HttpMethod>> routeBuilders = new LinkedHashMap<>();
    private final Set<RouteBuilder> pathBuilders = new LinkedHashSet<>();
    private final List<Route> additionalRoutes = new ArrayList<>();
    private Set<HttpMethod> methods = ImmutableSet.of();
    private Set<MediaType> consumeTypes = ImmutableSet.of();
    private Set<MediaType> produceTypes = ImmutableSet.of();

    static void ensureUniqueMediaTypes(Iterable<MediaType> types, String typeName) {
        requireNonNull(types, typeName);
        final Set<MediaType> set = new HashSet<>();
        for (final MediaType type : types) {
            if (!set.add(type)) {
                throw new IllegalArgumentException(
                        "duplicated media type in " + typeName + ": " + type);
            }
        }
    }

    /**
     * Sets the path pattern that an {@link HttpService} will be bound to.
     * Please refer to the <a href="https://armeria.dev/docs/server-basics#path-patterns">Path patterns</a>
     * in order to learn how to specify a path pattern.
     *
     * @throws IllegalArgumentException if the specified path pattern is invalid
     */
    public AbstractBindingBuilder path(String pathPattern) {
        pathBuilders.add(Route.builder().path(requireNonNull(pathPattern, "pathPattern"), pathPattern));
        return this;
    }

    /**
     * Sets the specified prefix which is a directory that an {@link HttpService} will be bound under.
     * {@code pathPrefix("/my/path")} is identical to {@code path("prefix:/my/path")}.
     *
     * @throws IllegalArgumentException if the specified path pattern is invalid
     */
    public AbstractBindingBuilder pathPrefix(String prefix) {
        pathBuilders.add(Route.builder().path(requireNonNull(prefix, "prefix"), prefix));
        return this;
    }

    /**
     * Sets the path pattern that an {@link HttpService} will be bound to, only supporting
     * {@link HttpMethod#GET} requests.
     * Please refer to the <a href="https://armeria.dev/docs/server-basics#path-patterns">Path patterns</a>
     * in order to learn how to specify a path pattern.
     *
     * @throws IllegalArgumentException if the specified path pattern is invalid
     */
    public AbstractBindingBuilder get(String pathPattern) {
        addRouteBuilder(pathPattern, GET);
        return this;
    }

    /**
     * Sets the path pattern that an {@link HttpService} will be bound to, only supporting
     * {@link HttpMethod#POST} requests.
     * Please refer to the <a href="https://armeria.dev/docs/server-basics#path-patterns">Path patterns</a>
     * in order to learn how to specify a path pattern.
     *
     * @throws IllegalArgumentException if the specified path pattern is invalid
     */
    public AbstractBindingBuilder post(String pathPattern) {
        addRouteBuilder(pathPattern, POST);
        return this;
    }

    /**
     * Sets the path pattern that an {@link HttpService} will be bound to, only supporting
     * {@link HttpMethod#PUT} requests.
     * Please refer to the <a href="https://armeria.dev/docs/server-basics#path-patterns">Path patterns</a>
     * in order to learn how to specify a path pattern.
     *
     * @throws IllegalArgumentException if the specified path pattern is invalid
     */
    public AbstractBindingBuilder put(String pathPattern) {
        addRouteBuilder(pathPattern, PUT);
        return this;
    }

    /**
     * Sets the path pattern that an {@link HttpService} will be bound to, only supporting
     * {@link HttpMethod#PATCH} requests.
     * Please refer to the <a href="https://armeria.dev/docs/server-basics#path-patterns">Path patterns</a>
     * in order to learn how to specify a path pattern.
     *
     * @throws IllegalArgumentException if the specified path pattern is invalid
     */
    public AbstractBindingBuilder patch(String pathPattern) {
        addRouteBuilder(pathPattern, PATCH);
        return this;
    }

    /**
     * Sets the path pattern that an {@link HttpService} will be bound to, only supporting
     * {@link HttpMethod#DELETE} requests.
     * Please refer to the <a href="https://armeria.dev/docs/server-basics#path-patterns">Path patterns</a>
     * in order to learn how to specify a path pattern.
     *
     * @throws IllegalArgumentException if the specified path pattern is invalid
     */
    public AbstractBindingBuilder delete(String pathPattern) {
        addRouteBuilder(pathPattern, DELETE);
        return this;
    }

    /**
     * Sets the path pattern that an {@link HttpService} will be bound to, only supporting
     * {@link HttpMethod#OPTIONS} requests.
     * Please refer to the <a href="https://armeria.dev/docs/server-basics#path-patterns">Path patterns</a>
     * in order to learn how to specify a path pattern.
     *
     * @throws IllegalArgumentException if the specified path pattern is invalid
     */
    public AbstractBindingBuilder options(String pathPattern) {
        addRouteBuilder(pathPattern, OPTIONS);
        return this;
    }

    /**
     * Sets the path pattern that an {@link HttpService} will be bound to, only supporting
     * {@link HttpMethod#HEAD} requests.
     * Please refer to the <a href="https://armeria.dev/docs/server-basics#path-patterns">Path patterns</a>
     * in order to learn how to specify a path pattern.
     *
     * @throws IllegalArgumentException if the specified path pattern is invalid
     */
    public AbstractBindingBuilder head(String pathPattern) {
        addRouteBuilder(pathPattern, HEAD);
        return this;
    }

    /**
     * Sets the path pattern that an {@link HttpService} will be bound to, only supporting
     * {@link HttpMethod#TRACE} requests.
     * Please refer to the <a href="https://armeria.dev/docs/server-basics#path-patterns">Path patterns</a>
     * in order to learn how to specify a path pattern.
     *
     * @throws IllegalArgumentException if the specified path pattern is invalid
     */
    public AbstractBindingBuilder trace(String pathPattern) {
        addRouteBuilder(pathPattern, TRACE);
        return this;
    }

    /**
     * Sets the path pattern that an {@link HttpService} will be bound to, only supporting
     * {@link HttpMethod#CONNECT} requests.
     * Please refer to the <a href="https://armeria.dev/docs/server-basics#path-patterns">Path patterns</a>
     * in order to learn how to specify a path pattern.
     *
     * @throws IllegalArgumentException if the specified path pattern is invalid
     */
    public AbstractBindingBuilder connect(String pathPattern) {
        addRouteBuilder(pathPattern, CONNECT);
        return this;
    }

    private void addRouteBuilder(String pathPattern, HttpMethod method) {
        addRouteBuilder(Route.builder().path(requireNonNull(pathPattern, "pathPattern"), pathPattern),
                        EnumSet.of(method));
    }

    private void addRouteBuilder(RouteBuilder routeBuilder, Set<HttpMethod> methods) {
        final Set<HttpMethod> methodSet = routeBuilders.computeIfAbsent(
                routeBuilder, key -> EnumSet.noneOf(HttpMethod.class));

        for (HttpMethod method : methods) {
            if (!methodSet.add(method)) {
                throw new IllegalArgumentException("duplicate HTTP method: " + method +
                                                   ", for: " + routeBuilder);
            }
        }
    }

    /**
     * Sets the {@link HttpMethod}s that an {@link HttpService} will support. If not set,
     * {@link HttpMethod#knownMethods()}s are set.
     *
     * @see #path(String)
     * @see #pathPrefix(String)
     */
    public AbstractBindingBuilder methods(HttpMethod... methods) {
        return methods(ImmutableSet.copyOf(requireNonNull(methods, "methods")));
    }

    /**
     * Sets the {@link HttpMethod}s that an {@link HttpService} will support. If not set,
     * {@link HttpMethod#knownMethods()}s are set.
     *
     * @see #path(String)
     * @see #pathPrefix(String)
     */
    public AbstractBindingBuilder methods(Iterable<HttpMethod> methods) {
        requireNonNull(methods, "methods");
        checkArgument(!Iterables.isEmpty(methods), "methods can't be empty.");
        this.methods = Sets.immutableEnumSet(methods);
        return this;
    }

    /**
     * Sets {@link MediaType}s that an {@link HttpService} will consume. If not set, the {@link HttpService}
     * will accept all media types.
     */
    public AbstractBindingBuilder consumes(MediaType... consumeTypes) {
        consumes(ImmutableSet.copyOf(requireNonNull(consumeTypes, "consumeTypes")));
        return this;
    }

    /**
     * Sets {@link MediaType}s that an {@link HttpService} will consume. If not set, the {@link HttpService}
     * will accept all media types.
     */
    public AbstractBindingBuilder consumes(Iterable<MediaType> consumeTypes) {
        ensureUniqueMediaTypes(consumeTypes, "consumeTypes");
        this.consumeTypes = ImmutableSet.copyOf(consumeTypes);
        return this;
    }

    /**
     * Sets {@link MediaType}s that an {@link HttpService} will produce to be used in
     * content negotiation. See <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">Accept header</a>
     * for more information.
     */
    public AbstractBindingBuilder produces(MediaType... produceTypes) {
        produces(ImmutableSet.copyOf(requireNonNull(produceTypes, "produceTypes")));
        return this;
    }

    /**
     * Sets {@link MediaType}s that an {@link HttpService} will produce to be used in
     * content negotiation. See <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">Accept header</a>
     * for more information.
     */
    public AbstractBindingBuilder produces(Iterable<MediaType> produceTypes) {
        ensureUniqueMediaTypes(produceTypes, "produceTypes");
        this.produceTypes = ImmutableSet.copyOf(produceTypes);
        return this;
    }

    /**
     * Sets the {@link Route} to accept a request if it matches all the specified predicates for
     * HTTP parameters. The predicate can be one of the following forms:
     * <ul>
     *     <li>{@code some-param=some-value} which means that the request must have a
     *     {@code some-param=some-value} parameter</li>
     *     <li>{@code some-param!=some-value} which means that the request must not have a
     *     {@code some-param=some-value} parameter</li>
     *     <li>{@code some-param} which means that the request must contain a {@code some-param} parameter</li>
     *     <li>{@code !some-param} which means that the request must not contain a {@code some-param}
     *     parameter</li>
     * </ul>
     *
     * <p>Note that these predicates can be evaluated only with the query string of the request URI.
     * Also note that each predicate will be evaluated with the decoded value of HTTP parameters,
     * so do not use percent-encoded value in the predicate.
     *
     * @see io.microspace.server.annotation.MatchesParam
     */
    public AbstractBindingBuilder matchesParams(String... paramPredicates) {
        return matchesParams(ImmutableList.copyOf(requireNonNull(paramPredicates, "paramPredicates")));
    }

    /**
     * Sets the {@link Route} to accept a request if it matches all the specified predicates for
     * HTTP parameters. The predicate can be one of the following forms:
     * <ul>
     *     <li>{@code some-param=some-value} which means that the request must have a
     *     {@code some-param=some-value} parameter</li>
     *     <li>{@code some-param!=some-value} which means that the request must not have a
     *     {@code some-param=some-value} parameter</li>
     *     <li>{@code some-param} which means that the request must contain a {@code some-param} parameter</li>
     *     <li>{@code !some-param} which means that the request must not contain a {@code some-param}
     *     parameter</li>
     * </ul>
     *
     * <p>Note that these predicates can be evaluated only with the query string of the request URI.
     * Also note that each predicate will be evaluated with the decoded value of HTTP parameters,
     * so do not use percent-encoded value in the predicate.
     *
     * @see io.microspace.server.annotation.MatchesParam
     */
    public AbstractBindingBuilder matchesParams(Iterable<String> paramPredicates) {
        this.paramPredicates.addAll(RoutingPredicate.copyOfParamPredicates(
                requireNonNull(paramPredicates, "paramPredicates")));
        return this;
    }

    /**
     * Sets the {@link Route} to accept a request when the specified {@code valuePredicate} evaluates
     * {@code true} with the value of the specified {@code paramName} parameter.
     */
    public AbstractBindingBuilder matchesParams(String paramName, Predicate<? super String> valuePredicate) {
        requireNonNull(paramName, "paramName");
        requireNonNull(valuePredicate, "valuePredicate");
        paramPredicates.add(RoutingPredicate.ofParams(paramName, valuePredicate));
        return this;
    }

    /**
     * Sets the {@link Route} to accept a request if it matches all the specified predicates for
     * {@link HttpHeaders}. The predicate can be one of the following forms:
     * <ul>
     *     <li>{@code some-header=some-value} which means that the request must have a
     *     {@code some-header: some-value} header</li>
     *     <li>{@code some-header!=some-value} which means that the request must not have a
     *     {@code some-header: some-value} header</li>
     *     <li>{@code some-header} which means that the request must contain a {@code some-header} header</li>
     *     <li>{@code !some-header} which means that the request must not contain a {@code some-header}
     *     header</li>
     * </ul>
     *
     * @see io.microspace.server.annotation.MatchesHeader
     */
    public AbstractBindingBuilder matchesHeaders(String... headerPredicates) {
        return matchesHeaders(ImmutableList.copyOf(requireNonNull(headerPredicates, "headerPredicates")));
    }

    /**
     * Sets the {@link Route} to accept a request if it matches all the specified predicates for
     * {@link HttpHeaders}. The predicate can be one of the following forms:
     * <ul>
     *     <li>{@code some-header=some-value} which means that the request must have a
     *     {@code some-header: some-value} header</li>
     *     <li>{@code some-header!=some-value} which means that the request must not have a
     *     {@code some-header: some-value} an header</li>
     *     <li>{@code some-header} which means that the request must contain a {@code some-header} header</li>
     *     <li>{@code !some-header} which means that the request must not contain a {@code some-header}
     *     header</li>
     * </ul>
     *
     * @see io.microspace.server.annotation.MatchesHeader
     */
    public AbstractBindingBuilder matchesHeaders(Iterable<String> headerPredicates) {
        this.headerPredicates.addAll(RoutingPredicate.copyOfHeaderPredicates(
                requireNonNull(headerPredicates, "headerPredicates")));
        return this;
    }

    /**
     * Sets the {@link Route} to accept a request when the specified {@code valuePredicate} evaluates
     * {@code true} with the value of the specified {@code headerName} header.
     */
    public AbstractBindingBuilder matchesHeaders(CharSequence headerName,
                                                 Predicate<? super String> valuePredicate) {
        requireNonNull(headerName, "headerName");
        requireNonNull(valuePredicate, "valuePredicate");
        headerPredicates.add(RoutingPredicate.ofHeaders(headerName, valuePredicate));
        return this;
    }

    /**
     * Specifies an additional {@link Route} that should be matched.
     */
    public AbstractBindingBuilder addRoute(Route route) {
        additionalRoutes.add(requireNonNull(route, "route"));
        return this;
    }

    /**
     * Returns a newly-created {@link Route}s based on the properties of this builder.
     *
     * @param fallbackRoutes the {@link Route}s to use when a user did not specify any {@link Route}s.
     */
    final List<Route> buildRouteList() {
        final ImmutableList.Builder<Route> builder = ImmutableList.builder();

        if (additionalRoutes.isEmpty()) {
            if (pathBuilders.isEmpty() && !methods.isEmpty()) {
                throw new IllegalStateException("Should set a path when the methods are set: " + methods);
            }
        }

        if (!pathBuilders.isEmpty()) {
            final Set<HttpMethod> pathMethods = methods.isEmpty() ? HttpMethod.knownMethods() : methods;
            pathBuilders.forEach(pathBuilder -> addRouteBuilder(pathBuilder, pathMethods));
        }

        routeBuilders.forEach((routeBuilder, routeMethods) ->
                                      builder.add(routeBuilder.methods(routeMethods)
                                                              .statusCode(HttpStatus.OK)
                                                              .consumes(consumeTypes)
                                                              .produces(produceTypes)
                                                              .matchesParams(paramPredicates)
                                                              .matchesHeaders(headerPredicates)
                                                              .build()));
        additionalRoutes.forEach(builder::add);
        return builder.build();
    }
}

