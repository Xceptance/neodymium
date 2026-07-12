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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.client.ResponseSchema;
import org.neodymium.ai.pipeline.ExecutionContext;

/**
 * TDD test suite validating prompt formatting builders, raw markdown JSON stripping,
 * missing brace recovery repairing, and GSON deserialization routing.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class PromptAndRepairTest
{
    /**
     * Test record DTO used for JSON serialization and repair verifications.
     *
     * @param status the outcome status string
     * @param description the outcome reasoning description
     */
    public static record TestDto(String status, String description)
    {
    }

    /**
     * Concrete test implementation of {@link AiPrompt} for validation.
     */
    private static final class TestPrompt implements AiPrompt<TestDto>
    {
        /**
         * Constructs a TestPrompt.
         */
        TestPrompt()
        {
        }

        @Override
        public ResponseSchema getResponseSchema()
        {
            return ResponseSchema.TEXT;
        }

        @Override
        public String compileSystemMessage(final ExecutionContext context)
        {
            return "system instructions";
        }

        @Override
        public String compileUserMessage(final ExecutionContext context)
        {
            return "user query";
        }

        @Override
        public TestDto parseResponse(final String rawContent, final ExecutionContext context) throws Exception
        {
            return ResponseRepairService.deserialize(rawContent, TestDto.class);
        }
    }

    /**
     * Constructs a default test instance.
     */
    public PromptAndRepairTest()
    {
    }

    /**
     * Verifies PromptBuilderService applies correct model-specific instructions wraps.
     */
    @Test
    public void testPromptBuilderServiceFormatting()
    {
        final String rawPrompt = "Write a test script";

        // 1. Gemini / Default (No wrapper tags)
        assertEquals(rawPrompt, PromptBuilderService.format("gemini-3.5-flash", rawPrompt, false));
        assertEquals(rawPrompt, PromptBuilderService.format("openai-gpt4", rawPrompt, true));

        // 2. Mistral wrapping
        assertEquals("[INST] Write a test script [/INST]", PromptBuilderService.format("mistral-large", rawPrompt, false));
        assertEquals("<<SYS>>\nWrite a test script\n<</SYS>>", PromptBuilderService.format("mistral-medium", rawPrompt, true));
    }

    /**
     * Verifies that ResponseRepairService strips code fences and repairs missing braces.
     */
    @Test
    public void testResponseRepairServiceRawStrings()
    {
        // 1. Balanced JSON wrapped in markdown fences
        final String fencedJson = """
            ```json
            {"status": "success", "description": "OK"}
            ```
            """;
        assertEquals("{\"status\": \"success\", \"description\": \"OK\"}", ResponseRepairService.repairJson(fencedJson));

        // 2. Unbalanced JSON (missing closing braces)
        final String brokenJson = "{\"status\": \"success\", \"description\": \"OK\"";
        assertEquals("{\"status\": \"success\", \"description\": \"OK\"}", ResponseRepairService.repairJson(brokenJson));

        // 3. Unbalanced JSON inside a markdown code block
        final String brokenFencedJson = """
            ```
            {"status": "success", "description": "OK"
            ```
            """;
        assertEquals("{\"status\": \"success\", \"description\": \"OK\"}", ResponseRepairService.repairJson(brokenFencedJson));
    }

    /**
     * Verifies deserializing repaired JSON payload.
     *
     * @throws Exception if parsing fails
     */
    @Test
    public void testAiPromptResponseDeserialization() throws Exception
    {
        final String rawOutput = """
            ```json
            {"status": "passed", "description": "System matches spec"
            ```
            """;

        final TestPrompt prompt = new TestPrompt();
        final TestDto dto = prompt.parseResponse(rawOutput, null);

        assertNotNull(dto);
        assertEquals("passed", dto.status());
        assertEquals("System matches spec", dto.description());
    }

    /**
     * Verifies that deserialization of invalid JSON throws IOException.
     */
    @Test
    public void testInvalidJsonDeserializationFailure()
    {
        final String invalidJson = "invalid JSON structure {";
        assertThrows(IOException.class, () -> {
            ResponseRepairService.deserialize(invalidJson, TestDto.class);
        });
    }
}
