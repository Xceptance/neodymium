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
package com.xceptance.neodymium.ai.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link AiStats} statistics tracker, verifying standard and
 * PESAP token usage counters, overall totals, resets, and summary printouts.
 *
 * @author AI-generated: Gemini 2.5 Flash
 */
class AiStatsTest
{
    @Test
    void defaultCountersAreZero()
    {
        final AiStats stats = new AiStats();

        // Standard counters
        assertEquals(0L, stats.getInputTokens());
        assertEquals(0L, stats.getOutputTokens());
        assertEquals(0L, stats.getCachedInputTokens());
        assertEquals(0L, stats.getTotalTokens());
        assertEquals(0, stats.getCallCount());

        // PESAP counters
        assertEquals(0L, stats.getPesapInputTokens());
        assertEquals(0L, stats.getPesapOutputTokens());
        assertEquals(0L, stats.getPesapCachedInputTokens());
        assertEquals(0L, stats.getPesapTotalTokens());
        assertEquals(0, stats.getPesapCallCount());

        // Overall counters
        assertEquals(0L, stats.getOverallInputTokens());
        assertEquals(0L, stats.getOverallOutputTokens());
        assertEquals(0L, stats.getOverallCachedInputTokens());
        assertEquals(0L, stats.getOverallTotalTokens());
        assertEquals(0, stats.getOverallCallCount());
    }

    @Test
    void recordsStandardCallsCorrectly()
    {
        final AiStats stats = new AiStats();

        // Call standard record
        stats.record(100L, 50L);
        stats.record(LlmMode.AGENT, 200L, 80L);
        stats.record(LlmMode.GENERATOR, 300L, 120L);

        // Standard counters
        assertEquals(600L, stats.getInputTokens());
        assertEquals(250L, stats.getOutputTokens());
        assertEquals(0L, stats.getCachedInputTokens());
        assertEquals(850L, stats.getTotalTokens());
        assertEquals(3, stats.getCallCount());

        // PESAP counters must be zero
        assertEquals(0L, stats.getPesapInputTokens());
        assertEquals(0L, stats.getPesapOutputTokens());
        assertEquals(0, stats.getPesapCallCount());

        // Overall counters
        assertEquals(600L, stats.getOverallInputTokens());
        assertEquals(250L, stats.getOverallOutputTokens());
        assertEquals(0L, stats.getOverallCachedInputTokens());
        assertEquals(850L, stats.getOverallTotalTokens());
        assertEquals(3, stats.getOverallCallCount());
    }

    @Test
    void recordsStandardCallsWithCachedTokensCorrectly()
    {
        final AiStats stats = new AiStats();

        stats.record(LlmMode.AGENT, 200L, 80L, 120L);
        stats.record(LlmMode.GENERATOR, 300L, 120L, 180L);

        // Standard counters
        assertEquals(500L, stats.getInputTokens());
        assertEquals(200L, stats.getOutputTokens());
        assertEquals(300L, stats.getCachedInputTokens());
        assertEquals(700L, stats.getTotalTokens());
        assertEquals(2, stats.getCallCount());

        // PESAP counters must be zero
        assertEquals(0L, stats.getPesapInputTokens());
        assertEquals(0L, stats.getPesapCachedInputTokens());

        // Overall counters
        assertEquals(500L, stats.getOverallInputTokens());
        assertEquals(200L, stats.getOverallOutputTokens());
        assertEquals(300L, stats.getOverallCachedInputTokens());
        assertEquals(700L, stats.getOverallTotalTokens());
        assertEquals(2, stats.getOverallCallCount());
    }

    @Test
    void recordsPesapCallsCorrectly()
    {
        final AiStats stats = new AiStats();

        // Call PESAP record
        stats.record(LlmMode.PESAP, 150L, 40L);
        stats.record(LlmMode.PESAP, 250L, 60L);

        // Standard counters must be zero
        assertEquals(0L, stats.getInputTokens());
        assertEquals(0L, stats.getOutputTokens());
        assertEquals(0, stats.getCallCount());

        // PESAP counters
        assertEquals(400L, stats.getPesapInputTokens());
        assertEquals(100L, stats.getPesapOutputTokens());
        assertEquals(0L, stats.getPesapCachedInputTokens());
        assertEquals(500L, stats.getPesapTotalTokens());
        assertEquals(2, stats.getPesapCallCount());

        // Overall counters
        assertEquals(400L, stats.getOverallInputTokens());
        assertEquals(100L, stats.getOverallOutputTokens());
        assertEquals(0L, stats.getOverallCachedInputTokens());
        assertEquals(500L, stats.getOverallTotalTokens());
        assertEquals(2, stats.getOverallCallCount());
    }

