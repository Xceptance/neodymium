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
import com.xceptance.aura.report.dto.RunReportDto;
import com.xceptance.aura.report.dto.TestBaseDataDto;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.repository.TestBaseBugRepository;
import com.xceptance.aura.report.repository.TestBaseVariationRepository;
import com.xceptance.aura.report.repository.TestBatchRepository;
import com.xceptance.aura.report.repository.TestRunRepository;
import com.xceptance.aura.report.service.AuraReportDataService;
import com.xceptance.aura.report.service.RunStorageSyncService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

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
    private AuraReportViewController controller;

    @BeforeEach
    public void setUp()
    {
        dataService = Mockito.mock(AuraReportDataService.class);
        syncService = Mockito.mock(RunStorageSyncService.class);
        runRepository = Mockito.mock(TestRunRepository.class);
        batchRepository = Mockito.mock(TestBatchRepository.class);
        variationRepository = Mockito.mock(TestBaseVariationRepository.class);
        bugRepository = Mockito.mock(TestBaseBugRepository.class);

        controller = new AuraReportViewController(
            dataService,
            syncService,
            runRepository,
            batchRepository,
            variationRepository,
            bugRepository
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
}
