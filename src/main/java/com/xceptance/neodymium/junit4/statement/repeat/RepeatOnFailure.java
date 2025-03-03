package com.xceptance.neodymium.junit4.statement.repeat;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(
{
  TYPE, METHOD
})
public @interface RepeatOnFailure
{
    int value() default 1;
}
