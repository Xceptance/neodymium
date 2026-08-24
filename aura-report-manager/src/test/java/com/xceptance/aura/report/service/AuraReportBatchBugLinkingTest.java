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

import com.xceptance.aura.report.dto.RunReportDto;
import com.xceptance.aura.report.dto.TestExecutionDto;
import com.xceptance.aura.report.entity.TestBatchEntity;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.repository.TestBaseBugRepository;
import com.xceptance.aura.report.repository.TestBatchRepository;
import com.xceptance.aura.report.repository.TestRunRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit and integration tests for batch-scoped bug linking, timeline propagation, and execution status updates.
 *
 * @author AI-generated: Gemini 3.6 Flash (High)
 * @author Xceptance GmbH 2026
 */
@SpringBootTest
public class AuraReportBatchBugLinkingTest
{
    @Autowired
    private AuraReportDataService dataService;

    @Autowired
    private TestBatchRepository batchRepository;

    @Autowired
    private TestRunRepository runRepository;

    @Autowired
    private TestBaseBugRepository bugRepository;

    @Autowired
    private LocalRunJsonStorageService storageService;

    private static final String BATCH_A = "Batch Alpha";
    private static final String BATCH_B = "Batch Beta";

    private static final String RUN_A1 = "run-a1";
    private static final String RUN_A2 = "run-a2";
    private static final String RUN_A3 = "run-a3";
    private static final String RUN_B1 = "run-b1";

    @BeforeEach
    public void setUp() throws IOException
    {
        bugRepository.deleteAll();
        batchRepository.deleteAll();
        runRepository.deleteAll();

        batchRepository.save(new TestBatchEntity(BATCH_A, "Staging", "Batch A", "US", "Chrome", RUN_A3));
        batchRepository.save(new TestBatchEntity(BATCH_B, "Staging", "Batch B", "US", "Chrome", RUN_B1));

        final long now = System.currentTimeMillis();

        // Run A1 (earliest)
        createRunAndReport(RUN_A1, BATCH_A, now - 300000L, "row-a1-1", "passed");

        // Run A2 (middle)
        createRunAndReport(RUN_A2, BATCH_A, now - 200000L, "row-a2-1", "passed");

        // Run A3 (latest in Batch A)
        createRunAndReport(RUN_A3, BATCH_A, now - 100000L, "row-a3-1", "passed");

        // Run B1 (in Batch B, same test variation)
        createRunAndReport(RUN_B1, BATCH_B, now - 150000L, "row-b1-1", "passed");
    }

    private void createRunAndReport(final String runId, final String batchName, final long startTime, final String rowId, final String status) throws IOException
    {
        final TestRunEntity runEntity = new TestRunEntity(
            runId, batchName, "COMPLETED", "Trigger", "Staging", "US", "Chrome", "2026-08-24 10:00:00", startTime
        );
        runEntity.setTotalTests(1);
        if ("passed".equals(status))
        {
            runEntity.setPassedCount(1);
        }
        else
        {
            runEntity.setFailedUnknownCount(1);
        }
        runRepository.save(runEntity);

        final TestExecutionDto exec = new TestExecutionDto(
            rowId, runId, "com.xceptance.neodymium.test.CheckoutTest", "testCheckoutFlow",
            "CheckoutTest testCheckoutFlow", "1.0s", "2026-08-24 10:00:00", status,
            "Java", "US", "Chrome", null, List.of(), null, "Checkout", List.of(),
            null, null, null, null
        );

        final RunReportDto report = new RunReportDto(
            runId, batchName, "2026-08-24 10:00:00", "1.0s", 1,
            "passed".equals(status) ? 1 : 0, 0, 0,
            "passed".equals(status) ? 0 : 1, 0, List.of(exec)
        );

        dataService.saveRunReportToDisk(runId, report);
    }

    @AfterEach
    public void tearDown() throws IOException
    {
        for (final String runId : List.of(RUN_A1, RUN_A2, RUN_A3, RUN_B1, "run-a4-ingest", "run_20260821_110929", "run_20260821_114903"))
        {
            final Path runDir = storageService.getRunDir(runId);
            if (Files.exists(runDir))
            {
                try (final var stream = Files.walk(runDir))
                {
                    stream.sorted(Comparator.reverseOrder())
                          .map(Path::toFile)
                          .forEach(File::delete);
                }
            }
            final Path flatFile = storageService.getRunJsonPath(runId);
            if (Files.exists(flatFile) && !Files.isDirectory(flatFile))
            {
                Files.deleteIfExists(flatFile);
            }
        }
    }

