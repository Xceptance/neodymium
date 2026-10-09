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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xceptance.aura.report.dto.BatchOverviewDataDto;
import com.xceptance.aura.report.dto.RunReportDto;
import com.xceptance.aura.report.dto.TestBaseVariationHistoryDto;
import com.xceptance.aura.report.dto.TestExecutionDto;
import com.xceptance.aura.report.entity.TestBaseBugEntity;
import com.xceptance.aura.report.entity.TestBaseVariationEntity;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.repository.TestBaseBugRepository;
import com.xceptance.aura.report.repository.TestBaseVariationRepository;
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

    @Autowired
    private TestBaseVariationRepository variationRepository;

    @Autowired
    private RunStorageSyncService runStorageSyncService;

    private static final String TEST_RUN_ID = "run-unlink-test-101";

    @BeforeEach
    public void setUp() throws IOException
    {
        dataService.clearCache();
        bugRepository.deleteAll();
        runRepository.deleteAll();
        variationRepository.deleteAll();

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

        final Path runDir = storageService.getRunDir(TEST_RUN_ID);
        final Path execDir = runDir.resolve("Checkout").resolve("CheckoutTest");
        Files.createDirectories(execDir);
        final Path execFile = execDir.resolve("exec-1.json");

        final ObjectNode execNode = new ObjectMapper().createObjectNode();
        execNode.put("id", "row-unlink-1");
        execNode.put("runId", TEST_RUN_ID);
        execNode.put("testClass", "CheckoutTest");
        execNode.put("title", "testCheckoutProcess");
        execNode.put("status", "failed");
        execNode.put("areaName", "Checkout");
        execNode.put("location", "US-West");
        execNode.put("browser", "Chrome");
        execNode.put("failureReason", "AssertionError: Element not found");
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(execFile.toFile(), execNode);

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

    @AfterEach
    public void tearDown() throws IOException
    {
        dataService.clearCache();
        for (final String runId : List.of(TEST_RUN_ID, "run-raw-no-id-99", "run-initial-1", "run-upfollowing-2", "run-ai-link-unlink-1", "run-persistence-102", "run-embedded-bug-103", "run-hist-sanitize-104", "run-unlink-execute-test-method", "run-composite-hash-key-105"))
        {
            final Path runDir = storageService.getRunDir(runId);
            if (Files.exists(runDir))
            {
                try (final var stream = Files.walk(runDir))
                {
                    stream.sorted(Comparator.reverseOrder())
                          .map(Path::toFile)
                          .forEach(java.io.File::delete);
                }
            }
            final Path flatFile = storageService.getRunJsonPath(runId);
            if (Files.exists(flatFile) && !Files.isDirectory(flatFile))
            {
                Files.deleteIfExists(flatFile);
            }
        }
        bugRepository.deleteAll();
        runRepository.deleteAll();
        variationRepository.deleteAll();
    }

    @Test
    public void testUnlinkBugFromInitialReportAfterUnlinkingFromUpfollowingReport() throws IOException
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
    public void testLinkAndUnlinkBugTicket() throws IOException
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
    public void testFirstBugLinkOnRawExecutionWithoutId() throws IOException
    {
        final String rawRunId = "run-raw-no-id-99";
        final TestRunEntity runEntity = new TestRunEntity(
            rawRunId, "Raw Run Batch", "COMPLETED", "Raw test", "Staging", "en_US", "Chrome", "2026-08-17 12:00:00", System.currentTimeMillis()
        );
        runRepository.save(runEntity);

        // Manually create a raw execution JSON file WITHOUT an "id" field
        final Path runDir = storageService.getRunDir(rawRunId);
        final Path rawExecDir = runDir.resolve("Browsing (default)").resolve("RawTest");
        Files.createDirectories(rawExecDir);
        final Path rawFile = rawExecDir.resolve("console-execution-1.json");

        final ObjectNode rawNode = new ObjectMapper().createObjectNode();
        rawNode.put("testClass", "RawTest");
        rawNode.put("title", "executeRawTest");
        rawNode.put("status", "failed");
        rawNode.put("areaName", "Browsing (default)");
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(rawFile.toFile(), rawNode);

        Assertions.assertEquals(1, countExecJsonFiles(rawRunId));

        // First Bug Link
        final TestExecutionDto linkedExec = dataService.addBugToExecution(rawRunId, "console-execution-1", "BUG-101");
        Assertions.assertNotNull(linkedExec);
        Assertions.assertTrue(linkedExec.getBugs().contains("BUG-101"));

        // Verify exactly 1 execution JSON file exists (no duplicate file created on first bug link!)
        Assertions.assertEquals(1, countExecJsonFiles(rawRunId));
    }

    @Test
    public void testAiDrivenExecutionBugLinkAndUnlinkUpdatesRunEntity() throws IOException
    {
        final String runId = "run-ai-link-unlink-1";
        final TestRunEntity runEntity = new TestRunEntity(
            runId,
            "AI Link Test Batch",
            "COMPLETED",
            "AI link test",
            "US-West",
            "en_US",
            "Chrome",
            "2026-08-17 12:00:00",
            System.currentTimeMillis()
        );
        runEntity.setTotalTests(1);
        runEntity.setFailedUnknownCount(1);
        runEntity.setFailedKnownCount(0);
        runEntity.setFailedUnknownAiCount(1);
        runEntity.setFailedKnownAiCount(0);
        runRepository.save(runEntity);

        final Path runDir = storageService.getRunDir(runId);
        final Path execDir = runDir.resolve("Browsing (default)").resolve("AiTest");
        Files.createDirectories(execDir);
        final Path execFile = execDir.resolve("exec-ai-1.json");

        final ObjectNode execNode = new ObjectMapper().createObjectNode();
        execNode.put("id", "row-ai-1");
        execNode.put("runId", runId);
        execNode.put("testClass", "AiTest");
        execNode.put("title", "testAiWorkflow");
        execNode.put("status", "failed");
        execNode.put("areaName", "Browsing (default)");
        execNode.put("location", "US-West");
        execNode.put("browser", "Chrome");
        execNode.put("failureReason", "AssertionError: Element not found");
        execNode.put("llmCallsCount", 2);
        execNode.put("executionMode", "LLM_ONLY");
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(execFile.toFile(), execNode);

        final TestExecutionDto exec = new TestExecutionDto(
            "row-ai-1",
            runId,
            "AiTest",
            "testAiWorkflow",
            "AiTest testAiWorkflow",
            "1.5s",
            "2026-08-17 12:00:00",
            "failed",
            "JUnit5",
            "US-West",
            "Chrome",
            "AssertionError: Element not found",
            List.of(),
            null,
            "Browsing (default)",
            List.of(),
            null,
            null,
            null,
            null
        );
        exec.setExecutionMode("LLM_ONLY");

        final RunReportDto report = new RunReportDto(
            runId,
            "AI Link Test Batch",
            "2026-08-17 12:00:00",
            "1.5s",
            1,
            0,
            0,
            0,
            1,
            0,
            List.of(exec)
        );
        dataService.saveRunReportToDisk(runId, report);

        // 1. Initial State
        Assertions.assertTrue(exec.isAiDriven());
        Assertions.assertTrue(exec.isUnknownAi());
        Assertions.assertFalse(exec.isKnownAi());

        // 2. Link Bug Ticket: BUG-AI-42
        dataService.addBugToExecution(runId, "row-ai-1", "BUG-AI-42");
        dataService.updateCachedReportBugsAndStats(runId);

        final RunReportDto reportAfterLink = dataService.getRunReport(runId);
        Assertions.assertEquals(1, reportAfterLink.getKnownCount());
        Assertions.assertEquals(0, reportAfterLink.getUnknownCount());
        Assertions.assertEquals(1, reportAfterLink.getKnownAiCount());
        Assertions.assertEquals(0, reportAfterLink.getUnknownAiCount());

        final TestRunEntity entityAfterLink = runRepository.findById(runId).orElseThrow();
        Assertions.assertEquals(1, entityAfterLink.getFailedKnownCount());
        Assertions.assertEquals(0, entityAfterLink.getFailedUnknownCount());
        Assertions.assertEquals(1, entityAfterLink.getFailedKnownAiCountSafe());
        Assertions.assertEquals(0, entityAfterLink.getFailedUnknownAiCountSafe());

        // 3. Unlink Bug Ticket: BUG-AI-42
        dataService.removeBugFromExecution(runId, "row-ai-1", "BUG-AI-42");
        dataService.updateCachedReportBugsAndStats(runId);

        final RunReportDto reportAfterUnlink = dataService.getRunReport(runId);
        Assertions.assertEquals(0, reportAfterUnlink.getKnownCount());
        Assertions.assertEquals(1, reportAfterUnlink.getUnknownCount());
        Assertions.assertEquals(0, reportAfterUnlink.getKnownAiCount());
        Assertions.assertEquals(1, reportAfterUnlink.getUnknownAiCount());

        final TestRunEntity entityAfterUnlink = runRepository.findById(runId).orElseThrow();
        Assertions.assertEquals(0, entityAfterUnlink.getFailedKnownCount());
        Assertions.assertEquals(1, entityAfterUnlink.getFailedUnknownCount());
        Assertions.assertEquals(0, entityAfterUnlink.getFailedKnownAiCountSafe());
        Assertions.assertEquals(1, entityAfterUnlink.getFailedUnknownAiCountSafe());
    }

    @Test
    public void testBugUnlinkPersistenceAcrossCacheEvictionAndDiskReload() throws IOException
    {
        final String runId = "run-persistence-102";
        final TestRunEntity runEntity = new TestRunEntity(
            runId,
            "Persistence Batch",
            "COMPLETED",
            "Cache eviction test",
            "US-West",
            "en_US",
            "Chrome",
            "2026-08-17 12:00:00",
            System.currentTimeMillis()
        );
        runEntity.setTotalTests(1);
        runEntity.setFailedUnknownCount(1);
        runEntity.setFailedKnownCount(0);
        runRepository.save(runEntity);

        final Path runDir = storageService.getRunDir(runId);
        final Path execDir = runDir.resolve("Browsing (default)").resolve("CartTest");
        Files.createDirectories(execDir);
        final Path execFile = execDir.resolve("console-execution-1.json");

        final ObjectNode execNode = new ObjectMapper().createObjectNode();
        execNode.put("id", "row-persist-1");
        execNode.put("runId", runId);
        execNode.put("testClass", "CartTest");
        execNode.put("title", "testAddCart");
        execNode.put("status", "failed");
        execNode.put("areaName", "Browsing (default)");
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(execFile.toFile(), execNode);

        // 1. Link bug
        dataService.addBugToExecution(runId, "row-persist-1", "BUG-8888");
        final RunReportDto reportLinked = dataService.getRunReport(runId);
        Assertions.assertEquals(1, reportLinked.getKnownCount());
        Assertions.assertEquals(0, reportLinked.getUnknownCount());

        // 2. Unlink bug
        final TestExecutionDto unlinked = dataService.removeBugFromExecution(runId, "row-persist-1", "BUG-8888");
        Assertions.assertEquals("failed-unknown", unlinked.getStatus());
        Assertions.assertFalse(unlinked.getBugs().contains("BUG-8888"));

        // 3. Clear in-memory cache to simulate page refresh / server reload
        dataService.clearCache();

        // 4. Re-fetch from disk storage
        final RunReportDto reloadedReport = dataService.getRunReport(runId);
        Assertions.assertEquals(0, reloadedReport.getKnownCount(), "Known count must remain 0 after cache eviction");
        Assertions.assertEquals(1, reloadedReport.getUnknownCount(), "Unknown count must be restored to 1 after cache eviction");
        Assertions.assertFalse(reloadedReport.getExecutions().get(0).getBugs().contains("BUG-8888"), "Bug list must not contain unlinked ticket");
        Assertions.assertEquals("failed-unknown", reloadedReport.getExecutions().get(0).getStatus(), "Status must persist as failed-unknown");

        // 5. Verify batch overview data reflects the updated counts
        final BatchOverviewDataDto overview = dataService.getBatchOverviewData();
        final TestRunEntity overviewRun = overview.getRuns().stream()
            .filter(r -> runId.equalsIgnoreCase(r.getId()))
            .findFirst()
            .orElseThrow();
        Assertions.assertEquals(0, overviewRun.getFailedKnownCount());
        Assertions.assertEquals(1, overviewRun.getFailedUnknownCount());
    }

    @Test
    public void testUnlinkEmbeddedBugOriginatingDirectlyFromDiskJson() throws IOException
    {
        final String runId = "run-embedded-bug-103";
        final TestRunEntity runEntity = new TestRunEntity(
            runId,
            "Embedded Bug Batch",
            "COMPLETED",
            "Raw bug test",
            "US-West",
            "en_US",
            "Chrome",
            "2026-08-17 12:00:00",
            System.currentTimeMillis()
        );
        runEntity.setTotalTests(1);
        runEntity.setFailedKnownCount(1);
        runEntity.setFailedUnknownCount(0);
        runRepository.save(runEntity);

        final Path runDir = storageService.getRunDir(runId);
        final Path execDir = runDir.resolve("Browsing (default)").resolve("OrderTest");
        Files.createDirectories(execDir);
        final Path execFile = execDir.resolve("console-execution-1.json");

        // Create execution on disk with bugs directly embedded (e.g. from @Bug annotation or runner)
        final ObjectNode execNode = new ObjectMapper().createObjectNode();
        execNode.put("id", "row-embedded-1");
        execNode.put("runId", runId);
        execNode.put("testClass", "OrderTest");
        execNode.put("title", "testOrderFlow");
        execNode.put("status", "failed-known");
        execNode.put("areaName", "Browsing (default)");
        final ArrayNode bugsArray = new ObjectMapper().createArrayNode();
        bugsArray.add("BUG-RAW-77");
        execNode.set("bugs", bugsArray);
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(execFile.toFile(), execNode);

        // Verify initial state sees the embedded bug
        final RunReportDto initialReport = dataService.getRunReport(runId);
        Assertions.assertEquals(1, initialReport.getKnownCount());
        Assertions.assertEquals(0, initialReport.getUnknownCount());
        Assertions.assertTrue(initialReport.getExecutions().get(0).getBugs().contains("BUG-RAW-77"));

        // Unlink the bug that originated from disk JSON
        final TestExecutionDto unlinked = dataService.removeBugFromExecution(runId, "row-embedded-1", "BUG-RAW-77");
        Assertions.assertEquals("failed-unknown", unlinked.getStatus());
        Assertions.assertFalse(unlinked.getBugs().contains("BUG-RAW-77"));

        // Clear cache (simulating browser page refresh /runs/refresh)
        dataService.clearCache();

        final RunReportDto refreshedReport = dataService.getRunReport(runId);
        Assertions.assertEquals(0, refreshedReport.getKnownCount(), "Known count must be 0 after page refresh");
        Assertions.assertEquals(1, refreshedReport.getUnknownCount(), "Unknown count must be 1 after page refresh");
        Assertions.assertEquals("failed-unknown", refreshedReport.getExecutions().get(0).getStatus());
        Assertions.assertFalse(refreshedReport.getExecutions().get(0).getBugs().contains("BUG-RAW-77"));

        // Verify tombstone was created in bug repository
        final List<TestBaseBugEntity> bugs = bugRepository.findByVariationIdAndBatchNameInAndEnvironmentIn(
            dataService.generateVariationId("OrderTest", null, "testOrderFlow", "Unknown", "Chrome"),
            List.of("Embedded Bug Batch", "ALL"),
            List.of("US-West", "ALL")
        );
        Assertions.assertTrue(bugs.stream().anyMatch(b -> runId.equalsIgnoreCase(b.getRemovedRunId())), "Tombstone record must exist");
    }

    @Test
    public void testUnlinkBugSanitizesVariationHistory() throws IOException
    {
        final String runId = "run-hist-sanitize-104";
        final TestRunEntity runEntity = new TestRunEntity(
            runId,
            "History Sanitize Batch",
            "COMPLETED",
            "History link test",
            "US-West",
            "en_US",
            "Chrome",
            "2026-08-17 12:00:00",
            System.currentTimeMillis()
        );
        runEntity.setTotalTests(1);
        runEntity.setFailedKnownCount(1);
        runEntity.setFailedUnknownCount(0);
        runRepository.save(runEntity);

        final Path runDir = storageService.getRunDir(runId);
        final Path execDir = runDir.resolve("Browsing (default)").resolve("SearchTest");
        Files.createDirectories(execDir);
        final Path execFile = execDir.resolve("console-execution-1.json");

        final ObjectNode execNode = new ObjectMapper().createObjectNode();
        execNode.put("id", "row-hist-1");
        execNode.put("runId", runId);
        execNode.put("testClass", "SearchTest");
        execNode.put("title", "testSearchFlow");
        execNode.put("status", "failed-known");
        execNode.put("areaName", "Browsing (default)");
        final ArrayNode bugsArray = new ObjectMapper().createArrayNode();
        bugsArray.add("BUG-HIST-99");
        execNode.set("bugs", bugsArray);
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(execFile.toFile(), execNode);

        final String varId = dataService.generateVariationId("SearchTest", null, "testSearchFlow", "Unknown", "Chrome");
        final TestBaseVariationEntity varEntity = new TestBaseVariationEntity(
            varId, "SearchTest", null, "testSearchFlow", "@Browsing (default)", "Unknown", "Chrome"
        );
        varEntity.setHistoryLinks("/run-report?runId=" + runId + "&executionId=row-hist-1&batch=History+Sanitize+Batch&engine=Java&ts=Recently&status=failed-known&bugs=BUG-HIST-99");
        varEntity.setLastStatus("failed-known");
        variationRepository.save(varEntity);

        // Unlink the bug
        dataService.removeBugFromExecution(runId, "row-hist-1", "BUG-HIST-99");

        // Verify variation history
        final List<TestBaseVariationHistoryDto> history = dataService.getVariationHistory(
            "SearchTest", "testSearchFlow", "Unknown", "Chrome"
        );
        Assertions.assertFalse(history.isEmpty());
        final TestBaseVariationHistoryDto latestHist = history.get(0);
        Assertions.assertEquals("failed-unknown", latestHist.getStatus(), "History link status must be updated to failed-unknown");
        Assertions.assertTrue(latestHist.getBugs() == null || latestHist.getBugs().isEmpty(), "History link bugs must be empty");

        final TestBaseVariationEntity updatedVar = variationRepository.findById(varId).orElseThrow();
        Assertions.assertEquals("failed-unknown", updatedVar.getLastStatus(), "Variation lastStatus must be synced to failed-unknown");
    }

    @Test
    public void testBugUnlinkPersistenceInMultiFileRunWithoutExplicitIdAndCompositeKey() throws IOException
    {
        final String runId = "run-multi-file-composite-test";
        final TestRunEntity runEntity = new TestRunEntity(
            runId,
            "Multi File Batch",
            "COMPLETED",
            "Regression batch for multi-file composite key unlinking",
            "US-West",
            "en_US",
            "Chrome_1500x1000",
            "2026-08-20 10:00:00",
            System.currentTimeMillis()
        );
        runEntity.setTotalTests(2);
        runEntity.setFailedUnknownCount(0);
        runEntity.setFailedKnownCount(1);
        runEntity.setPassedCount(1);
        runEntity.setSucceededFixedCount(0);
        runEntity.setIgnoredCount(0);
        runEntity.setPassRate(50.0);
        runRepository.save(runEntity);

        final Path runDir = storageService.getRunDir(runId);
        final Path classDir = runDir.resolve("WikipediaSearchTest");
        Files.createDirectories(classDir);

        // File 1: Has bugs, NO explicit "id" field, exactly like real console-execution-1.json
        final Path execFile1 = classDir.resolve("console-execution-1.json");
        final ObjectNode node1 = new ObjectMapper().createObjectNode();
        node1.put("runId", runId);
        node1.put("testClass", "WikipediaSearchTest");
        node1.put("testFile", "com.xceptance.neodymium.WikipediaSearchTest#searchWikipedia");
        node1.put("testId", "wikipedia_search_1");
        node1.put("datasetId", "wikipedia_search_1");
        node1.put("title", "wikipedia_search_1");
        node1.put("browser", "Chrome_1500x1000");
        node1.put("location", "Unknown");
        node1.put("status", "failed");
        final ArrayNode bugs1 = new ObjectMapper().createArrayNode();
        bugs1.add("BUG-COMPOSITE-1");
        node1.set("bugs", bugs1);
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(execFile1.toFile(), node1);

        // File 2: Clean pass, NO explicit "id" field (ensures jsonFiles.size() == 2)
        final Path execFile2 = classDir.resolve("console-execution-2.json");
        final ObjectNode node2 = new ObjectMapper().createObjectNode();
        node2.put("runId", runId);
        node2.put("testClass", "WikipediaSearchTest");
        node2.put("testFile", "com.xceptance.neodymium.WikipediaSearchTest#searchWikipedia");
        node2.put("testId", "wikipedia_search_2");
        node2.put("datasetId", "wikipedia_search_2");
        node2.put("title", "wikipedia_search_2");
        node2.put("browser", "Chrome_1500x1000");
        node2.put("location", "Unknown");
        node2.put("status", "passed");
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(execFile2.toFile(), node2);

        // Generate initial run.json
        storageService.buildRunJsonContent(runDir.toFile(), runId, true);

        // Initial report check
        dataService.clearCache();
        final RunReportDto initialReport = dataService.getRunReport(runId);
        Assertions.assertEquals(2, initialReport.getTotalCount());
        Assertions.assertEquals(1, initialReport.getKnownCount());
        Assertions.assertEquals(0, initialReport.getUnknownCount());

        final TestExecutionDto targetExec = initialReport.getExecutions().stream()
            .filter(e -> "wikipedia_search_1".equals(e.getTitle()))
            .findFirst()
            .orElseThrow();
        final String compositeKey = targetExec.getId();
        Assertions.assertTrue(compositeKey.contains("WikipediaSearchTest"));
        Assertions.assertTrue(compositeKey.contains("wikipedia_search_1"));

        // Unlink the bug using the composite rowId
        final TestExecutionDto unlinkedDto = dataService.removeBugFromExecution(runId, compositeKey, "BUG-COMPOSITE-1");
        Assertions.assertEquals("failed-unknown", unlinkedDto.getStatus());
        Assertions.assertTrue(unlinkedDto.getBugs() == null || unlinkedDto.getBugs().isEmpty());

        // Verify that console-execution-1.json on disk was directly updated!
        final ObjectNode diskNode1 = (ObjectNode) new ObjectMapper().readTree(execFile1.toFile());
        Assertions.assertEquals("failed-unknown", diskNode1.path("status").asText(), "Execution JSON on disk must have status updated to failed-unknown");
        Assertions.assertEquals(0, diskNode1.path("bugs").size(), "Execution JSON on disk must have empty bugs array");

        // Verify that run.json on disk was updated!
        final ObjectNode diskRunJson = (ObjectNode) new ObjectMapper().readTree(runDir.resolve("run.json").toFile());
        Assertions.assertEquals(0, diskRunJson.path("summary").path("known").asInt(), "run.json summary known count must be 0");
        Assertions.assertEquals(1, diskRunJson.path("summary").path("unknown").asInt(), "run.json summary unknown count must be 1");

        // SIMULATE PAGE RELOAD / REFRESH: Clear cache and reload report
        dataService.clearCache();
        final RunReportDto reloadedReport = dataService.getRunReport(runId);
        Assertions.assertEquals(0, reloadedReport.getKnownCount(), "After reload, knownCount must be 0");
        Assertions.assertEquals(1, reloadedReport.getUnknownCount(), "After reload, unknownCount must be 1");

        final TestExecutionDto reloadedExec = reloadedReport.getExecutions().stream()
            .filter(e -> "wikipedia_search_1".equals(e.getTitle()))
            .findFirst()
            .orElseThrow();
        Assertions.assertEquals("failed-unknown", reloadedExec.getStatus(), "After reload, execution status must stay failed-unknown");
        Assertions.assertTrue(reloadedExec.getBugs() == null || reloadedExec.getBugs().isEmpty(), "After reload, execution bugs must remain empty");
    }

    @Test
    public void testBugUnlinkPersistenceOverRefreshWithExecuteTestMethod() throws Exception
    {
        final String runId = "run-unlink-execute-test-method";
        final String testClass = "com.xceptance.neodymium.test.CheckoutTest";
        final String dataSet = "DataSet_1";
        final String browser = "Chrome";
        final String ticket = "BUG-EXEC-METHOD-1";

        // Setup run in repository
        final TestRunEntity runEntity = new TestRunEntity(
            runId,
            "ExecuteTest Batch",
            "COMPLETED",
            "Regression test for executeTest method bug unlinking",
            "US-West",
            "en_US",
            browser,
            "2026-10-09 16:30:00",
            System.currentTimeMillis()
        );
        runEntity.setTotalTests(1);
        runEntity.setFailedKnownCount(1);
        runEntity.setFailedUnknownCount(0);
        runEntity.setPassedCount(0);
        runEntity.setSucceededFixedCount(0);
        runEntity.setIgnoredCount(0);
        runEntity.setPassRate(0.0);
        runRepository.save(runEntity);

        // Setup execution JSON on disk
        final Path runDir = storageService.getRunDir(runId);
        Files.createDirectories(runDir);

        final ObjectMapper mapper = new ObjectMapper();
        final ObjectNode execNode = mapper.createObjectNode();
        final String execId = "exec-checkout-1";
        execNode.put("id", execId);
        execNode.put("testClass", testClass);
        execNode.put("testFile", testClass + "#executeTest");
        execNode.put("title", dataSet);
        execNode.put("browser", browser);
        execNode.put("location", "en_US");
        execNode.put("status", "failed-known");
        execNode.put("areaName", "Checkout");

        final ArrayNode bugsArr = mapper.createArrayNode();
        bugsArr.add(ticket);
        execNode.set("bugs", bugsArr);

        final Path execFile = runDir.resolve(execId + ".json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(execFile.toFile(), execNode);

        // Build run.json
        storageService.buildRunJsonContent(runDir.toFile(), runId, true);

        // Setup DB bug entity with varId lacking method (as created by scanner)
        final String varIdNoMethod = AuraReportDataService.generateVariationId(testClass, null, dataSet, "en_US", browser);
        final TestBaseBugEntity bugEntity = new TestBaseBugEntity(varIdNoMethod, ticket, "ExecuteTest Batch", "Regression test for executeTest method bug unlinking", runEntity.getStartTimeMs(), runId);
        bugRepository.save(bugEntity);

        // Setup variation entity with history link containing the bug
        final String historyUrl = "/run-report?runId=" + runId + "&executionId=" + execId + "&batch=ExecuteTest+Batch&engine=Java&ts=2026-10-09+16:30:00&status=failed-known&bugs=" + ticket;
        final TestBaseVariationEntity varEntity = new TestBaseVariationEntity(
            varIdNoMethod,
            testClass,
            dataSet,
            "Checkout",
            "en_US",
            browser
        );
        varEntity.setLastStatus("failed-known");
        varEntity.setLastExecutedAt(runEntity.getStartTimeMs());
        varEntity.setTotalExecutionsCount(1);
        varEntity.setHistoryLinks(historyUrl);
        variationRepository.save(varEntity);

        // 1. Initial report verification
        dataService.clearCache();
        final RunReportDto initialReport = dataService.getRunReport(runId);
        Assertions.assertEquals(1, initialReport.getTotalCount());
        Assertions.assertEquals(1, initialReport.getKnownCount());
        Assertions.assertEquals(0, initialReport.getUnknownCount());

        final TestExecutionDto initialExec = initialReport.getExecutions().get(0);
        Assertions.assertTrue(initialExec.getBugs() != null && initialExec.getBugs().contains(ticket));

        // 2. Unlink the bug
        final TestExecutionDto unlinkedDto = dataService.removeBugFromExecution(runId, initialExec.getId(), ticket);
        Assertions.assertEquals("failed-unknown", unlinkedDto.getStatus());
        Assertions.assertTrue(unlinkedDto.getBugs() == null || unlinkedDto.getBugs().isEmpty());

        // 3. Simulate page reload / top-bar refresh by running storage sync and clearing cache
        runStorageSyncService.syncLocalRunStorage();
        dataService.clearCache();

        // 4. Reload report after refresh and verify bug remains unlinked!
        final RunReportDto reloadedReport = dataService.getRunReport(runId);
        Assertions.assertEquals(0, reloadedReport.getKnownCount(), "After refresh, known count must remain 0");
        Assertions.assertEquals(1, reloadedReport.getUnknownCount(), "After refresh, unknown count must be 1");

        final TestExecutionDto reloadedExec = reloadedReport.getExecutions().get(0);
        Assertions.assertEquals("failed-unknown", reloadedExec.getStatus(), "After refresh, execution status must remain failed-unknown");
        Assertions.assertTrue(reloadedExec.getBugs() == null || reloadedExec.getBugs().isEmpty(), "After refresh, bugs list must remain empty");

        // 5. Verify variation entity history links and status in DB
        final TestBaseVariationEntity updatedVar = variationRepository.findById(varIdNoMethod).orElseThrow();
        Assertions.assertFalse(updatedVar.getHistoryLinks().contains(ticket), "Variation history links must not contain unlinked bug ticket");
        Assertions.assertEquals("failed-unknown", updatedVar.getLastStatus(), "Variation last status must be failed-unknown");
    }

    @Test
    public void testBugLinkAndUnlinkPersistenceWithCompositeKeyContainingHashes() throws Exception
    {
        final String runId = "run-composite-hash-key-105";
        final String compositeKey = "Aura_google_test_yaml_Test#executeYamlTest#Default#Chrome_1920x1080";
        final String ticket = "BUG-48868";

        final TestRunEntity runEntity = new TestRunEntity(
            runId, "Composite Key Run Batch", "COMPLETED", "Composite Key Unlink Test", "US-East", "en_US", "Chrome_1920x1080", "2026-08-17 12:00:00", System.currentTimeMillis()
        );
        runEntity.setTotalTests(1);
        runEntity.setFailedUnknownCount(1);
        runEntity.setFailedKnownCount(0);
        runEntity.setPassedCount(0);
        runEntity.setSucceededFixedCount(0);
        runEntity.setIgnoredCount(0);
        runEntity.setPassRate(0.0);
        runRepository.save(runEntity);

        final Path runDir = storageService.getRunDir(runId);
        final Path execDir = runDir.resolve("Aura_google_test_yaml_Test");
        Files.createDirectories(execDir);
        final Path execFile = execDir.resolve("console-execution-1.json");

        final ObjectNode execNode = new ObjectMapper().createObjectNode();
        execNode.put("runId", runId);
        execNode.put("testClass", "Aura_google_test_yaml_Test");
        execNode.put("testMethod", "executeYamlTest");
        execNode.put("title", "Default");
        execNode.put("status", "failed-unknown");
        execNode.put("location", "Unknown");
        execNode.put("browser", "Chrome_1920x1080");
        execNode.put("testFile", "com.xceptance.neodymium.aura.sandbox.Aura_google_test_yaml_Test#executeYamlTest");
        execNode.put("testId", "Default");
        execNode.put("datasetId", "Default");
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(execFile.toFile(), execNode);

        storageService.buildRunJsonContent(runDir.toFile(), runId, true);
        dataService.clearCache();

        // 1. Initial State: Execution has no bugs, failed-unknown
        final RunReportDto reportBefore = dataService.getRunReport(runId);
        Assertions.assertEquals(1, reportBefore.getUnknownCount());
        Assertions.assertEquals(0, reportBefore.getKnownCount());
        final TestExecutionDto execBefore = reportBefore.getExecutions().get(0);
        Assertions.assertEquals(compositeKey, execBefore.getId());
        Assertions.assertEquals("failed-unknown", execBefore.getStatus());

        // 2. Link bug ticket
        final TestExecutionDto linkedDto = dataService.addBugToExecution(runId, compositeKey, ticket);
        Assertions.assertEquals(compositeKey, linkedDto.getId());
        Assertions.assertEquals("failed-known", linkedDto.getStatus());
        Assertions.assertTrue(linkedDto.getBugs() != null && linkedDto.getBugs().contains(ticket));

        // 3. Verify getExecutionDetails on invalid/null ticket does not return hollow DTO
        final TestExecutionDto fallbackDto = dataService.removeBugFromExecution(runId, compositeKey, null);
        Assertions.assertEquals(compositeKey, fallbackDto.getId());
        Assertions.assertEquals("failed-known", fallbackDto.getStatus());
        Assertions.assertTrue(fallbackDto.getBugs() != null && fallbackDto.getBugs().contains(ticket));

        // 4. Unlink bug ticket with composite key
        final TestExecutionDto unlinkedDto = dataService.removeBugFromExecution(runId, compositeKey, ticket);
        Assertions.assertEquals(compositeKey, unlinkedDto.getId());
        Assertions.assertEquals("failed-unknown", unlinkedDto.getStatus());
        Assertions.assertTrue(unlinkedDto.getBugs() == null || unlinkedDto.getBugs().isEmpty());

        // 5. Simulate page reload / cache clear
        dataService.clearCache();
        final RunReportDto reloadedReport = dataService.getRunReport(runId);
        Assertions.assertEquals(0, reloadedReport.getKnownCount(), "Known count must be 0 after reload");
        Assertions.assertEquals(1, reloadedReport.getUnknownCount(), "Unknown count must be 1 after reload");

        final TestExecutionDto reloadedExec = reloadedReport.getExecutions().get(0);
        Assertions.assertEquals("failed-unknown", reloadedExec.getStatus());
        Assertions.assertTrue(reloadedExec.getBugs() == null || reloadedExec.getBugs().isEmpty(), "Bugs must remain empty on disk reload");
    }

    private long countExecJsonFiles(final String runId) throws IOException
    {
        final Path runDir = storageService.getRunDir(runId);
        if (!Files.exists(runDir))
        {
            return 0;
        }
        try (final var stream = Files.walk(runDir))
        {
            return stream.filter(p -> p.toString().endsWith(".json") && !p.getFileName().toString().equals("run.json") && !p.getFileName().toString().equals("batch.json"))
                         .count();
        }
    }
}
