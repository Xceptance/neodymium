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
package com.xceptance.aura.mcp.tool;

import com.xceptance.aura.core.service.AuraDiagnosticService;
import com.xceptance.aura.core.service.AuraExecutionService;
import com.xceptance.aura.core.service.AuraPlaybookService;
import com.xceptance.aura.core.service.impl.AuraDiagnosticServiceImpl;
import com.xceptance.aura.core.service.impl.AuraExecutionServiceImpl;
import com.xceptance.aura.core.service.impl.AuraPlaybookServiceImpl;
import com.xceptance.aura.mcp.dto.RunStatusResponseDto;
import com.xceptance.aura.mcp.security.HostAllowlistValidator;
import com.xceptance.aura.mcp.security.TwoStepConfirmationManager;
import com.xceptance.aura.mcp.service.AdaptivePayloadReporter;
import com.xceptance.aura.mcp.service.McpExecutionCoordinator;
import com.xceptance.aura.mcp.service.SeleniumErrorDistiller;
import com.xceptance.aura.report.service.AuraReportDataService;
import com.xceptance.neodymium.aura.AuraFileService;
import com.xceptance.neodymium.aura.AuraQueueService;
import com.xceptance.neodymium.aura.dto.YamlFileDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for AuraMcpToolService operations and security handshakes using the shared service layer.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class AuraMcpToolServiceTest
{
    private AuraFileService fileService;
    private AuraQueueService queueService;
    private AuraReportDataService reportDataService;
    private McpExecutionCoordinator executionCoordinator;
    private TwoStepConfirmationManager confirmationManager;
    private AuraMcpToolService toolService;

    @BeforeEach
    public void setUp()
    {
        fileService = Mockito.mock(AuraFileService.class);
        queueService = Mockito.mock(AuraQueueService.class);
        reportDataService = Mockito.mock(AuraReportDataService.class);
        executionCoordinator = Mockito.mock(McpExecutionCoordinator.class);
        final SeleniumErrorDistiller errorDistiller = new SeleniumErrorDistiller();
        final AdaptivePayloadReporter payloadReporter = new AdaptivePayloadReporter();
        final HostAllowlistValidator allowlistValidator = new HostAllowlistValidator();
        confirmationManager = new TwoStepConfirmationManager();

        final AuraPlaybookService playbookService = new AuraPlaybookServiceImpl(fileService, allowlistValidator);
        final AuraExecutionService executionService = new AuraExecutionServiceImpl(executionCoordinator, queueService, allowlistValidator, playbookService);
        final AuraDiagnosticService diagnosticService = new AuraDiagnosticServiceImpl(
            reportDataService,
            queueService,
            executionService,
            errorDistiller,
            payloadReporter
        );

        toolService = new AuraMcpToolService(
            playbookService,
            executionService,
            diagnosticService,
            confirmationManager
        );
    }

    @Test
    public void testListTests()
    {
        final YamlFileDto f1 = new YamlFileDto("tests/Login.yaml", List.of());
        final YamlFileDto f2 = new YamlFileDto("tests/Checkout.yaml", List.of());
        Mockito.when(fileService.getYamlFilesList()).thenReturn(List.of(f1, f2));

        final Map<String, Object> result = toolService.listTests(null);
        Assertions.assertEquals(2, result.get("total"));

        final Map<String, Object> filtered = toolService.listTests("login");
        Assertions.assertEquals(1, filtered.get("total"));
    }

    @Test
    public void testRunTestPathTraversalBlocked()
    {
        final Map<String, Object> res = toolService.runTest("../../evil.yaml", null, null);
        Assertions.assertEquals("ERROR", res.get("status"));
        Assertions.assertTrue(res.get("message").toString().contains("Invalid or unsafe test path"));
    }

    @Test
    public void testRunTestDelegatesToCoordinator()
    {
        final McpExecutionCoordinator.DispatchResult dispatch = new McpExecutionCoordinator.DispatchResult(
            "run_20260930_100000", "RUNNING", 0, "Started"
        );
        Mockito.when(executionCoordinator.submitRun("tests/Login.yaml", "Chrome", null)).thenReturn(dispatch);

        final Map<String, Object> res = toolService.runTest("tests/Login.yaml", "Chrome", null);
        Assertions.assertEquals("run_20260930_100000", res.get("runId"));
        Assertions.assertEquals("RUNNING", res.get("status"));
    }

    @Test
    public void testGetStatusEnqueued()
    {
        Mockito.when(executionCoordinator.getRunStatus("run_123")).thenReturn("ENQUEUED");

        final RunStatusResponseDto dto = toolService.getStatus("run_123");
        Assertions.assertEquals("run_123", dto.getRunId());
        Assertions.assertEquals("ENQUEUED", dto.getStatus());
        Assertions.assertTrue(dto.getSummary().contains("waiting in FIFO queue"));
    }

    @Test
    public void testDeleteTestTwoStepHandshake() throws IOException
    {
        // Step 1: Initial call requests confirmation
        final Map<String, Object> step1 = toolService.deleteTest("tests/Obsolete.yaml", null);
        Assertions.assertEquals("CONFIRMATION_REQUIRED", step1.get("status"));
        final String token = (String) step1.get("confirmationToken");
        Assertions.assertNotNull(token);

        // Step 2: Call with valid token succeeds
        Mockito.when(fileService.deleteYamlFile("tests/Obsolete.yaml")).thenReturn(true);
        final Map<String, Object> step2 = toolService.deleteTest("tests/Obsolete.yaml", token);
        Assertions.assertEquals("SUCCESS", step2.get("status"));
    }

    @Test
    public void testCancelRunTwoStepHandshake()
    {
        // Step 1: Initial call requests confirmation
        final Map<String, Object> step1 = toolService.cancelRun("run_abc", null);
        Assertions.assertEquals("CONFIRMATION_REQUIRED", step1.get("status"));
        final String token = (String) step1.get("confirmationToken");
        Assertions.assertNotNull(token);

        // Step 2: Call with valid token cancels run
        Mockito.when(queueService.isRunningQueue()).thenReturn(true);
        Mockito.when(queueService.getCurrentRunId()).thenReturn("run_abc");
        final Map<String, Object> step2 = toolService.cancelRun("run_abc", token);
        Assertions.assertEquals("CANCELLED", step2.get("status"));
        Mockito.verify(queueService).stopProcess();
    }

    @Test
    public void testCancelRunWhenActiveRunDiffersReturnsNotFound()
    {
        final Map<String, Object> step1 = toolService.cancelRun("run_different", null);
        Assertions.assertEquals("CONFIRMATION_REQUIRED", step1.get("status"));
        final String token = (String) step1.get("confirmationToken");

        Mockito.when(queueService.isRunningQueue()).thenReturn(true);
        Mockito.when(queueService.getCurrentRunId()).thenReturn("run_currently_active");
        final Map<String, Object> step2 = toolService.cancelRun("run_different", token);
        Assertions.assertEquals("NOT_FOUND", step2.get("status"));
        Mockito.verify(queueService, Mockito.never()).stopProcess();
    }

    @Test
    public void testRunSuite()
    {
        final McpExecutionCoordinator.DispatchResult dispatch = new McpExecutionCoordinator.DispatchResult(
            "run_suite_999", "RUNNING", 0, "Suite started"
        );
        Mockito.when(executionCoordinator.submitSuite(List.of("tests/Login.yaml", "tests/Cart.yaml"), "Chrome")).thenReturn(dispatch);

        final Map<String, Object> res = toolService.runSuite(List.of("tests/Login.yaml", "tests/Cart.yaml"), "Chrome");
        Assertions.assertEquals("run_suite_999", res.get("runId"));
        Assertions.assertEquals("RUNNING", res.get("status"));
    }

    @Test
    public void testGetRunFailuresNotFound()
    {
        final Map<String, Object> res = toolService.getRunFailures("run_missing", null, "0", 5);
        Assertions.assertEquals("Run report not found for runId: run_missing", res.get("error"));
    }
}
