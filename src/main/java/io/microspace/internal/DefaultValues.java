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

import javax.annotation.Nullable;

/**
 * Holds the default values used in annotation attributes.
 */
public final class DefaultValues {

    /**
     * A string constant defining unspecified values from users.
     *
     * @see Default#value()
     */
    public static final String UNSPECIFIED = "\n\t\t\n\t\t\n\000\001\002\n\t\t\t\t\n";

    /**
     * Returns whether the specified value is specified by a user.
     */
    public static boolean isSpecified(@Nullable String value) {
        return !UNSPECIFIED.equals(value);
    }

    /**
     * Returns whether the specified value is not specified by a user.
     */
    public static boolean isUnspecified(@Nullable String value) {
        return UNSPECIFIED.equals(value);
    }

    /**
     * Returns the specified value if it is specified by a user.
     */
    @Nullable
    public static String getSpecifiedValue(@Nullable String value) {
        return isSpecified(value) ? value : null;
    }

    private DefaultValues() {}
}
