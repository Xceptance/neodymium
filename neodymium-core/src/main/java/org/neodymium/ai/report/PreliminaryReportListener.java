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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.client.SutAttachment;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.event.ExecutionEvent;
import org.neodymium.ai.event.ExecutionListener;
import org.neodymium.ai.event.diagnostic.DiagnosticErrorEvent;
import org.neodymium.ai.event.diagnostic.DiagnosticWarningEvent;
import org.neodymium.ai.event.llm.LlmResponseReceivedEvent;
import org.neodymium.ai.event.structural.ActionExecutedEvent;
import org.neodymium.ai.event.structural.SessionFinishedEvent;
import org.neodymium.ai.event.structural.StateCapturedEvent;
import org.neodymium.ai.event.structural.StepFinishedEvent;
import org.neodymium.ai.event.structural.StepStartedEvent;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.playbook.linter.PlaybookLinterFinding;
import org.neodymium.ai.model.PlaybookStepStatus;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.StepStats;
import org.neodymium.ai.telemetry.MetricsCollector;
import org.neodymium.ai.tool.ToolDefinition;
import org.neodymium.util.Neodymium;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Execution event listener subscribed to {@link org.neodymium.ai.event.ExecutionEventBus}
 * that incrementally aggregates step actions, LLM invocations, token metrics, diagnostic errors,
 * and state screenshots, generating standalone HTML, Markdown, and JSON test reports to disk
 * upon session finish.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class PreliminaryReportListener implements ExecutionListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PreliminaryReportListener.class);
    private static final SimpleDateFormat FILE_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);

    private final boolean isCustomOutputDirectory;
    private final Path outputDirectory;
    private final Set<DiskReportFormat> formats;
    private final boolean enabled;
    private final TestExecutionReport report = new TestExecutionReport();
    private final HtmlReportGenerator htmlGenerator = new HtmlReportGenerator();
    private final MarkdownReportGenerator markdownGenerator = new MarkdownReportGenerator();
    private final JsonReportGenerator jsonGenerator = new JsonReportGenerator();
    private final HtmlIndexReportGenerator indexGenerator = new HtmlIndexReportGenerator();

    private TestExecutionReport.ReportStepEntry currentStep;
    private TestExecutionReport.ReportStepEntry currentParentStep;
    private final AtomicBoolean reportFlushed = new AtomicBoolean(false);
    private String lastBaseFileName;

    /**
     * Constructs a PreliminaryReportListener reading defaults from {@link AiConfiguration}.
     */
    public PreliminaryReportListener()
    {
        final AiConfiguration config = AiConfiguration.getInstance();
        this.enabled = config.isDiskReportEnabled();
        this.outputDirectory = Paths.get(config.getDiskReportDirectory());
        this.formats = DiskReportFormat.parseFormats(config.getDiskReportFormat());
        this.isCustomOutputDirectory = false;
        this.report.setStartTimeMs(System.currentTimeMillis());
    }

    /**
     * Constructs a PreliminaryReportListener with custom output directory and format set.
     *
     * @param outputDirectory output folder path
     * @param formats set of active report formats
     */
    public PreliminaryReportListener(final Path outputDirectory, final Set<DiskReportFormat> formats)
    {
        this(outputDirectory, formats, true);
    }

    /**
     * Constructs a PreliminaryReportListener with explicit directory, formats, and enabled flag.
     *
     * @param outputDirectory the target folder where preliminary report files are stored
     * @param formats the set of report formats to produce (HTML, Markdown, JSON)
     * @param enabled whether report emission is active
     */
    public PreliminaryReportListener(final Path outputDirectory, final Set<DiskReportFormat> formats, final boolean enabled)
    {
        this.isCustomOutputDirectory = outputDirectory != null;
        this.outputDirectory = outputDirectory != null ? outputDirectory : Paths.get(AiConfiguration.getInstance().getDiskReportDirectory());
        this.formats = formats != null ? formats : Set.of(DiskReportFormat.HTML, DiskReportFormat.MARKDOWN, DiskReportFormat.JSON);
        this.enabled = enabled;
        this.report.setStartTimeMs(System.currentTimeMillis());
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public Path getOutputDirectory()
    {
        return this.outputDirectory;
    }

    public Set<DiskReportFormat> getFormats()
    {
        return Collections.unmodifiableSet(this.formats);
    }

    public TestExecutionReport getReport()
    {
        return this.report;
    }

    /**
     * Returns the base filename of the most recently written report (without extension).
     *
     * @return the last generated base filename, or {@code null} if not yet flushed
     */
    public String getLastBaseFileName()
    {
        return this.lastBaseFileName;
    }

    @Override
    public void onEvent(final ExecutionEvent event)
    {
        if (!this.enabled || event == null)
        {
            return;
        }

        try
        {
            handleEvent(event);
        }
        catch (final Throwable t)
        {
            LOGGER.warn("PreliminaryReportListener encountered error while processing event {}: {}", event.getClass().getSimpleName(), t.getMessage(), t);
        }
    }

    private void handleEvent(final ExecutionEvent event)
    {
        final ExecutionContext activeCtx = ExecutionContext.getActiveContext();

        if (event instanceof StepStartedEvent stepStarted)
        {
            if (this.currentStep != null && "RUNNING".equalsIgnoreCase(this.currentStep.getStatus()))
            {
                this.currentStep.setStatus("SUCCESS");
                if (this.currentStep.getDurationMs() <= 0 && this.currentStep.getStartTimeMs() > 0)
                {
                    this.currentStep.setDurationMs(Math.max(1, System.currentTimeMillis() - this.currentStep.getStartTimeMs()));
                }
            }

            final PlaybookStep pbStep = stepStarted.getStep();
            final String rawInstruction = pbStep != null ? pbStep.getInstruction() : null;
            String resolvedInstruction = rawInstruction;

            if (activeCtx != null && activeCtx.getSessionData() != null && rawInstruction != null)
            {
                try
                {
                    resolvedInstruction = activeCtx.getSessionData().resolveVariables(rawInstruction);
                }
                catch (final Exception ignored)
                {
                }
            }

            final TestExecutionReport.ReportStepEntry stepEntry = new TestExecutionReport.ReportStepEntry(stepStarted.getStepIndex(), resolvedInstruction);
            stepEntry.setRawInstruction(rawInstruction);
            stepEntry.setStartTimeMs(System.currentTimeMillis());
            stepEntry.setStatus("RUNNING");

            if (pbStep != null)
            {
                stepEntry.setSourceFile(pbStep.getSourceFile());
                stepEntry.setLineNumber(pbStep.getLineNumber());
                stepEntry.setBug(pbStep.isBug());
                stepEntry.setBugDetails(pbStep.getBugDetails());
                stepEntry.setOptional(pbStep.isOptional());
                stepEntry.setContinueOnError(pbStep.isContinueOnError());
                stepEntry.setNoHealing(pbStep.isNoHealing());
                stepEntry.setVisual(pbStep.isVisualStep());
                if (pbStep.getSemanticIntent() != null)
                {
                    stepEntry.setSemanticIntent(pbStep.getSemanticIntent().name());
                }

                if (pbStep.getReasoning() != null)
                {
                    stepEntry.setReasoning(pbStep.getReasoning());
                }
                if (pbStep.getReasonings() != null && !pbStep.getReasonings().isEmpty())
                {
                    stepEntry.setReasonings(pbStep.getReasonings());
                }
            }
            else if (rawInstruction != null)
            {
                final boolean isVisualInstruction = PlaybookStep.VISUAL_PATTERN.matcher(rawInstruction).find()
                    || PlaybookStep.LAYOUT_PATTERN.matcher(rawInstruction).find();
                stepEntry.setVisual(isVisualInstruction);
            }

            if (pbStep != null && pbStep.getParent() != null)
            {
                final PlaybookStep rootPb = pbStep.getRootStep();
                TestExecutionReport.ReportStepEntry parentEntry = null;

                // Match root parent from most recent steps backwards by instruction / rawInstruction
                for (int i = this.report.getSteps().size() - 1; i >= 0; i--)
                {
                    final TestExecutionReport.ReportStepEntry entry = this.report.getSteps().get(i);
                    final boolean matchesInstruction = rootPb.getInstruction() != null
                        && (rootPb.getInstruction().equals(entry.getRawInstruction())
                            || rootPb.getInstruction().equals(entry.getInstruction()));

                    if (matchesInstruction)
                    {
                        parentEntry = entry;
                        break;
                    }
                }

                if (parentEntry == null)
                {
                    // Reconstruct parent root entry if not present yet
                    final String pRaw = rootPb.getInstruction();
                    String pResolved = pRaw;
                    if (activeCtx != null && activeCtx.getSessionData() != null && pRaw != null)
                    {
                        try
                        {
                            pResolved = activeCtx.getSessionData().resolveVariables(pRaw);
                        }
                        catch (final Exception ignored)
                        {
                        }
                    }
                    parentEntry = new TestExecutionReport.ReportStepEntry(this.report.getSteps().size(), pResolved);
                    parentEntry.setRawInstruction(pRaw);
                    parentEntry.setSourceFile(rootPb.getSourceFile());
                    parentEntry.setLineNumber(rootPb.getLineNumber());
                    parentEntry.setStatus("SUCCESS");
                    parentEntry.setBug(rootPb.isBug());
                    parentEntry.setBugDetails(rootPb.getBugDetails());
                    parentEntry.setOptional(rootPb.isOptional());
                    parentEntry.setContinueOnError(rootPb.isContinueOnError());
                    parentEntry.setNoHealing(rootPb.isNoHealing());
                    parentEntry.setVisual(rootPb.isVisualStep());
                    this.report.addStep(parentEntry);
                }

                // If an intermediate parent container was added to parentEntry, remove it in favor of leaf sub-steps
                if (pbStep.getParent() != rootPb && pbStep.getParent().getInstruction() != null)
                {
                    final String intermediateInstruction = pbStep.getParent().getInstruction();
                    parentEntry.removeSubStepIf(sub -> intermediateInstruction.equals(sub.getInstruction()) || intermediateInstruction.equals(sub.getRawInstruction()));
                }

                TestExecutionReport.ReportStepEntry existingSub = null;
                for (final TestExecutionReport.ReportStepEntry sub : parentEntry.getSubSteps())
                {
                    if (rawInstruction != null && (rawInstruction.equals(sub.getRawInstruction()) || rawInstruction.equals(sub.getInstruction())))
                    {
                        existingSub = sub;
                        break;
                    }
                }

                if (existingSub != null)
                {
                    existingSub.setStatus("RUNNING");
                    existingSub.setStartTimeMs(System.currentTimeMillis());
                    this.currentParentStep = parentEntry;
                    this.currentStep = existingSub;
                    return;
                }

                parentEntry.addSubStep(stepEntry);
                this.currentParentStep = parentEntry;
                this.currentStep = stepEntry;
                return;
            }

            if (pbStep != null && pbStep.hasSubSteps() && stepEntry.getSubSteps().isEmpty())
            {
                for (int s = 0; s < pbStep.getSubSteps().size(); s++)
                {
                    final PlaybookStep childStep = pbStep.getSubSteps().get(s);
                    final String cRaw = childStep.getInstruction();
                    String cResolved = cRaw;
                    if (activeCtx != null && activeCtx.getSessionData() != null && cRaw != null)
                    {
                        try
                        {
                            cResolved = activeCtx.getSessionData().resolveVariables(cRaw);
                        }
                        catch (final Exception ignored)
                        {
                        }
                    }
                    final TestExecutionReport.ReportStepEntry childEntry = new TestExecutionReport.ReportStepEntry(s, cResolved);
                    childEntry.setRawInstruction(cRaw);
                    childEntry.setSourceFile(childStep.getSourceFile());
                    childEntry.setLineNumber(childStep.getLineNumber());
                    childEntry.setStatus(childStep.getStatus() != null ? childStep.getStatus().name() : "PENDING");
                    childEntry.setBug(childStep.isBug());
                    childEntry.setBugDetails(childStep.getBugDetails());
                    childEntry.setOptional(childStep.isOptional());
                    childEntry.setContinueOnError(childStep.isContinueOnError());
                    childEntry.setNoHealing(childStep.isNoHealing());
                    childEntry.setVisual(childStep.isVisualStep());
                    stepEntry.addSubStep(childEntry);
                }
            }

            this.report.addStep(stepEntry);
            this.currentParentStep = null;
            this.currentStep = stepEntry;
        }
        else if (event instanceof StepFinishedEvent stepFinished)
        {
            final PlaybookStep pbStep = stepFinished.getStep();
            final PlaybookStepStatus status = stepFinished.getStatus();

            TestExecutionReport.ReportStepEntry targetStep = null;
            if (pbStep != null)
            {
                final String pbInstr = pbStep.getInstruction();
                if (this.currentStep != null && pbInstr != null
                    && (pbInstr.equals(this.currentStep.getRawInstruction()) || pbInstr.equals(this.currentStep.getInstruction())))
                {
                    targetStep = this.currentStep;
                }

                if (targetStep == null && pbInstr != null)
                {
                    for (int i = this.report.getSteps().size() - 1; i >= 0; i--)
                    {
                        final TestExecutionReport.ReportStepEntry rootEntry = this.report.getSteps().get(i);
                        if (pbInstr.equals(rootEntry.getRawInstruction()) || pbInstr.equals(rootEntry.getInstruction()))
                        {
                            targetStep = rootEntry;
                            break;
                        }
                        for (int j = rootEntry.getSubSteps().size() - 1; j >= 0; j--)
                        {
                            final TestExecutionReport.ReportStepEntry sub = rootEntry.getSubSteps().get(j);
                            if (pbInstr.equals(sub.getRawInstruction()) || pbInstr.equals(sub.getInstruction()))
                            {
                                targetStep = sub;
                                break;
                            }
                        }
                        if (targetStep != null)
                        {
                            break;
                        }
                    }
                }
            }

            if (targetStep == null)
            {
                targetStep = this.currentStep;
            }
            if (targetStep == null && !this.report.getSteps().isEmpty())
            {
                targetStep = this.report.getSteps().get(this.report.getSteps().size() - 1);
            }

            if (targetStep != null)
            {
                targetStep.setStatus(status != null ? status.name() : "SUCCESS");
                if (targetStep.getStartTimeMs() > 0)
                {
                    targetStep.setDurationMs(System.currentTimeMillis() - targetStep.getStartTimeMs());
                }
                if (targetStep.getDurationMs() <= 0 && !targetStep.getSubSteps().isEmpty())
                {
                    long subSum = 0;
                    for (final TestExecutionReport.ReportStepEntry sub : targetStep.getSubSteps())
                    {
                        if (sub.getDurationMs() > 0)
                        {
                            subSum += sub.getDurationMs();
                        }
                    }
                    targetStep.setDurationMs(subSum);
                }
                if (pbStep != null)
                {
                    targetStep.setBug(pbStep.isBug());
                    if (pbStep.getBugDetails() != null)
                    {
                        targetStep.setBugDetails(pbStep.getBugDetails());
                    }
                    if (pbStep.getReasoning() != null && !pbStep.getReasoning().isBlank())
                    {
                        targetStep.setReasoning(pbStep.getReasoning());
                    }
                    if (pbStep.getReasonings() != null && !pbStep.getReasonings().isEmpty())
                    {
                        targetStep.setReasonings(pbStep.getReasonings());
                    }
                    if (pbStep.getFailureReason() != null)
                    {
                        targetStep.setFailureReason(pbStep.getFailureReason());
                    }
                    if (pbStep.getSsimScore() != null)
                    {
                        targetStep.setSsimScore(pbStep.getSsimScore());
                    }
                    if (pbStep.getSsimMinScore() != null)
                    {
                        targetStep.setSsimMinScore(pbStep.getSsimMinScore());
                    }
                    if (pbStep.getBaselineMatrixPng() != null)
                    {
                        targetStep.setBaselineMatrixPng(pbStep.getBaselineMatrixPng());
                    }
                    if (pbStep.getReplayMatrixPng() != null)
                    {
                        targetStep.setReplayMatrixPng(pbStep.getReplayMatrixPng());
                    }
                    if (pbStep.getScreenshotHashDim() != null)
                    {
                        targetStep.setScreenshotHashDim(pbStep.getScreenshotHashDim());
                    }
                    if (pbStep.getSemanticIntent() != null)
                    {
                        targetStep.setSemanticIntent(pbStep.getSemanticIntent().name());
                    }
                    if (pbStep.getVerificationResult() != null)
                    {
                        targetStep.setVerificationResult(pbStep.getVerificationResult());
                    }
                    if (targetStep.getActions().isEmpty() && pbStep.getActions() != null && !pbStep.getActions().isEmpty())
                    {
                        for (final Action act : pbStep.getActions())
                        {
                            targetStep.addAction(new TestExecutionReport.ReportActionEntry(
                                act.getType(),
                                act.getTarget(),
                                act.getValue(),
                                act.getDescription(),
                                act.getReasoning(),
                                true,
                                null
                            ));
                        }
                    }

                    if (pbStep.hasSubSteps() && targetStep.getSubSteps().isEmpty())
                    {
                        for (int s = 0; s < pbStep.getSubSteps().size(); s++)
                        {
                            final PlaybookStep childStep = pbStep.getSubSteps().get(s);
                            final String cRaw = childStep.getInstruction();
                            String cResolved = cRaw;
                            if (activeCtx != null && activeCtx.getSessionData() != null && cRaw != null)
                            {
                                try
                                {
                                    cResolved = activeCtx.getSessionData().resolveVariables(cRaw);
                                }
                                catch (final Exception ignored)
                                {
                                }
                            }
                            final TestExecutionReport.ReportStepEntry childEntry = new TestExecutionReport.ReportStepEntry(s, cResolved);
                            childEntry.setRawInstruction(cRaw);
                            childEntry.setSourceFile(childStep.getSourceFile());
                            childEntry.setLineNumber(childStep.getLineNumber());
                            childEntry.setStatus(childStep.getStatus() != null ? childStep.getStatus().name() : "PENDING");
                            childEntry.setBug(childStep.isBug());
                            childEntry.setBugDetails(childStep.getBugDetails());
                            childEntry.setOptional(childStep.isOptional());
                            childEntry.setContinueOnError(childStep.isContinueOnError());
                            childEntry.setNoHealing(childStep.isNoHealing());
                            childEntry.setVisual(childStep.isVisualStep());
                            targetStep.addSubStep(childEntry);
                        }
                    }
                }

                if (!targetStep.getSubSteps().isEmpty())
                {
                    final boolean isStepSuccess = "SUCCESS".equalsIgnoreCase(targetStep.getStatus()) || "PASSED".equalsIgnoreCase(targetStep.getStatus());
                    for (int s = 0; s < targetStep.getSubSteps().size(); s++)
                    {
                        final TestExecutionReport.ReportStepEntry sub = targetStep.getSubSteps().get(s);
                        if (isStepSuccess)
                        {
                            if (sub.getStatus() == null || "PENDING".equalsIgnoreCase(sub.getStatus()) || "RUNNING".equalsIgnoreCase(sub.getStatus()))
                            {
                                sub.setStatus("SUCCESS");
                            }
                        }
                        else
                        {
                            if (sub.isBug())
                            {
                                sub.setStatus("FAILED");
                                if (sub.getFailureReason() == null)
                                {
                                    sub.setFailureReason(targetStep.getFailureReason());
                                }
                            }
                            else if (sub.getStatus() == null || "PENDING".equalsIgnoreCase(sub.getStatus()) || "RUNNING".equalsIgnoreCase(sub.getStatus()))
                            {
                                sub.setStatus("SUCCESS");
                            }
                        }
                    }
                    if (!isStepSuccess && targetStep.getSubSteps().stream().noneMatch(sub -> "FAILED".equalsIgnoreCase(sub.getStatus())))
                    {
                        final TestExecutionReport.ReportStepEntry lastSub = targetStep.getSubSteps().get(targetStep.getSubSteps().size() - 1);
                        lastSub.setStatus("FAILED");
                        lastSub.setFailureReason(targetStep.getFailureReason());
                    }
                }
                this.currentStep = targetStep;
                if (pbStep == null || pbStep.getParent() == null || targetStep == this.currentParentStep)
                {
                    this.currentParentStep = null;
                }
            }
        }
        else if (event instanceof ActionExecutedEvent actionExecuted)
        {
            if (this.currentStep != null && actionExecuted.getAction() != null)
            {
                final Action action = actionExecuted.getAction();
                final Action resolvedAction = actionExecuted.getResolvedAction();
                final TestExecutionReport.ReportActionEntry actionEntry = new TestExecutionReport.ReportActionEntry(
                    action.getType(),
                    action.getTarget(),
                    action.getValue(),
                    action.getDescription(),
                    action.getReasoning(),
                    actionExecuted.isSuccess(),
                    actionExecuted.getPhase()
                );
                if (resolvedAction != null)
                {
                    actionEntry.setResolvedTarget(resolvedAction.getTarget());
                    actionEntry.setResolvedValue(resolvedAction.getValue());
                }
                if (actionExecuted.getPhase() != null)
                {
                    this.currentStep.setMultiStage(true);
                }
                this.currentStep.addAction(actionEntry);
            }
        }
        else if (event instanceof LlmResponseReceivedEvent llmReceived)
        {
            final int stepIdx;
            final int subStepIdx;
            if (this.currentParentStep != null)
            {
                stepIdx = this.currentParentStep.getStepIndex();
                int sIdx = this.currentParentStep.getSubSteps().indexOf(this.currentStep);
                if (sIdx < 0 && this.currentStep != null)
                {
                    sIdx = this.currentStep.getStepIndex();
                }
                subStepIdx = sIdx;
            }
            else if (this.currentStep != null)
            {
                stepIdx = this.currentStep.getStepIndex();
                subStepIdx = -1;
            }
            else
            {
                stepIdx = -1;
                subStepIdx = -1;
            }

            final TestExecutionReport.ReportLlmCallEntry callEntry = new TestExecutionReport.ReportLlmCallEntry();
            callEntry.setStepIndex(stepIdx);
            callEntry.setSubStepIndex(subStepIdx);
            callEntry.setCapability(llmReceived.getCapability());
            callEntry.setDurationMs(llmReceived.getDurationMs());

            if (llmReceived.getRequest() != null)
            {
                callEntry.setSystemPrompt(llmReceived.getRequest().systemMessage());
                if (llmReceived.getRequest().isMultiTurn())
                {
                    callEntry.setUserPrompt(llmReceived.getRequest().dialogueConversationFormatted());
                }
                else
                {
                    callEntry.setUserPrompt(llmReceived.getRequest().userMessage());
                }
                if (llmReceived.getRequest().hasTools())
                {
                    callEntry.setAvailableTools(llmReceived.getRequest().tools().stream()
                            .map(ToolDefinition::toFormattedSummary)
                            .toList());
                }
            }

            if (llmReceived.getResponse() != null)
            {
                callEntry.setModelName(llmReceived.getResponse().modelName());
                final String content = llmReceived.getResponse().content();
                if (content != null && !content.isBlank())
                {
                    if (llmReceived.getResponse().hasToolCalls())
                    {
                        callEntry.setResponseContent(content + "\n\nTool Calls: " + llmReceived.getResponse().toolCalls());
                    }
                    else
                    {
                        callEntry.setResponseContent(content);
                    }
                }
                else if (llmReceived.getResponse().hasToolCalls())
                {
                    callEntry.setResponseContent("Tool Calls: " + llmReceived.getResponse().toolCalls());
                }
                else
                {
                    callEntry.setResponseContent(content);
                }

                final TokenUsage usage = llmReceived.getResponse().tokenUsage();
                if (usage != null)
                {
                    callEntry.setInputTokens(usage.inputTokenCount());
                    callEntry.setOutputTokens(usage.outputTokenCount());
                    callEntry.setCachedTokens(usage.cachedTokenCount());
                    callEntry.setTotalTokens(usage.totalTokenCount());
                    callEntry.setEstimatedCostUsd(MetricsCollector.calculateCost(usage, llmReceived.getResponse().modelName()));
                }

                if (callEntry.getResponseContent() != null && callEntry.getResponseContent().contains("\"status\": \"CONTINUE\""))
                {
                    if (this.currentStep != null)
                    {
                        this.currentStep.setMultiStage(true);
                    }
                }
            }

            this.report.addLlmCall(callEntry);
            if (this.currentStep != null)
            {
                this.currentStep.addLlmCall(callEntry);
            }
        }
        else if (event instanceof StateCapturedEvent stateCaptured)
        {
            if (stateCaptured.getState() != null && stateCaptured.getState().getAttachments() != null)
            {
                final int stepIdx;
                final int subStepIdx;
                if (this.currentParentStep != null)
                {
                    stepIdx = this.currentParentStep.getStepIndex();
                    int sIdx = this.currentParentStep.getSubSteps().indexOf(this.currentStep);
                    if (sIdx < 0 && this.currentStep != null)
                    {
                        sIdx = this.currentStep.getStepIndex();
                    }
                    subStepIdx = sIdx;
                }
                else if (this.currentStep != null)
                {
                    stepIdx = this.currentStep.getStepIndex();
                    subStepIdx = -1;
                }
                else
                {
                    stepIdx = 0;
                    subStepIdx = -1;
                }

                final boolean isVisual = this.currentStep != null && this.currentStep.isVisual();
                final String stepLabel = (stepIdx >= 0)
                    ? (subStepIdx >= 0 ? "Step #" + (stepIdx + 1) + "." + (subStepIdx + 1) : "Step #" + (stepIdx + 1))
                    : "Step";
                final String label = stepLabel + (isVisual ? " (Visual Verification)" : " Capture");

                for (final SutAttachment attachment : stateCaptured.getState().getAttachments())
                {
                    if (attachment != null && attachment.base64Data() != null && !attachment.base64Data().isEmpty())
                    {
                        if (this.currentStep != null)
                        {
                            final boolean alreadyHas = this.currentStep.getScreenshots().stream()
                                .anyMatch(s -> s.getBase64Data() != null && s.getBase64Data().equals(attachment.base64Data()));
                            if (alreadyHas)
                            {
                                continue;
                            }
                        }

                        final TestExecutionReport.ReportScreenshotEntry screenshot = new TestExecutionReport.ReportScreenshotEntry(
                            label,
                            stepIdx,
                            attachment.mediaType() != null ? attachment.mediaType() : "image/png",
                            attachment.base64Data(),
                            System.currentTimeMillis()
                        );
                        screenshot.setSubStepIndex(subStepIdx);
                        this.report.addScreenshot(screenshot);
                        if (this.currentStep != null)
                        {
                            this.currentStep.addScreenshot(screenshot);
                        }
                        LOGGER.debug("   📸 Recorded screenshot for {}: \"{}\" | Dimensions: {}",
                            stepLabel, label, screenshot.getDimensions() != null ? screenshot.getDimensions() : "unknown");
                    }
                }
            }
        }
        else if (event instanceof DiagnosticErrorEvent errorEvent)
        {
            final String msg = errorEvent.getMessage();
            if (msg != null && (msg.startsWith("Visual RCA analysis:") || msg.startsWith("Visual RCA Diagnosis:")))
            {
                final String cleanRca = msg.replaceFirst("^Visual RCA (analysis|Diagnosis):\\s*", "").trim();
                if (this.report.getVisualRcaExplanation() == null)
                {
                    this.report.setVisualRcaExplanation(cleanRca);
                }
            }
            else if (this.report.getFailureReason() == null)
            {
                this.report.setFailureReason(msg);
            }

            if (errorEvent.getCause() != null)
            {
                if (this.report.getFailureStackTrace() == null)
                {
                    final StringWriter sw = new StringWriter();
                    errorEvent.getCause().printStackTrace(new PrintWriter(sw));
                    this.report.setFailureStackTrace(sw.toString());
                }
                if (this.report.getFailureReason() == null)
                {
                    Throwable root = errorEvent.getCause();
                    while (root.getCause() != null && root != root.getCause())
                    {
                        root = root.getCause();
                    }
                    this.report.setFailureReason(root.getMessage() != null ? root.getMessage() : root.toString());
                }
            }
        }
        else if (event instanceof DiagnosticWarningEvent warningEvent)
        {
            this.report.addWarning(warningEvent.getMessage());
        }
        else if (event instanceof SessionFinishedEvent sessionFinished)
        {
            this.report.setEndTimeMs(System.currentTimeMillis());
            this.report.setDurationMs(sessionFinished.getDurationMs());
            this.report.setSuccess(sessionFinished.isSuccess());
            this.report.setStatus(sessionFinished.isSuccess() ? "PASSED" : "FAILED");
            this.report.addWarnings(sessionFinished.getWarnings());

            if (sessionFinished.isSuccess())
            {
                this.report.setFailureReason(null);
                this.report.setFailureStackTrace(null);
            }

            populateContextMetadata();
            resolveUnfinishedSteps();
            recalculateMetrics();
            flushReport();
        }
    }

    private void resolveUnfinishedSteps()
    {
        final List<TestExecutionReport.ReportStepEntry> rootSteps = this.report.getSteps();
        if (rootSteps.isEmpty())
        {
            return;
        }

        final List<TestExecutionReport.ReportStepEntry> leafSteps = new ArrayList<>();
        for (final TestExecutionReport.ReportStepEntry root : rootSteps)
        {
            collectLeafSteps(root, leafSteps);
        }

        if (this.report.isSuccess())
        {
            for (final TestExecutionReport.ReportStepEntry leaf : leafSteps)
            {
                if ("RUNNING".equalsIgnoreCase(leaf.getStatus()) || "PENDING".equalsIgnoreCase(leaf.getStatus()) || leaf.getStatus() == null)
                {
                    leaf.setStatus("SUCCESS");
                    if (leaf.getDurationMs() <= 0 && leaf.getStartTimeMs() > 0)
                    {
                        leaf.setDurationMs(Math.max(1, System.currentTimeMillis() - leaf.getStartTimeMs()));
                    }
                }
            }
        }
        else
        {
            // The session failed. Find the failing leaf step.
            int failedIndex = -1;
            for (int i = 0; i < leafSteps.size(); i++)
            {
                if ("FAILED".equalsIgnoreCase(leafSteps.get(i).getStatus()))
                {
                    failedIndex = i;
                    break;
                }
            }

            if (failedIndex == -1)
            {
                // No step was explicitly marked FAILED yet.
                // Find the active running step (or this.currentStep), or the last non-skipped step.
                for (int i = 0; i < leafSteps.size(); i++)
                {
                    final TestExecutionReport.ReportStepEntry leaf = leafSteps.get(i);
                    if (leaf == this.currentStep || "RUNNING".equalsIgnoreCase(leaf.getStatus()))
                    {
                        failedIndex = i;
                        break;
                    }
                }
                if (failedIndex == -1)
                {
                    failedIndex = leafSteps.size() - 1;
                }
            }

            for (int i = 0; i < leafSteps.size(); i++)
            {
                final TestExecutionReport.ReportStepEntry leaf = leafSteps.get(i);
                if (i < failedIndex)
                {
                    // Steps executed before the failure completed successfully
                    if ("RUNNING".equalsIgnoreCase(leaf.getStatus()) || "PENDING".equalsIgnoreCase(leaf.getStatus()) || leaf.getStatus() == null)
                    {
                        leaf.setStatus("SUCCESS");
                        if (leaf.getDurationMs() <= 0 && leaf.getStartTimeMs() > 0)
                        {
                            leaf.setDurationMs(Math.max(1, System.currentTimeMillis() - leaf.getStartTimeMs()));
                        }
                    }
                }
                else if (i == failedIndex)
                {
                    // The failing step
                    leaf.setStatus("FAILED");
                    if (leaf.getFailureReason() == null && this.report.getFailureReason() != null)
                    {
                        leaf.setFailureReason(this.report.getFailureReason());
                    }
                    if (leaf.getDurationMs() <= 0 && leaf.getStartTimeMs() > 0)
                    {
                        leaf.setDurationMs(Math.max(1, System.currentTimeMillis() - leaf.getStartTimeMs()));
                    }
                }
                else
                {
                    // Steps after the failure were not executed
                    if ("RUNNING".equalsIgnoreCase(leaf.getStatus()) || "PENDING".equalsIgnoreCase(leaf.getStatus()) || leaf.getStatus() == null)
                    {
                        leaf.setStatus("SKIPPED");
                        leaf.setFailureReason(null);
                    }
                }
            }
        }

        // Propagate status and durations to parent steps
        for (final TestExecutionReport.ReportStepEntry root : rootSteps)
        {
            resolveParentStepStatus(root);
        }
    }

    private void collectLeafSteps(final TestExecutionReport.ReportStepEntry entry, final List<TestExecutionReport.ReportStepEntry> leafList)
    {
        if (entry == null)
        {
            return;
        }
        if (entry.getSubSteps().isEmpty())
        {
            leafList.add(entry);
        }
        else
        {
            for (final TestExecutionReport.ReportStepEntry sub : entry.getSubSteps())
            {
                collectLeafSteps(sub, leafList);
            }
        }
    }

    private void resolveParentStepStatus(final TestExecutionReport.ReportStepEntry step)
    {
        if (step == null || step.getSubSteps().isEmpty())
        {
            return;
        }

        long totalSubDuration = 0;
        boolean anyFailed = false;
        boolean allSuccess = true;
        boolean allSkipped = true;
        String firstFailedReason = null;

        for (final TestExecutionReport.ReportStepEntry sub : step.getSubSteps())
        {
            resolveParentStepStatus(sub);
            totalSubDuration += sub.getDurationMs();
            final String subStatus = sub.getStatus();
            if ("FAILED".equalsIgnoreCase(subStatus))
            {
                anyFailed = true;
                allSuccess = false;
                allSkipped = false;
                if (firstFailedReason == null && sub.getFailureReason() != null)
                {
                    firstFailedReason = sub.getFailureReason();
                }
            }
            else if ("SUCCESS".equalsIgnoreCase(subStatus) || "PASSED".equalsIgnoreCase(subStatus) || "HEALED".equalsIgnoreCase(subStatus))
            {
                allSkipped = false;
            }
            else if ("SKIPPED".equalsIgnoreCase(subStatus))
            {
                allSuccess = false;
            }
            else
            {
                allSuccess = false;
                allSkipped = false;
            }
        }

        if (anyFailed)
        {
            step.setStatus("FAILED");
            if (step.getFailureReason() == null)
            {
                step.setFailureReason(firstFailedReason);
            }
        }
        else if (allSkipped)
        {
            step.setStatus("SKIPPED");
            step.setFailureReason(null);
        }
        else if (allSuccess)
        {
            step.setStatus("SUCCESS");
            step.setFailureReason(null);
        }
        else
        {
            step.setStatus(this.report.isSuccess() ? "SUCCESS" : "SKIPPED");
        }

        if (step.getDurationMs() <= 0)
        {
            step.setDurationMs(totalSubDuration);
        }
    }

    private void populateContextMetadata()
    {
        final ExecutionContext ctx = ExecutionContext.getActiveContext();
        if (ctx != null)
        {
            if (this.report.getTestClass() == null)
            {
                final String tc = (String) ctx.getTransientData().get("testClass");
                if (tc != null)
                {
                    this.report.setTestClass(tc);
                }
            }
            if (this.report.getTestMethod() == null)
            {
                final String tm = (String) ctx.getTransientData().get("testMethod");
                if (tm != null)
                {
                    this.report.setTestMethod(tm);
                }
            }
            if (this.report.getDatasetId() == null)
            {
                final String ds = (String) ctx.getTransientData().get(ExecutionContext.KEY_ACTIVE_DATASET_LABEL);
                if (ds != null)
                {
                    this.report.setDatasetId(ds);
                }
            }
            if (this.report.getExecutionMode() == null)
            {
                final Object mode = ctx.getTransientData().get(ExecutionContext.KEY_EXECUTION_MODE);
                if (mode != null)
                {
                    this.report.setExecutionMode(String.valueOf(mode));
                }
            }
            if (this.report.getPlaybookFile() == null)
            {
                final String pb = (String) ctx.getTransientData().get("playbookFile");
                if (pb != null)
                {
                    this.report.setPlaybookFile(pb);
                }
            }
            if (this.report.getVisualRcaExplanation() == null)
            {
                final String rca = (String) ctx.getTransientData().get(ExecutionContext.KEY_VISUAL_RCA_EXPLANATION);
                if (rca != null)
                {
                    this.report.setVisualRcaExplanation(rca);
                }
            }
            if (this.report.getFailureReason() == null)
            {
                final Object lastErr = ctx.getTransientData().get(ExecutionContext.KEY_LAST_EXECUTION_ERROR);
                if (lastErr instanceof Throwable t)
                {
                    this.report.setFailureReason(t.getMessage() != null ? t.getMessage() : t.toString());
                    if (this.report.getFailureStackTrace() == null)
                    {
                        final StringWriter sw = new StringWriter();
                        t.printStackTrace(new PrintWriter(sw));
                        this.report.setFailureStackTrace(sw.toString());
                    }
                }
                else if (lastErr != null)
                {
                    this.report.setFailureReason(lastErr.toString());
                }
            }

            @SuppressWarnings("unchecked")
            final List<PlaybookLinterFinding> linterFindings = (List<PlaybookLinterFinding>) ctx.getTransientData().get(ExecutionContext.KEY_PLAYBOOK_LINTER_FINDINGS);
            if (linterFindings != null && !linterFindings.isEmpty())
            {
                this.report.addLinterFindings(linterFindings);
            }

            @SuppressWarnings("unchecked")
            final List<PlaybookLinterFinding> postFlightFindings = (List<PlaybookLinterFinding>) ctx.getTransientData().get(ExecutionContext.KEY_POST_FLIGHT_LINTER_FINDINGS);
            if (postFlightFindings != null && !postFlightFindings.isEmpty())
            {
                this.report.addPostFlightFindings(postFlightFindings);
            }
        }

        if (this.report.getFailureReason() == null && !this.report.isSuccess())
        {
            for (final TestExecutionReport.ReportStepEntry st : this.report.getSteps())
            {
                if (st.getFailureReason() != null && !st.getFailureReason().isBlank())
                {
                    this.report.setFailureReason(st.getFailureReason());
                    break;
                }
                for (final TestExecutionReport.ReportStepEntry subSt : st.getSubSteps())
                {
                    if (subSt.getFailureReason() != null && !subSt.getFailureReason().isBlank())
                    {
                        this.report.setFailureReason(subSt.getFailureReason());
                        break;
                    }
                }
                if (this.report.getFailureReason() != null)
                {
                    break;
                }
            }
        }

        if (this.report.getTestName() == null)
        {
            final String globalTestName = Neodymium.getTestName();
            if (globalTestName != null && !globalTestName.isBlank())
            {
                this.report.setTestName(globalTestName);
            }
            else if (this.report.getTestClass() != null)
            {
                final String simpleName = extractSimpleClassName(this.report.getTestClass());
                final String method = this.report.getTestMethod() != null ? this.report.getTestMethod() : "test";
                this.report.setTestName(simpleName + "." + method);
            }
            else
            {
                this.report.setTestName("AI_Test_Run");
            }
        }
    }

    private void recalculateMetrics()
    {
        final TestExecutionReport.ReportMetrics m = this.report.getMetrics();
        final List<TestExecutionReport.ReportStepEntry> steps = this.report.getSteps();

        m.setTotalSteps(steps.size());
        int healed = 0;
        int failed = 0;
        int skipped = 0;

        for (final TestExecutionReport.ReportStepEntry step : steps)
        {
            final String st = step.getStatus() != null ? step.getStatus().toUpperCase() : "";
            if ("HEALED".equals(st))
            {
                healed++;
            }
            else if ("FAILED".equals(st))
            {
                failed++;
            }
            else if ("SKIPPED".equals(st))
            {
                skipped++;
            }
        }
        m.setHealedSteps(healed);
        m.setFailedSteps(failed);
        m.setSkippedSteps(skipped);

        final ExecutionContext ctx = ExecutionContext.getActiveContext();
        if (ctx != null)
        {
            final Integer replays = (Integer) ctx.getTransientData().get(ExecutionContext.KEY_TOTAL_REPLAYS);
            if (replays != null)
            {
                m.setTotalReplays(replays);
            }
            final Integer cacheHits = (Integer) ctx.getTransientData().get(ExecutionContext.KEY_INTERNAL_CACHE_HITS);
            if (cacheHits != null)
            {
                m.setInternalCacheHits(cacheHits);
            }

            final Integer stdCallsObj = (Integer) ctx.getTransientData().get(ExecutionContext.KEY_STANDARD_CALL_COUNT);
            final Integer judgeCallsObj = (Integer) ctx.getTransientData().get(ExecutionContext.KEY_JUDGE_CALL_COUNT);
            final Integer verifCallsObj = (Integer) ctx.getTransientData().get(ExecutionContext.KEY_VERIFICATION_CALL_COUNT);
            final Integer pesapCallsObj = (Integer) ctx.getTransientData().get(ExecutionContext.KEY_PESAP_CALL_COUNT);
            final Integer rcaCallsObj = (Integer) ctx.getTransientData().get(ExecutionContext.KEY_RCA_CALL_COUNT);
            final Integer linterCallsObj = (Integer) ctx.getTransientData().get(ExecutionContext.KEY_LINTER_CALL_COUNT);
            final Integer postFlightCallsObj = (Integer) ctx.getTransientData().get(ExecutionContext.KEY_POST_FLIGHT_LINTER_CALL_COUNT);

            final TokenUsage standardUsage = (TokenUsage) ctx.getTransientData().get(ExecutionContext.KEY_STANDARD_TOKEN_USAGE);
            final TokenUsage judgeUsage = (TokenUsage) ctx.getTransientData().get(ExecutionContext.KEY_JUDGE_TOKEN_USAGE);
            final TokenUsage verificationUsage = (TokenUsage) ctx.getTransientData().get(ExecutionContext.KEY_VERIFICATION_TOKEN_USAGE);
            final TokenUsage pesapUsage = (TokenUsage) ctx.getTransientData().get(ExecutionContext.KEY_PESAP_TOKEN_USAGE);
            final TokenUsage rcaUsage = (TokenUsage) ctx.getTransientData().get(ExecutionContext.KEY_RCA_TOKEN_USAGE);
            final TokenUsage linterUsage = (TokenUsage) ctx.getTransientData().get(ExecutionContext.KEY_LINTER_TOKEN_USAGE);
            final TokenUsage postFlightUsage = (TokenUsage) ctx.getTransientData().get(ExecutionContext.KEY_POST_FLIGHT_LINTER_TOKEN_USAGE);

            TokenUsage effectiveStandardUsage = standardUsage;
            int standardCalls = stdCallsObj != null ? stdCallsObj : (standardUsage != null ? 1 : 0);
            if (effectiveStandardUsage == null && this.report.getLlmCalls() != null && !this.report.getLlmCalls().isEmpty())
            {
                long fallbackIn = 0;
                long fallbackOut = 0;
                long fallbackCached = 0;
                long fallbackTotal = 0;
                int fallbackCalls = 0;
                for (final TestExecutionReport.ReportLlmCallEntry call : this.report.getLlmCalls())
                {
                    final String capability = call.getCapability();
                    if (!"LINTER".equalsIgnoreCase(capability) && !"POST_FLIGHT_LINTER".equalsIgnoreCase(capability)
                            && !"PESAP".equalsIgnoreCase(capability)
                            && !"JUDGE".equalsIgnoreCase(capability) && !"JUDGE_DISCUSSION".equalsIgnoreCase(capability)
                            && !"VERIFICATION".equalsIgnoreCase(capability)
                            && !"RCA".equalsIgnoreCase(capability) && !"VISUAL_RCA".equalsIgnoreCase(capability))
                    {
                        fallbackIn += call.getInputTokens();
                        fallbackOut += call.getOutputTokens();
                        fallbackCached += call.getCachedTokens();
                        fallbackTotal += call.getTotalTokens();
                        fallbackCalls++;
                    }
                }
                if (fallbackCalls > 0)
                {
                    effectiveStandardUsage = new TokenUsage((int) fallbackIn, (int) fallbackOut, (int) fallbackTotal, (int) fallbackCached);
                    if (standardCalls == 0)
                    {
                        standardCalls = fallbackCalls;
                    }
                }
            }

            final int judgeCalls = judgeCallsObj != null ? judgeCallsObj : (judgeUsage != null ? 1 : 0);
            final int verificationCalls = verifCallsObj != null ? verifCallsObj : (verificationUsage != null ? 1 : 0);
            final int pesapCalls = pesapCallsObj != null ? pesapCallsObj : (pesapUsage != null ? 1 : 0);
            final int rcaCalls = rcaCallsObj != null ? rcaCallsObj : (rcaUsage != null ? 1 : 0);
            final int linterCalls = linterCallsObj != null ? linterCallsObj : (linterUsage != null ? 1 : 0);
            final int postFlightCalls = postFlightCallsObj != null ? postFlightCallsObj : (postFlightUsage != null ? 1 : 0);

            final String activeModel = (String) ctx.getTransientData().getOrDefault(ExecutionContext.KEY_ACTIVE_MODEL, "default");

            final TestExecutionReport.CategoryTokenUsage actCat = buildCategoryUsage(standardCalls, effectiveStandardUsage, activeModel);
            final TestExecutionReport.CategoryTokenUsage pesapCat = buildCategoryUsage(pesapCalls, pesapUsage, activeModel);
            final TestExecutionReport.CategoryTokenUsage judgeCat = buildCategoryUsage(judgeCalls, judgeUsage, activeModel);
            final TestExecutionReport.CategoryTokenUsage verifCat = buildCategoryUsage(verificationCalls, verificationUsage, activeModel);
            final TestExecutionReport.CategoryTokenUsage rcaCat = buildCategoryUsage(rcaCalls, rcaUsage, activeModel);
            final TestExecutionReport.CategoryTokenUsage linterCat = buildCategoryUsage(linterCalls, linterUsage, activeModel);
            final TestExecutionReport.CategoryTokenUsage postFlightCat = buildCategoryUsage(postFlightCalls, postFlightUsage, activeModel);

            m.setAction(actCat);
            m.setPesap(pesapCat);
            m.setJudge(judgeCat);
            m.setVerification(verifCat);
            m.setVisualRca(rcaCat);
            m.setLinter(linterCat);
            m.setPostFlightLinter(postFlightCat);

            final int totalCalls = standardCalls + judgeCalls + verificationCalls + pesapCalls + rcaCalls + linterCalls + postFlightCalls;
            final long totalIn = actCat.getInputTokens() + pesapCat.getInputTokens() + judgeCat.getInputTokens() + verifCat.getInputTokens() + rcaCat.getInputTokens() + linterCat.getInputTokens() + postFlightCat.getInputTokens();
            final long totalOut = actCat.getOutputTokens() + pesapCat.getOutputTokens() + judgeCat.getOutputTokens() + verifCat.getOutputTokens() + rcaCat.getOutputTokens() + linterCat.getOutputTokens() + postFlightCat.getOutputTokens();
            final long totalCached = actCat.getCachedTokens() + pesapCat.getCachedTokens() + judgeCat.getCachedTokens() + verifCat.getCachedTokens() + rcaCat.getCachedTokens() + linterCat.getCachedTokens() + postFlightCat.getCachedTokens();
            final double totalCost = actCat.getEstimatedCostUsd() + pesapCat.getEstimatedCostUsd() + judgeCat.getEstimatedCostUsd() + verifCat.getEstimatedCostUsd() + rcaCat.getEstimatedCostUsd() + linterCat.getEstimatedCostUsd() + postFlightCat.getEstimatedCostUsd();

            if (totalCalls > 0 || totalIn > 0)
            {
                m.setTotalLlmCalls(totalCalls);
                m.setTokenUsageInput(totalIn);
                m.setTokenUsageOutput(totalOut);
                m.setTokenUsageCached(totalCached);
                m.setTotalTokens(totalIn + totalOut);
                m.setEstimatedCostUsd(totalCost);

                final TestExecutionReport.CategoryTokenUsage totCat = new TestExecutionReport.CategoryTokenUsage(totalCalls, totalIn, totalOut, totalCached, totalCost);
                m.setTotal(totCat);
            }
            else
            {
                recalculateFromDirectLlmCalls(m);
            }

            @SuppressWarnings("unchecked")
            final List<StepStats> stepStatsList = (List<StepStats>) ctx.getTransientData().get("execution.stepStatsList");
            if (stepStatsList != null && !stepStatsList.isEmpty())
            {
                final Map<String, Integer> contextLevelCounts = new LinkedHashMap<>();
                int totalEscalations = 0;

                for (int i = 0; i < stepStatsList.size(); i++)
                {
                    final StepStats stats = stepStatsList.get(i);
                    totalEscalations += aggregateStepStats(stats, contextLevelCounts);

                    if (i < steps.size())
                    {
                        final TestExecutionReport.ReportStepEntry stepEntry = steps.get(i);
                        mergeStepStats(stepEntry, stats);
                    }
                }

                m.setTotalEscalations(totalEscalations);
                m.setContextLevelCounts(contextLevelCounts);
            }
        }
        else
        {
            recalculateFromDirectLlmCalls(m);
        }
    }

    private static TestExecutionReport.CategoryTokenUsage buildCategoryUsage(final int calls, final TokenUsage usage, final String modelName)
    {
        final long in = usage != null ? usage.inputTokenCount() : 0;
        final long out = usage != null ? usage.outputTokenCount() : 0;
        final long cached = usage != null ? usage.cachedTokenCount() : 0;
        final double cost = usage != null ? MetricsCollector.calculateCost(usage, modelName) : 0.0;
        return new TestExecutionReport.CategoryTokenUsage(calls, in, out, cached, cost);
    }

    private void recalculateFromDirectLlmCalls(final TestExecutionReport.ReportMetrics m)
    {
        final List<TestExecutionReport.ReportLlmCallEntry> calls = this.report.getLlmCalls();
        if (calls == null)
        {
            return;
        }

        m.setTotalLlmCalls(calls.size());

        long inTokens = 0;
        long outTokens = 0;
        long cachedTokens = 0;
        double cost = 0.0;

        int actCalls = 0;
        long actIn = 0;
        long actOut = 0;
        long actCached = 0;
        double actCost = 0.0;

        int pesapCalls = 0;
        long pesapIn = 0;
        long pesapOut = 0;
        long pesapCached = 0;
        double pesapCost = 0.0;

        int judgeCalls = 0;
        long judgeIn = 0;
        long judgeOut = 0;
        long judgeCached = 0;
        double judgeCost = 0.0;

        int verifCalls = 0;
        long verifIn = 0;
        long verifOut = 0;
        long verifCached = 0;
        double verifCost = 0.0;

        int rcaCalls = 0;
        long rcaIn = 0;
        long rcaOut = 0;
        long rcaCached = 0;
        double rcaCost = 0.0;

        int linterCalls = 0;
        long linterIn = 0;
        long linterOut = 0;
        long linterCached = 0;
        double linterCost = 0.0;

        int postFlightCalls = 0;
        long postFlightIn = 0;
        long postFlightOut = 0;
        long postFlightCached = 0;
        double postFlightCost = 0.0;

        for (final TestExecutionReport.ReportLlmCallEntry call : calls)
        {
            inTokens += call.getInputTokens();
            outTokens += call.getOutputTokens();
            cachedTokens += call.getCachedTokens();
            cost += call.getEstimatedCostUsd();

            final String cap = call.getCapability();
            if ("RCA".equalsIgnoreCase(cap) || "VISUAL_RCA".equalsIgnoreCase(cap))
            {
                rcaIn += call.getInputTokens();
                rcaOut += call.getOutputTokens();
                rcaCached += call.getCachedTokens();
                rcaCost += call.getEstimatedCostUsd();
                rcaCalls++;
            }
            else if ("PESAP".equalsIgnoreCase(cap))
            {
                pesapIn += call.getInputTokens();
                pesapOut += call.getOutputTokens();
                pesapCached += call.getCachedTokens();
                pesapCost += call.getEstimatedCostUsd();
                pesapCalls++;
            }
            else if ("JUDGE".equalsIgnoreCase(cap) || "JUDGE_DISCUSSION".equalsIgnoreCase(cap))
            {
                judgeIn += call.getInputTokens();
                judgeOut += call.getOutputTokens();
                judgeCached += call.getCachedTokens();
                judgeCost += call.getEstimatedCostUsd();
                judgeCalls++;
            }
            else if ("VERIFICATION".equalsIgnoreCase(cap))
            {
                verifIn += call.getInputTokens();
                verifOut += call.getOutputTokens();
                verifCached += call.getCachedTokens();
                verifCost += call.getEstimatedCostUsd();
                verifCalls++;
            }
            else if ("LINTER".equalsIgnoreCase(cap))
            {
                linterIn += call.getInputTokens();
                linterOut += call.getOutputTokens();
                linterCached += call.getCachedTokens();
                linterCost += call.getEstimatedCostUsd();
                linterCalls++;
            }
            else if ("POST_FLIGHT_LINTER".equalsIgnoreCase(cap))
            {
                postFlightIn += call.getInputTokens();
                postFlightOut += call.getOutputTokens();
                postFlightCached += call.getCachedTokens();
                postFlightCost += call.getEstimatedCostUsd();
                postFlightCalls++;
            }
            else
            {
                actIn += call.getInputTokens();
                actOut += call.getOutputTokens();
                actCached += call.getCachedTokens();
                actCost += call.getEstimatedCostUsd();
                actCalls++;
            }
        }

        m.setTokenUsageInput(inTokens);
        m.setTokenUsageOutput(outTokens);
        m.setTokenUsageCached(cachedTokens);
        m.setTotalTokens(inTokens + outTokens);
        m.setEstimatedCostUsd(cost);

        final TestExecutionReport.CategoryTokenUsage totCat = new TestExecutionReport.CategoryTokenUsage(calls.size(), inTokens, outTokens, cachedTokens, cost);
        m.setTotal(totCat);
        m.setAction(new TestExecutionReport.CategoryTokenUsage(actCalls, actIn, actOut, actCached, actCost));
        m.setPesap(new TestExecutionReport.CategoryTokenUsage(pesapCalls, pesapIn, pesapOut, pesapCached, pesapCost));
        m.setJudge(new TestExecutionReport.CategoryTokenUsage(judgeCalls, judgeIn, judgeOut, judgeCached, judgeCost));
        m.setVerification(new TestExecutionReport.CategoryTokenUsage(verifCalls, verifIn, verifOut, verifCached, verifCost));
        m.setVisualRca(new TestExecutionReport.CategoryTokenUsage(rcaCalls, rcaIn, rcaOut, rcaCached, rcaCost));
        m.setLinter(new TestExecutionReport.CategoryTokenUsage(linterCalls, linterIn, linterOut, linterCached, linterCost));
        m.setPostFlightLinter(new TestExecutionReport.CategoryTokenUsage(postFlightCalls, postFlightIn, postFlightOut, postFlightCached, postFlightCost));
    }

    private static int aggregateStepStats(final StepStats stats, final Map<String, Integer> contextLevelCounts)
    {
        if (stats == null)
        {
            return 0;
        }
        int escalations = 0;
        final List<String> levels = stats.getContextLevels();
        if (levels != null && !levels.isEmpty())
        {
            escalations += Math.max(0, levels.size() - 1);
            for (final String lvl : levels)
            {
                if (lvl != null && !lvl.isBlank())
                {
                    final String normalized = lvl.trim().toUpperCase();
                    contextLevelCounts.put(normalized, contextLevelCounts.getOrDefault(normalized, 0) + 1);
                }
            }
        }
        for (final StepStats child : stats.getSubStats())
        {
            escalations += aggregateStepStats(child, contextLevelCounts);
        }
        return escalations;
    }

    private static void mergeStepStats(final TestExecutionReport.ReportStepEntry entry, final StepStats stats)
    {
        if (entry == null || stats == null)
        {
            return;
        }
        if (stats.getDurationMs() > 0)
        {
            entry.setDurationMs(stats.getDurationMs());
        }
        final List<String> levels = stats.getContextLevels();
        if (levels != null && !levels.isEmpty())
        {
            entry.setEscalations(Math.max(0, levels.size() - 1));
            entry.setContextLevels(String.join(" → ", levels));
        }
        entry.setPesapCalls(stats.getPesapCalls());
        entry.setPesapInputTokens(stats.getPesapInputTokens());
        entry.setPesapOutputTokens(stats.getPesapOutputTokens());
        entry.setPesapCachedTokens(stats.getPesapCachedTokens());

        entry.setStandardCalls(stats.getStandardCalls());
        entry.setStandardInputTokens(stats.getStandardInputTokens());
        entry.setStandardOutputTokens(stats.getStandardOutputTokens());
        entry.setStandardCachedTokens(stats.getStandardCachedTokens());

        entry.setVerificationCalls(stats.getVerificationCalls());
        entry.setVerificationInputTokens(stats.getVerificationInputTokens());
        entry.setVerificationOutputTokens(stats.getVerificationOutputTokens());
        entry.setVerificationCachedTokens(stats.getVerificationCachedTokens());

        entry.setRcaCalls(stats.getRcaCalls());
        entry.setRcaInputTokens(stats.getRcaInputTokens());
        entry.setRcaOutputTokens(stats.getRcaOutputTokens());
        entry.setRcaCachedTokens(stats.getRcaCachedTokens());

        if (stats.isMultiStage())
        {
            entry.setMultiStage(true);
        }

        if (stats.getSubStats() != null && !stats.getSubStats().isEmpty())
        {
            for (int s = 0; s < stats.getSubStats().size(); s++)
            {
                final StepStats sub = stats.getSubStats().get(s);
                TestExecutionReport.ReportStepEntry matchingSub = null;
                for (final TestExecutionReport.ReportStepEntry existingSub : entry.getSubSteps())
                {
                    if (sub.getInstruction() != null && (sub.getInstruction().equals(existingSub.getInstruction()) || sub.getInstruction().equals(existingSub.getRawInstruction())))
                    {
                        matchingSub = existingSub;
                        break;
                    }
                }

                if (matchingSub == null && s < entry.getSubSteps().size())
                {
                    matchingSub = entry.getSubSteps().get(s);
                }

                if (matchingSub != null)
                {
                    if (sub.getInstruction() != null && !sub.getInstruction().isBlank())
                    {
                        if (matchingSub.getRawInstruction() == null)
                        {
                            matchingSub.setRawInstruction(matchingSub.getInstruction());
                        }
                        if (matchingSub.getInstruction() != null && matchingSub.getInstruction().contains("${"))
                        {
                            matchingSub.setInstruction(sub.getInstruction());
                        }
                    }
                    mergeStepStats(matchingSub, sub);
                    if (matchingSub.getStatus() == null || "PENDING".equalsIgnoreCase(matchingSub.getStatus()) || "RUNNING".equalsIgnoreCase(matchingSub.getStatus()))
                    {
                        matchingSub.setStatus(sub.getFailureReason() != null ? "FAILED" : "SUCCESS");
                    }
                }
                else if (entry.getSubSteps().isEmpty())
                {
                    final TestExecutionReport.ReportStepEntry subEntry = new TestExecutionReport.ReportStepEntry(
                        s,
                        sub.getInstruction() != null ? sub.getInstruction() : "Sub-step " + (s + 1)
                    );
                    mergeStepStats(subEntry, sub);
                    subEntry.setStatus(sub.getFailureReason() != null ? "FAILED" : "SUCCESS");
                    entry.addSubStep(subEntry);
                }
            }
        }

        if (!entry.getSubSteps().isEmpty())
        {
            if (entry.getPesapCalls() == 0 && entry.getStandardCalls() == 0 && entry.getVerificationCalls() == 0 && entry.getRcaCalls() == 0)
            {
                for (final TestExecutionReport.ReportStepEntry child : entry.getSubSteps())
                {
                    entry.setPesapCalls(entry.getPesapCalls() + child.getPesapCalls());
                    entry.setPesapInputTokens(entry.getPesapInputTokens() + child.getPesapInputTokens());
                    entry.setPesapOutputTokens(entry.getPesapOutputTokens() + child.getPesapOutputTokens());
                    entry.setPesapCachedTokens(entry.getPesapCachedTokens() + child.getPesapCachedTokens());

                    entry.setStandardCalls(entry.getStandardCalls() + child.getStandardCalls());
                    entry.setStandardInputTokens(entry.getStandardInputTokens() + child.getStandardInputTokens());
                    entry.setStandardOutputTokens(entry.getStandardOutputTokens() + child.getStandardOutputTokens());
                    entry.setStandardCachedTokens(entry.getStandardCachedTokens() + child.getStandardCachedTokens());

                    entry.setVerificationCalls(entry.getVerificationCalls() + child.getVerificationCalls());
                    entry.setVerificationInputTokens(entry.getVerificationInputTokens() + child.getVerificationInputTokens());
                    entry.setVerificationOutputTokens(entry.getVerificationOutputTokens() + child.getVerificationOutputTokens());
                    entry.setVerificationCachedTokens(entry.getVerificationCachedTokens() + child.getVerificationCachedTokens());

                    entry.setRcaCalls(entry.getRcaCalls() + child.getRcaCalls());
                    entry.setRcaInputTokens(entry.getRcaInputTokens() + child.getRcaInputTokens());
                    entry.setRcaOutputTokens(entry.getRcaOutputTokens() + child.getRcaOutputTokens());
                    entry.setRcaCachedTokens(entry.getRcaCachedTokens() + child.getRcaCachedTokens());
                }
            }
        }
    }

    /**
     * Flushes generated reports to disk across configured formats.
     */
    public synchronized void flushReport()
    {
        if (!this.reportFlushed.compareAndSet(false, true))
        {
            return;
        }

        try
        {
            final Path configuredDiskReportDir = Paths.get(AiConfiguration.getInstance().getDiskReportDirectory());
            final Path rootOutputDir = (this.isCustomOutputDirectory && !this.outputDirectory.equals(configuredDiskReportDir))
                ? this.outputDirectory
                : Paths.get("target/ai-results");
            if (!Files.exists(rootOutputDir))
            {
                Files.createDirectories(rootOutputDir);
            }
            if (!Files.exists(this.outputDirectory))
            {
                Files.createDirectories(this.outputDirectory);
            }

            final String baseFileName = computeBaseFileName();
            this.lastBaseFileName = baseFileName;

            for (final DiskReportFormat format : this.formats)
            {
                switch (format)
                {
                    case HTML -> {
                        final String htmlContent = this.htmlGenerator.generate(this.report);
                        final Path htmlFile = rootOutputDir.resolve(baseFileName + ".html");
                        Files.writeString(htmlFile, htmlContent, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                        LOGGER.info("📊 Preliminary HTML report written: {}", htmlFile.toAbsolutePath());
                    }
                    case MARKDOWN -> {
                        final String mdContent = this.markdownGenerator.generate(this.report);
                        final Path mdFile = rootOutputDir.resolve(baseFileName + ".md");
                        Files.writeString(mdFile, mdContent, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                        LOGGER.info("📝 Preliminary Markdown report written: {}", mdFile.toAbsolutePath());
                    }
                    case JSON -> {
                        final String jsonContent = this.jsonGenerator.generate(this.report);
                        final Path jsonFile = rootOutputDir.resolve(baseFileName + ".json");
                        Files.writeString(jsonFile, jsonContent, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                        LOGGER.info("💾 Preliminary JSON report written: {}", jsonFile.toAbsolutePath());
                    }
                    case ALL -> {
                        // Handled by parseFormats expanding to HTML, MARKDOWN, JSON
                    }
                }
            }

            if (this.formats.contains(DiskReportFormat.HTML) || this.formats.contains(DiskReportFormat.ALL))
            {
                this.indexGenerator.updateIndex(rootOutputDir, this.report, baseFileName);
            }
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to write preliminary disk reports to {}: {}", this.outputDirectory, e.getMessage(), e);
        }
    }

    private String computeBaseFileName()
    {
        final StringBuilder sb = new StringBuilder();

        if (this.report.getTestClass() != null && !this.report.getTestClass().isBlank())
        {
            sb.append(extractSimpleClassName(this.report.getTestClass()));
        }
        else
        {
            sb.append("AiTest");
        }

        if (this.report.getTestMethod() != null && !this.report.getTestMethod().isBlank())
        {
            sb.append("_").append(this.report.getTestMethod());
        }

        if (this.report.getDatasetId() != null && !this.report.getDatasetId().isBlank() && !"default".equalsIgnoreCase(this.report.getDatasetId()))
        {
            sb.append("_").append(this.report.getDatasetId());
        }

        final long startTs = this.report.getStartTimeMs() > 0 ? this.report.getStartTimeMs() : System.currentTimeMillis();
        final String tsStr;
        synchronized (FILE_TIMESTAMP_FORMAT)
        {
            tsStr = FILE_TIMESTAMP_FORMAT.format(new Date(startTs));
        }
        sb.append("_").append(tsStr);

        final String raw = sb.toString().replaceAll("[^a-zA-Z0-9._-]", "_");

        String candidate = raw;
        int counter = 1;
        while (Files.exists(this.outputDirectory.resolve(candidate + ".html"))
            || Files.exists(this.outputDirectory.resolve(candidate + ".json"))
            || Files.exists(this.outputDirectory.resolve(candidate + ".md")))
        {
            candidate = raw + "_" + counter++;
        }

        return candidate;
    }

    private static String extractSimpleClassName(final String fqcn)
    {
        if (fqcn == null)
        {
            return "TestClass";
        }
        final int lastDot = fqcn.lastIndexOf('.');
        return lastDot >= 0 ? fqcn.substring(lastDot + 1) : fqcn;
    }
}
