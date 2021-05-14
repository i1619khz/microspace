package io.microspace.server.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies an order which is used to sort the annotated service methods.
 *
 * @author i1619kHz
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface Order {
    /**
     * An order of a method which is annotated by this annotation. The method with the smallest order
     * would be loaded at first. The value could be specified between {@value Integer#MIN_VALUE} and
     * {@value Integer#MAX_VALUE}. The default order is 0 which is the same as if this annotation is
     * not specified.
     */
    int value() default 0;
}
