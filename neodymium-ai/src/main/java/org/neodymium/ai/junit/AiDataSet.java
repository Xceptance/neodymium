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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Filter dataset parameterizations by testId using literal names and/or regex patterns.
 * Can be applied at class or method level, and is repeatable.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable(AiDataSets.class)
public @interface AiDataSet
{
    /**
     * Alias for {@link #include()} literal names or pattern filters.
     *
     * @return the inclusion filters
     */
    String[] value() default {};

    /**
     * Include dataset entries matching these literal testId names or regex patterns.
     *
     * @return the inclusion pattern filters
     */
    String[] include() default {};

    /**
     * Exclude dataset entries matching these literal testId names or regex patterns.
     *
     * @return the exclusion pattern filters
     */
    String[] exclude() default {};
}
