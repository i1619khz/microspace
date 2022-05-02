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
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * @author i1619kHz
 */
public final class RouteBuilder {
    private final List<RoutePredicate<QueryParams>> paramPredicates = new ArrayList<>();
    private final List<RoutePredicate<HttpHeaders>> headerPredicates = new ArrayList<>();
    private String pathPattern;
    private PathMapping pathMapping;
    private HttpStatus statusCode;
    private Set<HttpMethod> methods = ImmutableSet.of();
    private Set<MediaType> consumes = ImmutableSet.of();
    private Set<MediaType> produces = ImmutableSet.of();

    public RouteBuilder pathPattern(String pathPattern) {
        checkArgument(!Strings.isNullOrEmpty(pathPattern), "pathPrefix");
        this.pathPattern = pathPattern;
        pathPrefix(this.pathPattern);
        return this;
    }

    public RouteBuilder pathPrefix(String pathPrefix) {
        checkArgument(!Strings.isNullOrEmpty(pathPrefix), "pathPrefix");
        this.pathMapping = new ExactPathMapping();
        return this;
    }

    public RouteBuilder methods(HttpMethod... httpMethods) {
        methods(ImmutableSet.copyOf(requireNonNull(httpMethods, "httpMethods")));
        return this;
    }

    public RouteBuilder methods(Iterable<HttpMethod> httpMethods) {
        this.methods = ImmutableSet.copyOf(requireNonNull(httpMethods, "methods"));
        return this;
    }

    public RouteBuilder consumes(MediaType... consumes) {
        consumes(ImmutableSet.copyOf(requireNonNull(consumes, "consumes")));
        return this;
    }

    public RouteBuilder consumes(Iterable<MediaType> consumes) {
        checkArgument(null != consumes, "consumes");
        this.consumes = ImmutableSet.copyOf(requireNonNull(consumes));
        return this;
    }

    public RouteBuilder produces(MediaType... produces) {
        produces(ImmutableSet.copyOf(requireNonNull(produces, "produces")));
        return this;
    }

    public RouteBuilder produces(Iterable<MediaType> produces) {
        checkArgument(null != produces, "produces");
        this.produces = ImmutableSet.copyOf(requireNonNull(produces));
        return this;
    }

    public RouteBuilder matchesParams(String... paramPredicates) {
        return matchesParams(ImmutableList.copyOf(requireNonNull(paramPredicates, "paramPredicates")));
    }

    public RouteBuilder matchesParams(Iterable<String> paramPredicates) {
        this.paramPredicates.addAll(RoutePredicate.copyOfParamPredicates(
                requireNonNull(paramPredicates, "paramPredicates")));
        return this;
    }

    public RouteBuilder matchesParams(String paramName, Predicate<? super String> valuePredicate) {
        requireNonNull(paramName, "paramName");
        requireNonNull(valuePredicate, "valuePredicate");
        paramPredicates.add(RoutePredicate.ofParams(paramName, valuePredicate));
        return this;
    }

    public RouteBuilder matchesParams(List<RoutePredicate<QueryParams>> paramPredicates) {
        this.paramPredicates.addAll(requireNonNull(paramPredicates, "paramPredicates"));
        return this;
    }

    public RouteBuilder matchesHeaders(String... headerPredicates) {
        return matchesHeaders(ImmutableList.copyOf(requireNonNull(headerPredicates, "headerPredicates")));
    }

    public RouteBuilder matchesHeaders(Iterable<String> headerPredicates) {
        this.headerPredicates.addAll(RoutePredicate.copyOfHeaderPredicates(
                requireNonNull(headerPredicates, "headerPredicates")));
        return this;
    }

    public RouteBuilder matchesHeaders(CharSequence headerName, Predicate<? super String> valuePredicate) {
        requireNonNull(headerName, "headerName");
        requireNonNull(valuePredicate, "valuePredicate");
        headerPredicates.add(RoutePredicate.ofHeaders(headerName, valuePredicate));
        return this;
    }

    public RouteBuilder matchesHeaders(List<RoutePredicate<HttpHeaders>> headerPredicates) {
        this.headerPredicates.addAll(requireNonNull(headerPredicates, "headerPredicates"));
        return this;
    }

    public RouteBuilder statusCode(HttpStatus httpStatus) {
        this.statusCode = httpStatus;
        return this;
    }

    public Route build() {
        checkState(pathMapping != null, "Must set a path before calling this.");
        if ((!consumes.isEmpty() || !produces.isEmpty()) && methods.isEmpty()) {
            throw new IllegalStateException("Must set methods if consumes or produces is not empty." +
                                            " consumes: " + consumes + ", produces: " + produces);
        }
        final Set<HttpMethod> pathMethods = methods.isEmpty() ? HttpMethod.knownMethods() : methods;
        return new DefaultRoute(pathPattern, pathMapping, pathMethods, consumes,
                                produces, paramPredicates, headerPredicates, statusCode);
    }
}