    @Test
    void recordsPesapCallsWithCachedTokensCorrectly()
    {
        final AiStats stats = new AiStats();

        stats.record(LlmMode.PESAP, 400L, 100L, 250L);

        // Standard counters must be zero
        assertEquals(0L, stats.getInputTokens());
        assertEquals(0L, stats.getCachedInputTokens());

        // PESAP counters
        assertEquals(400L, stats.getPesapInputTokens());
        assertEquals(100L, stats.getPesapOutputTokens());
        assertEquals(250L, stats.getPesapCachedInputTokens());
        assertEquals(500L, stats.getPesapTotalTokens());
        assertEquals(1, stats.getPesapCallCount());

        // Overall counters
        assertEquals(400L, stats.getOverallInputTokens());
        assertEquals(100L, stats.getOverallOutputTokens());
        assertEquals(250L, stats.getOverallCachedInputTokens());
        assertEquals(500L, stats.getOverallTotalTokens());
        assertEquals(1, stats.getOverallCallCount());
    }

    @Test
    void recordsMixedCallsCorrectly()
    {
        final AiStats stats = new AiStats();

        // 3 Standard calls
        stats.record(100L, 50L);
        stats.record(LlmMode.AGENT, 200L, 80L);
        stats.record(LlmMode.GENERATOR, 300L, 120L);

        // 2 PESAP calls
        stats.record(LlmMode.PESAP, 150L, 40L);
        stats.record(LlmMode.PESAP, 250L, 60L);

        // Standard counters
        assertEquals(600L, stats.getInputTokens());
        assertEquals(250L, stats.getOutputTokens());
        assertEquals(850L, stats.getTotalTokens());
        assertEquals(3, stats.getCallCount());

        // PESAP counters
        assertEquals(400L, stats.getPesapInputTokens());
        assertEquals(100L, stats.getPesapOutputTokens());
        assertEquals(500L, stats.getPesapTotalTokens());
        assertEquals(2, stats.getPesapCallCount());

        // Overall counters
        assertEquals(1000L, stats.getOverallInputTokens());
        assertEquals(350L, stats.getOverallOutputTokens());
        assertEquals(1350L, stats.getOverallTotalTokens());
        assertEquals(5, stats.getOverallCallCount());
    }

    @Test
    void resetsCountersCorrectly()
    {
        final AiStats stats = new AiStats();

        stats.record(LlmMode.AGENT, 100L, 50L, 40L);
        stats.record(LlmMode.PESAP, 150L, 40L, 60L);

        stats.reset();

        assertEquals(0L, stats.getInputTokens());
        assertEquals(0L, stats.getCachedInputTokens());
        assertEquals(0L, stats.getPesapInputTokens());
        assertEquals(0L, stats.getPesapCachedInputTokens());
        assertEquals(0L, stats.getOverallInputTokens());
        assertEquals(0L, stats.getOverallCachedInputTokens());
        assertEquals(0, stats.getCallCount());
        assertEquals(0, stats.getPesapCallCount());
        assertEquals(0, stats.getOverallCallCount());
    }

    @Test
    void toSummaryStringAndLogSummaryFormat()
    {
        final AiStats stats = new AiStats();

        stats.record(LlmMode.AGENT, 100L, 50L, 40L);
        stats.record(LlmMode.PESAP, 150L, 40L, 60L);

        final String summary = stats.toSummaryString();

        // Verify key sections and exact values are present in the summary string
        assertTrue(summary.contains("LLM Standard:"));
        assertTrue(summary.contains("Input Tokens:  100 (Cached: 40, Efficiency: 40.0%)"));
        assertTrue(summary.contains("Output Tokens: 50"));
        assertTrue(summary.contains("Total Tokens:  150"));
        assertTrue(summary.contains("Total Calls:   1"));

        assertTrue(summary.contains("PESAP:"));
        assertTrue(summary.contains("Input Tokens:  150 (Cached: 60, Efficiency: 40.0%)"));
        assertTrue(summary.contains("Output Tokens: 40"));
        assertTrue(summary.contains("Total Tokens:  190"));
        assertTrue(summary.contains("Total Calls:   1"));

        assertTrue(summary.contains("Total:"));
        assertTrue(summary.contains("Input Tokens:  250 (Cached: 100, Efficiency: 40.0%)"));
        assertTrue(summary.contains("Output Tokens: 90"));
        assertTrue(summary.contains("Total Tokens:  340"));
        assertTrue(summary.contains("Total Calls:   2"));

        // Ensure logSummary runs without throwing exceptions
        stats.logSummary();
    }

    @Test
    void testClassExists()
    {
        try
        {
            final Class<?> clazz = Class.forName("dev.langchain4j.model.googleai.GoogleAiGeminiTokenUsage");
            assertNotNull(clazz);
        }
        catch (final ClassNotFoundException e)
        {
            fail("dev.langchain4j.model.googleai.GoogleAiGeminiTokenUsage class not found: " + e.getMessage());
        }
    }
}
