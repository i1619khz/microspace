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

import static java.util.Objects.requireNonNull;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.common.collect.Streams;

import io.microspace.internal.StringMultimapGetters;

/**
 * Provides the getter methods to {@link QueryParams} and {@link QueryParamsBuilder}.
 */
interface QueryParamGetters extends StringMultimapGetters</* IN_NAME */ String, /* NAME */ String> {

    /**
     * Returns the value of a parameter with the specified {@code name}. If there are more than one value for
     * the specified {@code name}, the first value in insertion order is returned.
     *
     * @param name the parameter name
     * @return the first parameter value if found, or {@code null} if there is no such parameter
     */
    @Override
    @Nullable
    String get(String name);

    /**
     * Returns the value of a parameter with the specified {@code name}. If there are more than one value for
     * the specified {@code name}, the first value in insertion order is returned.
     *
     * @param name         the parameter name
     * @param defaultValue the default value
     * @return the first parameter value, or {@code defaultValue} if there is no such parameter
     */
    @Override
    String get(String name, String defaultValue);

    /**
     * Returns all values for the parameter with the specified name. The returned {@link List} can't be
     * modified.
     *
     * @param name the parameter name
     * @return a {@link List} of parameter values or an empty {@link List} if there is no such parameter.
     */
    @Override
    List<String> getAll(String name);

    /**
     * Returns the {@code int} value of a parameter with the specified {@code name}. If there are more than one
     * value for the specified {@code name}, the first value in insertion order is returned.
     *
     * @param name the parameter name
     * @return the {@code int} value of the first value in insertion order or {@code null} if there is no such
     * parameter or it can't be converted to {@code int}.
     */
    @Override
    @Nullable
    Integer getInt(String name);

    /**
     * Returns the {@code int} value of a parameter with the specified {@code name}. If there are more than one
     * value for the specified {@code name}, the first value in insertion order is returned.
     *
     * @param name         the parameter name
     * @param defaultValue the default value
     * @return the {@code int} value of the first value in insertion order or {@code defaultValue} if there is
     * no such parameter or it can't be converted to {@code int}.
     */
    @Override
    int getInt(String name, int defaultValue);

    /**
     * Returns the {@code long} value of a parameter with the specified {@code name}. If there are more than
     * one value for the specified {@code name}, the first value in insertion order is returned.
     *
     * @param name the parameter name
     * @return the {@code long} value of the first value in insertion order or {@code null} if there is no such
     * parameter or it can't be converted to {@code long}.
     */
    @Override
    @Nullable
    Long getLong(String name);

    /**
     * Returns the {@code long} value of a parameter with the specified {@code name}. If there are more than
     * one value for the specified {@code name}, the first value in insertion order is returned.
     *
     * @param name         the parameter name
     * @param defaultValue the default value
     * @return the {@code long} value of the first value in insertion order or {@code defaultValue} if there is
     * no such parameter or it can't be converted to {@code long}.
     */
    @Override
    long getLong(String name, long defaultValue);

    /**
     * Returns the {@code float} value of a parameter with the specified {@code name}. If there are more than
     * one value for the specified {@code name}, the first value in insertion order is returned.
     *
     * @param name the parameter name
     * @return the {@code float} value of the first value in insertion order or {@code null} if there is no
     * such parameter or it can't be converted to {@code float}.
     */
    @Override
    @Nullable
    Float getFloat(String name);

    /**
     * Returns the {@code float} value of a parameter with the specified {@code name}. If there are more than
     * one value for the specified {@code name}, the first value in insertion order is returned.
     *
     * @param name         the parameter name
     * @param defaultValue the default value
     * @return the {@code float} value of the first value in insertion order or {@code defaultValue} if there
     * is no such parameter or it can't be converted to {@code float}.
     */
    @Override
    float getFloat(String name, float defaultValue);

    /**
     * Returns the {@code double} value of a parameter with the specified {@code name}. If there are more than
     * one value for the specified {@code name}, the first value in insertion order is returned.
     *
     * @param name the parameter name
     * @return the {@code double} value of the first value in insertion order or {@code null} if there is no
     * such parameter or it can't be converted to {@code double}.
     */
    @Override
    @Nullable
    Double getDouble(String name);

    /**
     * Returns the {@code double} value of a parameter with the specified {@code name}. If there are more than
     * one value for the specified {@code name}, the first value in insertion order is returned.
     *
     * @param name         the parameter name
     * @param defaultValue the default value
     * @return the {@code double} value of the first value in insertion order or {@code defaultValue} if there
     * is no such parameter or it can't be converted to {@code double}.
     */
    @Override
    double getDouble(String name, double defaultValue);

    /**
     * Returns the value of a parameter with the specified {@code name} in milliseconds. If there are more than
     * one value for the specified {@code name}, the first value in insertion order is returned.
     *
     * @param name the parameter name
     * @return the milliseconds value of the first value in insertion order or {@code null} if there is no such
     * parameter or it can't be converted to milliseconds.
     */
    @Override
    @Nullable
    Long getTimeMillis(String name);

