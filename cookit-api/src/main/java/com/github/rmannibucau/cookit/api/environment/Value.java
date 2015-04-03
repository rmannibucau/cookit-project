package com.github.rmannibucau.cookit.api.environment;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Qualifier
@Retention(RUNTIME)
@Target({ TYPE, METHOD, FIELD, PARAMETER })
public @interface Value {
    String EMPTY = "com.github.rmannibucau.cookit.api.environment.Value.EMPTY";

    /**
     * @return key of the parameter.
     */
    @Nonbinding
    String value() default "";

    /**
     * @return key of the parameter. Alias for value when used with or() for readability.
     */
    @Nonbinding
    String key() default "";

    /**
     * @return default value.
     */
    @Nonbinding
    String or() default EMPTY;
}
