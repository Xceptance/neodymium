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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xceptance.aura.report.dto.RunReportDto;
import com.xceptance.aura.report.dto.TestExecutionDto;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.repository.TestRunRepository;

/**
 * Verifies that test executions with status "finished" and zero failed steps
 * are correctly recognized as passed across DTOs and report generation.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@SpringBootTest
public class AuraReportFinishedStatusMappingTest
{
    private static final String TEST_RUN_ID = "run_test_finished_status";

    @Autowired
    private LocalRunJsonStorageService storageService;

    @Autowired
    private AuraReportDataService reportDataService;

    @Autowired
    private RunStorageSyncService syncService;

    @Autowired
    private TestRunRepository runRepository;

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
    public void testExecutionDtoDisplayStatusKeyForFinished() throws IOException
    {
        final ObjectNode cleanNode = mapper.createObjectNode();
        cleanNode.put("status", "finished");
        cleanNode.put("failedStepsCount", 0);
        cleanNode.put("failure", "NONE");
        final TestExecutionDto cleanDto = mapper.treeToValue(cleanNode, TestExecutionDto.class);
        Assertions.assertEquals("PASSED", cleanDto.getDisplayStatusKey());

        final ObjectNode bugNode = mapper.createObjectNode();
        bugNode.put("status", "finished");
        bugNode.put("failedStepsCount", 0);
        bugNode.put("failure", "NONE");
        final ArrayNode bugsArray = mapper.createArrayNode();
        bugsArray.add("BUG-123");
        bugNode.set("bugs", bugsArray);
        final TestExecutionDto bugDto = mapper.treeToValue(bugNode, TestExecutionDto.class);
        Assertions.assertEquals("SUCCEEDED_FIXED", bugDto.getDisplayStatusKey());

        final ObjectNode failedNode = mapper.createObjectNode();
        failedNode.put("status", "finished");
        failedNode.put("failedStepsCount", 1);
        final TestExecutionDto failedDto = mapper.treeToValue(failedNode, TestExecutionDto.class);
        Assertions.assertEquals("FAILED_UNKNOWN", failedDto.getDisplayStatusKey());

        final ObjectNode failedWithBugNode = mapper.createObjectNode();
        failedWithBugNode.put("status", "finished");
        failedWithBugNode.put("failedStepsCount", 1);
        final ArrayNode failedBugs = mapper.createArrayNode();
        failedBugs.add("BUG-999");
        failedWithBugNode.set("bugs", failedBugs);
        final TestExecutionDto failedBugDto = mapper.treeToValue(failedWithBugNode, TestExecutionDto.class);
        Assertions.assertEquals("FAILED_KNOWN", failedBugDto.getDisplayStatusKey());

        final ObjectNode failureNode = mapper.createObjectNode();
        failureNode.put("status", "finished");
        failureNode.put("failedStepsCount", 0);
        failureNode.put("failure", "ERROR");
        final TestExecutionDto failureDto = mapper.treeToValue(failureNode, TestExecutionDto.class);
        Assertions.assertEquals("FAILED_UNKNOWN", failureDto.getDisplayStatusKey());
    }

    @Test
    public void testRunReportStorageAndReportGenerationWithFinishedStatus() throws IOException
    {
        final Path runDir = storageService.getRunDir(TEST_RUN_ID);
        final Path classDir = runDir.resolve("com.xceptance.neodymium.tests.CheckoutFlowTest");
        Files.createDirectories(classDir);

        final ObjectNode exec = mapper.createObjectNode();
        exec.put("id", "console-execution-1");
        exec.put("testClass", "CheckoutFlowTest");
        exec.put("testMethod", "@Regression");
        exec.put("title", "Default");
        exec.put("status", "finished");
        exec.put("failedStepsCount", 0);
        exec.put("totalStepsCount", 2);

        final ObjectNode blocks = mapper.createObjectNode();
        final ArrayNode steps = mapper.createArrayNode();
        final ObjectNode step1 = mapper.createObjectNode();
        step1.put("instruction", "Click button");
        step1.put("status", "passed");
        steps.add(step1);
        blocks.set("steps", steps);
        exec.set("blocks", blocks);

        final Path execFile = classDir.resolve("console-execution-1.json");
        Files.writeString(execFile, mapper.writeValueAsString(exec));

        storageService.buildRunJsonContent(runDir.toFile(), TEST_RUN_ID, true);

        final RunReportDto report = reportDataService.getRunReport(TEST_RUN_ID);
        Assertions.assertNotNull(report);
        Assertions.assertEquals(1, report.getTotalCount());
        Assertions.assertEquals(1, report.getPassCount());
        Assertions.assertEquals(0, report.getUnknownCount());
        Assertions.assertEquals(0, report.getIgnoredCount());

        final TestExecutionDto execDto = report.getExecutions().get(0);
        Assertions.assertEquals("passed-clean", execDto.getStatus());
        Assertions.assertEquals("PASSED", execDto.getDisplayStatusKey());
    }

    @Test
    public void testRunReportStorageAndReportGenerationWithFinishedStatus_Failing() throws IOException
    {
        final Path runDir = storageService.getRunDir(TEST_RUN_ID);
        final Path classDir = runDir.resolve("com.xceptance.neodymium.tests.FailedCheckoutFlowTest");
        Files.createDirectories(classDir);

        final ObjectNode exec = mapper.createObjectNode();
        exec.put("id", "console-execution-failed-1");
        exec.put("testClass", "FailedCheckoutFlowTest");
        exec.put("testMethod", "@Regression");
        exec.put("title", "FailingSimulation");
        exec.put("status", "finished");
        exec.put("failedStepsCount", 1);
        exec.put("totalStepsCount", 2);

        final ObjectNode blocks = mapper.createObjectNode();
        final ArrayNode steps = mapper.createArrayNode();
        final ObjectNode step1 = mapper.createObjectNode();
        step1.put("instruction", "Assert title");
        step1.put("status", "failed");
        steps.add(step1);
        blocks.set("steps", steps);
        exec.set("blocks", blocks);

        final Path execFile = classDir.resolve("console-execution-failed-1.json");
        Files.writeString(execFile, mapper.writeValueAsString(exec));

        storageService.buildRunJsonContent(runDir.toFile(), TEST_RUN_ID, true);

        final RunReportDto report = reportDataService.getRunReport(TEST_RUN_ID);
        Assertions.assertNotNull(report);
        Assertions.assertEquals(1, report.getTotalCount());
        Assertions.assertEquals(0, report.getPassCount(), "Failed finished execution must not count as passed");
        Assertions.assertEquals(1, report.getUnknownCount(), "Failed finished execution must count as unknown failure");
        Assertions.assertEquals(0, report.getIgnoredCount(), "Failed finished execution must not be marked as ignored");

        final TestExecutionDto execDto = report.getExecutions().get(0);
        Assertions.assertEquals("failed-unknown", execDto.getStatus());
        Assertions.assertEquals("FAILED_UNKNOWN", execDto.getDisplayStatusKey());
    }

    @Test
    public void testExecutionDtoDisplayStatusKeyForRunningAndNull() throws Exception
    {
        final ObjectNode runningNode = mapper.createObjectNode();
        runningNode.put("status", "running");
        final TestExecutionDto runningDto = mapper.treeToValue(runningNode, TestExecutionDto.class);
        Assertions.assertEquals("RUNNING", runningDto.getDisplayStatusKey());

        final ObjectNode nullNode = mapper.createObjectNode();
        final TestExecutionDto nullDto = mapper.treeToValue(nullNode, TestExecutionDto.class);
        Assertions.assertEquals("RUNNING", nullDto.getDisplayStatusKey(), "DTO deserialized without status field defaults to running");

        final TestExecutionDto explicitNullDto = new TestExecutionDto();
        explicitNullDto.setStatus(null);
        Assertions.assertEquals("RUNNING", explicitNullDto.getDisplayStatusKey(), "Explicit null status must evaluate to RUNNING");

        final ObjectNode inProgNode = mapper.createObjectNode();
        inProgNode.put("status", "in_progress");
        final TestExecutionDto inProgDto = mapper.treeToValue(inProgNode, TestExecutionDto.class);
        Assertions.assertEquals("RUNNING", inProgDto.getDisplayStatusKey());

        final ObjectNode knownNode = mapper.createObjectNode();
        knownNode.put("status", "failed-known");
        final TestExecutionDto knownDto = mapper.treeToValue(knownNode, TestExecutionDto.class);
        Assertions.assertEquals("FAILED_KNOWN", knownDto.getDisplayStatusKey());

        final ObjectNode fixedNode = mapper.createObjectNode();
        fixedNode.put("status", "succeeded-fixed");
        final TestExecutionDto fixedDto = mapper.treeToValue(fixedNode, TestExecutionDto.class);
        Assertions.assertEquals("SUCCEEDED_FIXED", fixedDto.getDisplayStatusKey());

        final ObjectNode skippedNode = mapper.createObjectNode();
        skippedNode.put("status", "skipped");
        final TestExecutionDto skippedDto = mapper.treeToValue(skippedNode, TestExecutionDto.class);
        Assertions.assertEquals("SKIPPED", skippedDto.getDisplayStatusKey());

        final ObjectNode ignoredNode = mapper.createObjectNode();
        ignoredNode.put("status", "ignored");
        final TestExecutionDto ignoredDto = mapper.treeToValue(ignoredNode, TestExecutionDto.class);
        Assertions.assertEquals("SKIPPED", ignoredDto.getDisplayStatusKey());
    }

    @Test
    public void testComputeEffectiveStatusInterruptedRunsAndUnlink()
    {
        // Interrupted run with failed steps -> failed-unknown (does not swallow failure)
        Assertions.assertEquals("failed-unknown",
            AuraReportDataService.computeEffectiveStatus("running", 5, 2, false, null, false));

        // Interrupted run with failed steps and bugs -> failed-known
        Assertions.assertEquals("failed-known",
            AuraReportDataService.computeEffectiveStatus("running", 5, 2, true, null, false));

        // Interrupted clean run with 0 failed steps -> ignored
        Assertions.assertEquals("ignored",
            AuraReportDataService.computeEffectiveStatus("running", 5, 0, false, null, false));

        // Interrupted run with 0 total steps -> failed-unknown
        Assertions.assertEquals("failed-unknown",
            AuraReportDataService.computeEffectiveStatus("running", 0, 0, false, null, false));

        // Active run in progress -> running
        Assertions.assertEquals("running",
            AuraReportDataService.computeEffectiveStatus("running", 5, 0, false, null, true));

        // Null status on active run -> running
        Assertions.assertEquals("running",
            AuraReportDataService.computeEffectiveStatus(null, 0, 0, false, null, true));

        // Null status on inactive run -> failed-unknown
        Assertions.assertEquals("failed-unknown",
            AuraReportDataService.computeEffectiveStatus(null, 0, 0, false, null, false));

        // Unlinking bug on failed-known execution -> flips to failed-unknown
        Assertions.assertEquals("failed-unknown",
            AuraReportDataService.computeEffectiveStatus("failed-known", 5, 1, false, null, false));

        // Unlinking bug on succeeded-fixed execution -> flips to passed-clean
        Assertions.assertEquals("passed-clean",
            AuraReportDataService.computeEffectiveStatus("succeeded-fixed", 5, 0, false, null, false));
    }

    @Test
    public void testFinishedStatusWithNoneAndNullFailureStrings()
    {
        // 0 steps, no errors, failure = "NONE" -> passed-clean
        Assertions.assertEquals("passed-clean",
            AuraReportDataService.computeEffectiveStatus("finished", 0, 0, false, "NONE", false));

        // 0 steps, no errors, failure = null -> passed-clean
        Assertions.assertEquals("passed-clean",
            AuraReportDataService.computeEffectiveStatus("finished", 0, 0, false, null, false));

        // 0 steps, no errors, failure = blank -> passed-clean
        Assertions.assertEquals("passed-clean",
            AuraReportDataService.computeEffectiveStatus("finished", 0, 0, false, "   ", false));

        // 0 steps, with real error message -> failed-unknown
        Assertions.assertEquals("failed-unknown",
            AuraReportDataService.computeEffectiveStatus("finished", 0, 0, false, "AssertionError", false));
    }

    @Test
    public void testActiveRunIngestExecutionDoesNotInflateUnknownFailuresForRunningExecutions()
    {
        final TestRunEntity activeRun = new TestRunEntity(
            TEST_RUN_ID, "BatchA", "IN_PROGRESS", "CLI", "DEV", "US", "Chrome", "12:00:00", System.currentTimeMillis());
        runRepository.save(activeRun);

        final Map<String, Object> payload = Map.of(
            "id", "running-exec-1",
            "testClass", "CheckoutFlowTest",
            "testMethod", "@Regression",
            "title", "Default",
            "status", "running",
            "totalStepsCount", 1,
            "failedStepsCount", 0
        );

        reportDataService.ingestExecution(TEST_RUN_ID, payload);

        final TestRunEntity updatedRun = runRepository.findById(TEST_RUN_ID).orElseThrow();
        Assertions.assertEquals(0, updatedRun.getFailedUnknownCount(),
            "In-flight running execution in an active run must NOT be counted as unknown failure");
        Assertions.assertEquals(0, updatedRun.getPassedCount(),
            "In-flight running execution must not inflate pass count");
    }

    @Test
    public void testActiveRunStorageSyncDoesNotInflateUnknownFailuresForRunningExecutions() throws IOException
    {
        final TestRunEntity activeRun = new TestRunEntity(
            TEST_RUN_ID, "BatchA", "IN_PROGRESS", "CLI", "DEV", "US", "Chrome", "12:00:00", System.currentTimeMillis());
        runRepository.save(activeRun);

        final Path runDir = storageService.getRunDir(TEST_RUN_ID);
        final Path classDir = runDir.resolve("com.xceptance.neodymium.tests.CheckoutFlowTest");
        Files.createDirectories(classDir);

        final ObjectNode exec = mapper.createObjectNode();
        exec.put("id", "console-execution-running-1");
        exec.put("testClass", "CheckoutFlowTest");
        exec.put("testMethod", "@Regression");
        exec.put("title", "Default");
        exec.put("status", "running");
        exec.put("failedStepsCount", 0);
        exec.put("totalStepsCount", 2);

        final Path execFile = classDir.resolve("console-execution-running-1.json");
        Files.writeString(execFile, mapper.writeValueAsString(exec));

        syncService.importOrUpdateRunReport(TEST_RUN_ID);

        final TestRunEntity syncedRun = runRepository.findById(TEST_RUN_ID).orElseThrow();
        Assertions.assertEquals(0, syncedRun.getFailedUnknownCount(),
            "In-flight running execution synced from disk during active run must NOT count as unknown failure");
    }
}