    /**
     * Returns the value of a parameter with the specified {@code name} in milliseconds. If there are more than
     * one value for the specified {@code name}, the first value in insertion order is returned.
     *
     * @param name         the parameter name
     * @param defaultValue the default value
     * @return the milliseconds value of the first value in insertion order or {@code defaultValue} if there is
     * no such parameter or it can't be converted to milliseconds.
     */
    @Override
    long getTimeMillis(String name, long defaultValue);

    /**
     * Returns {@code true} if a parameter with the {@code name} exists, {@code false} otherwise.
     *
     * @param name the parameter name
     */
    @Override
    boolean contains(String name);

    /**
     * Returns {@code true} if a parameter with the {@code name} and {@code value} exists.
     *
     * @param name  the parameter name
     * @param value the parameter value to find
     */
    @Override
    boolean contains(String name, String value);

    /**
     * Returns {@code true} if a parameter with the {@code name} and {@code value} exists.
     *
     * @param name  the parameter name
     * @param value the parameter value
     * @return {@code true} if the parameter exists. {@code false} otherwise
     */
    @Override
    boolean containsObject(String name, Object value);

    /**
     * Returns {@code true} if a parameter with the {@code name} and {@code value} exists.
     *
     * @param name  the parameter name
     * @param value the parameter value
     * @return {@code true} if the parameter exists. {@code false} otherwise
     */
    @Override
    boolean containsInt(String name, int value);

    /**
     * Returns {@code true} if a parameter with the {@code name} and {@code value} exists.
     *
     * @param name  the parameter name
     * @param value the parameter value
     * @return {@code true} if the parameter exists. {@code false} otherwise
     */
    @Override
    boolean containsLong(String name, long value);

    /**
     * Returns {@code true} if a parameter with the {@code name} and {@code value} exists.
     *
     * @param name  the parameter name
     * @param value the parameter value
     * @return {@code true} if the parameter exists. {@code false} otherwise
     */
    @Override
    boolean containsFloat(String name, float value);

    /**
     * Returns {@code true} if a parameter with the {@code name} and {@code value} exists.
     *
     * @param name  the parameter name
     * @param value the parameter value
     * @return {@code true} if the parameter exists. {@code false} otherwise
     */
    @Override
    boolean containsDouble(String name, double value);

    /**
     * Returns {@code true} if a parameter with the {@code name} and {@code value} exists.
     *
     * @param name  the parameter name
     * @param value the parameter value
     * @return {@code true} if the parameter exists. {@code false} otherwise
     */
    @Override
    boolean containsTimeMillis(String name, long value);

    /**
     * Returns the number of parameters.
     */
    @Override
    int size();

    /**
     * Returns {@code true} if this parameters does not contain any entries.
     */
    @Override
    boolean isEmpty();

    /**
     * Returns a {@link Set} of all parameter names. The returned {@link Set} cannot be modified.
     */
    @Override
    Set<String> names();

    /**
     * Returns an {@link Iterator} that yields all parameter entries.
     */
    @Override
    Iterator<Entry<String, String>> iterator();

    /**
     * Returns an {@link Iterator} that yields all values of the parameters with the specified {@code name}.
     */
    @Override
    Iterator<String> valueIterator(String name);

    /**
     * Invokes the specified {@code action} for all parameter entries.
     */
    @Override
    void forEach(BiConsumer<String, String> action);

    /**
     * Invokes the specified {@code action} for all values of the parameters with the specified {@code name}.
     */
    @Override
    void forEachValue(String name, Consumer<String> action);

    /**
     * Returns a {@link Stream} that yields all parameter entries.
     */
    @Override
    default Stream<Entry<String, String>> stream() {
        return Streams.stream(iterator());
    }

    /**
     * Returns a {@link Stream} that yields all values of the parameters with the specified {@code name}.
     */
    @Override
    default Stream<String> valueStream(String name) {
        requireNonNull(name, "name");
        return Streams.stream(valueIterator(name));
    }

    /**
     * Encodes all parameter entries into a query string, as defined in
     * <a href="https://www.w3.org/TR/2014/REC-html5-20141028/forms.html#url-encoded-form-data">4.10.22.6,
     * HTML5 W3C Recommendation</a>.
     *
     * @return the encoded query string.
     */
    default String toQueryString() {
        return "";
    }

    /**
     * Encodes all parameter entries into a query string, as defined in
     * <a href="https://www.w3.org/TR/2014/REC-html5-20141028/forms.html#url-encoded-form-data">4.10.22.6,
     * HTML5 W3C Recommendation</a>, and appends it into the specified {@link StringBuilder}.
     *
     * @return the specified {@link StringBuilder} for method chaining.
     */
    default StringBuilder appendQueryString(StringBuilder buf) {
        requireNonNull(buf, "buf");
        return buf;
    }
}