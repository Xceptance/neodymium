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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public AuraManagerReportingController(final AuraReportingService reportingService, final AuraQueueService queueService)
    {
        this.reportingService = reportingService;
        this.queueService = queueService;
    }

    public void handleReportingHistory(final HttpExchange exchange) throws IOException
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

                    String runConfigRaw = null;
                    if (metadataFile.exists() && metadataFile.isFile())
                    {
                        try
                        {
                            final String meta = Files.readString(metadataFile.toPath(), StandardCharsets.UTF_8);
                            final com.google.gson.JsonObject metaObj = com.google.gson.JsonParser
                                    .parseString(meta).getAsJsonObject();
                            if (metaObj.has("runConfig") && !metaObj.get("runConfig").isJsonNull())
                            {
                                runConfigRaw = AuraHttpUtils.gson.toJson(metaObj.get("runConfig"));
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
                                    if (map.containsKey("yamlSource") && map.get("yamlSource") != null)
                                    {
                                        final String src = String.valueOf(map.get("yamlSource"));
                                        final String base = new File(src).getName();
                                        testMap.put("yamlLabel", base.endsWith(".yaml")
                                                ? base.substring(0, base.length() - 5) : base);
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
                            testMap.put("file", cf.getName());
                            testMap.put("status", testStatus);
                            final String safeLogName = testName.replaceAll("[^a-zA-Z0-9_\\-]", "_") + ".log";
                            testMap.put("hasLog", String.valueOf(new File(dir, safeLogName).exists()));
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
                    if (runConfigRaw != null)
                    {
                        item.put("runConfig", AuraHttpUtils.gson.fromJson(runConfigRaw, Object.class));
                    }
                    historyList.add(item);
                }
            }
        }

        final int total2 = historyList.size();
        for (int idx = 0; idx < total2; idx++)
        {
            historyList.get(idx).put("runNumber", total2 - idx);
        }

        AuraHttpUtils.sendJsonResponse(exchange, 200, AuraHttpUtils.gson.toJson(historyList));
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
        final String body = AuraHttpUtils.readBody(exchange);
        final DeleteReportRequest req = AuraHttpUtils.gson.fromJson(body, DeleteReportRequest.class);
        if (req == null || req.id == null)
        {
            LOGGER.error("[Aura Server] Delete report request failed: Missing 'id' in body");
            AuraHttpUtils.sendError(exchange, 400, "Missing 'id' in body");
            return;
        }

        LOGGER.info("[Aura Server] POST /api/reporting/delete - Request received for ID: {}", req.id);

        try
        {
            reportingService.deleteReport(req.id);
            LOGGER.info("[Aura Server] Deleted report history directory: {}", req.id);
            AuraHttpUtils.sendJsonResponse(exchange, 200, AuraHttpUtils.gson.toJson(Map.of("success", true)));
        }
        catch (final SecurityException se)
        {
            LOGGER.error("[Aura Server] Directory traversal attempt detected: {}", req.id);
            AuraHttpUtils.sendError(exchange, 403, se.getMessage());
        }
    }
}
