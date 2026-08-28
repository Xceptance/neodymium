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
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.neodymium.ai.model.PlaybookStepStatus;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.StepStats;
import org.neodymium.ai.telemetry.MetricsCollector;
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

    private final Path outputDirectory;
    private final Set<DiskReportFormat> formats;
    private final boolean enabled;
    private final TestExecutionReport report = new TestExecutionReport();
    private final HtmlReportGenerator htmlGenerator = new HtmlReportGenerator();
    private final MarkdownReportGenerator markdownGenerator = new MarkdownReportGenerator();
    private final JsonReportGenerator jsonGenerator = new JsonReportGenerator();
    private final HtmlIndexReportGenerator indexGenerator = new HtmlIndexReportGenerator();

    private TestExecutionReport.ReportStepEntry currentStep;
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
        this.outputDirectory = outputDirectory != null ? outputDirectory : Paths.get("target/ai-reports");
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

                if (pbStep.getReasoning() != null)
                {
                    stepEntry.setReasoning(pbStep.getReasoning());
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

                parentEntry.addSubStep(stepEntry);
                this.currentStep = stepEntry;
                return;
            }

            this.report.addStep(stepEntry);
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
                if (pbStep != null)
                {
                    if (pbStep.getReasoning() != null && !pbStep.getReasoning().isBlank())
                    {
                        targetStep.setReasoning(pbStep.getReasoning());
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
                }
            }
        }
        else if (event instanceof ActionExecutedEvent actionExecuted)
        {
            if (this.currentStep != null && actionExecuted.getAction() != null)
            {
                final org.neodymium.ai.action.Action action = actionExecuted.getAction();
                final org.neodymium.ai.action.Action resolvedAction = actionExecuted.getResolvedAction();
                final TestExecutionReport.ReportActionEntry actionEntry = new TestExecutionReport.ReportActionEntry(
                    action.getType(),
                    action.getTarget(),
                    action.getValue(),
                    action.getDescription(),
                    action.getReasoning(),
                    actionExecuted.isSuccess()
                );
                if (resolvedAction != null)
                {
                    actionEntry.setResolvedTarget(resolvedAction.getTarget());
                    actionEntry.setResolvedValue(resolvedAction.getValue());
                }
                this.currentStep.addAction(actionEntry);
            }
        }
        else if (event instanceof LlmResponseReceivedEvent llmReceived)
        {
            final int stepIdx = this.currentStep != null ? this.currentStep.getStepIndex() : 0;
            final TestExecutionReport.ReportLlmCallEntry callEntry = new TestExecutionReport.ReportLlmCallEntry();
            callEntry.setStepIndex(stepIdx);
            callEntry.setCapability(llmReceived.getCapability());
            callEntry.setDurationMs(llmReceived.getDurationMs());

            if (llmReceived.getRequest() != null)
            {
                callEntry.setSystemPrompt(llmReceived.getRequest().systemMessage());
                callEntry.setUserPrompt(llmReceived.getRequest().userMessage());
            }

            if (llmReceived.getResponse() != null)
            {
                callEntry.setModelName(llmReceived.getResponse().modelName());
                callEntry.setResponseContent(llmReceived.getResponse().content());

                final TokenUsage usage = llmReceived.getResponse().tokenUsage();
                if (usage != null)
                {
                    callEntry.setInputTokens(usage.inputTokenCount());
                    callEntry.setOutputTokens(usage.outputTokenCount());
                    callEntry.setCachedTokens(usage.cachedTokenCount());
                    callEntry.setTotalTokens(usage.totalTokenCount());
                    callEntry.setEstimatedCostUsd(MetricsCollector.calculateCost(usage, llmReceived.getResponse().modelName()));
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
                final int stepIdx = this.currentStep != null ? this.currentStep.getStepIndex() : 0;
                final boolean isVisual = this.currentStep != null && this.currentStep.isVisual();
                final String label = "Step #" + (stepIdx + 1) + (isVisual ? " (Visual Verification)" : " Capture");

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
                        this.report.addScreenshot(screenshot);
                        if (this.currentStep != null)
                        {
                            this.currentStep.addScreenshot(screenshot);
                        }
                        LOGGER.debug("   📸 Recorded screenshot for step #{}: \"{}\" | Dimensions: {}",
                            stepIdx + 1, label, screenshot.getDimensions() != null ? screenshot.getDimensions() : "unknown");
                    }
                }
            }
        }
        else if (event instanceof DiagnosticErrorEvent errorEvent)
        {
            if (this.report.getFailureReason() == null)
            {
                this.report.setFailureReason(errorEvent.getMessage());
            }
            if (errorEvent.getCause() != null && this.report.getFailureStackTrace() == null)
            {
                final StringWriter sw = new StringWriter();
                errorEvent.getCause().printStackTrace(new PrintWriter(sw));
                this.report.setFailureStackTrace(sw.toString());
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
        final List<TestExecutionReport.ReportStepEntry> steps = this.report.getSteps();
        for (int i = 0; i < steps.size(); i++)
        {
            resolveStep(steps.get(i));
        }
    }

    private void resolveStep(final TestExecutionReport.ReportStepEntry step)
    {
        if (step == null)
        {
            return;
        }

        if (!step.getSubSteps().isEmpty())
        {
            boolean anySubFailed = false;
            boolean allSubSuccess = true;
            long totalSubDuration = 0;

            for (final TestExecutionReport.ReportStepEntry sub : step.getSubSteps())
            {
                resolveStep(sub);
                totalSubDuration += sub.getDurationMs();
                if ("FAILED".equalsIgnoreCase(sub.getStatus()))
                {
                    anySubFailed = true;
                    allSubSuccess = false;
                }
                else if (!"SUCCESS".equalsIgnoreCase(sub.getStatus()) && !"PASSED".equalsIgnoreCase(sub.getStatus()) && !"HEALED".equalsIgnoreCase(sub.getStatus()))
                {
                    allSubSuccess = false;
                }
            }

            if (anySubFailed || (!this.report.isSuccess() && !allSubSuccess))
            {
                step.setStatus("FAILED");
                if (step.getFailureReason() == null && this.report.getFailureReason() != null)
                {
                    step.setFailureReason(this.report.getFailureReason());
                }
            }
            else if (allSubSuccess)
            {
                step.setStatus("SUCCESS");
            }
            else
            {
                step.setStatus(!this.report.isSuccess() ? "FAILED" : "SUCCESS");
            }

            if (step.getDurationMs() <= 0)
            {
                step.setDurationMs(totalSubDuration);
            }
        }
        else if ("RUNNING".equalsIgnoreCase(step.getStatus()) || "PENDING".equalsIgnoreCase(step.getStatus()) || step.getStatus() == null)
        {
            if (!this.report.isSuccess())
            {
                step.setStatus("FAILED");
                if (step.getFailureReason() == null && this.report.getFailureReason() != null)
                {
                    step.setFailureReason(this.report.getFailureReason());
                }
            }
            else
            {
                step.setStatus("SUCCESS");
            }

            if (step.getDurationMs() <= 0 && step.getStartTimeMs() > 0)
            {
                step.setDurationMs(Math.max(1, System.currentTimeMillis() - step.getStartTimeMs()));
            }
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

            final TokenUsage standardUsage = (TokenUsage) ctx.getTransientData().get(ExecutionContext.KEY_STANDARD_TOKEN_USAGE);
            final TokenUsage judgeUsage = (TokenUsage) ctx.getTransientData().get(ExecutionContext.KEY_JUDGE_TOKEN_USAGE);
            final TokenUsage verificationUsage = (TokenUsage) ctx.getTransientData().get(ExecutionContext.KEY_VERIFICATION_TOKEN_USAGE);
            final TokenUsage pesapUsage = (TokenUsage) ctx.getTransientData().get(ExecutionContext.KEY_PESAP_TOKEN_USAGE);
            final TokenUsage rcaUsage = (TokenUsage) ctx.getTransientData().get(ExecutionContext.KEY_RCA_TOKEN_USAGE);

            final int standardCalls = stdCallsObj != null ? stdCallsObj : (standardUsage != null ? 1 : 0);
            final int judgeCalls = judgeCallsObj != null ? judgeCallsObj : (judgeUsage != null ? 1 : 0);
            final int verificationCalls = verifCallsObj != null ? verifCallsObj : (verificationUsage != null ? 1 : 0);
            final int pesapCalls = pesapCallsObj != null ? pesapCallsObj : (pesapUsage != null ? 1 : 0);
            final int rcaCalls = rcaCallsObj != null ? rcaCallsObj : (rcaUsage != null ? 1 : 0);

            final String activeModel = (String) ctx.getTransientData().getOrDefault(ExecutionContext.KEY_ACTIVE_MODEL, "default");

            final TestExecutionReport.CategoryTokenUsage actCat = buildCategoryUsage(standardCalls, standardUsage, activeModel);
            final TestExecutionReport.CategoryTokenUsage pesapCat = buildCategoryUsage(pesapCalls, pesapUsage, activeModel);
            final TestExecutionReport.CategoryTokenUsage judgeCat = buildCategoryUsage(judgeCalls, judgeUsage, activeModel);
            final TestExecutionReport.CategoryTokenUsage verifCat = buildCategoryUsage(verificationCalls, verificationUsage, activeModel);
            final TestExecutionReport.CategoryTokenUsage rcaCat = buildCategoryUsage(rcaCalls, rcaUsage, activeModel);

            m.setAction(actCat);
            m.setPesap(pesapCat);
            m.setJudge(judgeCat);
            m.setVerification(verifCat);
            m.setVisualRca(rcaCat);

            final int totalCalls = standardCalls + judgeCalls + verificationCalls + pesapCalls + rcaCalls;
            final long totalIn = actCat.getInputTokens() + pesapCat.getInputTokens() + judgeCat.getInputTokens() + verifCat.getInputTokens() + rcaCat.getInputTokens();
            final long totalOut = actCat.getOutputTokens() + pesapCat.getOutputTokens() + judgeCat.getOutputTokens() + verifCat.getOutputTokens() + rcaCat.getOutputTokens();
            final long totalCached = actCat.getCachedTokens() + pesapCat.getCachedTokens() + judgeCat.getCachedTokens() + verifCat.getCachedTokens() + rcaCat.getCachedTokens();
            final double totalCost = actCat.getEstimatedCostUsd() + pesapCat.getEstimatedCostUsd() + judgeCat.getEstimatedCostUsd() + verifCat.getEstimatedCostUsd() + rcaCat.getEstimatedCostUsd();

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
        m.setTotalLlmCalls(calls.size());

        long inTokens = 0;
        long outTokens = 0;
        long cachedTokens = 0;
        double cost = 0.0;

        for (final TestExecutionReport.ReportLlmCallEntry call : calls)
        {
            inTokens += call.getInputTokens();
            outTokens += call.getOutputTokens();
            cachedTokens += call.getCachedTokens();
            cost += call.getEstimatedCostUsd();
        }

        m.setTokenUsageInput(inTokens);
        m.setTokenUsageOutput(outTokens);
        m.setTokenUsageCached(cachedTokens);
        m.setTotalTokens(inTokens + outTokens);
        m.setEstimatedCostUsd(cost);

        final TestExecutionReport.CategoryTokenUsage totCat = new TestExecutionReport.CategoryTokenUsage(calls.size(), inTokens, outTokens, cachedTokens, cost);
        m.setTotal(totCat);
        m.setAction(totCat);
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

                if (matchingSub != null)
                {
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
                        final Path htmlFile = this.outputDirectory.resolve(baseFileName + ".html");
                        Files.write(htmlFile, htmlContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                        LOGGER.info("📊 Preliminary HTML report written: {}", htmlFile.toAbsolutePath());
                    }
                    case MARKDOWN -> {
                        final String mdContent = this.markdownGenerator.generate(this.report);
                        final Path mdFile = this.outputDirectory.resolve(baseFileName + ".md");
                        Files.write(mdFile, mdContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                        LOGGER.info("📝 Preliminary Markdown report written: {}", mdFile.toAbsolutePath());
                    }
                    case JSON -> {
                        final String jsonContent = this.jsonGenerator.generate(this.report);
                        final Path jsonFile = this.outputDirectory.resolve(baseFileName + ".json");
                        Files.write(jsonFile, jsonContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                        LOGGER.info("💾 Preliminary JSON report written: {}", jsonFile.toAbsolutePath());
                    }
                    case ALL -> {
                        // Handled by parseFormats expanding to HTML, MARKDOWN, JSON
                    }
                }
            }

            if (this.formats.contains(DiskReportFormat.HTML) || this.formats.contains(DiskReportFormat.ALL))
            {
                this.indexGenerator.updateIndex(this.outputDirectory, this.report, baseFileName);
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
