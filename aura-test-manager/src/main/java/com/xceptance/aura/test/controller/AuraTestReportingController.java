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
package com.xceptance.aura.test.controller;

import com.xceptance.neodymium.aura.AuraHttpUtils;
import com.xceptance.neodymium.aura.AuraQueueService;
import com.xceptance.neodymium.aura.AuraReportingService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller for retrieving completed test run reports and execution logs.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@Controller
public class AuraTestReportingController
{
    private final AuraReportingService reportingService;
    private final AuraQueueService queueService;

    public AuraTestReportingController(final AuraReportingService reportingService, final AuraQueueService queueService)
    {
        this.reportingService = reportingService;
        this.queueService = queueService;
    }

    public List<Map<String, Object>> getHistoryList()
    {
        return enrichHistoryList(reportingService.getHistoryList());
    }

    public static List<Map<String, Object>> enrichHistoryList(final List<Map<String, Object>> historyList)
    {
        final int total = historyList.size();
        for (int idx = 0; idx < total; idx++)
        {
            final Map<String, Object> item = historyList.get(idx);
            item.put("runNumber", total - idx);
            item.putIfAbsent("skipped", "0");

            // Compute formatted timestamp
            final String timestamp = String.valueOf(item.get("timestamp"));
            try
            {
                final Instant instant = Instant.parse(timestamp);
                final LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                item.put("formattedTime", ldt.format(formatter));
            }
            catch (final Exception e)
            {
                item.put("formattedTime", timestamp != null && !timestamp.isEmpty() ? timestamp : String.valueOf(item.get("id")));
            }

            // Compute run duration label
            final Object durationMsObj = item.get("durationMs");
            final long durationMs = durationMsObj instanceof Number ? ((Number) durationMsObj).longValue() : 0L;
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

            if (item.containsKey("runConfig") && item.get("runConfig") != null)
            {
                final String escapedJson = AuraHttpUtils.gson.toJson(item.get("runConfig")).replace("'", "\\'");
                item.put("runConfigJson", escapedJson);
            }
        }
        return historyList;
    }


    @GetMapping("/api/reporting/history")
    public String getReportingHistoryFragment(final Model model)
    {
        final List<Map<String, Object>> runs = getHistoryList();
        final boolean isRunning = queueService.isRunningQueue();
        model.addAttribute("history", runs);
        model.addAttribute("reportingHistoryList", runs);
        model.addAttribute("running", isRunning);

        if (isRunning)
        {
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            model.addAttribute("runningStartTime", LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).format(formatter));
        }

        return "fragments/history :: reportingHistoryListContent";
    }

    @GetMapping("/api/reporting/run")
    public String getReportingRunFragment(@RequestParam(value = "id", required = false) final String id, final Model model)
    {
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

        if (foundItem != null)
        {
            model.addAttribute("tests", foundItem.get("tests"));
            model.addAttribute("reportId", id);
            model.addAttribute("selectedRunNumber", foundItem.get("runNumber"));
        }
        model.addAttribute("running", queueService.isRunningQueue());

        return "fragments/history :: historyTestsContent";
    }

    @PostMapping("/api/reporting/delete")
    public String deleteReportHtmx(@RequestParam(value = "id", required = false) final String idParam,
                                   final HttpServletRequest request,
                                   final Model model)
    {
        String id = idParam;
        if (id == null || id.isBlank())
        {
            id = request.getParameter("id");
        }

        if (id != null && !id.isBlank())
        {
            try
            {
                reportingService.deleteReport(id);
            }
            catch (final Exception ignored)
            {
            }
        }

        final List<Map<String, Object>> runs = getHistoryList();
        final boolean isRunning = queueService.isRunningQueue();
        model.addAttribute("history", runs);
        model.addAttribute("reportingHistoryList", runs);
        model.addAttribute("running", isRunning);

        return "fragments/history :: reportingHistoryListContent";
    }

    @GetMapping("/api/reporting/report/{reportId}/**")
    public ResponseEntity<Resource> serveReportFile(@PathVariable("reportId") final String reportId, final HttpServletRequest request)
    {
        final String fullPath = request.getRequestURI();
        final String prefix = "/api/reporting/report/" + reportId + "/";
        String assetPath = "";
        final int idx = fullPath.indexOf(prefix);
        if (idx != -1)
        {
            assetPath = fullPath.substring(idx + prefix.length());
        }

        try
        {
            final File file = reportingService.resolveReportAsset(reportId, assetPath);
            if (file == null || !file.exists() || !file.isFile())
            {
                return ResponseEntity.notFound().build();
            }
            final byte[] bytes = Files.readAllBytes(file.toPath());
            final String contentTypeStr = AuraHttpUtils.getMimeType(file.getName());
            final MediaType mediaType = MediaType.parseMediaType(contentTypeStr);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, mediaType.toString())
                    .body(new ByteArrayResource(bytes));
        }
        catch (final Exception e)
        {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping({"/api/reports", "/api/reporting/history-json"})
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getReports()
    {
        return ResponseEntity.ok(getHistoryList());
    }

    @GetMapping("/api/reports/{runId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getReportDetails(@PathVariable("runId") final String runId)
    {
        final List<Map<String, Object>> history = getHistoryList();
        for (final Map<String, Object> item : history)
        {
            if (runId.equals(item.get("id")) || runId.equals(item.get("runId")))
            {
                return ResponseEntity.ok(item);
            }
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/api/reports/{runId}/compile-allure")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> compileAllureReport(@PathVariable("runId") final String runId)
    {
        final Thread thread = new Thread(() -> reportingService.generateReport(List.of(), null, queueService.getRunStartTimeMs(), queueService.getGlobalTestsRun(), queueService.getGlobalPassed(), queueService.getGlobalFailed(), queueService.getGlobalSkipped(), queueService.isManuallyStopped(), queueService.getCurrentRunLogs(), queueService.getCurrentRunEvents()));
        thread.setName("NeodymiumAuraManualReportCompiler");
        thread.start();

        final Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/reports/{runId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteReport(@PathVariable("runId") final String runId)
    {
        boolean success = false;
        try
        {
            success = reportingService.deleteReport(runId);
        }
        catch (final Exception ignored)
        {
        }
        final Map<String, Object> response = new HashMap<>();
        response.put("status", success ? "SUCCESS" : "ERROR");
        return ResponseEntity.ok(response);
    }
}
