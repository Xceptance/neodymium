package com.xceptance.neodymium.common.browser;

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
/**
 * This annotation defines whether or not the browser stays open after the execution of the annotated test is finished.
 * It can be used for test methods and test classes. 
 * If it's used for both, then the method level annotation overrides the class level annotation.
 * Also, if used, the method level annotation and the class level annotation both override the corresponding neodymium configuration property (neodymium.webDriver.keepBrowserOpenOnFailure).
 * 
 * <p>If it's set to "true", the browser of every annotated test which fails stays open.</p>
 * <p>If it's set to "false" (default), the browser of every annotated test stays open.</p>
 * <p>If the annotation isn't used, the neodymium configuration property applies.</p>
 */
public @interface KeepBrowserOpen
{
    boolean onlyOnFailure() default false;
}
