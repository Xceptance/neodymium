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
@Deprecated
public @interface Browser
{
    String value() default DEFAULT_BROWSER_ID;
}
