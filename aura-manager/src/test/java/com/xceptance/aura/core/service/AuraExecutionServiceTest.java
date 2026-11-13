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

import com.xceptance.aura.core.dto.ExecutionLaunchResultDto;
import com.xceptance.aura.core.service.impl.AuraExecutionServiceImpl;
import com.xceptance.aura.mcp.security.HostAllowlistValidator;
import com.xceptance.aura.mcp.service.McpExecutionCoordinator;
import com.xceptance.neodymium.aura.AuraQueueService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

/**
 * Unit tests for AuraExecutionServiceImpl.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class AuraExecutionServiceTest
{
    private McpExecutionCoordinator coordinator;
    private AuraQueueService queueService;
    private HostAllowlistValidator allowlistValidator;
    private AuraPlaybookService playbookService;
    private AuraExecutionService executionService;

    @BeforeEach
    public void setUp()
    {
        coordinator = Mockito.mock(McpExecutionCoordinator.class);
        queueService = Mockito.mock(AuraQueueService.class);
        allowlistValidator = new HostAllowlistValidator();
        playbookService = Mockito.mock(AuraPlaybookService.class);
        Mockito.when(playbookService.resolvePlaybookPath(Mockito.anyString()))
            .thenAnswer(inv -> inv.getArgument(0));
        executionService = new AuraExecutionServiceImpl(coordinator, queueService, allowlistValidator, playbookService);
    }

    @Test
    public void testDispatchRunUnsafePathBlocked()
    {
        final ExecutionLaunchResultDto res = executionService.dispatchRun("../../../secret.yaml", null, null);
        Assertions.assertEquals("ERROR", res.getStatus());
        Assertions.assertTrue(res.getMessage().contains("Invalid or unsafe test path"));
    }

    @Test
    public void testDispatchRunSuccess()
    {
        Mockito.when(playbookService.resolvePlaybookPath("tests/Login.yaml")).thenReturn("tests/Login.yaml");
        Mockito.when(coordinator.submitRun("tests/Login.yaml", "Chrome", "ds1"))
            .thenReturn(new McpExecutionCoordinator.DispatchResult("run_001", "RUNNING", 0, "Dispatched"));

        final ExecutionLaunchResultDto res = executionService.dispatchRun("tests/Login.yaml", "Chrome", "ds1");
        Assertions.assertEquals("run_001", res.getRunId());
        Assertions.assertEquals("RUNNING", res.getStatus());
        Assertions.assertEquals(0, res.getQueuePosition());
    }

    @Test
    public void testDispatchRunWithFilenameResolution()
    {
        Mockito.when(playbookService.resolvePlaybookPath("Login.yaml")).thenReturn("tests/auth/Login.yaml");
        Mockito.when(coordinator.submitRun("tests/auth/Login.yaml", null, null))
            .thenReturn(new McpExecutionCoordinator.DispatchResult("run_002", "RUNNING", 0, "Dispatched"));

        final ExecutionLaunchResultDto res = executionService.dispatchRun("Login.yaml", null, null);
        Assertions.assertEquals("run_002", res.getRunId());
        Assertions.assertEquals("RUNNING", res.getStatus());
        Mockito.verify(coordinator).submitRun("tests/auth/Login.yaml", null, null);
    }

    @Test
    public void testDispatchSuite()
    {
        Mockito.when(coordinator.submitSuite(List.of("tests/Login.yaml", "tests/Cart.yaml"), "Chrome"))
            .thenReturn(new McpExecutionCoordinator.DispatchResult("run_suite_001", "RUNNING", 0, "Suite started"));

        final ExecutionLaunchResultDto res = executionService.dispatchSuite(List.of("tests/Login.yaml", "tests/Cart.yaml"), "Chrome");
        Assertions.assertEquals("run_suite_001", res.getRunId());
        Assertions.assertEquals("RUNNING", res.getStatus());
    }

    @Test
    public void testDispatchSuiteUnsafePathBlocked()
    {
        final ExecutionLaunchResultDto res = executionService.dispatchSuite(List.of("tests/Login.yaml", "../secret.yaml"), "Chrome");
        Assertions.assertEquals("ERROR", res.getStatus());
        Assertions.assertTrue(res.getMessage().contains("Invalid or unsafe test path"));
    }

    @Test
    public void testCancelRunWhenActive()
    {
        Mockito.when(queueService.isRunningQueue()).thenReturn(true);
        Mockito.when(queueService.getCurrentRunId()).thenReturn("run_001");
        final var res = executionService.cancelRun("run_001");
        Assertions.assertEquals("CANCELLED", res.getStatus().name());
        Mockito.verify(queueService).stopProcess();
    }

    @Test
    public void testCancelRunWhenActiveRunDiffersDoesNotKillProcess()
    {
        Mockito.when(queueService.isRunningQueue()).thenReturn(true);
        Mockito.when(queueService.getCurrentRunId()).thenReturn("run_001");
        final var res = executionService.cancelRun("run_999_different");
        Assertions.assertEquals("NOT_FOUND", res.getStatus().name());
        Mockito.verify(queueService, Mockito.never()).stopProcess();
    }

    @Test
    public void testDispatchRunPlaybookNotFound()
    {
        Mockito.when(playbookService.resolvePlaybookPath("tests/Missing.yaml")).thenReturn(null);
        final ExecutionLaunchResultDto res = executionService.dispatchRun("tests/Missing.yaml", null, null);
        Assertions.assertEquals("ERROR", res.getStatus());
        Assertions.assertTrue(res.getMessage().contains("Test playbook not found"));
    }

    @Test
    public void testDispatchSuitePlaybookNotFound()
    {
        Mockito.when(playbookService.resolvePlaybookPath("tests/Missing.yaml")).thenReturn(null);
        final ExecutionLaunchResultDto res = executionService.dispatchSuite(List.of("tests/Login.yaml", "tests/Missing.yaml"), null);
        Assertions.assertEquals("ERROR", res.getStatus());
        Assertions.assertTrue(res.getMessage().contains("Test playbook not found in suite"));
    }

    @Test
    public void testCancelRunWhenAlreadyCompleted()
    {
        Mockito.when(coordinator.getRunStatus("run_001")).thenReturn("COMPLETED");
        Mockito.when(queueService.isRunningQueue()).thenReturn(false);
        final var res = executionService.cancelRun("run_001");
        Assertions.assertEquals("ALREADY_COMPLETED", res.getStatus().name());
    }

    @Test
    public void testCancelRunWhenNotFoundAndQueueIdle()
    {
        Mockito.when(coordinator.getRunStatus("bogus_run")).thenReturn(null);
        Mockito.when(queueService.isRunningQueue()).thenReturn(false);
        final var res = executionService.cancelRun("bogus_run");
        Assertions.assertEquals("NOT_FOUND", res.getStatus().name());
        Assertions.assertTrue(res.getMessage().contains("was not found"));
    }
}
