/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.ai.tool;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Documents a parameter on an {@link Tool}-annotated method, providing metadata
 * and descriptions for automatic JSON Schema generation.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ToolParam
{
    /**
     * Optional custom name for the parameter in the generated JSON Schema.
     * If omitted or blank, the reflective parameter name is used.
     *
     * @return parameter name
     */
    String name() default "";

    /**
     * Documentation explaining the parameter's meaning, expected format, or allowed values.
     *
     * @return parameter description
     */
    String description() default "";

    /**
     * Indicates whether this parameter is required in the tool invocation.
     * Defaults to true.
     *
     * @return true if required
     */
    boolean required() default true;
}
