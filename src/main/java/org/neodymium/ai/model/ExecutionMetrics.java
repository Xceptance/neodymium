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
package org.neodymium.ai.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.model.ContextLevel;

/**
 * Immutable snapshot of execution telemetry statistics and execution mode context.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class ExecutionMetrics
{
    private final ExecutionMode executionMode;

    private final int llmCallCount;

    private final int standardCallCount;

    private final int verificationCallCount;

    private final int pesapCallCount;

    private final int judgeCallCount;

    private final int rcaCallCount;

    private final int stepCount;

    private final int healedStepCount;

    private final int softFailedStepCount;

    private final int replayedStepCount;

    private final int internalCacheHits;

    private final int totalEscalations;

    private final Map<String, Integer> contextLevelCounts;

    private final TokenUsage totalTokenUsage;

    /**
     * Constructs an ExecutionMetrics instance with standard telemetry parameters.
     *
     * @param executionMode the execution mode governing this run
     * @param llmCallCount total LLM API calls made
     * @param standardCallCount standard action extraction LLM calls
     * @param verificationCallCount post-action verification LLM calls
     * @param pesapCallCount PESAP pre-step analysis LLM calls
     * @param judgeCallCount quality judge LLM calls
     * @param stepCount total playbook steps executed
     * @param healedStepCount count of steps resolved via self-healing
     * @param softFailedStepCount count of soft/optional steps that failed but were tolerated
     * @param replayedStepCount count of steps executed via offline replay cache
     * @param internalCacheHits count of internal LLM prompt cache hits
     * @param totalTokenUsage total token usage across all LLM calls
     */
    public ExecutionMetrics(
        final ExecutionMode executionMode,
        final int llmCallCount,
        final int standardCallCount,
        final int verificationCallCount,
        final int pesapCallCount,
        final int judgeCallCount,
        final int stepCount,
        final int healedStepCount,
        final int softFailedStepCount,
        final int replayedStepCount,
        final int internalCacheHits,
        final TokenUsage totalTokenUsage
    )
    {
        this(
            executionMode,
            llmCallCount,
            standardCallCount,
            verificationCallCount,
            pesapCallCount,
            judgeCallCount,
            0,
            stepCount,
            healedStepCount,
            softFailedStepCount,
            replayedStepCount,
            internalCacheHits,
            0,
            null,
            totalTokenUsage
        );
    }

    /**
     * Constructs an ExecutionMetrics instance with all aggregated telemetry parameters including escalations and context levels.
     *
     * @param executionMode the execution mode governing this run
     * @param llmCallCount total LLM API calls made
     * @param standardCallCount standard action extraction LLM calls
     * @param verificationCallCount post-action verification LLM calls
     * @param pesapCallCount PESAP pre-step analysis LLM calls
     * @param judgeCallCount quality judge LLM calls
     * @param stepCount total playbook steps executed
     * @param healedStepCount count of steps resolved via self-healing
     * @param softFailedStepCount count of soft/optional steps that failed but were tolerated
     * @param replayedStepCount count of steps executed via offline replay cache
     * @param internalCacheHits count of internal LLM prompt cache hits
     * @param totalEscalations count of context level escalations
     * @param contextLevelCounts map of context level names to usage counts
     * @param totalTokenUsage total token usage across all LLM calls
     */
    public ExecutionMetrics(
        final ExecutionMode executionMode,
        final int llmCallCount,
        final int standardCallCount,
        final int verificationCallCount,
        final int pesapCallCount,
        final int judgeCallCount,
        final int stepCount,
        final int healedStepCount,
        final int softFailedStepCount,
        final int replayedStepCount,
        final int internalCacheHits,
        final int totalEscalations,
        final Map<String, Integer> contextLevelCounts,
        final TokenUsage totalTokenUsage
    )
    {
        this(
            executionMode,
            llmCallCount,
            standardCallCount,
            verificationCallCount,
            pesapCallCount,
            judgeCallCount,
            0,
            stepCount,
            healedStepCount,
            softFailedStepCount,
            replayedStepCount,
            internalCacheHits,
            totalEscalations,
            contextLevelCounts,
            totalTokenUsage
        );
    }

    /**
     * Constructs an ExecutionMetrics instance with all aggregated telemetry parameters including RCA, escalations and context levels.
     *
     * @param executionMode the execution mode governing this run
     * @param llmCallCount total LLM API calls made
     * @param standardCallCount standard action extraction LLM calls
     * @param verificationCallCount post-action verification LLM calls
     * @param pesapCallCount PESAP pre-step analysis LLM calls
     * @param judgeCallCount quality judge LLM calls
     * @param rcaCallCount Visual RCA LLM calls
     * @param stepCount total playbook steps executed
     * @param healedStepCount count of steps resolved via self-healing
     * @param softFailedStepCount count of soft/optional steps that failed but were tolerated
     * @param replayedStepCount count of steps executed via offline replay cache
     * @param internalCacheHits count of internal LLM prompt cache hits
     * @param totalEscalations count of context level escalations
     * @param contextLevelCounts map of context level names to usage counts
     * @param totalTokenUsage total token usage across all LLM calls
     */
    public ExecutionMetrics(
        final ExecutionMode executionMode,
        final int llmCallCount,
        final int standardCallCount,
        final int verificationCallCount,
        final int pesapCallCount,
        final int judgeCallCount,
        final int rcaCallCount,
        final int stepCount,
        final int healedStepCount,
        final int softFailedStepCount,
        final int replayedStepCount,
        final int internalCacheHits,
        final int totalEscalations,
        final Map<String, Integer> contextLevelCounts,
        final TokenUsage totalTokenUsage
    )
    {
        this.executionMode = executionMode != null ? executionMode : ExecutionMode.LLM_ONLY;
        this.llmCallCount = llmCallCount;
        this.standardCallCount = standardCallCount;
        this.verificationCallCount = verificationCallCount;
        this.pesapCallCount = pesapCallCount;
        this.judgeCallCount = judgeCallCount;
        this.rcaCallCount = rcaCallCount;
        this.stepCount = stepCount;
        this.healedStepCount = healedStepCount;
        this.softFailedStepCount = softFailedStepCount;
        this.replayedStepCount = replayedStepCount;
        this.internalCacheHits = internalCacheHits;
        this.totalEscalations = totalEscalations;
        this.contextLevelCounts = contextLevelCounts != null ? Collections.unmodifiableMap(new HashMap<>(contextLevelCounts)) : Collections.emptyMap();
        this.totalTokenUsage = totalTokenUsage != null ? totalTokenUsage : new TokenUsage(0, 0, 0);
    }

    /**
     * Returns the execution mode governing this execution run.
     *
     * @return the execution mode
     */
    public ExecutionMode getExecutionMode()
    {
        return this.executionMode;
    }

    /**
     * Checks if this run performed live LLM action generation.
     *
     * @return true if live execution mode, false otherwise
     */
    public boolean isLive()
    {
        return this.executionMode.isLive();
    }

    /**
     * Checks if this run executed pre-recorded actions from replay cache.
     *
     * @return true if replay mode, false otherwise
     */
    public boolean isReplay()
    {
        return this.executionMode.isReplay();
    }

    /**
     * Checks if this run automatically recorded executed actions to playbook storage.
     *
     * @return true if recording mode, false otherwise
     */
    public boolean isRecording()
    {
        return this.executionMode.isRecording();
    }

    /**
     * Checks if this run supports self-healing on step replay failure.
     *
     * @return true if healing supported, false otherwise
     */
    public boolean supportsHealing()
    {
        return this.executionMode.supportsHealing();
    }

    /**
     * Checks if this run executed under strict replay mode without LLM fallbacks.
     *
     * @return true if strict replay mode, false otherwise
     */
    public boolean isStrictReplay()
    {
        return this.executionMode == ExecutionMode.REPLAY_STRICT;
    }

    /**
     * Checks if any step was resolved via self-healing.
     *
     * @return true if healed step count > 0, false otherwise
     */
    public boolean wasHealed()
    {
        return this.healedStepCount > 0;
    }

    /**
     * Returns the total count of LLM API calls.
     *
     * @return total LLM calls
     */
    public int getLlmCallCount()
    {
        return this.llmCallCount;
    }

    /**
     * Returns the count of standard action extraction LLM calls.
     *
     * @return standard LLM calls
     */
    public int getStandardCallCount()
    {
        return this.standardCallCount;
    }

    /**
     * Returns the count of post-action verification LLM calls.
     *
     * @return verification LLM calls
     */
    public int getVerificationCallCount()
    {
        return this.verificationCallCount;
    }

    /**
     * Returns the count of PESAP pre-step analysis LLM calls.
     *
     * @return PESAP LLM calls
     */
    public int getPesapCallCount()
    {
        return this.pesapCallCount;
    }

    /**
     * Returns the count of quality judge LLM calls.
     *
     * @return judge LLM calls
     */
    public int getJudgeCallCount()
    {
        return this.judgeCallCount;
    }

    /**
     * Returns the count of Visual RCA LLM calls.
     *
     * @return Visual RCA LLM calls
     */
    public int getRcaCallCount()
    {
        return this.rcaCallCount;
    }

    /**
     * Returns the total count of executed playbook steps.
     *
     * @return step count
     */
    public int getStepCount()
    {
        return this.stepCount;
    }

    /**
     * Returns the count of steps resolved via self-healing.
     *
     * @return healed step count
     */
    public int getHealedStepCount()
    {
        return this.healedStepCount;
    }

    /**
     * Returns the count of soft/optional playbook steps that failed but were tolerated.
     *
     * @return soft failed step count
     */
    public int getSoftFailedStepCount()
    {
        return this.softFailedStepCount;
    }

    /**
     * Returns the count of steps executed via offline replay cache.
     *
     * @return replayed step count
     */
    public int getReplayedStepCount()
    {
        return this.replayedStepCount;
    }

    /**
     * Returns the count of internal LLM prompt cache hits.
     *
     * @return cache hits count
     */
    public int getInternalCacheHits()
    {
        return this.internalCacheHits;
    }

    /**
     * Returns the total count of context level escalations across all steps.
     *
     * @return total escalations
     */
    public int getTotalEscalations()
    {
        return this.totalEscalations;
    }

    /**
     * Returns the usage count of the specified ContextLevel enum.
     *
     * @param level the ContextLevel enum
     * @return usage count, or 0 if never used
     */
    public int getContextLevelCount(final ContextLevel level)
    {
        return level != null ? getContextLevelCount(level.name()) : 0;
    }

    /**
     * Returns the usage count of the specified context level name string.
     *
     * @param levelName the context level name
     * @return usage count, or 0 if never used
     */
    public int getContextLevelCount(final String levelName)
    {
        if (levelName == null || levelName.isBlank())
        {
            return 0;
        }
        final Integer count = this.contextLevelCounts.get(levelName.trim().toUpperCase());
        return count != null ? count : 0;
    }

    /**
     * Returns an unmodifiable map of context level names to usage counts.
     *
     * @return map of context level counts
     */
    public Map<String, Integer> getContextLevelCounts()
    {
        return this.contextLevelCounts;
    }

    /**
     * Returns the total token usage across all LLM calls.
     *
     * @return total token usage
     */
    public TokenUsage getTotalTokenUsage()
    {
        return this.totalTokenUsage;
    }
}
