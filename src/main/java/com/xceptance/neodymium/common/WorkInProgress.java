package com.xceptance.neodymium.common;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Works like the opposite of {@code @Ignore}. If tests in a class are annotated with {@code @WorkInProgress}, only
 * those tests will be executed. All other tests of the class, not being annotated with {@code @WorkInProgress}, get
 * skipped. The {@code @WorkInProgress} annotation is per default disabled but can be enabled via Neodymium property
 * "workInProgress".
 */
@Retention(RUNTIME)
@Target(
{
  METHOD
})
public @interface WorkInProgress
{
}
