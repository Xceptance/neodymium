/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
