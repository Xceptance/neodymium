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

    private final ObjectMapper mapper = new ObjectMapper();

    @AfterEach
    public void cleanup()
    {
        reportDataService.evictRunReportCache(TEST_RUN_ID);
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
}
