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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xceptance.aura.report.dto.RunReportDto;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.repository.TestRunRepository;

/**
 * Regression safety net for DEF-20261111-2315:
 * 1. Mid-run live execution merging without duplicate rows on the report page when simple class names vs FQCN are used.
 * 2. Suite tag detection from execution JSON files and resolution on active context cards for console runs.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@SpringBootTest
public class AuraMidRunDeduplicationAndSuiteTagTest
{
    private static final String DEDUP_RUN_ID = "run_midrun_dedup_test";
    private static final String CONSOLE_TAG_RUN_ID = "run_console_tag_test";

    @Autowired
    private LocalRunJsonStorageService storageService;

    @Autowired
    private AuraReportDataService reportDataService;

    @Autowired
    private TestRunRepository runRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    @AfterEach
    public void cleanup()
    {
        for (final String runId : List.of(DEDUP_RUN_ID, CONSOLE_TAG_RUN_ID))
        {
            reportDataService.evictRunReportCache(runId);
            runRepository.deleteById(runId);
            final Path runDir = storageService.getRunDir(runId);
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
    }

    @Test
    public void testMidRunDeduplicationOnReport() throws IOException
    {
        final Path runDir = storageService.getRunDir(DEDUP_RUN_ID);
        final Path execDir = runDir.resolve("Default").resolve("Aura_123_Test");
        Files.createDirectories(execDir);

        // 1. Simulate execution on disk with FQCN and generated console ID
        final ObjectNode diskNode = mapper.createObjectNode();
        diskNode.put("id", "console-execution-1");
        diskNode.put("testClass", "com.xceptance.neodymium.aura.sandbox.Aura_123_Test");
        diskNode.put("testMethod", "executeTest");
        diskNode.put("title", "Default");
        diskNode.put("browser", "Chrome");
        diskNode.put("location", "Default");
        diskNode.put("status", "running");
        diskNode.put("totalStepsCount", 1);
        diskNode.put("failedStepsCount", 0);
        diskNode.put("active", true);

        final Path execFile = execDir.resolve("console-execution-1.json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(execFile.toFile(), diskNode);

        final TestRunEntity entity = new TestRunEntity();
        entity.setId(DEDUP_RUN_ID);
        entity.setBatchName("Default");
        entity.setEnvironment("Default");
        entity.setStatus("IN_PROGRESS");
        runRepository.save(entity);

        // 2. Ingest live execution via AuraQueueService with simple class name and null method
        final Map<String, Object> livePayload = new HashMap<>();
        livePayload.put("id", "console-execution-1");
        livePayload.put("runId", DEDUP_RUN_ID);
        livePayload.put("testClass", "Aura_123_Test");
        livePayload.put("title", "Default");
        livePayload.put("browser", "Chrome");
        livePayload.put("location", "Default");
        livePayload.put("status", "passed");
        livePayload.put("totalStepsCount", 1);
        livePayload.put("failedStepsCount", 0);

        reportDataService.ingestExecution(DEDUP_RUN_ID, livePayload);

        // 3. Fetch run report while run is active
        final RunReportDto report = reportDataService.getRunReport(DEDUP_RUN_ID);

        Assertions.assertNotNull(report);
        Assertions.assertEquals(1, report.getExecutions().size(), "Active mid-run report must not contain duplicate test execution rows");
        Assertions.assertTrue(report.getExecutions().get(0).isFinished(), "Live completed status must take precedence over running disk execution");
    }

    @Test
    public void testConsoleRunSuiteTagExtractionAndResolution() throws IOException
    {
        final Path runDir = storageService.getRunDir(CONSOLE_TAG_RUN_ID);
        final Path execDir = runDir.resolve("Default").resolve("CheckoutTest");
        Files.createDirectories(execDir);

        // 1. Write execution JSON containing Suite tag
        final ObjectNode execNode = mapper.createObjectNode();
        execNode.put("id", "exec-checkout-1");
        execNode.put("testClass", "CheckoutTest");
        execNode.put("title", "Default");
        execNode.put("browser", "Chrome");
        execNode.put("location", "Default");
        execNode.put("status", "passed");
        execNode.put("totalStepsCount", 1);
        execNode.put("failedStepsCount", 0);

        final ObjectNode tagsNode = mapper.createObjectNode();
        final ArrayNode suiteArray = mapper.createArrayNode();
        suiteArray.add("CheckoutSuite");
        tagsNode.set("Suite", suiteArray);
        execNode.set("tags", tagsNode);

        final Path execFile = execDir.resolve("exec-checkout-1.json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(execFile.toFile(), execNode);

        // 2. Build run JSON content
        storageService.buildRunJsonContent(runDir.toFile(), CONSOLE_TAG_RUN_ID, true);

        // 3. Verify batch.json was generated and contains Suite name and tag
        final Path batchJsonFile = runDir.resolve("batch.json");
        Assertions.assertTrue(Files.exists(batchJsonFile));
        final JsonNode batchJson = mapper.readTree(batchJsonFile.toFile());
        Assertions.assertEquals("CheckoutSuite", batchJson.path("name").asText());
        Assertions.assertTrue(batchJson.has("tags"));

        // 4. Verify run.json contains tags.Suite
        final Path runJsonFile = runDir.resolve("run.json");
        Assertions.assertTrue(Files.exists(runJsonFile));
        final JsonNode runJson = mapper.readTree(runJsonFile.toFile());
        Assertions.assertTrue(runJson.has("tags"));
        Assertions.assertEquals("CheckoutSuite", runJson.path("tags").path("Suite").asText());

        // 5. Verify reportDataService.getRunTagsMap reads from disk
        final Map<String, List<String>> runTags = reportDataService.getRunTagsMap(CONSOLE_TAG_RUN_ID);
        Assertions.assertTrue(runTags.containsKey("suite"));
        Assertions.assertTrue(runTags.get("suite").contains("CheckoutSuite"));

        // 6. Verify resolveEffectiveBatchName resolves to CheckoutSuite
        final String effectiveBatch = AuraReportDataService.resolveEffectiveBatchNameFromMap("", runTags);
        Assertions.assertEquals("CheckoutSuite", effectiveBatch);
    }
}
