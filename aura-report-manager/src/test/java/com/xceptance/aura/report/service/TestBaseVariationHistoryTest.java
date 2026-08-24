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

import com.xceptance.aura.report.dto.TestBaseVariationHistoryDto;
import com.xceptance.aura.report.entity.TestBaseVariationEntity;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.repository.TestBatchRepository;
import com.xceptance.aura.report.repository.TestBaseBugRepository;
import com.xceptance.aura.report.repository.TestBaseVariationRepository;
import com.xceptance.aura.report.repository.TestRunRepository;
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
        Assertions.assertEquals("HEALED / FIXED", history.get(0).getStatusLabel());
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
        Assertions.assertEquals("FAILED", history.get(0).getStatusLabel());
        Assertions.assertTrue(history.get(0).getBugs().isEmpty());
    }
}
