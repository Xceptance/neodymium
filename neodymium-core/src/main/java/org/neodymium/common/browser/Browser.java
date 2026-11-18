/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.neodymium.common.browser;

import static org.neodymium.common.browser.configuration.MultibrowserConfiguration.DEFAULT_BROWSER_ID;
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
