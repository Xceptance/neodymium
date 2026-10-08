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

import com.xceptance.aura.report.dto.TestBaseAreaDto;
import com.xceptance.aura.report.dto.TestBaseClassDto;
import com.xceptance.aura.report.dto.TestBaseDataDto;
import com.xceptance.aura.report.dto.TestBaseVariationHistoryDto;
import com.xceptance.aura.report.entity.TestBaseVariationEntity;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.repository.TestBatchRepository;
import com.xceptance.aura.report.repository.TestBaseBugRepository;
import com.xceptance.aura.report.repository.TestBaseVariationRepository;
import com.xceptance.aura.report.repository.TestRunRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
        storageService = Mockito.mock(LocalRunJsonStorageService.class);
        attachmentStorageService = Mockito.mock(AttachmentStorageService.class);

        dataService = new AuraReportDataService(
            runRepository,
            batchRepository,
            variationRepository,
            bugRepository,
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
        // Test enriched relative URL with query parameters
        varEntity.setHistoryLinks("/run-report?runId=RUN_100&executionId=exec-100&batch=Nightly+Regression&engine=Java&ts=Yesterday&status=passed-clean&bugs=BUG-1%3BBUG-2");

        final TestRunEntity runEntity = new TestRunEntity("RUN_100", "Nightly Regression", "COMPLETED", "Staging", "Manual", "Java", "Chrome", "Yesterday", 5000L);

        Mockito.when(variationRepository.findById(varId)).thenReturn(Optional.of(varEntity));
        Mockito.when(runRepository.findAllById(List.of("RUN_100"))).thenReturn(List.of(runEntity));

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
        // Link stored with status=failed-known but empty bugs parameter
        varEntity.setHistoryLinks("/run-report?runId=RUN_200&executionId=exec-200&batch=Daily+Build&engine=Java&ts=Today&status=failed-known&bugs=");

        final TestRunEntity runEntity = new TestRunEntity("RUN_200", "Daily Build", "COMPLETED", "Staging", "Manual", "Java", "Chrome", "Today", 6000L);

        Mockito.when(variationRepository.findById(varId)).thenReturn(Optional.of(varEntity));
        Mockito.when(runRepository.findAllById(List.of("RUN_200"))).thenReturn(List.of(runEntity));

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
        varEntity.setHistoryLinks("/run-report?runId=RUN_HEALED_LINK&executionId=exec-hl-1&batch=Nightly&engine=Java&ts=Today&status=passed-clean&healed=true");

        final TestRunEntity runEntity = new TestRunEntity("RUN_HEALED_LINK", "Nightly", "COMPLETED", "Staging", "Manual", "Java", "Chrome", "Today", 7000L);

        Mockito.when(variationRepository.findById(varId)).thenReturn(Optional.of(varEntity));
        Mockito.when(runRepository.findAllById(List.of("RUN_HEALED_LINK"))).thenReturn(List.of(runEntity));

        final List<TestBaseVariationHistoryDto> history = dataService.getVariationHistory(testClass, dataSet, location, browser);

        Assertions.assertNotNull(history);
        Assertions.assertEquals(1, history.size());
        final TestBaseVariationHistoryDto dto = history.get(0);
        Assertions.assertEquals("RUN_HEALED_LINK", dto.getRunId());
        Assertions.assertTrue(dto.isHealed(), "DTO should be marked as healed");
        Assertions.assertTrue(dto.isPassedHealed(), "DTO should be passed healed");
        Assertions.assertFalse(dto.isFailedHealed(), "DTO should not be failed healed");
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
        // Links stored out of order (Sep 18, then Oct 7, then Sep 22)
        final String linkSep18 = "/run-report?runId=run_20260918_145300&executionId=exec-1&batch=Batch1&engine=Java&ts=2026-09-18+14%3A53%3A29&status=failed-unknown";
        final String linkOct07 = "/run-report?runId=run_20261007_155456&executionId=exec-2&batch=Batch2&engine=Java&ts=2026-10-07+15%3A55%3A37&status=passed-clean";
        final String linkSep22 = "/run-report?runId=run_20260922_100430&executionId=exec-3&batch=Batch3&engine=Java&ts=2026-09-22+10%3A04%3A50&status=failed-unknown";
        varEntity.setHistoryLinks(linkSep18 + "," + linkOct07 + "," + linkSep22);

        final TestRunEntity runSep18 = new TestRunEntity("run_20260918_145300", "Batch1", "COMPLETED", "Staging", "Manual", "Java", "Chrome", "2026-09-18 14:53:29", 1000L);
        final TestRunEntity runOct07 = new TestRunEntity("run_20261007_155456", "Batch2", "COMPLETED", "Staging", "Manual", "Java", "Chrome", "2026-10-07 15:55:37", 3000L);
        final TestRunEntity runSep22 = new TestRunEntity("run_20260922_100430", "Batch3", "COMPLETED", "Staging", "Manual", "Java", "Chrome", "2026-09-22 10:04:50", 2000L);

        Mockito.when(variationRepository.findById(varId)).thenReturn(Optional.of(varEntity));
        Mockito.when(runRepository.findAllById(Mockito.anyList())).thenReturn(List.of(runSep18, runOct07, runSep22));

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
    public void testSortHistoryLinksDescending()
    {
        final String linkSep18 = "/run-report?runId=run_20260918_145300&ts=2026-09-18+14%3A53%3A29&status=failed-unknown";
        final String linkOct07 = "/run-report?runId=run_20261007_155456&ts=2026-10-07+15%3A55%3A37&status=passed-clean";
        final String linkSep22 = "/run-report?runId=run_20260922_100430&ts=2026-09-22+10%3A04%3A50&status=failed-unknown";

        final List<String> sorted = AuraReportDataService.sortHistoryLinksDescending(linkSep18 + "," + linkOct07 + "," + linkSep22);
        Assertions.assertEquals(3, sorted.size());
        Assertions.assertTrue(sorted.get(0).contains("run_20261007_155456"), "Oct 07 should be first");
        Assertions.assertTrue(sorted.get(1).contains("run_20260922_100430"), "Sep 22 should be second");
        Assertions.assertTrue(sorted.get(2).contains("run_20260918_145300"), "Sep 18 should be third");
    }

    @Test
    public void testGetTestBaseDataUpdatesStaleStatusToLatestExecution()
    {
        final String testClass = "com.xceptance.neodymium.test.CheckoutTest";
        final String dataSet = "Payment_VISA";
        final String location = "US";
        final String browser = "Chrome";
        final String varId = AuraReportDataService.generateVariationId(testClass, dataSet, location, browser);

        final TestBaseVariationEntity varEntity = new TestBaseVariationEntity(varId, testClass, "testCheckout", dataSet, "@General", location, browser);
        // Entity previously had failed-unknown, but latest run is passed-clean
        varEntity.setLastStatus("failed-unknown");

        final String linkOlderFailed = "/run-report?runId=run_20260918_145300&ts=2026-09-18+14%3A53%3A29&status=failed-unknown";
        final String linkNewerPassed = "/run-report?runId=run_20261007_155456&ts=2026-10-07+15%3A55%3A37&status=passed-clean";
        varEntity.setHistoryLinks(linkOlderFailed + "," + linkNewerPassed);

        Mockito.when(variationRepository.findAllByOrderByLastExecutedAtDesc()).thenReturn(List.of(varEntity));
        Mockito.when(bugRepository.findAll()).thenReturn(List.of());
        Mockito.when(batchRepository.findAll()).thenReturn(List.of());
        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of());

        final TestBaseDataDto data = dataService.getTestBaseData();
        Assertions.assertNotNull(data);
        Assertions.assertEquals(1, data.getTotalVariationsCount());

        final TestBaseAreaDto area = data.getAreas().get(0);
        final TestBaseClassDto classDto = area.getTestClasses().get(0);
        final TestBaseVariationEntity updatedVar = classDto.getVariations().get(0);

        Assertions.assertEquals("passed-clean", updatedVar.getLastStatus(), "Last status should be updated to passed-clean from newest execution link");
        Assertions.assertEquals("passed-clean", varEntity.getLastStatus(), "Entity lastStatus should be updated");
        Assertions.assertFalse(varEntity.isHealed(), "Entity healed should be false for clean pass");

        Mockito.verify(variationRepository, Mockito.atLeastOnce()).save(varEntity);
    }

    @Test
    public void testGetTestBaseDataDerivesHealedFromLatestExecution()
    {
        final String testClass = "com.xceptance.neodymium.test.LoginTest";
        final String dataSet = "StandardUser";
        final String location = "DE";
        final String browser = "Firefox";
        final String varId = AuraReportDataService.generateVariationId(testClass, dataSet, location, browser);

        final TestBaseVariationEntity varEntity = new TestBaseVariationEntity(varId, testClass, "testLogin", dataSet, "@General", location, browser);
        varEntity.setLastStatus("failed-unknown");

        final String linkOlderPassed = "/run-report?runId=run_20260918_145300&ts=2026-09-18+14%3A53%3A29&status=passed-clean";
        final String linkNewerHealed = "/run-report?runId=run_20261007_155456&ts=2026-10-07+15%3A55%3A37&status=passed-clean&healed=true";
        varEntity.setHistoryLinks(linkOlderPassed + "," + linkNewerHealed);

        Mockito.when(variationRepository.findAllByOrderByLastExecutedAtDesc()).thenReturn(List.of(varEntity));
        Mockito.when(bugRepository.findAll()).thenReturn(List.of());
        Mockito.when(batchRepository.findAll()).thenReturn(List.of());
        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of());

        final TestBaseDataDto data = dataService.getTestBaseData();
        Assertions.assertNotNull(data);

        final TestBaseAreaDto area = data.getAreas().get(0);
        final TestBaseClassDto classDto = area.getTestClasses().get(0);
        final TestBaseVariationEntity updatedVar = classDto.getVariations().get(0);

        Assertions.assertEquals("healed", updatedVar.getLastStatus(), "Last status should be healed when latest execution link is healed");
        Assertions.assertTrue(updatedVar.isHealed(), "Variation should be marked as healed");
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
    public void testGetTestBaseDataReconcilesBrowserFromHistoryLinkAndMergesDuplicates()
    {
        final String testClass = "Aura_my_test_yaml_Test";
        final String testMethod = "executeYamlTest";
        final String dataSet = "en";
        final String location = "EN";

        // Obsolete variation created with fallback browser "Chrome"
        final String oldVarId = AuraReportDataService.generateVariationId(testClass, testMethod, dataSet, location, "Chrome");
        final TestBaseVariationEntity oldVar = new TestBaseVariationEntity(oldVarId, testClass, testMethod, dataSet, "@General", location, "Chrome");
        oldVar.setTotalExecutionsCount(77);
        oldVar.setLastStatus("passed-clean");
        final String runLink = "/run-report?runId=run_20261006_170254&executionId=Aura_my_test_yaml_Test%23executeYamlTest%23en%23Chrome_1920x1080&batch=Unknown&engine=Java&ts=2026-10-06T15%3A04%3A15.263Z&status=passed-clean";
        oldVar.setHistoryLinks(runLink);

        // Canonical variation created by live ingestion with exact browser "Chrome_1920x1080" and 0 history links
        final String canonicalVarId = AuraReportDataService.generateVariationId(testClass, testMethod, dataSet, location, "Chrome_1920x1080");
        final TestBaseVariationEntity canonicalVar = new TestBaseVariationEntity(canonicalVarId, testClass, testMethod, dataSet, "@General", location, "Chrome_1920x1080");
        canonicalVar.setTotalExecutionsCount(2);
        canonicalVar.setLastStatus("ignored");
        canonicalVar.setHistoryLinks(null);

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

        Assertions.assertEquals("Chrome_1920x1080", mergedVar.getBrowser(), "Merged variation must retain exact browser profile");
        Assertions.assertEquals(1, mergedVar.getTotalExecutionsCount(), "Total executions count should be derived from valid history links");
        Assertions.assertNotNull(mergedVar.getHistoryLinks(), "History links must be populated from the historical runs");
        Assertions.assertTrue(mergedVar.getHistoryLinks().contains("run_20261006_170254"));

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
}


