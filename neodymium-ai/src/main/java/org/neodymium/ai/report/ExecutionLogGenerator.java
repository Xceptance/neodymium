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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.neodymium.ai.playbook.linter.PlaybookLinterFinding;

/**
 * Generator that formats a {@link TestExecutionReport} into a clean, human-readable
 * chronological plain-text execution log.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class ExecutionLogGenerator
{
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS 'UTC'").withZone(ZoneOffset.UTC);
    private static final DecimalFormat COST_FORMAT = new DecimalFormat("$#,##0.0000", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.US));

    /**
     * Constructs a default ExecutionLogGenerator.
     */
    public ExecutionLogGenerator()
    {
    }

    /**
     * Converts a test execution report to a structured plain-text log string.
     *
     * @param report the test execution report
     * @return plain text log output
     */
    public String generate(final TestExecutionReport report)
    {
        if (report == null)
        {
            return "Test Execution Log: No execution report data available.\n";
        }

        final StringBuilder sb = new StringBuilder();
        final String testName = report.getTestName() != null ? report.getTestName() : "AI Test Execution";

        sb.append("================================================================================\n");
        sb.append("Neodymium AI Test Execution Log: ").append(testName).append("\n");
        sb.append("================================================================================\n");

        if (report.getTestClass() != null && !report.getTestClass().isBlank())
        {
            sb.append("Test Class:       ").append(report.getTestClass()).append("\n");
        }
        if (report.getTestMethod() != null && !report.getTestMethod().isBlank())
        {
            sb.append("Test Method:      ").append(report.getTestMethod()).append("\n");
        }
        if (report.getDatasetId() != null && !report.getDatasetId().isBlank())
        {
            sb.append("Dataset:          ").append(report.getDatasetId()).append("\n");
        }
        if (report.getExecutionMode() != null && !report.getExecutionMode().isBlank())
        {
            sb.append("Execution Mode:   ").append(report.getExecutionMode()).append("\n");
        }
        if (report.getPlaybookFile() != null && !report.getPlaybookFile().isBlank())
        {
            sb.append("Playbook File:    ").append(report.getPlaybookFile()).append("\n");
        }

        final long startMs = report.getStartTimeMs();
        if (startMs > 0)
        {
            sb.append("Start Time:       ").append(ISO_FORMATTER.format(Instant.ofEpochMilli(startMs))).append("\n");
        }
        final long endMs = report.getEndTimeMs();
        if (endMs > 0)
        {
            sb.append("End Time:         ").append(ISO_FORMATTER.format(Instant.ofEpochMilli(endMs))).append("\n");
        }
        sb.append("Duration:         ").append(NUMBER_FORMAT.format(report.getDurationMs())).append(" ms\n");
        sb.append("Status:           ").append(report.isSuccess() ? "PASSED" : "FAILED");
        if (report.isExpectedFailure())
        {
            sb.append(" (Expected Failure)");
        }
        sb.append("\n");
        sb.append("================================================================================\n\n");

        // Failure Details if failed
        if (!report.isSuccess() && (report.getFailureReason() != null || report.getFailureStackTrace() != null))
        {
            sb.append("--- FAILURE DETAILS ---\n");
            if (report.getFailureReason() != null && !report.getFailureReason().isBlank())
            {
                sb.append("Reason:\n  ").append(report.getFailureReason().trim()).append("\n\n");
            }
            if (report.getFailureStackTrace() != null && !report.getFailureStackTrace().isBlank())
            {
                sb.append("Stack Trace:\n");
                for (final String line : report.getFailureStackTrace().split("\\R"))
                {
                    sb.append("  ").append(line).append("\n");
                }
                sb.append("\n");
            }
        }

        // Steps Timeline
        final List<TestExecutionReport.ReportStepEntry> steps = report.getSteps();
        if (!steps.isEmpty())
        {
            sb.append("--- EXECUTION STEPS TIMELINE ---\n");
            for (int i = 0; i < steps.size(); i++)
            {
                final TestExecutionReport.ReportStepEntry step = steps.get(i);
                final int stepNum = step.getStepIndex() > 0 ? step.getStepIndex() : (i + 1);
                sb.append(String.format(Locale.ROOT, "[Step %02d] %s\n", stepNum, step.getInstruction() != null ? step.getInstruction() : ""));
                sb.append(String.format(Locale.ROOT, "  Status:   %s (%s ms)\n", step.getStatus() != null ? step.getStatus() : "UNKNOWN", NUMBER_FORMAT.format(step.getDurationMs())));

                if (step.getActions() != null && !step.getActions().isEmpty())
                {
                    sb.append("  Actions:\n");
                    for (final TestExecutionReport.ReportActionEntry action : step.getActions())
                    {
                        final String status = action.isSuccess() ? "SUCCESS" : "FAILED";
                        sb.append(String.format(Locale.ROOT, "    - %s: %s -> %s\n",
                            action.getType() != null ? action.getType() : "ACTION",
                            action.getDescription() != null ? action.getDescription() : (action.getTarget() != null ? action.getTarget() : ""),
                            status));
                        if (action.getReasoning() != null && !action.getReasoning().isBlank())
                        {
                            sb.append("      Reasoning: ").append(action.getReasoning().trim()).append("\n");
                        }
                    }
                }

                if (step.getFailureReason() != null && !step.getFailureReason().isBlank())
                {
                    sb.append("  Step Error: ").append(step.getFailureReason().trim()).append("\n");
                }
                sb.append("\n");
            }
        }

        // LLM Interactions Summary
        final List<TestExecutionReport.ReportLlmCallEntry> llmCalls = report.getLlmCalls();
        if (!llmCalls.isEmpty())
        {
            sb.append("--- LLM INTERACTIONS ---\n");
            long totalInTokens = 0;
            long totalOutTokens = 0;
            double totalCost = 0.0;

            for (int i = 0; i < llmCalls.size(); i++)
            {
                final TestExecutionReport.ReportLlmCallEntry call = llmCalls.get(i);
                totalInTokens += call.getInputTokens();
                totalOutTokens += call.getOutputTokens();
                totalCost += call.getEstimatedCostUsd();

                sb.append(String.format(Locale.ROOT, "  #%d [%s] Model: %s | Latency: %s ms | Tokens: %s in, %s out | Cost: %s\n",
                    i + 1,
                    call.getCapability() != null ? call.getCapability() : "CALL",
                    call.getModelName() != null ? call.getModelName() : "unknown",
                    NUMBER_FORMAT.format(call.getDurationMs()),
                    NUMBER_FORMAT.format(call.getInputTokens()),
                    NUMBER_FORMAT.format(call.getOutputTokens()),
                    COST_FORMAT.format(call.getEstimatedCostUsd())));
            }
            sb.append(String.format(Locale.ROOT, "  Total: %d calls | %s in tokens | %s out tokens | %s total cost\n\n",
                llmCalls.size(),
                NUMBER_FORMAT.format(totalInTokens),
                NUMBER_FORMAT.format(totalOutTokens),
                COST_FORMAT.format(totalCost)));
        }

        // Findings
        if (report.getLinterFindings() != null && !report.getLinterFindings().isEmpty())
        {
            sb.append("--- PLAYBOOK FINDINGS ---\n");
            for (final PlaybookLinterFinding finding : report.getLinterFindings())
            {
                sb.append(String.format(Locale.ROOT, "  [%s] %s: %s\n",
                    finding.severity(), finding.category(), finding.message()));
            }
            sb.append("\n");
        }

        sb.append("================================================================================\n");
        sb.append("End of Execution Log\n");
        sb.append("================================================================================\n");

        return sb.toString();
    }
}
