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
import java.nio.file.Paths;
import java.util.Comparator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xceptance.aura.report.dto.RunReportDto;

/**
 * Tests that early test failures (0 steps executed due to configuration or setup issues)
 * are correctly assigned a failed status in run reports and run.json summaries.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@SpringBootTest
public class EarlyFailureReportTest
{
    @Autowired
    private RunStorageSyncService syncService;

    @Autowired
    private AuraReportDataService dataService;

    @Autowired
    private LocalRunJsonStorageService storageService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    public void tearDown() throws IOException
    {
        final Path runsDir = Paths.get("storage/runs");
        if (Files.exists(runsDir))
        {
            Files.list(runsDir)
                .filter(p -> p.getFileName().toString().startsWith("early-fail-run-"))
                .forEach(runDir -> {
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
                });
        }
    }

    @Test
    public void testEarlyFailureZeroStepsReportedAsFailed() throws Exception
    {
        final String runId = "early-fail-run-888";
        final Path runDir = Paths.get("storage", "runs", runId);
        final Path classDir = runDir.resolve("General").resolve("ConfigErrorTestCase");
        Files.createDirectories(classDir);

        // Execution JSON representing a test that failed early during setup before step execution
        final String earlyFailureExecJson = "{"
            + "\"id\":\"exec-early-fail-1\","
            + "\"testClass\":\"ConfigErrorTestCase\","
            + "\"testMethod\":\"testConfiguration\","
            + "\"title\":\"Initial Setup Failure\","
            + "\"status\":\"failed\","
            + "\"totalStepsCount\":0,"
            + "\"failedStepsCount\":0,"
            + "\"error\":\"Missing GEMINI_API_KEY configuration property\","
            + "\"browser\":\"Chrome\""
            + "}";

        Files.writeString(classDir.resolve("console-execution-1.json"), earlyFailureExecJson);

        final boolean imported = syncService.importOrUpdateRunReport(runId);
        Assertions.assertTrue(imported, "Run report should be imported from disk executions");

        final RunReportDto report = dataService.getRunReport(runId);
        Assertions.assertNotNull(report, "Run report should be retrievable");
        Assertions.assertEquals(1, report.getTotalCount(), "Total count should be 1");
        Assertions.assertEquals(1, report.getUnknownCount(), "Unknown failed count should be 1");
        Assertions.assertEquals(0, report.getPassCount(), "Pass count should be 0");

        final Path runJsonPath = runDir.resolve("run.json");
        Assertions.assertTrue(Files.exists(runJsonPath), "run.json file should exist on disk");

        final JsonNode rootNode = objectMapper.readTree(runJsonPath.toFile());
        final JsonNode summaryNode = rootNode.has("summary") ? rootNode.path("summary") : rootNode.path("summaryCounts");
        Assertions.assertEquals(1, summaryNode.path("total").asInt(), "Summary total should be 1");
        Assertions.assertEquals(1, summaryNode.path("unknown").asInt(), "Summary unknown failed should be 1");
        Assertions.assertEquals(0, summaryNode.path("pass").asInt(), "Summary pass should be 0");
    }

    @Test
    public void testMultiDatasetEarlyFailureReporting() throws Exception
    {
        final String runId = "early-fail-run-889";
        final Path runDir = Paths.get("storage", "runs", runId);
        final Path classDir = runDir.resolve("General").resolve("MultiDatasetTestCase");
        Files.createDirectories(classDir);

        // Dataset 1 passed execution
        final String dataset1ExecJson = "{"
            + "\"id\":\"exec-multi-ds-1\","
            + "\"testClass\":\"MultiDatasetTestCase\","
            + "\"testMethod\":\"testWikipediaSearch\","
            + "\"title\":\"Dataset 1 - Pass\","
            + "\"status\":\"passed\","
            + "\"totalStepsCount\":2,"
            + "\"failedStepsCount\":0,"
            + "\"passedStepsCount\":2,"
            + "\"browser\":\"Chrome\""
            + "}";

        // Dataset 2 failed early execution snapshot (0 steps executed)
        final String dataset2ExecJson = "{"
            + "\"id\":\"exec-multi-ds-2\","
            + "\"testClass\":\"MultiDatasetTestCase\","
            + "\"testMethod\":\"testWikipediaSearch\","
            + "\"title\":\"Dataset 2 - Early Failure\","
            + "\"status\":\"failed\","
            + "\"totalStepsCount\":0,"
            + "\"failedStepsCount\":0,"
            + "\"browser\":\"Chrome\""
            + "}";

        Files.writeString(classDir.resolve("console-execution-1.json"), dataset1ExecJson);
        Files.writeString(classDir.resolve("console-execution-2.json"), dataset2ExecJson);

        final boolean imported = syncService.importOrUpdateRunReport(runId);
        Assertions.assertTrue(imported, "Run report should be imported from disk executions");

        final RunReportDto report = dataService.getRunReport(runId);
        Assertions.assertNotNull(report, "Run report should be retrievable");
        Assertions.assertEquals(2, report.getTotalCount(), "Total count should be 2 datasets");
        Assertions.assertEquals(1, report.getPassCount(), "Pass count should be 1");
        Assertions.assertEquals(1, report.getUnknownCount(), "Unknown failed count should be 1");

        final Path runJsonPath = runDir.resolve("run.json");
        Assertions.assertTrue(Files.exists(runJsonPath), "run.json file should exist on disk");

        final JsonNode rootNode = objectMapper.readTree(runJsonPath.toFile());
        final JsonNode summaryNode = rootNode.has("summary") ? rootNode.path("summary") : rootNode.path("summaryCounts");
        Assertions.assertEquals(2, summaryNode.path("total").asInt(), "Summary total should be 2");
        Assertions.assertEquals(1, summaryNode.path("pass").asInt(), "Summary pass count should be 1");
        Assertions.assertEquals(1, summaryNode.path("unknown").asInt(), "Summary unknown failed should be 1");
    }
}
