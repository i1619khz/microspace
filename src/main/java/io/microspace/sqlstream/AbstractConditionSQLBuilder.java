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
package io.microspace.sqlstream;

import java.io.Serializable;

import io.microspace.sqlstream.condition.Between;
import io.microspace.sqlstream.condition.Compare;
import io.microspace.sqlstream.condition.In;
import io.microspace.sqlstream.condition.Like;
import io.microspace.sqlstream.condition.Null;
import io.microspace.sqlstream.condition.Size;

/**
 * @author i1619kHz
 */
abstract class AbstractConditionSQLBuilder<R extends Serializable, RType>
        implements Compare<R, RType>, Size<R, RType>, Between<R, RType>,
                   In<R, RType>, Like<R, RType>, Null<RType> {
}
