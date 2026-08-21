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
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.repository.TestBatchRepository;
import com.xceptance.aura.report.repository.TestBaseBugRepository;
import com.xceptance.aura.report.repository.TestBaseVariationRepository;
import com.xceptance.aura.report.repository.TestRunRepository;
import java.util.List;
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
        Assertions.assertEquals("Chrome", AuraReportDataService.normalizeBrowser("Chrome_1500x1000"));
        Assertions.assertEquals("Chrome", AuraReportDataService.normalizeBrowser("chrome"));
        Assertions.assertEquals("Chrome", AuraReportDataService.normalizeBrowser("Chrome"));
        Assertions.assertEquals("Chrome", AuraReportDataService.normalizeBrowser("chrome_headless"));
        Assertions.assertEquals("Firefox", AuraReportDataService.normalizeBrowser("firefox"));
        Assertions.assertEquals("Firefox", AuraReportDataService.normalizeBrowser("ff_desktop"));
        Assertions.assertEquals("Edge", AuraReportDataService.normalizeBrowser("edge_headless"));
        Assertions.assertEquals("Safari", AuraReportDataService.normalizeBrowser("safari"));
        Assertions.assertEquals("Chrome", AuraReportDataService.normalizeBrowser(null));
        Assertions.assertEquals("Chrome", AuraReportDataService.normalizeBrowser("   "));
    }

    @Test
    public void testGetVariationHistoryMatchingBrowserProfiles()
    {
        final TestRunEntity run1 = new TestRunEntity("RUN_1", "Batch A", "COMPLETED", "Main", "Manual", "Java", "Chrome", "10:00", 1000L);
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

        final List<TestBaseVariationHistoryDto> history = dataService.getVariationHistory(
            "HomepageTest",
            "canyon_homepage_US",
            "US",
            "Chrome"
        );

        Assertions.assertNotNull(history);
        Assertions.assertEquals(2, history.size());

        final TestBaseVariationHistoryDto entry1 = history.get(0);
        Assertions.assertEquals("RUN_2", entry1.getRunId());
        Assertions.assertEquals("Batch B", entry1.getBatchName());

        final TestBaseVariationHistoryDto entry2 = history.get(1);
        Assertions.assertEquals("RUN_1", entry2.getRunId());
        Assertions.assertEquals("Batch A", entry2.getBatchName());
    }
}
