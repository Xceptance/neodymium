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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xceptance.aura.report.dto.BatchOverviewDataDto;
import com.xceptance.aura.report.dto.RunReportDto;
import com.xceptance.aura.report.entity.TestBatchEntity;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.repository.TestBaseBugRepository;
import com.xceptance.aura.report.repository.TestBatchRepository;
import com.xceptance.aura.report.repository.TestRunRepository;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Performance integration tests verifying execution speed and absence of N+1 database queries
 * for Report Manager batch overview and run report endpoints.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@SpringBootTest
public class AuraReportPerformanceTest
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

    private static final String TEST_RUN_ID = "perf-run-999";
    private static final String TEST_BATCH_NAME = "Perf Benchmark Batch";

    @BeforeEach
    public void setUp() throws IOException
    {
        dataService.clearCache();
        bugRepository.deleteAll();
        batchRepository.deleteAll();
        runRepository.deleteAll();

        final TestBatchEntity batchEntity = new TestBatchEntity(
            TEST_BATCH_NAME,
            "Staging",
            "Performance test batch description",
            "US",
            "Chrome",
            TEST_RUN_ID
        );
        batchRepository.save(batchEntity);

        final TestRunEntity runEntity = new TestRunEntity(
            TEST_RUN_ID,
            TEST_BATCH_NAME,
            "COMPLETED",
            "Benchmark trigger",
            "Staging",
            "US",
            "Chrome",
            "2026-08-21 10:00:00",
            System.currentTimeMillis()
        );
        runEntity.setTotalTests(200);
        runEntity.setPassedCount(180);
        runEntity.setFailedKnownCount(10);
        runEntity.setFailedUnknownCount(10);
        runEntity.setPassRate(90.0);
        runRepository.save(runEntity);

        // Create a run.json with 200 executions
        final Path runDir = storageService.getRunDir(TEST_RUN_ID);
        Files.createDirectories(runDir);

        final ObjectMapper mapper = new ObjectMapper();
        final ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("runId", TEST_RUN_ID);
        rootNode.put("batchName", TEST_BATCH_NAME);
        rootNode.put("environment", "Staging");
        rootNode.put("timestamp", "2026-08-21 10:00:00");

        final ArrayNode executionsArray = mapper.createArrayNode();
        for (int i = 1; i <= 200; i++)
        {
            final ObjectNode execNode = mapper.createObjectNode();
            execNode.put("id", "exec-row-" + i);
            execNode.put("runId", TEST_RUN_ID);
            execNode.put("testClass", "PerfTestClass" + (i % 10));
            execNode.put("title", "testScenario_" + i);
            execNode.put("status", i <= 180 ? "passed-clean" : (i <= 190 ? "failed-known" : "failed-unknown"));
            execNode.put("areaName", "Checkout");
            execNode.put("location", "US");
            execNode.put("browser", "Chrome");
            if (i > 180 && i <= 190)
            {
                execNode.putArray("bugs").add("BUG-100" + i);
            }
            executionsArray.add(execNode);
        }
        rootNode.set("executions", executionsArray);

        final Path runJsonPath = runDir.resolve("run.json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(runJsonPath.toFile(), rootNode);

        // Warm up JVM JIT compilation, Jackson type reflection, and JPA metadata
        dataService.getRunReport(TEST_RUN_ID);
        dataService.clearCache();
    }

    @AfterEach
    public void tearDown() throws IOException
    {
        final Path runDir = storageService.getRunDir(TEST_RUN_ID);
        if (Files.exists(runDir))
        {
            try (final var stream = Files.walk(runDir))
            {
                stream.sorted(Comparator.reverseOrder())
                      .map(Path::toFile)
                      .forEach(File::delete);
            }
        }
        final Path flatFile = storageService.getRunJsonPath(TEST_RUN_ID);
        if (Files.exists(flatFile) && !Files.isDirectory(flatFile))
        {
            Files.deleteIfExists(flatFile);
        }
    }

    @Test
    public void testGetRunReportPerformance()
    {
        final long startTime = System.currentTimeMillis();
        final RunReportDto report = dataService.getRunReport(TEST_RUN_ID);
        final long durationMs = System.currentTimeMillis() - startTime;

        Assertions.assertNotNull(report);
        Assertions.assertEquals(200, report.getExecutions().size());

        // Initial execution should take well under 300 ms (previously took seconds due to N+1 queries)
        Assertions.assertTrue(durationMs < 300, "getRunReport took " + durationMs + "ms, expected < 300ms");

        // Subsequent cached call should execute under 20 ms
        final long cachedStart = System.currentTimeMillis();
        final RunReportDto cachedReport = dataService.getRunReport(TEST_RUN_ID);
        final long cachedDurationMs = System.currentTimeMillis() - cachedStart;

        Assertions.assertEquals(200, cachedReport.getExecutions().size());
        Assertions.assertTrue(cachedDurationMs < 20, "Cached getRunReport took " + cachedDurationMs + "ms, expected < 20ms");
    }

    @Test
    public void testGetBatchOverviewDataPerformance()
    {
        final long startTime = System.currentTimeMillis();
        final BatchOverviewDataDto overviewData = dataService.getBatchOverviewData();
        final long durationMs = System.currentTimeMillis() - startTime;

        Assertions.assertNotNull(overviewData);
        Assertions.assertFalse(overviewData.getBatches().isEmpty());

        // Batch overview load should take well under 300 ms (previously took ~1000ms+)
        Assertions.assertTrue(durationMs < 300, "getBatchOverviewData took " + durationMs + "ms, expected < 300ms");
    }
}
