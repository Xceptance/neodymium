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
package com.xceptance.aura.core.service;

import com.xceptance.aura.core.dto.FailurePageResultDto;
import com.xceptance.aura.core.service.impl.AuraDiagnosticServiceImpl;
import com.xceptance.aura.mcp.dto.DistilledFailureDto;
import com.xceptance.aura.mcp.dto.RunStatusResponseDto;
import com.xceptance.aura.mcp.service.AdaptivePayloadReporter;
import com.xceptance.aura.mcp.service.SeleniumErrorDistiller;
import com.xceptance.aura.report.dto.RunReportDto;
import com.xceptance.aura.report.dto.TestExecutionDto;
import com.xceptance.aura.report.service.AuraReportDataService;
import com.xceptance.neodymium.aura.AuraQueueService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for AuraDiagnosticServiceImpl.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class AuraDiagnosticServiceTest
{
    private AuraReportDataService reportDataService;
    private AuraQueueService queueService;
    private AuraExecutionService executionService;
    private AuraDiagnosticService diagnosticService;

    @BeforeEach
    public void setUp()
    {
        reportDataService = Mockito.mock(AuraReportDataService.class);
        queueService = Mockito.mock(AuraQueueService.class);
        executionService = Mockito.mock(AuraExecutionService.class);
        final SeleniumErrorDistiller distiller = new SeleniumErrorDistiller();
        final AdaptivePayloadReporter reporter = new AdaptivePayloadReporter();

        diagnosticService = new AuraDiagnosticServiceImpl(
            reportDataService,
            queueService,
            executionService,
            distiller,
            reporter
        );
    }

    @Test
    public void testGetRunStatusEnqueued()
    {
        Mockito.when(executionService.getCoordinatorRunStatus("run_enq")).thenReturn("ENQUEUED");

        final RunStatusResponseDto res = diagnosticService.getRunStatus("run_enq");
        Assertions.assertEquals("run_enq", res.getRunId());
        Assertions.assertEquals("ENQUEUED", res.getStatus());
        Assertions.assertTrue(res.getSummary().contains("waiting in FIFO queue"));
    }

    @Test
    public void testGetRunStatusNotFound()
    {
        Mockito.when(executionService.getCoordinatorRunStatus("run_unknown")).thenReturn(null);
        Mockito.when(reportDataService.getRunReport("run_unknown")).thenReturn(null);
        Mockito.when(queueService.isRunningQueue()).thenReturn(false);

        final RunStatusResponseDto res = diagnosticService.getRunStatus("run_unknown");
        Assertions.assertEquals("NOT_FOUND", res.getStatus());
    }

    @Test
    public void testGetTestFailuresPagination()
    {
        final TestExecutionDto exec1 = new TestExecutionDto(
            "1", "run_123", "TestClass", "test1", "LoginTest", "LoginTest",
            "tests/Login.yaml", "tests/Login.yaml", "FAILED", "Java", "UNKNOWN", null,
            "Chrome", "org.openqa.selenium.NoSuchElementException: Cannot locate element with locator: By.id: login-btn",
            new ArrayList<>(), null, "Area", new ArrayList<>(), null, null,
            null, null, null, null, "MODE", null, null, null, null, 0L, 1, 1, 100L, "100ms", 0, 0L, 0.0,
            null, "Cannot locate element", null, null, null, null
        );

        final TestExecutionDto exec2 = new TestExecutionDto(
            "2", "run_123", "TestClass", "test2", "CartTest", "CartTest",
            "tests/Cart.yaml", "tests/Cart.yaml", "FAILED", "Java", "UNKNOWN", null,
            "Chrome", "org.openqa.selenium.TimeoutException: Expected condition failed: waiting for visibility of element located by By.cssSelector: .cart-item",
            new ArrayList<>(), null, "Area", new ArrayList<>(), null, null,
            null, null, null, null, "MODE", null, null, null, null, 0L, 1, 1, 200L, "200ms", 0, 0L, 0.0,
            null, "Timeout waiting for element", null, null, null, null
        );

        final RunReportDto report = new RunReportDto(
            "run_123", "Batch", "2026-10-02T10:00:00Z", "300ms",
            2, 0, 0, 0, 2, 0, 0,
            List.of(exec1, exec2), List.of(), 0, 0L, 0.0
        );

        Mockito.when(reportDataService.getRunReport("run_123")).thenReturn(report);

        // Test cursor pagination batch 1 (limit 1, cursor 0)
        final FailurePageResultDto<DistilledFailureDto> page1 = diagnosticService.getRunFailures("run_123", null, "0", 1);
        Assertions.assertEquals(2, page1.getTotalCount());
        Assertions.assertEquals(1, page1.getFailures().size());
        Assertions.assertEquals("LoginTest", page1.getFailures().get(0).getTestName());
        Assertions.assertTrue(page1.isHasMore());
        Assertions.assertEquals("1", page1.getNextCursor());

        // Test cursor pagination batch 2 using nextCursor
        final FailurePageResultDto<DistilledFailureDto> page2 = diagnosticService.getRunFailures("run_123", null, page1.getNextCursor(), 1);
        Assertions.assertEquals(1, page2.getFailures().size());
        Assertions.assertEquals("CartTest", page2.getFailures().get(0).getTestName());
        Assertions.assertFalse(page2.isHasMore());
        Assertions.assertNull(page2.getNextCursor());

        // Test single execution failure lookup
        final DistilledFailureDto single = diagnosticService.getExecutionFailure("run_123", "LoginTest");
        Assertions.assertNotNull(single);
        Assertions.assertEquals("LoginTest", single.getTestName());
        Assertions.assertTrue(single.getClusterCategory().startsWith("ELEMENT_NOT_FOUND"));

        // Test category prefix filtering
        final FailurePageResultDto<DistilledFailureDto> filteredPage =
                diagnosticService.getRunFailures("run_123", "ELEMENT_NOT_FOUND", "0", 5);
        Assertions.assertEquals(1, filteredPage.getTotalCount());
        Assertions.assertEquals("LoginTest", filteredPage.getFailures().get(0).getTestName());
    }

    @Test
    public void testGetRunStatusExcludesHealedFromFailures()
    {
        final RunReportDto report = new RunReportDto(
            "run_healed", "Batch", "2026-10-02T10:00:00Z", "300ms",
            2, 1, 1, 0, 0, 0, 0,
            List.of(), List.of(), 0, 0L, 0.0
        );
        Mockito.when(reportDataService.getRunReport("run_healed")).thenReturn(report);

        final RunStatusResponseDto res = diagnosticService.getRunStatus("run_healed");
        Assertions.assertEquals(0, res.getMetrics().getFailed());
        Assertions.assertEquals(2, res.getMetrics().getPassed(), "Passed count includes healed tests so total equals passed + failed + skipped");
        Assertions.assertEquals(2, res.getMetrics().getTotal());
        Assertions.assertEquals("PASSED", res.getStatus());
    }

    @Test
    public void testGetRunStatusWhenCancelledWithoutReport()
    {
        Mockito.when(reportDataService.getRunReport("run_cancelled")).thenReturn(null);
        Mockito.when(executionService.getCoordinatorRunStatus("run_cancelled")).thenReturn("CANCELLED");

        final RunStatusResponseDto res = diagnosticService.getRunStatus("run_cancelled");
        Assertions.assertEquals("CANCELLED", res.getStatus());
        Assertions.assertEquals(0, res.getMetrics().getTotal());
    }

    @Test
    public void testGetRunStatusWhenSkippedOnly()
    {
        final RunReportDto report = new RunReportDto(
            "run_skipped", "Batch", "2026-10-02T10:00:00Z", "100ms",
            3, 0, 0, 0, 0, 3, 0,
            List.of(), List.of(), 0, 0L, 0.0
        );
        Mockito.when(reportDataService.getRunReport("run_skipped")).thenReturn(report);

        final RunStatusResponseDto res = diagnosticService.getRunStatus("run_skipped");
        Assertions.assertEquals("SKIPPED", res.getStatus());
        Assertions.assertEquals(3, res.getMetrics().getSkipped());
        Assertions.assertEquals(0, res.getMetrics().getPassed());
        Assertions.assertEquals(0, res.getMetrics().getFailed());
    }

    @Test
    public void testGetRunFailuresFilteringByUnclassifiedWithLocator()
    {
        final TestExecutionDto exec = new TestExecutionDto(
            "1", "run_123", "TestClass", "test1", "UnclassifiedTest", "UnclassifiedTest",
            "tests/Unclassified.yaml", "tests/Unclassified.yaml", "FAILED", "Java", "UNKNOWN", null,
            "Chrome", "Some custom error occurred with locator: By.id: custom-btn",
            new ArrayList<>(), null, "Area", new ArrayList<>(), null, null,
            null, null, null, null, "MODE", null, null, null, null, 0L, 1, 1, 100L, "100ms", 0, 0L, 0.0,
            null, "Custom error", null, null, null, null
        );
        final RunReportDto report = new RunReportDto(
            "run_123", "Batch", "2026-10-02T10:00:00Z", "100ms",
            1, 0, 0, 1, 0, 0, 0,
            List.of(exec), List.of(), 1, 100L, 0.0
        );
        Mockito.when(reportDataService.getRunReport("run_123")).thenReturn(report);

        final FailurePageResultDto<DistilledFailureDto> failures = diagnosticService.getRunFailures("run_123", "UNCLASSIFIED", "0", 10);
        Assertions.assertEquals(1, failures.getTotalCount());
        Assertions.assertEquals(1, failures.getFailures().size());

        final FailurePageResultDto<DistilledFailureDto> failuresUnknown = diagnosticService.getRunFailures("run_123", "UNKNOWN", "0", 10);
        Assertions.assertEquals(1, failuresUnknown.getTotalCount());
        Assertions.assertEquals(1, failuresUnknown.getFailures().size());
    }
}
