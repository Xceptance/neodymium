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

import com.xceptance.aura.report.dto.RunReportDto;
import com.xceptance.aura.report.dto.TestBaseDataDto;
import com.xceptance.aura.report.repository.TestBaseBugRepository;
import com.xceptance.aura.report.repository.TestBaseVariationRepository;
import com.xceptance.aura.report.repository.TestBatchRepository;
import com.xceptance.aura.report.repository.TestRunRepository;
import com.xceptance.aura.report.service.AuraReportDataService;
import com.xceptance.aura.report.service.RunStorageSyncService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
}
