package com.xceptance.neodymium.common.retry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(
{
  ElementType.TYPE, ElementType.METHOD
})
@Inherited
@Repeatable(Retries.class)
public @interface Retry
{
    int value() default 3;

    String[] exceptions() default {};
}
