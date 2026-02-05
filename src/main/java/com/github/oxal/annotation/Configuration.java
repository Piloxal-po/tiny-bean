package com.github.oxal.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a class is a configuration class.
 * <p>
 * Configuration classes are automatically registered as beans.
 * Their fields are populated from the application properties file based on the specified prefix.
 * <p>
 * Nested objects are supported for recursive configuration.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Bean
public @interface Configuration {

    /**
     * The prefix of the properties to bind to this configuration class.
     * Example: "server" will map "server.port" to the "port" field.
     */
    String prefix() default "";
}
