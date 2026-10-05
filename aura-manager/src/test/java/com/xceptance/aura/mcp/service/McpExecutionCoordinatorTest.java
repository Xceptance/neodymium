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
package com.xceptance.aura.mcp.service;

import com.xceptance.aura.mcp.service.McpExecutionCoordinator.DispatchResult;
import com.xceptance.neodymium.aura.AuraQueueService;
import com.xceptance.neodymium.aura.dto.RunRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Unit tests for {@link McpExecutionCoordinator} covering queue dispatching,
 * synchronous targetRunId injection, cancellation, and FIFO draining.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public class McpExecutionCoordinatorTest
{
    private AuraQueueService queueService;
    private AtomicReference<Consumer<String>> listenerRef;
    private McpExecutionCoordinator coordinator;

    @BeforeEach
    public void setUp()
    {
        queueService = Mockito.mock(AuraQueueService.class);
        listenerRef = new AtomicReference<>();
        Mockito.doAnswer(invocation -> {
            final Consumer<String> listener = invocation.getArgument(0);
            listenerRef.set(listener);
            return null;
        }).when(queueService).addRunCompletedListener(Mockito.any());

        coordinator = new McpExecutionCoordinator(queueService);
    }

    @Test
    public void testSubmitRunWhenQueueIdle()
    {
        Mockito.when(queueService.isRunningQueue()).thenReturn(false);

        final DispatchResult result = coordinator.submitRun("tests/sample.yaml", "Chrome", "data-1");

        Assertions.assertNotNull(result);
        Assertions.assertEquals("RUNNING", result.getStatus());
        Assertions.assertEquals(0, result.getQueuePosition());
        Assertions.assertTrue(result.getRunId().startsWith("run_"));

        final ArgumentCaptor<RunRequest> reqCaptor = ArgumentCaptor.forClass(RunRequest.class);
        Mockito.verify(queueService).executeQueue(reqCaptor.capture(), Mockito.eq(8080));

        final RunRequest capturedReq = reqCaptor.getValue();
        Assertions.assertEquals(result.getRunId(), capturedReq.targetRunId);
        Assertions.assertEquals("tests/sample.yaml", capturedReq.datasets.getFirst().file);
    }

    @Test
    public void testSubmitRunWhenQueueBusyEnqueues()
    {
        Mockito.when(queueService.isRunningQueue()).thenReturn(true);

        final DispatchResult res1 = coordinator.submitRun("tests/test1.yaml", "Chrome", null);
        Assertions.assertEquals("ENQUEUED", res1.getStatus());
        Assertions.assertEquals(1, res1.getQueuePosition());

        final DispatchResult res2 = coordinator.submitRun("tests/test2.yaml", "Firefox", null);
        Assertions.assertEquals("ENQUEUED", res2.getStatus());
        Assertions.assertEquals(2, res2.getQueuePosition());

        Mockito.verify(queueService, Mockito.never()).executeQueue(Mockito.any(), Mockito.anyInt());
    }

    @Test
    public void testActiveRunCompletionDrainsPendingQueue()
    {
        Mockito.when(queueService.isRunningQueue()).thenReturn(true);

        final DispatchResult enqueued = coordinator.submitRun("tests/queued.yaml", "Chrome", null);
        Assertions.assertEquals("ENQUEUED", enqueued.getStatus());

        // Simulate active run finishing
        Assertions.assertNotNull(listenerRef.get(), "Run completion listener must be registered");
        listenerRef.get().accept("some_other_run");

        // Now queueService should have been invoked for queued.yaml
        final ArgumentCaptor<RunRequest> reqCaptor = ArgumentCaptor.forClass(RunRequest.class);
        Mockito.verify(queueService).executeQueue(reqCaptor.capture(), Mockito.eq(8080));
        Assertions.assertEquals(enqueued.getRunId(), reqCaptor.getValue().targetRunId);
        Assertions.assertEquals("RUNNING", coordinator.getRunStatus(enqueued.getRunId()));
    }

    @Test
    public void testCancelEnqueuedRunPurgesFromQueue()
    {
        Mockito.when(queueService.isRunningQueue()).thenReturn(true);

        final DispatchResult res = coordinator.submitRun("tests/toBeCancelled.yaml", "Chrome", null);
        Assertions.assertEquals("ENQUEUED", res.getStatus());

        final boolean cancelled = coordinator.cancelRun(res.getRunId());
        Assertions.assertTrue(cancelled);
        Assertions.assertEquals("CANCELLED", coordinator.getRunStatus(res.getRunId()));

        // When previous run finishes, cancelled run should NOT be executed
        listenerRef.get().accept("active_run");
        Mockito.verify(queueService, Mockito.never()).executeQueue(Mockito.any(), Mockito.anyInt());
    }

    @Test
    public void testCancelActiveRunDelegatesToQueueService()
    {
        Mockito.when(queueService.isRunningQueue()).thenReturn(false);

        final DispatchResult res = coordinator.submitRun("tests/running.yaml", "Chrome", null);
        Assertions.assertEquals("RUNNING", res.getStatus());

        Mockito.when(queueService.isRunningQueue()).thenReturn(true);
        Mockito.when(queueService.getCurrentRunId()).thenReturn(res.getRunId());

        final boolean cancelled = coordinator.cancelRun(res.getRunId());
        Assertions.assertTrue(cancelled);
        Mockito.verify(queueService).stopCurrentTest();
        Assertions.assertEquals("CANCELLED", coordinator.getRunStatus(res.getRunId()));
    }

    @Test
    public void testSubmitSuiteGeneratesTargetRunId()
    {
        Mockito.when(queueService.isRunningQueue()).thenReturn(false);

        final DispatchResult result = coordinator.submitSuite(List.of("tests/s1.yaml", "tests/s2.yaml"), "Chrome");
        Assertions.assertEquals("RUNNING", result.getStatus());

        final ArgumentCaptor<RunRequest> reqCaptor = ArgumentCaptor.forClass(RunRequest.class);
        Mockito.verify(queueService).executeQueue(reqCaptor.capture(), Mockito.eq(8080));
        Assertions.assertEquals(result.getRunId(), reqCaptor.getValue().targetRunId);
        Assertions.assertEquals(2, reqCaptor.getValue().datasets.size());
    }
}
