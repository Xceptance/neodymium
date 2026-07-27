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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for multi-dimensional prompt resolution in {@link AiAgentPrompts}.
 *
 * @author AI-generated: Gemini 3.5 Pro
 * @author Xceptance GmbH 2026
 */
public class AiAgentPromptsTest
{
    @BeforeEach
    public void setUp()
    {
        AiAgentPrompts.clearCache();
    }

    @Test
    public void testDefaultSelenideResolution()
    {
        final String prompt = AiAgentPrompts.getPrompt("system-prompt-rules.md", ExecutionEngine.SELENIDE, "gemini-2.5-flash");
        Assertions.assertNotNull(prompt);
        Assertions.assertFalse(prompt.isBlank());
    }

    @Test
    public void testPlaywrightEngineResolution()
    {
        final String prompt = AiAgentPrompts.getPrompt("system-prompt-rules.md", ExecutionEngine.PLAYWRIGHT, "gpt-4o");
        Assertions.assertNotNull(prompt);
        Assertions.assertFalse(prompt.isBlank());
    }

    @Test
    public void testExecutionEngineFromName()
    {
        Assertions.assertEquals(ExecutionEngine.SELENIDE, ExecutionEngine.fromName("selenide"));
        Assertions.assertEquals(ExecutionEngine.PLAYWRIGHT, ExecutionEngine.fromName("playwright"));
        Assertions.assertEquals(ExecutionEngine.SELENIDE, ExecutionEngine.fromName("unknown-engine"));
        Assertions.assertEquals(ExecutionEngine.SELENIDE, ExecutionEngine.fromName(null));
    }
}
