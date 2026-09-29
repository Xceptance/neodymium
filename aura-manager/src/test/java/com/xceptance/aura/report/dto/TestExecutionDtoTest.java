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
package com.xceptance.aura.report.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link TestExecutionDto}.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public class TestExecutionDtoTest
{
    @Test
    public void noArgConstructorDefaultsLlmResponsibilityJsonToEmptyString()
    {
        final TestExecutionDto dto = new TestExecutionDto();
        assertNotNull(dto.getLlmResponsibilityJson());
        assertEquals("", dto.getLlmResponsibilityJson());
    }

    @Test
    public void deserializesLlmResponsibilityJsonFromJson() throws Exception
    {
        final String json = "{"
            + "\"id\":\"exec-1\","
            + "\"runId\":\"run-1\","
            + "\"testClass\":\"WikiTest\","
            + "\"title\":\"search\","
            + "\"llmResponsibility\":{"
            + "  \"action\":{\"calls\":3,\"inputTokens\":800,\"outputTokens\":80,\"cachedTokens\":0,\"totalTokens\":880,\"estimatedCostUsd\":0.0012},"
            + "  \"pesap\":{\"calls\":1,\"inputTokens\":200,\"outputTokens\":20,\"cachedTokens\":0,\"totalTokens\":220,\"estimatedCostUsd\":0.0001},"
            + "  \"judge\":{\"calls\":0,\"inputTokens\":0,\"outputTokens\":0,\"cachedTokens\":0,\"totalTokens\":0,\"estimatedCostUsd\":0.0},"
            + "  \"verification\":{\"calls\":0,\"inputTokens\":0,\"outputTokens\":0,\"cachedTokens\":0,\"totalTokens\":0,\"estimatedCostUsd\":0.0},"
            + "  \"visualRca\":{\"calls\":1,\"inputTokens\":100,\"outputTokens\":10,\"cachedTokens\":0,\"totalTokens\":110,\"estimatedCostUsd\":0.0},"
            + "  \"total\":{\"calls\":5,\"inputTokens\":1100,\"outputTokens\":110,\"cachedTokens\":0,\"totalTokens\":1210,\"estimatedCostUsd\":0.0013}"
            + "}"
            + "}";

        final TestExecutionDto dto = new ObjectMapper().readValue(json, TestExecutionDto.class);
        final String responsibility = dto.getLlmResponsibilityJson();
        assertNotNull(responsibility);
        assertTrue(responsibility.contains("\"action\""), "responsibility must contain 'action' bucket");
        assertTrue(responsibility.contains("\"pesap\""), "responsibility must contain 'pesap' bucket");
        assertTrue(responsibility.contains("\"judge\""), "responsibility must contain 'judge' bucket");
        assertTrue(responsibility.contains("\"verification\""), "responsibility must contain 'verification' bucket");
        assertTrue(responsibility.contains("\"visualRca\""), "responsibility must contain 'visualRca' bucket");
        assertTrue(responsibility.contains("\"total\""), "responsibility must contain 'total' bucket");
        assertTrue(responsibility.contains("\"inputTokens\":800"), "responsibility must preserve per-bucket inputTokens");
    }

    @Test
    public void missingLlmResponsibilityJsonStaysEmptyAfterDeserialization() throws Exception
    {
        final String json = "{\"id\":\"exec-2\",\"runId\":\"run-2\",\"testClass\":\"WikiTest\",\"title\":\"search\"}";
        final TestExecutionDto dto = new ObjectMapper().readValue(json, TestExecutionDto.class);
        assertEquals("", dto.getLlmResponsibilityJson());
    }

    @Test
    public void deserializesContextLevelCountsFromMetricsBlock() throws Exception
    {
        final String json = "{"
            + "\"id\":\"exec-3\","
            + "\"runId\":\"run-3\","
            + "\"testClass\":\"WikiTest\","
            + "\"title\":\"search\","
            + "\"metrics\":{"
            + "  \"totalSteps\":6,"
            + "  \"contextLevelCounts\":{\"MINIMAL\":2,\"LEAN\":2,\"STANDARD\":1,\"VISUAL\":1,\"VISUAL_LEAN\":1,\"VISUAL_RICH\":1}"
            + "}"
            + "}";

        final TestExecutionDto dto = new ObjectMapper().readValue(json, TestExecutionDto.class);
        final String counts = dto.getContextLevelCountsJson();
        assertNotNull(counts);
        assertTrue(counts.contains("\"MINIMAL\":2"), "counts must contain MINIMAL");
        assertTrue(counts.contains("\"LEAN\":2"), "counts must contain LEAN");
        assertTrue(counts.contains("\"STANDARD\":1"), "counts must contain STANDARD");
        assertTrue(counts.contains("\"VISUAL\":1"), "counts must contain VISUAL");
        assertTrue(counts.contains("\"VISUAL_LEAN\":1"), "counts must contain VISUAL_LEAN");
        assertTrue(counts.contains("\"VISUAL_RICH\":1"), "counts must contain VISUAL_RICH");
    }

    @Test
    public void missingMetricsBlockYieldsEmptyContextLevelCounts() throws Exception
    {
        final String json = "{\"id\":\"exec-4\",\"runId\":\"run-4\",\"testClass\":\"WikiTest\",\"title\":\"search\"}";
        final TestExecutionDto dto = new ObjectMapper().readValue(json, TestExecutionDto.class);
        assertEquals("", dto.getContextLevelCountsJson());
    }

    @Test
    public void metricsBlockWithoutContextLevelCountsYieldsEmpty() throws Exception
    {
        final String json = "{\"id\":\"exec-5\",\"runId\":\"run-5\",\"testClass\":\"WikiTest\",\"title\":\"search\",\"metrics\":{\"totalSteps\":3}}";
        final TestExecutionDto dto = new ObjectMapper().readValue(json, TestExecutionDto.class);
        assertEquals("", dto.getContextLevelCountsJson());
    }
}
