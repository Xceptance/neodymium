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

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks cumulative AI execution statistics across all LLM calls and agent operations.
 * Covers token usage, context level distribution, escalation events, retries,
 * and non-LLM execution paths (replays, direct parses).
 * <p>
 * Thread-safe via atomic counters.
 *
 * @author AI-generated: Gemini 2.5 Flash
 */
public class AiStats
{
    private static final Logger LOG = LoggerFactory.getLogger(AiStats.class);

    // === Token usage ===
    private final AtomicLong inputTokens = new AtomicLong();
    private final AtomicLong outputTokens = new AtomicLong();
    private final AtomicLong cachedInputTokens = new AtomicLong();
    private final AtomicInteger callCount = new AtomicInteger();

    // === PESAP Token usage ===
    private final AtomicLong pesapInputTokens = new AtomicLong();
    private final AtomicLong pesapOutputTokens = new AtomicLong();
    private final AtomicLong pesapCachedInputTokens = new AtomicLong();
    private final AtomicInteger pesapCallCount = new AtomicInteger();

    // === Context level distribution ===
    private final EnumMap<ContextLevel, AtomicInteger> contextLevelCounts = new EnumMap<>(ContextLevel.class);

    // === Escalation counters ===
    private final AtomicInteger llmEscalationCount = new AtomicInteger();
    private final AtomicInteger errorEscalationCount = new AtomicInteger();

    // === Retry counters ===
    private final AtomicInteger errorRetryCount = new AtomicInteger();
    private final AtomicInteger noActionsRetryCount = new AtomicInteger();

    // === Non-LLM execution paths ===
    private final AtomicInteger replayCount = new AtomicInteger();
    private final AtomicInteger directParseCount = new AtomicInteger();

