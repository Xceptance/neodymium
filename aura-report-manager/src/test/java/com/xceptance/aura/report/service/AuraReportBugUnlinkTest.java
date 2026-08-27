/*
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.xceptance.aura.report.service;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xceptance.aura.report.dto.RunReportDto;
import com.xceptance.aura.report.dto.TestExecutionDto;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.repository.TestBaseBugRepository;
import com.xceptance.aura.report.repository.TestRunRepository;

/**
 * Tests linking and unlinking bug tickets from test executions and verifying status updates and persistence.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@SpringBootTest
public class AuraReportBugUnlinkTest
{
    @Autowired
    private AuraReportDataService dataService;

    @Autowired
    private TestRunRepository runRepository;

    @Autowired
    private TestBaseBugRepository bugRepository;

    @Autowired
    private LocalRunJsonStorageService storageService;

    private static final String TEST_RUN_ID = "run-unlink-test-101";

    @BeforeEach
    public void setUp() throws java.io.IOException
    {
        bugRepository.deleteAll();

        final TestRunEntity runEntity = new TestRunEntity(
            TEST_RUN_ID,
            "Unlink Test Batch",
            "COMPLETED",
            "Regression batch for bug unlinking",
            "US-West",
            "en_US",
            "Chrome",
            "2026-08-17 12:00:00",
            System.currentTimeMillis()
        );
        runEntity.setTotalTests(1);
        runEntity.setFailedUnknownCount(1);
        runEntity.setFailedKnownCount(0);
        runEntity.setPassedCount(0);
        runEntity.setSucceededFixedCount(0);
        runEntity.setIgnoredCount(0);
        runEntity.setPassRate(0.0);
        runRepository.save(runEntity);

        final java.nio.file.Path runDir = storageService.getRunDir(TEST_RUN_ID);
        final java.nio.file.Path execDir = runDir.resolve("Checkout").resolve("CheckoutTest");
        java.nio.file.Files.createDirectories(execDir);
        final java.nio.file.Path execFile = execDir.resolve("exec-1.json");

        final com.fasterxml.jackson.databind.node.ObjectNode execNode = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        execNode.put("id", "row-unlink-1");
        execNode.put("runId", TEST_RUN_ID);
        execNode.put("testClass", "CheckoutTest");
        execNode.put("title", "testCheckoutProcess");
        execNode.put("status", "failed");
        execNode.put("areaName", "Checkout");
        execNode.put("location", "US-West");
        execNode.put("browser", "Chrome");
        execNode.put("failureReason", "AssertionError: Element not found");
        new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(execFile.toFile(), execNode);

        final TestExecutionDto exec = new TestExecutionDto(
            "row-unlink-1",
            TEST_RUN_ID,
            "com.xceptance.neodymium.test.CheckoutTest",
            "testCheckoutProcess",
            "com.xceptance.neodymium.test.CheckoutTest testCheckoutProcess",
            "1.0s",
            "2026-08-17 12:00:00",
            "failed",
            "JUnit5",
            "US-West",
            "Chrome",
            "AssertionError: Element not found",
            List.of(),
            null,
            "Checkout",
            List.of(),
            null,
            null,
            null,
            null
        );

        final RunReportDto report = new RunReportDto(
            TEST_RUN_ID,
            "Unlink Test Batch",
            "2026-08-17 12:00:00",
            "1.0s",
            1,
            0,
            0,
            0,
            1,
            0,
            List.of(exec)
        );

        dataService.saveRunReportToDisk(TEST_RUN_ID, report);
    }

    @org.junit.jupiter.api.AfterEach
    public void tearDown() throws java.io.IOException
    {
        for (final String runId : List.of(TEST_RUN_ID, "run-raw-no-id-99", "run-initial-1", "run-upfollowing-2"))
        {
            final java.nio.file.Path runDir = storageService.getRunDir(runId);
            if (java.nio.file.Files.exists(runDir))
            {
                try (final var stream = java.nio.file.Files.walk(runDir))
                {
                    stream.sorted(java.util.Comparator.reverseOrder())
                          .map(java.nio.file.Path::toFile)
                          .forEach(java.io.File::delete);
                }
            }
            final java.nio.file.Path flatFile = storageService.getRunJsonPath(runId);
            if (java.nio.file.Files.exists(flatFile) && !java.nio.file.Files.isDirectory(flatFile))
            {
                java.nio.file.Files.deleteIfExists(flatFile);
            }
        }
    }

    @Test
    public void testUnlinkBugFromInitialReportAfterUnlinkingFromUpfollowingReport() throws java.io.IOException
    {
        final String run1 = "run-initial-1";
        final String run2 = "run-upfollowing-2";

        final TestRunEntity runEntity1 = new TestRunEntity(
            run1, "Unlink Order Batch", "COMPLETED", "Run 1", "US-West", "en_US", "Chrome", "2026-08-17 12:00:00", 100000L
        );
        runRepository.save(runEntity1);

        final TestRunEntity runEntity2 = new TestRunEntity(
            run2, "Unlink Order Batch", "COMPLETED", "Run 2", "US-West", "en_US", "Chrome", "2026-08-17 13:00:00", 200000L
        );
        runRepository.save(runEntity2);

        final TestExecutionDto exec1 = new TestExecutionDto(
            "row-order-1", run1, "com.xceptance.neodymium.test.CheckoutTest", "testCheckoutProcess",
            "CheckoutTest testCheckoutProcess", "1.0s", "2026-08-17 12:00:00", "failed", "JUnit5",
            "US-West", "Chrome", "AssertionError", List.of(), null, "Checkout", List.of(), null, null, null, null
        );
        final RunReportDto report1 = new RunReportDto(run1, "Unlink Order Batch", "2026-08-17 12:00:00", "1.0s", 1, 0, 0, 0, 1, 0, List.of(exec1));
        dataService.saveRunReportToDisk(run1, report1);

        final TestExecutionDto exec2 = new TestExecutionDto(
            "row-order-2", run2, "com.xceptance.neodymium.test.CheckoutTest", "testCheckoutProcess",
            "CheckoutTest testCheckoutProcess", "1.0s", "2026-08-17 13:00:00", "failed", "JUnit5",
            "US-West", "Chrome", "AssertionError", List.of(), null, "Checkout", List.of(), null, null, null, null
        );
        final RunReportDto report2 = new RunReportDto(run2, "Unlink Order Batch", "2026-08-17 13:00:00", "1.0s", 1, 0, 0, 0, 1, 0, List.of(exec2));
        dataService.saveRunReportToDisk(run2, report2);

        // 1. Link BUG-1234 in Run 1
        dataService.addBugToExecution(run1, "row-order-1", "BUG-1234");
        dataService.updateCachedReportBugsAndStats(run1);
        dataService.updateCachedReportBugsAndStats(run2);

        Assertions.assertTrue(dataService.getRunReport(run1).getExecutions().get(0).getBugs().contains("BUG-1234"));
        Assertions.assertTrue(dataService.getRunReport(run2).getExecutions().get(0).getBugs().contains("BUG-1234"));

        // 2. Unlink BUG-1234 in Run 2 (the upfollowing report)
        dataService.removeBugFromExecution(run2, "row-order-2", "BUG-1234");
        dataService.updateCachedReportBugsAndStats(run1);
        dataService.updateCachedReportBugsAndStats(run2);

        Assertions.assertTrue(dataService.getRunReport(run1).getExecutions().get(0).getBugs().contains("BUG-1234"));
        Assertions.assertFalse(dataService.getRunReport(run2).getExecutions().get(0).getBugs().contains("BUG-1234"));

        // 3. Unlink BUG-1234 in Run 1 (the initial report where it was first linked)
        dataService.removeBugFromExecution(run1, "row-order-1", "BUG-1234");
        dataService.updateCachedReportBugsAndStats(run1);
        dataService.updateCachedReportBugsAndStats(run2);

        Assertions.assertFalse(dataService.getRunReport(run1).getExecutions().get(0).getBugs().contains("BUG-1234"));
        Assertions.assertEquals("failed-unknown", dataService.getRunReport(run1).getExecutions().get(0).getStatus());
        Assertions.assertFalse(dataService.getRunReport(run2).getExecutions().get(0).getBugs().contains("BUG-1234"));
        Assertions.assertEquals("failed-unknown", dataService.getRunReport(run2).getExecutions().get(0).getStatus());
    }

    @Test
    public void testLinkAndUnlinkBugTicket() throws java.io.IOException
    {
        // 1. Initial State: 1 failed-unknown execution
        final RunReportDto initialReport = dataService.getRunReport(TEST_RUN_ID);
        Assertions.assertEquals(1, initialReport.getUnknownCount());
        Assertions.assertEquals(0, initialReport.getKnownCount());
        Assertions.assertEquals("failed-unknown", initialReport.getExecutions().get(0).getStatus());
        Assertions.assertEquals(1, countExecJsonFiles(TEST_RUN_ID));

        // 2. Link Bug Ticket: BUG-9999
        final TestExecutionDto linkedExec = dataService.addBugToExecution(TEST_RUN_ID, "row-unlink-1", "BUG-9999");
        Assertions.assertTrue(linkedExec.getBugs().contains("BUG-9999"));
        Assertions.assertEquals("failed-known", linkedExec.getStatus());

        final RunReportDto reportAfterLink = dataService.getRunReport(TEST_RUN_ID);
        Assertions.assertEquals(0, reportAfterLink.getUnknownCount());
        Assertions.assertEquals(1, reportAfterLink.getKnownCount());
        Assertions.assertEquals(1, countExecJsonFiles(TEST_RUN_ID));

        // 3. Unlink Bug Ticket: BUG-9999
        final TestExecutionDto unlinkedExec = dataService.removeBugFromExecution(TEST_RUN_ID, "row-unlink-1", "BUG-9999");
        Assertions.assertFalse(unlinkedExec.getBugs().contains("BUG-9999"));
        Assertions.assertEquals("failed-unknown", unlinkedExec.getStatus());

        final RunReportDto reportAfterUnlink = dataService.getRunReport(TEST_RUN_ID);
        Assertions.assertEquals(1, reportAfterUnlink.getUnknownCount());
        Assertions.assertEquals(0, reportAfterUnlink.getKnownCount());
        Assertions.assertEquals(1, countExecJsonFiles(TEST_RUN_ID));
    }

    @Test
    public void testFirstBugLinkOnRawExecutionWithoutId() throws java.io.IOException
    {
        final String rawRunId = "run-raw-no-id-99";
        final TestRunEntity runEntity = new TestRunEntity(
            rawRunId, "Raw Run Batch", "COMPLETED", "Raw test", "Staging", "en_US", "Chrome", "2026-08-17 12:00:00", System.currentTimeMillis()
        );
        runRepository.save(runEntity);

        // Manually create a raw execution JSON file WITHOUT an "id" field
        final java.nio.file.Path runDir = storageService.getRunDir(rawRunId);
        final java.nio.file.Path rawExecDir = runDir.resolve("Browsing (default)").resolve("RawTest");
        java.nio.file.Files.createDirectories(rawExecDir);
        final java.nio.file.Path rawFile = rawExecDir.resolve("console-execution-1.json");

        final com.fasterxml.jackson.databind.node.ObjectNode rawNode = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        rawNode.put("testClass", "RawTest");
        rawNode.put("title", "executeRawTest");
        rawNode.put("status", "failed");
        rawNode.put("areaName", "Browsing (default)");
        new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(rawFile.toFile(), rawNode);

        Assertions.assertEquals(1, countExecJsonFiles(rawRunId));

        // First Bug Link
        final TestExecutionDto linkedExec = dataService.addBugToExecution(rawRunId, "console-execution-1", "BUG-101");
        Assertions.assertNotNull(linkedExec);
        Assertions.assertTrue(linkedExec.getBugs().contains("BUG-101"));

        // Verify exactly 1 execution JSON file exists (no duplicate file created on first bug link!)
        Assertions.assertEquals(1, countExecJsonFiles(rawRunId));
    }

    private long countExecJsonFiles(final String runId) throws java.io.IOException
    {
        final java.nio.file.Path runDir = storageService.getRunDir(runId);
        if (!java.nio.file.Files.exists(runDir))
        {
            return 0;
        }
        try (final var stream = java.nio.file.Files.walk(runDir))
        {
            return stream.filter(p -> p.toString().endsWith(".json") && !p.getFileName().toString().equals("run.json") && !p.getFileName().toString().equals("batch.json"))
                         .count();
        }
    }
}
