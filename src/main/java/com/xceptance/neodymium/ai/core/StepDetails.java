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
import java.util.List;

import com.xceptance.neodymium.ai.action.Action;

/**
 * Details of a single instruction step executed by the AI.
 *
 * // AI-generated: Gemini 3.5 Flash
 */
public final class StepDetails
{
    private final String rawInstruction;
    private String expandedInstruction;
    private final List<Action> actions;
    private final List<LlmCallDetails> llmCalls;
    private long durationMs;
    private String failureReason;
    private ContextLevel pesapPredictedContextLevel;
    private final List<String> pesapWarnings;

    public StepDetails(final String rawInstruction)
    {
        this.rawInstruction = rawInstruction;
        this.expandedInstruction = rawInstruction;
        this.actions = Collections.synchronizedList(new ArrayList<>());
        this.llmCalls = Collections.synchronizedList(new ArrayList<>());
        this.pesapWarnings = Collections.synchronizedList(new ArrayList<>());
    }

    public final String getRawInstruction()
    {
        return this.rawInstruction;
    }

    public final String getExpandedInstruction()
    {
        return this.expandedInstruction;
    }

    public final void setExpandedInstruction(final String expandedInstruction)
    {
        this.expandedInstruction = expandedInstruction;
    }

    public final List<Action> getActions()
    {
        return this.actions;
    }

    public final List<LlmCallDetails> getLlmCalls()
    {
        return this.llmCalls;
    }

    public final long getDurationMs()
    {
        return this.durationMs;
    }

    public final void setDurationMs(final long durationMs)
    {
        this.durationMs = durationMs;
    }

    public final String getFailureReason()
    {
        return this.failureReason;
    }

    public final void setFailureReason(final String failureReason)
    {
        this.failureReason = failureReason;
    }

    public final ContextLevel getPesapPredictedContextLevel()
    {
        return this.pesapPredictedContextLevel;
    }

    public final void setPesapPredictedContextLevel(final ContextLevel pesapPredictedContextLevel)
    {
        this.pesapPredictedContextLevel = pesapPredictedContextLevel;
    }

    public final List<String> getPesapWarnings()
    {
        return this.pesapWarnings;
    }

    public final void addPesapWarning(final String warning)
    {
        this.pesapWarnings.add(warning);
    }
}
