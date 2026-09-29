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
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.prompt.VerificationResult;
import org.neodymium.ai.tool.ToolCall;
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
    @JsonProperty("noReplay")
    private boolean noReplay;

    /**
     * Flag indicating that this step is optional, meaning failures do not break the test.
     */
    @JsonProperty("optional")
    private boolean optional;

    /**
     * Flag indicating that this step expects a bug (failing is expected, success is a failure).
     */
    @JsonProperty("bug")
    private boolean bug;

    /**
     * Optional description or ID of the bug.
     */
    @JsonProperty("bugDetails")
    private String bugDetails;

    /**
     * Flag indicating that test execution should continue even if this step fails or has unexpected success.
     */
    @JsonProperty("continueOnError")
    private boolean continueOnError;

    /**
     * Flag indicating that self-healing is disabled for this step.
     */
    @JsonProperty("noHealing")
    private boolean noHealing;

    /**
     * Nested child steps in the composite hierarchy if this step was split or structured.
     */
    private final List<PlaybookStep> subSteps = new ArrayList<>();

    /**
     * The parent playbook step in the composite hierarchy, if any.
     */
    @JsonIgnore
    private transient PlaybookStep parent;

    /**
     * The concrete executed actions list associated with this step.
     */
    private final List<Action> actions = new ArrayList<>();

    /**
     * The executed tool calls associated with this step in the unified tooling architecture.
     */
    @JsonProperty("toolCalls")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<ToolCall> toolCalls = new ArrayList<>();

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
     * Flag indicating whether full-page screenshot capturing was used for this step's baseline.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean fullPage;

    /**
     * Evaluated SSIM score against baseline during replay.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double ssimScore;

    /**
     * Minimum required SSIM score for visual gate pass.
     */
    @JsonProperty("ssimMinScore")
    @JsonAlias("threshold")
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
     * The semantic outcome verification evaluation result, if verification was executed for this step.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private VerificationResult verificationResult;

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
     * Current schema version for recorded playbook steps.
     */
    public static final String CURRENT_SCHEMA_VERSION = "4.0";

    /**
     * The schema version of the recorded playbook step.
     */
    private String schemaVersion = CURRENT_SCHEMA_VERSION;

    /**
     * The context level (e.g. VISUAL_LEAN, LEAN) recorded for this step during execution.
     */
    private String contextLevel;

    /**
     * AI reasoning explanation generated for this step.
     */
    private String reasoning;

    /**
     * AI reasoning explanations generated across all execution stages for this step.
     */
    private final List<String> reasonings = new ArrayList<>();

    /**
     * The recorded execution duration in milliseconds.
     */
    private Long durationMs;

    /**
     * The timestamp in milliseconds when step execution started.
     */
    private Long startTimeMs;

    /**
     * The recorded pre-step delay in milliseconds.
     */
    private Long delayMs;

    /**
     * Custom execution timeout for this step in milliseconds, or null if default timeout applies.
     */
    private Long timeoutMs;


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

            if (NO_REPLAY_PATTERN.matcher(cleaned).find())
            {
                this.noReplay = true;
                cleaned = cleaned.replaceAll("(?i)\\s*\\(\\s*no-replay\\s*\\)\\s*", " ");
            }

            if (OPTIONAL_PATTERN.matcher(cleaned).find())
            {
                this.optional = true;
                cleaned = cleaned.replaceAll("(?i)\\s*\\(\\s*(optional|soft)\\s*\\)\\s*", " ");
            }

            final Matcher bugMatcher = BUG_PATTERN.matcher(cleaned);
            if (bugMatcher.find())
            {
                this.bug = true;
                this.bugDetails = bugMatcher.group(1) != null ? bugMatcher.group(1).trim() : null;
                cleaned = cleaned.replaceAll("(?i)\\s*\\(\\s*bug(?:\\s*:\\s*[^)]+)?\\s*\\)\\s*", " ");
            }

            if (CONTINUE_ON_ERROR_PATTERN.matcher(cleaned).find())
            {
                this.continueOnError = true;
                cleaned = cleaned.replaceAll("(?i)\\s*\\(\\s*continue-on-error\\s*\\)\\s*", " ");
            }

            if (NO_HEALING_PATTERN.matcher(cleaned).find())
            {
                this.noHealing = true;
                cleaned = cleaned.replaceAll("(?i)\\s*\\(\\s*no-healing\\s*\\)\\s*", " ");
            }

            final Matcher timeoutMatcher = TIMEOUT_PATTERN.matcher(cleaned);
            if (timeoutMatcher.find())
            {
                final long val = Long.parseLong(timeoutMatcher.group(1));
                final String unit = timeoutMatcher.group(2);
                if (unit != null && unit.equalsIgnoreCase("s"))
                {
                    this.timeoutMs = val * 1000L;
                }
                else
                {
                    this.timeoutMs = val;
                }
                cleaned = cleaned.replaceAll("(?i)\\s*\\(\\s*timeout\\s*:\\s*\\d+(?:ms|s)?\\s*\\)\\s*", " ");
            }

            final Matcher standaloneThreshMatcher = STANDALONE_THRESHOLD_PATTERN.matcher(cleaned);
            if (standaloneThreshMatcher.find())
            {
                final Double parsed = parseThreshold(standaloneThreshMatcher.group(1));
                if (parsed != null)
                {
                    this.ssimMinScore = parsed;
                }
                cleaned = cleaned.replaceAll("(?i)\\s*\\(\\s*(?:threshold|ssim|min-score|minScore)\\s*[:=]\\s*[0-9.]+%?\\s*\\)\\s*", " ");
            }

            final Matcher visualMatcher = VISUAL_TAG_PARAM_PATTERN.matcher(cleaned);
            if (visualMatcher.find())
            {
                final String paramStr = visualMatcher.group(1);
                if (paramStr != null && !paramStr.isBlank())
                {
                    final String[] tokens = paramStr.split(",");
                    for (final String rawToken : tokens)
                    {
                        final String token = rawToken.trim();
                        if (token.equalsIgnoreCase("full"))
                        {
                            this.fullPage = true;
                        }
                        else
                        {
                            final Matcher threshMatcher = THRESHOLD_PARAM_PATTERN.matcher(token);
                            if (threshMatcher.matches())
                            {
                                final Double parsed = parseThreshold(threshMatcher.group(1));
                                if (parsed != null)
                                {
                                    this.ssimMinScore = parsed;
                                }
                            }
                            else
                            {
                                final Double parsed = parseThreshold(token);
                                if (parsed != null)
                                {
                                    this.ssimMinScore = parsed;
                                }
                            }
                        }
                    }
                }
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
    @JsonIgnore
    public boolean isNoReplay()
    {
        return hasNoReplayRecursive(true, true);
    }

    private boolean hasNoReplayRecursive(final boolean checkAncestors, final boolean checkDescendants)
    {
        if (this.noReplay)
        {
            return true;
        }
        if (checkAncestors && this.parent != null && this.parent.hasNoReplayRecursive(true, false))
        {
            return true;
        }
        if (checkDescendants && this.subSteps != null)
        {
            for (final PlaybookStep sub : this.subSteps)
            {
                if (sub.hasNoReplayRecursive(false, true))
                {
                    return true;
                }
            }
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
     * If this step, any of its parent steps, or any child sub-step is optional, returns true.
     *
     * @return true if optional, false otherwise
     */
    @JsonIgnore
    public boolean isOptional()
    {
        return hasOptionalRecursive(true, true);
    }

    private boolean hasOptionalRecursive(final boolean checkAncestors, final boolean checkDescendants)
    {
        if (this.optional)
        {
            return true;
        }
        if (checkAncestors && this.parent != null && this.parent.hasOptionalRecursive(true, false))
        {
            return true;
        }
        if (checkDescendants && this.subSteps != null)
        {
            for (final PlaybookStep sub : this.subSteps)
            {
                if (sub.hasOptionalRecursive(false, true))
                {
                    return true;
                }
            }
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
     * If this step, any of its parent steps, or any child sub-step expects a bug, returns true.
     *
     * @return true if bug expected, false otherwise
     */
    @JsonIgnore
    public boolean isBug()
    {
        return hasBugRecursive(true, true);
    }

    private boolean hasBugRecursive(final boolean checkAncestors, final boolean checkDescendants)
    {
        if (this.bug)
        {
            return true;
        }
        if (checkAncestors && this.parent != null && this.parent.hasBugRecursive(true, false))
        {
            return true;
        }
        if (checkDescendants && this.subSteps != null)
        {
            for (final PlaybookStep sub : this.subSteps)
            {
                if (sub.hasBugRecursive(false, true))
                {
                    return true;
                }
            }
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
     * If this step does not have bug details, it will check the parent chain and child sub-steps.
     *
     * @return the bug details, or null
     */
    @JsonIgnore
    public String getBugDetails()
    {
        return getBugDetailsRecursive(true, true);
    }

    private String getBugDetailsRecursive(final boolean checkAncestors, final boolean checkDescendants)
    {
        if (this.bugDetails != null)
        {
            return this.bugDetails;
        }
        if (checkAncestors && this.parent != null)
        {
            final String parentDetails = this.parent.getBugDetailsRecursive(true, false);
            if (parentDetails != null)
            {
                return parentDetails;
            }
        }
        if (checkDescendants && this.subSteps != null)
        {
            for (final PlaybookStep sub : this.subSteps)
            {
                final String subDetails = sub.getBugDetailsRecursive(false, true);
                if (subDetails != null)
                {
                    return subDetails;
                }
            }
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
     * If this step, any of its parent steps, or any child sub-step continues on error, returns true.
     *
     * @return true if continue on error, false otherwise
     */
    @JsonIgnore
    public boolean isContinueOnError()
    {
        return hasContinueOnErrorRecursive(true, true);
    }

    private boolean hasContinueOnErrorRecursive(final boolean checkAncestors, final boolean checkDescendants)
    {
        if (this.continueOnError)
        {
            return true;
        }
        if (checkAncestors && this.parent != null && this.parent.hasContinueOnErrorRecursive(true, false))
        {
            return true;
        }
        if (checkDescendants && this.subSteps != null)
        {
            for (final PlaybookStep sub : this.subSteps)
            {
                if (sub.hasContinueOnErrorRecursive(false, true))
                {
                    return true;
                }
            }
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
     * If this step, any of its parent steps, or any child sub-step has self-healing disabled, returns true.
     *
     * @return true if self-healing is disabled, false otherwise
     */
    @JsonIgnore
    public boolean isNoHealing()
    {
        return hasNoHealingRecursive(true, true);
    }

    private boolean hasNoHealingRecursive(final boolean checkAncestors, final boolean checkDescendants)
    {
        if (this.noHealing)
        {
            return true;
        }
        if (checkAncestors && this.parent != null && this.parent.hasNoHealingRecursive(true, false))
        {
            return true;
        }
        if (checkDescendants && this.subSteps != null)
        {
            for (final PlaybookStep sub : this.subSteps)
            {
                if (sub.hasNoHealingRecursive(false, true))
                {
                    return true;
                }
            }
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
     * Adds an action to the executed actions list.
     *
     * @param action the action to add
     */
    public void addAction(final Action action)
    {
        if (action != null)
        {
            this.actions.add(action);
        }
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
     * Returns the list of executed tool calls for this step. If no tool calls are explicitly
     * recorded but legacy recorded actions exist, transparently synthesizes tool calls from them.
     *
     * @return the unmodifiable tool calls list
     */
    public List<ToolCall> getToolCalls()
    {
        if (this.toolCalls.isEmpty() && !this.actions.isEmpty())
        {
            final List<ToolCall> synthesized = new ArrayList<>();
            for (final Action action : this.actions)
            {
                if (action != null)
                {
                    synthesized.add(action.toToolCall());
                }
            }
            return Collections.unmodifiableList(synthesized);
        }
        return Collections.unmodifiableList(this.toolCalls);
    }

    /**
     * Sets the executed tool calls for this step.
     *
     * @param toolCalls tool calls to set
     */
    public void setToolCalls(final List<ToolCall> toolCalls)
    {
        this.toolCalls.clear();
        if (toolCalls != null)
        {
            this.toolCalls.addAll(toolCalls);
        }
    }

    /**
     * Adds an executed tool call to this step.
     *
     * @param toolCall tool call to add
     */
    public void addToolCall(final ToolCall toolCall)
    {
        if (toolCall != null)
        {
            this.toolCalls.add(toolCall);
        }
    }

    /**
     * Returns the parent playbook step, if any.
     *
     * @return the parent playbook step
     */
    @JsonIgnore
    public PlaybookStep getParent()
    {
        return this.parent;
    }

    /**
     * Sets the parent playbook step.
     *
     * @param parent the parent step to set
     */
    @JsonIgnore
    public void setParent(final PlaybookStep parent)
    {
        this.parent = parent;
    }

    /**
     * Traverses up the parent hierarchy to find the top-most root playbook step.
     *
     * @return the top-most root playbook step, or this if this step has no parent
     */
    @JsonIgnore
    public PlaybookStep getRootStep()
    {
        PlaybookStep current = this;
        while (current.parent != null)
        {
            current = current.parent;
        }
        return current;
    }

    private static final Pattern NO_REPLAY_PATTERN = Pattern.compile("(?i)\\(\\s*no-replay\\s*\\)");
    private static final Pattern OPTIONAL_PATTERN = Pattern.compile("(?i)\\(\\s*(optional|soft)\\s*\\)");
    private static final Pattern BUG_PATTERN = Pattern.compile("(?i)\\(\\s*bug(?:\\s*:\\s*([^)]+))?\\s*\\)");
    private static final Pattern CONTINUE_ON_ERROR_PATTERN = Pattern.compile("(?i)\\(\\s*continue-on-error\\s*\\)");
    private static final Pattern NO_HEALING_PATTERN = Pattern.compile("(?i)\\(\\s*no-healing\\s*\\)");
    private static final Pattern TIMEOUT_PATTERN = Pattern.compile("(?i)\\(\\s*timeout\\s*:\\s*(\\d+)(ms|s)?\\s*\\)");

    public static final Pattern VISUAL_FULL_PATTERN = Pattern.compile("(?i)\\(\\s*visual\\s*:[^)]*\\bfull\\b[^)]*\\)");
    public static final Pattern VISUAL_PATTERN = Pattern.compile("(?i)\\(\\s*visual(?:\\s*:[^)]+)?\\s*\\)");
    private static final Pattern VISUAL_TAG_PARAM_PATTERN = Pattern.compile("(?i)\\(\\s*visual(?:\\s*:\\s*([^)]+))?\\s*\\)");
    private static final Pattern THRESHOLD_PARAM_PATTERN = Pattern.compile("(?i)^(?:threshold|ssim|min-score|minScore)\\s*[:=]\\s*([0-9.]+%?)$");
    private static final Pattern STANDALONE_THRESHOLD_PATTERN = Pattern.compile("(?i)\\(\\s*(?:threshold|ssim|min-score|minScore)\\s*[:=]\\s*([0-9.]+%?)\\s*\\)");
    public static final Pattern LAYOUT_PATTERN = Pattern.compile("(?i)\\(\\s*layout\\s*\\)");
    public static final Pattern HINT_PATTERN = Pattern.compile("(?i)\\(\\s*hint\\s*:\\s*[^)]+\\)");
    public static final Pattern INTERACTIVE_ACTION_PATTERN =
        Pattern.compile("(?i)\\b(type|click|select|clear|submit|fill|press|enter|hover|drag|drop|scroll|check|uncheck|choose)\\b");

    /**
     * Parses a threshold value string into a normalized double between 0.0 and 1.0.
     * Supports percentages (e.g. "98%" -> 0.98) and standard decimals (e.g. "0.98").
     *
     * @param text the raw threshold string
     * @return normalized double threshold, or null if invalid
     */
    public static Double parseThreshold(final String text)
    {
        if (text == null || text.isBlank())
        {
            return null;
        }
        String cleaned = text.trim();
        final boolean isPercent = cleaned.endsWith("%");
        if (isPercent)
        {
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }
        try
        {
            double val = Double.parseDouble(cleaned);
            if (isPercent || val > 1.0)
            {
                val = val / 100.0;
            }
            if (val < 0.0 || val > 1.0)
            {
                return null;
            }
            return val;
        }
        catch (final NumberFormatException e)
        {
            return null;
        }
    }

    /**
     * Checks if this step provides an explicit selector hint.
     *
     * @return true if the instruction contains (hint: <selector>), false otherwise
     */
    @JsonIgnore
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
    @JsonIgnore
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
     * @return true if the fullPage flag is true, or if the instruction contains (visual: full), (visual:full), false otherwise
     */
    @JsonIgnore
    public boolean isFullPageVisualStep()
    {
        if (Boolean.TRUE.equals(this.fullPage))
        {
            return true;
        }
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
     * Checks if this step has child sub-steps (milestones).
     *
     * @return true if sub-steps collection is not empty, false otherwise
     */
    public boolean hasSubSteps()
    {
        return !this.subSteps.isEmpty();
    }

    /**
     * Returns the full composite instruction including any nested sub-steps.
     * If this step has no sub-steps, returns {@link #getInstruction()}.
     *
     * @return full composite instruction
     */
    @JsonIgnore
    public String getFullInstruction()
    {
        if (!hasSubSteps())
        {
            return getInstruction() != null ? getInstruction() : "";
        }

        final StringBuilder sb = new StringBuilder();
        if (this.instruction != null && !this.instruction.isBlank())
        {
            sb.append(this.instruction.trim());
            if (!this.instruction.trim().endsWith(":"))
            {
                sb.append(":");
            }
        }
        for (final PlaybookStep sub : this.subSteps)
        {
            final String subText = sub.getFullInstruction();
            if (!subText.isBlank())
            {
                if (sb.length() > 0)
                {
                    sb.append("\n");
                }
                sb.append("  - ").append(subText.replace("\n", "\n    "));
            }
        }
        return sb.toString();
    }

    /**
     * Checks if any child sub-step contains interactive action verbs (e.g. type, click, submit, clear).
     *
     * @return true if at least one sub-step contains an interactive operation
     */
    @JsonIgnore
    public boolean hasInteractiveSubSteps()
    {
        if (!hasSubSteps())
        {
            return false;
        }
        for (final PlaybookStep sub : this.subSteps)
        {
            final String text = sub.getInstruction();
            if (text != null && INTERACTIVE_ACTION_PATTERN.matcher(text).find())
            {
                return true;
            }
            if (sub.hasInteractiveSubSteps())
            {
                return true;
            }
        }
        return false;
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
     * Returns whether this step captures/requires a full-page screenshot.
     *
     * @return true if full-page screenshot was captured, false or null otherwise
     */
    public Boolean isFullPage()
    {
        return this.fullPage;
    }

    /**
     * Sets whether this step captures/requires a full-page screenshot.
     *
     * @param fullPage true if full-page screenshot
     */
    public void setFullPage(final Boolean fullPage)
    {
        this.fullPage = fullPage;
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
     * Sets the visual assertion threshold alias.
     *
     * @param threshold the threshold score
     */
    @JsonProperty("threshold")
    public void setThreshold(final Double threshold)
    {
        this.ssimMinScore = threshold;
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
        if (this.reasoning != null)
        {
            return this.reasoning;
        }
        if (!this.reasonings.isEmpty())
        {
            return String.join("\n\n", this.reasonings);
        }
        return null;
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
     * Returns all AI reasonings generated across execution stages for this step.
     *
     * @return an unmodifiable list of stage reasonings
     */
    public List<String> getReasonings()
    {
        return Collections.unmodifiableList(this.reasonings);
    }

    /**
     * Adds an AI reasoning explanation for an execution stage.
     *
     * @param reasoning the reasoning string to add
     */
    public void addReasoning(final String reasoning)
    {
        if (reasoning != null && !reasoning.isBlank())
        {
            this.reasonings.add(reasoning);
        }
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
     * Returns the timestamp in milliseconds when step execution started.
     *
     * @return the start timestamp in milliseconds, or {@code null} if not recorded
     */
    public Long getStartTimeMs()
    {
        return this.startTimeMs;
    }

    /**
     * Sets the timestamp in milliseconds when step execution started.
     *
     * @param startTimeMs the start timestamp in milliseconds
     */
    public void setStartTimeMs(final Long startTimeMs)
    {
        this.startTimeMs = startTimeMs;
    }

    /**
     * Sets the timestamp in milliseconds when step execution started.
     *
     * @param startTimeMs the start timestamp in milliseconds
     */
    public void setStartTimeMs(final long startTimeMs)
    {
        this.startTimeMs = startTimeMs;
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

    /**
     * Retrieves the custom timeout for this step in milliseconds, if configured.
     *
     * @return timeout in milliseconds, or null
     */
    public Long getTimeoutMs()
    {
        return this.timeoutMs;
    }

    /**
     * Sets the custom timeout for this step in milliseconds.
     *
     * @param timeoutMs timeout in milliseconds
     */
    public void setTimeoutMs(final Long timeoutMs)
    {
        this.timeoutMs = timeoutMs;
    }

    /**
     * Sets the custom timeout for this step in milliseconds.
     *
     * @param timeoutMs timeout in milliseconds
     */
    public void setTimeoutMs(final long timeoutMs)
    {
        this.timeoutMs = timeoutMs;
    }

    /**
     * Gets the semantic outcome verification evaluation result.
     *
     * @return the verification result, or null if not verified
     */
    public VerificationResult getVerificationResult()
    {
        return this.verificationResult;
    }

    /**
     * Sets the semantic outcome verification evaluation result.
     *
     * @param verificationResult the verification result to set
     */
    public void setVerificationResult(final VerificationResult verificationResult)
    {
        this.verificationResult = verificationResult;
    }
}
