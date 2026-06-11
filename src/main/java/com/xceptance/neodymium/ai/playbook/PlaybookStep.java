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
package com.xceptance.neodymium.ai.playbook;

import java.util.ArrayList;
import java.util.List;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor.ActionExecutionException;
import com.xceptance.neodymium.ai.core.ContextLevel;

/**
 * Represents a single execution step within a {@link Playbook}.
 * Contains the original natural language instruction prompt, the generative AI's reasoning,
 * the resulting executable {@link Action}s, perceptual screenshot hashes, healed states,
 * and optional expected visual/functional bug and failure annotations.
 *
 * @author AI-generated: Gemini 2.5 Flash
 */
public final class PlaybookStep
{
    private String promptLine;
    private String reasoning;
    private List<Action> actions;
    private String screenshotHash;
    private boolean expectedFailure;
    private String bugId;
    private String expectedErrorType;
    private String expectedErrorMessage;
    private transient String lastFailure;

    /**
     * The context level that was successfully used for this step.
     * {@code null} if not yet recorded. Used to skip predictable escalation
     * failures on subsequent runs and healing attempts.
     */
    private ContextLevel healedContextLevel;

    /**
     * Constructs an empty PlaybookStep with an empty action list.
     */
    public PlaybookStep()
    {
        this.actions = new ArrayList<>();
    }

    /**
     * Constructs a PlaybookStep initialized with prompt, reasoning, and actions.
     *
     * @param promptLine the natural language instruction prompt
     * @param reasoning  the reasoning provided by the AI
     * @param actions    the resolved list of executable actions
     */
    public PlaybookStep(final String promptLine, final String reasoning, final List<Action> actions)
    {
        this.promptLine = promptLine;
        this.reasoning = reasoning;
        this.actions = actions;
    }

    /**
     * Gets the original natural language prompt line.
     *
     * @return the prompt line string
     */
    public String getPromptLine()
    {
        return this.promptLine;
    }

    /**
     * Sets the natural language prompt line.
     *
     * @param promptLine the new prompt line string to set
     */
    public void setPromptLine(final String promptLine)
    {
        this.promptLine = promptLine;
    }

    /**
     * Gets the reasoning trace of the AI for this step.
     *
     * @return the reasoning string
     */
    public String getReasoning()
    {
        return this.reasoning;
    }

    /**
     * Sets the reasoning trace of the AI for this step.
     *
     * @param reasoning the new reasoning string to set
     */
    public void setReasoning(final String reasoning)
    {
        this.reasoning = reasoning;
    }

    /**
     * Gets the list of resolved, executable actions for this step.
     *
     * @return the list of {@link Action}s
     */
    public List<Action> getActions()
    {
        return this.actions;
    }

    /**
     * Sets the list of resolved, executable actions for this step.
     *
     * @param actions the new list of {@link Action}s
     */
    public void setActions(final List<Action> actions)
    {
        this.actions = actions;
    }

    /**
     * Gets the computed perceptual screenshot hash of the SUT page before this step was executed.
     *
     * @return the screenshot dHash hexadecimal string
     */
    public String getScreenshotHash()
    {
        return this.screenshotHash;
    }

    /**
     * Sets the computed perceptual screenshot hash of the SUT page.
     *
     * @param screenshotHash the screenshot dHash hexadecimal string to set
     */
    public void setScreenshotHash(final String screenshotHash)
    {
        this.screenshotHash = screenshotHash;
    }

    /**
     * Checks if this step failed execution during the current run.
     *
     * @return {@code true} if a failure message is present, {@code false} otherwise
     */
    public boolean failed()
    {
        return this.lastFailure != null;
    }

    /**
     * Records the execution failure exception encountered on this step.
     *
     * @param e the execution exception to record
     */
    public void setFailure(final ActionExecutionException e)
    {
        this.lastFailure = (e != null) ? e.getMessage() : null;
    }
    
    /**
     * Gets the last recorded execution failure message.
     *
     * @return the failure message, or {@code null} if no failure occurred
     */
    public String getLastFailure()
    {
        return this.lastFailure;
    }

    /**
     * Returns the context level that was needed when this step was last
     * successfully healed, or {@code null} if this step was never healed.
     *
     * @return the healed context level, or {@code null}
     */
    public ContextLevel getHealedContextLevel()
    {
        return this.healedContextLevel;
    }

    /**
     * Records the context level that was needed for healing. This allows
     * subsequent healing attempts to start at this level instead of
     * {@link ContextLevel#LEAN}, avoiding a predictable wasted call.
     *
     * @param level the context level that succeeded during healing
     */
    public void setHealedContextLevel(final ContextLevel level)
    {
        this.healedContextLevel = level;
    }

    /**
     * Checks if this step is expected to fail (annotated visual or functional bug).
     *
     * @return {@code true} if expected to fail, {@code false} otherwise
     */
    public boolean isExpectedFailure()
    {
        return this.expectedFailure;
    }

    /**
     * Sets whether this step is expected to fail.
     *
     * @param expectedFailure the expected failure status to set
     */
    public void setExpectedFailure(final boolean expectedFailure)
    {
        this.expectedFailure = expectedFailure;
    }

    /**
     * Gets the associated bug/defect tracker ID for this expected failure.
     *
     * @return the bug ID string
     */
    public String getBugId()
    {
        return this.bugId;
    }

    /**
     * Sets the associated bug/defect tracker ID.
     *
     * @param bugId the bug ID string to set
     */
    public void setBugId(final String bugId)
    {
        this.bugId = bugId;
    }

    /**
     * Gets the expected error class or exception type name for the failure.
     *
     * @return the expected error type name
     */
    public String getExpectedErrorType()
    {
        return this.expectedErrorType;
    }

    /**
     * Sets the expected error class or exception type name.
     *
     * @param expectedErrorType the expected error type to set
     */
    public void setExpectedErrorType(final String expectedErrorType)
    {
        this.expectedErrorType = expectedErrorType;
    }

    /**
     * Gets the expected exception message string or substring.
     *
     * @return the expected error message substring
     */
    public String getExpectedErrorMessage()
    {
        return this.expectedErrorMessage;
    }

    /**
     * Sets the expected exception message string or substring.
     *
     * @param expectedErrorMessage the expected error message to set
     */
    public void setExpectedErrorMessage(final String expectedErrorMessage)
    {
        this.expectedErrorMessage = expectedErrorMessage;
    }

    @Override
    public String toString()
    {
        return "PlaybookStep [promptLine=" + promptLine + ", reasoning=" + reasoning + ", actions=" + actions + ", screenshotHash=" + screenshotHash + ", expectedFailure=" + expectedFailure + ", bugId=" + bugId + ", expectedErrorType=" + expectedErrorType + ", expectedErrorMessage=" + expectedErrorMessage + "]";
    }
}
