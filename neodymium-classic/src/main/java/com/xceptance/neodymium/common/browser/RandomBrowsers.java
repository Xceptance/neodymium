package com.xceptance.neodymium.common.browser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a class or method with {@link RandomBrowsers} to randomly select browsers for the test execution. The random
 * browsers will be picked among the ones, mentioned with {@link Browser} in the method or class (or super class)
 * <p>
 * The <b>value</b> defines the number of randomly picked browsers. Please pay attention to the fact, that the value
 * should be lower or equal to the number of browsers, annotated with {@link Browser} for the target method or class.
 * <p>
 * Default is 1 which means the test will be executed for the single random browser.
 * <p>
 * Any value below 1 will deactivate this annotation.
 * 
 * @author o.omelianchuk
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(
{
  ElementType.TYPE, ElementType.METHOD
})
@Inherited
public @interface RandomBrowsers
{
    int value() default 1;
}
