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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xceptance.aura.report.dto.RunReportDto;
import com.xceptance.aura.report.dto.TestBaseDataDto;
import com.xceptance.aura.report.dto.TestExecutionDto;
import com.xceptance.aura.report.entity.TestBaseBugEntity;
import com.xceptance.aura.report.entity.TestBaseVariationEntity;
import com.xceptance.aura.report.entity.TestBatchEntity;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.repository.TestBaseBugRepository;
import com.xceptance.aura.report.repository.TestBaseVariationRepository;
import com.xceptance.aura.report.repository.TestBatchRepository;
import com.xceptance.aura.report.repository.TestRunRepository;
import java.io.File;
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

/**
 * Integration tests verifying flaky test detection, annotation, filtering, and reporting
 * across historical runs for tests suffering recurring unknown failures.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
@SpringBootTest
public class FlakyTestReportingTest
{
    @Autowired
    private AuraReportDataService dataService;

    @Autowired
    private TestBatchRepository batchRepository;

    @Autowired
    private TestRunRepository runRepository;

    @Autowired
    private TestBaseVariationRepository variationRepository;

    @Autowired
    private TestBaseBugRepository bugRepository;

    @Autowired
    private LocalRunJsonStorageService storageService;

    private static final String BATCH_NAME = "Flaky Detection Suite";
    private static final String TEST_CLASS = "com.xceptance.aura.FlakyOrderTest";
    private static final String TEST_METHOD = "testCheckout";
    private static final String DATA_SET = "Default";
    private static final String LOCATION = "US";
    private static final String BROWSER = "Chrome";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp()
    {
        dataService.clearCache();
        bugRepository.deleteAll();
        variationRepository.deleteAll();
        batchRepository.deleteAll();
        runRepository.deleteAll();

        final TestBatchEntity batch = new TestBatchEntity(
            BATCH_NAME,
            "Staging",
            "Batch for flaky test verification",
            LOCATION,
            BROWSER,
            "run-flaky-current"
        );
        batchRepository.save(batch);
    }

    @AfterEach
    public void tearDown()
    {
        dataService.clearCache();
        cleanupRunOnDisk("run-flaky-1");
        cleanupRunOnDisk("run-flaky-2");
        cleanupRunOnDisk("run-flaky-current");
    }

    @Test
    public void testFlakyTestDetectedAcrossPreviousRuns() throws IOException
    {
        final long now = System.currentTimeMillis();

        // 1. Previous Run 1 (10 minutes ago) - test failed with unknown error
        createAndSaveRun("run-flaky-1", now - 600000L, "failed-unknown");

        // 2. Previous Run 2 (5 minutes ago) - test failed with unknown error
        createAndSaveRun("run-flaky-2", now - 300000L, "failed-unknown");

        // 3. Current Run - test passes clean
        createAndSaveRun("run-flaky-current", now, "passed-clean");

        final RunReportDto report = dataService.getRunReport("run-flaky-current");
        Assertions.assertNotNull(report);
        Assertions.assertEquals(1, report.getFlakyCount(), "Report must count 1 flaky test");

        final List<TestExecutionDto> executions = report.getExecutions();
        Assertions.assertNotNull(executions);
        Assertions.assertEquals(1, executions.size());

        final TestExecutionDto currentExec = executions.get(0);
        Assertions.assertTrue(currentExec.isFlaky(), "Execution must be marked as flaky");
        Assertions.assertEquals(2, currentExec.getFlakyFailureCount(), "Must record 2 historical failures");
        Assertions.assertEquals(2, currentExec.getFlakyTotalRuns(), "Must record 2 historical runs");
        Assertions.assertNotNull(currentExec.getFlakyReason());
        Assertions.assertTrue(currentExec.getFlakyReason().contains("failed in 2 of 2 previous runs (100%) due to unknown failures"),
            "Reason must explain failure frequency: " + currentExec.getFlakyReason());
    }

    @Test
    public void testKnownFailuresDoNotTriggerFlaky() throws IOException
    {
        final long now = System.currentTimeMillis();

        // 1. Previous Run 1 - failed
        createAndSaveRun("run-flaky-1", now - 600000L, "failed");

        // 2. Previous Run 2 - failed
        createAndSaveRun("run-flaky-2", now - 300000L, "failed");

        // Link a known bug ticket to this test variation for the runs
        final String varId = AuraReportDataService.generateVariationId(TEST_CLASS, TEST_METHOD, DATA_SET, LOCATION, BROWSER);
        final TestBaseBugEntity bug = new TestBaseBugEntity(
            varId,
            "BUG-9999",
            BATCH_NAME,
            "Staging",
            now,
            "run-flaky-1",
            "CRITICAL",
            null
        );
        bugRepository.save(bug);

        // 3. Current Run
        createAndSaveRun("run-flaky-current", now, "passed-clean");

        final RunReportDto report = dataService.getRunReport("run-flaky-current");
        Assertions.assertNotNull(report);
        Assertions.assertEquals(0, report.getFlakyCount(), "Known failures must not count towards flakiness");

        final TestExecutionDto currentExec = report.getExecutions().get(0);
        Assertions.assertFalse(currentExec.isFlaky(), "Execution must not be flaky when previous failures had known bugs");
    }

    @Test
    public void testSingleFailureDoesNotTriggerFlaky() throws IOException
    {
        final long now = System.currentTimeMillis();

        // Run 1: Passed
        createAndSaveRun("run-flaky-1", now - 600000L, "passed-clean");

        // Run 2: Failed once with unknown failure
        createAndSaveRun("run-flaky-2", now - 300000L, "failed-unknown");

        // Current Run
        createAndSaveRun("run-flaky-current", now, "passed-clean");

        final RunReportDto report = dataService.getRunReport("run-flaky-current");
        Assertions.assertNotNull(report);
        Assertions.assertEquals(0, report.getFlakyCount(), "1 failure does not meet default threshold of 2");

        final TestExecutionDto currentExec = report.getExecutions().get(0);
        Assertions.assertFalse(currentExec.isFlaky());
    }

    @Test
    public void testFilterRunReportByFlakyStatus() throws IOException
    {
        final long now = System.currentTimeMillis();

        // Previous runs have 2 unknown failures for FlakyOrderTest
        createAndSaveRun("run-flaky-1", now - 600000L, "failed-unknown");
        createAndSaveRun("run-flaky-2", now - 300000L, "failed-unknown");

        // Current run has 2 executions: 1 for FlakyOrderTest and 1 for StableOrderTest
        createAndSaveRunWithTwoTests("run-flaky-current", now);

        final RunReportDto fullReport = dataService.getRunReport("run-flaky-current");
        Assertions.assertEquals(2, fullReport.getTotalCount());
        Assertions.assertEquals(1, fullReport.getFlakyCount());

        final RunReportDto filteredReport = dataService.getFilteredRunReport(
            "run-flaky-current",
            "FLAKY",
            null, null, null, null
        );

        Assertions.assertNotNull(filteredReport);
        Assertions.assertEquals(1, filteredReport.getExecutions().size(), "Filtered report must only include the flaky test");
        Assertions.assertEquals(TEST_CLASS, filteredReport.getExecutions().get(0).getTestClass());
        Assertions.assertTrue(filteredReport.getExecutions().get(0).isFlaky());
    }

    @Test
    public void testTestBaseVariationAnnotatedAsFlaky() throws IOException
    {
        final long now = System.currentTimeMillis();

        createAndSaveRun("run-flaky-1", now - 600000L, "failed-unknown");
        createAndSaveRun("run-flaky-2", now - 300000L, "failed-unknown");
        createAndSaveRun("run-flaky-current", now, "passed-clean");

        final TestBaseDataDto testBase = dataService.getTestBaseData();
        Assertions.assertNotNull(testBase);
        Assertions.assertNotNull(testBase.getAreas());

        final boolean hasFlakyVariation = testBase.getAreas().stream()
            .flatMap(a -> a.getTestClasses().stream())
            .flatMap(c -> c.getVariations().stream())
            .anyMatch(v -> TEST_CLASS.equals(v.getTestClassName()) && v.isFlaky());

        Assertions.assertTrue(hasFlakyVariation, "Test Base must reflect flaky status on the variation entity");
    }

    private void createAndSaveRun(final String runId, final long startTimeMs, final String status) throws IOException
    {
        final TestRunEntity entity = new TestRunEntity(
            runId,
            BATCH_NAME,
            "COMPLETED",
            "Schedule",
            "Staging",
            LOCATION,
            BROWSER,
            "2026-10-30 15:00:00",
            startTimeMs
        );
        entity.setTotalTests(1);
        if ("failed-unknown".equalsIgnoreCase(status) || "failed".equalsIgnoreCase(status))
        {
            entity.setFailedUnknownCount(1);
        }
        else
        {
            entity.setPassedCount(1);
        }
        runRepository.save(entity);

        final String varId = AuraReportDataService.generateVariationId(TEST_CLASS, TEST_METHOD, DATA_SET, LOCATION, BROWSER);
        final TestBaseVariationEntity varEntity = new TestBaseVariationEntity(
            varId,
            TEST_CLASS,
            TEST_METHOD,
            DATA_SET,
            "@Checkout",
            LOCATION,
            BROWSER
        );
        varEntity.setLastStatus(status);
        varEntity.setTotalExecutionsCount(1);
        varEntity.setLastExecutedAt(startTimeMs);
        variationRepository.save(varEntity);

        final Path runDir = storageService.getRunDir(runId);
        Files.createDirectories(runDir);

        final ObjectNode root = objectMapper.createObjectNode();
        final ObjectNode metrics = objectMapper.createObjectNode();

        final ObjectNode exec = objectMapper.createObjectNode();
        exec.put("id", "exec-" + runId);
        exec.put("testClass", TEST_CLASS);
        exec.put("testMethod", TEST_METHOD);
        exec.put("title", DATA_SET);
        exec.put("location", LOCATION);
        exec.put("browser", BROWSER);
        exec.put("areaName", "Checkout");
        exec.put("status", status);
        exec.put("totalStepsCount", 5);
        exec.put("durationMs", 1500L);
        exec.put("timestampMs", startTimeMs);

        metrics.set(exec.get("id").asText(), exec);
        root.set("executionMetrics", metrics);

        Files.writeString(runDir.resolve("run.json"), objectMapper.writeValueAsString(root));
    }

    private void createAndSaveRunWithTwoTests(final String runId, final long startTimeMs) throws IOException
    {
        final TestRunEntity entity = new TestRunEntity(
            runId,
            BATCH_NAME,
            "COMPLETED",
            "Schedule",
            "Staging",
            LOCATION,
            BROWSER,
            "2026-10-30 15:00:00",
            startTimeMs
        );
        entity.setTotalTests(2);
        entity.setPassedCount(2);
        runRepository.save(entity);

        final Path runDir = storageService.getRunDir(runId);
        Files.createDirectories(runDir);

        final ObjectNode root = objectMapper.createObjectNode();
        final ObjectNode metrics = objectMapper.createObjectNode();

        // 1. Flaky test execution
        final ObjectNode exec1 = objectMapper.createObjectNode();
        exec1.put("id", "exec-flaky-order");
        exec1.put("testClass", TEST_CLASS);
        exec1.put("testMethod", TEST_METHOD);
        exec1.put("title", DATA_SET);
        exec1.put("location", LOCATION);
        exec1.put("browser", BROWSER);
        exec1.put("areaName", "Checkout");
        exec1.put("status", "passed-clean");
        exec1.put("totalStepsCount", 5);
        exec1.put("durationMs", 1500L);
        exec1.put("timestampMs", startTimeMs);

        // 2. Stable test execution (never failed before)
        final ObjectNode exec2 = objectMapper.createObjectNode();
        exec2.put("id", "exec-stable-order");
        exec2.put("testClass", "com.xceptance.aura.StableOrderTest");
        exec2.put("testMethod", "testLogin");
        exec2.put("title", DATA_SET);
        exec2.put("location", LOCATION);
        exec2.put("browser", BROWSER);
        exec2.put("areaName", "Auth");
        exec2.put("status", "passed-clean");
        exec2.put("totalStepsCount", 3);
        exec2.put("durationMs", 1000L);
        exec2.put("timestampMs", startTimeMs);

        metrics.set(exec1.get("id").asText(), exec1);
        metrics.set(exec2.get("id").asText(), exec2);
        root.set("executionMetrics", metrics);

        Files.writeString(runDir.resolve("run.json"), objectMapper.writeValueAsString(root));
    }

    private void cleanupRunOnDisk(final String runId)
    {
        try
        {
            final Path dir = storageService.getRunDir(runId);
            if (dir != null && Files.exists(dir))
            {
                Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
        }
        catch (final Exception ignored)
        {
        }
    }
}
