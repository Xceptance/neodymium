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
package com.xceptance.aura.report.controller;

import com.xceptance.aura.report.dto.AreaRunPointDto;
import com.xceptance.aura.report.dto.AreaSummaryDto;
import com.xceptance.aura.report.dto.BatchAreaTrendDto;
import com.xceptance.aura.report.dto.BatchOverviewDataDto;
import com.xceptance.aura.report.dto.RunReportDto;
import com.xceptance.aura.report.dto.TestBaseDataDto;
import com.xceptance.aura.report.dto.TestBaseVariationHistoryDto;
import com.xceptance.aura.report.dto.TestExecutionDto;
import com.xceptance.aura.report.entity.TestBatchEntity;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.repository.TestBaseBugRepository;
import com.xceptance.aura.report.repository.TestBaseVariationRepository;
import com.xceptance.aura.report.repository.TestBatchRepository;
import com.xceptance.aura.report.repository.TestRunRepository;
import com.xceptance.aura.report.service.AuraReportDataService;
import com.xceptance.aura.report.service.RunStorageSyncService;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Spring MVC View Controller serving Aura Report Manager page views and HTMX partial routes backed 100% by database and JSON storage.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@Controller
public class AuraReportViewController
{
    private static final Logger LOG = LoggerFactory.getLogger(AuraReportViewController.class);

    private final AuraReportDataService dataService;
    private final RunStorageSyncService runStorageSyncService;
    private final TestRunRepository runRepository;
    private final TestBatchRepository batchRepository;
    private final TestBaseVariationRepository variationRepository;
    private final TestBaseBugRepository bugRepository;
    private final ObjectMapper objectMapper;

    public AuraReportViewController(
        final AuraReportDataService dataService,
        final RunStorageSyncService runStorageSyncService,
        final TestRunRepository runRepository,
        final TestBatchRepository batchRepository,
        final TestBaseVariationRepository variationRepository,
        final TestBaseBugRepository bugRepository)
    {
        this.dataService = dataService;
        this.runStorageSyncService = runStorageSyncService;
        this.runRepository = runRepository;
        this.batchRepository = batchRepository;
        this.variationRepository = variationRepository;
        this.bugRepository = bugRepository;
        this.objectMapper = new ObjectMapper();
    }

    @GetMapping({"/report", "/batch-overview", "/fragments/batch-overview"})
    public String batchOverview(
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        final Model model)
    {
        final BatchOverviewDataDto overviewData = dataService.getBatchOverviewData();

        model.addAttribute("pageTitle", "Overview of Runs & Known Batches");
        model.addAttribute("activeTab", "Directory");
        model.addAttribute("batches", overviewData.getBatches());
        model.addAttribute("runs", overviewData.getRuns());
        model.addAttribute("filterEnvs", overviewData.getFilterEnvs());
        model.addAttribute("filterLocales", overviewData.getFilterLocales());
        model.addAttribute("filterBrowsers", overviewData.getFilterBrowsers());
        model.addAttribute("viewFragment", "fragments/batch-overview :: batchOverview");

        if ("true".equals(hxRequest)) {
            return "fragments/batch-overview :: batchOverview";
        }
        return "index";
    }

    @PostMapping("/runs/refresh")
    public String refreshAndResyncRuns(
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        @RequestHeader(value = "HX-Current-URL", required = false) final String currentUrl,
        @RequestHeader(value = "Referer", required = false) final String referer,
        final Model model)
    {
        runStorageSyncService.syncLocalRunStorage();
        dataService.clearCache();

        final String targetUrl = currentUrl != null && !currentUrl.isBlank() ? currentUrl : referer;
        if (targetUrl != null)
        {
            if (targetUrl.contains("/run-report"))
            {
                final String runId = extractQueryParam(targetUrl, "runId");
                final String executionId = extractQueryParam(targetUrl, "executionId");
                final String testExecutionId = extractQueryParam(targetUrl, "testExecutionId");
                final String subTab = extractQueryParam(targetUrl, "subTab");
                return runReport(runId, executionId, testExecutionId, subTab, hxRequest, model);
            }
            else if (targetUrl.contains("/batch-history"))
            {
                final String batchName = extractQueryParam(targetUrl, "batchName");
                final String limitParam = extractQueryParam(targetUrl, "limit");
                Integer limit = null;
                if (limitParam != null && !limitParam.isBlank())
                {
                    try
                    {
                        limit = Integer.parseInt(limitParam.trim());
                    }
                    catch (final NumberFormatException ignored)
                    {
                        // Fall back to no limit
                    }
                }
                return batchHistory(batchName != null && !batchName.isBlank() ? batchName : "Unknown", limit, hxRequest, model);
            }
            else if (targetUrl.contains("/test-base"))
            {
                final String testName = extractQueryParam(targetUrl, "testName");
                final String dataSet = extractQueryParam(targetUrl, "dataSet");
                final String location = extractQueryParam(targetUrl, "location");
                final String browser = extractQueryParam(targetUrl, "browser");
                return testBase(testName, dataSet, location, browser, hxRequest, model);
            }
        }

        return batchOverview(hxRequest, model);
    }

