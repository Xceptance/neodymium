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
package org.neodymium.ai.pipeline;

import java.util.ArrayList;
import java.util.List;
import org.neodymium.ai.action.Action;

/**
 * Tracks and aggregates detailed execution statistics for a single playbook step.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class StepStats
{
    private final String instruction;

    private final long startTime;

    private long durationMs;

    private final List<String> contextLevels = new ArrayList<>();

    private final List<Action> actions = new ArrayList<>();

    private int standardCalls;

    private long standardInputTokens;

    private long standardOutputTokens;

    private long standardCachedTokens;

    private int verificationCalls;

    private long verificationInputTokens;

    private long verificationOutputTokens;

    private long verificationCachedTokens;

    private String failureReason;

    private boolean replayed;

    /**
     * Constructs a new StepStats object for an instruction.
     *
     * @param instruction the step instruction text
     * @param startTime the system time when step execution started
     */
    public StepStats(final String instruction, final long startTime)
    {
        this.instruction = instruction;
        this.startTime = startTime;
    }

    /**
     * Gets the instruction of the step.
     *
     * @return the instruction
     */
    public String getInstruction()
    {
        return this.instruction;
    }

    /**
     * Gets the start time of the step.
     *
     * @return the start time in milliseconds
     */
    public long getStartTime()
    {
        return this.startTime;
    }

    /**
     * Gets the duration of the step.
     *
     * @return the duration in milliseconds
     */
    public long getDurationMs()
    {
        return this.durationMs;
    }

    /**
     * Sets the duration of the step.
     *
     * @param durationMs the duration in milliseconds
     */
    public void setDurationMs(final long durationMs)
    {
        this.durationMs = durationMs;
    }

    /**
     * Gets the context levels traversed.
     *
     * @return the list of context levels
     */
    public List<String> getContextLevels()
    {
        return this.contextLevels;
    }

    /**
     * Gets the actions executed.
     *
     * @return the list of actions
     */
    public List<Action> getActions()
    {
        return this.actions;
    }

    /**
     * Gets the standard LLM calls count.
     *
     * @return the count
     */
    public int getStandardCalls()
    {
        return this.standardCalls;
    }

    /**
     * Adds standard LLM call tokens to the aggregates.
     *
     * @param input the input tokens
     * @param output the output tokens
     * @param cached the cached tokens
     */
    public void addStandardCall(final int input, final int output, final int cached)
    {
        this.standardCalls++;
        this.standardInputTokens += input;
        this.standardOutputTokens += output;
        this.standardCachedTokens += cached;
    }

    /**
     * Gets the standard input tokens count.
     *
     * @return the count
     */
    public long getStandardInputTokens()
    {
        return this.standardInputTokens;
    }

    /**
     * Gets the standard output tokens count.
     *
     * @return the count
     */
    public long getStandardOutputTokens()
    {
        return this.standardOutputTokens;
    }

    /**
     * Gets the standard cached tokens count.
     *
     * @return the count
     */
    public long getStandardCachedTokens()
    {
        return this.standardCachedTokens;
    }

    /**
     * Gets the verification LLM calls count.
     *
     * @return the count
     */
    public int getVerificationCalls()
    {
        return this.verificationCalls;
    }

    /**
     * Adds verification LLM call tokens to the aggregates.
     *
     * @param input the input tokens
     * @param output the output tokens
     * @param cached the cached tokens
     */
    public void addVerificationCall(final int input, final int output, final int cached)
    {
        this.verificationCalls++;
        this.verificationInputTokens += input;
        this.verificationOutputTokens += output;
        this.verificationCachedTokens += cached;
    }

    /**
     * Gets the verification input tokens count.
     *
     * @return the count
     */
    public long getVerificationInputTokens()
    {
        return this.verificationInputTokens;
    }

    /**
     * Gets the verification output tokens count.
     *
     * @return the count
     */
    public long getVerificationOutputTokens()
    {
        return this.verificationOutputTokens;
    }

    /**
     * Gets the verification cached tokens count.
     *
     * @return the count
     */
    public long getVerificationCachedTokens()
    {
        return this.verificationCachedTokens;
    }

    /**
     * Gets the failure reason of the step, if any.
     *
     * @return the failure reason, or null if successful
     */
    public String getFailureReason()
    {
        return this.failureReason;
    }

    /**
     * Sets the failure reason of the step.
     *
     * @param failureReason the failure reason
     */
    public void setFailureReason(final String failureReason)
    {
        this.failureReason = failureReason;
    }

    /**
     * Gets whether this step was replayed.
     *
     * @return true if replayed, false if live LLM mode
     */
    public boolean isReplayed()
    {
        return this.replayed;
    }

    /**
     * Sets whether this step was replayed.
     *
     * @param replayed true if replayed, false if live LLM mode
     */
    public void setReplayed(final boolean replayed)
    {
        this.replayed = replayed;
    }
}
