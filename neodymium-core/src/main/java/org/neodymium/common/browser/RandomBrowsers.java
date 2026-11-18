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
