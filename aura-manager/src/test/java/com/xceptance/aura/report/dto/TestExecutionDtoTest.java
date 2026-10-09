/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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
package com.xceptance.aura.report.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TestExecutionDto} deserialization and findings getters.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class TestExecutionDtoTest
{
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testDeserializationWithLinterAndPostFlightFindings() throws Exception
    {
        final String json = """
            {
                "id": "exec-1",
                "testClass": "com.xceptance.test.DemoTest",
                "testMethod": "testLogin",
                "title": "US Login",
                "status": "passed-clean",
                "linterFindings": [
                    {
                        "stepIndex": 5,
                        "lineNumber": 5,
                        "category": "VAGUE_TARGET",
                        "severity": "WARNING",
                        "message": "Target description is overly ambiguous.",
                        "rawInstruction": "click on \\"Log In\\"",
                        "resolvedInstruction": "click on \\"Log In\\"",
                        "suggestedRewrite": "click the \\"Log In\\" button in the top navigation bar",
                        "scope": "PRE_FLIGHT"
                    }
                ],
                "postFlightFindings": [
                    {
                        "stepIndex": 6,
                        "lineNumber": 6,
                        "category": "SLOW_STEP",
                        "severity": "INFO",
                        "message": "Step took 4,210 ms to stabilize and execute.",
                        "rawInstruction": "wait for search input",
                        "resolvedInstruction": "wait for search input",
                        "suggestedRewrite": "assert search input is visible",
                        "scope": "POST_FLIGHT"
                    }
                ]
            }
            """;

        final TestExecutionDto dto = objectMapper.readValue(json, TestExecutionDto.class);

        Assertions.assertNotNull(dto);
        Assertions.assertEquals("exec-1", dto.getId());
        Assertions.assertEquals("com.xceptance.test.DemoTest", dto.getTestClass());

        final JsonNode linterFindings = dto.getLinterFindings();
        Assertions.assertNotNull(linterFindings);
        Assertions.assertTrue(linterFindings.isArray());
        Assertions.assertEquals(1, linterFindings.size());
        Assertions.assertEquals("VAGUE_TARGET", linterFindings.get(0).path("category").asText());
        Assertions.assertEquals("WARNING", linterFindings.get(0).path("severity").asText());
        Assertions.assertEquals("click on \"Log In\"", linterFindings.get(0).path("rawInstruction").asText());
        Assertions.assertEquals("click the \"Log In\" button in the top navigation bar", linterFindings.get(0).path("suggestedRewrite").asText());

        final JsonNode postFlightFindings = dto.getPostFlightFindings();
        Assertions.assertNotNull(postFlightFindings);
        Assertions.assertTrue(postFlightFindings.isArray());
        Assertions.assertEquals(1, postFlightFindings.size());
        Assertions.assertEquals("SLOW_STEP", postFlightFindings.get(0).path("category").asText());
        Assertions.assertEquals("INFO", postFlightFindings.get(0).path("severity").asText());
        Assertions.assertEquals("assert search input is visible", postFlightFindings.get(0).path("suggestedRewrite").asText());

        // Verify JSON serialization ensures true JSON array output (not POJO metadata)
        final String serializedJson = objectMapper.writeValueAsString(dto);
        final JsonNode serializedTree = objectMapper.readTree(serializedJson);

        final JsonNode serializedLinter = serializedTree.path("linterFindings");
        Assertions.assertTrue(serializedLinter.isArray(), "linterFindings must serialize as a JSON array, got: " + serializedLinter);
        Assertions.assertEquals(1, serializedLinter.size());
        Assertions.assertEquals("VAGUE_TARGET", serializedLinter.get(0).path("category").asText());

        final JsonNode serializedPostFlight = serializedTree.path("postFlightFindings");
        Assertions.assertTrue(serializedPostFlight.isArray(), "postFlightFindings must serialize as a JSON array, got: " + serializedPostFlight);
        Assertions.assertEquals(1, serializedPostFlight.size());
        Assertions.assertEquals("SLOW_STEP", serializedPostFlight.get(0).path("category").asText());

        Assertions.assertTrue(dto.getLinterFindingsJson().contains("\"VAGUE_TARGET\""));
        Assertions.assertTrue(dto.getPostFlightFindingsJson().contains("\"SLOW_STEP\""));
    }

    @Test
    public void testDeserializationWithoutFindings() throws Exception
    {
        final String json = """
            {
                "id": "exec-clean",
                "testClass": "com.xceptance.test.DemoTest",
                "status": "passed-clean"
            }
            """;

        final TestExecutionDto dto = objectMapper.readValue(json, TestExecutionDto.class);

        Assertions.assertNotNull(dto);
        Assertions.assertEquals("exec-clean", dto.getId());
        Assertions.assertNull(dto.getLinterFindings());
        Assertions.assertNull(dto.getPostFlightFindings());
    }

    @Test
    public void testDefaultConstructorFindingsAreNull()
    {
        final TestExecutionDto dto = new TestExecutionDto();

        Assertions.assertNull(dto.getLinterFindings());
        Assertions.assertNull(dto.getPostFlightFindings());
    }

    @Test
    public void testDisplayModeMappings() throws Exception
    {
        // 1. REPLAY_WITH_HEALING with no healing -> PLAYBOOK
        final String jsonPlaybook = """
            {
                "id": "exec-pb",
                "testClass": "com.xceptance.test.DemoTest",
                "executionMode": "REPLAY_WITH_HEALING",
                "healed": false
            }
            """;
        final TestExecutionDto dtoPlaybook = objectMapper.readValue(jsonPlaybook, TestExecutionDto.class);
        Assertions.assertEquals("PLAYBOOK", dtoPlaybook.getDisplayMode());

        // 2. REPLAY_WITH_HEALING with healing -> HEALED
        final String jsonHealed = """
            {
                "id": "exec-healed",
                "testClass": "com.xceptance.test.DemoTest",
                "executionMode": "REPLAY_WITH_HEALING",
                "healed": true
            }
            """;
        final TestExecutionDto dtoHealed = objectMapper.readValue(jsonHealed, TestExecutionDto.class);
        Assertions.assertEquals("HEALED", dtoHealed.getDisplayMode());

        // 3. LLM_RECORDING -> AI
        final String jsonAi = """
            {
                "id": "exec-ai",
                "testClass": "com.xceptance.test.DemoTest",
                "executionMode": "LLM_RECORDING"
            }
            """;
        final TestExecutionDto dtoAi = objectMapper.readValue(jsonAi, TestExecutionDto.class);
        Assertions.assertEquals("AI", dtoAi.getDisplayMode());

        // 4. No mode specified -> JAVA
        final String jsonJava = """
            {
                "id": "exec-java",
                "testClass": "com.xceptance.test.DemoTest"
            }
            """;
        final TestExecutionDto dtoJava = objectMapper.readValue(jsonJava, TestExecutionDto.class);
        Assertions.assertEquals("JAVA", dtoJava.getDisplayMode());

        // 5. REPLAY_STRICT -> PLAYBOOK
        final String jsonStrict = """
            {
                "id": "exec-strict",
                "testClass": "com.xceptance.test.DemoTest",
                "executionMode": "REPLAY_STRICT"
            }
            """;
        final TestExecutionDto dtoStrict = objectMapper.readValue(jsonStrict, TestExecutionDto.class);
        Assertions.assertEquals("PLAYBOOK", dtoStrict.getDisplayMode());

        // 6. FORCE_RECORDING / LLM_ONLY -> AI
        final String jsonForce = """
            {
                "id": "exec-force",
                "testClass": "com.xceptance.test.DemoTest",
                "executionMode": "FORCE_RECORDING"
            }
            """;
        final TestExecutionDto dtoForce = objectMapper.readValue(jsonForce, TestExecutionDto.class);
        Assertions.assertEquals("AI", dtoForce.getDisplayMode());
    }

    @Test
    public void testAiDrivenAndStatusPredicates()
    {
        final TestExecutionDto execPassAi = new TestExecutionDto();
        execPassAi.setStatus("passed");
        execPassAi.setExecutionMode("LLM_RECORDING");

        Assertions.assertTrue(execPassAi.isAiDriven());
        Assertions.assertTrue(execPassAi.isPassAi());
        Assertions.assertFalse(execPassAi.isFixedAi());
        Assertions.assertFalse(execPassAi.isKnownAi());
        Assertions.assertFalse(execPassAi.isUnknownAi());

        final TestExecutionDto execFixedAi = new TestExecutionDto();
        execFixedAi.setStatus("succeeded-fixed");
        execFixedAi.setExecutionMode("LLM_RECORDING");

        Assertions.assertTrue(execFixedAi.isAiDriven());
        Assertions.assertFalse(execFixedAi.isPassAi());
        Assertions.assertTrue(execFixedAi.isFixedAi());
        Assertions.assertFalse(execFixedAi.isKnownAi());
        Assertions.assertFalse(execFixedAi.isUnknownAi());

        final TestExecutionDto execKnownAi = new TestExecutionDto();
        execKnownAi.setStatus("failed-known");
        execKnownAi.setExecutionMode("FORCE_RECORDING");

        Assertions.assertTrue(execKnownAi.isAiDriven());
        Assertions.assertFalse(execKnownAi.isPassAi());
        Assertions.assertFalse(execKnownAi.isFixedAi());
        Assertions.assertTrue(execKnownAi.isKnownAi());
        Assertions.assertFalse(execKnownAi.isUnknownAi());

        final TestExecutionDto execUnknownAi = new TestExecutionDto();
        execUnknownAi.setStatus("failed-unknown");
        execUnknownAi.setExecutionMode("LLM_ONLY");

        Assertions.assertTrue(execUnknownAi.isAiDriven());
        Assertions.assertFalse(execUnknownAi.isPassAi());
        Assertions.assertFalse(execUnknownAi.isFixedAi());
        Assertions.assertFalse(execUnknownAi.isKnownAi());
        Assertions.assertTrue(execUnknownAi.isUnknownAi());

        final TestExecutionDto execPlaybook = new TestExecutionDto();
        execPlaybook.setStatus("passed");
        execPlaybook.setExecutionMode("REPLAY_STRICT");

        Assertions.assertFalse(execPlaybook.isAiDriven());
        Assertions.assertFalse(execPlaybook.isPassAi());
        Assertions.assertFalse(execPlaybook.isFixedAi());
        Assertions.assertFalse(execPlaybook.isKnownAi());
        Assertions.assertFalse(execPlaybook.isUnknownAi());
    }
}
