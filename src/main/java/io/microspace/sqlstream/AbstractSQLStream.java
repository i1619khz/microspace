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

import static java.util.stream.Collectors.joining;
import static io.microspace.sqlstream.SQLConstants.asterisk;
import static io.microspace.sqlstream.SQLConstants.comma;
import static io.microspace.sqlstream.SQLConstants.count;
import static io.microspace.sqlstream.SQLConstants.distinct;
import static io.microspace.sqlstream.SQLConstants.leftParenthesis;
import static io.microspace.sqlstream.SQLConstants.rightParenthesis;
import static io.microspace.sqlstream.SQLConstants.select;
import static io.microspace.sqlstream.SQLConstants.selectAll;

import java.util.Arrays;

import org.sql2o.Sql2o;

/**
 * @author i1619kHz
 */
abstract class AbstractSQLStream<T> implements SQLAction<T> {
    protected final Sql2o sql2o;
    protected final StringBuilder sqlAction = new StringBuilder();
    protected T typedThis = (T) this;

    AbstractSQLStream(Sql2o sql2o) {
        this.sql2o = sql2o;
    }

    @Override
    public T select() {
        this.sqlAction.append(selectAll);
        return typedThis;
    }

    @Override
    public T select(String... fieldNames) {
        assert fieldNames != null : "fileNames cannot be set to null";
        if (fieldNames.length == 0) {
            return select();
        } else {
            this.sqlAction.append(select).append(String.join(comma, fieldNames));
        }
        return typedThis;
    }

    @Override
    public T distinct(String... fieldNames) {
        assert fieldNames != null : "fileNames cannot be set to null";
        if (this.sqlAction.toString().startsWith(selectAll)) {
            sqlAction.replace(0, selectAll.length(), select);
        } else {
            this.sqlAction.append(distinct).append(
                    String.join(comma, fieldNames));
        }
        return typedThis;
    }

    @Override
    public T count(String fieldNames) {
        assert fieldNames != null : "fileNames cannot be set to null";
        if (sqlAction.toString().startsWith(selectAll)) {
            sqlAction.replace(0, selectAll.length(), select);
        } else {
            sqlAction.append(count).append(leftParenthesis).append(
                    String.join(comma, fieldNames)).append(rightParenthesis);
        }
        return typedThis;
    }

    @Override
    public T count() {
        return count(asterisk);
    }

    @Override
    public <K, R> T count(TypeFunction<K, R> typeFunctions) {
        assert typeFunctions != null : "fieldFunctions cannot be set to null";
        return count(Lambdas.lambdaColumnName(typeFunctions));
    }

    @Override
    public <K, R> T select(TypeFunction<K, R>... typeFunctions) {
        assert typeFunctions != null : "fieldFunctions cannot be set to null";
        return select(Arrays.stream(typeFunctions)
                            .map(Lambdas::lambdaColumnName)
                            .collect(joining(",")));
    }

    @Override
    public <K, R> T distinct(TypeFunction<K, R>... typeFunctions) {
        this.distinct(Arrays.stream(typeFunctions)
                            .map(Lambdas::lambdaColumnName)
                            .collect(joining(comma)));
        return typedThis;
    }

    public Sql2o getSql2o() {
        return sql2o;
    }
}
