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
import com.xceptance.aura.report.dto.BatchLatestExecutionsDto;
import com.xceptance.aura.report.dto.BatchLatestVariationDto;
import com.xceptance.aura.report.dto.BatchOverviewDataDto;
import com.xceptance.aura.report.dto.BatchSummaryDto;
import com.xceptance.aura.report.dto.DistributionRatioDto;
import com.xceptance.aura.report.dto.GroupingTagOptionDto;
import com.xceptance.aura.report.dto.RunReportDto;
import com.xceptance.aura.report.dto.RunTagTypeFilterDto;
import com.xceptance.aura.report.dto.TagRatioCardDto;
import com.xceptance.aura.report.dto.TestBaseAreaDto;
import com.xceptance.aura.report.dto.TestBaseClassDto;
import com.xceptance.aura.report.dto.TestBaseDataDto;
import com.xceptance.aura.report.dto.TestBaseVariationHistoryDto;
import com.xceptance.aura.report.dto.TestExecutionDto;
import com.xceptance.aura.report.dto.widget.DashboardPageConfigDto;
import com.xceptance.aura.report.dto.widget.DashboardWidgetConfigDto;
import com.xceptance.aura.report.dto.widget.DashboardWidgetViewDto;
import com.xceptance.aura.report.dto.widget.WidgetColorPolarity;
import com.xceptance.aura.report.dto.widget.WidgetContributingExecutionsDto;
import com.xceptance.aura.report.dto.widget.WidgetFilterConfigDto;
import com.xceptance.aura.report.dto.widget.WidgetMetric;
import com.xceptance.aura.report.dto.widget.WidgetPointScope;
import com.xceptance.aura.report.dto.widget.WidgetPresentation;
import com.xceptance.aura.report.dto.widget.WidgetTrendMode;
import com.xceptance.aura.report.dto.widget.WidgetValueFormat;
import com.xceptance.aura.report.entity.TestBaseBugEntity;
import com.xceptance.aura.report.entity.TestBatchEntity;
import com.xceptance.aura.report.entity.TestBaseVariationEntity;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.repository.TestBaseBugRepository;
import com.xceptance.aura.report.repository.TestBaseVariationRepository;
import com.xceptance.aura.report.service.WidgetAggregationService;
import com.xceptance.aura.report.repository.TestBatchRepository;
import com.xceptance.aura.report.repository.TestRunRepository;
import com.xceptance.aura.report.service.AuraReportDataService;
import com.xceptance.aura.report.service.ReportDashboardSettingService;
import com.xceptance.aura.report.service.RunStorageSyncService;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
    private final ReportDashboardSettingService dashboardSettingService;
    private final WidgetAggregationService widgetAggregationService;
    private final ObjectMapper objectMapper;

    @Autowired
    public AuraReportViewController(
        final AuraReportDataService dataService,
        final RunStorageSyncService runStorageSyncService,
        final TestRunRepository runRepository,
        final TestBatchRepository batchRepository,
        final TestBaseVariationRepository variationRepository,
        final TestBaseBugRepository bugRepository,
        final ReportDashboardSettingService dashboardSettingService,
        @Autowired(required = false) final WidgetAggregationService widgetAggregationService)
    {
        this.dataService = dataService;
        this.runStorageSyncService = runStorageSyncService;
        this.runRepository = runRepository;
        this.batchRepository = batchRepository;
        this.variationRepository = variationRepository;
        this.bugRepository = bugRepository;
        this.dashboardSettingService = dashboardSettingService;
        this.widgetAggregationService = (widgetAggregationService != null) ? widgetAggregationService : new WidgetAggregationService();
        this.objectMapper = new ObjectMapper();
    }

    public AuraReportViewController(
        final AuraReportDataService dataService,
        final RunStorageSyncService runStorageSyncService,
        final TestRunRepository runRepository,
        final TestBatchRepository batchRepository,
        final TestBaseVariationRepository variationRepository,
        final TestBaseBugRepository bugRepository,
        final ReportDashboardSettingService dashboardSettingService)
    {
        this(dataService, runStorageSyncService, runRepository, batchRepository, variationRepository, bugRepository, dashboardSettingService, new WidgetAggregationService());
    }

    @ModelAttribute("allKnownTagMap")
    public Map<String, List<String>> populateAllKnownTagMap()
    {
        return dataService.getAllKnownTagMap();
    }

    @ModelAttribute("allKnownTags")
    public List<String> populateAllKnownTags()
    {
        return dataService.getAllKnownTagsList();
    }

    /**
     * Root URL handler displaying the first configured dashboard page on server start.
     * Falls back to batch overview if no dashboards exist.
     *
     * @param hxRequest HTMX request header
     * @param response HTTP response
     * @param model Spring MVC model
     * @return custom dashboard view or batch overview fallback
     */
    @GetMapping("/")
    public String rootIndex(
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        final HttpServletResponse response,
        final Model model)
    {
        final List<DashboardPageConfigDto> rootPages = (dashboardSettingService != null)
            ? dashboardSettingService.getRootCustomPages()
            : Collections.emptyList();

        final DashboardPageConfigDto firstDashboard;
        if (rootPages != null && !rootPages.isEmpty())
        {
            firstDashboard = rootPages.get(0);
        }
        else
        {
            final List<DashboardPageConfigDto> allPages = (dashboardSettingService != null)
                ? dashboardSettingService.getCustomPages()
                : Collections.emptyList();
            firstDashboard = (allPages != null && !allPages.isEmpty()) ? allPages.get(0) : null;
        }

        if (firstDashboard != null && firstDashboard.getId() != null)
        {
            return getCustomDashboardPage(firstDashboard.getId(), hxRequest, response, model);
        }

        return batchOverview(null, hxRequest, model);
    }

    @GetMapping({"/report", "/batch-overview", "/fragments/batch-overview"})
    public String batchOverview(
        @RequestParam(name = "groupBy", required = false) final String groupBy,
        @RequestParam(name = "limit", required = false) final Integer limit,
        @RequestParam(name = "excludeRuns", required = false) final String excludeRuns,
        @RequestParam(name = "olderThanDate", required = false) final String olderThanDate,
        @RequestParam(name = "startDate", required = false) final String startDate,
        @RequestParam(name = "endDate", required = false) final String endDate,
        @RequestParam(name = "runBrowsers", required = false) final String runBrowsers,
        @RequestParam(name = "excludeRunBrowsers", required = false) final String excludeRunBrowsers,
        @RequestParam(name = "runLocales", required = false) final String runLocales,
        @RequestParam(name = "excludeRunLocales", required = false) final String excludeRunLocales,
        @RequestParam(name = "runEnvs", required = false) final String runEnvs,
        @RequestParam(name = "excludeRunEnvs", required = false) final String excludeRunEnvs,
        @RequestParam(name = "runAreas", required = false) final String runAreas,
        @RequestParam(name = "excludeRunAreas", required = false) final String excludeRunAreas,
        @RequestParam(name = "runTags", required = false) final String runTags,
        @RequestParam(name = "excludeRunTags", required = false) final String excludeRunTags,
        @RequestParam(name = "asOfRunId", required = false) final String asOfRunId,
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        final Model model,
        @RequestParam(required = false) final Map<String, String> allRequestParams)
    {
        return batchHistory(
            null, "all", null, groupBy, limit, excludeRuns, olderThanDate, startDate, endDate,
            runBrowsers, excludeRunBrowsers,
            runLocales, excludeRunLocales,
            runEnvs, excludeRunEnvs,
            runAreas, excludeRunAreas,
            runTags, excludeRunTags,
            asOfRunId,
            hxRequest, model, allRequestParams
        );
    }

    public String batchOverview(
        final String groupBy,
        final String hxRequest,
        final Model model)
    {
        return batchHistory(
            null, "all", null, groupBy, null, null, null, null, null,
            null, null,
            null, null,
            null, null,
            null, null,
            null, null,
            null,
            hxRequest, model, Collections.emptyMap()
        );
    }

    public String pinGrouping(
        final String groupBy,
        final String hxRequest,
        final String currentUrl,
        final String referer,
        final Model model)
    {
        return pinGrouping(groupBy, null, hxRequest, currentUrl, referer, model);
    }

    @PostMapping({"/runs/pin-grouping", "/batch-overview/pin-grouping"})
    public String pinGrouping(
        @RequestParam(name = "groupBy", required = false) final String groupBy,
        @RequestParam(name = "groupType", required = false) final String groupTypeParam,
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        @RequestHeader(value = "HX-Current-URL", required = false) final String currentUrl,
        @RequestHeader(value = "Referer", required = false) final String referer,
        final Model model)
    {
        final String targetUrl = currentUrl != null && !currentUrl.isBlank() ? currentUrl : referer;
        final String effectiveGroupType = (groupTypeParam != null && !groupTypeParam.isBlank())
            ? groupTypeParam.trim()
            : ((targetUrl != null) ? extractQueryParam(targetUrl, "groupType") : null);

        if (dashboardSettingService != null)
        {
            if (effectiveGroupType != null && !effectiveGroupType.isBlank() && !"all".equalsIgnoreCase(effectiveGroupType))
            {
                dashboardSettingService.togglePinnedGrouping(effectiveGroupType, groupBy);
            }
            else
            {
                dashboardSettingService.togglePinnedGrouping(groupBy);
            }
        }

        if (targetUrl != null && targetUrl.contains("/batch-history"))
        {
            final String batchName = extractQueryParam(targetUrl, "batchName");
            final String groupType = effectiveGroupType;
            final String groupValue = extractQueryParam(targetUrl, "groupValue");
            final String effectiveGroupBy = (groupBy != null && !groupBy.isBlank()) ? groupBy : extractQueryParam(targetUrl, "groupBy");
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
                }
            }
            return batchHistory(
                batchName, groupType, groupValue, effectiveGroupBy, limit,
                extractQueryParam(targetUrl, "excludeRuns"),
                extractQueryParam(targetUrl, "olderThanDate"),
                extractQueryParam(targetUrl, "startDate"),
                extractQueryParam(targetUrl, "endDate"),
                extractQueryParam(targetUrl, "runBrowsers"),
                extractQueryParam(targetUrl, "excludeRunBrowsers"),
                extractQueryParam(targetUrl, "runLocales"),
                extractQueryParam(targetUrl, "excludeRunLocales"),
                extractQueryParam(targetUrl, "runEnvs"),
                extractQueryParam(targetUrl, "excludeRunEnvs"),
                extractQueryParam(targetUrl, "runAreas"),
                extractQueryParam(targetUrl, "excludeRunAreas"),
                extractQueryParam(targetUrl, "runTags"),
                extractQueryParam(targetUrl, "excludeRunTags"),
                extractQueryParam(targetUrl, "asOfRunId"),
                hxRequest, model, Collections.emptyMap()
            );
        }

        final String effectiveGroupBy = (groupBy != null && !groupBy.isBlank()) ? groupBy : ((targetUrl != null) ? extractQueryParam(targetUrl, "groupBy") : null);
        return batchOverview(effectiveGroupBy, hxRequest, model);
    }

    @PostMapping({
        "/runs/toggle-identity-dimension", "/batch-history/toggle-identity-dimension",
        "/runs/toggle-uniqueness", "/batch-history/toggle-uniqueness"
    })
    public String toggleExecutionIdentityDimension(
        @RequestParam("dimension") final String dimension,
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        @RequestHeader(value = "HX-Current-URL", required = false) final String currentUrl,
        @RequestHeader(value = "Referer", required = false) final String referer,
        final Model model)
    {
        if (dashboardSettingService != null)
        {
            dashboardSettingService.toggleExecutionIdentityDimension(dimension);
        }

        final String targetUrl = currentUrl != null && !currentUrl.isBlank() ? currentUrl : referer;
        if (targetUrl != null && targetUrl.contains("/batch-history"))
        {
            final String batchName = extractQueryParam(targetUrl, "batchName");
            final String groupType = extractQueryParam(targetUrl, "groupType");
            final String groupValue = extractQueryParam(targetUrl, "groupValue");
            final String limitParam = extractQueryParam(targetUrl, "limit");
            final String excludeRuns = extractQueryParam(targetUrl, "excludeRuns");
            final String olderThanDate = extractQueryParam(targetUrl, "olderThanDate");
            final String startDate = extractQueryParam(targetUrl, "startDate");
            final String endDate = extractQueryParam(targetUrl, "endDate");
            final String runBrowsers = extractQueryParam(targetUrl, "runBrowsers");
            final String runLocales = extractQueryParam(targetUrl, "runLocales");
            final String runEnvs = extractQueryParam(targetUrl, "runEnvs");
            final String runAreas = extractQueryParam(targetUrl, "runAreas");
            final String runTags = extractQueryParam(targetUrl, "runTags");
            Integer limit = null;
            if (limitParam != null && !limitParam.isBlank())
            {
                try
                {
                    limit = Integer.parseInt(limitParam.trim());
                }
                catch (final NumberFormatException ignored)
                {
                }
            }
            final Map<String, String> allParams = AuraReportDataService.parseQueryParams(targetUrl);
            return batchHistory(batchName, groupType, groupValue, limit, excludeRuns, olderThanDate, startDate, endDate, runBrowsers, runLocales, runEnvs, runAreas, runTags, hxRequest, model, allParams);
        }

        final String effectiveGroupBy = (targetUrl != null) ? extractQueryParam(targetUrl, "groupBy") : null;
        return batchOverview(effectiveGroupBy, hxRequest, model);
    }

    /**
     * @deprecated Use {@link #toggleExecutionIdentityDimension(String, String, String, String, Model)} instead.
     */
    @Deprecated
    public String toggleUniquenessDimension(
        final String dimension,
        final String hxRequest,
        final String currentUrl,
        final String referer,
        final Model model)
    {
        return toggleExecutionIdentityDimension(dimension, hxRequest, currentUrl, referer, model);
    }

    @PostMapping({
        "/runs/toggle-variations-identity", "/batch-history/toggle-variations-identity",
        "/runs/toggle-variations-uniqueness", "/batch-history/toggle-variations-uniqueness"
    })
    public String toggleVariationsApplyIdentity(
        @RequestParam(name = "apply", required = false) final Boolean apply,
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        @RequestHeader(value = "HX-Current-URL", required = false) final String currentUrl,
        @RequestHeader(value = "Referer", required = false) final String referer,
        final Model model)
    {
        if (dashboardSettingService != null)
        {
            if (apply != null)
            {
                dashboardSettingService.setVariationsApplyIdentity(apply);
            }
            else
            {
                dashboardSettingService.toggleVariationsApplyIdentity();
            }
        }

        final String targetUrl = currentUrl != null && !currentUrl.isBlank() ? currentUrl : referer;
        if (targetUrl != null && targetUrl.contains("/batch-history"))
        {
            final String batchName = extractQueryParam(targetUrl, "batchName");
            final String groupType = extractQueryParam(targetUrl, "groupType");
            final String groupValue = extractQueryParam(targetUrl, "groupValue");
            final String limitParam = extractQueryParam(targetUrl, "limit");
            final String excludeRuns = extractQueryParam(targetUrl, "excludeRuns");
            final String olderThanDate = extractQueryParam(targetUrl, "olderThanDate");
            final String startDate = extractQueryParam(targetUrl, "startDate");
            final String endDate = extractQueryParam(targetUrl, "endDate");
            final String runBrowsers = extractQueryParam(targetUrl, "runBrowsers");
            final String runLocales = extractQueryParam(targetUrl, "runLocales");
            final String runEnvs = extractQueryParam(targetUrl, "runEnvs");
            final String runAreas = extractQueryParam(targetUrl, "runAreas");
            final String runTags = extractQueryParam(targetUrl, "runTags");
            Integer limit = null;
            if (limitParam != null && !limitParam.isBlank())
            {
                try
                {
                    limit = Integer.parseInt(limitParam.trim());
                }
                catch (final NumberFormatException ignored)
                {
                }
            }
            final Map<String, String> allParams = AuraReportDataService.parseQueryParams(targetUrl);
            return batchHistory(batchName, groupType, groupValue, limit, excludeRuns, olderThanDate, startDate, endDate, runBrowsers, runLocales, runEnvs, runAreas, runTags, hxRequest, model, allParams);
        }

        final String effectiveGroupBy = (targetUrl != null) ? extractQueryParam(targetUrl, "groupBy") : null;
        return batchOverview(effectiveGroupBy, hxRequest, model);
    }

    /**
     * @deprecated Use {@link #toggleVariationsApplyIdentity(Boolean, String, String, String, Model)} instead.
     */
    @Deprecated
    public String toggleVariationsApplyUniqueness(
        final Boolean apply,
        final String hxRequest,
        final String currentUrl,
        final String referer,
        final Model model)
    {
        return toggleVariationsApplyIdentity(apply, hxRequest, currentUrl, referer, model);
    }


    public String batchOverview(
        final String hxRequest,
        final Model model)
    {
        return batchOverview(null, hxRequest, model);
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
            if (targetUrl.contains("/dashboard-pages/"))
            {
                final String pageId = extractDashboardPageId(targetUrl);
                if (pageId != null && !pageId.isBlank())
                {
                    return getCustomDashboardPage(pageId, hxRequest, model);
                }
            }
            else if (isRootUrl(targetUrl))
            {
                return rootIndex(hxRequest, null, model);
            }
            else if (targetUrl.contains("/run-report"))
            {
                final String runId = extractQueryParam(targetUrl, "runId");
                final String executionId = extractQueryParam(targetUrl, "executionId");
                final String testExecutionId = extractQueryParam(targetUrl, "testExecutionId");
                final String variationId = extractQueryParam(targetUrl, "variationId");
                final String testName = extractQueryParam(targetUrl, "testName");
                final String subTab = extractQueryParam(targetUrl, "subTab");
                return runReport(runId, executionId, testExecutionId, variationId, testName, subTab, hxRequest, model);
            }
            else if (targetUrl.contains("/batch-history"))
            {
                final String batchName = extractQueryParam(targetUrl, "batchName");
                final String groupType = extractQueryParam(targetUrl, "groupType");
                final String groupValue = extractQueryParam(targetUrl, "groupValue");
                final String limitParam = extractQueryParam(targetUrl, "limit");
                final String excludeRuns = extractQueryParam(targetUrl, "excludeRuns");
                final String olderThanDate = extractQueryParam(targetUrl, "olderThanDate");
                final String startDate = extractQueryParam(targetUrl, "startDate");
                final String endDate = extractQueryParam(targetUrl, "endDate");
                final String runBrowsers = extractQueryParam(targetUrl, "runBrowsers");
                final String runLocales = extractQueryParam(targetUrl, "runLocales");
                final String runEnvs = extractQueryParam(targetUrl, "runEnvs");
                final String runAreas = extractQueryParam(targetUrl, "runAreas");
                final String runTags = extractQueryParam(targetUrl, "runTags");
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
                final Map<String, String> allParams = AuraReportDataService.parseQueryParams(targetUrl);
                return batchHistory(
                    batchName, groupType, groupValue, limit, excludeRuns, olderThanDate, startDate, endDate,
                    runBrowsers, runLocales, runEnvs, runAreas, runTags, hxRequest, model, allParams
                );
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

        final String groupBy = (targetUrl != null) ? extractQueryParam(targetUrl, "groupBy") : null;
        return batchOverview(groupBy, hxRequest, model);
    }

    @PostMapping({"/runs/{runId}/delete", "/runs/delete"})
    @DeleteMapping({"/runs/{runId}", "/runs/{runId}/delete"})
    public String deleteRun(
        @PathVariable(value = "runId", required = false) final String pathRunId,
        @RequestParam(value = "runId", required = false) final String paramRunId,
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        @RequestHeader(value = "HX-Current-URL", required = false) final String currentUrl,
        @RequestHeader(value = "Referer", required = false) final String referer,
        final Model model)
    {
        final String runId = (pathRunId != null && !pathRunId.isBlank()) ? pathRunId : paramRunId;
        if (runId != null && !runId.isBlank())
        {
            dataService.removeRunFromHistory(runId);
        }

        final String targetUrl = currentUrl != null && !currentUrl.isBlank() ? currentUrl : referer;
        if (targetUrl != null)
        {
            if (targetUrl.contains("/dashboard-pages/"))
            {
                final String pageId = extractDashboardPageId(targetUrl);
                if (pageId != null && !pageId.isBlank())
                {
                    return getCustomDashboardPage(pageId, hxRequest, model);
                }
            }
            else if (isRootUrl(targetUrl))
            {
                return rootIndex(hxRequest, null, model);
            }
            else if (targetUrl.contains("/batch-history"))
            {
                final String batchName = extractQueryParam(targetUrl, "batchName");
                final String groupType = extractQueryParam(targetUrl, "groupType");
                final String groupValue = extractQueryParam(targetUrl, "groupValue");
                final String limitParam = extractQueryParam(targetUrl, "limit");
                final String excludeRuns = extractQueryParam(targetUrl, "excludeRuns");
                final String olderThanDate = extractQueryParam(targetUrl, "olderThanDate");
                final String startDate = extractQueryParam(targetUrl, "startDate");
                final String endDate = extractQueryParam(targetUrl, "endDate");
                final String runBrowsers = extractQueryParam(targetUrl, "runBrowsers");
                final String runLocales = extractQueryParam(targetUrl, "runLocales");
                final String runEnvs = extractQueryParam(targetUrl, "runEnvs");
                final String runAreas = extractQueryParam(targetUrl, "runAreas");
                final String runTags = extractQueryParam(targetUrl, "runTags");
                Integer limit = null;
                if (limitParam != null && !limitParam.isBlank())
                {
                    try
                    {
                        limit = Integer.parseInt(limitParam.trim());
                    }
                    catch (final NumberFormatException ignored)
                    {
                    }
                }
                final Map<String, String> allParams = AuraReportDataService.parseQueryParams(targetUrl);
                return batchHistory(batchName, groupType, groupValue, limit, excludeRuns, olderThanDate, startDate, endDate, runBrowsers, runLocales, runEnvs, runAreas, runTags, hxRequest, model, allParams);
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

        final String groupBy = (targetUrl != null) ? extractQueryParam(targetUrl, "groupBy") : null;
        return batchOverview(groupBy, hxRequest, model);
    }

    /**
     * Helper to re-render the batch overview or batch history view preserving any query parameters from current URL or Referer.
     *
     * @param targetUrl target request URL
     * @param hxRequest HTMX request header flag
     * @param model Spring MVC model
     * @return view name or fragment
     */
    private String refreshBatchView(final String targetUrl, final String hxRequest, final Model model)
    {
        if (targetUrl != null && targetUrl.contains("/dashboard-pages/"))
        {
            final String pageId = extractDashboardPageId(targetUrl);
            if (pageId != null && !pageId.isBlank())
            {
                return getCustomDashboardPage(pageId, hxRequest, model);
            }
        }

        if (isRootUrl(targetUrl))
        {
            return rootIndex(hxRequest, null, model);
        }

        if (targetUrl != null && targetUrl.contains("/batch-history"))
        {
            final String batchName = extractQueryParam(targetUrl, "batchName");
            final String groupType = extractQueryParam(targetUrl, "groupType");
            final String groupValue = extractQueryParam(targetUrl, "groupValue");
            final String limitParam = extractQueryParam(targetUrl, "limit");
            final String excludeRuns = extractQueryParam(targetUrl, "excludeRuns");
            final String olderThanDate = extractQueryParam(targetUrl, "olderThanDate");
            final String startDate = extractQueryParam(targetUrl, "startDate");
            final String endDate = extractQueryParam(targetUrl, "endDate");
            final String runBrowsers = extractQueryParam(targetUrl, "runBrowsers");
            final String runLocales = extractQueryParam(targetUrl, "runLocales");
            final String runEnvs = extractQueryParam(targetUrl, "runEnvs");
            final String runAreas = extractQueryParam(targetUrl, "runAreas");
            final String runTags = extractQueryParam(targetUrl, "runTags");
            Integer limit = null;
            if (limitParam != null && !limitParam.isBlank())
            {
                try
                {
                    limit = Integer.parseInt(limitParam.trim());
                }
                catch (final NumberFormatException ignored)
                {
                }
            }
            final Map<String, String> allParams = AuraReportDataService.parseQueryParams(targetUrl);
            return batchHistory(
                batchName, groupType, groupValue, limit, excludeRuns, olderThanDate, startDate, endDate,
                runBrowsers, runLocales, runEnvs, runAreas, runTags, hxRequest, model, allParams
            );
        }

        final String groupBy = (targetUrl != null) ? extractQueryParam(targetUrl, "groupBy") : null;
        return batchOverview(groupBy, hxRequest, model);
    }

    /**
     * Extracts a custom dashboard page ID from a URL containing /dashboard-pages/{pageId}.
     *
     * @param url input URL
     * @return page ID, or null if not present
     */
    private String extractDashboardPageId(final String url)
    {
        if (url == null || !url.contains("/dashboard-pages/"))
        {
            return null;
        }
        final int start = url.indexOf("/dashboard-pages/") + "/dashboard-pages/".length();
        int end = url.indexOf('?', start);
        if (end == -1)
        {
            end = url.indexOf('#', start);
        }
        if (end == -1)
        {
            end = url.length();
        }
        final String rawId = url.substring(start, end).trim();
        return rawId.isEmpty() ? null : rawId;
    }

    /**
     * Checks if the given URL represents the root URL path ('/' or empty path).
     *
     * @param url input URL
     * @return true if URL points to root path, false otherwise
     */
    private boolean isRootUrl(final String url)
    {
        if (url == null || url.isBlank())
        {
            return false;
        }
        try
        {
            final URI uri = URI.create(url.trim());
            final String path = uri.getPath();
            return path == null || path.isEmpty() || "/".equals(path);
        }
        catch (final Exception ignored)
        {
            final String clean = url.split("[?#]", 2)[0].trim();
            return clean.isEmpty() || "/".equals(clean) || clean.matches("^https?://[^/]+/?$");
        }
    }

    /**
     * Resolves the target dashboard page ID from a URL, extracting /dashboard-pages/{pageId}
     * or resolving to the primary configured dashboard page if the URL is the root URL.
     *
     * @param url input URL
     * @return resolved dashboard page ID, or null if none
     */
    private String resolveTargetPageId(final String url)
    {
        final String pageId = extractDashboardPageId(url);
        if (pageId != null && !pageId.isBlank())
        {
            return pageId;
        }
        if (isRootUrl(url))
        {
            final List<DashboardPageConfigDto> rootPages = (dashboardSettingService != null)
                ? dashboardSettingService.getRootCustomPages()
                : Collections.emptyList();
            if (rootPages != null && !rootPages.isEmpty())
            {
                return rootPages.get(0).getId();
            }
            final List<DashboardPageConfigDto> allPages = (dashboardSettingService != null)
                ? dashboardSettingService.getCustomPages()
                : Collections.emptyList();
            if (allPages != null && !allPages.isEmpty())
            {
                return allPages.get(0).getId();
            }
        }
        return null;
    }

    /**
     * Renders a custom dashboard page with its configured widgets, or as an empty page with an Add Widget button.
     *
     * @param pageId ID of the custom dashboard page
     * @param hxRequest HTMX request header
     * @param response HTTP response
     * @param model Spring MVC model
     * @return custom dashboard page fragment or full page index view
     */
    @GetMapping({"/dashboard-pages/{pageId}", "/fragments/dashboard-pages/{pageId}"})
    public String getCustomDashboardPage(
        @PathVariable("pageId") final String pageId,
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        final HttpServletResponse response,
        final Model model)
    {
        final DashboardPageConfigDto page = (dashboardSettingService != null)
            ? dashboardSettingService.getCustomPage(pageId)
            : null;

        if (page == null)
        {
            return "redirect:/";
        }

        if ("true".equals(hxRequest) && response != null)
        {
            response.setHeader("HX-Push-Url", "/dashboard-pages/" + page.getId());
        }

        final List<DashboardWidgetConfigDto> widgetConfigs = (page.getWidgets() != null)
            ? page.getWidgets()
            : Collections.emptyList();

        final List<TestRunEntity> allRuns = (runRepository != null)
            ? runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()
            : Collections.emptyList();

        final Map<String, String> runAreasMap = new HashMap<>();
        for (final TestRunEntity r : allRuns)
        {
            if (r != null && r.getId() != null)
            {
                if (r.getAreasCsv() != null && !r.getAreasCsv().isBlank())
                {
                    runAreasMap.put(r.getId(), r.getAreasCsv());
                }
                else
                {
                    final List<String> fallbackAreas = (dataService != null)
                        ? dataService.getRunAreas(r)
                        : List.of(AuraReportDataService.normalizeArea("@Browsing (default)"));
                    runAreasMap.put(r.getId(), String.join(",", fallbackAreas));
                }
            }
        }

        final List<TestRunEntity> pageEffectiveRuns;
        if (page.hasRunFilters() && dataService != null)
        {
            final WidgetFilterConfigDto rf = page.getRunFilters();
            final List<TestRunEntity> scoped = new ArrayList<>();
            final Long fromTs = rf.getFromTimestampMs();
            final Long toTs = rf.getToTimestampMs();

            for (final TestRunEntity r : allRuns)
            {
                final Long startMs = r.getStartTimeMs();
                if (fromTs != null && startMs != null && startMs < fromTs)
                {
                    continue;
                }
                if (toTs != null && startMs != null && startMs > toTs)
                {
                    continue;
                }
                final TestRunEntity sc = dataService.scopeRunToCriteria(
                    r,
                    rf.getBrowsers(), rf.getExcludeBrowsers(),
                    rf.getLocales(), rf.getExcludeLocales(),
                    rf.getEnvs(), rf.getExcludeEnvs(),
                    rf.getAreas(), rf.getExcludeAreas(),
                    rf.getTags(), rf.getExcludeTags(),
                    null,
                    rf.getTagsByType(), rf.getExcludeTagsByType()
                );
                if (sc != null && sc.getTotalTests() != null && sc.getTotalTests() > 0)
                {
                    scoped.add(sc);
                }
            }
            if (rf.getLimit() != null && rf.getLimit() > 0)
            {
                scoped.sort(Comparator.comparing(
                    TestRunEntity::getStartTimeMs,
                    Comparator.nullsLast(Comparator.reverseOrder())
                ));
                if (scoped.size() > rf.getLimit())
                {
                    pageEffectiveRuns = new ArrayList<>(scoped.subList(0, rf.getLimit()));
                }
                else
                {
                    pageEffectiveRuns = scoped;
                }
            }
            else
            {
                pageEffectiveRuns = scoped;
            }
        }
        else
        {
            pageEffectiveRuns = allRuns;
        }

        final List<DashboardWidgetViewDto> dashboardWidgets;
        if (!widgetConfigs.isEmpty() && widgetAggregationService != null)
        {
            final List<String> globalIdentity = (dashboardSettingService != null)
                ? dashboardSettingService.getExecutionIdentityDimensions()
                : ReportDashboardSettingService.DEFAULT_EXECUTION_IDENTITY_DIMENSIONS;
            final List<String> identityDimensions = page.getEffectiveExecutionIdentityDimensions(globalIdentity);

            final WidgetFilterConfigDto pageFilters = page.hasRunFilters() ? page.getRunFilters() : null;
            final Long fromTs = (pageFilters != null) ? pageFilters.getFromTimestampMs() : null;
            final Long toTs = (pageFilters != null) ? pageFilters.getToTimestampMs() : null;
            final Set<String> runBrowsers = (pageFilters != null) ? pageFilters.getBrowsers() : Collections.emptySet();
            final Set<String> excludedBrowsers = (pageFilters != null) ? pageFilters.getExcludeBrowsers() : Collections.emptySet();
            final Set<String> runLocales = (pageFilters != null) ? pageFilters.getLocales() : Collections.emptySet();
            final Set<String> excludedLocales = (pageFilters != null) ? pageFilters.getExcludeLocales() : Collections.emptySet();
            final Set<String> runEnvs = (pageFilters != null) ? pageFilters.getEnvs() : Collections.emptySet();
            final Set<String> excludedEnvs = (pageFilters != null) ? pageFilters.getExcludeEnvs() : Collections.emptySet();
            final Set<String> runAreas = (pageFilters != null) ? pageFilters.getAreas() : Collections.emptySet();
            final Set<String> excludedAreas = (pageFilters != null) ? pageFilters.getExcludeAreas() : Collections.emptySet();
            final Set<String> runTags = (pageFilters != null) ? pageFilters.getTags() : Collections.emptySet();
            final Set<String> excludedTags = (pageFilters != null) ? pageFilters.getExcludeTags() : Collections.emptySet();
            final Map<String, Set<String>> runTagsByType = (pageFilters != null) ? pageFilters.getTagsByType() : Collections.emptyMap();
            final Map<String, Set<String>> excludedTagsByType = (pageFilters != null) ? pageFilters.getExcludeTagsByType() : Collections.emptyMap();

            final BatchLatestExecutionsDto latestExecResult = (dataService != null)
                ? dataService.getBatchLatestTestExecutionsSummary(
                    "all",
                    pageEffectiveRuns,
                    Collections.emptySet(),
                    fromTs,
                    toTs,
                    runBrowsers,
                    excludedBrowsers,
                    runLocales,
                    excludedLocales,
                    runEnvs,
                    excludedEnvs,
                    runAreas,
                    excludedAreas,
                    runTags,
                    excludedTags,
                    runAreasMap,
                    runTagsByType,
                    excludedTagsByType,
                    identityDimensions,
                    null
                )
                : null;

            final List<BatchLatestVariationDto> latestVariations = (latestExecResult != null && latestExecResult.getLatestVariations() != null)
                ? latestExecResult.getLatestVariations()
                : Collections.emptyList();

            final List<DistributionRatioDto> browserRatios = (latestExecResult != null && latestExecResult.getBrowserRatios() != null)
                ? latestExecResult.getBrowserRatios()
                : Collections.emptyList();

            final List<TagRatioCardDto> tagRatioCards = Collections.emptyList();

            final List<TestBaseBugEntity> allBugs = (bugRepository != null)
                ? bugRepository.findAll()
                : Collections.emptyList();

            final List<String> allTagKeys = (dataService != null)
                ? dataService.getAllTagKeys()
                : Collections.emptyList();

            dashboardWidgets = widgetAggregationService.aggregateWidgets(
                widgetConfigs, pageEffectiveRuns, latestVariations, browserRatios, tagRatioCards,
                (latestExecResult != null) ? latestExecResult.getTokenCostTrend() : null,
                (latestExecResult != null) ? latestExecResult.getSuiteEvolution() : null,
                allBugs,
                dataService,
                allRuns,
                "all",
                identityDimensions,
                runAreasMap,
                allTagKeys
            );
        }
        else
        {
            dashboardWidgets = Collections.emptyList();
        }

        final List<BatchSummaryDto> groupBatches;
        final Map<String, String> groupDashboardUrls = new LinkedHashMap<>();
        if (page.isGroupParent() && dataService != null)
        {
            final String groupType = page.getGroupType() != null ? page.getGroupType() : "browser";
            final WidgetPointScope scope = page.getGroupCardScope();
            final List<String> activeDims = parseGroupDimensions(groupType);
            final BatchOverviewDataDto groupOverview = dataService.getGroupOverviewData(
                activeDims.isEmpty() ? List.of("browser") : activeDims, pageEffectiveRuns, null, null, Collections.emptyMap(), scope
            );
            groupBatches = (groupOverview != null && groupOverview.getBatches() != null)
                ? groupOverview.getBatches()
                : Collections.emptyList();

            for (final BatchSummaryDto b : groupBatches)
            {
                if (b.getName() != null && !b.getName().isBlank())
                {
                    final DashboardPageConfigDto subPage = (dashboardSettingService != null)
                        ? dashboardSettingService.ensureSubDashboardForGroup(page.getId(), groupType, b.getName(), scope)
                        : null;
                    if (subPage != null)
                    {
                        groupDashboardUrls.put(b.getName(), "/dashboard-pages/" + subPage.getId());
                    }
                }
            }
        }
        else
        {
            groupBatches = Collections.emptyList();
        }

        final DashboardPageConfigDto parentDashboard = (page.isSubPage() && dashboardSettingService != null)
            ? dashboardSettingService.getCustomPage(page.getParentPageId())
            : null;

        final Set<String> prohibitedGroupDimensions = (dashboardSettingService != null)
            ? dashboardSettingService.getProhibitedGroupDimensions(page.getId())
            : Collections.emptySet();
        final String prohibitedGroupDimensionsCsv = String.join(",", prohibitedGroupDimensions);

        populateCandidateFilterAndIdentityAttributes(model);

        model.addAttribute("customPage", page);
        model.addAttribute("pageTitle", page.getName());
        model.addAttribute("activeCustomPageId", page.getId());
        model.addAttribute("activeTab", "Page_" + page.getId());
        model.addAttribute("dashboardWidgets", dashboardWidgets);
        model.addAttribute("runAreasMap", runAreasMap);
        model.addAttribute("isCustomWidgetsConfigured", !dashboardWidgets.isEmpty());
        model.addAttribute("isGroupDashboard", page.isGroupParent());
        model.addAttribute("groupType", page.getGroupType());
        model.addAttribute("groupCardScope", page.getGroupCardScope());
        model.addAttribute("groupBatches", groupBatches);
        model.addAttribute("groupDashboardUrls", groupDashboardUrls);
        model.addAttribute("parentDashboard", parentDashboard);
        model.addAttribute("isSubPage", page.isSubPage());
        model.addAttribute("prohibitedGroupDimensions", prohibitedGroupDimensions);
        model.addAttribute("prohibitedGroupDimensionsCsv", prohibitedGroupDimensionsCsv);
        model.addAttribute("availableWidgetTags", resolveAvailableWidgetTagKeys());
        model.addAttribute("allMetrics", WidgetMetric.values());
        model.addAttribute("allPresentations", WidgetPresentation.values());
        model.addAttribute("allTrendModes", WidgetTrendMode.values());
        model.addAttribute("allValueFormats", WidgetValueFormat.values());
        model.addAttribute("allPointScopes", WidgetPointScope.values());
        model.addAttribute("isHxRequest", "true".equals(hxRequest));
        model.addAttribute("customPages", (dashboardSettingService != null) ? dashboardSettingService.getCustomPages() : Collections.emptyList());
        model.addAttribute("rootCustomPages", (dashboardSettingService != null) ? dashboardSettingService.getRootCustomPages() : Collections.emptyList());
        model.addAttribute("viewFragment", "fragments/custom-dashboard-page :: customDashboardPage");

        if ("true".equals(hxRequest))
        {
            return "fragments/custom-dashboard-page :: customDashboardPage";
        }
        return "index";
    }

    public String getCustomDashboardPage(
        final String pageId,
        final String hxRequest,
        final Model model)
    {
        return getCustomDashboardPage(pageId, hxRequest, null, model);
    }

    /**
     * Creates a new custom dashboard page (or group dashboard) and redirects to it.
     *
     * @param name page name
     * @param dashboardType dashboard type ("standard" or "group")
     * @param groupBy grouping dimension (e.g. "browser", "environment", "batch", "locale", "area", or tag)
     * @param groupCardScope card status evaluation scope ("LATEST_TEST_STATUS" or "LAST_RUN")
     * @param executionIdentityDimensions comma-separated execution identity dimensions
     * @param filterLimit maximum runs limit
     * @param filterStartDate filter start date (YYYY-MM-DD)
     * @param filterEndDate filter end date (YYYY-MM-DD)
     * @param filterBrowsers comma-separated browser inclusions
     * @param excludeFilterBrowsers comma-separated browser exclusions
     * @param filterLocales comma-separated locale inclusions
     * @param excludeFilterLocales comma-separated locale exclusions
     * @param filterEnvs comma-separated environment inclusions
     * @param excludeFilterEnvs comma-separated environment exclusions
     * @param filterAreas comma-separated area inclusions
     * @param excludeFilterAreas comma-separated area exclusions
     * @param filterTags comma-separated tag inclusions
     * @param excludeFilterTags comma-separated tag exclusions
     * @param allRequestParams dynamic tag parameters
     * @param hxRequest HTMX request header
     * @param acceptHeader Accept header
     * @param response HTTP response
     * @param model Spring MVC model
     * @return redirect or JSON response
     */
    @PostMapping("/dashboard-pages/create")
    public Object createCustomDashboardPage(
        @RequestParam(name = "name", required = false) final String name,
        @RequestParam(name = "dashboardType", required = false) final String dashboardType,
        @RequestParam(name = "groupBy", required = false) final String groupBy,
        @RequestParam(name = "groupCardScope", required = false) final String groupCardScopeStr,
        @RequestParam(name = "executionIdentityDimensions", required = false) final String executionIdentityDimensions,
        @RequestParam(name = "filterLimit", required = false) final Integer filterLimit,
        @RequestParam(name = "filterStartDate", required = false) final String filterStartDate,
        @RequestParam(name = "filterEndDate", required = false) final String filterEndDate,
        @RequestParam(name = "filterBrowsers", required = false) final String filterBrowsers,
        @RequestParam(name = "excludeFilterBrowsers", required = false) final String excludeFilterBrowsers,
        @RequestParam(name = "filterLocales", required = false) final String filterLocales,
        @RequestParam(name = "excludeFilterLocales", required = false) final String excludeFilterLocales,
        @RequestParam(name = "filterEnvs", required = false) final String filterEnvs,
        @RequestParam(name = "excludeFilterEnvs", required = false) final String excludeFilterEnvs,
        @RequestParam(name = "filterAreas", required = false) final String filterAreas,
        @RequestParam(name = "excludeFilterAreas", required = false) final String excludeFilterAreas,
        @RequestParam(name = "filterTags", required = false) final String filterTags,
        @RequestParam(name = "excludeFilterTags", required = false) final String excludeFilterTags,
        @RequestParam(name = "copyFromPageId", required = false) final String copyFromPageId,
        @RequestParam(required = false) final Map<String, String> allRequestParams,
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        @RequestHeader(value = "Accept", required = false) final String acceptHeader,
        final HttpServletResponse response,
        final Model model)
    {
        final String effectiveName = (name != null && !name.isBlank()) ? name.trim() : "Custom Dashboard";
        final List<String> parsedIdentityDims;
        if (executionIdentityDimensions != null && !executionIdentityDimensions.isBlank())
        {
            parsedIdentityDims = Arrays.stream(executionIdentityDimensions.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        }
        else
        {
            parsedIdentityDims = Collections.singletonList("browser");
        }

        final Map<String, Set<String>> filterTagsByType = new LinkedHashMap<>();
        final Map<String, Set<String>> excludeFilterTagsByType = new LinkedHashMap<>();
        if (allRequestParams != null)
        {
            for (final Map.Entry<String, String> entry : allRequestParams.entrySet())
            {
                final String k = entry.getKey();
                final String v = entry.getValue();
                if (k.startsWith("filterTag_") && v != null && !v.isBlank())
                {
                    final String tagKeySuffix = k.substring("filterTag_".length());
                    filterTagsByType.put(tagKeySuffix, parseFilterCsv(v));
                }
                else if (k.startsWith("excludeFilterTag_") && v != null && !v.isBlank())
                {
                    final String tagKeySuffix = k.substring("excludeFilterTag_".length());
                    excludeFilterTagsByType.put(tagKeySuffix, parseFilterCsv(v));
                }
            }
        }

        final WidgetFilterConfigDto runFilters = new WidgetFilterConfigDto(
            parseFilterCsv(filterBrowsers),
            parseFilterCsv(excludeFilterBrowsers),
            parseFilterCsv(filterLocales),
            parseFilterCsv(excludeFilterLocales),
            parseFilterCsv(filterEnvs),
            parseFilterCsv(excludeFilterEnvs),
            parseFilterCsv(filterAreas),
            parseFilterCsv(excludeFilterAreas),
            parseFilterCsv(filterTags),
            parseFilterCsv(excludeFilterTags),
            filterTagsByType,
            excludeFilterTagsByType,
            filterLimit,
            (filterStartDate != null && !filterStartDate.isBlank()) ? filterStartDate.trim() : null,
            (filterEndDate != null && !filterEndDate.isBlank()) ? filterEndDate.trim() : null
        );

        final WidgetFilterConfigDto effectiveRunFilters = runFilters.isEmpty() ? null : runFilters;

        final boolean isGroupCreation = "group".equalsIgnoreCase(dashboardType)
            || (groupBy != null && !groupBy.isBlank() && !"standard".equalsIgnoreCase(dashboardType));

        final DashboardPageConfigDto newPage;
        if (isGroupCreation)
        {
            final List<String> parsedGroupDims = parseGroupDimensions(groupBy);
            final List<String> effectiveGroupDims = parsedGroupDims.isEmpty() ? List.of("browser") : parsedGroupDims;
            final String cleanGroupBy = String.join(",", effectiveGroupDims);
            final WidgetPointScope groupCardScope = (groupCardScopeStr != null && !groupCardScopeStr.isBlank())
                ? WidgetPointScope.fromString(groupCardScopeStr)
                : WidgetPointScope.LATEST_TEST_STATUS;

            final List<String> groupValues;
            if (dataService != null)
            {
                final BatchOverviewDataDto overview = dataService.getGroupOverviewData(effectiveGroupDims);
                if (overview != null && overview.getBatches() != null && !overview.getBatches().isEmpty())
                {
                    groupValues = overview.getBatches().stream()
                        .map(BatchSummaryDto::getName)
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList());
                }
                else
                {
                    groupValues = Collections.emptyList();
                }
            }
            else
            {
                groupValues = Collections.emptyList();
            }

            if (dashboardSettingService != null)
            {
                if (copyFromPageId != null && !copyFromPageId.isBlank())
                {
                    newPage = dashboardSettingService.createGroupDashboard(
                        effectiveName, cleanGroupBy, groupCardScope, parsedIdentityDims, effectiveRunFilters, groupValues, copyFromPageId
                    );
                }
                else
                {
                    newPage = dashboardSettingService.createGroupDashboard(
                        effectiveName, cleanGroupBy, groupCardScope, parsedIdentityDims, effectiveRunFilters, groupValues
                    );
                }
            }
            else
            {
                newPage = new DashboardPageConfigDto(
                    UUID.randomUUID().toString(), effectiveName, Collections.emptyList(), 0, System.currentTimeMillis(),
                    parsedIdentityDims, effectiveRunFilters, null, true, cleanGroupBy, null, groupCardScope
                );
            }
        }
        else if (dashboardSettingService != null)
        {
            if (copyFromPageId != null && !copyFromPageId.isBlank())
            {
                newPage = dashboardSettingService.createCustomPage(effectiveName, parsedIdentityDims, effectiveRunFilters, copyFromPageId);
            }
            else if (effectiveRunFilters == null && (parsedIdentityDims.size() == 1 && "browser".equals(parsedIdentityDims.get(0))))
            {
                newPage = dashboardSettingService.createCustomPage(effectiveName);
            }
            else
            {
                newPage = dashboardSettingService.createCustomPage(effectiveName, parsedIdentityDims, effectiveRunFilters);
            }
        }
        else
        {
            newPage = new DashboardPageConfigDto(UUID.randomUUID().toString(), effectiveName, Collections.emptyList(), 0, System.currentTimeMillis(), parsedIdentityDims, effectiveRunFilters);
        }

        if (acceptHeader != null && acceptHeader.contains("application/json"))
        {
            final Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", newPage.getId());
            result.put("name", newPage.getName());
            return ResponseEntity.ok(result);
        }

        if ("true".equals(hxRequest) && response != null)
        {
            response.setHeader("HX-Redirect", "/dashboard-pages/" + newPage.getId());
        }
        return "redirect:/dashboard-pages/" + newPage.getId();
    }

    public Object createCustomDashboardPage(
        final String name,
        final String hxRequest,
        final String acceptHeader,
        final HttpServletResponse response,
        final Model model)
    {
        return createCustomDashboardPage(name, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, hxRequest, acceptHeader, response, model);
    }

    public Object createCustomDashboardPage(
        final String name,
        final String executionIdentityDimensions,
        final Integer filterLimit,
        final String filterStartDate,
        final String filterEndDate,
        final String filterBrowsers,
        final String excludeFilterBrowsers,
        final String filterLocales,
        final String excludeFilterLocales,
        final String filterEnvs,
        final String excludeFilterEnvs,
        final String filterAreas,
        final String excludeFilterAreas,
        final String filterTags,
        final String excludeFilterTags,
        final Map<String, String> allRequestParams,
        final String hxRequest,
        final String acceptHeader,
        final HttpServletResponse response,
        final Model model)
    {
        return createCustomDashboardPage(name, null, null, null, executionIdentityDimensions, filterLimit, filterStartDate, filterEndDate, filterBrowsers, excludeFilterBrowsers, filterLocales, excludeFilterLocales, filterEnvs, excludeFilterEnvs, filterAreas, excludeFilterAreas, filterTags, excludeFilterTags, null, allRequestParams, hxRequest, acceptHeader, response, model);
    }

    public Object createCustomDashboardPage(
        final String name,
        final String dashboardType,
        final String groupBy,
        final String groupCardScopeStr,
        final String executionIdentityDimensions,
        final Integer filterLimit,
        final String filterStartDate,
        final String filterEndDate,
        final String filterBrowsers,
        final String excludeFilterBrowsers,
        final String filterLocales,
        final String excludeFilterLocales,
        final String filterEnvs,
        final String excludeFilterEnvs,
        final String filterAreas,
        final String excludeFilterAreas,
        final String filterTags,
        final String excludeFilterTags,
        final Map<String, String> allRequestParams,
        final String hxRequest,
        final String acceptHeader,
        final HttpServletResponse response,
        final Model model)
    {
        return createCustomDashboardPage(name, dashboardType, groupBy, groupCardScopeStr, executionIdentityDimensions, filterLimit, filterStartDate, filterEndDate, filterBrowsers, excludeFilterBrowsers, filterLocales, excludeFilterLocales, filterEnvs, excludeFilterEnvs, filterAreas, excludeFilterAreas, filterTags, excludeFilterTags, null, allRequestParams, hxRequest, acceptHeader, response, model);
    }

    /**
     * Deletes a custom dashboard page and redirects to the overview.
     *
     * @param pageId ID of the page to delete
     * @param hxRequest HTMX request header
     * @param acceptHeader Accept header
     * @param response HTTP response
     * @return redirect to overview or JSON confirmation
     */
    @PostMapping("/dashboard-pages/delete")
    public Object deleteCustomDashboardPage(
        @RequestParam("pageId") final String pageId,
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        @RequestHeader(value = "Accept", required = false) final String acceptHeader,
        final HttpServletResponse response)
    {
        if (dashboardSettingService != null && pageId != null && !pageId.isBlank())
        {
            dashboardSettingService.deleteCustomPage(pageId);
        }
        if (acceptHeader != null && acceptHeader.contains("application/json"))
        {
            return ResponseEntity.ok(Map.of("deleted", true, "id", pageId != null ? pageId : ""));
        }
        if ("true".equals(hxRequest) && response != null)
        {
            response.setHeader("HX-Redirect", "/");
        }
        return "redirect:/";
    }

    public String deleteCustomDashboardPage(
        final String pageId,
        final String hxRequest,
        final HttpServletResponse response)
    {
        final Object res = deleteCustomDashboardPage(pageId, hxRequest, null, response);
        return (res instanceof String) ? (String) res : "redirect:/";
    }

    /**
     * Copies widgets configuration from a source dashboard to a target dashboard.
     *
     * @param targetPageId target dashboard ID (required)
     * @param sourcePageId source dashboard ID (required)
     * @param mode copy mode ("replace" or "append", default "replace")
     * @param hxRequest HTMX request header
     * @param acceptHeader Accept header
     * @param response HTTP response
     * @param model Spring MVC model
     * @return redirect or JSON response
     */
    @PostMapping("/dashboard-pages/copy-widgets")
    public Object copyDashboardWidgets(
        @RequestParam("targetPageId") final String targetPageId,
        @RequestParam("sourcePageId") final String sourcePageId,
        @RequestParam(name = "mode", defaultValue = "replace") final String mode,
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        @RequestHeader(value = "Accept", required = false) final String acceptHeader,
        final HttpServletResponse response,
        final Model model)
    {
        final boolean isJson = acceptHeader != null && acceptHeader.contains("application/json");

        if (dashboardSettingService == null)
        {
            if (isJson)
            {
                return ResponseEntity.internalServerError().body(Map.of("error", "Dashboard setting service is not available"));
            }
            return "redirect:/";
        }

        if (targetPageId == null || targetPageId.isBlank())
        {
            if (isJson)
            {
                return ResponseEntity.badRequest().body(Map.of("error", "Target dashboard page ID is required"));
            }
            return "redirect:/";
        }

        if (sourcePageId == null || sourcePageId.isBlank())
        {
            if (isJson)
            {
                return ResponseEntity.badRequest().body(Map.of("error", "Source dashboard page ID is required"));
            }
            return "redirect:/dashboard-pages/" + targetPageId + "?error=missing_source";
        }

        try
        {
            final boolean append = "append".equalsIgnoreCase(mode);
            final List<DashboardWidgetConfigDto> copied = dashboardSettingService.copyWidgets(sourcePageId, targetPageId, append);

            if (isJson)
            {
                final Map<String, Object> result = new LinkedHashMap<>();
                result.put("success", true);
                result.put("targetPageId", targetPageId);
                result.put("sourcePageId", sourcePageId);
                result.put("copiedCount", copied.size());
                result.put("mode", append ? "append" : "replace");
                return ResponseEntity.ok(result);
            }

            if ("true".equals(hxRequest) && response != null)
            {
                response.setHeader("HX-Redirect", "/dashboard-pages/" + targetPageId);
            }
            return "redirect:/dashboard-pages/" + targetPageId;
        }
        catch (final IllegalArgumentException e)
        {
            if (isJson)
            {
                return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
            }
            if ("true".equals(hxRequest) && response != null)
            {
                response.setHeader("HX-Redirect", "/dashboard-pages/" + targetPageId + "?error=copy_failed");
            }
            return "redirect:/dashboard-pages/" + targetPageId + "?error=copy_failed";
        }
    }

    /**
     * Converts an existing custom dashboard page into a group dashboard page with its own grouping dimension,
     * status evaluation scope, execution identity dimensions, run filters, and auto-generated child sub-dashboards.
     *
     * @param pageId ID of the page to convert
     * @param groupBy grouping dimension (e.g. "browser", "locale", "environment", "area", "batch", or custom tag)
     * @param groupCardScopeStr card status evaluation scope ("LATEST_TEST_STATUS" or "LAST_RUN")
     * @param executionIdentityDimensions comma-separated execution identity dimensions (e.g. "browser,locale")
     * @param filterLimit maximum runs evaluated
     * @param filterStartDate optional ISO date string for start of run filter timespan
     * @param filterEndDate optional ISO date string for end of run filter timespan
     * @param filterBrowsers comma-separated browser inclusions
     * @param excludeFilterBrowsers comma-separated browser exclusions
     * @param filterLocales comma-separated locale inclusions
     * @param excludeFilterLocales comma-separated locale exclusions
     * @param filterEnvs comma-separated environment inclusions
     * @param excludeFilterEnvs comma-separated environment exclusions
     * @param filterAreas comma-separated area inclusions
     * @param excludeFilterAreas comma-separated area exclusions
     * @param filterTags comma-separated tag inclusions
     * @param excludeFilterTags comma-separated tag exclusions
     * @param allRequestParams full request parameters map for dynamic tag filters
     * @param hxRequest HTMX request header
     * @param acceptHeader Accept header
     * @param response HTTP response
     * @return redirect to converted page or JSON response
     */
    @PostMapping("/dashboard-pages/convert-to-group")
    public Object convertCustomDashboardPageToGroup(
        @RequestParam("pageId") final String pageId,
        @RequestParam(value = "groupBy", required = false) final String groupBy,
        @RequestParam(value = "groupCardScope", required = false) final String groupCardScopeStr,
        @RequestParam(value = "executionIdentityDimensions", required = false) final String executionIdentityDimensions,
        @RequestParam(value = "filterLimit", required = false) final Integer filterLimit,
        @RequestParam(value = "filterStartDate", required = false) final String filterStartDate,
        @RequestParam(value = "filterEndDate", required = false) final String filterEndDate,
        @RequestParam(value = "filterBrowsers", required = false) final String filterBrowsers,
        @RequestParam(value = "excludeFilterBrowsers", required = false) final String excludeFilterBrowsers,
        @RequestParam(value = "filterLocales", required = false) final String filterLocales,
        @RequestParam(value = "excludeFilterLocales", required = false) final String excludeFilterLocales,
        @RequestParam(value = "filterEnvs", required = false) final String filterEnvs,
        @RequestParam(value = "excludeFilterEnvs", required = false) final String excludeFilterEnvs,
        @RequestParam(value = "filterAreas", required = false) final String filterAreas,
        @RequestParam(value = "excludeFilterAreas", required = false) final String excludeFilterAreas,
        @RequestParam(value = "filterTags", required = false) final String filterTags,
        @RequestParam(value = "excludeFilterTags", required = false) final String excludeFilterTags,
        @RequestParam(required = false) final Map<String, String> allRequestParams,
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        @RequestHeader(value = "Accept", required = false) final String acceptHeader,
        final HttpServletResponse response)
    {
        if (dashboardSettingService == null || pageId == null || pageId.isBlank())
        {
            return "redirect:/";
        }
        final DashboardPageConfigDto page = dashboardSettingService.getCustomPage(pageId);
        if (page == null)
        {
            return "redirect:/";
        }

        final List<String> requestedDims = parseGroupDimensions(groupBy);
        final Set<String> prohibited = (dashboardSettingService != null)
            ? dashboardSettingService.getProhibitedGroupDimensions(pageId)
            : Collections.emptySet();

        for (final String dim : requestedDims)
        {
            if (prohibited.contains(dim))
            {
                if (acceptHeader != null && acceptHeader.contains("application/json"))
                {
                    final Map<String, Object> err = new LinkedHashMap<>();
                    err.put("error", "Cannot group by '" + dim + "': this dimension was already used in a previous grouping.");
                    err.put("prohibitedDimension", dim);
                    return ResponseEntity.badRequest().body(err);
                }
                if ("true".equals(hxRequest) && response != null)
                {
                    response.setHeader("HX-Redirect", "/dashboard-pages/" + pageId + "?error=prohibited_dimension");
                }
                return "redirect:/dashboard-pages/" + pageId + "?error=prohibited_dimension";
            }
        }

        final List<String> effectiveGroupDims = requestedDims.isEmpty() ? List.of("browser") : requestedDims;
        final String cleanGroupBy = String.join(",", effectiveGroupDims);
        final WidgetPointScope groupCardScope = (groupCardScopeStr != null && !groupCardScopeStr.isBlank())
            ? WidgetPointScope.fromString(groupCardScopeStr)
            : WidgetPointScope.LATEST_TEST_STATUS;

        final List<String> parsedIdentityDims;
        if (executionIdentityDimensions != null && !executionIdentityDimensions.isBlank())
        {
            parsedIdentityDims = Arrays.stream(executionIdentityDimensions.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        }
        else
        {
            parsedIdentityDims = null;
        }

        final Map<String, Set<String>> filterTagsByType = new LinkedHashMap<>();
        final Map<String, Set<String>> excludeFilterTagsByType = new LinkedHashMap<>();
        if (allRequestParams != null)
        {
            for (final Map.Entry<String, String> entry : allRequestParams.entrySet())
            {
                final String k = entry.getKey();
                final String v = entry.getValue();
                if (k.startsWith("filterTag_") && v != null && !v.isBlank())
                {
                    final String tagKeySuffix = k.substring("filterTag_".length());
                    filterTagsByType.put(tagKeySuffix, parseFilterCsv(v));
                }
                else if (k.startsWith("excludeFilterTag_") && v != null && !v.isBlank())
                {
                    final String tagKeySuffix = k.substring("excludeFilterTag_".length());
                    excludeFilterTagsByType.put(tagKeySuffix, parseFilterCsv(v));
                }
            }
        }

        final WidgetFilterConfigDto runFilters = new WidgetFilterConfigDto(
            parseFilterCsv(filterBrowsers),
            parseFilterCsv(excludeFilterBrowsers),
            parseFilterCsv(filterLocales),
            parseFilterCsv(excludeFilterLocales),
            parseFilterCsv(filterEnvs),
            parseFilterCsv(excludeFilterEnvs),
            parseFilterCsv(filterAreas),
            parseFilterCsv(excludeFilterAreas),
            parseFilterCsv(filterTags),
            parseFilterCsv(excludeFilterTags),
            filterTagsByType,
            excludeFilterTagsByType,
            filterLimit,
            (filterStartDate != null && !filterStartDate.isBlank()) ? filterStartDate.trim() : null,
            (filterEndDate != null && !filterEndDate.isBlank()) ? filterEndDate.trim() : null
        );

        final WidgetFilterConfigDto effectiveRunFilters = !runFilters.isEmpty()
            ? runFilters
            : (page.hasRunFilters() ? page.getRunFilters() : null);

        final List<TestRunEntity> allRuns = (runRepository != null)
            ? runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()
            : Collections.emptyList();

        final List<TestRunEntity> pageEffectiveRuns;
        if (effectiveRunFilters != null && dataService != null)
        {
            final List<TestRunEntity> scoped = new ArrayList<>();
            final Long fromTs = effectiveRunFilters.getFromTimestampMs();
            final Long toTs = effectiveRunFilters.getToTimestampMs();

            for (final TestRunEntity r : allRuns)
            {
                final Long startMs = r.getStartTimeMs();
                if (fromTs != null && startMs != null && startMs < fromTs)
                {
                    continue;
                }
                if (toTs != null && startMs != null && startMs > toTs)
                {
                    continue;
                }
                final TestRunEntity sc = dataService.scopeRunToCriteria(
                    r,
                    effectiveRunFilters.getBrowsers(), effectiveRunFilters.getExcludeBrowsers(),
                    effectiveRunFilters.getLocales(), effectiveRunFilters.getExcludeLocales(),
                    effectiveRunFilters.getEnvs(), effectiveRunFilters.getExcludeEnvs(),
                    effectiveRunFilters.getAreas(), effectiveRunFilters.getExcludeAreas(),
                    effectiveRunFilters.getTags(), effectiveRunFilters.getExcludeTags(),
                    null,
                    effectiveRunFilters.getTagsByType(), effectiveRunFilters.getExcludeTagsByType()
                );
                if (sc != null && sc.getTotalTests() != null && sc.getTotalTests() > 0)
                {
                    scoped.add(sc);
                }
            }
            pageEffectiveRuns = scoped;
        }
        else
        {
            pageEffectiveRuns = allRuns;
        }

        final List<String> groupValues;
        if (dataService != null)
        {
            final BatchOverviewDataDto overview = dataService.getGroupOverviewData(
                effectiveGroupDims, pageEffectiveRuns, null, null, Collections.emptyMap(), groupCardScope
            );
            if (overview != null && overview.getBatches() != null && !overview.getBatches().isEmpty())
            {
                groupValues = overview.getBatches().stream()
                    .map(BatchSummaryDto::getName)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            }
            else
            {
                groupValues = Collections.emptyList();
            }
        }
        else
        {
            groupValues = Collections.emptyList();
        }

        final DashboardPageConfigDto converted;
        if (parsedIdentityDims != null || (effectiveRunFilters != null && effectiveRunFilters != page.getRunFilters()))
        {
            converted = dashboardSettingService.convertToGroupDashboard(
                pageId, cleanGroupBy, groupCardScope, parsedIdentityDims, effectiveRunFilters, groupValues
            );
        }
        else
        {
            converted = dashboardSettingService.convertToGroupDashboard(
                pageId, cleanGroupBy, groupCardScope, groupValues
            );
        }

        if (acceptHeader != null && acceptHeader.contains("application/json"))
        {
            final Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", converted != null ? converted.getId() : pageId);
            result.put("name", converted != null ? converted.getName() : "");
            result.put("groupType", converted != null ? converted.getGroupType() : cleanGroupBy);
            result.put("isGroupParent", converted != null && converted.isGroupParent());
            return ResponseEntity.ok(result);
        }

        if ("true".equals(hxRequest) && response != null)
        {
            response.setHeader("HX-Redirect", "/dashboard-pages/" + pageId);
        }
        return "redirect:/dashboard-pages/" + pageId;
    }

    public Object convertCustomDashboardPageToGroup(
        final String pageId,
        final String groupBy,
        final String groupCardScopeStr,
        final String hxRequest,
        final String acceptHeader,
        final HttpServletResponse response)
    {
        return convertCustomDashboardPageToGroup(pageId, groupBy, groupCardScopeStr, null, null, null, null, null, null, null, null, null, null, null, null, null, null, Collections.emptyMap(), hxRequest, acceptHeader, response);
    }

    public String convertCustomDashboardPageToGroup(
        final String pageId,
        final String groupBy,
        final String groupCardScopeStr,
        final String hxRequest,
        final HttpServletResponse response)
    {
        final Object res = convertCustomDashboardPageToGroup(pageId, groupBy, groupCardScopeStr, hxRequest, null, response);
        return (res instanceof String) ? (String) res : ("redirect:/dashboard-pages/" + pageId);
    }

    /**
     * Resolves the list of distinct available tag keys for widget dimension configuration.
     * Includes primary dimensions (Browser, Locale, Area, Environment, Branch, Release)
     * and all discovered tag keys from the data service.
     *
     * @return sorted list of unique tag keys
     */
    private List<String> resolveAvailableWidgetTagKeys()
    {
        final Set<String> tagKeySet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        tagKeySet.add("Browser");
        tagKeySet.add("Locale");
        tagKeySet.add("Area");
        tagKeySet.add("Environment");
        tagKeySet.add("Branch");
        tagKeySet.add("Release");
        if (dataService != null)
        {
            tagKeySet.addAll(dataService.getAllKnownTagMap().keySet());
            for (final String key : dataService.getAllTagKeys())
            {
                if (key != null && !key.isBlank() && !"batch".equalsIgnoreCase(key))
                {
                    tagKeySet.add(key.trim());
                }
            }
        }
        return new ArrayList<>(tagKeySet);
    }

    private DashboardWidgetConfigDto findWidgetById(final List<DashboardWidgetConfigDto> widgets, final String widgetId)
    {
        if (widgets == null || widgetId == null || widgetId.isBlank())
        {
            return null;
        }
        for (final DashboardWidgetConfigDto w : widgets)
        {
            if (widgetId.equals(w.getId()))
            {
                return w;
            }
            if (w.isContainer() && w.hasChildren())
            {
                for (final DashboardWidgetConfigDto child : w.getChildren())
                {
                    if (widgetId.equals(child.getId()))
                    {
                        return child;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Populates candidate browsers, locales, environments, areas, and available identity tags into the model
     * for filter selectors and execution identity configurators.
     *
     * @param model Spring MVC model
     */
    private void populateCandidateFilterAndIdentityAttributes(final Model model)
    {
        if (model == null)
        {
            return;
        }
        final List<String> availableTags = resolveAvailableWidgetTagKeys();

        final List<String> candidateBrowsers = new ArrayList<>();
        final List<String> candidateLocales = new ArrayList<>();
        final List<String> candidateEnvs = new ArrayList<>();
        final List<String> candidateAreas = new ArrayList<>();
        final List<String> availableIdentityTags = new ArrayList<>();

        if (runRepository != null)
        {
            final List<TestRunEntity> allRuns = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc();
            final Set<String> bSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            final Set<String> lSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            final Set<String> eSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            final Set<String> aSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            for (final TestRunEntity r : allRuns)
            {
                if (r.getBrowsersCsv() != null && !r.getBrowsersCsv().isBlank())
                {
                    Arrays.stream(r.getBrowsersCsv().split(",")).map(String::trim).filter(s -> !s.isEmpty()).forEach(bSet::add);
                }
                if (r.getLocalesCsv() != null && !r.getLocalesCsv().isBlank())
                {
                    Arrays.stream(r.getLocalesCsv().split(","))
                        .map(String::trim)
                        .filter(AuraReportDataService::isRealLocale)
                        .forEach(lSet::add);
                }
                if (r.getEnvironment() != null && AuraReportDataService.isRealEnvironment(r.getEnvironment()))
                {
                    eSet.add(r.getEnvironment().trim());
                }
                if (dataService != null)
                {
                    dataService.getRunAreas(r).forEach(a -> {
                        if (AuraReportDataService.isRealArea(a))
                        {
                            aSet.add(AuraReportDataService.normalizeArea(a));
                        }
                    });
                }
            }
            if (dataService != null)
            {
                final Map<String, List<String>> allKnown = dataService.getAllKnownTagMap();
                if (allKnown.containsKey("browser"))
                {
                    bSet.addAll(allKnown.get("browser"));
                }
                if (allKnown.containsKey("location"))
                {
                    allKnown.get("location").forEach(loc -> {
                        if (AuraReportDataService.isRealLocale(loc))
                        {
                            lSet.add(loc.trim());
                        }
                    });
                }
                if (allKnown.containsKey("environment"))
                {
                    allKnown.get("environment").forEach(env -> {
                        if (AuraReportDataService.isRealEnvironment(env))
                        {
                            eSet.add(env.trim());
                        }
                    });
                }
                if (allKnown.containsKey("area"))
                {
                    allKnown.get("area").forEach(a -> {
                        if (AuraReportDataService.isRealArea(a))
                        {
                            aSet.add(AuraReportDataService.normalizeArea(a));
                        }
                    });
                }
            }
            if (bSet.isEmpty())
            {
                bSet.add("Chrome");
            }
            candidateBrowsers.addAll(bSet);
            candidateLocales.addAll(lSet);
            candidateEnvs.addAll(eSet);
            for (final String a : aSet)
            {
                if (AuraReportDataService.isRealArea(a))
                {
                    candidateAreas.add(a);
                }
            }
        }
        else
        {
            candidateBrowsers.add("Chrome");
        }

        if (dataService != null)
        {
            for (final String key : dataService.getAllTagKeys())
            {
                if (key != null && !key.isBlank() && !"batch".equalsIgnoreCase(key) && !availableIdentityTags.contains(key.trim()))
                {
                    availableIdentityTags.add(key.trim());
                }
            }
        }
        for (final String t : availableTags)
        {
            if (t != null && !t.isBlank() && !availableIdentityTags.contains(t.trim()))
            {
                availableIdentityTags.add(t.trim());
            }
        }
        availableIdentityTags.sort(String.CASE_INSENSITIVE_ORDER);

        final Map<String, List<String>> candidateCustomTags = (dataService != null)
            ? dataService.getCandidateCustomTags()
            : Collections.emptyMap();

        model.addAttribute("candidateBrowsers", candidateBrowsers);
        model.addAttribute("candidateLocales", candidateLocales);
        model.addAttribute("candidateEnvs", candidateEnvs);
        model.addAttribute("candidateAreas", candidateAreas);
        model.addAttribute("availableIdentityTags", availableIdentityTags);
        model.addAttribute("candidateCustomTags", candidateCustomTags);
    }

    /**
     * Returns the widget configurator modal dialog fragment.
     *
     * @param widgetId optional widget ID to edit existing widget
     * @param pageIdParam optional page ID parameter
     * @param currentUrl current URL header
     * @param referer referer header
     * @param model Spring MVC model
     * @return widget configurator modal fragment
     */
    @GetMapping({"/widgets/config-modal", "/fragments/widgets/config-modal"})
    public String getWidgetConfigModal(
        @RequestParam(name = "widgetId", required = false) final String widgetId,
        @RequestParam(name = "pageId", required = false) final String pageIdParam,
        @RequestHeader(value = "HX-Current-URL", required = false) final String currentUrl,
        @RequestHeader(value = "Referer", required = false) final String referer,
        final Model model)
    {
        final String targetUrl = currentUrl != null && !currentUrl.isBlank() ? currentUrl : referer;
        final String resolvedPageId = (pageIdParam != null && !pageIdParam.isBlank())
            ? pageIdParam
            : resolveTargetPageId(targetUrl);

        final List<DashboardWidgetConfigDto> allWidgets;
        if (dashboardSettingService != null)
        {
            allWidgets = (resolvedPageId != null && !resolvedPageId.isBlank())
                ? dashboardSettingService.getWidgetsForPage(resolvedPageId)
                : dashboardSettingService.getDashboardWidgets();
        }
        else
        {
            allWidgets = ReportDashboardSettingService.getDefaultWidgets();
        }

        final DashboardWidgetConfigDto found = (widgetId != null && !widgetId.isBlank())
            ? findWidgetById(allWidgets, widgetId)
            : null;
        final DashboardWidgetConfigDto targetWidget = (found != null)
            ? found
            : new DashboardWidgetConfigDto();

        final List<String> availableTags = resolveAvailableWidgetTagKeys();

        populateCandidateFilterAndIdentityAttributes(model);

        final DashboardPageConfigDto customPage = (dashboardSettingService != null && resolvedPageId != null && !resolvedPageId.isBlank())
            ? dashboardSettingService.getCustomPage(resolvedPageId)
            : null;
        if (customPage != null)
        {
            model.addAttribute("customPage", customPage);
        }

        model.addAttribute("widget", targetWidget);
        model.addAttribute("isNewWidget", found == null);
        model.addAttribute("dashboardWidgets", allWidgets);
        model.addAttribute("availableWidgetTags", availableTags);
        model.addAttribute("allMetrics", WidgetMetric.values());
        model.addAttribute("allPresentations", WidgetPresentation.values());
        model.addAttribute("allTrendModes", WidgetTrendMode.values());
        model.addAttribute("allValueFormats", WidgetValueFormat.values());
        model.addAttribute("allPointScopes", WidgetPointScope.values());
        model.addAttribute("isOpen", true);
        model.addAttribute("pageId", resolvedPageId);

        return "fragments/widget-configurator-modal :: widgetConfiguratorModal";
    }

    /**
     * Backward-compatible overload for {@link #getWidgetConfigModal(String, String, String, String, Model)}.
     *
     * @param widgetId optional widget ID
     * @param model Spring MVC model
     * @return widget configurator modal fragment
     */
    public String getWidgetConfigModal(
        final String widgetId,
        final Model model)
    {
        return getWidgetConfigModal(widgetId, null, null, null, model);
    }

    /**
     * Backward-compatible overload for {@link #getWidgetExecutions(String, String, String, String, Model)}.
     *
     * @param widgetId target widget ID
     * @param currentUrl current URL from HTMX header
     * @param referer referer URL header
     * @param model Spring MVC model
     * @return widget executions modal fragment
     */
    public String getWidgetExecutions(
        final String widgetId,
        final String currentUrl,
        final String referer,
        final Model model)
    {
        return getWidgetExecutions(widgetId, null, currentUrl, referer, model);
    }

    /**
     * Returns the widget contributing test executions modal dialog fragment,
     * showing test executions grouped by runs.
     *
     * @param widgetId target widget ID
     * @param excludeRuns optional comma-separated list of run IDs to exclude
     * @param currentUrl current URL from HTMX header
     * @param referer referer URL header
     * @param model Spring MVC model
     * @return widget executions modal fragment
     */
    @GetMapping({"/widgets/executions", "/fragments/widgets/executions"})
    public String getWidgetExecutions(
        @RequestParam(name = "widgetId", required = false) final String widgetId,
        @RequestParam(name = "excludeRuns", required = false) final String excludeRuns,
        @RequestHeader(value = "HX-Current-URL", required = false) final String currentUrl,
        @RequestHeader(value = "Referer", required = false) final String referer,
        final Model model)
    {
        final String targetUrl = (currentUrl != null && !currentUrl.isBlank()) ? currentUrl : referer;
        final String resolvedPageId = resolveTargetPageId(targetUrl);
        final List<DashboardWidgetConfigDto> allWidgets;
        if (dashboardSettingService != null)
        {
            allWidgets = (resolvedPageId != null && !resolvedPageId.isBlank())
                ? dashboardSettingService.getWidgetsForPage(resolvedPageId)
                : dashboardSettingService.getDashboardWidgets();
        }
        else
        {
            allWidgets = ReportDashboardSettingService.getDefaultWidgets();
        }

        final DashboardWidgetConfigDto found = (widgetId != null && !widgetId.isBlank())
            ? findWidgetById(allWidgets, widgetId)
            : null;
        final DashboardWidgetConfigDto targetWidget = (found != null)
            ? found
            : (allWidgets.isEmpty() ? new DashboardWidgetConfigDto() : allWidgets.get(0));
        final String batchName = (targetUrl != null) ? extractQueryParam(targetUrl, "batchName") : null;
        final String effectiveExcludeRuns = (excludeRuns != null && !excludeRuns.isBlank())
            ? excludeRuns
            : ((targetUrl != null) ? extractQueryParam(targetUrl, "excludeRuns") : null);

        final Set<String> excludedRunIds = new HashSet<>();
        if (effectiveExcludeRuns != null && !effectiveExcludeRuns.isBlank())
        {
            for (final String rId : effectiveExcludeRuns.split(","))
            {
                final String trimmed = rId.trim();
                if (!trimmed.isEmpty())
                {
                    excludedRunIds.add(trimmed);
                }
            }
        }

        final List<TestRunEntity> rawRuns;
        if (runRepository != null)
        {
            if (batchName != null && !batchName.isBlank() && !"Default".equalsIgnoreCase(batchName) && !"all".equalsIgnoreCase(batchName))
            {
                rawRuns = runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc(batchName);
            }
            else
            {
                rawRuns = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc();
            }
        }
        else
        {
            rawRuns = Collections.emptyList();
        }

        final List<TestRunEntity> runs = excludedRunIds.isEmpty()
            ? rawRuns
            : rawRuns.stream()
                .filter(r -> r.getId() != null && !excludedRunIds.contains(r.getId()))
                .collect(Collectors.toList());

        final DashboardPageConfigDto customPage = (dashboardSettingService != null && resolvedPageId != null && !resolvedPageId.isBlank())
            ? dashboardSettingService.getCustomPage(resolvedPageId)
            : null;

        final List<String> globalIdentity = (customPage != null)
            ? customPage.getEffectiveExecutionIdentityDimensions(
                (dashboardSettingService != null) ? dashboardSettingService.getExecutionIdentityDimensions() : ReportDashboardSettingService.DEFAULT_EXECUTION_IDENTITY_DIMENSIONS
              )
            : ((dashboardSettingService != null)
                ? dashboardSettingService.getExecutionIdentityDimensions()
                : ReportDashboardSettingService.DEFAULT_EXECUTION_IDENTITY_DIMENSIONS);
        final List<String> widgetIdentity = targetWidget.getEffectiveExecutionIdentityDimensions(globalIdentity);

        final WidgetFilterConfigDto effectiveRunFilters = targetWidget.hasOwnRunFilters()
            ? targetWidget.getRunFilters()
            : (customPage != null && customPage.hasRunFilters() ? customPage.getRunFilters() : null);

        final BatchLatestExecutionsDto latestExec;
        final List<TestRunEntity> effectiveRuns;
        if (effectiveRunFilters != null && effectiveRunFilters.hasAnyFilters() && dataService != null)
        {
            final WidgetFilterConfigDto rf = effectiveRunFilters;
            final List<TestRunEntity> scoped = new ArrayList<>();
            final Long fromTs = rf.getFromTimestampMs();
            final Long toTs = rf.getToTimestampMs();

            for (final TestRunEntity r : runs)
            {
                final Long startMs = r.getStartTimeMs();
                if (fromTs != null && startMs != null && startMs < fromTs)
                {
                    continue;
                }
                if (toTs != null && startMs != null && startMs > toTs)
                {
                    continue;
                }
                final TestRunEntity sc = dataService.scopeRunToCriteria(
                    r,
                    rf.getBrowsers(), rf.getExcludeBrowsers(),
                    rf.getLocales(), rf.getExcludeLocales(),
                    rf.getEnvs(), rf.getExcludeEnvs(),
                    rf.getAreas(), rf.getExcludeAreas(),
                    rf.getTags(), rf.getExcludeTags(),
                    null,
                    rf.getTagsByType(), rf.getExcludeTagsByType()
                );
                if (sc != null && sc.getTotalTests() != null && sc.getTotalTests() > 0)
                {
                    scoped.add(sc);
                }
            }
            if (rf.getLimit() != null && rf.getLimit() > 0)
            {
                scoped.sort(Comparator.comparing(
                    TestRunEntity::getStartTimeMs,
                    Comparator.nullsLast(Comparator.reverseOrder())
                ));
                if (scoped.size() > rf.getLimit())
                {
                    effectiveRuns = new ArrayList<>(scoped.subList(0, rf.getLimit()));
                }
                else
                {
                    effectiveRuns = scoped;
                }
            }
            else
            {
                effectiveRuns = scoped;
            }
            latestExec = dataService.getBatchLatestTestExecutionsSummary(
                batchName,
                effectiveRuns,
                excludedRunIds,
                fromTs, toTs,
                rf.getBrowsers(), rf.getExcludeBrowsers(),
                rf.getLocales(), rf.getExcludeLocales(),
                rf.getEnvs(), rf.getExcludeEnvs(),
                rf.getAreas(), rf.getExcludeAreas(),
                rf.getTags(), rf.getExcludeTags(),
                Collections.emptyMap(),
                rf.getTagsByType(), rf.getExcludeTagsByType(),
                widgetIdentity
            );
        }
        else if (dataService != null && !runs.isEmpty())
        {
            effectiveRuns = runs;
            latestExec = dataService.getBatchLatestTestExecutionsSummary(
                batchName, runs, excludedRunIds, null, null,
                Collections.emptySet(), Collections.emptySet(),
                Collections.emptySet(), Collections.emptySet(),
                Collections.emptySet(), Collections.emptySet(),
                Collections.emptySet(), Collections.emptySet(),
                Collections.emptySet(), Collections.emptySet(),
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
                widgetIdentity
            );
        }
        else
        {
            effectiveRuns = runs;
            latestExec = null;
        }

        final List<BatchLatestVariationDto> latestVars = (latestExec != null && latestExec.getLatestVariations() != null)
            ? latestExec.getLatestVariations()
            : Collections.emptyList();

        final WidgetContributingExecutionsDto widgetExecutions = (widgetAggregationService != null)
            ? widgetAggregationService.resolveContributingExecutions(targetWidget, effectiveRuns, latestVars, dataService)
            : new WidgetContributingExecutionsDto(
                targetWidget.getId(), targetWidget.getTitle(), targetWidget.getMetric(),
                targetWidget.getPresentation(), targetWidget.getPointScope(), targetWidget.getTrendMode(),
                targetWidget.getTagKey(), 0, 0, 0, 0, 0, 0, 0, 0.0, 0.0, 0L, Collections.emptyList()
            );

        model.addAttribute("widgetExecutions", widgetExecutions);
        model.addAttribute("widget", targetWidget);
        model.addAttribute("excludedRunIds", excludedRunIds);
        model.addAttribute("excludeRuns", effectiveExcludeRuns != null ? effectiveExcludeRuns : "");
        model.addAttribute("isOpen", true);

        return "fragments/widget-executions-modal :: widgetExecutionsModal";
    }

    /**
     * Saves or updates a dashboard widget configuration.
     *
     * @param id widget ID
     * @param title widget title
     * @param metricStr metric dimension string
     * @param tagKey tag key if TAG metric
     * @param presentationStr presentation format string
     * @param trendModeStr trend grouping mode string
     * @param valueFormatStr value format string
     * @param pointScopeStr point scope string
     * @param width widget card width
     * @param hxRequest HTMX request header flag
     * @param currentUrl current URL header
     * @param referer referer URL header
     * @param model Spring MVC model
     * @return updated batch history view or redirect
     */
    @PostMapping({"/widgets/save", "/batch-history/widgets/save"})
    public String saveWidget(
        @RequestParam(name = "id", required = false) final String id,
        @RequestParam(name = "title", required = false) final String title,
        @RequestParam("metric") final String metricStr,
        @RequestParam(name = "tagKey", required = false) final String tagKey,
        @RequestParam("presentation") final String presentationStr,
        @RequestParam(name = "trendMode", required = false) final String trendModeStr,
        @RequestParam(name = "valueFormat", required = false) final String valueFormatStr,
        @RequestParam(name = "colorPolarity", required = false) final String colorPolarityStr,
        @RequestParam(name = "pointScope", required = false) final String pointScopeStr,
        @RequestParam(name = "width", required = false, defaultValue = "half") final String width,
        @RequestParam(name = "showDots", required = false, defaultValue = "false") final boolean showDots,
        @RequestParam(name = "filled", required = false, defaultValue = "false") final boolean filled,
        @RequestParam(name = "showHealedAi", required = false, defaultValue = "false") final boolean showHealedAi,
        @RequestParam(name = "overrideExecutionIdentity", required = false, defaultValue = "false") final boolean overrideExecutionIdentity,
        @RequestParam(name = "executionIdentityDimensions", required = false) final String executionIdentityDimensions,
        @RequestParam(name = "overrideRunFilters", required = false, defaultValue = "false") final boolean overrideRunFilters,
        @RequestParam(name = "filterLimit", required = false) final Integer filterLimit,
        @RequestParam(name = "filterStartDate", required = false) final String filterStartDate,
        @RequestParam(name = "filterEndDate", required = false) final String filterEndDate,
        @RequestParam(name = "filterBrowsers", required = false) final String filterBrowsers,
        @RequestParam(name = "excludeFilterBrowsers", required = false) final String excludeFilterBrowsers,
        @RequestParam(name = "filterLocales", required = false) final String filterLocales,
        @RequestParam(name = "excludeFilterLocales", required = false) final String excludeFilterLocales,
        @RequestParam(name = "filterEnvs", required = false) final String filterEnvs,
        @RequestParam(name = "excludeFilterEnvs", required = false) final String excludeFilterEnvs,
        @RequestParam(name = "filterAreas", required = false) final String filterAreas,
        @RequestParam(name = "excludeFilterAreas", required = false) final String excludeFilterAreas,
        @RequestParam(name = "filterTags", required = false) final String filterTags,
        @RequestParam(name = "excludeFilterTags", required = false) final String excludeFilterTags,
        @RequestParam(required = false) final Map<String, String> allRequestParams,
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        @RequestHeader(value = "HX-Current-URL", required = false) final String currentUrl,
        @RequestHeader(value = "Referer", required = false) final String referer,
        final Model model)
    {
        final WidgetMetric metric = WidgetMetric.fromString(metricStr);
        final WidgetPresentation presentation = (metric == WidgetMetric.TEST_VARIATIONS)
            ? WidgetPresentation.TEST_VARIATIONS
            : ((metric == WidgetMetric.RUNS)
                ? WidgetPresentation.RUNS
                : WidgetPresentation.fromString(presentationStr));
        final WidgetTrendMode trendMode = (trendModeStr != null && !trendModeStr.isBlank())
            ? WidgetTrendMode.valueOf(trendModeStr.toUpperCase(Locale.ROOT))
            : WidgetTrendMode.BY_RUNS;
        final WidgetValueFormat valueFormat = (valueFormatStr != null && !valueFormatStr.isBlank())
            ? WidgetValueFormat.valueOf(valueFormatStr.toUpperCase(Locale.ROOT))
            : WidgetValueFormat.PERCENT;
        final WidgetColorPolarity colorPolarity = WidgetColorPolarity.fromString(colorPolarityStr);
        final WidgetPointScope pointScope = (metric == WidgetMetric.TEST_VARIATIONS || metric == WidgetMetric.RUNS)
            ? WidgetPointScope.LATEST_TEST_STATUS
            : ((pointScopeStr != null && !pointScopeStr.isBlank())
                ? WidgetPointScope.valueOf(pointScopeStr.toUpperCase(Locale.ROOT))
                : WidgetPointScope.LATEST_TEST_STATUS);
        final String effectiveWidth = (metric == WidgetMetric.RUNS || presentation == WidgetPresentation.RUNS || metric == WidgetMetric.TEST_VARIATIONS || presentation == WidgetPresentation.TEST_VARIATIONS)
            ? "full"
            : width;

        final boolean isRuns = (metric == WidgetMetric.RUNS || presentation == WidgetPresentation.RUNS || metric == WidgetMetric.NUMBER_OF_RUNS);
        final boolean effectiveOverrideExecutionIdentity = !isRuns && overrideExecutionIdentity;

        final List<String> parsedIdentityDims;
        if (effectiveOverrideExecutionIdentity && executionIdentityDimensions != null && !executionIdentityDimensions.isBlank())
        {
            final List<String> dims = Arrays.stream(executionIdentityDimensions.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
            if (dims.stream().noneMatch("browser"::equalsIgnoreCase))
            {
                dims.add(0, "browser");
            }
            parsedIdentityDims = dims;
        }
        else if (effectiveOverrideExecutionIdentity)
        {
            parsedIdentityDims = new ArrayList<>(ReportDashboardSettingService.DEFAULT_EXECUTION_IDENTITY_DIMENSIONS);
        }
        else
        {
            parsedIdentityDims = Collections.emptyList();
        }

        final Map<String, Set<String>> filterTagsByType = new LinkedHashMap<>();
        final Map<String, Set<String>> excludeFilterTagsByType = new LinkedHashMap<>();
        if (allRequestParams != null)
        {
            for (final Map.Entry<String, String> entry : allRequestParams.entrySet())
            {
                final String k = entry.getKey();
                final String v = entry.getValue();
                if (k.startsWith("filterTag_") && v != null && !v.isBlank())
                {
                    final String tagKeySuffix = k.substring("filterTag_".length());
                    filterTagsByType.put(tagKeySuffix, parseFilterCsv(v));
                }
                else if (k.startsWith("excludeFilterTag_") && v != null && !v.isBlank())
                {
                    final String tagKeySuffix = k.substring("excludeFilterTag_".length());
                    excludeFilterTagsByType.put(tagKeySuffix, parseFilterCsv(v));
                }
            }
        }

        final WidgetFilterConfigDto runFilters = new WidgetFilterConfigDto(
            parseFilterCsv(filterBrowsers),
            parseFilterCsv(excludeFilterBrowsers),
            parseFilterCsv(filterLocales),
            parseFilterCsv(excludeFilterLocales),
            parseFilterCsv(filterEnvs),
            parseFilterCsv(excludeFilterEnvs),
            parseFilterCsv(filterAreas),
            parseFilterCsv(excludeFilterAreas),
            parseFilterCsv(filterTags),
            parseFilterCsv(excludeFilterTags),
            filterTagsByType,
            excludeFilterTagsByType,
            filterLimit,
            filterStartDate,
            filterEndDate
        );
        final String safeTagKey = (metric == WidgetMetric.TAG && tagKey != null && !tagKey.isBlank())
            ? tagKey.trim()
            : "";

        final String safeTitle = sanitizeWidgetTitle(title);

        final DashboardWidgetConfigDto widget = new DashboardWidgetConfigDto(
            id,
            safeTitle,
            metric,
            safeTagKey,
            presentation,
            trendMode,
            valueFormat,
            pointScope,
            effectiveWidth,
            0,
            showHealedAi,
            effectiveOverrideExecutionIdentity,
            parsedIdentityDims,
            overrideRunFilters,
            runFilters,
            colorPolarity,
            showDots,
            filled
        );

        final String pageIdParam = (allRequestParams != null) ? allRequestParams.get("pageId") : null;
        final String targetUrl = currentUrl != null && !currentUrl.isBlank() ? currentUrl : referer;
        final String resolvedPageId = (pageIdParam != null && !pageIdParam.isBlank())
            ? pageIdParam
            : resolveTargetPageId(targetUrl);

        if (dashboardSettingService != null)
        {
            if (resolvedPageId != null && !resolvedPageId.isBlank())
            {
                dashboardSettingService.addOrUpdateWidget(resolvedPageId, widget);
            }
            else
            {
                dashboardSettingService.addOrUpdateWidget(widget);
            }
        }

        return refreshBatchView(targetUrl, hxRequest, model);
    }

    public String saveWidget(
        final String id,
        final String title,
        final String metricStr,
        final String tagKey,
        final String presentationStr,
        final String trendModeStr,
        final String valueFormatStr,
        final String pointScopeStr,
        final String width,
        final boolean showHealedAi,
        final boolean overrideExecutionIdentity,
        final String executionIdentityDimensions,
        final boolean overrideRunFilters,
        final Integer filterLimit,
        final String filterStartDate,
        final String filterEndDate,
        final String filterBrowsers,
        final String excludeFilterBrowsers,
        final String filterLocales,
        final String excludeFilterLocales,
        final String filterEnvs,
        final String excludeFilterEnvs,
        final String filterAreas,
        final String excludeFilterAreas,
        final String filterTags,
        final String excludeFilterTags,
        final Map<String, String> allRequestParams,
        final String hxRequest,
        final String currentUrl,
        final String referer,
        final Model model)
    {
        return saveWidget(id, title, metricStr, tagKey, presentationStr, trendModeStr, valueFormatStr, null, pointScopeStr, width, false, false, showHealedAi, overrideExecutionIdentity, executionIdentityDimensions, overrideRunFilters, filterLimit, filterStartDate, filterEndDate, filterBrowsers, excludeFilterBrowsers, filterLocales, excludeFilterLocales, filterEnvs, excludeFilterEnvs, filterAreas, excludeFilterAreas, filterTags, excludeFilterTags, allRequestParams, hxRequest, currentUrl, referer, model);
    }

    public String saveWidget(
        final String id,
        final String title,
        final String metricStr,
        final String tagKey,
        final String presentationStr,
        final String trendModeStr,
        final String valueFormatStr,
        final String pointScopeStr,
        final String width,
        final boolean showHealedAi,
        final boolean overrideExecutionIdentity,
        final String executionIdentityDimensions,
        final boolean overrideRunFilters,
        final String filterBrowsers,
        final String excludeFilterBrowsers,
        final String filterLocales,
        final String excludeFilterLocales,
        final String filterEnvs,
        final String excludeFilterEnvs,
        final String filterAreas,
        final String excludeFilterAreas,
        final String filterTags,
        final String excludeFilterTags,
        final Map<String, String> allRequestParams,
        final String hxRequest,
        final String currentUrl,
        final String referer,
        final Model model)
    {
        return saveWidget(id, title, metricStr, tagKey, presentationStr, trendModeStr, valueFormatStr, null, pointScopeStr, width, false, false, showHealedAi, overrideExecutionIdentity, executionIdentityDimensions, overrideRunFilters, null, null, null, filterBrowsers, excludeFilterBrowsers, filterLocales, excludeFilterLocales, filterEnvs, excludeFilterEnvs, filterAreas, excludeFilterAreas, filterTags, excludeFilterTags, allRequestParams, hxRequest, currentUrl, referer, model);
    }

    public String saveWidget(
        final String id,
        final String title,
        final String metricStr,
        final String tagKey,
        final String presentationStr,
        final String trendModeStr,
        final String valueFormatStr,
        final String pointScopeStr,
        final String width,
        final String hxRequest,
        final String currentUrl,
        final String referer,
        final Model model)
    {
        return saveWidget(id, title, metricStr, tagKey, presentationStr, trendModeStr, valueFormatStr, null, pointScopeStr, width, false, false, false, false, null, false, null, null, null, null, null, null, null, null, null, null, null, null, null, null, hxRequest, currentUrl, referer, model);
    }

    public String saveWidget(
        final String id,
        final String title,
        final String metricStr,
        final String tagKey,
        final String presentationStr,
        final String trendModeStr,
        final String valueFormatStr,
        final String pointScopeStr,
        final String width,
        final boolean showHealedAi,
        final String hxRequest,
        final String currentUrl,
        final String referer,
        final Model model)
    {
        return saveWidget(id, title, metricStr, tagKey, presentationStr, trendModeStr, valueFormatStr, null, pointScopeStr, width, false, false, showHealedAi, false, null, false, null, null, null, null, null, null, null, null, null, null, null, null, null, null, hxRequest, currentUrl, referer, model);
    }

    /**
     * Sanitizes an incoming widget title, stripping unintended duplicate tokens caused by
     * multi-input form submissions or legacy comma concatenations.
     *
     * @param title raw input title
     * @return cleaned title, or null if blank
     */
    static String sanitizeWidgetTitle(final String title)
    {
        if (title == null || title.isBlank())
        {
            return null;
        }
        final String trimmed = title.trim();
        if (!trimmed.contains(","))
        {
            return trimmed;
        }

        final String[] parts = Arrays.stream(trimmed.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toArray(String[]::new);

        if (parts.length == 0)
        {
            return null;
        }

        boolean allSame = true;
        for (int i = 1; i < parts.length; i++)
        {
            if (!parts[i].equalsIgnoreCase(parts[0]))
            {
                allSame = false;
                break;
            }
        }
        if (allSame)
        {
            return parts[0];
        }

        return parts[0];
    }

    /**
     * Deletes a widget from dashboard configuration.
     *
     * @param widgetId widget ID to remove
     * @param hxRequest HTMX request header flag
     * @param currentUrl current URL header
     * @param referer referer URL header
     * @param model Spring MVC model
     * @return updated batch history view
     */
    @PostMapping({"/widgets/delete", "/batch-history/widgets/delete"})
    public String deleteWidget(
        @RequestParam("widgetId") final String widgetId,
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        @RequestHeader(value = "HX-Current-URL", required = false) final String currentUrl,
        @RequestHeader(value = "Referer", required = false) final String referer,
        final Model model)
    {
        final String targetUrl = currentUrl != null && !currentUrl.isBlank() ? currentUrl : referer;
        final String resolvedPageId = resolveTargetPageId(targetUrl);
        if (dashboardSettingService != null && widgetId != null && !widgetId.isBlank())
        {
            if (resolvedPageId != null && !resolvedPageId.isBlank())
            {
                dashboardSettingService.deleteWidget(resolvedPageId, widgetId);
            }
            else
            {
                dashboardSettingService.deleteWidget(widgetId);
            }
        }
        return refreshBatchView(targetUrl, hxRequest, model);
    }

    /**
     * Toggles data point dots visibility on a trend graph widget.
     *
     * @param widgetId target widget ID
     * @param showDotsParam explicit toggle state, or null to invert
     * @param hxRequest HTMX request header flag
     * @param currentUrl current URL header
     * @param referer referer URL header
     * @param model Spring MVC model
     * @return updated batch history view
     */
    @PostMapping({"/widgets/toggle-dots", "/batch-history/widgets/toggle-dots"})
    public String toggleWidgetDots(
        @RequestParam("widgetId") final String widgetId,
        @RequestParam(name = "showDots", required = false) final Boolean showDotsParam,
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        @RequestHeader(value = "HX-Current-URL", required = false) final String currentUrl,
        @RequestHeader(value = "Referer", required = false) final String referer,
        final Model model)
    {
        final String targetUrl = currentUrl != null && !currentUrl.isBlank() ? currentUrl : referer;
        final String resolvedPageId = resolveTargetPageId(targetUrl);
        if (dashboardSettingService != null && widgetId != null && !widgetId.isBlank())
        {
            final List<DashboardWidgetConfigDto> pageWidgets = (resolvedPageId != null && !resolvedPageId.isBlank())
                ? dashboardSettingService.getWidgetsForPage(resolvedPageId)
                : dashboardSettingService.getDashboardWidgets();
            final DashboardWidgetConfigDto existing = pageWidgets.stream()
                .filter(w -> widgetId.equals(w.getId()))
                .findFirst()
                .orElse(null);
            if (existing != null)
            {
                final boolean newShowDots = (showDotsParam != null) ? showDotsParam : !existing.isShowDots();
                if (resolvedPageId != null && !resolvedPageId.isBlank())
                {
                    dashboardSettingService.addOrUpdateWidget(resolvedPageId, existing.withShowDots(newShowDots));
                }
                else
                {
                    dashboardSettingService.addOrUpdateWidget(existing.withShowDots(newShowDots));
                }
            }
        }
        return refreshBatchView(targetUrl, hxRequest, model);
    }

    /**
     * Toggles filled vs non-filled chart presentation on a trend graph widget.
     *
     * @param widgetId target widget ID
     * @param filledParam explicit toggle state, or null to invert
     * @param hxRequest HTMX request header flag
     * @param currentUrl current URL header
     * @param referer referer URL header
     * @param model Spring MVC model
     * @return updated batch history view
     */
    @PostMapping({"/widgets/toggle-filled", "/batch-history/widgets/toggle-filled"})
    public String toggleWidgetFilled(
        @RequestParam("widgetId") final String widgetId,
        @RequestParam(name = "filled", required = false) final Boolean filledParam,
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        @RequestHeader(value = "HX-Current-URL", required = false) final String currentUrl,
        @RequestHeader(value = "Referer", required = false) final String referer,
        final Model model)
    {
        final String targetUrl = currentUrl != null && !currentUrl.isBlank() ? currentUrl : referer;
        final String resolvedPageId = resolveTargetPageId(targetUrl);
        if (dashboardSettingService != null && widgetId != null && !widgetId.isBlank())
        {
            final List<DashboardWidgetConfigDto> pageWidgets = (resolvedPageId != null && !resolvedPageId.isBlank())
                ? dashboardSettingService.getWidgetsForPage(resolvedPageId)
                : dashboardSettingService.getDashboardWidgets();
            final DashboardWidgetConfigDto existing = pageWidgets.stream()
                .filter(w -> widgetId.equals(w.getId()))
                .findFirst()
                .orElse(null);
            if (existing != null)
            {
                final boolean newFilled = (filledParam != null) ? filledParam : !existing.isFilled();
                if (resolvedPageId != null && !resolvedPageId.isBlank())
                {
                    dashboardSettingService.addOrUpdateWidget(resolvedPageId, existing.withFilled(newFilled));
                }
                else
                {
                    dashboardSettingService.addOrUpdateWidget(existing.withFilled(newFilled));
                }
            }
        }
        return refreshBatchView(targetUrl, hxRequest, model);
    }

    /**
     * Resets dashboard widgets back to the system default layout.
     *
     * @param hxRequest HTMX request header flag
     * @param currentUrl current URL header
     * @param referer referer URL header
     * @param model Spring MVC model
     * @return updated batch history view
     */
    @PostMapping({"/widgets/reset", "/batch-history/widgets/reset"})
    public String resetWidgets(
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        @RequestHeader(value = "HX-Current-URL", required = false) final String currentUrl,
        @RequestHeader(value = "Referer", required = false) final String referer,
        final Model model)
    {
        final String targetUrl = currentUrl != null && !currentUrl.isBlank() ? currentUrl : referer;
        final String resolvedPageId = resolveTargetPageId(targetUrl);
        if (dashboardSettingService != null)
        {
            if (resolvedPageId != null && !resolvedPageId.isBlank())
            {
                dashboardSettingService.resetWidgetsForPage(resolvedPageId);
            }
            else
            {
                dashboardSettingService.resetDefaultWidgets();
            }
        }
        return refreshBatchView(targetUrl, hxRequest, model);
    }

    /**
     * Reorders dashboard widgets based on comma-separated ID sequence.
     *
     * @param widgetIdsCsv comma-separated widget IDs
     * @param hxRequest HTMX request header flag
     * @param currentUrl current URL header
     * @param referer referer URL header
     * @param model Spring MVC model
     * @return updated batch history view
     */
    @PostMapping({"/widgets/reorder", "/batch-history/widgets/reorder"})
    public String reorderWidgets(
        @RequestParam("widgetIds") final String widgetIdsCsv,
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        @RequestHeader(value = "HX-Current-URL", required = false) final String currentUrl,
        @RequestHeader(value = "Referer", required = false) final String referer,
        final Model model)
    {
        final String targetUrl = currentUrl != null && !currentUrl.isBlank() ? currentUrl : referer;
        final String resolvedPageId = resolveTargetPageId(targetUrl);
        if (dashboardSettingService != null && widgetIdsCsv != null && !widgetIdsCsv.isBlank())
        {
            final List<String> ids = Arrays.stream(widgetIdsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
            if (resolvedPageId != null && !resolvedPageId.isBlank())
            {
                dashboardSettingService.reorderWidgets(resolvedPageId, ids);
            }
            else
            {
                dashboardSettingService.reorderWidgets(ids);
            }
        }
        return refreshBatchView(targetUrl, hxRequest, model);
    }

    /**
     * Reorders child widgets within a container widget group according to the provided ID sequence.
     *
     * @param containerId ID of the container widget
     * @param childIdsCsv comma-separated child widget IDs
     * @param hxRequest HTMX request header flag
     * @param currentUrl current URL header
     * @param referer referer URL header
     * @param model Spring MVC model
     * @return updated batch history view
     */
    @PostMapping({"/widgets/reorder-children", "/batch-history/widgets/reorder-children"})
    public String reorderContainerChildren(
        @RequestParam("containerId") final String containerId,
        @RequestParam("childIds") final String childIdsCsv,
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        @RequestHeader(value = "HX-Current-URL", required = false) final String currentUrl,
        @RequestHeader(value = "Referer", required = false) final String referer,
        final Model model)
    {
        final String targetUrl = currentUrl != null && !currentUrl.isBlank() ? currentUrl : referer;
        final String resolvedPageId = resolveTargetPageId(targetUrl);
        if (dashboardSettingService != null && containerId != null && !containerId.isBlank() && childIdsCsv != null && !childIdsCsv.isBlank())
        {
            final List<String> ids = Arrays.stream(childIdsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
            if (resolvedPageId != null && !resolvedPageId.isBlank())
            {
                dashboardSettingService.reorderContainerChildren(resolvedPageId, containerId, ids);
            }
            else
            {
                dashboardSettingService.reorderContainerChildren(containerId, ids);
            }
        }
        return refreshBatchView(targetUrl, hxRequest, model);
    }

    /**
     * Unites two dashboard widgets into a single container if they share identical contributing runs.
     *
     * @param targetWidgetId ID of the target widget or container
     * @param sourceWidgetId ID of the source widget being dropped
     * @param hxRequest HTMX request header flag
     * @param currentUrl current URL header
     * @param referer referer URL header
     * @param model Spring MVC model
     * @return updated batch history view
     */
    @PostMapping({"/widgets/combine", "/batch-history/widgets/combine"})
    public String combineWidgets(
        @RequestParam("targetWidgetId") final String targetWidgetId,
        @RequestParam("sourceWidgetId") final String sourceWidgetId,
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        @RequestHeader(value = "HX-Current-URL", required = false) final String currentUrl,
        @RequestHeader(value = "Referer", required = false) final String referer,
        final Model model)
    {
        final String targetUrl = currentUrl != null && !currentUrl.isBlank() ? currentUrl : referer;
        final String resolvedPageId = resolveTargetPageId(targetUrl);
        if (dashboardSettingService != null && targetWidgetId != null && sourceWidgetId != null && !targetWidgetId.equals(sourceWidgetId))
        {
            final List<DashboardWidgetConfigDto> allWidgets = (resolvedPageId != null && !resolvedPageId.isBlank())
                ? dashboardSettingService.getWidgetsForPage(resolvedPageId)
                : dashboardSettingService.getDashboardWidgets();
            DashboardWidgetConfigDto target = null;
            DashboardWidgetConfigDto source = null;
            for (final DashboardWidgetConfigDto w : allWidgets)
            {
                if (w.getId().equals(targetWidgetId))
                {
                    target = w;
                }
                if (w.getId().equals(sourceWidgetId))
                {
                    source = w;
                }
            }

            if (target != null && source != null)
            {
                final String batchName = extractQueryParam(targetUrl, "batch");
                final List<TestRunEntity> runs;
                if (runRepository != null)
                {
                    if (batchName != null && !batchName.isBlank() && !"all".equalsIgnoreCase(batchName))
                    {
                        runs = runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc(batchName);
                    }
                    else
                    {
                        runs = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc();
                    }
                }
                else
                {
                    runs = Collections.emptyList();
                }

                final BatchLatestExecutionsDto execSummary = (dataService != null)
                    ? dataService.getBatchLatestTestExecutionsSummary(
                        (batchName != null && !batchName.isBlank()) ? batchName : "all",
                        runs,
                        Collections.emptySet(),
                        null)
                    : null;
                final List<BatchLatestVariationDto> latestVars = (execSummary != null && execSummary.getLatestVariations() != null)
                    ? execSummary.getLatestVariations()
                    : Collections.emptyList();

                final List<String> globalIdentity = (dashboardSettingService != null)
                    ? dashboardSettingService.getExecutionIdentityDimensions()
                    : ReportDashboardSettingService.DEFAULT_EXECUTION_IDENTITY_DIMENSIONS;
                final List<String> targetIdentity = target.getEffectiveExecutionIdentityDimensions(globalIdentity);
                final List<String> sourceIdentity = source.getEffectiveExecutionIdentityDimensions(globalIdentity);
                final boolean sameIdentity = Objects.equals(targetIdentity, sourceIdentity);

                if (target.isRuns() || source.isRuns())
                {
                    model.addAttribute("widgetWarningMessage", "Cannot unite widgets: Runs list widget requires full width and cannot be combined into a container.");
                }
                else if (!sameIdentity)
                {
                    model.addAttribute("widgetWarningMessage", "Cannot unite widgets: they have different execution identities.");
                }
                else
                {
                    final boolean sameRuns = (widgetAggregationService != null)
                        ? widgetAggregationService.areBasedOnSameContributingRuns(target, source, runs, latestVars, dataService)
                        : true;

                    if (sameRuns)
                    {
                        if (resolvedPageId != null && !resolvedPageId.isBlank())
                        {
                            dashboardSettingService.combineWidgets(resolvedPageId, targetWidgetId, sourceWidgetId);
                        }
                        else
                        {
                            dashboardSettingService.combineWidgets(targetWidgetId, sourceWidgetId);
                        }
                    }
                    else
                    {
                        model.addAttribute("widgetWarningMessage", "Cannot unite widgets: they are based on different contributing runs.");
                    }
                }
            }
        }
        return refreshBatchView(targetUrl, hxRequest, model);
    }

    /**
     * Separates a container widget back into its individual child widgets.
     *
     * @param containerId ID of the container widget to separate
     * @param hxRequest HTMX request header flag
     * @param currentUrl current URL header
     * @param referer referer URL header
     * @param model Spring MVC model
     * @return updated batch history view
     */
    @PostMapping({"/widgets/separate", "/batch-history/widgets/separate"})
    public String separateContainer(
        @RequestParam("containerId") final String containerId,
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        @RequestHeader(value = "HX-Current-URL", required = false) final String currentUrl,
        @RequestHeader(value = "Referer", required = false) final String referer,
        final Model model)
    {
        final String targetUrl = currentUrl != null && !currentUrl.isBlank() ? currentUrl : referer;
        final String resolvedPageId = resolveTargetPageId(targetUrl);
        if (dashboardSettingService != null && containerId != null && !containerId.isBlank())
        {
            if (resolvedPageId != null && !resolvedPageId.isBlank())
            {
                dashboardSettingService.separateContainer(resolvedPageId, containerId);
            }
            else
            {
                dashboardSettingService.separateContainer(containerId);
            }
        }
        return refreshBatchView(targetUrl, hxRequest, model);
    }

    /**
     * Separates a single child widget out of a container into an independent top-level widget.
     *
     * @param containerId optional container ID
     * @param widgetId ID of the child widget to extract
     * @param hxRequest HTMX request header flag
     * @param currentUrl current URL header
     * @param referer referer URL header
     * @param model Spring MVC model
     * @return updated batch history view
     */
    @PostMapping({"/widgets/separate-child", "/batch-history/widgets/separate-child"})
    public String separateChildWidget(
        @RequestParam(name = "containerId", required = false) final String containerId,
        @RequestParam("widgetId") final String widgetId,
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        @RequestHeader(value = "HX-Current-URL", required = false) final String currentUrl,
        @RequestHeader(value = "Referer", required = false) final String referer,
        final Model model)
    {
        final String targetUrl = currentUrl != null && !currentUrl.isBlank() ? currentUrl : referer;
        final String resolvedPageId = resolveTargetPageId(targetUrl);
        if (dashboardSettingService != null && widgetId != null && !widgetId.isBlank())
        {
            if (resolvedPageId != null && !resolvedPageId.isBlank())
            {
                dashboardSettingService.separateChildWidget(resolvedPageId, containerId, widgetId);
            }
            else
            {
                dashboardSettingService.separateChildWidget(containerId, widgetId);
            }
        }
        return refreshBatchView(targetUrl, hxRequest, model);
    }

    /**
     * Renames a dashboard widget or container group.
     *
     * @param widgetId ID of the widget or container to rename
     * @param title new title for the widget
     * @param pageIdParam optional page ID parameter
     * @param hxRequest HTMX request header flag
     * @param currentUrl current URL header
     * @param referer referer URL header
     * @param model Spring MVC model
     * @return updated batch history or custom page view
     */
    @PostMapping({"/widgets/rename", "/batch-history/widgets/rename"})
    public String renameWidget(
        @RequestParam("widgetId") final String widgetId,
        @RequestParam("title") final String title,
        @RequestParam(name = "pageId", required = false) final String pageIdParam,
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        @RequestHeader(value = "HX-Current-URL", required = false) final String currentUrl,
        @RequestHeader(value = "Referer", required = false) final String referer,
        final Model model)
    {
        final String targetUrl = currentUrl != null && !currentUrl.isBlank() ? currentUrl : referer;
        final String resolvedPageId = (pageIdParam != null && !pageIdParam.isBlank())
            ? pageIdParam
            : resolveTargetPageId(targetUrl);
        if (dashboardSettingService != null && widgetId != null && !widgetId.isBlank() && title != null && !title.isBlank())
        {
            if (resolvedPageId != null && !resolvedPageId.isBlank())
            {
                dashboardSettingService.renameWidget(resolvedPageId, widgetId, title.trim());
            }
            else
            {
                dashboardSettingService.renameWidget(widgetId, title.trim());
            }
        }
        return refreshBatchView(targetUrl, hxRequest, model);
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

    private Set<String> parseFilterCsv(final String csv)
    {
        if (csv == null || csv.isBlank())
        {
            return Collections.emptySet();
        }
        return Arrays.stream(csv.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static List<String> parseGroupDimensions(final String groupBy)
    {
        if (groupBy == null || groupBy.isBlank())
        {
            return new ArrayList<>();
        }
        final Set<String> result = new LinkedHashSet<>();
        final String[] tokens = groupBy.split(",");
        for (final String t : tokens)
        {
            final String clean = t.trim().toLowerCase(Locale.ROOT);
            if ("none".equals(clean) || "all".equals(clean))
            {
                continue;
            }
            if ("tag".equals(clean) || "batch".equals(clean))
            {
                result.add("batch");
            }
            else if ("environment".equals(clean) || "env".equals(clean))
            {
                result.add("environment");
            }
            else if ("locale".equals(clean) || "locales".equals(clean))
            {
                result.add("locale");
            }
            else if ("browser".equals(clean) || "browsers".equals(clean))
            {
                result.add("browser");
            }
            else if ("area".equals(clean) || "areas".equals(clean))
            {
                result.add("area");
            }
            else if (clean.startsWith("tag:"))
            {
                final String tagSub = t.trim().substring(4).trim();
                if (!tagSub.isEmpty())
                {
                    result.add(tagSub);
                }
            }
            else if (!clean.isEmpty())
            {
                result.add(t.trim());
            }
        }
        return new ArrayList<>(result);
    }

    public static String buildGroupLabel(final List<String> dimensions)
    {
        if (dimensions == null || dimensions.isEmpty())
        {
            return "Known Batches";
        }
        final List<String> labels = new ArrayList<>();
        for (final String dim : dimensions)
        {
            if ("environment".equalsIgnoreCase(dim))
            {
                labels.add("Environments");
            }
            else if ("locale".equalsIgnoreCase(dim))
            {
                labels.add("Locales");
            }
            else if ("browser".equalsIgnoreCase(dim))
            {
                labels.add("Browsers");
            }
            else if ("area".equalsIgnoreCase(dim))
            {
                labels.add("Areas");
            }
            else if ("batch".equalsIgnoreCase(dim))
            {
                labels.add("Batches");
            }
            else
            {
                final String cap = dim.substring(0, 1).toUpperCase(Locale.ROOT) + dim.substring(1);
                labels.add(cap + " Tags");
            }
        }
        if (labels.size() == 1)
        {
            return labels.get(0);
        }
        if (labels.size() == 2)
        {
            return labels.get(0) + " & " + labels.get(1);
        }
        final String allExceptLast = String.join(", ", labels.subList(0, labels.size() - 1));
        return allExceptLast + " & " + labels.get(labels.size() - 1);
    }

    public static String buildUrlWithParams(final String basePath, final Map<String, String> params)
    {
        final String effectiveBase = (basePath != null && !basePath.isBlank()) ? basePath : "/";
        final StringBuilder sb = new StringBuilder(effectiveBase);
        boolean first = !effectiveBase.contains("?");
        if (params != null)
        {
            for (final Map.Entry<String, String> entry : params.entrySet())
            {
                if (entry.getValue() == null || entry.getValue().isBlank())
                {
                    continue;
                }
                sb.append(first ? "?" : "&");
                sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                sb.append("=");
                sb.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8).replace("%2C", ","));
                first = false;
            }
        }
        return sb.toString();
    }

    public static String computeToggleUrl(
        final String basePath,
        final Map<String, String> baseParams,
        final List<String> activeDimensions,
        final String targetDimension)
    {
        final List<String> next = new ArrayList<>(activeDimensions != null ? activeDimensions : Collections.emptyList());
        int existingIndex = -1;
        for (int i = 0; i < next.size(); i++)
        {
            if (next.get(i).equalsIgnoreCase(targetDimension))
            {
                existingIndex = i;
                break;
            }
        }
        if (existingIndex >= 0)
        {
            next.remove(existingIndex);
        }
        else
        {
            next.add(targetDimension);
        }

        final Map<String, String> params = new LinkedHashMap<>(baseParams != null ? baseParams : Collections.emptyMap());
        params.put("groupBy", next.isEmpty() ? "none" : String.join(",", next));
        return buildUrlWithParams(basePath, params);
    }

    public static String computeToggleUrl(final List<String> activeDimensions, final String targetDimension)
    {
        return computeToggleUrl("/", Collections.emptyMap(), activeDimensions, targetDimension);
    }

    public List<GroupingTagOptionDto> buildGroupingTagOptions(
        final String basePath,
        final Map<String, String> baseParams,
        final List<String> activeDimensions,
        final List<String> excludedDimensions)
    {
        if (dataService == null)
        {
            return Collections.emptyList();
        }
        final List<String> tagKeys = dataService.getAllTagKeys();
        if (tagKeys == null || tagKeys.isEmpty())
        {
            return Collections.emptyList();
        }
        final List<GroupingTagOptionDto> options = new ArrayList<>();
        final Set<String> builtInKeywords = Set.of("browser", "browsers", "none", "all");
        for (final String key : tagKeys)
        {
            if (key == null || key.isBlank())
            {
                continue;
            }
            final String trimmedKey = key.trim();
            if (builtInKeywords.contains(trimmedKey.toLowerCase(Locale.ROOT)))
            {
                continue;
            }
            if (excludedDimensions != null && excludedDimensions.stream().anyMatch(trimmedKey::equalsIgnoreCase))
            {
                continue;
            }
            final boolean isActive = activeDimensions != null && activeDimensions.stream().anyMatch(trimmedKey::equalsIgnoreCase);
            final String toggleUrl = computeToggleUrl(basePath, baseParams, activeDimensions, trimmedKey);
            final String displayName = "Tag (" + trimmedKey + ")";
            options.add(new GroupingTagOptionDto(trimmedKey, displayName, toggleUrl, isActive));
        }
        return options;
    }

    public List<GroupingTagOptionDto> buildGroupingTagOptions(final List<String> activeDimensions)
    {
        return buildGroupingTagOptions("/", Collections.emptyMap(), activeDimensions, Collections.emptyList());
    }

    private boolean matchesAllDimensions(
        final TestRunEntity r,
        final List<String> dimensions,
        final String[] parts)
    {
        for (int i = 0; i < dimensions.size(); i++)
        {
            final String dim = dimensions.get(i);
            final String expectedVal = (i < parts.length) ? parts[i].trim() : "";
            if (expectedVal.isEmpty())
            {
                continue;
            }
            if ("environment".equals(dim))
            {
                final String rEnv = (r.getEnvironment() != null && !r.getEnvironment().isBlank()) ? r.getEnvironment().trim() : "Default";
                if (!expectedVal.equalsIgnoreCase(rEnv))
                {
                    return false;
                }
            }
            else if ("locale".equals(dim))
            {
                if (r.getLocalesCsv() != null && !r.getLocalesCsv().isBlank())
                {
                    final boolean match = Arrays.stream(r.getLocalesCsv().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .anyMatch(expectedVal::equalsIgnoreCase);
                    if (!match)
                    {
                        return false;
                    }
                }
                else if (!"Default".equalsIgnoreCase(expectedVal))
                {
                    return false;
                }
            }
            else if ("browser".equals(dim))
            {
                if (r.getBrowsersCsv() != null && !r.getBrowsersCsv().isBlank())
                {
                    final boolean match = Arrays.stream(r.getBrowsersCsv().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .anyMatch(expectedVal::equalsIgnoreCase);
                    if (!match)
                    {
                        return false;
                    }
                }
                else if (!("Chrome".equalsIgnoreCase(expectedVal) || "Default".equalsIgnoreCase(expectedVal)))
                {
                    return false;
                }
            }
            else if ("area".equals(dim))
            {
                final List<String> rAreas = (dataService != null) ? dataService.getRunAreas(r) : List.of(AuraReportDataService.normalizeArea("@Browsing (default)"));
                final boolean match = rAreas.stream().anyMatch(a ->
                    AuraReportDataService.normalizeArea(a).equalsIgnoreCase(AuraReportDataService.normalizeArea(expectedVal))
                );
                if (!match)
                {
                    return false;
                }
            }
            else if ("batch".equalsIgnoreCase(dim))
            {
                final String rBatch = (r.getBatchName() != null && !r.getBatchName().isBlank()) ? r.getBatchName().trim() : "Default";
                if (!expectedVal.equalsIgnoreCase(rBatch))
                {
                    return false;
                }
            }
            else
            {
                final List<String> runTagVals = (dataService != null) ? dataService.getRunTagValues(r, dim) : Collections.emptyList();
                final boolean match;
                if (runTagVals.isEmpty())
                {
                    match = "Default".equalsIgnoreCase(expectedVal) || ("Not " + dim).equalsIgnoreCase(expectedVal);
                }
                else if (dataService != null && dataService.isBooleanTag(dim))
                {
                    final boolean hasTrue = runTagVals.stream().anyMatch(v -> "true".equalsIgnoreCase(v) || dim.equalsIgnoreCase(v));
                    if (dim.equalsIgnoreCase(expectedVal) || "true".equalsIgnoreCase(expectedVal))
                    {
                        match = hasTrue;
                    }
                    else if (("Not " + dim).equalsIgnoreCase(expectedVal) || "false".equalsIgnoreCase(expectedVal) || "Default".equalsIgnoreCase(expectedVal))
                    {
                        match = !hasTrue;
                    }
                    else
                    {
                        match = false;
                    }
                }
                else
                {
                    match = runTagVals.stream().anyMatch(expectedVal::equalsIgnoreCase)
                        || ("Default".equalsIgnoreCase(expectedVal) && runTagVals.stream().anyMatch("Default"::equalsIgnoreCase));
                }
                if (!match)
                {
                    return false;
                }
            }
        }
        return true;
    }

    public String batchHistory(
        final String batchName,
        final Integer limit,
        final String hxRequest,
        final Model model)
    {
        return batchHistory(batchName, null, null, limit, null, null, hxRequest, model);
    }

    public String batchHistory(
        final String batchName,
        final Integer limit,
        final String excludeRuns,
        final String olderThanDate,
        final String hxRequest,
        final Model model)
    {
        return batchHistory(batchName, null, null, limit, excludeRuns, olderThanDate, null, null, null, null, null, null, null, hxRequest, model, Collections.emptyMap());
    }

    public String batchHistory(
        final String batchName,
        final String groupType,
        final String groupValue,
        final Integer limit,
        final String excludeRuns,
        final String olderThanDate,
        final String hxRequest,
        final Model model)
    {
        return batchHistory(batchName, groupType, groupValue, limit, excludeRuns, olderThanDate, null, null, null, null, null, null, null, hxRequest, model, Collections.emptyMap());
    }

    public String batchHistory(
        final String batchName,
        final String groupType,
        final String groupValue,
        final String groupBy,
        final Integer limit,
        final String excludeRuns,
        final String olderThanDate,
        final String hxRequest,
        final Model model)
    {
        return batchHistory(
            batchName, groupType, groupValue, groupBy, limit, excludeRuns, olderThanDate,
            null, null, null, null, null, null, null, null, null, null, null, null,
            null,
            hxRequest, model, Collections.emptyMap()
        );
    }

    public String batchHistory(
        final String batchName,
        final String groupType,
        final String groupValue,
        final Integer limit,
        final String excludeRuns,
        final String olderThanDate,
        final String runBrowsers,
        final String runLocales,
        final String runEnvs,
        final String runAreas,
        final String runTags,
        final String hxRequest,
        final Model model)
    {
        return batchHistory(batchName, groupType, groupValue, limit, excludeRuns, olderThanDate, null, null, runBrowsers, runLocales, runEnvs, runAreas, runTags, hxRequest, model, Collections.emptyMap());
    }

    public String batchHistory(
        final String batchName,
        final String groupType,
        final String groupValue,
        final Integer limit,
        final String excludeRuns,
        final String olderThanDate,
        final String startDate,
        final String endDate,
        final String runBrowsers,
        final String runLocales,
        final String runEnvs,
        final String runAreas,
        final String runTags,
        final String hxRequest,
        final Model model,
        final Map<String, String> allRequestParams)
    {
        return batchHistory(
            batchName, groupType, groupValue, null, limit, excludeRuns, olderThanDate, startDate, endDate,
            runBrowsers, null,
            runLocales, null,
            runEnvs, null,
            runAreas, null,
            runTags, null,
            null,
            hxRequest, model, allRequestParams
        );
    }

    public String batchHistory(
        final String batchName,
        final String groupType,
        final String groupValue,
        final Integer limit,
        final String excludeRuns,
        final String olderThanDate,
        final String startDate,
        final String endDate,
        final String runBrowsers,
        final String excludeRunBrowsers,
        final String runLocales,
        final String excludeRunLocales,
        final String runEnvs,
        final String excludeRunEnvs,
        final String runAreas,
        final String excludeRunAreas,
        final String runTags,
        final String excludeRunTags,
        final String hxRequest,
        final Model model,
        final Map<String, String> allRequestParams)
    {
        return batchHistory(
            batchName, groupType, groupValue, null, limit, excludeRuns, olderThanDate, startDate, endDate,
            runBrowsers, excludeRunBrowsers,
            runLocales, excludeRunLocales,
            runEnvs, excludeRunEnvs,
            runAreas, excludeRunAreas,
            runTags, excludeRunTags,
            null,
            hxRequest, model, allRequestParams
        );
    }

    @GetMapping({"/batch-history", "/fragments/batch-history"})
    public String batchHistory(
        @RequestParam(name = "batchName", required = false) final String batchName,
        @RequestParam(name = "groupType", required = false) final String groupType,
        @RequestParam(name = "groupValue", required = false) final String groupValue,
        @RequestParam(name = "groupBy", required = false) final String groupBy,
        @RequestParam(name = "limit", required = false) final Integer limit,
        @RequestParam(name = "excludeRuns", required = false) final String excludeRuns,
        @RequestParam(name = "olderThanDate", required = false) final String olderThanDate,
        @RequestParam(name = "startDate", required = false) final String startDate,
        @RequestParam(name = "endDate", required = false) final String endDate,
        @RequestParam(name = "runBrowsers", required = false) final String runBrowsers,
        @RequestParam(name = "excludeRunBrowsers", required = false) final String excludeRunBrowsers,
        @RequestParam(name = "runLocales", required = false) final String runLocales,
        @RequestParam(name = "excludeRunLocales", required = false) final String excludeRunLocales,
        @RequestParam(name = "runEnvs", required = false) final String runEnvs,
        @RequestParam(name = "excludeRunEnvs", required = false) final String excludeRunEnvs,
        @RequestParam(name = "runAreas", required = false) final String runAreas,
        @RequestParam(name = "excludeRunAreas", required = false) final String excludeRunAreas,
        @RequestParam(name = "runTags", required = false) final String runTags,
        @RequestParam(name = "excludeRunTags", required = false) final String excludeRunTags,
        @RequestParam(name = "asOfRunId", required = false) final String asOfRunId,
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        final Model model,
        @RequestParam(required = false) final Map<String, String> allRequestParams)
    {
        final Map<String, String> effectiveAllParams = (allRequestParams != null) ? new LinkedHashMap<>(allRequestParams) : new LinkedHashMap<>();
        final String effectiveAsOfRunParam = (asOfRunId != null && !asOfRunId.isBlank())
            ? asOfRunId.trim()
            : effectiveAllParams.get("asOfRunId");
        if (runLocales != null && !runLocales.isBlank() && !effectiveAllParams.containsKey("runTag_Locale"))
        {
            effectiveAllParams.put("runTag_Locale", runLocales);
        }
        if (excludeRunLocales != null && !excludeRunLocales.isBlank() && !effectiveAllParams.containsKey("excludeRunTag_Locale"))
        {
            effectiveAllParams.put("excludeRunTag_Locale", excludeRunLocales);
        }
        if (runAreas != null && !runAreas.isBlank() && !effectiveAllParams.containsKey("runTag_Area"))
        {
            effectiveAllParams.put("runTag_Area", runAreas);
        }
        if (excludeRunAreas != null && !excludeRunAreas.isBlank() && !effectiveAllParams.containsKey("excludeRunTag_Area"))
        {
            effectiveAllParams.put("excludeRunTag_Area", excludeRunAreas);
        }
        final Set<String> selectedRunBrowsers = parseFilterCsv(runBrowsers);
        final Set<String> excludedRunBrowsers = parseFilterCsv(excludeRunBrowsers);
        final Set<String> selectedRunLocales = parseFilterCsv(runLocales);
        final Set<String> excludedRunLocales = parseFilterCsv(excludeRunLocales);
        final Set<String> selectedRunEnvs = parseFilterCsv(runEnvs);
        final Set<String> excludedRunEnvs = parseFilterCsv(excludeRunEnvs);
        final Set<String> selectedRunAreas = parseFilterCsv(runAreas);
        final Set<String> excludedRunAreas = parseFilterCsv(excludeRunAreas);
        final Set<String> selectedRunTags = parseFilterCsv(runTags);
        final Set<String> excludedRunTags = parseFilterCsv(excludeRunTags);

        final List<String> groupDimensions = parseGroupDimensions(groupType);
        final String effectiveGroupType;
        final String effectiveGroupValue;
        if (!groupDimensions.isEmpty())
        {
            effectiveGroupType = String.join(",", groupDimensions);
            effectiveGroupValue = groupValue != null ? groupValue.trim() : "";
        }
        else if ("all".equalsIgnoreCase(batchName))
        {
            effectiveGroupType = "all";
            effectiveGroupValue = "";
        }
        else if (batchName != null && !batchName.isBlank())
        {
            effectiveGroupType = "batch";
            effectiveGroupValue = batchName.trim();
            groupDimensions.add("batch");
        }
        else
        {
            effectiveGroupType = "all";
            effectiveGroupValue = "";
        }

        final String rawGroupBy = (groupBy != null && !groupBy.isBlank())
            ? groupBy
            : effectiveAllParams.get("groupBy");
        final String effectiveGroupBy;
        if (rawGroupBy != null && !rawGroupBy.isBlank())
        {
            effectiveGroupBy = rawGroupBy.trim();
        }
        else if (effectiveGroupValue.isEmpty() && dashboardSettingService != null)
        {
            effectiveGroupBy = dashboardSettingService.getPinnedGrouping();
        }
        else if (!effectiveGroupValue.isEmpty() && dashboardSettingService != null)
        {
            effectiveGroupBy = dashboardSettingService.getPinnedInnerGrouping(effectiveGroupType);
        }
        else
        {
            effectiveGroupBy = null;
        }

        final Set<String> excludedRunIds = new HashSet<>();
        if (excludeRuns != null && !excludeRuns.isBlank())
        {
            for (final String rId : excludeRuns.split(","))
            {
                final String trimmed = rId.trim();
                if (!trimmed.isEmpty())
                {
                    excludedRunIds.add(trimmed);
                }
            }
        }

        final String effectiveStartDate = (startDate != null && !startDate.isBlank())
            ? startDate.trim()
            : ((olderThanDate != null && !olderThanDate.isBlank()) ? olderThanDate.trim() : null);
        final String effectiveEndDate = (endDate != null && !endDate.isBlank()) ? endDate.trim() : null;

        Long fromTimestampMs = null;
        if (effectiveStartDate != null)
        {
            try
            {
                final LocalDate parsedStart = LocalDate.parse(effectiveStartDate);
                fromTimestampMs = parsedStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
            catch (final DateTimeParseException e)
            {
                LOG.warn("Failed to parse startDate parameter '{}': {}", effectiveStartDate, e.getMessage());
            }
        }

        Long toTimestampMs = null;
        if (effectiveEndDate != null)
        {
            try
            {
                final LocalDate parsedEnd = LocalDate.parse(effectiveEndDate);
                toTimestampMs = parsedEnd.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
            catch (final DateTimeParseException e)
            {
                LOG.warn("Failed to parse endDate parameter '{}': {}", effectiveEndDate, e.getMessage());
            }
        }

        final List<TestRunEntity> allRuns;
        final String scopeTitle;
        final String backUrl;
        final Optional<TestBatchEntity> batchOpt;
        final String[] parts;

        if (groupDimensions.size() == 1)
        {
            final String singleDim = groupDimensions.get(0);
            parts = new String[] { effectiveGroupValue };
            if ("environment".equals(singleDim))
            {
                final List<TestRunEntity> rawRuns = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc();
                allRuns = rawRuns.stream()
                    .filter(r -> {
                        final String rEnv = (r.getEnvironment() != null && !r.getEnvironment().isBlank()) ? r.getEnvironment().trim() : "Default";
                        return effectiveGroupValue.equalsIgnoreCase(rEnv);
                    })
                    .collect(Collectors.toList());
                scopeTitle = effectiveGroupValue.isBlank() ? "Default" : effectiveGroupValue;
                backUrl = "/?groupBy=environment";
                batchOpt = Optional.empty();
            }
            else if ("locale".equals(singleDim))
            {
                final List<TestRunEntity> rawRuns = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc();
                allRuns = rawRuns.stream()
                    .filter(r -> {
                        if (r.getLocalesCsv() != null && !r.getLocalesCsv().isBlank())
                        {
                            return Arrays.stream(r.getLocalesCsv().split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .anyMatch(effectiveGroupValue::equalsIgnoreCase);
                        }
                        return "Default".equalsIgnoreCase(effectiveGroupValue);
                    })
                    .collect(Collectors.toList());
                scopeTitle = effectiveGroupValue.isBlank() ? "Default" : effectiveGroupValue;
                backUrl = "/?groupBy=locale";
                batchOpt = Optional.empty();
            }
            else if ("browser".equals(singleDim))
            {
                final List<TestRunEntity> rawRuns = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc();
                allRuns = rawRuns.stream()
                    .filter(r -> {
                        if (r.getBrowsersCsv() != null && !r.getBrowsersCsv().isBlank())
                        {
                            return Arrays.stream(r.getBrowsersCsv().split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .anyMatch(effectiveGroupValue::equalsIgnoreCase);
                        }
                        return "Chrome".equalsIgnoreCase(effectiveGroupValue) || "Default".equalsIgnoreCase(effectiveGroupValue);
                    })
                    .collect(Collectors.toList());
                scopeTitle = effectiveGroupValue.isBlank() ? "Chrome" : effectiveGroupValue;
                backUrl = "/?groupBy=browser";
                batchOpt = Optional.empty();
            }
            else if ("area".equals(singleDim))
            {
                final List<TestRunEntity> rawRuns = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc();
                allRuns = rawRuns.stream()
                    .filter(r -> {
                        final List<String> rAreas = (dataService != null) ? dataService.getRunAreas(r) : List.of(AuraReportDataService.normalizeArea("@Browsing (default)"));
                        return rAreas.stream().anyMatch(a ->
                            AuraReportDataService.normalizeArea(a).equalsIgnoreCase(AuraReportDataService.normalizeArea(effectiveGroupValue))
                        );
                    })
                    .collect(Collectors.toList());
                scopeTitle = effectiveGroupValue.isBlank() ? AuraReportDataService.normalizeArea("@Browsing (default)") : effectiveGroupValue;
                backUrl = "/?groupBy=area";
                batchOpt = Optional.empty();
            }
            else if ("batch".equalsIgnoreCase(singleDim))
            {
                allRuns = runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc(effectiveGroupValue);
                scopeTitle = effectiveGroupValue.isBlank() ? "Default" : effectiveGroupValue;
                backUrl = "/?groupBy=batch";
                batchOpt = batchRepository.findById(effectiveGroupValue);
            }
            else
            {
                final List<TestRunEntity> rawRuns = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc();
                allRuns = rawRuns.stream()
                    .filter(r -> {
                        final List<String> rTagVals = (dataService != null) ? dataService.getRunTagValues(r, singleDim) : Collections.emptyList();
                        if (rTagVals.isEmpty())
                        {
                            return "Default".equalsIgnoreCase(effectiveGroupValue) || ("Not " + singleDim).equalsIgnoreCase(effectiveGroupValue);
                        }
                        if (dataService != null && dataService.isBooleanTag(singleDim, rawRuns))
                        {
                            final boolean hasTrue = rTagVals.stream().anyMatch(v -> "true".equalsIgnoreCase(v) || singleDim.equalsIgnoreCase(v));
                            if (singleDim.equalsIgnoreCase(effectiveGroupValue) || "true".equalsIgnoreCase(effectiveGroupValue))
                            {
                                return hasTrue;
                            }
                            if (("Not " + singleDim).equalsIgnoreCase(effectiveGroupValue) || "false".equalsIgnoreCase(effectiveGroupValue) || "Default".equalsIgnoreCase(effectiveGroupValue))
                            {
                                return !hasTrue;
                            }
                        }
                        return rTagVals.stream().anyMatch(effectiveGroupValue::equalsIgnoreCase)
                            || ("Default".equalsIgnoreCase(effectiveGroupValue) && rTagVals.stream().anyMatch("Default"::equalsIgnoreCase));
                    })
                    .collect(Collectors.toList());
                scopeTitle = effectiveGroupValue.isBlank() ? "Default" : effectiveGroupValue;
                backUrl = "/?groupBy=" + singleDim;
                batchOpt = Optional.empty();
            }
        }
        else if (groupDimensions.size() > 1)
        {
            if (effectiveGroupValue.contains(" • "))
            {
                parts = effectiveGroupValue.split(" • ");
            }
            else if (effectiveGroupValue.contains(" / "))
            {
                parts = effectiveGroupValue.split(" / ");
            }
            else if (effectiveGroupValue.contains(" | "))
            {
                parts = effectiveGroupValue.split(" \\| ");
            }
            else
            {
                parts = new String[] { effectiveGroupValue };
            }

            final List<TestRunEntity> rawRuns = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc();
            allRuns = rawRuns.stream()
                .filter(r -> matchesAllDimensions(r, groupDimensions, parts))
                .collect(Collectors.toList());
            scopeTitle = effectiveGroupValue.isBlank() ? "Grouped Runs" : effectiveGroupValue;
            backUrl = "/?groupBy=" + effectiveGroupType;

            if (groupDimensions.contains("batch"))
            {
                final int batchIdx = groupDimensions.indexOf("batch");
                final String batchVal = (batchIdx >= 0 && batchIdx < parts.length) ? parts[batchIdx].trim() : "";
                batchOpt = (!batchVal.isEmpty()) ? batchRepository.findById(batchVal) : Optional.empty();
            }
            else
            {
                batchOpt = Optional.empty();
            }
        }
        else // "all"
        {
            parts = new String[0];
            allRuns = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc();
            scopeTitle = "All Runs";
            backUrl = null;
            batchOpt = Optional.empty();
        }

        if (dataService != null)
        {
            dataService.populateRunTags(allRuns);
        }

        final List<String> activeDimensions;
        final List<BatchSummaryDto> batches;
        final String groupLabel;

        if (effectiveGroupValue.isEmpty())
        {
            activeDimensions = parseGroupDimensions(effectiveGroupBy);
            if (!activeDimensions.isEmpty())
            {
                final BatchOverviewDataDto overviewData = (activeDimensions.size() == 1)
                    ? dataService.getGroupOverviewData(activeDimensions.get(0))
                    : dataService.getGroupOverviewData(activeDimensions);
                final BatchOverviewDataDto safeOverviewData = (overviewData != null)
                    ? overviewData
                    : new BatchOverviewDataDto(List.of(), List.of(), List.of(), List.of(), List.of());
                batches = safeOverviewData.getBatches() != null ? safeOverviewData.getBatches() : Collections.emptyList();
                groupLabel = buildGroupLabel(activeDimensions);
            }
            else
            {
                batches = Collections.emptyList();
                groupLabel = null;
            }
        }
        else
        {
            final List<String> parsedInner = parseGroupDimensions(effectiveGroupBy);
            parsedInner.removeIf(dim -> groupDimensions.stream().anyMatch(dim::equalsIgnoreCase));
            activeDimensions = parsedInner;

            if (!activeDimensions.isEmpty() && dataService != null)
            {
                final Map<String, String> parentCriteria = new LinkedHashMap<>();
                for (int i = 0; i < groupDimensions.size() && i < parts.length; i++)
                {
                    parentCriteria.put(groupDimensions.get(i), parts[i]);
                }
                final BatchOverviewDataDto overviewData = dataService.getGroupOverviewData(
                    activeDimensions, allRuns, effectiveGroupType, effectiveGroupValue, parentCriteria
                );
                final BatchOverviewDataDto safeOverviewData = (overviewData != null)
                    ? overviewData
                    : new BatchOverviewDataDto(List.of(), List.of(), List.of(), List.of(), List.of());
                batches = safeOverviewData.getBatches() != null ? safeOverviewData.getBatches() : Collections.emptyList();
                groupLabel = buildGroupLabel(activeDimensions);
            }
            else
            {
                batches = Collections.emptyList();
                groupLabel = null;
            }
        }

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

        final Set<String> effectiveRunBrowsers = new LinkedHashSet<>(selectedRunBrowsers);
        final Set<String> effectiveRunLocales = new LinkedHashSet<>(selectedRunLocales);
        final Set<String> effectiveRunEnvs = new LinkedHashSet<>(selectedRunEnvs);
        final Set<String> effectiveRunAreas = new LinkedHashSet<>(selectedRunAreas);
        final Set<String> effectiveRunTags = new LinkedHashSet<>(selectedRunTags);

        for (int i = 0; i < groupDimensions.size() && i < parts.length; i++)
        {
            final String dim = groupDimensions.get(i);
            final String val = parts[i].trim();
            if (val.isEmpty())
            {
                continue;
            }
            if ("browser".equalsIgnoreCase(dim))
            {
                effectiveRunBrowsers.add(AuraReportDataService.normalizeBrowser(val));
            }
            else if ("locale".equalsIgnoreCase(dim))
            {
                effectiveRunLocales.add(AuraReportDataService.normalizeLocation(val));
            }
            else if ("environment".equalsIgnoreCase(dim))
            {
                effectiveRunEnvs.add(val);
            }
            else if ("area".equalsIgnoreCase(dim))
            {
                effectiveRunAreas.add(AuraReportDataService.normalizeArea(val));
            }
        }

        final List<TestRunEntity> chronoRuns = new ArrayList<>(runs);
        Collections.reverse(chronoRuns);

        final Map<String, List<AreaRunPointDto>> areaPointsMap = new LinkedHashMap<>();
        final Map<String, String> runAreasMap = new HashMap<>();
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
                if (rReport.getAreaSummaries() != null && !rReport.getAreaSummaries().isEmpty())
                {
                    final List<String> runAreaList = new ArrayList<>();
                    for (final AreaSummaryDto area : rReport.getAreaSummaries())
                    {
                        final String areaName = area.getAreaName() != null ? area.getAreaName() : "Browsing (default)";
                        final String normArea = AuraReportDataService.normalizeArea(areaName);
                        runAreaList.add(normArea);
                        if (!effectiveRunAreas.isEmpty())
                        {
                            final boolean matchArea = effectiveRunAreas.stream().anyMatch(ra ->
                                AuraReportDataService.normalizeArea(ra).equalsIgnoreCase(normArea)
                            );
                            if (!matchArea)
                            {
                                continue;
                            }
                        }
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
                    runAreasMap.put(r.getId(), String.join(",", runAreaList));
                }
                else
                {
                    final List<String> fallbackAreas = (dataService != null) ? dataService.getRunAreas(r) : List.of(AuraReportDataService.normalizeArea("@Browsing (default)"));
                    runAreasMap.put(r.getId(), String.join(",", fallbackAreas));
                }
            }
            else
            {
                final List<String> fallbackAreas = (dataService != null) ? dataService.getRunAreas(r) : List.of(AuraReportDataService.normalizeArea("@Browsing (default)"));
                runAreasMap.put(r.getId(), String.join(",", fallbackAreas));
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

        final Set<String> candidateBrowsersSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        final Set<String> candidateLocalesSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        final Set<String> candidateEnvsSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        final Set<String> candidateTagsSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        final Set<String> candidateAreasSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        final List<String> allRunIds = allRuns.stream().map(TestRunEntity::getId).filter(Objects::nonNull).collect(Collectors.toList());
        final Map<String, List<String>> candidateTagsByType = (dataService != null) ? dataService.getCandidateTagValues(allRunIds) : Collections.emptyMap();
        if (candidateTagsByType.containsKey("batch"))
        {
            candidateTagsSet.addAll(candidateTagsByType.get("batch"));
        }

        for (final TestRunEntity r : allRuns)
        {
            if (r.getBrowsersCsv() != null && !r.getBrowsersCsv().isBlank())
            {
                Arrays.stream(r.getBrowsersCsv().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(candidateBrowsersSet::add);
            }
            if (r.getLocalesCsv() != null && !r.getLocalesCsv().isBlank())
            {
                Arrays.stream(r.getLocalesCsv().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(candidateLocalesSet::add);
            }
            if (r.getEnvironment() != null && !r.getEnvironment().isBlank())
            {
                candidateEnvsSet.add(r.getEnvironment().trim());
            }
            if (r.getBatchName() != null && !r.getBatchName().isBlank())
            {
                candidateTagsSet.add(r.getBatchName().trim());
            }
        }
        for (final String aCsv : runAreasMap.values())
        {
            if (aCsv != null && !aCsv.isBlank())
            {
                Arrays.stream(aCsv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(AuraReportDataService::normalizeArea)
                    .forEach(candidateAreasSet::add);
            }
        }
        if (candidateBrowsersSet.isEmpty())
        {
            candidateBrowsersSet.add("Chrome");
        }
        if (candidateLocalesSet.isEmpty())
        {
            candidateLocalesSet.add("Default");
        }
        if (candidateEnvsSet.isEmpty())
        {
            candidateEnvsSet.add("Default");
        }
        if (candidateTagsSet.isEmpty())
        {
            candidateTagsSet.add("Default");
        }
        if (candidateAreasSet.isEmpty())
        {
            candidateAreasSet.add("@Browsing (default)");
        }

        final List<String> candidateBrowsers = new ArrayList<>(candidateBrowsersSet);
        final List<String> candidateLocales = new ArrayList<>(candidateLocalesSet);
        final List<String> candidateEnvs = new ArrayList<>(candidateEnvsSet);
        final List<String> candidateTags = new ArrayList<>(candidateTagsSet);
        final List<String> candidateAreas = new ArrayList<>(candidateAreasSet);

        final List<String> allTagKeys = new ArrayList<>();
        if (dataService != null)
        {
            for (final String key : dataService.getAllTagKeys())
            {
                if (key != null && !key.isBlank() && !"batch".equalsIgnoreCase(key) && !allTagKeys.contains(key.trim()))
                {
                    allTagKeys.add(key.trim());
                }
            }
        }
        for (final String key : candidateTagsByType.keySet())
        {
            if (key != null && !key.isBlank() && !"batch".equalsIgnoreCase(key) && !allTagKeys.contains(key.trim()))
            {
                allTagKeys.add(key.trim());
            }
        }

        final Map<String, Set<String>> activeTagFiltersByType = new LinkedHashMap<>();
        final Map<String, Set<String>> excludedTagFiltersByType = new LinkedHashMap<>();
        final List<RunTagTypeFilterDto> tagTypeFilters = new ArrayList<>();
        int dynamicTagFiltersCount = 0;

        for (final String tagKey : allTagKeys)
        {
            final List<String> rawCandidates = candidateTagsByType.getOrDefault(tagKey, Collections.emptyList());
            final List<String> candidates;
            if (dataService != null && dataService.isBooleanTag(tagKey, allRuns))
            {
                candidates = List.of(tagKey, "Not " + tagKey);
            }
            else
            {
                final Set<String> candSet = new LinkedHashSet<>(rawCandidates);
                final boolean hasUntagged = allRuns.stream().anyMatch(r -> dataService != null && dataService.getRunTagValues(r, tagKey).isEmpty());
                if (hasUntagged && !candSet.contains("Default"))
                {
                    candSet.add("Default");
                }
                candidates = new ArrayList<>(candSet);
            }
            final String pName = "runTag_" + tagKey.replaceAll("[^a-zA-Z0-9_]", "_");
            final String pVal = effectiveAllParams.get(pName);
            final Set<String> selectedVals = (pVal != null) ? parseFilterCsv(pVal) : Collections.emptySet();

            final String exName = "excludeRunTag_" + tagKey.replaceAll("[^a-zA-Z0-9_]", "_");
            final String exVal = effectiveAllParams.get(exName);
            final Set<String> excludedVals = (exVal != null) ? parseFilterCsv(exVal) : Collections.emptySet();

            if (!candidates.isEmpty())
            {
                final RunTagTypeFilterDto dto = new RunTagTypeFilterDto(tagKey, candidates, selectedVals, excludedVals);
                tagTypeFilters.add(dto);
                if (!selectedVals.isEmpty())
                {
                    activeTagFiltersByType.put(tagKey, selectedVals);
                    dynamicTagFiltersCount += selectedVals.size();
                }
                if (!excludedVals.isEmpty())
                {
                    excludedTagFiltersByType.put(tagKey, excludedVals);
                    dynamicTagFiltersCount += excludedVals.size();
                }
            }
        }

        for (int i = 0; i < groupDimensions.size() && i < parts.length; i++)
        {
            final String dim = groupDimensions.get(i);
            final String val = parts[i].trim();
            if (!val.isEmpty() && !"browser".equalsIgnoreCase(dim) && !"locale".equalsIgnoreCase(dim)
                && !"environment".equalsIgnoreCase(dim) && !"area".equalsIgnoreCase(dim)
                && !"batch".equalsIgnoreCase(dim))
            {
                activeTagFiltersByType.computeIfAbsent(dim, k -> new LinkedHashSet<>()).add(val);
            }
        }

        final String effectiveBatchArg;
        if (groupDimensions.contains("batch"))
        {
            if (groupDimensions.size() == 1)
            {
                effectiveBatchArg = effectiveGroupValue;
            }
            else
            {
                final int bIdx = groupDimensions.indexOf("batch");
                effectiveBatchArg = (bIdx >= 0 && bIdx < parts.length) ? parts[bIdx].trim() : "all";
            }
        }
        else
        {
            effectiveBatchArg = "all";
        }
        final boolean hasExclusionCriteria = !excludedRunBrowsers.isEmpty()
            || !excludedRunLocales.isEmpty()
            || !excludedRunEnvs.isEmpty()
            || !excludedRunAreas.isEmpty()
            || !excludedRunTags.isEmpty()
            || !excludedTagFiltersByType.isEmpty();

        final List<String> rawIdentityDims = (dashboardSettingService != null)
            ? dashboardSettingService.getExecutionIdentityDimensions()
            : null;
        final List<String> identityDimensions = (rawIdentityDims != null && !rawIdentityDims.isEmpty())
            ? rawIdentityDims
            : ReportDashboardSettingService.DEFAULT_EXECUTION_IDENTITY_DIMENSIONS;
        final boolean isDefaultIdentity = ReportDashboardSettingService.isDefaultExecutionIdentity(identityDimensions);

        final BatchLatestExecutionsDto latestExecResult;
        if (effectiveAsOfRunParam != null && !effectiveAsOfRunParam.isBlank())
        {
            latestExecResult = dataService.getBatchLatestTestExecutionsSummary(
                effectiveBatchArg,
                runs,
                excludedRunIds,
                fromTimestampMs,
                toTimestampMs,
                effectiveRunBrowsers,
                excludedRunBrowsers,
                effectiveRunLocales,
                excludedRunLocales,
                effectiveRunEnvs,
                excludedRunEnvs,
                effectiveRunAreas,
                excludedRunAreas,
                effectiveRunTags,
                excludedRunTags,
                runAreasMap,
                activeTagFiltersByType,
                excludedTagFiltersByType,
                identityDimensions,
                effectiveAsOfRunParam
            );
        }
        else if (!isDefaultIdentity)
        {
            latestExecResult = dataService.getBatchLatestTestExecutionsSummary(
                effectiveBatchArg,
                runs,
                excludedRunIds,
                fromTimestampMs,
                toTimestampMs,
                effectiveRunBrowsers,
                excludedRunBrowsers,
                effectiveRunLocales,
                excludedRunLocales,
                effectiveRunEnvs,
                excludedRunEnvs,
                effectiveRunAreas,
                excludedRunAreas,
                effectiveRunTags,
                excludedRunTags,
                runAreasMap,
                activeTagFiltersByType,
                excludedTagFiltersByType,
                identityDimensions
            );
        }
        else if (!hasExclusionCriteria)
        {
            if (toTimestampMs == null && activeTagFiltersByType.isEmpty())
            {
                if (effectiveRunBrowsers.isEmpty() && effectiveRunLocales.isEmpty() && effectiveRunEnvs.isEmpty() && effectiveRunAreas.isEmpty() && effectiveRunTags.isEmpty())
                {
                    latestExecResult = dataService.getBatchLatestTestExecutionsSummary(
                        effectiveBatchArg,
                        runs,
                        excludedRunIds,
                        fromTimestampMs
                    );
                }
                else
                {
                    latestExecResult = dataService.getBatchLatestTestExecutionsSummary(
                        effectiveBatchArg,
                        runs,
                        excludedRunIds,
                        fromTimestampMs,
                        effectiveRunBrowsers,
                        effectiveRunLocales,
                        effectiveRunEnvs,
                        effectiveRunAreas,
                        effectiveRunTags,
                        runAreasMap
                    );
                }
            }
            else
            {
                latestExecResult = dataService.getBatchLatestTestExecutionsSummary(
                    effectiveBatchArg,
                    runs,
                    excludedRunIds,
                    fromTimestampMs,
                    toTimestampMs,
                    effectiveRunBrowsers,
                    effectiveRunLocales,
                    effectiveRunEnvs,
                    effectiveRunAreas,
                    effectiveRunTags,
                    runAreasMap,
                    activeTagFiltersByType
                );
            }
        }
        else
        {
            latestExecResult = dataService.getBatchLatestTestExecutionsSummary(
                effectiveBatchArg,
                runs,
                excludedRunIds,
                fromTimestampMs,
                toTimestampMs,
                effectiveRunBrowsers,
                excludedRunBrowsers,
                effectiveRunLocales,
                excludedRunLocales,
                effectiveRunEnvs,
                excludedRunEnvs,
                effectiveRunAreas,
                excludedRunAreas,
                effectiveRunTags,
                excludedRunTags,
                runAreasMap,
                activeTagFiltersByType,
                excludedTagFiltersByType
            );
        }
        final BatchSummaryDto batchSummary = (latestExecResult != null) ? latestExecResult.getBatchSummary() : null;
        final Long effectiveFromMs = fromTimestampMs;
        final Long effectiveToMs = toTimestampMs;
        final List<TestRunEntity> defaultEligibleRuns = runs.stream()
            .filter(r -> r.getId() != null && !excludedRunIds.contains(r.getId()))
            .filter(r -> effectiveFromMs == null || (r.getStartTimeMs() != null && r.getStartTimeMs() >= effectiveFromMs))
            .filter(r -> effectiveToMs == null || (r.getStartTimeMs() != null && r.getStartTimeMs() <= effectiveToMs))
            .collect(Collectors.toList());
        final List<TestRunEntity> contributingRuns = (latestExecResult != null && latestExecResult.getContributingRuns() != null)
            ? latestExecResult.getContributingRuns()
            : defaultEligibleRuns;
        final List<TestRunEntity> eligibleRuns = (latestExecResult != null && latestExecResult.getEligibleRuns() != null)
            ? latestExecResult.getEligibleRuns()
            : defaultEligibleRuns;
        final List<DistributionRatioDto> browserRatios = (latestExecResult != null && latestExecResult.getBrowserRatios() != null)
            ? latestExecResult.getBrowserRatios()
            : Collections.emptyList();
        final List<DistributionRatioDto> localeRatios = (latestExecResult != null && latestExecResult.getLocaleRatios() != null)
            ? latestExecResult.getLocaleRatios()
            : Collections.emptyList();
        final List<DistributionRatioDto> environmentRatios = (latestExecResult != null && latestExecResult.getEnvironmentRatios() != null)
            ? latestExecResult.getEnvironmentRatios()
            : Collections.emptyList();
        final List<DistributionRatioDto> areaRatios = (latestExecResult != null && latestExecResult.getAreaRatios() != null)
            ? latestExecResult.getAreaRatios()
            : Collections.emptyList();
        final List<DistributionRatioDto> tagRatios = (latestExecResult != null && latestExecResult.getTagRatios() != null)
            ? latestExecResult.getTagRatios()
            : Collections.emptyList();
        final List<BatchLatestVariationDto> latestVariations = (latestExecResult != null && latestExecResult.getLatestVariations() != null)
            ? latestExecResult.getLatestVariations()
            : Collections.emptyList();

        final List<TagRatioCardDto> tagRatioCards = new ArrayList<>();
        for (final String tagKey : allTagKeys)
        {
            List<DistributionRatioDto> ratios = (latestExecResult != null) ? latestExecResult.getTagRatios(tagKey) : Collections.emptyList();
            if (ratios.isEmpty() && latestVariations != null && !latestVariations.isEmpty())
            {
                final Map<String, Integer> valCounts = new LinkedHashMap<>();
                for (final BatchLatestVariationDto v : latestVariations)
                {
                    final String val = v.getTagValue(tagKey);
                    final String effectiveVal = (val != null && !val.isBlank()) ? val : "Default";
                    valCounts.put(effectiveVal, valCounts.getOrDefault(effectiveVal, 0) + 1);
                }
                final int total = valCounts.values().stream().mapToInt(Integer::intValue).sum();
                final String[] palette = new String[] {
                    "#10b981", "#3b82f6", "#f59e0b", "#8b5cf6", "#ec4899", "#06b6d4", "#6366f1", "#14b8a6"
                };
                int cIdx = 0;
                final List<DistributionRatioDto> calculated = new ArrayList<>();
                for (final Map.Entry<String, Integer> entry : valCounts.entrySet())
                {
                    final double pct = total > 0 ? ((double) entry.getValue() / total * 100.0) : 0.0;
                    calculated.add(new DistributionRatioDto(
                        entry.getKey(),
                        entry.getValue(),
                        pct,
                        palette[cIdx % palette.length]
                    ));
                    cIdx++;
                }
                ratios = calculated;
            }
            tagRatioCards.add(new TagRatioCardDto(tagKey, ratios));
        }

        final boolean applyIdentityToVariations = dashboardSettingService != null
            && dashboardSettingService.isVariationsApplyIdentity();

        final List<TestBaseClassDto> batchTestClasses;
        if (applyIdentityToVariations && !isDefaultIdentity)
        {
            batchTestClasses = dataService.getBatchTestBaseClasses(
                effectiveBatchArg,
                eligibleRuns,
                effectiveRunBrowsers,
                excludedRunBrowsers,
                effectiveRunLocales,
                excludedRunLocales,
                effectiveRunEnvs,
                excludedRunEnvs,
                effectiveRunAreas,
                excludedRunAreas,
                effectiveRunTags,
                excludedRunTags,
                identityDimensions
            );
        }
        else if (!hasExclusionCriteria)
        {
            if (effectiveRunBrowsers.isEmpty() && effectiveRunLocales.isEmpty() && effectiveRunEnvs.isEmpty() && effectiveRunAreas.isEmpty() && effectiveRunTags.isEmpty())
            {
                batchTestClasses = dataService.getBatchTestBaseClasses(effectiveBatchArg, eligibleRuns);
            }
            else
            {
                batchTestClasses = dataService.getBatchTestBaseClasses(
                    effectiveBatchArg,
                    eligibleRuns,
                    effectiveRunBrowsers,
                    effectiveRunLocales,
                    effectiveRunEnvs,
                    effectiveRunAreas,
                    effectiveRunTags
                );
            }
        }
        else
        {
            batchTestClasses = dataService.getBatchTestBaseClasses(
                effectiveBatchArg,
                eligibleRuns,
                effectiveRunBrowsers,
                excludedRunBrowsers,
                effectiveRunLocales,
                excludedRunLocales,
                effectiveRunEnvs,
                excludedRunEnvs,
                effectiveRunAreas,
                excludedRunAreas,
                effectiveRunTags,
                excludedRunTags
            );
        }
        final List<TestBaseAreaDto> batchTestAreas = dataService.groupBatchTestClassesByArea(batchTestClasses);
        final int totalBatchVariationsCount = batchTestClasses.stream()
            .mapToInt(TestBaseClassDto::getVariationCount)
            .sum();

        final boolean hasScopingCriteria = !effectiveRunBrowsers.isEmpty()
            || !excludedRunBrowsers.isEmpty()
            || !effectiveRunLocales.isEmpty()
            || !excludedRunLocales.isEmpty()
            || !effectiveRunEnvs.isEmpty()
            || !excludedRunEnvs.isEmpty()
            || !effectiveRunAreas.isEmpty()
            || !excludedRunAreas.isEmpty()
            || !effectiveRunTags.isEmpty()
            || !excludedRunTags.isEmpty()
            || !activeTagFiltersByType.isEmpty()
            || !excludedTagFiltersByType.isEmpty();

        final List<TestRunEntity> displayedRuns;
        if (hasScopingCriteria)
        {
            displayedRuns = new ArrayList<>();
            for (final TestRunEntity r : runs)
            {
                final TestRunEntity scoped;
                if (dataService == null)
                {
                    scoped = null;
                }
                else if (!hasExclusionCriteria)
                {
                    if (activeTagFiltersByType.isEmpty())
                    {
                        scoped = dataService.scopeRunToCriteria(
                            r,
                            effectiveRunBrowsers,
                            effectiveRunLocales,
                            effectiveRunEnvs,
                            effectiveRunAreas,
                            effectiveRunTags
                        );
                    }
                    else
                    {
                        scoped = dataService.scopeRunToCriteria(
                            r,
                            effectiveRunBrowsers,
                            effectiveRunLocales,
                            effectiveRunEnvs,
                            effectiveRunAreas,
                            effectiveRunTags,
                            null,
                            activeTagFiltersByType
                        );
                    }
                }
                else
                {
                    scoped = dataService.scopeRunToCriteria(
                        r,
                        effectiveRunBrowsers,
                        excludedRunBrowsers,
                        effectiveRunLocales,
                        excludedRunLocales,
                        effectiveRunEnvs,
                        excludedRunEnvs,
                        effectiveRunAreas,
                        excludedRunAreas,
                        effectiveRunTags,
                        excludedRunTags,
                        null,
                        activeTagFiltersByType,
                        excludedTagFiltersByType
                    );
                }
                displayedRuns.add(scoped != null ? scoped : r);
            }
        }
        else
        {
            displayedRuns = runs;
        }

        final List<String> batchLocales = displayedRuns.stream()
            .flatMap(r -> r.getLocalesCsv() != null ? Arrays.stream(r.getLocalesCsv().split(",")) : Stream.empty())
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        final List<String> batchBrowsers = displayedRuns.stream()
            .flatMap(r -> r.getBrowsersCsv() != null ? Arrays.stream(r.getBrowsersCsv().split(",")) : Stream.empty())
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        final String batchEnvironment = batchOpt.map(TestBatchEntity::getEnvironment)
            .orElseGet(() -> displayedRuns.stream()
                .map(TestRunEntity::getEnvironment)
                .filter(e -> e != null && !e.isBlank())
                .findFirst()
                .orElse("Default"));

        final Set<String> filterEnvsSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        allRuns.stream()
            .map(TestRunEntity::getEnvironment)
            .filter(e -> e != null && !e.isBlank())
            .map(String::trim)
            .forEach(filterEnvsSet::add);
        for (final BatchLatestVariationDto v : latestVariations)
        {
            if (v.getEnvironment() != null && !v.getEnvironment().isBlank())
            {
                filterEnvsSet.add(v.getEnvironment().trim());
            }
        }
        for (final TestBaseClassDto cls : batchTestClasses)
        {
            if (cls.getVariations() != null)
            {
                for (final TestBaseVariationEntity v : cls.getVariations())
                {
                    if (v.getLastEnvironment() != null && !v.getLastEnvironment().isBlank())
                    {
                        filterEnvsSet.add(v.getLastEnvironment().trim());
                    }
                }
            }
        }
        if (filterEnvsSet.isEmpty())
        {
            filterEnvsSet.add(batchEnvironment);
        }
        final List<String> filterEnvs = new ArrayList<>(filterEnvsSet);

        final Set<String> filterLocalesSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        allRuns.stream()
            .flatMap(r -> r.getLocalesCsv() != null ? Arrays.stream(r.getLocalesCsv().split(",")) : Stream.empty())
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .map(AuraReportDataService::normalizeLocation)
            .forEach(filterLocalesSet::add);
        for (final BatchLatestVariationDto v : latestVariations)
        {
            if (v.getLocation() != null && !v.getLocation().isBlank())
            {
                filterLocalesSet.add(AuraReportDataService.normalizeLocation(v.getLocation()));
            }
        }
        for (final TestBaseClassDto cls : batchTestClasses)
        {
            if (cls.getVariations() != null)
            {
                for (final TestBaseVariationEntity v : cls.getVariations())
                {
                    if (v.getLocation() != null && !v.getLocation().isBlank())
                    {
                        filterLocalesSet.add(AuraReportDataService.normalizeLocation(v.getLocation()));
                    }
                }
            }
        }
        if (filterLocalesSet.isEmpty())
        {
            filterLocalesSet.addAll(batchLocales);
        }
        if (filterLocalesSet.isEmpty())
        {
            filterLocalesSet.add("Default");
        }
        final List<String> filterLocales = new ArrayList<>(filterLocalesSet);

        final Set<String> filterBrowsersSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        allRuns.stream()
            .flatMap(r -> r.getBrowsersCsv() != null ? Arrays.stream(r.getBrowsersCsv().split(",")) : Stream.empty())
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .map(AuraReportDataService::normalizeBrowser)
            .forEach(filterBrowsersSet::add);
        for (final BatchLatestVariationDto v : latestVariations)
        {
            if (v.getBrowser() != null && !v.getBrowser().isBlank())
            {
                filterBrowsersSet.add(AuraReportDataService.normalizeBrowser(v.getBrowser()));
            }
        }
        for (final TestBaseClassDto cls : batchTestClasses)
        {
            if (cls.getVariations() != null)
            {
                for (final TestBaseVariationEntity v : cls.getVariations())
                {
                    if (v.getBrowser() != null && !v.getBrowser().isBlank())
                    {
                        filterBrowsersSet.add(AuraReportDataService.normalizeBrowser(v.getBrowser()));
                    }
                }
            }
        }
        if (filterBrowsersSet.isEmpty())
        {
            filterBrowsersSet.addAll(batchBrowsers);
        }
        if (filterBrowsersSet.isEmpty())
        {
            filterBrowsersSet.add("Chrome");
        }
        final List<String> filterBrowsers = new ArrayList<>(filterBrowsersSet);

        final Set<String> filterAreasSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (final BatchAreaTrendDto trend : areaTrends)
        {
            if (trend.getAreaName() != null && !trend.getAreaName().isBlank())
            {
                filterAreasSet.add(AuraReportDataService.normalizeArea(trend.getAreaName()));
            }
        }
        for (final BatchLatestVariationDto v : latestVariations)
        {
            if (v.getArea() != null && !v.getArea().isBlank())
            {
                filterAreasSet.add(AuraReportDataService.normalizeArea(v.getArea()));
            }
        }
        for (final TestBaseClassDto cls : batchTestClasses)
        {
            if (cls.getVariations() != null)
            {
                for (final TestBaseVariationEntity v : cls.getVariations())
                {
                    if (v.getAreaTag() != null && !v.getAreaTag().isBlank())
                    {
                        filterAreasSet.add(AuraReportDataService.normalizeArea(v.getAreaTag()));
                    }
                }
            }
        }
        if (filterAreasSet.isEmpty())
        {
            filterAreasSet.add("@Browsing (default)");
        }
        final List<String> filterAreas = new ArrayList<>(filterAreasSet);

        final String dateTimespanLabel;
        if (effectiveStartDate != null && effectiveEndDate != null)
        {
            dateTimespanLabel = effectiveStartDate + " to " + effectiveEndDate;
        }
        else if (effectiveStartDate != null)
        {
            dateTimespanLabel = "From " + effectiveStartDate;
        }
        else if (effectiveEndDate != null)
        {
            dateTimespanLabel = "Until " + effectiveEndDate;
        }
        else
        {
            dateTimespanLabel = "";
        }

        final int activeTimespanCount = (effectiveStartDate != null || effectiveEndDate != null) ? 1 : 0;
        final int activeRunFiltersCount = selectedRunBrowsers.size() + excludedRunBrowsers.size()
            + selectedRunLocales.size() + excludedRunLocales.size()
            + selectedRunEnvs.size() + excludedRunEnvs.size()
            + selectedRunAreas.size() + excludedRunAreas.size()
            + selectedRunTags.size() + excludedRunTags.size()
            + dynamicTagFiltersCount + activeTimespanCount;
        final boolean hasActiveRunFilters = activeRunFiltersCount > 0
            || !excludedRunIds.isEmpty();

        final String effectiveBatchName;
        if (groupDimensions.contains("batch"))
        {
            if (groupDimensions.size() == 1)
            {
                effectiveBatchName = effectiveGroupValue;
            }
            else
            {
                final int bIdx = groupDimensions.indexOf("batch");
                effectiveBatchName = (bIdx >= 0 && bIdx < parts.length) ? parts[bIdx].trim() : "all";
            }
        }
        else if ("all".equals(effectiveGroupType))
        {
            effectiveBatchName = "all";
        }
        else
        {
            effectiveBatchName = effectiveGroupValue;
        }
        model.addAttribute("batchName", effectiveBatchName);
        model.addAttribute("groupType", effectiveGroupType);
        model.addAttribute("groupValue", effectiveGroupValue);
        final String pinnedGrouping = (dashboardSettingService != null)
            ? (effectiveGroupValue.isEmpty() ? dashboardSettingService.getPinnedGrouping() : dashboardSettingService.getPinnedInnerGrouping(effectiveGroupType))
            : null;
        final boolean isPinned;
        if (dashboardSettingService == null)
        {
            isPinned = false;
        }
        else if (effectiveGroupValue.isEmpty())
        {
            isPinned = dashboardSettingService.isGroupingPinned(
                activeDimensions.isEmpty() ? "none" : String.join(",", activeDimensions)
            );
        }
        else
        {
            isPinned = dashboardSettingService.isGroupingPinned(
                effectiveGroupType,
                activeDimensions.isEmpty() ? "none" : String.join(",", activeDimensions)
            );
        }
        model.addAttribute("pinnedGroupBy", pinnedGrouping);
        model.addAttribute("isGroupingPinned", isPinned);
        model.addAttribute("groupBy", activeDimensions.isEmpty() ? "none" : String.join(",", activeDimensions));
        model.addAttribute("groupLabel", groupLabel);
        model.addAttribute("batches", batches);
        model.addAttribute("scopeTitle", scopeTitle);
        model.addAttribute("backUrl", backUrl);

        final String basePath;
        final Map<String, String> baseParams = new LinkedHashMap<>();
        if (!effectiveGroupValue.isEmpty())
        {
            basePath = "/batch-history";
            baseParams.put("groupType", effectiveGroupType);
            baseParams.put("groupValue", effectiveGroupValue);
        }
        else
        {
            basePath = "/";
        }

        final String toggleNoneUrl;
        if (baseParams.isEmpty())
        {
            toggleNoneUrl = "/?groupBy=none";
        }
        else
        {
            final Map<String, String> noneParams = new LinkedHashMap<>(baseParams);
            noneParams.put("groupBy", "none");
            toggleNoneUrl = buildUrlWithParams(basePath, noneParams);
        }
        model.addAttribute("toggleNoneUrl", toggleNoneUrl);
        model.addAttribute("toggleBatchUrl", computeToggleUrl(basePath, baseParams, activeDimensions, "batch"));
        model.addAttribute("toggleEnvUrl", computeToggleUrl(basePath, baseParams, activeDimensions, "environment"));
        model.addAttribute("toggleLocaleUrl", computeToggleUrl(basePath, baseParams, activeDimensions, "locale"));
        model.addAttribute("toggleBrowserUrl", computeToggleUrl(basePath, baseParams, activeDimensions, "browser"));
        model.addAttribute("toggleAreaUrl", computeToggleUrl(basePath, baseParams, activeDimensions, "area"));
        model.addAttribute("isNoneActive", activeDimensions.isEmpty() && groupDimensions.isEmpty());
        model.addAttribute("isBatchActive", activeDimensions.contains("batch") || groupDimensions.contains("batch"));
        model.addAttribute("isEnvActive", activeDimensions.contains("environment") || groupDimensions.contains("environment"));
        model.addAttribute("isLocaleActive", activeDimensions.contains("locale") || groupDimensions.contains("locale"));
        model.addAttribute("isBrowserActive", activeDimensions.contains("browser") || groupDimensions.contains("browser"));
        model.addAttribute("isAreaActive", activeDimensions.contains("area") || groupDimensions.contains("area"));
        model.addAttribute("showBatchPill", !groupDimensions.contains("batch"));
        model.addAttribute("showEnvPill", !groupDimensions.contains("environment"));
        model.addAttribute("showLocalePill", !groupDimensions.contains("locale"));
        model.addAttribute("showBrowserPill", !groupDimensions.contains("browser"));
        model.addAttribute("showAreaPill", !groupDimensions.contains("area"));
        model.addAttribute("activeGroupDimensions", activeDimensions);
        model.addAttribute("groupingTags", buildGroupingTagOptions(basePath, baseParams, activeDimensions, groupDimensions));

        final boolean isBrowserIdentity = dashboardSettingService != null
            ? dashboardSettingService.isExecutionIdentityDimensionActive("browser")
            : true;
        final boolean isLocaleIdentity = dashboardSettingService != null
            ? dashboardSettingService.isExecutionIdentityDimensionActive("locale")
            : true;
        final boolean isEnvIdentity = dashboardSettingService != null
            ? dashboardSettingService.isExecutionIdentityDimensionActive("environment")
            : false;

        final List<String> availableIdentityTags = new ArrayList<>();
        for (final String k : allTagKeys)
        {
            final String normK = ReportDashboardSettingService.normalizeDimensionName(k);
            if (!"browser".equalsIgnoreCase(normK))
            {
                if (availableIdentityTags.stream().noneMatch(existing -> existing.equalsIgnoreCase(k)))
                {
                    availableIdentityTags.add(k);
                }
            }
        }
        for (final String activeDim : identityDimensions)
        {
            final String normDim = ReportDashboardSettingService.normalizeDimensionName(activeDim);
            if (!"browser".equalsIgnoreCase(normDim))
            {
                final boolean alreadyPresent = availableIdentityTags.stream()
                    .anyMatch(existing -> existing.equalsIgnoreCase(activeDim) || existing.equalsIgnoreCase(normDim));
                if (!alreadyPresent)
                {
                    final String canonical = (activeDim != null && !activeDim.isBlank())
                        ? (activeDim.substring(0, 1).toUpperCase(Locale.ROOT) + activeDim.substring(1))
                        : activeDim;
                    availableIdentityTags.add(canonical);
                }
            }
        }
        availableIdentityTags.sort(String.CASE_INSENSITIVE_ORDER);

        final List<String> activeIdentityCustomTags = new ArrayList<>();
        for (final String d : identityDimensions)
        {
            final String norm = ReportDashboardSettingService.normalizeDimensionName(d);
            if (!"browser".equals(norm))
            {
                final String canonical = allTagKeys.stream()
                    .filter(k -> k.equalsIgnoreCase(d))
                    .findFirst()
                    .orElse((d != null && !d.isBlank()) ? (d.substring(0, 1).toUpperCase(Locale.ROOT) + d.substring(1)) : d);
                if (!activeIdentityCustomTags.contains(canonical))
                {
                    activeIdentityCustomTags.add(canonical);
                }
            }
        }

        model.addAttribute("identityDimensions", identityDimensions);
        model.addAttribute("uniquenessDimensions", identityDimensions);
        model.addAttribute("isBrowserIdentity", isBrowserIdentity);
        model.addAttribute("isBrowserUnique", isBrowserIdentity);
        model.addAttribute("isLocaleIdentity", isLocaleIdentity);
        model.addAttribute("isLocaleUnique", isLocaleIdentity);
        model.addAttribute("isEnvIdentity", isEnvIdentity);
        model.addAttribute("isEnvUnique", isEnvIdentity);
        model.addAttribute("availableIdentityTags", availableIdentityTags);
        model.addAttribute("availableUniquenessTags", availableIdentityTags);
        model.addAttribute("applyIdentityToVariations", applyIdentityToVariations);
        model.addAttribute("applyUniquenessToVariations", applyIdentityToVariations);
        model.addAttribute("activeIdentityCustomTags", activeIdentityCustomTags);
        model.addAttribute("activeUniquenessCustomTags", activeIdentityCustomTags);

        model.addAttribute("batch", batchOpt.orElse(null));
        model.addAttribute("batchSummary", batchSummary);
        model.addAttribute("latestExecResult", latestExecResult);
        model.addAttribute("tokenTrend", (latestExecResult != null) ? latestExecResult.getTokenCostTrend() : null);
        model.addAttribute("suiteEvolution", (latestExecResult != null) ? latestExecResult.getSuiteEvolution() : null);
        model.addAttribute("latestVariations", latestVariations);
        model.addAttribute("contributingRuns", contributingRuns);
        model.addAttribute("batchEnvironment", batchEnvironment);
        model.addAttribute("browserRatios", browserRatios);
        model.addAttribute("localeRatios", localeRatios);
        model.addAttribute("environmentRatios", environmentRatios);
        model.addAttribute("areaRatios", areaRatios);
        model.addAttribute("tagRatios", tagRatios);
        model.addAttribute("tagRatioCards", tagRatioCards);
        model.addAttribute("excludedRunIds", excludedRunIds);
        model.addAttribute("excludeRuns", excludeRuns != null ? excludeRuns : "");
        model.addAttribute("startDate", effectiveStartDate != null ? effectiveStartDate : "");
        model.addAttribute("endDate", effectiveEndDate != null ? effectiveEndDate : "");
        model.addAttribute("dateTimespanLabel", dateTimespanLabel);
        model.addAttribute("olderThanDate", effectiveStartDate != null ? effectiveStartDate : "");
        model.addAttribute("tagTypeFilters", tagTypeFilters);
        model.addAttribute("activeTagFiltersByType", activeTagFiltersByType);
        model.addAttribute("candidateTagsByType", candidateTagsByType);
        model.addAttribute("allTagKeys", allTagKeys);
        model.addAttribute("runBrowsers", runBrowsers != null ? runBrowsers : "");
        model.addAttribute("excludeRunBrowsers", excludeRunBrowsers != null ? excludeRunBrowsers : "");
        model.addAttribute("runLocales", runLocales != null ? runLocales : "");
        model.addAttribute("excludeRunLocales", excludeRunLocales != null ? excludeRunLocales : "");
        model.addAttribute("runEnvs", runEnvs != null ? runEnvs : "");
        model.addAttribute("excludeRunEnvs", excludeRunEnvs != null ? excludeRunEnvs : "");
        model.addAttribute("runAreas", runAreas != null ? runAreas : "");
        model.addAttribute("excludeRunAreas", excludeRunAreas != null ? excludeRunAreas : "");
        model.addAttribute("runTags", runTags != null ? runTags : "");
        model.addAttribute("excludeRunTags", excludeRunTags != null ? excludeRunTags : "");
        model.addAttribute("selectedRunBrowsers", selectedRunBrowsers);
        model.addAttribute("excludedRunBrowsers", excludedRunBrowsers);
        model.addAttribute("selectedRunLocales", selectedRunLocales);
        model.addAttribute("excludedRunLocales", excludedRunLocales);
        model.addAttribute("selectedRunEnvs", selectedRunEnvs);
        model.addAttribute("excludedRunEnvs", excludedRunEnvs);
        model.addAttribute("selectedRunAreas", selectedRunAreas);
        model.addAttribute("excludedRunAreas", excludedRunAreas);
        model.addAttribute("selectedRunTags", selectedRunTags);
        model.addAttribute("excludedRunTags", excludedRunTags);
        model.addAttribute("excludedTagFiltersByType", excludedTagFiltersByType);
        model.addAttribute("candidateBrowsers", candidateBrowsers);
        model.addAttribute("candidateLocales", candidateLocales);
        model.addAttribute("candidateEnvs", candidateEnvs);
        model.addAttribute("candidateAreas", candidateAreas);
        model.addAttribute("candidateTags", candidateTags);
        model.addAttribute("activeRunFiltersCount", activeRunFiltersCount);
        model.addAttribute("hasActiveRunFilters", hasActiveRunFilters);
        model.addAttribute("eligibleRunsCount", (latestExecResult != null) ? latestExecResult.getEligibleRunsCount() : contributingRuns.size());
        model.addAttribute("filterEnvs", filterEnvs);
        model.addAttribute("filterLocales", filterLocales);
        model.addAttribute("filterBrowsers", filterBrowsers);
        model.addAttribute("filterAreas", filterAreas);
        model.addAttribute("runAreasMap", runAreasMap);
        model.addAttribute("runs", displayedRuns);
        model.addAttribute("totalRunsCount", hasScopingCriteria ? displayedRuns.size() : totalRunsCount);
        model.addAttribute("currentLimit", effectiveLimit);
        model.addAttribute("showingRunsCount", displayedRuns.size());
        model.addAttribute("batchLocales", batchLocales);
        model.addAttribute("batchBrowsers", batchBrowsers);
        model.addAttribute("areaTrends", areaTrends);
        model.addAttribute("batchTestClasses", batchTestClasses);
        model.addAttribute("batchTestAreas", batchTestAreas);
        model.addAttribute("totalBatchVariationsCount", totalBatchVariationsCount);

        final boolean isHistoricalSnapshot = (latestExecResult != null) && latestExecResult.isHistoricalSnapshot();
        final String effectiveSnapshotRunId = (latestExecResult != null) ? latestExecResult.getAsOfRunId() : null;
        final String asOfTimestampLabel = (latestExecResult != null) ? latestExecResult.getAsOfTimestampLabel() : null;

        model.addAttribute("isHistoricalSnapshot", isHistoricalSnapshot);
        model.addAttribute("asOfRunId", effectiveSnapshotRunId);
        model.addAttribute("asOfTimestampLabel", asOfTimestampLabel);

        final Map<String, String> resetParams = new LinkedHashMap<>(effectiveAllParams);
        resetParams.remove("asOfRunId");
        final StringBuilder resetUrlBuilder = new StringBuilder("/batch-history");
        if (!resetParams.isEmpty())
        {
            resetUrlBuilder.append("?");
            int pIdx = 0;
            for (final Map.Entry<String, String> p : resetParams.entrySet())
            {
                if (pIdx > 0)
                {
                    resetUrlBuilder.append("&");
                }
                resetUrlBuilder.append(URLEncoder.encode(p.getKey(), StandardCharsets.UTF_8))
                               .append("=")
                               .append(URLEncoder.encode(p.getValue(), StandardCharsets.UTF_8));
                pIdx++;
            }
        }
        model.addAttribute("batchHistoryResetUrl", resetUrlBuilder.toString());

        final boolean isCustomWidgetsConfigured = (dashboardSettingService != null) && dashboardSettingService.isCustomWidgetsConfigured();
        final List<DashboardWidgetConfigDto> widgetConfigs = (dashboardSettingService != null)
            ? dashboardSettingService.getDashboardWidgets()
            : ReportDashboardSettingService.getDefaultWidgets();
        final List<TestBaseBugEntity> allBugs = (bugRepository != null) ? bugRepository.findAll() : Collections.emptyList();
        final List<DashboardWidgetViewDto> dashboardWidgets = (widgetAggregationService != null)
            ? widgetAggregationService.aggregateWidgets(
                widgetConfigs, displayedRuns, latestVariations, browserRatios, tagRatioCards,
                (latestExecResult != null) ? latestExecResult.getTokenCostTrend() : null,
                (latestExecResult != null) ? latestExecResult.getSuiteEvolution() : null,
                allBugs,
                dataService,
                runs,
                effectiveBatchArg,
                identityDimensions,
                runAreasMap,
                allTagKeys)
            : Collections.emptyList();

        model.addAttribute("dashboardWidgets", dashboardWidgets);
        model.addAttribute("isCustomWidgetsConfigured", isCustomWidgetsConfigured);

        final List<String> availableWidgetTags = resolveAvailableWidgetTagKeys();
        for (final String key : allTagKeys)
        {
            if (key != null && !key.isBlank() && !availableWidgetTags.contains(key))
            {
                availableWidgetTags.add(key);
            }
        }
        model.addAttribute("availableWidgetTags", availableWidgetTags);
        model.addAttribute("allMetrics", WidgetMetric.values());
        model.addAttribute("allPresentations", WidgetPresentation.values());
        model.addAttribute("allTrendModes", WidgetTrendMode.values());
        model.addAttribute("allValueFormats", WidgetValueFormat.values());
        model.addAttribute("allPointScopes", WidgetPointScope.values());

        final String effectivePageTitle = "all".equals(effectiveGroupType) ? "Runs overview" : scopeTitle;
        model.addAttribute("pageTitle", effectivePageTitle);
        model.addAttribute("activeTab", "Directory");
        model.addAttribute("viewFragment", "fragments/batch-history :: batchHistory");

        if ("true".equals(hxRequest))
        {
            return "fragments/batch-history :: batchHistory";
        }
        return "index";
    }

    public String runReport(
        final String runId,
        final String executionId,
        final String testExecutionId,
        final String subTab,
        final String hxRequest,
        final Model model)
    {
        return runReport(runId, executionId, testExecutionId, null, null, subTab, hxRequest, model);
    }

    @GetMapping({"/run-report", "/fragments/run-report"})
    public String runReport(
        @RequestParam(name = "runId", required = false) final String runId,
        @RequestParam(name = "executionId", required = false) final String executionId,
        @RequestParam(name = "testExecutionId", required = false) final String testExecutionId,
        @RequestParam(name = "variationId", required = false) final String variationId,
        @RequestParam(name = "testName", required = false) final String testName,
        @RequestParam(name = "subTab", required = false) final String subTab,
        @RequestHeader(value = "HX-Request", required = false) final String hxRequest,
        final Model model)
    {
        String targetExecutionId = (executionId != null && !executionId.isEmpty()) ? executionId : testExecutionId;
        final RunReportDto report = dataService.getRunReport(runId);
        if ((targetExecutionId == null || targetExecutionId.isBlank()) && report != null && report.getExecutions() != null)
        {
            if (variationId != null && !variationId.isBlank())
            {
                for (final TestExecutionDto exec : report.getExecutions())
                {
                    if (variationId.equalsIgnoreCase(exec.getId()))
                    {
                        targetExecutionId = exec.getId();
                        break;
                    }
                    final String execVarId = AuraReportDataService.generateVariationId(
                        exec.getTestClass(), exec.getTestMethod(), exec.getTitle(), exec.getLocation(), exec.getBrowser()
                    );
                    if (variationId.equalsIgnoreCase(execVarId))
                    {
                        targetExecutionId = exec.getId();
                        break;
                    }
                    final String legacyVarId = AuraReportDataService.generateVariationId(
                        exec.getTestClass(), exec.getTitle(), exec.getLocation(), exec.getBrowser()
                    );
                    if (variationId.equalsIgnoreCase(legacyVarId))
                    {
                        targetExecutionId = exec.getId();
                        break;
                    }
                }
            }
            if ((targetExecutionId == null || targetExecutionId.isBlank()) && testName != null && !testName.isBlank())
            {
                for (final TestExecutionDto exec : report.getExecutions())
                {
                    if (testName.equalsIgnoreCase(exec.getTestMethod())
                        || testName.equalsIgnoreCase(exec.getTestClass())
                        || testName.equalsIgnoreCase(exec.getTestName())
                        || (exec.getTestMethod() != null && testName.contains(exec.getTestMethod())))
                    {
                        targetExecutionId = exec.getId();
                        break;
                    }
                }
            }
        }
        final String effectiveBatchName = report.getBatchName() != null && !report.getBatchName().isBlank()
            ? report.getBatchName()
            : "Default";
        final String activeSubTab = (targetExecutionId != null && !targetExecutionId.isBlank() && (subTab == null || subTab.isBlank()))
            ? "runReportSubTabAllTests"
            : (subTab != null && !subTab.isBlank() ? subTab : "runReportSubTabOverview");
        model.addAttribute("runId", report.getRunId());
        model.addAttribute("targetExecutionId", targetExecutionId);
        model.addAttribute("activeSubTab", activeSubTab);
        model.addAttribute("report", report);
        model.addAttribute("batchName", effectiveBatchName);
        model.addAttribute("activeBatchName", effectiveBatchName);
        model.addAttribute("filterModes", report.getFilterModes());
        model.addAttribute("filterBrowsers", report.getFilterBrowsers());
        model.addAttribute("filterLocales", report.getFilterLocales());
        model.addAttribute("browserRatios", report.getBrowserRatios());
        model.addAttribute("localeRatios", report.getLocaleRatios());
        model.addAttribute("pageTitle", "Run #" + (report.getRunId() != null && !report.getRunId().isBlank() ? report.getRunId() : "RUN_ID"));
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
        model.addAttribute("filterModes", filteredReport.getFilterModes());
        model.addAttribute("filterBrowsers", filteredReport.getFilterBrowsers());
        model.addAttribute("filterLocales", filteredReport.getFilterLocales());
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
        model.addAttribute("hasRealAreas", testBaseData.hasRealAreas());
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
        model.addAttribute("report", report);
        model.addAttribute("allKnownTagMap", dataService.getAllKnownTagMap());
        model.addAttribute("allKnownTags", dataService.getAllKnownTagsList());

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
        model.addAttribute("report", report);
        model.addAttribute("allKnownTagMap", dataService.getAllKnownTagMap());
        model.addAttribute("allKnownTags", dataService.getAllKnownTagsList());
        return "fragments/side-panel-step-list :: sidePanelBugSection";
    }

    @PostMapping("/fragments/test-side-panel/bugs")
    public String addBugToExecution(
        @RequestParam("runId") final String runId,
        @RequestParam("rowId") final String rowId,
        @RequestParam("bugTicket") final String bugTicket,
        @RequestParam(name = "severity", required = false, defaultValue = "MAJOR") final String severity,
        @RequestParam(name = "boundTags", required = false) final String boundTags,
        @RequestHeader(value = "HX-Current-URL", required = false) final String currentUrl,
        @RequestHeader(value = "Referer", required = false) final String referer,
        final Model model,
        final HttpServletResponse response)
    {
        final String effectiveRunId = resolveEffectiveRunId(runId, currentUrl, referer);
        final long totalStart = System.currentTimeMillis();

        final long addStart = System.currentTimeMillis();
        final TestExecutionDto updatedExec = dataService.addBugToExecution(effectiveRunId, rowId, bugTicket, severity, boundTags);
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
        model.addAttribute("report", report);
        model.addAttribute("allKnownTagMap", dataService.getAllKnownTagMap());
        model.addAttribute("allKnownTags", dataService.getAllKnownTagsList());

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
        model.addAttribute("report", report);
        model.addAttribute("allKnownTagMap", dataService.getAllKnownTagMap());
        model.addAttribute("allKnownTags", dataService.getAllKnownTagsList());

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
