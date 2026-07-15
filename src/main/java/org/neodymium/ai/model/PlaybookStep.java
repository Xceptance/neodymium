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

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.neodymium.ai.action.Action;

/**
 * Represents a single logical instruction/step within a Playbook.
 * Can be a composite parent containing nested sub-steps.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class PlaybookStep
{
    /**
     * The natural language instruction describing what needs to be executed in this step.
     */
    private String instruction;

    /**
     * Nested child steps in the composite hierarchy if this step was split or structured.
     */
    private final List<PlaybookStep> subSteps = new ArrayList<>();

    /**
     * The parent playbook step in the composite hierarchy, if any.
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    private transient PlaybookStep parent;

    /**
     * The concrete executed actions list associated with this step.
     */
    private final List<Action> actions = new ArrayList<>();

    /**
     * The current execution state of this step.
     */
    private PlaybookStepStatus status = PlaybookStepStatus.PENDING;

    /**
     * Flag indicating whether the step execution failed.
     */
    private boolean failed;

    /**
     * The error trace or failure message describing the cause of execution failure.
     */
    private String failureReason;

    /**
     * The baseline state text content (e.g. HTML/DOM) recorded for this step.
     */
    private String baselineState;

    /**
     * The perceptual screenshot hash (dHash) of the page visual state.
     */
    private String screenshotHash;

    /**
     * The line number in the source file where this step is defined.
     */
    private int lineNumber = -1;

    /**
     * The source file identifier where this step is defined.
     */
    private String sourceFile;

    /**
     * Returns the line number in the source file.
     *
     * @return the line number, or -1 if unknown
     */
    public int getLineNumber()
    {
        return this.lineNumber;
    }

    /**
     * Sets the line number in the source file.
     *
     * @param lineNumber the line number to set
     */
    public void setLineNumber(final int lineNumber)
    {
        this.lineNumber = lineNumber;
    }

    /**
     * Returns the source file identifier.
     *
     * @return the source file name/path, or null if unknown
     */
    public String getSourceFile()
    {
        return this.sourceFile;
    }

    /**
     * Sets the source file identifier.
     *
     * @param sourceFile the source file to set
     */
    public void setSourceFile(final String sourceFile)
    {
        this.sourceFile = sourceFile;
    }

    /**
     * Constructs an empty PlaybookStep for serialization.
     */
    public PlaybookStep()
    {
    }

    /**
     * Constructs a PlaybookStep with a natural language instruction.
     *
     * @param instruction the natural language instruction prompt
     */
    public PlaybookStep(final String instruction)
    {
        this.instruction = instruction;
    }

    /**
     * Returns the natural language instruction prompt.
     *
     * @return the instruction prompt
     */
    public String getInstruction()
    {
        return this.instruction;
    }

    /**
     * Sets the natural language instruction prompt.
     *
     * @param instruction the instruction prompt to set
     */
    public void setInstruction(final String instruction)
    {
        this.instruction = instruction;
    }

    /**
     * Returns the nested sub-steps collection of this step.
     *
     * @return the nested sub-steps list
     */
    public List<PlaybookStep> getSubSteps()
    {
        return this.subSteps;
    }

    /**
     * Sets the nested sub-steps collection of this step.
     *
     * @param subSteps the sub-steps to set
     */
    public void setSubSteps(final List<PlaybookStep> subSteps)
    {
        this.subSteps.clear();
        if (subSteps != null)
        {
            this.subSteps.addAll(subSteps);
        }
    }

    /**
     * Returns the concrete executed actions list of this step (leaves only).
     *
     * @return the actions list
     */
    public List<Action> getActions()
    {
        return this.actions;
    }

    /**
     * Sets the concrete executed actions list of this step.
     *
     * @param actions the actions list to set
     */
    public void setActions(final List<Action> actions)
    {
        this.actions.clear();
        if (actions != null)
        {
            this.actions.addAll(actions);
        }
    }

    /**
     * Returns the parent playbook step, if any.
     *
     * @return the parent playbook step
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public PlaybookStep getParent()
    {
        return this.parent;
    }

    /**
     * Sets the parent playbook step.
     *
     * @param parent the parent step to set
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public void setParent(final PlaybookStep parent)
    {
        this.parent = parent;
    }

    /**
     * Checks if this step is a visual-only or layout verification step.
     *
     * @return true if the instruction indicates a visual verification, false otherwise
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isVisualStep()
    {
        if (this.instruction == null)
        {
            return false;
        }
        final String lower = this.instruction.toLowerCase();
        return lower.contains("(visual)") || lower.contains("(layout)");
    }

    /**
     * Returns the current execution status of this step.
     *
     * @return the step status
     */
    public PlaybookStepStatus getStatus()
    {
        return this.status;
    }

    /**
     * Sets the execution status of this step.
     *
     * @param status the step status to set
     */
    public void setStatus(final PlaybookStepStatus status)
    {
        this.status = status;
    }

    /**
     * Checks if this step is a composite step containing sub-steps.
     *
     * @return true if sub-steps collection is not empty, false otherwise
     */
    public boolean isComposite()
    {
        return !this.subSteps.isEmpty();
    }

    /**
     * Checks if this step has failed execution.
     *
     * @return true if failed, false otherwise
     */
    public boolean isFailed()
    {
        return this.failed;
    }

    /**
     * Sets the failure flag of this step.
     *
     * @param failed the failure status flag to set
     */
    public void setFailed(final boolean failed)
    {
        this.failed = failed;
    }

    /**
     * Returns the failure reason trace if execution failed.
     *
     * @return the failure reason string or null
     */
    public String getFailureReason()
    {
        return this.failureReason;
    }

    /**
     * Sets the failure reason trace of this step.
     *
     * @param failureReason the failure reason string to set
     */
    public void setFailureReason(final String failureReason)
    {
        this.failureReason = failureReason;
    }

    /**
     * Returns the baseline state text content recorded for this step.
     *
     * @return the baseline state string or null
     */
    public String getBaselineState()
    {
        return this.baselineState;
    }

    /**
     * Sets the baseline state text content recorded for this step.
     *
     * @param baselineState the baseline state string to set
     */
    public void setBaselineState(final String baselineState)
    {
        this.baselineState = baselineState;
    }

    /**
     * Returns the screenshot hash (dHash) recorded for this step.
     *
     * @return the screenshot hash hex string or null
     */
    public String getScreenshotHash()
    {
        return this.screenshotHash;
    }

    /**
     * Sets the screenshot hash (dHash) recorded for this step.
     *
     * @param screenshotHash the screenshot hash hex string to set
     */
    public void setScreenshotHash(final String screenshotHash)
    {
        this.screenshotHash = screenshotHash;
    }
}
