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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.xceptance.neodymium.ai.action.Action;

/**
 * Aggregates results of an AI execution run.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class AiExecutionResult
{
    private final List<StepDetails> steps;
    private final List<LookupDetails> lookups;
    private final List<EscalationDetails> escalations;
    private final Map<String, String> testDataSnapshot;
    private final transient AiBrowser aiBrowser;

    // cumulative token/metric deltas
    private long inputTokens;
    private long outputTokens;
    private long cachedTokens;
    private long totalTokens;

    private long pesapInputTokens;
    private long pesapOutputTokens;
    private long pesapCachedTokens;
    private long pesapTotalTokens;

    private int retryCount;
    private int escalationCount;
    private int replayCount;
    private int directParseCount;
    private int pesapCallCount;

    private long durationMs;

    public AiExecutionResult(final Map<String, String> testDataSnapshot, final AiBrowser aiBrowser)
    {
        this.steps = Collections.synchronizedList(new ArrayList<>());
        this.lookups = Collections.synchronizedList(new ArrayList<>());
        this.escalations = Collections.synchronizedList(new ArrayList<>());
        this.testDataSnapshot = Collections.unmodifiableMap(new HashMap<>(testDataSnapshot));
        this.aiBrowser = aiBrowser;
    }

    public final List<StepDetails> getSteps()
    {
        return this.steps;
    }

    public final List<LookupDetails> getLookups()
    {
        return this.lookups;
    }

    public final List<EscalationDetails> getEscalations()
    {
        return this.escalations;
    }

    public final Map<String, String> getTestDataSnapshot()
    {
        return this.testDataSnapshot;
    }

    // Helper to aggregate LLM calls from all steps
    public final List<LlmCallDetails> getLlmCalls()
    {
        final List<LlmCallDetails> allCalls = new ArrayList<>();
        synchronized (this.steps)
        {
            for (final StepDetails step : this.steps)
            {
                allCalls.addAll(step.getLlmCalls());
            }
        }
        return Collections.unmodifiableList(allCalls);
    }

    // Helper to aggregate executed actions from all steps
    public final List<Action> getActions()
    {
        final List<Action> allActions = new ArrayList<>();
        synchronized (this.steps)
        {
            for (final StepDetails step : this.steps)
            {
                allActions.addAll(step.getActions());
            }
        }
        return Collections.unmodifiableList(allActions);
    }

    public final boolean hasActionType(final String type)
    {
        if (type == null)
        {
            return false;
        }
        synchronized (this.steps)
        {
            for (final StepDetails step : this.steps)
            {
                for (final Action action : step.getActions())
                {
                    if (type.equals(action.getType()))
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public final boolean hasDirectParse()
    {
        return this.directParseCount > 0;
    }

    // Getters and Setters for token stats and metrics
    public final long getInputTokens()
    {
        return this.inputTokens;
    }

    public final void setInputTokens(final long inputTokens)
    {
        this.inputTokens = inputTokens;
    }

    public final long getOutputTokens()
    {
        return this.outputTokens;
    }

    public final void setOutputTokens(final long outputTokens)
    {
        this.outputTokens = outputTokens;
    }

    public final long getCachedTokens()
    {
        return this.cachedTokens;
    }

    public final void setCachedTokens(final long cachedTokens)
    {
        this.cachedTokens = cachedTokens;
    }

    public final long getTotalTokens()
    {
        return this.totalTokens;
    }

    public final void setTotalTokens(final long totalTokens)
    {
        this.totalTokens = totalTokens;
    }

    public final long getPesapInputTokens()
    {
        return this.pesapInputTokens;
    }

    public final void setPesapInputTokens(final long pesapInputTokens)
    {
        this.pesapInputTokens = pesapInputTokens;
    }

    public final long getPesapOutputTokens()
    {
        return this.pesapOutputTokens;
    }

    public final void setPesapOutputTokens(final long pesapOutputTokens)
    {
        this.pesapOutputTokens = pesapOutputTokens;
    }

    public final long getPesapCachedTokens()
    {
        return this.pesapCachedTokens;
    }

    public final void setPesapCachedTokens(final long pesapCachedTokens)
    {
        this.pesapCachedTokens = pesapCachedTokens;
    }

    public final long getPesapTotalTokens()
    {
        return this.pesapTotalTokens;
    }

    public final void setPesapTotalTokens(final long pesapTotalTokens)
    {
        this.pesapTotalTokens = pesapTotalTokens;
    }

    public final int getRetryCount()
    {
        return this.retryCount;
    }

    public final void setRetryCount(final int retryCount)
    {
        this.retryCount = retryCount;
    }

    public final int getEscalationCount()
    {
        return this.escalationCount;
    }

    public final void setEscalationCount(final int escalationCount)
    {
        this.escalationCount = escalationCount;
    }

    public final int getReplayCount()
    {
        return this.replayCount;
    }

    public final void setReplayCount(final int replayCount)
    {
        this.replayCount = replayCount;
    }

    public final int getDirectParseCount()
    {
        return this.directParseCount;
    }

    public final void setDirectParseCount(final int directParseCount)
    {
        this.directParseCount = directParseCount;
    }

    public final int getPesapCallCount()
    {
        return this.pesapCallCount;
    }

    public final void setPesapCallCount(final int pesapCallCount)
    {
        this.pesapCallCount = pesapCallCount;
    }

    public final long getDurationMs()
    {
        return this.durationMs;
    }

    public final void setDurationMs(final long durationMs)
    {
        this.durationMs = durationMs;
    }

    public final boolean isSuccess()
    {
        synchronized (this.steps)
        {
            for (final StepDetails step : this.steps)
            {
                if (step.getFailureReason() != null)
                {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Logs the cumulative AI execution statistics.
     *
     * @return the stats logger helper instance
     */
    public final AiStatsLogger logAiStats()
    {
        if (this.aiBrowser != null && this.aiBrowser.getStats() != null)
        {
            this.aiBrowser.getStats().logSummary();
        }
        return new AiStatsLogger(this.aiBrowser);
    }

    /**
     * Logs the step-by-step trace statistics for this execution.
     *
     * @return the step stats logger helper instance
     */
    public final AiStepStatsLogger logAiStepStats()
    {
        if (this.aiBrowser != null)
        {
            this.aiBrowser.logStepSummary(this);
        }
        return new AiStepStatsLogger(this.aiBrowser);
    }

    /**
     * Resets the cumulative statistics and clears the execution steps.
     *
     * @return this execution result instance
     */
    public final AiExecutionResult reset()
    {
        if (this.aiBrowser != null)
        {
            if (this.aiBrowser.getStats() != null)
            {
                this.aiBrowser.getStats().reset();
            }
            this.aiBrowser.clearExecutionResults();
        }
        return this;
    }

    /**
     * Resetter helper for AI execution statistics.
     */
    public static class AiStatsLogger
    {
        private final AiBrowser browser;

        public AiStatsLogger(final AiBrowser browser)
        {
            this.browser = browser;
        }

        public final void reset()
        {
            if (this.browser != null && this.browser.getStats() != null)
            {
                this.browser.getStats().reset();
            }
        }
    }

    /**
     * Resetter helper for step-by-step statistics.
     */
    public static class AiStepStatsLogger
    {
        private final AiBrowser browser;

        public AiStepStatsLogger(final AiBrowser browser)
        {
            this.browser = browser;
        }

        public final void reset()
        {
            if (this.browser != null)
            {
                this.browser.clearExecutionResults();
            }
        }
    }
}
