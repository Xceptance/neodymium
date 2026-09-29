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
import com.xceptance.aura.report.dto.BatchSummaryDto;
import com.xceptance.aura.report.dto.RunReportDto;
import com.xceptance.aura.report.dto.TestExecutionDto;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies that active bugs count on batch cards in getAllBatches is calculated dynamically.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@SpringBootTest
public class AuraReportBatchActiveBugsTest
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

    private static final String TEST_RUN_ID = "run-batch-active-bugs-101";
    private static final String TEST_BATCH_NAME = "Bug Batch Dynamic";

    @BeforeEach
    public void setUp() throws IOException
    {
        bugRepository.deleteAll();
        batchRepository.deleteAll();
        runRepository.deleteAll();

        final TestBatchEntity batchEntity = new TestBatchEntity(
            TEST_BATCH_NAME,
            "Staging",
            "Batch active bug test description",
            "US",
            "Chrome",
            TEST_RUN_ID
        );
        batchRepository.save(batchEntity);

        final TestRunEntity runEntity = new TestRunEntity(
            TEST_RUN_ID,
            TEST_BATCH_NAME,
            "COMPLETED",
            "Regression trigger",
            "Staging",
            "US",
            "Chrome",
            "2026-08-20 14:00:00",
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

        final Path runDir = storageService.getRunDir(TEST_RUN_ID);
        final Path execDir = runDir.resolve("Search").resolve("ProductSearchTest");
        Files.createDirectories(execDir);
        final Path execFile = execDir.resolve("exec-bug-1.json");

        final ObjectMapper mapper = new ObjectMapper();
        final ObjectNode execNode = mapper.createObjectNode();
        execNode.put("id", "row-bug-test-1");
        execNode.put("runId", TEST_RUN_ID);
        execNode.put("testClass", "ProductSearchTest");
        execNode.put("title", "testKeywordSearch");
        execNode.put("status", "failed-known");
        execNode.put("areaName", "Search");
        execNode.put("location", "US");
        execNode.put("browser", "Chrome");
        execNode.put("failure", "ProductNotFoundException");
        execNode.putArray("bugs").add("BUG-5001");

        mapper.writerWithDefaultPrettyPrinter().writeValue(execFile.toFile(), execNode);

        final TestExecutionDto exec = new TestExecutionDto(
            "row-bug-test-1",
            TEST_RUN_ID,
            "com.xceptance.neodymium.test.ProductSearchTest",
            "testKeywordSearch",
            "com.xceptance.neodymium.test.ProductSearchTest testKeywordSearch",
            "1.2s",
            "2026-08-20 14:00:00",
            "failed-known",
            "Java",
            "US",
            "Chrome",
            "ProductNotFoundException",
            List.of("BUG-5001"),
            null,
            "Search",
            List.of(),
            null,
            null,
            null,
            null
        );

        final RunReportDto report = new RunReportDto(
            TEST_RUN_ID,
            TEST_BATCH_NAME,
            "2026-08-20 14:00:00",
            "1.2s",
            1,
            0,
            0,
            1,
            0,
            0,
            List.of(exec)
        );

        dataService.saveRunReportToDisk(TEST_RUN_ID, report);
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
    public void testGetAllBatchesCalculatesActiveBugsDynamically()
    {
        final List<BatchSummaryDto> batches = dataService.getAllBatches();
        Assertions.assertFalse(batches.isEmpty());

        final Optional<BatchSummaryDto> targetBatchOpt = batches.stream()
            .filter(b -> TEST_BATCH_NAME.equalsIgnoreCase(b.getName()))
            .findFirst();

        Assertions.assertTrue(targetBatchOpt.isPresent(), "Target batch should exist in getAllBatches()");
        final BatchSummaryDto batch = targetBatchOpt.get();

        Assertions.assertEquals("1 Active Bug", batch.getActiveBugs());
        Assertions.assertEquals("1 Active Bug", batch.getKnownBugs());
    }
}
