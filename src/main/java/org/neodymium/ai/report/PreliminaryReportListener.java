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
import java.util.Collections;
import java.util.List;
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

    private final Path outputDirectory;
    private final Set<DiskReportFormat> formats;
    private final boolean enabled;
    private final TestExecutionReport report = new TestExecutionReport();
    private final HtmlReportGenerator htmlGenerator = new HtmlReportGenerator();
    private final MarkdownReportGenerator markdownGenerator = new MarkdownReportGenerator();
    private final JsonReportGenerator jsonGenerator = new JsonReportGenerator();

    private TestExecutionReport.ReportStepEntry currentStep;
    private final AtomicBoolean reportFlushed = new AtomicBoolean(false);

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
     * @param outputDirectory output folder path
     * @param formats set of active report formats
     * @param enabled whether reporting is active
     */
    public PreliminaryReportListener(final Path outputDirectory, final Set<DiskReportFormat> formats, final boolean enabled)
    {
        this.outputDirectory = outputDirectory != null ? outputDirectory : Paths.get("target/ai-reports");
        this.formats = formats != null && !formats.isEmpty() ? formats : Collections.singleton(DiskReportFormat.HTML);
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
        if (event instanceof StepStartedEvent stepStarted)
        {
            final PlaybookStep pbStep = stepStarted.getStep();
            final String instruction = pbStep != null ? pbStep.getInstruction() : "Step " + (stepStarted.getStepIndex() + 1);
            final TestExecutionReport.ReportStepEntry stepEntry = new TestExecutionReport.ReportStepEntry(stepStarted.getStepIndex(), instruction);
            stepEntry.setStartTimeMs(System.currentTimeMillis());
            stepEntry.setStatus("RUNNING");

            if (pbStep != null)
            {
                stepEntry.setSourceFile(pbStep.getSourceFile());
                stepEntry.setLineNumber(pbStep.getLineNumber());
                if (pbStep.getReasoning() != null)
                {
                    stepEntry.setReasoning(pbStep.getReasoning());
                }
            }

            this.report.addStep(stepEntry);
            this.currentStep = stepEntry;
        }
        else if (event instanceof StepFinishedEvent stepFinished)
        {
            final PlaybookStep pbStep = stepFinished.getStep();
            final PlaybookStepStatus status = stepFinished.getStatus();

            TestExecutionReport.ReportStepEntry targetStep = this.currentStep;
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
                }
            }
        }
        else if (event instanceof ActionExecutedEvent actionExecuted)
        {
            if (this.currentStep != null && actionExecuted.getAction() != null)
            {
                final org.neodymium.ai.action.Action action = actionExecuted.getAction();
                final TestExecutionReport.ReportActionEntry actionEntry = new TestExecutionReport.ReportActionEntry(
                    action.getType(),
                    action.getTarget(),
                    action.getValue(),
                    action.getDescription(),
                    action.getReasoning(),
                    actionExecuted.isSuccess()
                );
                this.currentStep.addAction(actionEntry);
            }
        }
        else if (event instanceof LlmResponseReceivedEvent llmReceived)
        {
            final TestExecutionReport.ReportLlmCallEntry callEntry = new TestExecutionReport.ReportLlmCallEntry();
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
        }
        else if (event instanceof StateCapturedEvent stateCaptured)
        {
            if (stateCaptured.getState() != null && stateCaptured.getState().getAttachments() != null)
            {
                final int stepIdx = this.currentStep != null ? this.currentStep.getStepIndex() : 0;
                for (final SutAttachment attachment : stateCaptured.getState().getAttachments())
                {
                    if (attachment != null && attachment.base64Data() != null && !attachment.base64Data().isEmpty())
                    {
                        final TestExecutionReport.ReportScreenshotEntry screenshot = new TestExecutionReport.ReportScreenshotEntry(
                            "Step " + (stepIdx + 1) + " Capture",
                            stepIdx,
                            attachment.mediaType() != null ? attachment.mediaType() : "image/png",
                            attachment.base64Data(),
                            System.currentTimeMillis()
                        );
                        this.report.addScreenshot(screenshot);
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

            populateContextMetadata();
            recalculateMetrics();
            flushReport();
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

            for (final DiskReportFormat format : this.formats)
            {
                switch (format)
                {
                    case HTML -> {
                        final String htmlContent = this.htmlGenerator.generate(this.report);
                        final Path htmlFile = this.outputDirectory.resolve(baseFileName + ".html");
                        Files.writeString(htmlFile, htmlContent, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                        LOGGER.info("📊 Preliminary HTML report written: {}", htmlFile.toAbsolutePath());
                    }
                    case MARKDOWN -> {
                        final String mdContent = this.markdownGenerator.generate(this.report);
                        final Path mdFile = this.outputDirectory.resolve(baseFileName + ".md");
                        Files.writeString(mdFile, mdContent, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                        LOGGER.info("📝 Preliminary Markdown report written: {}", mdFile.toAbsolutePath());
                    }
                    case JSON -> {
                        final String jsonContent = this.jsonGenerator.generate(this.report);
                        final Path jsonFile = this.outputDirectory.resolve(baseFileName + ".json");
                        Files.writeString(jsonFile, jsonContent, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                        LOGGER.info("💾 Preliminary JSON report written: {}", jsonFile.toAbsolutePath());
                    }
                    case ALL -> {
                        // Handled by parseFormats expanding to HTML, MARKDOWN, JSON
                    }
                }
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

        final String raw = sb.toString();
        return raw.replaceAll("[^a-zA-Z0-9._-]", "_");
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
