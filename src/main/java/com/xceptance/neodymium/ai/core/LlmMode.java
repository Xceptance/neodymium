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
package com.xceptance.neodymium.ai.core;

/**
 * Defines the operational mode of an {@link LlmClient}, which controls
 * which temperature setting is read from {@code AiConfiguration}.
 *
 * <ul>
 *   <li>{@link #AGENT} — used during {@code @NeodymiumTest} execution;
 *       reads {@code neodymium.ai.temperature} (low / deterministic).</li>
 *   <li>{@link #GENERATOR} — used during {@code @NeodymiumTestGenerator} exploration;
 *       reads {@code neodymium.ai.generate.temperature} (high / creative).</li>
 * </ul>
  *
 * @author AI-generated: Gemini 2.5 Flash
*/
public enum LlmMode
{
    /** Standard AI agent mode for {@code @NeodymiumTest} test execution. */
    AGENT,

    /** Exploratory generator mode for {@code @NeodymiumTestGenerator} prompt generation. */
    GENERATOR,

    /** Pre-Execution Static Analysis Phase (PESAP) classification and linting mode. */
    PESAP
}