    @Test
    public void testBugLinkingAppliesToCurrentReportAndSubsequentReportsInBatchOnly()
    {
        // Link bug BUG-100 on Run A2 (middle run in Batch A)
        dataService.addBugToExecution(RUN_A2, "row-a2-1", "BUG-100");

        // 1. Run A1 (earlier in Batch A) should NOT have BUG-100
        final RunReportDto reportA1 = dataService.getRunReport(RUN_A1);
        Assertions.assertTrue(reportA1.getExecutions().get(0).getBugs().isEmpty(), "Run A1 before bug link should have no bugs");

        // 2. Run A2 (linked report itself) SHOULD have BUG-100 and status succeeded-fixed
        final RunReportDto reportA2 = dataService.getRunReport(RUN_A2);
        Assertions.assertEquals(List.of("BUG-100"), reportA2.getExecutions().get(0).getBugs());
        Assertions.assertEquals("succeeded-fixed", reportA2.getExecutions().get(0).getStatus());
        Assertions.assertEquals(1, reportA2.getFixedCount());

        // 3. Run A3 (subsequent in Batch A) SHOULD also have BUG-100 and status succeeded-fixed
        final RunReportDto reportA3 = dataService.getRunReport(RUN_A3);
        Assertions.assertEquals(List.of("BUG-100"), reportA3.getExecutions().get(0).getBugs());
        Assertions.assertEquals("succeeded-fixed", reportA3.getExecutions().get(0).getStatus());
        Assertions.assertEquals(1, reportA3.getFixedCount());

        // 4. Run B1 (in Batch B, same variation) should NOT have BUG-100 (batch isolation)
        final RunReportDto reportB1 = dataService.getRunReport(RUN_B1);
        Assertions.assertTrue(reportB1.getExecutions().get(0).getBugs().isEmpty(), "Run B1 in Batch B should not receive Batch A bug");
    }

    @Test
    public void testIngestionAppliesKnownBugsForBatch()
    {
        // Link bug BUG-200 on Run A2
        dataService.addBugToExecution(RUN_A2, "row-a2-1", "BUG-200");

        // Create new Run A4 in Batch A via ingestion
        final String runA4 = "run-a4-ingest";
        final TestRunEntity runEntityA4 = new TestRunEntity(
            runA4, BATCH_A, "IN_PROGRESS", "Trigger", "Staging", "US", "Chrome", "2026-08-24 10:05:00", System.currentTimeMillis()
        );
        runRepository.save(runEntityA4);

        final Map<String, Object> payload = new HashMap<>();
        payload.put("id", "row-a4-1");
        payload.put("runId", runA4);
        payload.put("testClass", "com.xceptance.neodymium.test.CheckoutTest");
        payload.put("title", "testCheckoutFlow");
        payload.put("status", "passed");
        payload.put("location", "US");
        payload.put("browser", "Chrome");

        dataService.ingestExecution(runA4, payload);

        // Verify ingested execution in Run A4 has BUG-200 attached and status succeeded-fixed
        final RunReportDto reportA4 = dataService.getRunReport(runA4);
        Assertions.assertEquals(List.of("BUG-200"), reportA4.getExecutions().get(0).getBugs());
        Assertions.assertEquals("succeeded-fixed", reportA4.getExecutions().get(0).getStatus());
        Assertions.assertEquals(1, reportA4.getFixedCount());
    }

    @Test
    public void testBugUnlinkingScope()
    {
        // Link bug on Run A1 (applies to A1, A2, A3)
        dataService.addBugToExecution(RUN_A1, "row-a1-1", "BUG-300");

        Assertions.assertEquals(List.of("BUG-300"), dataService.getRunReport(RUN_A1).getExecutions().get(0).getBugs());
        Assertions.assertEquals(List.of("BUG-300"), dataService.getRunReport(RUN_A2).getExecutions().get(0).getBugs());
        Assertions.assertEquals(List.of("BUG-300"), dataService.getRunReport(RUN_A3).getExecutions().get(0).getBugs());

        // Unlink bug on Run A3
        dataService.removeBugFromExecution(RUN_A3, "row-a3-1", "BUG-300");

        // Run A1 and A2 should still have BUG-300
        Assertions.assertEquals(List.of("BUG-300"), dataService.getRunReport(RUN_A1).getExecutions().get(0).getBugs());
        Assertions.assertEquals(List.of("BUG-300"), dataService.getRunReport(RUN_A2).getExecutions().get(0).getBugs());

        // Run A3 should have BUG-300 removed
        Assertions.assertTrue(dataService.getRunReport(RUN_A3).getExecutions().get(0).getBugs().isEmpty());
        Assertions.assertEquals("passed-clean", dataService.getRunReport(RUN_A3).getExecutions().get(0).getStatus());
    }

    @Test
    public void testVariationHistoryReflectsLinkedBugsForUnknownBatch() throws IOException
    {
        final String run1 = "run_20260821_110929";
        final String run2 = "run_20260821_114903";
        final String batchName = "Unknown";
        final long now = System.currentTimeMillis();

        createRunAndReport(run1, batchName, now - 500000L, "row-u1-1", "passed");
        createRunAndReport(run2, batchName, now - 400000L, "row-u2-1", "passed");

        final String varId = dataService.generateVariationId("com.xceptance.neodymium.test.CheckoutTest", "testCheckoutFlow", "US", "Chrome");

        // Link bug on run 1
        dataService.addBugToExecution(run1, "row-u1-1", "BUG-CANYON-1");

        // Verify variation history dynamically reflects BUG-CANYON-1 and status succeeded-fixed for run 2
        final var history = dataService.getVariationHistory("com.xceptance.neodymium.test.CheckoutTest", "testCheckoutFlow", "US", "Chrome");
        Assertions.assertFalse(history.isEmpty(), "History should not be empty");

        final var run2HistoryOpt = history.stream().filter(h -> run2.equals(h.getRunId())).findFirst();
        Assertions.assertTrue(run2HistoryOpt.isPresent(), "Run 2 should be in variation history");
        Assertions.assertEquals(List.of("BUG-CANYON-1"), run2HistoryOpt.get().getBugs(), "Run 2 history should dynamically reflect BUG-CANYON-1");
        Assertions.assertEquals("succeeded-fixed", run2HistoryOpt.get().getStatus(), "Run 2 status should be succeeded-fixed in history");
    }
}
