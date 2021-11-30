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

/**
 * The type of the path which was specified when a {@link Route} is created.
 *
 * @see RouteBuilder#path(String, String)
 */
public enum RoutePathType {

    /**
     * The exact path type. e.g, "/foo"
     */
    EXACT(true),

    /**
     * The prefix path type. e.g, "/", "/foo/"
     */
    PREFIX(true),

    /**
     * The path which contains path parameters. e.g, "/:", "/foo/:/bar/:"
     */
    PARAMETERIZED(true),

    /**
     * The regex path type. e.g, {@code "^/(?<foo>.*)$"}
     * The {@link Route} which is created using {@link RouteBuilder#glob(String)} and
     * {@link RouteBuilder#regex(String)} can be this type.
     */
    REGEX(false),

    /**
     * The path which has the prefix and the regex.
     *
     * @see RouteBuilder#path(String, String)
     */
    REGEX_WITH_PREFIX(false);

    private final boolean hasTriePath;

    RoutePathType(boolean hasTriePath) {
        this.hasTriePath = hasTriePath;
    }

    /**
     * Tells whether this {@link RoutePathType} has a trie path or not.
     */
    public boolean hasTriePath() {
        return hasTriePath;
    }
}
