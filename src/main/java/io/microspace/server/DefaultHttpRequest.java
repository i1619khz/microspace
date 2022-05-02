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

import java.net.URL;

import com.google.common.collect.ImmutableList;

/**
 * @author i1619kHz
 */
public class DefaultHttpRequest implements HttpRequest {
    @Override
    public Cookie cookie(String cookieKey) {
        return null;
    }

    @Override
    public Iterable<Cookie> cookies() {
        return ImmutableList.of();
    }

    @Override
    public Header header() {
        return null;
    }

    @Override
    public Iterable<Header> headers() {
        return null;
    }

    @Override
    public String uri() {
        return null;
    }

    @Override
    public String origin() {
        return null;
    }

    @Override
    public String href() {
        return null;
    }

    @Override
    public String method() {
        return null;
    }

    @Override
    public String path() {
        return null;
    }

    @Override
    public QueryParams query() {
        return null;
    }

    @Override
    public String queryString() {
        return null;
    }

    @Override
    public String search() {
        return null;
    }

    @Override
    public String host() {
        return null;
    }

    @Override
    public String hostname() {
        return null;
    }

    @Override
    public URL url() {
        return null;
    }

    @Override
    public boolean fresh() {
        return false;
    }

    @Override
    public boolean stale() {
        return false;
    }

    @Override
    public boolean idempotent() {
        return false;
    }

    @Override
    public String protocol() {
        return null;
    }

    @Override
    public boolean secure() {
        return false;
    }

    @Override
    public String ip() {
        return null;
    }

    @Override
    public String[] ips() {
        return new String[0];
    }

    @Override
    public String[] subdomains() {
        return new String[0];
    }

    @Override
    public String[] accepts() {
        return new String[0];
    }

    @Override
    public String[] acceptsEncodings() {
        return new String[0];
    }

    @Override
    public String[] acceptsCharsets() {
        return new String[0];
    }

    @Override
    public String[] acceptsLanguages() {
        return new String[0];
    }

    @Override
    public String contentType(CharSequence type) {
        return null;
    }
}
