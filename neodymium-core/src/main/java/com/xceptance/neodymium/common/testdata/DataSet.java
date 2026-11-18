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

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation can be used to limit and override data set execution for an entire class or a single method.
 * <p>
 * The <b>value</b> is an integer array that can either be a single index of a data set or a range of data set indices.
 * <p>
 * Default is {} which will not have any effect on execution unless there is a {@link SuppressDataSets} annotation
 * involved. In case a class is annotated with {@link SuppressDataSets} and a test method is annotated @DataSet()
 * then it will override suppression and enforce the method to run with <b>all</b> data sets
 * <p>
 * Examples:
 * <p>
 * {@code @DataSet(1)} -> executes only the first data set
 * {@code @DataSet({1, 5})} -> executes the first 5 data sets
 * </p>
 */
@Retention(RUNTIME)
@Target(
{
  TYPE, METHOD
})
@Repeatable(DataSets.class)
@Deprecated
public @interface DataSet
{
    /**
     * get data set index
     * 
     * @return
     */
    int[] value() default {};
    
    /**
     * get data set id
     * 
     * @return
     */
    String id() default "";
}
