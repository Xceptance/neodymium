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
import org.junit.jupiter.api.TestTemplate;

/**
 * Annotation to define an inline YAML playbook or multi-line step prompt directly on a test method.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@TestTemplate
public @interface AiInlinePlaybook
{
    /**
     * The inline multi-line YAML playbook string or step prompt text block.
     *
     * @return the inline playbook content
     */
    String value();

    /**
     * Optional custom execution name identifier for logging/reporting.
     *
     * @return the name identifier
     */
    String name() default "";
}