    /**
     * Initializes all context level counters to zero.
     */
    public AiStats()
    {
        for (final ContextLevel level : ContextLevel.values())
        {
            contextLevelCounts.put(level, new AtomicInteger());
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Token usage
    // ──────────────────────────────────────────────────────────────

    /**
     * Records standard (AGENT/GENERATOR) token usage.
     *
     * @param input  number of input (prompt) tokens
     * @param output number of output (completion) tokens
     */
    public void record(final long input, final long output)
    {
        record(LlmMode.AGENT, input, output, 0L);
    }

    /**
     * Records token usage based on the specified operational mode.
     *
     * @param mode   the operational mode under which the call was made
     * @param input  number of input (prompt) tokens
     * @param output number of output (completion) tokens
     */
    public void record(final LlmMode mode, final long input, final long output)
    {
        record(mode, input, output, 0L);
    }

    /**
     * Records token usage based on the specified operational mode, including cached tokens.
     *
     * @param mode   the operational mode under which the call was made
     * @param input  number of input (prompt) tokens
     * @param output number of output (completion) tokens
     * @param cached number of cached input tokens
     */
    public void record(final LlmMode mode, final long input, final long output, final long cached)
    {
        if (mode == LlmMode.PESAP)
        {
            pesapInputTokens.addAndGet(input);
            pesapOutputTokens.addAndGet(output);
            pesapCachedInputTokens.addAndGet(cached);
            pesapCallCount.incrementAndGet();

            if (cached > 0)
            {
                LOG.debug("   📊 PESAP Tokens: {} in ({} cached) → {} out  (call #{})",
                        input, cached, output, pesapCallCount.get());
            }
            else
            {
                LOG.debug("   📊 PESAP Tokens: {} in → {} out  (call #{})",
                        input, output, pesapCallCount.get());
            }
        }
        else
        {
            inputTokens.addAndGet(input);
            outputTokens.addAndGet(output);
            cachedInputTokens.addAndGet(cached);
            callCount.incrementAndGet();

            if (cached > 0)
            {
                LOG.debug("   📊 Tokens: {} in ({} cached) → {} out  (call #{})",
                        input, cached, output, callCount.get());
            }
            else
            {
                LOG.debug("   📊 Tokens: {} in → {} out  (call #{})",
                        input, output, callCount.get());
            }
        }
    }

    public long getInputTokens()
    {
        return inputTokens.get();
    }

    public long getOutputTokens()
    {
        return outputTokens.get();
    }

    /**
     * Returns the cumulative number of cached input (prompt) tokens for standard LLM calls.
     *
     * @return the number of cached input tokens
     */
    public long getCachedInputTokens()
    {
        return cachedInputTokens.get();
    }

    /**
     * Returns the cumulative number of cached input (prompt) tokens for PESAP calls.
     *
     * @return the number of cached PESAP input tokens
     */
    public long getPesapCachedInputTokens()
    {
        return pesapCachedInputTokens.get();
    }

    /**
     * Returns the total cumulative number of cached input (prompt) tokens across both standard and PESAP calls.
     *
     * @return the overall cached input tokens
     */
    public long getOverallCachedInputTokens()
    {
        return cachedInputTokens.get() + pesapCachedInputTokens.get();
    }

    public long getTotalTokens()
    {
        return inputTokens.get() + outputTokens.get();
    }

    public long getPesapInputTokens()
    {
        return pesapInputTokens.get();
    }

    public long getPesapOutputTokens()
    {
        return pesapOutputTokens.get();
    }

    public long getPesapTotalTokens()
    {
        return pesapInputTokens.get() + pesapOutputTokens.get();
    }

    public int getPesapCallCount()
    {
        return pesapCallCount.get();
    }

    public long getOverallInputTokens()
    {
        return inputTokens.get() + pesapInputTokens.get();
    }

    public long getOverallOutputTokens()
    {
        return outputTokens.get() + pesapOutputTokens.get();
    }

    public long getOverallTotalTokens()
    {
        return getTotalTokens() + getPesapTotalTokens();
    }

    public int getOverallCallCount()
    {
        return callCount.get() + pesapCallCount.get();
    }

    public int getCallCount()
    {
        return callCount.get();
    }

    // ──────────────────────────────────────────────────────────────
    // Context level tracking
    // ──────────────────────────────────────────────────────────────

    /**
     * Records that an LLM call was made at the given context level.
     *
     * @param level the context level used for the LLM call
     */
    public void recordContextLevel(final ContextLevel level)
    {
        contextLevelCounts.get(level).incrementAndGet();
    }

    /**
     * Returns the number of LLM calls made at the given context level.
     *
     * @param level the context level to query
     * @return the count
     */
    public int getContextLevelCount(final ContextLevel level)
    {
        return contextLevelCounts.get(level).get();
    }

    // ──────────────────────────────────────────────────────────────
    // Escalation tracking
    // ──────────────────────────────────────────────────────────────

    /**
     * Records a context escalation event.
     *
     * @param llmRequested {@code true} if the LLM explicitly requested the escalation
     *                     via {@code "status": "ESCALATE"}, {@code false} if it was
     *                     triggered by an execution error or assertion failure
     */
    public void recordEscalation(final boolean llmRequested)
    {
        if (llmRequested)
        {
            llmEscalationCount.incrementAndGet();
        }
        else
        {
            errorEscalationCount.incrementAndGet();
        }
    }

    /**
     * Returns the total number of escalation events.
     *
     * @return LLM-requested + error-triggered escalations
     */
    public int getTotalEscalationCount()
    {
        return llmEscalationCount.get() + errorEscalationCount.get();
    }

    public int getLlmEscalationCount()
    {
        return llmEscalationCount.get();
    }

    public int getErrorEscalationCount()
    {
        return errorEscalationCount.get();
    }

    // ──────────────────────────────────────────────────────────────
    // Retry tracking
    // ──────────────────────────────────────────────────────────────

    /**
     * Records a retry event.
     *
     * @param noActions {@code true} if the retry was caused by an empty actions
     *                  response, {@code false} if caused by an execution error
     */
    public void recordRetry(final boolean noActions)
    {
        if (noActions)
        {
            noActionsRetryCount.incrementAndGet();
        }
        else
        {
            errorRetryCount.incrementAndGet();
        }
    }

    /**
     * Returns the total number of retry events.
     *
     * @return error retries + no-actions retries
     */
    public int getTotalRetryCount()
    {
        return errorRetryCount.get() + noActionsRetryCount.get();
    }

    public int getErrorRetryCount()
    {
        return errorRetryCount.get();
    }

    public int getNoActionsRetryCount()
    {
        return noActionsRetryCount.get();
    }

    // ──────────────────────────────────────────────────────────────
    // Non-LLM execution path tracking
    // ──────────────────────────────────────────────────────────────

    /**
     * Records a step that was replayed from the playbook without calling the LLM.
     */
    public void recordReplay()
    {
        replayCount.incrementAndGet();
    }

    public int getReplayCount()
    {
        return replayCount.get();
    }

    /**
     * Records a step that was resolved by a plugin or regex without calling the LLM.
     */
    public void recordDirectParse()
    {
        directParseCount.incrementAndGet();
    }

    public int getDirectParseCount()
    {
        return directParseCount.get();
    }

    // ──────────────────────────────────────────────────────────────
    // Summary
    // ──────────────────────────────────────────────────────────────

    /**
     * Logs a summary of all cumulative stats.
     */
    public void logSummary()
    {
        final long standardIn = inputTokens.get();
        final long standardOut = outputTokens.get();
        final long standardTotal = standardIn + standardOut;
        final long standardCached = cachedInputTokens.get();

        final long pesapIn = pesapInputTokens.get();
        final long pesapOut = pesapOutputTokens.get();
        final long pesapTotal = pesapIn + pesapOut;
        final long pesapCached = pesapCachedInputTokens.get();

        final long overallIn = standardIn + pesapIn;
        final long overallOut = standardOut + pesapOut;
        final long overallTotal = standardTotal + pesapTotal;
        final long overallCached = standardCached + pesapCached;

        LOG.debug("======== 📊 AI Execution Statistics ========");
        LOG.debug("   LLM Standard:");
        LOG.debug("     Calls:          {}", callCount.get());
        LOG.debug("     Input tokens:   {} (Cached: {}, Efficiency: {})",
                standardIn, standardCached, String.format("%.1f%%", standardIn > 0 ? (double) standardCached / standardIn * 100.0 : 0.0));
        LOG.debug("     Output tokens:  {}", standardOut);
        LOG.debug("     Total tokens:   {}", standardTotal);
        LOG.debug("   ---");
        LOG.debug("   PESAP (Pre-Execution Static Analysis):");
        LOG.debug("     Calls:          {}", pesapCallCount.get());
        LOG.debug("     Input tokens:   {} (Cached: {}, Efficiency: {})",
                pesapIn, pesapCached, String.format("%.1f%%", pesapIn > 0 ? (double) pesapCached / pesapIn * 100.0 : 0.0));
        LOG.debug("     Output tokens:  {}", pesapOut);
        LOG.debug("     Total tokens:   {}", pesapTotal);
        LOG.debug("   ---");
        LOG.debug("   Total:");
        LOG.debug("     Calls:          {}", callCount.get() + pesapCallCount.get());
        LOG.debug("     Input tokens:   {} (Cached: {}, Efficiency: {})",
                overallIn, overallCached, String.format("%.1f%%", overallIn > 0 ? (double) overallCached / overallIn * 100.0 : 0.0));
        LOG.debug("     Output tokens:  {}", overallOut);
        LOG.debug("     Total tokens:   {}", overallTotal);
        LOG.debug("   ---");
        LOG.debug("   Context Levels:");
        for (final ContextLevel level : ContextLevel.values())
        {
            LOG.debug("     {}", String.format("%-10s %d", level.name() + ":", contextLevelCounts.get(level).get()));
        }
        LOG.debug("   ---");
        LOG.debug("   Escalations:      {}  (LLM: {}, Error: {})",
                getTotalEscalationCount(), llmEscalationCount.get(), errorEscalationCount.get());
        LOG.debug("   Retries:          {}  (Error: {}, No-Actions: {})",
                getTotalRetryCount(), errorRetryCount.get(), noActionsRetryCount.get());
        LOG.debug("   ---");
        LOG.debug("   Replays:          {}", replayCount.get());
        LOG.debug("   Direct Parses:    {}", directParseCount.get());
        LOG.debug("=============================================");
    }

    /**
     * Generates a plain-text summary suitable for Allure attachments.
     *
     * @return the formatted summary string
     */
    public String toSummaryString()
    {
        final StringBuilder sb = new StringBuilder();

        sb.append("Token Usage Summary\n\n");
        
        sb.append("LLM Standard:\n");
        sb.append(String.format("  Input Tokens:  %d (Cached: %d, Efficiency: %.1f%%)%n",
                inputTokens.get(), cachedInputTokens.get(),
                inputTokens.get() > 0 ? (double) cachedInputTokens.get() / inputTokens.get() * 100.0 : 0.0));
        sb.append(String.format("  Output Tokens: %d%n", outputTokens.get()));
        sb.append(String.format("  Total Tokens:  %d%n", getTotalTokens()));
        sb.append(String.format("  Total Calls:   %d%n", callCount.get()));
        
        sb.append("\nPESAP:\n");
        sb.append(String.format("  Input Tokens:  %d (Cached: %d, Efficiency: %.1f%%)%n",
                pesapInputTokens.get(), pesapCachedInputTokens.get(),
                pesapInputTokens.get() > 0 ? (double) pesapCachedInputTokens.get() / pesapInputTokens.get() * 100.0 : 0.0));
        sb.append(String.format("  Output Tokens: %d%n", pesapOutputTokens.get()));
        sb.append(String.format("  Total Tokens:  %d%n", getPesapTotalTokens()));
        sb.append(String.format("  Total Calls:   %d%n", pesapCallCount.get()));

        sb.append("\nTotal:\n");
        sb.append(String.format("  Input Tokens:  %d (Cached: %d, Efficiency: %.1f%%)%n",
                getOverallInputTokens(), getOverallCachedInputTokens(),
                getOverallInputTokens() > 0 ? (double) getOverallCachedInputTokens() / getOverallInputTokens() * 100.0 : 0.0));
        sb.append(String.format("  Output Tokens: %d%n", getOverallOutputTokens()));
        sb.append(String.format("  Total Tokens:  %d%n", getOverallTotalTokens()));
        sb.append(String.format("  Total Calls:   %d%n", getOverallCallCount()));

        sb.append("\nExecution Statistics\n\n");
        sb.append("Context Levels Used:\n");
        for (final ContextLevel level : ContextLevel.values())
        {
            sb.append(String.format("  %-10s %d%n", level.name() + ":", contextLevelCounts.get(level).get()));
        }

        sb.append(String.format("%nEscalations:     %d  (LLM-requested: %d, Error-triggered: %d)%n",
                getTotalEscalationCount(), llmEscalationCount.get(), errorEscalationCount.get()));
        sb.append(String.format("Retries:         %d  (Error: %d, No-Actions: %d)%n",
                getTotalRetryCount(), errorRetryCount.get(), noActionsRetryCount.get()));
        sb.append(String.format("Replays:         %d%n", replayCount.get()));
        sb.append(String.format("Direct Parses:   %d%n", directParseCount.get()));

        return sb.toString();
    }

    /**
     * Resets all counters.
     */
    public void reset()
    {
        inputTokens.set(0);
        outputTokens.set(0);
        cachedInputTokens.set(0);
        callCount.set(0);

        pesapInputTokens.set(0);
        pesapOutputTokens.set(0);
        pesapCachedInputTokens.set(0);
        pesapCallCount.set(0);

        for (final AtomicInteger counter : contextLevelCounts.values())
        {
            counter.set(0);
        }

        llmEscalationCount.set(0);
        errorEscalationCount.set(0);
        errorRetryCount.set(0);
        noActionsRetryCount.set(0);
        replayCount.set(0);
        directParseCount.set(0);
    }
}
