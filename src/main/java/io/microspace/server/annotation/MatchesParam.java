package io.microspace.server.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies a predicate which evaluates whether a request can be accepted by a service method.
 *
 * @author i1619kHz
 */
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(MatchesParams.class)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface MatchesParam {
    /**
     * The predicate which evaluates whether a request can be accepted by a service method.
     *
     * @see RouteBuilder#matchesParams(String...)
     * @see RouteBuilder#matchesParams(Iterable)
     */
    String value();
}
