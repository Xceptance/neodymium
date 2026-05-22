/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xceptance.neodymium.ai.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link AiStats} statistics tracker, verifying standard and
 * PESAP token usage counters, overall totals, resets, and summary printouts.
 *
 * // AI-generated: Gemini 3.5 Flash
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
        assertEquals(0L, stats.getTotalTokens());
        assertEquals(0, stats.getCallCount());

        // PESAP counters
        assertEquals(0L, stats.getPesapInputTokens());
        assertEquals(0L, stats.getPesapOutputTokens());
        assertEquals(0L, stats.getPesapTotalTokens());
        assertEquals(0, stats.getPesapCallCount());

        // Overall counters
        assertEquals(0L, stats.getOverallInputTokens());
        assertEquals(0L, stats.getOverallOutputTokens());
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
        assertEquals(850L, stats.getTotalTokens());
        assertEquals(3, stats.getCallCount());

        // PESAP counters must be zero
        assertEquals(0L, stats.getPesapInputTokens());
        assertEquals(0L, stats.getPesapOutputTokens());
        assertEquals(0, stats.getPesapCallCount());

        // Overall counters
        assertEquals(600L, stats.getOverallInputTokens());
        assertEquals(250L, stats.getOverallOutputTokens());
        assertEquals(850L, stats.getOverallTotalTokens());
        assertEquals(3, stats.getOverallCallCount());
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
        assertEquals(500L, stats.getPesapTotalTokens());
        assertEquals(2, stats.getPesapCallCount());

        // Overall counters
        assertEquals(400L, stats.getOverallInputTokens());
        assertEquals(100L, stats.getOverallOutputTokens());
        assertEquals(500L, stats.getOverallTotalTokens());
        assertEquals(2, stats.getOverallCallCount());
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

        stats.record(100L, 50L);
        stats.record(LlmMode.PESAP, 150L, 40L);

        stats.reset();

        assertEquals(0L, stats.getInputTokens());
        assertEquals(0L, stats.getPesapInputTokens());
        assertEquals(0L, stats.getOverallInputTokens());
        assertEquals(0, stats.getCallCount());
        assertEquals(0, stats.getPesapCallCount());
        assertEquals(0, stats.getOverallCallCount());
    }

    @Test
    void toSummaryStringAndLogSummaryFormat()
    {
        final AiStats stats = new AiStats();

        stats.record(100L, 50L);
        stats.record(LlmMode.PESAP, 150L, 40L);

        final String summary = stats.toSummaryString();

        // Verify key sections and exact values are present in the summary string
        assertTrue(summary.contains("LLM Standard:"));
        assertTrue(summary.contains("Input Tokens:  100"));
        assertTrue(summary.contains("Output Tokens: 50"));
        assertTrue(summary.contains("Total Tokens:  150"));
        assertTrue(summary.contains("Total Calls:   1"));

        assertTrue(summary.contains("PESAP:"));
        assertTrue(summary.contains("Input Tokens:  150"));
        assertTrue(summary.contains("Output Tokens: 40"));
        assertTrue(summary.contains("Total Tokens:  190"));
        assertTrue(summary.contains("Total Calls:   1"));

        assertTrue(summary.contains("Total:"));
        assertTrue(summary.contains("Input Tokens:  250"));
        assertTrue(summary.contains("Output Tokens: 90"));
        assertTrue(summary.contains("Total Tokens:  340"));
        assertTrue(summary.contains("Total Calls:   2"));

        // Ensure logSummary runs without throwing exceptions
        stats.logSummary();
    }
}
