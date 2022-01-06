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
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Streams;

import io.netty.util.AsciiString;

/**
 * @author i1619kHz
 */
public class RoutePredicate<T> implements Predicate<T> {
    private static final Logger logger = LoggerFactory.getLogger(RoutePredicate.class);

    /**
     * Patterns used to parse a given predicate. The predicate can be one of the following forms:
     * <ul>
     *     <li>{@code some-name=some-value} which means that the request must have a
     *     {@code some-name=some-value} header or parameter</li>
     *     <li>{@code some-name!=some-value} which means that the request must not have a
     *     {@code some-name=some-value} header or parameter</li>
     *     <li>{@code some-name} which means that the request must contain a {@code some-name} header or
     *     parameter</li>
     *     <li>{@code !some-name} which means that the request must not contain a {@code some-name} header or
     *     parameter</li>
     * </ul>
     */
    private static final Pattern CONTAIN_PATTERN = Pattern.compile("^\\s*([!]?)([^\\s=><!]+)\\s*$");
    private static final Pattern COMPARE_PATTERN = Pattern.compile("^\\s*([^\\s!><=]+)\\s*([><!]?=|>|<)(.*)$");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s");
    private final CharSequence name;
    private final Predicate<T> delegate;

    RoutePredicate(CharSequence name, Predicate<T> delegate) {
        this.name = requireNonNull(name, "name");
        this.delegate = requireNonNull(delegate, "delegate");
    }

    static List<RoutePredicate<HttpHeaders>> copyOfHeaderPredicates(Iterable<String> predicates) {
        return Streams.stream(predicates)
                      .map(RoutePredicate::ofHeaders).collect(toImmutableList());
    }

    static List<RoutePredicate<QueryParams>> copyOfParamPredicates(Iterable<String> predicates) {
        return Streams.stream(predicates)
                      .map(RoutePredicate::ofParams).collect(toImmutableList());
    }

    static RoutePredicate<HttpHeaders> ofHeaders(CharSequence headerName,
                                                 Predicate<? super String> valuePredicate) {
        final AsciiString name = HttpHeaderNames.of(headerName);
        return new RoutePredicate<>(headerName, headers ->
                headers.getAll(name).stream().anyMatch(valuePredicate));
    }

    @VisibleForTesting
    static RoutePredicate<HttpHeaders> ofHeaders(String headersPredicate) {
        requireNonNull(headersPredicate, "headersPredicate");
        return of(headersPredicate, HttpHeaderNames::of, name -> headers -> headers.contains(name),
                  (name, value) -> headers -> headers.getAll(name).stream().anyMatch(value::equals));
    }

    static RoutePredicate<QueryParams> ofParams(String paramName,
                                                Predicate<? super String> valuePredicate) {
        return new RoutePredicate<>(paramName, params ->
                params.getAll(paramName).stream().anyMatch(valuePredicate));
    }

    @VisibleForTesting
    static RoutePredicate<QueryParams> ofParams(String paramsPredicate) {
        requireNonNull(paramsPredicate, "paramsPredicate");
        return of(paramsPredicate, Function.identity(), name -> params -> params.contains(name),
                  (name, value) -> params -> params.getAll(name).stream().anyMatch(value::equals));
    }

    @VisibleForTesting
    static <T, U> RoutePredicate<T> of(String predicateExpr,
                                       Function<String, U> nameConverter,
                                       Function<U, Predicate<T>> containsPredicateFactory,
                                       BiFunction<U, String, Predicate<T>> equalsPredicateFactory) {
        final Matcher containMatcher = CONTAIN_PATTERN.matcher(predicateExpr);
        if (containMatcher.matches()) {
            final U name = nameConverter.apply(containMatcher.group(2));
            final Predicate<T> predicate = containsPredicateFactory.apply(name);
            if ("!".equals(containMatcher.group(1))) {
                return new RoutePredicate<>("not_" + containMatcher.group(2), predicate.negate());
            } else {
                return new RoutePredicate<>(predicateExpr, predicate);
            }
        }

        final Matcher compareMatcher = COMPARE_PATTERN.matcher(predicateExpr);
        checkArgument(compareMatcher.matches(),
                      "Invalid predicate: %s (expected: '%s' or '%s')",
                      predicateExpr, CONTAIN_PATTERN.pattern(), COMPARE_PATTERN.pattern());
        assert compareMatcher.groupCount() == 3;

        final String name = compareMatcher.group(1);
        final String comparator = compareMatcher.group(2);
        final String value = compareMatcher.group(3);

        final Predicate<T> predicate = equalsPredicateFactory.apply(nameConverter.apply(name), value);
        final String noWsValue = WHITESPACE_PATTERN.matcher(value).replaceAll("_");
        if ("=".equals(comparator)) {
            return new RoutePredicate<>(name + "_eq_" + noWsValue, predicate);
        } else {
            assert "!=".equals(comparator);
            return new RoutePredicate<>(name + "_ne_" + noWsValue, predicate.negate());
        }
    }

    CharSequence name() {
        return name;
    }

    /**
     * Tests the specified {@code t} object.
     *
     * @see DefaultRoute where this predicate is evaluated
     */
    @Override
    public boolean test(T t) {
        try {
            return delegate.test(t);
        } catch (Throwable cause) {
            // Do not write the following log message every time because an abnormal request may be
            // from an abusing user or a hacker and logging it every time may affect system performance.
            logger.warn("Failed to evaluate the value of header or param '{}'. " +
                        "You MUST catch and handle this exception properly: " +
                        "input={}", name, t, cause);
            return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof final RoutePredicate<?> that)) {
            return false;
        }

        return name.equals(that.name) &&
               delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return name.hashCode() * 31 + delegate.hashCode();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("name", name)
                          .add("delegate", delegate)
                          .toString();
    }
}
