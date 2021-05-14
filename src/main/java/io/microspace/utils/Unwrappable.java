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
package io.microspace.utils;

import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * @author i1619kHz
 */
public interface Unwrappable {
    /**
     * Unwraps this object into the object of the specified {@code type}.
     * Use this method instead of an explicit downcast. For example:
     * <pre>{@code
     * class Foo {}
     *
     * class Bar<T> extends AbstractWrapper<T> {
     *     Bar(T delegate) {
     *         super(delegate);
     *     }
     * }
     *
     * class Qux<T> extends AbstractWrapper<T> {
     *     Qux(T delegate) {
     *         super(delegate);
     *     }
     * }
     *
     * Qux qux = new Qux(new Bar(new Foo()));
     * Foo foo = qux.as(Foo.class);
     * Bar bar = qux.as(Bar.class);
     * }</pre>
     *
     * @param type the type of the object to return
     * @return the object of the specified {@code type} if found, or {@code null} if not found.
     */
    @Nullable
    default <T> T as(Class<T> type) {
        requireNonNull(type, "type");
        return type.isInstance(this) ? type.cast(this) : null;
    }

    /**
     * Unwraps this object and returns the object being decorated. If this {@link Unwrappable} is the innermost
     * object, this method returns itself. For example:
     * <pre>{@code
     * class Foo implements Unwrappable {}
     *
     * class Bar<T extends Unwrappable> extends AbstractUnwrappable<T> {
     *     Bar(T delegate) {
     *         super(delegate);
     *     }
     * }
     *
     * class Qux<T extends Unwrappable> extends AbstractUnwrappable<T> {
     *     Qux(T delegate) {
     *         super(delegate);
     *     }
     * }
     *
     * Foo foo = new Foo();
     * assert foo.unwrap() == foo;
     *
     * Bar<Foo> bar = new Bar<>(foo);
     * assert bar.unwrap() == foo;
     *
     * Qux<Bar<Foo>> qux = new Qux<>(bar);
     * assert qux.unwrap() == bar;
     * assert qux.unwrap().unwrap() == foo;
     * }</pre>
     */
    default Unwrappable unwrap() {
        return this;
    }
}
