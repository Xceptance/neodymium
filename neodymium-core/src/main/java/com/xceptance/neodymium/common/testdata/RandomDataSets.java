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
package com.xceptance.neodymium.common.testdata;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotate a class or method with {@link RandomDataSets} to randomly select data sets for the test execution.
 * <p>
 * The <b>value</b> defines the number of randomly picked data sets. Please pay attention to the fact, that the value
 * should not exceed the number of available data sets, otherwise the exception will be thrown. Note: if the test is
 * also annotated with {@link DataSet} annotations, the random data sets will be selected among the ones mentioned in
 * that annotations
 * <p>
 * Default is 1 which means the test will be executed for the single random data set.
 * <p>
 * Any value below 1 will deactivate this annotation.
 * 
 * @author o.omelianchuk
 */
@Retention(RUNTIME)
@Target(
{
  TYPE, METHOD
})
@Deprecated
public @interface RandomDataSets
{
    int value() default 1;
}
