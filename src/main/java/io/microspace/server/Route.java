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
import java.util.Set;

/**
 * @author i1619kHz
 */
public interface Route {
    static RouteBuilder builder() {
        return new RouteBuilder();
    }

    String fullPath();

    RouteResult apply(RouteContext context);

    /**
     * Returns the names of the path parameters extracted by this mapping.
     */
    Set<String> paramNames();

    /**
     * Returns the path pattern of this {@link Route}. The returned path pattern is different according to
     * the value of {@link #pathType()}.
     *
     * <ul>
     *   <li>{@linkplain RoutePathType#EXACT EXACT}: {@code "/foo"} or {@code "/foo/bar"}</li>
     *   <li>{@linkplain RoutePathType#PREFIX PREFIX}: {@code "/foo/*"}</li>
     *   <li>{@linkplain RoutePathType#PARAMETERIZED PARAMETERIZED}: {@code "/foo/:bar"} or
     *       {@code "/foo/:bar/:qux}</li>
     *   <li>{@linkplain RoutePathType#REGEX REGEX} may have a glob pattern or a regular expression:
     *     <ul>
     *       <li><code>"/*&#42;/foo"</code> if the {@link Route} was created using
     *           {@link RouteBuilder#glob(String)}</li>
     *       <li>{@code "^/(?(.+)/)?foo$"} if the {@link Route} was created using
     *           {@link RouteBuilder#regex(String)}</li>
     *     </ul>
     *   </li>
     *   <li>{@linkplain RoutePathType#REGEX_WITH_PREFIX REGEX_WITH_PREFIX} may have a glob pattern or
     *       a regular expression with a prefix:
     *     <ul>
     *       <li>{@code "/foo/bar/**"} if the {@link Route} was created using
     *           {@code RouteBuilder.path("/foo/", "glob:/bar/**")}</li>
     *       <li>{@code "/foo/(bar|baz)"} if the {@link Route} was created using
     *           {@code RouteBuilder.path("/foo/", "regex:/(bar|baz)")}</li>
     *     </ul>
     *   </li>
     * </ul>
     */
    String patternString();

    /**
     * Returns the type of the path which was specified when this is created.
     */
    RoutePathType pathType();

    /**
     * Returns the list of paths that this {@link Route} has. The paths are different according to the value
     * of {@link #pathType()}. If the path type has a {@linkplain RoutePathType#hasTriePath() trie path},
     * this method will return a two-element list whose first element is the path that represents the type and
     * the second element is the trie path. {@link RoutePathType#EXACT}, {@link RoutePathType#PREFIX} and
     * {@link RoutePathType#PARAMETERIZED} have the trie path.
     *
     * <ul>
     *   <li>EXACT: {@code [ "/foo", "/foo" ]} (The trie path is the same.)</li>
     *   <li>PREFIX: {@code [ "/foo/", "/foo/*" ]}</li>
     *   <li>PARAMETERIZED: {@code [ "/foo/:", "/foo/:" ]} (The trie path is the same.)</li>
     * </ul>
     *
     * <p>{@link RoutePathType#REGEX} may have one or two paths. If the {@link Route} was created from a glob
     * pattern, it will have two paths where the first one is the regular expression and the second one
     * is the glob pattern, e.g. <code>[ "^/(?(.+)/)?foo$", "/*&#42;/foo" ]</code>.
     * If not created from a glob pattern, it will have only one path, which is the regular expression,
     * e.g, {@code [ "^/(?<foo>.*)$" ]}</p>
     *
     * <p>{@link RoutePathType#REGEX_WITH_PREFIX} has two paths. The first one is the regex and the second
     * one is the path. e.g, {@code [ "^/(?<foo>.*)$", "/bar/" ]}
     */
    List<String> paths();

    /**
     * Returns the complexity of this {@link Route}. A higher complexity indicates more expensive computation
     * for route matching, usually due to additional number of checks.
     */
    int complexity();

    /**
     * Returns the {@link Set} of non-empty {@link HttpMethod}s that this {@link Route} supports.
     */
    Set<HttpMethod> methods();

    /**
     * Returns the {@link Set} of {@link MediaType}s that this {@link Route} consumes.
     */
    Set<MediaType> consumes();

    /**
     * Returns the {@link Set} of {@link MediaType}s that this {@link Route} produces.
     */
    Set<MediaType> produces();
}
