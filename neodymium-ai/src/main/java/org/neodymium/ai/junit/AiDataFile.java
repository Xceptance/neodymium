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
package org.neodymium.ai.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares an external test data file (YAML or JSON) to bind to an AI test.
 * <p>
 * When applied to a programmatic test (e.g. {@code @AiPlaybook(AiPlaybook.PROGRAMMATIC)}),
 * the data file provides datasets, inclusions, and prompt add-ons, while any {@code steps:}
 * block in the file is ignored so that steps remain exclusively driven by Java code.
 * </p>
 * <p>
 * Can be declared at the test class level or on individual test methods.
 * </p>
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AiDataFile
{
    /**
     * The path to the test data file (package-relative or absolute from classpath root with a leading slash).
     * If empty, the runner attempts convention auto-discovery (e.g. {@code <TestClassName>.yaml}).
     *
     * @return the test data file path
     */
    String value() default "";
}
