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
final class DefaultRouterContext implements RouteContext {
    @Override
    public String hostname() {
        return null;
    }

    @Override
    public HttpMethod method() {
        return null;
    }

    @Override
    public String path() {
        return null;
    }

    @Nullable
    @Override
    public String query() {
        return null;
    }

    @Override
    public QueryParams params() {
        return null;
    }

    @Nullable
    @Override
    public MediaType contentType() {
        return null;
    }

    @Override
    public List<MediaType> acceptTypes() {
        return null;
    }

    @Override
    public RequestHeaders headers() {
        return null;
    }

    @Override
    public void deferStatusException(HttpStatusException cause) {

    }

    @Nullable
    @Override
    public HttpStatusException deferredStatusException() {
        return null;
    }

    @Override
    public boolean isCorsPreflight() {
        return false;
    }
}
