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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.neodymium.ai.playbook.linter.LinterSeverity;
import org.neodymium.ai.playbook.linter.PlaybookLinterFinding;

/**
 * Report generator producing standalone, interactive, single-file HTML documents
 * with a Light Mode UI, draggable and resizable step inspector, runtime variable resolution,
 * plain-text escaped prompts, sub-step hierarchy visualization, and step control badges.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class HtmlReportGenerator
{
    private static final DecimalFormat COST_FORMAT = new DecimalFormat("$#,##0.0000", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.systemDefault());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Constructs an HtmlReportGenerator.
     */
    public HtmlReportGenerator()
    {
    }

    /**
     * Converts a test execution report to a complete self-contained HTML report with a Light Mode UI
     * and resizable interactive step inspector.
     *
     * @param report the test execution report
     * @return HTML document string
     */
    public String generate(final TestExecutionReport report)
    {
        if (report == null)
        {
            return "<!DOCTYPE html><html><body><h1>No Report Data Available</h1></body></html>";
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

        // 1. Top Navigation Bar / Brand Header
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

        // 2. Summary Metric Cards
        sb.append("  <section class=\"metrics-grid\">\n");

        sb.append("    <div class=\"metric-card\">\n");
        sb.append("      <div class=\"metric-label\">Duration</div>\n");
        sb.append("      <div class=\"metric-value\">").append(NUMBER_FORMAT.format(report.getDurationMs())).append("<span class=\"unit\">ms</span></div>\n");
        sb.append("      <div class=\"metric-sub\">Status: <strong class=\"").append(statusClass).append("\">").append(statusLabel).append("</strong></div>\n");
        sb.append("    </div>\n");

        sb.append("    <div class=\"metric-card\">\n");
        sb.append("      <div class=\"metric-label\">Playbook Steps</div>\n");
        sb.append("      <div class=\"metric-value\">").append(m.getTotalSteps()).append("</div>\n");
        sb.append("      <div class=\"metric-sub\">✨ Healed: ").append(m.getHealedSteps()).append(" | ❌ Failed: ").append(m.getFailedSteps()).append("</div>\n");
        sb.append("    </div>\n");

        sb.append("    <div class=\"metric-card\">\n");
        sb.append("      <div class=\"metric-label\">LLM Invocations</div>\n");
        sb.append("      <div class=\"metric-value\">").append(m.getTotalLlmCalls()).append("</div>\n");
        sb.append("      <div class=\"metric-sub\">Tokens: ").append(NUMBER_FORMAT.format(m.getTotalTokens())).append(" (Cached: ").append(NUMBER_FORMAT.format(m.getTokenUsageCached())).append(")</div>\n");
        sb.append("    </div>\n");

        sb.append("    <div class=\"metric-card\">\n");
        sb.append("      <div class=\"metric-label\">Estimated Cost</div>\n");
        sb.append("      <div class=\"metric-value text-accent\">").append(COST_FORMAT.format(m.getEstimatedCostUsd())).append("</div>\n");
        sb.append("      <div class=\"metric-sub\">In: ").append(NUMBER_FORMAT.format(m.getTokenUsageInput())).append(" | Out: ").append(NUMBER_FORMAT.format(m.getTokenUsageOutput())).append("</div>\n");
        sb.append("    </div>\n");

        sb.append("  </section>\n");

        // 3. LLM Responsibility Breakdown Table
        sb.append("  <section class=\"card-section\">\n");
        sb.append("    <h2 class=\"section-title\">🤖 AI LLM Responsibility & Token Accounting</h2>\n");
        sb.append("    <div class=\"table-container\">\n");
        sb.append("      <table class=\"data-table\">\n");
        sb.append("        <thead><tr><th>Category</th><th>Calls</th><th>Total Tokens</th><th>Input Tokens</th><th>Output Tokens</th><th>Cached Tokens</th><th>Est. Cost</th></tr></thead>\n");
        sb.append("        <tbody>\n");

        appendHtmlCategoryRow(sb, "<strong>Total</strong>", m.getTotal(), "row-total");
        appendHtmlCategoryRow(sb, "Playbook Pre-Flight Linter", m.getLinter(), "");
        appendHtmlCategoryRow(sb, "PESAP (Pre-Execution Semantic Anchor)", m.getPesap(), "");
        appendHtmlCategoryRow(sb, "Action (Standard Generation)", m.getAction(), "");
        appendHtmlCategoryRow(sb, "Self-Judging Validation", m.getJudge(), "");
        appendHtmlCategoryRow(sb, "Semantic Outcome Verification", m.getVerification(), "");
        appendHtmlCategoryRow(sb, "Visual Root Cause Analysis (RCA)", m.getVisualRca(), "");

        sb.append("        </tbody>\n");
        sb.append("      </table>\n");
        sb.append("    </div>\n");

        if (m.getTotalEscalations() > 0 || m.getTotalReplays() > 0 || m.getInternalCacheHits() > 0 || !m.getContextLevelCounts().isEmpty())
        {
            sb.append("    <div class=\"stats-sub-row\">\n");
            if (m.getTotalEscalations() > 0)
            {
                sb.append("      <div class=\"stats-tag\">⚡ Total Context Escalations: <strong>").append(m.getTotalEscalations()).append("</strong></div>\n");
            }
            if (m.getTotalReplays() > 0)
            {
                sb.append("      <div class=\"stats-tag\">🎟️ Replayed Steps: <strong>").append(m.getTotalReplays()).append("</strong></div>\n");
            }
            if (m.getInternalCacheHits() > 0)
            {
                sb.append("      <div class=\"stats-tag\">⚡ Prompt Cache Hits: <strong>").append(m.getInternalCacheHits()).append("</strong></div>\n");
            }
            for (final Map.Entry<String, Integer> entry : m.getContextLevelCounts().entrySet())
            {
                sb.append("      <div class=\"stats-tag level-tag\">").append(escapeHtml(entry.getKey())).append(": <strong>").append(entry.getValue()).append("</strong></div>\n");
            }
            sb.append("    </div>\n");
        }

        sb.append("  </section>\n");

        // 4. Failure Diagnostic Box (STRICTLY rendered only if the test actually failed)
        if (!isPassed && (report.getFailureReason() != null || report.getVisualRcaExplanation() != null || (report.getFailureStackTrace() != null && !report.getFailureStackTrace().isBlank())))
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

        // 5. Warnings Box (if any)
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

        // Pre-Flight Findings Box (if any)
        final List<PlaybookLinterFinding> linterFindings = report.getLinterFindings();
        if (!linterFindings.isEmpty())
        {
            sb.append("  <section class=\"diagnostic-box\" style=\"border-left: 4px solid #6366f1; background: var(--card-bg, #ffffff); margin-bottom: 24px; padding: 18px 24px; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.1);\">\n");
            sb.append("    <div class=\"box-header\" style=\"font-size: 1.15rem; font-weight: 700; margin-bottom: 14px; color: var(--text-primary, #1e293b); display: flex; align-items: center; gap: 8px;\">📋 Playbook Quality & Pre-Flight Findings (").append(linterFindings.size()).append(")</div>\n");
            sb.append("    <div class=\"table-container\">\n");
            sb.append("      <table class=\"data-table\">\n");
            sb.append("        <thead><tr><th>Line / Step</th><th>Category</th><th>Severity</th><th>Message & Suggested Rewrite</th></tr></thead>\n");
            sb.append("        <tbody>\n");
            for (final PlaybookLinterFinding f : linterFindings)
            {
                final String lineLabel = f.lineNumber() > 0 ? "L" + f.lineNumber() : "Step " + f.stepIndex();
                final String sevBadge = f.severity() == LinterSeverity.ERROR
                    ? "<span class=\"badge badge-error\" style=\"background:#ef4444;color:#fff;padding:2px 8px;border-radius:4px;font-size:0.75rem;font-weight:600;\">ERROR</span>"
                    : f.severity() == LinterSeverity.WARNING
                    ? "<span class=\"badge badge-warning\" style=\"background:#f59e0b;color:#fff;padding:2px 8px;border-radius:4px;font-size:0.75rem;font-weight:600;\">WARNING</span>"
                    : "<span class=\"badge badge-info\" style=\"background:#3b82f6;color:#fff;padding:2px 8px;border-radius:4px;font-size:0.75rem;font-weight:600;\">INFO</span>";
                final String cat = f.category() != null ? f.category().name() : "GENERAL";

                sb.append("        <tr>\n");
                sb.append("          <td><code>").append(escapeHtml(lineLabel)).append("</code></td>\n");
                sb.append("          <td><span style=\"font-weight:600;font-size:0.85rem;\">").append(escapeHtml(cat)).append("</span></td>\n");
                sb.append("          <td>").append(sevBadge).append("</td>\n");
                sb.append("          <td>\n");
                sb.append("            <div style=\"font-weight:600;margin-bottom:4px;\">").append(escapeHtml(f.message())).append("</div>\n");
                if (f.rawInstruction() != null && !f.rawInstruction().equals(f.resolvedInstruction()))
                {
                    sb.append("            <div style=\"font-size:0.8rem;color:var(--text-secondary,#64748b);\">Template: <code>").append(escapeHtml(f.rawInstruction())).append("</code></div>\n");
                    sb.append("            <div style=\"font-size:0.8rem;color:var(--text-secondary,#64748b);\">Resolved: <code>").append(escapeHtml(f.resolvedInstruction())).append("</code></div>\n");
                }
                else if (f.rawInstruction() != null && !f.rawInstruction().isBlank())
                {
                    sb.append("            <div style=\"font-size:0.8rem;color:var(--text-secondary,#64748b);\">Instruction: <code>").append(escapeHtml(f.rawInstruction())).append("</code></div>\n");
                }
                if (f.suggestedRewrite() != null && !f.suggestedRewrite().isBlank())
                {
                    sb.append("            <div style=\"margin-top:6px;font-size:0.85rem;background:rgba(99,102,241,0.06);padding:6px 10px;border-radius:4px;border-left:3px solid #6366f1;\">💡 <em>Suggested Rewrite:</em><br><code style=\"white-space:pre-wrap;\">").append(escapeHtml(f.suggestedRewrite())).append("</code></div>\n");
                }
                sb.append("          </td>\n");
                sb.append("        </tr>\n");
            }
            sb.append("        </tbody>\n");
            sb.append("      </table>\n");
            sb.append("    </div>\n");
            sb.append("  </section>\n");
        }

        // 6. Interactive Execution Steps Section with Resizable Split Inspector
        final List<TestExecutionReport.ReportStepEntry> steps = report.getSteps();
        sb.append("  <section class=\"steps-section\">\n");
        sb.append("    <div class=\"section-header-row\">\n");
        sb.append("      <h2 class=\"section-title\">Execution Steps (").append(steps.size()).append(")</h2>\n");
        sb.append("      <div class=\"section-header-actions\">\n");
        sb.append("        <button class=\"btn-toggle-inspector\" id=\"btnToggleInspector\" onclick=\"toggleInspector()\">🔍 Toggle Step Inspector</button>\n");
        sb.append("      </div>\n");
        sb.append("    </div>\n");

        if (steps.isEmpty())
        {
            sb.append("    <div class=\"empty-state\">No step records captured for this execution.</div>\n");
        }
        else
        {
            sb.append("    <div class=\"steps-split-layout\" id=\"stepsSplitLayout\">\n");

            // Left Column: Full-Width Step List
            sb.append("      <div class=\"steps-master-pane\" id=\"stepsMasterList\">\n");
            for (int i = 0; i < steps.size(); i++)
            {
                final TestExecutionReport.ReportStepEntry step = steps.get(i);
                renderStepCardWithSubSteps(sb, step, i);
            }
            sb.append("      </div>\n");

            // Draggable Vertical Splitter
            sb.append("      <div class=\"split-resizer\" id=\"splitResizer\" title=\"Drag to adjust inspector width\"></div>\n");

            // Right Column: Step Inspector (Collapsible & Resizable)
            sb.append("      <div class=\"steps-inspector-pane\" id=\"stepInspectorPane\">\n");
            sb.append("        <div class=\"inspector-card\">\n");
            sb.append("          <div class=\"inspector-header\" id=\"inspectorHeader\">\n");
            sb.append("            <div class=\"inspector-header-top\">\n");
            sb.append("              <div class=\"inspector-header-left\">\n");
            sb.append("                <span class=\"inspector-badge\" id=\"inspStepBadge\">Step #1</span>\n");
            sb.append("                <span class=\"step-status-pill\" id=\"inspStatusPill\">SUCCESS</span>\n");
            sb.append("                <span class=\"inspector-meta\" id=\"inspDuration\">0 ms</span>\n");
            sb.append("                <span class=\"context-badge\" id=\"inspContextBadge\" style=\"display:none;\"></span>\n");
            sb.append("                <span class=\"badge-flag visual-badge\" id=\"inspVisualBadge\" style=\"display:none;\">📸 VISUAL</span>\n");
            sb.append("                <span class=\"badge-flag bug-badge\" id=\"inspBugBadge\" style=\"display:none;\">🐛 BUG EXPECTED</span>\n");
            sb.append("                <span class=\"badge-flag intent-badge\" id=\"inspIntentBadge\" style=\"display:none;\"></span>\n");
            sb.append("                <span class=\"badge-flag verification-badge-pass\" id=\"inspVerificationBadge\" style=\"display:none;\"></span>\n");
            sb.append("              </div>\n");
            sb.append("              <div class=\"inspector-header-controls\">\n");
            sb.append("                <div class=\"width-presets\">\n");
            sb.append("                  <button class=\"btn-preset\" onclick=\"setInspectorWidth(520)\" title=\"Standard Width (520px)\">◧ 520px</button>\n");
            sb.append("                  <button class=\"btn-preset\" onclick=\"setInspectorWidth(750)\" title=\"Wide Width (750px)\">◨ 750px</button>\n");
            sb.append("                  <button class=\"btn-preset\" onclick=\"setInspectorWidth(950)\" title=\"Max Width (950px)\">◫ 950px</button>\n");
            sb.append("                </div>\n");
            sb.append("                <button class=\"btn-close-inspector\" onclick=\"closeInspector()\" title=\"Close Inspector\">✕</button>\n");
            sb.append("              </div>\n");
            sb.append("            </div>\n");
            sb.append("            <div class=\"inspector-instruction\" id=\"inspInstruction\">Select a step</div>\n");
            sb.append("            <div class=\"inspector-raw-template\" id=\"inspRawTemplate\" style=\"display:none;\"></div>\n");
            sb.append("            <div class=\"inspector-sub-meta\" id=\"inspSourceFile\"></div>\n");
            sb.append("            <div class=\"inspector-error-banner\" id=\"inspErrorBanner\" style=\"display:none;\"></div>\n");
            sb.append("          </div>\n");

            // Tab Navigation Bar
            sb.append("          <div class=\"inspector-tabs\">\n");
            sb.append("            <button class=\"tab-btn active\" id=\"tabBtn-llm\" onclick=\"switchInspectorTab('llm')\">🤖 LLM Details (<span id=\"tabLlmCount\">0</span>)</button>\n");
            sb.append("            <button class=\"tab-btn\" id=\"tabBtn-actions\" onclick=\"switchInspectorTab('actions')\">🎯 Actions (<span id=\"tabActionsCount\">0</span>)</button>\n");
            sb.append("            <button class=\"tab-btn\" id=\"tabBtn-visuals\" onclick=\"switchInspectorTab('visuals')\">📸 Visuals (<span id=\"tabVisualsCount\">0</span>)</button>\n");
            sb.append("            <button class=\"tab-btn\" id=\"tabBtn-verification\" onclick=\"switchInspectorTab('verification')\">🔍 Verification <span class=\"pill-verif-badge\" id=\"tabVerifBadge\" style=\"display:none;\"></span></button>\n");
            sb.append("            <button class=\"tab-btn\" id=\"tabBtn-reasoning\" onclick=\"switchInspectorTab('reasoning')\">🧠 AI Notes <span class=\"pill-error-count\" id=\"tabErrorBadge\" style=\"display:none;\">Error</span></button>\n");
            sb.append("          </div>\n");

            // Tab Content Panels
            sb.append("          <div class=\"inspector-content\">\n");
            sb.append("            <div class=\"tab-panel active\" id=\"panel-llm\"></div>\n");
            sb.append("            <div class=\"tab-panel\" id=\"panel-actions\"></div>\n");
            sb.append("            <div class=\"tab-panel\" id=\"panel-visuals\"></div>\n");
            sb.append("            <div class=\"tab-panel\" id=\"panel-verification\"></div>\n");
            sb.append("            <div class=\"tab-panel\" id=\"panel-reasoning\"></div>\n");
            sb.append("          </div>\n");

            sb.append("        </div>\n");
            sb.append("      </div>\n");

            sb.append("    </div>\n");
        }
        sb.append("  </section>\n");

        // 7. Global LLM Interactions Summary (if any)
        final List<TestExecutionReport.ReportLlmCallEntry> llmCalls = report.getLlmCalls();
        if (!llmCalls.isEmpty())
        {
            sb.append("  <section class=\"card-section\">\n");
            sb.append("    <h2 class=\"section-title\">💬 All LLM Invocations & Prompt Trace (").append(llmCalls.size()).append(")</h2>\n");
            sb.append("    <div class=\"table-container\">\n");
            sb.append("      <table class=\"data-table\">\n");
            sb.append("        <thead><tr><th>#</th><th>Step</th><th>Capability</th><th>Model</th><th>Duration</th><th>In Tokens</th><th>Out Tokens</th><th>Cached</th><th>Est. Cost</th></tr></thead>\n");
            sb.append("        <tbody>\n");
            for (int i = 0; i < llmCalls.size(); i++)
            {
                final TestExecutionReport.ReportLlmCallEntry call = llmCalls.get(i);
                sb.append("          <tr>\n");
                sb.append("            <td>").append(i + 1).append("</td>\n");
                sb.append("            <td><span class=\"step-ref\" onclick=\"openAndSelectStep(").append(call.getStepIndex()).append(")\">Step #").append(call.getStepIndex() + 1).append("</span></td>\n");
                sb.append("            <td><span class=\"badge-role\">").append(escapeHtml(call.getCapability() != null ? call.getCapability() : "-")).append("</span></td>\n");
                sb.append("            <td><code>").append(escapeHtml(call.getModelName() != null ? call.getModelName() : "default")).append("</code></td>\n");
                sb.append("            <td>").append(NUMBER_FORMAT.format(call.getDurationMs())).append(" ms</td>\n");
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

        // 8. Global Screenshots Gallery (if any)
        final List<TestExecutionReport.ReportScreenshotEntry> screenshots = report.getScreenshots();
        if (!screenshots.isEmpty())
        {
            sb.append("  <section class=\"card-section\">\n");
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
                final String scName = sc.getName() != null ? sc.getName() : "Screenshot #" + (s + 1);
                final String dims = sc.getDimensions();
                final Integer width = sc.getWidth();
                final Integer height = sc.getHeight();
                sb.append("      <div class=\"screenshot-card\">\n");
                sb.append("        <div class=\"screenshot-header\"><span>").append(escapeHtml(scName)).append("</span>");
                if (dims != null && !dims.isEmpty())
                {
                    sb.append("<span class=\"screenshot-dim-badge\">").append(escapeHtml(dims)).append("</span>");
                }
                sb.append("</div>\n");
                if (dataSrc != null)
                {
                    sb.append("        <img src=\"").append(dataSrc).append("\" alt=\"Captured screenshot\" class=\"screenshot-img\" loading=\"lazy\" onclick=\"openLightbox(this.src, '").append(escapeAttr(scName + (dims != null ? " (" + dims + ")" : ""))).append("')\" title=\"Click to view full size\" />\n");
                }
                if (width != null && height != null)
                {
                    sb.append("        <div class=\"screenshot-meta-bar\">")
                      .append("<span class=\"dim-pill\">Width: <strong>").append(width).append("px</strong></span>")
                      .append("<span class=\"dim-pill\">Height: <strong>").append(height).append("px</strong></span>")
                      .append("</div>\n");
                }
                sb.append("      </div>\n");
            }
            sb.append("    </div>\n");
            sb.append("  </section>\n");
        }

        // 9. Footer
        sb.append("  <footer class=\"report-footer\">\n");
        sb.append("    Generated by <strong>Neodymium Aura AI</strong> Preliminary Disk Report Listener &bull; Xceptance GmbH 2026\n");
        sb.append("  </footer>\n");

        sb.append("</div>\n");

        // 10. Interactive Screenshot Lightbox Modal
        sb.append("  <div class=\"lightbox-overlay\" id=\"reportLightbox\" onclick=\"handleLightboxBackdropClick(event)\">\n");
        sb.append("    <div class=\"lightbox-dialog\" role=\"dialog\" aria-modal=\"true\">\n");
        sb.append("      <div class=\"lightbox-header\">\n");
        sb.append("        <div class=\"lightbox-title\" id=\"lightboxTitle\">Screenshot Preview</div>\n");
        sb.append("        <div class=\"lightbox-controls\">\n");
        sb.append("          <a id=\"lightboxDownloadBtn\" class=\"lightbox-btn\" download=\"screenshot.png\" href=\"#\" title=\"Download Screenshot\">⬇️ Download</a>\n");
        sb.append("          <button class=\"lightbox-btn lightbox-close\" onclick=\"closeLightbox()\" title=\"Close (Esc)\">✕</button>\n");
        sb.append("        </div>\n");
        sb.append("      </div>\n");
        sb.append("      <div class=\"lightbox-body\">\n");
        sb.append("        <img id=\"lightboxImg\" class=\"lightbox-img\" src=\"\" alt=\"Full size preview\" />\n");
        sb.append("      </div>\n");
        sb.append("    </div>\n");
        sb.append("  </div>\n");

        // Embedded Step Data Island & Reactive Controller JS
        appendClientScript(sb, report);

        sb.append("</body>\n</html>\n");

        return sb.toString();
    }

    private static void appendHtmlCategoryRow(final StringBuilder sb, final String label, final TestExecutionReport.CategoryTokenUsage cat, final String rowClass)
    {
        final int calls = cat != null ? cat.getCalls() : 0;
        final long total = cat != null ? cat.getTotalTokens() : 0;
        final long in = cat != null ? cat.getInputTokens() : 0;
        final long out = cat != null ? cat.getOutputTokens() : 0;
        final long cached = cat != null ? cat.getCachedTokens() : 0;
        final double cost = cat != null ? cat.getEstimatedCostUsd() : 0.0;

        sb.append("          <tr class=\"").append(rowClass).append("\">\n");
        sb.append("            <td>").append(label).append("</td>\n");
        sb.append("            <td>").append(NUMBER_FORMAT.format(calls)).append("</td>\n");
        sb.append("            <td><strong>").append(NUMBER_FORMAT.format(total)).append("</strong></td>\n");
        sb.append("            <td>").append(NUMBER_FORMAT.format(in)).append("</td>\n");
        sb.append("            <td>").append(NUMBER_FORMAT.format(out)).append("</td>\n");
        sb.append("            <td>").append(NUMBER_FORMAT.format(cached)).append("</td>\n");
        sb.append("            <td><strong class=\"text-accent\">").append(COST_FORMAT.format(cost)).append("</strong></td>\n");
        sb.append("          </tr>\n");
    }

    private static void appendStepBadges(final StringBuilder sb, final TestExecutionReport.ReportStepEntry step)
    {
        if (step.isBug())
        {
            final String tooltip = step.getBugDetails() != null ? "Expected bug: " + escapeHtml(step.getBugDetails()) : "Expected bug";
            sb.append("              <span class=\"badge-flag bug-badge\" title=\"").append(tooltip).append("\">🐛 BUG EXPECTED</span>\n");
        }
        if (step.isVisual())
        {
            sb.append("              <span class=\"badge-flag visual-badge\">📸 VISUAL</span>\n");
        }
        if (step.getSemanticIntent() != null && !step.getSemanticIntent().isBlank())
        {
            sb.append("              <span class=\"badge-flag intent-badge\" title=\"Semantic Intent: ").append(escapeHtml(step.getSemanticIntent())).append("\">🎯 ").append(escapeHtml(step.getSemanticIntent())).append("</span>\n");
        }
        if (step.isOptional())
        {
            sb.append("              <span class=\"badge-flag optional-badge\">OPTIONAL</span>\n");
        }
        if (step.isNoHealing())
        {
            sb.append("              <span class=\"badge-flag\">NO-HEALING</span>\n");
        }
        if (step.isContinueOnError())
        {
            sb.append("              <span class=\"badge-flag\">CONTINUE-ON-ERROR</span>\n");
        }

        if (step.getContextLevels() != null && !step.getContextLevels().isBlank())
        {
            sb.append("              <span class=\"context-badge\">").append(escapeHtml(step.getContextLevels())).append("</span>\n");
        }
        if (step.getEscalations() > 0)
        {
            sb.append("              <span class=\"escalation-badge\">⚡ ").append(step.getEscalations()).append(" esc</span>\n");
        }
        if (step.getVerificationResult() != null)
        {
            final boolean pass = step.getVerificationResult().passed();
            final String badgeCls = pass ? "verification-badge-pass" : "verification-badge-fail";
            final String badgeLabel = pass ? "🔍 VERIFIED" : "⚠️ VERIF FAILED";
            final String summary = step.getVerificationResult().getOverallVerdict() != null && step.getVerificationResult().getOverallVerdict().summary() != null
                ? escapeHtml(step.getVerificationResult().getOverallVerdict().summary())
                : badgeLabel;
            sb.append("              <span class=\"badge-flag ").append(badgeCls).append("\" title=\"").append(summary).append("\">").append(badgeLabel).append("</span>\n");
        }
    }

    private static void appendScreenshotPreviews(final StringBuilder sb, final List<TestExecutionReport.ReportScreenshotEntry> screenshots, final String labelPrefix)
    {
        if (screenshots.isEmpty())
        {
            return;
        }

        sb.append("            <div class=\"step-card-screenshots-preview\">\n");
        for (int scIdx = 0; scIdx < Math.min(3, screenshots.size()); scIdx++)
        {
            final TestExecutionReport.ReportScreenshotEntry sc = screenshots.get(scIdx);
            final String dataSrc = sc.getBase64Data() != null && sc.getBase64Data().startsWith("data:")
                ? sc.getBase64Data()
                : "data:" + (sc.getMediaType() != null ? sc.getMediaType() : "image/png") + ";base64," + (sc.getBase64Data() != null ? sc.getBase64Data() : "");
            final String scName = sc.getName() != null ? sc.getName() : labelPrefix + " Screenshot";
            final String dims = sc.getDimensions();
            final String dimAnnotation = (dims != null ? " (" + dims + ")" : "");
            final String scTitle = scName + (sc.getWidth() != null && sc.getHeight() != null ? " (Width: " + sc.getWidth() + "px, Height: " + sc.getHeight() + "px)" : dimAnnotation);
            sb.append("              <img src=\"").append(dataSrc).append("\" alt=\"Step screenshot preview\" class=\"preview-thumb\" onclick=\"event.stopPropagation(); openLightbox(this.src, '").append(escapeAttr(scName + dimAnnotation)).append("')\" title=\"").append(escapeHtml(scTitle)).append(" - Click to expand\" loading=\"lazy\" />\n");
        }
        if (screenshots.size() > 3)
        {
            sb.append("              <span class=\"preview-more-badge\">+").append(screenshots.size() - 3).append(" more</span>\n");
        }
        sb.append("            </div>\n");
    }

    private static void appendFooterTags(final StringBuilder sb, final TestExecutionReport.ReportStepEntry step, final int parentIndex, final int subIndex, final boolean hasSubSteps)
    {
        final boolean isSub = subIndex >= 0;
        final String footerClass = isSub ? "sub-step-footer" : "step-card-footer";
        final int llmCount = !step.getLlmCalls().isEmpty() ? step.getLlmCalls().size() : (step.getPesapCalls() + step.getStandardCalls() + step.getVerificationCalls() + step.getRcaCalls());

        if (!step.getActions().isEmpty() || step.getPesapCalls() > 0 || step.getStandardCalls() > 0 || step.getVerificationCalls() > 0 || step.getRcaCalls() > 0 || hasSubSteps || !step.getLlmCalls().isEmpty() || !step.getScreenshots().isEmpty() || step.getSsimScore() != null || step.getVerificationResult() != null)
        {
            sb.append("          <div class=\"").append(footerClass).append("\">\n");
            if (step.getSsimScore() != null)
            {
                final double score = step.getSsimScore();
                final double min = step.getSsimMinScore() != null ? step.getSsimMinScore() : 0.99;
                final boolean pass = score >= min;
                sb.append("            <span class=\"footer-tag ").append(pass ? "ssim-pass" : "ssim-fail").append("\">🖼️ SSIM: ")
                    .append(String.format("%.4f", score)).append(" (Min: ").append(String.format("%.2f", min)).append(")</span>\n");
            }
            if (step.getVerificationResult() != null)
            {
                final boolean pass = step.getVerificationResult().passed();
                sb.append("            <span class=\"footer-tag ").append(pass ? "verif-pass" : "verif-fail")
                  .append("\" onclick=\"event.stopPropagation(); openAndSelectStep(").append(parentIndex).append(", ").append(subIndex).append(", 'verification')\">🔍 Verification: ").append(pass ? "PASSED" : "FAILED").append("</span>\n");
            }
            if (!step.getActions().isEmpty())
            {
                sb.append("            <span class=\"footer-tag\" onclick=\"event.stopPropagation(); openAndSelectStep(").append(parentIndex).append(", ").append(subIndex).append(", 'actions')\">🎯 ").append(step.getActions().size()).append(" action(s)</span>\n");
            }
            if (!step.getScreenshots().isEmpty())
            {
                final String firstDims = step.getScreenshots().get(0).getDimensions();
                final String dimText = firstDims != null ? " (" + firstDims + ")" : "";
                sb.append("            <span class=\"footer-tag highlight\" onclick=\"event.stopPropagation(); openAndSelectStep(").append(parentIndex).append(", ").append(subIndex).append(", 'visuals')\">📸 ").append(step.getScreenshots().size()).append(" screenshot(s)").append(dimText).append("</span>\n");
            }
            if (llmCount > 0)
            {
                sb.append("            <span class=\"footer-tag\" onclick=\"event.stopPropagation(); openAndSelectStep(").append(parentIndex).append(", ").append(subIndex).append(", 'llm')\">🤖 ").append(llmCount).append(" LLM call(s)</span>\n");
            }
            if (hasSubSteps)
            {
                sb.append("            <span class=\"footer-tag highlight\">✂️ Split into ").append(step.getSubSteps().size()).append(" sub-step(s)</span>\n");
            }
            sb.append("          </div>\n");
        }
    }

    private static void renderStepCardWithSubSteps(final StringBuilder sb, final TestExecutionReport.ReportStepEntry step, final int index)
    {
        final String stepStatus = step.getStatus() != null ? step.getStatus().toUpperCase() : "PENDING";
        final String stepPillClass = "PASSED".equals(stepStatus) || "SUCCESS".equals(stepStatus) ? "pill-pass"
            : "HEALED".equals(stepStatus) ? "pill-heal"
            : "FAILED".equals(stepStatus) ? "pill-fail"
            : "SKIPPED".equals(stepStatus) ? "pill-skip" : "pill-pending";

        final boolean hasSubSteps = !step.getSubSteps().isEmpty();

        sb.append("        <div class=\"step-card step-select-item\" id=\"step-item-").append(index).append("\">\n");
        sb.append("          <div class=\"step-header\">\n");
        sb.append("            <div class=\"step-header-left\">\n");
        sb.append("              <span class=\"step-number\">#").append(index + 1).append("</span>\n");
        sb.append("              <span class=\"step-status-pill ").append(stepPillClass).append("\">").append(stepStatus).append("</span>\n");
        appendStepBadges(sb, step);
        sb.append("            </div>\n");
        sb.append("            <div class=\"step-header-right\">\n");
        if (step.getDurationMs() > 0)
        {
            sb.append("              <span class=\"step-duration\">").append(NUMBER_FORMAT.format(step.getDurationMs())).append(" ms</span>\n");
        }
        sb.append("              <button class=\"btn-inspect-step\" onclick=\"openAndSelectStep(").append(index).append(", -1)\">🔍 Inspect</button>\n");
        sb.append("            </div>\n");
        sb.append("          </div>\n");

        sb.append("          <div class=\"step-body\" onclick=\"openAndSelectStep(").append(index).append(", -1)\">\n");
        sb.append("            <div class=\"step-instruction-full\">").append(escapeHtml(step.getInstruction())).append("</div>\n");
        if (step.getSourceFile() != null)
        {
            sb.append("            <div class=\"step-source-meta\">📄 ").append(escapeHtml(step.getSourceFile()))
              .append(step.getLineNumber() > 0 ? ":" + step.getLineNumber() : "").append("</div>\n");
        }
        if (step.getFailureReason() != null && !step.getFailureReason().isBlank())
        {
            sb.append("            <div class=\"step-card-error-banner\">❌ <strong>Error:</strong> ")
              .append(escapeHtml(step.getFailureReason())).append("</div>\n");
        }
        appendScreenshotPreviews(sb, step.getScreenshots(), "Step #" + (index + 1));
        sb.append("          </div>\n");

        // Render Action and LLM Summary tags for parent
        appendFooterTags(sb, step, index, -1, hasSubSteps);

        // Render Nested Sub-Steps Hierarchy if compound step was split
        if (hasSubSteps)
        {
            sb.append("          <div class=\"sub-steps-container\">\n");
            sb.append("            <div class=\"sub-steps-header\">✂️ JIT Compound Step Split:</div>\n");
            for (int s = 0; s < step.getSubSteps().size(); s++)
            {
                final TestExecutionReport.ReportStepEntry sub = step.getSubSteps().get(s);
                final String subStatus = sub.getStatus() != null ? sub.getStatus().toUpperCase() : "SUCCESS";
                final String subPillClass = "PASSED".equals(subStatus) || "SUCCESS".equals(subStatus) ? "pill-pass"
                    : "HEALED".equals(subStatus) ? "pill-heal"
                    : "FAILED".equals(subStatus) ? "pill-fail" : "pill-pending";

                sb.append("            <div class=\"sub-step-card\" id=\"substep-item-").append(index).append("-").append(s).append("\">\n");
                sb.append("              <div class=\"sub-step-header\">\n");
                sb.append("                <div class=\"sub-step-header-left\">\n");
                sb.append("                  <span class=\"sub-step-number\">#").append(index + 1).append(".").append(s + 1).append("</span>\n");
                sb.append("                  <span class=\"step-status-pill ").append(subPillClass).append("\">").append(subStatus).append("</span>\n");
                appendStepBadges(sb, sub);
                sb.append("                  <span class=\"sub-step-instruction\" onclick=\"openAndSelectStep(").append(index).append(", ").append(s).append(")\">").append(escapeHtml(sub.getInstruction())).append("</span>\n");
                sb.append("                </div>\n");
                sb.append("                <div class=\"sub-step-header-right\">\n");
                if (sub.getDurationMs() > 0)
                {
                    sb.append("                  <span class=\"sub-step-duration\">").append(NUMBER_FORMAT.format(sub.getDurationMs())).append(" ms</span>\n");
                }
                sb.append("                  <button class=\"btn-inspect-substep\" onclick=\"openAndSelectStep(").append(index).append(", ").append(s).append(")\">🔍 Inspect</button>\n");
                sb.append("                </div>\n");
                sb.append("              </div>\n");

                if (sub.getFailureReason() != null && !sub.getFailureReason().isBlank())
                {
                    sb.append("              <div class=\"step-card-error-banner\">❌ <strong>Error:</strong> ")
                      .append(escapeHtml(sub.getFailureReason())).append("</div>\n");
                }

                if (!sub.getScreenshots().isEmpty())
                {
                    sb.append("              <div class=\"sub-step-body\" onclick=\"openAndSelectStep(").append(index).append(", ").append(s).append(")\">\n");
                    appendScreenshotPreviews(sb, sub.getScreenshots(), "Step #" + (index + 1) + "." + (s + 1));
                    sb.append("              </div>\n");
                }

                appendFooterTags(sb, sub, index, s, false);
                sb.append("            </div>\n");
            }
            sb.append("          </div>\n");
        }

        sb.append("        </div>\n");
    }

    private static void appendClientScript(final StringBuilder sb, final TestExecutionReport report)
    {
        sb.append("<script id=\"stepDataPayload\" type=\"application/json\">\n");
        try
        {
            sb.append(OBJECT_MAPPER.writeValueAsString(report.getSteps()));
        }
        catch (final Exception e)
        {
            sb.append("[]");
        }
        sb.append("\n</script>\n");

        sb.append("""
        <script>
        (function() {
            var rawJson = document.getElementById('stepDataPayload').textContent;
            var steps = [];
            try {
                steps = JSON.parse(rawJson) || [];
            } catch(e) {
                console.error("Failed to parse steps payload", e);
            }

            var currentParentIdx = 0;
            var currentSubIdx = -1;
            var currentTab = 'llm';
            var isInspectorOpen = false;
            var currentInspectorWidth = 520;

            function formatNumber(n) {
                if (n == null) return "0";
                return n.toString().replace(/\\B(?=(\\d{3})+(?!\\d))/g, ",");
            }

            function formatCost(c) {
                if (c == null) return "$0.0000";
                return "$" + Number(c).toFixed(4);
            }

            function escapeHtml(str) {
                if (str == null) return '';
                return String(str)
                    .replace(/&/g, '&amp;')
                    .replace(/</g, '&lt;')
                    .replace(/>/g, '&gt;')
                    .replace(/"/g, '&quot;')
                    .replace(/'/g, '&#39;');
            }

            window.setInspectorWidth = function(widthPx) {
                currentInspectorWidth = Math.max(380, Math.min(1200, widthPx));
                var layout = document.getElementById('stepsSplitLayout');
                if (layout && layout.classList.contains('inspector-open')) {
                    layout.style.gridTemplateColumns = '1fr 8px ' + currentInspectorWidth + 'px';
                }
            };

            window.toggleInspector = function() {
                if (isInspectorOpen) {
                    window.closeInspector();
                } else {
                    window.openInspector();
                }
            };

            window.openInspector = function() {
                isInspectorOpen = true;
                var layout = document.getElementById('stepsSplitLayout');
                if (layout) {
                    layout.classList.add('inspector-open');
                    layout.style.gridTemplateColumns = '1fr 8px ' + currentInspectorWidth + 'px';
                }
                var btn = document.getElementById('btnToggleInspector');
                if (btn) btn.classList.add('active');
            };

            window.closeInspector = function() {
                isInspectorOpen = false;
                var layout = document.getElementById('stepsSplitLayout');
                if (layout) {
                    layout.classList.remove('inspector-open');
                    layout.style.gridTemplateColumns = '1fr';
                }
                var btn = document.getElementById('btnToggleInspector');
                if (btn) btn.classList.remove('active');
                document.querySelectorAll('.step-select-item').forEach(function(item) {
                    item.classList.remove('active');
                });
                document.querySelectorAll('.sub-step-card').forEach(function(item) {
                    item.classList.remove('active');
                });
            };

            // Draggable splitter initialization
            var resizer = document.getElementById('splitResizer');
            if (resizer) {
                var isDragging = false;
                resizer.addEventListener('mousedown', function(e) {
                    isDragging = true;
                    document.body.style.cursor = 'col-resize';
                    document.body.style.userSelect = 'none';
                });

                document.addEventListener('mousemove', function(e) {
                    if (!isDragging) return;
                    var container = document.getElementById('stepsSplitLayout');
                    if (!container) return;
                    var containerRect = container.getBoundingClientRect();
                    var newWidth = containerRect.right - e.clientX;
                    window.setInspectorWidth(newWidth);
                });

                document.addEventListener('mouseup', function(e) {
                    if (isDragging) {
                        isDragging = false;
                        document.body.style.cursor = '';
                        document.body.style.userSelect = '';
                    }
                });
            }

            window.switchInspectorTab = function(tabId) {
                currentTab = tabId;
                var tabBtns = document.querySelectorAll('.inspector-tabs .tab-btn');
                tabBtns.forEach(function(btn) { btn.classList.remove('active'); });

                var targetBtn = document.getElementById('tabBtn-' + tabId);
                if (targetBtn) targetBtn.classList.add('active');

                var panels = document.querySelectorAll('.inspector-content .tab-panel');
                panels.forEach(function(p) { p.classList.remove('active'); });

                var targetPanel = document.getElementById('panel-' + tabId);
                if (targetPanel) targetPanel.classList.add('active');
            };

            window.copyLlmField = function(callIndex, fieldName, btn) {
                var step = getActiveStepObject();
                if (!step || !step.llmCalls || !step.llmCalls[callIndex]) return;
                var val = step.llmCalls[callIndex][fieldName] || '';
                if (navigator.clipboard) {
                    navigator.clipboard.writeText(val).then(function() {
                        var original = btn.textContent;
                        btn.textContent = "✓ Copied";
                        setTimeout(function() { btn.textContent = original; }, 1500);
                    });
                }
            };

            window.copyActionTarget = function(actionIndex, btn) {
                var step = getActiveStepObject();
                if (!step || !step.actions || !step.actions[actionIndex]) return;
                var act = step.actions[actionIndex];
                var val = act.resolvedTarget || act.target || '';
                if (navigator.clipboard) {
                    navigator.clipboard.writeText(val).then(function() {
                        var original = btn.textContent;
                        btn.textContent = "✓ Copied";
                        setTimeout(function() { btn.textContent = original; }, 1500);
                    });
                }
            };

            function getActiveStepObject() {
                if (currentParentIdx < 0 || currentParentIdx >= steps.length) return null;
                var parent = steps[currentParentIdx];
                if (currentSubIdx >= 0 && parent.subSteps && currentSubIdx < parent.subSteps.length) {
                    return parent.subSteps[currentSubIdx];
                }
                return parent;
            }

            window.openAndSelectStep = function(parentIndex, subIndex, preferredTab) {
                window.openInspector();
                window.selectStep(parentIndex, subIndex, preferredTab);
            };

            window.selectStep = function(parentIndex, subIndex, preferredTab) {
                if (parentIndex < 0 || parentIndex >= steps.length) return;
                currentParentIdx = parentIndex;
                currentSubIdx = (subIndex != null ? subIndex : -1);

                // Highlight active step item
                document.querySelectorAll('.step-select-item').forEach(function(item, idx) {
                    if (idx === parentIndex && currentSubIdx < 0) {
                        item.classList.add('active');
                    } else {
                        item.classList.remove('active');
                    }
                });

                document.querySelectorAll('.sub-step-card').forEach(function(item) {
                    item.classList.remove('active');
                });
                if (currentSubIdx >= 0) {
                    var subEl = document.getElementById('substep-item-' + parentIndex + '-' + currentSubIdx);
                    if (subEl) subEl.classList.add('active');
                }

                var step = getActiveStepObject();
                if (!step) return;

                // Update Header
                var badgeLabel = currentSubIdx >= 0 ? ('Step #' + (parentIndex + 1) + '.' + (currentSubIdx + 1)) : ('Step #' + (parentIndex + 1));
                document.getElementById('inspStepBadge').textContent = badgeLabel;

                var statusPill = document.getElementById('inspStatusPill');
                var status = (step.status || 'PENDING').toUpperCase();
                statusPill.textContent = status;
                statusPill.className = 'step-status-pill ' + (
                    status === 'SUCCESS' || status === 'PASSED' ? 'pill-pass' :
                    status === 'HEALED' ? 'pill-heal' :
                    status === 'FAILED' ? 'pill-fail' :
                    status === 'SKIPPED' ? 'pill-skip' : 'pill-pending'
                );

                document.getElementById('inspDuration').textContent = formatNumber(step.durationMs || 0) + ' ms';
                document.getElementById('inspInstruction').textContent = step.instruction || 'No instruction';

                var rawTpl = document.getElementById('inspRawTemplate');
                if (step.rawInstruction && step.rawInstruction !== step.instruction) {
                    rawTpl.textContent = 'Template: ' + step.rawInstruction;
                    rawTpl.style.display = 'block';
                } else {
                    rawTpl.style.display = 'none';
                }

                var bugBadge = document.getElementById('inspBugBadge');
                if (step.bug) {
                    bugBadge.style.display = 'inline-block';
                    bugBadge.textContent = step.bugDetails ? ('🐛 BUG: ' + step.bugDetails) : '🐛 BUG EXPECTED';
                } else {
                    bugBadge.style.display = 'none';
                }

                var visBadge = document.getElementById('inspVisualBadge');
                if (step.visual) {
                    visBadge.style.display = 'inline-block';
                } else {
                    visBadge.style.display = 'none';
                }

                var intentBadge = document.getElementById('inspIntentBadge');
                if (step.semanticIntent) {
                    intentBadge.style.display = 'inline-block';
                    intentBadge.textContent = '🎯 ' + step.semanticIntent;
                    intentBadge.title = 'Semantic Intent: ' + step.semanticIntent;
                } else {
                    intentBadge.style.display = 'none';
                }

                var verifBadge = document.getElementById('inspVerificationBadge');
                var tabVerifBadge = document.getElementById('tabVerifBadge');
                if (step.verificationResult) {
                    var vPass = step.verificationResult.passed !== undefined ? step.verificationResult.passed : (step.verificationResult.overallVerdict ? step.verificationResult.overallVerdict.passed : false);
                    verifBadge.style.display = 'inline-block';
                    verifBadge.textContent = vPass ? '🔍 VERIFIED' : '⚠️ VERIF FAILED';
                    verifBadge.className = 'badge-flag ' + (vPass ? 'verification-badge-pass' : 'verification-badge-fail');

                    tabVerifBadge.style.display = 'inline-block';
                    tabVerifBadge.textContent = vPass ? 'PASS' : 'FAIL';
                    tabVerifBadge.className = 'pill-verif-badge ' + (vPass ? 'pill-pass' : 'pill-fail');
                } else {
                    verifBadge.style.display = 'none';
                    tabVerifBadge.style.display = 'none';
                }

                var srcEl = document.getElementById('inspSourceFile');
                if (step.sourceFile) {
                    srcEl.textContent = '📄 ' + step.sourceFile + (step.lineNumber ? ':' + step.lineNumber : '');
                    srcEl.style.display = 'block';
                } else {
                    srcEl.style.display = 'none';
                }

                var ctxBadge = document.getElementById('inspContextBadge');
                if (step.contextLevels) {
                    ctxBadge.textContent = step.contextLevels + (step.escalations ? ' (⚡ ' + step.escalations + ' esc)' : '');
                    ctxBadge.style.display = 'inline-block';
                } else {
                    ctxBadge.style.display = 'none';
                }

                var errEl = document.getElementById('inspErrorBanner');
                if (step.failureReason) {
                    errEl.innerHTML = '❌ <strong>Error:</strong> <span>' + escapeHtml(step.failureReason) + '</span>';
                    errEl.style.display = 'block';
                } else {
                    errEl.style.display = 'none';
                }

                var errBadge = document.getElementById('tabErrorBadge');
                if (errBadge) {
                    errBadge.style.display = (step.failureReason && !step.bug) ? 'inline-block' : 'none';
                }

                // Update Tab Counts
                var llmCalls = step.llmCalls || [];
                var actions = step.actions || [];
                var visuals = step.screenshots || [];

                document.getElementById('tabLlmCount').textContent = llmCalls.length;
                document.getElementById('tabActionsCount').textContent = actions.length;
                document.getElementById('tabVisualsCount').textContent = visuals.length;

                // Tab Auto-Selection
                if (preferredTab) {
                    window.switchInspectorTab(preferredTab);
                } else if (step.verificationResult && !step.verificationResult.passed) {
                    window.switchInspectorTab('verification');
                } else if (step.failureReason && llmCalls.length === 0) {
                    window.switchInspectorTab('reasoning');
                } else if (step.visual || (visuals.length > 0 && llmCalls.length === 0)) {
                    window.switchInspectorTab('visuals');
                } else {
                    window.switchInspectorTab('llm');
                }

                // 1. Render LLM Panel (Safe Text Rendering via DOM textContent)
                var llmPanel = document.getElementById('panel-llm');
                llmPanel.innerHTML = '';
                if (llmCalls.length === 0) {
                    var emptyDiv = document.createElement('div');
                    emptyDiv.className = 'empty-inspector-state';
                    emptyDiv.textContent = 'No direct LLM completion calls recorded for this step (replayed or deterministic).';
                    llmPanel.appendChild(emptyDiv);
                } else {
                    llmCalls.forEach(function(call, ci) {
                        var card = document.createElement('div');
                        card.className = 'llm-call-card';

                        var header = document.createElement('div');
                        header.className = 'llm-call-header';
                        header.innerHTML = '<div class="llm-call-title">Call #' + (ci + 1) + ' &bull; <code>' + (call.modelName || 'default') + '</code> (' + (call.capability || 'TEXT') + ')</div>' +
                                           '<div class="llm-call-meta">' + formatNumber(call.durationMs || 0) + ' ms | ' + formatNumber(call.totalTokens || 0) + ' tokens (' + formatCost(call.estimatedCostUsd) + ')</div>';
                        card.appendChild(header);

                        if (call.systemPrompt) {
                            var sec = createPromptSection('System Prompt:', call.systemPrompt, ci, 'systemPrompt');
                            card.appendChild(sec);
                        }

                        if (call.userPrompt) {
                            var sec = createPromptSection('User Prompt & DOM Context (Plain Text):', call.userPrompt, ci, 'userPrompt');
                            card.appendChild(sec);
                        }

                        if (call.responseContent) {
                            var sec = createPromptSection('Raw Model Response:', call.responseContent, ci, 'responseContent');
                            sec.classList.add('response-section');
                            card.appendChild(sec);
                        }

                        llmPanel.appendChild(card);
                    });
                }

                // 2. Render Actions Panel
                var actionsPanel = document.getElementById('panel-actions');
                actionsPanel.innerHTML = '';
                if (actions.length === 0) {
                    var emptyDiv = document.createElement('div');
                    emptyDiv.className = 'empty-inspector-state';
                    emptyDiv.textContent = 'No target DOM actions executed in this step.';
                    actionsPanel.appendChild(emptyDiv);
                } else {
                    var tableWrapper = document.createElement('div');
                    tableWrapper.className = 'table-container';
                    var table = document.createElement('table');
                    table.className = 'data-table actions-table';
                    table.innerHTML = '<thead><tr><th>#</th><th>Action</th><th>Target Selector</th><th>Value</th><th>Reasoning</th><th>Result</th></tr></thead>';
                    var tbody = document.createElement('tbody');
                    actions.forEach(function(a, ai) {
                        var tr = document.createElement('tr');
                        var resClass = a.success ? 'status-pass' : 'status-fail';
                        var resText = a.success ? 'SUCCESS' : 'FAILED';

                        var displayTarget = a.resolvedTarget || a.target || '-';
                        var hasTargetTpl = a.target && a.resolvedTarget && a.target !== a.resolvedTarget;
                        var targetHtml = '<code class="code-selector" onclick="copyActionTarget(' + ai + ', this)" title="Click to copy">' + escapeHtml(displayTarget) + '</code>';
                        if (hasTargetTpl) {
                            targetHtml += '<div class="action-tpl-note" title="Original Parameterized Template">Template: <code>' + escapeHtml(a.target) + '</code></div>';
                        }

                        var displayValue = a.resolvedValue || a.value || '-';
                        var hasValueTpl = a.value && a.resolvedValue && a.value !== a.resolvedValue;
                        var valueHtml = '<code>' + escapeHtml(displayValue) + '</code>';
                        if (hasValueTpl) {
                            valueHtml += '<div class="action-tpl-note" title="Original Parameterized Template">Template: <code>' + escapeHtml(a.value) + '</code></div>';
                        }

                        tr.innerHTML = '<td>' + (ai + 1) + '</td>' +
                                       '<td><span class="badge-action">' + escapeHtml(a.type || '-') + '</span></td>' +
                                       '<td>' + targetHtml + '</td>' +
                                       '<td>' + valueHtml + '</td>' +
                                       '<td class="text-muted">' + escapeHtml(a.reasoning || a.description || '-') + '</td>' +
                                       '<td><span class="' + resClass + '">' + resText + '</span></td>';
                        tbody.appendChild(tr);
                    });
                    table.appendChild(tbody);
                    tableWrapper.appendChild(table);
                    actionsPanel.appendChild(tableWrapper);
                }

                // 3. Render Visuals Panel
                var visualsPanel = document.getElementById('panel-visuals');
                visualsPanel.innerHTML = '';

                // Render SSIM Baseline vs Replay comparison card if visual hashing occurred
                if (step.ssimScore !== undefined && step.ssimScore !== null) {
                    var ssimBox = document.createElement('div');
                    ssimBox.className = 'ssim-comparison-box';
                    var ssimPass = step.ssimScore >= (step.ssimMinScore || 0.99);
                    var ssimDim = step.screenshotHashDim || 128;

                    ssimBox.innerHTML = '<div class="ssim-score-header">' +
                        '<span class="badge ' + (ssimPass ? 'pill-pass' : 'pill-fail') + '">' +
                        '🖼️ SSIM Score: ' + Number(step.ssimScore).toFixed(4) + ' (Min: ' + Number(step.ssimMinScore || 0.99).toFixed(2) + ')' +
                        '</span>' +
                        '</div>' +
                        '<div class="ssim-matrices-grid">' +
                        (step.baselineMatrixPng ? 
                            '<div class="ssim-matrix-card">' +
                            '<div class="ssim-matrix-label">Recorded Baseline (' + ssimDim + 'x' + ssimDim + ')</div>' +
                            '<img src="' + step.baselineMatrixPng + '" class="ssim-matrix-img" alt="Baseline Matrix" title="Baseline SSIM Luminance Matrix (' + ssimDim + 'x' + ssimDim + ')" />' +
                            '</div>' : '') +
                        (step.replayMatrixPng ? 
                            '<div class="ssim-matrix-card">' +
                            '<div class="ssim-matrix-label">Replay Capture (' + ssimDim + 'x' + ssimDim + ')</div>' +
                            '<img src="' + step.replayMatrixPng + '" class="ssim-matrix-img" alt="Replay Matrix" title="Replay SSIM Luminance Matrix (' + ssimDim + 'x' + ssimDim + ')" />' +
                            '</div>' : '') +
                        '</div>';
                    visualsPanel.appendChild(ssimBox);
                }

                if (visuals.length === 0 && (step.ssimScore === undefined || step.ssimScore === null)) {
                    var emptyDiv = document.createElement('div');
                    emptyDiv.className = 'empty-inspector-state';
                    emptyDiv.textContent = 'No state screenshots captured during this step.';
                    visualsPanel.appendChild(emptyDiv);
                } else if (visuals.length > 0) {
                    var grid = document.createElement('div');
                    grid.className = 'screenshots-grid';
                    visuals.forEach(function(sc, si) {
                        var card = document.createElement('div');
                        card.className = 'screenshot-card';

                        var header = document.createElement('div');
                        header.className = 'screenshot-header';
                        var nameSpan = document.createElement('span');
                        nameSpan.textContent = sc.name || ('Screenshot #' + (si + 1));
                        header.appendChild(nameSpan);
                        var dims = sc.dimensions || (sc.width && sc.height ? (sc.width + 'x' + sc.height + ' px') : '');
                        if (dims) {
                            var dimBadge = document.createElement('span');
                            dimBadge.className = 'screenshot-dim-badge';
                            dimBadge.textContent = dims;
                            header.appendChild(dimBadge);
                        }
                        card.appendChild(header);

                        var src = sc.base64Data || '';
                        if (src && !src.startsWith('data:')) {
                            src = 'data:' + (sc.mediaType || 'image/png') + ';base64,' + src;
                        }
                        var img = document.createElement('img');
                        img.src = src;
                        img.className = 'screenshot-img';
                        img.title = 'Click to expand';
                        var scLabel = (sc.name || ('Screenshot #' + (si + 1))) + (dims ? ' (' + dims + ')' : '');
                        img.onclick = function() { window.openLightbox(this.src, scLabel); };
                        card.appendChild(img);

                        if (sc.width && sc.height) {
                            var metaBar = document.createElement('div');
                            metaBar.className = 'screenshot-meta-bar';
                            metaBar.innerHTML = '<span class="dim-pill">Width: <strong>' + sc.width + 'px</strong></span>' +
                                                '<span class="dim-pill">Height: <strong>' + sc.height + 'px</strong></span>';
                            card.appendChild(metaBar);
                        }
                        grid.appendChild(card);
                    });
                    visualsPanel.appendChild(grid);
                }

                // 4. Render Verification Panel
                var verifPanel = document.getElementById('panel-verification');
                verifPanel.innerHTML = '';
                if (!step.verificationResult) {
                    var emptyDiv = document.createElement('div');
                    emptyDiv.className = 'empty-inspector-state';
                    emptyDiv.textContent = 'No semantic outcome verification recorded for this step.';
                    verifPanel.appendChild(emptyDiv);
                } else {
                    var vr = step.verificationResult;
                    var vPass = vr.passed !== undefined ? vr.passed : (vr.overallVerdict ? vr.overallVerdict.passed : false);
                    var vCard = document.createElement('div');
                    vCard.className = 'verification-card';

                    var vHeader = document.createElement('div');
                    vHeader.className = 'verification-header';
                    vHeader.innerHTML = '<div class=\"verification-title\"><span>🔍 Semantic Outcome Verification</span></div>' +
                                        '<span class=\"step-status-pill ' + (vPass ? 'pill-pass' : 'pill-fail') + '\">' + (vPass ? 'VERDICT: PASSED' : 'VERDICT: FAILED') + '</span>';
                    vCard.appendChild(vHeader);

                    var ovSummary = (vr.overallVerdict && vr.overallVerdict.summary) ? vr.overallVerdict.summary : '';
                    if (ovSummary) {
                        var sumBox = document.createElement('div');
                        sumBox.className = 'verification-summary';
                        sumBox.innerHTML = '<strong>Overall Assessment:</strong> ' + escapeHtml(ovSummary);
                        vCard.appendChild(sumBox);
                    }

                    if (vr.rubrics) {
                        var rGrid = document.createElement('div');
                        rGrid.className = 'rubrics-grid';

                        var rubricsList = [
                            { name: '🎯 Intent Match Check', data: vr.rubrics.intentMatch },
                            { name: '🖼️ Visual Delta Check', data: vr.rubrics.visualDelta },
                            { name: '🛡️ Absence of Errors Check', data: vr.rubrics.absenceOfErrors }
                        ];

                        rubricsList.forEach(function(item) {
                            if (item.data) {
                                var rItemCard = document.createElement('div');
                                rItemCard.className = 'rubric-item-card';
                                var score = (item.data.score || 'UNKNOWN').toUpperCase();
                                var scoreClass = (score === 'PASS' || score === 'PASSED') ? 'pill-pass' :
                                                 (score === 'FAIL' || score === 'FAILED') ? 'pill-fail' : 'pill-pending';

                                rItemCard.innerHTML = '<div class=\"rubric-item-header\">' +
                                                      '<span class=\"rubric-name\">' + item.name + '</span>' +
                                                      '<span class=\"rubric-score ' + scoreClass + '\">' + escapeHtml(score) + '</span>' +
                                                      '</div>' +
                                                      '<div class=\"rubric-analysis\">' + escapeHtml(item.data.analysis || 'No detailed analysis provided.') + '</div>';
                                rGrid.appendChild(rItemCard);
                            }
                        });

                        vCard.appendChild(rGrid);
                    }

                    verifPanel.appendChild(vCard);
                }

                // 5. Render Reasoning Panel
                var reasPanel = document.getElementById('panel-reasoning');
                reasPanel.innerHTML = '';
                if (step.reasoning) {
                    var rCard = document.createElement('div');
                    rCard.className = 'reasoning-card';
                    rCard.innerHTML = '<div class="reasoning-title">🧠 Step Intent & AI Reasoning:</div>';
                    var rBody = document.createElement('div');
                    rBody.className = 'reasoning-body';
                    rBody.textContent = step.reasoning;
                    rCard.appendChild(rBody);
                    reasPanel.appendChild(rCard);
                }
                if (step.failureReason) {
                    var fCard = document.createElement('div');
                    fCard.className = step.bug ? 'reasoning-card' : 'failure-card';
                    var fTitle = step.bug ? '🐛 Expected Bug / Defect Encountered:' : '❌ Failure Reason:';
                    fCard.innerHTML = '<div class="' + (step.bug ? 'reasoning-title' : 'failure-title') + '">' + fTitle + '</div>';
                    var fBody = document.createElement('div');
                    fBody.className = step.bug ? 'reasoning-body' : 'failure-body';
                    fBody.textContent = step.failureReason;
                    fCard.appendChild(fBody);
                    reasPanel.appendChild(fCard);
                }
                if (!step.reasoning && !step.failureReason) {
                    var emptyDiv = document.createElement('div');
                    emptyDiv.className = 'empty-inspector-state';
                    emptyDiv.textContent = 'No specific AI reasoning or error trace recorded for this step.';
                    reasPanel.appendChild(emptyDiv);
                }
            };

            // Lightbox Modal Functions
            window.openLightbox = function(src, title) {
                var overlay = document.getElementById('reportLightbox');
                var img = document.getElementById('lightboxImg');
                var titleEl = document.getElementById('lightboxTitle');
                var downloadBtn = document.getElementById('lightboxDownloadBtn');
                if (!overlay || !img) return;

                img.src = src;
                if (titleEl) {
                    titleEl.textContent = title || 'Screenshot Preview';
                }
                if (downloadBtn) {
                    downloadBtn.href = src;
                    var cleanTitle = (title ? title.replace(/[^a-zA-Z0-9_-]/g, '_') : 'screenshot');
                    downloadBtn.download = cleanTitle + '.png';
                }
                overlay.classList.add('active');
                document.body.style.overflow = 'hidden';
            };

            window.closeLightbox = function() {
                var overlay = document.getElementById('reportLightbox');
                if (overlay) {
                    overlay.classList.remove('active');
                }
                document.body.style.overflow = '';
            };

            window.handleLightboxBackdropClick = function(event) {
                if (event && (event.target.id === 'reportLightbox' || event.target.classList.contains('lightbox-body') || event.target.classList.contains('lightbox-overlay'))) {
                    window.closeLightbox();
                }
            };

            document.addEventListener('keydown', function(e) {
                if (e.key === 'Escape') {
                    window.closeLightbox();
                }
            });

            function createPromptSection(label, text, callIdx, fieldName) {
                var sec = document.createElement('div');
                sec.className = 'prompt-section';

                var row = document.createElement('div');
                row.className = 'prompt-label-row';

                var labelSpan = document.createElement('span');
                labelSpan.textContent = label;
                row.appendChild(labelSpan);

                var copyBtn = document.createElement('button');
                copyBtn.className = 'btn-copy';
                copyBtn.textContent = 'Copy';
                copyBtn.onclick = function() { window.copyLlmField(callIdx, fieldName, copyBtn); };
                row.appendChild(copyBtn);

                sec.appendChild(row);

                var pre = document.createElement('pre');
                pre.className = 'prompt-text';
                pre.textContent = text;
                sec.appendChild(pre);

                return sec;
            }

            // Auto-focus on failed step if test failed, but keep inspector closed by default unless user clicks inspect
            if (steps.length > 0) {
                var firstFail = steps.findIndex(function(s) { return (s.status || '').toUpperCase() === 'FAILED'; });
                currentParentIdx = firstFail >= 0 ? firstFail : 0;
            }
        })();
        </script>
        """);
    }

    private static void appendStyles(final StringBuilder sb)
    {
        sb.append("""
            :root {
                --bg: #f8fafc;
                --card-bg: #ffffff;
                --card-header-bg: #f1f5f9;
                --card-hover: #f8fafc;
                --border: #e2e8f0;
                --border-subtle: #cbd5e1;
                --text: #0f172a;
                --text-muted: #64748b;
                --text-sub: #475569;
                --accent-primary: #0284c7;
                --accent-primary-light: #e0f2fe;
                --accent-success: #16a34a;
                --accent-success-light: #dcfce7;
                --accent-danger: #dc2626;
                --accent-danger-light: #fee2e2;
                --accent-warning: #d97706;
                --accent-warning-light: #fef3c7;
                --accent-purple: #7c3aed;
                --accent-purple-light: #f3e8ff;
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
                max-width: 1440px;
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
                box-shadow: 0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.04);
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
                background: #f1f5f9;
                color: var(--text-sub);
                padding: 0.2rem 0.6rem;
                border-radius: 6px;
                border: 1px solid var(--border);
                font-family: var(--font-mono);
            }
            .meta-badge.highlight {
                color: var(--accent-primary);
                border-color: var(--accent-primary);
                background: var(--accent-primary-light);
                font-weight: 600;
            }
            .status-pill {
                font-size: 1rem;
                font-weight: 800;
                padding: 0.5rem 1.4rem;
                border-radius: 9999px;
                text-transform: uppercase;
                letter-spacing: 0.05em;
            }
            .status-pass { background: var(--accent-success-light); color: var(--accent-success); border: 1px solid var(--accent-success); }
            .status-fail { background: var(--accent-danger-light); color: var(--accent-danger); border: 1px solid var(--accent-danger); }
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
                box-shadow: 0 1px 3px rgba(0,0,0,0.06);
            }
            .metric-label {
                font-size: 0.8rem;
                text-transform: uppercase;
                letter-spacing: 0.05em;
                color: var(--text-muted);
                font-weight: 600;
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
            .card-section {
                background: var(--card-bg);
                border: 1px solid var(--border);
                border-radius: 10px;
                padding: 1.5rem;
                box-shadow: 0 1px 3px rgba(0,0,0,0.06);
            }
            .diagnostic-box {
                border-radius: 10px;
                padding: 1.25rem;
                display: flex;
                flex-direction: column;
                gap: 0.75rem;
            }
            .failure-box {
                background: var(--accent-danger-light);
                border: 1px solid rgba(220, 38, 38, 0.3);
            }
            .warning-box {
                background: var(--accent-warning-light);
                border: 1px solid rgba(217, 119, 6, 0.3);
            }
            .box-header {
                font-size: 1rem;
                font-weight: 700;
                color: var(--accent-danger);
            }
            .warning-box .box-header { color: var(--accent-warning); }
            .failure-reason {
                font-size: 0.95rem;
                color: #991b1b;
                font-family: var(--font-mono);
            }
            .visual-rca-box {
                background: #ffffff;
                border: 1px solid rgba(220, 38, 38, 0.3);
                border-radius: 8px;
                padding: 1rem;
            }
            .rca-title {
                font-weight: 600;
                color: var(--accent-primary);
                font-size: 0.9rem;
                margin-bottom: 0.4rem;
            }
            .rca-content {
                font-size: 0.88rem;
                color: var(--text-sub);
                white-space: pre-wrap;
            }
            .stacktrace-details summary {
                cursor: pointer;
                color: var(--text-muted);
                font-size: 0.85rem;
            }
            .code-block {
                background: #f1f5f9;
                border: 1px solid var(--border);
                border-radius: 6px;
                padding: 0.75rem;
                margin-top: 0.5rem;
                font-family: var(--font-mono);
                font-size: 0.8rem;
                color: #1e293b;
                overflow-x: auto;
            }
            .warning-list {
                padding-left: 1.25rem;
                font-size: 0.85rem;
                color: #92400e;
            }
            .section-title {
                font-size: 1.25rem;
                font-weight: 600;
                color: var(--text);
            }
            .section-header-row {
                display: flex;
                justify-content: space-between;
                align-items: center;
                margin-bottom: 1rem;
            }
            .btn-toggle-inspector {
                background: var(--card-bg);
                border: 1px solid var(--border);
                border-radius: 6px;
                padding: 0.4rem 0.8rem;
                font-size: 0.85rem;
                font-weight: 600;
                color: var(--accent-primary);
                cursor: pointer;
                transition: all 0.15s ease;
            }
            .btn-toggle-inspector:hover, .btn-toggle-inspector.active {
                background: var(--accent-primary-light);
                border-color: var(--accent-primary);
            }
            .steps-split-layout {
                display: grid;
                grid-template-columns: 1fr;
                gap: 0;
                align-items: start;
                transition: grid-template-columns 0.15s ease;
            }
            .steps-split-layout.inspector-open {
                grid-template-columns: 1fr 8px 520px;
            }
            @media (max-width: 1100px) {
                .steps-split-layout.inspector-open {
                    grid-template-columns: 1fr;
                }
                .split-resizer {
                    display: none !important;
                }
            }
            .split-resizer {
                display: none;
                width: 8px;
                cursor: col-resize;
                align-self: stretch;
                background: transparent;
                position: relative;
                transition: background 0.15s ease;
            }
            .split-resizer::after {
                content: "";
                position: absolute;
                top: 0;
                bottom: 0;
                left: 3px;
                width: 2px;
                background: var(--border);
                border-radius: 2px;
            }
            .split-resizer:hover::after, .split-resizer:active::after {
                background: var(--accent-primary);
                width: 4px;
                left: 2px;
            }
            .steps-split-layout.inspector-open .split-resizer {
                display: block;
            }
            .steps-master-pane {
                display: flex;
                flex-direction: column;
                gap: 0.85rem;
            }
            .step-card {
                background: var(--card-bg);
                border: 1px solid var(--border);
                border-radius: 10px;
                overflow: hidden;
                box-shadow: 0 1px 2px rgba(0,0,0,0.04);
                transition: all 0.15s ease-in-out;
            }
            .step-card:hover {
                border-color: var(--border-subtle);
                box-shadow: 0 2px 4px rgba(0,0,0,0.06);
            }
            .step-select-item.active {
                border-color: var(--accent-primary);
                box-shadow: 0 0 0 2px var(--accent-primary-light), 0 2px 4px rgba(0,0,0,0.06);
            }
            .step-header {
                background: #f8fafc;
                padding: 0.75rem 1.25rem;
                display: flex;
                justify-content: space-between;
                align-items: center;
                border-bottom: 1px solid var(--border);
            }
            .step-select-item.active .step-header {
                background: var(--accent-primary-light);
            }
            .step-header-left {
                display: flex;
                align-items: center;
                gap: 0.5rem;
                flex-wrap: wrap;
            }
            .step-number {
                font-weight: 700;
                color: var(--accent-primary);
                font-family: var(--font-mono);
                font-size: 0.95rem;
            }
            .step-status-pill {
                font-size: 0.72rem;
                font-weight: 700;
                padding: 0.15rem 0.5rem;
                border-radius: 4px;
                text-transform: uppercase;
            }
            .pill-pass { background: var(--accent-success-light); color: var(--accent-success); border: 1px solid rgba(22, 163, 74, 0.3); }
            .pill-heal { background: var(--accent-purple-light); color: var(--accent-purple); border: 1px solid rgba(124, 58, 237, 0.3); }
            .pill-fail { background: var(--accent-danger-light); color: var(--accent-danger); border: 1px solid rgba(220, 38, 38, 0.3); }
            .pill-skip { background: #f1f5f9; color: var(--text-muted); border: 1px solid var(--border); }
            .pill-pending { background: var(--accent-warning-light); color: var(--accent-warning); border: 1px solid rgba(217, 119, 6, 0.3); }
            .badge-flag {
                font-size: 0.72rem;
                font-weight: 700;
                padding: 0.15rem 0.5rem;
                border-radius: 4px;
                text-transform: uppercase;
                background: #f1f5f9;
                color: var(--text-sub);
                border: 1px solid var(--border);
            }
            .badge-flag.bug-badge {
                background: var(--accent-warning-light);
                color: var(--accent-warning);
                border-color: rgba(217, 119, 6, 0.4);
            }
            .badge-flag.optional-badge {
                background: var(--accent-purple-light);
                color: var(--accent-purple);
                border-color: rgba(124, 58, 237, 0.4);
            }
            .badge-flag.intent-badge {
                background: #e0f2fe;
                color: #0369a1;
                border-color: rgba(3, 105, 161, 0.35);
            }
            .badge-flag.verification-badge-pass {
                background: var(--accent-success-light);
                color: var(--accent-success);
                border-color: rgba(22, 163, 74, 0.4);
            }
            .badge-flag.verification-badge-fail {
                background: var(--accent-danger-light);
                color: var(--accent-danger);
                border-color: rgba(220, 38, 38, 0.4);
            }
            .step-header-right {
                display: flex;
                align-items: center;
                gap: 0.6rem;
                font-size: 0.8rem;
                color: var(--text-muted);
                font-family: var(--font-mono);
            }
            .btn-inspect-step, .btn-inspect-substep {
                background: #ffffff;
                border: 1px solid var(--border);
                border-radius: 6px;
                padding: 0.25rem 0.6rem;
                font-size: 0.78rem;
                font-weight: 600;
                color: var(--accent-primary);
                cursor: pointer;
                transition: all 0.15s ease;
            }
            .btn-inspect-step:hover, .btn-inspect-substep:hover {
                background: var(--accent-primary-light);
                border-color: var(--accent-primary);
            }
            .step-body {
                padding: 1rem 1.25rem;
                cursor: pointer;
            }
            .step-instruction-full {
                font-weight: 600;
                font-size: 0.95rem;
                color: var(--text);
                line-height: 1.4;
                word-break: break-word;
                white-space: normal;
            }
            .step-source-meta {
                font-size: 0.78rem;
                color: var(--text-muted);
                font-family: var(--font-mono);
                margin-top: 0.4rem;
            }
            .step-card-footer {
                padding: 0.5rem 1.25rem;
                display: flex;
                flex-wrap: wrap;
                gap: 0.4rem;
                background: #ffffff;
                border-top: 1px solid var(--border);
            }
            .footer-tag {
                font-size: 0.72rem;
                background: #f1f5f9;
                color: var(--text-sub);
                padding: 0.15rem 0.45rem;
                border-radius: 4px;
                font-weight: 500;
            }
            .footer-tag.highlight {
                background: var(--accent-primary-light);
                color: var(--accent-primary);
                font-weight: 600;
            }
            .footer-tag.verif-pass {
                background: var(--accent-success-light);
                color: var(--accent-success);
                font-weight: 600;
            }
            .footer-tag.verif-fail {
                background: var(--accent-danger-light);
                color: var(--accent-danger);
                font-weight: 600;
            }
            .pill-verif-badge {
                font-size: 0.7rem;
                font-weight: 700;
                padding: 0.1rem 0.4rem;
                border-radius: 9999px;
                margin-left: 0.3rem;
                text-transform: uppercase;
            }
            .sub-steps-container {
                background: #f8fafc;
                border-top: 1px solid var(--border);
                padding: 0.75rem 1.25rem;
                display: flex;
                flex-direction: column;
                gap: 0.5rem;
            }
            .sub-steps-header {
                font-size: 0.8rem;
                font-weight: 700;
                color: var(--text-muted);
                text-transform: uppercase;
                letter-spacing: 0.05em;
            }
            .sub-step-card {
                background: #ffffff;
                border: 1px solid var(--border);
                border-radius: 6px;
                padding: 0.6rem 0.75rem;
                transition: all 0.15s ease;
                display: flex;
                flex-direction: column;
                gap: 0.4rem;
            }
            .sub-step-card.active {
                border-color: var(--accent-primary);
                background: #ffffff;
                box-shadow: 0 0 0 2px var(--accent-primary-light);
            }
            .sub-step-header {
                display: flex;
                align-items: center;
                justify-content: space-between;
                gap: 0.6rem;
                flex-wrap: wrap;
            }
            .sub-step-header-left {
                display: flex;
                align-items: center;
                gap: 0.5rem;
                flex-wrap: wrap;
                flex: 1;
            }
            .sub-step-header-right {
                display: flex;
                align-items: center;
                gap: 0.5rem;
            }
            .sub-step-number {
                font-family: var(--font-mono);
                font-weight: 700;
                font-size: 0.85rem;
                color: var(--accent-primary);
            }
            .sub-step-instruction {
                font-size: 0.88rem;
                color: var(--text);
                font-weight: 500;
                flex: 1;
                cursor: pointer;
            }
            .sub-step-duration {
                font-size: 0.75rem;
                font-family: var(--font-mono);
                color: var(--text-muted);
            }
            .sub-step-body {
                padding: 0.2rem 0;
                cursor: pointer;
            }
            .sub-step-footer {
                display: flex;
                flex-wrap: wrap;
                gap: 0.4rem;
                padding-top: 0.35rem;
                border-top: 1px solid #f1f5f9;
            }
            .steps-inspector-pane {
                display: none;
                position: sticky;
                top: 1rem;
                max-height: calc(100vh - 2rem);
                overflow-y: auto;
            }
            .steps-split-layout.inspector-open .steps-inspector-pane {
                display: block;
            }
            .inspector-card {
                background: var(--card-bg);
                border: 1px solid var(--border);
                border-radius: 12px;
                box-shadow: 0 4px 12px rgba(0,0,0,0.08);
                display: flex;
                flex-direction: column;
                overflow: hidden;
            }
            .inspector-header {
                background: #f8fafc;
                padding: 1.25rem 1.5rem;
                border-bottom: 1px solid var(--border);
                display: flex;
                flex-direction: column;
                gap: 0.4rem;
            }
            .inspector-header-top {
                display: flex;
                justify-content: space-between;
                align-items: center;
                flex-wrap: wrap;
                gap: 0.5rem;
            }
            .inspector-header-left {
                display: flex;
                align-items: center;
                gap: 0.5rem;
                flex-wrap: wrap;
            }
            .inspector-header-controls {
                display: flex;
                align-items: center;
                gap: 0.4rem;
            }
            .width-presets {
                display: flex;
                gap: 0.2rem;
            }
            .btn-preset {
                background: #ffffff;
                border: 1px solid var(--border);
                border-radius: 4px;
                padding: 0.15rem 0.4rem;
                font-size: 0.7rem;
                font-weight: 600;
                color: var(--text-sub);
                cursor: pointer;
            }
            .btn-preset:hover {
                background: var(--accent-primary-light);
                color: var(--accent-primary);
            }
            .inspector-badge {
                font-size: 0.95rem;
                font-weight: 700;
                color: var(--accent-primary);
                font-family: var(--font-mono);
            }
            .btn-close-inspector {
                background: #ffffff;
                border: 1px solid var(--border);
                border-radius: 6px;
                width: 28px;
                height: 28px;
                display: flex;
                align-items: center;
                justify-content: center;
                font-weight: 700;
                font-size: 0.9rem;
                color: var(--text-muted);
                cursor: pointer;
            }
            .btn-close-inspector:hover {
                background: var(--accent-danger-light);
                color: var(--accent-danger);
                border-color: var(--accent-danger);
            }
            .inspector-instruction {
                font-size: 1.05rem;
                font-weight: 700;
                color: var(--text);
                line-height: 1.4;
            }
            .inspector-raw-template {
                font-size: 0.8rem;
                color: var(--text-muted);
                font-family: var(--font-mono);
                background: #f1f5f9;
                padding: 0.2rem 0.5rem;
                border-radius: 4px;
                display: inline-block;
            }
            .inspector-sub-meta {
                font-size: 0.8rem;
                color: var(--text-muted);
                font-family: var(--font-mono);
            }
            .step-card-error-banner {
                background: #fef2f2;
                border: 1px solid #fecaca;
                border-left: 4px solid var(--accent-danger);
                border-radius: 6px;
                color: #991b1b;
                padding: 0.5rem 0.75rem;
                font-size: 0.82rem;
                margin-top: 0.5rem;
                word-break: break-word;
            }
            .inspector-error-banner {
                background: #fef2f2;
                border: 1px solid #fecaca;
                border-left: 4px solid var(--accent-danger);
                border-radius: 6px;
                color: #991b1b;
                padding: 0.6rem 0.85rem;
                font-size: 0.85rem;
                margin-top: 0.4rem;
                word-break: break-word;
            }
            .pill-error-count {
                background: var(--accent-danger);
                color: #ffffff;
                padding: 0.1rem 0.4rem;
                border-radius: 9999px;
                font-size: 0.7rem;
                font-weight: 700;
                margin-left: 0.3rem;
            }
            .inspector-tabs {
                display: flex;
                background: #f1f5f9;
                border-bottom: 1px solid var(--border);
                padding: 0.25rem 0.5rem 0;
                gap: 0.25rem;
                overflow-x: auto;
            }
            .tab-btn {
                background: transparent;
                border: none;
                border-radius: 6px 6px 0 0;
                padding: 0.6rem 0.9rem;
                font-size: 0.82rem;
                font-weight: 600;
                color: var(--text-muted);
                cursor: pointer;
                white-space: nowrap;
                transition: all 0.15s ease;
            }
            .tab-btn:hover {
                color: var(--text);
                background: rgba(255,255,255,0.5);
            }
            .tab-btn.active {
                background: var(--card-bg);
                color: var(--accent-primary);
                border-bottom: 2px solid var(--accent-primary);
            }
            .inspector-content {
                padding: 1.25rem;
                min-height: 350px;
                display: flex;
                flex-direction: column;
                gap: 1rem;
            }
            .tab-panel {
                display: none;
                flex-direction: column;
                gap: 1rem;
            }
            .tab-panel.active {
                display: flex;
            }
            .empty-inspector-state {
                text-align: center;
                padding: 3rem 1rem;
                color: var(--text-muted);
                font-size: 0.9rem;
            }
            .llm-call-card {
                border: 1px solid var(--border);
                border-radius: 8px;
                background: #f8fafc;
                overflow: hidden;
            }
            .llm-call-header {
                background: #f1f5f9;
                padding: 0.6rem 1rem;
                display: flex;
                justify-content: space-between;
                align-items: center;
                border-bottom: 1px solid var(--border);
                flex-wrap: wrap;
                gap: 0.4rem;
            }
            .llm-call-title {
                font-weight: 600;
                font-size: 0.85rem;
            }
            .llm-call-meta {
                font-size: 0.78rem;
                font-family: var(--font-mono);
                color: var(--text-muted);
            }
            .prompt-section {
                padding: 0.75rem 1rem;
                border-bottom: 1px solid var(--border);
            }
            .prompt-section:last-child {
                border-bottom: none;
            }
            .prompt-label-row {
                display: flex;
                justify-content: space-between;
                align-items: center;
                margin-bottom: 0.4rem;
                font-size: 0.78rem;
                font-weight: 600;
                color: var(--text-muted);
                text-transform: uppercase;
                letter-spacing: 0.05em;
            }
            .prompt-text {
                background: #ffffff;
                border: 1px solid var(--border);
                border-radius: 6px;
                padding: 0.75rem;
                font-family: var(--font-mono);
                font-size: 0.78rem;
                color: #1e293b;
                max-height: 260px;
                overflow-y: auto;
                white-space: pre-wrap;
                word-break: break-word;
            }
            .response-section {
                background: #ffffff;
            }
            .btn-copy {
                background: #ffffff;
                border: 1px solid var(--border);
                border-radius: 4px;
                padding: 0.15rem 0.45rem;
                font-size: 0.7rem;
                font-weight: 600;
                color: var(--text-sub);
                cursor: pointer;
            }
            .btn-copy:hover {
                background: #f1f5f9;
                color: var(--text);
            }
            .code-selector {
                cursor: pointer;
                background: #f1f5f9;
                padding: 0.1rem 0.3rem;
                border-radius: 4px;
            }
            .code-selector:hover {
                background: var(--accent-primary-light);
                color: var(--accent-primary);
            }
            .action-tpl-note {
                font-size: 0.72rem;
                color: var(--text-sub, #64748b);
                margin-top: 3px;
                opacity: 0.85;
            }
            .action-tpl-note code {
                font-size: 0.7rem;
                background: #f1f5f9;
                padding: 1px 4px;
                border-radius: 3px;
            }
            .reasoning-card {
                background: var(--accent-primary-light);
                border-left: 4px solid var(--accent-primary);
                border-radius: 0 8px 8px 0;
                padding: 1rem;
            }
            .reasoning-title {
                font-weight: 700;
                color: var(--accent-primary);
                font-size: 0.9rem;
                margin-bottom: 0.3rem;
            }
            .reasoning-body {
                font-size: 0.9rem;
                color: #0369a1;
            }
            .failure-card {
                background: var(--accent-danger-light);
                border-left: 4px solid var(--accent-danger);
                border-radius: 0 8px 8px 0;
                padding: 1rem;
            }
            .failure-title {
                font-weight: 700;
                color: var(--accent-danger);
                font-size: 0.9rem;
                margin-bottom: 0.3rem;
            }
            .failure-body {
                font-size: 0.88rem;
                color: #991b1b;
                font-family: var(--font-mono);
            }
            .verification-card {
                background: #ffffff;
                border: 1px solid var(--border);
                border-radius: 8px;
                padding: 1rem;
                box-shadow: 0 1px 3px rgba(0,0,0,0.05);
            }
            .verification-header {
                display: flex;
                align-items: center;
                justify-content: space-between;
                margin-bottom: 0.75rem;
                padding-bottom: 0.5rem;
                border-bottom: 1px solid var(--border);
                flex-wrap: wrap;
                gap: 0.5rem;
            }
            .verification-title {
                font-weight: 700;
                font-size: 0.95rem;
                color: var(--text);
                display: flex;
                align-items: center;
                gap: 0.5rem;
            }
            .verification-summary {
                font-size: 0.88rem;
                color: var(--text);
                line-height: 1.5;
                margin-bottom: 1rem;
                background: #f8fafc;
                padding: 0.75rem;
                border-radius: 6px;
                border-left: 4px solid var(--accent-primary);
            }
            .rubrics-grid {
                display: grid;
                grid-template-columns: 1fr;
                gap: 0.75rem;
            }
            .rubric-item-card {
                background: #f8fafc;
                border: 1px solid var(--border);
                border-radius: 6px;
                padding: 0.75rem;
            }
            .rubric-item-header {
                display: flex;
                align-items: center;
                justify-content: space-between;
                margin-bottom: 0.35rem;
                flex-wrap: wrap;
                gap: 0.4rem;
            }
            .rubric-name {
                font-weight: 600;
                font-size: 0.85rem;
                color: var(--text);
            }
            .rubric-score {
                font-size: 0.72rem;
                font-weight: 700;
                padding: 0.15rem 0.45rem;
                border-radius: 4px;
                text-transform: uppercase;
            }
            .rubric-analysis {
                font-size: 0.82rem;
                color: var(--text-sub);
                line-height: 1.4;
            }
            .context-badge {
                background: #f1f5f9;
                color: var(--text-sub);
                padding: 0.15rem 0.5rem;
                border-radius: 4px;
                font-size: 0.75rem;
                font-weight: 600;
                border: 1px solid var(--border);
            }
            .escalation-badge {
                background: var(--accent-warning-light);
                color: var(--accent-warning);
                padding: 0.15rem 0.5rem;
                border-radius: 4px;
                font-size: 0.75rem;
                font-weight: 700;
                border: 1px solid rgba(217, 119, 6, 0.3);
            }
            .data-table {
                width: 100%;
                border-collapse: collapse;
                font-size: 0.85rem;
                text-align: left;
            }
            .data-table th {
                background: #f8fafc;
                color: var(--text-sub);
                padding: 0.6rem 0.8rem;
                font-weight: 600;
                border-bottom: 2px solid var(--border);
            }
            .data-table td {
                padding: 0.6rem 0.8rem;
                border-bottom: 1px solid var(--border);
                color: var(--text);
            }
            .data-table tr.row-total {
                background: #f1f5f9;
                font-weight: 700;
            }
            .data-table tr:hover { background: #f8fafc; }
            .badge-action {
                background: var(--accent-primary-light);
                color: var(--accent-primary);
                padding: 0.15rem 0.45rem;
                border-radius: 4px;
                font-size: 0.75rem;
                font-weight: 600;
                font-family: var(--font-mono);
            }
            .badge-role {
                background: var(--accent-purple-light);
                color: var(--accent-purple);
                padding: 0.15rem 0.45rem;
                border-radius: 4px;
                font-size: 0.75rem;
                font-weight: 600;
                font-family: var(--font-mono);
            }
            .step-ref {
                cursor: pointer;
                color: var(--accent-primary);
                font-weight: 600;
                font-family: var(--font-mono);
            }
            .step-ref:hover { text-decoration: underline; }
            .stats-sub-row {
                display: flex;
                flex-wrap: wrap;
                gap: 0.75rem;
                margin-top: 1rem;
                padding-top: 1rem;
                border-top: 1px solid var(--border);
            }
            .stats-tag {
                font-size: 0.8rem;
                background: #f1f5f9;
                padding: 0.3rem 0.7rem;
                border-radius: 6px;
                border: 1px solid var(--border);
                color: var(--text-sub);
            }
            .stats-tag.level-tag {
                font-family: var(--font-mono);
                background: #f8fafc;
            }
            .table-container {
                background: var(--card-bg);
                border: 1px solid var(--border);
                border-radius: 8px;
                overflow-x: auto;
            }
            .screenshots-grid {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
                gap: 1rem;
            }
            .screenshot-card {
                background: var(--card-bg);
                border: 1px solid var(--border);
                border-radius: 8px;
                overflow: hidden;
                box-shadow: 0 1px 3px rgba(0,0,0,0.06);
            }
            .screenshot-header {
                background: #f8fafc;
                padding: 0.5rem 0.8rem;
                font-size: 0.8rem;
                font-weight: 600;
                color: var(--text-muted);
                border-bottom: 1px solid var(--border);
                display: flex;
                align-items: center;
                justify-content: space-between;
                gap: 0.5rem;
            }
            .screenshot-dim-badge {
                display: inline-block;
                background: #e2e8f0;
                color: #475569;
                font-size: 0.7rem;
                font-weight: 600;
                padding: 0.15rem 0.4rem;
                border-radius: 4px;
                font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
            }
            .screenshot-meta-bar {
                background: #f8fafc;
                padding: 0.4rem 0.8rem;
                font-size: 0.75rem;
                color: var(--text-muted);
                border-top: 1px solid var(--border);
                display: flex;
                align-items: center;
                gap: 0.6rem;
            }
            .dim-pill {
                display: inline-flex;
                align-items: center;
                gap: 0.25rem;
                font-size: 0.72rem;
                color: #475569;
                font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
            }
            .dim-pill strong {
                color: #0f172a;
            }
            .screenshot-img {
                width: 100%;
                height: auto;
                display: block;
                cursor: pointer;
            }
            .visual-badge {
                background: #fdf2f8;
                color: #db2777;
                border-color: #fbcfe8;
            }
            .step-card-screenshots-preview {
                display: flex;
                flex-wrap: wrap;
                gap: 0.5rem;
                margin-top: 0.6rem;
                align-items: center;
            }
            .ssim-comparison-box {
                background: #f8fafc;
                border: 1px solid var(--border);
                border-radius: 8px;
                padding: 1rem;
                margin-bottom: 1.2rem;
            }
            .ssim-score-header {
                display: flex;
                align-items: center;
                gap: 0.5rem;
                margin-bottom: 0.8rem;
            }
            .ssim-matrices-grid {
                display: flex;
                flex-wrap: wrap;
                gap: 1.5rem;
                align-items: flex-start;
            }
            .ssim-matrix-card {
                display: flex;
                flex-direction: column;
                align-items: center;
                gap: 0.4rem;
                background: #ffffff;
                padding: 0.6rem;
                border: 1px solid var(--border);
                border-radius: 6px;
            }
            .ssim-matrix-label {
                font-size: 0.75rem;
                font-weight: 600;
                color: var(--text-muted);
            }
            .ssim-matrix-img {
                width: 128px;
                height: 128px;
                image-rendering: pixelated;
                border: 1px solid var(--border);
                border-radius: 4px;
                background: #000000;
            }
            .ssim-pass {
                background: #f0fdf4 !important;
                color: #166534 !important;
                border: 1px solid #bbf7d0 !important;
            }
            .ssim-fail {
                background: #fef2f2 !important;
                color: #991b1b !important;
                border: 1px solid #fecaca !important;
            }
            .preview-thumb {
                width: 90px;
                height: 60px;
                object-fit: cover;
                border-radius: 4px;
                border: 1px solid var(--border);
                box-shadow: 0 1px 2px rgba(0,0,0,0.05);
                cursor: pointer;
                transition: transform 0.15s ease, box-shadow 0.15s ease;
            }
            .preview-thumb:hover {
                transform: scale(1.08);
                box-shadow: 0 4px 8px rgba(0,0,0,0.15);
                border-color: var(--primary);
            }
            .preview-more-badge {
                font-size: 0.75rem;
                font-weight: 600;
                color: var(--text-muted);
                background: #f1f5f9;
                padding: 0.2rem 0.5rem;
                border-radius: 4px;
                border: 1px solid var(--border);
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
            .lightbox-overlay {
                display: none;
                position: fixed;
                top: 0;
                left: 0;
                width: 100vw;
                height: 100vh;
                background: rgba(15, 23, 42, 0.85);
                backdrop-filter: blur(8px);
                -webkit-backdrop-filter: blur(8px);
                z-index: 10000;
                align-items: center;
                justify-content: center;
                opacity: 0;
                transition: opacity 0.2s ease-in-out;
            }
            .lightbox-overlay.active {
                display: flex;
                opacity: 1;
            }
            .lightbox-dialog {
                max-width: 95vw;
                max-height: 95vh;
                display: flex;
                flex-direction: column;
                background: #0f172a;
                border: 1px solid rgba(255, 255, 255, 0.15);
                border-radius: 12px;
                overflow: hidden;
                box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.7);
                animation: lightboxZoomIn 0.2s cubic-bezier(0.16, 1, 0.3, 1);
            }
            @keyframes lightboxZoomIn {
                from { transform: scale(0.95); opacity: 0; }
                to { transform: scale(1); opacity: 1; }
            }
            .lightbox-header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                padding: 0.75rem 1.25rem;
                background: #1e293b;
                border-bottom: 1px solid rgba(255, 255, 255, 0.1);
                color: #f8fafc;
            }
            .lightbox-title {
                font-size: 0.95rem;
                font-weight: 600;
                letter-spacing: -0.01em;
                color: #f8fafc;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
                max-width: 70vw;
            }
            .lightbox-controls {
                display: flex;
                align-items: center;
                gap: 0.5rem;
            }
            .lightbox-btn {
                background: rgba(255, 255, 255, 0.1);
                border: 1px solid rgba(255, 255, 255, 0.2);
                color: #f8fafc;
                padding: 0.35rem 0.75rem;
                border-radius: 6px;
                font-size: 0.8rem;
                font-weight: 600;
                cursor: pointer;
                text-decoration: none;
                transition: all 0.15s;
                display: inline-flex;
                align-items: center;
                gap: 0.3rem;
            }
            .lightbox-btn:hover {
                background: rgba(255, 255, 255, 0.2);
                color: #ffffff;
            }
            .lightbox-close {
                font-size: 1.1rem;
                line-height: 1;
                padding: 0.35rem 0.65rem;
            }
            .lightbox-close:hover {
                background: #ef4444;
                border-color: #ef4444;
            }
            .lightbox-body {
                padding: 1rem;
                display: flex;
                align-items: center;
                justify-content: center;
                overflow: auto;
                max-height: calc(95vh - 55px);
                background: #020617;
            }
            .lightbox-img {
                max-width: 90vw;
                max-height: 82vh;
                object-fit: contain;
                border-radius: 6px;
                box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.5);
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

    private static String escapeAttr(final String text)
    {
        if (text == null)
        {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("\"", "&quot;")
                   .replace("'", "\\'")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;");
    }
}
