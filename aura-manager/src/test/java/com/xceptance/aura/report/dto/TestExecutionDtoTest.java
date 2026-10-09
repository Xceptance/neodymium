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
}
