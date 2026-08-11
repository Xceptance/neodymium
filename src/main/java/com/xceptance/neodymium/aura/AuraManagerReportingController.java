/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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
package com.xceptance.neodymium.aura;

import com.sun.net.httpserver.HttpExchange;
import com.xceptance.neodymium.aura.dto.DeleteReportRequest;
import com.xceptance.neodymium.aura.dto.RunRequest;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.Context;

/**
 * Controller handling reporting history serving, manual report compilation triggers, and report deletion.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class AuraManagerReportingController
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraManagerReportingController.class);

    private final AuraReportingService reportingService;
    private final AuraQueueService queueService;
    private final NeodymiumAuraManager manager;

    public AuraManagerReportingController(final AuraReportingService reportingService, final AuraQueueService queueService, final NeodymiumAuraManager manager)
    {
        this.reportingService = reportingService;
        this.queueService = queueService;
        this.manager = manager;
    }

    public void handleReportingHistory(final HttpExchange exchange) throws IOException
    {
        final List<Map<String, Object>> historyList = getHistoryList();
        final Context context = new Context();
        context.setVariable("history", historyList);
        context.setVariable("running", queueService.isRunningQueue());

        if (queueService.isRunningQueue())
        {
            final long runStartMs = queueService.getRunStartTimeMs();
            final java.time.Instant instant = java.time.Instant.ofEpochMilli(runStartMs);
            final java.time.LocalDateTime ldt = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
            final java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            context.setVariable("runningStartTime", ldt.format(formatter));
        }

        final String html = manager.getTemplateEngine().process("dashboard", Set.of("reportingHistoryList"), context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }

    public void handleReportingHistoryJson(final HttpExchange exchange) throws IOException
    {
        final List<Map<String, Object>> historyList = getHistoryList();
        AuraHttpUtils.sendJsonResponse(exchange, 200, AuraHttpUtils.gson.toJson(historyList));
    }

    public void handleReportingRun(final HttpExchange exchange) throws IOException
    {
        final Map<String, String> params = AuraHttpUtils.getRequestParams(exchange);
        final String id = params.get("id");

        final List<Map<String, Object>> historyList = getHistoryList();
        Map<String, Object> foundItem = null;
        for (final Map<String, Object> item : historyList)
        {
            if (id != null && id.equals(item.get("id")))
            {
                foundItem = item;
                break;
            }
        }

        final Context context = new Context();
        if (foundItem != null)
        {
            context.setVariable("tests", foundItem.get("tests"));
            context.setVariable("reportId", id);
            context.setVariable("selectedRunNumber", foundItem.get("runNumber"));
        }
        context.setVariable("running", queueService.isRunningQueue());

        final String html = manager.getTemplateEngine().process("dashboard", Set.of("historyTests"), context);
        AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }

    public void handleServeReportFile(final HttpExchange exchange) throws IOException
    {
        final String path = exchange.getRequestURI().getPath();
        final String prefix = "/api/reporting/report/";
        final String subPath = path.substring(prefix.length());
        final int slashIdx = subPath.indexOf('/');
        if (slashIdx == -1)
        {
            AuraHttpUtils.sendError(exchange, 404, "File not found");
            return;
        }
        final String reportId = subPath.substring(0, slashIdx);
        final String assetPath = subPath.substring(slashIdx + 1);
        try
        {
            final File file = reportingService.resolveReportAsset(reportId, assetPath);
            if (!file.exists() || !file.isFile())
            {
                AuraHttpUtils.sendError(exchange, 404, "File not found");
                return;
            }
            final byte[] bytes = Files.readAllBytes(file.toPath());
            final String contentType = AuraHttpUtils.getMimeType(file.getName());
            AuraHttpUtils.sendResponse(exchange, 200, contentType, bytes);
        }
        catch (final SecurityException se)
        {
            AuraHttpUtils.sendError(exchange, 403, se.getMessage());
        }
    }

    public void handleGenerateReporting(final HttpExchange exchange) throws IOException
    {
        if (queueService.isRunningQueue())
        {
            LOGGER.error("[Aura Server] Reporting compilation trigger failed: Queue is currently executing");
            AuraHttpUtils.sendError(exchange, 409, "Cannot compile report while queue is executing");
            return;
        }

        LOGGER.info("[Aura Server] Spawning thread to compile reporting files...");

        final String body = AuraHttpUtils.readBody(exchange);
        final RunRequest req = AuraHttpUtils.gson.fromJson(body, RunRequest.class);
        final Thread thread = new Thread(() -> reportingService.generateReport(List.of(), req, queueService.getRunStartTimeMs(), queueService.getGlobalTestsRun(), queueService.getGlobalPassed(), queueService.getGlobalFailed(), queueService.getGlobalSkipped(), queueService.isManuallyStopped(), queueService.getCurrentRunLogs(), queueService.getCurrentRunEvents()));
        thread.setName("NeodymiumAuraManualReportCompiler");
        thread.start();

        AuraHttpUtils.sendJsonResponse(exchange, 200, AuraHttpUtils.gson.toJson(Map.of("success", true)));
    }

    public void handleDeleteReport(final HttpExchange exchange) throws IOException
    {
        final Map<String, String> params = AuraHttpUtils.getRequestParams(exchange);
        final String id = params.get("id");

        if (id == null || id.isEmpty())
        {
            LOGGER.error("[Aura Server] Delete report request failed: Missing 'id'");
            AuraHttpUtils.sendError(exchange, 400, "Missing 'id'");
            return;
        }

        LOGGER.info("[Aura Server] POST /api/reporting/delete - Request received for ID: {}", id);

        try
        {
            final boolean deleted = reportingService.deleteReport(id);
            if (!deleted)
            {
                LOGGER.error("[Aura Server] Delete report request failed: Report not found for ID: {}", id);
                AuraHttpUtils.sendError(exchange, 404, "Report not found: " + id);
                return;
            }
            LOGGER.info("[Aura Server] Deleted report history directory: {}", id);

            // Rebuild history list and return the updated history fragment for HTMX OOB / in-place swap
            final List<Map<String, Object>> historyList = getHistoryList();
            final Context context = new Context();
            context.setVariable("history", historyList);
            context.setVariable("running", queueService.isRunningQueue());

            final String html = manager.getTemplateEngine().process("dashboard", Set.of("reportingHistoryList"), context);
            AuraHttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
        }
        catch (final SecurityException se)
        {
            LOGGER.error("[Aura Server] Directory traversal attempt detected: {}", id);
            AuraHttpUtils.sendError(exchange, 403, se.getMessage());
        }
    }

    public List<Map<String, Object>> getHistoryList() throws IOException
    {
        final File historyDir = reportingService.getReportHistoryDir();
        final List<Map<String, Object>> historyList = new ArrayList<>();

        if (historyDir.exists() && historyDir.isDirectory())
        {
            final File[] dirs = historyDir.listFiles(File::isDirectory);
            if (dirs != null)
            {
                Arrays.sort(dirs, (a, b) -> b.getName().compareTo(a.getName()));
                for (final File dir : dirs)
                {
                    String status = "Passed";
                    String timestamp = "";
                    String total = "-";
                    String passed = "-";
                    String failed = "-";
                    long durationMs = 0L;
                    boolean headless = false;
                    boolean allureEnabled = true;
                    boolean videoEnabled = false;
                    final File metadataFile = new File(dir, "metadata.json");
                    if (metadataFile.exists() && metadataFile.isFile())
                    {
                        try
                        {
                            final String meta = Files.readString(metadataFile.toPath(), StandardCharsets.UTF_8);
                            final Map<?, ?> map = AuraHttpUtils.gson.fromJson(meta, Map.class);
                            if (map != null)
                            {
                                if (map.containsKey("status"))
                                {
                                    status = String.valueOf(map.get("status"));
                                }
                                if (map.containsKey("timestamp"))
                                {
                                    timestamp = String.valueOf(map.get("timestamp"));
                                }
                                if (map.containsKey("total"))
                                {
                                    total = String.valueOf(
                                            Math.round(Double.parseDouble(String.valueOf(map.get("total")))));
                                }
                                if (map.containsKey("passed"))
                                {
                                    passed = String.valueOf(
                                            Math.round(Double.parseDouble(String.valueOf(map.get("passed")))));
                                }
                                if (map.containsKey("failed"))
                                {
                                    failed = String.valueOf(
                                            Math.round(Double.parseDouble(String.valueOf(map.get("failed")))));
                                }
                                if (map.containsKey("durationMs"))
                                {
                                    try
                                    {
                                        durationMs = Math.round(Double.parseDouble(String.valueOf(map.get("durationMs"))));
                                    }
                                    catch (final NumberFormatException ignore)
                                    {
                                        // keep default
                                    }
                                }
                                if (map.containsKey("headless"))
                                {
                                    headless = Boolean.parseBoolean(String.valueOf(map.get("headless")));
                                }
                                if (map.containsKey("allureEnabled"))
                                {
                                    allureEnabled = Boolean.parseBoolean(String.valueOf(map.get("allureEnabled")));
                                }
                                if (map.containsKey("videoEnabled"))
                                {
                                    videoEnabled = Boolean.parseBoolean(String.valueOf(map.get("videoEnabled")));
                                }
                            }
                        }
                        catch (final Exception e)
                        {
                            // ignore
                        }
                    }

                    Object runConfig = null;
                    if (metadataFile.exists() && metadataFile.isFile())
                    {
                        try
                        {
                            final String meta = Files.readString(metadataFile.toPath(), StandardCharsets.UTF_8);
                            final com.google.gson.JsonObject metaObj = com.google.gson.JsonParser
                                    .parseString(meta).getAsJsonObject();
                            if (metaObj.has("runConfig") && !metaObj.get("runConfig").isJsonNull())
                            {
                                final String runConfigRaw = AuraHttpUtils.gson.toJson(metaObj.get("runConfig"));
                                runConfig = AuraHttpUtils.gson.fromJson(runConfigRaw, Object.class);
                            }
                        }
                        catch (final Exception e)
                        {
                            // ignore
                        }
                    }

                    final File indexHtml = new File(new File(dir, "allure-report"), "index.html");
                    final boolean hasReport = indexHtml.exists() && indexHtml.isFile();

                    final List<Map<String, String>> tests = new ArrayList<>();
                    final File[] consoleFiles = dir
                            .listFiles((d, name) -> name.startsWith("console-execution") && name.endsWith(".json"));
                    boolean hasInteractiveReport = false;
                    if (consoleFiles != null)
                    {
                        for (final File cf : consoleFiles)
                        {
                            hasInteractiveReport = true;
                            String testName = cf.getName().substring("console-execution".length());
                            if (testName.startsWith("-"))
                            {
                                testName = testName.substring(1);
                            }
                            if (testName.endsWith(".json"))
                            {
                                testName = testName.substring(0, testName.length() - 5);
                            }
                            if (testName.isEmpty())
                            {
                                testName = "Default";
                            }

                            String testStatus = "Unknown";
                            final Map<String, String> testMap = new HashMap<>();

                            try
                            {
                                final String content = Files.readString(cf.toPath(), StandardCharsets.UTF_8);
                                @SuppressWarnings("unchecked")
                                final Map<String, Object> map = AuraHttpUtils.gson.fromJson(content, Map.class);
                                if (map != null)
                                {
                                    if (map.containsKey("testName"))
                                    {
                                        testName = String.valueOf(map.get("testName"));
                                    }
                                    if (map.containsKey("status"))
                                    {
                                        final String rawStatus = String.valueOf(map.get("status"));
                                        if ("Passed".equals(rawStatus) || "Failed".equals(rawStatus) || "aborted".equalsIgnoreCase(rawStatus))
                                        {
                                            testStatus = "aborted".equalsIgnoreCase(rawStatus) ? "Aborted" : rawStatus;
                                        }
                                        else
                                        {
                                            boolean hasFailedStep = false;
                                            if (map.containsKey("reasoningFailed")
                                                    && Boolean.TRUE.equals(map.get("reasoningFailed")))
                                            {
                                                hasFailedStep = true;
                                            }
                                            if (!hasFailedStep && map.containsKey("steps"))
                                            {
                                                @SuppressWarnings("unchecked")
                                                final List<Map<String, Object>> steps =
                                                        (List<Map<String, Object>>) map.get("steps");
                                                if (steps != null)
                                                {
                                                    for (final Map<String, Object> step : steps)
                                                    {
                                                        if ("failed".equals(step.get("status")))
                                                        {
                                                            hasFailedStep = true;
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                            testStatus = hasFailedStep ? "Failed" : "Passed";
                                        }
                                    }
                                    else
                                    {
                                        boolean hasFailedStep = false;
                                        if (map.containsKey("reasoningFailed")
                                                && Boolean.TRUE.equals(map.get("reasoningFailed")))
                                        {
                                            hasFailedStep = true;
                                        }
                                        if (!hasFailedStep && map.containsKey("steps"))
                                        {
                                            @SuppressWarnings("unchecked")
                                            final List<Map<String, Object>> steps = (List<Map<String, Object>>) map
                                                    .get("steps");
                                            if (steps != null)
                                            {
                                                for (final Map<String, Object> step : steps)
                                                {
                                                    if ("failed".equals(step.get("status")))
                                                    {
                                                        hasFailedStep = true;
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                        testStatus = hasFailedStep ? "Failed" : "Passed";
                                    }
                                    if (map.containsKey("browser"))
                                    {
                                        testMap.put("browser", String.valueOf(map.get("browser")));
                                    }
                                    if (map.containsKey("stats"))
                                    {
                                        @SuppressWarnings("unchecked")
                                        final Map<String, Object> stats = (Map<String, Object>) map.get("stats");
                                        if (stats != null && stats.containsKey("durationMs"))
                                        {
                                            testMap.put("durationMs", String.valueOf(stats.get("durationMs")));
                                        }
                                    }
                                    if (map.containsKey("testId") && map.get("testId") != null)
                                    {
                                        testMap.put("testId", String.valueOf(map.get("testId")));
                                    }
                                    if ((map.containsKey("playbookFile") && map.get("playbookFile") != null) || (map.containsKey("yamlSource") && map.get("yamlSource") != null))
                                    {
                                        final String src = String.valueOf(map.get("playbookFile") != null ? map.get("playbookFile") : map.get("yamlSource"));
                                        final String base = new File(src).getName();
                                        testMap.put("yamlLabel", base);
                                    }
                                    if (map.containsKey("playbookMode") && map.get("playbookMode") != null)
                                    {
                                        testMap.put("playbookMode", String.valueOf(map.get("playbookMode")));
                                    }
                                }
                            }
                            catch (final Exception e)
                            {
                                // ignore
                            }

                            testMap.put("name", testName);

                            String displayName = testName;
                            if (displayName.contains("."))
                            {
                                displayName = displayName.substring(displayName.lastIndexOf('.') + 1);
                            }
                            final String yamlLabel = testMap.get("yamlLabel");
                            final String testId = testMap.get("testId");
                            if (yamlLabel != null && testId != null && !testId.isEmpty())
                            {
                                displayName = yamlLabel + " (" + testId + ")";
                            }
                            else if (testId != null && !testId.isEmpty())
                            {
                                displayName = testId;
                            }
                            else if (yamlLabel != null && !yamlLabel.isEmpty())
                            {
                                displayName = yamlLabel;
                            }
                            testMap.put("displayName", displayName);

                            testMap.put("file", cf.getName());
                            testMap.put("status", testStatus);
                            final String safeLogName = testName.replaceAll("[^a-zA-Z0-9_\\-]", "_") + ".log";
                            testMap.put("hasLog", String.valueOf(new File(dir, safeLogName).exists()));

                            // Calculate test case duration label
                            String testDurationLabel = "";
                            if (testMap.containsKey("durationMs"))
                            {
                                try
                                {
                                    final double dMs = Double.parseDouble(testMap.get("durationMs"));
                                    testDurationLabel = String.format("%.1fs", dMs / 1000.0);
                                }
                                catch (final Exception ignore)
                                {
                                    // ignore
                                }
                            }
                            testMap.put("durationLabel", testDurationLabel);

                            tests.add(testMap);
                        }
                    }

                    final Map<String, Object> item = new HashMap<>();
                    item.put("id", dir.getName());
                    item.put("status", status);
                    item.put("timestamp", timestamp);
                    item.put("total", total);
                    item.put("passed", passed);
                    item.put("failed", failed);
                    item.put("durationMs", durationMs);
                    item.put("headless", headless);
                    item.put("allureEnabled", allureEnabled);
                    item.put("videoEnabled", videoEnabled);
                    item.put("hasReport", hasReport);
                    item.put("hasInteractiveReport", hasInteractiveReport);
                    item.put("tests", tests);
                    if (runConfig != null)
                    {
                        item.put("runConfig", runConfig);
                    }
                    historyList.add(item);
                }
            }
        }

        final int total2 = historyList.size();
        for (int idx = 0; idx < total2; idx++)
        {
            final Map<String, Object> item = historyList.get(idx);
            item.put("runNumber", total2 - idx);

            // Compute formatted timestamp
            final String timestamp = String.valueOf(item.get("timestamp"));
            try
            {
                final java.time.Instant instant = java.time.Instant.parse(timestamp);
                final java.time.LocalDateTime ldt = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
                final java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                item.put("formattedTime", ldt.format(formatter));
            }
            catch (final Exception e)
            {
                item.put("formattedTime", timestamp != null && !timestamp.isEmpty() ? timestamp : String.valueOf(item.get("id")));
            }

            // Compute run duration label
            final long durationMs = (long) item.get("durationMs");
            String durationLabel = "";
            if (durationMs > 0L)
            {
                final long durSec = Math.round(durationMs / 1000.0);
                if (durSec < 60)
                {
                    durationLabel = durSec + "s";
                }
                else
                {
                    durationLabel = (durSec / 60) + "m " + (durSec % 60) + "s";
                }
            }
            item.put("durationLabel", durationLabel);

            // Compute run config JSON and test case rerun payloads
            if (item.containsKey("runConfig") && item.get("runConfig") != null)
            {
                final String escapedJson = AuraHttpUtils.gson.toJson(item.get("runConfig")).replace("'", "\\'");
                item.put("runConfigJson", escapedJson);

                @SuppressWarnings("unchecked")
                final Map<String, Object> rc = (Map<String, Object>) item.get("runConfig");
                @SuppressWarnings("unchecked")
                final List<Map<String, String>> tests = (List<Map<String, String>>) item.get("tests");

                if (tests != null && rc.containsKey("datasets"))
                {
                    @SuppressWarnings("unchecked")
                    final List<Map<String, Object>> datasets = (List<Map<String, Object>>) rc.get("datasets");
                    if (datasets != null)
                    {
                        for (final Map<String, String> testMap : tests)
                        {
                            Map<String, Object> matchedDataset = null;
                            final String yamlLabel = testMap.get("yamlLabel");
                            if (yamlLabel != null)
                            {
                                for (final Map<String, Object> d : datasets)
                                {
                                    final String fileVal = String.valueOf(d.get("file"));
                                    final String fname = new File(fileVal).getName();
                                    if (fname.equals(yamlLabel + ".yaml") || fname.equals(yamlLabel))
                                    {
                                        matchedDataset = d;
                                        break;
                                    }
                                }
                            }
                            if (matchedDataset == null && !datasets.isEmpty())
                            {
                                matchedDataset = datasets.get(0);
                            }
                            if (matchedDataset != null)
                            {
                                String finalId = matchedDataset.containsKey("id") ? String.valueOf(matchedDataset.get("id")) : null;
                                if (finalId == null && testMap.containsKey("testId"))
                                {
                                    finalId = testMap.get("testId");
                                    if (finalId != null && finalId.startsWith("Dataset "))
                                    {
                                        finalId = finalId.substring(8);
                                    }
                                }
                                final Map<String, Object> singlePayload = new HashMap<>();
                                final Map<String, Object> datasetEntry = new HashMap<>();
                                datasetEntry.put("file", matchedDataset.get("file"));
                                datasetEntry.put("id", finalId);
                                singlePayload.put("datasets", List.of(datasetEntry));
                                singlePayload.put("headless", rc.get("headless"));
                                singlePayload.put("interactive", rc.get("interactive"));
                                singlePayload.put("allure", rc.get("allure"));
                                singlePayload.put("video", rc.get("video"));
                                singlePayload.put("keepOpen", rc.get("keepOpen"));

                                final String payloadStr = AuraHttpUtils.gson.toJson(singlePayload).replace("'", "\\'");
                                testMap.put("rerunPayload", payloadStr);
                            }
                        }
                    }
                }
            }
        }

        return historyList;
    }
}
