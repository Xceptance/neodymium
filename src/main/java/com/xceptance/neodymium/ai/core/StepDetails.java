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
    private String pesapDirection;
    private String pesapStepType;
    private String pesapExpectedTargetTagName;
    private boolean pesapPageNavigation;
    private boolean pesapRequiresJavaMethods;
    private boolean replayed;
    private boolean directParse;

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

    /**
     * Returns the JIT PESAP direction guidance for this step.
     *
     * @return the direction hint, or {@code null} if not set
     */
    public final String getPesapDirection()
    {
        return this.pesapDirection;
    }

    /**
     * Sets the JIT PESAP direction guidance for this step.
     *
     * @param pesapDirection the direction hint
     */
    public final void setPesapDirection(final String pesapDirection)
    {
        this.pesapDirection = pesapDirection;
    }

    /**
     * Checks if this step was replayed from a playbook.
     *
     * @return true if replayed, false otherwise
     */
    public final boolean isReplayed()
    {
        return this.replayed;
    }

    /**
     * Sets whether this step was replayed from a playbook.
     *
     * @param replayed the replay status
     */
    public final void setReplayed(final boolean replayed)
    {
        this.replayed = replayed;
    }

    /**
     * Checks if this step was resolved via direct parsing (no-LLM match).
     *
     * @return true if directly parsed, false otherwise
     */
    public final boolean isDirectParse()
    {
        return this.directParse;
    }

    /**
     * Sets whether this step was resolved via direct parsing.
     *
     * @param directParse the direct parse status
     */
    public final void setDirectParse(final boolean directParse)
    {
        this.directParse = directParse;
    }

    /**
     * Gets the predicted PESAP step type.
     *
     * @return the step type
     */
    public final String getPesapStepType()
    {
        return this.pesapStepType;
    }

    /**
     * Sets the predicted PESAP step type.
     *
     * @param pesapStepType the step type
     */
    public final void setPesapStepType(final String pesapStepType)
    {
        this.pesapStepType = pesapStepType;
    }

    /**
     * Gets the predicted PESAP expected target tag name.
     *
     * @return the target tag name
     */
    public final String getPesapExpectedTargetTagName()
    {
        return this.pesapExpectedTargetTagName;
    }

    /**
     * Sets the predicted PESAP expected target tag name.
     *
     * @param pesapExpectedTargetTagName the target tag name
     */
    public final void setPesapExpectedTargetTagName(final String pesapExpectedTargetTagName)
    {
        this.pesapExpectedTargetTagName = pesapExpectedTargetTagName;
    }

    /**
     * Checks if PESAP predicted that this step would cause page navigation.
     *
     * @return true if page navigation is predicted, false otherwise
     */
    public final boolean isPesapPageNavigation()
    {
        return this.pesapPageNavigation;
    }

    /**
     * Sets whether PESAP predicted that this step would cause page navigation.
     *
     * @param pesapPageNavigation the page navigation prediction
     */
    public final void setPesapPageNavigation(final boolean pesapPageNavigation)
    {
        this.pesapPageNavigation = pesapPageNavigation;
    }

    /**
     * Checks if PESAP predicted that this step requires custom Java methods.
     *
     * @return true if custom Java methods are predicted, false otherwise
     */
    public final boolean isPesapRequiresJavaMethods()
    {
        return this.pesapRequiresJavaMethods;
    }

    /**
     * Sets whether PESAP predicted that this step requires custom Java methods.
     *
     * @param pesapRequiresJavaMethods the custom Java methods prediction
     */
    public final void setPesapRequiresJavaMethods(final boolean pesapRequiresJavaMethods)
    {
        this.pesapRequiresJavaMethods = pesapRequiresJavaMethods;
    }

    /**
     * Extracts and returns the reasoning from the first LLM call of this step, if present.
     *
     * @return the reasoning explanation, or an empty string if no LLM calls were made or no reasoning is present
     */
    public final String getReasoning()
    {
        if (this.llmCalls.isEmpty())
        {
            return "";
        }
        return this.llmCalls.get(0).getReasoning();
    }

    private String originalUnsplitInstruction;

    /**
     * Gets the original unsplit instruction if this is a split virtual step.
     *
     * @return the original compound instruction, or {@code null}
     */
    public final String getOriginalUnsplitInstruction()
    {
        return this.originalUnsplitInstruction;
    }

    /**
     * Sets the original unsplit instruction if this is a split virtual step.
     *
     * @param originalUnsplitInstruction the original compound instruction
     */
    public final void setOriginalUnsplitInstruction(final String originalUnsplitInstruction)
    {
        this.originalUnsplitInstruction = originalUnsplitInstruction;
    }
}
