package com.xceptance.neodymium.common.browser;

import static com.xceptance.neodymium.common.browser.configuration.MultibrowserConfiguration.DEFAULT_BROWSER_ID;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a class or method with {@link Browser} and add as annotation value a list of test targets (browsertag).
 * These browsertags refer to browser profiles that are configured in browser.properties located in config folder. See
 * <a href="https://github.com/Xceptance/neodymium/wiki/Multi-browser-support">Multi-browser-support</a> for examples on
 * the <a href="https://github.com/Xceptance/neodymium/wiki">github wiki</a>.
 * 
 * @author m.kaufmann
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(
{
  ElementType.TYPE, ElementType.METHOD
})
@Inherited
@Repeatable(Browsers.class)
public @interface Browser
{
    String value() default DEFAULT_BROWSER_ID;
}
