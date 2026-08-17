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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Report generator producing standalone, interactive, single-file HTML documents
 * from {@link TestExecutionReport} with embedded styles, SVG icons, and base64 screenshots.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class HtmlReportGenerator
{
    private static final DecimalFormat COST_FORMAT = new DecimalFormat("$#,##0.0000", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.systemDefault());

    /**
     * Constructs an HtmlReportGenerator.
     */
    public HtmlReportGenerator()
    {
    }

    /**
     * Converts a test execution report to a complete self-contained HTML report.
     *
     * @param report the test execution report
     * @return HTML document string
     */
    public String generate(final TestExecutionReport report)
    {
        if (report == null)
        {
            return "<html><body><h1>No Report Data</h1></body></html>";
        }

        final StringBuilder sb = new StringBuilder();
        final String testTitle = report.getTestName() != null ? report.getTestName() : "AI Test Execution";
        final boolean isPassed = report.isSuccess();
        final String statusClass = isPassed ? "status-pass" : "status-fail";
        final String statusLabel = isPassed ? "PASSED" : "FAILED";
        final TestExecutionReport.ReportMetrics m = report.getMetrics() != null ? report.getMetrics() : new TestExecutionReport.ReportMetrics();

        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        sb.append("<meta charset=\"UTF-8\">\n");
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("<title>AI Test Report: ").append(escapeHtml(testTitle)).append("</title>\n");
        sb.append("<style>\n");
        appendStyles(sb);
        sb.append("</style>\n</head>\n<body>\n");

        sb.append("<div class=\"report-container\">\n");

        // Top Navigation Bar / Brand Header
        sb.append("  <header class=\"report-header\">\n");
        sb.append("    <div class=\"brand-title\">\n");
        sb.append("      <span class=\"brand-icon\">⚡</span>\n");
        sb.append("      <div>\n");
        sb.append("        <h1>").append(escapeHtml(testTitle)).append("</h1>\n");
        sb.append("        <div class=\"test-meta-sub\">\n");
        if (report.getTestClass() != null)
        {
            sb.append("<span class=\"meta-badge\">Class: ").append(escapeHtml(report.getTestClass())).append("</span>");
        }
        if (report.getTestMethod() != null)
        {
            sb.append("<span class=\"meta-badge\">Method: ").append(escapeHtml(report.getTestMethod())).append("</span>");
        }
        if (report.getDatasetId() != null && !report.getDatasetId().isEmpty())
        {
            sb.append("<span class=\"meta-badge highlight\">Dataset: ").append(escapeHtml(report.getDatasetId())).append("</span>");
        }
        if (report.getExecutionMode() != null)
        {
            sb.append("<span class=\"meta-badge\">Mode: ").append(escapeHtml(report.getExecutionMode())).append("</span>");
        }
        if (report.getStartTimeMs() > 0)
        {
            sb.append("<span class=\"meta-badge\">").append(TIME_FORMATTER.format(Instant.ofEpochMilli(report.getStartTimeMs()))).append("</span>");
        }
        sb.append("        </div>\n");
        sb.append("      </div>\n");
        sb.append("    </div>\n");
        sb.append("    <div class=\"status-pill ").append(statusClass).append("\">").append(statusLabel).append("</div>\n");
        sb.append("  </header>\n");

        // Summary Metric Cards
        sb.append("  <section class=\"metrics-grid\">\n");

        // Card 1: Status & Duration
        sb.append("    <div class=\"metric-card\">\n");
        sb.append("      <div class=\"metric-label\">Duration</div>\n");
        sb.append("      <div class=\"metric-value\">").append(NUMBER_FORMAT.format(report.getDurationMs())).append("<span class=\"unit\">ms</span></div>\n");
        sb.append("      <div class=\"metric-sub\">Status: <strong class=\"").append(statusClass).append("\">").append(statusLabel).append("</strong></div>\n");
        sb.append("    </div>\n");

        // Card 2: Steps & Healing
        sb.append("    <div class=\"metric-card\">\n");
        sb.append("      <div class=\"metric-label\">Playbook Steps</div>\n");
        sb.append("      <div class=\"metric-value\">").append(m.getTotalSteps()).append("</div>\n");
        sb.append("      <div class=\"metric-sub\">✨ Healed: ").append(m.getHealedSteps()).append(" | ❌ Failed: ").append(m.getFailedSteps()).append("</div>\n");
        sb.append("    </div>\n");

        // Card 3: LLM Calls & Tokens
        sb.append("    <div class=\"metric-card\">\n");
        sb.append("      <div class=\"metric-label\">LLM Invocations</div>\n");
        sb.append("      <div class=\"metric-value\">").append(m.getTotalLlmCalls()).append("</div>\n");
        sb.append("      <div class=\"metric-sub\">Tokens: ").append(NUMBER_FORMAT.format(m.getTotalTokens())).append(" (Cached: ").append(NUMBER_FORMAT.format(m.getTokenUsageCached())).append(")</div>\n");
        sb.append("    </div>\n");

        // Card 4: Estimated Cost
        sb.append("    <div class=\"metric-card\">\n");
        sb.append("      <div class=\"metric-label\">Estimated Cost</div>\n");
        sb.append("      <div class=\"metric-value text-accent\">").append(COST_FORMAT.format(m.getEstimatedCostUsd())).append("</div>\n");
        sb.append("      <div class=\"metric-sub\">In: ").append(NUMBER_FORMAT.format(m.getTokenUsageInput())).append(" | Out: ").append(NUMBER_FORMAT.format(m.getTokenUsageOutput())).append("</div>\n");
        sb.append("    </div>\n");

        sb.append("  </section>\n");

        // Failure Diagnostic Box (if failed)
        if (!isPassed || report.getFailureReason() != null || report.getVisualRcaExplanation() != null)
        {
            sb.append("  <section class=\"diagnostic-box failure-box\">\n");
            sb.append("    <div class=\"box-header\">🚨 Execution Failure Details</div>\n");
            if (report.getFailureReason() != null)
            {
                sb.append("    <div class=\"failure-reason\"><strong>Error:</strong> ").append(escapeHtml(report.getFailureReason())).append("</div>\n");
            }
            if (report.getVisualRcaExplanation() != null)
            {
                sb.append("    <div class=\"visual-rca-box\">\n");
                sb.append("      <div class=\"rca-title\">🔍 Visual Root Cause Analysis (RCA)</div>\n");
                sb.append("      <div class=\"rca-content\">").append(escapeHtml(report.getVisualRcaExplanation())).append("</div>\n");
                sb.append("    </div>\n");
            }
            if (report.getFailureStackTrace() != null && !report.getFailureStackTrace().isBlank())
            {
                sb.append("    <details class=\"stacktrace-details\">\n");
                sb.append("      <summary>View Stack Trace</summary>\n");
                sb.append("      <pre class=\"code-block\">").append(escapeHtml(report.getFailureStackTrace())).append("</pre>\n");
                sb.append("    </details>\n");
            }
            sb.append("  </section>\n");
        }

        // Warnings Box (if any)
        if (!report.getWarnings().isEmpty())
        {
            sb.append("  <section class=\"diagnostic-box warning-box\">\n");
            sb.append("    <div class=\"box-header\">⚠️ Execution Warnings (").append(report.getWarnings().size()).append(")</div>\n");
            sb.append("    <ul class=\"warning-list\">\n");
            for (final String warning : report.getWarnings())
            {
                sb.append("      <li>").append(escapeHtml(warning)).append("</li>\n");
            }
            sb.append("    </ul>\n");
            sb.append("  </section>\n");
        }

        // Steps Execution Section
        final List<TestExecutionReport.ReportStepEntry> steps = report.getSteps();
        sb.append("  <section class=\"steps-section\">\n");
        sb.append("    <h2 class=\"section-title\">Execution Steps (").append(steps.size()).append(")</h2>\n");

        if (steps.isEmpty())
        {
            sb.append("    <div class=\"empty-state\">No step records captured for this execution.</div>\n");
        }
        else
        {
            sb.append("    <div class=\"steps-list\">\n");
            for (int i = 0; i < steps.size(); i++)
            {
                final TestExecutionReport.ReportStepEntry step = steps.get(i);
                final String stepStatus = step.getStatus() != null ? step.getStatus().toUpperCase() : "PENDING";
                final String stepPillClass = "PASSED".equals(stepStatus) || "SUCCESS".equals(stepStatus) ? "pill-pass"
                    : "HEALED".equals(stepStatus) ? "pill-heal"
                    : "FAILED".equals(stepStatus) ? "pill-fail"
                    : "SKIPPED".equals(stepStatus) ? "pill-skip" : "pill-pending";

                sb.append("      <div class=\"step-card\">\n");
                sb.append("        <div class=\"step-header\">\n");
                sb.append("          <div class=\"step-header-left\">\n");
                sb.append("            <span class=\"step-number\">#").append(i + 1).append("</span>\n");
                sb.append("            <span class=\"step-status-pill ").append(stepPillClass).append("\">").append(stepStatus).append("</span>\n");
                sb.append("            <span class=\"step-instruction\">").append(escapeHtml(step.getInstruction())).append("</span>\n");
                sb.append("          </div>\n");
                sb.append("          <div class=\"step-header-right\">\n");
                if (step.getDurationMs() > 0)
                {
                    sb.append("            <span class=\"step-duration\">").append(step.getDurationMs()).append(" ms</span>\n");
                }
                if (step.getSourceFile() != null && step.getLineNumber() > 0)
                {
                    sb.append("            <span class=\"step-source\">").append(escapeHtml(step.getSourceFile())).append(":").append(step.getLineNumber()).append("</span>\n");
                }
                sb.append("          </div>\n");
                sb.append("        </div>\n");

                // Step Body
                sb.append("        <div class=\"step-body\">\n");

                // AI Reasoning
                if (step.getReasoning() != null && !step.getReasoning().isBlank())
                {
                    sb.append("          <div class=\"step-reasoning\">\n");
                    sb.append("            <span class=\"reasoning-tag\">🧠 AI Reasoning:</span> ").append(escapeHtml(step.getReasoning())).append("\n");
                    sb.append("          </div>\n");
                }

                // Failure Reason
                if (step.getFailureReason() != null && !step.getFailureReason().isBlank())
                {
                    sb.append("          <div class=\"step-failure-tag\">❌ Failure Reason: ").append(escapeHtml(step.getFailureReason())).append("</div>\n");
                }

                // Actions Table
                if (!step.getActions().isEmpty())
                {
                    sb.append("          <div class=\"actions-container\">\n");
                    sb.append("            <div class=\"sub-title\">Executed Target Actions:</div>\n");
                    sb.append("            <table class=\"data-table\">\n");
                    sb.append("              <thead><tr><th>#</th><th>Action</th><th>Target Selector</th><th>Value</th><th>Reasoning / Note</th><th>Result</th></tr></thead>\n");
                    sb.append("              <tbody>\n");
                    for (int a = 0; a < step.getActions().size(); a++)
                    {
                        final TestExecutionReport.ReportActionEntry act = step.getActions().get(a);
                        final String actResClass = act.isSuccess() ? "status-pass" : "status-fail";
                        final String actResLabel = act.isSuccess() ? "SUCCESS" : "FAILED";
                        sb.append("                <tr>\n");
                        sb.append("                  <td>").append(a + 1).append("</td>\n");
                        sb.append("                  <td><span class=\"badge-action\">").append(escapeHtml(act.getType())).append("</span></td>\n");
                        sb.append("                  <td><code>").append(escapeHtml(act.getTarget() != null ? act.getTarget() : "-")).append("</code></td>\n");
                        sb.append("                  <td><code>").append(escapeHtml(act.getValue() != null ? act.getValue() : "-")).append("</code></td>\n");
                        sb.append("                  <td class=\"text-muted\">").append(escapeHtml(act.getReasoning() != null ? act.getReasoning() : (act.getDescription() != null ? act.getDescription() : "-"))).append("</td>\n");
                        sb.append("                  <td><span class=\"").append(actResClass).append("\">").append(actResLabel).append("</span></td>\n");
                        sb.append("                </tr>\n");
                    }
                    sb.append("              </tbody>\n");
                    sb.append("            </table>\n");
                    sb.append("          </div>\n");
                }

                sb.append("        </div>\n");
                sb.append("      </div>\n");
            }
            sb.append("    </div>\n");
        }
        sb.append("  </section>\n");

        // Screenshots Gallery
        final List<TestExecutionReport.ReportScreenshotEntry> screenshots = report.getScreenshots();
        if (!screenshots.isEmpty())
        {
            sb.append("  <section class=\"screenshots-section\">\n");
            sb.append("    <h2 class=\"section-title\">Captured Screenshots & Visual State (").append(screenshots.size()).append(")</h2>\n");
            sb.append("    <div class=\"screenshots-grid\">\n");
            for (int s = 0; s < screenshots.size(); s++)
            {
                final TestExecutionReport.ReportScreenshotEntry sc = screenshots.get(s);
                String dataSrc = sc.getBase64Data();
                if (dataSrc != null && !dataSrc.startsWith("data:"))
                {
                    dataSrc = "data:" + (sc.getMediaType() != null ? sc.getMediaType() : "image/png") + ";base64," + dataSrc;
                }
                sb.append("      <div class=\"screenshot-card\">\n");
                sb.append("        <div class=\"screenshot-header\">").append(escapeHtml(sc.getName() != null ? sc.getName() : "Screenshot #" + (s + 1))).append("</div>\n");
                if (dataSrc != null)
                {
                    sb.append("        <img src=\"").append(dataSrc).append("\" alt=\"Captured screenshot\" class=\"screenshot-img\" loading=\"lazy\" onclick=\"window.open(this.src)\" title=\"Click to view full size\" />\n");
                }
                sb.append("      </div>\n");
            }
            sb.append("    </div>\n");
            sb.append("  </section>\n");
        }

        // LLM Interactions Table
        final List<TestExecutionReport.ReportLlmCallEntry> llmCalls = report.getLlmCalls();
        if (!llmCalls.isEmpty())
        {
            sb.append("  <section class=\"llm-section\">\n");
            sb.append("    <h2 class=\"section-title\">LLM Call Audit & Interaction Details (").append(llmCalls.size()).append(")</h2>\n");
            sb.append("    <div class=\"table-container\">\n");
            sb.append("      <table class=\"data-table\">\n");
            sb.append("        <thead><tr><th>#</th><th>Capability</th><th>Model</th><th>Duration</th><th>Input Tokens</th><th>Output Tokens</th><th>Cached</th><th>Est. Cost</th></tr></thead>\n");
            sb.append("        <tbody>\n");
            for (int i = 0; i < llmCalls.size(); i++)
            {
                final TestExecutionReport.ReportLlmCallEntry call = llmCalls.get(i);
                sb.append("          <tr>\n");
                sb.append("            <td>").append(i + 1).append("</td>\n");
                sb.append("            <td><span class=\"badge-role\">").append(escapeHtml(call.getCapability())).append("</span></td>\n");
                sb.append("            <td>").append(escapeHtml(call.getModelName() != null ? call.getModelName() : "default")).append("</td>\n");
                sb.append("            <td>").append(call.getDurationMs()).append(" ms</td>\n");
                sb.append("            <td>").append(NUMBER_FORMAT.format(call.getInputTokens())).append("</td>\n");
                sb.append("            <td>").append(NUMBER_FORMAT.format(call.getOutputTokens())).append("</td>\n");
                sb.append("            <td>").append(NUMBER_FORMAT.format(call.getCachedTokens())).append("</td>\n");
                sb.append("            <td><strong class=\"text-accent\">").append(COST_FORMAT.format(call.getEstimatedCostUsd())).append("</strong></td>\n");
                sb.append("          </tr>\n");
            }
            sb.append("        </tbody>\n");
            sb.append("      </table>\n");
            sb.append("    </div>\n");
            sb.append("  </section>\n");
        }

        // Footer
        sb.append("  <footer class=\"report-footer\">\n");
        sb.append("    Generated by <strong>Neodymium Aura AI</strong> Preliminary Disk Report Listener &bull; Xceptance GmbH 2026\n");
        sb.append("  </footer>\n");

        sb.append("</div>\n");
        sb.append("</body>\n</html>\n");

        return sb.toString();
    }

    private static void appendStyles(final StringBuilder sb)
    {
        sb.append("""
            :root {
                --bg: #0f172a;
                --card-bg: #1e293b;
                --card-hover: #334155;
                --border: #334155;
                --text: #f8fafc;
                --text-muted: #94a3b8;
                --accent-primary: #38bdf8;
                --accent-success: #22c55e;
                --accent-danger: #ef4444;
                --accent-warning: #f59e0b;
                --accent-purple: #a855f7;
                --font-sans: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                --font-mono: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
            }
            * { box-sizing: border-box; margin: 0; padding: 0; }
            body {
                background-color: var(--bg);
                color: var(--text);
                font-family: var(--font-sans);
                line-height: 1.5;
                padding: 2rem 1rem;
            }
            .report-container {
                max-width: 1200px;
                margin: 0 auto;
                display: flex;
                flex-direction: column;
                gap: 1.5rem;
            }
            .report-header {
                background: var(--card-bg);
                border: 1px solid var(--border);
                border-radius: 12px;
                padding: 1.5rem 2rem;
                display: flex;
                justify-content: space-between;
                align-items: center;
                box-shadow: 0 4px 12px rgba(0,0,0,0.2);
            }
            .brand-title {
                display: flex;
                align-items: center;
                gap: 1rem;
            }
            .brand-icon {
                font-size: 2.2rem;
            }
            h1 {
                font-size: 1.5rem;
                font-weight: 700;
                color: var(--text);
            }
            .test-meta-sub {
                display: flex;
                flex-wrap: wrap;
                gap: 0.5rem;
                margin-top: 0.4rem;
            }
            .meta-badge {
                font-size: 0.75rem;
                background: #0f172a;
                color: var(--text-muted);
                padding: 0.2rem 0.6rem;
                border-radius: 6px;
                border: 1px solid var(--border);
                font-family: var(--font-mono);
            }
            .meta-badge.highlight {
                color: var(--accent-primary);
                border-color: var(--accent-primary);
            }
            .status-pill {
                font-size: 1rem;
                font-weight: 800;
                padding: 0.5rem 1.4rem;
                border-radius: 9999px;
                text-transform: uppercase;
                letter-spacing: 0.05em;
            }
            .status-pass { background: rgba(34, 197, 94, 0.15); color: var(--accent-success); border: 1px solid var(--accent-success); }
            .status-fail { background: rgba(239, 68, 68, 0.15); color: var(--accent-danger); border: 1px solid var(--accent-danger); }
            .metrics-grid {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
                gap: 1rem;
            }
            .metric-card {
                background: var(--card-bg);
                border: 1px solid var(--border);
                border-radius: 10px;
                padding: 1.25rem;
                display: flex;
                flex-direction: column;
                gap: 0.25rem;
            }
            .metric-label {
                font-size: 0.8rem;
                text-transform: uppercase;
                letter-spacing: 0.05em;
                color: var(--text-muted);
            }
            .metric-value {
                font-size: 1.8rem;
                font-weight: 700;
                color: var(--text);
            }
            .metric-value .unit {
                font-size: 0.9rem;
                color: var(--text-muted);
                margin-left: 0.25rem;
            }
            .metric-sub {
                font-size: 0.8rem;
                color: var(--text-muted);
                margin-top: 0.25rem;
            }
            .text-accent { color: var(--accent-primary) !important; }
            .diagnostic-box {
                border-radius: 10px;
                padding: 1.25rem;
                display: flex;
                flex-direction: column;
                gap: 0.75rem;
            }
            .failure-box {
                background: rgba(239, 68, 68, 0.1);
                border: 1px solid rgba(239, 68, 68, 0.4);
            }
            .warning-box {
                background: rgba(245, 158, 11, 0.1);
                border: 1px solid rgba(245, 158, 11, 0.4);
            }
            .box-header {
                font-size: 1rem;
                font-weight: 700;
                color: var(--accent-danger);
            }
            .warning-box .box-header { color: var(--accent-warning); }
            .failure-reason {
                font-size: 0.95rem;
                color: #fca5a5;
                font-family: var(--font-mono);
            }
            .visual-rca-box {
                background: #0f172a;
                border: 1px solid rgba(239, 68, 68, 0.3);
                border-radius: 8px;
                padding: 1rem;
            }
            .rca-title {
                font-weight: 600;
                color: #38bdf8;
                font-size: 0.9rem;
                margin-bottom: 0.4rem;
            }
            .rca-content {
                font-size: 0.88rem;
                color: #cbd5e1;
                white-space: pre-wrap;
            }
            .stacktrace-details summary {
                cursor: pointer;
                color: var(--text-muted);
                font-size: 0.85rem;
            }
            .code-block {
                background: #020617;
                border: 1px solid var(--border);
                border-radius: 6px;
                padding: 0.75rem;
                margin-top: 0.5rem;
                font-family: var(--font-mono);
                font-size: 0.8rem;
                color: #f1f5f9;
                overflow-x: auto;
            }
            .warning-list {
                padding-left: 1.25rem;
                font-size: 0.85rem;
                color: #fde68a;
            }
            .section-title {
                font-size: 1.25rem;
                font-weight: 600;
                margin-bottom: 1rem;
                color: var(--text);
            }
            .steps-list {
                display: flex;
                flex-direction: column;
                gap: 1rem;
            }
            .step-card {
                background: var(--card-bg);
                border: 1px solid var(--border);
                border-radius: 10px;
                overflow: hidden;
            }
            .step-header {
                background: #1e293b;
                padding: 0.9rem 1.25rem;
                display: flex;
                justify-content: space-between;
                align-items: center;
                border-bottom: 1px solid var(--border);
            }
            .step-header-left {
                display: flex;
                align-items: center;
                gap: 0.75rem;
                flex: 1;
            }
            .step-number {
                font-weight: 700;
                color: var(--accent-primary);
                font-family: var(--font-mono);
                font-size: 0.9rem;
            }
            .step-status-pill {
                font-size: 0.7rem;
                font-weight: 700;
                padding: 0.15rem 0.5rem;
                border-radius: 4px;
                text-transform: uppercase;
            }
            .pill-pass { background: rgba(34, 197, 94, 0.2); color: var(--accent-success); }
            .pill-heal { background: rgba(168, 85, 247, 0.2); color: var(--accent-purple); }
            .pill-fail { background: rgba(239, 68, 68, 0.2); color: var(--accent-danger); }
            .pill-skip { background: rgba(148, 163, 184, 0.2); color: var(--text-muted); }
            .pill-pending { background: rgba(245, 158, 11, 0.2); color: var(--accent-warning); }
            .step-instruction {
                font-weight: 600;
                font-size: 0.95rem;
                color: var(--text);
            }
            .step-header-right {
                display: flex;
                align-items: center;
                gap: 0.75rem;
                font-size: 0.8rem;
                color: var(--text-muted);
                font-family: var(--font-mono);
            }
            .step-body {
                padding: 1.25rem;
                display: flex;
                flex-direction: column;
                gap: 0.75rem;
            }
            .step-reasoning {
                background: #0f172a;
                border-left: 3px solid var(--accent-primary);
                padding: 0.6rem 0.9rem;
                border-radius: 0 6px 6px 0;
                font-size: 0.85rem;
                color: #cbd5e1;
            }
            .reasoning-tag {
                font-weight: 700;
                color: var(--accent-primary);
            }
            .step-failure-tag {
                background: rgba(239, 68, 68, 0.15);
                border-left: 3px solid var(--accent-danger);
                padding: 0.5rem 0.8rem;
                color: #fca5a5;
                font-size: 0.85rem;
                font-family: var(--font-mono);
            }
            .sub-title {
                font-size: 0.85rem;
                font-weight: 600;
                color: var(--text-muted);
                margin-bottom: 0.4rem;
            }
            .data-table {
                width: 100%;
                border-collapse: collapse;
                font-size: 0.85rem;
                text-align: left;
            }
            .data-table th {
                background: #0f172a;
                color: var(--text-muted);
                padding: 0.6rem 0.8rem;
                font-weight: 600;
                border-bottom: 1px solid var(--border);
            }
            .data-table td {
                padding: 0.6rem 0.8rem;
                border-bottom: 1px solid rgba(51, 65, 85, 0.5);
            }
            .data-table tr:last-child td { border-bottom: none; }
            .data-table tr:hover { background: rgba(51, 65, 85, 0.3); }
            .badge-action {
                background: rgba(56, 189, 248, 0.15);
                color: var(--accent-primary);
                padding: 0.15rem 0.45rem;
                border-radius: 4px;
                font-size: 0.75rem;
                font-weight: 600;
                font-family: var(--font-mono);
            }
            .badge-role {
                background: rgba(168, 85, 247, 0.15);
                color: var(--accent-purple);
                padding: 0.15rem 0.45rem;
                border-radius: 4px;
                font-size: 0.75rem;
                font-weight: 600;
                font-family: var(--font-mono);
            }
            .table-container {
                background: var(--card-bg);
                border: 1px solid var(--border);
                border-radius: 10px;
                overflow: hidden;
            }
            .screenshots-grid {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
                gap: 1rem;
            }
            .screenshot-card {
                background: var(--card-bg);
                border: 1px solid var(--border);
                border-radius: 8px;
                overflow: hidden;
            }
            .screenshot-header {
                background: #1e293b;
                padding: 0.5rem 0.8rem;
                font-size: 0.8rem;
                font-weight: 600;
                color: var(--text-muted);
                border-bottom: 1px solid var(--border);
            }
            .screenshot-img {
                width: 100%;
                height: auto;
                display: block;
                cursor: pointer;
                transition: transform 0.2s ease;
            }
            .screenshot-img:hover {
                opacity: 0.95;
            }
            .report-footer {
                text-align: center;
                font-size: 0.8rem;
                color: var(--text-muted);
                padding: 1.5rem 0;
                border-top: 1px solid var(--border);
                margin-top: 1rem;
            }
            .empty-state {
                background: var(--card-bg);
                border: 1px solid var(--border);
                border-radius: 8px;
                padding: 2rem;
                text-align: center;
                color: var(--text-muted);
            }
        """);
    }

    private static String escapeHtml(final String text)
    {
        if (text == null)
        {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
