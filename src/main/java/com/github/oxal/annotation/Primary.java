package com.github.oxal.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a bean should be given preference when multiple candidates
 * are qualified to autowire a single-valued dependency. If exactly one
 * 'primary' bean exists among the candidates, it will be the autowired value.
 *
 * <p>This annotation can be used on classes annotated with {@link Bean} or on
 * methods annotated with {@link Bean}.
 *
 * <p><b>NOTE:</b> If multiple beans of the same type are marked as {@code @Primary},
 * the container will throw an exception, as the ambiguity cannot be resolved.
 *
 * @see Qualifier
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Primary {
}
