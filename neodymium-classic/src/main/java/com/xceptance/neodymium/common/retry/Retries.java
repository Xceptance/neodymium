package com.xceptance.neodymium.common.retry;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * <b>Do not use this annotation</b>. This annotation is just the wrapper for repeated {@link Retry} annotations.
 */
@Retention(RUNTIME)
@Target(
{
  TYPE, METHOD
})
@Inherited
public @interface Retries
{
    Retry[] value();
}
