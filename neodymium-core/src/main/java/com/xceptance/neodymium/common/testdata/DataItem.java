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
@Deprecated
public @interface DataItem
{
    /**
     * get JSON path to data item object
     * 
     * @return
     */
    String value() default "";
}
