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
package org.neodymium.ai.prompt;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.pipeline.ExecutionContext;

/**
 * Unit tests for {@link ActionExtractionPrompt}.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class ActionExtractionPromptTest
{
    @Test
    public void testCompileUserMessageIncludesPreviousFailureWhenPresent()
    {
        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final ExecutionContext context = new ExecutionContext(null);

        context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Verify order total");
        context.getTransientData().put(ExecutionContext.KEY_LAST_EXECUTION_ERROR, new RuntimeException("Element not found: text=Total Paid"));

        final String userMessage = prompt.compileUserMessage(context);

        Assertions.assertTrue(userMessage.contains("PREVIOUS ATTEMPT FAILURE"));
        Assertions.assertTrue(userMessage.contains("Element not found: text=Total Paid"));
        Assertions.assertTrue(userMessage.contains("Instruction: Verify order total"));
    }

    @Test
    public void testCompileUserMessageOmitsPreviousFailureWhenAbsent()
    {
        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final ExecutionContext context = new ExecutionContext(null);

        context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Verify order total");

        final String userMessage = prompt.compileUserMessage(context);

        Assertions.assertFalse(userMessage.contains("PREVIOUS ATTEMPT FAILURE"));
        Assertions.assertTrue(userMessage.contains("Instruction: Verify order total"));
    }
}
