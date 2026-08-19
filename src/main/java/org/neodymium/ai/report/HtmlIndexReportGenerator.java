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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generator and aggregator for creating and updating a live, reverse-chronological
 * {@code index.html} test execution dashboard in the reports directory.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class HtmlIndexReportGenerator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlIndexReportGenerator.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat COST_FORMAT = new DecimalFormat("$#,##0.0000", DecimalFormatSymbols.getInstance(Locale.US));
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private static final SimpleDateFormat DATE_PART_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat TIME_PART_FORMAT = new SimpleDateFormat("HH:mm:ss", Locale.US);

    private static final String REGISTRY_FILE = "index-data.json";
    private static final String INDEX_HTML_FILE = "index.html";

    /**
     * Data transfer object representing a single test execution entry in the index registry.
     */
    public static final class IndexEntry
    {
        private String baseFileName;
        private String htmlFileName;
        private String jsonFileName;
        private String mdFileName;
        private String testClass;
        private String testMethod;
        private String testName;
        private String datasetId;
        private String status;
        private String executionMode;
        private long timestamp;
        private long durationMs;
        private int stepCount;
        private int passedSteps;
        private int healedSteps;
        private int failedSteps;
        private int llmCalls;
        private long totalTokens;
        private double estimatedCostUsd;
        private String failureReason;
        private boolean bug;
        private String bugDetails;

        public IndexEntry()
        {
        }

        public static IndexEntry fromReport(final TestExecutionReport report, final String baseFileName)
        {
            final IndexEntry entry = new IndexEntry();
            entry.setBaseFileName(baseFileName);
            entry.setHtmlFileName(baseFileName + ".html");
            entry.setJsonFileName(baseFileName + ".json");
            entry.setMdFileName(baseFileName + ".md");
            entry.setTestClass(report.getTestClass());
            entry.setTestMethod(report.getTestMethod());
            entry.setTestName(report.getTestName());
            entry.setDatasetId(report.getDatasetId());
            entry.setStatus(report.getStatus() != null ? report.getStatus() : "UNKNOWN");
            entry.setExecutionMode(report.getExecutionMode());
            final long timestamp = report.getStartTimeMs() > 0 ? report.getStartTimeMs() : System.currentTimeMillis();
            entry.setTimestamp(timestamp);
            entry.setDurationMs(report.getDurationMs());

            final TestExecutionReport.ReportMetrics m = report.getMetrics();
            if (m != null)
            {
                entry.setStepCount(m.getTotalSteps());
                entry.setPassedSteps(Math.max(0, m.getTotalSteps() - m.getHealedSteps() - m.getFailedSteps() - m.getSkippedSteps()));
                entry.setHealedSteps(m.getHealedSteps());
                entry.setFailedSteps(m.getFailedSteps());
                entry.setLlmCalls(m.getTotalLlmCalls());
                entry.setTotalTokens(m.getTotalTokens());
                entry.setEstimatedCostUsd(m.getEstimatedCostUsd());

                if (m.getHealedSteps() > 0 && !"FAILED".equalsIgnoreCase(entry.getStatus()))
                {
                    entry.setStatus("HEALED");
                }
            }
            else if (report.getSteps() != null)
            {
                entry.setStepCount(report.getSteps().size());
            }

            entry.setFailureReason(report.getFailureReason());

            if (report.getSteps() != null)
            {
                final boolean hasBug = report.getSteps().stream().anyMatch(TestExecutionReport.ReportStepEntry::isBug);
                entry.setBug(hasBug);
                for (final TestExecutionReport.ReportStepEntry s : report.getSteps())
                {
                    if (s.isBug() && s.getBugDetails() != null)
                    {
                        entry.setBugDetails(s.getBugDetails());
                        break;
                    }
                }
            }

            return entry;
        }

        public String getBaseFileName()
        {
            return this.baseFileName;
        }

        public void setBaseFileName(final String baseFileName)
        {
            this.baseFileName = baseFileName;
        }

        public String getHtmlFileName()
        {
            return this.htmlFileName;
        }

        public void setHtmlFileName(final String htmlFileName)
        {
            this.htmlFileName = htmlFileName;
        }

        public String getJsonFileName()
        {
            return this.jsonFileName;
        }

        public void setJsonFileName(final String jsonFileName)
        {
            this.jsonFileName = jsonFileName;
        }

        public String getMdFileName()
        {
            return this.mdFileName;
        }

        public void setMdFileName(final String mdFileName)
        {
            this.mdFileName = mdFileName;
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

        public String getTestName()
        {
            return this.testName;
        }

        public void setTestName(final String testName)
        {
            this.testName = testName;
        }

        public String getDatasetId()
        {
            return this.datasetId;
        }

        public void setDatasetId(final String datasetId)
        {
            this.datasetId = datasetId;
        }

        public String getStatus()
        {
            return this.status;
        }

        public void setStatus(final String status)
        {
            this.status = status;
        }

        public String getExecutionMode()
        {
            return this.executionMode;
        }

        public void setExecutionMode(final String executionMode)
        {
            this.executionMode = executionMode;
        }

        public long getTimestamp()
        {
            return this.timestamp;
        }

        public void setTimestamp(final long timestamp)
        {
            this.timestamp = timestamp;
        }

        public long getDurationMs()
        {
            return this.durationMs;
        }

        public void setDurationMs(final long durationMs)
        {
            this.durationMs = durationMs;
        }

        public int getStepCount()
        {
            return this.stepCount;
        }

        public void setStepCount(final int stepCount)
        {
            this.stepCount = stepCount;
        }

        public int getPassedSteps()
        {
            return this.passedSteps;
        }

        public void setPassedSteps(final int passedSteps)
        {
            this.passedSteps = passedSteps;
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

        public int getLlmCalls()
        {
            return this.llmCalls;
        }

        public void setLlmCalls(final int llmCalls)
        {
            this.llmCalls = llmCalls;
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

        public String getFailureReason()
        {
            return this.failureReason;
        }

        public void setFailureReason(final String failureReason)
        {
            this.failureReason = failureReason;
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
    }

    /**
     * Updates the report index files ({@code index-data.json} and {@code index.html}) in the target directory.
     *
     * @param outputDirectory the target directory containing test reports
     * @param currentReport the newly finished test execution report
     * @param baseFileName the base filename of the report
     */
    public synchronized void updateIndex(final Path outputDirectory, final TestExecutionReport currentReport, final String baseFileName)
    {
        if (outputDirectory == null || currentReport == null || baseFileName == null || baseFileName.isBlank())
        {
            return;
        }

        try
        {
            if (!Files.exists(outputDirectory))
            {
                Files.createDirectories(outputDirectory);
            }

            final Path registryPath = outputDirectory.resolve(REGISTRY_FILE);
            final Map<String, IndexEntry> entriesByBase = new LinkedHashMap<>();

            // 1. Read existing index registry if present
            if (Files.exists(registryPath))
            {
                try
                {
                    final List<IndexEntry> existing = OBJECT_MAPPER.readValue(registryPath.toFile(), new TypeReference<List<IndexEntry>>() {});
                    if (existing != null)
                    {
                        for (final IndexEntry e : existing)
                        {
                            if (e.getBaseFileName() != null)
                            {
                                entriesByBase.put(e.getBaseFileName(), e);
                            }
                        }
                    }
                }
                catch (final Exception e)
                {
                    LOGGER.warn("Could not parse existing index registry file {}, rebuilding from scratch: {}", registryPath, e.getMessage());
                }
            }

            // 2. Discover/scan any unindexed *.json reports in the output directory
            scanDirectoryForReports(outputDirectory, entriesByBase);

            // 3. Put/replace current test run entry
            final IndexEntry currentEntry = IndexEntry.fromReport(currentReport, baseFileName);
            entriesByBase.put(baseFileName, currentEntry);

            // 4. Sort entries reverse-chronologically (newest on top)
            final List<IndexEntry> sortedEntries = new ArrayList<>(entriesByBase.values());
            sortedEntries.sort(Comparator.comparingLong(IndexEntry::getTimestamp).reversed());

            // 5. Persist registry JSON
            OBJECT_MAPPER.writeValue(registryPath.toFile(), sortedEntries);

            // 6. Generate and write index.html
            final String htmlContent = generateIndexHtml(sortedEntries);
            final Path indexPath = outputDirectory.resolve(INDEX_HTML_FILE);
            Files.writeString(indexPath, htmlContent, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

            LOGGER.info("📑 Test execution index updated: {}", indexPath.toAbsolutePath());
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to update test execution index in {}: {}", outputDirectory, e.getMessage(), e);
        }
    }

    /**
     * Refreshes and rebuilds the report index files ({@code index-data.json} and {@code index.html})
     * by scanning the target directory for all report JSON files.
     *
     * @param outputDirectory the target directory containing test reports
     */
    public synchronized void refreshIndex(final Path outputDirectory)
    {
        if (outputDirectory == null)
        {
            return;
        }

        try
        {
            if (!Files.exists(outputDirectory))
            {
                return;
            }

            final Path registryPath = outputDirectory.resolve(REGISTRY_FILE);
            final Map<String, IndexEntry> entriesByBase = new LinkedHashMap<>();

            // 1. Read existing index registry if present
            if (Files.exists(registryPath))
            {
                try
                {
                    final List<IndexEntry> existing = OBJECT_MAPPER.readValue(registryPath.toFile(), new TypeReference<List<IndexEntry>>() {});
                    if (existing != null)
                    {
                        for (final IndexEntry e : existing)
                        {
                            if (e.getBaseFileName() != null)
                            {
                                entriesByBase.put(e.getBaseFileName(), e);
                            }
                        }
                    }
                }
                catch (final Exception e)
                {
                    LOGGER.warn("Could not parse existing index registry file {}, rebuilding from scratch: {}", registryPath, e.getMessage());
                }
            }

            // 2. Discover/scan any unindexed *.json reports in the output directory
            scanDirectoryForReports(outputDirectory, entriesByBase);

            // 3. Sort entries reverse-chronologically (newest on top)
            final List<IndexEntry> sortedEntries = new ArrayList<>(entriesByBase.values());
            sortedEntries.sort(Comparator.comparingLong(IndexEntry::getTimestamp).reversed());

            // 4. Persist registry JSON
            OBJECT_MAPPER.writeValue(registryPath.toFile(), sortedEntries);

            // 5. Generate and write index.html
            final String htmlContent = generateIndexHtml(sortedEntries);
            final Path indexPath = outputDirectory.resolve(INDEX_HTML_FILE);
            Files.writeString(indexPath, htmlContent, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

            LOGGER.info("📑 Test execution index refreshed: {}", indexPath.toAbsolutePath());
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to refresh test execution index in {}: {}", outputDirectory, e.getMessage(), e);
        }
    }

    /**
     * CLI entry point for refreshing index.html in the specified or default target/ai-reports directory.
     *
     * @param args optional target directory path
     */
    public static void main(final String[] args)
    {
        final Path dir = args != null && args.length > 0 ? Path.of(args[0]) : Path.of("target/ai-reports");
        new HtmlIndexReportGenerator().refreshIndex(dir);
    }

    private static void scanDirectoryForReports(final Path outputDirectory, final Map<String, IndexEntry> targetMap)
    {
        final File dir = outputDirectory.toFile();
        final File[] jsonFiles = dir.listFiles((d, name) -> name.endsWith(".json") && !name.equals(REGISTRY_FILE));
        if (jsonFiles == null || jsonFiles.length == 0)
        {
            return;
        }

        for (final File jsonFile : jsonFiles)
        {
            final String name = jsonFile.getName();
            final String base = name.substring(0, name.length() - 5);
            if (!targetMap.containsKey(base))
            {
                try
                {
                    final TestExecutionReport rep = OBJECT_MAPPER.readValue(jsonFile, TestExecutionReport.class);
                    if (rep != null)
                    {
                        targetMap.put(base, IndexEntry.fromReport(rep, base));
                    }
                }
                catch (final Exception ignored)
                {
                    // Ignore non-report JSON files
                }
            }
        }
    }

    /**
     * Generates standalone HTML document content for the test execution index.
     *
     * @param entries the list of test index entries (expected in reverse-chronological order)
     * @return the complete HTML string
     */
    public String generateIndexHtml(final List<IndexEntry> entries)
    {
        final List<IndexEntry> safeEntries = entries != null ? entries : Collections.emptyList();

        int passed = 0;
        int healed = 0;
        int failed = 0;
        int skipped = 0;
        long totalDurationMs = 0;
        long totalTokens = 0;
        double totalCostUsd = 0.0;
        int totalLlmCalls = 0;

        final Map<String, Integer> modeCounts = new LinkedHashMap<>();
        for (final IndexEntry e : safeEntries)
        {
            final String st = e.getStatus() != null ? e.getStatus().toUpperCase() : "";
            if (st.contains("FAIL"))
            {
                failed++;
            }
            else if (st.contains("HEAL") || (e.getHealedSteps() > 0 && (st.contains("PASS") || st.contains("SUCC"))))
            {
                healed++;
            }
            else if (st.contains("PASS") || st.contains("SUCC"))
            {
                passed++;
            }
            else if (st.contains("SKIP"))
            {
                skipped++;
            }

            if (e.getExecutionMode() != null && !e.getExecutionMode().isBlank())
            {
                final String modeName = e.getExecutionMode().trim();
                modeCounts.put(modeName, modeCounts.getOrDefault(modeName, 0) + 1);
            }

            totalDurationMs += e.getDurationMs();
            totalTokens += e.getTotalTokens();
            totalCostUsd += e.getEstimatedCostUsd();
            totalLlmCalls += e.getLlmCalls();
        }

        final int totalTests = safeEntries.size();
        final String lastUpdatedStr = DATE_FORMAT.format(new Date());

        final StringBuilder sb = new StringBuilder(100000);
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        sb.append("  <meta charset=\"UTF-8\">\n");
        sb.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("  <title>Neodymium Aura • AI Test Execution Index</title>\n");
        sb.append("  <style>\n");
        appendCss(sb);
        sb.append("  </style>\n");
        sb.append("</head>\n<body>\n");

        // Top Navigation Bar
        sb.append("  <nav class=\"top-nav\">\n");
        sb.append("    <div class=\"nav-brand\">\n");
        sb.append("      <span class=\"brand-icon\">⚡</span>\n");
        sb.append("      <span class=\"brand-title\">Neodymium Aura</span>\n");
        sb.append("      <span class=\"brand-subtitle\">• AI Test Execution Index</span>\n");
        sb.append("    </div>\n");
        sb.append("    <div class=\"nav-meta\">\n");
        sb.append("      <span class=\"meta-updated\">🕒 Last Updated: ").append(lastUpdatedStr).append("</span>\n");
        sb.append("      <button class=\"btn-refresh\" onclick=\"window.location.reload()\" title=\"Reload index\">🔄 Refresh</button>\n");
        sb.append("    </div>\n");
        sb.append("  </nav>\n");

        sb.append("  <main class=\"container\">\n");

        // KPI Summary Ribbon
        sb.append("    <section class=\"kpi-grid\">\n");
        sb.append("      <div class=\"kpi-card\">\n");
        sb.append("        <div class=\"kpi-label\">Total Executions</div>\n");
        sb.append("        <div class=\"kpi-value\">").append(NUMBER_FORMAT.format(totalTests)).append("</div>\n");
        sb.append("        <div class=\"kpi-sub\">Completed test runs</div>\n");
        sb.append("      </div>\n");

        sb.append("      <div class=\"kpi-card card-passed\">\n");
        sb.append("        <div class=\"kpi-label\">Passed</div>\n");
        sb.append("        <div class=\"kpi-value text-success\">").append(NUMBER_FORMAT.format(passed)).append("</div>\n");
        sb.append("        <div class=\"kpi-sub\">Direct passes</div>\n");
        sb.append("      </div>\n");

        sb.append("      <div class=\"kpi-card card-healed\">\n");
        sb.append("        <div class=\"kpi-label\">Healed</div>\n");
        sb.append("        <div class=\"kpi-value text-healed\">").append(NUMBER_FORMAT.format(healed)).append("</div>\n");
        sb.append("        <div class=\"kpi-sub\">Self-healed</div>\n");
        sb.append("      </div>\n");

        sb.append("      <div class=\"kpi-card card-failed\">\n");
        sb.append("        <div class=\"kpi-label\">Failed</div>\n");
        sb.append("        <div class=\"kpi-value text-fail\">").append(NUMBER_FORMAT.format(failed)).append("</div>\n");
        sb.append("        <div class=\"kpi-sub\">Broken tests</div>\n");
        sb.append("      </div>\n");

        sb.append("      <div class=\"kpi-card\">\n");
        sb.append("        <div class=\"kpi-label\">Total Duration</div>\n");
        sb.append("        <div class=\"kpi-value\">").append(formatDuration(totalDurationMs)).append("</div>\n");
        sb.append("        <div class=\"kpi-sub\">Cumulative runtime</div>\n");
        sb.append("      </div>\n");

        sb.append("      <div class=\"kpi-card\">\n");
        sb.append("        <div class=\"kpi-label\">Total LLM Cost</div>\n");
        sb.append("        <div class=\"kpi-value\">").append(COST_FORMAT.format(totalCostUsd)).append("</div>\n");
        sb.append("        <div class=\"kpi-sub\">").append(NUMBER_FORMAT.format(totalLlmCalls)).append(" calls (").append(NUMBER_FORMAT.format(totalTokens)).append(" tokens)</div>\n");
        sb.append("      </div>\n");
        sb.append("    </section>\n");

        // Filter & Search Toolbar
        sb.append("    <section class=\"toolbar-section\">\n");
        sb.append("      <div class=\"toolbar-controls\">\n");
        sb.append("        <div class=\"filter-tabs\">\n");
        sb.append("          <button class=\"filter-tab active\" onclick=\"filterByStatus('ALL')\">All (").append(totalTests).append(")</button>\n");
        sb.append("          <button class=\"filter-tab tab-pass\" onclick=\"filterByStatus('PASSED')\">Passed (").append(passed).append(")</button>\n");
        sb.append("          <button class=\"filter-tab tab-heal\" onclick=\"filterByStatus('HEALED')\">Healed (").append(healed).append(")</button>\n");
        sb.append("          <button class=\"filter-tab tab-fail\" onclick=\"filterByStatus('FAILED')\">Failed (").append(failed).append(")</button>\n");
        if (skipped > 0)
        {
            sb.append("          <button class=\"filter-tab tab-skip\" onclick=\"filterByStatus('SKIPPED')\">Skipped (").append(skipped).append(")</button>\n");
        }
        sb.append("        </div>\n");

        sb.append("        <div class=\"filter-mode-group\">\n");
        sb.append("          <label for=\"modeFilter\" class=\"filter-label\">⚙️ Mode:</label>\n");
        sb.append("          <select id=\"modeFilter\" class=\"mode-select\" onchange=\"filterByMode(this.value)\">\n");
        sb.append("            <option value=\"ALL\">All Modes (").append(totalTests).append(")</option>\n");
        for (final Map.Entry<String, Integer> modeEntry : modeCounts.entrySet())
        {
            final String modeName = modeEntry.getKey();
            final int count = modeEntry.getValue();
            final String icon = modeName.contains("FORCE") ? "🔴"
                : modeName.contains("STRICT") ? "🟢"
                : modeName.contains("HEAL") ? "✨"
                : modeName.contains("RECORD") ? "📹" : "⚙️";
            sb.append("            <option value=\"").append(escapeAttr(modeName)).append("\">")
              .append(icon).append(" ").append(escapeHtml(modeName)).append(" (").append(count).append(")</option>\n");
        }
        sb.append("          </select>\n");
        sb.append("        </div>\n");
        sb.append("      </div>\n");

        sb.append("      <div class=\"search-wrapper\">\n");
        sb.append("        <input type=\"text\" id=\"searchInput\" placeholder=\"🔍 Search test, class, dataset...\" oninput=\"applyFilter()\" />\n");
        sb.append("      </div>\n");
        sb.append("    </section>\n");

        // Main Executions Table
        sb.append("    <section class=\"table-container\">\n");
        sb.append("      <table class=\"executions-table\" id=\"executionsTable\">\n");
        sb.append("        <thead>\n");
        sb.append("          <tr>\n");
        sb.append("            <th onclick=\"sortTable(0)\" style=\"cursor:pointer; width:90px;\">Timestamp ⬍</th>\n");
        sb.append("            <th onclick=\"sortTable(1)\" style=\"cursor:pointer; width:85px;\">Status ⬍</th>\n");
        sb.append("            <th onclick=\"sortTable(2)\" style=\"cursor:pointer;\">Test Case & Details ⬍</th>\n");
        sb.append("            <th style=\"width:100px;\">Mode</th>\n");
        sb.append("            <th onclick=\"sortTable(4)\" style=\"cursor:pointer; width:60px;\">Steps ⬍</th>\n");
        sb.append("            <th onclick=\"sortTable(5)\" style=\"cursor:pointer; width:75px;\">Duration ⬍</th>\n");
        sb.append("            <th style=\"width:105px;\">AI Usage</th>\n");
        sb.append("            <th style=\"width:40px; text-align:center;\">Report</th>\n");
        sb.append("          </tr>\n");
        sb.append("        </thead>\n");
        sb.append("        <tbody>\n");

        if (safeEntries.isEmpty())
        {
            sb.append("          <tr class=\"empty-row\"><td colspan=\"8\">No test execution reports found in this directory.</td></tr>\n");
        }
        else
        {
            for (final IndexEntry entry : safeEntries)
            {
                final String st = entry.getStatus() != null ? entry.getStatus().toUpperCase() : "UNKNOWN";
                final String statusCategory = (st.contains("FAIL")) ? "FAILED"
                    : (st.contains("HEAL") || (entry.getHealedSteps() > 0 && (st.contains("PASS") || st.contains("SUCC")))) ? "HEALED"
                    : (st.contains("PASS") || st.contains("SUCC")) ? "PASSED"
                    : (st.contains("SKIP")) ? "SKIPPED" : "OTHER";

                final String pillClass = "PASSED".equals(statusCategory) ? "pill-pass"
                    : "HEALED".equals(statusCategory) ? "pill-heal"
                    : "FAILED".equals(statusCategory) ? "pill-fail"
                    : "SKIPPED".equals(statusCategory) ? "pill-skip" : "pill-pending";

                final String statusEmoji = "PASSED".equals(statusCategory) ? "✅"
                    : "HEALED".equals(statusCategory) ? "✨"
                    : "FAILED".equals(statusCategory) ? "❌"
                    : "SKIPPED".equals(statusCategory) ? "⏭️" : "⏳";

                final String formattedDate = DATE_PART_FORMAT.format(new Date(entry.getTimestamp()));
                final String formattedTime = TIME_PART_FORMAT.format(new Date(entry.getTimestamp()));
                final String testTitle = entry.getTestName() != null ? entry.getTestName()
                    : (entry.getTestClass() != null ? extractSimpleClassName(entry.getTestClass()) + "." + (entry.getTestMethod() != null ? entry.getTestMethod() : "test") : entry.getBaseFileName());
                final String modeStr = entry.getExecutionMode() != null ? entry.getExecutionMode().trim() : "";

                sb.append("          <tr class=\"execution-row\" data-status=\"").append(statusCategory)
                  .append("\" data-mode=\"").append(escapeAttr(modeStr))
                  .append("\" data-search=\"").append(escapeAttr((testTitle + " " + entry.getTestClass() + " " + entry.getTestMethod() + " " + entry.getDatasetId() + " " + modeStr + " " + entry.getFailureReason()).toLowerCase()))
                  .append("\">\n");

                // 0. Timestamp (Compact 2-line layout)
                sb.append("            <td data-sort=\"").append(entry.getTimestamp()).append("\">\n");
                sb.append("              <div class=\"timestamp-date\">").append(formattedDate).append("</div>\n");
                sb.append("              <div class=\"timestamp-time\">").append(formattedTime).append("</div>\n");
                sb.append("            </td>\n");

                // 1. Status Column
                sb.append("            <td><span class=\"status-pill ").append(pillClass).append("\">").append(statusEmoji).append(" ").append(escapeHtml(st)).append("</span></td>\n");

                // 2. Test Case Column
                sb.append("            <td>\n");
                sb.append("              <div class=\"test-name-line\">\n");
                sb.append("                <a href=\"").append(escapeAttr(entry.getHtmlFileName())).append("\" class=\"test-title-link\">").append(escapeHtml(testTitle)).append("</a>\n");
                if (entry.getDatasetId() != null && !entry.getDatasetId().isBlank() && !"default".equalsIgnoreCase(entry.getDatasetId()))
                {
                    sb.append("                <span class=\"dataset-pill\">").append(escapeHtml(entry.getDatasetId())).append("</span>\n");
                }
                if (entry.isBug())
                {
                    final String tip = entry.getBugDetails() != null ? "Expected bug: " + escapeHtml(entry.getBugDetails()) : "Expected bug";
                    sb.append("                <span class=\"bug-badge\" title=\"").append(escapeAttr(tip)).append("\">🐛 BUG</span>\n");
                }
                sb.append("              </div>\n");
                if (entry.getTestClass() != null)
                {
                    final String simpleClass = extractSimpleClassName(entry.getTestClass());
                    final String methodPart = entry.getTestMethod() != null ? "#" + entry.getTestMethod() : "";
                    final String simpleMeta = simpleClass + methodPart;
                    final String fullMeta = entry.getTestClass() + methodPart;
                    sb.append("              <div class=\"test-meta-line\" title=\"").append(escapeAttr(fullMeta)).append("\">").append(escapeHtml(simpleMeta)).append("</div>\n");
                }
                if (entry.getFailureReason() != null && !entry.getFailureReason().isBlank())
                {
                    sb.append("              <div class=\"failure-reason-snip\">⚠️ ").append(escapeHtml(entry.getFailureReason())).append("</div>\n");
                }
                sb.append("            </td>\n");

                // 3. Execution Mode
                sb.append("            <td>\n");
                if (entry.getExecutionMode() != null && !entry.getExecutionMode().isBlank())
                {
                    sb.append("              <span class=\"mode-badge\" onclick=\"filterByMode('").append(escapeAttr(entry.getExecutionMode())).append("')\" style=\"cursor:pointer;\" title=\"Click to filter by mode: ").append(escapeAttr(entry.getExecutionMode())).append("\">").append(escapeHtml(entry.getExecutionMode())).append("</span>\n");
                }
                else
                {
                    sb.append("              <span class=\"mode-badge mode-muted\">-</span>\n");
                }
                sb.append("            </td>\n");

                // 4. Steps Count
                sb.append("            <td data-sort=\"").append(entry.getStepCount()).append("\">\n");
                sb.append("              <span class=\"steps-count\">").append(entry.getStepCount()).append("</span>\n");
                if (entry.getHealedSteps() > 0 || entry.getFailedSteps() > 0)
                {
                    sb.append("              <div class=\"steps-sub\">");
                    if (entry.getHealedSteps() > 0)
                    {
                        sb.append("<span class=\"text-healed\">✨ ").append(entry.getHealedSteps()).append(" healed</span> ");
                    }
                    if (entry.getFailedSteps() > 0)
                    {
                        sb.append("<span class=\"text-fail\">❌ ").append(entry.getFailedSteps()).append(" failed</span>");
                    }
                    sb.append("</div>\n");
                }
                sb.append("            </td>\n");

                // 5. Duration
                sb.append("            <td data-sort=\"").append(entry.getDurationMs()).append("\">").append(escapeHtml(formatDuration(entry.getDurationMs()))).append("</td>\n");

                // 6. AI / LLM Usage
                sb.append("            <td>\n");
                if (entry.getLlmCalls() > 0 || entry.getTotalTokens() > 0)
                {
                    sb.append("              <div class=\"llm-stat\">🤖 ").append(entry.getLlmCalls()).append(" call(s)</div>\n");
                    sb.append("              <div class=\"llm-tokens\">").append(NUMBER_FORMAT.format(entry.getTotalTokens())).append(" toks <span class=\"llm-cost\">(").append(COST_FORMAT.format(entry.getEstimatedCostUsd())).append(")</span></div>\n");
                }
                else
                {
                    sb.append("              <span class=\"llm-muted\">Replayed / 0 LLM</span>\n");
                }
                sb.append("            </td>\n");

                // 7. Action Links (Icon-only report button)
                sb.append("            <td style=\"text-align:center;\">\n");
                sb.append("              <a href=\"").append(escapeAttr(entry.getHtmlFileName())).append("\" class=\"btn-open-report\" title=\"Open detailed report for ").append(escapeAttr(testTitle)).append("\" aria-label=\"Open Report\">📊</a>\n");
                sb.append("            </td>\n");

                sb.append("          </tr>\n");
            }
        }

        sb.append("        </tbody>\n");
        sb.append("      </table>\n");
        sb.append("    </section>\n");

        // Footer
        sb.append("    <footer class=\"report-footer\">\n");
        sb.append("      Neodymium Aura AI Test Automation Framework &bull; Built by Xceptance GmbH 2026\n");
        sb.append("    </footer>\n");

        sb.append("  </main>\n");

        // Embedded Client Script for Search, Status Filtering, and Sorting
        sb.append("  <script>\n");
        appendClientScript(sb);
        sb.append("  </script>\n");

        sb.append("</body>\n</html>\n");
        return sb.toString();
    }

    private static String formatDuration(final long durationMs)
    {
        if (durationMs < 1000)
        {
            return durationMs + " ms";
        }
        final double seconds = durationMs / 1000.0;
        if (seconds < 60.0)
        {
            return String.format(Locale.US, "%.1f s", seconds);
        }
        final long minutes = durationMs / 60000;
        final double remainingSecs = (durationMs % 60000) / 1000.0;
        return String.format(Locale.US, "%dm %.1fs", minutes, remainingSecs);
    }

    private static String extractSimpleClassName(final String fqcn)
    {
        if (fqcn == null) return "TestClass";
        final int lastDot = fqcn.lastIndexOf('.');
        return lastDot >= 0 ? fqcn.substring(lastDot + 1) : fqcn;
    }

    private static String escapeHtml(final String text)
    {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    private static String escapeAttr(final String text)
    {
        return escapeHtml(text);
    }

    private static void appendCss(final StringBuilder sb)
    {
        sb.append("""
            :root {
                --font-sans: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                --font-mono: 'JetBrains Mono', 'Fira Code', Menlo, Monaco, Consolas, monospace;
                --bg: #f8fafc;
                --card-bg: #ffffff;
                --text: #0f172a;
                --text-muted: #64748b;
                --text-sub: #475569;
                --border: #e2e8f0;
                --primary: #0284c7;
                --primary-light: #e0f2fe;
                --success: #16a34a;
                --success-bg: #f0fdf4;
                --success-border: #bbf7d0;
                --healed: #9333ea;
                --healed-bg: #faf5ff;
                --healed-border: #e9d5ff;
                --fail: #dc2626;
                --fail-bg: #fef2f2;
                --fail-border: #fecaca;
                --skip: #64748b;
                --skip-bg: #f1f5f9;
                --shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
                --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1);
            }
            * { box-sizing: border-box; margin: 0; padding: 0; }
            body {
                font-family: var(--font-sans);
                background-color: var(--bg);
                color: var(--text);
                line-height: 1.5;
                font-size: 14px;
                -webkit-font-smoothing: antialiased;
            }
            .top-nav {
                background: #0f172a;
                color: #ffffff;
                padding: 1rem 2rem;
                display: flex;
                justify-content: space-between;
                align-items: center;
                box-shadow: var(--shadow-md);
                position: sticky;
                top: 0;
                z-index: 100;
            }
            .nav-brand {
                display: flex;
                align-items: center;
                gap: 0.6rem;
            }
            .brand-icon { font-size: 1.4rem; }
            .brand-title { font-size: 1.25rem; font-weight: 700; letter-spacing: -0.02em; color: #f8fafc; }
            .brand-subtitle { font-size: 0.95rem; font-weight: 400; color: #94a3b8; }
            .nav-meta {
                display: flex;
                align-items: center;
                gap: 1rem;
            }
            .meta-updated { font-size: 0.85rem; color: #cbd5e1; font-family: var(--font-mono); }
            .btn-refresh {
                background: #1e293b;
                color: #f8fafc;
                border: 1px solid #334155;
                padding: 0.35rem 0.8rem;
                border-radius: 6px;
                cursor: pointer;
                font-weight: 600;
                font-size: 0.85rem;
                transition: background 0.15s;
            }
            .btn-refresh:hover { background: #334155; }
            .container {
                max-width: 1600px;
                width: 100%;
                margin: 1.25rem auto;
                padding: 0 1.25rem;
            }
            .kpi-grid {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
                gap: 1rem;
                margin-bottom: 1.5rem;
            }
            .kpi-card {
                background: var(--card-bg);
                border: 1px solid var(--border);
                border-radius: 10px;
                padding: 1.1rem;
                box-shadow: var(--shadow-sm);
                display: flex;
                flex-direction: column;
                justify-content: space-between;
            }
            .kpi-label { font-size: 0.8rem; font-weight: 600; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 0.3rem; }
            .kpi-value { font-size: 1.85rem; font-weight: 800; letter-spacing: -0.03em; line-height: 1.1; margin-bottom: 0.3rem; }
            .kpi-sub { font-size: 0.75rem; color: var(--text-muted); }
            .text-success { color: var(--success); }
            .text-healed { color: var(--healed); }
            .text-fail { color: var(--fail); }
            .card-passed { border-top: 3px solid var(--success); }
            .card-healed { border-top: 3px solid var(--healed); }
            .card-failed { border-top: 3px solid var(--fail); }
            .toolbar-section {
                display: flex;
                justify-content: space-between;
                align-items: center;
                flex-wrap: wrap;
                gap: 1rem;
                margin-bottom: 1rem;
            }
            .toolbar-controls {
                display: flex;
                align-items: center;
                gap: 0.75rem;
                flex-wrap: wrap;
            }
            .filter-mode-group {
                display: flex;
                align-items: center;
                gap: 0.4rem;
                background: #f1f5f9;
                padding: 0.25rem 0.55rem;
                border-radius: 8px;
                border: 1px solid var(--border);
            }
            .filter-label {
                font-size: 0.8rem;
                font-weight: 700;
                color: var(--text-sub);
                user-select: none;
                white-space: nowrap;
            }
            .mode-select {
                background: var(--card-bg);
                color: var(--text);
                border: 1px solid var(--border);
                border-radius: 6px;
                padding: 0.35rem 0.65rem;
                font-size: 0.82rem;
                font-weight: 600;
                outline: none;
                cursor: pointer;
                transition: all 0.15s;
            }
            .mode-select:focus {
                border-color: var(--primary);
                box-shadow: 0 0 0 2px rgba(2, 132, 199, 0.15);
            }
            .filter-tabs {
                display: flex;
                gap: 0.4rem;
                background: #f1f5f9;
                padding: 0.3rem;
                border-radius: 8px;
                border: 1px solid var(--border);
            }
            .filter-tab {
                background: transparent;
                border: none;
                padding: 0.4rem 0.9rem;
                border-radius: 6px;
                font-size: 0.85rem;
                font-weight: 600;
                color: var(--text-sub);
                cursor: pointer;
                transition: all 0.15s;
            }
            .filter-tab:hover { background: #e2e8f0; color: var(--text); }
            .filter-tab.active {
                background: var(--card-bg);
                color: var(--text);
                box-shadow: 0 1px 3px rgba(0,0,0,0.1);
            }
            .filter-tab.tab-pass.active { color: var(--success); }
            .filter-tab.tab-heal.active { color: var(--healed); }
            .filter-tab.tab-fail.active { color: var(--fail); }
            .search-wrapper { flex: 1; max-width: 380px; }
            .search-wrapper input {
                width: 100%;
                padding: 0.55rem 1rem;
                font-size: 0.85rem;
                border: 1px solid var(--border);
                border-radius: 8px;
                background: var(--card-bg);
                outline: none;
                transition: border-color 0.15s;
            }
            .search-wrapper input:focus {
                border-color: var(--primary);
                box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.15);
            }
            .table-container {
                background: var(--card-bg);
                border: 1px solid var(--border);
                border-radius: 10px;
                overflow-x: auto;
                -webkit-overflow-scrolling: touch;
                box-shadow: var(--shadow-sm);
            }
            .executions-table {
                width: 100%;
                border-collapse: collapse;
                text-align: left;
                table-layout: auto;
            }
            .executions-table th {
                background: #f8fafc;
                padding: 0.5rem 0.6rem;
                font-size: 0.72rem;
                font-weight: 700;
                color: var(--text-muted);
                text-transform: uppercase;
                letter-spacing: 0.05em;
                border-bottom: 1px solid var(--border);
                user-select: none;
                white-space: nowrap;
            }
            .executions-table td {
                padding: 0.5rem 0.6rem;
                border-bottom: 1px solid var(--border);
                vertical-align: middle;
            }
            .executions-table tr:last-child td { border-bottom: none; }
            .executions-table tr.execution-row:hover {
                background-color: #f8fafc;
            }
            .status-pill {
                display: inline-flex;
                align-items: center;
                gap: 0.25rem;
                padding: 0.2rem 0.45rem;
                border-radius: 20px;
                font-size: 0.70rem;
                font-weight: 700;
                letter-spacing: 0.02em;
                white-space: nowrap;
            }
            .pill-pass { background: var(--success-bg); color: var(--success); border: 1px solid var(--success-border); }
            .pill-heal { background: var(--healed-bg); color: var(--healed); border: 1px solid var(--healed-border); }
            .pill-fail { background: var(--fail-bg); color: var(--fail); border: 1px solid var(--fail-border); }
            .pill-skip { background: var(--skip-bg); color: var(--skip); border: 1px solid var(--border); }
            .pill-pending { background: #fffbeb; color: #b45309; border: 1px solid #fde68a; }
            .test-name-line {
                display: flex;
                align-items: center;
                gap: 0.4rem;
                flex-wrap: wrap;
                margin-bottom: 0.2rem;
            }
            .test-title-link {
                color: var(--text);
                font-weight: 700;
                font-size: 0.85rem;
                text-decoration: none;
                transition: color 0.15s;
                word-break: break-word;
            }
            .test-title-link:hover {
                color: var(--primary);
                text-decoration: underline;
            }
            .dataset-pill {
                background: #f1f5f9;
                color: var(--text-sub);
                font-size: 0.7rem;
                font-weight: 600;
                padding: 0.1rem 0.4rem;
                border-radius: 4px;
                border: 1px solid var(--border);
            }
            .bug-badge {
                background: #fef2f2;
                color: #b91c1c;
                border: 1px solid #fca5a5;
                font-size: 0.7rem;
                font-weight: 700;
                padding: 0.1rem 0.35rem;
                border-radius: 4px;
            }
            .test-meta-line {
                font-family: var(--font-mono);
                font-size: 0.72rem;
                color: var(--text-muted);
                max-width: 400px;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
            }
            .failure-reason-snip {
                font-size: 0.75rem;
                color: var(--fail);
                margin-top: 0.2rem;
                max-width: 350px;
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
            }
            .mode-badge {
                display: inline-block;
                background: #f1f5f9;
                color: var(--text-sub);
                font-size: 0.7rem;
                font-weight: 700;
                padding: 0.15rem 0.45rem;
                border-radius: 4px;
                border: 1px solid var(--border);
                white-space: nowrap;
                transition: transform 0.1s, box-shadow 0.1s;
            }
            .mode-badge:hover {
                transform: translateY(-1px);
                box-shadow: 0 2px 4px rgba(0,0,0,0.08);
            }
            .mode-muted { color: var(--text-muted); opacity: 0.7; }
            .steps-count { font-weight: 700; font-size: 0.82rem; }
            .steps-sub { font-size: 0.7rem; margin-top: 0.1rem; }
            .llm-stat { font-weight: 600; font-size: 0.78rem; color: var(--text); }
            .llm-tokens { font-size: 0.7rem; color: var(--text-muted); }
            .llm-cost { font-weight: 600; color: #0284c7; }
            .llm-muted { font-size: 0.72rem; color: var(--text-muted); font-style: italic; }
            .timestamp-date { font-weight: 600; font-size: 0.8rem; }
            .timestamp-time { font-size: 0.72rem; color: var(--text-muted); font-family: var(--font-mono); }
            .btn-open-report {
                display: inline-flex;
                align-items: center;
                justify-content: center;
                width: 2rem;
                height: 2rem;
                background: #f1f5f9;
                color: var(--text-sub);
                border-radius: 6px;
                font-size: 0.95rem;
                text-decoration: none;
                border: 1px solid var(--border);
                transition: all 0.15s ease;
                user-select: none;
            }
            .btn-open-report:hover {
                background: var(--primary);
                color: #ffffff;
                border-color: var(--primary);
                box-shadow: 0 2px 6px rgba(2, 132, 199, 0.3);
                transform: translateY(-1px);
            }
            .empty-row td {
                text-align: center;
                padding: 3rem 1rem;
                color: var(--text-muted);
                font-size: 0.95rem;
            }
            .report-footer {
                text-align: center;
                padding: 2rem 0;
                color: var(--text-muted);
                font-size: 0.8rem;
                border-top: 1px solid var(--border);
                margin-top: 2rem;
            }
        """);
    }

    private static void appendClientScript(final StringBuilder sb)
    {
        sb.append("""
            var currentStatusFilter = 'ALL';
            var currentModeFilter = 'ALL';

            window.filterByStatus = function(status) {
                currentStatusFilter = status;
                document.querySelectorAll('.filter-tab').forEach(function(btn) {
                    btn.classList.remove('active');
                });
                if (status === 'ALL') document.querySelector('.filter-tab:nth-child(1)').classList.add('active');
                else if (status === 'PASSED') document.querySelector('.filter-tab.tab-pass').classList.add('active');
                else if (status === 'HEALED') document.querySelector('.filter-tab.tab-heal').classList.add('active');
                else if (status === 'FAILED') document.querySelector('.filter-tab.tab-fail').classList.add('active');
                else if (status === 'SKIPPED') {
                    var sk = document.querySelector('.filter-tab.tab-skip');
                    if (sk) sk.classList.add('active');
                }
                applyFilter();
            };

            window.filterByMode = function(mode) {
                currentModeFilter = mode || 'ALL';
                var sel = document.getElementById('modeFilter');
                if (sel && sel.value !== currentModeFilter) {
                    sel.value = currentModeFilter;
                }
                applyFilter();
            };

            window.applyFilter = function() {
                var search = (document.getElementById('searchInput').value || '').trim().toLowerCase();
                var rows = document.querySelectorAll('#executionsTable tbody tr.execution-row');

                rows.forEach(function(row) {
                    var rowStatus = row.getAttribute('data-status') || '';
                    var rowMode = row.getAttribute('data-mode') || '';
                    var rowSearch = row.getAttribute('data-search') || '';

                    var matchesStatus = (currentStatusFilter === 'ALL' || rowStatus === currentStatusFilter);
                    var matchesMode = (currentModeFilter === 'ALL' || rowMode === currentModeFilter);
                    var matchesSearch = (!search || rowSearch.indexOf(search) !== -1);

                    if (matchesStatus && matchesMode && matchesSearch) {
                        row.style.display = '';
                    } else {
                        row.style.display = 'none';
                    }
                });
            };

            var sortDirections = {};
            window.sortTable = function(colIndex) {
                var table = document.getElementById('executionsTable');
                var tbody = table.querySelector('tbody');
                var rows = Array.from(tbody.querySelectorAll('tr.execution-row'));
                if (rows.length <= 1) return;

                var isAsc = sortDirections[colIndex] = !sortDirections[colIndex];

                rows.sort(function(a, b) {
                    var aCell = a.children[colIndex];
                    var bCell = b.children[colIndex];

                    var aVal = aCell.getAttribute('data-sort') || aCell.textContent.trim();
                    var bVal = bCell.getAttribute('data-sort') || bCell.textContent.trim();

                    var aNum = parseFloat(aVal);
                    var bNum = parseFloat(bVal);

                    if (!isNaN(aNum) && !isNaN(bNum)) {
                        return isAsc ? (aNum - bNum) : (bNum - aNum);
                    }
                    return isAsc ? aVal.localeCompare(bVal) : bVal.localeCompare(aVal);
                });

                rows.forEach(function(row) {
                    tbody.appendChild(row);
                });
            };
        """);
    }
}
