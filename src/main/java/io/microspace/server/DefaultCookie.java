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

import java.util.Objects;

import javax.annotation.Nullable;

import com.google.common.base.MoreObjects;

/**
 * @author i1619kHz
 */
public class DefaultCookie implements Cookie {
    private final String name;
    private final String value;
    private final boolean valueQuoted;
    @Nullable
    private final String domain;
    @Nullable
    private final String path;
    private final long maxAge;
    private final boolean secure;
    private final boolean httpOnly;
    @Nullable
    private final String sameSite;

    DefaultCookie(String name, String value, boolean valueQuoted,
                  @Nullable String domain, @Nullable String path,
                  long maxAge, boolean secure, boolean httpOnly,
                  @Nullable String sameSite) {
        this.name = name;
        this.value = value;
        this.valueQuoted = valueQuoted;
        this.domain = domain;
        this.path = path;
        this.maxAge = maxAge;
        this.secure = secure;
        this.httpOnly = httpOnly;
        this.sameSite = sameSite;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public boolean isValueQuoted() {
        return valueQuoted;
    }

    @Override
    public String domain() {
        return domain;
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public long maxAge() {
        return maxAge;
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    @Override
    public boolean isHttpOnly() {
        return httpOnly;
    }

    @Override
    public String sameSite() {
        return sameSite;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }

    @Override
    public String toString() {
        final MoreObjects.ToStringHelper helper
                = MoreObjects.toStringHelper(this).omitNullValues()
                             .add("name", name)
                             .add("value", !value.isEmpty() ? value : "<EMPTY>")
                             .add("valueQuoted", valueQuoted)
                             .add("domain", domain)
                             .add("path", path);

        if (maxAge != Cookie.UNDEFINED_MAX_AGE) {
            helper.add("maxAge", maxAge);
        }

        if (secure) {
            helper.addValue("secure");
        }

        if (httpOnly) {
            helper.addValue("httpOnly");
        }

        helper.add("sameSite", sameSite);
        return helper.toString();
    }

    @Override
    public int compareTo(Cookie o) {
        return 0;
    }
}
