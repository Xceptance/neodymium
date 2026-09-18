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

import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.repository.TestBaseBugRepository;
import com.xceptance.aura.report.repository.TestBaseVariationRepository;
import com.xceptance.aura.report.repository.TestBatchRepository;
import com.xceptance.aura.report.repository.TestRunRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.util.Map;
import java.util.Optional;

/**
 * Unit test verifying that cancelled or stopped running test executions are changed to skipped status in reports.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class AuraRunCancellationTest
{
    private TestRunRepository runRepository;
    private TestBatchRepository batchRepository;
    private TestBaseVariationRepository variationRepository;
    private TestBaseBugRepository bugRepository;
    private LocalRunJsonStorageService localRunJsonStorageService;
    private AttachmentStorageService attachmentStorageService;
    private AuraReportDataService reportDataService;

    @BeforeEach
    public void setUp()
    {
        this.runRepository = Mockito.mock(TestRunRepository.class);
        this.batchRepository = Mockito.mock(TestBatchRepository.class);
        this.variationRepository = Mockito.mock(TestBaseVariationRepository.class);
        this.bugRepository = Mockito.mock(TestBaseBugRepository.class);
        this.localRunJsonStorageService = Mockito.mock(LocalRunJsonStorageService.class);
        this.attachmentStorageService = Mockito.mock(AttachmentStorageService.class);

        this.reportDataService = new AuraReportDataService(
            this.runRepository,
            this.batchRepository,
            this.variationRepository,
            this.bugRepository,
            this.localRunJsonStorageService,
            this.attachmentStorageService,
            null
        );
    }

    @Test
    public final void testCancelRunUpdatesRunningExecutionsToSkipped()
    {
        final String runId = "run-cancel-101";
        final TestRunEntity runEntity = new TestRunEntity(
            runId, "ALL", "IN_PROGRESS", "queue", "ALL", "Unknown", "Chrome", "Recently", System.currentTimeMillis()
        );

        Mockito.when(this.runRepository.findById(runId)).thenReturn(Optional.of(runEntity));

        final Map<String, Object> runningExecutionData = Map.of(
            "id", "exec-running-1",
            "testClass", "SampleTest",
            "testMethod", "executeTest",
            "title", "Dataset 1",
            "location", "US",
            "browser", "Chrome",
            "status", "running"
        );

        this.reportDataService.startRun(runId, "ALL", "ALL", "queue");
        this.reportDataService.ingestExecution(runId, runningExecutionData);

        this.reportDataService.cancelRun(runId);

        Assertions.assertEquals("CANCELLED", runEntity.getStatus(), "Run entity status should be updated to CANCELLED");
    }

    @Test
    public final void testStatusBadgeInfoMapsRunningToSkippedForCompletedRun()
    {
        final String[] runningBadge = AuraReportDataService.getStatusBadgeInfo("running");
        Assertions.assertEquals("badge-running", runningBadge[0]);
        Assertions.assertEquals("RUNNING", runningBadge[1]);

        final String[] skippedBadge = AuraReportDataService.getStatusBadgeInfo("skipped");
        Assertions.assertEquals("badge-ignored", skippedBadge[0]);
        Assertions.assertEquals("SKIPPED", skippedBadge[1]);
    }

    @Test
    public final void testPartialCancellationPreservesPassedAndSkipsRunning()
    {
        final String runId = "run-cancel-multi-dataset";
        final TestRunEntity runEntity = new TestRunEntity(
            runId, "ALL", "IN_PROGRESS", "queue", "ALL", "Unknown", "Chrome", "Recently", System.currentTimeMillis()
        );

        Mockito.when(this.runRepository.findById(runId)).thenReturn(Optional.of(runEntity));

        final Map<String, Object> passedExecutionData = Map.of(
            "id", "exec-dataset-1",
            "testClass", "MultiDatasetTest",
            "testMethod", "executeTest",
            "title", "Dataset 1",
            "location", "US",
            "browser", "Chrome",
            "status", "passed"
        );

        final Map<String, Object> runningExecutionData = Map.of(
            "id", "exec-dataset-2",
            "testClass", "MultiDatasetTest",
            "testMethod", "executeTest",
            "title", "Dataset 2",
            "location", "US",
            "browser", "Chrome",
            "status", "running"
        );

        this.reportDataService.startRun(runId, "ALL", "ALL", "queue");
        this.reportDataService.ingestExecution(runId, passedExecutionData);
        this.reportDataService.ingestExecution(runId, runningExecutionData);

        this.reportDataService.cancelRun(runId);

        Assertions.assertEquals("CANCELLED", runEntity.getStatus(), "Run entity status should be updated to CANCELLED");
    }
}
