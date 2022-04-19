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
package io.microspace.sqlstream.condition;

import java.io.Serializable;

import io.microspace.sqlstream.Lambdas;

/**
 * @author i1619kHz
 */
public interface Size<R extends Serializable, RType> extends Serializable {
    default <V> RType gt(boolean condition, R typeFunction, V value) {
        return this.gt(condition, Lambdas.lambdaColumnName(typeFunction), value);
    }

    default <V> RType gt(R typeFunction, V value) {
        return this.gt(Lambdas.lambdaColumnName(typeFunction), value);
    }

    default <V> RType ge(boolean condition, R typeFunction, V value) {
        return this.ge(condition, Lambdas.lambdaColumnName(typeFunction), value);
    }

    default <V> RType ge(R typeFunction, V value) {
        return this.ge(Lambdas.lambdaColumnName(typeFunction), value);
    }

    default <V> RType lt(boolean condition, R typeFunction, V value) {
        return this.lt(condition, Lambdas.lambdaColumnName(typeFunction), value);
    }

    default <V> RType lt(R typeFunction, V value) {
        return this.lt(Lambdas.lambdaColumnName(typeFunction), value);
    }

    default <V> RType le(boolean condition, R typeFunction, V value) {
        return this.le(condition, Lambdas.lambdaColumnName(typeFunction), value);
    }

    default <V> RType le(R typeFunction, V value) {
        return this.le(Lambdas.lambdaColumnName(typeFunction), value);
    }

    <V> RType gt(boolean condition, String fieldName, V value);

    <V> RType gt(String fieldName, V value);

    <V> RType ge(boolean condition, String fieldName, V value);

    <V> RType ge(String fieldName, V value);

    <V> RType lt(boolean condition, String fieldName, V value);

    <V> RType lt(String fieldName, V value);

    <V> RType le(boolean condition, String fieldName, V value);

    <V> RType le(String fieldName, V value);
}
