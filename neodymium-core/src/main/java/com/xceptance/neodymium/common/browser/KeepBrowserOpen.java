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
@Deprecated
public @interface KeepBrowserOpen
{
    boolean onlyOnFailure() default false;
}
