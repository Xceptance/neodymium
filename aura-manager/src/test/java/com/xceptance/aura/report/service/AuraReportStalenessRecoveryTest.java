/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xceptance.aura.report.dto.RunReportDto;
import com.xceptance.aura.report.dto.TestExecutionDto;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.repository.TestRunRepository;
import com.xceptance.neodymium.ai.console.InteractiveConsoleEngine;
import com.xceptance.neodymium.aura.AuraInteractiveService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.neodymium.ai.config.AiConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies staleness detection and automatic report recovery when run.json was generated
 * during test execution and newer finalized test results are written afterwards.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@SpringBootTest
public class AuraReportStalenessRecoveryTest
{
    private static final String TEST_RUN_ID = "run_test_stale_recovery";

    @Autowired
    private LocalRunJsonStorageService storageService;

    @Autowired
    private AuraReportDataService reportDataService;

    @Autowired
    private RunStorageSyncService syncService;

    @Autowired
    private TestRunRepository runRepository;

    @Autowired
    private AuraInteractiveService interactiveService;

    private final ObjectMapper mapper = new ObjectMapper();

    @AfterEach
    public void cleanup()
    {
        reportDataService.evictRunReportCache(TEST_RUN_ID);
        runRepository.deleteById(TEST_RUN_ID);
        final Path runDir = storageService.getRunDir(TEST_RUN_ID);
        if (runDir != null && Files.exists(runDir))
        {
            try
            {
                Files.walk(runDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
            catch (final IOException ignored)
            {
            }
        }
    }

    @Test
    public void testStaleMidRunJsonIsDetectedAndRebuiltOnGetRunReport() throws IOException
    {
        final Path runDir = storageService.getRunDir(TEST_RUN_ID);
        final Path classDir = runDir.resolve("com.xceptance.neodymium.tests.StaleRecoveryTest");
        Files.createDirectories(classDir);

        // 1. Create completed execution 1
        final ObjectNode exec1 = mapper.createObjectNode();
        exec1.put("id", "console-execution-1");
        exec1.put("testClass", "StaleRecoveryTest");
        exec1.put("testMethod", "testSuccess");
        exec1.put("title", "Execution 1");
        exec1.put("status", "passed");
        exec1.put("failedStepsCount", 0);
        exec1.put("totalStepsCount", 2);
        exec1.put("durationMs", 5000L);

        final Path execFile1 = classDir.resolve("console-execution-1.json");
        Files.writeString(execFile1, mapper.writeValueAsString(exec1));

        // 2. Create in-progress execution 2
        final ObjectNode exec2 = mapper.createObjectNode();
        exec2.put("id", "console-execution-2");
        exec2.put("testClass", "StaleRecoveryTest");
        exec2.put("testMethod", "testInFlight");
        exec2.put("title", "Execution 2");
        exec2.put("status", "running");
        exec2.put("failedStepsCount", 0);
        exec2.put("totalStepsCount", 0);
        exec2.put("durationMs", 0L);

        final Path execFile2 = classDir.resolve("console-execution-2.json");
        Files.writeString(execFile2, mapper.writeValueAsString(exec2));

        // 3. Aura Manager generates mid-run run.json while test is active
        storageService.buildRunJsonContent(runDir.toFile(), TEST_RUN_ID, true, true);

        final TestRunEntity activeRun = new TestRunEntity(
            TEST_RUN_ID,
            "Default",
            "IN_PROGRESS",
            "Console",
            "DEV",
            "Default",
            "Chrome_1920x1080",
            "Recently",
            System.currentTimeMillis()
        );
        activeRun.setTotalTests(2);
        activeRun.setRunningCount(1);
        activeRun.setPassedCount(1);
        runRepository.save(activeRun);

        final Path runJsonPath = runDir.resolve("run.json");
        Assertions.assertTrue(Files.exists(runJsonPath), "run.json should exist on disk");

        // 4. Test 2 completes and fails in runner, writing updated console-execution-2.json
        exec2.put("status", "failed");
        exec2.put("failedStepsCount", 1);
        exec2.put("totalStepsCount", 2);
        exec2.put("durationMs", 40887L);
        exec2.put("failure", "org.openqa.selenium.TimeoutException: Timed out waiting for element");

        final ObjectNode blocks = mapper.createObjectNode();
        final ArrayNode steps = mapper.createArrayNode();
        final ObjectNode step1 = mapper.createObjectNode();
        step1.put("instruction", "Open page");
        step1.put("status", "passed");
        steps.add(step1);
        final ObjectNode step2 = mapper.createObjectNode();
        step2.put("instruction", "Click element");
        step2.put("status", "failed");
        steps.add(step2);
        blocks.set("steps", steps);
        exec2.set("blocks", blocks);

        Files.writeString(execFile2, mapper.writeValueAsString(exec2));

        // Set the stale run.json timestamp to 10 seconds in the past to simulate it being older than the final test execution
        final boolean timestampSet = runJsonPath.toFile().setLastModified(System.currentTimeMillis() - 10_000L);
        Assertions.assertTrue(timestampSet, "Failed to set run.json file timestamp");

        // 5. Verify staleness detector identifies the stale run.json
        Assertions.assertTrue(storageService.isRunJsonStale(TEST_RUN_ID), "Run JSON should be detected as stale");

        // 6. Request report via AuraReportDataService (simulates UI or API report call)
        final RunReportDto report = reportDataService.getRunReport(TEST_RUN_ID);
        Assertions.assertNotNull(report, "Generated report must not be null");
        Assertions.assertEquals(2, report.getTotalCount(), "Total tests must be 2");
        Assertions.assertEquals(1, report.getPassCount(), "Pass count must be 1");
        Assertions.assertEquals(1, report.getUnknownCount(), "Failed unknown count must be 1");
        Assertions.assertEquals(0, report.getRunningCount(), "Running count must be 0");

        final TestExecutionDto retrievedExec2 = report.getExecutions().stream()
            .filter(e -> "console-execution-2".equals(e.getId()))
            .findFirst()
            .orElseThrow();
        Assertions.assertEquals("failed-unknown", retrievedExec2.getStatus(), "Execution 2 status must be failed-unknown");
        Assertions.assertEquals(40887L, retrievedExec2.getDurationMs(), "Execution 2 duration must be 40887 ms");

        // 7. Verify run.json was rewritten and is no longer stale
        Assertions.assertFalse(storageService.isRunJsonStale(TEST_RUN_ID), "Run JSON should no longer be stale after rebuild");

        // 8. Verify DB entity was updated to COMPLETED with runningCount = 0
        final Optional<TestRunEntity> dbRunOpt = runRepository.findById(TEST_RUN_ID);
        Assertions.assertTrue(dbRunOpt.isPresent(), "Test run entity must exist in DB");
        final TestRunEntity dbRun = dbRunOpt.get();
        Assertions.assertEquals("COMPLETED", dbRun.getStatus(), "DB status must be COMPLETED");
        Assertions.assertEquals(0, dbRun.getRunningCount(), "DB running count must be 0");
        Assertions.assertEquals(1, dbRun.getFailedUnknownCount(), "DB failed unknown count must be 1");
    }

    @Test
    public void testSyncLocalRunStorageRepairsStaleRunAndUndeletes() throws IOException
    {
        final Path runDir = storageService.getRunDir(TEST_RUN_ID);
        final Path classDir = runDir.resolve("com.xceptance.neodymium.tests.SyncStaleTest");
        Files.createDirectories(classDir);

        final ObjectNode exec1 = mapper.createObjectNode();
        exec1.put("id", "console-execution-1");
        exec1.put("testClass", "SyncStaleTest");
        exec1.put("testMethod", "testOne");
        exec1.put("title", "Execution 1");
        exec1.put("status", "running");
        exec1.put("failedStepsCount", 0);
        exec1.put("totalStepsCount", 0);
        exec1.put("durationMs", 0L);

        final Path execFile1 = classDir.resolve("console-execution-1.json");
        Files.writeString(execFile1, mapper.writeValueAsString(exec1));

        // Create stale run.json with in-progress status while test was running
        storageService.buildRunJsonContent(runDir.toFile(), TEST_RUN_ID, true, true);

        // Now test finishes and passes, but run.json on disk is still the in-progress one from 10 seconds ago
        exec1.put("status", "passed");
        exec1.put("failedStepsCount", 0);
        exec1.put("totalStepsCount", 1);
        exec1.put("durationMs", 2000L);
        Files.writeString(execFile1, mapper.writeValueAsString(exec1));

        final Path runJsonPath = runDir.resolve("run.json");
        runJsonPath.toFile().setLastModified(System.currentTimeMillis() - 10_000L);

        // Populate DB entity simulating a soft-deleted run with stale running count
        final TestRunEntity initialRun = new TestRunEntity(
            TEST_RUN_ID,
            "Default",
            "IN_PROGRESS",
            "Console",
            "DEV",
            "Default",
            "Chrome_1920x1080",
            "Recently",
            System.currentTimeMillis()
        );
        initialRun.setIsDeleted(true);
        initialRun.setTotalTests(1);
        initialRun.setRunningCount(1);
        initialRun.setPassedCount(0);
        initialRun.setPassedHealedCount(0);
        initialRun.setSucceededFixedHealedCount(0);
        initialRun.setFailedKnownHealedCount(0);
        initialRun.setFailedUnknownHealedCount(0);
        runRepository.save(initialRun);

        // Execution is no longer active on disk, so isRunJsonStale is true
        Assertions.assertTrue(storageService.isRunJsonStale(TEST_RUN_ID), "Run JSON should be detected as stale");

        // Run syncLocalRunStorage (simulating user clicking Refresh)
        final int importedCount = syncService.syncLocalRunStorage();
        Assertions.assertTrue(importedCount >= 1, "Sync service should process stale run");

        final Optional<TestRunEntity> updatedRunOpt = runRepository.findById(TEST_RUN_ID);
        Assertions.assertTrue(updatedRunOpt.isPresent(), "Run entity must be present in DB");
        final TestRunEntity updatedRun = updatedRunOpt.get();
        Assertions.assertEquals(Boolean.FALSE, updatedRun.getIsDeleted(), "Run entity should be un-deleted");
        Assertions.assertEquals("COMPLETED", updatedRun.getStatus(), "Run entity status should be COMPLETED");
        Assertions.assertEquals(0, updatedRun.getRunningCount(), "Run entity runningCount should be 0");
        Assertions.assertEquals(1, updatedRun.getPassedCount(), "Run entity passedCount should be 1");
    }

    @Test
    public void testInteractivePushStateAutomaticallyFinalizesMidRunReportAndDatabaseEntity(@TempDir final Path tempDir)
        throws IOException
    {
        runWithPushStateEnvironment(tempDir, () -> {
            // 1. Push initial active ("running") execution state via AuraInteractiveService
            final ObjectNode runningPayload = buildPushPayload(
                "com.xceptance.neodymium.tests.PushStateRecoveryTest",
                "testPushLifecycle",
                "dataset_1",
                "running",
                0L
            );

            final String pushResult1 = interactiveService.pushState(mapper.writeValueAsString(runningPayload), false);
            Assertions.assertEquals("ok", pushResult1);

            final Optional<TestRunEntity> inProgressEntityOpt = runRepository.findById(TEST_RUN_ID);
            Assertions.assertTrue(inProgressEntityOpt.isPresent(), "Run entity must be created on first running push");
            final TestRunEntity inProgressEntity = inProgressEntityOpt.get();
            Assertions.assertEquals("IN_PROGRESS", inProgressEntity.getStatus(), "Run status must initially be IN_PROGRESS");
            Assertions.assertEquals(1, inProgressEntity.getRunningCount(), "Running count must be 1");

            // 2. Fetch report mid-run to populate run.json and in-progress report state
            final RunReportDto midRunReport = reportDataService.getRunReport(TEST_RUN_ID);
            Assertions.assertNotNull(midRunReport, "Mid-run report must not be null");
            Assertions.assertEquals(1, midRunReport.getRunningCount(), "Mid-run report should show 1 running execution");

            // 3. Push terminal failed execution state for the same execution
            final ObjectNode failedPayload = runningPayload.deepCopy();
            failedPayload.put("status", "failed");
            failedPayload.put("duration", 46090L);
            failedPayload.put("failureReason", "Element not found");

            final String pushResult2 = interactiveService.pushState(mapper.writeValueAsString(failedPayload), false);
            Assertions.assertEquals("ok", pushResult2);

            // 4. Verify DB entity automatically transitioned to COMPLETED with runningCount = 0
            final Optional<TestRunEntity> completedEntityOpt = runRepository.findById(TEST_RUN_ID);
            Assertions.assertTrue(completedEntityOpt.isPresent(), "Run entity must remain in DB");
            final TestRunEntity completedEntity = completedEntityOpt.get();
            Assertions.assertEquals("COMPLETED", completedEntity.getStatus(), "DB status must transition to COMPLETED");
            Assertions.assertEquals(0, completedEntity.getRunningCount(), "DB running count must be 0");
            Assertions.assertEquals(1, completedEntity.getFailedUnknownCount(), "DB failedUnknownCount must be 1");

            // 5. Request report again and verify cache eviction + regenerated finalized state
            final RunReportDto finalReport = reportDataService.getRunReport(TEST_RUN_ID);
            Assertions.assertNotNull(finalReport, "Final report must not be null");
            Assertions.assertEquals(0, finalReport.getRunningCount(), "Final report runningCount must be 0");
            Assertions.assertEquals(1, finalReport.getUnknownCount(), "Final report unknownCount must be 1");
            Assertions.assertEquals(1, finalReport.getExecutions().size(), "Final report must contain 1 execution");

            final TestExecutionDto finalExec = finalReport.getExecutions().get(0);
            Assertions.assertEquals("failed-unknown", finalExec.getStatus(), "Execution status must be failed-unknown");
            Assertions.assertEquals(46090L, finalExec.getDurationMs(), "Execution durationMs must be 46090");
        });
    }

    @Test
    public void testMultiTestConsoleRunStaysActiveBetweenTestsUntilRunFinished(@TempDir final Path tempDir)
        throws IOException
    {
        runWithPushStateEnvironment(tempDir, () -> {
            // 1. Test 1 runs and completes with runActive=true while Test 2 has not started yet
            final ObjectNode test1Running = buildPushPayload(
                "com.xceptance.neodymium.tests.MultiDatasetConsoleTest",
                "executeSearch",
                "dataset_1",
                "running",
                0L
            );
            test1Running.put("runActive", true);
            interactiveService.pushState(mapper.writeValueAsString(test1Running), false);

            final ObjectNode test1Passed = test1Running.deepCopy();
            test1Passed.put("status", "passed");
            test1Passed.put("duration", 12000L);
            interactiveService.pushState(mapper.writeValueAsString(test1Passed), false);

            // During the inter-test transition gap (Test 1 passed, Test 2 not created yet), run must remain active
            Assertions.assertTrue(storageService.isRunCurrentlyActiveOnDisk(TEST_RUN_ID),
                "Run must stay active on disk during inter-test transition when runActive=true");
            final TestRunEntity midRunEntity = runRepository.findById(TEST_RUN_ID).orElseThrow();
            Assertions.assertEquals("IN_PROGRESS", midRunEntity.getStatus(),
                "DB status must remain IN_PROGRESS between Test 1 and Test 2");

            // 2. Test 2 executes and passes with runActive=true (still tearing down browser before root store closes)
            final ObjectNode test2Running = buildPushPayload(
                "com.xceptance.neodymium.tests.MultiDatasetConsoleTest",
                "executeSearch",
                "dataset_2",
                "running",
                0L
            );
            test2Running.put("runActive", true);
            interactiveService.pushState(mapper.writeValueAsString(test2Running), false);

            final ObjectNode test2Passed = test2Running.deepCopy();
            test2Passed.put("status", "passed");
            test2Passed.put("duration", 15000L);
            interactiveService.pushState(mapper.writeValueAsString(test2Passed), false);

            // 3. Entire JUnit run completes -> markRunFinished pushes runActive=false
            final ObjectNode test2RunFinished = test2Passed.deepCopy();
            test2RunFinished.put("runActive", false);
            interactiveService.pushState(mapper.writeValueAsString(test2RunFinished), false);

            Assertions.assertFalse(storageService.isRunCurrentlyActiveOnDisk(TEST_RUN_ID),
                "Run must immediately become inactive once runActive=false is pushed");
            final TestRunEntity completedEntity = runRepository.findById(TEST_RUN_ID).orElseThrow();
            Assertions.assertEquals("COMPLETED", completedEntity.getStatus(),
                "DB status must transition to COMPLETED once runActive=false is pushed");
            Assertions.assertEquals(2, completedEntity.getPassedCount(), "Both datasets must be recorded as passed");
            Assertions.assertEquals(0, completedEntity.getRunningCount(), "Running count must be 0");
        });
    }

    private ObjectNode buildPushPayload(
        final String testClass,
        final String testMethod,
        final String datasetId,
        final String status,
        final long durationMs
    )
    {
        final ObjectNode payload = mapper.createObjectNode();
        payload.put("runId", TEST_RUN_ID);
        payload.put("testClass", testClass);
        payload.put("testMethod", testMethod);
        payload.put("testFile", testClass + "#" + testMethod);
        payload.put("title", datasetId);
        payload.put("datasetId", datasetId);
        payload.put("browser", "Chrome_1500x1000");
        payload.put("status", status);
        payload.put("duration", durationMs);
        return payload;
    }

    private void runWithPushStateEnvironment(final Path tempDir, final ThrowingIoAction action) throws IOException
    {
        final String storageRootPath = storageService.getRunDir(TEST_RUN_ID).getParent().toAbsolutePath().toString();
        final String prevStorageRunsDir = System.getProperty("neodymium.ai.storageRuns.directory");
        final String prevConsoleLogsDir = System.getProperty("neodymium.ai.consoleExecutionLogs.directory");
        final String prevDiskReportDir = System.getProperty("neodymium.ai.report.disk.directory");
        final String prevAllureDir = System.getProperty("allure.results.directory");
        final String prevConsoleLogs = System.getProperty("neodymium.ai.consoleExecutionLogs");
        final String prevManagerActive = System.getProperty("neodymium.managerActive");

        System.setProperty("neodymium.ai.storageRuns.directory", storageRootPath);
        System.setProperty("neodymium.ai.consoleExecutionLogs.directory", tempDir.resolve("console-logs").toString());
        System.setProperty("neodymium.ai.report.disk.directory", tempDir.resolve("ai-reports").toString());
        System.setProperty("allure.results.directory", tempDir.resolve("allure-results").toString());
        System.setProperty("neodymium.ai.consoleExecutionLogs", "true");
        System.clearProperty("neodymium.managerActive");
        AiConfiguration.resetInstance();
        InteractiveConsoleEngine.resetExecutionIndexes();
        interactiveService.resetExecutionIndexes();
        interactiveService.setCurrentConsoleEngine(null);

        try
        {
            action.run();
        }
        finally
        {
            restoreSystemProperty("neodymium.ai.storageRuns.directory", prevStorageRunsDir);
            restoreSystemProperty("neodymium.ai.consoleExecutionLogs.directory", prevConsoleLogsDir);
            restoreSystemProperty("neodymium.ai.report.disk.directory", prevDiskReportDir);
            restoreSystemProperty("allure.results.directory", prevAllureDir);
            restoreSystemProperty("neodymium.ai.consoleExecutionLogs", prevConsoleLogs);
            restoreSystemProperty("neodymium.managerActive", prevManagerActive);
            interactiveService.setCurrentConsoleEngine(null);
            interactiveService.resetExecutionIndexes();
            InteractiveConsoleEngine.resetExecutionIndexes();
            AiConfiguration.resetInstance();
        }
    }

    @FunctionalInterface
    private interface ThrowingIoAction
    {
        void run() throws IOException;
    }

    private static void restoreSystemProperty(final String key, final String previousValue)
    {
        if (previousValue == null)
        {
            System.clearProperty(key);
        }
        else
        {
            System.setProperty(key, previousValue);
        }
    }
}
