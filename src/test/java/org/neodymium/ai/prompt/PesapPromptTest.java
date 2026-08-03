/*
 * Apache License 2.0
 *
 * Copyright (c) 2026 Xceptance
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neodymium.ai.prompt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.prompt.PesapPrompt.PesapResult;

/**
 * Unit tests validating prompt compilation and JSON response parsing in PesapPrompt.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class PesapPromptTest
{
    /**
     * Default constructor.
     */
    public PesapPromptTest()
    {
    }

    /**
     * Verifies that the prompt correctly compiles the flow context.
     */
    @Test
    public void testCompileUserMessage()
    {
        final PesapPrompt prompt = new PesapPrompt(
            "Click Submit button",
            "Fill in username",
            List.of("Verify order success", "Go back to catalog")
        );

        final String userMessage = prompt.compileUserMessage(new ExecutionContext(null));
        
        assertNotNull(userMessage);
        assertTrue(userMessage.contains("[PREVIOUS] Step: Fill in username"));
        assertTrue(userMessage.contains("[CURRENT]  Step: Click Submit button"));
        assertTrue(userMessage.contains("[NEXT]     Step: Verify order success"));
        // Should only contain at most 2 next steps as context, but we check first
        assertTrue(userMessage.contains("## Flow Context"));
    }

    /**
     * Verifies that the prompt successfully parses minified JSON responses from the LLM.
     */
    @Test
    public void testParseResponseSuccess() throws Exception
    {
        final String rawJson = """
            {
              "c": "AXTREE",
              "jm": false,
              "sp": ["Click Submit", "Verify success"]
            }
            """;

        final PesapPrompt prompt = new PesapPrompt("instruction", null, null);
        final PesapResult result = prompt.parseResponse(rawJson, new ExecutionContext(null));

        assertNotNull(result);
        assertEquals("AXTREE", result.contextLevel());
        assertFalse(result.requiresJavaMethods());
        assertEquals(2, result.splitSteps().size());
        assertEquals("Click Submit", result.splitSteps().get(0));
        assertEquals("Verify success", result.splitSteps().get(1));
    }

    /**
     * Verifies that the prompt parses fallback defaults on empty or invalid response.
     */
    @Test
    public void testParseResponseFallback() throws Exception
    {
        final PesapPrompt prompt = new PesapPrompt("instruction", null, null);
        final PesapResult result = prompt.parseResponse("", new ExecutionContext(null));

        assertNotNull(result);
        assertEquals("AXTREE", result.contextLevel());
        assertFalse(result.requiresJavaMethods());
        assertTrue(result.splitSteps().isEmpty());
    }

    /**
     * Verifies that context window bounds next steps to max 3 and truncates excessively long instructions.
     */
    @Test
    public void testCompileUserMessage_boundedContextAndLength()
    {
        final String longInstruction = "A".repeat(1000);
        final PesapPrompt prompt = new PesapPrompt(
            longInstruction,
            "Previous step",
            List.of("Next 1", "Next 2", "Next 3", "Next 4 overflow")
        );

        final String userMessage = prompt.compileUserMessage(new ExecutionContext(null));

        assertNotNull(userMessage);
        assertTrue(userMessage.contains("Next 1"));
        assertTrue(userMessage.contains("Next 2"));
        assertTrue(userMessage.contains("Next 3"));
        assertFalse(userMessage.contains("Next 4 overflow"), "Next step list should be bounded to 3 next steps.");
        assertFalse(userMessage.contains("A".repeat(600)), "Instruction should be truncated to prevent context bloat.");
    }
}
