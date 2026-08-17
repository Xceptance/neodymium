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
import java.util.List;
import java.util.Locale;

/**
 * Report generator producing GitHub-flavored Markdown documents from {@link TestExecutionReport}.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class MarkdownReportGenerator
{
    private static final DecimalFormat COST_FORMAT = new DecimalFormat("$#,##0.0000", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.US));

    /**
     * Constructs a MarkdownReportGenerator.
     */
    public MarkdownReportGenerator()
    {
    }

    /**
     * Converts a test execution report to a Markdown string.
     *
     * @param report the test execution report
     * @return markdown formatted string
     */
    public String generate(final TestExecutionReport report)
    {
        if (report == null)
        {
            return "# Test Execution Report\n\nNo report data available.\n";
        }

        final StringBuilder sb = new StringBuilder();

        final String testTitle = report.getTestName() != null ? report.getTestName() : "AI Test Execution";
        final String statusEmoji = report.isSuccess() ? "✅" : "❌";
        final String statusBadge = report.isSuccess() ? "**PASSED**" : "**FAILED**";

        sb.append("# ").append(statusEmoji).append(" Test Execution Report: ").append(testTitle).append("\n\n");

        // 1. Metadata Table
        sb.append("## Overview\n\n");
        sb.append("| Property | Value |\n");
        sb.append("| :--- | :--- |\n");
        sb.append("| **Status** | ").append(statusBadge).append(" |\n");
        sb.append("| **Duration** | ").append(NUMBER_FORMAT.format(report.getDurationMs())).append(" ms |\n");
        if (report.getDatasetId() != null && !report.getDatasetId().isEmpty())
        {
            sb.append("| **Dataset** | `").append(report.getDatasetId()).append("` |\n");
        }
        if (report.getExecutionMode() != null)
        {
            sb.append("| **Execution Mode** | `").append(report.getExecutionMode()).append("` |\n");
        }
        if (report.getPlaybookFile() != null)
        {
            sb.append("| **Playbook File** | `").append(report.getPlaybookFile()).append("` |\n");
        }
        if (report.getTestClass() != null)
        {
            sb.append("| **Test Class** | `").append(report.getTestClass()).append("` |\n");
        }
        if (report.getTestMethod() != null)
        {
            sb.append("| **Test Method** | `").append(report.getTestMethod()).append("` |\n");
        }
        sb.append("\n");

        // 2. Metrics Summary
        final TestExecutionReport.ReportMetrics m = report.getMetrics() != null ? report.getMetrics() : new TestExecutionReport.ReportMetrics();
        sb.append("## AI Metrics & Token Usage\n\n");
        sb.append("| Metric | Count / Volume |\n");
        sb.append("| :--- | :--- |\n");
        sb.append("| **Total Steps** | ").append(m.getTotalSteps()).append(" (Healed: ").append(m.getHealedSteps()).append(", Failed: ").append(m.getFailedSteps()).append(", Skipped: ").append(m.getSkippedSteps()).append(") |\n");
        sb.append("| **Total LLM Calls** | ").append(m.getTotalLlmCalls()).append(" |\n");
        sb.append("| **Input Tokens** | ").append(NUMBER_FORMAT.format(m.getTokenUsageInput())).append(" |\n");
        sb.append("| **Output Tokens** | ").append(NUMBER_FORMAT.format(m.getTokenUsageOutput())).append(" |\n");
        sb.append("| **Cached Tokens** | ").append(NUMBER_FORMAT.format(m.getTokenUsageCached())).append(" |\n");
        sb.append("| **Total Tokens** | ").append(NUMBER_FORMAT.format(m.getTotalTokens())).append(" |\n");
        sb.append("| **Estimated Cost** | ").append(COST_FORMAT.format(m.getEstimatedCostUsd())).append(" |\n");
        if (m.getTotalReplays() > 0)
        {
            sb.append("| **Replayed Steps** | ").append(m.getTotalReplays()).append(" |\n");
        }
        if (m.getInternalCacheHits() > 0)
        {
            sb.append("| **Cache Hits** | ").append(m.getInternalCacheHits()).append(" |\n");
        }
        sb.append("\n");

        // 3. Diagnostics & Errors (if present)
        if (!report.isSuccess() || report.getFailureReason() != null || report.getVisualRcaExplanation() != null)
        {
            sb.append("## 🚨 Failure Diagnostics\n\n");
            if (report.getFailureReason() != null)
            {
                sb.append("> **Error**: ").append(report.getFailureReason()).append("\n\n");
            }
            if (report.getVisualRcaExplanation() != null)
            {
                sb.append("### Visual RCA Diagnosis\n\n");
                sb.append("```\n").append(report.getVisualRcaExplanation()).append("\n```\n\n");
            }
            if (report.getFailureStackTrace() != null && !report.getFailureStackTrace().isBlank())
            {
                sb.append("<details>\n<summary><b>Stack Trace</b></summary>\n\n");
                sb.append("```java\n").append(report.getFailureStackTrace()).append("\n```\n");
                sb.append("</details>\n\n");
            }
        }

        // Warnings
        if (!report.getWarnings().isEmpty())
        {
            sb.append("### ⚠️ Execution Warnings\n\n");
            for (final String warning : report.getWarnings())
            {
                sb.append("- ").append(warning).append("\n");
            }
            sb.append("\n");
        }

        // 4. Execution Steps
        final List<TestExecutionReport.ReportStepEntry> steps = report.getSteps();
        if (!steps.isEmpty())
        {
            sb.append("## Step Details\n\n");
            for (int i = 0; i < steps.size(); i++)
            {
                final TestExecutionReport.ReportStepEntry step = steps.get(i);
                final String stepStatusEmoji = "PASSED".equalsIgnoreCase(step.getStatus()) || "SUCCESS".equalsIgnoreCase(step.getStatus()) ? "✅"
                    : "HEALED".equalsIgnoreCase(step.getStatus()) ? "✨"
                    : "FAILED".equalsIgnoreCase(step.getStatus()) ? "❌"
                    : "SKIPPED".equalsIgnoreCase(step.getStatus()) ? "⏭️" : "⏳";

                sb.append("### Step ").append(i + 1).append(": ").append(stepStatusEmoji).append(" ").append(escapeMarkdown(step.getInstruction())).append("\n\n");
                sb.append("- **Status:** `").append(step.getStatus()).append("`\n");
                if (step.getDurationMs() > 0)
                {
                    sb.append("- **Duration:** ").append(step.getDurationMs()).append(" ms\n");
                }
                if (step.getSourceFile() != null && step.getLineNumber() > 0)
                {
                    sb.append("- **Source:** `").append(step.getSourceFile()).append(":").append(step.getLineNumber()).append("`\n");
                }
                if (step.getReasoning() != null && !step.getReasoning().isBlank())
                {
                    sb.append("- **AI Reasoning:** _").append(escapeMarkdown(step.getReasoning())).append("_\n");
                }
                if (step.getFailureReason() != null)
                {
                    sb.append("- **Failure Reason:** `").append(step.getFailureReason()).append("`\n");
                }

                // Actions Table
                if (!step.getActions().isEmpty())
                {
                    sb.append("\n**Executed Actions:**\n\n");
                    sb.append("| # | Type | Target Selector | Value | Result |\n");
                    sb.append("| :--- | :--- | :--- | :--- | :--- |\n");
                    for (int a = 0; a < step.getActions().size(); a++)
                    {
                        final TestExecutionReport.ReportActionEntry act = step.getActions().get(a);
                        final String actResult = act.isSuccess() ? "✅ Success" : "❌ Failed";
                        sb.append("| ").append(a + 1).append(" | `").append(act.getType() != null ? act.getType() : "").append("` | `")
                            .append(escapeMarkdown(act.getTarget() != null ? act.getTarget() : "-")).append("` | `")
                            .append(escapeMarkdown(act.getValue() != null ? act.getValue() : "-")).append("` | ")
                            .append(actResult).append(" |\n");
                    }
                }

                sb.append("\n");
            }
        }

        // 5. LLM Call Details
        final List<TestExecutionReport.ReportLlmCallEntry> llmCalls = report.getLlmCalls();
        if (!llmCalls.isEmpty())
        {
            sb.append("## LLM Interactions\n\n");
            sb.append("| # | Capability | Model | Duration | In Tokens | Out Tokens | Cached | Cost |\n");
            sb.append("| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |\n");
            for (int i = 0; i < llmCalls.size(); i++)
            {
                final TestExecutionReport.ReportLlmCallEntry call = llmCalls.get(i);
                sb.append("| ").append(i + 1).append(" | `").append(call.getCapability() != null ? call.getCapability() : "").append("` | ")
                    .append(call.getModelName() != null ? call.getModelName() : "default").append(" | ")
                    .append(call.getDurationMs()).append(" ms | ")
                    .append(NUMBER_FORMAT.format(call.getInputTokens())).append(" | ")
                    .append(NUMBER_FORMAT.format(call.getOutputTokens())).append(" | ")
                    .append(NUMBER_FORMAT.format(call.getCachedTokens())).append(" | ")
                    .append(COST_FORMAT.format(call.getEstimatedCostUsd())).append(" |\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private static String escapeMarkdown(final String text)
    {
        if (text == null)
        {
            return "";
        }
        return text.replace("|", "\\|").replace("\n", " ");
    }
}