    private String extractQueryParam(final String url, final String paramName)
    {
        if (url == null)
        {
            return null;
        }
        final int hashIdx = url.indexOf('#');
        final String cleanUrl = hashIdx != -1 ? url.substring(0, hashIdx) : url;
        if (!cleanUrl.contains("?"))
        {
            return null;
        }
        final String queryString = cleanUrl.substring(cleanUrl.indexOf('?') + 1);
        final String[] pairs = queryString.split("&");
        for (final String pair : pairs)
        {
            final String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2 && keyValue[0].equalsIgnoreCase(paramName))
            {
                return URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    @GetMapping({"/batch-history", "/fragments/batch-history"})
    public String batchHistory(
        @RequestParam(name = "batchName", defaultValue = "Unknown") final String batchName,
        @RequestParam(name = "limit", required = false) final Integer limit,
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        final Model model)
    {
        final List<TestRunEntity> allRuns = runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc(batchName);
        final int totalRunsCount = allRuns.size();

        final Integer effectiveLimit = (limit != null && limit > 0) ? limit : null;
        final List<TestRunEntity> runs;
        if (effectiveLimit != null && effectiveLimit < totalRunsCount)
        {
            runs = new ArrayList<>(allRuns.subList(0, effectiveLimit));
        }
        else
        {
            runs = new ArrayList<>(allRuns);
        }
        final Optional<TestBatchEntity> batchOpt = batchRepository.findById(batchName);

        final List<String> batchLocales = runs.stream()
            .flatMap(r -> r.getLocalesCsv() != null ? Arrays.stream(r.getLocalesCsv().split(",")) : Stream.empty())
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        final List<String> batchBrowsers = runs.stream()
            .flatMap(r -> r.getBrowsersCsv() != null ? Arrays.stream(r.getBrowsersCsv().split(",")) : Stream.empty())
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        final String batchEnvironment = batchOpt.map(TestBatchEntity::getEnvironment)
            .orElseGet(() -> runs.stream()
                .map(TestRunEntity::getEnvironment)
                .filter(e -> e != null && !e.isBlank())
                .findFirst()
                .orElse("Unknown"));

        final List<TestRunEntity> chronoRuns = new ArrayList<>(runs);
        java.util.Collections.reverse(chronoRuns);

        final Map<String, List<AreaRunPointDto>> areaPointsMap = new LinkedHashMap<>();
        for (final TestRunEntity r : chronoRuns)
        {
            final RunReportDto rReport = dataService.getRunReport(r.getId());
            if (rReport != null)
            {
                boolean changed = false;
                if (!Objects.equals(r.getPassedHealedCount(), rReport.getPassHealedCount()))
                {
                    r.setPassedHealedCount(rReport.getPassHealedCount());
                    changed = true;
                }
                if (!Objects.equals(r.getSucceededFixedHealedCount(), rReport.getFixedHealedCount()))
                {
                    r.setSucceededFixedHealedCount(rReport.getFixedHealedCount());
                    changed = true;
                }
                if (!Objects.equals(r.getFailedKnownHealedCount(), rReport.getKnownHealedCount()))
                {
                    r.setFailedKnownHealedCount(rReport.getKnownHealedCount());
                    changed = true;
                }
                if (!Objects.equals(r.getFailedUnknownHealedCount(), rReport.getUnknownHealedCount()))
                {
                    r.setFailedUnknownHealedCount(rReport.getUnknownHealedCount());
                    changed = true;
                }
                if (!Objects.equals(r.getPassedAiCount(), rReport.getPassAiCount()))
                {
                    r.setPassedAiCount(rReport.getPassAiCount());
                    changed = true;
                }
                if (!Objects.equals(r.getSucceededFixedAiCount(), rReport.getFixedAiCount()))
                {
                    r.setSucceededFixedAiCount(rReport.getFixedAiCount());
                    changed = true;
                }
                if (!Objects.equals(r.getFailedKnownAiCount(), rReport.getKnownAiCount()))
                {
                    r.setFailedKnownAiCount(rReport.getKnownAiCount());
                    changed = true;
                }
                if (!Objects.equals(r.getFailedUnknownAiCount(), rReport.getUnknownAiCount()))
                {
                    r.setFailedUnknownAiCount(rReport.getUnknownAiCount());
                    changed = true;
                }
                if (!Objects.equals(r.getPassedCount(), rReport.getPassCount()))
                {
                    r.setPassedCount(rReport.getPassCount());
                    changed = true;
                }
                if (!Objects.equals(r.getSucceededFixedCount(), rReport.getFixedCount()))
                {
                    r.setSucceededFixedCount(rReport.getFixedCount());
                    changed = true;
                }
                if (!Objects.equals(r.getFailedKnownCount(), rReport.getKnownCount()))
                {
                    r.setFailedKnownCount(rReport.getKnownCount());
                    changed = true;
                }
                if (!Objects.equals(r.getFailedUnknownCount(), rReport.getUnknownCount()))
                {
                    r.setFailedUnknownCount(rReport.getUnknownCount());
                    changed = true;
                }
                if (!Objects.equals(r.getIgnoredCount(), rReport.getIgnoredCount()))
                {
                    r.setIgnoredCount(rReport.getIgnoredCount());
                    changed = true;
                }
                if (!Objects.equals(r.getTotalTests(), rReport.getTotalCount()))
                {
                    r.setTotalTests(rReport.getTotalCount());
                    changed = true;
                }
                if (changed)
                {
                    r.recalculatePassRate();
                    runRepository.save(r);
                }
                if (rReport.getAreaSummaries() != null)
                {
                    for (final AreaSummaryDto area : rReport.getAreaSummaries())
                    {
                        final String areaName = area.getAreaName() != null ? area.getAreaName() : "Browsing (default)";
                        areaPointsMap.computeIfAbsent(areaName, k -> new ArrayList<>())
                            .add(new AreaRunPointDto(
                                r.getId(),
                                r.getTimestampLabel(),
                                area.getPassCount(),
                                area.getFixedCount(),
                                area.getKnownCount(),
                                area.getUnknownCount(),
                                area.getIgnoredCount(),
                                area.getTotalCount(),
                                area.getPassHealedCount(),
                                area.getFixedHealedCount(),
                                area.getKnownHealedCount(),
                                area.getUnknownHealedCount(),
                                area.getPassAiCount(),
                                area.getFixedAiCount(),
                                area.getKnownAiCount(),
                                area.getUnknownAiCount()
                            ));
                    }
                }
            }
        }

        final List<BatchAreaTrendDto> areaTrends = new ArrayList<>();
        for (final Map.Entry<String, List<AreaRunPointDto>> entry : areaPointsMap.entrySet())
        {
            final String areaName = entry.getKey();
            final List<AreaRunPointDto> points = entry.getValue();
            final AreaRunPointDto latestPoint = !points.isEmpty() ? points.get(points.size() - 1) : null;
            final int latestCount = latestPoint != null ? latestPoint.getTotalCount() : 0;

            final String badgeClass;
            if (latestPoint != null && latestPoint.getUnknownCount() > 0)
            {
                badgeClass = "badge-unknown-fail";
            }
            else if (latestPoint != null && latestPoint.getKnownCount() > 0)
            {
                badgeClass = "badge-known-fail";
            }
            else if (latestPoint != null && latestPoint.getFixedCount() > 0)
            {
                badgeClass = "badge-fixed";
            }
            else if (latestPoint != null && latestPoint.getPassCount() > 0)
            {
                badgeClass = "badge-pass";
            }
            else
            {
                badgeClass = "badge-ignored";
            }

            final String areaGroup = "areaTrendGroup" + areaName.replaceAll("[^a-zA-Z0-9]", "");
            areaTrends.add(new BatchAreaTrendDto(
                areaName,
                areaGroup,
                latestCount,
                badgeClass,
                latestCount + " Executions in Last Run",
                points
            ));
        }

        model.addAttribute("batchName", batchName);
        model.addAttribute("batch", batchOpt.orElse(null));
        model.addAttribute("batchEnvironment", batchEnvironment);
        model.addAttribute("runs", runs);
        model.addAttribute("totalRunsCount", totalRunsCount);
        model.addAttribute("currentLimit", effectiveLimit);
        model.addAttribute("showingRunsCount", runs.size());
        model.addAttribute("batchLocales", batchLocales);
        model.addAttribute("batchBrowsers", batchBrowsers);
        model.addAttribute("areaTrends", areaTrends);
        model.addAttribute("pageTitle", "Batch History Overview - " + batchName);
        model.addAttribute("activeTab", "BatchHistory");
        model.addAttribute("viewFragment", "fragments/batch-history :: batchHistory");

        if ("true".equals(hxRequest)) {
            return "fragments/batch-history :: batchHistory";
        }
        return "index";
    }

    @GetMapping({"/run-report", "/fragments/run-report"})
    public String runReport(
        @RequestParam(name = "runId", required = false) final String runId,
        @RequestParam(name = "executionId", required = false) final String executionId,
        @RequestParam(name = "testExecutionId", required = false) final String testExecutionId,
        @RequestParam(name = "subTab", required = false) final String subTab,
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        final Model model)
    {
        final String targetExecutionId = executionId != null && !executionId.isEmpty() ? executionId : testExecutionId;
        final RunReportDto report = dataService.getRunReport(runId);
        final String activeSubTab = subTab != null && !subTab.isBlank() ? subTab : "runReportSubTabOverview";
        model.addAttribute("runId", report.getRunId());
        model.addAttribute("targetExecutionId", targetExecutionId);
        model.addAttribute("activeSubTab", activeSubTab);
        model.addAttribute("report", report);
        model.addAttribute("pageTitle", "Run Report #" + report.getRunId());
        model.addAttribute("activeTab", "RunReport");
        model.addAttribute("viewFragment", "fragments/run-report :: runReport");

        if ("true".equals(hxRequest)) {
            return "fragments/run-report :: runReport";
        }
        return "index";
    }

    @GetMapping("/fragments/run-report/filtered-tests")
    public String filteredTestsPartial(
        @RequestParam(name = "runId", defaultValue = "#RUN_ID") final String runId,
        @RequestParam(name = "status", required = false) final String status,
        @RequestParam(name = "locations", required = false) final List<String> locations,
        @RequestParam(name = "browsers", required = false) final List<String> browsers,
        @RequestParam(name = "bugs", required = false) final List<String> bugs,
        @RequestParam(name = "failures", required = false) final List<String> failures,
        final Model model)
    {
        final RunReportDto filteredReport = dataService.getFilteredRunReport(runId, status, locations, browsers, bugs, failures);
        model.addAttribute("runId", runId);
        model.addAttribute("report", filteredReport);
        model.addAttribute("activeStatusFilter", status);

        return "fragments/run-report :: allTestsContent";
    }

    @GetMapping({"/test-base", "/fragments/test-base"})
    public String testBase(
        @RequestParam(name = "testName", required = false) final String testName,
        @RequestParam(name = "dataSet", required = false) final String dataSet,
        @RequestParam(name = "location", required = false) final String location,
        @RequestParam(name = "browser", required = false) final String browser,
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        final Model model)
    {
        final TestBaseDataDto testBaseData = dataService.getTestBaseData();

        model.addAttribute("filterBatches", testBaseData.getFilterBatches());
        model.addAttribute("filterLocales", testBaseData.getFilterLocales());
        model.addAttribute("filterBrowsers", testBaseData.getFilterBrowsers());
        model.addAttribute("areas", testBaseData.getAreas());
        model.addAttribute("bugsMap", testBaseData.getBugsMap());
        model.addAttribute("totalClassesCount", testBaseData.getTotalTestClassesCount());
        model.addAttribute("totalVariationsCount", testBaseData.getTotalVariationsCount());
        model.addAttribute("targetTestName", testName);
        model.addAttribute("targetDataSet", dataSet);
        model.addAttribute("targetLocation", location);
        model.addAttribute("targetBrowser", browser);
        model.addAttribute("pageTitle", "Comprehensive Test Base");
        model.addAttribute("activeTab", "TestBase");
        model.addAttribute("viewFragment", "fragments/test-base :: testBase");

        if ("true".equals(hxRequest)) {
            return "fragments/test-base :: testBase";
        }
        return "index";
    }

    @GetMapping("/fragments/test-base/variation-history")
    public String getVariationHistoryFragment(
        @RequestParam(name = "testClass", defaultValue = "") final String testClass,
        @RequestParam(name = "testMethod", defaultValue = "") final String testMethod,
        @RequestParam(name = "dataSet", defaultValue = "") final String dataSet,
        @RequestParam(name = "location", defaultValue = "") final String location,
        @RequestParam(name = "browser", defaultValue = "") final String browser,
        final Model model)
    {
        final List<TestBaseVariationHistoryDto> historyRuns = dataService.getVariationHistory(
            testClass, testMethod, dataSet, location, browser
        );

        model.addAttribute("historyRuns", historyRuns);
        return "fragments/side-panel-step-list :: variationHistoryRows";
    }

    @GetMapping("/test-side-panel")
    public String testSidePanelFragment(
        @RequestParam(name = "runId", defaultValue = "#RUN_ID") final String runId,
        @RequestParam(name = "rowId", defaultValue = "") final String rowId,
        @RequestParam(name = "testName", defaultValue = "") final String testName,
        @RequestParam(name = "dataSet", defaultValue = "") final String dataSet,
        @RequestHeader(value = "HX-Current-URL", required = false) final String currentUrl,
        @RequestHeader(value = "Referer", required = false) final String referer,
        final Model model)
    {
        final String effectiveRunId = resolveEffectiveRunId(runId, currentUrl, referer);
        final RunReportDto report = dataService.getRunReport(effectiveRunId);
        TestExecutionDto currentExec = null;
        if (report != null && report.getExecutions() != null)
        {
            for (final TestExecutionDto e : report.getExecutions())
            {
                if (e.getId() != null && e.getId().equalsIgnoreCase(rowId))
                {
                    currentExec = e;
                    break;
                }
            }
        }
        if (currentExec == null)
        {
            currentExec = dataService.getExecutionDetails(effectiveRunId, rowId);
        }

        model.addAttribute("runId", effectiveRunId);
        model.addAttribute("rowId", rowId);
        model.addAttribute("testName", testName);
        model.addAttribute("dataSet", dataSet);
        model.addAttribute("exec", currentExec);

        return "fragments/test-side-panel :: testSidePanel";
    }

    @GetMapping("/fragments/test-side-panel/steps")
    @ResponseBody
    public ResponseEntity<TestExecutionDto> getExecutionSteps(
        @RequestParam(name = "runId", required = false) final String runId,
        @RequestParam(name = "rowId", required = false) final String rowId)
    {
        final TestExecutionDto exec = dataService.getExecutionDetails(runId, rowId);
        if (exec == null)
        {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(exec);
    }

    @GetMapping("/api/runs/{runId}/{testClass}/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveRunFile(
        @PathVariable("runId") final String runId,
        @PathVariable("testClass") final String testClass,
        @PathVariable("filename") final String filename)
    {
        final Path filePath = Paths.get("storage/runs", runId, testClass, filename);
        if (!Files.exists(filePath) || !Files.isReadable(filePath))
        {
            final Path defaultPath = Paths.get("target/aura-sandbox/allure-results", filename);
            if (Files.exists(defaultPath) && Files.isReadable(defaultPath))
            {
                return servePath(defaultPath, filename);
            }
            return ResponseEntity.notFound().build();
        }
        return servePath(filePath, filename);
    }

    private ResponseEntity<Resource> servePath(final Path path, final String filename)
    {
        try
        {
            final Resource resource = new UrlResource(path.toUri());
            String contentType = "application/octet-stream";
            if (filename.endsWith(".mp4"))
            {
                contentType = "video/mp4";
            }
            else if (filename.endsWith(".webm"))
            {
                contentType = "video/webm";
            }
            else if (filename.endsWith(".png"))
            {
                contentType = "image/png";
            }
            else if (filename.endsWith(".html"))
            {
                contentType = "text/html";
            }
            else if (filename.endsWith(".json"))
            {
                contentType = "application/json";
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        }
        catch (final Exception e)
        {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/fragments/test-side-panel/bugs")
    public String getBugSection(
        @RequestParam(name = "runId", defaultValue = "#RUN_ID") final String runId,
        @RequestParam(name = "rowId", defaultValue = "") final String rowId,
        @RequestHeader(value = "HX-Current-URL", required = false) final String currentUrl,
        @RequestHeader(value = "Referer", required = false) final String referer,
        final Model model)
    {
        final String effectiveRunId = resolveEffectiveRunId(runId, currentUrl, referer);
        final RunReportDto report = dataService.getRunReport(effectiveRunId);
        TestExecutionDto currentExec = null;
        if (report != null && report.getExecutions() != null)
        {
            for (final TestExecutionDto e : report.getExecutions())
            {
                if (e.getId() != null && e.getId().equalsIgnoreCase(rowId))
                {
                    currentExec = e;
                    break;
                }
            }
        }
        if (currentExec == null)
        {
            currentExec = dataService.getExecutionDetails(effectiveRunId, rowId);
        }
        model.addAttribute("runId", effectiveRunId);
        model.addAttribute("rowId", rowId);
        model.addAttribute("exec", currentExec);
        return "fragments/side-panel-step-list :: sidePanelBugSection";
    }

    @PostMapping("/fragments/test-side-panel/bugs")
    public String addBugToExecution(
        @RequestParam("runId") final String runId,
        @RequestParam("rowId") final String rowId,
        @RequestParam("bugTicket") final String bugTicket,
        @RequestHeader(value = "HX-Current-URL", required = false) final String currentUrl,
        @RequestHeader(value = "Referer", required = false) final String referer,
        final Model model,
        final HttpServletResponse response)
    {
        final String effectiveRunId = resolveEffectiveRunId(runId, currentUrl, referer);
        final long totalStart = System.currentTimeMillis();

        final long addStart = System.currentTimeMillis();
        final TestExecutionDto updatedExec = dataService.addBugToExecution(effectiveRunId, rowId, bugTicket);
        final long addDuration = System.currentTimeMillis() - addStart;

        final long reportStart = System.currentTimeMillis();
        final RunReportDto report = dataService.getRunReport(effectiveRunId);
        final long reportDuration = System.currentTimeMillis() - reportStart;

        try
        {
            final Map<String, Object> triggerMap = Map.of(
                "bugUpdated", Map.of(
                    "runId", effectiveRunId,
                    "rowId", rowId,
                    "status", updatedExec.getStatus() != null ? updatedExec.getStatus() : "",
                    "bugs", updatedExec.getBugs() != null ? updatedExec.getBugs() : List.of(),
                    "pass", report.getPassCount(),
                    "fixed", report.getFixedCount(),
                    "known", report.getKnownCount(),
                    "unknown", report.getUnknownCount(),
                    "ignored", report.getIgnoredCount()
                )
            );
            response.setHeader("HX-Trigger", objectMapper.writeValueAsString(triggerMap));
        }
        catch (final Exception e)
        {
            LOG.error("Failed to serialize HX-Trigger header: {}", e.getMessage());
        }

        model.addAttribute("runId", effectiveRunId);
        model.addAttribute("rowId", rowId);
        model.addAttribute("exec", updatedExec);

        final long totalDuration = System.currentTimeMillis() - totalStart;
        LOG.info("[PERF] Controller addBugToExecution total={} ms (addBug={} ms, getRunReport={} ms) runId={}, rowId={}, ticket={}",
            totalDuration, addDuration, reportDuration, effectiveRunId, rowId, bugTicket);

        return "fragments/side-panel-step-list :: sidePanelBugSection";
    }

    @DeleteMapping("/fragments/test-side-panel/bugs")
    public String removeBugFromExecution(
        @RequestParam(name = "runId", required = false) final String runId,
        @RequestParam(name = "rowId", required = false) final String rowIdParam,
        @RequestParam(name = "amp;rowId", required = false) final String ampRowId,
        @RequestParam(name = "bugTicket", required = false) final String bugTicketParam,
        @RequestParam(name = "amp;bugTicket", required = false) final String ampBugTicket,
        @RequestHeader(value = "HX-Current-URL", required = false) final String currentUrl,
        @RequestHeader(value = "Referer", required = false) final String referer,
        final Model model,
        final HttpServletResponse response)
    {
        final String effectiveRunId = resolveEffectiveRunId(runId, currentUrl, referer);
        final long totalStart = System.currentTimeMillis();
        final String effectiveRowId = rowIdParam != null ? rowIdParam : ampRowId;
        final String effectiveBugTicket = bugTicketParam != null ? bugTicketParam : ampBugTicket;

        final long removeStart = System.currentTimeMillis();
        final TestExecutionDto updatedExec = dataService.removeBugFromExecution(effectiveRunId, effectiveRowId, effectiveBugTicket);
        final TestExecutionDto effectiveExec = (updatedExec != null && updatedExec.getId() != null && !updatedExec.getId().isBlank())
            ? updatedExec
            : Optional.ofNullable(dataService.getExecutionDetails(effectiveRunId, effectiveRowId)).orElse(updatedExec);
        final long removeDuration = System.currentTimeMillis() - removeStart;

        final long reportStart = System.currentTimeMillis();
        final RunReportDto report = dataService.getRunReport(effectiveRunId);
        final long reportDuration = System.currentTimeMillis() - reportStart;

        try
        {
            final Map<String, Object> triggerMap = Map.of(
                "bugUpdated", Map.of(
                    "runId", effectiveRunId,
                    "rowId", effectiveRowId,
                    "status", effectiveExec.getStatus() != null ? effectiveExec.getStatus() : "",
                    "bugs", effectiveExec.getBugs() != null ? effectiveExec.getBugs() : List.of(),
                    "pass", report.getPassCount(),
                    "fixed", report.getFixedCount(),
                    "known", report.getKnownCount(),
                    "unknown", report.getUnknownCount(),
                    "ignored", report.getIgnoredCount()
                )
            );
            response.setHeader("HX-Trigger", objectMapper.writeValueAsString(triggerMap));
        }
        catch (final Exception e)
        {
            LOG.error("Failed to serialize HX-Trigger header: {}", e.getMessage());
        }

        model.addAttribute("runId", effectiveRunId);
        model.addAttribute("rowId", effectiveRowId);
        model.addAttribute("exec", effectiveExec);

        final long totalDuration = System.currentTimeMillis() - totalStart;
        LOG.info("[PERF] Controller removeBugFromExecution total={} ms (removeBug={} ms, getRunReport={} ms) runId={}, rowId={}, ticket={}",
            totalDuration, removeDuration, reportDuration, effectiveRunId, effectiveRowId, effectiveBugTicket);

        return "fragments/side-panel-step-list :: sidePanelBugSection";
    }

    private String resolveEffectiveRunId(final String runId, final String currentUrl, final String referer)
    {
        if (runId != null)
        {
            final String clean = runId.trim().replaceAll("^#+", "");
            if (!clean.isEmpty() && !"RUN_ID".equalsIgnoreCase(clean))
            {
                return clean;
            }
        }
        final String targetUrl = (currentUrl != null && !currentUrl.isBlank()) ? currentUrl : referer;
        if (targetUrl != null)
        {
            final String extracted = extractQueryParam(targetUrl, "runId");
            if (extracted != null)
            {
                final String clean = extracted.trim().replaceAll("^#+", "");
                if (!clean.isEmpty() && !"RUN_ID".equalsIgnoreCase(clean))
                {
                    return clean;
                }
            }
        }
        return (runId != null && !runId.isBlank()) ? runId.trim().replaceAll("^#+", "") : "#RUN_ID";
    }
}
