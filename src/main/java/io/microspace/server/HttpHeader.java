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
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import io.netty.util.AsciiString;

/**
 * @author i1619kHz
 */
public class HttpHeader implements Header {
    @Override
    public boolean isEndOfStream() {
        return false;
    }

    @Nullable
    @Override
    public MediaType contentType() {
        return null;
    }

    @Nullable
    @Override
    public String get(CharSequence name) {
        return null;
    }

    @Override
    public String get(CharSequence name, String defaultValue) {
        return null;
    }

    @Override
    public List<String> getAll(CharSequence name) {
        return null;
    }

    @Nullable
    @Override
    public Integer getInt(CharSequence name) {
        return null;
    }

    @Override
    public int getInt(CharSequence name, int defaultValue) {
        return 0;
    }

    @Nullable
    @Override
    public Long getLong(CharSequence name) {
        return null;
    }

    @Override
    public long getLong(CharSequence name, long defaultValue) {
        return 0;
    }

    @Nullable
    @Override
    public Float getFloat(CharSequence name) {
        return null;
    }

    @Override
    public float getFloat(CharSequence name, float defaultValue) {
        return 0;
    }

    @Nullable
    @Override
    public Double getDouble(CharSequence name) {
        return null;
    }

    @Override
    public double getDouble(CharSequence name, double defaultValue) {
        return 0;
    }

    @Nullable
    @Override
    public Long getTimeMillis(CharSequence name) {
        return null;
    }

    @Override
    public long getTimeMillis(CharSequence name, long defaultValue) {
        return 0;
    }

    @Override
    public boolean contains(CharSequence name) {
        return false;
    }

    @Override
    public boolean contains(CharSequence name, String value) {
        return false;
    }

    @Override
    public boolean containsObject(CharSequence name, Object value) {
        return false;
    }

    @Override
    public boolean containsInt(CharSequence name, int value) {
        return false;
    }

    @Override
    public boolean containsLong(CharSequence name, long value) {
        return false;
    }

    @Override
    public boolean containsFloat(CharSequence name, float value) {
        return false;
    }

    @Override
    public boolean containsDouble(CharSequence name, double value) {
        return false;
    }

    @Override
    public boolean containsTimeMillis(CharSequence name, long value) {
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
    public Set<AsciiString> names() {
        return null;
    }

    @Override
    public Iterator<Map.Entry<AsciiString, String>> iterator() {
        return null;
    }

    @Override
    public Iterator<String> valueIterator(CharSequence name) {
        return null;
    }

    @Override
    public void forEach(BiConsumer<AsciiString, String> action) {

    }

    @Override
    public void forEachValue(CharSequence name, Consumer<String> action) {

    }
}
