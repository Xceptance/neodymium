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
import org.neodymium.ai.client.ResponseSchema;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;

/**
 * Unit tests validating {@link VisualRcaPrompt} compilation, SUT URL/title grounding,
 * null-safety, and response parsing.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class VisualRcaPromptTest
{
    @Test
    public void testVisualRcaPromptWithTwoArguments()
    {
        final VisualRcaPrompt prompt = new VisualRcaPrompt("Click checkout button", "ElementNotInteractableException");
        final String userMessage = prompt.compileUserMessage(null);

        Assertions.assertTrue(userMessage.contains("Failed Instruction: Click checkout button"));
        Assertions.assertTrue(userMessage.contains("Failure Details: ElementNotInteractableException"));
        Assertions.assertFalse(userMessage.contains("Current Page URL:"));
        Assertions.assertFalse(userMessage.contains("Current Page Title:"));
        Assertions.assertNull(prompt.getPageUrl());
        Assertions.assertNull(prompt.getPageTitle());
        Assertions.assertEquals(ResponseSchema.TEXT, prompt.getResponseSchema());
    }

    @Test
    public void testVisualRcaPromptWithFourArguments()
    {
        final VisualRcaPrompt prompt = new VisualRcaPrompt(
            "Assert subtotal is $31.98",
            "AssertionError: expected $31.98",
            "https://verla-store.internal/cart",
            "Shopping Cart - Verla"
        );
        final String userMessage = prompt.compileUserMessage(null);

        Assertions.assertTrue(userMessage.contains("Failed Instruction: Assert subtotal is $31.98"));
        Assertions.assertTrue(userMessage.contains("Failure Details: AssertionError: expected $31.98"));
        Assertions.assertTrue(userMessage.contains("Current Page URL: https://verla-store.internal/cart"));
        Assertions.assertTrue(userMessage.contains("Current Page Title: Shopping Cart - Verla"));
        Assertions.assertEquals("https://verla-store.internal/cart", prompt.getPageUrl());
        Assertions.assertEquals("Shopping Cart - Verla", prompt.getPageTitle());
    }

    @Test
    public void testVisualRcaPromptWithNullsAndBlanks()
    {
        final VisualRcaPrompt prompt = new VisualRcaPrompt(null, null, "   ", null);
        final String userMessage = prompt.compileUserMessage(null);

        Assertions.assertTrue(userMessage.contains("Failed Instruction: (Unknown instruction)"));
        Assertions.assertTrue(userMessage.contains("Failure Details: (No error message)"));
        Assertions.assertFalse(userMessage.contains("Current Page URL:"));
        Assertions.assertFalse(userMessage.contains("Current Page Title:"));
    }

    @Test
    public void testVisualRcaPromptResponseParsing()
    {
        final VisualRcaPrompt prompt = new VisualRcaPrompt("Verify cart", "TimeoutException");

        Assertions.assertEquals("Button is disabled", prompt.parseResponse("  Button is disabled  \n", null));
        Assertions.assertEquals("", prompt.parseResponse(null, null));
    }

    @Test
    public void testVisualRcaPromptSystemMessage()
    {
        final VisualRcaPrompt prompt = new VisualRcaPrompt("Verify cart", "TimeoutException");
        final ExecutionContext context = new ExecutionContext(new SessionData());
        final String systemMessage = prompt.compileSystemMessage(context);

        Assertions.assertNotNull(systemMessage);
        Assertions.assertTrue(systemMessage.contains("Visual Root Cause Analysis"));
        Assertions.assertTrue(systemMessage.contains("Zero Premise Bias"));
        Assertions.assertTrue(systemMessage.contains("Zero Confabulation"));
    }
}
