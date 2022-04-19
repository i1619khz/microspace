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
