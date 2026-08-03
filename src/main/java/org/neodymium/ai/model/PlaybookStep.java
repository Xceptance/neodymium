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
     * Flag indicating that this step should bypass the replay cache and execute live.
     */
    private boolean noReplay;

    /**
     * Flag indicating that this step is optional, meaning failures do not break the test.
     */
    private boolean optional;

    /**
     * Flag indicating that this step expects a bug (failing is expected, success is a failure).
     */
    private boolean bug;

    /**
     * Optional description or ID of the bug.
     */
    private String bugDetails;

    /**
     * Flag indicating that test execution should continue even if this step fails or has unexpected success.
     */
    private boolean continueOnError;

    /**
     * Flag indicating that self-healing is disabled for this step.
     */
    private boolean noHealing;

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
     * The SHA-256 hash of the source YAML playbook from which this recording step was generated.
     */
    private String sourceYamlHash;

    /**
     * The schema version of the recorded playbook step.
     */
    private String schemaVersion = "2.0";

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
        setInstruction(instruction);
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
        if (instruction != null)
        {
            String cleaned = instruction;

            final java.util.regex.Pattern noReplayPattern = java.util.regex.Pattern.compile("(?i)\\(\\s*no-replay\\s*\\)");
            if (noReplayPattern.matcher(cleaned).find())
            {
                this.noReplay = true;
                cleaned = cleaned.replaceAll("(?i)\\s*\\(\\s*no-replay\\s*\\)\\s*", " ");
            }

            final java.util.regex.Pattern optionalPattern = java.util.regex.Pattern.compile("(?i)\\(\\s*(optional|soft)\\s*\\)");
            if (optionalPattern.matcher(cleaned).find())
            {
                this.optional = true;
                cleaned = cleaned.replaceAll("(?i)\\s*\\(\\s*(optional|soft)\\s*\\)\\s*", " ");
            }

            final java.util.regex.Pattern bugPattern = java.util.regex.Pattern.compile("(?i)\\(\\s*bug(?:\\s*:\\s*([^)]+))?\\s*\\)");
            final java.util.regex.Matcher bugMatcher = bugPattern.matcher(cleaned);
            if (bugMatcher.find())
            {
                this.bug = true;
                this.bugDetails = bugMatcher.group(1) != null ? bugMatcher.group(1).trim() : null;
                cleaned = cleaned.replaceAll("(?i)\\s*\\(\\s*bug(?:\\s*:\\s*[^)]+)?\\s*\\)\\s*", " ");
            }

            final java.util.regex.Pattern continueOnErrorPattern = java.util.regex.Pattern.compile("(?i)\\(\\s*continue-on-error\\s*\\)");
            if (continueOnErrorPattern.matcher(cleaned).find())
            {
                this.continueOnError = true;
                cleaned = cleaned.replaceAll("(?i)\\s*\\(\\s*continue-on-error\\s*\\)\\s*", " ");
            }

            final java.util.regex.Pattern noHealingPattern = java.util.regex.Pattern.compile("(?i)\\(\\s*no-healing\\s*\\)");
            if (noHealingPattern.matcher(cleaned).find())
            {
                this.noHealing = true;
                cleaned = cleaned.replaceAll("(?i)\\s*\\(\\s*no-healing\\s*\\)\\s*", " ");
            }

            this.instruction = cleaned.trim();
        }
        else
        {
            this.instruction = instruction;
        }
    }

    /**
     * Checks if this step is marked as no-replay.
     * If this step or any of its parent steps is no-replay, returns true.
     *
     * @return true if no-replay, false otherwise
     */
    public boolean isNoReplay()
    {
        if (this.noReplay)
        {
            return true;
        }
        if (this.parent != null)
        {
            return this.parent.isNoReplay();
        }
        return false;
    }

    /**
     * Sets the no-replay flag.
     *
     * @param noReplay the no-replay flag to set
     */
    public void setNoReplay(final boolean noReplay)
    {
        this.noReplay = noReplay;
    }

    /**
     * Checks if this step is marked as optional.
     * If this step or any of its parent steps is optional, returns true.
     *
     * @return true if optional, false otherwise
     */
    public boolean isOptional()
    {
        if (this.optional)
        {
            return true;
        }
        if (this.parent != null)
        {
            return this.parent.isOptional();
        }
        return false;
    }

    /**
     * Sets the optional flag.
     *
     * @param optional the optional flag to set
     */
    public void setOptional(final boolean optional)
    {
        this.optional = optional;
    }

    /**
     * Checks if this step expects a bug.
     * If this step or any of its parent steps expects a bug, returns true.
     *
     * @return true if bug expected, false otherwise
     */
    public boolean isBug()
    {
        if (this.bug)
        {
            return true;
        }
        if (this.parent != null)
        {
            return this.parent.isBug();
        }
        return false;
    }

    /**
     * Sets the bug flag.
     *
     * @param bug the bug flag to set
     */
    public void setBug(final boolean bug)
    {
        this.bug = bug;
    }

    /**
     * Gets the bug details.
     * If this step does not have bug details, it will check the parent chain.
     *
     * @return the bug details, or null
     */
    public String getBugDetails()
    {
        if (this.bugDetails != null)
        {
            return this.bugDetails;
        }
        if (this.parent != null)
        {
            return this.parent.getBugDetails();
        }
        return null;
    }

    /**
     * Sets the bug details.
     *
     * @param bugDetails the bug details to set
     */
    public void setBugDetails(final String bugDetails)
    {
        this.bugDetails = bugDetails;
    }

    /**
     * Checks if this step continues on error.
     * If this step or any of its parent steps continues on error, returns true.
     *
     * @return true if continue on error, false otherwise
     */
    public boolean isContinueOnError()
    {
        if (this.continueOnError)
        {
            return true;
        }
        if (this.parent != null)
        {
            return this.parent.isContinueOnError();
        }
        return false;
    }

    /**
     * Sets the continue-on-error flag.
     *
     * @param continueOnError the continueOnError flag to set
     */
    public void setContinueOnError(final boolean continueOnError)
    {
        this.continueOnError = continueOnError;
    }

    /**
     * Checks if this step has self-healing disabled.
     * If this step or any of its parent steps has self-healing disabled, returns true.
     *
     * @return true if self-healing is disabled, false otherwise
     */
    public boolean isNoHealing()
    {
        if (this.noHealing)
        {
            return true;
        }
        if (this.parent != null)
        {
            return this.parent.isNoHealing();
        }
        return false;
    }

    /**
     * Sets the no-healing flag.
     *
     * @param noHealing the no-healing flag to set
     */
    public void setNoHealing(final boolean noHealing)
    {
        this.noHealing = noHealing;
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

    /**
     * Returns the SHA-256 hash of the source YAML playbook file.
     *
     * @return the source YAML hash or null if unrecorded
     */
    public String getSourceYamlHash()
    {
        return this.sourceYamlHash;
    }

    /**
     * Sets the SHA-256 hash of the source YAML playbook file.
     *
     * @param sourceYamlHash the SHA-256 hash to set
     */
    public void setSourceYamlHash(final String sourceYamlHash)
    {
        this.sourceYamlHash = sourceYamlHash;
    }

    /**
     * Returns the schema version of this recorded step.
     *
     * @return the schema version string
     */
    public String getSchemaVersion()
    {
        return this.schemaVersion;
    }

    /**
     * Sets the schema version of this recorded step.
     *
     * @param schemaVersion the schema version to set
     */
    public void setSchemaVersion(final String schemaVersion)
    {
        this.schemaVersion = schemaVersion;
    }
}
