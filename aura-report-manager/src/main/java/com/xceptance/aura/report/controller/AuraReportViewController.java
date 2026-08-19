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

import com.xceptance.aura.report.dto.BatchOverviewDataDto;
import com.xceptance.aura.report.dto.RunReportDto;
import com.xceptance.aura.report.dto.TestBaseDataDto;
import com.xceptance.aura.report.dto.TestExecutionDto;
import com.xceptance.aura.report.entity.TestBatchEntity;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.repository.TestBaseBugRepository;
import com.xceptance.aura.report.repository.TestBaseVariationRepository;
import com.xceptance.aura.report.repository.TestBatchRepository;
import com.xceptance.aura.report.repository.TestRunRepository;
import com.xceptance.aura.report.service.AuraReportDataService;
import com.xceptance.aura.report.service.RunStorageSyncService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Spring MVC View Controller serving Aura Report Manager page views and HTMX partial routes backed 100% by database and JSON storage.
 *
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

    @GetMapping({"/", "/report", "/batch-overview", "/fragments/batch-overview"})
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
        final Model model)
    {
        runStorageSyncService.syncLocalRunStorage();
        return batchOverview(hxRequest, model);
    }

    @GetMapping({"/batch-history", "/fragments/batch-history"})
    public String batchHistory(
        @RequestParam(name = "batchName", defaultValue = "Unknown") final String batchName,
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        final Model model)
    {
        final List<TestRunEntity> runs = runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc(batchName);
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

        model.addAttribute("batchName", batchName);
        model.addAttribute("batch", batchOpt.orElse(null));
        model.addAttribute("batchEnvironment", batchEnvironment);
        model.addAttribute("runs", runs);
        model.addAttribute("batchLocales", batchLocales);
        model.addAttribute("batchBrowsers", batchBrowsers);
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
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        final Model model)
    {
        final RunReportDto report = dataService.getRunReport(runId);
        model.addAttribute("runId", report.getRunId());
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
        model.addAttribute("pageTitle", "Comprehensive Test Base");
        model.addAttribute("activeTab", "TestBase");
        model.addAttribute("viewFragment", "fragments/test-base :: testBase");

        if ("true".equals(hxRequest)) {
            return "fragments/test-base :: testBase";
        }
        return "index";
    }

    @GetMapping("/test-side-panel")
    public String testSidePanelFragment(
        @RequestParam(name = "runId", defaultValue = "#RUN_ID") final String runId,
        @RequestParam(name = "rowId", defaultValue = "") final String rowId,
        @RequestParam(name = "testName", defaultValue = "") final String testName,
        @RequestParam(name = "dataSet", defaultValue = "") final String dataSet,
        final Model model)
    {
        final RunReportDto report = dataService.getRunReport(runId);
        TestExecutionDto currentExec = null;
        for (final TestExecutionDto e : report.getExecutions()) {
            if (e.getId().equals(rowId)) {
                currentExec = e;
                break;
            }
        }

        model.addAttribute("runId", runId);
        model.addAttribute("rowId", rowId);
        model.addAttribute("testName", testName);
        model.addAttribute("dataSet", dataSet);
        model.addAttribute("exec", currentExec);

        return "fragments/test-side-panel :: testSidePanel";
    }

    @GetMapping("/fragments/test-side-panel/bugs")
    public String getBugSection(
        @RequestParam(name = "runId", defaultValue = "#RUN_ID") final String runId,
        @RequestParam(name = "rowId", defaultValue = "") final String rowId,
        final Model model)
    {
        final RunReportDto report = dataService.getRunReport(runId);
        TestExecutionDto currentExec = null;
        for (final TestExecutionDto e : report.getExecutions()) {
            if (e.getId().equalsIgnoreCase(rowId)) {
                currentExec = e;
                break;
            }
        }
        model.addAttribute("runId", runId);
        model.addAttribute("rowId", rowId);
        model.addAttribute("exec", currentExec);
        return "fragments/side-panel-step-list :: sidePanelBugSection";
    }

    @PostMapping("/fragments/test-side-panel/bugs")
    public String addBugToExecution(
        @RequestParam("runId") final String runId,
        @RequestParam("rowId") final String rowId,
        @RequestParam("bugTicket") final String bugTicket,
        final Model model,
        final HttpServletResponse response)
    {
        final TestExecutionDto updatedExec = dataService.addBugToExecution(runId, rowId, bugTicket);
        final RunReportDto report = dataService.getRunReport(runId);

        try
        {
            final Map<String, Object> triggerMap = Map.of(
                "bugUpdated", Map.of(
                    "runId", runId,
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

        model.addAttribute("runId", runId);
        model.addAttribute("rowId", rowId);
        model.addAttribute("exec", updatedExec);
        return "fragments/side-panel-step-list :: sidePanelBugSection";
    }

    @DeleteMapping("/fragments/test-side-panel/bugs")
    public String removeBugFromExecution(
        @RequestParam(name = "runId", required = false) final String runId,
        @RequestParam(name = "rowId", required = false) final String rowIdParam,
        @RequestParam(name = "amp;rowId", required = false) final String ampRowId,
        @RequestParam(name = "bugTicket", required = false) final String bugTicketParam,
        @RequestParam(name = "amp;bugTicket", required = false) final String ampBugTicket,
        final Model model,
        final HttpServletResponse response)
    {
        final String effectiveRowId = rowIdParam != null ? rowIdParam : ampRowId;
        final String effectiveBugTicket = bugTicketParam != null ? bugTicketParam : ampBugTicket;
        final TestExecutionDto updatedExec = dataService.removeBugFromExecution(runId, effectiveRowId, effectiveBugTicket);
        final RunReportDto report = dataService.getRunReport(runId);

        try
        {
            final Map<String, Object> triggerMap = Map.of(
                "bugUpdated", Map.of(
                    "runId", runId,
                    "rowId", effectiveRowId,
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

        model.addAttribute("runId", runId);
        model.addAttribute("rowId", effectiveRowId);
        model.addAttribute("exec", updatedExec);
        return "fragments/side-panel-step-list :: sidePanelBugSection";
    }
}
