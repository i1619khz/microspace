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
