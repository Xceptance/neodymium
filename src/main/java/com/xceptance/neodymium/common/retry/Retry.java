package com.xceptance.neodymium.common.retry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark flaky tests that often fail due to the same error but for which there is a hope that they may
 * succeed on retry. </br>
 * Use it as the following: @Retry(maxNumberOfRetries = 4, exceptions = { "HERE" })</br>
 * </br>
 * It's possible to let the definition of maxNumberOfRetries out, then it will be set to the default value - 3. </br>
 * If you let out the definition of exceptions, test will be retried on every error
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(
{
  ElementType.TYPE, ElementType.METHOD
})
@Inherited
@Repeatable(Retries.class)
public @interface Retry
{
    int maxNumberOfRetries() default 3;

    String[] exceptions() default
    {
      ""
    };
}
