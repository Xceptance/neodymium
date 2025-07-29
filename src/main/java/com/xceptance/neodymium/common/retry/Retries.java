package com.xceptance.neodymium.common.retry;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

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
