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

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.client.ResponseSchema;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.steps.AgentToolLoopStep;
import org.neodymium.ai.tool.ToolCall;

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
        Assertions.assertEquals(ResponseSchema.ASSERTION, prompt.getResponseSchema());
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

        final String json = """
            {
              "rubrics": {
                "targetPresence": { "analysis": "Total element missing", "score": "MISSING" },
                "formValidation": { "analysis": "Province field is empty with validation tooltip", "score": "ERROR_PRESENT" },
                "flowState": { "analysis": "Still on checkout page", "score": "STUCK" },
                "obstruction": { "analysis": "No modal dialog", "score": "CLEAR" }
              },
              "rootCause": "Checkout halted due to missing province field."
            }
            """;

        final VisualRcaResult parsed = prompt.parseResponse(json, null);
        Assertions.assertNotNull(parsed);
        Assertions.assertEquals("Checkout halted due to missing province field.", parsed.getRootCause());
        Assertions.assertNotNull(parsed.getRubrics());
        Assertions.assertEquals("MISSING", parsed.getRubrics().targetPresence().score());
        Assertions.assertEquals("ERROR_PRESENT", parsed.getRubrics().formValidation().score());
        Assertions.assertEquals("STUCK", parsed.getRubrics().flowState().score());
        Assertions.assertEquals("CLEAR", parsed.getRubrics().obstruction().score());
        Assertions.assertTrue(parsed.toFormattedDiagnosis().contains("Root Cause Analysis"));

        final VisualRcaResult fallback = prompt.parseResponse("  Button is disabled  \n", null);
        Assertions.assertNotNull(fallback);
        Assertions.assertEquals("Button is disabled", fallback.getRootCause());
        Assertions.assertNull(fallback.getRubrics());

        final VisualRcaResult empty = prompt.parseResponse(null, null);
        Assertions.assertNotNull(empty);
        Assertions.assertTrue(empty.getRootCause().contains("No RCA diagnosis"));
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

    @Test
    public void testVisualRcaPromptWithExplicitToolInteractions()
    {
        final VisualRcaPrompt prompt = new VisualRcaPrompt(
            "Assert total",
            "AssertionError: expected $31.98",
            "https://example.com/checkout",
            "Checkout",
            "1. inspect(selector=\"#total\")\n2. assert_text(selector=\"#total\", expectedText=\"$31.98\")"
        );
        Assertions.assertEquals("1. inspect(selector=\"#total\")\n2. assert_text(selector=\"#total\", expectedText=\"$31.98\")", prompt.getRecentToolInteractions());

        final String userMessage = prompt.compileUserMessage(null);
        Assertions.assertTrue(userMessage.contains("## Recent Tool Interactions in this Step:"));
        Assertions.assertTrue(userMessage.contains("1. inspect(selector=\"#total\")"));
        Assertions.assertTrue(userMessage.contains("2. assert_text(selector=\"#total\", expectedText=\"$31.98\")"));
    }

    @Test
    public void testVisualRcaPromptExtractsToolInteractionsFromContext()
    {
        final VisualRcaPrompt prompt = new VisualRcaPrompt("Click submit", "TimeoutException");
        final ExecutionContext context = new ExecutionContext(new SessionData());
        final ObjectMapper mapper = new ObjectMapper();
        final ObjectNode args = mapper.createObjectNode();
        args.put("selector", "button.submit");
        final ToolCall call = new ToolCall("call-1", "click", args);

        context.getTransientData().put(AgentToolLoopStep.KEY_EXECUTED_TOOL_CALLS, List.of(call));

        final String userMessage = prompt.compileUserMessage(context);
        Assertions.assertTrue(userMessage.contains("## Recent Tool Interactions in this Step:"));
        Assertions.assertTrue(userMessage.contains("1. click(selector=\"button.submit\")"));
    }

    @Test
    public void testFromFormattedDiagnosis()
    {
        Assertions.assertNull(VisualRcaResult.fromFormattedDiagnosis(null));
        Assertions.assertNull(VisualRcaResult.fromFormattedDiagnosis("   "));

        final String simple = "Element was not visible within 5 seconds";
        final VisualRcaResult simpleResult = VisualRcaResult.fromFormattedDiagnosis(simple);
        Assertions.assertNotNull(simpleResult);
        Assertions.assertEquals(simple, simpleResult.getRootCause());
        Assertions.assertNull(simpleResult.getRubrics());

        final String markdown = """
            **Root Cause Analysis:**

            The test failed due to an incorrect locator selector ({#selectLanguage}). English language options are visibly present and accessible.

            ### Diagnostic Rubrics
            - **Target Presence Check:** `[FOUND]` The target language option is visibly rendered on the page in multiple locations.
            - **Form & Validation Check:** `[CLEAN]` No validation bubbles or errors are present.
            - **Navigation & Flow State:** `[STUCK]` The test execution halted on landing page.
            - **Action Obstruction Check:** `[CLEAR]` The user interface is completely visible and unobstructed.
            """;

        final VisualRcaResult parsed = VisualRcaResult.fromFormattedDiagnosis(markdown);
        Assertions.assertNotNull(parsed);
        Assertions.assertEquals("The test failed due to an incorrect locator selector ({#selectLanguage}). English language options are visibly present and accessible.", parsed.getRootCause());
        Assertions.assertNotNull(parsed.getRubrics());
        Assertions.assertEquals("FOUND", parsed.getRubrics().targetPresence().score());
        Assertions.assertTrue(parsed.getRubrics().targetPresence().analysis().contains("visibly rendered"));
        Assertions.assertEquals("CLEAN", parsed.getRubrics().formValidation().score());
        Assertions.assertEquals("STUCK", parsed.getRubrics().flowState().score());
        Assertions.assertEquals("CLEAR", parsed.getRubrics().obstruction().score());

        // Round-trip check
        final String reformatted = parsed.toFormattedDiagnosis();
        final VisualRcaResult roundTrip = VisualRcaResult.fromFormattedDiagnosis(reformatted);
        Assertions.assertNotNull(roundTrip);
        Assertions.assertEquals(parsed.getRootCause(), roundTrip.getRootCause());
        Assertions.assertEquals(parsed.getRubrics().targetPresence().score(), roundTrip.getRubrics().targetPresence().score());
        Assertions.assertEquals(parsed.getRubrics().flowState().score(), roundTrip.getRubrics().flowState().score());
    }
}
