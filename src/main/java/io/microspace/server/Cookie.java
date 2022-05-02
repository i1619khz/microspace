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

import javax.annotation.Nullable;

/**
 * @author i1619kHz
 */
public interface Cookie extends Comparable<Cookie> {

    /**
     * Constant for undefined MaxAge attribute value.
     */
    long UNDEFINED_MAX_AGE = Long.MIN_VALUE;

    /**
     * Returns a newly created {@link Cookie}.
     *
     * @param name  the name of the {@link Cookie}
     * @param value the value of the {@link Cookie}
     */
    static Cookie of(String name, String value) {
        return builder(name, value).build();
    }

    /**
     * Returns a newly created {@link CookieBuilder} which builds a {@link Cookie}.
     *
     * @param name  the name of the {@link Cookie}
     * @param value the value of the {@link Cookie}
     */
    static CookieBuilder builder(String name, String value) {
        return new CookieBuilder(name, value);
    }

    /**
     * Returns the name of this {@link Cookie}.
     */
    String name();

    /**
     * Returns the value of this {@link Cookie}.
     */
    String value();

    /**
     * Returns whether the raw value of this {@link Cookie} was wrapped with double quotes
     * in the original {@code "Set-Cookie"} header.
     */
    boolean isValueQuoted();

    /**
     * Returns the domain of this {@link Cookie}.
     *
     * @return the domain, or {@code null}.
     */
    @Nullable
    String domain();

    /**
     * Returns the path of this {@link Cookie}.
     *
     * @return the path, or {@code null}.
     */
    @Nullable
    String path();

    /**
     * Returns the maximum age of this {@link Cookie} in seconds.
     *
     * @return the maximum age, or {@link Cookie#UNDEFINED_MAX_AGE} if unspecified.
     */
    long maxAge();

    /**
     * Returns whether this {@link Cookie} is secure.
     */
    boolean isSecure();

    /**
     * Returns whether this {@link Cookie} can only be accessed via HTTP.
     * If this returns {@code true}, the {@link Cookie} cannot be accessed through client side script.
     * However, it works only if the browser supports it.
     * Read <a href="http://www.owasp.org/index.php/HTTPOnly">here</a> for more information.
     */
    boolean isHttpOnly();

    /**
     * Returns the <a href="https://tools.ietf.org/html/draft-ietf-httpbis-rfc6265bis-03#section-4.1.2.7"
     * >{@code "SameSite"}</a> attribute of this {@link Cookie}.
     *
     * @return the {@code "SameSite"} attribute, or {@code null}.
     */
    @Nullable
    String sameSite();
}
