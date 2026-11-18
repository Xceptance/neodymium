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

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Use this annotation to instantiate object with values stemming from the test data.
 * <p>
 * By default, the JSON objects or primitives with a name matching the name of the variable will be parsed to the
 * variable. In case there are no JSON objects that match the variable name found, the JSON objects or names that match
 * the fields of the variable will be used to instantiate the corresponding fields.
 * </p>
 * <p>
 * In case you would like to change the path to the JSON used for the variable instantiation, please pass the JSON path
 * to the corresponding JSON to the <b>value</b> of the annotation
 * </p>
 */
@Retention(RUNTIME)
@Target(
{
  ElementType.FIELD
})
public @interface DataItem
{
    /**
     * get JSON path to data item object
     * 
     * @return
     */
    String value() default "";
}
