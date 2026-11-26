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
package org.neodymium.common.browser;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * By default the methods annotated with @{@code After} in JUnit4 and with @{@code AfterEach} in Junit5 are executed in
 * the same browser. If it'required to start new browser for one of after methods, please, annotate it
 * with @StartNewBrowserForCleanUp. In case all after methods need to be executed in separate browser each, annotate the
 * whole class with @StartNewBrowserForCleanUp. If all after methods, except for one require a new browser, annotate the
 * class with @StartNewBrowserForCleanUp and the one method that doesn't need new browser with @SuppressBrowsers
 */
@Retention(RUNTIME)
@Target(
{
  TYPE, METHOD
})
public @interface StartNewBrowserForCleanUp
{
}
