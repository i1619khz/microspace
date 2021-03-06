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
import io.netty.util.AsciiString;

/**
 * Provides the getter methods to {@link HttpHeaders} and {@link HttpHeadersBuilder}.
 */
interface HttpHeaderGetters extends StringMultimapGetters</* IN_NAME */ CharSequence, /* NAME */ AsciiString> {

    /**
     * Tells whether the headers correspond to the last frame in an HTTP/2 stream.
     */
    boolean isEndOfStream();

    /**
     * Returns the parsed {@code "content-type"} header.
     *
     * @return the parsed {@link MediaType} if present and valid, or {@code null} otherwise.
     */
    @Nullable
    MediaType contentType();

    /**
     * Returns the value of a header with the specified {@code name}. If there are more than one value for
     * the specified {@code name}, the first value in insertion order is returned.
     *
     * @param name the name of the header to retrieve
     * @return the first header value if the header is found, or {@code null} if there's no such header
     */
    @Override
    @Nullable
    String get(CharSequence name);

    /**
     * Returns the value of a header with the specified {@code name}. If there are more than one value for
     * the specified {@code name}, the first value in insertion order is returned.
     *
     * @param name         the name of the header to retrieve
     * @param defaultValue the default value
     * @return the first header value or {@code defaultValue} if there is no such header
     */
    @Override
    String get(CharSequence name, String defaultValue);

    /**
     * Returns all values for the header with the specified name. The returned {@link List} can't be modified.
     *
     * @param name the name of the header to retrieve
     * @return a {@link List} of header values or an empty {@link List} if there is no such header.
     */
    @Override
    List<String> getAll(CharSequence name);

    /**
     * Returns the {@code int} value of a header with the specified {@code name}. If there are more than one
     * value for the specified {@code name}, the first value in insertion order is returned.
     *
     * @param name the name of the header to retrieve
     * @return the {@code int} value of the first value in insertion order or {@code null} if there is no such
     * header or it can't be converted to {@code int}.
     */
    @Override
    @Nullable
    Integer getInt(CharSequence name);

    /**
     * Returns the {@code int} value of a header with the specified {@code name}. If there are more than one
     * value for the specified {@code name}, the first value in insertion order is returned.
     *
     * @param name         the name of the header to retrieve
     * @param defaultValue the default value
     * @return the {@code int} value of the first value in insertion order or {@code defaultValue} if there is
     * no such header or it can't be converted to {@code int}.
     */
    @Override
    int getInt(CharSequence name, int defaultValue);

    /**
     * Returns the {@code long} value of a header with the specified {@code name}. If there are more than one
     * value for the specified {@code name}, the first value in insertion order is returned.
     *
     * @param name the name of the header to retrieve
     * @return the {@code long} value of the first value in insertion order or {@code null} if there is no such
     * header or it can't be converted to {@code long}.
     */
    @Override
    @Nullable
    Long getLong(CharSequence name);

    /**
     * Returns the {@code long} value of a header with the specified {@code name}. If there are more than one
     * value for the specified {@code name}, the first value in insertion order is returned.
     *
     * @param name         the name of the header to retrieve
     * @param defaultValue the default value
     * @return the {@code long} value of the first value in insertion order or {@code defaultValue} if there is
     * no such header or it can't be converted to {@code long}.
     */
    @Override
    long getLong(CharSequence name, long defaultValue);

    /**
     * Returns the {@code float} value of a header with the specified {@code name}. If there are more than one
     * value for the specified {@code name}, the first value in insertion order is returned.
     *
     * @param name the name of the header to retrieve
     * @return the {@code float} value of the first value in insertion order or {@code null} if there is no
     * such header or it can't be converted to {@code float}.
     */
    @Override
    @Nullable
    Float getFloat(CharSequence name);

    /**
     * Returns the {@code float} value of a header with the specified {@code name}. If there are more than one
     * value for the specified {@code name}, the first value in insertion order is returned.
     *
     * @param name         the name of the header to retrieve
     * @param defaultValue the default value
     * @return the {@code float} value of the first value in insertion order or {@code defaultValue} if there
     * is no such header or it can't be converted to {@code float}.
     */
    @Override
    float getFloat(CharSequence name, float defaultValue);

    /**
     * Returns the {@code double} value of a header with the specified {@code name}. If there are more than one
     * value for the specified {@code name}, the first value in insertion order is returned.
     *
     * @param name the name of the header to retrieve
     * @return the {@code double} value of the first value in insertion order or {@code null} if there is no
     * such header or it can't be converted to {@code double}.
     */
    @Override
    @Nullable
    Double getDouble(CharSequence name);

