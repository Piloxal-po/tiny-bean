package com.github.oxal.annotation.context;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to be executed before the application context is loaded.
 * The method must be public and have no parameters.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface BeforeContextLoad {
    /**
     * The order of execution. Lower values have higher priority.
     *
     * @return the order value
     */
    int order() default 100;
}
