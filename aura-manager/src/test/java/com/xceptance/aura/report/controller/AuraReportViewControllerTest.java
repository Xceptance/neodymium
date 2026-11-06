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
import com.xceptance.aura.report.dto.ContributingRunCostDto;
import com.xceptance.aura.report.dto.DistributionRatioDto;
import com.xceptance.aura.report.dto.GroupingTagOptionDto;
import com.xceptance.aura.report.dto.RunReportDto;
import com.xceptance.aura.report.dto.RunTagTypeFilterDto;
import com.xceptance.aura.report.dto.TagRatioCardDto;
import com.xceptance.aura.report.dto.TestBaseAreaDto;
import com.xceptance.aura.report.dto.TestBaseClassDto;
import com.xceptance.aura.report.dto.TestBaseDataDto;
import com.xceptance.aura.report.dto.TestExecutionDto;
import com.xceptance.aura.report.dto.TokenCostPointGroupDto;
import com.xceptance.aura.report.dto.TokenCostTrendDto;
import com.xceptance.aura.report.dto.widget.DashboardPageConfigDto;
import com.xceptance.aura.report.dto.widget.DashboardWidgetConfigDto;
import com.xceptance.aura.report.dto.widget.DashboardWidgetViewDto;
import com.xceptance.aura.report.dto.widget.WidgetContributingExecutionsDto;
import com.xceptance.aura.report.dto.widget.WidgetFilterConfigDto;
import com.xceptance.aura.report.dto.widget.WidgetMetric;
import com.xceptance.aura.report.dto.widget.WidgetPointScope;
import com.xceptance.aura.report.dto.widget.WidgetPresentation;
import com.xceptance.aura.report.dto.widget.WidgetTrendMode;
import com.xceptance.aura.report.dto.widget.WidgetValueFormat;
import com.xceptance.aura.report.entity.TestBaseVariationEntity;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.entity.TestRunTagEntity;
import jakarta.servlet.http.HttpServletResponse;
import com.xceptance.aura.report.repository.TestBaseBugRepository;
import com.xceptance.aura.report.repository.TestBaseVariationRepository;
import com.xceptance.aura.report.repository.TestBatchRepository;
import com.xceptance.aura.report.repository.TestRunRepository;
import com.xceptance.aura.report.repository.TestRunTagRepository;
import com.xceptance.aura.report.service.AuraReportDataService;
import com.xceptance.aura.report.service.ReportDashboardSettingService;
import com.xceptance.aura.report.service.RunStorageSyncService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.linkbuilder.StandardLinkBuilder;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Unit tests for AuraReportViewController.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class AuraReportViewControllerTest
{
    private AuraReportDataService dataService;
    private RunStorageSyncService syncService;
    private TestRunRepository runRepository;
    private TestBatchRepository batchRepository;
    private TestBaseVariationRepository variationRepository;
    private TestBaseBugRepository bugRepository;
    private ReportDashboardSettingService settingService;
    private AuraReportViewController controller;

    @BeforeEach
    public void setUp()
    {
        dataService = Mockito.mock(AuraReportDataService.class);
        syncService = Mockito.mock(RunStorageSyncService.class);
        runRepository = Mockito.mock(TestRunRepository.class);
        batchRepository = Mockito.mock(TestBatchRepository.class);
        variationRepository = Mockito.mock(TestBaseVariationRepository.class);
        settingService = Mockito.mock(ReportDashboardSettingService.class);
        Mockito.when(settingService.getExecutionIdentityDimensions()).thenReturn(ReportDashboardSettingService.DEFAULT_EXECUTION_IDENTITY_DIMENSIONS);
        Mockito.when(settingService.getUniquenessDimensions()).thenReturn(ReportDashboardSettingService.DEFAULT_UNIQUENESS_DIMENSIONS);
        Mockito.when(settingService.isExecutionIdentityDimensionActive("browser")).thenReturn(true);
        Mockito.when(settingService.isExecutionIdentityDimensionActive("locale")).thenReturn(true);
        Mockito.when(settingService.isExecutionIdentityDimensionActive("environment")).thenReturn(false);
        Mockito.when(settingService.isUniquenessDimensionActive("browser")).thenReturn(true);
        Mockito.when(settingService.isUniquenessDimensionActive("locale")).thenReturn(true);
        Mockito.when(settingService.isUniquenessDimensionActive("environment")).thenReturn(false);
        Mockito.when(settingService.isVariationsApplyIdentity()).thenReturn(false);
        Mockito.when(settingService.isVariationsApplyUniqueness()).thenReturn(false);

        controller = new AuraReportViewController(
            dataService,
            syncService,
            runRepository,
            batchRepository,
            variationRepository,
            bugRepository,
            settingService
        );
    }

    @Test
    public void testTestBaseModelAttributes()
    {
        final TestBaseDataDto testBaseData = new TestBaseDataDto(
            List.of("Batch1"),
            List.of("US", "DE"),
            List.of("Chrome"),
            List.of(),
            Map.of(),
            5,
            10
        );

        Mockito.when(dataService.getTestBaseData()).thenReturn(testBaseData);

        final Model model = new ConcurrentModel();
        final String view = controller.testBase(
            "CheckoutProcessTest",
            "DE CreditCard",
            "DE",
            "Chrome",
            "false",
            model
        );

        Assertions.assertEquals("index", view);
        Assertions.assertEquals("CheckoutProcessTest", model.getAttribute("targetTestName"));
        Assertions.assertEquals("DE CreditCard", model.getAttribute("targetDataSet"));
        Assertions.assertEquals("DE", model.getAttribute("targetLocation"));
        Assertions.assertEquals("Chrome", model.getAttribute("targetBrowser"));
    }

    @Test
    public void testRunReportSubTabModelAttributes()
    {
        final RunReportDto dummyReport = new RunReportDto(
            "RUN-100",
            "BatchAlpha",
            "2026-09-15",
            "10s",
            1,
            1,
            0,
            0,
            0,
            0,
            new ArrayList<>()
        );
        Mockito.when(dataService.getRunReport("RUN-100")).thenReturn(dummyReport);

        final Model model = new ConcurrentModel();
        final String view = controller.runReport(
            "RUN-100",
            "EXEC-1",
            null,
            "runReportSubTabAllTests",
            "true",
            model
        );

        Assertions.assertEquals("fragments/run-report :: runReport", view);
        Assertions.assertEquals("RUN-100", model.getAttribute("runId"));
        Assertions.assertEquals("EXEC-1", model.getAttribute("targetExecutionId"));
        Assertions.assertEquals("runReportSubTabAllTests", model.getAttribute("activeSubTab"));
        Assertions.assertEquals("BatchAlpha", model.getAttribute("batchName"));
        Assertions.assertEquals("BatchAlpha", model.getAttribute("activeBatchName"));
        Assertions.assertEquals("Run #RUN-100", model.getAttribute("pageTitle"));
        Assertions.assertNotNull(model.getAttribute("filterModes"));
        Assertions.assertNotNull(model.getAttribute("filterBrowsers"));
        Assertions.assertNotNull(model.getAttribute("filterLocales"));
    }

    @Test
    public void testRefreshAndResyncRunsPreservesSubTab()
    {
        final RunReportDto dummyReport = new RunReportDto(
            "RUN-200",
            "BatchBeta",
            "2026-09-15",
            "15s",
            2,
            2,
            0,
            0,
            0,
            0,
            new ArrayList<>()
        );
        Mockito.when(dataService.getRunReport("RUN-200")).thenReturn(dummyReport);

        final Model model = new ConcurrentModel();
        final String currentUrl = "http://localhost:8080/run-report?runId=RUN-200&executionId=EXEC-555&subTab=runReportSubTabAllTests";
        final String view = controller.refreshAndResyncRuns("true", currentUrl, null, model);

        Assertions.assertEquals("fragments/run-report :: runReport", view);
        Assertions.assertEquals("RUN-200", model.getAttribute("runId"));
        Assertions.assertEquals("EXEC-555", model.getAttribute("targetExecutionId"));
        Assertions.assertEquals("runReportSubTabAllTests", model.getAttribute("activeSubTab"));
        Mockito.verify(syncService, Mockito.times(1)).syncLocalRunStorage();
        Mockito.verify(dataService, Mockito.times(1)).clearCache();
    }

    @Test
    public void testBatchHistoryWithoutLimitReturnsAllRuns()
    {
        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-1");
        r1.setBatchName("SmokeBatch");
        r1.setEnvironment("Staging");
        r1.setLocalesCsv("US,DE");
        r1.setBrowsersCsv("Chrome");

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-2");
        r2.setBatchName("SmokeBatch");
        r2.setEnvironment("Staging");
        r2.setLocalesCsv("US");
        r2.setBrowsersCsv("Firefox");

        final List<TestRunEntity> allRuns = Arrays.asList(r1, r2);
        Mockito.when(runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc("SmokeBatch")).thenReturn(allRuns);
        Mockito.when(batchRepository.findById("SmokeBatch")).thenReturn(Optional.empty());

        final Model model = new ConcurrentModel();
        final String view = controller.batchHistory("SmokeBatch", null, "true", model);

        Assertions.assertEquals("fragments/batch-history :: batchHistory", view);
        Assertions.assertEquals("SmokeBatch", model.getAttribute("batchName"));
        Assertions.assertEquals(2, model.getAttribute("totalRunsCount"));
        Assertions.assertNull(model.getAttribute("currentLimit"));
        Assertions.assertEquals(2, model.getAttribute("showingRunsCount"));

        final List<?> runs = (List<?>) model.getAttribute("runs");
        Assertions.assertNotNull(runs);
        Assertions.assertEquals(2, runs.size());
        Assertions.assertNotNull(model.getAttribute("batchTestClasses"));
        Assertions.assertEquals(0, model.getAttribute("totalBatchVariationsCount"));
    }

    @Test
    public void testBatchHistoryPopulatesKnownBatchVariations()
    {
        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-smoke-1");
        r1.setBatchName("SmokeBatch");
        final List<TestRunEntity> allRuns = List.of(r1);

        Mockito.when(runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc("SmokeBatch")).thenReturn(allRuns);
        Mockito.when(batchRepository.findById("SmokeBatch")).thenReturn(Optional.empty());

        final TestBaseVariationEntity var1 = new TestBaseVariationEntity("var-1", "CheckoutTest", "testCheckout", "Default", "@Checkout", "US", "Chrome");
        final TestBaseClassDto classDto = new TestBaseClassDto("CheckoutTest", "batchClassContainerCheckoutTest", 1, List.of(var1));
        final TestBaseAreaDto areaDto = new TestBaseAreaDto("@Checkout", "batchAreaCheckout", "batchTabBtnCheckout", "shopping_cart", 1, List.of(classDto));
        Mockito.when(dataService.getBatchTestBaseClasses("SmokeBatch", allRuns)).thenReturn(List.of(classDto));
        Mockito.when(dataService.groupBatchTestClassesByArea(List.of(classDto))).thenReturn(List.of(areaDto));

        final Model model = new ConcurrentModel();
        final String view = controller.batchHistory("SmokeBatch", null, "true", model);

        Assertions.assertEquals("fragments/batch-history :: batchHistory", view);
        Assertions.assertEquals(1, model.getAttribute("totalBatchVariationsCount"));

        @SuppressWarnings("unchecked")
        final List<TestBaseClassDto> classes = (List<TestBaseClassDto>) model.getAttribute("batchTestClasses");
        Assertions.assertNotNull(classes);
        Assertions.assertEquals(1, classes.size());
        Assertions.assertEquals("CheckoutTest", classes.get(0).getClassName());
        Assertions.assertEquals(1, classes.get(0).getVariationCount());

        @SuppressWarnings("unchecked")
        final List<TestBaseAreaDto> areas = (List<TestBaseAreaDto>) model.getAttribute("batchTestAreas");
        Assertions.assertNotNull(areas);
        Assertions.assertEquals(1, areas.size());
        Assertions.assertEquals("@Checkout", areas.get(0).getAreaName());
        Assertions.assertEquals(1, areas.get(0).getTotalVariationsCount());
    }

    @Test
    public void testBatchHistoryRenderingWithAreaGroups()
    {
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        final SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        final StandardLinkBuilder linkBuilder = new StandardLinkBuilder()
        {
            @Override
            protected String computeContextPath(final IExpressionContext context, final String base, final Map<String, Object> parameters)
            {
                return "";
            }
        };
        engine.setLinkBuilder(linkBuilder);

        final Context context = new Context(Locale.US);
        context.setVariable("batchName", "SmokeBatch");
        context.setVariable("groupType", "batch");
        context.setVariable("groupValue", "SmokeBatch");
        context.setVariable("groupBy", "batch");
        context.setVariable("scopeTitle", "SmokeBatch");
        context.setVariable("backUrl", "");
        context.setVariable("isGroupingPinned", false);

        final BatchSummaryDto batchSummary = new BatchSummaryDto(
            "SmokeBatch", "SmokeBatch", "Production", "Description", "Today 12:00", "100%", "None",
            List.of("en-US"), List.of("Chrome"), "run-101", 1, 0, 0, 0, 0, 1);
        context.setVariable("batchSummary", batchSummary);
        context.setVariable("contributingRuns", List.of());
        context.setVariable("browserRatios", List.of());
        context.setVariable("localeRatios", List.of());
        context.setVariable("environmentRatios", List.of());
        context.setVariable("areaRatios", List.of());
        context.setVariable("tagRatios", List.of());
        context.setVariable("latestVariations", List.of());
        context.setVariable("runs", List.of());
        context.setVariable("totalRunsCount", 0);
        context.setVariable("currentLimit", 10);
        context.setVariable("showingRunsCount", 0);
        context.setVariable("areaTrends", List.of());

        final TestBaseVariationEntity var1 = new TestBaseVariationEntity("var-101", "CheckoutTest", "testCheckout", "Default", "@Checkout", "US", "Chrome");
        var1.setLastStatus("passed-clean");
        var1.setTotalExecutionsCount(1);
        final TestBaseClassDto classDto = new TestBaseClassDto("CheckoutTest", "batchClassContainerCheckoutTest", 1, List.of(var1));
        final TestBaseAreaDto areaDto = new TestBaseAreaDto("@Checkout", "batchAreaCheckout", "batchTabBtnCheckout", "shopping_cart", 1, List.of(classDto));

        context.setVariable("batchTestAreas", List.of(areaDto));
        context.setVariable("batchTestClasses", List.of(classDto));
        context.setVariable("totalBatchVariationsCount", 1);

        final String html = engine.process("fragments/batch-history", Collections.singleton("batchHistory"), context);
        Assertions.assertNotNull(html);
        Assertions.assertTrue(html.contains("id=\"pageBatchHistoryOverview\""), "Should render pageBatchHistoryOverview");
        Assertions.assertTrue(html.contains("id=\"batchRunsHistoryTableBody\""), "Should render runs table body");
    }

    @Test
    public void testBatchHistoryDirectoryFiltersIncludeDefaultAndNormalizedLocalesAndBrowsers()
    {
        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-wiki-1");
        r1.setBatchName("Wikipedia");
        r1.setEnvironment("Default");
        r1.setLocalesCsv("EN");
        r1.setBrowsersCsv("Chrome_1920x1080");
        final List<TestRunEntity> allRuns = List.of(r1);

        Mockito.when(runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc("Wikipedia")).thenReturn(allRuns);
        Mockito.when(batchRepository.findById("Wikipedia")).thenReturn(Optional.empty());

        // Variations have Default location alongside EN
        final TestBaseVariationEntity var1 = new TestBaseVariationEntity("var-1", "WikipediaSearch", "testSearch", "Default", "@Wikipedia", "Default", "Chrome_1920x1080");
        final TestBaseClassDto classDto = new TestBaseClassDto("WikipediaSearch", "batchClassContainerWikipediaSearch", 1, List.of(var1));
        Mockito.when(dataService.getBatchTestBaseClasses("Wikipedia", allRuns)).thenReturn(List.of(classDto));

        final Model model = new ConcurrentModel();
        final String view = controller.batchHistory("Wikipedia", null, "true", model);

        Assertions.assertEquals("fragments/batch-history :: batchHistory", view);
        @SuppressWarnings("unchecked")
        final List<String> filterLocales = (List<String>) model.getAttribute("filterLocales");
        Assertions.assertNotNull(filterLocales);
        Assertions.assertTrue(filterLocales.contains("Default"), "filterLocales must contain Default");
        Assertions.assertTrue(filterLocales.contains("EN") || filterLocales.contains("en"), "filterLocales must contain EN");

        @SuppressWarnings("unchecked")
        final List<String> filterBrowsers = (List<String>) model.getAttribute("filterBrowsers");
        Assertions.assertNotNull(filterBrowsers);
        Assertions.assertTrue(filterBrowsers.contains("Chrome_1920x1080"), "filterBrowsers must contain Chrome_1920x1080");

        @SuppressWarnings("unchecked")
        final List<String> filterAreas = (List<String>) model.getAttribute("filterAreas");
        Assertions.assertNotNull(filterAreas);
        Assertions.assertTrue(filterAreas.contains("@Wikipedia"), "filterAreas must contain @Wikipedia");
    }

    @Test
    public void testBatchHistoryWithCustomLimitAppliesLimit()
    {
        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-1");
        r1.setBatchName("SmokeBatch");
        r1.setEnvironment("Staging");
        r1.setLocalesCsv("US,DE");
        r1.setBrowsersCsv("Chrome");

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-2");
        r2.setBatchName("SmokeBatch");
        r2.setEnvironment("Staging");
        r2.setLocalesCsv("FR");
        r2.setBrowsersCsv("Firefox");

        final TestRunEntity r3 = new TestRunEntity();
        r3.setId("run-3");
        r3.setBatchName("SmokeBatch");
        r3.setEnvironment("Staging");
        r3.setLocalesCsv("JP");
        r3.setBrowsersCsv("Safari");

        final List<TestRunEntity> allRuns = Arrays.asList(r1, r2, r3);
        Mockito.when(runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc("SmokeBatch")).thenReturn(allRuns);
        Mockito.when(batchRepository.findById("SmokeBatch")).thenReturn(Optional.empty());

        final Model model = new ConcurrentModel();
        final String view = controller.batchHistory("SmokeBatch", 2, "true", model);

        Assertions.assertEquals("fragments/batch-history :: batchHistory", view);
        Assertions.assertEquals("SmokeBatch", model.getAttribute("batchName"));
        Assertions.assertEquals(3, model.getAttribute("totalRunsCount"));
        Assertions.assertEquals(2, model.getAttribute("currentLimit"));
        Assertions.assertEquals(2, model.getAttribute("showingRunsCount"));

        final List<?> runs = (List<?>) model.getAttribute("runs");
        Assertions.assertNotNull(runs);
        Assertions.assertEquals(2, runs.size());
        Assertions.assertSame(r1, runs.get(0));
        Assertions.assertSame(r2, runs.get(1));

        @SuppressWarnings("unchecked")
        final List<String> locales = (List<String>) model.getAttribute("batchLocales");
        Assertions.assertNotNull(locales);
        Assertions.assertTrue(locales.contains("US"));
        Assertions.assertTrue(locales.contains("DE"));
        Assertions.assertTrue(locales.contains("FR"));
        Assertions.assertFalse(locales.contains("JP"));

        @SuppressWarnings("unchecked")
        final List<String> browsers = (List<String>) model.getAttribute("batchBrowsers");
        Assertions.assertNotNull(browsers);
        Assertions.assertTrue(browsers.contains("Chrome"));
        Assertions.assertTrue(browsers.contains("Firefox"));
        Assertions.assertFalse(browsers.contains("Safari"));

        Mockito.verify(dataService).getBatchLatestTestExecutionsSummary(
            Mockito.eq("SmokeBatch"),
            Mockito.eq(List.of(r1, r2)),
            Mockito.anySet(),
            Mockito.nullable(Long.class)
        );
        Mockito.verify(dataService).getBatchTestBaseClasses(
            Mockito.eq("SmokeBatch"),
            Mockito.eq(List.of(r1, r2))
        );
    }

    @Test
    public void testBatchHistoryWithExclusionsAndDateCutoff()
    {
        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-1");
        r1.setBatchName("SmokeBatch");
        r1.setStartTimeMs(1700000000000L);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-2");
        r2.setBatchName("SmokeBatch");
        r2.setStartTimeMs(1770000000000L);

        final List<TestRunEntity> allRuns = Arrays.asList(r2, r1);
        Mockito.when(runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc("SmokeBatch")).thenReturn(allRuns);
        Mockito.when(batchRepository.findById("SmokeBatch")).thenReturn(Optional.empty());

        final BatchSummaryDto summary = new BatchSummaryDto(
            "summary-1",
            "SmokeBatch",
            "Staging",
            "Desc",
            "Today",
            "100%",
            "0",
            List.of("en_US"),
            List.of("Chrome")
        );
        final BatchLatestExecutionsDto mockResult = new BatchLatestExecutionsDto(
            summary,
            List.of(r2),
            List.of(new DistributionRatioDto("Chrome", 1, 100.0, "var(--status-pass)")),
            List.of(new DistributionRatioDto("en_US", 1, 100.0, "var(--status-pass)")),
            List.of(new DistributionRatioDto("Staging", 1, 100.0, "var(--status-pass)")),
            List.of(),
            1
        );

        Mockito.when(dataService.getBatchLatestTestExecutionsSummary(
            Mockito.eq("SmokeBatch"),
            Mockito.eq(allRuns),
            Mockito.argThat(set -> set != null && set.contains("run-1")),
            Mockito.isNotNull()
        )).thenReturn(mockResult);

        final Model model = new ConcurrentModel();
        final String view = controller.batchHistory("SmokeBatch", null, "run-1", "2026-01-01", "true", model);

        Assertions.assertEquals("fragments/batch-history :: batchHistory", view);
        Assertions.assertEquals("SmokeBatch", model.getAttribute("batchName"));
        Assertions.assertEquals("run-1", model.getAttribute("excludeRuns"));
        Assertions.assertEquals("2026-01-01", model.getAttribute("olderThanDate"));
        Assertions.assertEquals(summary, model.getAttribute("batchSummary"));

        @SuppressWarnings("unchecked")
        final List<TestRunEntity> contributingRuns = (List<TestRunEntity>) model.getAttribute("contributingRuns");
        Assertions.assertNotNull(contributingRuns);
        Assertions.assertEquals(1, contributingRuns.size());
        Assertions.assertEquals("run-2", contributingRuns.get(0).getId());

        @SuppressWarnings("unchecked")
        final List<DistributionRatioDto> browserRatios = (List<DistributionRatioDto>) model.getAttribute("browserRatios");
        Assertions.assertNotNull(browserRatios);
        Assertions.assertEquals(1, browserRatios.size());

        @SuppressWarnings("unchecked")
        final List<DistributionRatioDto> localeRatios = (List<DistributionRatioDto>) model.getAttribute("localeRatios");
        Assertions.assertNotNull(localeRatios);
        Assertions.assertEquals(1, localeRatios.size());

        @SuppressWarnings("unchecked")
        final List<DistributionRatioDto> environmentRatios = (List<DistributionRatioDto>) model.getAttribute("environmentRatios");
        Assertions.assertNotNull(environmentRatios);
        Assertions.assertEquals(1, environmentRatios.size());

        Assertions.assertNotNull(model.getAttribute("latestExecResult"));
        Assertions.assertNotNull(model.getAttribute("latestVariations"));

        Mockito.verify(dataService).getBatchTestBaseClasses(
            Mockito.eq("SmokeBatch"),
            Mockito.eq(List.of(r2))
        );
    }

    @Test
    public void testBatchHistoryWithRunScopeFilters()
    {
        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-1");
        r1.setBatchName("SmokeBatch");
        r1.setEnvironment("Default");
        r1.setBrowsersCsv("Chrome_1920x1080");
        r1.setLocalesCsv("EN");
        r1.setStartTimeMs(1000L);

        final List<TestRunEntity> allRuns = Arrays.asList(r1);
        Mockito.when(runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc("SmokeBatch")).thenReturn(allRuns);
        Mockito.when(batchRepository.findById("SmokeBatch")).thenReturn(Optional.empty());

        final BatchSummaryDto summary = new BatchSummaryDto("batch-SmokeBatch", "SmokeBatch", "Default", "", "Today", "100%", "", List.of(), List.of());
        final BatchLatestExecutionsDto mockResult = new BatchLatestExecutionsDto(
            summary,
            List.of(r1),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            1,
            1
        );

        Mockito.when(dataService.getBatchLatestTestExecutionsSummary(
            Mockito.eq("SmokeBatch"),
            Mockito.eq(allRuns),
            Mockito.anySet(),
            Mockito.isNull(),
            Mockito.argThat(set -> set != null && set.contains("Chrome_1920x1080")),
            Mockito.argThat(set -> set != null && set.contains("EN")),
            Mockito.argThat(set -> set != null && set.contains("Default")),
            Mockito.anySet(),
            Mockito.argThat(set -> set != null && set.contains("SmokeBatch")),
            Mockito.anyMap()
        )).thenReturn(mockResult);

        final Model model = new ConcurrentModel();
        final String view = controller.batchHistory(
            "SmokeBatch",
            null,
            null,
            null,
            null,
            null,
            "Chrome_1920x1080",
            "EN",
            "Default",
            null,
            "SmokeBatch",
            "true",
            model
        );

        Assertions.assertEquals("fragments/batch-history :: batchHistory", view);
        Assertions.assertEquals("Chrome_1920x1080", model.getAttribute("runBrowsers"));
        Assertions.assertEquals("EN", model.getAttribute("runLocales"));
        Assertions.assertEquals("Default", model.getAttribute("runEnvs"));
        Assertions.assertEquals("SmokeBatch", model.getAttribute("runTags"));
        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("hasActiveRunFilters"));
        Assertions.assertEquals(4, model.getAttribute("activeRunFiltersCount"));
        Assertions.assertEquals(1, model.getAttribute("eligibleRunsCount"));
        Assertions.assertNotNull(model.getAttribute("candidateBrowsers"));
        Assertions.assertNotNull(model.getAttribute("candidateLocales"));
        Assertions.assertNotNull(model.getAttribute("candidateEnvs"));
        Assertions.assertNotNull(model.getAttribute("candidateTags"));
        Assertions.assertNotNull(model.getAttribute("candidateAreas"));

        Mockito.verify(dataService).getBatchTestBaseClasses(
            Mockito.eq("SmokeBatch"),
            Mockito.eq(List.of(r1)),
            Mockito.argThat(set -> set != null && set.contains("Chrome_1920x1080")),
            Mockito.argThat(set -> set != null && set.contains("EN")),
            Mockito.argThat(set -> set != null && set.contains("Default")),
            Mockito.anySet(),
            Mockito.argThat(set -> set != null && set.contains("SmokeBatch"))
        );
    }

    @Test
    public void testBatchHistoryWithLimitGreaterThanTotalClampsGracefully()
    {
        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-1");
        r1.setBatchName("SmokeBatch");

        final List<TestRunEntity> allRuns = Arrays.asList(r1);
        Mockito.when(runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc("SmokeBatch")).thenReturn(allRuns);
        Mockito.when(batchRepository.findById("SmokeBatch")).thenReturn(Optional.empty());

        final Model model = new ConcurrentModel();
        final String view = controller.batchHistory("SmokeBatch", 50, "true", model);

        Assertions.assertEquals("fragments/batch-history :: batchHistory", view);
        Assertions.assertEquals(1, model.getAttribute("totalRunsCount"));
        Assertions.assertEquals(50, model.getAttribute("currentLimit"));
        Assertions.assertEquals(1, model.getAttribute("showingRunsCount"));

        final List<?> runs = (List<?>) model.getAttribute("runs");
        Assertions.assertNotNull(runs);
        Assertions.assertEquals(1, runs.size());
    }

    @Test
    public void testRefreshAndResyncRunsPreservesLimit()
    {
        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-1");
        r1.setBatchName("SmokeBatch");

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-2");
        r2.setBatchName("SmokeBatch");

        final TestRunEntity r3 = new TestRunEntity();
        r3.setId("run-3");
        r3.setBatchName("SmokeBatch");

        final List<TestRunEntity> allRuns = Arrays.asList(r1, r2, r3);
        Mockito.when(runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc("SmokeBatch")).thenReturn(allRuns);
        Mockito.when(batchRepository.findById("SmokeBatch")).thenReturn(Optional.empty());

        final Model model = new ConcurrentModel();
        final String currentUrl = "http://localhost:8080/batch-history?batchName=SmokeBatch&limit=1";
        final String view = controller.refreshAndResyncRuns("true", currentUrl, null, model);

        Assertions.assertEquals("fragments/batch-history :: batchHistory", view);
        Assertions.assertEquals("SmokeBatch", model.getAttribute("batchName"));
        Assertions.assertEquals(3, model.getAttribute("totalRunsCount"));
        Assertions.assertEquals(1, model.getAttribute("currentLimit"));
        Assertions.assertEquals(1, model.getAttribute("showingRunsCount"));
    }

    @Test
    public void testBatchHistoryPopulatesGranularHealingInAreaTrends()
    {
        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-100");
        r1.setBatchName("SmokeBatch");
        r1.setStartTimeMs(1000L);

        final List<TestRunEntity> allRuns = Arrays.asList(r1);
        Mockito.when(runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc("SmokeBatch")).thenReturn(allRuns);
        Mockito.when(batchRepository.findById("SmokeBatch")).thenReturn(Optional.empty());

        final RunReportDto runReport = Mockito.mock(RunReportDto.class);
        final AreaSummaryDto areaSummary = Mockito.mock(AreaSummaryDto.class);
        Mockito.when(areaSummary.getAreaName()).thenReturn("Checkout");
        Mockito.when(areaSummary.getPassCount()).thenReturn(4);
        Mockito.when(areaSummary.getFixedCount()).thenReturn(2);
        Mockito.when(areaSummary.getKnownCount()).thenReturn(1);
        Mockito.when(areaSummary.getUnknownCount()).thenReturn(1);
        Mockito.when(areaSummary.getIgnoredCount()).thenReturn(0);
        Mockito.when(areaSummary.getTotalCount()).thenReturn(8);
        Mockito.when(areaSummary.getPassHealedCount()).thenReturn(2);
        Mockito.when(areaSummary.getFixedHealedCount()).thenReturn(1);
        Mockito.when(areaSummary.getKnownHealedCount()).thenReturn(1);
        Mockito.when(areaSummary.getUnknownHealedCount()).thenReturn(0);

        Mockito.when(runReport.getAreaSummaries()).thenReturn(Arrays.asList(areaSummary));
        Mockito.when(dataService.getRunReport("run-100")).thenReturn(runReport);

        final Model model = new ConcurrentModel();
        final String view = controller.batchHistory("SmokeBatch", null, "true", model);

        Assertions.assertEquals("fragments/batch-history :: batchHistory", view);
        @SuppressWarnings("unchecked")
        final List<BatchAreaTrendDto> areaTrends = (List<BatchAreaTrendDto>) model.getAttribute("areaTrends");
        Assertions.assertNotNull(areaTrends);
        Assertions.assertEquals(1, areaTrends.size());

        final BatchAreaTrendDto trend = areaTrends.get(0);
        Assertions.assertEquals("Checkout", trend.getAreaName());
        Assertions.assertEquals(1, trend.getRunPoints().size());

        final AreaRunPointDto point = trend.getRunPoints().get(0);
        Assertions.assertEquals("run-100", point.getRunId());
        Assertions.assertEquals(4, point.getPassCount());
        Assertions.assertEquals(2, point.getPassHealedCount());
        Assertions.assertEquals(2, point.getPassCleanCount());
        Assertions.assertEquals(2, point.getFixedCount());
        Assertions.assertEquals(1, point.getFixedHealedCount());
        Assertions.assertEquals(1, point.getFixedCleanCount());
        Assertions.assertEquals(1, point.getKnownCount());
        Assertions.assertEquals(1, point.getKnownHealedCount());
        Assertions.assertEquals(0, point.getKnownCleanCount());
        Assertions.assertEquals(1, point.getUnknownCount());
        Assertions.assertEquals(0, point.getUnknownHealedCount());
        Assertions.assertEquals(1, point.getUnknownCleanCount());
    }

    @Test
    public void testRemoveBugFromExecutionResolvesRunIdFromCurrentUrlWhenPlaceholderProvided()
    {
        final String runIdPlaceholder = "#RUN_ID";
        final String currentUrl = "/run-report?runId=run-resolved-456";
        final TestExecutionDto execDto = new TestExecutionDto();
        execDto.setId("row-1");
        execDto.setStatus("failed-unknown");

        final RunReportDto reportDto = Mockito.mock(RunReportDto.class);
        Mockito.when(reportDto.getRunId()).thenReturn("run-resolved-456");

        Mockito.when(dataService.removeBugFromExecution("run-resolved-456", "row-1", "BUG-123"))
            .thenReturn(execDto);
        Mockito.when(dataService.getRunReport("run-resolved-456"))
            .thenReturn(reportDto);

        final Model model = new ConcurrentModel();
        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        final String view = controller.removeBugFromExecution(
            runIdPlaceholder, "row-1", null, "BUG-123", null, currentUrl, null, model, response);

        Assertions.assertEquals("fragments/side-panel-step-list :: sidePanelBugSection", view);
        Assertions.assertEquals("run-resolved-456", model.getAttribute("runId"));
        Mockito.verify(dataService).removeBugFromExecution("run-resolved-456", "row-1", "BUG-123");
    }

    @Test
    public void testRemoveBugFromExecutionStripsHashPrefixFromRunId()
    {
        final String runIdWithHash = "#run-with-hash-789";
        final TestExecutionDto execDto = new TestExecutionDto();
        execDto.setId("row-2");
        execDto.setStatus("failed-unknown");

        final RunReportDto reportDto = Mockito.mock(RunReportDto.class);
        Mockito.when(reportDto.getRunId()).thenReturn("run-with-hash-789");

        Mockito.when(dataService.removeBugFromExecution("run-with-hash-789", "row-2", "BUG-456"))
            .thenReturn(execDto);
        Mockito.when(dataService.getRunReport("run-with-hash-789"))
            .thenReturn(reportDto);

        final Model model = new ConcurrentModel();
        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        final String view = controller.removeBugFromExecution(
            runIdWithHash, "row-2", null, "BUG-456", null, null, null, model, response);

        Assertions.assertEquals("fragments/side-panel-step-list :: sidePanelBugSection", view);
        Assertions.assertEquals("run-with-hash-789", model.getAttribute("runId"));
        Mockito.verify(dataService).removeBugFromExecution("run-with-hash-789", "row-2", "BUG-456");
    }

    @Test
    public void testRemoveBugFromExecutionWithCompositeKeyContainingHashes()
    {
        final String runId = "run-comp-1";
        final String compositeRowId = "Aura_google_test_yaml_Test#executeYamlTest#Default#Chrome_1920x1080";
        final String ticket = "BUG-48868";

        final TestExecutionDto execDto = new TestExecutionDto();
        execDto.setId(compositeRowId);
        execDto.setStatus("failed-unknown");
        execDto.setBugs(List.of());

        final RunReportDto reportDto = Mockito.mock(RunReportDto.class);
        Mockito.when(reportDto.getRunId()).thenReturn(runId);

        Mockito.when(dataService.removeBugFromExecution(runId, compositeRowId, ticket))
            .thenReturn(execDto);
        Mockito.when(dataService.getRunReport(runId))
            .thenReturn(reportDto);

        final Model model = new ConcurrentModel();
        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        final String view = controller.removeBugFromExecution(
            runId, compositeRowId, null, ticket, null, null, null, model, response);

        Assertions.assertEquals("fragments/side-panel-step-list :: sidePanelBugSection", view);
        Assertions.assertEquals(runId, model.getAttribute("runId"));
        Assertions.assertEquals(compositeRowId, model.getAttribute("rowId"));
        Assertions.assertEquals(execDto, model.getAttribute("exec"));
        Mockito.verify(dataService).removeBugFromExecution(runId, compositeRowId, ticket);
    }

    @Test
    public void testRemoveBugFromExecutionFallsBackToExecutionDetailsWhenRemovalYieldsHollowDto()
    {
        final String runId = "run-comp-2";
        final String compositeRowId = "Aura_test#method#title#Chrome";

        final TestExecutionDto existingExec = new TestExecutionDto();
        existingExec.setId(compositeRowId);
        existingExec.setStatus("failed-known");
        existingExec.setBugs(List.of("BUG-EXISTING-1"));

        final RunReportDto reportDto = Mockito.mock(RunReportDto.class);
        Mockito.when(reportDto.getRunId()).thenReturn(runId);

        // When dataService returns a hollow DTO (e.g. bugTicket was null or not found)
        Mockito.when(dataService.removeBugFromExecution(runId, compositeRowId, null))
            .thenReturn(new TestExecutionDto());
        Mockito.when(dataService.getExecutionDetails(runId, compositeRowId))
            .thenReturn(existingExec);
        Mockito.when(dataService.getRunReport(runId))
            .thenReturn(reportDto);

        final Model model = new ConcurrentModel();
        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        final String view = controller.removeBugFromExecution(
            runId, compositeRowId, null, null, null, null, null, model, response);

        Assertions.assertEquals("fragments/side-panel-step-list :: sidePanelBugSection", view);
        Assertions.assertEquals(existingExec, model.getAttribute("exec"));
        Assertions.assertEquals(List.of("BUG-EXISTING-1"), ((TestExecutionDto) model.getAttribute("exec")).getBugs());
    }

    @Test
    public void testDeleteRunRemovesRunAndReturnsBatchOverview()
    {
        final BatchOverviewDataDto overviewData = new BatchOverviewDataDto(
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );
        Mockito.when(dataService.getGroupOverviewData("batch")).thenReturn(overviewData);

        final Model model = new ConcurrentModel();
        final String view = controller.deleteRun("RUN-100", null, "true", "http://localhost:8080/batch-overview?groupBy=batch", null, model);

        Assertions.assertEquals("fragments/batch-history :: batchHistory", view);
        Mockito.verify(dataService).removeRunFromHistory("RUN-100");
    }

    @Test
    public void testDeleteRunFromBatchHistoryReturnsBatchHistoryFragment()
    {
        final String batchName = "SmokeBatch";
        final List<TestRunEntity> runs = List.of();
        Mockito.when(runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc(batchName)).thenReturn(runs);
        Mockito.when(batchRepository.findById(batchName)).thenReturn(Optional.empty());

        final Model model = new ConcurrentModel();
        final String view = controller.deleteRun(null, "RUN-200", "true", "http://localhost:8080/batch-history?batchName=" + batchName + "&limit=10", null, model);

        Assertions.assertEquals("fragments/batch-history :: batchHistory", view);
        Assertions.assertEquals(batchName, model.getAttribute("batchName"));
        Mockito.verify(dataService).removeRunFromHistory("RUN-200");
    }

    @Test
    public void testDeleteRunFromTestBaseReturnsTestBaseFragment()
    {
        final TestBaseDataDto testBaseData = new TestBaseDataDto(
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            Map.of(),
            0,
            0
        );
        Mockito.when(dataService.getTestBaseData()).thenReturn(testBaseData);

        final Model model = new ConcurrentModel();
        final String view = controller.deleteRun("RUN-300", null, "true", "http://localhost:8080/test-base?testName=CheckoutTest", null, model);

        Assertions.assertEquals("fragments/test-base :: testBase", view);
        Assertions.assertEquals("CheckoutTest", model.getAttribute("targetTestName"));
        Mockito.verify(dataService).removeRunFromHistory("RUN-300");
    }

    @Test
    public void testBatchOverviewFullPageAndHtmx()
    {
        final BatchOverviewDataDto overviewData = new BatchOverviewDataDto(
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );
        Mockito.when(dataService.getGroupOverviewData("environment")).thenReturn(overviewData);

        final Model model = new ConcurrentModel();
        final String viewFull = controller.batchOverview("environment", "false", model);
        Assertions.assertEquals("index", viewFull);
        Assertions.assertEquals("Directory", model.getAttribute("activeTab"));
        Assertions.assertEquals("Runs overview", model.getAttribute("pageTitle"));
        Assertions.assertEquals("fragments/batch-history :: batchHistory", model.getAttribute("viewFragment"));

        final String viewHtmx = controller.batchOverview("environment", "true", model);
        Assertions.assertEquals("fragments/batch-history :: batchHistory", viewHtmx);

        // Without grouping parameter, defaults to all runs history
        final Model allRunsModel = new ConcurrentModel();
        final String viewAllRuns = controller.batchOverview(null, "false", allRunsModel);
        Assertions.assertEquals("index", viewAllRuns);
        Assertions.assertEquals("Directory", allRunsModel.getAttribute("activeTab"));
        Assertions.assertEquals("Runs overview", allRunsModel.getAttribute("pageTitle"));
        Assertions.assertEquals("fragments/batch-history :: batchHistory", allRunsModel.getAttribute("viewFragment"));
    }

    @Test
    public void testRootRouteMappingForBatchOverview() throws Exception
    {
        final BatchOverviewDataDto overviewData = new BatchOverviewDataDto(
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );
        Mockito.when(dataService.getBatchOverviewData()).thenReturn(overviewData);

        final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(view().name("index"))
            .andExpect(model().attribute("activeTab", "Directory"));

        mockMvc.perform(get("/report"))
            .andExpect(status().isOk())
            .andExpect(view().name("index"))
            .andExpect(model().attribute("activeTab", "Directory"));
    }

    @Test
    public void testParseGroupDimensionsAndBuildGroupLabel()
    {
        final List<String> singleDim = AuraReportViewController.parseGroupDimensions("environment");
        Assertions.assertEquals(List.of("environment"), singleDim);
        Assertions.assertEquals("Environments", AuraReportViewController.buildGroupLabel(singleDim));

        final List<String> areaDim = AuraReportViewController.parseGroupDimensions("area");
        Assertions.assertEquals(List.of("area"), areaDim);
        Assertions.assertEquals("Areas", AuraReportViewController.buildGroupLabel(areaDim));

        final List<String> multiDims = AuraReportViewController.parseGroupDimensions("batch, environment, browser, area");
        Assertions.assertEquals(List.of("batch", "environment", "browser", "area"), multiDims);
        Assertions.assertEquals("Batches, Environments, Browsers & Areas", AuraReportViewController.buildGroupLabel(multiDims));

        final List<String> aliasDims = AuraReportViewController.parseGroupDimensions("tag,env,locales,browsers,areas,none,all");
        Assertions.assertEquals(List.of("batch", "environment", "locale", "browser", "area"), aliasDims);

        final List<String> dynamicDims = AuraReportViewController.parseGroupDimensions("Release, environment");
        Assertions.assertEquals(List.of("Release", "environment"), dynamicDims);
        Assertions.assertEquals("Release Tags & Environments", AuraReportViewController.buildGroupLabel(dynamicDims));

        final List<String> emptyDims = AuraReportViewController.parseGroupDimensions("");
        Assertions.assertTrue(emptyDims.isEmpty());
        Assertions.assertEquals("Known Batches", AuraReportViewController.buildGroupLabel(emptyDims));
    }

    @Test
    public void testComputeToggleUrl()
    {
        final List<String> initial = List.of("batch", "environment");

        // Toggle on dimension not currently active -> appends
        final String addedLocale = AuraReportViewController.computeToggleUrl(initial, "locale");
        Assertions.assertEquals("/?groupBy=batch,environment,locale", addedLocale);

        final String addedArea = AuraReportViewController.computeToggleUrl(initial, "area");
        Assertions.assertEquals("/?groupBy=batch,environment,area", addedArea);

        // Toggle off active dimension -> removes
        final String removedBatch = AuraReportViewController.computeToggleUrl(initial, "batch");
        Assertions.assertEquals("/?groupBy=environment", removedBatch);

        // Case-insensitive toggle off
        final String removedEnvCase = AuraReportViewController.computeToggleUrl(initial, "ENVIRONMENT");
        Assertions.assertEquals("/?groupBy=batch", removedEnvCase);

        // Toggle off last active dimension -> resets to "/?groupBy=none"
        final String cleared = AuraReportViewController.computeToggleUrl(List.of("batch"), "batch");
        Assertions.assertEquals("/?groupBy=none", cleared);
    }

    @Test
    public void testBatchOverviewMultiDimensionalGrouping()
    {
        final BatchOverviewDataDto overviewData = new BatchOverviewDataDto(
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );
        Mockito.when(dataService.getGroupOverviewData(List.of("batch", "environment"))).thenReturn(overviewData);

        final Model model = new ConcurrentModel();
        final String view = controller.batchOverview("batch,environment", "false", model);

        Assertions.assertEquals("index", view);
        Assertions.assertEquals("Directory", model.getAttribute("activeTab"));
        Assertions.assertEquals("Runs overview", model.getAttribute("pageTitle"));
        Assertions.assertEquals("batch,environment", model.getAttribute("groupBy"));
        Assertions.assertEquals("Batches & Environments", model.getAttribute("groupLabel"));
        Assertions.assertEquals(true, model.getAttribute("isBatchActive"));
        Assertions.assertEquals(true, model.getAttribute("isEnvActive"));
        Assertions.assertEquals(false, model.getAttribute("isLocaleActive"));
        Assertions.assertEquals(false, model.getAttribute("isBrowserActive"));
        Assertions.assertEquals(false, model.getAttribute("isAreaActive"));
        Assertions.assertEquals("/?groupBy=environment", model.getAttribute("toggleBatchUrl"));
        Assertions.assertEquals("/?groupBy=batch", model.getAttribute("toggleEnvUrl"));
        Assertions.assertEquals("/?groupBy=batch,environment,locale", model.getAttribute("toggleLocaleUrl"));
        Assertions.assertEquals("/?groupBy=batch,environment,browser", model.getAttribute("toggleBrowserUrl"));
        Assertions.assertEquals("/?groupBy=batch,environment,area", model.getAttribute("toggleAreaUrl"));
        Assertions.assertEquals("fragments/batch-history :: batchHistory", model.getAttribute("viewFragment"));
    }

    @Test
    public void testBatchOverviewAreaGrouping()
    {
        final BatchOverviewDataDto overviewData = new BatchOverviewDataDto(
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );
        Mockito.when(dataService.getGroupOverviewData("area")).thenReturn(overviewData);

        final Model model = new ConcurrentModel();
        final String view = controller.batchOverview("area", "false", model);

        Assertions.assertEquals("index", view);
        Assertions.assertEquals("Directory", model.getAttribute("activeTab"));
        Assertions.assertEquals("Runs overview", model.getAttribute("pageTitle"));
        Assertions.assertEquals("area", model.getAttribute("groupBy"));
        Assertions.assertEquals("Areas", model.getAttribute("groupLabel"));
        Assertions.assertEquals(false, model.getAttribute("isBatchActive"));
        Assertions.assertEquals(false, model.getAttribute("isEnvActive"));
        Assertions.assertEquals(false, model.getAttribute("isLocaleActive"));
        Assertions.assertEquals(false, model.getAttribute("isBrowserActive"));
        Assertions.assertEquals(true, model.getAttribute("isAreaActive"));
        Assertions.assertEquals("/?groupBy=none", model.getAttribute("toggleAreaUrl"));
        Assertions.assertEquals("/?groupBy=area,batch", model.getAttribute("toggleBatchUrl"));
        Assertions.assertEquals("fragments/batch-history :: batchHistory", model.getAttribute("viewFragment"));
    }

    @Test
    public void testBatchHistoryMultiDimensionalGroupingFiltering()
    {
        final TestRunEntity run1 = new TestRunEntity();
        run1.setId("run-1");
        run1.setBatchName("SmokeBatch");
        run1.setEnvironment("Staging");
        run1.setBrowsersCsv("Chrome");
        run1.setLocalesCsv("en_US");
        run1.setStartTimeMs(1000L);

        final TestRunEntity run2 = new TestRunEntity();
        run2.setId("run-2");
        run2.setBatchName("SmokeBatch");
        run2.setEnvironment("Production");
        run2.setBrowsersCsv("Chrome");
        run2.setLocalesCsv("en_US");
        run2.setStartTimeMs(2000L);

        final TestRunEntity run3 = new TestRunEntity();
        run3.setId("run-3");
        run3.setBatchName("RegressionBatch");
        run3.setEnvironment("Staging");
        run3.setBrowsersCsv("Chrome");
        run3.setLocalesCsv("en_US");
        run3.setStartTimeMs(3000L);

        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of(run3, run2, run1));

        final Model model = new ConcurrentModel();
        final String view = controller.batchHistory(
            null,
            "batch,environment",
            "SmokeBatch • Staging",
            10,
            null,
            null,
            "false",
            model
        );

        Assertions.assertEquals("index", view);
        Assertions.assertEquals("batch,environment", model.getAttribute("groupType"));
        Assertions.assertEquals("SmokeBatch • Staging", model.getAttribute("groupValue"));
        Assertions.assertEquals("SmokeBatch", model.getAttribute("batchName"));
        Assertions.assertEquals("SmokeBatch • Staging", model.getAttribute("scopeTitle"));
        Assertions.assertEquals("/?groupBy=batch,environment", model.getAttribute("backUrl"));
        Assertions.assertEquals(true, model.getAttribute("isBatchActive"));
        Assertions.assertEquals(true, model.getAttribute("isEnvActive"));
        Assertions.assertEquals(false, model.getAttribute("isLocaleActive"));
        Assertions.assertEquals(false, model.getAttribute("isBrowserActive"));

        Assertions.assertEquals("SmokeBatch • Staging", model.getAttribute("pageTitle"));

        @SuppressWarnings("unchecked")
        final List<TestRunEntity> runs = (List<TestRunEntity>) model.getAttribute("runs");
        Assertions.assertNotNull(runs);
        Assertions.assertEquals(1, runs.size());
        Assertions.assertEquals("run-1", runs.get(0).getId());
    }

    @Test
    public void testBatchHistorySingleDimensionGroupingHeading()
    {
        final TestRunEntity run = new TestRunEntity();
        run.setId("run-env-1");
        run.setBatchName("SmokeBatch");
        run.setEnvironment("Default");
        run.setBrowsersCsv("Chrome");
        run.setLocalesCsv("en_US");
        run.setStartTimeMs(1000L);

        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of(run));
        Mockito.when(runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc("SmokeBatch")).thenReturn(List.of(run));

        final Model envModel = new ConcurrentModel();
        controller.batchHistory(null, "environment", "Default", 10, null, null, "false", envModel);
        Assertions.assertEquals("Default", envModel.getAttribute("scopeTitle"));
        Assertions.assertEquals("Default", envModel.getAttribute("pageTitle"));

        final Model browserModel = new ConcurrentModel();
        controller.batchHistory(null, "browser", "Chrome", 10, null, null, "false", browserModel);
        Assertions.assertEquals("Chrome", browserModel.getAttribute("scopeTitle"));
        Assertions.assertEquals("Chrome", browserModel.getAttribute("pageTitle"));

        final Model batchModel = new ConcurrentModel();
        controller.batchHistory("SmokeBatch", null, null, 10, null, null, "false", batchModel);
        Assertions.assertEquals("SmokeBatch", batchModel.getAttribute("scopeTitle"));
        Assertions.assertEquals("SmokeBatch", batchModel.getAttribute("pageTitle"));

        final Model areaModel = new ConcurrentModel();
        controller.batchHistory(null, "area", "@Checkout", 10, null, null, "false", areaModel);
        Assertions.assertEquals("@Checkout", areaModel.getAttribute("scopeTitle"));
        Assertions.assertEquals("@Checkout", areaModel.getAttribute("pageTitle"));
        Assertions.assertEquals(true, areaModel.getAttribute("isAreaActive"));
        Assertions.assertEquals("/?groupBy=area", areaModel.getAttribute("backUrl"));
    }

    @Test
    public void testBatchHistoryAreaGroupingFiltering()
    {
        final TestRunEntity run1 = new TestRunEntity();
        run1.setId("run-area-1");
        run1.setBatchName("SmokeBatch");
        run1.setEnvironment("Default");
        run1.setBrowsersCsv("Chrome");
        run1.setLocalesCsv("en_US");
        run1.setAreasCsv("@Checkout");
        run1.setStartTimeMs(1000L);

        final TestRunEntity run2 = new TestRunEntity();
        run2.setId("run-area-2");
        run2.setBatchName("SmokeBatch");
        run2.setEnvironment("Default");
        run2.setBrowsersCsv("Chrome");
        run2.setLocalesCsv("en_US");
        run2.setAreasCsv("@Cart");
        run2.setStartTimeMs(2000L);

        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of(run2, run1));
        Mockito.when(dataService.getRunAreas(run1)).thenReturn(List.of("@Checkout"));
        Mockito.when(dataService.getRunAreas(run2)).thenReturn(List.of("@Cart"));

        final Model model = new ConcurrentModel();
        final String view = controller.batchHistory(
            null,
            "area",
            "@Checkout",
            10,
            null,
            null,
            "false",
            model
        );

        Assertions.assertEquals("index", view);
        Assertions.assertEquals("area", model.getAttribute("groupType"));
        Assertions.assertEquals("@Checkout", model.getAttribute("groupValue"));
        Assertions.assertEquals("@Checkout", model.getAttribute("scopeTitle"));
        Assertions.assertEquals("/?groupBy=area", model.getAttribute("backUrl"));
        Assertions.assertEquals(true, model.getAttribute("isAreaActive"));

        @SuppressWarnings("unchecked")
        final List<TestRunEntity> runs = (List<TestRunEntity>) model.getAttribute("runs");
        Assertions.assertNotNull(runs);
        Assertions.assertEquals(1, runs.size());
        Assertions.assertEquals("run-area-1", runs.get(0).getId());
    }

    @Test
    public void testBatchHistoryMultiDimensionalGroupingWithArea()
    {
        final TestRunEntity run1 = new TestRunEntity();
        run1.setId("run-compound-1");
        run1.setBatchName("SmokeBatch");
        run1.setEnvironment("Staging");
        run1.setBrowsersCsv("Chrome");
        run1.setLocalesCsv("en_US");
        run1.setAreasCsv("@Checkout");
        run1.setStartTimeMs(1000L);

        final TestRunEntity run2 = new TestRunEntity();
        run2.setId("run-compound-2");
        run2.setBatchName("SmokeBatch");
        run2.setEnvironment("Staging");
        run2.setBrowsersCsv("Chrome");
        run2.setLocalesCsv("en_US");
        run2.setAreasCsv("@Cart");
        run2.setStartTimeMs(2000L);

        final TestRunEntity run3 = new TestRunEntity();
        run3.setId("run-compound-3");
        run3.setBatchName("RegressionBatch");
        run3.setEnvironment("Staging");
        run3.setBrowsersCsv("Chrome");
        run3.setLocalesCsv("en_US");
        run3.setAreasCsv("@Checkout");
        run3.setStartTimeMs(3000L);

        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of(run3, run2, run1));
        Mockito.when(dataService.getRunAreas(run1)).thenReturn(List.of("@Checkout"));
        Mockito.when(dataService.getRunAreas(run2)).thenReturn(List.of("@Cart"));
        Mockito.when(dataService.getRunAreas(run3)).thenReturn(List.of("@Checkout"));

        final Model model = new ConcurrentModel();
        final String view = controller.batchHistory(
            null,
            "batch,area",
            "SmokeBatch • @Checkout",
            10,
            null,
            null,
            "false",
            model
        );

        Assertions.assertEquals("index", view);
        Assertions.assertEquals("batch,area", model.getAttribute("groupType"));
        Assertions.assertEquals("SmokeBatch • @Checkout", model.getAttribute("groupValue"));
        Assertions.assertEquals("SmokeBatch • @Checkout", model.getAttribute("scopeTitle"));
        Assertions.assertEquals("/?groupBy=batch,area", model.getAttribute("backUrl"));
        Assertions.assertEquals(true, model.getAttribute("isBatchActive"));
        Assertions.assertEquals(true, model.getAttribute("isAreaActive"));
        Assertions.assertEquals(false, model.getAttribute("isEnvActive"));

        @SuppressWarnings("unchecked")
        final List<TestRunEntity> runs = (List<TestRunEntity>) model.getAttribute("runs");
        Assertions.assertNotNull(runs);
        Assertions.assertEquals(1, runs.size());
        Assertions.assertEquals("run-compound-1", runs.get(0).getId());
    }

    @Test
    public void testBatchHistoryTemplateRendersWithoutSpelErrorsWhenAiPassedTestsPresent()
    {
        final SpringTemplateEngine engine = new SpringTemplateEngine();
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        engine.setTemplateResolver(resolver);

        final StandardLinkBuilder linkBuilder = new StandardLinkBuilder()
        {
            @Override
            protected String computeContextPath(final IExpressionContext context, final String base, final Map<String, Object> parameters)
            {
                return "";
            }
        };
        engine.setLinkBuilder(linkBuilder);

        final BatchSummaryDto batchSummary = new BatchSummaryDto(
            "batch-1",
            "BatchOne",
            "Staging",
            "Batch description",
            "Recently",
            "100%",
            "0 known",
            List.of("en_US"),
            List.of("Chrome"),
            "run-101",
            10,
            2,
            1,
            0,
            0,
            13,
            2,
            1,
            0,
            0,
            3,
            1,
            1,
            0
        );

        final TestRunEntity contributingRun = new TestRunEntity();
        contributingRun.setId("run-101");
        contributingRun.setPassedCount(10);
        contributingRun.setTotalTests(10);

        final BatchLatestVariationDto latestVar = new BatchLatestVariationDto(
            "TestClass|testMethod||US|Chrome",
            "passed-clean",
            false,
            "JAVA",
            "run-101",
            "Staging",
            "Chrome",
            "US"
        );
        final BatchLatestExecutionsDto latestExecResult = new BatchLatestExecutionsDto(
            batchSummary,
            List.of(contributingRun),
            List.of(new DistributionRatioDto("Chrome", 1, 100.0, "var(--status-pass)")),
            List.of(new DistributionRatioDto("en_US", 1, 100.0, "var(--status-pass)")),
            List.of(new DistributionRatioDto("Staging", 1, 100.0, "var(--status-pass)")),
            List.of(new DistributionRatioDto("@Checkout", 1, 100.0, "var(--status-pass)")),
            List.of(new DistributionRatioDto("SmokeBatch", 1, 100.0, "var(--status-pass)")),
            List.of(latestVar),
            1
        );

        final Context context = new Context();
        context.setVariable("batchName", "BatchOne");
        context.setVariable("batchSummary", batchSummary);
        context.setVariable("latestExecResult", latestExecResult);
        context.setVariable("latestVariations", List.of(latestVar));
        context.setVariable("contributingRuns", List.of(contributingRun));
        context.setVariable("excludeRuns", "run-99");
        context.setVariable("olderThanDate", "2026-01-01");
        context.setVariable("excludedRunIds", Collections.singleton("run-99"));
        context.setVariable("batchEnvironment", "Staging");
        context.setVariable("browserRatios", List.of(new DistributionRatioDto("Chrome", 1, 100.0, "var(--status-pass)")));
        context.setVariable("localeRatios", List.of(new DistributionRatioDto("en_US", 1, 100.0, "var(--status-pass)")));
        context.setVariable("environmentRatios", List.of(new DistributionRatioDto("Staging", 1, 100.0, "var(--status-pass)")));
        context.setVariable("areaRatios", List.of(new DistributionRatioDto("@Checkout", 1, 100.0, "var(--status-pass)")));
        context.setVariable("tagRatios", List.of(new DistributionRatioDto("SmokeBatch", 1, 100.0, "var(--status-pass)")));
        context.setVariable("filterEnvs", List.of("Staging"));
        context.setVariable("filterLocales", List.of("en_US"));
        context.setVariable("filterBrowsers", List.of("Chrome"));
        context.setVariable("filterAreas", List.of("@Checkout"));
        final TestBaseVariationEntity varEntity = new TestBaseVariationEntity("var-101", "CheckoutTest", "testCheckout", "Default", "@Checkout", "US", "Chrome");
        varEntity.setLastStatus("passed-clean");
        varEntity.setTotalExecutionsCount(1);
        varEntity.setLastBatch("SmokeBatch");
        final TestBaseClassDto classDto = new TestBaseClassDto("CheckoutTest", "batchClassContainerCheckoutTest", 1, List.of(varEntity));

        final TestRunEntity historyRun = new TestRunEntity();
        historyRun.setId("run-101");
        historyRun.setBatchName("SmokeBatch");
        historyRun.setEnvironment("Staging");

        context.setVariable("runs", List.of(historyRun));
        context.setVariable("totalRunsCount", 1);
        context.setVariable("currentLimit", 10);
        context.setVariable("showingRunsCount", 1);
        context.setVariable("batchLocales", List.of("en_US"));
        context.setVariable("batchBrowsers", List.of("Chrome"));
        context.setVariable("areaTrends", List.of());
        context.setVariable("batchTestClasses", List.of(classDto));
        context.setVariable("totalBatchVariationsCount", 1);

        final String html = engine.process("fragments/batch-history", Collections.singleton("batchHistory"), context);
        Assertions.assertNotNull(html);
        Assertions.assertTrue(html.contains("id=\"pageBatchHistoryOverview\""), "batch-history fragment should render page container");
        Assertions.assertTrue(html.contains("id=\"batchRunsHistoryTableBody\""), "batch-history fragment should render runs table body");
        Assertions.assertTrue(html.contains("run-101"), "batch-history fragment should render run ID");
        Assertions.assertTrue(html.contains("data-batch=\"SmokeBatch\""), "Rendered HTML should contain data-batch attribute for SmokeBatch");
    }

    @Test
    public void testBatchHistoryRenderingFragmentWithEmptySummary()
    {
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        final SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        final StandardLinkBuilder linkBuilder = new StandardLinkBuilder()
        {
            @Override
            protected String computeContextPath(
                final IExpressionContext expressionContext,
                final String base,
                final Map<String, Object> parameters)
            {
                return "";
            }
        };
        engine.setLinkBuilder(linkBuilder);

        final BatchSummaryDto emptySummary = new BatchSummaryDto(
            "batch-1",
            "BatchOne",
            "Default",
            "",
            "No runs",
            "N/A",
            "",
            List.of(),
            List.of()
        );

        final Context context = new Context();
        context.setVariable("batchName", "BatchOne");
        context.setVariable("batchSummary", emptySummary);
        context.setVariable("latestExecResult", new BatchLatestExecutionsDto(emptySummary, List.of(), List.of(), List.of(), List.of(), List.of(), 0));
        context.setVariable("latestVariations", List.of());
        context.setVariable("contributingRuns", List.of());
        context.setVariable("excludeRuns", "run-101");
        context.setVariable("olderThanDate", "2026-01-01");
        context.setVariable("excludedRunIds", Collections.singleton("run-101"));
        context.setVariable("batchEnvironment", "Default");
        context.setVariable("browserRatios", List.of());
        context.setVariable("localeRatios", List.of());
        context.setVariable("environmentRatios", List.of());
        context.setVariable("areaRatios", List.of());
        context.setVariable("filterEnvs", List.of("Default"));
        context.setVariable("filterLocales", List.of());
        context.setVariable("filterBrowsers", List.of());
        context.setVariable("filterAreas", List.of());
        context.setVariable("runs", List.of());
        context.setVariable("totalRunsCount", 1);
        context.setVariable("currentLimit", null);
        context.setVariable("showingRunsCount", 1);
        context.setVariable("batchLocales", List.of());
        context.setVariable("batchBrowsers", List.of());
        context.setVariable("areaTrends", List.of());
        context.setVariable("batchTestClasses", List.of());
        context.setVariable("totalBatchVariationsCount", 0);

        final String html = engine.process("fragments/batch-history", Collections.singleton("batchHistory"), context);
        Assertions.assertNotNull(html);
        Assertions.assertTrue(html.contains("id=\"pageBatchHistoryOverview\""), "Container should be rendered");
        Assertions.assertTrue(html.contains("id=\"batchRunsHistoryTableBody\""), "Runs table body should be rendered");
        Assertions.assertTrue(html.contains("List of All Runs"), "Runs table header should be rendered");
    }

    @Test
    public void testBatchHistoryGroupingToolbarWhenGrouped()
    {
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        final SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        final StandardLinkBuilder linkBuilder = new StandardLinkBuilder()
        {
            @Override
            protected String computeContextPath(
                final IExpressionContext expressionContext,
                final String base,
                final Map<String, Object> parameters)
            {
                return "";
            }
        };
        engine.setLinkBuilder(linkBuilder);

        final BatchSummaryDto summary = new BatchSummaryDto(
            "batch-1",
            "BatchOne",
            "Default",
            "",
            "No runs",
            "N/A",
            "",
            List.of(),
            List.of()
        );

        final Context context = new Context();
        context.setVariable("batchName", "BatchOne");
        context.setVariable("batchSummary", summary);
        context.setVariable("latestExecResult", new BatchLatestExecutionsDto(summary, List.of(), List.of(), List.of(), List.of(), List.of(), 0));
        context.setVariable("latestVariations", List.of());
        context.setVariable("contributingRuns", List.of());
        context.setVariable("backUrl", "/?groupBy=environment");
        context.setVariable("batchEnvironment", "Default");
        context.setVariable("browserRatios", List.of());
        context.setVariable("localeRatios", List.of());
        context.setVariable("environmentRatios", List.of());
        context.setVariable("areaRatios", List.of());
        context.setVariable("filterEnvs", List.of("Default"));
        context.setVariable("filterLocales", List.of());
        context.setVariable("filterBrowsers", List.of());
        context.setVariable("filterAreas", List.of());
        context.setVariable("runs", List.of());
        context.setVariable("totalRunsCount", 0);
        context.setVariable("currentLimit", null);
        context.setVariable("showingRunsCount", 0);
        context.setVariable("batchLocales", List.of());
        context.setVariable("batchBrowsers", List.of());
        context.setVariable("areaTrends", List.of());
        context.setVariable("batchTestClasses", List.of());
        context.setVariable("totalBatchVariationsCount", 0);

        final String html = engine.process("fragments/batch-history", Collections.singleton("batchHistory"), context);
        Assertions.assertNotNull(html);
        Assertions.assertTrue(html.contains("id=\"pageBatchHistoryOverview\""), "Container should be rendered");
        Assertions.assertTrue(html.contains("id=\"batchRunsHistoryTableBody\""), "Runs table body should be rendered");
    }

    @Test
    public void testBatchOverviewAppliesPinnedGroupingWhenGroupByNull()
    {
        Mockito.when(settingService.getPinnedGrouping()).thenReturn("batch,environment");
        Mockito.when(settingService.isGroupingPinned("batch,environment")).thenReturn(true);

        final BatchOverviewDataDto overviewData = new BatchOverviewDataDto(
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );
        Mockito.when(dataService.getGroupOverviewData(List.of("batch", "environment"))).thenReturn(overviewData);

        final Model model = new ConcurrentModel();
        final String view = controller.batchOverview((String) null, "false", model);

        Assertions.assertEquals("index", view);
        Assertions.assertEquals("batch,environment", model.getAttribute("groupBy"));
        Assertions.assertEquals("Batches & Environments", model.getAttribute("groupLabel"));
        Assertions.assertEquals("batch,environment", model.getAttribute("pinnedGroupBy"));
        Assertions.assertEquals(true, model.getAttribute("isGroupingPinned"));
        Assertions.assertEquals(true, model.getAttribute("isBatchActive"));
        Assertions.assertEquals(true, model.getAttribute("isEnvActive"));
    }

    @Test
    public void testBatchOverviewExplicitNoneBypassesPinnedGrouping()
    {
        Mockito.when(settingService.getPinnedGrouping()).thenReturn("batch");
        Mockito.when(settingService.isGroupingPinned("all")).thenReturn(false);
        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of());

        final Model model = new ConcurrentModel();
        final String view = controller.batchOverview("none", "false", model);

        Assertions.assertEquals("index", view);
        Assertions.assertEquals("all", model.getAttribute("groupType"));
        Assertions.assertEquals("All Runs", model.getAttribute("scopeTitle"));
        Assertions.assertEquals("batch", model.getAttribute("pinnedGroupBy"));
        Assertions.assertEquals(false, model.getAttribute("isGroupingPinned"));
    }

    @Test
    public void testPinGroupingEndpointTogglesAndPersists()
    {
        final Model model = new ConcurrentModel();
        final BatchOverviewDataDto overviewData = new BatchOverviewDataDto(
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );
        Mockito.when(dataService.getGroupOverviewData("environment")).thenReturn(overviewData);
        Mockito.when(dataService.getGroupOverviewData(List.of("environment"))).thenReturn(overviewData);

        final String view = controller.pinGrouping("environment", "true", "http://localhost:8080/batch-overview?groupBy=environment", null, model);

        Mockito.verify(settingService).togglePinnedGrouping("environment");
        Assertions.assertEquals("fragments/batch-history :: batchHistory", view);
    }

    @Test
    public void testPinGroupingWhenInBatchHistory()
    {
        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of());

        final Model model = new ConcurrentModel();
        final String view = controller.pinGrouping("none", "true", "http://localhost:8080/batch-history?groupType=all", null, model);

        Mockito.verify(settingService).togglePinnedGrouping("none");
        Assertions.assertEquals("fragments/batch-history :: batchHistory", view);
    }

    @Test
    public void testBatchOverviewTemplateRendersPinButton()
    {
        final SpringTemplateEngine engine = new SpringTemplateEngine();
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        engine.setTemplateResolver(resolver);

        final StandardLinkBuilder linkBuilder = new StandardLinkBuilder()
        {
            @Override
            protected String computeContextPath(
                final IExpressionContext expressionContext,
                final String base,
                final Map<String, Object> parameters)
            {
                return "";
            }
        };
        engine.setLinkBuilder(linkBuilder);

        final Context context = new Context();
        context.setVariable("batchName", "all");
        context.setVariable("groupBy", "batch");
        context.setVariable("groupLabel", "Tags (Batches)");
        context.setVariable("isGroupingPinned", true);
        context.setVariable("isBatchActive", true);
        context.setVariable("isEnvActive", false);
        context.setVariable("isLocaleActive", false);
        context.setVariable("isBrowserActive", false);
        context.setVariable("isAreaActive", false);
        context.setVariable("batches", List.of());
        context.setVariable("filterEnvs", List.of());
        context.setVariable("filterLocales", List.of());
        context.setVariable("filterBrowsers", List.of());
        context.setVariable("filterAreas", List.of());
        context.setVariable("runs", List.of());
        context.setVariable("totalRunsCount", 0);
        context.setVariable("showingRunsCount", 0);
        context.setVariable("areaTrends", List.of());
        context.setVariable("batchTestClasses", List.of());
        context.setVariable("totalBatchVariationsCount", 0);
        context.setVariable("tagRatioCards", List.of());
        context.setVariable("browserRatios", List.of());
        context.setVariable("localeRatios", List.of());
        context.setVariable("areaRatios", List.of());

        final String html = engine.process("fragments/batch-history", Collections.singleton("batchHistory"), context);
        Assertions.assertNotNull(html);
        Assertions.assertTrue(html.contains("id=\"pageBatchHistoryOverview\""), "Should render pageBatchHistoryOverview");
        Assertions.assertTrue(html.contains("id=\"batchRunsHistoryTableBody\""), "Should render runs table body");
    }

    @Test
    public void testBatchHistoryTemplateRendersPinButtonWhenNotGrouped()
    {
        final SpringTemplateEngine engine = new SpringTemplateEngine();
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        engine.setTemplateResolver(resolver);

        final StandardLinkBuilder linkBuilder = new StandardLinkBuilder()
        {
            @Override
            protected String computeContextPath(
                final IExpressionContext expressionContext,
                final String base,
                final Map<String, Object> parameters)
            {
                return "";
            }
        };
        engine.setLinkBuilder(linkBuilder);

        final BatchSummaryDto summary = new BatchSummaryDto(
            "batch-1",
            "all",
            "Default",
            "",
            "No runs",
            "N/A",
            "",
            List.of(),
            List.of()
        );

        final Context context = new Context();
        context.setVariable("batchName", "all");
        context.setVariable("groupBy", "none");
        context.setVariable("isGroupingPinned", false);
        context.setVariable("batchSummary", summary);
        context.setVariable("latestExecResult", new BatchLatestExecutionsDto(summary, List.of(), List.of(), List.of(), List.of(), List.of(), 0));
        context.setVariable("latestVariations", List.of());
        context.setVariable("contributingRuns", List.of());
        context.setVariable("backUrl", null);
        context.setVariable("batchEnvironment", "Default");
        context.setVariable("browserRatios", List.of());
        context.setVariable("localeRatios", List.of());
        context.setVariable("environmentRatios", List.of());
        context.setVariable("areaRatios", List.of());
        context.setVariable("filterEnvs", List.of());
        context.setVariable("filterLocales", List.of());
        context.setVariable("filterBrowsers", List.of());
        context.setVariable("filterAreas", List.of());
        context.setVariable("runs", List.of());
        context.setVariable("totalRunsCount", 0);
        context.setVariable("currentLimit", null);
        context.setVariable("showingRunsCount", 0);
        context.setVariable("batchLocales", List.of());
        context.setVariable("batchBrowsers", List.of());
        context.setVariable("areaTrends", List.of());
        context.setVariable("batchTestClasses", List.of());
        context.setVariable("totalBatchVariationsCount", 0);

        final String html = engine.process("fragments/batch-history", Collections.singleton("batchHistory"), context);
        Assertions.assertNotNull(html);
        Assertions.assertTrue(html.contains("id=\"pageBatchHistoryOverview\""), "Should render pageBatchHistoryOverview");
        Assertions.assertTrue(html.contains("id=\"batchRunsHistoryTableBody\""), "Should render runs table body");
    }

    @Test
    public void testBatchHistoryScopeFiltersTemplateRendersSelectAllAndCheckboxes()
    {
        final SpringTemplateEngine engine = new SpringTemplateEngine();
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        engine.setTemplateResolver(resolver);

        final StandardLinkBuilder linkBuilder = new StandardLinkBuilder()
        {
            @Override
            protected String computeContextPath(
                final IExpressionContext expressionContext,
                final String base,
                final Map<String, Object> parameters)
            {
                return "";
            }
        };
        engine.setLinkBuilder(linkBuilder);

        final BatchSummaryDto summary = new BatchSummaryDto(
            "batch-1",
            "SmokeBatch",
            "Default",
            "",
            "No runs",
            "N/A",
            "",
            List.of(),
            List.of()
        );

        final Context context = new Context();
        context.setVariable("batchName", "SmokeBatch");
        context.setVariable("groupBy", "none");
        context.setVariable("isGroupingPinned", false);
        context.setVariable("batchSummary", summary);
        context.setVariable("latestExecResult", new BatchLatestExecutionsDto(summary, List.of(), List.of(), List.of(), List.of(), List.of(), 0));
        context.setVariable("latestVariations", List.of());
        context.setVariable("contributingRuns", List.of());
        context.setVariable("backUrl", null);
        context.setVariable("batchEnvironment", "Default");
        context.setVariable("browserRatios", List.of());
        context.setVariable("localeRatios", List.of());
        context.setVariable("environmentRatios", List.of());
        context.setVariable("areaRatios", List.of());
        context.setVariable("tagRatios", List.of());
        context.setVariable("runs", List.of());
        context.setVariable("totalRunsCount", 2);
        context.setVariable("eligibleRunsCount", 2);
        context.setVariable("currentLimit", null);
        context.setVariable("showingRunsCount", 2);
        context.setVariable("batchLocales", List.of());
        context.setVariable("batchBrowsers", List.of());
        context.setVariable("areaTrends", List.of());
        context.setVariable("batchTestClasses", List.of());
        context.setVariable("totalBatchVariationsCount", 0);

        context.setVariable("candidateBrowsers", List.of("Chrome_1920x1080", "Firefox_1920x1080"));
        context.setVariable("candidateLocales", List.of("EN", "DE"));
        context.setVariable("candidateEnvs", List.of("Default", "Staging"));
        context.setVariable("candidateAreas", List.of("@Checkout", "@Login"));
        context.setVariable("candidateTags", List.of("SmokeBatch", "Nightly"));

        context.setVariable("selectedRunBrowsers", List.of());
        context.setVariable("selectedRunLocales", List.of());
        context.setVariable("selectedRunEnvs", List.of());
        context.setVariable("selectedRunAreas", List.of());
        context.setVariable("selectedRunTags", List.of());
        context.setVariable("hasActiveRunFilters", false);

        final String html = engine.process("fragments/batch-history", Collections.singleton("batchHistory"), context);
        Assertions.assertNotNull(html);

        Assertions.assertTrue(html.contains("id=\"pageBatchHistoryOverview\""), "Container should be rendered");
        Assertions.assertTrue(html.contains("id=\"batchRunsHistoryTableBody\""), "Runs table body should be rendered");

        // Now test with _none_ selected for browsers and active filters bar
        final Context contextWithNone = new Context();
        for (final String varName : context.getVariableNames())
        {
            contextWithNone.setVariable(varName, context.getVariable(varName));
        }
        contextWithNone.setVariable("selectedRunBrowsers", new LinkedHashSet<>(Set.of("_none_")));
        contextWithNone.setVariable("hasActiveRunFilters", true);

        final String htmlWithNone = engine.process("fragments/batch-history", Collections.singleton("batchHistory"), contextWithNone);
        Assertions.assertNotNull(htmlWithNone);
        Assertions.assertTrue(htmlWithNone.contains("id=\"pageBatchHistoryOverview\""), "Container should be rendered");
        Assertions.assertTrue(htmlWithNone.contains("id=\"batchRunsHistoryTableBody\""), "Runs table body should be rendered");
    }

    @Test
    public void testBatchHistoryVariationsReflectHistoryLimitAndDropdownFilters()
    {
        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-h1");
        r1.setBatchName("SmokeHistory");
        r1.setStartTimeMs(2000L);
        r1.setBrowsersCsv("Chrome");

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-h2");
        r2.setBatchName("SmokeHistory");
        r2.setStartTimeMs(1000L);
        r2.setBrowsersCsv("Chrome");

        final List<TestRunEntity> allRuns = List.of(r1, r2);
        Mockito.when(runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc("SmokeHistory")).thenReturn(allRuns);
        Mockito.when(batchRepository.findById("SmokeHistory")).thenReturn(Optional.empty());

        final BatchSummaryDto summary = new BatchSummaryDto("b-1", "SmokeHistory", "Default", "", "", "", "", List.of(), List.of());
        final BatchLatestExecutionsDto mockResult1 = new BatchLatestExecutionsDto(
            summary,
            List.of(r1),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            1,
            1,
            List.of(r1)
        );
        Mockito.when(dataService.getBatchLatestTestExecutionsSummary(
            Mockito.eq("SmokeHistory"),
            Mockito.eq(List.of(r1)),
            Mockito.anySet(),
            Mockito.isNull()
        )).thenReturn(mockResult1);

        // When limit = 1: eligibleRuns has only r1
        final Model model1 = new ConcurrentModel();
        controller.batchHistory("SmokeHistory", 1, "true", model1);
        Mockito.verify(dataService).getBatchTestBaseClasses("SmokeHistory", List.of(r1));

        // When limit = 2: eligibleRuns has r1 and r2
        final BatchLatestExecutionsDto mockResult2 = new BatchLatestExecutionsDto(
            summary,
            List.of(r1),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            2,
            2,
            List.of(r1, r2)
        );
        Mockito.when(dataService.getBatchLatestTestExecutionsSummary(
            Mockito.eq("SmokeHistory"),
            Mockito.eq(List.of(r1, r2)),
            Mockito.anySet(),
            Mockito.isNull()
        )).thenReturn(mockResult2);

        final Model model2 = new ConcurrentModel();
        controller.batchHistory("SmokeHistory", 2, "true", model2);
        Mockito.verify(dataService).getBatchTestBaseClasses("SmokeHistory", List.of(r1, r2));
    }

    @Test
    public void testBatchHistoryAreaGroupingExecutionsIsolation()
    {
        final TestRunEntity rawRun = new TestRunEntity();
        rawRun.setId("run-mixed-1");
        rawRun.setBatchName("SmokeBatch");
        rawRun.setEnvironment("Default");
        rawRun.setBrowsersCsv("Chrome,Firefox");
        rawRun.setLocalesCsv("en_US,de_DE");
        rawRun.setTotalTests(7);

        final TestRunEntity scopedRun = new TestRunEntity();
        scopedRun.setId("run-mixed-1");
        scopedRun.setBatchName("SmokeBatch");
        scopedRun.setEnvironment("Default");
        scopedRun.setBrowsersCsv("Chrome");
        scopedRun.setLocalesCsv("en_US");
        scopedRun.setTotalTests(2);
        scopedRun.setPassedCount(2);

        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of(rawRun));
        Mockito.when(dataService.getRunAreas(rawRun)).thenReturn(List.of("@Browsing", "@Search"));

        final AreaSummaryDto searchArea = new AreaSummaryDto("@Search", "search", "", 2, 2, 0, 0, 0, 0, 0, List.of());
        final AreaSummaryDto browsingArea = new AreaSummaryDto("@Browsing", "browsing", "", 5, 4, 1, 0, 0, 0, 0, List.of());
        final RunReportDto runReport = new RunReportDto(
            "run-mixed-1",
            "SmokeBatch",
            "2026-10-26 12:00:00",
            "10s",
            7,
            6,
            1,
            0,
            0,
            0,
            0,
            List.of(),
            List.of(browsingArea, searchArea),
            0,
            0L,
            0.0,
            Map.of()
        );
        Mockito.when(dataService.getRunReport("run-mixed-1")).thenReturn(runReport);

        Mockito.when(dataService.scopeRunToCriteria(
            Mockito.eq(rawRun),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.eq(Set.of("@Search")),
            Mockito.anySet()
        )).thenReturn(scopedRun);

        final BatchSummaryDto summary = new BatchSummaryDto("summary", "@Search", "Default", "", "No runs", "N/A", "", List.of(), List.of());
        final BatchLatestExecutionsDto mockResult = new BatchLatestExecutionsDto(
            summary,
            List.of(scopedRun),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            1,
            1,
            List.of(scopedRun)
        );
        Mockito.when(dataService.getBatchLatestTestExecutionsSummary(
            Mockito.anyString(),
            Mockito.anyList(),
            Mockito.anySet(),
            Mockito.any(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.eq(Set.of("@Search")),
            Mockito.anySet(),
            Mockito.anyMap()
        )).thenReturn(mockResult);

        final Model model = new ConcurrentModel();
        final String view = controller.batchHistory(
            null,
            "area",
            "@Search",
            10,
            null,
            null,
            "false",
            model
        );

        Assertions.assertEquals("index", view);
        Assertions.assertEquals("area", model.getAttribute("groupType"));
        Assertions.assertEquals("@Search", model.getAttribute("groupValue"));

        @SuppressWarnings("unchecked")
        final List<TestRunEntity> runs = (List<TestRunEntity>) model.getAttribute("runs");
        Assertions.assertNotNull(runs);
        Assertions.assertEquals(1, runs.size());
        Assertions.assertEquals(2, runs.get(0).getTotalTests(), "Displayed run in table must show only @Search executions");

        @SuppressWarnings("unchecked")
        final List<TestRunEntity> contributingRuns = (List<TestRunEntity>) model.getAttribute("contributingRuns");
        Assertions.assertNotNull(contributingRuns);
        Assertions.assertEquals(1, contributingRuns.size());
        Assertions.assertEquals(2, contributingRuns.get(0).getTotalTests(), "Contributing run must show only @Search executions");

        @SuppressWarnings("unchecked")
        final List<BatchAreaTrendDto> areaTrends = (List<BatchAreaTrendDto>) model.getAttribute("areaTrends");
        Assertions.assertNotNull(areaTrends);
        Assertions.assertEquals(1, areaTrends.size(), "Area trends should only contain @Search");
        Assertions.assertEquals("@Search", areaTrends.get(0).getAreaName());
    }

    @Test
    public void testBatchHistoryTimespanFiltering()
    {
        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-timespan-1");
        r1.setBatchName("SmokeBatch");
        r1.setEnvironment("Default");
        r1.setStartTimeMs(1717200000000L);

        final List<TestRunEntity> allRuns = Arrays.asList(r1);
        Mockito.when(runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc("SmokeBatch")).thenReturn(allRuns);
        Mockito.when(batchRepository.findById("SmokeBatch")).thenReturn(Optional.empty());

        final BatchSummaryDto summary = new BatchSummaryDto("batch-SmokeBatch", "SmokeBatch", "Default", "", "Today", "100%", "", List.of(), List.of());
        final BatchLatestExecutionsDto mockResult = new BatchLatestExecutionsDto(
            summary,
            List.of(r1),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            1,
            1,
            List.of(r1)
        );

        Mockito.when(dataService.getBatchLatestTestExecutionsSummary(
            Mockito.eq("SmokeBatch"),
            Mockito.eq(allRuns),
            Mockito.anySet(),
            Mockito.isNotNull(),
            Mockito.isNotNull(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anyMap(),
            Mockito.anyMap()
        )).thenReturn(mockResult);

        final Model model = new ConcurrentModel();
        final String view = controller.batchHistory(
            "SmokeBatch",
            null,
            null,
            null,
            null,
            null,
            "2026-01-01",
            "2026-06-30",
            null,
            null,
            null,
            null,
            null,
            "true",
            model,
            Collections.emptyMap()
        );

        Assertions.assertEquals("fragments/batch-history :: batchHistory", view);
        Assertions.assertEquals("2026-01-01", model.getAttribute("startDate"));
        Assertions.assertEquals("2026-06-30", model.getAttribute("endDate"));
        Assertions.assertEquals("2026-01-01 to 2026-06-30", model.getAttribute("dateTimespanLabel"));
        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("hasActiveRunFilters"));
    }

    @Test
    public void testBatchHistoryDynamicTagFiltersByType()
    {
        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-tag-1");
        r1.setBatchName("SmokeBatch");
        r1.setEnvironment("Default");
        r1.setStartTimeMs(1000L);

        final List<TestRunEntity> allRuns = Arrays.asList(r1);
        Mockito.when(runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc("SmokeBatch")).thenReturn(allRuns);
        Mockito.when(batchRepository.findById("SmokeBatch")).thenReturn(Optional.empty());

        Mockito.when(dataService.getAllTagKeys()).thenReturn(List.of("batch", "owner", "team"));
        Mockito.when(dataService.getCandidateTagValues(Mockito.anyCollection())).thenReturn(Map.of(
            "owner", List.of("alice", "bob"),
            "team", List.of("checkout", "cart")
        ));

        final BatchSummaryDto summary = new BatchSummaryDto("batch-SmokeBatch", "SmokeBatch", "Default", "", "Today", "100%", "", List.of(), List.of());
        final BatchLatestExecutionsDto mockResult = new BatchLatestExecutionsDto(
            summary,
            List.of(r1),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            1,
            1,
            List.of(r1)
        );

        Mockito.when(dataService.getBatchLatestTestExecutionsSummary(
            Mockito.eq("SmokeBatch"),
            Mockito.eq(allRuns),
            Mockito.anySet(),
            Mockito.isNull(),
            Mockito.isNull(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anyMap(),
            Mockito.argThat(map -> map != null && map.containsKey("owner") && map.get("owner").contains("alice"))
        )).thenReturn(mockResult);

        final Model model = new ConcurrentModel();
        final Map<String, String> requestParams = Map.of(
            "runTag_owner", "alice",
            "runTag_team", "checkout"
        );

        final String view = controller.batchHistory(
            "SmokeBatch",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "true",
            model,
            requestParams
        );

        Assertions.assertEquals("fragments/batch-history :: batchHistory", view);
        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("hasActiveRunFilters"));

        @SuppressWarnings("unchecked")
        final List<RunTagTypeFilterDto> tagTypeFilters = (List<RunTagTypeFilterDto>) model.getAttribute("tagTypeFilters");
        Assertions.assertNotNull(tagTypeFilters);
        Assertions.assertTrue(tagTypeFilters.stream().anyMatch(f -> "owner".equalsIgnoreCase(f.getTagKey())));
        Assertions.assertTrue(tagTypeFilters.stream().anyMatch(f -> "team".equalsIgnoreCase(f.getTagKey())));

        @SuppressWarnings("unchecked")
        final Map<String, Set<String>> activeTagFiltersByType = (Map<String, Set<String>>) model.getAttribute("activeTagFiltersByType");
        Assertions.assertNotNull(activeTagFiltersByType);
        Assertions.assertTrue(activeTagFiltersByType.containsKey("owner"));
        Assertions.assertTrue(activeTagFiltersByType.get("owner").contains("alice"));
    }

    @Test
    public void testBatchHistoryTemplateRendersDynamicTagDropdownsAndTimespanInputs()
    {
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        final SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        final StandardLinkBuilder linkBuilder = new StandardLinkBuilder()
        {
            @Override
            protected String computeContextPath(
                final IExpressionContext context,
                final String exp,
                final Map<String, Object> format)
            {
                return "";
            }
        };
        engine.setLinkBuilder(linkBuilder);

        final BatchSummaryDto summary = new BatchSummaryDto("batch-1", "SmokeBatch", "Default", "", "Today", "100%", "", List.of(), List.of());
        final Context context = new Context();
        context.setVariable("batchName", "SmokeBatch");
        context.setVariable("groupBy", "none");
        context.setVariable("isGroupingPinned", false);
        context.setVariable("batchSummary", summary);
        context.setVariable("latestExecResult", new BatchLatestExecutionsDto(summary, List.of(), List.of(), List.of(), List.of(), List.of(), 0));
        context.setVariable("latestVariations", List.of());
        context.setVariable("contributingRuns", List.of());
        context.setVariable("backUrl", null);
        context.setVariable("batchEnvironment", "Default");
        context.setVariable("browserRatios", List.of());
        context.setVariable("localeRatios", List.of());
        context.setVariable("environmentRatios", List.of());
        context.setVariable("areaRatios", List.of());
        context.setVariable("tagRatios", List.of());
        context.setVariable("runs", List.of());
        context.setVariable("totalRunsCount", 1);
        context.setVariable("eligibleRunsCount", 1);
        context.setVariable("currentLimit", null);
        context.setVariable("showingRunsCount", 1);
        context.setVariable("batchLocales", List.of());
        context.setVariable("batchBrowsers", List.of());
        context.setVariable("areaTrends", List.of());
        context.setVariable("batchTestClasses", List.of());
        context.setVariable("totalBatchVariationsCount", 0);

        final RunTagTypeFilterDto ownerFilter = new RunTagTypeFilterDto(
            "owner",
            List.of("alice", "bob"),
            Set.of("alice")
        );
        context.setVariable("tagTypeFilters", List.of(ownerFilter));
        context.setVariable("startDate", "2026-01-01");
        context.setVariable("endDate", "2026-01-31");
        context.setVariable("dateTimespanLabel", "2026-01-01 to 2026-01-31");
        context.setVariable("hasActiveRunFilters", true);

        final String html = engine.process("fragments/batch-history", Collections.singleton("batchHistory"), context);
        Assertions.assertNotNull(html);

        Assertions.assertTrue(html.contains("id=\"pageBatchHistoryOverview\""), "Should render pageBatchHistoryOverview");
        Assertions.assertTrue(html.contains("id=\"batchRunsHistoryTableBody\""), "Should render runs table body");
    }

    @Test
    public void testBatchOverviewRecentRunsTableRendersTagsColumnAndMultilineChips()
    {
        final SpringTemplateEngine engine = new SpringTemplateEngine();
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        engine.setTemplateResolver(resolver);

        final StandardLinkBuilder linkBuilder = new StandardLinkBuilder()
        {
            @Override
            protected String computeContextPath(
                final IExpressionContext expressionContext,
                final String base,
                final Map<String, Object> parameters)
            {
                return "";
            }
        };
        engine.setLinkBuilder(linkBuilder);

        final TestRunEntity runWithTags = new TestRunEntity();
        runWithTags.setId("run_stg_emea_20261021_092700");
        runWithTags.setBatchName("stg-emea");
        runWithTags.setEnvironment("STG");
        runWithTags.setTriggerSource("Scheduled");
        runWithTags.setLocalesCsv("DE,FR,JP,KR,PT,SE,UK,US");
        runWithTags.setBrowsersCsv("Chrome_headless,Chrome_iPhoneX,Firefox_1920x1080,Safari_1920x1080");
        runWithTags.setTotalTests(103);
        runWithTags.setPassedCount(83);
        runWithTags.setTags(List.of(
            new TestRunTagEntity(runWithTags.getId(), "Suite", "stg-emea"),
            new TestRunTagEntity(runWithTags.getId(), "Branch", "main"),
            new TestRunTagEntity(runWithTags.getId(), "Release", "2026.10"),
            new TestRunTagEntity(runWithTags.getId(), "Build", "build-1039")
        ));

        final TestRunEntity fallbackRun = new TestRunEntity();
        fallbackRun.setId("run_fallback");
        fallbackRun.setBatchName("custom-dev");
        fallbackRun.setEnvironment("DEV");
        fallbackRun.setLocalesCsv("Default");
        fallbackRun.setBrowsersCsv("Chrome");
        fallbackRun.setTotalTests(10);
        fallbackRun.setPassedCount(10);

        final Context context = new Context(Locale.US);
        context.setVariable("batchName", "all");
        context.setVariable("groupBy", "batch");
        context.setVariable("groupLabel", "Batches");
        context.setVariable("batches", List.of());
        context.setVariable("runs", List.of(runWithTags, fallbackRun));
        context.setVariable("totalRunsCount", 2);
        context.setVariable("showingRunsCount", 2);
        context.setVariable("filterEnvs", List.of("STG", "DEV"));
        context.setVariable("filterLocales", List.of("DE", "FR"));
        context.setVariable("filterBrowsers", List.of("Chrome_headless", "Chrome"));
        context.setVariable("filterAreas", List.of());
        context.setVariable("areaTrends", List.of());
        context.setVariable("batchTestClasses", List.of());
        context.setVariable("totalBatchVariationsCount", 0);
        context.setVariable("tagRatioCards", List.of());
        context.setVariable("browserRatios", List.of());
        context.setVariable("localeRatios", List.of());
        context.setVariable("areaRatios", List.of());

        final String html = engine.process("fragments/batch-history", Collections.singleton("batchHistory"), context);
        Assertions.assertNotNull(html);

        // Header renamed to Tags
        Assertions.assertTrue(html.contains("<th>Tags</th>"), "Table header should be 'Tags'");
        Assertions.assertFalse(html.contains("<th>Batch Suite</th>"), "Table header 'Batch Suite' should no longer exist");

        // Tags column content
        Assertions.assertTrue(html.contains("Suite:</span> <span"), "Should render Suite tag label");
        Assertions.assertTrue(html.contains("stg-emea"), "Should render batch name value");
        Assertions.assertTrue(html.contains("Branch:"), "Should render Branch tag key");
        Assertions.assertTrue(html.contains("main"), "Should render Branch tag value");
        Assertions.assertTrue(html.contains("Release:"), "Should render Release tag key");
        Assertions.assertTrue(html.contains("2026.10"), "Should render Release tag value");
        Assertions.assertTrue(html.contains("Build:"), "Should render Build tag key");
        Assertions.assertTrue(html.contains("build-1039"), "Should render Build tag value");

        // Multiline chips for locales & browsers
        Assertions.assertTrue(html.contains("cell-locales"), "Should render cell-locales wrapper");
        Assertions.assertTrue(html.contains("tag-chip tag-locale"), "Should render individual locale chips");
        Assertions.assertTrue(html.contains(">DE</span>"), "Should render DE locale chip");
        Assertions.assertTrue(html.contains(">US</span>"), "Should render US locale chip");

        Assertions.assertTrue(html.contains("cell-browsers"), "Should render cell-browsers wrapper");
        Assertions.assertTrue(html.contains("tag-chip tag-browser"), "Should render individual browser chips");
        Assertions.assertTrue(html.contains(">Chrome_headless</span>"), "Should render Chrome_headless chip");
        Assertions.assertTrue(html.contains(">Firefox_1920x1080</span>"), "Should render Firefox_1920x1080 chip");

        // Fallback run renders Suite tag chip
        Assertions.assertTrue(html.contains("custom-dev"), "Fallback run should render custom-dev in tags cell");
    }

    @Test
    public void testBatchHistoryAndBatchSpecificRunsTableRenderTagsColumn()
    {
        final SpringTemplateEngine engine = new SpringTemplateEngine();
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        engine.setTemplateResolver(resolver);

        final StandardLinkBuilder linkBuilder = new StandardLinkBuilder()
        {
            @Override
            protected String computeContextPath(
                final IExpressionContext expressionContext,
                final String base,
                final Map<String, Object> parameters)
            {
                return "";
            }
        };
        engine.setLinkBuilder(linkBuilder);

        final TestRunEntity runWithTags = new TestRunEntity();
        runWithTags.setId("run_stg_emea_20261021_092700");
        runWithTags.setBatchName("stg-emea");
        runWithTags.setEnvironment("STG");
        runWithTags.setTriggerSource("Scheduled");
        runWithTags.setLocalesCsv("DE,FR,JP,KR,PT,SE,UK,US");
        runWithTags.setBrowsersCsv("Chrome_headless,Safari_1920x1080");
        runWithTags.setTotalTests(103);
        runWithTags.setPassedCount(83);
        runWithTags.setTags(List.of(
            new TestRunTagEntity(runWithTags.getId(), "Suite", "stg-emea"),
            new TestRunTagEntity(runWithTags.getId(), "Branch", "main"),
            new TestRunTagEntity(runWithTags.getId(), "Release", "2026.10")
        ));

        final Context context = new Context(Locale.US);
        context.setVariable("batchName", "stg-emea");
        context.setVariable("runs", List.of(runWithTags));
        context.setVariable("totalRunsCount", 1);
        context.setVariable("filterEnvs", List.of("STG"));
        context.setVariable("filterLocales", List.of("DE"));
        context.setVariable("filterBrowsers", List.of("Chrome_headless"));

        final String historyHtml = engine.process("fragments/batch-history", Collections.singleton("batchHistory"), context);
        Assertions.assertNotNull(historyHtml);

        // Header has Tags
        Assertions.assertTrue(historyHtml.contains("<th>Tags</th>"), "Batch history runs table must contain '<th>Tags</th>'");
        Assertions.assertTrue(historyHtml.contains("Suite:</span> <span"), "Batch history runs table must render Suite tag");
        Assertions.assertTrue(historyHtml.contains("stg-emea"), "Batch history runs table must render batch suite value");
        Assertions.assertTrue(historyHtml.contains("Branch:"), "Batch history runs table must render Branch tag key");
        Assertions.assertTrue(historyHtml.contains("col-action"), "Batch history runs table must render col-action class");
        Assertions.assertTrue(historyHtml.contains("col-status-breakdown"), "Batch history runs table must render col-status-breakdown class");
        Assertions.assertTrue(historyHtml.contains("col-pass-rate"), "Batch history runs table must render col-pass-rate class");
        Assertions.assertFalse(historyHtml.contains("<th>Environment</th>"), "Batch history runs table must NOT contain standalone Environment column");

        Assertions.assertTrue(historyHtml.contains("<tbody id=\"batchRunsHistoryTableBody\">"), "History must render batchRunsHistoryTableBody");
    }

    @Test
    public void testPopulateRunTagsInjectsEnvironmentTag()
    {
        final TestRunRepository runRepo = Mockito.mock(TestRunRepository.class);
        final TestBatchRepository batchRepo = Mockito.mock(TestBatchRepository.class);
        final TestBaseVariationRepository varRepo = Mockito.mock(TestBaseVariationRepository.class);
        final TestBaseBugRepository bugRepo = Mockito.mock(TestBaseBugRepository.class);
        final TestRunTagRepository tagRepo = Mockito.mock(TestRunTagRepository.class);

        final AuraReportDataService svc = new AuraReportDataService(
            runRepo,
            batchRepo,
            varRepo,
            bugRepo,
            tagRepo,
            null,
            null,
            null
        );

        final TestRunEntity run = new TestRunEntity();
        run.setId("run-env-123");
        run.setBatchName("SmokeSuite");
        run.setEnvironment("STG");

        Mockito.when(tagRepo.findByRunIdIn(List.of("run-env-123"))).thenReturn(List.of(
            new TestRunTagEntity("run-env-123", "Branch", "develop")
        ));

        svc.populateRunTags(List.of(run));

        Assertions.assertNotNull(run.getTags());
        Assertions.assertTrue(run.getTags().stream().anyMatch(t -> "Suite".equalsIgnoreCase(t.getTagKey()) && "SmokeSuite".equals(t.getTagValue())));
        Assertions.assertTrue(run.getTags().stream().anyMatch(t -> "Environment".equalsIgnoreCase(t.getTagKey()) && "STG".equals(t.getTagValue())));
        Assertions.assertTrue(run.getTags().stream().anyMatch(t -> "Branch".equalsIgnoreCase(t.getTagKey()) && "develop".equals(t.getTagValue())));
    }

    @Test
    public void testBuildGroupingTagOptionsFromDatabase()
    {
        Mockito.when(dataService.getAllTagKeys()).thenReturn(List.of("Release", "Team", "browser", "environment"));

        final List<GroupingTagOptionDto> options = controller.buildGroupingTagOptions(List.of("Release"));

        Assertions.assertEquals(3, options.size(), "Core built-in keywords like 'browser' must be excluded, while dynamic tags including 'environment' must be included");

        final GroupingTagOptionDto releaseOpt = options.get(0);
        Assertions.assertEquals("Release", releaseOpt.getTagKey());
        Assertions.assertEquals("Tag (Release)", releaseOpt.getDisplayName());
        Assertions.assertTrue(releaseOpt.isActive());
        Assertions.assertEquals("/?groupBy=none", releaseOpt.getToggleUrl(), "Toggling off the only active dimension resets to none");

        final GroupingTagOptionDto teamOpt = options.get(1);
        Assertions.assertEquals("Team", teamOpt.getTagKey());
        Assertions.assertEquals("Tag (Team)", teamOpt.getDisplayName());
        Assertions.assertFalse(teamOpt.isActive());
        Assertions.assertEquals("/?groupBy=Release,Team", teamOpt.getToggleUrl());

        final GroupingTagOptionDto envOpt = options.get(2);
        Assertions.assertEquals("environment", envOpt.getTagKey());
        Assertions.assertEquals("Tag (environment)", envOpt.getDisplayName());
        Assertions.assertFalse(envOpt.isActive());

        // Batch and browser should NOT be present
        Assertions.assertTrue(options.stream().noneMatch(o -> "browser".equalsIgnoreCase(o.getTagKey())));
        Assertions.assertTrue(options.stream().noneMatch(o -> "Batch".equalsIgnoreCase(o.getTagKey())));
    }

    @Test
    public void testBatchOverviewDynamicTagGrouping()
    {
        Mockito.when(dataService.getAllTagKeys()).thenReturn(List.of("Release"));
        final BatchOverviewDataDto overviewData = new BatchOverviewDataDto(
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );
        Mockito.when(dataService.getGroupOverviewData("Release")).thenReturn(overviewData);

        final Model model = new ConcurrentModel();
        final String view = controller.batchOverview("Release", "false", model);

        Assertions.assertEquals("index", view);
        Assertions.assertEquals("Directory", model.getAttribute("activeTab"));
        Assertions.assertEquals("Release", model.getAttribute("groupBy"));
        Assertions.assertEquals("Release Tags", model.getAttribute("groupLabel"));

        @SuppressWarnings("unchecked")
        final List<GroupingTagOptionDto> groupingTags = (List<GroupingTagOptionDto>) model.getAttribute("groupingTags");
        Assertions.assertNotNull(groupingTags);
        Assertions.assertEquals(1, groupingTags.size());
        Assertions.assertEquals("Release", groupingTags.get(0).getTagKey());
        Assertions.assertTrue(groupingTags.get(0).isActive());
    }

    @Test
    public void testBatchHistoryDynamicTagGroupingScoping()
    {
        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-rel-1");
        r1.setBatchName("Smoke");
        r1.setEnvironment("Default");
        r1.setBrowsersCsv("Chrome");
        r1.setLocalesCsv("Default");
        r1.setStartTimeMs(1000L);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-rel-2");
        r2.setBatchName("Regression");
        r2.setEnvironment("Default");
        r2.setBrowsersCsv("Chrome");
        r2.setLocalesCsv("Default");
        r2.setStartTimeMs(2000L);

        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of(r2, r1));
        Mockito.when(dataService.getAllTagKeys()).thenReturn(List.of("Release"));
        Mockito.when(dataService.getRunTagValues(r1, "Release")).thenReturn(List.of("v1.0"));
        Mockito.when(dataService.getRunTagValues(r2, "Release")).thenReturn(List.of("v2.0"));

        final Model model = new ConcurrentModel();
        final String view = controller.batchHistory(null, "Release", "v1.0", null, null, null, "false", model);

        Assertions.assertEquals("index", view);
        Assertions.assertEquals("v1.0", model.getAttribute("scopeTitle"));
        Assertions.assertEquals("/?groupBy=Release", model.getAttribute("backUrl"));

        @SuppressWarnings("unchecked")
        final List<TestRunEntity> runs = (List<TestRunEntity>) model.getAttribute("runs");
        Assertions.assertNotNull(runs);
        Assertions.assertEquals(1, runs.size());
        Assertions.assertEquals("run-rel-1", runs.get(0).getId());
    }

    @Test
    public void testBatchHistoryBooleanTagScoping()
    {
        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-custom-true");
        r1.setBatchName("Smoke");
        r1.setEnvironment("Default");
        r1.setBrowsersCsv("Chrome");
        r1.setLocalesCsv("Default");
        r1.setStartTimeMs(1000L);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-custom-untagged");
        r2.setBatchName("Regression");
        r2.setEnvironment("Default");
        r2.setBrowsersCsv("Chrome");
        r2.setLocalesCsv("Default");
        r2.setStartTimeMs(2000L);

        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of(r2, r1));
        Mockito.when(dataService.getAllTagKeys()).thenReturn(List.of("Custom"));
        Mockito.when(dataService.isBooleanTag(Mockito.eq("Custom"), Mockito.anyCollection())).thenReturn(true);
        Mockito.when(dataService.getRunTagValues(r1, "Custom")).thenReturn(List.of("true"));
        Mockito.when(dataService.getRunTagValues(r2, "Custom")).thenReturn(Collections.emptyList());

        final Model modelNotCustom = new ConcurrentModel();
        final String viewNotCustom = controller.batchHistory(null, "Custom", "Not Custom", null, null, null, "false", modelNotCustom);

        Assertions.assertEquals("index", viewNotCustom);
        Assertions.assertEquals("Not Custom", modelNotCustom.getAttribute("scopeTitle"));
        Assertions.assertEquals("/?groupBy=Custom", modelNotCustom.getAttribute("backUrl"));

        @SuppressWarnings("unchecked")
        final List<TestRunEntity> notCustomRuns = (List<TestRunEntity>) modelNotCustom.getAttribute("runs");
        Assertions.assertNotNull(notCustomRuns);
        Assertions.assertEquals(1, notCustomRuns.size());
        Assertions.assertEquals("run-custom-untagged", notCustomRuns.get(0).getId());

        final Model modelCustom = new ConcurrentModel();
        final String viewCustom = controller.batchHistory(null, "Custom", "Custom", null, null, null, "false", modelCustom);

        Assertions.assertEquals("index", viewCustom);
        Assertions.assertEquals("Custom", modelCustom.getAttribute("scopeTitle"));
        Assertions.assertEquals("/?groupBy=Custom", modelCustom.getAttribute("backUrl"));

        @SuppressWarnings("unchecked")
        final List<TestRunEntity> customRuns = (List<TestRunEntity>) modelCustom.getAttribute("runs");
        Assertions.assertNotNull(customRuns);
        Assertions.assertEquals(1, customRuns.size());
        Assertions.assertEquals("run-custom-true", customRuns.get(0).getId());
    }

    @Test
    public void testBatchOverviewTemplateRendersDynamicTagsAndOmitsBatchWhenAbsent()
    {
        final SpringTemplateEngine engine = new SpringTemplateEngine();
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        engine.setTemplateResolver(resolver);

        final StandardLinkBuilder linkBuilder = new StandardLinkBuilder()
        {
            @Override
            protected String computeContextPath(
                final IExpressionContext expressionContext,
                final String base,
                final Map<String, Object> parameters)
            {
                return "";
            }
        };
        engine.setLinkBuilder(linkBuilder);

        final Context context = new Context(Locale.US);
        context.setVariable("batchName", "all");
        context.setVariable("groupBy", "Release");
        context.setVariable("groupLabel", "Release Tags");
        context.setVariable("isGroupingPinned", false);
        context.setVariable("isEnvActive", false);
        context.setVariable("isLocaleActive", false);
        context.setVariable("isBrowserActive", false);
        context.setVariable("isAreaActive", false);
        context.setVariable("batches", List.of());
        context.setVariable("runs", List.of());
        context.setVariable("totalRunsCount", 0);
        context.setVariable("showingRunsCount", 0);
        context.setVariable("filterEnvs", List.of());
        context.setVariable("filterLocales", List.of());
        context.setVariable("filterBrowsers", List.of());
        context.setVariable("filterAreas", List.of());
        context.setVariable("areaTrends", List.of());
        context.setVariable("batchTestClasses", List.of());
        context.setVariable("totalBatchVariationsCount", 0);
        context.setVariable("tagRatioCards", List.of());
        context.setVariable("browserRatios", List.of());
        context.setVariable("localeRatios", List.of());
        context.setVariable("areaRatios", List.of());
        context.setVariable("groupingTags", List.of(
            new GroupingTagOptionDto("Release", "Tag (Release)", "/?groupBy=none", true)
        ));

        final String html = engine.process("fragments/batch-history", Collections.singleton("batchHistory"), context);
        Assertions.assertNotNull(html);
        Assertions.assertTrue(html.contains("id=\"pageBatchHistoryOverview\""), "Container should be rendered");
        Assertions.assertTrue(html.contains("id=\"batchRunsHistoryTableBody\""), "Runs table body should be rendered");
    }

    @Test
    public void testBatchHistoryRunScopeExclusionAndInclusionDisplay()
    {
        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-101");
        r1.setBatchName("SmokeBatch");
        r1.setEnvironment("Staging");
        r1.setLocalesCsv("US,DE");
        r1.setBrowsersCsv("Chrome,Safari");
        r1.setPassedCount(10);
        r1.setTotalTests(10);

        final List<TestRunEntity> allRuns = List.of(r1);
        Mockito.when(runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc("SmokeBatch")).thenReturn(allRuns);
        Mockito.when(batchRepository.findById("SmokeBatch")).thenReturn(Optional.empty());

        final BatchSummaryDto summary = new BatchSummaryDto("batch-SmokeBatch", "SmokeBatch", "Default", "", "Today", "100%", "", List.of(), List.of());
        final BatchLatestExecutionsDto mockResult = new BatchLatestExecutionsDto(
            summary,
            List.of(r1),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            1,
            1
        );

        Mockito.when(dataService.getBatchLatestTestExecutionsSummary(
            Mockito.eq("SmokeBatch"),
            Mockito.eq(allRuns),
            Mockito.anySet(),
            Mockito.isNull(),
            Mockito.isNull(),
            Mockito.anySet(),
            Mockito.argThat(set -> set != null && set.contains("Safari")),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anyMap(),
            Mockito.anyMap(),
            Mockito.anyMap()
        )).thenReturn(mockResult);

        Mockito.when(dataService.getBatchTestBaseClasses(
            Mockito.eq("SmokeBatch"),
            Mockito.anyList(),
            Mockito.anySet(),
            Mockito.argThat(set -> set != null && set.contains("Safari")),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet()
        )).thenReturn(List.of());

        Mockito.when(dataService.groupBatchTestClassesByArea(Mockito.anyList())).thenReturn(List.of());

        final Model model = new ConcurrentModel();
        final String view = controller.batchHistory(
            "SmokeBatch",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "Safari",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "true",
            model,
            Map.of("excludeRunBrowsers", "Safari")
        );

        Assertions.assertEquals("fragments/batch-history :: batchHistory", view);
        Assertions.assertEquals("Safari", model.getAttribute("excludeRunBrowsers"));
        @SuppressWarnings("unchecked")
        final Set<String> excludedBrowsers = (Set<String>) model.getAttribute("excludedRunBrowsers");
        Assertions.assertNotNull(excludedBrowsers);
        Assertions.assertTrue(excludedBrowsers.contains("Safari"));
        Assertions.assertEquals(1, model.getAttribute("activeRunFiltersCount"));
        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("hasActiveRunFilters"));

        final SpringTemplateEngine engine = new SpringTemplateEngine();
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        engine.setTemplateResolver(resolver);

        final StandardLinkBuilder linkBuilder = new StandardLinkBuilder()
        {
            @Override
            protected String computeContextPath(
                final IExpressionContext expressionContext,
                final String base,
                final Map<String, Object> parameters)
            {
                return "";
            }
        };
        engine.setLinkBuilder(linkBuilder);

        final Context exclusionContext = new Context(Locale.US);
        model.asMap().forEach(exclusionContext::setVariable);

        final String htmlExclusion = engine.process("fragments/batch-history", Collections.singleton("batchHistory"), exclusionContext);
        Assertions.assertNotNull(htmlExclusion);
        Assertions.assertTrue(htmlExclusion.contains("id=\"pageBatchHistoryOverview\""), "Container should be rendered");
        Assertions.assertTrue(htmlExclusion.contains("id=\"batchRunsHistoryTableBody\""), "Runs table body should be rendered");

        final Context inclusionContext = new Context(Locale.US);
        model.asMap().forEach(inclusionContext::setVariable);
        inclusionContext.setVariable("excludeRunBrowsers", "");
        inclusionContext.setVariable("excludedRunBrowsers", Set.of());
        inclusionContext.setVariable("runBrowsers", "Chrome");
        inclusionContext.setVariable("selectedRunBrowsers", Set.of("Chrome"));
        inclusionContext.setVariable("hasActiveRunFilters", true);
        inclusionContext.setVariable("activeRunFiltersCount", 1);

        final String htmlInclusion = engine.process("fragments/batch-history", Collections.singleton("batchHistory"), inclusionContext);
        Assertions.assertNotNull(htmlInclusion);
        Assertions.assertTrue(htmlInclusion.contains("id=\"pageBatchHistoryOverview\""), "Container should be rendered");
        Assertions.assertTrue(htmlInclusion.contains("id=\"batchRunsHistoryTableBody\""), "Runs table body should be rendered");
    }

    @Test
    public void testConsideredRunsFilterOmitsLegacyTagsWhenNoDynamicTags()
    {
        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-no-dyn-1");
        r1.setBatchName("SmokeBatch");
        r1.setEnvironment("Default");
        r1.setBrowsersCsv("Chrome");
        r1.setLocalesCsv("Default");
        r1.setStartTimeMs(1000L);

        final List<TestRunEntity> allRuns = List.of(r1);
        Mockito.when(runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc("SmokeBatch")).thenReturn(allRuns);
        Mockito.when(batchRepository.findById("SmokeBatch")).thenReturn(Optional.empty());
        Mockito.when(dataService.getAllTagKeys()).thenReturn(List.of());
        Mockito.when(dataService.getCandidateTagValues(Mockito.anyCollection())).thenReturn(Collections.emptyMap());

        final BatchSummaryDto summary = new BatchSummaryDto("batch-SmokeBatch", "SmokeBatch", "Default", "", "Today", "100%", "", List.of(), List.of());
        final BatchLatestExecutionsDto mockResult = new BatchLatestExecutionsDto(
            summary,
            List.of(r1),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            1,
            1,
            List.of(r1)
        );

        Mockito.when(dataService.getBatchLatestTestExecutionsSummary(
            Mockito.eq("SmokeBatch"),
            Mockito.eq(allRuns),
            Mockito.anySet(),
            Mockito.isNull(),
            Mockito.isNull(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anyMap(),
            Mockito.anyMap()
        )).thenReturn(mockResult);

        final Model model = new ConcurrentModel();
        final String view = controller.batchHistory(
            "SmokeBatch",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "true",
            model,
            Collections.emptyMap()
        );

        Assertions.assertEquals("fragments/batch-history :: batchHistory", view);

        @SuppressWarnings("unchecked")
        final List<RunTagTypeFilterDto> tagTypeFilters = (List<RunTagTypeFilterDto>) model.getAttribute("tagTypeFilters");
        Assertions.assertNotNull(tagTypeFilters);
        Assertions.assertTrue(tagTypeFilters.isEmpty(), "When no dynamic tags exist, tagTypeFilters must be empty");

        final SpringTemplateEngine engine = new SpringTemplateEngine();
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        engine.setTemplateResolver(resolver);

        final StandardLinkBuilder linkBuilder = new StandardLinkBuilder()
        {
            @Override
            protected String computeContextPath(
                final IExpressionContext expressionContext,
                final String base,
                final Map<String, Object> parameters)
            {
                return "";
            }
        };
        engine.setLinkBuilder(linkBuilder);

        final Context context = new Context(Locale.US);
        model.asMap().forEach(context::setVariable);

        final String html = engine.process("fragments/batch-history", Collections.singleton("batchHistory"), context);
        Assertions.assertNotNull(html);
        Assertions.assertTrue(html.contains("id=\"pageBatchHistoryOverview\""), "Container should be rendered");
        Assertions.assertTrue(html.contains("id=\"batchRunsHistoryTableBody\""), "Runs table body should be rendered");
    }

    @Test
    public void testConsideredRunsFilterRendersOnlyDynamicTags()
    {
        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-dyn-1");
        r1.setBatchName("SmokeBatch");
        r1.setEnvironment("Default");
        r1.setBrowsersCsv("Chrome");
        r1.setLocalesCsv("Default");
        r1.setStartTimeMs(1000L);

        final List<TestRunEntity> allRuns = List.of(r1);
        Mockito.when(runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc("SmokeBatch")).thenReturn(allRuns);
        Mockito.when(batchRepository.findById("SmokeBatch")).thenReturn(Optional.empty());
        Mockito.when(dataService.getAllTagKeys()).thenReturn(List.of("Release", "Owner"));
        Mockito.when(dataService.getCandidateTagValues(Mockito.anyCollection())).thenReturn(Map.of(
            "Release", List.of("2026.10", "2026.11"),
            "Owner", List.of("alice", "bob")
        ));

        final BatchSummaryDto summary = new BatchSummaryDto("batch-SmokeBatch", "SmokeBatch", "Default", "", "Today", "100%", "", List.of(), List.of());
        final BatchLatestExecutionsDto mockResult = new BatchLatestExecutionsDto(
            summary,
            List.of(r1),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            1,
            1,
            List.of(r1)
        );

        Mockito.when(dataService.getBatchLatestTestExecutionsSummary(
            Mockito.eq("SmokeBatch"),
            Mockito.eq(allRuns),
            Mockito.anySet(),
            Mockito.isNull(),
            Mockito.isNull(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anyMap(),
            Mockito.anyMap()
        )).thenReturn(mockResult);

        final Model model = new ConcurrentModel();
        final String view = controller.batchHistory(
            "SmokeBatch",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "true",
            model,
            Collections.emptyMap()
        );

        Assertions.assertEquals("fragments/batch-history :: batchHistory", view);

        @SuppressWarnings("unchecked")
        final List<RunTagTypeFilterDto> tagTypeFilters = (List<RunTagTypeFilterDto>) model.getAttribute("tagTypeFilters");
        Assertions.assertNotNull(tagTypeFilters);
        Assertions.assertEquals(2, tagTypeFilters.size());
        Assertions.assertTrue(tagTypeFilters.stream().anyMatch(f -> "Release".equalsIgnoreCase(f.getTagKey())));
        Assertions.assertTrue(tagTypeFilters.stream().anyMatch(f -> "Owner".equalsIgnoreCase(f.getTagKey())));
        Assertions.assertFalse(tagTypeFilters.stream().anyMatch(f -> "batch".equalsIgnoreCase(f.getTagKey())));
        Assertions.assertFalse(tagTypeFilters.stream().anyMatch(f -> "Tags".equals(f.getDisplayName())));

        final SpringTemplateEngine engine = new SpringTemplateEngine();
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        engine.setTemplateResolver(resolver);

        final StandardLinkBuilder linkBuilder = new StandardLinkBuilder()
        {
            @Override
            protected String computeContextPath(
                final IExpressionContext expressionContext,
                final String base,
                final Map<String, Object> parameters)
            {
                return "";
            }
        };
        engine.setLinkBuilder(linkBuilder);

        final Context context = new Context(Locale.US);
        model.asMap().forEach(context::setVariable);

        final String html = engine.process("fragments/batch-history", Collections.singleton("batchHistory"), context);
        Assertions.assertNotNull(html);
        Assertions.assertTrue(html.contains("id=\"pageBatchHistoryOverview\""), "Container should be rendered");
        Assertions.assertTrue(html.contains("id=\"batchRunsHistoryTableBody\""), "Runs table body should be rendered");
    }

    @Test
    public void testToggleExecutionIdentityDimensionEndpointCallsService()
    {
        final TestRunEntity r1 = new TestRunEntity("r1", "SmokeBatch", "COMPLETED", "Main", "DEV", "Java", "Chrome", "10:00", 1000L);
        Mockito.when(runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc("SmokeBatch")).thenReturn(List.of(r1));
        Mockito.when(settingService.getExecutionIdentityDimensions()).thenReturn(List.of("browser", "locale", "environment"));
        Mockito.when(settingService.isExecutionIdentityDimensionActive("environment")).thenReturn(true);
        Mockito.when(settingService.isExecutionIdentityDimensionActive("browser")).thenReturn(true);
        Mockito.when(settingService.isExecutionIdentityDimensionActive("locale")).thenReturn(true);

        final Model model = new ConcurrentModel();
        final String view = controller.toggleExecutionIdentityDimension(
            "environment",
            "true",
            "http://localhost:8080/batch-history?batchName=SmokeBatch",
            null,
            model
        );

        Mockito.verify(settingService).toggleExecutionIdentityDimension("environment");
        Assertions.assertEquals("fragments/batch-history :: batchHistory", view);
        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("isEnvIdentity"));
        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("isEnvUnique"));
    }

    @Test
    public void testToggleUniquenessEndpointCallsService()
    {
        final TestRunEntity r1 = new TestRunEntity("r1", "SmokeBatch", "COMPLETED", "Main", "DEV", "Java", "Chrome", "10:00", 1000L);
        Mockito.when(runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc("SmokeBatch")).thenReturn(List.of(r1));
        Mockito.when(settingService.getExecutionIdentityDimensions()).thenReturn(List.of("browser", "locale", "environment"));
        Mockito.when(settingService.isExecutionIdentityDimensionActive("environment")).thenReturn(true);
        Mockito.when(settingService.isExecutionIdentityDimensionActive("browser")).thenReturn(true);
        Mockito.when(settingService.isExecutionIdentityDimensionActive("locale")).thenReturn(true);

        final Model model = new ConcurrentModel();
        final String view = controller.toggleUniquenessDimension(
            "environment",
            "true",
            "http://localhost:8080/batch-history?batchName=SmokeBatch",
            null,
            model
        );

        Mockito.verify(settingService).toggleExecutionIdentityDimension("environment");
        Assertions.assertEquals("fragments/batch-history :: batchHistory", view);
        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("isEnvIdentity"));
        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("isEnvUnique"));
    }

    @Test
    public void testBatchHistoryExposesUniquenessModelAttributesAndRendersTemplate()
    {
        final TestRunEntity r1 = new TestRunEntity("r1", "SmokeBatch", "COMPLETED", "Main", "DEV", "Java", "Chrome", "10:00", 1000L);
        r1.setTags(List.of(new TestRunTagEntity("r1", "Release", "21")));
        Mockito.when(runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc("SmokeBatch")).thenReturn(List.of(r1));
        Mockito.when(settingService.getExecutionIdentityDimensions()).thenReturn(List.of("browser", "locale", "environment", "Release"));
        Mockito.when(settingService.isExecutionIdentityDimensionActive("environment")).thenReturn(true);
        Mockito.when(settingService.isExecutionIdentityDimensionActive("browser")).thenReturn(true);
        Mockito.when(settingService.isExecutionIdentityDimensionActive("locale")).thenReturn(true);

        final BatchSummaryDto summary = new BatchSummaryDto("batch-SmokeBatch", "SmokeBatch", "Default", "", "Today", "100%", "", List.of(), List.of());
        final BatchLatestExecutionsDto latestExecResult = new BatchLatestExecutionsDto(
            summary,
            List.of(r1),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            1,
            1
        );
        Mockito.when(dataService.getBatchLatestTestExecutionsSummary(
            Mockito.eq("SmokeBatch"),
            Mockito.anyList(),
            Mockito.anySet(),
            Mockito.any(),
            Mockito.any(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anyMap(),
            Mockito.anyMap(),
            Mockito.anyMap(),
            Mockito.eq(List.of("browser", "locale", "environment", "Release"))
        )).thenReturn(latestExecResult);

        final Model model = new ConcurrentModel();
        final String view = controller.batchHistory("SmokeBatch", null, "true", model);

        Assertions.assertEquals("fragments/batch-history :: batchHistory", view);
        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("isEnvUnique"));
        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("isBrowserUnique"));
        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("isLocaleUnique"));
        @SuppressWarnings("unchecked")
        final List<String> availableTags = (List<String>) model.getAttribute("availableUniquenessTags");
        Assertions.assertNotNull(availableTags);
        Assertions.assertTrue(availableTags.stream().anyMatch(t -> "Release".equalsIgnoreCase(t)));

        // Verify Thymeleaf template rendering with uniqueness bar
        final SpringTemplateEngine engine = new SpringTemplateEngine();
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        engine.setTemplateResolver(resolver);

        final StandardLinkBuilder linkBuilder = new StandardLinkBuilder()
        {
            @Override
            protected String computeContextPath(
                final IExpressionContext expressionContext,
                final String base,
                final Map<String, Object> parameters)
            {
                return "";
            }
        };
        engine.setLinkBuilder(linkBuilder);

        final Context context = new Context(Locale.US);
        model.asMap().forEach(context::setVariable);

        final String html = engine.process("fragments/batch-history", Collections.singleton("batchHistory"), context);
        Assertions.assertNotNull(html);
        Assertions.assertTrue(html.contains("id=\"pageBatchHistoryOverview\""), "Container should be rendered");
        Assertions.assertTrue(html.contains("id=\"batchRunsHistoryTableBody\""), "Runs table body should be rendered");
    }

    @Test
    public void testToggleVariationsApplyIdentityEndpoint()
    {
        final TestRunEntity r1 = new TestRunEntity("r1", "SmokeBatch", "COMPLETED", "Main", "DEV", "Java", "Chrome", "10:00", 1000L);
        Mockito.when(runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc("SmokeBatch")).thenReturn(List.of(r1));
        Mockito.when(settingService.isVariationsApplyIdentity()).thenReturn(true);

        final Model model = new ConcurrentModel();
        final String view = controller.toggleVariationsApplyIdentity(
            null,
            "true",
            "http://localhost:8080/batch-history?batchName=SmokeBatch",
            null,
            model
        );

        Mockito.verify(settingService).toggleVariationsApplyIdentity();
        Assertions.assertEquals("fragments/batch-history :: batchHistory", view);
        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("applyIdentityToVariations"));
        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("applyUniquenessToVariations"));
    }

    @Test
    public void testToggleVariationsApplyUniquenessEndpoint()
    {
        final TestRunEntity r1 = new TestRunEntity("r1", "SmokeBatch", "COMPLETED", "Main", "DEV", "Java", "Chrome", "10:00", 1000L);
        Mockito.when(runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc("SmokeBatch")).thenReturn(List.of(r1));
        Mockito.when(settingService.isVariationsApplyIdentity()).thenReturn(true);

        final Model model = new ConcurrentModel();
        final String view = controller.toggleVariationsApplyUniqueness(
            null,
            "true",
            "http://localhost:8080/batch-history?batchName=SmokeBatch",
            null,
            model
        );

        Mockito.verify(settingService).toggleVariationsApplyIdentity();
        Assertions.assertEquals("fragments/batch-history :: batchHistory", view);
        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("applyIdentityToVariations"));
        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("applyUniquenessToVariations"));
    }

    @Test
    public void testBatchHistoryWithVariationsApplyUniquenessEnabled()
    {
        final TestRunEntity r1 = new TestRunEntity("r1", "SmokeBatch", "COMPLETED", "Main", "DEV", "Java", "Chrome", "10:00", 1000L);
        r1.setTags(List.of(new TestRunTagEntity("r1", "Release", "21")));
        Mockito.when(runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc("SmokeBatch")).thenReturn(List.of(r1));
        Mockito.when(settingService.getExecutionIdentityDimensions()).thenReturn(List.of("browser", "locale", "environment", "Release"));
        Mockito.when(settingService.getUniquenessDimensions()).thenReturn(List.of("browser", "locale", "environment", "Release"));
        Mockito.when(settingService.isExecutionIdentityDimensionActive("environment")).thenReturn(true);
        Mockito.when(settingService.isExecutionIdentityDimensionActive("browser")).thenReturn(true);
        Mockito.when(settingService.isExecutionIdentityDimensionActive("locale")).thenReturn(true);
        Mockito.when(settingService.isUniquenessDimensionActive("environment")).thenReturn(true);
        Mockito.when(settingService.isUniquenessDimensionActive("browser")).thenReturn(true);
        Mockito.when(settingService.isUniquenessDimensionActive("locale")).thenReturn(true);
        Mockito.when(settingService.isVariationsApplyIdentity()).thenReturn(true);
        Mockito.when(settingService.isVariationsApplyUniqueness()).thenReturn(true);

        final BatchSummaryDto summary = new BatchSummaryDto("batch-SmokeBatch", "SmokeBatch", "Default", "", "Today", "100%", "", List.of(), List.of());
        final BatchLatestExecutionsDto latestExecResult = new BatchLatestExecutionsDto(
            summary,
            List.of(r1),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            1,
            1
        );
        Mockito.when(dataService.getBatchLatestTestExecutionsSummary(
            Mockito.eq("SmokeBatch"),
            Mockito.anyList(),
            Mockito.anySet(),
            Mockito.any(),
            Mockito.any(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anyMap(),
            Mockito.anyMap(),
            Mockito.anyMap(),
            Mockito.eq(List.of("browser", "locale", "environment", "Release"))
        )).thenReturn(latestExecResult);

        final TestBaseVariationEntity var1 = new TestBaseVariationEntity("var-1", "LoginTest", "testLogin", "Default", "@General", "US", "Chrome");
        var1.setLastEnvironment("DEV");
        var1.setUniquenessProperties(Map.of("Environment", "DEV", "Release", "21"));
        final TestBaseClassDto classDto = new TestBaseClassDto("LoginTest", "batchClassContainerLoginTest", 1, List.of(var1));
        Mockito.when(dataService.getBatchTestBaseClasses(
            Mockito.eq("SmokeBatch"),
            Mockito.anyList(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.eq(List.of("browser", "locale", "environment", "Release"))
        )).thenReturn(List.of(classDto));

        final Model model = new ConcurrentModel();
        final String view = controller.batchHistory("SmokeBatch", null, "true", model);

        Assertions.assertEquals("fragments/batch-history :: batchHistory", view);
        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("applyIdentityToVariations"));
        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("applyUniquenessToVariations"));
        @SuppressWarnings("unchecked")
        final List<String> activeCustomTags = (List<String>) model.getAttribute("activeUniquenessCustomTags");
        Assertions.assertNotNull(activeCustomTags);
        Assertions.assertTrue(activeCustomTags.contains("Release"));

        // Verify Thymeleaf template rendering with dynamic table columns
        final SpringTemplateEngine engine = new SpringTemplateEngine();
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        engine.setTemplateResolver(resolver);

        final StandardLinkBuilder linkBuilder = new StandardLinkBuilder()
        {
            @Override
            protected String computeContextPath(
                final IExpressionContext expressionContext,
                final String base,
                final Map<String, Object> parameters)
            {
                return "";
            }
        };
        engine.setLinkBuilder(linkBuilder);

        final Context context = new Context(Locale.US);
        model.asMap().forEach(context::setVariable);

        final String html = engine.process("fragments/batch-history", Collections.singleton("batchHistory"), context);
        Assertions.assertNotNull(html);
        Assertions.assertTrue(html.contains("id=\"pageBatchHistoryOverview\""), "Container should be rendered");
        Assertions.assertTrue(html.contains("id=\"batchRunsHistoryTableBody\""), "Runs table body should be rendered");
    }

    @Test
    public void testBatchHistoryVariationsUseDefaultDimensionsWhenUniquenessCheckboxUnchecked()
    {
        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-dev");
        r1.setBatchName("SmokeBatch");
        r1.setEnvironment("DEV");
        r1.setLocalesCsv("US");
        r1.setBrowsersCsv("Chrome");

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-stg");
        r2.setBatchName("SmokeBatch");
        r2.setEnvironment("STG");
        r2.setLocalesCsv("US");
        r2.setBrowsersCsv("Chrome");

        final List<TestRunEntity> allRuns = List.of(r1, r2);
        Mockito.when(runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc("SmokeBatch")).thenReturn(allRuns);
        Mockito.when(batchRepository.findById("SmokeBatch")).thenReturn(Optional.empty());

        // Custom identity / uniqueness is active for charts (e.g. environment and Release)
        Mockito.when(settingService.getExecutionIdentityDimensions()).thenReturn(List.of("browser", "locale", "environment", "Release"));
        Mockito.when(settingService.getUniquenessDimensions()).thenReturn(List.of("browser", "locale", "environment", "Release"));
        // But variations identity / uniqueness checkbox is UNCHECKED
        Mockito.when(settingService.isVariationsApplyIdentity()).thenReturn(false);
        Mockito.when(settingService.isVariationsApplyUniqueness()).thenReturn(false);

        final BatchSummaryDto summary = new BatchSummaryDto("batch-SmokeBatch", "SmokeBatch", "DEV", "", "Recently", "100%", "", List.of("US"), List.of("Chrome"), "run-stg", 2, 0, 0, 0, 0, 2);
        final BatchLatestExecutionsDto latestExecResult = new BatchLatestExecutionsDto(summary, allRuns, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), 2, 2, allRuns);
        Mockito.when(dataService.getBatchLatestTestExecutionsSummary(
            Mockito.eq("SmokeBatch"),
            Mockito.anyList(),
            Mockito.anySet(),
            Mockito.any(),
            Mockito.any(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anyMap(),
            Mockito.anyMap(),
            Mockito.anyMap(),
            Mockito.eq(List.of("browser", "locale", "environment", "Release"))
        )).thenReturn(latestExecResult);

        // When checkbox is unchecked, getBatchTestBaseClasses must invoke canonical 2-arg overload without custom dimensions
        final TestBaseVariationEntity mergedVar = new TestBaseVariationEntity("var-canonical", "LoginTest", "testLogin", "Default", "@General", "US", "Chrome");
        final TestBaseClassDto classDto = new TestBaseClassDto("LoginTest", "batchClassContainerLoginTest", 1, List.of(mergedVar));
        Mockito.when(dataService.getBatchTestBaseClasses("SmokeBatch", allRuns)).thenReturn(List.of(classDto));

        final Model model = new ConcurrentModel();
        final String view = controller.batchHistory("SmokeBatch", null, "true", model);

        Assertions.assertEquals("fragments/batch-history :: batchHistory", view);
        Assertions.assertEquals(Boolean.FALSE, model.getAttribute("applyIdentityToVariations"));
        Assertions.assertEquals(Boolean.FALSE, model.getAttribute("applyUniquenessToVariations"));
        Assertions.assertEquals(1, model.getAttribute("totalBatchVariationsCount"));

        // Verify dataService.getBatchTestBaseClasses was called with canonical 2-arg overload
        Mockito.verify(dataService).getBatchTestBaseClasses("SmokeBatch", allRuns);
        Mockito.verify(dataService, Mockito.never()).getBatchTestBaseClasses(
            Mockito.any(),
            Mockito.any(),
            Mockito.any(),
            Mockito.any(),
            Mockito.any(),
            Mockito.any(),
            Mockito.any(),
            Mockito.any(),
            Mockito.any(),
            Mockito.any(),
            Mockito.any(),
            Mockito.any(),
            Mockito.any()
        );

        // Verify Thymeleaf template does NOT render environment or release headers
        final SpringTemplateEngine engine = new SpringTemplateEngine();
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        engine.setTemplateResolver(resolver);

        final StandardLinkBuilder linkBuilder = new StandardLinkBuilder()
        {
            @Override
            protected String computeContextPath(
                final IExpressionContext expressionContext,
                final String base,
                final Map<String, Object> parameters)
            {
                return "";
            }
        };
        engine.setLinkBuilder(linkBuilder);

        final Context context = new Context(Locale.US);
        model.asMap().forEach(context::setVariable);

        final String html = engine.process("fragments/batch-history", Collections.singleton("batchHistory"), context);
        Assertions.assertNotNull(html);
        Assertions.assertTrue(html.contains("id=\"pageBatchHistoryOverview\""), "Container should be rendered");
        Assertions.assertTrue(html.contains("id=\"batchRunsHistoryTableBody\""), "Runs table body should be rendered");
    }

    @Test
    public void testDynamicTagRatioCardsGenerationAndRendering()
    {
        final String batchName = "batch-tags-test";
        final TestRunEntity run1 = new TestRunEntity();
        run1.setId("run-tag-1");
        run1.setBatchName(batchName);
        run1.setStatus("passed");
        run1.setStartTimeMs(1000L);
        run1.setEndTimeMs(2000L);
        run1.setEnvironment("PROD");
        run1.setBrowsersCsv("Chrome");
        run1.setLocalesCsv("US");

        final TestRunTagEntity tag1 = new TestRunTagEntity();
        tag1.setTagKey("Release");
        tag1.setTagValue("1.0");
        run1.setTags(List.of(tag1));

        final List<TestRunEntity> runs = List.of(run1);
        Mockito.when(runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc(batchName)).thenReturn(runs);
        Mockito.when(dataService.getAllTagKeys()).thenReturn(List.of("Release", "Team"));

        final BatchSummaryDto batchSummary = new BatchSummaryDto(
            batchName, batchName, "PROD", "desc", "now", "100%", "", List.of("US"), List.of("Chrome")
        );
        final BatchLatestVariationDto varDto = new BatchLatestVariationDto(
            "TestClass#testMethod#Default#US#Chrome",
            "passed-clean", false, "JAVA", "run-tag-1", "PROD",
            "Chrome", "US", "@Browsing", "Default", Map.of("Release", "1.0", "Team", "Core")
        );
        final BatchLatestExecutionsDto summary = new BatchLatestExecutionsDto(
            batchSummary,
            runs,
            List.of(new DistributionRatioDto("Chrome", 1, 100.0, "#3b82f6")),
            List.of(new DistributionRatioDto("US", 1, 100.0, "#10b981")),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Map.of(
                "Release", List.of(new DistributionRatioDto("1.0", 1, 100.0, "#10b981")),
                "Team", List.of(new DistributionRatioDto("Core", 1, 100.0, "#3b82f6"))
            ),
            List.of(varDto),
            1,
            1,
            runs
        );

        Mockito.when(dataService.getBatchLatestTestExecutionsSummary(
            Mockito.eq(batchName),
            Mockito.any(),
            Mockito.any(),
            Mockito.any()
        )).thenReturn(summary);

        final TestBaseVariationEntity mergedVar = new TestBaseVariationEntity("var-canonical", "LoginTest", "testLogin", "Default", "@General", "US", "Chrome");
        final TestBaseClassDto classDto = new TestBaseClassDto("LoginTest", "batchClassContainerLoginTest", 1, List.of(mergedVar));
        Mockito.when(dataService.getBatchTestBaseClasses(Mockito.eq(batchName), Mockito.any())).thenReturn(List.of(classDto));

        final Model model = new ConcurrentModel();
        final String viewName = controller.batchHistory(batchName, null, "true", model);

        Assertions.assertEquals("fragments/batch-history :: batchHistory", viewName);
        Assertions.assertTrue(model.containsAttribute("tagRatioCards"), "Model must contain tagRatioCards");

        @SuppressWarnings("unchecked")
        final List<TagRatioCardDto> tagRatioCards = (List<TagRatioCardDto>) model.getAttribute("tagRatioCards");
        Assertions.assertNotNull(tagRatioCards);
        Assertions.assertEquals(2, tagRatioCards.size(), "Should have cards for Release and Team");

        final TagRatioCardDto releaseCard = tagRatioCards.stream()
            .filter(c -> "Release".equalsIgnoreCase(c.getTagKey()))
            .findFirst()
            .orElse(null);
        Assertions.assertNotNull(releaseCard);
        Assertions.assertEquals("Tag: Release Ratio", releaseCard.getDisplayName());
        Assertions.assertEquals("tag_Release", releaseCard.getRatioType());
        Assertions.assertEquals("batchClearTag_ReleaseFilterBtn", releaseCard.getClearBtnId());
        Assertions.assertEquals(1, releaseCard.getRatios().size());

        // Thymeleaf rendering test
        final SpringTemplateEngine engine = new SpringTemplateEngine();
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        engine.setTemplateResolver(resolver);

        final StandardLinkBuilder linkBuilder = new StandardLinkBuilder()
        {
            @Override
            protected String computeContextPath(
                final IExpressionContext expressionContext,
                final String base,
                final Map<String, Object> parameters)
            {
                return "";
            }
        };
        engine.setLinkBuilder(linkBuilder);

        final Context context = new Context(Locale.US);
        model.asMap().forEach(context::setVariable);

        final String html = engine.process("fragments/batch-history", Collections.singleton("batchHistory"), context);
        Assertions.assertNotNull(html);
        Assertions.assertTrue(html.contains("id=\"pageBatchHistoryOverview\""), "Container should be rendered");
        Assertions.assertTrue(html.contains("id=\"batchRunsHistoryTableBody\""), "Runs table body should be rendered");
    }

    @Test
    public void testBatchHistoryInnerGroupingCardsAndScoping()
    {
        final TestRunEntity r1 = new TestRunEntity("r1", "SmokeBatch", "COMPLETED", "Main", "Production", "Java", "Chrome", "10:00", 1000L);
        r1.setBrowsersCsv("Chrome");
        final TestRunEntity r2 = new TestRunEntity("r2", "SmokeBatch", "COMPLETED", "Main", "Production", "Java", "Firefox", "11:00", 1000L);
        r2.setBrowsersCsv("Firefox");
        final TestRunEntity r3 = new TestRunEntity("r3", "SmokeBatch", "COMPLETED", "Main", "Staging", "Java", "Safari", "12:00", 1000L);
        r3.setBrowsersCsv("Safari");

        final List<TestRunEntity> allRuns = List.of(r1, r2, r3);
        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(allRuns);

        final BatchSummaryDto mockSummary = new BatchSummaryDto(
            "Production", "All Batches", "Production", "", "", "", "", List.of(), List.of()
        );
        final BatchLatestExecutionsDto mockResult = new BatchLatestExecutionsDto(
            mockSummary, List.of(), List.of(), List.of(), List.of(), List.of(), 0
        );
        Mockito.when(dataService.getBatchLatestTestExecutionsSummary(
            Mockito.anyString(), Mockito.anyList(), Mockito.any(), Mockito.any()
        )).thenReturn(mockResult);

        final BatchSummaryDto chromeCard = new BatchSummaryDto(
            "Chrome", "Chrome", "Production", "", "10 runs", "100%", "", List.of(), List.of(),
            "/batch-history?groupType=environment%2Cbrowser&groupValue=Production+%E2%80%A2+Chrome"
        );
        final BatchSummaryDto firefoxCard = new BatchSummaryDto(
            "Firefox", "Firefox", "Production", "", "8 runs", "100%", "", List.of(), List.of(),
            "/batch-history?groupType=environment%2Cbrowser&groupValue=Production+%E2%80%A2+Firefox"
        );
        final BatchOverviewDataDto subOverview = new BatchOverviewDataDto(
            List.of(chromeCard, firefoxCard), List.of(), List.of(), List.of(), List.of()
        );

        Mockito.when(dataService.getGroupOverviewData(
            Mockito.eq(List.of("browser")),
            Mockito.anyList(),
            Mockito.eq("environment"),
            Mockito.eq("Production"),
            Mockito.anyMap()
        )).thenReturn(subOverview);

        final Model model = new ConcurrentModel();
        final String view = controller.batchHistory(
            "All Batches",
            "environment",
            "Production",
            "browser",
            50,
            null,
            null,
            "true",
            model
        );

        Assertions.assertEquals("fragments/batch-history :: batchHistory", view);

        @SuppressWarnings("unchecked")
        final List<BatchSummaryDto> batches = (List<BatchSummaryDto>) model.getAttribute("batches");
        Assertions.assertNotNull(batches);
        Assertions.assertEquals(2, batches.size());
        Assertions.assertEquals("Chrome", batches.get(0).getName());
        Assertions.assertEquals("/batch-history?groupType=environment%2Cbrowser&groupValue=Production+%E2%80%A2+Chrome", batches.get(0).getHistoryUrl());
        Assertions.assertEquals("Firefox", batches.get(1).getName());

        // Parent dimension (environment) must be excluded from inner grouping options
        Assertions.assertEquals(Boolean.FALSE, model.getAttribute("showEnvPill"));
        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("showBrowserPill"));
        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("isBrowserActive"));
        Assertions.assertEquals("browser", model.getAttribute("groupBy"));

        final String toggleNoneUrl = (String) model.getAttribute("toggleNoneUrl");
        Assertions.assertNotNull(toggleNoneUrl);
        Assertions.assertTrue(toggleNoneUrl.contains("groupBy=none"));
        Assertions.assertTrue(toggleNoneUrl.contains("groupType=environment"));
        Assertions.assertTrue(toggleNoneUrl.contains("groupValue=Production"));
    }

    @Test
    public void testBatchHistoryHierarchicalBackUrl()
    {
        final TestRunEntity r1 = new TestRunEntity("r1", "SmokeBatch", "COMPLETED", "Main", "Production", "Java", "Chrome", "10:00", 1000L);
        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of(r1));

        final BatchSummaryDto mockSummary = new BatchSummaryDto(
            "Production • Chrome", "All Batches", "Production", "", "", "", "", List.of(), List.of()
        );
        final BatchLatestExecutionsDto mockResult = new BatchLatestExecutionsDto(
            mockSummary, List.of(), List.of(), List.of(), List.of(), List.of(), 0
        );
        Mockito.when(dataService.getBatchLatestTestExecutionsSummary(
            Mockito.anyString(), Mockito.anyList(), Mockito.any(), Mockito.any()
        )).thenReturn(mockResult);

        final Model model = new ConcurrentModel();
        controller.batchHistory(
            "All Batches",
            "environment,browser",
            "Production • Chrome",
            50,
            null,
            null,
            "true",
            model
        );

        final String backUrl = (String) model.getAttribute("backUrl");
        Assertions.assertNotNull(backUrl);
        Assertions.assertEquals("/?groupBy=environment,browser", backUrl);
    }

    @Test
    public void testPinInnerGroupingViaEndpoint()
    {
        final TestRunEntity r1 = new TestRunEntity("r1", "SmokeBatch", "COMPLETED", "Main", "Production", "Java", "Chrome", "10:00", 1000L);
        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of(r1));

        final BatchSummaryDto mockSummary = new BatchSummaryDto(
            "Production", "All Batches", "Production", "", "", "", "", List.of(), List.of()
        );
        final BatchLatestExecutionsDto mockResult = new BatchLatestExecutionsDto(
            mockSummary, List.of(), List.of(), List.of(), List.of(), List.of(), 0
        );
        Mockito.when(dataService.getBatchLatestTestExecutionsSummary(
            Mockito.anyString(), Mockito.anyList(), Mockito.any(), Mockito.any()
        )).thenReturn(mockResult);

        final Model model = new ConcurrentModel();
        final String view = controller.pinGrouping(
            "browser",
            "environment",
            "true",
            "http://localhost:8080/batch-history?groupType=environment&groupValue=Production",
            null,
            model
        );

        Mockito.verify(settingService).togglePinnedGrouping("environment", "browser");
        Assertions.assertEquals("fragments/batch-history :: batchHistory", view);
    }

    @Test
    public void testBatchHistoryInnerGroupingTemplateRendersCardsAboveMetrics()
    {
        final SpringTemplateEngine engine = new SpringTemplateEngine();
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        engine.setTemplateResolver(resolver);

        final StandardLinkBuilder linkBuilder = new StandardLinkBuilder()
        {
            @Override
            protected String computeContextPath(
                final IExpressionContext expressionContext,
                final String base,
                final Map<String, Object> parameters)
            {
                return "";
            }
        };
        engine.setLinkBuilder(linkBuilder);

        final BatchSummaryDto chromeCard = new BatchSummaryDto(
            "Chrome", "Chrome", "Production", "", "10 runs", "100%", "", List.of(), List.of(),
            "/batch-history?groupType=environment,browser&groupValue=Production%20%E2%80%A2%20Chrome"
        );
        final BatchSummaryDto parentSummary = new BatchSummaryDto(
            "Production", "All Batches", "Production", "", "20 runs", "100%", "", List.of(), List.of()
        );

        final Context context = new Context(Locale.US);
        context.setVariable("batchName", "All Batches");
        context.setVariable("groupType", "environment");
        context.setVariable("groupValue", "Production");
        context.setVariable("groupBy", "browser");
        context.setVariable("groupLabel", "Browsers");
        context.setVariable("backUrl", "/");
        context.setVariable("isGroupingPinned", false);
        context.setVariable("showBatchPill", true);
        context.setVariable("showEnvPill", false);
        context.setVariable("showLocalePill", true);
        context.setVariable("showBrowserPill", true);
        context.setVariable("showAreaPill", true);
        context.setVariable("isBrowserActive", true);
        context.setVariable("batches", List.of(chromeCard));
        context.setVariable("batchSummary", parentSummary);
        context.setVariable("latestExecResult", new BatchLatestExecutionsDto(parentSummary, List.of(), List.of(), List.of(), List.of(), List.of(), 0));
        context.setVariable("latestVariations", List.of());
        context.setVariable("contributingRuns", List.of());
        context.setVariable("runs", List.of());
        context.setVariable("totalRunsCount", 0);
        context.setVariable("showingRunsCount", 0);
        context.setVariable("areaTrends", List.of());
        context.setVariable("batchTestClasses", List.of());
        context.setVariable("totalBatchVariationsCount", 0);
        context.setVariable("tagRatioCards", List.of());
        context.setVariable("browserRatios", List.of());
        context.setVariable("localeRatios", List.of());
        context.setVariable("environmentRatios", List.of());
        context.setVariable("areaRatios", List.of());
        context.setVariable("filterEnvs", List.of());
        context.setVariable("filterLocales", List.of());
        context.setVariable("filterBrowsers", List.of());
        context.setVariable("filterAreas", List.of());
        context.setVariable("selectedRunBrowsers", List.of());
        context.setVariable("selectedRunLocales", List.of());
        context.setVariable("selectedRunEnvs", List.of());
        context.setVariable("selectedRunAreas", List.of());
        context.setVariable("selectedRunTags", List.of());
        context.setVariable("hasActiveRunFilters", false);

        final String html = engine.process("fragments/batch-history", Collections.singleton("batchHistory"), context);
        Assertions.assertNotNull(html);

        Assertions.assertTrue(html.contains("id=\"pageBatchHistoryOverview\""), "Container should be rendered");
        Assertions.assertTrue(html.contains("id=\"batchRunsHistoryTableBody\""), "Runs table body should be rendered");
    }

    @Test
    public void testBatchHistoryTokenCostTrendGroupsJsonUnescaped() throws Exception
    {
        final SpringTemplateEngine engine = new SpringTemplateEngine();
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        engine.setTemplateResolver(resolver);

        final StandardLinkBuilder linkBuilder = new StandardLinkBuilder()
        {
            @Override
            protected String computeContextPath(
                final IExpressionContext expressionContext,
                final String base,
                final Map<String, Object> parameters)
            {
                return "";
            }
        };
        engine.setLinkBuilder(linkBuilder);

        final BatchSummaryDto parentSummary = new BatchSummaryDto(
            "Production", "All Batches", "Production", "", "20 runs", "100%", "", List.of(), List.of()
        );

        final ContributingRunCostDto run1 = new ContributingRunCostDto(
            "run_001", "2026-10-28 12:00", "Chrome", "Production", "Regression", 15, 0.2500, 10000, 75.0
        );
        final ContributingRunCostDto run2 = new ContributingRunCostDto(
            "run_002", "2026-10-28 12:05", "Firefox", "Production", "Regression", 5, 0.0833, 3000, 25.0
        );
        final TokenCostPointGroupDto pointGroup = new TokenCostPointGroupDto(
            0, "Point 1", "run_002", "2026-10-28 12:05", 0.3333, 13000L, 0.0500, 17.65, List.of(run1, run2)
        );
        final TokenCostTrendDto trend = new TokenCostTrendDto(
            0.3333, 0.2833, 13000L, List.of(0.2833, 0.3333), List.of(pointGroup)
        );

        final Context context = new Context(Locale.US);
        context.setVariable("batchName", "All Batches");
        context.setVariable("groupType", null);
        context.setVariable("groupValue", null);
        context.setVariable("groupBy", null);
        context.setVariable("groupLabel", null);
        context.setVariable("backUrl", "/");
        context.setVariable("isGroupingPinned", false);
        context.setVariable("showBatchPill", true);
        context.setVariable("showEnvPill", false);
        context.setVariable("showLocalePill", true);
        context.setVariable("showBrowserPill", true);
        context.setVariable("showAreaPill", true);
        context.setVariable("isBrowserActive", true);
        context.setVariable("batches", List.of());
        context.setVariable("batchSummary", parentSummary);
        context.setVariable("latestExecResult", new BatchLatestExecutionsDto(parentSummary, List.of(), List.of(), List.of(), List.of(), List.of(), 0));
        context.setVariable("latestVariations", List.of());
        context.setVariable("contributingRuns", List.of());
        context.setVariable("runs", List.of());
        context.setVariable("totalRunsCount", 0);
        context.setVariable("showingRunsCount", 0);
        context.setVariable("areaTrends", List.of());
        context.setVariable("batchTestClasses", List.of());
        context.setVariable("totalBatchVariationsCount", 0);
        context.setVariable("tagRatioCards", List.of());
        context.setVariable("browserRatios", List.of());
        context.setVariable("localeRatios", List.of());
        context.setVariable("environmentRatios", List.of());
        context.setVariable("areaRatios", List.of());
        context.setVariable("tokenTrend", trend);
        context.setVariable("selectedRunBrowsers", List.of());
        context.setVariable("selectedRunLocales", List.of());
        context.setVariable("selectedRunEnvs", List.of());
        context.setVariable("selectedRunAreas", List.of());
        context.setVariable("selectedRunTags", List.of());
        context.setVariable("hasActiveRunFilters", false);

        final String html = engine.process("fragments/batch-history", Collections.singleton("batchHistory"), context);
        Assertions.assertNotNull(html);

        Assertions.assertTrue(html.contains("id=\"pageBatchHistoryOverview\""), "Container should be rendered");
        Assertions.assertTrue(html.contains("id=\"batchRunsHistoryTableBody\""), "Runs table body should be rendered");
    }

    @Test
    public void testGetWidgetConfigModal_NewWidget()
    {
        final Model model = new ConcurrentModel();
        final String view = controller.getWidgetConfigModal(null, model);

        Assertions.assertEquals("fragments/widget-configurator-modal :: widgetConfiguratorModal", view);
        Assertions.assertNotNull(model.getAttribute("widget"));
        Assertions.assertNotNull(model.getAttribute("allMetrics"));
        Assertions.assertNotNull(model.getAttribute("allPresentations"));
        Assertions.assertNotNull(model.getAttribute("allTrendModes"));
        Assertions.assertNotNull(model.getAttribute("allValueFormats"));
        Assertions.assertNotNull(model.getAttribute("allPointScopes"));
        @SuppressWarnings("unchecked")
        final List<String> availableTags = (List<String>) model.getAttribute("availableWidgetTags");
        Assertions.assertNotNull(availableTags);
        Assertions.assertTrue(availableTags.contains("Browser"));
        Assertions.assertTrue(availableTags.contains("Locale"));
        Assertions.assertTrue(availableTags.contains("Area"));
        Assertions.assertNotNull(model.getAttribute("candidateBrowsers"));
        Assertions.assertNotNull(model.getAttribute("candidateLocales"));
        Assertions.assertNotNull(model.getAttribute("candidateEnvs"));
        Assertions.assertNotNull(model.getAttribute("candidateAreas"));
        Assertions.assertNotNull(model.getAttribute("availableIdentityTags"));
        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("isOpen"));
    }

    @Test
    public void testGetWidgetConfigModal_ExistingWidget()
    {
        final DashboardWidgetConfigDto existing = new DashboardWidgetConfigDto(
            "w-custom-1", "My Bug Perlenkette", WidgetMetric.BUGS, null,
            WidgetPresentation.PERLENKETTE, WidgetTrendMode.DAILY, WidgetValueFormat.ABSOLUTE, "full", 1
        );
        Mockito.when(settingService.getDashboardWidgets()).thenReturn(List.of(existing));

        final Model model = new ConcurrentModel();
        final String view = controller.getWidgetConfigModal("w-custom-1", model);

        Assertions.assertEquals("fragments/widget-configurator-modal :: widgetConfiguratorModal", view);
        final DashboardWidgetConfigDto widget = (DashboardWidgetConfigDto) model.getAttribute("widget");
        Assertions.assertNotNull(widget);
        Assertions.assertEquals("w-custom-1", widget.getId());
        Assertions.assertEquals("My Bug Perlenkette", widget.getTitle());
        Assertions.assertEquals(WidgetMetric.BUGS, widget.getMetric());
        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("isOpen"));
    }

    @Test
    public void testSaveWidget_InvokesSettingService()
    {
        final Model model = new ConcurrentModel();
        final String result = controller.saveWidget(
            "w-new", "Pass Rate Perlenkette", "STATUS", null, "PERLENKETTE", "LATEST_APPROACH", "PERCENT", "LAST_RUN", "half",
            "true", "/batch-history", null, model
        );

        Mockito.verify(settingService).addOrUpdateWidget(Mockito.argThat(w ->
            "w-new".equals(w.getId())
            && "Pass Rate Perlenkette".equals(w.getTitle())
            && WidgetMetric.STATUS == w.getMetric()
            && WidgetPresentation.PERLENKETTE == w.getPresentation()
            && WidgetTrendMode.LATEST_APPROACH == w.getTrendMode()
            && WidgetValueFormat.PERCENT == w.getValueFormat()
            && WidgetPointScope.LAST_RUN == w.getPointScope()
            && !w.isShowHealedAi()
        ));
        Assertions.assertNotNull(result);
    }

    @Test
    public void testSaveWidget_ForbidsPerlenketteForTagMetric_FallsBackToPieChart()
    {
        final Model model = new ConcurrentModel();
        final String result = controller.saveWidget(
            "w-tag-perlenkette", "Browser String of Pearls", "TAG", "browser", "PERLENKETTE", "BY_RUNS", "PERCENT", "LATEST_TEST_STATUS", "half",
            "true", "/batch-history", null, model
        );

        Mockito.verify(settingService).addOrUpdateWidget(Mockito.argThat(w ->
            "w-tag-perlenkette".equals(w.getId())
            && WidgetMetric.TAG == w.getMetric()
            && "browser".equals(w.getTagKey())
            && WidgetPresentation.PIE_CHART == w.getPresentation()
        ));
        Assertions.assertNotNull(result);
    }

    @Test
    public void testSaveWidget_WithShowHealedAi()
    {
        final Model model = new ConcurrentModel();
        final String result = controller.saveWidget(
            "w-healed-1", "Status Donut", "STATUS", null, "PIE_CHART", "LATEST_APPROACH", "PERCENT", "LATEST_TEST_STATUS", "half",
            true, "true", "/batch-history", null, model
        );

        Mockito.verify(settingService).addOrUpdateWidget(Mockito.argThat(w ->
            "w-healed-1".equals(w.getId())
            && w.isShowHealedAi()
        ));
        Assertions.assertNotNull(result);
    }

    @Test
    public void testSaveWidget_WithQuarterWidth()
    {
        final Model model = new ConcurrentModel();
        final String result = controller.saveWidget(
            "w-quarter-1", "Quarter KPI", "STATUS", null, "PIE_CHART", "LATEST_APPROACH", "PERCENT", "LATEST_TEST_STATUS", "quarter",
            "true", "/batch-history", null, model
        );

        Mockito.verify(settingService).addOrUpdateWidget(Mockito.argThat(w ->
            "w-quarter-1".equals(w.getId())
            && w.isQuarterWidth()
            && !w.isHalfWidth()
            && !w.isFullWidth()
            && "quarter".equals(w.getWidth())
        ));
        Assertions.assertNotNull(result);
    }

    @Test
    public void testDeleteWidget_InvokesSettingService()
    {
        final Model model = new ConcurrentModel();
        final String result = controller.deleteWidget("w-delete-me", "true", "/batch-history", null, model);

        Mockito.verify(settingService).deleteWidget("w-delete-me");
        Assertions.assertNotNull(result);
    }

    @Test
    public void testResetWidgets_InvokesSettingService()
    {
        final Model model = new ConcurrentModel();
        final String result = controller.resetWidgets("true", "/batch-history", null, model);

        Mockito.verify(settingService).resetDefaultWidgets();
        Assertions.assertNotNull(result);
    }

    @Test
    public void testReorderWidgets_InvokesSettingService()
    {
        final Model model = new ConcurrentModel();
        final String result = controller.reorderWidgets("w-3,w-1,w-2", "true", "/batch-history", null, model);

        Mockito.verify(settingService).reorderWidgets(List.of("w-3", "w-1", "w-2"));
        Assertions.assertNotNull(result);
    }

    @Test
    public void testBatchHistory_CustomWidgetsConfigured_PopulatesModel()
    {
        Mockito.when(settingService.isCustomWidgetsConfigured()).thenReturn(true);
        final DashboardWidgetConfigDto customWidget = new DashboardWidgetConfigDto(
            "custom-w", "Custom Token Trend", WidgetMetric.COSTS, null,
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.DAILY, WidgetValueFormat.PERCENT, "full", 0
        );
        Mockito.when(settingService.getDashboardWidgets()).thenReturn(List.of(customWidget));

        final Model model = new ConcurrentModel();
        final String view = controller.batchHistory(
            null, "all", null, null, null, null, null, null,
            null, null, null, null, null, "true", model, Collections.emptyMap()
        );

        Assertions.assertEquals("fragments/batch-history :: batchHistory", view);
        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("isCustomWidgetsConfigured"));
        @SuppressWarnings("unchecked")
        final List<DashboardWidgetViewDto> widgets = (List<DashboardWidgetViewDto>) model.getAttribute("dashboardWidgets");
        Assertions.assertNotNull(widgets);
        Assertions.assertEquals(1, widgets.size());
        Assertions.assertEquals("Custom Token Trend", widgets.get(0).getTitle());
        @SuppressWarnings("unchecked")
        final List<String> availableTags = (List<String>) model.getAttribute("availableWidgetTags");
        Assertions.assertNotNull(availableTags);
        Assertions.assertTrue(availableTags.contains("Browser"));
    }

    @Test
    public void testGetWidgetExecutions_DefaultWidget_PopulatesModelAndReturnsView()
    {
        final Model model = new ConcurrentModel();
        final String view = controller.getWidgetExecutions(null, null, null, null, model);

        Assertions.assertEquals("fragments/widget-executions-modal :: widgetExecutionsModal", view);
        Assertions.assertNotNull(model.getAttribute("widget"));
        final WidgetContributingExecutionsDto executions = (WidgetContributingExecutionsDto) model.getAttribute("widgetExecutions");
        Assertions.assertNotNull(executions);
        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("isOpen"));
    }

    @Test
    public void testGetWidgetExecutions_SpecificWidgetId_ResolvesContributingExecutions()
    {
        final DashboardWidgetConfigDto customWidget = new DashboardWidgetConfigDto(
            "custom-widget-1", "Custom Status Pie", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, WidgetPointScope.LAST_RUN, "half", 0
        );
        Mockito.when(settingService.getDashboardWidgets()).thenReturn(List.of(customWidget));

        final TestRunEntity run = new TestRunEntity();
        run.setId("run-modal-1");
        run.setBatchName("BatchTest");
        run.setStartTimeMs(1000L);
        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of(run));

        final TestExecutionDto exec = new TestExecutionDto(
            "exec-1", "run-modal-1", "MyClass", "testMethod", "TestMethod", "passed-clean", "US", "Chrome", Collections.emptyMap()
        );
        final RunReportDto report = Mockito.mock(RunReportDto.class);
        Mockito.when(report.getExecutions()).thenReturn(List.of(exec));
        Mockito.when(dataService.getRunReport("run-modal-1")).thenReturn(report);

        final Model model = new ConcurrentModel();
        final String view = controller.getWidgetExecutions("custom-widget-1", null, null, null, model);

        Assertions.assertEquals("fragments/widget-executions-modal :: widgetExecutionsModal", view);
        final DashboardWidgetConfigDto widget = (DashboardWidgetConfigDto) model.getAttribute("widget");
        Assertions.assertNotNull(widget);
        Assertions.assertEquals("custom-widget-1", widget.getId());
        Assertions.assertEquals("Custom Status Pie", widget.getTitle());

        final WidgetContributingExecutionsDto executions = (WidgetContributingExecutionsDto) model.getAttribute("widgetExecutions");
        Assertions.assertNotNull(executions);
        Assertions.assertEquals("custom-widget-1", executions.getWidgetId());
        Assertions.assertEquals("Custom Status Pie", executions.getWidgetTitle());
        Assertions.assertEquals(1, executions.getTotalExecutionsCount());
        Assertions.assertEquals(1, executions.getOverallPassCount());
        Assertions.assertEquals(1, executions.getTotalRunsCount());
        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("isOpen"));
    }

    @Test
    public void testGetWidgetExecutions_WithExcludeRuns_OmitsExcludedRunsFromContribution()
    {
        final DashboardWidgetConfigDto customWidget = new DashboardWidgetConfigDto(
            "custom-widget-ex", "Custom Status Pie", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, WidgetPointScope.LAST_RUN, "half", 0
        );
        Mockito.when(settingService.getDashboardWidgets()).thenReturn(List.of(customWidget));

        final TestRunEntity run1 = new TestRunEntity();
        run1.setId("run-include-1");
        run1.setBatchName("BatchTest");
        run1.setStartTimeMs(2000L);

        final TestRunEntity run2 = new TestRunEntity();
        run2.setId("run-exclude-2");
        run2.setBatchName("BatchTest");
        run2.setStartTimeMs(1000L);

        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of(run1, run2));

        final TestExecutionDto exec1 = new TestExecutionDto(
            "exec-1", "run-include-1", "MyClass", "testMethod", "TestMethod", "passed-clean", "US", "Chrome", Collections.emptyMap()
        );
        final RunReportDto report1 = Mockito.mock(RunReportDto.class);
        Mockito.when(report1.getExecutions()).thenReturn(List.of(exec1));
        Mockito.when(dataService.getRunReport("run-include-1")).thenReturn(report1);

        final Model model = new ConcurrentModel();
        final String view = controller.getWidgetExecutions("custom-widget-ex", "run-exclude-2", null, null, model);

        Assertions.assertEquals("fragments/widget-executions-modal :: widgetExecutionsModal", view);
        Assertions.assertEquals("run-exclude-2", model.getAttribute("excludeRuns"));
        final Set<?> excluded = (Set<?>) model.getAttribute("excludedRunIds");
        Assertions.assertNotNull(excluded);
        Assertions.assertTrue(excluded.contains("run-exclude-2"));

        final WidgetContributingExecutionsDto executions = (WidgetContributingExecutionsDto) model.getAttribute("widgetExecutions");
        Assertions.assertNotNull(executions);
        Assertions.assertEquals(1, executions.getTotalRunsCount(), "Excluded run should not be present in contributing runs");
        Assertions.assertEquals(1, executions.getTotalExecutionsCount());
        Assertions.assertEquals("run-include-1", executions.getRunGroups().get(0).getRunId());
    }

    @Test
    public void testGetWidgetExecutions_ContainerWidget_ResolvesContributingExecutionsForGroup()
    {
        final DashboardWidgetConfigDto child1 = new DashboardWidgetConfigDto(
            "child-1", "Status Donut", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS, "half", 0
        );
        final DashboardWidgetConfigDto child2 = new DashboardWidgetConfigDto(
            "child-2", "Browser Ratio", WidgetMetric.TAG, "browser",
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS, "half", 1
        );
        final DashboardWidgetConfigDto container = DashboardWidgetConfigDto.createContainer("container-group-1", "Group Status & Browser", List.of(child1, child2), 0);
        Mockito.when(settingService.getDashboardWidgets()).thenReturn(List.of(container));

        final TestRunEntity run = new TestRunEntity();
        run.setId("run-group-1");
        run.setBatchName("BatchTest");
        run.setStartTimeMs(1500L);
        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of(run));

        final TestExecutionDto exec = new TestExecutionDto(
            "exec-g1", "run-group-1", "GroupClass", "groupMethod", "GroupMethod", "passed-clean", "US", "Chrome", Collections.emptyMap()
        );
        final RunReportDto report = Mockito.mock(RunReportDto.class);
        Mockito.when(report.getExecutions()).thenReturn(List.of(exec));
        Mockito.when(dataService.getRunReport("run-group-1")).thenReturn(report);

        final BatchLatestExecutionsDto latestExec = Mockito.mock(BatchLatestExecutionsDto.class);
        final BatchLatestVariationDto variation = new BatchLatestVariationDto(
            "var-1", "passed-clean", false, "JAVA", "run-group-1", "Default", "Chrome", "US", "GroupClass", "groupMethod", Collections.emptyMap(), 0.0, 0L, "exec-g1"
        );
        Mockito.when(latestExec.getLatestVariations()).thenReturn(List.of(variation));
        Mockito.when(dataService.getBatchLatestTestExecutionsSummary(
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any()
        )).thenReturn(latestExec);

        final Model model = new ConcurrentModel();
        final String view = controller.getWidgetExecutions("container-group-1", null, null, null, model);

        Assertions.assertEquals("fragments/widget-executions-modal :: widgetExecutionsModal", view);
        final DashboardWidgetConfigDto widget = (DashboardWidgetConfigDto) model.getAttribute("widget");
        Assertions.assertNotNull(widget);
        Assertions.assertEquals("container-group-1", widget.getId());
        Assertions.assertTrue(widget.isContainer());

        final WidgetContributingExecutionsDto executions = (WidgetContributingExecutionsDto) model.getAttribute("widgetExecutions");
        Assertions.assertNotNull(executions);
        Assertions.assertEquals("container-group-1", executions.getWidgetId());
        Assertions.assertEquals(1, executions.getTotalRunsCount());
        Assertions.assertEquals(1, executions.getTotalExecutionsCount());
        Assertions.assertEquals("run-group-1", executions.getRunGroups().get(0).getRunId());
    }

    @Test
    public void testSaveWidget_WithOwnExecutionIdentityAndRunFilters()
    {
        final Model model = new ConcurrentModel();
        final Map<String, String> extraParams = Map.of(
            "filterTag_device", "mobile,tablet",
            "excludeFilterTag_device", "desktop"
        );

        final String result = controller.saveWidget(
            "w-isolated-1",
            "Custom Isolated Widget",
            "STATUS",
            null,
            "PIE_CHART",
            "LATEST_APPROACH",
            "PERCENT",
            "LATEST_TEST_STATUS",
            "half",
            false,
            true,
            "browser,locale,tag:tier",
            true,
            "Chrome,Firefox",
            "Safari",
            "US,DE",
            "FR",
            "Prod,Staging",
            "Dev",
            "Checkout",
            "Cart",
            "smoke",
            "flaky",
            extraParams,
            "true",
            "/batch-history",
            null,
            model
        );

        Mockito.verify(settingService).addOrUpdateWidget(Mockito.argThat(w ->
            "w-isolated-1".equals(w.getId())
            && "Custom Isolated Widget".equals(w.getTitle())
            && w.hasOwnExecutionIdentity()
            && List.of("browser", "locale", "tag:tier").equals(w.getExecutionIdentityDimensions())
            && w.hasOwnRunFilters()
            && w.getRunFilters() != null
            && w.getRunFilters().getBrowsers().contains("Chrome")
            && w.getRunFilters().getBrowsers().contains("Firefox")
            && w.getRunFilters().getExcludeBrowsers().contains("Safari")
            && w.getRunFilters().getLocales().contains("US")
            && w.getRunFilters().getLocales().contains("DE")
            && w.getRunFilters().getExcludeLocales().contains("FR")
            && w.getRunFilters().getEnvs().contains("Prod")
            && w.getRunFilters().getEnvs().contains("Staging")
            && w.getRunFilters().getExcludeEnvs().contains("Dev")
            && w.getRunFilters().getAreas().contains("Checkout")
            && w.getRunFilters().getExcludeAreas().contains("Cart")
            && w.getRunFilters().getTags().contains("smoke")
            && w.getRunFilters().getExcludeTags().contains("flaky")
            && w.getRunFilters().getTagsByType().containsKey("device")
            && w.getRunFilters().getTagsByType().get("device").contains("mobile")
            && w.getRunFilters().getExcludeTagsByType().containsKey("device")
            && w.getRunFilters().getExcludeTagsByType().get("device").contains("desktop")
        ));
        Assertions.assertNotNull(result);
    }

    @Test
    public void testSaveWidget_WithLimitAndTimestampFilters_CorrectlyPersistsCriteria()
    {
        final Model model = new ConcurrentModel();
        final String result = controller.saveWidget(
            "w-limit-test",
            "Widget With Limit and Dates",
            "STATUS",
            null,
            "PIE_CHART",
            "LATEST_APPROACH",
            "PERCENT",
            "LATEST_TEST_STATUS",
            "half",
            false,
            false,
            null,
            true,
            25,
            "2026-10-01",
            "2026-10-31",
            "Chrome",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            Collections.emptyMap(),
            "true",
            "/batch-history",
            null,
            model
        );

        Mockito.verify(settingService).addOrUpdateWidget(Mockito.argThat(w ->
            "w-limit-test".equals(w.getId())
            && w.hasOwnRunFilters()
            && w.getRunFilters() != null
            && Integer.valueOf(25).equals(w.getRunFilters().getLimit())
            && "2026-10-01".equals(w.getRunFilters().getStartDate())
            && "2026-10-31".equals(w.getRunFilters().getEndDate())
            && w.getRunFilters().getBrowsers().contains("Chrome")
        ));
        Assertions.assertNotNull(result);
    }

    @Test
    public void testGetWidgetExecutions_WithOwnRunFilters_ScopesRunsAndPassesFilters()
    {
        final WidgetFilterConfigDto runFilters = new WidgetFilterConfigDto(
            Set.of("Firefox"), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptyMap(), Collections.emptyMap()
        );

        final DashboardWidgetConfigDto customWidget = new DashboardWidgetConfigDto(
            "widget-ff", "Firefox Donut", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, WidgetPointScope.LAST_RUN,
            "half", 0, false, false, Collections.emptyList(), true, runFilters
        );
        Mockito.when(settingService.getDashboardWidgets()).thenReturn(List.of(customWidget));

        final TestRunEntity run = new TestRunEntity();
        run.setId("run-ff-1");
        run.setBatchName("BatchTest");
        run.setStartTimeMs(1000L);
        run.setTotalTests(5);
        Mockito.when(runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc("BatchTest")).thenReturn(List.of(run));
        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of(run));

        Mockito.when(dataService.scopeRunToCriteria(
            Mockito.eq(run),
            Mockito.eq(Set.of("Firefox")), Mockito.any(),
            Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any(),
            Mockito.isNull(),
            Mockito.any(), Mockito.any()
        )).thenReturn(run);

        final TestExecutionDto exec = new TestExecutionDto(
            "exec-ff-1", "run-ff-1", "MyClass", "testMethod", "TestMethod", "passed-clean", "US", "Firefox", Collections.emptyMap()
        );
        final RunReportDto report = Mockito.mock(RunReportDto.class);
        Mockito.when(report.getExecutions()).thenReturn(List.of(exec));
        Mockito.when(dataService.getRunReport("run-ff-1")).thenReturn(report);

        final Model model = new ConcurrentModel();
        final String view = controller.getWidgetExecutions("widget-ff", null, "/batch-history?batchName=BatchTest", null, model);

        Assertions.assertEquals("fragments/widget-executions-modal :: widgetExecutionsModal", view);
        Mockito.verify(dataService).scopeRunToCriteria(
            Mockito.eq(run),
            Mockito.eq(Set.of("Firefox")), Mockito.any(),
            Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any(),
            Mockito.isNull(),
            Mockito.any(), Mockito.any()
        );
        final WidgetContributingExecutionsDto executions = (WidgetContributingExecutionsDto) model.getAttribute("widgetExecutions");
        Assertions.assertNotNull(executions);
        Assertions.assertEquals("widget-ff", executions.getWidgetId());
    }

    @Test
    public void testCombineWidgets_WhenSameContributingRuns_InvokesSettingService()
    {
        final DashboardWidgetConfigDto w1 = new DashboardWidgetConfigDto(
            "w-target", "Status Donut", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0, false, false, Collections.emptyList(), true, null
        );
        final DashboardWidgetConfigDto w2 = new DashboardWidgetConfigDto(
            "w-source", "Failure List", WidgetMetric.STATUS, null,
            WidgetPresentation.LIST, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 1, false, false, Collections.emptyList(), true, null
        );
        Mockito.when(settingService.getDashboardWidgets()).thenReturn(List.of(w1, w2));

        final TestRunEntity run = new TestRunEntity("run-1", "DefaultBatch", "COMPLETED", "CI", "staging", "en-US", "Chrome", "2026-11-01 10:00", 1730455200000L);
        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of(run));

        final BatchLatestVariationDto var1 = new BatchLatestVariationDto("k1", "passed-clean", false, "JAVA", "run-1", "Default", "Chrome", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L);
        final BatchLatestExecutionsDto execSummary = Mockito.mock(BatchLatestExecutionsDto.class);
        Mockito.when(execSummary.getLatestVariations()).thenReturn(List.of(var1));
        Mockito.when(dataService.getBatchLatestTestExecutionsSummary(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(execSummary);

        final Model model = new ConcurrentModel();
        final String result = controller.combineWidgets("w-target", "w-source", "true", "/batch-history", null, model);

        Mockito.verify(settingService).combineWidgets("w-target", "w-source");
        Assertions.assertNull(model.getAttribute("widgetWarningMessage"));
        Assertions.assertNotNull(result);
    }

    @Test
    public void testCombineWidgets_WhenDifferentContributingRuns_SetsWarningMessage()
    {
        final DashboardWidgetConfigDto w1 = new DashboardWidgetConfigDto(
            "w-target", "Status Donut", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0, false, false, Collections.emptyList(), true, null
        );
        final DashboardWidgetConfigDto w2 = new DashboardWidgetConfigDto(
            "w-source", "Status Trend", WidgetMetric.STATUS, null,
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "full", 1, false, false, Collections.emptyList(), true, null
        );
        Mockito.when(settingService.getDashboardWidgets()).thenReturn(List.of(w1, w2));

        final TestRunEntity run1 = new TestRunEntity("run-1", "DefaultBatch", "COMPLETED", "CI", "staging", "en-US", "Chrome", "2026-11-01 10:00", 1730455200000L);
        final TestRunEntity run2 = new TestRunEntity("run-2", "DefaultBatch", "COMPLETED", "CI", "staging", "en-US", "Firefox", "2026-11-01 11:00", 1730458800000L);
        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of(run1, run2));

        final BatchLatestVariationDto var1 = new BatchLatestVariationDto("k1", "passed-clean", false, "JAVA", "run-1", "Default", "Chrome", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L);
        final BatchLatestExecutionsDto execSummary = Mockito.mock(BatchLatestExecutionsDto.class);
        Mockito.when(execSummary.getLatestVariations()).thenReturn(List.of(var1));
        Mockito.when(dataService.getBatchLatestTestExecutionsSummary(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(execSummary);

        final Model model = new ConcurrentModel();
        final String result = controller.combineWidgets("w-target", "w-source", "true", "/batch-history", null, model);

        Mockito.verify(settingService, Mockito.never()).combineWidgets(Mockito.anyString(), Mockito.anyString());
        Assertions.assertNotNull(model.getAttribute("widgetWarningMessage"));
        Assertions.assertTrue(model.getAttribute("widgetWarningMessage").toString().contains("different contributing runs"));
        Assertions.assertNotNull(result);
    }

    @Test
    public void testCombineWidgets_WhenDifferentExecutionIdentities_SetsWarningMessage()
    {
        final DashboardWidgetConfigDto w1 = new DashboardWidgetConfigDto(
            "w-target", "Status Donut", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0, false, true, List.of("environment"), false, null
        );
        final DashboardWidgetConfigDto w2 = new DashboardWidgetConfigDto(
            "w-source", "Browser Ratio", WidgetMetric.TAG, "browser",
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 1, false, true, List.of("locale"), false, null
        );
        Mockito.when(settingService.getDashboardWidgets()).thenReturn(List.of(w1, w2));
        Mockito.when(settingService.getExecutionIdentityDimensions()).thenReturn(List.of("browser"));

        final Model model = new ConcurrentModel();
        final String result = controller.combineWidgets("w-target", "w-source", "true", "/batch-history", null, model);

        Mockito.verify(settingService, Mockito.never()).combineWidgets(Mockito.anyString(), Mockito.anyString());
        Assertions.assertNotNull(model.getAttribute("widgetWarningMessage"));
        Assertions.assertTrue(model.getAttribute("widgetWarningMessage").toString().contains("different execution identities"));
        Assertions.assertNotNull(result);
    }

    @Test
    public void testCombineWidgets_WhenMatchingCustomExecutionIdentities_CombinesSuccessfully()
    {
        final DashboardWidgetConfigDto w1 = new DashboardWidgetConfigDto(
            "w-target", "Status Donut", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0, false, true, List.of("environment"), false, null
        );
        final DashboardWidgetConfigDto w2 = new DashboardWidgetConfigDto(
            "w-source", "Browser Ratio", WidgetMetric.TAG, "browser",
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 1, false, true, List.of("environment"), false, null
        );
        Mockito.when(settingService.getDashboardWidgets()).thenReturn(List.of(w1, w2));
        Mockito.when(settingService.getExecutionIdentityDimensions()).thenReturn(List.of("browser"));

        final TestRunEntity run = new TestRunEntity("run-1", "DefaultBatch", "COMPLETED", "CI", "staging", "en-US", "Chrome", "2026-11-01 10:00", 1730455200000L);
        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of(run));

        final BatchLatestVariationDto var1 = new BatchLatestVariationDto("k1", "passed-clean", false, "JAVA", "run-1", "Default", "Chrome", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L);
        final BatchLatestExecutionsDto execSummary = Mockito.mock(BatchLatestExecutionsDto.class);
        Mockito.when(execSummary.getLatestVariations()).thenReturn(List.of(var1));
        Mockito.when(dataService.getBatchLatestTestExecutionsSummary(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(execSummary);

        final Model model = new ConcurrentModel();
        final String result = controller.combineWidgets("w-target", "w-source", "true", "/batch-history", null, model);

        Mockito.verify(settingService).combineWidgets("w-target", "w-source");
        Assertions.assertNull(model.getAttribute("widgetWarningMessage"));
        Assertions.assertNotNull(result);
    }

    @Test
    public void testSeparateContainer_InvokesSettingService()
    {
        final Model model = new ConcurrentModel();
        final String result = controller.separateContainer("container-1", "true", "/batch-history", null, model);

        Mockito.verify(settingService).separateContainer("container-1");
        Assertions.assertNotNull(result);
    }

    @Test
    public void testRenameWidget_InvokesSettingServiceDefaultPage()
    {
        final Model model = new ConcurrentModel();
        final String result = controller.renameWidget("widget-1", "New Widget Name", null, "true", "/batch-history", null, model);

        Mockito.verify(settingService).renameWidget("widget-1", "New Widget Name");
        Assertions.assertNotNull(result);
    }

    @Test
    public void testRenameWidget_InvokesSettingServiceCustomPage()
    {
        final Model model = new ConcurrentModel();
        final String result = controller.renameWidget("widget-2", "Custom Group Name", "page-123", "true", "/dashboard-pages/page-123", null, model);

        Mockito.verify(settingService).renameWidget("page-123", "widget-2", "Custom Group Name");
        Assertions.assertNotNull(result);
    }

    @Test
    public void testCreateCustomDashboardPage_RedirectsToNewPage()
    {
        final DashboardPageConfigDto createdPage = new DashboardPageConfigDto(
            "page-123", "Executive Dashboard", Collections.emptyList(), 0, System.currentTimeMillis()
        );
        Mockito.when(settingService.createCustomPage("Executive Dashboard")).thenReturn(createdPage);

        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        final Model model = new ConcurrentModel();

        final Object result = controller.createCustomDashboardPage("Executive Dashboard", "true", null, response, model);

        Mockito.verify(settingService).createCustomPage("Executive Dashboard");
        Mockito.verify(response).setHeader("HX-Redirect", "/dashboard-pages/page-123");
        Assertions.assertEquals("redirect:/dashboard-pages/page-123", result);
    }

    @Test
    public void testCreateCustomDashboardPage_JsonResponse()
    {
        final DashboardPageConfigDto createdPage = new DashboardPageConfigDto(
            "page-456", "API Dashboard", Collections.emptyList(), 0, System.currentTimeMillis()
        );
        Mockito.when(settingService.createCustomPage("API Dashboard")).thenReturn(createdPage);

        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        final Model model = new ConcurrentModel();

        final Object result = controller.createCustomDashboardPage("API Dashboard", null, "application/json", response, model);

        Assertions.assertInstanceOf(ResponseEntity.class, result);
        final ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
        Assertions.assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
        Assertions.assertNotNull(responseEntity.getBody());
    }

    @Test
    public void testGetCustomDashboardPage_EmptyPage_RendersEmptyFragment()
    {
        final DashboardPageConfigDto emptyPage = new DashboardPageConfigDto(
            "page-empty", "Empty Page", Collections.emptyList(), 0, System.currentTimeMillis()
        );
        Mockito.when(settingService.getCustomPage("page-empty")).thenReturn(emptyPage);

        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        final Model model = new ConcurrentModel();
        final String view = controller.getCustomDashboardPage("page-empty", "true", response, model);

        Assertions.assertEquals("fragments/custom-dashboard-page :: customDashboardPage", view);
        Assertions.assertEquals(emptyPage, model.getAttribute("customPage"));
        Assertions.assertEquals("Empty Page", model.getAttribute("pageTitle"));
        Assertions.assertEquals("page-empty", model.getAttribute("activeCustomPageId"));
        Assertions.assertEquals(true, model.getAttribute("isHxRequest"));
        Assertions.assertNotNull(model.getAttribute("customPages"));
        Mockito.verify(response).setHeader("HX-Push-Url", "/dashboard-pages/page-empty");

        final Object widgets = model.getAttribute("dashboardWidgets");
        Assertions.assertNotNull(widgets);
        Assertions.assertInstanceOf(List.class, widgets);
        Assertions.assertTrue(((List<?>) widgets).isEmpty(), "Empty page must have empty dashboardWidgets list");
    }

    @Test
    public void testDeleteCustomDashboardPage_RedirectsToHome()
    {
        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        final String result = controller.deleteCustomDashboardPage("page-del", "true", response);

        Mockito.verify(settingService).deleteCustomPage("page-del");
        Mockito.verify(response).setHeader("HX-Redirect", "/");
        Assertions.assertEquals("redirect:/", result);
    }

    @Test
    public void testDeleteCustomDashboardPage_JsonResponse()
    {
        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        final Object result = controller.deleteCustomDashboardPage("page-del-json", null, "application/json", response);

        Mockito.verify(settingService).deleteCustomPage("page-del-json");
        Assertions.assertInstanceOf(ResponseEntity.class, result);
        final ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
        Assertions.assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
        Assertions.assertNotNull(responseEntity.getBody());
    }

    @Test
    public void testCreateCustomDashboardPage_WithIdentityAndRunFilters_PassesToService()
    {
        final DashboardPageConfigDto createdPage = new DashboardPageConfigDto(
            "page-custom-filters", "Release Dashboard", Collections.emptyList(), 0, System.currentTimeMillis()
        );
        Mockito.when(settingService.createCustomPage(
            Mockito.eq("Release Dashboard"),
            Mockito.eq(List.of("browser", "locale")),
            Mockito.any(WidgetFilterConfigDto.class)
        )).thenReturn(createdPage);

        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        final Model model = new ConcurrentModel();

        final Object result = controller.createCustomDashboardPage(
            "Release Dashboard",
            "browser,locale",
            10,
            "2026-10-01",
            "2026-10-31",
            "Chrome",
            null,
            "en_US",
            null,
            "Prod",
            null,
            "@Browsing",
            null,
            "smoke",
            null,
            null,
            "true",
            null,
            response,
            model
        );

        Mockito.verify(settingService).createCustomPage(
            Mockito.eq("Release Dashboard"),
            Mockito.eq(List.of("browser", "locale")),
            Mockito.any(WidgetFilterConfigDto.class)
        );
        Mockito.verify(response).setHeader("HX-Redirect", "/dashboard-pages/page-custom-filters");
        Assertions.assertEquals("redirect:/dashboard-pages/page-custom-filters", result);
    }

    @Test
    public void testGetCustomDashboardPage_WithPageRunFilters_PopulatesModel()
    {
        final WidgetFilterConfigDto runFilters = new WidgetFilterConfigDto(
            Set.of("Chrome"), Collections.emptySet(),
            Set.of("en_US"), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptyMap(), Collections.emptyMap(),
            5, null, null
        );
        final DashboardPageConfigDto filteredPage = new DashboardPageConfigDto(
            "page-scoped",
            "Scoped Page",
            Collections.emptyList(),
            0,
            System.currentTimeMillis(),
            List.of("browser", "environment"),
            runFilters
        );
        Mockito.when(settingService.getCustomPage("page-scoped")).thenReturn(filteredPage);

        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        final Model model = new ConcurrentModel();
        final String view = controller.getCustomDashboardPage("page-scoped", "true", response, model);

        Assertions.assertEquals("fragments/custom-dashboard-page :: customDashboardPage", view);
        Assertions.assertEquals(filteredPage, model.getAttribute("customPage"));
        Assertions.assertEquals("Scoped Page", model.getAttribute("pageTitle"));
        Assertions.assertEquals("page-scoped", model.getAttribute("activeCustomPageId"));
        Assertions.assertNotNull(model.getAttribute("candidateBrowsers"));
        Assertions.assertNotNull(model.getAttribute("candidateLocales"));
        Assertions.assertNotNull(model.getAttribute("availableIdentityTags"));
    }

    @Test
    public void testGetCustomDashboardPage_PassesPageIdentityAndFiltersToDataService()
    {
        final WidgetFilterConfigDto runFilters = new WidgetFilterConfigDto(
            Set.of("Chrome"), Collections.emptySet(),
            Set.of("en_US"), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptyMap(), Collections.emptyMap(),
            5, "2026-10-01", "2026-10-31"
        );
        final DashboardWidgetConfigDto widget = new DashboardWidgetConfigDto(
            "w-status", "Status", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH,
            WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0, false, false, Collections.emptyList(), false, null
        );
        final DashboardPageConfigDto page = new DashboardPageConfigDto(
            "page-with-widgets",
            "Filtered Page",
            List.of(widget),
            0,
            System.currentTimeMillis(),
            List.of("browser", "environment"),
            runFilters
        );
        Mockito.when(settingService.getCustomPage("page-with-widgets")).thenReturn(page);

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-1");
        r1.setTotalTests(10);
        r1.setStartTimeMs(1728000000000L);
        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of(r1));
        Mockito.when(dataService.scopeRunToCriteria(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(r1);

        final BatchLatestExecutionsDto mockExec = Mockito.mock(BatchLatestExecutionsDto.class);
        Mockito.when(mockExec.getLatestVariations()).thenReturn(Collections.emptyList());
        Mockito.when(mockExec.getBrowserRatios()).thenReturn(Collections.emptyList());
        Mockito.when(dataService.getBatchLatestTestExecutionsSummary(
            Mockito.eq("all"),
            Mockito.anyList(),
            Mockito.anySet(),
            Mockito.any(),
            Mockito.any(),
            Mockito.eq(Set.of("Chrome")),
            Mockito.anySet(),
            Mockito.eq(Set.of("en_US")),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anyMap(),
            Mockito.anyMap(),
            Mockito.anyMap(),
            Mockito.eq(List.of("browser", "environment")),
            Mockito.isNull()
        )).thenReturn(mockExec);

        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        final Model model = new ConcurrentModel();
        final String view = controller.getCustomDashboardPage("page-with-widgets", "true", response, model);

        Assertions.assertEquals("fragments/custom-dashboard-page :: customDashboardPage", view);
        Mockito.verify(dataService).getBatchLatestTestExecutionsSummary(
            Mockito.eq("all"),
            Mockito.anyList(),
            Mockito.anySet(),
            Mockito.any(),
            Mockito.any(),
            Mockito.eq(Set.of("Chrome")),
            Mockito.anySet(),
            Mockito.eq(Set.of("en_US")),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anyMap(),
            Mockito.anyMap(),
            Mockito.anyMap(),
            Mockito.eq(List.of("browser", "environment")),
            Mockito.isNull()
        );
    }

    @Test
    public void testGetWidgetExecutions_InheritsPageIdentityAndFiltersWhenNoWidgetOverride()
    {
        final WidgetFilterConfigDto runFilters = new WidgetFilterConfigDto(
            Set.of("Firefox"), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptyMap(), Collections.emptyMap(),
            3, null, null
        );
        final DashboardWidgetConfigDto widget = new DashboardWidgetConfigDto(
            "w-inherit", "Inheriting Widget", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH,
            WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0, false, false, Collections.emptyList(), false, null
        );
        final DashboardPageConfigDto page = new DashboardPageConfigDto(
            "page-inherit",
            "Inherit Page",
            List.of(widget),
            0,
            System.currentTimeMillis(),
            List.of("browser", "environment"),
            runFilters
        );
        Mockito.when(settingService.getCustomPage("page-inherit")).thenReturn(page);
        Mockito.when(settingService.getWidgetsForPage("page-inherit")).thenReturn(List.of(widget));

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-ff");
        r1.setTotalTests(5);
        r1.setStartTimeMs(1728000000000L);
        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of(r1));
        Mockito.when(dataService.scopeRunToCriteria(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(r1);

        final BatchLatestExecutionsDto mockExec = Mockito.mock(BatchLatestExecutionsDto.class);
        Mockito.when(mockExec.getLatestVariations()).thenReturn(Collections.emptyList());
        Mockito.when(dataService.getBatchLatestTestExecutionsSummary(
            Mockito.any(),
            Mockito.anyList(),
            Mockito.anySet(),
            Mockito.any(),
            Mockito.any(),
            Mockito.eq(Set.of("Firefox")),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anyMap(),
            Mockito.anyMap(),
            Mockito.anyMap(),
            Mockito.eq(List.of("browser", "environment"))
        )).thenReturn(mockExec);

        final Model model = new ConcurrentModel();
        final String view = controller.getWidgetExecutions("w-inherit", null, "/dashboard-pages/page-inherit", null, model);

        Assertions.assertEquals("fragments/widget-executions-modal :: widgetExecutionsModal", view);
        Mockito.verify(dataService).getBatchLatestTestExecutionsSummary(
            Mockito.any(),
            Mockito.anyList(),
            Mockito.anySet(),
            Mockito.any(),
            Mockito.any(),
            Mockito.eq(Set.of("Firefox")),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anySet(),
            Mockito.anyMap(),
            Mockito.anyMap(),
            Mockito.anyMap(),
            Mockito.eq(List.of("browser", "environment"))
        );
    }

    @Test
    public void testSaveWidget_KnownTestVariations()
    {
        final Model model = new ConcurrentModel();
        final String result = controller.saveWidget(
            "w-variations-1", "Known Test Variations", "TEST_VARIATIONS", null, null, null, null, null, "half",
            "true", "/batch-history", null, model
        );

        Mockito.verify(settingService).addOrUpdateWidget(Mockito.argThat(w ->
            "w-variations-1".equals(w.getId())
            && WidgetMetric.TEST_VARIATIONS == w.getMetric()
            && WidgetPresentation.TEST_VARIATIONS == w.getPresentation()
            && "full".equals(w.getWidth())
        ));
        Assertions.assertNotNull(result);
    }

    @Test
    public void testWidgetConfiguratorModal_TestVariationsRendering()
    {
        final SpringTemplateEngine engine = new SpringTemplateEngine();
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        engine.setTemplateResolver(resolver);

        final StandardLinkBuilder linkBuilder = new StandardLinkBuilder()
        {
            @Override
            protected String computeContextPath(
                final IExpressionContext expressionContext,
                final String base,
                final Map<String, Object> parameters)
            {
                return "";
            }
        };
        engine.setLinkBuilder(linkBuilder);

        final DashboardWidgetConfigDto widget = new DashboardWidgetConfigDto(
            "w-var", "Known Test Base Variations", WidgetMetric.TEST_VARIATIONS, null,
            WidgetPresentation.TEST_VARIATIONS, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "full", 0
        );

        final Context context = new Context(Locale.US);
        context.setVariable("widget", widget);
        context.setVariable("isOpen", true);
        context.setVariable("availableWidgetTags", List.of("Browser", "Locale"));
        context.setVariable("candidateBrowsers", List.of("Chrome"));
        context.setVariable("candidateLocales", List.of("en-US"));
        context.setVariable("candidateEnvs", List.of("Default"));
        context.setVariable("candidateAreas", List.of("@Browsing"));
        context.setVariable("availableIdentityTags", List.of("Browser", "Environment"));
        context.setVariable("dashboardWidgets", List.of(widget));

        final String html = engine.process("fragments/widget-configurator-modal", Set.of("widgetConfiguratorModal"), context);
        Assertions.assertNotNull(html);
        Assertions.assertTrue(html.contains("Test Variations"));
        Assertions.assertTrue(html.contains("id=\"widgetFixedPresentationInput\""));
        Assertions.assertTrue(html.contains("Known Test Base Variations &amp; Datasets") || html.contains("Known Test Base Variations & Datasets"));
    }

    @Test
    public void testDashboardWidgets_TestVariationsRendering()
    {
        final SpringTemplateEngine engine = new SpringTemplateEngine();
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        engine.setTemplateResolver(resolver);

        final StandardLinkBuilder linkBuilder = new StandardLinkBuilder()
        {
            @Override
            protected String computeContextPath(
                final IExpressionContext expressionContext,
                final String base,
                final Map<String, Object> parameters)
            {
                return "";
            }
        };
        engine.setLinkBuilder(linkBuilder);

        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-test-var", "Known Test Base Variations", WidgetMetric.TEST_VARIATIONS, null,
            WidgetPresentation.TEST_VARIATIONS, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "full", 0, false, true, List.of("browser", "environment"), false, null
        );

        final TestBaseVariationEntity variation = new TestBaseVariationEntity();
        variation.setId("101");
        variation.setTestClassName("CheckoutProcessTest");
        variation.setTestMethodName("testOrderPlacement");
        variation.setDataSetLabel("Default");
        variation.setLocation("US");
        variation.setBrowser("Chrome");
        variation.setExecutionMode("PLAYBOOK");
        variation.setLastStatus("passed-clean");
        variation.setTotalExecutionsCount(3);
        variation.setIdentityProperties(Map.of("environment", "Production"));

        final TestBaseClassDto classDto = new TestBaseClassDto("CheckoutProcessTest", "class-container-1", 1, List.of(variation));
        final TestBaseAreaDto areaDto = new TestBaseAreaDto("@Browsing", "area-pane-1", "tab-1", "folder_open", 1, List.of(classDto));

        final DashboardWidgetViewDto view = DashboardWidgetViewDto.createTestVariationsView(
            config, List.of(areaDto), List.of(classDto), 1, List.of("Environment"), true, Set.of("run-1")
        );

        final Context context = new Context(Locale.US);
        context.setVariable("dashboardWidgets", List.of(view));
        context.setVariable("isCustomWidgetsConfigured", true);

        final String html = engine.process("fragments/dashboard-widgets", Set.of("dashboardWidgetsSection"), context);
        Assertions.assertNotNull(html);
        Assertions.assertTrue(html.contains("widget-test-variations-card"));
        Assertions.assertTrue(html.contains("collapsible-panel"), "Widget card must have collapsible-panel class");
        Assertions.assertTrue(html.contains("widget-test-variations-card collapsible-panel"), "Widget card must start collapsed by default without 'expanded' class");
        Assertions.assertFalse(html.contains("collapsible-panel expanded"), "Widget card must not have 'expanded' class by default");
        Assertions.assertTrue(html.contains("toggleBatchVariationsPanel(this)"), "Widget header must trigger toggleBatchVariationsPanel on click");
        Assertions.assertTrue(html.contains("panel-collapse-chevron"), "Widget header must render collapse indicator chevron");
        Assertions.assertTrue(html.contains("flex-column"), "Body must use flex-column to stack categories vertically");
        Assertions.assertTrue(html.contains("class=\"area-group test-row-group\""), "Area group must start collapsed by default without 'expanded' class");
        Assertions.assertTrue(html.contains("class=\"test-class-container tb-class-item\""), "Test class container must start collapsed by default without 'expanded' class");
        Assertions.assertFalse(html.contains("class=\"area-group test-row-group expanded\""), "Area group must not have 'expanded' class by default");
        Assertions.assertTrue(html.contains("Area: <span"));
        Assertions.assertTrue(html.contains("@Browsing"));
        Assertions.assertTrue(html.contains("CheckoutProcessTest"));
        Assertions.assertTrue(html.contains("testOrderPlacement"));
        Assertions.assertTrue(html.contains("Production"));
        Assertions.assertTrue(html.contains("PASSED"));
    }

    @Test
    public void testCreateCustomDashboardPage_GroupDashboard_CallsCreateGroupDashboard()
    {
        final DashboardPageConfigDto createdGroup = new DashboardPageConfigDto(
            "group-123", "Browser Matrix", Collections.emptyList(), 0, System.currentTimeMillis(),
            List.of("browser"), null, null, true, "browser", null, WidgetPointScope.LAST_RUN, Collections.emptyList()
        );
        Mockito.when(settingService.createGroupDashboard(
            Mockito.eq("Browser Matrix"),
            Mockito.eq("browser"),
            Mockito.eq(WidgetPointScope.LAST_RUN),
            Mockito.anyList(),
            Mockito.any(),
            Mockito.anyList()
        )).thenReturn(createdGroup);

        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        final Model model = new ConcurrentModel();

        final Object result = controller.createCustomDashboardPage(
            "Browser Matrix",
            "group",
            "browser",
            "LAST_RUN",
            "browser",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            Collections.emptyMap(),
            "true",
            null,
            response,
            model
        );

        Mockito.verify(settingService).createGroupDashboard(
            Mockito.eq("Browser Matrix"),
            Mockito.eq("browser"),
            Mockito.eq(WidgetPointScope.LAST_RUN),
            Mockito.anyList(),
            Mockito.any(),
            Mockito.anyList()
        );
        Mockito.verify(response).setHeader("HX-Redirect", "/dashboard-pages/group-123");
        Assertions.assertEquals("redirect:/dashboard-pages/group-123", result);
    }

    @Test
    public void testGetCustomDashboardPage_GroupParent_PopulatesGroupOverviewAndUrls()
    {
        final DashboardPageConfigDto groupParent = new DashboardPageConfigDto(
            "group-parent-1", "Browsers", Collections.emptyList(), 0, System.currentTimeMillis(),
            List.of("browser"), null, null, true, "browser", null, WidgetPointScope.LATEST_TEST_STATUS, Collections.emptyList()
        );
        Mockito.when(settingService.getCustomPage("group-parent-1")).thenReturn(groupParent);

        final BatchSummaryDto b1 = new BatchSummaryDto("b-1", "Chrome", "Default", "", "", "100%", "0", Collections.emptyList(), List.of("Chrome"));
        final BatchOverviewDataDto overviewData = new BatchOverviewDataDto(List.of(b1), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        Mockito.when(dataService.getGroupOverviewData(
            Mockito.eq(List.of("browser")), Mockito.anyList(), Mockito.isNull(), Mockito.isNull(), Mockito.anyMap(), Mockito.eq(WidgetPointScope.LATEST_TEST_STATUS)
        )).thenReturn(overviewData);

        final DashboardPageConfigDto subPage = new DashboardPageConfigDto(
            "sub-chrome-1", "Chrome", Collections.emptyList(), 0, System.currentTimeMillis(),
            List.of("browser"), null, "group-parent-1", false, "browser", "Chrome", WidgetPointScope.LATEST_TEST_STATUS, Collections.emptyList()
        );
        Mockito.when(settingService.ensureSubDashboardForGroup(
            "group-parent-1", "browser", "Chrome", WidgetPointScope.LATEST_TEST_STATUS
        )).thenReturn(subPage);

        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        final Model model = new ConcurrentModel();
        final String view = controller.getCustomDashboardPage("group-parent-1", "true", response, model);

        Assertions.assertEquals("fragments/custom-dashboard-page :: customDashboardPage", view);
        Assertions.assertEquals(true, model.getAttribute("isGroupDashboard"));
        Assertions.assertEquals("browser", model.getAttribute("groupType"));
        Assertions.assertEquals(WidgetPointScope.LATEST_TEST_STATUS, model.getAttribute("groupCardScope"));
        final Object batches = model.getAttribute("groupBatches");
        Assertions.assertNotNull(batches);
        Assertions.assertEquals(1, ((List<?>) batches).size());

        @SuppressWarnings("unchecked")
        final Map<String, String> urls = (Map<String, String>) model.getAttribute("groupDashboardUrls");
        Assertions.assertNotNull(urls);
        Assertions.assertEquals("/dashboard-pages/sub-chrome-1", urls.get("Chrome"));
    }

    @Test
    public void testGetCustomDashboardPage_SubPage_PopulatesParentDashboard()
    {
        final DashboardPageConfigDto groupParent = new DashboardPageConfigDto(
            "group-parent-1", "Browsers", Collections.emptyList(), 0, System.currentTimeMillis(),
            List.of("browser"), null, null, true, "browser", null, WidgetPointScope.LATEST_TEST_STATUS, Collections.emptyList()
        );
        final DashboardPageConfigDto subPage = new DashboardPageConfigDto(
            "sub-chrome-1", "Chrome", Collections.emptyList(), 0, System.currentTimeMillis(),
            List.of("browser"), null, "group-parent-1", false, "browser", "Chrome", WidgetPointScope.LATEST_TEST_STATUS, Collections.emptyList()
        );
        Mockito.when(settingService.getCustomPage("sub-chrome-1")).thenReturn(subPage);
        Mockito.when(settingService.getCustomPage("group-parent-1")).thenReturn(groupParent);

        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        final Model model = new ConcurrentModel();
        final String view = controller.getCustomDashboardPage("sub-chrome-1", "true", response, model);

        Assertions.assertEquals("fragments/custom-dashboard-page :: customDashboardPage", view);
        Assertions.assertEquals(true, model.getAttribute("isSubPage"));
        Assertions.assertEquals(groupParent, model.getAttribute("parentDashboard"));
        Assertions.assertEquals(false, model.getAttribute("isGroupDashboard"));
    }

    @Test
    public void testSanitizeWidgetTitle()
    {
        Assertions.assertNull(AuraReportViewController.sanitizeWidgetTitle(null));
        Assertions.assertNull(AuraReportViewController.sanitizeWidgetTitle(""));
        Assertions.assertNull(AuraReportViewController.sanitizeWidgetTitle("   "));
        Assertions.assertNull(AuraReportViewController.sanitizeWidgetTitle(", ,,"));

        Assertions.assertEquals("Production Pass Rate",
            AuraReportViewController.sanitizeWidgetTitle("Production Pass Rate"));
        Assertions.assertEquals("Status Pie Chart",
            AuraReportViewController.sanitizeWidgetTitle("Status Pie Chart,Status Pie Chart"));
        Assertions.assertEquals("Status Pie Chart",
            AuraReportViewController.sanitizeWidgetTitle("Status Pie Chart, Status Pie Chart, Status Pie Chart"));
        Assertions.assertEquals("Custom Title",
            AuraReportViewController.sanitizeWidgetTitle("Custom Title,Status Pie Chart"));
        Assertions.assertEquals("Custom Title",
            AuraReportViewController.sanitizeWidgetTitle("Custom Title, Status Pie Chart, Status Pie Chart"));
    }

    @Test
    public void testSaveWidgetSanitizesCommaAppendedTitles()
    {
        final Model model = new ConcurrentModel();
        controller.saveWidget(
            "w-save-test-1",
            "Custom Title,Status Pie Chart",
            "STATUS",
            null,
            "PIE_CHART",
            "LATEST_APPROACH",
            "PERCENT",
            "LATEST_TEST_STATUS",
            "half",
            "true",
            "/batch-history",
            null,
            model
        );

        final org.mockito.ArgumentCaptor<DashboardWidgetConfigDto> captor = org.mockito.ArgumentCaptor.forClass(DashboardWidgetConfigDto.class);
        Mockito.verify(settingService, Mockito.atLeastOnce()).addOrUpdateWidget(captor.capture());
        final DashboardWidgetConfigDto captured = captor.getValue();
        Assertions.assertNotNull(captured);
        Assertions.assertEquals("Custom Title", captured.getTitle(), "Title should have duplicate appended token stripped");
    }

    @Test
    public void testGetWidgetConfigModal_IsNewWidgetFlag()
    {
        final Model modelNew = new ConcurrentModel();
        controller.getWidgetConfigModal(null, modelNew);
        Assertions.assertEquals(Boolean.TRUE, modelNew.getAttribute("isNewWidget"));

        final DashboardWidgetConfigDto existing = new DashboardWidgetConfigDto(
            "w-exist-1", "Existing Title", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0, false, false, Collections.emptyList(), false, null
        );
        Mockito.when(settingService.getDashboardWidgets()).thenReturn(List.of(existing));

        final Model modelEdit = new ConcurrentModel();
        controller.getWidgetConfigModal("w-exist-1", modelEdit);
        Assertions.assertEquals(Boolean.FALSE, modelEdit.getAttribute("isNewWidget"));
    }

    @Test
    public void testConvertCustomDashboardPageToGroup()
    {
        final DashboardPageConfigDto existing = new DashboardPageConfigDto(
            "sub-1", "TEST", Collections.emptyList(), 1, System.currentTimeMillis(),
            List.of("browser"), null, "parent-1", false, "batch", "TEST", WidgetPointScope.LATEST_TEST_STATUS, Collections.emptyList()
        );
        Mockito.when(settingService.getCustomPage("sub-1")).thenReturn(existing);

        final DashboardPageConfigDto converted = new DashboardPageConfigDto(
            "sub-1", "TEST", Collections.emptyList(), 1, System.currentTimeMillis(),
            List.of("browser"), null, "parent-1", true, "browser", "TEST", WidgetPointScope.LAST_RUN, Collections.emptyList()
        );
        Mockito.when(settingService.convertToGroupDashboard(
            Mockito.eq("sub-1"),
            Mockito.eq("browser"),
            Mockito.eq(WidgetPointScope.LAST_RUN),
            Mockito.anyList()
        )).thenReturn(converted);

        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        final Object result = controller.convertCustomDashboardPageToGroup(
            "sub-1", "browser", "LAST_RUN", "true", null, response
        );

        Mockito.verify(settingService).convertToGroupDashboard(
            Mockito.eq("sub-1"),
            Mockito.eq("browser"),
            Mockito.eq(WidgetPointScope.LAST_RUN),
            Mockito.anyList()
        );
        Mockito.verify(response).setHeader("HX-Redirect", "/dashboard-pages/sub-1");
        Assertions.assertEquals("redirect:/dashboard-pages/sub-1", result);
    }

    @Test
    public void testConvertCustomDashboardPageToGroupRejectsProhibitedDimension()
    {
        final DashboardPageConfigDto existing = new DashboardPageConfigDto(
            "sub-1", "TEST", Collections.emptyList(), 1, System.currentTimeMillis(),
            List.of("browser"), null, "parent-1", false, "batch", "TEST", WidgetPointScope.LATEST_TEST_STATUS, Collections.emptyList()
        );
        Mockito.when(settingService.getCustomPage("sub-1")).thenReturn(existing);
        Mockito.when(settingService.getProhibitedGroupDimensions("sub-1")).thenReturn(Set.of("batch"));

        final HttpServletResponse responseJson = Mockito.mock(HttpServletResponse.class);
        final Object resultJson = controller.convertCustomDashboardPageToGroup(
            "sub-1", "batch", "LATEST_TEST_STATUS", "true", "application/json", responseJson
        );

        Assertions.assertInstanceOf(ResponseEntity.class, resultJson);
        final ResponseEntity<?> responseEntity = (ResponseEntity<?>) resultJson;
        Assertions.assertEquals(400, responseEntity.getStatusCode().value());
        Assertions.assertTrue(responseEntity.getBody().toString().contains("Cannot group by 'batch'"));

        final HttpServletResponse responseHtml = Mockito.mock(HttpServletResponse.class);
        final Object resultHtml = controller.convertCustomDashboardPageToGroup(
            "sub-1", "batch", "LATEST_TEST_STATUS", "false", "text/html", responseHtml
        );
        Assertions.assertEquals("redirect:/dashboard-pages/sub-1?error=prohibited_dimension", resultHtml);
    }

    @Test
    public void testCreateCustomDashboardPageWithMultipleDimensions()
    {
        final DashboardPageConfigDto created = new DashboardPageConfigDto(
            "page-multi-1", "Multi Group", Collections.emptyList(), 0, System.currentTimeMillis(),
            List.of("browser"), null, null, true, "batch,browser", null, WidgetPointScope.LATEST_TEST_STATUS, Collections.emptyList()
        );

        Mockito.when(settingService.createGroupDashboard(
            Mockito.eq("Multi Group"),
            Mockito.eq("batch,browser"),
            Mockito.eq(WidgetPointScope.LATEST_TEST_STATUS),
            Mockito.anyList(),
            Mockito.any(),
            Mockito.anyList()
        )).thenReturn(created);

        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        final Model model = Mockito.mock(Model.class);

        final Object result = controller.createCustomDashboardPage(
            "Multi Group", "group", "batch,browser", "LATEST_TEST_STATUS",
            "browser", null, null, null, null, null, null, null, null, null,
            null, null, null, null, Collections.emptyMap(), "false", "text/html", response, model
        );

        Mockito.verify(settingService).createGroupDashboard(
            Mockito.eq("Multi Group"),
            Mockito.eq("batch,browser"),
            Mockito.eq(WidgetPointScope.LATEST_TEST_STATUS),
            Mockito.anyList(),
            Mockito.any(),
            Mockito.anyList()
        );
        Assertions.assertEquals("redirect:/dashboard-pages/page-multi-1", result);
    }
}