    /**
     * Returns the {@code double} value of a header with the specified {@code name}. If there are more than one
     * value for the specified {@code name}, the first value in insertion order is returned.
     *
     * @param name         the name of the header to retrieve
     * @param defaultValue the default value
     * @return the {@code double} value of the first value in insertion order or {@code defaultValue} if there
     * is no such header or it can't be converted to {@code double}.
     */
    @Override
    double getDouble(CharSequence name, double defaultValue);

    /**
     * Returns the value of a header with the specified {@code name} in milliseconds. If there are more than
     * one value for the specified {@code name}, the first value in insertion order is returned.
     *
     * @param name the name of the header to retrieve
     * @return the milliseconds value of the first value in insertion order or {@code null} if there is no such
     * header or it can't be converted to milliseconds.
     */
    @Override
    @Nullable
    Long getTimeMillis(CharSequence name);

    /**
     * Returns the value of a header with the specified {@code name} in milliseconds. If there are more than
     * one value for the specified {@code name}, the first value in insertion order is returned.
     *
     * @param name         the name of the header to retrieve
     * @param defaultValue the default value
     * @return the milliseconds value of the first value in insertion order or {@code defaultValue} if there is
     * no such header or it can't be converted to milliseconds.
     */
    @Override
    long getTimeMillis(CharSequence name, long defaultValue);

    /**
     * Returns {@code true} if a header with the {@code name} exists, {@code false} otherwise.
     *
     * @param name the header name
     */
    @Override
    boolean contains(CharSequence name);

    /**
     * Returns {@code true} if a header with the {@code name} and {@code value} exists.
     *
     * @param name  the header name
     * @param value the header value of the header to find
     */
    @Override
    boolean contains(CharSequence name, String value);

    /**
     * Returns {@code true} if a header with the {@code name} and {@code value} exists.
     *
     * @param name  the header name
     * @param value the header value
     * @return {@code true} if the header exists. {@code false} otherwise
     */
    @Override
    boolean containsObject(CharSequence name, Object value);

    /**
     * Returns {@code true} if a header with the {@code name} and {@code value} exists.
     *
     * @param name  the header name
     * @param value the header value
     * @return {@code true} if the header exists. {@code false} otherwise
     */
    @Override
    boolean containsInt(CharSequence name, int value);

    /**
     * Returns {@code true} if a header with the {@code name} and {@code value} exists.
     *
     * @param name  the header name
     * @param value the header value
     * @return {@code true} if the header exists. {@code false} otherwise
     */
    @Override
    boolean containsLong(CharSequence name, long value);

    /**
     * Returns {@code true} if a header with the {@code name} and {@code value} exists.
     *
     * @param name  the header name
     * @param value the header value
     * @return {@code true} if the header exists. {@code false} otherwise
     */
    @Override
    boolean containsFloat(CharSequence name, float value);

    /**
     * Returns {@code true} if a header with the {@code name} and {@code value} exists.
     *
     * @param name  the header name
     * @param value the header value
     * @return {@code true} if the header exists. {@code false} otherwise
     */
    @Override
    boolean containsDouble(CharSequence name, double value);

    /**
     * Returns {@code true} if a header with the {@code name} and {@code value} exists.
     *
     * @param name  the header name
     * @param value the header value
     * @return {@code true} if the header exists. {@code false} otherwise
     */
    @Override
    boolean containsTimeMillis(CharSequence name, long value);

    /**
     * Returns the number of headers.
     */
    @Override
    int size();

    /**
     * Returns {@code true} if this headers does not contain any entries.
     */
    @Override
    boolean isEmpty();

    /**
     * Returns a {@link Set} of all header names. The returned {@link Set} cannot be modified.
     */
    @Override
    Set<AsciiString> names();

    /**
     * Returns an {@link Iterator} that yields all header entries. The iteration order is as follows:
     * <ol>
     *   <li>All pseudo headers (order not specified).</li>
     *   <li>All non-pseudo headers (in insertion order).</li>
     * </ol>
     */
    @Override
    Iterator<Entry<AsciiString, String>> iterator();

    /**
     * Returns an {@link Iterator} that yields all values of the headers with the specified {@code name}.
     */
    @Override
    Iterator<String> valueIterator(CharSequence name);

    /**
     * Invokes the specified {@code action} for all header entries.
     */
    @Override
    void forEach(BiConsumer<AsciiString, String> action);

    /**
     * Invokes the specified {@code action} for all values of the headers with the specified {@code name}.
     */
    @Override
    void forEachValue(CharSequence name, Consumer<String> action);

    /**
     * Returns a {@link Stream} that yields all header entries.
     */
    @Override
    default Stream<Entry<AsciiString, String>> stream() {
        return Streams.stream(iterator());
    }

    /**
     * Returns a {@link Stream} that yields all values of the headers with the specified {@code name}.
     */
    @Override
    default Stream<String> valueStream(CharSequence name) {
        requireNonNull(name, "name");
        return Streams.stream(valueIterator(name));
    }
}
