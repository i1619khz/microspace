package io.microspace.server.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The containing annotation type for {@link MatchesParam}.
 *
 * @author i1619kHz
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface MatchesParams {
    /**
     * An array of {@link MatchesParam} annotations.
     */
    MatchesParam[] value();
}
