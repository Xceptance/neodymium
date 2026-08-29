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
package org.neodymium.ai.report;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import org.neodymium.ai.prompt.VerificationResult;

/**
 * Data model encapsulating full execution metadata, steps, actions, LLM calls,
 * token metrics, diagnostics, and attachments for preliminary test reporting.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class TestExecutionReport
{
    private String testName;
    private String testClass;
    private String testMethod;
    private String datasetId;
    private String executionMode;
    private String playbookFile;
    private String status = "UNKNOWN";
    private boolean success;
    private long startTimeMs;
    private long endTimeMs;
    private long durationMs;
    private String failureReason;
    private String failureStackTrace;
    private String visualRcaExplanation;

    private final List<String> warnings = new ArrayList<>();
    private final List<ReportStepEntry> steps = new ArrayList<>();
    private final List<ReportLlmCallEntry> llmCalls = new ArrayList<>();
    private final List<ReportScreenshotEntry> screenshots = new ArrayList<>();
    private ReportMetrics metrics = new ReportMetrics();

    /**
     * Constructs a default TestExecutionReport.
     */
    public TestExecutionReport()
    {
    }

    public String getTestName()
    {
        return this.testName;
    }

    public void setTestName(final String testName)
    {
        this.testName = testName;
    }

    public String getTestClass()
    {
        return this.testClass;
    }

    public void setTestClass(final String testClass)
    {
        this.testClass = testClass;
    }

    public String getTestMethod()
    {
        return this.testMethod;
    }

    public void setTestMethod(final String testMethod)
    {
        this.testMethod = testMethod;
    }

    public String getDatasetId()
    {
        return this.datasetId;
    }

    public void setDatasetId(final String datasetId)
    {
        this.datasetId = datasetId;
    }

    public String getExecutionMode()
    {
        return this.executionMode;
    }

    public void setExecutionMode(final String executionMode)
    {
        this.executionMode = executionMode;
    }

    public String getPlaybookFile()
    {
        return this.playbookFile;
    }

    public void setPlaybookFile(final String playbookFile)
    {
        this.playbookFile = playbookFile;
    }

    public String getStatus()
    {
        return this.status;
    }

    public void setStatus(final String status)
    {
        this.status = status;
    }

    public boolean isSuccess()
    {
        return this.success;
    }

    public void setSuccess(final boolean success)
    {
        this.success = success;
    }

    public long getStartTimeMs()
    {
        return this.startTimeMs;
    }

    public void setStartTimeMs(final long startTimeMs)
    {
        this.startTimeMs = startTimeMs;
    }

    public long getEndTimeMs()
    {
        return this.endTimeMs;
    }

    public void setEndTimeMs(final long endTimeMs)
    {
        this.endTimeMs = endTimeMs;
    }

    public long getDurationMs()
    {
        return this.durationMs;
    }

    public void setDurationMs(final long durationMs)
    {
        this.durationMs = durationMs;
    }

    public String getFailureReason()
    {
        return this.failureReason;
    }

    public void setFailureReason(final String failureReason)
    {
        this.failureReason = failureReason;
    }

    public String getFailureStackTrace()
    {
        return this.failureStackTrace;
    }

    public void setFailureStackTrace(final String failureStackTrace)
    {
        this.failureStackTrace = failureStackTrace;
    }

    public String getVisualRcaExplanation()
    {
        return this.visualRcaExplanation;
    }

    public void setVisualRcaExplanation(final String visualRcaExplanation)
    {
        this.visualRcaExplanation = visualRcaExplanation;
    }

    public List<String> getWarnings()
    {
        return Collections.unmodifiableList(this.warnings);
    }

    public void addWarning(final String warning)
    {
        if (warning != null && !this.warnings.contains(warning))
        {
            this.warnings.add(warning);
        }
    }

    public void addWarnings(final List<String> warnings)
    {
        if (warnings != null)
        {
            for (final String w : warnings)
            {
                addWarning(w);
            }
        }
    }

    public List<ReportStepEntry> getSteps()
    {
        return Collections.unmodifiableList(this.steps);
    }

    public void addStep(final ReportStepEntry step)
    {
        if (step != null)
        {
            this.steps.add(step);
        }
    }

    public List<ReportLlmCallEntry> getLlmCalls()
    {
        return Collections.unmodifiableList(this.llmCalls);
    }

    public void addLlmCall(final ReportLlmCallEntry call)
    {
        if (call != null)
        {
            this.llmCalls.add(call);
        }
    }

    public List<ReportScreenshotEntry> getScreenshots()
    {
        return Collections.unmodifiableList(this.screenshots);
    }

    public void addScreenshot(final ReportScreenshotEntry screenshot)
    {
        if (screenshot != null)
        {
            this.screenshots.add(screenshot);
        }
    }

    public ReportMetrics getMetrics()
    {
        return this.metrics;
    }

    public void setMetrics(final ReportMetrics metrics)
    {
        this.metrics = metrics != null ? metrics : new ReportMetrics();
    }

    /**
     * Record of a single executed or planned playbook step.
     */
    public static final class ReportStepEntry
    {
        private int stepIndex;
        private String instruction;
        private String status = "PENDING";
        private long startTimeMs;
        private long durationMs;
        private String sourceFile;
        private int lineNumber;
        private String reasoning;
        private String failureReason;
        private int escalations;
        private String contextLevels;
        private int pesapCalls;
        private long pesapInputTokens;
        private long pesapOutputTokens;
        private long pesapCachedTokens;
        private int standardCalls;
        private long standardInputTokens;
        private long standardOutputTokens;
        private long standardCachedTokens;
        private int verificationCalls;
        private long verificationInputTokens;
        private long verificationOutputTokens;
        private long verificationCachedTokens;
        private int rcaCalls;
        private long rcaInputTokens;
        private long rcaOutputTokens;
        private long rcaCachedTokens;
        private VerificationResult verificationResult;
        private final List<ReportActionEntry> actions = new ArrayList<>();
        private final List<ReportStepEntry> subSteps = new ArrayList<>();
        private final List<ReportLlmCallEntry> llmCalls = new ArrayList<>();
        private final List<ReportScreenshotEntry> screenshots = new ArrayList<>();
        private String rawInstruction;
        private boolean bug;
        private String bugDetails;
        private boolean optional;
        private boolean continueOnError;
        private boolean noHealing;
        private boolean visual;
        private Double ssimScore;
        private Double ssimMinScore;
        private String baselineMatrixPng;
        private String replayMatrixPng;
        private Integer screenshotHashDim;
        private String semanticIntent;

        public ReportStepEntry()
        {
        }

        public ReportStepEntry(final int stepIndex, final String instruction)
        {
            this.stepIndex = stepIndex;
            this.instruction = instruction;
        }

        public int getStepIndex()
        {
            return this.stepIndex;
        }

        public void setStepIndex(final int stepIndex)
        {
            this.stepIndex = stepIndex;
        }

        public String getInstruction()
        {
            return this.instruction;
        }

        public void setInstruction(final String instruction)
        {
            this.instruction = instruction;
        }

        public String getStatus()
        {
            return this.status;
        }

        public void setStatus(final String status)
        {
            this.status = status;
        }

        public long getStartTimeMs()
        {
            return this.startTimeMs;
        }

        public void setStartTimeMs(final long startTimeMs)
        {
            this.startTimeMs = startTimeMs;
        }

        public long getDurationMs()
        {
            return this.durationMs;
        }

        public void setDurationMs(final long durationMs)
        {
            this.durationMs = durationMs;
        }

        public String getSourceFile()
        {
            return this.sourceFile;
        }

        public void setSourceFile(final String sourceFile)
        {
            this.sourceFile = sourceFile;
        }

        public int getLineNumber()
        {
            return this.lineNumber;
        }

        public void setLineNumber(final int lineNumber)
        {
            this.lineNumber = lineNumber;
        }

        public String getReasoning()
        {
            return this.reasoning;
        }

        public void setReasoning(final String reasoning)
        {
            this.reasoning = reasoning;
        }

        public String getFailureReason()
        {
            return this.failureReason;
        }

        public void setFailureReason(final String failureReason)
        {
            this.failureReason = failureReason;
        }

        public int getEscalations()
        {
            return this.escalations;
        }

        public void setEscalations(final int escalations)
        {
            this.escalations = escalations;
        }

        public String getContextLevels()
        {
            return this.contextLevels;
        }

        public void setContextLevels(final String contextLevels)
        {
            this.contextLevels = contextLevels;
        }

        public int getPesapCalls()
        {
            return this.pesapCalls;
        }

        public void setPesapCalls(final int pesapCalls)
        {
            this.pesapCalls = pesapCalls;
        }

        public long getPesapInputTokens()
        {
            return this.pesapInputTokens;
        }

        public void setPesapInputTokens(final long pesapInputTokens)
        {
            this.pesapInputTokens = pesapInputTokens;
        }

        public long getPesapOutputTokens()
        {
            return this.pesapOutputTokens;
        }

        public void setPesapOutputTokens(final long pesapOutputTokens)
        {
            this.pesapOutputTokens = pesapOutputTokens;
        }

        public long getPesapCachedTokens()
        {
            return this.pesapCachedTokens;
        }

        public void setPesapCachedTokens(final long pesapCachedTokens)
        {
            this.pesapCachedTokens = pesapCachedTokens;
        }

        public int getStandardCalls()
        {
            return this.standardCalls;
        }

        public void setStandardCalls(final int standardCalls)
        {
            this.standardCalls = standardCalls;
        }

        public long getStandardInputTokens()
        {
            return this.standardInputTokens;
        }

        public void setStandardInputTokens(final long standardInputTokens)
        {
            this.standardInputTokens = standardInputTokens;
        }

        public long getStandardOutputTokens()
        {
            return this.standardOutputTokens;
        }

        public void setStandardOutputTokens(final long standardOutputTokens)
        {
            this.standardOutputTokens = standardOutputTokens;
        }

        public long getStandardCachedTokens()
        {
            return this.standardCachedTokens;
        }

        public void setStandardCachedTokens(final long standardCachedTokens)
        {
            this.standardCachedTokens = standardCachedTokens;
        }

        public List<ReportActionEntry> getActions()
        {
            return Collections.unmodifiableList(this.actions);
        }

        public void addAction(final ReportActionEntry action)
        {
            if (action != null)
            {
                this.actions.add(action);
            }
        }

        public List<ReportStepEntry> getSubSteps()
        {
            return Collections.unmodifiableList(this.subSteps);
        }

        public void addSubStep(final ReportStepEntry subStep)
        {
            if (subStep != null)
            {
                this.subSteps.add(subStep);
            }
        }

        public void removeSubStepIf(final java.util.function.Predicate<ReportStepEntry> filter)
        {
            if (filter != null)
            {
                this.subSteps.removeIf(filter);
            }
        }

        public List<ReportLlmCallEntry> getLlmCalls()
        {
            return Collections.unmodifiableList(this.llmCalls);
        }

        public void addLlmCall(final ReportLlmCallEntry call)
        {
            if (call != null)
            {
                this.llmCalls.add(call);
            }
        }

        public List<ReportScreenshotEntry> getScreenshots()
        {
            return Collections.unmodifiableList(this.screenshots);
        }

        public void addScreenshot(final ReportScreenshotEntry screenshot)
        {
            if (screenshot != null)
            {
                this.screenshots.add(screenshot);
            }
        }

        public String getRawInstruction()
        {
            return this.rawInstruction;
        }

        public void setRawInstruction(final String rawInstruction)
        {
            this.rawInstruction = rawInstruction;
        }

        public boolean isBug()
        {
            return this.bug;
        }

        public void setBug(final boolean bug)
        {
            this.bug = bug;
        }

        public String getBugDetails()
        {
            return this.bugDetails;
        }

        public void setBugDetails(final String bugDetails)
        {
            this.bugDetails = bugDetails;
        }

        public boolean isOptional()
        {
            return this.optional;
        }

        public void setOptional(final boolean optional)
        {
            this.optional = optional;
        }

        public boolean isContinueOnError()
        {
            return this.continueOnError;
        }

        public void setContinueOnError(final boolean continueOnError)
        {
            this.continueOnError = continueOnError;
        }

        public boolean isNoHealing()
        {
            return this.noHealing;
        }

        public void setNoHealing(final boolean noHealing)
        {
            this.noHealing = noHealing;
        }

        public boolean isVisual()
        {
            return this.visual;
        }

        public void setVisual(final boolean visual)
        {
            this.visual = visual;
        }

        public Double getSsimScore()
        {
            return this.ssimScore;
        }

        public void setSsimScore(final Double ssimScore)
        {
            this.ssimScore = ssimScore;
        }

        public Double getSsimMinScore()
        {
            return this.ssimMinScore;
        }

        public void setSsimMinScore(final Double ssimMinScore)
        {
            this.ssimMinScore = ssimMinScore;
        }

        public String getBaselineMatrixPng()
        {
            return this.baselineMatrixPng;
        }

        public void setBaselineMatrixPng(final String baselineMatrixPng)
        {
            this.baselineMatrixPng = baselineMatrixPng;
        }

        public String getReplayMatrixPng()
        {
            return this.replayMatrixPng;
        }

        public void setReplayMatrixPng(final String replayMatrixPng)
        {
            this.replayMatrixPng = replayMatrixPng;
        }

        public Integer getScreenshotHashDim()
        {
            return this.screenshotHashDim;
        }

        public void setScreenshotHashDim(final Integer screenshotHashDim)
        {
            this.screenshotHashDim = screenshotHashDim;
        }

        public String getSemanticIntent()
        {
            return this.semanticIntent;
        }

        public void setSemanticIntent(final String semanticIntent)
        {
            this.semanticIntent = semanticIntent;
        }

        public int getVerificationCalls()
        {
            return this.verificationCalls;
        }

        public void setVerificationCalls(final int verificationCalls)
        {
            this.verificationCalls = verificationCalls;
        }

        public long getVerificationInputTokens()
        {
            return this.verificationInputTokens;
        }

        public void setVerificationInputTokens(final long verificationInputTokens)
        {
            this.verificationInputTokens = verificationInputTokens;
        }

        public long getVerificationOutputTokens()
        {
            return this.verificationOutputTokens;
        }

        public void setVerificationOutputTokens(final long verificationOutputTokens)
        {
            this.verificationOutputTokens = verificationOutputTokens;
        }

        public long getVerificationCachedTokens()
        {
            return this.verificationCachedTokens;
        }

        public void setVerificationCachedTokens(final long verificationCachedTokens)
        {
            this.verificationCachedTokens = verificationCachedTokens;
        }

        public int getRcaCalls()
        {
            return this.rcaCalls;
        }

        public void setRcaCalls(final int rcaCalls)
        {
            this.rcaCalls = rcaCalls;
        }

        public long getRcaInputTokens()
        {
            return this.rcaInputTokens;
        }

        public void setRcaInputTokens(final long rcaInputTokens)
        {
            this.rcaInputTokens = rcaInputTokens;
        }

        public long getRcaOutputTokens()
        {
            return this.rcaOutputTokens;
        }

        public void setRcaOutputTokens(final long rcaOutputTokens)
        {
            this.rcaOutputTokens = rcaOutputTokens;
        }

        public long getRcaCachedTokens()
        {
            return this.rcaCachedTokens;
        }

        public void setRcaCachedTokens(final long rcaCachedTokens)
        {
            this.rcaCachedTokens = rcaCachedTokens;
        }

        public VerificationResult getVerificationResult()
        {
            return this.verificationResult;
        }

        public void setVerificationResult(final VerificationResult verificationResult)
        {
            this.verificationResult = verificationResult;
        }
    }

    /**
     * Record of a single target action executed during a step.
     */
    public static final class ReportActionEntry
    {
        private String type;
        private String target;
        private String resolvedTarget;
        private String value;
        private String resolvedValue;
        private String description;
        private String reasoning;
        private boolean success;

        public ReportActionEntry()
        {
        }

        public ReportActionEntry(
            final String type,
            final String target,
            final String value,
            final String description,
            final String reasoning,
            final boolean success
        )
        {
            this.type = type;
            this.target = target;
            this.value = value;
            this.description = description;
            this.reasoning = reasoning;
            this.success = success;
        }

        public String getType()
        {
            return this.type;
        }

        public void setType(final String type)
        {
            this.type = type;
        }

        public String getTarget()
        {
            return this.target;
        }

        public void setTarget(final String target)
        {
            this.target = target;
        }

        public String getResolvedTarget()
        {
            return this.resolvedTarget;
        }

        public void setResolvedTarget(final String resolvedTarget)
        {
            this.resolvedTarget = resolvedTarget;
        }

        public String getValue()
        {
            return this.value;
        }

        public void setValue(final String value)
        {
            this.value = value;
        }

        public String getResolvedValue()
        {
            return this.resolvedValue;
        }

        public void setResolvedValue(final String resolvedValue)
        {
            this.resolvedValue = resolvedValue;
        }

        public String getDescription()
        {
            return this.description;
        }

        public void setDescription(final String description)
        {
            this.description = description;
        }

        public String getReasoning()
        {
            return this.reasoning;
        }

        public void setReasoning(final String reasoning)
        {
            this.reasoning = reasoning;
        }

        public boolean isSuccess()
        {
            return this.success;
        }

        public void setSuccess(final boolean success)
        {
            this.success = success;
        }
    }

    /**
     * Record of an LLM provider request/response completion call.
     */
    public static final class ReportLlmCallEntry
    {
        private int stepIndex;
        private String capability;
        private String modelName;
        private long durationMs;
        private long inputTokens;
        private long outputTokens;
        private long cachedTokens;
        private long totalTokens;
        private double estimatedCostUsd;
        private String systemPrompt;
        private String userPrompt;
        private String responseContent;

        public ReportLlmCallEntry()
        {
        }

        public int getStepIndex()
        {
            return this.stepIndex;
        }

        public void setStepIndex(final int stepIndex)
        {
            this.stepIndex = stepIndex;
        }

        public String getCapability()
        {
            return this.capability;
        }

        public void setCapability(final String capability)
        {
            this.capability = capability;
        }

        public String getModelName()
        {
            return this.modelName;
        }

        public void setModelName(final String modelName)
        {
            this.modelName = modelName;
        }

        public long getDurationMs()
        {
            return this.durationMs;
        }

        public void setDurationMs(final long durationMs)
        {
            this.durationMs = durationMs;
        }

        public long getInputTokens()
        {
            return this.inputTokens;
        }

        public void setInputTokens(final long inputTokens)
        {
            this.inputTokens = inputTokens;
        }

        public long getOutputTokens()
        {
            return this.outputTokens;
        }

        public void setOutputTokens(final long outputTokens)
        {
            this.outputTokens = outputTokens;
        }

        public long getCachedTokens()
        {
            return this.cachedTokens;
        }

        public void setCachedTokens(final long cachedTokens)
        {
            this.cachedTokens = cachedTokens;
        }

        public long getTotalTokens()
        {
            return this.totalTokens;
        }

        public void setTotalTokens(final long totalTokens)
        {
            this.totalTokens = totalTokens;
        }

        public double getEstimatedCostUsd()
        {
            return this.estimatedCostUsd;
        }

        public void setEstimatedCostUsd(final double estimatedCostUsd)
        {
            this.estimatedCostUsd = estimatedCostUsd;
        }

        public String getSystemPrompt()
        {
            return this.systemPrompt;
        }

        public void setSystemPrompt(final String systemPrompt)
        {
            this.systemPrompt = systemPrompt;
        }

        public String getUserPrompt()
        {
            return this.userPrompt;
        }

        public void setUserPrompt(final String userPrompt)
        {
            this.userPrompt = userPrompt;
        }

        public String getResponseContent()
        {
            return this.responseContent;
        }

        public void setResponseContent(final String responseContent)
        {
            this.responseContent = responseContent;
        }
    }

    /**
     * Record of a captured screenshot or state attachment.
     */
    public static final class ReportScreenshotEntry
    {
        private String name;
        private int stepIndex;
        private String mediaType;
        private String base64Data;
        private long timestamp;
        private Integer width;
        private Integer height;

        public ReportScreenshotEntry()
        {
        }

        public ReportScreenshotEntry(
            final String name,
            final int stepIndex,
            final String mediaType,
            final String base64Data,
            final long timestamp
        )
        {
            this(name, stepIndex, mediaType, base64Data, timestamp, null, null);
        }

        public ReportScreenshotEntry(
            final String name,
            final int stepIndex,
            final String mediaType,
            final String base64Data,
            final long timestamp,
            final Integer width,
            final Integer height
        )
        {
            this.name = name;
            this.stepIndex = stepIndex;
            this.mediaType = mediaType;
            this.base64Data = base64Data;
            this.timestamp = timestamp;
            this.width = width;
            this.height = height;
            if (this.width == null || this.height == null)
            {
                resolveDimensionsFromBase64();
            }
        }

        public String getName()
        {
            return this.name;
        }

        public void setName(final String name)
        {
            this.name = name;
        }

        public int getStepIndex()
        {
            return this.stepIndex;
        }

        public void setStepIndex(final int stepIndex)
        {
            this.stepIndex = stepIndex;
        }

        public String getMediaType()
        {
            return this.mediaType;
        }

        public void setMediaType(final String mediaType)
        {
            this.mediaType = mediaType;
        }

        public String getBase64Data()
        {
            return this.base64Data;
        }

        public void setBase64Data(final String base64Data)
        {
            this.base64Data = base64Data;
            if (this.width == null || this.height == null)
            {
                resolveDimensionsFromBase64();
            }
        }

        public long getTimestamp()
        {
            return this.timestamp;
        }

        public void setTimestamp(final long timestamp)
        {
            this.timestamp = timestamp;
        }

        public Integer getWidth()
        {
            if (this.width == null && this.base64Data != null)
            {
                resolveDimensionsFromBase64();
            }
            return this.width;
        }

        public void setWidth(final Integer width)
        {
            this.width = width;
        }

        public Integer getHeight()
        {
            if (this.height == null && this.base64Data != null)
            {
                resolveDimensionsFromBase64();
            }
            return this.height;
        }

        public void setHeight(final Integer height)
        {
            this.height = height;
        }

        public String getDimensions()
        {
            final Integer w = getWidth();
            final Integer h = getHeight();
            if (w != null && h != null)
            {
                return w + "x" + h + " px";
            }
            return null;
        }

        public void resolveDimensionsFromBase64()
        {
            if ((this.width == null || this.height == null) && this.base64Data != null && !this.base64Data.isEmpty())
            {
                try
                {
                    String raw = this.base64Data;
                    final int commaIdx = raw.indexOf(',');
                    if (commaIdx != -1)
                    {
                        raw = raw.substring(commaIdx + 1);
                    }
                    final byte[] bytes = Base64.getDecoder().decode(raw);
                    final BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                    if (img != null)
                    {
                        this.width = img.getWidth();
                        this.height = img.getHeight();
                    }
                }
                catch (final Exception ignored)
                {
                }
            }
        }
    }

    /**
     * Token consumption and call count for a specific LLM responsibility category.
     */
    public static final class CategoryTokenUsage
    {
        private int calls;
        private long inputTokens;
        private long outputTokens;
        private long cachedTokens;
        private long totalTokens;
        private double estimatedCostUsd;

        public CategoryTokenUsage()
        {
        }

        public CategoryTokenUsage(
            final int calls,
            final long inputTokens,
            final long outputTokens,
            final long cachedTokens,
            final double estimatedCostUsd
        )
        {
            this.calls = calls;
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
            this.cachedTokens = cachedTokens;
            this.totalTokens = inputTokens + outputTokens;
            this.estimatedCostUsd = estimatedCostUsd;
        }

        public int getCalls()
        {
            return this.calls;
        }

        public void setCalls(final int calls)
        {
            this.calls = calls;
        }

        public long getInputTokens()
        {
            return this.inputTokens;
        }

        public void setInputTokens(final long inputTokens)
        {
            this.inputTokens = inputTokens;
            this.totalTokens = this.inputTokens + this.outputTokens;
        }

        public long getOutputTokens()
        {
            return this.outputTokens;
        }

        public void setOutputTokens(final long outputTokens)
        {
            this.outputTokens = outputTokens;
            this.totalTokens = this.inputTokens + this.outputTokens;
        }

        public long getCachedTokens()
        {
            return this.cachedTokens;
        }

        public void setCachedTokens(final long cachedTokens)
        {
            this.cachedTokens = cachedTokens;
        }

        public long getTotalTokens()
        {
            return this.totalTokens;
        }

        public void setTotalTokens(final long totalTokens)
        {
            this.totalTokens = totalTokens;
        }

        public double getEstimatedCostUsd()
        {
            return this.estimatedCostUsd;
        }

        public void setEstimatedCostUsd(final double estimatedCostUsd)
        {
            this.estimatedCostUsd = estimatedCostUsd;
        }
    }

    /**
     * Summary metrics covering steps, tokens, replays, and cost.
     */
    public static final class ReportMetrics
    {
        private int totalSteps;
        private int healedSteps;
        private int failedSteps;
        private int skippedSteps;
        private int totalLlmCalls;
        private long tokenUsageInput;
        private long tokenUsageOutput;
        private long tokenUsageCached;
        private long totalTokens;
        private double estimatedCostUsd;
        private int totalReplays;
        private int internalCacheHits;
        private int totalEscalations;
        private Map<String, Integer> contextLevelCounts = new LinkedHashMap<>();

        private CategoryTokenUsage total = new CategoryTokenUsage();
        private CategoryTokenUsage action = new CategoryTokenUsage();
        private CategoryTokenUsage pesap = new CategoryTokenUsage();
        private CategoryTokenUsage judge = new CategoryTokenUsage();
        private CategoryTokenUsage verification = new CategoryTokenUsage();
        private CategoryTokenUsage visualRca = new CategoryTokenUsage();

        public ReportMetrics()
        {
        }

        public int getTotalSteps()
        {
            return this.totalSteps;
        }

        public void setTotalSteps(final int totalSteps)
        {
            this.totalSteps = totalSteps;
        }

        public int getHealedSteps()
        {
            return this.healedSteps;
        }

        public void setHealedSteps(final int healedSteps)
        {
            this.healedSteps = healedSteps;
        }

        public int getFailedSteps()
        {
            return this.failedSteps;
        }

        public void setFailedSteps(final int failedSteps)
        {
            this.failedSteps = failedSteps;
        }

        public int getSkippedSteps()
        {
            return this.skippedSteps;
        }

        public void setSkippedSteps(final int skippedSteps)
        {
            this.skippedSteps = skippedSteps;
        }

        public int getTotalLlmCalls()
        {
            return this.totalLlmCalls;
        }

        public void setTotalLlmCalls(final int totalLlmCalls)
        {
            this.totalLlmCalls = totalLlmCalls;
            this.total.setCalls(totalLlmCalls);
        }

        public long getTokenUsageInput()
        {
            return this.tokenUsageInput;
        }

        public void setTokenUsageInput(final long tokenUsageInput)
        {
            this.tokenUsageInput = tokenUsageInput;
            this.total.setInputTokens(tokenUsageInput);
        }

        public long getTokenUsageOutput()
        {
            return this.tokenUsageOutput;
        }

        public void setTokenUsageOutput(final long tokenUsageOutput)
        {
            this.tokenUsageOutput = tokenUsageOutput;
            this.total.setOutputTokens(tokenUsageOutput);
        }

        public long getTokenUsageCached()
        {
            return this.tokenUsageCached;
        }

        public void setTokenUsageCached(final long tokenUsageCached)
        {
            this.tokenUsageCached = tokenUsageCached;
            this.total.setCachedTokens(tokenUsageCached);
        }

        public long getTotalTokens()
        {
            return this.totalTokens;
        }

        public void setTotalTokens(final long totalTokens)
        {
            this.totalTokens = totalTokens;
            this.total.setTotalTokens(totalTokens);
        }

        public double getEstimatedCostUsd()
        {
            return this.estimatedCostUsd;
        }

        public void setEstimatedCostUsd(final double estimatedCostUsd)
        {
            this.estimatedCostUsd = estimatedCostUsd;
            this.total.setEstimatedCostUsd(estimatedCostUsd);
        }

        public int getTotalReplays()
        {
            return this.totalReplays;
        }

        public void setTotalReplays(final int totalReplays)
        {
            this.totalReplays = totalReplays;
        }

        public int getInternalCacheHits()
        {
            return this.internalCacheHits;
        }

        public void setInternalCacheHits(final int internalCacheHits)
        {
            this.internalCacheHits = internalCacheHits;
        }

        public int getTotalEscalations()
        {
            return this.totalEscalations;
        }

        public void setTotalEscalations(final int totalEscalations)
        {
            this.totalEscalations = totalEscalations;
        }

        public Map<String, Integer> getContextLevelCounts()
        {
            return Collections.unmodifiableMap(this.contextLevelCounts);
        }

        public void setContextLevelCounts(final Map<String, Integer> contextLevelCounts)
        {
            this.contextLevelCounts = contextLevelCounts != null ? new LinkedHashMap<>(contextLevelCounts) : new LinkedHashMap<>();
        }

        public CategoryTokenUsage getTotal()
        {
            return this.total;
        }

        public void setTotal(final CategoryTokenUsage total)
        {
            this.total = total != null ? total : new CategoryTokenUsage();
        }

        public CategoryTokenUsage getAction()
        {
            return this.action;
        }

        public void setAction(final CategoryTokenUsage action)
        {
            this.action = action != null ? action : new CategoryTokenUsage();
        }

        public CategoryTokenUsage getPesap()
        {
            return this.pesap;
        }

        public void setPesap(final CategoryTokenUsage pesap)
        {
            this.pesap = pesap != null ? pesap : new CategoryTokenUsage();
        }

        public CategoryTokenUsage getJudge()
        {
            return this.judge;
        }

        public void setJudge(final CategoryTokenUsage judge)
        {
            this.judge = judge != null ? judge : new CategoryTokenUsage();
        }

        public CategoryTokenUsage getVerification()
        {
            return this.verification;
        }

        public void setVerification(final CategoryTokenUsage verification)
        {
            this.verification = verification != null ? verification : new CategoryTokenUsage();
        }

        public CategoryTokenUsage getVisualRca()
        {
            return this.visualRca;
        }

        public void setVisualRca(final CategoryTokenUsage visualRca)
        {
            this.visualRca = visualRca != null ? visualRca : new CategoryTokenUsage();
        }
    }
}
