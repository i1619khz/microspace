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
package io.microspace.internal;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nullable;

/**
 * @param <IN_NAME> the type of the user-specified names, which may be more permissive than {@link NAME}
 * @param <NAME>    the actual type of the names
 */
public interface StringMultimapGetters<IN_NAME extends CharSequence, NAME extends IN_NAME>
        extends Iterable<Entry<NAME, String>> {

    @Nullable
    String get(IN_NAME name);

    String get(IN_NAME name, String defaultValue);

    List<String> getAll(IN_NAME name);

    @Nullable
    Integer getInt(IN_NAME name);

    int getInt(IN_NAME name, int defaultValue);

    @Nullable
    Long getLong(IN_NAME name);

    long getLong(IN_NAME name, long defaultValue);

    @Nullable
    Float getFloat(IN_NAME name);

    float getFloat(IN_NAME name, float defaultValue);

    @Nullable
    Double getDouble(IN_NAME name);

    double getDouble(IN_NAME name, double defaultValue);

    @Nullable
    Long getTimeMillis(IN_NAME name);

    long getTimeMillis(IN_NAME name, long defaultValue);

    boolean contains(IN_NAME name);

    boolean contains(IN_NAME name, String value);

    boolean containsObject(IN_NAME name, Object value);

    boolean containsInt(IN_NAME name, int value);

    boolean containsLong(IN_NAME name, long value);

    boolean containsFloat(IN_NAME name, float value);

    boolean containsDouble(IN_NAME name, double value);

    boolean containsTimeMillis(IN_NAME name, long value);

    int size();

    boolean isEmpty();

    Set<NAME> names();

    @Override
    Iterator<Entry<NAME, String>> iterator();

    Iterator<String> valueIterator(IN_NAME name);

    void forEach(BiConsumer<NAME, String> action);

    void forEachValue(IN_NAME name, Consumer<String> action);

    Stream<Entry<NAME, String>> stream();

    Stream<String> valueStream(IN_NAME name);
}
