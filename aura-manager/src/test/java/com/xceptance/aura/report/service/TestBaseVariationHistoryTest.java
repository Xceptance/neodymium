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
package com.xceptance.aura.report.service;

import com.xceptance.aura.report.dto.BatchLatestExecutionsDto;
import com.xceptance.aura.report.dto.BatchLatestVariationDto;
import com.xceptance.aura.report.dto.BatchOverviewDataDto;
import com.xceptance.aura.report.dto.BatchSuiteEvolutionDto;
import com.xceptance.aura.report.dto.BatchSuiteEvolutionPointDto;
import com.xceptance.aura.report.dto.BatchSummaryDto;
import com.xceptance.aura.report.dto.DistributionRatioDto;
import com.xceptance.aura.report.dto.TestBaseAreaDto;
import com.xceptance.aura.report.dto.TestBaseClassDto;
import com.xceptance.aura.report.dto.TestBaseDataDto;
import com.xceptance.aura.report.dto.TestBaseVariationHistoryDto;
import com.xceptance.aura.report.entity.TestBaseVariationEntity;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.entity.TestRunTagEntity;
import com.xceptance.aura.report.repository.TestBatchRepository;
import com.xceptance.aura.report.repository.TestBaseBugRepository;
import com.xceptance.aura.report.repository.TestBaseVariationRepository;
import com.xceptance.aura.report.repository.TestRunRepository;
import com.xceptance.aura.report.repository.TestRunTagRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for browser normalization and variation execution history retrieval.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public class TestBaseVariationHistoryTest
{
    private TestRunRepository runRepository;
    private TestBatchRepository batchRepository;
    private TestBaseVariationRepository variationRepository;
    private TestBaseBugRepository bugRepository;
    private TestRunTagRepository tagRepository;
    private LocalRunJsonStorageService storageService;
    private AttachmentStorageService attachmentStorageService;
    private AuraReportDataService dataService;

    @BeforeEach
    public void setUp()
    {
        runRepository = Mockito.mock(TestRunRepository.class);
        batchRepository = Mockito.mock(TestBatchRepository.class);
        variationRepository = Mockito.mock(TestBaseVariationRepository.class);
        bugRepository = Mockito.mock(TestBaseBugRepository.class);
        tagRepository = Mockito.mock(TestRunTagRepository.class);
        storageService = Mockito.mock(LocalRunJsonStorageService.class);
        attachmentStorageService = Mockito.mock(AttachmentStorageService.class);

        dataService = new AuraReportDataService(
            runRepository,
            batchRepository,
            variationRepository,
            bugRepository,
            tagRepository,
            storageService,
            attachmentStorageService
        );
    }

    @Test
    public void testNormalizeBrowser()
    {
        Assertions.assertEquals("Chrome_1500x1000", AuraReportDataService.normalizeBrowser("Chrome_1500x1000"));
        Assertions.assertEquals("Chrome", AuraReportDataService.normalizeBrowser("chrome"));
        Assertions.assertEquals("Chrome", AuraReportDataService.normalizeBrowser("Chrome"));
        Assertions.assertEquals("Chrome_headless", AuraReportDataService.normalizeBrowser("chrome_headless"));
        Assertions.assertEquals("Firefox", AuraReportDataService.normalizeBrowser("firefox"));
        Assertions.assertEquals("Ff_desktop", AuraReportDataService.normalizeBrowser("ff_desktop"));
        Assertions.assertEquals("Edge_headless", AuraReportDataService.normalizeBrowser("edge_headless"));
        Assertions.assertEquals("Safari", AuraReportDataService.normalizeBrowser("safari"));
        Assertions.assertEquals("Chrome", AuraReportDataService.normalizeBrowser(null));
        Assertions.assertEquals("Chrome", AuraReportDataService.normalizeBrowser("   "));
    }

    @Test
    public void testGetVariationHistoryMatchingBrowserProfiles()
    {
        final TestRunEntity run1 = new TestRunEntity("RUN_1", "Batch A", "COMPLETED", "Main", "Manual", "Java", "Chrome_1500x1000", "10:00", 1000L);
        final TestRunEntity run2 = new TestRunEntity("RUN_2", "Batch B", "COMPLETED", "Main", "Manual", "Java", "Chrome", "11:00", 2000L);

        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of(run2, run1));

        final String jsonRun1 = """
            {
              "executions": [
                {
                  "id": "e1",
                  "testClass": "HomepageTest",
                  "title": "canyon_homepage_US",
                  "location": "US",
                  "browser": "Chrome_1500x1000",
                  "status": "passed-clean",
                  "engine": "Java"
                }
              ]
            }
            """;

        final String jsonRun2 = """
            {
              "executions": [
                {
                  "id": "e2",
                  "testClass": "HomepageTest",
                  "title": "canyon_homepage_US",
                  "location": "US",
                  "browser": "chrome",
                  "status": "succeeded-fixed",
                  "engine": "Java"
                }
              ]
            }
            """;

        Mockito.when(storageService.readRunJson("RUN_1")).thenReturn(java.util.Optional.of(jsonRun1));
        Mockito.when(storageService.readRunJson("RUN_2")).thenReturn(java.util.Optional.of(jsonRun2));

        // Query history specifically for Chrome_1500x1000 -> should only match run1
        final List<TestBaseVariationHistoryDto> historyProfile = dataService.getVariationHistory(
            "HomepageTest",
            "canyon_homepage_US",
            "US",
            "Chrome_1500x1000"
        );

        Assertions.assertNotNull(historyProfile);
        Assertions.assertEquals(1, historyProfile.size());
        Assertions.assertEquals("RUN_1", historyProfile.get(0).getRunId());
        Assertions.assertEquals("e1", historyProfile.get(0).getExecutionId());
        Assertions.assertEquals("Batch A", historyProfile.get(0).getBatchName());

        // Query history specifically for generic Chrome -> should only match run2
        final List<TestBaseVariationHistoryDto> historyChrome = dataService.getVariationHistory(
            "HomepageTest",
            "canyon_homepage_US",
            "US",
            "Chrome"
        );

        Assertions.assertNotNull(historyChrome);
        Assertions.assertEquals(1, historyChrome.size());
        Assertions.assertEquals("RUN_2", historyChrome.get(0).getRunId());
        Assertions.assertEquals("e2", historyChrome.get(0).getExecutionId());
        Assertions.assertEquals("Batch B", historyChrome.get(0).getBatchName());
    }

    @Test
    public void testGetVariationHistoryFromRepository()
    {
        final String testClass = "CheckoutTest";
        final String dataSet = "Payment_VISA";
        final String location = "US";
        final String browser = "Chrome";
        final String varId = AuraReportDataService.generateVariationId(testClass, dataSet, location, browser);

        final TestBaseVariationEntity varEntity = new TestBaseVariationEntity(varId, testClass, dataSet, "@General", location, browser);
        final TestRunEntity runEntity = new TestRunEntity("RUN_100", "Nightly Regression", "COMPLETED", "Staging", "Manual", "Java", "Chrome", "Yesterday", 5000L);

        final String run100Json = """
            {
              "runId": "RUN_100",
              "batch": "Nightly Regression",
              "executions": [
                {
                  "id": "exec-100",
                  "testClass": "CheckoutTest",
                  "title": "Payment_VISA",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "passed-clean",
                  "bugs": ["BUG-1", "BUG-2"]
                }
              ]
            }
            """;
        Mockito.when(storageService.readRunJson("RUN_100")).thenReturn(Optional.of(run100Json));
        Mockito.when(variationRepository.findById(varId)).thenReturn(Optional.of(varEntity));
        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of(runEntity));

        final List<TestBaseVariationHistoryDto> history = dataService.getVariationHistory(testClass, dataSet, location, browser);

        Assertions.assertNotNull(history);
        Assertions.assertEquals(1, history.size());
        Assertions.assertEquals("RUN_100", history.get(0).getRunId());
        Assertions.assertEquals("exec-100", history.get(0).getExecutionId());
        Assertions.assertEquals("Nightly Regression", history.get(0).getBatchName());
        Assertions.assertEquals("SUCCEEDED-FIXED", history.get(0).getStatusLabel());
        Assertions.assertEquals(List.of("BUG-1", "BUG-2"), history.get(0).getBugs());
    }

    @Test
    public void testGetVariationHistoryFailedKnownWithoutBugsNormalizesToFailedUnknown()
    {
        final String testClass = "LoginTest";
        final String dataSet = "ValidUser";
        final String location = "US";
        final String browser = "Chrome";
        final String varId = AuraReportDataService.generateVariationId(testClass, dataSet, location, browser);

        final TestBaseVariationEntity varEntity = new TestBaseVariationEntity(varId, testClass, dataSet, "@General", location, browser);
        final TestRunEntity runEntity = new TestRunEntity("RUN_200", "Daily Build", "COMPLETED", "Staging", "Manual", "Java", "Chrome", "Today", 6000L);

        final String run200Json = """
            {
              "runId": "RUN_200",
              "batch": "Daily Build",
              "executions": [
                {
                  "id": "exec-200",
                  "testClass": "LoginTest",
                  "title": "ValidUser",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "failed-known",
                  "bugs": []
                }
              ]
            }
            """;
        Mockito.when(storageService.readRunJson("RUN_200")).thenReturn(Optional.of(run200Json));
        Mockito.when(variationRepository.findById(varId)).thenReturn(Optional.of(varEntity));
        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of(runEntity));

        final List<TestBaseVariationHistoryDto> history = dataService.getVariationHistory(testClass, dataSet, location, browser);

        Assertions.assertNotNull(history);
        Assertions.assertEquals(1, history.size());
        Assertions.assertEquals("RUN_200", history.get(0).getRunId());
        Assertions.assertEquals("failed-unknown", history.get(0).getStatus());
        Assertions.assertEquals("UNKNOWN FAIL", history.get(0).getStatusLabel());
        Assertions.assertTrue(history.get(0).getBugs().isEmpty());
    }

    @Test
    public void testTestMethodNameEntityPersistence()
    {
        final String testClass = "com.xceptance.aura.CheckoutTest";
        final String testMethod = "testGuestCheckoutPaymentBad";
        final String dataSet = "bad";
        final String location = "US";
        final String browser = "Chrome";
        final String varId = AuraReportDataService.generateVariationId(testClass, testMethod, dataSet, location, browser);

        final TestBaseVariationEntity varEntity = new TestBaseVariationEntity(varId, testClass, testMethod, dataSet, "@General", location, browser);

        Assertions.assertEquals(varId, varEntity.getId());
        Assertions.assertEquals(testClass, varEntity.getTestClassName());
        Assertions.assertEquals(testMethod, varEntity.getTestMethodName());
        Assertions.assertEquals(dataSet, varEntity.getDataSetLabel());
        Assertions.assertEquals(location, varEntity.getLocation());
        Assertions.assertEquals(browser, varEntity.getBrowser());

        varEntity.setTestMethodName("testGuestCheckoutPaymentUpdated");
        Assertions.assertEquals("testGuestCheckoutPaymentUpdated", varEntity.getTestMethodName());
    }

    @Test
    public void testGetVariationHistoryFiltersByTestMethod()
    {
        final TestRunEntity run1 = new TestRunEntity("RUN_1", "Batch A", "COMPLETED", "Main", "Manual", "Java", "Chrome", "10:00", 1000L);

        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of(run1));

        final String jsonRun1 = """
            {
              "executions": [
                {
                  "id": "e3",
                  "testClass": "PlaybookTest",
                  "testMethod": "test3",
                  "title": "default",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "passed-clean",
                  "engine": "Java"
                },
                {
                  "id": "e7",
                  "testClass": "PlaybookTest",
                  "testMethod": "test7_AnnotationDrivenExternalPlaybookConvention",
                  "title": "default",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "passed-clean",
                  "engine": "Java"
                }
              ]
            }
            """;

        Mockito.when(storageService.readRunJson("RUN_1")).thenReturn(Optional.of(jsonRun1));

        final List<TestBaseVariationHistoryDto> historyTest7 = dataService.getVariationHistory(
            "PlaybookTest",
            "test7_AnnotationDrivenExternalPlaybookConvention",
            "default",
            "US",
            "Chrome"
        );

        Assertions.assertNotNull(historyTest7);
        Assertions.assertEquals(1, historyTest7.size());
        Assertions.assertEquals("RUN_1", historyTest7.get(0).getRunId());
        Assertions.assertEquals("e7", historyTest7.get(0).getExecutionId());
        Assertions.assertEquals("Batch A", historyTest7.get(0).getBatchName());
        Assertions.assertEquals("Java", historyTest7.get(0).getEngine());
    }

    @Test
    public void testGetVariationHistoryWithHealedExecutionFromJson()
    {
        final TestRunEntity run1 = new TestRunEntity("RUN_HEALED", "Batch Healed", "COMPLETED", "Main", "Manual", "Java", "Chrome", "12:00", 3000L);

        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of(run1));

        final String jsonHealed = """
            {
              "executions": [
                {
                  "id": "e_healed_1",
                  "testClass": "HealedTest",
                  "title": "canyon_healed_US",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "passed-clean",
                  "healed": true,
                  "engine": "Java"
                }
              ]
            }
            """;

        Mockito.when(storageService.readRunJson("RUN_HEALED")).thenReturn(Optional.of(jsonHealed));

        final List<TestBaseVariationHistoryDto> history = dataService.getVariationHistory(
            "HealedTest",
            "canyon_healed_US",
            "US",
            "Chrome"
        );

        Assertions.assertNotNull(history);
        Assertions.assertEquals(1, history.size());
        final TestBaseVariationHistoryDto dto = history.get(0);
        Assertions.assertEquals("RUN_HEALED", dto.getRunId());
        Assertions.assertEquals("e_healed_1", dto.getExecutionId());
        Assertions.assertTrue(dto.isHealed(), "DTO should be marked as healed");
        Assertions.assertTrue(dto.isPassedHealed(), "DTO should be passed healed");
        Assertions.assertFalse(dto.isFailedHealed(), "DTO should not be failed healed");
        Assertions.assertEquals("HEALED", dto.getDisplayMode(), "Execution mode should be resolved to HEALED");
        Assertions.assertEquals("PASSED", dto.getStatusLabel(), "Status label should be PASSED without HEALED in status");
    }

    @Test
    public void testGetVariationHistoryWithHealedFromHistoryLinks()
    {
        final String testClass = "HealedLinkTest";
        final String dataSet = "Payment_VISA";
        final String location = "US";
        final String browser = "Chrome";
        final String varId = AuraReportDataService.generateVariationId(testClass, dataSet, location, browser);

        final TestBaseVariationEntity varEntity = new TestBaseVariationEntity(varId, testClass, dataSet, "@General", location, browser);
        final TestRunEntity runEntity = new TestRunEntity("RUN_HEALED_LINK", "Nightly", "COMPLETED", "Staging", "Manual", "Java", "Chrome", "Today", 7000L);

        final String runHealedJson = """
            {
              "runId": "RUN_HEALED_LINK",
              "batch": "Nightly",
              "executions": [
                {
                  "id": "exec-hl-1",
                  "testClass": "HealedLinkTest",
                  "title": "Payment_VISA",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "passed-clean",
                  "healed": true
                }
              ]
            }
            """;
        Mockito.when(storageService.readRunJson("RUN_HEALED_LINK")).thenReturn(Optional.of(runHealedJson));
        Mockito.when(variationRepository.findById(varId)).thenReturn(Optional.of(varEntity));
        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of(runEntity));

        final List<TestBaseVariationHistoryDto> history = dataService.getVariationHistory(testClass, dataSet, location, browser);

        Assertions.assertNotNull(history);
        Assertions.assertEquals(1, history.size());
        final TestBaseVariationHistoryDto dto = history.get(0);
        Assertions.assertEquals("RUN_HEALED_LINK", dto.getRunId());
        Assertions.assertTrue(dto.isHealed(), "DTO should be marked as healed");
        Assertions.assertTrue(dto.isPassedHealed(), "DTO should be passed healed");
        Assertions.assertFalse(dto.isFailedHealed(), "DTO should not be failed healed");
        Assertions.assertEquals("HEALED", dto.getDisplayMode(), "Execution mode should be resolved to HEALED");
        Assertions.assertEquals("PASSED", dto.getStatusLabel(), "Status label should be PASSED without HEALED in status");
    }

    @Test
    public void testGetVariationHistorySortedByTimestampDescending()
    {
        final String testClass = "com.xceptance.neodymium.test.CheckoutTest";
        final String dataSet = "Payment_VISA";
        final String location = "US";
        final String browser = "Chrome";
        final String varId = AuraReportDataService.generateVariationId(testClass, dataSet, location, browser);

        final TestBaseVariationEntity varEntity = new TestBaseVariationEntity(varId, testClass, dataSet, "@General", location, browser);

        final TestRunEntity runSep18 = new TestRunEntity("run_20260918_145300", "Batch1", "COMPLETED", "Staging", "Manual", "Java", "Chrome", "2026-09-18 14:53:29", null);
        final TestRunEntity runOct07 = new TestRunEntity("run_20261007_155456", "Batch2", "COMPLETED", "Staging", "Manual", "Java", "Chrome", "2026-10-07 15:55:37", null);
        final TestRunEntity runSep22 = new TestRunEntity("run_20260922_100430", "Batch3", "COMPLETED", "Staging", "Manual", "Java", "Chrome", "2026-09-22 10:04:50", null);

        final String jsonSep18 = """
            { "runId": "run_20260918_145300", "batch": "Batch1", "executions": [{ "id": "exec-1", "testClass": "com.xceptance.neodymium.test.CheckoutTest", "title": "Payment_VISA", "location": "US", "browser": "Chrome", "status": "failed-unknown" }] }
            """;
        final String jsonOct07 = """
            { "runId": "run_20261007_155456", "batch": "Batch2", "executions": [{ "id": "exec-2", "testClass": "com.xceptance.neodymium.test.CheckoutTest", "title": "Payment_VISA", "location": "US", "browser": "Chrome", "status": "passed-clean" }] }
            """;
        final String jsonSep22 = """
            { "runId": "run_20260922_100430", "batch": "Batch3", "executions": [{ "id": "exec-3", "testClass": "com.xceptance.neodymium.test.CheckoutTest", "title": "Payment_VISA", "location": "US", "browser": "Chrome", "status": "failed-unknown" }] }
            """;

        Mockito.when(storageService.readRunJson("run_20260918_145300")).thenReturn(Optional.of(jsonSep18));
        Mockito.when(storageService.readRunJson("run_20261007_155456")).thenReturn(Optional.of(jsonOct07));
        Mockito.when(storageService.readRunJson("run_20260922_100430")).thenReturn(Optional.of(jsonSep22));

        Mockito.when(variationRepository.findById(varId)).thenReturn(Optional.of(varEntity));
        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of(runSep18, runOct07, runSep22));

        final List<TestBaseVariationHistoryDto> history = dataService.getVariationHistory(testClass, dataSet, location, browser);

        Assertions.assertNotNull(history);
        Assertions.assertEquals(3, history.size());
        Assertions.assertEquals("run_20261007_155456", history.get(0).getRunId());
        Assertions.assertEquals("2026-10-07 15:55:37", history.get(0).getTimestamp());

        Assertions.assertEquals("run_20260922_100430", history.get(1).getRunId());
        Assertions.assertEquals("2026-09-22 10:04:50", history.get(1).getTimestamp());

        Assertions.assertEquals("run_20260918_145300", history.get(2).getRunId());
        Assertions.assertEquals("2026-09-18 14:53:29", history.get(2).getTimestamp());
    }

    @Test
    public void testDtoTimestampComparatorWithIsoAndRecently()
    {
        final TestBaseVariationHistoryDto dtoRecently = new TestBaseVariationHistoryDto(
            "run_20261008_120000", "exec-r", "Batch", "MODE", "Java", "Recently", "passed-clean", "badge-pass", "PASSED", List.of()
        );
        final TestBaseVariationHistoryDto dtoOct07 = new TestBaseVariationHistoryDto(
            "run_20261007_155456", "exec-1", "Batch", "MODE", "Java", "2026-10-07 15:55:37", "passed-clean", "badge-pass", "PASSED", List.of()
        );
        final TestBaseVariationHistoryDto dtoSep18 = new TestBaseVariationHistoryDto(
            "run_20260918_145300", "exec-2", "Batch", "MODE", "Java", "2026-09-18T14:53:29Z", "failed-unknown", "badge-fail", "FAILED", List.of()
        );

        final List<TestBaseVariationHistoryDto> list = new ArrayList<>(List.of(dtoSep18, dtoRecently, dtoOct07));
        list.sort(TestBaseVariationHistoryDto.BY_TIMESTAMP_DESC);

        Assertions.assertEquals("run_20261008_120000", list.get(0).getRunId());
        Assertions.assertEquals("run_20261007_155456", list.get(1).getRunId());
        Assertions.assertEquals("run_20260918_145300", list.get(2).getRunId());
    }

    @Test
    public void testVariationEntityHealedField()
    {
        final TestBaseVariationEntity entity = new TestBaseVariationEntity();
        Assertions.assertFalse(entity.isHealed(), "Default healed should be false");
        entity.setHealed(true);
        Assertions.assertTrue(entity.isHealed(), "Healed should be true after setter");
    }

    @Test
    public void testGetTestBaseDataUpdatesStaleStatusToLatestExecution()
    {
        final String testClass = "com.xceptance.neodymium.test.CheckoutTest";
        final String dataSet = "Payment_VISA";
        final String location = "US";
        final String browser = "Chrome";
        final String varId = AuraReportDataService.generateVariationId(testClass, dataSet, location, browser);

        // Older duplicate had failed-unknown
        final TestBaseVariationEntity olderVar = new TestBaseVariationEntity(varId + "-old", testClass, "testCheckout", dataSet, "@General", location, browser);
        olderVar.setLastStatus("failed-unknown");
        olderVar.setLastExecutedAt(1000L);
        olderVar.setTotalExecutionsCount(1);

        // Newer duplicate has passed-clean
        final TestBaseVariationEntity newerVar = new TestBaseVariationEntity(varId, testClass, "testCheckout", dataSet, "@General", location, browser);
        newerVar.setLastStatus("passed-clean");
        newerVar.setLastExecutedAt(2000L);
        newerVar.setTotalExecutionsCount(1);

        Mockito.when(variationRepository.findAllByOrderByLastExecutedAtDesc()).thenReturn(List.of(olderVar, newerVar));
        Mockito.when(bugRepository.findAll()).thenReturn(List.of());
        Mockito.when(batchRepository.findAll()).thenReturn(List.of());
        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of());

        final TestBaseDataDto data = dataService.getTestBaseData();
        Assertions.assertNotNull(data);
        Assertions.assertEquals(1, data.getTotalVariationsCount());

        final TestBaseAreaDto area = data.getAreas().get(0);
        final TestBaseClassDto classDto = area.getTestClasses().get(0);
        final TestBaseVariationEntity updatedVar = classDto.getVariations().get(0);

        Assertions.assertEquals("passed-clean", updatedVar.getLastStatus(), "Last status should be updated to passed-clean from newest execution");
        Assertions.assertFalse(updatedVar.isHealed(), "Entity healed should be false for clean pass");

        Mockito.verify(variationRepository, Mockito.atLeastOnce()).deleteAll(Mockito.anyList());
    }

    @Test
    public void testGetTestBaseDataDerivesHealedFromLatestExecution()
    {
        final String testClass = "com.xceptance.neodymium.test.LoginTest";
        final String dataSet = "StandardUser";
        final String location = "DE";
        final String browser = "Firefox";
        final String varId = AuraReportDataService.generateVariationId(testClass, dataSet, location, browser);

        final TestBaseVariationEntity olderVar = new TestBaseVariationEntity(varId + "-old", testClass, "testLogin", dataSet, "@General", location, browser);
        olderVar.setLastStatus("failed-unknown");
        olderVar.setLastExecutedAt(1000L);
        olderVar.setHealed(false);

        final TestBaseVariationEntity newerVar = new TestBaseVariationEntity(varId, testClass, "testLogin", dataSet, "@General", location, browser);
        newerVar.setLastStatus("passed-clean");
        newerVar.setLastExecutedAt(2000L);
        newerVar.setHealed(true);
        newerVar.setExecutionMode("HEALED");

        Mockito.when(variationRepository.findAllByOrderByLastExecutedAtDesc()).thenReturn(List.of(olderVar, newerVar));
        Mockito.when(bugRepository.findAll()).thenReturn(List.of());
        Mockito.when(batchRepository.findAll()).thenReturn(List.of());
        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of());

        final TestBaseDataDto data = dataService.getTestBaseData();
        Assertions.assertNotNull(data);

        final TestBaseAreaDto area = data.getAreas().get(0);
        final TestBaseClassDto classDto = area.getTestClasses().get(0);
        final TestBaseVariationEntity updatedVar = classDto.getVariations().get(0);

        Assertions.assertEquals("passed-clean", updatedVar.getLastStatus(), "Last status should be passed-clean independent of healing");
        Assertions.assertTrue(updatedVar.isHealed(), "Variation should be marked as healed");
        Assertions.assertEquals("HEALED", updatedVar.getDisplayMode(), "Execution mode should be HEALED when latest execution is healed");
    }

    @Test
    public void testGetTestBaseDataStrictlyResolvesLatestExecutionMode()
    {
        final String testClass = "com.xceptance.neodymium.test.CheckoutTest";
        final String dataSet = "DEU";
        final String location = "DE";
        final String browser = "Chrome";
        final String varId = AuraReportDataService.generateVariationId(testClass, dataSet, location, browser);

        final TestBaseVariationEntity var1 = new TestBaseVariationEntity(varId + "-1", testClass, "testCheckout", dataSet, "@General", location, browser);
        var1.setLastExecutedAt(1000L);
        var1.setLastStatus("passed-clean");
        var1.setExecutionMode("JAVA");

        final TestBaseVariationEntity var2 = new TestBaseVariationEntity(varId + "-2", testClass, "testCheckout", dataSet, "@General", location, browser);
        var2.setLastExecutedAt(2000L);
        var2.setLastStatus("passed-clean");
        var2.setHealed(true);
        var2.setExecutionMode("HEALED");

        final TestBaseVariationEntity var3 = new TestBaseVariationEntity(varId, testClass, "testCheckout", dataSet, "@General", location, browser);
        var3.setLastExecutedAt(3000L);
        var3.setLastStatus("failed-unknown");
        var3.setExecutionMode("AI");

        Mockito.when(variationRepository.findAllByOrderByLastExecutedAtDesc()).thenReturn(List.of(var1, var3, var2));
        Mockito.when(bugRepository.findAll()).thenReturn(List.of());
        Mockito.when(batchRepository.findAll()).thenReturn(List.of());
        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of());

        final TestBaseDataDto data = dataService.getTestBaseData();
        Assertions.assertNotNull(data);

        final TestBaseVariationEntity resultVar = data.getAreas().get(0).getTestClasses().get(0).getVariations().get(0);
        Assertions.assertEquals("AI", resultVar.getDisplayMode(), "Latest execution mode (AI) must be displayed, not older HEALED or JAVA mode");
        Assertions.assertEquals("failed-unknown", resultVar.getLastStatus(), "Latest status must be failed-unknown");
    }

    @Test
    public void testExtractBrowserFromIdOrKey()
    {
        Assertions.assertEquals("Chrome_1920x1080", AuraReportDataService.extractBrowserFromIdOrKey("Aura_my_test_yaml_Test#executeYamlTest#en#Chrome_1920x1080"));
        Assertions.assertEquals("FF_1024x768", AuraReportDataService.extractBrowserFromIdOrKey("Aura_my_test_yaml_Test#en#FF_1024x768"));
        Assertions.assertEquals("Chrome_1920x1080", AuraReportDataService.extractBrowserFromIdOrKey("Aura_my_test_yaml_Test#executeYamlTest#en#Chrome_1920x1080#2"));
        Assertions.assertNull(AuraReportDataService.extractBrowserFromIdOrKey("console-execution-1"));
        Assertions.assertNull(AuraReportDataService.extractBrowserFromIdOrKey(""));
        Assertions.assertNull(AuraReportDataService.extractBrowserFromIdOrKey(null));
    }

    @Test
    public void testGetTestBaseDataMergesDuplicateVariationsAndSumsExecutions()
    {
        final String testClass = "Aura_my_test_yaml_Test";
        final String testMethod = "executeYamlTest";
        final String dataSet = "en";
        final String location = "EN";
        final String browser = "Chrome_1920x1080";

        final String canonicalVarId = AuraReportDataService.generateVariationId(testClass, testMethod, dataSet, location, browser);
        final String oldVarId = canonicalVarId + "-duplicate";

        // Obsolete duplicate variation
        final TestBaseVariationEntity oldVar = new TestBaseVariationEntity(oldVarId, testClass, testMethod, dataSet, "@General", location, browser);
        oldVar.setTotalExecutionsCount(77);
        oldVar.setLastStatus("ignored");
        oldVar.setLastExecutedAt(1000L);

        // Canonical variation
        final TestBaseVariationEntity canonicalVar = new TestBaseVariationEntity(canonicalVarId, testClass, testMethod, dataSet, "@General", location, browser);
        canonicalVar.setTotalExecutionsCount(2);
        canonicalVar.setLastStatus("passed-clean");
        canonicalVar.setLastExecutedAt(2000L);

        Mockito.when(variationRepository.findAllByOrderByLastExecutedAtDesc()).thenReturn(List.of(oldVar, canonicalVar));
        Mockito.when(bugRepository.findAll()).thenReturn(List.of());
        Mockito.when(batchRepository.findAll()).thenReturn(List.of());
        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of());

        final TestBaseDataDto data = dataService.getTestBaseData();
        Assertions.assertNotNull(data);
        Assertions.assertEquals(1, data.getTotalVariationsCount(), "Both variations must be merged into 1 variation");

        final TestBaseAreaDto area = data.getAreas().get(0);
        final TestBaseClassDto classDto = area.getTestClasses().get(0);
        final TestBaseVariationEntity mergedVar = classDto.getVariations().get(0);

        Assertions.assertEquals(canonicalVarId, mergedVar.getId(), "Canonical ID must be retained");
        Assertions.assertEquals("Chrome_1920x1080", mergedVar.getBrowser(), "Merged variation must retain exact browser profile");
        Assertions.assertEquals(79, mergedVar.getTotalExecutionsCount(), "Total executions count should be summed across duplicates");
        Assertions.assertEquals("passed-clean", mergedVar.getLastStatus(), "Latest status should be retained");

        Mockito.verify(variationRepository, Mockito.atLeastOnce()).deleteAll(Mockito.argThat(iterable ->
        {
            if (iterable == null)
            {
                return false;
            }
            for (final TestBaseVariationEntity v : iterable)
            {
                if (oldVarId.equals(v.getId()))
                {
                    return true;
                }
            }
            return false;
        }));
    }

    @Test
    public void testTestBaseVariationHistoryDtoDisplayModeResolution()
    {
        // 1. AI Modes
        final TestBaseVariationHistoryDto dtoAi = new TestBaseVariationHistoryDto(
            "R1", "e1", "B1", "LLM_RECORDING", "Java", "2026-10-14 10:00:00", "passed-clean", "badge-pass", "PASSED", List.of()
        );
        Assertions.assertEquals("AI", dtoAi.getDisplayMode());

        final TestBaseVariationHistoryDto dtoAiForce = new TestBaseVariationHistoryDto(
            "R1b", "e1b", "B1", "FORCE_RECORDING", "Java", "2026-10-14 10:00:00", "passed-clean", "badge-pass", "PASSED", List.of()
        );
        Assertions.assertEquals("AI", dtoAiForce.getDisplayMode());

        // 2. Playbook Modes without Healing
        final TestBaseVariationHistoryDto dtoPlaybook = new TestBaseVariationHistoryDto(
            "R2", "e2", "B1", "REPLAY_STRICT", "Java", "2026-10-14 10:00:00", "passed-clean", "badge-pass", "PASSED", List.of()
        );
        Assertions.assertEquals("PLAYBOOK", dtoPlaybook.getDisplayMode());

        final TestBaseVariationHistoryDto dtoReplayNoHeal = new TestBaseVariationHistoryDto(
            "R2b", "e2b", "B1", "REPLAY_WITH_HEALING", "Java", "2026-10-14 10:00:00", "passed-clean", "badge-pass", "PASSED", List.of(), false, false, false
        );
        Assertions.assertEquals("PLAYBOOK", dtoReplayNoHeal.getDisplayMode());

        // 3. Healed Mode
        final TestBaseVariationHistoryDto dtoReplayHealed = new TestBaseVariationHistoryDto(
            "R3", "e3", "B1", "REPLAY_WITH_HEALING", "Java", "2026-10-14 10:00:00", "passed-clean", "badge-pass", "PASSED", List.of(), true, true, false
        );
        Assertions.assertEquals("HEALED", dtoReplayHealed.getDisplayMode());

        // 4. Standard Java Mode
        final TestBaseVariationHistoryDto dtoJava = new TestBaseVariationHistoryDto(
            "R4", "e4", "B1", "JAVA", "Java", "2026-10-14 10:00:00", "passed-clean", "badge-pass", "PASSED", List.of()
        );
        Assertions.assertEquals("JAVA", dtoJava.getDisplayMode());

        final TestBaseVariationHistoryDto dtoDefault = new TestBaseVariationHistoryDto(
            "R5", "e5", "B1", "", "Java", "2026-10-14 10:00:00", "passed-clean", "badge-pass", "PASSED", List.of()
        );
        Assertions.assertEquals("JAVA", dtoDefault.getDisplayMode());
    }

    @Test
    public void testGetStatusBadgeInfoNormalizesHealedToPassed()
    {
        final String[] badgeInfo = AuraReportDataService.getStatusBadgeInfo("healed");
        Assertions.assertEquals("badge-pass", badgeInfo[0]);
        Assertions.assertEquals("PASSED", badgeInfo[1]);
    }

    @Test
    public void testGetBatchTestBaseClassesExtractsAndGroupsVariations()
    {
        final TestRunEntity run1 = new TestRunEntity("run-1", "SmokeBatch", "COMPLETED", "Manual", "Staging", "US", "Chrome", "10:00", 2000L);
        final TestRunEntity run2 = new TestRunEntity("run-2", "SmokeBatch", "COMPLETED", "Manual", "Staging", "US,DE", "Chrome,Firefox", "09:00", 1000L);

        Mockito.when(runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc("SmokeBatch")).thenReturn(List.of(run1, run2));
        Mockito.when(bugRepository.findAll()).thenReturn(List.of());

        final String jsonRun1 = """
            {
              "executions": [
                {
                  "id": "e1",
                  "testClass": "CheckoutTest",
                  "testMethod": "testCheckoutFlow",
                  "title": "US_Chrome",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "passed-clean",
                  "mode": "AI",
                  "areaName": "@Checkout"
                }
              ]
            }
            """;

        final String jsonRun2 = """
            {
              "executions": [
                {
                  "id": "e2",
                  "testClass": "CheckoutTest",
                  "testMethod": "testCheckoutFlow",
                  "title": "US_Chrome",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "failed",
                  "mode": "JAVA",
                  "areaName": "@Checkout"
                },
                {
                  "id": "e3",
                  "testClass": "SearchTest",
                  "testMethod": "testSearchFlow",
                  "title": "DE_Firefox",
                  "location": "DE",
                  "browser": "Firefox",
                  "status": "passed-clean",
                  "mode": "PLAYBOOK",
                  "areaName": "@Search"
                }
              ]
            }
            """;

        Mockito.when(storageService.readRunJson("run-1")).thenReturn(Optional.of(jsonRun1));
        Mockito.when(storageService.readRunJson("run-2")).thenReturn(Optional.of(jsonRun2));

        final List<TestBaseClassDto> classDtos = dataService.getBatchTestBaseClasses("SmokeBatch");

        Assertions.assertNotNull(classDtos);
        Assertions.assertEquals(2, classDtos.size(), "Should have 2 test classes: CheckoutTest and SearchTest");

        final TestBaseClassDto checkoutClass = classDtos.stream()
            .filter(c -> "CheckoutTest".equals(c.getClassName()))
            .findFirst()
            .orElseThrow();
        Assertions.assertEquals(1, checkoutClass.getVariationCount());
        final TestBaseVariationEntity checkoutVar = checkoutClass.getVariations().get(0);
        Assertions.assertEquals("CheckoutTest", checkoutVar.getTestClassName());
        Assertions.assertEquals("testCheckoutFlow", checkoutVar.getTestMethodName());
        Assertions.assertEquals("US", checkoutVar.getLocation());
        Assertions.assertEquals("Chrome", checkoutVar.getBrowser());
        Assertions.assertEquals("passed-clean", checkoutVar.getLastStatus());
        Assertions.assertEquals("AI", checkoutVar.getDisplayMode());
        Assertions.assertEquals(2, checkoutVar.getTotalExecutionsCount());

        final TestBaseClassDto searchClass = classDtos.stream()
            .filter(c -> "SearchTest".equals(c.getClassName()))
            .findFirst()
            .orElseThrow();
        Assertions.assertEquals(1, searchClass.getVariationCount());
        final TestBaseVariationEntity searchVar = searchClass.getVariations().get(0);
        Assertions.assertEquals("SearchTest", searchVar.getTestClassName());
        Assertions.assertEquals("testSearchFlow", searchVar.getTestMethodName());
        Assertions.assertEquals("DE", searchVar.getLocation());
        Assertions.assertEquals("Firefox", searchVar.getBrowser());
        Assertions.assertEquals("passed-clean", searchVar.getLastStatus());
        Assertions.assertEquals("PLAYBOOK", searchVar.getDisplayMode());
        Assertions.assertEquals(1, searchVar.getTotalExecutionsCount());
    }

    @Test
    public void testBatchTestBaseClassesSetsLastRunIdAndEnvironment()
    {
        final TestRunEntity run1 = new TestRunEntity("RUN_1", "Batch A", "COMPLETED", "Main", "Production", "Java", "Chrome", "10:00", 1000L);
        run1.setEnvironment("Production");

        final String jsonRun1 = """
            {
              "executions": [
                {
                  "testClass": "com.xceptance.neodymium.test.LoginTest",
                  "testMethod": "testLogin",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "passed-clean",
                  "durationMs": 1500
                }
              ]
            }
            """;
        Mockito.when(storageService.readRunJson("RUN_1")).thenReturn(Optional.of(jsonRun1));

        final List<TestBaseClassDto> classes = dataService.getBatchTestBaseClasses("Batch A", List.of(run1));
        Assertions.assertFalse(classes.isEmpty());
        final TestBaseClassDto cls = classes.get(0);
        Assertions.assertEquals("com.xceptance.neodymium.test.LoginTest", cls.getClassName());
        Assertions.assertEquals(1, cls.getVariations().size());
        final TestBaseVariationEntity var = cls.getVariations().get(0);
        Assertions.assertEquals("RUN_1", var.getLastRunId());
        Assertions.assertEquals("Production", var.getLastEnvironment());
    }

    @Test
    public void testBatchLatestExecutionsPopulatesLatestVariationsWithEnvironmentAndStatus()
    {
        final TestRunEntity run1 = new TestRunEntity("RUN_1", "Batch A", "COMPLETED", "Main", "Staging", "Java", "Chrome", "10:00", 1000L);
        run1.setEnvironment("Staging");

        final String jsonRun1 = """
            {
              "executions": [
                {
                  "testClass": "com.xceptance.neodymium.test.LoginTest",
                  "testMethod": "testLogin",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "passed-clean",
                  "durationMs": 1500
                }
              ]
            }
            """;
        Mockito.when(storageService.readRunJson("RUN_1")).thenReturn(Optional.of(jsonRun1));

        final BatchLatestExecutionsDto result = dataService.getBatchLatestTestExecutionsSummary("Batch A", List.of(run1), Collections.emptySet(), null);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getLatestVariations().size());
        final BatchLatestVariationDto v = result.getLatestVariations().get(0);
        Assertions.assertEquals("RUN_1", v.getRunId());
        Assertions.assertEquals("Staging", v.getEnvironment());
        Assertions.assertEquals("passed-clean", v.getStatus());
        Assertions.assertEquals("Chrome", v.getBrowser());
        Assertions.assertEquals("US", v.getLocation());
    }

    @Test
    public void testBatchLatestExecutionsCalculatesEnvironmentRatios()
    {
        final TestRunEntity run1 = new TestRunEntity("RUN_1", "Batch A", "COMPLETED", "Main", "Staging", "Java", "Chrome", "10:00", 2000L);
        run1.setEnvironment("Staging");
        final TestRunEntity run2 = new TestRunEntity("RUN_2", "Batch A", "COMPLETED", "Main", "Production", "Java", "Chrome", "09:00", 1000L);
        run2.setEnvironment("Production");

        final String jsonRun1 = """
            {
              "executions": [
                {
                  "testClass": "com.xceptance.neodymium.test.LoginTest",
                  "testMethod": "testLogin",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "passed-clean",
                  "durationMs": 1500
                }
              ]
            }
            """;
        final String jsonRun2 = """
            {
              "executions": [
                {
                  "testClass": "com.xceptance.neodymium.test.OrderTest",
                  "testMethod": "testOrder",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "passed-clean",
                  "durationMs": 1800
                }
              ]
            }
            """;
        Mockito.when(storageService.readRunJson("RUN_1")).thenReturn(Optional.of(jsonRun1));
        Mockito.when(storageService.readRunJson("RUN_2")).thenReturn(Optional.of(jsonRun2));

        final BatchLatestExecutionsDto result = dataService.getBatchLatestTestExecutionsSummary("Batch A", List.of(run1, run2), Collections.emptySet(), null);
        Assertions.assertNotNull(result);

        final List<DistributionRatioDto> envRatios = result.getEnvironmentRatios();
        Assertions.assertNotNull(envRatios);
        Assertions.assertEquals(2, envRatios.size());

        final DistributionRatioDto stagingRatio = envRatios.stream()
            .filter(r -> "Staging".equals(r.getName()))
            .findFirst()
            .orElse(null);
        Assertions.assertNotNull(stagingRatio);
        Assertions.assertEquals(1, stagingRatio.getCount());
        Assertions.assertEquals(50.0, stagingRatio.getPercentage(), 0.01);

        final DistributionRatioDto prodRatio = envRatios.stream()
            .filter(r -> "Production".equals(r.getName()))
            .findFirst()
            .orElse(null);
        Assertions.assertNotNull(prodRatio);
        Assertions.assertEquals(1, prodRatio.getCount());
        Assertions.assertEquals(50.0, prodRatio.getPercentage(), 0.01);
    }

    @Test
    public void testBatchLatestExecutionsCalculatesAreaRatios()
    {
        final TestRunEntity run1 = new TestRunEntity("RUN_1", "Batch A", "COMPLETED", "Main", "Staging", "Java", "Chrome", "10:00", 2000L);
        final TestRunEntity run2 = new TestRunEntity("RUN_2", "Batch A", "COMPLETED", "Main", "Staging", "Java", "Chrome", "09:00", 1000L);

        final String jsonRun1 = """
            {
              "executions": [
                {
                  "testClass": "com.xceptance.neodymium.test.LoginTest",
                  "testMethod": "testLogin",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "passed-clean",
                  "area": "Authentication",
                  "durationMs": 1500
                }
              ]
            }
            """;
        final String jsonRun2 = """
            {
              "executions": [
                {
                  "testClass": "com.xceptance.neodymium.test.OrderTest",
                  "testMethod": "testOrder",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "passed-clean",
                  "area": "@Checkout",
                  "durationMs": 1800
                }
              ]
            }
            """;
        Mockito.when(storageService.readRunJson("RUN_1")).thenReturn(Optional.of(jsonRun1));
        Mockito.when(storageService.readRunJson("RUN_2")).thenReturn(Optional.of(jsonRun2));

        final BatchLatestExecutionsDto result = dataService.getBatchLatestTestExecutionsSummary("Batch A", List.of(run1, run2), Collections.emptySet(), null);
        Assertions.assertNotNull(result);

        final List<DistributionRatioDto> areaRatios = result.getAreaRatios();
        Assertions.assertNotNull(areaRatios);
        Assertions.assertEquals(2, areaRatios.size());

        final DistributionRatioDto authRatio = areaRatios.stream()
            .filter(r -> "@Authentication".equals(r.getName()))
            .findFirst()
            .orElse(null);
        Assertions.assertNotNull(authRatio);
        Assertions.assertEquals(1, authRatio.getCount());
        Assertions.assertEquals(50.0, authRatio.getPercentage(), 0.01);

        final DistributionRatioDto checkoutRatio = areaRatios.stream()
            .filter(r -> "@Checkout".equals(r.getName()))
            .findFirst()
            .orElse(null);
        Assertions.assertNotNull(checkoutRatio);
        Assertions.assertEquals(1, checkoutRatio.getCount());
        Assertions.assertEquals(50.0, checkoutRatio.getPercentage(), 0.01);
    }

    @Test
    public void testBatchTestBaseClassesAndSummaryExcludesClassPlaceholderExecutions()
    {
        final TestRunEntity run1 = new TestRunEntity("RUN_1", "SmokeBatch", "COMPLETED", "Main", "Production", "Java", "Chrome", "10:00", 1000L);
        final TestRunEntity run2 = new TestRunEntity("RUN_2", "SmokeBatch", "COMPLETED", "Main", "Production", "Java", "Chrome", "11:00", 2000L);

        final String jsonRun1 = """
            {
              "executions": [
                {
                  "testClass": "Aura_my_test_yaml_Test",
                  "testMethod": "executeYamlTest",
                  "title": "en",
                  "location": "Default",
                  "browser": "Chrome_1920x1080",
                  "status": "passed-clean",
                  "durationMs": 1500
                }
              ]
            }
            """;

        final String jsonRun2 = """
            {
              "executions": [
                {
                  "testClass": "Aura_my_test_yaml_Test",
                  "testMethod": "",
                  "title": "Aura_my_test_yaml_Test",
                  "location": "Default",
                  "browser": "Chrome_1920x1080",
                  "status": "skipped",
                  "durationMs": 0
                }
              ]
            }
            """;
        Mockito.when(storageService.readRunJson("RUN_1")).thenReturn(Optional.of(jsonRun1));
        Mockito.when(storageService.readRunJson("RUN_2")).thenReturn(Optional.of(jsonRun2));

        final List<TestBaseClassDto> classes = dataService.getBatchTestBaseClasses("SmokeBatch", List.of(run2, run1));
        Assertions.assertEquals(1, classes.size());
        final TestBaseClassDto classDto = classes.get(0);
        Assertions.assertEquals("Aura_my_test_yaml_Test", classDto.getClassName());
        Assertions.assertEquals(1, classDto.getVariationCount(), "Class container placeholder should not be counted as a variation");
        Assertions.assertEquals(1, classDto.getVariations().size());
        final TestBaseVariationEntity variation = classDto.getVariations().get(0);
        Assertions.assertEquals("executeYamlTest", variation.getTestMethodName());
        Assertions.assertEquals("en", variation.getDataSetLabel());

        final BatchLatestExecutionsDto summary = dataService.getBatchLatestTestExecutionsSummary("SmokeBatch", List.of(run2, run1));
        Assertions.assertNotNull(summary);
        Assertions.assertEquals(1, summary.getTotalVariationsCount(), "Total variations in summary should exclude class placeholder");
        Assertions.assertEquals(1, summary.getLatestVariations().size());
        Assertions.assertTrue(summary.getLatestVariations().get(0).getKey().contains("executeYamlTest"));
    }

    @Test
    public void testGroupBatchTestClassesByAreaAndGetBatchTestBaseAreas()
    {
        final TestBaseVariationEntity var1 = new TestBaseVariationEntity("var-1", "CheckoutTest", "testCheckout", "Default", "@Checkout", "US", "Chrome");
        final TestBaseClassDto class1 = new TestBaseClassDto("CheckoutTest", "batchClassContainerCheckoutTest", 1, List.of(var1));

        final TestBaseVariationEntity var2 = new TestBaseVariationEntity("var-2", "SearchTest", "testSearch", "Default", "@Search", "DE", "Firefox");
        final TestBaseVariationEntity var3 = new TestBaseVariationEntity("var-3", "SearchTest", "testSearchExtended", "DataSet2", "@Search", "DE", "Firefox");
        final TestBaseClassDto class2 = new TestBaseClassDto("SearchTest", "batchClassContainerSearchTest", 2, List.of(var2, var3));

        final TestBaseVariationEntity var4 = new TestBaseVariationEntity("var-4", "GeneralTest", "testGeneral", "Default", "@General", "US", "Chrome");
        final TestBaseClassDto class3 = new TestBaseClassDto("GeneralTest", "batchClassContainerGeneralTest", 1, List.of(var4));

        final List<TestBaseAreaDto> areas = dataService.groupBatchTestClassesByArea(List.of(class1, class2, class3));
        Assertions.assertNotNull(areas);
        Assertions.assertEquals(3, areas.size());

        final TestBaseAreaDto checkoutArea = areas.stream()
            .filter(a -> "@Checkout".equals(a.getAreaName()))
            .findFirst()
            .orElseThrow();
        Assertions.assertEquals("batchAreaCheckout", checkoutArea.getAreaPaneId());
        Assertions.assertEquals("shopping_cart", checkoutArea.getIcon());
        Assertions.assertEquals(1, checkoutArea.getClassCount());
        Assertions.assertEquals(1, checkoutArea.getTotalVariationsCount());
        Assertions.assertEquals(1, checkoutArea.getTestClasses().size());

        final TestBaseAreaDto searchArea = areas.stream()
            .filter(a -> "@Search".equals(a.getAreaName()))
            .findFirst()
            .orElseThrow();
        Assertions.assertEquals("batchAreaSearch", searchArea.getAreaPaneId());
        Assertions.assertEquals("search", searchArea.getIcon());
        Assertions.assertEquals(1, searchArea.getClassCount());
        Assertions.assertEquals(2, searchArea.getTotalVariationsCount());

        final TestBaseAreaDto browsingArea = areas.stream()
            .filter(a -> "@Browsing (default)".equals(a.getAreaName()))
            .findFirst()
            .orElseThrow();
        Assertions.assertEquals("batchAreaBrowsingdefault", browsingArea.getAreaPaneId());
        Assertions.assertEquals("folder", browsingArea.getIcon());
        Assertions.assertEquals(1, browsingArea.getClassCount());
        Assertions.assertEquals(1, browsingArea.getTotalVariationsCount());

        Assertions.assertTrue(dataService.groupBatchTestClassesByArea(null).isEmpty());
        Assertions.assertTrue(dataService.groupBatchTestClassesByArea(List.of()).isEmpty());
    }

    @Test
    public void testBatchLatestExecutionsFiltersByRunBrowsers()
    {
        final TestRunEntity run1 = new TestRunEntity("RUN_1", "Batch A", "COMPLETED", "Main", "Default", "Java", "Chrome_1920x1080", "10:00", 2000L);
        run1.setBrowsersCsv("Chrome_1920x1080");
        final TestRunEntity run2 = new TestRunEntity("RUN_2", "Batch A", "COMPLETED", "Main", "Default", "Java", "Firefox", "09:00", 1000L);
        run2.setBrowsersCsv("Firefox");

        final String jsonRun1 = """
            {
              "executions": [
                {
                  "testClass": "com.xceptance.neodymium.test.ChromeTest",
                  "testMethod": "testChrome",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome_1920x1080",
                  "status": "passed-clean",
                  "durationMs": 1500
                }
              ]
            }
            """;
        final String jsonRun2 = """
            {
              "executions": [
                {
                  "testClass": "com.xceptance.neodymium.test.FirefoxTest",
                  "testMethod": "testFirefox",
                  "title": "Default",
                  "location": "US",
                  "browser": "Firefox",
                  "status": "passed-clean",
                  "durationMs": 1200
                }
              ]
            }
            """;
        Mockito.when(storageService.readRunJson("RUN_1")).thenReturn(Optional.of(jsonRun1));
        Mockito.when(storageService.readRunJson("RUN_2")).thenReturn(Optional.of(jsonRun2));

        final BatchLatestExecutionsDto result = dataService.getBatchLatestTestExecutionsSummary(
            "Batch A",
            List.of(run1, run2),
            Collections.emptySet(),
            null,
            Set.of("Chrome_1920x1080"),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptyMap()
        );

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getEligibleRunsCount());
        Assertions.assertEquals(1, result.getContributingRuns().size());
        Assertions.assertEquals("RUN_1", result.getContributingRuns().get(0).getId());
        Assertions.assertEquals(1, result.getLatestVariations().size());
        Assertions.assertEquals("RUN_1", result.getLatestVariations().get(0).getRunId());
        Assertions.assertEquals("Chrome_1920x1080", result.getLatestVariations().get(0).getBrowser());
    }

    @Test
    public void testBatchLatestExecutionsFiltersByRunEnvsAndLocales()
    {
        final TestRunEntity runStaging = new TestRunEntity("RUN_STG", "Batch A", "COMPLETED", "Main", "Staging", "Java", "Chrome", "10:00", 2000L);
        runStaging.setEnvironment("Staging");
        runStaging.setLocalesCsv("DEU");
        final TestRunEntity runProd = new TestRunEntity("RUN_PROD", "Batch A", "COMPLETED", "Main", "Production", "Java", "Chrome", "09:00", 1000L);
        runProd.setEnvironment("Production");
        runProd.setLocalesCsv("EN");

        final String jsonStaging = """
            {
              "executions": [
                {
                  "testClass": "com.xceptance.neodymium.test.StgTest",
                  "testMethod": "testStg",
                  "title": "Default",
                  "location": "DEU",
                  "browser": "Chrome",
                  "status": "passed-clean",
                  "durationMs": 1500
                }
              ]
            }
            """;
        final String jsonProd = """
            {
              "executions": [
                {
                  "testClass": "com.xceptance.neodymium.test.ProdTest",
                  "testMethod": "testProd",
                  "title": "Default",
                  "location": "EN",
                  "browser": "Chrome",
                  "status": "passed-clean",
                  "durationMs": 1200
                }
              ]
            }
            """;
        Mockito.when(storageService.readRunJson("RUN_STG")).thenReturn(Optional.of(jsonStaging));
        Mockito.when(storageService.readRunJson("RUN_PROD")).thenReturn(Optional.of(jsonProd));

        final BatchLatestExecutionsDto result = dataService.getBatchLatestTestExecutionsSummary(
            "Batch A",
            List.of(runStaging, runProd),
            Collections.emptySet(),
            null,
            Collections.emptySet(),
            Set.of("DEU"),
            Set.of("Staging"),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptyMap()
        );

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getEligibleRunsCount());
        Assertions.assertEquals(1, result.getContributingRuns().size());
        Assertions.assertEquals("RUN_STG", result.getContributingRuns().get(0).getId());
        Assertions.assertEquals("Staging", result.getLatestVariations().get(0).getEnvironment());
    }

    @Test
    public void testBatchLatestExecutionsFiltersByRunTagsAndAreas()
    {
        final TestRunEntity runWiki = new TestRunEntity("RUN_WIKI", "Wikipedia", "COMPLETED", "Main", "Default", "Java", "Chrome", "10:00", 2000L);
        runWiki.setBatchName("Wikipedia");
        final TestRunEntity runSmoke = new TestRunEntity("RUN_SMOKE", "Smoke", "COMPLETED", "Main", "Default", "Java", "Chrome", "09:00", 1000L);
        runSmoke.setBatchName("Smoke");

        final String jsonWiki = """
            {
              "executions": [
                {
                  "testClass": "com.xceptance.neodymium.test.WikiSearchTest",
                  "testMethod": "testSearch",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "passed-clean",
                  "areaName": "SearchArea",
                  "durationMs": 1500
                }
              ]
            }
            """;
        final String jsonSmoke = """
            {
              "executions": [
                {
                  "testClass": "com.xceptance.neodymium.test.SmokeTest",
                  "testMethod": "testSmoke",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "passed-clean",
                  "areaName": "CheckoutArea",
                  "durationMs": 1200
                }
              ]
            }
            """;
        Mockito.when(storageService.readRunJson("RUN_WIKI")).thenReturn(Optional.of(jsonWiki));
        Mockito.when(storageService.readRunJson("RUN_SMOKE")).thenReturn(Optional.of(jsonSmoke));

        final Map<String, String> areasMap = Map.of(
            "RUN_WIKI", "@SearchArea",
            "RUN_SMOKE", "@CheckoutArea"
        );

        final BatchLatestExecutionsDto result = dataService.getBatchLatestTestExecutionsSummary(
            "all",
            List.of(runWiki, runSmoke),
            Collections.emptySet(),
            null,
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Set.of("@SearchArea"),
            Set.of("Wikipedia"),
            areasMap
        );

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getEligibleRunsCount());
        Assertions.assertEquals(1, result.getContributingRuns().size());
        Assertions.assertEquals("RUN_WIKI", result.getContributingRuns().get(0).getId());
    }

    @Test
    public void testBatchLatestExecutionsFiltersExecutionsInsideEligibleRun()
    {
        final TestRunEntity runMulti = new TestRunEntity("RUN_MULTI", "Batch A", "COMPLETED", "Main", "Default", "Java", "Chrome, Firefox", "10:00", 2000L);
        runMulti.setBrowsersCsv("Chrome, Firefox");

        final String jsonMulti = """
            {
              "executions": [
                {
                  "testClass": "com.xceptance.neodymium.test.MultiTest",
                  "testMethod": "testRun",
                  "title": "DataSet1",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "passed-clean",
                  "durationMs": 1000
                },
                {
                  "testClass": "com.xceptance.neodymium.test.MultiTest",
                  "testMethod": "testRun",
                  "title": "DataSet2",
                  "location": "US",
                  "browser": "Firefox",
                  "status": "failed-unknown",
                  "durationMs": 1000
                }
              ]
            }
            """;
        Mockito.when(storageService.readRunJson("RUN_MULTI")).thenReturn(Optional.of(jsonMulti));

        final BatchLatestExecutionsDto result = dataService.getBatchLatestTestExecutionsSummary(
            "Batch A",
            List.of(runMulti),
            Collections.emptySet(),
            null,
            Set.of("Chrome"),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptyMap()
        );

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getEligibleRunsCount());
        Assertions.assertEquals(1, result.getContributingRuns().size());
        // Firefox execution should be excluded from variations
        Assertions.assertEquals(1, result.getLatestVariations().size());
        Assertions.assertEquals("Chrome", result.getLatestVariations().get(0).getBrowser());
        // Ratio should show only Chrome (100%)
        Assertions.assertEquals(1, result.getBrowserRatios().size());
        Assertions.assertEquals("Chrome", result.getBrowserRatios().get(0).getName());
        Assertions.assertEquals(100.0, result.getBrowserRatios().get(0).getPercentage(), 0.01);
    }

    @Test
    public void testBatchTestBaseClassesFilteredByRunDimensions()
    {
        final TestRunEntity runMulti = new TestRunEntity();
        runMulti.setId("RUN_MULTI_DIM");
        runMulti.setBatchName("BatchDim");
        runMulti.setEnvironment("Staging");
        runMulti.setBrowsersCsv("Chrome,Firefox");
        runMulti.setLocalesCsv("US,DE");
        runMulti.setStartTimeMs(1000L);

        final String jsonMulti = """
            {
              "executions": [
                {
                  "testClass": "com.xceptance.neodymium.test.CheckoutTest",
                  "testMethod": "testCheckout",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "areaName": "@Checkout",
                  "status": "passed-clean",
                  "durationMs": 1000
                },
                {
                  "testClass": "com.xceptance.neodymium.test.SearchTest",
                  "testMethod": "testSearch",
                  "title": "Default",
                  "location": "DE",
                  "browser": "Firefox",
                  "areaName": "@Search",
                  "status": "passed-clean",
                  "durationMs": 1000
                }
              ]
            }
            """;
        Mockito.when(storageService.readRunJson("RUN_MULTI_DIM")).thenReturn(Optional.of(jsonMulti));

        // 1. Filter by Browser = Chrome: only CheckoutTest should remain
        final List<TestBaseClassDto> chromeClasses = dataService.getBatchTestBaseClasses(
            "BatchDim",
            List.of(runMulti),
            Set.of("Chrome"),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet()
        );
        Assertions.assertEquals(1, chromeClasses.size());
        Assertions.assertEquals("com.xceptance.neodymium.test.CheckoutTest", chromeClasses.get(0).getClassName());

        // 2. Filter by Area = @Search: only SearchTest should remain
        final List<TestBaseClassDto> searchClasses = dataService.getBatchTestBaseClasses(
            "BatchDim",
            List.of(runMulti),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Set.of("@Search"),
            Collections.emptySet()
        );
        Assertions.assertEquals(1, searchClasses.size());
        Assertions.assertEquals("com.xceptance.neodymium.test.SearchTest", searchClasses.get(0).getClassName());

        // 3. Filter by _none_: 0 variations
        final List<TestBaseClassDto> noneClasses = dataService.getBatchTestBaseClasses(
            "BatchDim",
            List.of(runMulti),
            Set.of("_none_"),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet()
        );
        Assertions.assertTrue(noneClasses.isEmpty());
    }

    @Test
    public void testBatchTestBaseClassesVariationCountReflectsHistoryLimitAndEligibleRuns()
    {
        final TestRunEntity runLatest = new TestRunEntity();
        runLatest.setId("RUN_LATEST");
        runLatest.setBatchName("HistoryBatch");
        runLatest.setStartTimeMs(3000L);

        final TestRunEntity runMiddle = new TestRunEntity();
        runMiddle.setId("RUN_MIDDLE");
        runMiddle.setBatchName("HistoryBatch");
        runMiddle.setStartTimeMs(2000L);

        final TestRunEntity runOldest = new TestRunEntity();
        runOldest.setId("RUN_OLDEST");
        runOldest.setBatchName("HistoryBatch");
        runOldest.setStartTimeMs(1000L);

        final String jsonLatest = """
            {
              "executions": [
                {
                  "testClass": "com.xceptance.neodymium.test.ClassA",
                  "testMethod": "testMethodA",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "passed-clean"
                }
              ]
            }
            """;
        final String jsonMiddle = """
            {
              "executions": [
                {
                  "testClass": "com.xceptance.neodymium.test.ClassA",
                  "testMethod": "testMethodA",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "passed-clean"
                },
                {
                  "testClass": "com.xceptance.neodymium.test.ClassB",
                  "testMethod": "testMethodB",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "passed-clean"
                }
              ]
            }
            """;
        final String jsonOldest = """
            {
              "executions": [
                {
                  "testClass": "com.xceptance.neodymium.test.ClassA",
                  "testMethod": "testMethodA",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "passed-clean"
                },
                {
                  "testClass": "com.xceptance.neodymium.test.ClassB",
                  "testMethod": "testMethodB",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "passed-clean"
                },
                {
                  "testClass": "com.xceptance.neodymium.test.ClassC",
                  "testMethod": "testMethodC",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "passed-clean"
                }
              ]
            }
            """;
        Mockito.when(storageService.readRunJson("RUN_LATEST")).thenReturn(Optional.of(jsonLatest));
        Mockito.when(storageService.readRunJson("RUN_MIDDLE")).thenReturn(Optional.of(jsonMiddle));
        Mockito.when(storageService.readRunJson("RUN_OLDEST")).thenReturn(Optional.of(jsonOldest));

        // Limit = 1: only RUN_LATEST
        final List<TestBaseClassDto> classesLimit1 = dataService.getBatchTestBaseClasses("HistoryBatch", List.of(runLatest));
        Assertions.assertEquals(1, classesLimit1.size());
        Assertions.assertEquals("com.xceptance.neodymium.test.ClassA", classesLimit1.get(0).getClassName());
        Assertions.assertEquals(1, classesLimit1.get(0).getVariations().get(0).getTotalExecutionsCount());

        // Limit = 2: RUN_LATEST + RUN_MIDDLE
        final List<TestBaseClassDto> classesLimit2 = dataService.getBatchTestBaseClasses("HistoryBatch", List.of(runLatest, runMiddle));
        Assertions.assertEquals(2, classesLimit2.size());
        final int execCountA2 = classesLimit2.stream().filter(c -> c.getClassName().endsWith("ClassA")).findFirst().get().getVariations().get(0).getTotalExecutionsCount();
        final int execCountB2 = classesLimit2.stream().filter(c -> c.getClassName().endsWith("ClassB")).findFirst().get().getVariations().get(0).getTotalExecutionsCount();
        Assertions.assertEquals(2, execCountA2);
        Assertions.assertEquals(1, execCountB2);

        // Limit = 3: All 3 runs
        final List<TestBaseClassDto> classesLimit3 = dataService.getBatchTestBaseClasses("HistoryBatch", List.of(runLatest, runMiddle, runOldest));
        Assertions.assertEquals(3, classesLimit3.size());
        final int execCountA3 = classesLimit3.stream().filter(c -> c.getClassName().endsWith("ClassA")).findFirst().get().getVariations().get(0).getTotalExecutionsCount();
        final int execCountB3 = classesLimit3.stream().filter(c -> c.getClassName().endsWith("ClassB")).findFirst().get().getVariations().get(0).getTotalExecutionsCount();
        final int execCountC3 = classesLimit3.stream().filter(c -> c.getClassName().endsWith("ClassC")).findFirst().get().getVariations().get(0).getTotalExecutionsCount();
        Assertions.assertEquals(3, execCountA3);
        Assertions.assertEquals(2, execCountB3);
        Assertions.assertEquals(1, execCountC3);

        // Verify getBatchLatestTestExecutionsSummary retains eligibleRuns
        final BatchLatestExecutionsDto summaryDto = dataService.getBatchLatestTestExecutionsSummary("HistoryBatch", List.of(runLatest, runMiddle));
        Assertions.assertNotNull(summaryDto.getEligibleRuns());
        Assertions.assertEquals(2, summaryDto.getEligibleRuns().size());
        Assertions.assertEquals("RUN_LATEST", summaryDto.getEligibleRuns().get(0).getId());
        Assertions.assertEquals("RUN_MIDDLE", summaryDto.getEligibleRuns().get(1).getId());
    }

    @Test
    public void testGetGroupOverviewDataWithAreaGrouping()
    {
        final TestRunEntity run1 = new TestRunEntity();
        run1.setId("RUN_AREA_1");
        run1.setBatchName("SmokeBatch");
        run1.setEnvironment("Default");
        run1.setBrowsersCsv("Chrome");
        run1.setLocalesCsv("en_US");
        run1.setAreasCsv("@Checkout");
        run1.setStartTimeMs(1000L);

        final TestRunEntity run2 = new TestRunEntity();
        run2.setId("RUN_AREA_2");
        run2.setBatchName("SmokeBatch");
        run2.setEnvironment("Default");
        run2.setBrowsersCsv("Chrome");
        run2.setLocalesCsv("en_US");
        run2.setAreasCsv("@Cart");
        run2.setStartTimeMs(2000L);

        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of(run2, run1));

        final BatchOverviewDataDto data = dataService.getGroupOverviewData(List.of("area"));
        Assertions.assertNotNull(data);
        final List<BatchSummaryDto> batches = data.getBatches();
        Assertions.assertEquals(2, batches.size());
        final List<String> groupNames = batches.stream().map(BatchSummaryDto::getName).sorted().toList();
        Assertions.assertEquals(List.of("@Cart", "@Checkout"), groupNames);
    }

    @Test
    public void testBatchLatestContributingRunsScopedToAreaGrouping()
    {
        final TestRunEntity run1 = new TestRunEntity();
        run1.setId("RUN_MIXED_1");
        run1.setBatchName("SmokeBatch");
        run1.setStartTimeMs(1000L);
        run1.setBrowsersCsv("Chrome,Firefox");
        run1.setLocalesCsv("US,DE");
        run1.setTotalTests(2);

        final TestRunEntity run2 = new TestRunEntity();
        run2.setId("RUN_MIXED_2");
        run2.setBatchName("SmokeBatch");
        run2.setStartTimeMs(2000L);
        run2.setBrowsersCsv("Chrome");
        run2.setLocalesCsv("US");
        run2.setTotalTests(2);

        final String jsonRun1 = """
            {
              "executions": [
                {
                  "testClass": "com.xceptance.neodymium.test.SearchTest",
                  "testMethod": "testSearchBasic",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "areaName": "@Search",
                  "status": "passed-clean"
                },
                {
                  "testClass": "com.xceptance.neodymium.test.BrowsingTest",
                  "testMethod": "testBrowseCatalog",
                  "title": "Default",
                  "location": "DE",
                  "browser": "Firefox",
                  "areaName": "@Browsing",
                  "status": "failed-unknown"
                }
              ]
            }
            """;

        final String jsonRun2 = """
            {
              "executions": [
                {
                  "testClass": "com.xceptance.neodymium.test.SearchTest",
                  "testMethod": "testSearchAdvanced",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "areaName": "@Search",
                  "status": "passed-clean"
                },
                {
                  "testClass": "com.xceptance.neodymium.test.CheckoutTest",
                  "testMethod": "testCheckoutPayment",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "areaName": "@Checkout",
                  "status": "passed-clean"
                }
              ]
            }
            """;

        Mockito.when(storageService.readRunJson("RUN_MIXED_1")).thenReturn(Optional.of(jsonRun1));
        Mockito.when(storageService.readRunJson("RUN_MIXED_2")).thenReturn(Optional.of(jsonRun2));

        final BatchLatestExecutionsDto result = dataService.getBatchLatestTestExecutionsSummary(
            "all",
            List.of(run2, run1),
            Collections.emptySet(),
            null,
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Set.of("@Search"),
            Collections.emptySet(),
            Collections.emptyMap()
        );

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.getBatchSummary().getLatestTotalTests());
        Assertions.assertEquals(2, result.getBatchSummary().getLatestPassedCount());

        final List<TestRunEntity> contributingRuns = result.getContributingRuns();
        Assertions.assertNotNull(contributingRuns);
        Assertions.assertEquals(2, contributingRuns.size());

        // Each contributing run must only report the executions in @Search (1 test each, not 2)
        final TestRunEntity scopedRun2 = contributingRuns.get(0);
        Assertions.assertEquals("RUN_MIXED_2", scopedRun2.getId());
        Assertions.assertEquals(1, scopedRun2.getTotalTests(), "Contributing run should only count executions in @Search");
        Assertions.assertEquals(1, scopedRun2.getPassedCleanCount());
        Assertions.assertEquals(0, scopedRun2.getFailedUnknownCount());

        final TestRunEntity scopedRun1 = contributingRuns.get(1);
        Assertions.assertEquals("RUN_MIXED_1", scopedRun1.getId());
        Assertions.assertEquals(1, scopedRun1.getTotalTests(), "Contributing run should only count executions in @Search, not other areas");
        Assertions.assertEquals(1, scopedRun1.getPassedCleanCount());
        Assertions.assertEquals(0, scopedRun1.getFailedUnknownCount());

        // Test scopeRunToCriteria directly as well
        final TestRunEntity directScoped1 = dataService.scopeRunToCriteria(
            run1,
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Set.of("@Search"),
            Collections.emptySet()
        );
        Assertions.assertNotNull(directScoped1);
        Assertions.assertEquals(1, directScoped1.getTotalTests());
        Assertions.assertEquals(1, directScoped1.getPassedCount());
        Assertions.assertEquals(0, directScoped1.getFailedUnknownCount());
        Assertions.assertEquals("Chrome", directScoped1.getBrowsersCsv());
    }

    @Test
    public void testBatchLatestTestExecutionsSummaryWithExcludedDimensions()
    {
        final TestRunEntity run1 = new TestRunEntity();
        run1.setId("RUN_EXCLUDE_1");
        run1.setBatchName("SmokeBatch");
        run1.setEnvironment("Staging");
        run1.setStartTimeMs(1000L);
        run1.setBrowsersCsv("Chrome,Firefox");
        run1.setLocalesCsv("US,DE");
        run1.setTotalTests(2);
        run1.setPassedCount(1);
        run1.setFailedUnknownCount(1);

        final TestRunEntity run2 = new TestRunEntity();
        run2.setId("RUN_EXCLUDE_2");
        run2.setBatchName("SmokeBatch");
        run2.setEnvironment("Staging");
        run2.setStartTimeMs(2000L);
        run2.setBrowsersCsv("Chrome");
        run2.setLocalesCsv("US");
        run2.setTotalTests(2);
        run2.setPassedCount(2);

        final String jsonRun1 = """
            {
              "executions": [
                {
                  "testClass": "com.xceptance.neodymium.test.SearchTest",
                  "testMethod": "testSearchBasic",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "areaName": "@Search",
                  "status": "passed-clean"
                },
                {
                  "testClass": "com.xceptance.neodymium.test.BrowsingTest",
                  "testMethod": "testBrowseCatalog",
                  "title": "Default",
                  "location": "DE",
                  "browser": "Firefox",
                  "areaName": "@Browsing",
                  "status": "failed-unknown"
                }
              ]
            }
            """;

        final String jsonRun2 = """
            {
              "executions": [
                {
                  "testClass": "com.xceptance.neodymium.test.SearchTest",
                  "testMethod": "testSearchAdvanced",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "areaName": "@Search",
                  "status": "passed-clean"
                },
                {
                  "testClass": "com.xceptance.neodymium.test.CheckoutTest",
                  "testMethod": "testCheckoutPayment",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "areaName": "@Checkout",
                  "status": "passed-clean"
                }
              ]
            }
            """;

        Mockito.when(storageService.readRunJson("RUN_EXCLUDE_1")).thenReturn(Optional.of(jsonRun1));
        Mockito.when(storageService.readRunJson("RUN_EXCLUDE_2")).thenReturn(Optional.of(jsonRun2));

        final BatchLatestExecutionsDto result = dataService.getBatchLatestTestExecutionsSummary(
            "SmokeBatch",
            List.of(run2, run1),
            Collections.emptySet(),
            null,
            null,
            Collections.emptySet(),
            Set.of("Firefox"),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptyMap(),
            Collections.emptyMap(),
            Collections.emptyMap()
        );

        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.getBatchSummary().getLatestTotalTests(), "Total tests must exclude the Firefox execution");
        Assertions.assertEquals(3, result.getBatchSummary().getLatestPassedCount());
        Assertions.assertEquals(0, result.getBatchSummary().getLatestUnknownCount());

        final List<TestRunEntity> contributingRuns = result.getContributingRuns();
        Assertions.assertNotNull(contributingRuns);
        Assertions.assertEquals(2, contributingRuns.size());

        final TestRunEntity scopedRun1 = contributingRuns.stream()
            .filter(r -> "RUN_EXCLUDE_1".equals(r.getId()))
            .findFirst()
            .orElse(null);
        Assertions.assertNotNull(scopedRun1);
        Assertions.assertEquals(1, scopedRun1.getTotalTests(), "RUN_EXCLUDE_1 should only count the non-excluded Chrome execution");
        Assertions.assertEquals(1, scopedRun1.getPassedCleanCount());
        Assertions.assertEquals(0, scopedRun1.getFailedUnknownCount());
        Assertions.assertEquals("Chrome", scopedRun1.getBrowsersCsv());

        final TestRunEntity envExcluded = dataService.scopeRunToCriteria(
            run1,
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Set.of("Staging"),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            null,
            Collections.emptyMap(),
            Collections.emptyMap()
        );
        Assertions.assertNotNull(envExcluded);
        Assertions.assertEquals(0, envExcluded.getTotalTests(), "Run with excluded environment must produce an empty scoped run");

        final TestRunEntity areaExcluded = dataService.scopeRunToCriteria(
            run1,
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Set.of("@Browsing"),
            Collections.emptySet(),
            Collections.emptySet(),
            null,
            Collections.emptyMap(),
            Collections.emptyMap()
        );
        Assertions.assertNotNull(areaExcluded);
        Assertions.assertEquals(1, areaExcluded.getTotalTests(), "Must exclude @Browsing execution");
        Assertions.assertEquals(1, areaExcluded.getPassedCleanCount());
        Assertions.assertEquals(0, areaExcluded.getFailedUnknownCount());
    }

    @Test
    public void testGroupOverviewBooleanTagNamingAndUntaggedRunInclusion()
    {
        final TestRunEntity run1 = new TestRunEntity();
        run1.setId("RUN_BOOL_1");
        run1.setBatchName("SmokeBatch");
        run1.setEnvironment("Default");
        run1.setBrowsersCsv("Chrome");
        run1.setLocalesCsv("Default");
        run1.setAreasCsv("@Browsing (default)");
        run1.setStartTimeMs(1000L);
        run1.setTotalTests(1);
        run1.setPassedCount(1);

        final TestRunEntity run2 = new TestRunEntity();
        run2.setId("RUN_BOOL_2");
        run2.setBatchName("SmokeBatch");
        run2.setEnvironment("Default");
        run2.setBrowsersCsv("Chrome");
        run2.setLocalesCsv("Default");
        run2.setAreasCsv("@Browsing (default)");
        run2.setStartTimeMs(2000L);
        run2.setTotalTests(1);
        run2.setPassedCount(1);

        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of(run2, run1));

        final TestRunTagEntity tag1 = new TestRunTagEntity("RUN_BOOL_1", "Custom", "true");
        Mockito.when(tagRepository.findByRunIdIn(Mockito.anyCollection())).thenReturn(List.of(tag1));
        Mockito.when(tagRepository.findByRunId("RUN_BOOL_1")).thenReturn(List.of(tag1));
        Mockito.when(tagRepository.findByRunId("RUN_BOOL_2")).thenReturn(Collections.emptyList());
        Mockito.when(tagRepository.findDistinctTagValuesByTagKey("Custom")).thenReturn(List.of("true"));

        final String jsonRun1 = """
            {
              "runId": "RUN_BOOL_1",
              "batch": "SmokeBatch",
              "executions": [
                {
                  "id": "e1",
                  "testClass": "TestA",
                  "testMethod": "testRun",
                  "title": "ds1",
                  "location": "Default",
                  "browser": "Chrome",
                  "area": "Browsing (default)",
                  "status": "passed-clean"
                }
              ]
            }
            """;
        final String jsonRun2 = """
            {
              "runId": "RUN_BOOL_2",
              "batch": "SmokeBatch",
              "executions": [
                {
                  "id": "e2",
                  "testClass": "TestA",
                  "testMethod": "testRun",
                  "title": "ds1",
                  "location": "Default",
                  "browser": "Chrome",
                  "area": "Browsing (default)",
                  "status": "passed-clean"
                }
              ]
            }
            """;
        Mockito.when(storageService.readRunJson("RUN_BOOL_1")).thenReturn(Optional.of(jsonRun1));
        Mockito.when(storageService.readRunJson("RUN_BOOL_2")).thenReturn(Optional.of(jsonRun2));

        final BatchOverviewDataDto data = dataService.getGroupOverviewData(List.of("Custom"));
        Assertions.assertNotNull(data);
        final List<BatchSummaryDto> batches = data.getBatches();
        Assertions.assertEquals(2, batches.size());

        final BatchSummaryDto customCard = batches.stream().filter(b -> "Custom".equals(b.getName())).findFirst().orElse(null);
        final BatchSummaryDto notCustomCard = batches.stream().filter(b -> "Not Custom".equals(b.getName())).findFirst().orElse(null);

        Assertions.assertNotNull(customCard, "Group card for boolean true must be named 'Custom'");
        Assertions.assertEquals(1, customCard.getLatestTotalTests());
        Assertions.assertEquals(1, customCard.getLatestPassedCount());

        Assertions.assertNotNull(notCustomCard, "Group card for untagged runs must be named 'Not Custom'");
        Assertions.assertEquals(1, notCustomCard.getLatestTotalTests(), "Untagged runs must not be dropped and must contain tests");
        Assertions.assertEquals(1, notCustomCard.getLatestPassedCount());
    }

    @Test
    public void testGroupOverviewNonBooleanTagUntaggedRunInclusion()
    {
        final TestRunEntity run1 = new TestRunEntity();
        run1.setId("RUN_REL_1");
        run1.setBatchName("SmokeBatch");
        run1.setEnvironment("Default");
        run1.setBrowsersCsv("Chrome");
        run1.setLocalesCsv("Default");
        run1.setAreasCsv("@Browsing (default)");
        run1.setStartTimeMs(1000L);
        run1.setTotalTests(1);
        run1.setPassedCount(1);

        final TestRunEntity run2 = new TestRunEntity();
        run2.setId("RUN_REL_2");
        run2.setBatchName("SmokeBatch");
        run2.setEnvironment("Default");
        run2.setBrowsersCsv("Chrome");
        run2.setLocalesCsv("Default");
        run2.setAreasCsv("@Browsing (default)");
        run2.setStartTimeMs(2000L);
        run2.setTotalTests(1);
        run2.setPassedCount(1);

        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of(run2, run1));

        final TestRunTagEntity tag1 = new TestRunTagEntity("RUN_REL_1", "Release", "2026.10");
        Mockito.when(tagRepository.findByRunIdIn(Mockito.anyCollection())).thenReturn(List.of(tag1));
        Mockito.when(tagRepository.findByRunId("RUN_REL_1")).thenReturn(List.of(tag1));
        Mockito.when(tagRepository.findByRunId("RUN_REL_2")).thenReturn(Collections.emptyList());
        Mockito.when(tagRepository.findDistinctTagValuesByTagKey("Release")).thenReturn(List.of("2026.10"));

        final String jsonRun1 = """
            {
              "runId": "RUN_REL_1",
              "batch": "SmokeBatch",
              "executions": [
                {
                  "id": "e1",
                  "testClass": "TestA",
                  "testMethod": "testRun",
                  "title": "ds1",
                  "location": "Default",
                  "browser": "Chrome",
                  "area": "Browsing (default)",
                  "status": "passed-clean"
                }
              ]
            }
            """;
        final String jsonRun2 = """
            {
              "runId": "RUN_REL_2",
              "batch": "SmokeBatch",
              "executions": [
                {
                  "id": "e2",
                  "testClass": "TestA",
                  "testMethod": "testRun",
                  "title": "ds1",
                  "location": "Default",
                  "browser": "Chrome",
                  "area": "Browsing (default)",
                  "status": "passed-clean"
                }
              ]
            }
            """;
        Mockito.when(storageService.readRunJson("RUN_REL_1")).thenReturn(Optional.of(jsonRun1));
        Mockito.when(storageService.readRunJson("RUN_REL_2")).thenReturn(Optional.of(jsonRun2));

        final BatchOverviewDataDto data = dataService.getGroupOverviewData(List.of("Release"));
        Assertions.assertNotNull(data);
        final List<BatchSummaryDto> batches = data.getBatches();
        Assertions.assertEquals(2, batches.size());

        final BatchSummaryDto releaseCard = batches.stream().filter(b -> "2026.10".equals(b.getName())).findFirst().orElse(null);
        final BatchSummaryDto defaultCard = batches.stream().filter(b -> "Default".equals(b.getName())).findFirst().orElse(null);

        Assertions.assertNotNull(releaseCard);
        Assertions.assertEquals(1, releaseCard.getLatestTotalTests());

        Assertions.assertNotNull(defaultCard, "Group card for untagged runs must be named 'Default'");
        Assertions.assertEquals(1, defaultCard.getLatestTotalTests(), "Untagged runs in Default must not be dropped");
    }

    @Test
    public void testBatchLatestWithEnvironmentUniquenessPreservesDevAndStg()
    {
        final TestRunEntity runDev = new TestRunEntity("RUN_DEV", "SmokeBatch", "COMPLETED", "Main", "DEV", "Java", "Chrome", "10:00", 1000L);
        runDev.setEnvironment("DEV");
        final TestRunEntity runStg = new TestRunEntity("RUN_STG", "SmokeBatch", "COMPLETED", "Main", "STG", "Java", "Chrome", "11:00", 2000L);
        runStg.setEnvironment("STG");

        final String jsonDev = """
            {
              "runId": "RUN_DEV",
              "batch": "SmokeBatch",
              "executions": [
                {
                  "id": "e_dev",
                  "testClass": "com.xceptance.neodymium.test.LoginTest",
                  "testMethod": "testLogin",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "area": "Browsing (default)",
                  "status": "passed-clean",
                  "durationMs": 1200
                }
              ]
            }
            """;
        final String jsonStg = """
            {
              "runId": "RUN_STG",
              "batch": "SmokeBatch",
              "executions": [
                {
                  "id": "e_stg",
                  "testClass": "com.xceptance.neodymium.test.LoginTest",
                  "testMethod": "testLogin",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "area": "Browsing (default)",
                  "status": "passed-clean",
                  "durationMs": 1300
                }
              ]
            }
            """;

        Mockito.when(storageService.readRunJson("RUN_DEV")).thenReturn(Optional.of(jsonDev));
        Mockito.when(storageService.readRunJson("RUN_STG")).thenReturn(Optional.of(jsonStg));

        final List<TestRunEntity> allRuns = List.of(runDev, runStg);

        // 1. Default uniqueness: browser + locale only.
        // runStg is newer (2000L > 1000L), so it clobbers runDev.
        final BatchLatestExecutionsDto defaultResult = dataService.getBatchLatestTestExecutionsSummary(
            "SmokeBatch",
            allRuns,
            List.of("browser", "locale")
        );
        Assertions.assertNotNull(defaultResult);
        Assertions.assertEquals(1, defaultResult.getContributingRuns().size(), "Default uniqueness should keep only latest run");
        Assertions.assertEquals("RUN_STG", defaultResult.getContributingRuns().get(0).getId());
        Assertions.assertEquals(1, defaultResult.getLatestVariations().size());
        Assertions.assertEquals("STG", defaultResult.getLatestVariations().get(0).getEnvironment());

        // 2. Custom uniqueness: browser + locale + environment.
        // DEV and STG have different environments, so both are considered unique targets and preserved.
        final BatchLatestExecutionsDto envResult = dataService.getBatchLatestTestExecutionsSummary(
            "SmokeBatch",
            allRuns,
            List.of("browser", "locale", "environment")
        );
        Assertions.assertNotNull(envResult);
        Assertions.assertEquals(2, envResult.getContributingRuns().size(), "Environment uniqueness should preserve both DEV and STG runs");
        Assertions.assertEquals(2, envResult.getLatestVariations().size(), "Both DEV and STG variations should be included in latest status");
        final Set<String> envs = Set.of(
            envResult.getLatestVariations().get(0).getEnvironment(),
            envResult.getLatestVariations().get(1).getEnvironment()
        );
        Assertions.assertTrue(envs.contains("DEV"));
        Assertions.assertTrue(envs.contains("STG"));
    }

    @Test
    public void testBatchLatestWithTagUniquenessPreservesReleases()
    {
        final TestRunEntity runR21 = new TestRunEntity("RUN_R21", "SmokeBatch", "COMPLETED", "Main", "QA", "Java", "Chrome", "10:00", 1000L);
        runR21.setTags(List.of(new TestRunTagEntity("RUN_R21", "Release", "21")));

        final TestRunEntity runR22 = new TestRunEntity("RUN_R22", "SmokeBatch", "COMPLETED", "Main", "QA", "Java", "Chrome", "11:00", 2000L);
        runR22.setTags(List.of(new TestRunTagEntity("RUN_R22", "Release", "22")));

        final String jsonR21 = """
            {
              "runId": "RUN_R21",
              "batch": "SmokeBatch",
              "executions": [
                {
                  "id": "e_r21",
                  "testClass": "com.xceptance.neodymium.test.LoginTest",
                  "testMethod": "testLogin",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "area": "Browsing (default)",
                  "status": "passed-clean",
                  "durationMs": 1200
                }
              ]
            }
            """;
        final String jsonR22 = """
            {
              "runId": "RUN_R22",
              "batch": "SmokeBatch",
              "executions": [
                {
                  "id": "e_r22",
                  "testClass": "com.xceptance.neodymium.test.LoginTest",
                  "testMethod": "testLogin",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "area": "Browsing (default)",
                  "status": "passed-clean",
                  "durationMs": 1300
                }
              ]
            }
            """;

        Mockito.when(storageService.readRunJson("RUN_R21")).thenReturn(Optional.of(jsonR21));
        Mockito.when(storageService.readRunJson("RUN_R22")).thenReturn(Optional.of(jsonR22));

        final List<TestRunEntity> allRuns = List.of(runR21, runR22);

        // 1. Without Release tag in uniqueness: R22 clobbers R21
        final BatchLatestExecutionsDto defaultResult = dataService.getBatchLatestTestExecutionsSummary(
            "SmokeBatch",
            allRuns,
            List.of("browser", "locale")
        );
        Assertions.assertNotNull(defaultResult);
        Assertions.assertEquals(1, defaultResult.getContributingRuns().size());
        Assertions.assertEquals("RUN_R22", defaultResult.getContributingRuns().get(0).getId());

        // 2. With Release tag in uniqueness: both Release 21 and Release 22 are preserved
        final BatchLatestExecutionsDto tagResult = dataService.getBatchLatestTestExecutionsSummary(
            "SmokeBatch",
            allRuns,
            List.of("browser", "locale", "Release")
        );
        Assertions.assertNotNull(tagResult);
        Assertions.assertEquals(2, tagResult.getContributingRuns().size(), "Release tag uniqueness should preserve both R21 and R22 runs");
        Assertions.assertEquals(2, tagResult.getLatestVariations().size());
    }

    @Test
    public void testBatchTestBaseClassesWithAndWithoutUniquenessDimensions()
    {
        final TestRunEntity runDev = new TestRunEntity("RUN_DEV", "SmokeBatch", "COMPLETED", "Main", "DEV", "Java", "Chrome", "10:00", 1000L);
        runDev.setEnvironment("DEV");
        final TestRunEntity runStg = new TestRunEntity("RUN_STG", "SmokeBatch", "COMPLETED", "Main", "STG", "Java", "Chrome", "11:00", 2000L);
        runStg.setEnvironment("STG");

        final String jsonDev = """
            {
              "runId": "RUN_DEV",
              "batch": "SmokeBatch",
              "executions": [
                {
                  "id": "e_dev",
                  "testClass": "com.xceptance.neodymium.test.LoginTest",
                  "testMethod": "testLogin",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "area": "Browsing (default)",
                  "status": "passed-clean",
                  "durationMs": 1200
                }
              ]
            }
            """;
        final String jsonStg = """
            {
              "runId": "RUN_STG",
              "batch": "SmokeBatch",
              "executions": [
                {
                  "id": "e_stg",
                  "testClass": "com.xceptance.neodymium.test.LoginTest",
                  "testMethod": "testLogin",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "area": "Browsing (default)",
                  "status": "passed-clean",
                  "durationMs": 1300
                }
              ]
            }
            """;

        Mockito.when(storageService.readRunJson("RUN_DEV")).thenReturn(Optional.of(jsonDev));
        Mockito.when(storageService.readRunJson("RUN_STG")).thenReturn(Optional.of(jsonStg));

        final List<TestRunEntity> allRuns = List.of(runDev, runStg);

        // 1. Without uniqueness dimensions: DEV and STG merge into 1 canonical variation
        final List<TestBaseClassDto> canonicalClasses = dataService.getBatchTestBaseClasses("SmokeBatch", allRuns);
        Assertions.assertEquals(1, canonicalClasses.size());
        Assertions.assertEquals(1, canonicalClasses.get(0).getVariations().size());
        Assertions.assertEquals(1, canonicalClasses.get(0).getVariationCount());

        // 2. With uniqueness dimensions: DEV and STG split into 2 variations, with uniquenessProperties populated
        final List<TestBaseClassDto> splitClasses = dataService.getBatchTestBaseClasses(
            "SmokeBatch",
            allRuns,
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            List.of("browser", "locale", "environment")
        );
        Assertions.assertEquals(1, splitClasses.size());
        Assertions.assertEquals(2, splitClasses.get(0).getVariations().size());
        final List<TestBaseVariationEntity> vars = splitClasses.get(0).getVariations();
        Assertions.assertTrue(vars.stream().anyMatch(v -> "DEV".equals(v.getIdentityProperty("Environment"))));
        Assertions.assertTrue(vars.stream().anyMatch(v -> "STG".equals(v.getIdentityProperty("Environment"))));
        Assertions.assertTrue(vars.stream().anyMatch(v -> "DEV".equals(v.getUniquenessProperty("Environment"))));
        Assertions.assertTrue(vars.stream().anyMatch(v -> "STG".equals(v.getUniquenessProperty("Environment"))));
    }

    @Test
    public void testBatchTestBaseClassesDefaultsToCanonicalUniquenessEvenWhenDashboardHasCustomDimensions()
    {
        final ReportDashboardSettingService mockSettingService = Mockito.mock(ReportDashboardSettingService.class);
        Mockito.when(mockSettingService.getExecutionIdentityDimensions()).thenReturn(List.of("browser", "locale", "environment"));
        Mockito.when(mockSettingService.getUniquenessDimensions()).thenReturn(List.of("browser", "locale", "environment"));

        final AuraReportDataService serviceWithSettings = new AuraReportDataService(
            runRepository,
            batchRepository,
            variationRepository,
            bugRepository,
            tagRepository,
            storageService,
            attachmentStorageService,
            null,
            mockSettingService
        );

        final TestRunEntity runDev = new TestRunEntity("RUN_DEV", "SmokeBatch", "COMPLETED", "Main", "DEV", "Java", "Chrome", "10:00", 1000L);
        runDev.setEnvironment("DEV");
        final TestRunEntity runStg = new TestRunEntity("RUN_STG", "SmokeBatch", "COMPLETED", "Main", "STG", "Java", "Chrome", "11:00", 2000L);
        runStg.setEnvironment("STG");

        final String jsonDev = """
            {
              "runId": "RUN_DEV",
              "batch": "SmokeBatch",
              "executions": [
                {
                  "id": "e_dev",
                  "testClass": "com.xceptance.neodymium.test.LoginTest",
                  "testMethod": "testLogin",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "area": "Browsing (default)",
                  "status": "passed-clean",
                  "durationMs": 1200
                }
              ]
            }
            """;
        final String jsonStg = """
            {
              "runId": "RUN_STG",
              "batch": "SmokeBatch",
              "executions": [
                {
                  "id": "e_stg",
                  "testClass": "com.xceptance.neodymium.test.LoginTest",
                  "testMethod": "testLogin",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "area": "Browsing (default)",
                  "status": "passed-clean",
                  "durationMs": 1300
                }
              ]
            }
            """;

        Mockito.when(storageService.readRunJson("RUN_DEV")).thenReturn(Optional.of(jsonDev));
        Mockito.when(storageService.readRunJson("RUN_STG")).thenReturn(Optional.of(jsonStg));

        final List<TestRunEntity> allRuns = List.of(runDev, runStg);

        // When uniquenessDimensions is not passed or empty: must merge DEV and STG into 1 canonical variation
        final List<TestBaseClassDto> defaultClasses = serviceWithSettings.getBatchTestBaseClasses("SmokeBatch", allRuns);
        Assertions.assertEquals(1, defaultClasses.size());
        Assertions.assertEquals(1, defaultClasses.get(0).getVariations().size());
        Assertions.assertEquals(1, defaultClasses.get(0).getVariationCount());

        // When explicit uniquenessDimensions is passed: must split DEV and STG into 2 variations
        final List<TestBaseClassDto> customClasses = serviceWithSettings.getBatchTestBaseClasses(
            "SmokeBatch",
            allRuns,
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            List.of("browser", "locale", "environment")
        );
        Assertions.assertEquals(1, customClasses.size());
        Assertions.assertEquals(2, customClasses.get(0).getVariations().size());
        Assertions.assertEquals(2, customClasses.get(0).getVariationCount());
    }

    @Test
    public void testBatchLatestExecutionsPreservesVariationSpecificTagsInRatioCardAndDto()
    {
        final TestRunEntity run = new TestRunEntity("RUN_MULTI_LOC", "MultiBatch", "COMPLETED", "Main", "STG", "Java", "Chrome", "10:00", 1000L);
        final TestRunTagEntity tagEng = new TestRunTagEntity("RUN_MULTI_LOC", "locale", "ENG");
        final TestRunTagEntity tagRus = new TestRunTagEntity("RUN_MULTI_LOC", "locale", "RUS");
        Mockito.when(tagRepository.findByRunIdIn(Mockito.anyList())).thenReturn(List.of(tagEng, tagRus));

        final String runJson = """
            {
              "runId": "RUN_MULTI_LOC",
              "batch": "MultiBatch",
              "executions": [
                {
                  "id": "exec_en",
                  "testClass": "com.xceptance.neodymium.test.LoginTest",
                  "testMethod": "testLogin",
                  "title": "en",
                  "location": "Default",
                  "browser": "Chrome",
                  "area": "Browsing (default)",
                  "status": "passed-clean",
                  "durationMs": 1200,
                  "tags": {
                    "locale": ["ENG"]
                  }
                },
                {
                  "id": "exec_ru",
                  "testClass": "com.xceptance.neodymium.test.LoginTest",
                  "testMethod": "testLogin",
                  "title": "ru",
                  "location": "Default",
                  "browser": "Chrome",
                  "area": "Browsing (default)",
                  "status": "passed-clean",
                  "durationMs": 1300,
                  "tags": {
                    "locale": ["RUS"]
                  }
                }
              ]
            }
            """;

        Mockito.when(storageService.readRunJson("RUN_MULTI_LOC")).thenReturn(Optional.of(runJson));

        final BatchLatestExecutionsDto result = dataService.getBatchLatestTestExecutionsSummary("MultiBatch", List.of(run), (List<String>) null);
        Assertions.assertNotNull(result);

        final Map<String, List<DistributionRatioDto>> ratios = result.getTagRatiosByType();
        Assertions.assertNotNull(ratios);
        final List<DistributionRatioDto> localeRatios = ratios.get("locale");
        Assertions.assertNotNull(localeRatios, "locale ratio card should exist");
        Assertions.assertEquals(2, localeRatios.size(), "Should contain 2 distinct locale entries (ENG and RUS)");

        final DistributionRatioDto engRatio = localeRatios.stream()
            .filter(r -> "ENG".equals(r.getName()))
            .findFirst()
            .orElse(null);
        final DistributionRatioDto rusRatio = localeRatios.stream()
            .filter(r -> "RUS".equals(r.getName()))
            .findFirst()
            .orElse(null);

        Assertions.assertNotNull(engRatio, "ENG should be in ratio card");
        Assertions.assertEquals(1, engRatio.getCount());
        Assertions.assertNotNull(rusRatio, "RUS should be in ratio card");
        Assertions.assertEquals(1, rusRatio.getCount());

        final List<BatchLatestVariationDto> variations = result.getLatestVariations();
        Assertions.assertEquals(2, variations.size());

        final BatchLatestVariationDto enVar = variations.stream()
            .filter(v -> "ENG".equals(v.getTagValue("locale")))
            .findFirst()
            .orElse(null);
        final BatchLatestVariationDto ruVar = variations.stream()
            .filter(v -> "RUS".equals(v.getTagValue("locale")))
            .findFirst()
            .orElse(null);

        Assertions.assertNotNull(enVar);
        Assertions.assertEquals("ENG", enVar.getTags().get("locale"));

        Assertions.assertNotNull(ruVar);
        Assertions.assertEquals("RUS", ruVar.getTags().get("locale"));
    }

    @Test
    public void testBatchSuiteEvolutionCalculation()
    {
        final TestRunEntity run1 = new TestRunEntity("RUN_1", "Batch Evo", "COMPLETED", "Main", "QA", "Java", "Chrome", "10:00", 1000L);
        final TestRunEntity run2 = new TestRunEntity("RUN_2", "Batch Evo", "COMPLETED", "Main", "QA", "Java", "Chrome", "11:00", 2000L);
        final TestRunEntity run3 = new TestRunEntity("RUN_3", "Batch Evo", "COMPLETED", "Main", "QA", "Java", "Chrome", "12:00", 3000L);

        final String jsonRun1 = """
            {
              "runId": "RUN_1",
              "executions": [
                {
                  "testClass": "TestA",
                  "testMethod": "m1",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "passed-clean"
                },
                {
                  "testClass": "TestB",
                  "testMethod": "m2",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "failed-unknown"
                }
              ]
            }
            """;

        final String jsonRun2 = """
            {
              "runId": "RUN_2",
              "executions": [
                {
                  "testClass": "TestB",
                  "testMethod": "m2",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "passed-clean"
                },
                {
                  "testClass": "TestC",
                  "testMethod": "m3",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "passed-clean"
                }
              ]
            }
            """;

        final String jsonRun3 = """
            {
              "runId": "RUN_3",
              "executions": [
                {
                  "testClass": "TestD",
                  "testMethod": "m4",
                  "title": "Default",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "failed-unknown"
                }
              ]
            }
            """;

        Mockito.when(storageService.readRunJson("RUN_1")).thenReturn(Optional.of(jsonRun1));
        Mockito.when(storageService.readRunJson("RUN_2")).thenReturn(Optional.of(jsonRun2));
        Mockito.when(storageService.readRunJson("RUN_3")).thenReturn(Optional.of(jsonRun3));

        final BatchLatestExecutionsDto result = dataService.getBatchLatestTestExecutionsSummary("Batch Evo", List.of(run3, run2, run1));
        Assertions.assertNotNull(result);

        final BatchSuiteEvolutionDto evolution = result.getSuiteEvolution();
        Assertions.assertNotNull(evolution);
        Assertions.assertEquals(3, evolution.getPoints().size());
        Assertions.assertEquals(2, evolution.getInitialTestsCount());
        Assertions.assertEquals(4, evolution.getFinalTestsCount());
        Assertions.assertEquals(2, evolution.getNetTestsGrowth());
        Assertions.assertEquals(50.0, evolution.getInitialPassRate(), 0.01);
        Assertions.assertEquals(75.0, evolution.getFinalPassRate(), 0.01);
        Assertions.assertEquals(25.0, evolution.getPassRateDelta(), 0.01);
        Assertions.assertTrue(evolution.isPassRatePositive());
        Assertions.assertEquals("+2 tests", evolution.getGrowthLabel());

        final BatchSuiteEvolutionPointDto p0 = evolution.getPoints().get(0);
        Assertions.assertEquals("RUN_1", p0.getRunId());
        Assertions.assertEquals(2, p0.getTotalTestsKnown());
        Assertions.assertEquals(2, p0.getNewTestsCount());
        Assertions.assertEquals(1, p0.getPassCleanCount());
        Assertions.assertEquals(1, p0.getUnknownCleanCount());
        Assertions.assertEquals(50.0, p0.getPassRate(), 0.01);

        final BatchSuiteEvolutionPointDto p1 = evolution.getPoints().get(1);
        Assertions.assertEquals("RUN_2", p1.getRunId());
        Assertions.assertEquals(3, p1.getTotalTestsKnown());
        Assertions.assertEquals(1, p1.getNewTestsCount());
        Assertions.assertEquals(3, p1.getPassCleanCount());
        Assertions.assertEquals(0, p1.getUnknownCleanCount());
        Assertions.assertEquals(100.0, p1.getPassRate(), 0.01);

        final BatchSuiteEvolutionPointDto p2 = evolution.getPoints().get(2);
        Assertions.assertEquals("RUN_3", p2.getRunId());
        Assertions.assertEquals(4, p2.getTotalTestsKnown());
        Assertions.assertEquals(1, p2.getNewTestsCount());
        Assertions.assertEquals(3, p2.getPassCleanCount());
        Assertions.assertEquals(1, p2.getUnknownCleanCount());
        Assertions.assertEquals(75.0, p2.getPassRate(), 0.01);
    }

    @Test
    public void testHistoricalTimeTravelRewind()
    {
        final TestRunEntity run1 = new TestRunEntity("RUN_1", "Batch Evo", "COMPLETED", "Main", "STG", "Java", "Chrome", "2026-10-26 10:00", 1000L);
        final TestRunEntity run2 = new TestRunEntity("RUN_2", "Batch Evo", "COMPLETED", "Main", "STG", "Java", "Chrome", "2026-10-27 10:00", 2000L);
        final TestRunEntity run3 = new TestRunEntity("RUN_3", "Batch Evo", "COMPLETED", "Main", "STG", "Java", "Chrome", "2026-10-28 10:00", 3000L);

        final String jsonRun1 = """
            {
              "runId": "RUN_1",
              "batch": "Batch Evo",
              "executions": [
                {
                  "id": "e1_1",
                  "testClass": "com.xceptance.neodymium.test.LoginTest",
                  "testMethod": "testLogin",
                  "title": "US",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "passed-clean"
                },
                {
                  "id": "e1_2",
                  "testClass": "com.xceptance.neodymium.test.CheckoutTest",
                  "testMethod": "testCheckout",
                  "title": "US",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "failed-unknown"
                }
              ]
            }
            """;

        final String jsonRun2 = """
            {
              "runId": "RUN_2",
              "batch": "Batch Evo",
              "executions": [
                {
                  "id": "e2_1",
                  "testClass": "com.xceptance.neodymium.test.CheckoutTest",
                  "testMethod": "testCheckout",
                  "title": "US",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "passed-clean"
                },
                {
                  "id": "e2_2",
                  "testClass": "com.xceptance.neodymium.test.SearchTest",
                  "testMethod": "testSearch",
                  "title": "US",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "passed-clean"
                }
              ]
            }
            """;

        final String jsonRun3 = """
            {
              "runId": "RUN_3",
              "batch": "Batch Evo",
              "executions": [
                {
                  "id": "e3_1",
                  "testClass": "com.xceptance.neodymium.test.CartTest",
                  "testMethod": "testAddToCart",
                  "title": "US",
                  "location": "US",
                  "browser": "Chrome",
                  "status": "failed-unknown"
                }
              ]
            }
            """;

        Mockito.when(storageService.readRunJson("RUN_1")).thenReturn(Optional.of(jsonRun1));
        Mockito.when(storageService.readRunJson("RUN_2")).thenReturn(Optional.of(jsonRun2));
        Mockito.when(storageService.readRunJson("RUN_3")).thenReturn(Optional.of(jsonRun3));

        final BatchLatestExecutionsDto rewindResult = dataService.getBatchLatestTestExecutionsSummary(
            "Batch Evo",
            List.of(run3, run2, run1),
            Collections.emptySet(),
            null,
            null,
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptyMap(),
            Collections.emptyMap(),
            Collections.emptyMap(),
            null,
            "RUN_2"
        );

        Assertions.assertNotNull(rewindResult);
        Assertions.assertTrue(rewindResult.isHistoricalSnapshot());
        Assertions.assertEquals("RUN_2", rewindResult.getAsOfRunId());
        Assertions.assertEquals(run2.getTimestampLabel(), rewindResult.getAsOfTimestampLabel());

        final BatchSummaryDto summary = rewindResult.getBatchSummary();
        Assertions.assertNotNull(summary);
        Assertions.assertEquals(3, summary.getLatestTotalTests(), "At RUN_2 milestone, exactly 3 tests were known");
        Assertions.assertEquals(3, summary.getLatestPassedCount(), "At RUN_2 milestone, all 3 tests passed");
        Assertions.assertEquals(0, summary.getLatestUnknownCount(), "At RUN_2 milestone, zero unknown failures existed");
        Assertions.assertEquals("100% Pass", summary.getLatestPassRate());

        final List<BatchLatestVariationDto> variations = rewindResult.getLatestVariations();
        Assertions.assertEquals(3, variations.size(), "Only tests up to RUN_2 exist in historical snapshot");
        Assertions.assertTrue(variations.stream().noneMatch(v -> v.getKey().contains("CartTest")), "CartTest only ran in RUN_3 and must not appear in RUN_2 snapshot");

        final BatchSuiteEvolutionDto evolution = rewindResult.getSuiteEvolution();
        Assertions.assertNotNull(evolution);
        Assertions.assertEquals(3, evolution.getPoints().size(), "Full evolution curve must remain visible");
        Assertions.assertEquals("RUN_2", evolution.getActiveRunId(), "Active run on evolution curve must be RUN_2");
        Assertions.assertTrue(evolution.isHistoricalActive(), "Evolution should report historical active mode");
        Assertions.assertEquals(3, evolution.getActivePoint().getTotalTestsKnown());
    }
}



