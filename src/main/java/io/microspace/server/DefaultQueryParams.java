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

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.annotation.Nullable;

public class DefaultQueryParams implements QueryParams {

    @Nullable
    @Override
    public String get(String name) {
        return null;
    }

    @Override
    public String get(String name, String defaultValue) {
        return null;
    }

    @Override
    public List<String> getAll(String name) {
        return null;
    }

    @Nullable
    @Override
    public Integer getInt(String name) {
        return null;
    }

    @Override
    public int getInt(String name, int defaultValue) {
        return 0;
    }

    @Nullable
    @Override
    public Long getLong(String name) {
        return null;
    }

    @Override
    public long getLong(String name, long defaultValue) {
        return 0;
    }

    @Nullable
    @Override
    public Float getFloat(String name) {
        return null;
    }

    @Override
    public float getFloat(String name, float defaultValue) {
        return 0;
    }

    @Nullable
    @Override
    public Double getDouble(String name) {
        return null;
    }

    @Override
    public double getDouble(String name, double defaultValue) {
        return 0;
    }

    @Nullable
    @Override
    public Long getTimeMillis(String name) {
        return null;
    }

    @Override
    public long getTimeMillis(String name, long defaultValue) {
        return 0;
    }

    @Override
    public boolean contains(String name) {
        return false;
    }

    @Override
    public boolean contains(String name, String value) {
        return false;
    }

    @Override
    public boolean containsObject(String name, Object value) {
        return false;
    }

    @Override
    public boolean containsInt(String name, int value) {
        return false;
    }

    @Override
    public boolean containsLong(String name, long value) {
        return false;
    }

    @Override
    public boolean containsFloat(String name, float value) {
        return false;
    }

    @Override
    public boolean containsDouble(String name, double value) {
        return false;
    }

    @Override
    public boolean containsTimeMillis(String name, long value) {
        return false;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public Set<String> names() {
        return null;
    }

    @Override
    public Iterator<Entry<String, String>> iterator() {
        return null;
    }

    @Override
    public Iterator<String> valueIterator(String name) {
        return null;
    }

    @Override
    public void forEach(BiConsumer<String, String> action) {

    }

    @Override
    public void forEachValue(String name, Consumer<String> action) {

    }
}
