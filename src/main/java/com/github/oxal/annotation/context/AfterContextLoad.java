package com.github.oxal.annotation.context;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to be executed after the application context has been loaded.
 * The method must be public. It can have zero parameters or a single parameter of type {@link com.github.oxal.context.Context}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AfterContextLoad {
    /**
     * The order of execution. Lower values have higher priority.
     *
     * @return the order value
     */
    int order() default 100;
}
