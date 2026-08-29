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

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.neodymium.ai.model.SemanticIntent;
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
              "i": "CLICK",
              "sp": ["Click Submit", "Verify success"]
            }
            """;

        final PesapPrompt prompt = new PesapPrompt("instruction");
        final PesapResult result = prompt.parseResponse(rawJson, new ExecutionContext(null));

        assertNotNull(result);
        assertEquals("LEAN", result.contextLevel());
        assertFalse(result.requiresJavaMethods());
        assertEquals(SemanticIntent.CLICK, result.intent());
        assertEquals(2, result.splitSteps().size());
        assertEquals("Click Submit", result.splitSteps().get(0));
        assertEquals("Verify success", result.splitSteps().get(1));
    }

    /**
     * Verifies that the prompt successfully parses assertion and metadata intents.
     */
    @Test
    public void testParseResponseIntents() throws Exception
    {
        final PesapPrompt prompt = new PesapPrompt("instruction");

        final String assertJson = """
            {"c":"MINIMAL","jm":false,"i":"ASSERT"}
            """;
        final PesapResult assertResult = prompt.parseResponse(assertJson, new ExecutionContext(null));
        assertEquals(SemanticIntent.ASSERT, assertResult.intent());
        assertEquals("STANDARD", assertResult.contextLevel());
        assertTrue(assertResult.intent().isAssertion());

        final String metaJson = """
            {"c":"STANDARD","jm":false,"i":"ASSERT_METADATA"}
            """;
        final PesapResult metaResult = prompt.parseResponse(metaJson, new ExecutionContext(null));
        assertEquals(SemanticIntent.ASSERT_METADATA, metaResult.intent());
        assertEquals("MINIMAL", metaResult.contextLevel());
        assertTrue(metaResult.intent().isAssertion());

        final String typeJson = """
            {"c":"LEAN","jm":false,"i":"TYPE"}
            """;
        final PesapResult typeResult = prompt.parseResponse(typeJson, new ExecutionContext(null));
        assertEquals(SemanticIntent.TYPE, typeResult.intent());
        assertEquals("LEAN", typeResult.contextLevel());
        assertTrue(typeResult.intent().isMutating());
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
              "jm": false,
              "i": "ASSERT"
            }
            """;

        final PesapPrompt prompt = new PesapPrompt("There is a green checkmark (visual)");
        final PesapResult result = prompt.parseResponse(rawJson, new ExecutionContext(null));

        assertNotNull(result);
        assertEquals("VISUAL", result.contextLevel());
        assertFalse(result.requiresJavaMethods());
        assertEquals(SemanticIntent.ASSERT, result.intent());
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
        assertNull(result.intent());
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
        assertTrue(systemMessage.contains("Semantic Intent ('i')"));
        assertTrue(systemMessage.contains("Sequential multi-action interaction chains requiring intermediate UI state changes"));
        assertTrue(systemMessage.contains("Complete Action Invariant"));
    }

    /**
     * Verifies that the prompt successfully parses intent from simulated responses for multilingual instructions.
     */
    @Test
    public void testParseResponseMultilingualIntents() throws Exception
    {
        final PesapPrompt prompt = new PesapPrompt("test");
        final ExecutionContext ctx = new ExecutionContext(null);

        // French: "Vérifier que le total du panier est 99,00 €" -> ASSERT
        final String frJson = """
            {"c":"LEAN","jm":false,"i":"ASSERT"}
            """;
        final PesapResult frResult = prompt.parseResponse(frJson, ctx);
        assertEquals(SemanticIntent.ASSERT, frResult.intent());
        assertEquals("STANDARD", frResult.contextLevel());
        assertTrue(frResult.intent().isAssertion());

        // German: "Klicken Sie auf den Button 'In den Warenkorb'" -> CLICK
        final String deJson = """
            {"c":"LEAN","jm":false,"i":"CLICK"}
            """;
        final PesapResult deResult = prompt.parseResponse(deJson, ctx);
        assertEquals(SemanticIntent.CLICK, deResult.intent());
        assertTrue(deResult.intent().isMutating());

        // Japanese: "タイトルが 'マイストア' であることを確認する" -> ASSERT_METADATA
        final String jpJson = """
            {"c":"MINIMAL","jm":false,"i":"ASSERT_METADATA"}
            """;
        final PesapResult jpResult = prompt.parseResponse(jpJson, ctx);
        assertEquals(SemanticIntent.ASSERT_METADATA, jpResult.intent());
        assertTrue(jpResult.intent().isAssertion());

        // English: "Select 'Express Shipping' from dropdown" -> SELECT
        final String enJson = """
            {"c":"LEAN","jm":false,"i":"SELECT"}
            """;
        final PesapResult enResult = prompt.parseResponse(enJson, ctx);
        assertEquals(SemanticIntent.SELECT, enResult.intent());
        assertTrue(enResult.intent().isMutating());
    }
}
