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
package com.xceptance.aura.config;

import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.repository.TestRunRepository;
import com.xceptance.neodymium.aura.AuraInteractiveService;
import com.xceptance.neodymium.aura.AuraQueueService;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

/**
 * Unit test verifying AuraGlobalModelAdvice controller advice model attribute population.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class AuraGlobalModelAdviceTest
{
    private AuraQueueService queueService;
    private AuraGlobalModelAdvice advice;

    @BeforeAll
    public static void beforeAll()
    {
        System.setProperty("neodymium.aura.test", "true");
    }

    @BeforeEach
    public void setUp()
    {
        final AuraInteractiveService interactiveService = new AuraInteractiveService();
        this.queueService = new AuraQueueService(null, interactiveService);
        this.advice = new AuraGlobalModelAdvice(this.queueService);
    }

    @Test
    public final void testAddGlobalAttributesWhenNoRunActive()
    {
        final Model model = new ConcurrentModel();
        advice.addGlobalAttributes(model);

        Assertions.assertEquals(Boolean.FALSE, model.getAttribute("hasRunContext"));
        Assertions.assertEquals("", model.getAttribute("activeRunId"));
        Assertions.assertEquals("", model.getAttribute("activeBatchName"));
        Assertions.assertEquals("", model.getAttribute("activeRunStatus"));
        Assertions.assertEquals("", model.getAttribute("activeRunReportUrl"));
        Assertions.assertEquals(Boolean.FALSE, model.getAttribute("running"));
    }

    @Test
    public final void testAddGlobalAttributesWhenRunActiveWithBatchName()
    {
        final AuraQueueService mockQueueService = Mockito.mock(AuraQueueService.class);
        Mockito.when(mockQueueService.getCurrentRunId()).thenReturn("run_20261021_121942");
        Mockito.when(mockQueueService.getCurrentBatchName()).thenReturn("Wikipedia");
        Mockito.when(mockQueueService.isRunningQueue()).thenReturn(true);

        final AuraGlobalModelAdvice activeAdvice = new AuraGlobalModelAdvice(mockQueueService);
        final Model model = new ConcurrentModel();
        activeAdvice.addGlobalAttributes(model);

        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("hasRunContext"));
        Assertions.assertEquals("run_20261021_121942", model.getAttribute("activeRunId"));
        Assertions.assertEquals("Wikipedia", model.getAttribute("activeBatchName"));
        Assertions.assertEquals("running", model.getAttribute("activeRunStatus"));
        Assertions.assertEquals("/run-report?runId=run_20261021_121942", model.getAttribute("activeRunReportUrl"));
        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("running"));
    }

    @Test
    public final void testExternalRunContextFallbackAndProgress()
    {
        final AuraQueueService mockQueueService = Mockito.mock(AuraQueueService.class);
        Mockito.when(mockQueueService.getCurrentRunId()).thenReturn("");
        Mockito.when(mockQueueService.isRunningQueue()).thenReturn(false);

        final TestRunRepository mockRunRepo = Mockito.mock(TestRunRepository.class);
        final TestRunEntity runningRun = new TestRunEntity();
        runningRun.setId("run_20261110_120000");
        runningRun.setBatchName("CliSuite");
        runningRun.setStatus("IN_PROGRESS");
        runningRun.setTotalTests(10);
        runningRun.setRunningCount(2);

        Mockito.when(mockRunRepo.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of(runningRun));

        final AuraGlobalModelAdvice externalAdvice = new AuraGlobalModelAdvice(mockQueueService, null, mockRunRepo, null);
        final Model model = new ConcurrentModel();
        externalAdvice.addGlobalAttributes(model);

        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("hasRunContext"));
        Assertions.assertEquals("run_20261110_120000", model.getAttribute("activeRunId"));
        Assertions.assertEquals("CliSuite", model.getAttribute("activeBatchName"));
        Assertions.assertEquals("running", model.getAttribute("activeRunStatus"));
        Assertions.assertEquals("/run-report?runId=run_20261110_120000", model.getAttribute("activeRunReportUrl"));
        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("running"));
        Assertions.assertEquals(8, model.getAttribute("activeRunExecutedCount"));
        Assertions.assertEquals(10, model.getAttribute("activeRunScheduledCount"));
        Assertions.assertEquals("8 of 10 tests (2 running)", model.getAttribute("activeRunProgress"));
    }

    @Test
    public final void testExternalRunContextFallbackCompletedRun()
    {
        final AuraQueueService mockQueueService = Mockito.mock(AuraQueueService.class);
        Mockito.when(mockQueueService.getCurrentRunId()).thenReturn("");
        Mockito.when(mockQueueService.isRunningQueue()).thenReturn(false);

        final TestRunRepository mockRunRepo = Mockito.mock(TestRunRepository.class);
        final TestRunEntity completedRun = new TestRunEntity();
        completedRun.setId("run_20261110_110000");
        completedRun.setBatchName("SmokeBatch");
        completedRun.setStatus("COMPLETED");
        completedRun.setTotalTests(5);
        completedRun.setRunningCount(0);

        Mockito.when(mockRunRepo.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of(completedRun));

        final AuraGlobalModelAdvice externalAdvice = new AuraGlobalModelAdvice(mockQueueService, null, mockRunRepo, null);
        final Model model = new ConcurrentModel();
        externalAdvice.addGlobalAttributes(model);

        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("hasRunContext"));
        Assertions.assertEquals("run_20261110_110000", model.getAttribute("activeRunId"));
        Assertions.assertEquals("SmokeBatch", model.getAttribute("activeBatchName"));
        Assertions.assertEquals("finished", model.getAttribute("activeRunStatus"));
        Assertions.assertEquals(Boolean.FALSE, model.getAttribute("running"));
        Assertions.assertEquals(5, model.getAttribute("activeRunExecutedCount"));
        Assertions.assertEquals(5, model.getAttribute("activeRunScheduledCount"));
        Assertions.assertEquals("5 tests completed", model.getAttribute("activeRunProgress"));
    }

    @Test
    public final void testExternalRunContextWithStaleQueueStateAndDefaultBatchSuppression()
    {
        final AuraQueueService mockQueueService = Mockito.mock(AuraQueueService.class);
        // Leftover stale queue run ID in memory while queue is idle
        Mockito.when(mockQueueService.getCurrentRunId()).thenReturn("stale_queue_run_999");
        Mockito.when(mockQueueService.getCurrentBatchName()).thenReturn("Default");
        Mockito.when(mockQueueService.isRunningQueue()).thenReturn(false);

        final TestRunRepository mockRunRepo = Mockito.mock(TestRunRepository.class);
        final TestRunEntity runningRun = new TestRunEntity();
        runningRun.setId("#run_from_console_125");
        runningRun.setBatchName("Default");
        runningRun.setStatus("IN_PROGRESS");
        runningRun.setTotalTests(3);
        runningRun.setRunningCount(1);

        Mockito.when(mockRunRepo.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of(runningRun));

        final AuraGlobalModelAdvice externalAdvice = new AuraGlobalModelAdvice(mockQueueService, null, mockRunRepo, null);
        final Model model = new ConcurrentModel();
        externalAdvice.addGlobalAttributes(model);

        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("hasRunContext"));
        Assertions.assertEquals("#run_from_console_125", model.getAttribute("activeRunId"));
        Assertions.assertEquals("", model.getAttribute("activeBatchName"));
        Assertions.assertEquals("running", model.getAttribute("activeRunStatus"));
        Assertions.assertEquals("/run-report?runId=#run_from_console_125", model.getAttribute("activeRunReportUrl"));
        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("running"));
        Assertions.assertEquals(2, model.getAttribute("activeRunExecutedCount"));
        Assertions.assertEquals(3, model.getAttribute("activeRunScheduledCount"));
        Assertions.assertEquals("2 of 3 tests (1 running)", model.getAttribute("activeRunProgress"));
    }

    @Test
    public final void testAddGlobalAttributesHxRequestFlag()
    {
        final Model modelHx = new ConcurrentModel();
        advice.addGlobalAttributes("true", modelHx);
        Assertions.assertEquals(Boolean.TRUE, modelHx.getAttribute("isHxRequest"));

        final Model modelNonHx = new ConcurrentModel();
        advice.addGlobalAttributes(null, modelNonHx);
        Assertions.assertEquals(Boolean.FALSE, modelNonHx.getAttribute("isHxRequest"));

        final Model modelBlankHx = new ConcurrentModel();
        advice.addGlobalAttributes("  ", modelBlankHx);
        Assertions.assertEquals(Boolean.FALSE, modelBlankHx.getAttribute("isHxRequest"));
    }
}
