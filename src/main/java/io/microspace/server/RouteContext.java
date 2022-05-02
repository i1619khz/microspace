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

import java.util.List;

import javax.annotation.Nullable;

/**
 * @author i1619kHz
 */
public interface RouteContext {
    /**
     * Returns the virtual host name of the request.
     */
    String hostname();

    /**
     * Returns {@link HttpMethod} of the request.
     */
    HttpMethod method();

    /**
     * Returns the absolute path retrieved from the request,
     * as defined in <a href="https://tools.ietf.org/html/rfc3986">RFC3986</a>.
     */
    String path();

    /**
     * Returns the query retrieved from the request,
     * as defined in <a href="https://tools.ietf.org/html/rfc3986">RFC3986</a>.
     */
    @Nullable
    String query();

    /**
     * Returns the query parameters retrieved from the request path.
     */
    QueryParams params();

    /**
     * Returns {@link MediaType} specified by 'Content-Type' header of the request.
     */
    @Nullable
    MediaType contentType();

    /**
     * Returns a list of {@link MediaType}s that are specified in accept in the order
     * of client-side preferences. If the client does not send the header, this will contain only
     * {@link MediaType#ANY_TYPE}.
     */
    List<MediaType> acceptTypes();

    /**
     * Returns the {@link RequestHeaders} retrieved from the request.
     */
    RequestHeaders headers();

    /**
     * Defers throwing an {@link HttpStatusException} until reaching the end of the service list.
     */
    void deferStatusException(HttpStatusException cause);

    /**
     * Returns a deferred {@link HttpStatusException} which was previously set via
     * {@link #deferStatusException(HttpStatusException)}.
     */
    @Nullable
    HttpStatusException deferredStatusException();

    /**
     * Returns {@code true} if this context is for a CORS preflight request.
     */
    boolean isCorsPreflight();

    /**
     * Returns {@code true} if this context requires matching the predicates for query parameters.
     *
     * @see RouteBuilder#matchesParams(Iterable)
     */
    default boolean requiresMatchingParamsPredicates() {
        return true;
    }

    /**
     * Returns {@code true} if this context requires matching the predicates for HTTP headers.
     *
     * @see RouteBuilder#matchesHeaders(Iterable)
     */
    default boolean requiresMatchingHeadersPredicates() {
        return true;
    }
}
