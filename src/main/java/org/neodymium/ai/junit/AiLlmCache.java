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
 * Opt-in annotation for Neodymium AI engine testing that enables in-memory
 * LLM response caching across annotated test methods in a test class execution.
 *
 * <p>When present on a test class or test method, identical prompt instructions return cached
 * responses instantly without making duplicate live LLM calls during internal testing.</p>
 *
 * <ul>
 *   <li><b>Class Scope:</b> When placed on a test class, the cache persists across all annotated methods in the class.</li>
 *   <li><b>Method Scope:</b> When placed on a single test method without class annotation, the cache is isolated strictly to that method.</li>
 * </ul>
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface AiLlmCache
{
}
