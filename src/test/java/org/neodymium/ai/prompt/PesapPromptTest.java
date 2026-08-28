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
     * Verifies that the prompt correctly compiles the active instruction.
     */
    @Test
    public void testCompileUserMessage()
    {
        final PesapPrompt prompt = new PesapPrompt("Click Submit button");
        final String userMessage = prompt.compileUserMessage(new ExecutionContext(null));
        
        assertNotNull(userMessage);
        assertTrue(userMessage.contains("## Active Instruction"));
        assertTrue(userMessage.contains("Click Submit button"));
    }

    /**
     * Verifies that the prompt successfully parses minified JSON responses from the LLM.
     */
    @Test
    public void testParseResponseSuccess() throws Exception
    {
        final String rawJson = """
            {
              "c": "LEAN",
              "jm": false,
              "sp": ["Click Submit", "Verify success"]
            }
            """;

        final PesapPrompt prompt = new PesapPrompt("instruction");
        final PesapResult result = prompt.parseResponse(rawJson, new ExecutionContext(null));

        assertNotNull(result);
        assertEquals("LEAN", result.contextLevel());
        assertFalse(result.requiresJavaMethods());
        assertEquals(2, result.splitSteps().size());
        assertEquals("Click Submit", result.splitSteps().get(0));
        assertEquals("Verify success", result.splitSteps().get(1));
    }

    /**
     * Verifies that the prompt successfully parses VISUAL context level from minified JSON response.
     */
    @Test
    public void testParseResponseVisualMinimal() throws Exception
    {
        final String rawJson = """
            {
              "c": "VISUAL",
              "jm": false
            }
            """;

        final PesapPrompt prompt = new PesapPrompt("There is a green checkmark (visual)");
        final PesapResult result = prompt.parseResponse(rawJson, new ExecutionContext(null));

        assertNotNull(result);
        assertEquals("VISUAL", result.contextLevel());
        assertFalse(result.requiresJavaMethods());
        assertTrue(result.splitSteps().isEmpty());
    }

    /**
     * Verifies that the prompt parses fallback defaults on empty or invalid response.
     */
    @Test
    public void testParseResponseFallback() throws Exception
    {
        final PesapPrompt prompt = new PesapPrompt("instruction");
        final PesapResult result = prompt.parseResponse("", new ExecutionContext(null));

        assertNotNull(result);
        assertEquals("LEAN", result.contextLevel());
        assertFalse(result.requiresJavaMethods());
        assertTrue(result.splitSteps().isEmpty());
    }

    /**
     * Verifies that excessively long instructions are truncated.
     */
    @Test
    public void testCompileUserMessage_truncatesExcessiveLength()
    {
        final String longInstruction = "A".repeat(1000);
        final PesapPrompt prompt = new PesapPrompt(longInstruction);
        final String userMessage = prompt.compileUserMessage(new ExecutionContext(null));

        assertNotNull(userMessage);
        assertTrue(userMessage.contains("## Active Instruction"));
        assertTrue(userMessage.contains("[TRUNCATED]"));
        assertFalse(userMessage.contains("A".repeat(600)), "Instruction should be truncated to prevent context bloat.");
    }

    /**
     * Verifies that the system message properly includes language-agnostic multi-action step splitting instructions.
     */
    @Test
    public void testCompileSystemMessage_containsLanguageAgnosticSplittingRules()
    {
        final PesapPrompt prompt = new PesapPrompt("Open the country selector and click \"Poland\"");
        final String systemMessage = prompt.compileSystemMessage(new ExecutionContext(null));

        assertNotNull(systemMessage);
        assertTrue(systemMessage.contains("Step Splitting ('sp')"));
        assertTrue(systemMessage.contains("Sequential multi-action interaction chains requiring intermediate UI state changes"));
        assertTrue(systemMessage.contains("Language & Token Preservation"));
    }
}
