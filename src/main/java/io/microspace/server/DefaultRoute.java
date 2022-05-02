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
import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * @author i1619kHz
 */
final class DefaultRoute implements Route {
    private final String prefix;
    private final HttpStatus statusCode;
    private final PathMapping pathMapping;
    private final Set<HttpMethod> methods;
    private final Set<MediaType> consumes;
    private final Set<MediaType> produces;

    private final List<RoutePredicate<QueryParams>> paramPredicates;
    private final List<RoutePredicate<HttpHeaders>> headerPredicates;

    private final int hashCode;

    DefaultRoute(String prefix, PathMapping pathMapping,
                 Set<HttpMethod> methods,
                 Set<MediaType> consumes,
                 Set<MediaType> produces,
                 List<RoutePredicate<QueryParams>> paramPredicates,
                 List<RoutePredicate<HttpHeaders>> headerPredicates,
                 HttpStatus statusCode) {
        checkArgument(!Strings.isNullOrEmpty(prefix), "path is null");
        checkArgument(null != pathMapping, "pathMapping is null");
        checkArgument(!requireNonNull(methods, "methods").isEmpty(), "methods is empty.");

        this.prefix = prefix;
        this.pathMapping = requireNonNull(pathMapping);
        this.methods = Sets.immutableEnumSet(methods);
        this.consumes = ImmutableSet.copyOf(requireNonNull(consumes, "consumes"));
        this.produces = ImmutableSet.copyOf(requireNonNull(produces, "produces"));
        this.paramPredicates = ImmutableList.copyOf(requireNonNull(paramPredicates, "paramPredicates"));
        this.headerPredicates = ImmutableList.copyOf(requireNonNull(headerPredicates, "headerPredicates"));
        this.statusCode = statusCode;

        hashCode = Objects.hash(this.pathMapping, this.methods, this.consumes, this.produces,
                                this.paramPredicates, this.headerPredicates);
    }

    @Override
    public String fullPath() {
        return (prefix + pathMapping.path());
    }

    @Override
    public RouteResult apply(RouteContext context) {
        return null;
    }

    @Override
    public Set<String> paramNames() {
        return null;
    }

    @Override
    public String patternString() {
        return null;
    }

    @Override
    public RoutePathType pathType() {
        return null;
    }

    @Override
    public List<String> paths() {
        return null;
    }

    @Override
    public int complexity() {
        return 0;
    }

    @Override
    public Set<HttpMethod> methods() {
        return null;
    }

    @Override
    public Set<MediaType> consumes() {
        return null;
    }

    @Override
    public Set<MediaType> produces() {
        return null;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("prefix", prefix)
                          .add("statusCode", statusCode)
                          .add("pathMapping", pathMapping)
                          .add("methods", methods)
                          .add("consumes", consumes)
                          .add("produces", produces)
                          .add("paramPredicates", paramPredicates)
                          .add("headerPredicates", headerPredicates)
                          .add("hashCode", hashCode)
                          .toString();
    }
}
