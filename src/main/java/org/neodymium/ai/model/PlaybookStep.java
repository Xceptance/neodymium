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
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.util.ScreenshotHasher;

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
     * The matrix dimension for the perceptual SSIM screenshot hash (e.g. 128 for 128x128 full-page or 64 for 64x64 tile/legacy).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer screenshotHashDim;

    /**
     * Evaluated SSIM score against baseline during replay.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double ssimScore;

    /**
     * Minimum required SSIM score for visual gate pass.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double ssimMinScore;

    /**
     * PNG data URI of the recorded baseline SSIM luminance matrix.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String baselineMatrixPng;

    /**
     * PNG data URI of the live replay SSIM luminance matrix.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String replayMatrixPng;

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
     * The target automation framework for which this step was recorded (e.g., SELENIUM_SELENIDE or PLAYWRIGHT).
     */
    private String targetFramework = "SELENIUM_SELENIDE";

    /**
     * The semantic context or intent description of this step.
     */
    private String semanticContext;

    /**
     * The DOM Feature Vector snapshot of the target interactive element.
     */
    private DomFeatureVector domFeatureVector;

    /**
     * The schema version of the recorded playbook step.
     */
    private String schemaVersion = "3.0";

    /**
     * The context level (e.g. VISUAL_LEAN, LEAN) recorded for this step during execution.
     */
    private String contextLevel;

    /**
     * AI reasoning explanation generated for this step.
     */
    private String reasoning;

    /**
     * The recorded execution duration in milliseconds.
     */
    private Long durationMs;

    /**
     * The recorded pre-step delay in milliseconds.
     */
    private Long delayMs;

    /**
     * Returns the recorded context level for this step.
     *
     * @return the context level name, or null if not recorded
     */
    public String getContextLevel()
    {
        return this.contextLevel;
    }

    /**
     * Sets the recorded context level for this step.
     *
     * @param contextLevel the context level name to set
     */
    public void setContextLevel(final String contextLevel)
    {
        this.contextLevel = contextLevel;
    }

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
     * Traverses up the parent hierarchy to find the top-most root playbook step.
     *
     * @return the top-most root playbook step, or this if this step has no parent
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public PlaybookStep getRootStep()
    {
        PlaybookStep current = this;
        while (current.parent != null)
        {
            current = current.parent;
        }
        return current;
    }

    public static final Pattern VISUAL_FULL_PATTERN = Pattern.compile("(?i)\\(\\s*visual\\s*:\\s*full\\s*\\)");
    public static final Pattern VISUAL_PATTERN = Pattern.compile("(?i)\\(\\s*visual(?:\\s*:\\s*full)?\\s*\\)");
    public static final Pattern LAYOUT_PATTERN = Pattern.compile("(?i)\\(\\s*layout\\s*\\)");
    public static final Pattern HINT_PATTERN = Pattern.compile("(?i)\\(\\s*hint\\s*:\\s*[^)]+\\)");

    /**
     * Checks if this step provides an explicit selector hint.
     *
     * @return true if the instruction contains (hint: <selector>), false otherwise
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isHintStep()
    {
        if (this.instruction == null)
        {
            return false;
        }
        return HINT_PATTERN.matcher(this.instruction).find();
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
        return VISUAL_PATTERN.matcher(this.instruction).find() || LAYOUT_PATTERN.matcher(this.instruction).find();
    }

    /**
     * Checks if this step explicitly requests full-page visual context.
     *
     * @return true if the instruction contains (visual: full), (visual:full), false otherwise
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isFullPageVisualStep()
    {
        if (this.instruction == null)
        {
            return false;
        }
        return VISUAL_FULL_PATTERN.matcher(this.instruction).find() || LAYOUT_PATTERN.matcher(this.instruction).find();
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
     * Returns the matrix dimension of the perceptual screenshot hash (e.g. 128 for 128x128, 64 for 64x64).
     *
     * @return the matrix dimension or null if unrecorded
     */
    public Integer getScreenshotHashDim()
    {
        return this.screenshotHashDim;
    }

    /**
     * Sets the matrix dimension of the perceptual screenshot hash.
     *
     * @param screenshotHashDim the matrix dimension (e.g. 128 or 64)
     */
    public void setScreenshotHashDim(final Integer screenshotHashDim)
    {
        this.screenshotHashDim = screenshotHashDim;
    }

    /**
     * Returns the evaluated SSIM score during replay.
     *
     * @return the SSIM score or null
     */
    public Double getSsimScore()
    {
        return this.ssimScore;
    }

    /**
     * Sets the evaluated SSIM score during replay.
     *
     * @param ssimScore the SSIM score
     */
    public void setSsimScore(final Double ssimScore)
    {
        this.ssimScore = ssimScore;
    }

    /**
     * Returns the minimum required SSIM score for this step.
     *
     * @return the minimum required SSIM score or null
     */
    public Double getSsimMinScore()
    {
        return this.ssimMinScore;
    }

    /**
     * Sets the minimum required SSIM score for this step.
     *
     * @param ssimMinScore the minimum required SSIM score
     */
    public void setSsimMinScore(final Double ssimMinScore)
    {
        this.ssimMinScore = ssimMinScore;
    }

    /**
     * Returns the PNG data URI of the recorded baseline SSIM luminance matrix.
     *
     * @return the baseline matrix PNG data URI or null
     */
    public String getBaselineMatrixPng()
    {
        return this.baselineMatrixPng;
    }

    /**
     * Sets the PNG data URI of the recorded baseline SSIM luminance matrix.
     *
     * @param baselineMatrixPng the baseline matrix PNG data URI
     */
    public void setBaselineMatrixPng(final String baselineMatrixPng)
    {
        this.baselineMatrixPng = baselineMatrixPng;
    }

    /**
     * Returns the PNG data URI of the live replay SSIM luminance matrix.
     *
     * @return the replay matrix PNG data URI or null
     */
    public String getReplayMatrixPng()
    {
        return this.replayMatrixPng;
    }

    /**
     * Sets the PNG data URI of the live replay SSIM luminance matrix.
     *
     * @param replayMatrixPng the replay matrix PNG data URI
     */
    public void setReplayMatrixPng(final String replayMatrixPng)
    {
        this.replayMatrixPng = replayMatrixPng;
    }

    /**
     * Sets the screenshot hash (dHash) recorded for this step.
     *
     * @param screenshotHash the screenshot hash hex string to set
     */
    public void setScreenshotHash(final String screenshotHash)
    {
        this.screenshotHash = screenshotHash;
        if (this.screenshotHash != null && !this.screenshotHash.isBlank() && this.screenshotHashDim == null)
        {
            final int inferredDim = ScreenshotHasher.getMatrixDimension(screenshotHash);
            if (inferredDim > 0)
            {
                this.screenshotHashDim = inferredDim;
            }
        }
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
     * Returns the target automation framework for which this step was recorded.
     *
     * @return the target framework name (e.g. SELENIUM_SELENIDE or PLAYWRIGHT)
     */
    public String getTargetFramework()
    {
        return this.targetFramework;
    }

    /**
     * Sets the target automation framework for which this step was recorded.
     *
     * @param targetFramework the target framework name to set
     */
    public void setTargetFramework(final String targetFramework)
    {
        this.targetFramework = targetFramework;
    }

    /**
     * Returns the semantic context or intent description of this step.
     *
     * @return the semantic context description
     */
    public String getSemanticContext()
    {
        return this.semanticContext;
    }

    /**
     * Sets the semantic context or intent description of this step.
     *
     * @param semanticContext the semantic context description to set
     */
    public void setSemanticContext(final String semanticContext)
    {
        this.semanticContext = semanticContext;
    }

    /**
     * Returns the DOM Feature Vector snapshot for this step's target element.
     *
     * @return the DOM feature vector
     */
    public DomFeatureVector getDomFeatureVector()
    {
        return this.domFeatureVector;
    }

    /**
     * Sets the DOM Feature Vector snapshot for this step's target element.
     *
     * @param domFeatureVector the DOM feature vector to set
     */
    public void setDomFeatureVector(final DomFeatureVector domFeatureVector)
    {
        this.domFeatureVector = domFeatureVector;
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

    /**
     * Returns the AI reasoning associated with this step.
     *
     * @return the reasoning string, or null if not set
     */
    public String getReasoning()
    {
        return this.reasoning;
    }

    /**
     * Sets the AI reasoning for this step.
     *
     * @param reasoning the reasoning string to set
     */
    public void setReasoning(final String reasoning)
    {
        this.reasoning = reasoning;
    }

    /**
     * Returns the recorded execution duration in milliseconds.
     *
     * @return the execution duration in milliseconds, or {@code null} if not recorded
     */
    public Long getDurationMs()
    {
        return this.durationMs;
    }

    /**
     * Sets the recorded execution duration in milliseconds.
     *
     * @param durationMs the execution duration in milliseconds
     */
    public void setDurationMs(final Long durationMs)
    {
        this.durationMs = durationMs;
    }

    /**
     * Sets the recorded execution duration in milliseconds.
     *
     * @param durationMs the execution duration in milliseconds
     */
    public void setDurationMs(final long durationMs)
    {
        this.durationMs = durationMs;
    }

    /**
     * Returns the recorded pre-step delay in milliseconds.
     *
     * @return the delay in milliseconds, or {@code null} if not recorded
     */
    public Long getDelayMs()
    {
        return this.delayMs;
    }

    /**
     * Sets the recorded pre-step delay in milliseconds.
     *
     * @param delayMs the delay in milliseconds
     */
    public void setDelayMs(final Long delayMs)
    {
        this.delayMs = delayMs;
    }

    /**
     * Sets the recorded pre-step delay in milliseconds.
     *
     * @param delayMs the delay in milliseconds
     */
    public void setDelayMs(final long delayMs)
    {
        this.delayMs = delayMs;
    }
}
