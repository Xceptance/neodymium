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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xceptance.neodymium.ai.console.InteractiveConsoleEngine;
import com.xceptance.neodymium.aura.AuraInteractiveService;
import com.xceptance.aura.report.dto.AreaSummaryDto;
import com.xceptance.aura.report.dto.RunReportDto;
import com.xceptance.aura.report.dto.TestClassSummaryDto;
import com.xceptance.aura.report.dto.TestExecutionDto;
import com.xceptance.aura.report.entity.TestBaseBugEntity;
import com.xceptance.aura.report.entity.TestBaseVariationEntity;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.entity.TestRunTagEntity;
import com.xceptance.aura.report.repository.TestBaseBugRepository;
import com.xceptance.aura.report.repository.TestBaseVariationRepository;
import com.xceptance.aura.report.repository.TestRunRepository;

/**
 * Tests report storage synchronization and dynamic run.json generation on report read.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@SpringBootTest
public class RunStorageSyncServiceTest
{
    private static final String TEST_RUN_ID = "run-gen-test-999";

    @Autowired
    private RunStorageSyncService syncService;

    @Autowired
    private LocalRunJsonStorageService storageService;

    @Autowired
    private TestRunRepository runRepository;

    @Autowired
    private TestBaseBugRepository bugRepository;

    @Autowired
    private AuraReportDataService dataService;

    @Autowired
    private TestBaseVariationRepository variationRepository;

    @AfterEach
    public void cleanup() throws IOException
    {
        final Path testRunPath = Paths.get("storage", "runs", TEST_RUN_ID);
        if (Files.exists(testRunPath))
        {
            try (var stream = Files.walk(testRunPath))
            {
                stream.sorted(Comparator.reverseOrder())
                      .map(Path::toFile)
                      .forEach(File::delete);
            }
        }
    }

    @Test
    public void testSyncLocalRunStorageWithFreshRun() throws IOException
    {
        final String freshRunId = "run-sync-fresh-test";
        final Path runDir = Paths.get("storage", "runs", freshRunId);
        final Path execDir = runDir.resolve("Checkout").resolve("CheckoutProcessTest");

        try
        {
            Files.createDirectories(execDir);

            final String execJson = """
                {
                    "id": "exec-fresh-1",
                    "title": "CheckoutProcessTest [US Chrome]",
                    "testClass": "CheckoutProcessTest",
                    "status": "passed-clean",
                    "location": "US",
                    "browser": "Chrome"
                }
                """;
            Files.writeString(execDir.resolve("exec-1.json"), execJson);

            final int syncedCount = syncService.syncLocalRunStorage();
            Assertions.assertTrue(syncedCount > 0, "Expected at least 1 new run report synced into DB");

            final Optional<TestRunEntity> freshRunOpt = runRepository.findById(freshRunId);
            Assertions.assertTrue(freshRunOpt.isPresent(), "Fresh run should be synced into database");
            Assertions.assertTrue(Files.exists(runDir.resolve("run.json")), "run.json should be generated for fresh run");
        }
        finally
        {
            deleteRecursively(runDir);
            runRepository.findById(freshRunId).ifPresent(runRepository::delete);
        }
    }

    @Test
    public void testSyncLocalRunStorageSkipsRunWithRunJsonAndDbEntry() throws IOException
    {
        final String runId = "run-sync-skip-test";
        final Path runDir = Paths.get("storage", "runs", runId);
        final Path execDir = runDir.resolve("Checkout").resolve("CheckoutProcessTest");

        try
        {
            Files.createDirectories(execDir);

            final String execJson = """
                {
                    "id": "exec-skip-1",
                    "title": "CheckoutProcessTest [US Chrome]",
                    "testClass": "CheckoutProcessTest",
                    "status": "passed-clean",
                    "location": "US",
                    "browser": "Chrome"
                }
                """;
            Files.writeString(execDir.resolve("exec-1.json"), execJson);

            Assertions.assertTrue(syncService.importOrUpdateRunReport(runId), "import should populate DB and run.json");

            final Path runJsonPath = runDir.resolve("run.json");
            Assertions.assertTrue(Files.exists(runJsonPath), "run.json should exist after import");

            Files.writeString(runJsonPath, "{\"marker\":\"preserve-me\"}");

            syncService.syncLocalRunStorage();

            final String diskContent = Files.readString(runJsonPath);
            Assertions.assertTrue(diskContent.contains("preserve-me"),
                "run.json must not be regenerated for an already initialized run");
        }
        finally
        {
            deleteRecursively(runDir);
            runRepository.findById(runId).ifPresent(runRepository::delete);
        }
    }

    @Test
    public void testSyncLocalRunStorageInitializesRunMissingRunJsonButInDb() throws IOException
    {
        final String runId = "run-sync-missing-json-test";
        final Path runDir = Paths.get("storage", "runs", runId);
        final Path execDir = runDir.resolve("Checkout").resolve("CheckoutProcessTest");

        try
        {
            Files.createDirectories(execDir);

            final String execJson = """
                {
                    "id": "exec-missing-json-1",
                    "title": "CheckoutProcessTest [US Chrome]",
                    "testClass": "CheckoutProcessTest",
                    "status": "passed-clean",
                    "location": "US",
                    "browser": "Chrome"
                }
                """;
            Files.writeString(execDir.resolve("exec-1.json"), execJson);

            Assertions.assertTrue(syncService.importOrUpdateRunReport(runId), "import should populate DB and run.json");

            final Path runJsonPath = runDir.resolve("run.json");
            Assertions.assertTrue(Files.exists(runJsonPath), "run.json should exist after import");
            Files.delete(runJsonPath);
            Assertions.assertFalse(Files.exists(runJsonPath), "run.json should be deleted for this test");

            syncService.syncLocalRunStorage();

            Assertions.assertTrue(Files.exists(runJsonPath), "run.json should be regenerated for run missing it on disk");

            final Optional<TestRunEntity> runOpt = runRepository.findById(runId);
            Assertions.assertTrue(runOpt.isPresent(), "Run should remain in database");
        }
        finally
        {
            deleteRecursively(runDir);
            runRepository.findById(runId).ifPresent(runRepository::delete);
        }
    }

    @Test
    public void testRunJsonGeneratedOnRead() throws IOException
    {
        final Path runDir = Paths.get("storage", "runs", TEST_RUN_ID);
        final Path execDir = runDir.resolve("Checkout").resolve("CheckoutProcessTest");
        Files.createDirectories(execDir);

        final String execJson = """
            {
                "id": "exec-999-1",
                "title": "CheckoutProcessTest [US Chrome]",
                "testClass": "CheckoutProcessTest",
                "status": "passed-clean",
                "location": "US",
                "browser": "Chrome"
            }
            """;
        Files.writeString(execDir.resolve("exec-1.json"), execJson);

        final Path runJsonPath = runDir.resolve("run.json");
        Assertions.assertFalse(Files.exists(runJsonPath), "run.json should not exist prior to read");

        final Optional<String> runJsonOpt = storageService.readRunJson(TEST_RUN_ID);
        Assertions.assertTrue(runJsonOpt.isPresent(), "readRunJson should dynamically assemble and return run JSON");
        Assertions.assertTrue(Files.exists(runJsonPath), "run.json file SHOULD be created on disk when missing");
        Assertions.assertTrue(runJsonOpt.get().contains("executionMetrics"), "run.json should contain executionMetrics");
        Assertions.assertTrue(runJsonOpt.get().contains("exec-999-1"));
    }

    @Test
    public void testUnassignedCategoryDefaultsToBrowsingWithoutMovingFile() throws IOException
    {
        final String unassignedRunId = "run-unassigned-test-888";
        final Path runDir = Paths.get("storage", "runs", unassignedRunId);
        Files.createDirectories(runDir);

        final Path flatExecFile = runDir.resolve("unassigned-exec.json");
        final String flatExecJson = """
            {
                "id": "exec-888-1",
                "title": "UnassignedTest [US Chrome]",
                "testClass": "UnassignedTest",
                "status": "passed-clean",
                "location": "US",
                "browser": "Chrome"
            }
            """;
        Files.writeString(flatExecFile, flatExecJson);

        try
        {
            final Optional<String> runJsonOpt = storageService.readRunJson(unassignedRunId);
            Assertions.assertTrue(runJsonOpt.isPresent(), "readRunJson should return generated run.json");
            Assertions.assertTrue(runJsonOpt.get().contains("executionMetrics"), "Expected run.json to contain executionMetrics");

            Assertions.assertTrue(Files.exists(flatExecFile), "Flat execution JSON file must stay at the run root");
            Assertions.assertFalse(Files.exists(runDir.resolve("Browsing (default)")), "No Browsing (default) folder should be created on disk");

            Assertions.assertEquals("Default", findAreaForClass(dataService.getRunReport(unassignedRunId), "UnassignedTest"),
                "Flat execution without a category should be grouped under the default category in the run report");

            final JsonNode enrichedExec = new ObjectMapper().readTree(flatExecFile.toFile());
            Assertions.assertEquals("Default", enrichedExec.path("areaName").asText(),
                "Enriched execution JSON should carry the default category while staying in place");
        }
        finally
        {
            deleteRecursively(runDir);
        }
    }

    @Test
    public void testTwoLevelFolderStructureDefaultsToBrowsingCategory() throws IOException
    {
        final String twoLevelRunId = "run-two-level-777";
        final Path runDir = Paths.get("storage", "runs", twoLevelRunId);
        final Path classDir = runDir.resolve("GoogleTest");
        Files.createDirectories(classDir);

        final Path execFile = classDir.resolve("console-execution-1.json");
        final String execJson = """
            {
                "runId": "run_777",
                "status": "failed",
                "testName": "Google Test",
                "testFile": "com.xceptance.neodymium.test.examples.GoogleTest#executeGoogleTest",
                "browser": "chrome"
            }
            """;
        Files.writeString(execFile, execJson);

        try
        {
            final Optional<String> runJsonOpt = storageService.readRunJson(twoLevelRunId);
            Assertions.assertTrue(runJsonOpt.isPresent(), "readRunJson should return generated run.json");
            Assertions.assertTrue(runJsonOpt.get().contains("executionMetrics"), "Expected run.json to contain executionMetrics");

            Assertions.assertTrue(Files.exists(execFile), "Execution JSON file must stay in its uncategorized class folder");
            Assertions.assertTrue(Files.exists(classDir), "Class folder without a category must not be removed");
            Assertions.assertFalse(Files.exists(runDir.resolve("Browsing (default)")), "No Browsing (default) folder should be created on disk");

            final JsonNode runJsonRoot = new ObjectMapper().readTree(runJsonOpt.get());
            final JsonNode metrics = runJsonRoot.path("executionMetrics");
            Assertions.assertTrue(metrics.isObject() && metrics.size() > 0, "Expected run.json executionMetrics to be present");
            Assertions.assertEquals("Default", metrics.fields().next().getValue().path("areaName").asText(),
                "Expected run.json executionMetrics to carry the default category for the uncategorized class");
            Assertions.assertEquals("GoogleTest", metrics.fields().next().getValue().path("testClass").asText(),
                "Expected run.json executionMetrics to map the execution to the class folder name");

            Assertions.assertEquals("Default", findAreaForClass(dataService.getRunReport(twoLevelRunId), "GoogleTest"),
                "Class folder without a category should be grouped under the default category in the run report");
        }
        finally
        {
            deleteRecursively(runDir);
        }
    }

    @Test
    public void testExecutionWithAreaTagsResolvesAreaWithoutFolder() throws IOException
    {
        final String runId = "run-area-tags-888";
        final Path runDir = Paths.get("storage", "runs", runId);
        final Path classDir = runDir.resolve("Aura_my_test_yaml_Test");
        Files.createDirectories(classDir);

        final Path execFile = classDir.resolve("console-execution-1.json");
        final String execJson = """
            {
                "runId": "run_888",
                "status": "passed",
                "testName": "my-test.yaml · en",
                "testClass": "Aura_my_test_yaml_Test",
                "testFile": "com.xceptance.neodymium.aura.sandbox.Aura_my_test_yaml_Test#executeYamlTest",
                "tags": {
                    "Area": ["Search", "Browsing"],
                    "locale": ["ENG"],
                    "Project": ["Wiki"]
                }
            }
            """;
        Files.writeString(execFile, execJson);

        try
        {
            final Optional<String> runJsonOpt = storageService.readRunJson(runId);
            Assertions.assertTrue(runJsonOpt.isPresent(), "readRunJson should return generated run.json");

            final JsonNode runJsonRoot = new ObjectMapper().readTree(runJsonOpt.get());
            final JsonNode metrics = runJsonRoot.path("executionMetrics");
            Assertions.assertTrue(metrics.isObject() && metrics.size() > 0, "Expected run.json executionMetrics to be present");
            Assertions.assertEquals("@Search", metrics.fields().next().getValue().path("areaName").asText(),
                "Expected run.json executionMetrics to resolve area from tags.Area");

            final JsonNode enrichedExec = new ObjectMapper().readTree(execFile.toFile());
            Assertions.assertEquals("@Search", enrichedExec.path("areaName").asText(),
                "Enriched execution JSON on disk should carry the resolved area from tags");

            Assertions.assertEquals("@Search", findAreaForClass(dataService.getRunReport(runId), "Aura_my_test_yaml_Test"),
                "Run report should group test under @Search rather than Browsing (default)");
        }
        finally
        {
            deleteRecursively(runDir);
        }
    }

    @Test
    public void testMixedLayoutKeepsUncategorizedClassFoldersInPlace() throws IOException
    {
        final String mixedRunId = "run-mixed-layout-666";
        final Path runDir = Paths.get("storage", "runs", mixedRunId);

        final Path categorizedExecFile = runDir.resolve("Checkout").resolve("CheckoutProcessTest").resolve("exec-1.json");
        Files.createDirectories(categorizedExecFile.getParent());
        Files.writeString(categorizedExecFile, """
            {
                "id": "exec-666-1",
                "title": "CheckoutProcessTest [US Chrome]",
                "testClass": "CheckoutProcessTest",
                "status": "passed-clean",
                "location": "US",
                "browser": "Chrome"
            }
            """);

        final Path uncategorizedClassDir = runDir.resolve("StandaloneTest");
        Files.createDirectories(uncategorizedClassDir);
        final Path uncategorizedExecFile = uncategorizedClassDir.resolve("exec-2.json");
        Files.writeString(uncategorizedExecFile, """
            {
                "id": "exec-666-2",
                "title": "StandaloneTest [US Chrome]",
                "testClass": "StandaloneTest",
                "status": "failed",
                "location": "US",
                "browser": "Chrome"
            }
            """);

        try
        {
            final Optional<String> runJsonOpt = storageService.readRunJson(mixedRunId);
            Assertions.assertTrue(runJsonOpt.isPresent(), "readRunJson should return generated run.json");

            Assertions.assertTrue(Files.exists(categorizedExecFile), "Categorized execution JSON must stay in its category class folder");
            Assertions.assertTrue(Files.exists(uncategorizedExecFile), "Uncategorized execution JSON must stay in its class folder at the run root");
            Assertions.assertFalse(Files.exists(runDir.resolve("Browsing (default)")), "No Browsing (default) folder should be created on disk");

            final RunReportDto report = dataService.getRunReport(mixedRunId);
            Assertions.assertEquals("@Checkout", findAreaForClass(report, "CheckoutProcessTest"),
                "Class folder inside a category folder should keep the category from its folder");
            Assertions.assertEquals("Default", findAreaForClass(report, "StandaloneTest"),
                "Class folder without subfolders at the run root should be grouped under the default category");

            final JsonNode enrichedExec = new ObjectMapper().readTree(uncategorizedExecFile.toFile());
            Assertions.assertEquals("Default", enrichedExec.path("areaName").asText(),
                "Enriched uncategorized execution JSON should carry the default category while staying in place");
            Assertions.assertEquals("StandaloneTest", enrichedExec.path("testClass").asText(),
                "Enriched uncategorized execution JSON should keep its class name");
        }
        finally
        {
            deleteRecursively(runDir);
        }
    }

    @Test
    public void testUpdateExecutionInRunKeepsUncategorizedClassFolderInPlace() throws IOException
    {
        final String updateRunId = "run-update-in-place-555";
        final Path runDir = Paths.get("storage", "runs", updateRunId);
        final Path classDir = runDir.resolve("StandaloneUpdateTest");
        Files.createDirectories(classDir);
        final Path execFile = classDir.resolve("exec-1.json");
        Files.writeString(execFile, """
            {
                "id": "exec-update-1",
                "title": "StandaloneUpdateTest [US Chrome]",
                "testClass": "StandaloneUpdateTest",
                "status": "passed-clean",
                "location": "US",
                "browser": "Chrome"
            }
            """);

        try
        {
            final boolean updated = storageService.updateExecutionInRun(updateRunId, "exec-update-1", node -> node.put("status", "failed"));
            Assertions.assertTrue(updated, "updateExecutionInRun should update the matching execution");

            Assertions.assertTrue(Files.exists(execFile), "Updated execution JSON must stay in its uncategorized class folder");
            Assertions.assertFalse(Files.exists(runDir.resolve("Browsing (default)")), "No Browsing (default) folder should be created on update");

            final JsonNode updatedExec = new ObjectMapper().readTree(execFile.toFile());
            Assertions.assertEquals("failed", updatedExec.path("status").asText(), "Execution JSON content should be updated in place");
        }
        finally
        {
            deleteRecursively(runDir);
        }
    }

    @Test
    public void testEarliestStartTimeInRunJson() throws IOException
    {
        final String runId = "run-earliest-time-test";
        final Path runDir = Paths.get("storage", "runs", runId);
        if (Files.exists(runDir))
        {
            try (var stream = Files.walk(runDir))
            {
                stream.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
        final Path execDir = runDir.resolve("Browsing (default)").resolve("CartTest");
        Files.createDirectories(execDir);

        final String exec1Json = """
            {
                "id": "exec-later",
                "testClass": "CartTest",
                "status": "passed-clean",
                "startTime": "2026-08-20 10:00:00"
            }
            """;
        final String exec2Json = """
            {
                "id": "exec-earlier",
                "testClass": "CartTest",
                "status": "passed-clean",
                "startTime": "2026-08-20 06:15:00"
            }
            """;

        Files.writeString(execDir.resolve("exec-1.json"), exec1Json);
        Files.writeString(execDir.resolve("exec-2.json"), exec2Json);

        try
        {
            final boolean synced = syncService.importOrUpdateRunReport(runId);
            Assertions.assertTrue(synced, "syncService should import report into DB");

            final Optional<TestRunEntity> entityOpt = runRepository.findById(runId);
            Assertions.assertTrue(entityOpt.isPresent(), "TestRunEntity should be in database");
            final String label = entityOpt.get().getTimestampLabel();
            Assertions.assertTrue(label != null && (label.contains("06:15:00") || label.contains("08:15:00")),
                "Expected DB entity to contain earliest start time '06:15:00' (or timezone converted '08:15:00') but was '" + label + "'");
        }
        finally
        {
            if (Files.exists(runDir))
            {
                try (var stream = Files.walk(runDir))
                {
                    stream.sorted(Comparator.reverseOrder())
                          .map(Path::toFile)
                          .forEach(File::delete);
                }
            }
        }
    }

    @Test
    public void testLocalesCollectedFromLocaleFieldInRunJson() throws IOException
    {
        final String runId = "run-locale-collection-test";
        final Path runDir = Paths.get("storage", "runs", runId);
        final Path execDir = runDir.resolve("Browsing (default)").resolve("LocaleTest");
        Files.createDirectories(execDir);

        final String exec1Json = """
            {
                "id": "exec-de",
                "testClass": "LocaleTest",
                "status": "passed-clean",
                "locale": "DE",
                "browser": "Chrome"
            }
            """;
        final String exec2Json = """
            {
                "id": "exec-fr",
                "testClass": "LocaleTest",
                "status": "passed-clean",
                "locale": "FR",
                "browser": "Firefox"
            }
            """;

        Files.writeString(execDir.resolve("exec-1.json"), exec1Json);
        Files.writeString(execDir.resolve("exec-2.json"), exec2Json);

        try
        {
            final boolean synced = syncService.importOrUpdateRunReport(runId);
            Assertions.assertTrue(synced, "syncService should import report into DB");

            final Optional<TestRunEntity> entityOpt = runRepository.findById(runId);
            Assertions.assertTrue(entityOpt.isPresent(), "TestRunEntity should be in database");
            final String locales = entityOpt.get().getLocalesCsv();
            Assertions.assertTrue(locales != null && locales.contains("DE") && locales.contains("FR"),
                "Expected DB entity to contain locales DE and FR");
        }
        finally
        {
            if (Files.exists(runDir))
            {
                try (var stream = Files.walk(runDir))
                {
                    stream.sorted(Comparator.reverseOrder())
                          .map(Path::toFile)
                          .forEach(File::delete);
                }
            }
        }
    }

    @Test
    public void testStorageSyncPreservesDatabaseBugs() throws IOException
    {
        final String runId = "run-sync-db-bugs-test";
        final Path runDir = Paths.get("storage", "runs", runId);
        final Path execDir = runDir.resolve("General (default)").resolve("SyncBugTest");
        Files.createDirectories(execDir);

        final String execJson = """
            {
                "id": "exec-sync-bug-1",
                "testClass": "SyncBugTest",
                "title": "SyncBugTest [US Chrome]",
                "status": "failed",
                "location": "US",
                "browser": "Chrome"
            }
            """;
        Files.writeString(execDir.resolve("exec-1.json"), execJson);

        final String varId = AuraReportDataService.generateVariationId("SyncBugTest", "SyncBugTest [US Chrome]", "US", "Chrome");
        final TestBaseBugEntity bug = new TestBaseBugEntity(varId, "BUG-SYNC-999", "Default", "Default", System.currentTimeMillis(), runId);
        bugRepository.save(bug);

        try
        {
            final boolean success = syncService.importOrUpdateRunReport(runId);
            Assertions.assertTrue(success, "importOrUpdateRunReport should succeed for new run");

            final Optional<TestRunEntity> runOpt = runRepository.findById(runId);
            Assertions.assertTrue(runOpt.isPresent(), "Run entity should be created in database");

            final TestRunEntity runEntity = runOpt.get();
            Assertions.assertEquals(1, runEntity.getFailedKnownCount(), "Failed known count should be 1 after storage sync incorporating DB bugs");
            Assertions.assertEquals(0, runEntity.getFailedUnknownCount(), "Failed unknown count should be 0");
        }
        finally
        {
            bugRepository.delete(bug);
            if (Files.exists(runDir))
            {
                try (var stream = Files.walk(runDir))
                {
                    stream.sorted(Comparator.reverseOrder())
                          .map(Path::toFile)
                          .forEach(File::delete);
                }
            }
        }
    }

    @Test
    public void testRunDurationCalculation() throws IOException
    {
        final String runId = "run-duration-calc-test";
        final Path runDir = Paths.get("storage", "runs", runId);
        final Path execDir = runDir.resolve("General (default)").resolve("DurationTest");
        Files.createDirectories(execDir);

        final String exec1Json = """
            {
                "id": "exec-dur-1",
                "testClass": "DurationTest",
                "title": "DurationTest1",
                "status": "passed",
                "startTime": "2026-08-20T10:00:00.000Z",
                "duration": 5000
            }
            """;
        final String exec2Json = """
            {
                "id": "exec-dur-2",
                "testClass": "DurationTest",
                "title": "DurationTest2",
                "status": "passed",
                "startTime": "2026-08-20T10:02:00.000Z",
                "duration": 3000
            }
            """;

        Files.writeString(execDir.resolve("exec-1.json"), exec1Json);
        Files.writeString(execDir.resolve("exec-2.json"), exec2Json);

        try
        {
            final boolean synced = syncService.importOrUpdateRunReport(runId);
            Assertions.assertTrue(synced, "syncService should import report into DB");

            final Optional<TestRunEntity> entityOpt = runRepository.findById(runId);
            Assertions.assertTrue(entityOpt.isPresent(), "TestRunEntity should be in database");

            final TestRunEntity entity = entityOpt.get();
            Assertions.assertEquals(123000L, entity.getDurationMs(), "Expected durationMs to be 123,000 ms");
            Assertions.assertEquals("2 min 3 s", entity.getFormattedDuration(), "Expected formattedDuration to be '2 min 3 s'");
        }
        finally
        {
            if (Files.exists(runDir))
            {
                try (var stream = Files.walk(runDir))
                {
                    stream.sorted(Comparator.reverseOrder())
                          .map(Path::toFile)
                          .forEach(File::delete);
                }
            }
        }
    }

    @Test
    public void testDifferentTestMethodsSameClassDatasetBrowser() throws IOException
    {
        final String runId = "run-method-diff-test";
        final Path runDir = Paths.get("storage", "runs", runId);
        final Path execDir = runDir.resolve("Integration").resolve("VerlaProgrammaticDemoTest");
        Files.createDirectories(execDir);

        final String exec1Json = """
            {
                "id": "exec-m1",
                "testClass": "VerlaProgrammaticDemoTest",
                "testMethod": "test3_AnnotationDrivenInlinePlaybookTextBlocks",
                "datasetId": "default",
                "title": "default",
                "browser": "Chrome",
                "status": "passed-clean",
                "junitTags": ["VerlaProgrammaticDemoTest", "test3_AnnotationDrivenInlinePlaybookTextBlocks", "Dataset: default"],
                "steps": [{"title": "Open homepage", "status": "PASSED"}]
            }
            """;
        final String exec2Json = """
            {
                "id": "exec-m2",
                "testClass": "VerlaProgrammaticDemoTest",
                "testMethod": "test7_AnnotationDrivenExternalPlaybookConvention",
                "datasetId": "default",
                "title": "default",
                "browser": "Chrome",
                "status": "passed-clean",
                "junitTags": ["VerlaProgrammaticDemoTest", "test7_AnnotationDrivenExternalPlaybookConvention", "Dataset: default"],
                "steps": [{"title": "Open homepage", "status": "PASSED"}]
            }
            """;
        final String exec3Json = """
            {
                "id": "exec-m3",
                "testClass": "VerlaProgrammaticDemoTest",
                "testMethod": "test6_AnnotationDrivenExternalPlaybookExplicit",
                "datasetId": "default",
                "title": "default",
                "browser": "Chrome",
                "status": "passed-clean",
                "junitTags": ["VerlaProgrammaticDemoTest", "test6_AnnotationDrivenExternalPlaybookExplicit", "Dataset: default"],
                "steps": [{"title": "Open homepage", "status": "PASSED"}]
            }
            """;

        Files.writeString(execDir.resolve("console-execution-1.json"), exec1Json);
        Files.writeString(execDir.resolve("console-execution-2.json"), exec2Json);
        Files.writeString(execDir.resolve("console-execution-3.json"), exec3Json);

        try
        {
            final boolean synced = syncService.importOrUpdateRunReport(runId);
            Assertions.assertTrue(synced, "syncService should import report into DB");

            final Optional<TestRunEntity> entityOpt = runRepository.findById(runId);
            Assertions.assertTrue(entityOpt.isPresent(), "TestRunEntity should be in database");

            final TestRunEntity entity = entityOpt.get();
            Assertions.assertEquals(3, entity.getTotalTests(), "Expected 3 total tests for 3 different test methods");

            final Optional<String> runJsonOpt = storageService.readRunJson(runId);
            Assertions.assertTrue(runJsonOpt.isPresent(), "run.json should be generated");

            final String runJson = runJsonOpt.get();
            Assertions.assertTrue(runJson.contains("test3_AnnotationDrivenInlinePlaybookTextBlocks"), "run.json should contain test3 method key");
            Assertions.assertTrue(runJson.contains("test7_AnnotationDrivenExternalPlaybookConvention"), "run.json should contain test7 method key");
            Assertions.assertTrue(runJson.contains("test6_AnnotationDrivenExternalPlaybookExplicit"), "run.json should contain test6 method key");

            final TestExecutionDto exec3Details = dataService.getExecutionDetails(runId, "VerlaProgrammaticDemoTest#test3_AnnotationDrivenInlinePlaybookTextBlocks#Default#Chrome");
            Assertions.assertNotNull(exec3Details, "getExecutionDetails should locate execution by test method key");
            Assertions.assertNotNull(exec3Details.getSteps(), "Steps list should be populated for test3 execution");
        }
        finally
        {
            if (Files.exists(runDir))
            {
                try (var stream = Files.walk(runDir))
                {
                    stream.sorted(Comparator.reverseOrder())
                          .map(Path::toFile)
                          .forEach(File::delete);
                }
            }
        }
    }

    /**
     * Returns the area name that groups the given test class in the run report, or {@code null} if the class is not
     * listed.
     */
    private String findAreaForClass(final RunReportDto report, final String className)
    {
        if (report == null)
        {
            return null;
        }
        for (final AreaSummaryDto area : report.getAreaSummaries())
        {
            for (final TestClassSummaryDto testClass : area.getTestClasses())
            {
                if (className.equals(testClass.getClassName()))
                {
                    return area.getAreaName();
                }
            }
        }
        return null;
    }

    /**
     * Deletes the given directory recursively, ignoring a missing path.
     */
    private void deleteRecursively(final Path path) throws IOException
    {
        if (Files.exists(path))
        {
            try (var stream = Files.walk(path))
            {
                stream.sorted(Comparator.reverseOrder())
                      .map(Path::toFile)
                      .forEach(File::delete);
            }
        }
    }

    @Test
    public void testHealedActionTargetPreservedInStorageService() throws Exception
    {
        final String runId = "run_test_healed_action_" + System.currentTimeMillis();
        final Path runDir = Paths.get("storage", "runs", runId);
        final Path execDir = runDir.resolve("Aura_my_test_yaml_Test");
        Files.createDirectories(execDir);

        final String healedJson = """
            {
              "runId": "%s",
              "status": "passed",
              "testName": "Aura_my_test_yaml_Test",
              "blocks": {
                "steps": [
                  {
                    "instruction": "Select language",
                    "status": "passed",
                    "actions": [
                      {
                        "type": "SELECT",
                        "target": "#searchLanguage",
                        "resolvedTarget": "#searchLanguage",
                        "expectedTarget": "#selectLanguage",
                        "originalTarget": "#selectLanguage",
                        "healedFrom": "#selectLanguage",
                        "value": "en",
                        "resolvedValue": "en",
                        "healed": true
                      }
                    ]
                  }
                ]
              }
            }
            """.formatted(runId);

        final Path execFile = execDir.resolve("console-execution-1.json");
        Files.writeString(execFile, healedJson);

        try
        {
            final ObjectMapper mapper = new ObjectMapper();
            final JsonNode rootNode = mapper.readTree(execFile.toFile());
            final TestExecutionDto dto = mapper.treeToValue(rootNode, TestExecutionDto.class);
            Assertions.assertNotNull(dto.getBlocks(), "Blocks should be populated");

            final JsonNode actionNode = dto.getBlocks().path("steps").get(0).path("actions").get(0);
            Assertions.assertEquals("#searchLanguage", actionNode.path("target").asText());
            Assertions.assertEquals("#searchLanguage", actionNode.path("resolvedTarget").asText());
            Assertions.assertEquals("#selectLanguage", actionNode.path("expectedTarget").asText());
            Assertions.assertEquals("#selectLanguage", actionNode.path("originalTarget").asText());
            Assertions.assertEquals("#selectLanguage", actionNode.path("healedFrom").asText());
            Assertions.assertTrue(actionNode.path("healed").asBoolean(), "healed flag should be true");
        }
        finally
        {
            deleteRecursively(runDir);
        }
    }

    @Test
    public void testHealedStepAndExecutionStatusInStorageAndDto() throws IOException
    {
        final String runId = "run-healed-status-test";
        final Path runDir = Paths.get("storage", "runs", runId);
        final Path execDir = runDir.resolve("Yaml").resolve("YamlTest");
        Files.createDirectories(execDir);

        final String healedJson = """
            {
              "id": "yaml-exec-1",
              "runId": "%s",
              "testClass": "YamlTest",
              "status": "passed",
              "blocks": {
                "steps": [
                  {
                    "instruction": "Select language",
                    "status": "healed",
                    "healed": true,
                    "actions": [
                      {
                        "type": "SELECT",
                        "target": "#searchLanguage",
                        "expectedTarget": "#selectLanguage",
                        "healed": true
                      }
                    ]
                  }
                ]
              }
            }
            """.formatted(runId);

        final Path execFile = execDir.resolve("console-execution-1.json");
        Files.writeString(execFile, healedJson);

        try
        {
            final ObjectMapper mapper = new ObjectMapper();
            final JsonNode rootNode = mapper.readTree(execFile.toFile());
            final TestExecutionDto dto = mapper.treeToValue(rootNode, TestExecutionDto.class);
            Assertions.assertEquals("PASSED", dto.getDisplayStatusKey());
            Assertions.assertTrue(dto.isHealed(), "Execution should be marked as healed");
            Assertions.assertTrue(dto.isPassedHealed(), "Execution should be passed healed");
            Assertions.assertFalse(dto.isFailedHealed(), "Execution should not be failed healed");

            final Optional<String> runJsonOpt = storageService.buildRunJsonContent(runDir.toFile(), runId, true);
            Assertions.assertTrue(runJsonOpt.isPresent(), "run.json content should be generated");
            final JsonNode runNode = mapper.readTree(runJsonOpt.get());
            Assertions.assertEquals(0, runNode.path("summary").path("fixed").asInt(), "Fixed count should be 0 because no bugs are linked");
            Assertions.assertEquals(1, runNode.path("summary").path("pass").asInt(), "Passed count should be 1");
            final RunReportDto report = dataService.getRunReport(runId);
            Assertions.assertNotNull(report, "RunReportDto should not be null");
            Assertions.assertEquals(0, report.getFixedCount(), "RunReport should reflect 0 fixed tests (no bugs)");
            Assertions.assertEquals(1, report.getPassCount(), "RunReport should reflect 1 passed test");
            Assertions.assertEquals(1, report.getPassedHealedCount(), "RunReport should reflect 1 passed healed test");
            Assertions.assertEquals(0, report.getFailedHealedCount(), "RunReport should reflect 0 failed healed tests");
            Assertions.assertEquals(1, report.getHealedCount(), "RunReport should reflect 1 healed step");
            Assertions.assertEquals("PASSED", report.getExecutions().get(0).getDisplayStatusKey());
        }
        finally
        {
            deleteRecursively(runDir);
        }
    }

    @Test
    public void testFailedHealedStepAndExecutionStatusInStorageAndDto() throws Exception
    {
        final String runId = "run-failed-healed-test";
        final Path runDir = Paths.get("storage", "runs", runId);
        final Path execDir = runDir.resolve("Yaml").resolve("YamlFailTest");
        Files.createDirectories(execDir);

        final String failedHealedJson = """
            {
              "id": "yaml-exec-fail-1",
              "runId": "%s",
              "testClass": "YamlFailTest",
              "status": "failed-unknown",
              "failure": "Element not clickable",
              "blocks": {
                "steps": [
                  {
                    "instruction": "Select language",
                    "status": "healed",
                    "healed": true,
                    "actions": [
                      {
                        "type": "SELECT",
                        "target": "#searchLanguage",
                        "expectedTarget": "#selectLanguage",
                        "healed": true
                      }
                    ]
                  },
                  {
                    "instruction": "Click checkout",
                    "status": "failed",
                    "error": "Element not clickable"
                  }
                ]
              }
            }
            """.formatted(runId);

        final Path execFile = execDir.resolve("console-execution-fail-1.json");
        Files.writeString(execFile, failedHealedJson);

        try
        {
            final ObjectMapper mapper = new ObjectMapper();
            final JsonNode rootNode = mapper.readTree(execFile.toFile());
            final TestExecutionDto dto = mapper.treeToValue(rootNode, TestExecutionDto.class);
            Assertions.assertEquals("FAILED_UNKNOWN", dto.getDisplayStatusKey());
            Assertions.assertTrue(dto.isHealed(), "Execution should be marked as healed");
            Assertions.assertFalse(dto.isPassedHealed(), "Execution should not be passed healed");
            Assertions.assertTrue(dto.isFailedHealed(), "Execution should be failed healed");

            final Optional<String> runJsonOpt = storageService.buildRunJsonContent(runDir.toFile(), runId, true);
            Assertions.assertTrue(runJsonOpt.isPresent(), "run.json content should be generated");
            final JsonNode runNode = mapper.readTree(runJsonOpt.get());
            Assertions.assertEquals(0, runNode.path("summary").path("fixed").asInt(), "Fixed count should be 0");
            Assertions.assertEquals(0, runNode.path("summary").path("pass").asInt(), "Passed count should be 0");
            Assertions.assertEquals(1, runNode.path("summary").path("unknown").asInt(), "Unknown fail count should be 1");
            final RunReportDto report = dataService.getRunReport(runId);
            Assertions.assertNotNull(report, "RunReportDto should not be null");
            Assertions.assertEquals(0, report.getFixedCount(), "RunReport should reflect 0 fixed tests");
            Assertions.assertEquals(0, report.getPassCount(), "RunReport should reflect 0 passed tests");
            Assertions.assertEquals(0, report.getPassedHealedCount(), "RunReport should reflect 0 passed healed tests");
            Assertions.assertEquals(1, report.getFailedHealedCount(), "RunReport should reflect 1 failed healed test");
            Assertions.assertEquals(1, report.getHealedCount(), "RunReport should reflect 1 healed step");
            Assertions.assertEquals("FAILED_UNKNOWN", report.getExecutions().get(0).getDisplayStatusKey());
        }
        finally
        {
            deleteRecursively(runDir);
        }
    }

    @Test
    public void testSyncExecutionWithHealedSetsVariationEntityHealedAndHistoryLink() throws IOException
    {
        final String runId = "run-healed-sync-test";
        final Path runDir = Paths.get("storage", "runs", runId);
        final Path execDir = runDir.resolve("Checkout").resolve("HealedSyncTest");

        final String testClass = "HealedSyncTest";
        final String testMethod = "testAutoHeal";
        final String dataSet = "Default";
        final String location = "US";
        final String browser = "Chrome";
        final String varId = AuraReportDataService.generateVariationId(testClass, testMethod, dataSet, location, browser);

        try
        {
            Files.createDirectories(execDir);

            final String execJson = """
                {
                    "id": "exec-healed-1",
                    "title": "Default",
                    "testClass": "HealedSyncTest",
                    "testMethod": "testAutoHeal",
                    "status": "passed-clean",
                    "healed": true,
                    "location": "US",
                    "browser": "Chrome"
                }
                """;
            Files.writeString(execDir.resolve("exec-1.json"), execJson);

            Assertions.assertTrue(syncService.importOrUpdateRunReport(runId), "Import should succeed");

            final Optional<TestBaseVariationEntity> varOpt = variationRepository.findById(varId);
            Assertions.assertTrue(varOpt.isPresent(), "Variation entity should be created/updated in database");
            final TestBaseVariationEntity varEntity = varOpt.get();
            Assertions.assertTrue(varEntity.isHealed(), "Variation entity should be marked as healed");
            Assertions.assertEquals("HEALED", varEntity.getExecutionMode(), "Variation execution mode should be HEALED");
        }
        finally
        {
            deleteRecursively(runDir);
            runRepository.findById(runId).ifPresent(runRepository::delete);
            variationRepository.findById(varId).ifPresent(variationRepository::delete);
        }
    }

    @Test
    public void testEarliestTestStartTimeOverridesContainerTimestamp() throws IOException
    {
        final String runId = "run-earliest-override-test";
        final Path runDir = Paths.get("storage", "runs", runId);
        if (Files.exists(runDir))
        {
            deleteRecursively(runDir);
        }
        final Path execDir = runDir.resolve("Browsing (default)").resolve("CartTest");
        Files.createDirectories(execDir);

        // Pre-create a run.json with a top-level container timestamp of 05:00:00
        final String initialRunJson = """
            {
                "runId": "run-earliest-override-test",
                "timestamp": "2026-08-20 05:00:00",
                "startTimeMs": 1787202000000
            }
            """;
        Files.writeString(runDir.resolve("run.json"), initialRunJson);

        final String exec1Json = """
            {
                "id": "exec-later",
                "testClass": "CartTest",
                "status": "passed-clean",
                "startTime": "2026-08-20 10:00:00"
            }
            """;
        final String exec2Json = """
            {
                "id": "exec-earliest",
                "testClass": "CartTest",
                "status": "passed-clean",
                "startTime": "2026-08-20 07:15:00"
            }
            """;

        Files.writeString(execDir.resolve("exec-1.json"), exec1Json);
        Files.writeString(execDir.resolve("exec-2.json"), exec2Json);

        try
        {
            final boolean synced = syncService.importOrUpdateRunReport(runId);
            Assertions.assertTrue(synced, "syncService should import report into DB");

            final Optional<TestRunEntity> entityOpt = runRepository.findById(runId);
            Assertions.assertTrue(entityOpt.isPresent(), "TestRunEntity should be in database");

            final TestRunEntity entity = entityOpt.get();
            final String label = entity.getTimestampLabel();
            // Earliest test starts at 07:15:00, not the container's 05:00:00
            Assertions.assertFalse(label != null && label.contains("05:00:00"),
                "Run timestamp must NOT be the container timestamp (05:00:00) when tests have start times");
            Assertions.assertTrue(label != null && (label.contains("07:15:00") || label.contains("09:15:00")),
                "Expected run timestamp to be defined by earliest test '07:15:00' (or local tz '09:15:00'), but was: " + label);

            // Also verify that the generated run.json has the updated earliest startTimeMs
            final Optional<String> runJsonOpt = storageService.readRunJson(runId);
            Assertions.assertTrue(runJsonOpt.isPresent(), "run.json should exist");
            final JsonNode runJsonRoot = new ObjectMapper().readTree(runJsonOpt.get());
            Assertions.assertEquals(entity.getStartTimeMs(), runJsonRoot.path("startTimeMs").asLong(),
                "run.json startTimeMs should match DB entity startTimeMs");
        }
        finally
        {
            deleteRecursively(runDir);
            runRepository.findById(runId).ifPresent(runRepository::delete);
        }
    }

    @Test
    public void testLiveIngestionAndFinishRunUpdatesRunTimestampToEarliestTest() throws IOException
    {
        final String runId = "run-live-earliest-test";
        final Path runDir = Paths.get("storage", "runs", runId);
        if (Files.exists(runDir))
        {
            deleteRecursively(runDir);
        }

        try
        {
            dataService.startRun(runId, "EarliestTestBatch", "PROD", "TEST");

            final Map<String, Object> exec1 = new HashMap<>();
            exec1.put("id", "exec-live-1");
            exec1.put("testClass", "LiveTest");
            exec1.put("title", "LiveTest1");
            exec1.put("status", "passed");
            exec1.put("startTime", "2026-08-20 11:30:00");

            dataService.ingestExecution(runId, exec1);

            Optional<TestRunEntity> entityOpt = runRepository.findById(runId);
            Assertions.assertTrue(entityOpt.isPresent());
            String label = entityOpt.get().getTimestampLabel();
            Assertions.assertTrue(label != null && (label.contains("11:30:00") || label.contains("13:30:00")),
                "Expected timestamp from exec1, but was: " + label);

            // Ingest an execution that started earlier
            final Map<String, Object> exec2 = new HashMap<>();
            exec2.put("id", "exec-live-2");
            exec2.put("testClass", "LiveTest");
            exec2.put("title", "LiveTest2");
            exec2.put("status", "passed");
            exec2.put("startTime", "2026-08-20 08:15:00");

            dataService.ingestExecution(runId, exec2);

            entityOpt = runRepository.findById(runId);
            Assertions.assertTrue(entityOpt.isPresent());
            label = entityOpt.get().getTimestampLabel();
            Assertions.assertTrue(label != null && (label.contains("08:15:00") || label.contains("10:15:00")),
                "Expected run timestamp updated to earlier exec2 '08:15:00', but was: " + label);

            // Finish run
            dataService.finishRun(runId);

            entityOpt = runRepository.findById(runId);
            Assertions.assertTrue(entityOpt.isPresent());
            final TestRunEntity finishedRun = entityOpt.get();
            final String finishLabel = finishedRun.getTimestampLabel();
            Assertions.assertTrue(finishLabel != null && (finishLabel.contains("08:15:00") || finishLabel.contains("10:15:00")),
                "Expected finished run timestamp to remain earliest test '08:15:00', but was: " + finishLabel);
        }
        finally
        {
            deleteRecursively(runDir);
            runRepository.findById(runId).ifPresent(runRepository::delete);
        }
    }

    @Test
    public void testMixedLocalesAndBrowsersIncludesDefaultAndSpecificInLocalesCsvAndBrowsersCsv() throws IOException
    {
        final String runId = "run-mixed-locales-test";
        final Path runDir = Paths.get("storage", "runs", runId);
        if (Files.exists(runDir))
        {
            deleteRecursively(runDir);
        }

        final Path classDir = runDir.resolve("WikipediaTest");
        Files.createDirectories(classDir);

        // Exec 1: locale = "en", browser = "Chrome_1920x1080"
        Files.writeString(classDir.resolve("exec-1.json"), """
            {
                "id": "exec-mixed-1",
                "title": "Wikipedia Search [en]",
                "testClass": "WikipediaTest",
                "status": "passed",
                "locale": "en",
                "browser": "Chrome_1920x1080"
            }
            """);

        // Exec 2: no locale, browser = "Chrome_1920x1080"
        Files.writeString(classDir.resolve("exec-2.json"), """
            {
                "id": "exec-mixed-2",
                "title": "Wikipedia Search [Default 1]",
                "testClass": "WikipediaTest",
                "status": "passed",
                "browser": "Chrome_1920x1080"
            }
            """);

        // Exec 3: no locale, browser = "Chrome_1920x1080"
        Files.writeString(classDir.resolve("exec-3.json"), """
            {
                "id": "exec-mixed-3",
                "title": "Wikipedia Search [Default 2]",
                "testClass": "WikipediaTest",
                "status": "failed",
                "browser": "Chrome_1920x1080"
            }
            """);

        try
        {
            final Optional<String> runJsonOpt = storageService.readRunJson(runId);
            Assertions.assertTrue(runJsonOpt.isPresent(), "readRunJson should generate run.json");

            final ObjectMapper mapper = new ObjectMapper();
            final JsonNode runJsonNode = mapper.readTree(runJsonOpt.get());

            // Verify top-level locales in generated run.json includes both Default and EN
            final JsonNode localesNode = runJsonNode.path("locales");
            Assertions.assertTrue(localesNode.isArray(), "locales in run.json must be an array");
            final Set<String> localesInJson = new HashSet<>();
            localesNode.forEach(l -> localesInJson.add(l.asText()));
            Assertions.assertTrue(localesInJson.contains("Default"), "locales in run.json must contain Default");
            Assertions.assertTrue(localesInJson.contains("EN") || localesInJson.contains("en"), "locales in run.json must contain EN");

            // Verify top-level browsers in generated run.json is Chrome_1920x1080
            final JsonNode browsersNode = runJsonNode.path("browsers");
            Assertions.assertTrue(browsersNode.isArray(), "browsers in run.json must be an array");
            final Set<String> browsersInJson = new HashSet<>();
            browsersNode.forEach(b -> browsersInJson.add(b.asText()));
            Assertions.assertTrue(browsersInJson.contains("Chrome_1920x1080"), "browsers in run.json must contain Chrome_1920x1080");

            final boolean synced = syncService.importOrUpdateRunReport(runId);
            Assertions.assertTrue(synced, "importOrUpdateRunReport should succeed");

            // Verify DB entity TestRunEntity created by syncService
            final Optional<TestRunEntity> entityOpt = runRepository.findById(runId);
            Assertions.assertTrue(entityOpt.isPresent(), "TestRunEntity should be saved in DB");
            final TestRunEntity entity = entityOpt.get();
            final String localesCsv = entity.getLocalesCsv();
            Assertions.assertNotNull(localesCsv);
            Assertions.assertTrue(localesCsv.contains("Default"), "localesCsv must contain Default");
            Assertions.assertTrue(localesCsv.contains("EN") || localesCsv.contains("en"), "localesCsv must contain EN");

            final String browsersCsv = entity.getBrowsersCsv();
            Assertions.assertNotNull(browsersCsv);
            Assertions.assertTrue(browsersCsv.contains("Chrome_1920x1080"), "browsersCsv must contain Chrome_1920x1080");
        }
        finally
        {
            deleteRecursively(runDir);
            runRepository.findById(runId).ifPresent(runRepository::delete);
        }
    }

    @Test
    public void testExtractLocationFromNode() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();

        // 1. From direct "locale" field
        final JsonNode nodeDirectLocale = mapper.readTree("{\"locale\": \"de_DE\"}");
        Assertions.assertEquals("de_DE", RunStorageSyncService.extractLocationFromNode(nodeDirectLocale));

        // 2. From direct "location" field
        final JsonNode nodeDirectLocation = mapper.readTree("{\"location\": \"US\"}");
        Assertions.assertEquals("US", RunStorageSyncService.extractLocationFromNode(nodeDirectLocation));

        // 3. From tags.locale array
        final JsonNode nodeTagLocaleArray = mapper.readTree("{\"tags\": {\"locale\": [\"RUS\", \"ENG\"]}}");
        Assertions.assertEquals("RUS", RunStorageSyncService.extractLocationFromNode(nodeTagLocaleArray));

        // 4. From tags.locale string
        final JsonNode nodeTagLocaleString = mapper.readTree("{\"tags\": {\"locale\": \"ENG\"}}");
        Assertions.assertEquals("ENG", RunStorageSyncService.extractLocationFromNode(nodeTagLocaleString));

        // 5. From tags.location string
        final JsonNode nodeTagLocationString = mapper.readTree("{\"tags\": {\"location\": \"FR\"}}");
        Assertions.assertEquals("FR", RunStorageSyncService.extractLocationFromNode(nodeTagLocationString));

        // 6. When empty or absent
        final JsonNode nodeEmpty = mapper.readTree("{}");
        Assertions.assertEquals("", RunStorageSyncService.extractLocationFromNode(nodeEmpty));
    }

    @Test
    public void testImportOrUpdateRunReportExtractsLocalesFromTags() throws IOException
    {
        final String runId = "run-tags-locale-test";
        final Path runDir = Paths.get("storage", "runs", runId);
        final Path classDir = runDir.resolve("Search").resolve("LocaleTagTest");
        Files.createDirectories(classDir);

        Files.writeString(classDir.resolve("exec-en.json"), """
            {
                "id": "exec-en",
                "title": "Search [en]",
                "testClass": "LocaleTagTest",
                "status": "passed",
                "browser": "Chrome",
                "tags": {
                    "locale": ["ENG"]
                }
            }
            """);

        Files.writeString(classDir.resolve("exec-ru.json"), """
            {
                "id": "exec-ru",
                "title": "Search [ru]",
                "testClass": "LocaleTagTest",
                "status": "passed",
                "browser": "Chrome",
                "tags": {
                    "locale": ["RUS"]
                }
            }
            """);

        try
        {
            final boolean synced = syncService.importOrUpdateRunReport(runId);
            Assertions.assertTrue(synced, "importOrUpdateRunReport should succeed");

            final Optional<TestRunEntity> entityOpt = runRepository.findById(runId);
            Assertions.assertTrue(entityOpt.isPresent(), "TestRunEntity should be saved in DB");
            final TestRunEntity entity = entityOpt.get();
            final String localesCsv = entity.getLocalesCsv();
            Assertions.assertNotNull(localesCsv);
            Assertions.assertTrue(localesCsv.contains("ENG"), "localesCsv must contain ENG");
            Assertions.assertTrue(localesCsv.contains("RUS"), "localesCsv must contain RUS");
        }
        finally
        {
            deleteRecursively(runDir);
            runRepository.findById(runId).ifPresent(runRepository::delete);
        }
    }

    @Test
    public void testExecutionMetricsDeserializationWithObjectBugs() throws IOException
    {
        final String runId = "run-obj-bugs-test";
        final Path runDir = Paths.get("storage", "runs", runId);
        final Path classDir = runDir.resolve("@Orders").resolve("OrderHistoryTest");
        Files.createDirectories(classDir);

        final Path execFile = classDir.resolve("console-execution-34.json");
        Files.writeString(execFile, """
            {
                "runId": "run-obj-bugs-test",
                "testName": "OrderHistoryTest · US",
                "status": "failed",
                "areaName": "@Orders",
                "testClass": "OrderHistoryTest",
                "testMethod": "verifyCustomerOrderHistory",
                "title": "US",
                "id": "console-execution-34",
                "location": "US",
                "browser": "Chrome_iPhoneX",
                "bugs": [
                    {
                        "id": "HUG-85323",
                        "title": "Identified issue in OrderHistoryTest",
                        "url": "https://jira.xceptance.de/browse/HUG-85323",
                        "status": "Open"
                    }
                ]
            }
            """);

        try
        {
            final Optional<String> runJsonOpt = storageService.readRunJson(runId);
            Assertions.assertTrue(runJsonOpt.isPresent(), "readRunJson should return generated run.json");

            final RunReportDto report = dataService.getRunReport(runId);
            Assertions.assertNotNull(report, "RunReportDto should not be null");
            Assertions.assertEquals(1, report.getExecutions().size(), "Report must contain the test execution");

            final TestExecutionDto exec = report.getExecutions().get(0);
            Assertions.assertEquals("OrderHistoryTest", exec.getTestClass());
            Assertions.assertEquals("verifyCustomerOrderHistory", exec.getTestMethod());
            Assertions.assertEquals(1, exec.getBugs().size());
            Assertions.assertEquals("HUG-85323", exec.getBugs().get(0));
            Assertions.assertEquals("failed-known", exec.getStatus());

            final JsonNode runJsonRoot = new ObjectMapper().readTree(runJsonOpt.get());
            final JsonNode metrics = runJsonRoot.path("executionMetrics");
            Assertions.assertTrue(metrics.isObject() && metrics.size() > 0, "run.json executionMetrics must be present");
            final String execKey = LocalRunJsonStorageService.buildExecutionKey("OrderHistoryTest", "verifyCustomerOrderHistory", "US", "Chrome_iPhoneX");
            final JsonNode metricObj = metrics.path(execKey);
            Assertions.assertTrue(metricObj.isObject(), "Execution metric should be mapped under composite key: " + execKey);
            Assertions.assertTrue(metricObj.path("bugs").isArray(), "Bugs in executionMetrics must be serialized as an array");
            Assertions.assertEquals("HUG-85323", metricObj.path("bugs").get(0).asText());
        }
        finally
        {
            deleteRecursively(runDir);
            runRepository.findById(runId).ifPresent(runRepository::delete);
        }
    }

    @Test
    public void testExecutionDetailsResolvesWithFolderFallbackAndMissingTopLevelMetadata() throws Exception
    {
        final String runId = "run_test_step_resolution_" + System.currentTimeMillis();
        final Path runDir = storageService.getRunDir(runId);
        final String parentFolderName = "Aura_tests_stokke_stokkeOrderPayPalTest_yml_Test";
        final Path testFolder = runDir.resolve(parentFolderName);

        try
        {
            Files.createDirectories(testFolder);
            final String executionJson = """
                {
                    "testFile": "com.xceptance.test.PayPalTest#testOrderPayPal",
                    "datasetId": "default",
                    "status": "failed-assertion",
                    "blocks": {
                        "steps": [
                            { "name": "Navigate to Home", "status": "PASSED" },
                            { "name": "Add to Cart", "status": "FAILED" }
                        ]
                    }
                }
                """;
            Files.writeString(testFolder.resolve("console-execution-1.json"), executionJson);

            // 1. Resolve by composite key using parent folder and method
            final TestExecutionDto dtoByKey = dataService.getExecutionDetails(
                runId,
                parentFolderName + "#testOrderPayPal#default#Chrome"
            );
            Assertions.assertNotNull(dtoByKey, "Execution details must be located using folder and method composite key");
            Assertions.assertNotNull(dtoByKey.getBlocks(), "Execution blocks must be populated");
            Assertions.assertEquals(2, dtoByKey.getBlocks().path("steps").size());

            // 2. Resolve when runId carries '#' prefix from frontend URL
            final TestExecutionDto dtoWithHash = dataService.getExecutionDetails(
                "#" + runId,
                parentFolderName + "#console-execution-1"
            );
            Assertions.assertNotNull(dtoWithHash, "Execution details must be resolved even with leading hash in runId");

            // 3. Resolve using folder prefix match
            final TestExecutionDto dtoByPrefix = dataService.getExecutionDetails(
                runId,
                parentFolderName + "#exec-random-postfix"
            );
            Assertions.assertNotNull(dtoByPrefix, "Execution details must match based on unique class folder prefix");
        }
        finally
        {
            deleteRecursively(runDir);
        }
    }

    @Test
    public void testMultiDatasetExecutionResolutionInSharedFolderDoesNotCrossTalk() throws IOException
    {
        final String runId = "run-multi-dataset-test";
        final Path runDir = Paths.get("storage", "runs", runId);
        final String parentFolderName = "Aura_tests_stokke_stokkeOrderPayPalTest_yml_Test";
        final Path testFolder = runDir.resolve(parentFolderName);

        try
        {
            Files.createDirectories(testFolder);

            // Dataset A (File 1)
            final String exec1Json = """
                {
                    "id": "order-us-dataset",
                    "testClass": "com.xceptance.neodymium.aura.sandbox.Aura_tests_stokke_stokkeOrderPayPalTest_yml_Test",
                    "testMethod": "executeYamlTest",
                    "title": "Order US Product",
                    "browser": "Chrome_1500x1000",
                    "status": "passed-clean",
                    "blocks": { "steps": [{ "name": "Step US" }] }
                }
                """;
            Files.writeString(testFolder.resolve("console-execution-1.json"), exec1Json);

            // Dataset B (File 2)
            final String exec2Json = """
                {
                    "id": "order-de-dataset",
                    "testClass": "com.xceptance.neodymium.aura.sandbox.Aura_tests_stokke_stokkeOrderPayPalTest_yml_Test",
                    "testMethod": "executeYamlTest",
                    "title": "Order DE Product",
                    "browser": "Chrome_1500x1000",
                    "status": "failed",
                    "blocks": { "steps": [{ "name": "Step DE 1" }, { "name": "Step DE 2" }] }
                }
                """;
            Files.writeString(testFolder.resolve("console-execution-2.json"), exec2Json);

            // 1. Resolve Dataset B by simple class name key (must NOT be hijacked by File 1)
            final TestExecutionDto dtoB = dataService.getExecutionDetails(
                runId,
                parentFolderName + "#executeYamlTest#Order DE Product#Chrome_1500x1000"
            );
            Assertions.assertNotNull(dtoB, "Dataset B must be resolved");
            Assertions.assertEquals("Order DE Product", dtoB.getTitle(), "Dataset B must not return Dataset A");
            Assertions.assertEquals("failed", dtoB.getStatus());
            Assertions.assertEquals(2, dtoB.getBlocks().path("steps").size());

            // 2. Resolve Dataset A by simple class name key
            final TestExecutionDto dtoA = dataService.getExecutionDetails(
                runId,
                parentFolderName + "#executeYamlTest#Order US Product#Chrome_1500x1000"
            );
            Assertions.assertNotNull(dtoA, "Dataset A must be resolved");
            Assertions.assertEquals("Order US Product", dtoA.getTitle(), "Dataset A must return Dataset A");
            Assertions.assertEquals("passed-clean", dtoA.getStatus());
            Assertions.assertEquals(1, dtoA.getBlocks().path("steps").size());

            // 3. In multi-file folder, an arbitrary unknown postfix must NOT greedily match File 1
            final TestExecutionDto dtoAmbiguous = dataService.getExecutionDetails(
                runId,
                parentFolderName + "#arbitrary-unknown-id"
            );
            Assertions.assertNull(dtoAmbiguous, "Ambiguous prefix on multi-dataset folder must not hijack first file");
        }
        finally
        {
            deleteRecursively(runDir);
        }
    }

    @Test
    public void testFuzzyFolderFallbackDoesNotHijackOtherDatasetOfSameClass() throws IOException
    {
        final String runId = "run-fuzzy-no-hijack-test";
        final Path runDir = Paths.get("storage", "runs", runId);
        final Path testFolder = runDir.resolve("OrderTest");

        try
        {
            Files.createDirectories(testFolder);
            // Only dataset A has been written so far (live run); dataset B is still pending.
            Files.writeString(testFolder.resolve("console-execution-1.json"), """
                {
                    "id": "order-us-dataset",
                    "testClass": "com.example.OrderTest",
                    "testMethod": "order",
                    "title": "Order US",
                    "browser": "Chrome",
                    "status": "running"
                }
                """);

            Assertions.assertNull(dataService.getExecutionDetails(runId, "OrderTest#order#Order DE#Chrome#order-de-dataset"),
                                  "A pending dataset must not be answered with the data of another dataset");
        }
        finally
        {
            deleteRecursively(runDir);
        }
    }

    @Test
    public void testSimpleClassKeyCollisionAcrossPackagesIsNotGuessed() throws IOException
    {
        final String runId = "run-simple-key-collision-test";
        final Path runDir = Paths.get("storage", "runs", runId);

        try
        {
            for (final String pkg : new String[]
            {
                "a", "b"
            })
            {
                final Path folder = runDir.resolve("com_" + pkg + "_LoginTest");
                Files.createDirectories(folder);
                Files.writeString(folder.resolve("console-execution-1.json"), """
                    {
                        "id": "exec-%s",
                        "testClass": "com.%s.LoginTest",
                        "testMethod": "login",
                        "title": "Default",
                        "browser": "Chrome",
                        "status": "passed-clean"
                    }
                    """.formatted(pkg, pkg));
            }

            Assertions.assertNull(dataService.getExecutionDetails(runId, "LoginTest#login#Default#Chrome"),
                                  "Ambiguous simple-class key must not resolve to an arbitrary class");
            final TestExecutionDto exact = dataService.getExecutionDetails(runId, "com.b.LoginTest#login#Default#Chrome");
            Assertions.assertNotNull(exact, "Fully qualified key stays unambiguous");
            Assertions.assertEquals("exec-b", exact.getId());
        }
        finally
        {
            deleteRecursively(runDir);
        }
    }

    @Test
    public void testExecutionDetailsResolvesViaRunJsonFallbackWithSimpleKey() throws IOException

    {
        final String runId = "run-fallback-simple-key-test";
        final Path runDir = Paths.get("storage", "runs", runId);

        try
        {
            Files.createDirectories(runDir);

            final String runJsonContent = """
                {
                    "runId": "run-fallback-simple-key-test",
                    "executionMetrics": {
                        "com.xceptance.neodymium.aura.sandbox.PayPalTest#testOrderPayPal#default#Chrome": {
                            "id": "paypal-exec-1",
                            "testClass": "com.xceptance.neodymium.aura.sandbox.PayPalTest",
                            "testMethod": "testOrderPayPal",
                            "title": "PayPal Payment Test",
                            "browser": "Chrome",
                            "status": "passed-clean",
                            "blocks": {
                                "steps": [
                                    { "name": "Log in to PayPal", "status": "PASSED" },
                                    { "name": "Approve Payment", "status": "PASSED" }
                                ]
                            }
                        }
                    },
                    "executions": [
                        {
                            "id": "checkout-exec-2",
                            "testClass": "com.xceptance.neodymium.aura.sandbox.CheckoutTest",
                            "testMethod": "testCheckout",
                            "title": "Guest Checkout",
                            "browser": "Firefox",
                            "status": "failed",
                            "blocks": {
                                "steps": [
                                    { "name": "Enter Address", "status": "PASSED" },
                                    { "name": "Place Order", "status": "FAILED" }
                                ]
                            }
                        }
                    ]
                }
                """;
            Files.writeString(runDir.resolve("run.json"), runJsonContent);

            // 1. Resolve from executionMetrics using simple class key
            final TestExecutionDto dtoFromMetrics = dataService.getExecutionDetails(
                runId,
                "PayPalTest#testOrderPayPal#default#Chrome"
            );
            Assertions.assertNotNull(dtoFromMetrics, "Execution details must be resolved from executionMetrics via simple key");
            Assertions.assertEquals("paypal-exec-1", dtoFromMetrics.getId());
            Assertions.assertEquals("PayPal Payment Test", dtoFromMetrics.getTitle());
            Assertions.assertEquals(2, dtoFromMetrics.getBlocks().path("steps").size());

            // 2. Resolve from executions array using simple class key
            final TestExecutionDto dtoFromArray = dataService.getExecutionDetails(
                runId,
                "CheckoutTest#testCheckout#Guest Checkout#Firefox"
            );
            Assertions.assertNotNull(dtoFromArray, "Execution details must be resolved from executions array via simple key");
            Assertions.assertEquals("checkout-exec-2", dtoFromArray.getId());
            Assertions.assertEquals("Guest Checkout", dtoFromArray.getTitle());
            Assertions.assertEquals(2, dtoFromArray.getBlocks().path("steps").size());
        }
        finally
        {
            deleteRecursively(runDir);
        }
    }

    @Test
    public void testExecutionDetailsResolvesViaLiveBufferWithSimpleKey()
    {
        final String runId = "run-live-buffer-simple-key-test";

        final Map<String, Object> payload = new HashMap<>();
        payload.put("id", "live-exec-1");
        payload.put("testClass", "com.xceptance.neodymium.aura.sandbox.OrderLiveTest");
        payload.put("testMethod", "testSubmitOrder");
        payload.put("title", "Live Submit Order");
        payload.put("browser", "Chrome_1920x1080");
        payload.put("status", "running");
        payload.put("blocks", Map.of("steps", List.of(
            Map.of("name", "Submit Step 1", "status", "PASSED")
        )));

        dataService.ingestExecution(runId, payload);

        // Resolve via simple class key
        final TestExecutionDto dto = dataService.getExecutionDetails(
            runId,
            "OrderLiveTest#testSubmitOrder#Live Submit Order#Chrome_1920x1080"
        );
        Assertions.assertNotNull(dto, "Execution details must be resolved from live buffer via simple key");
        Assertions.assertEquals("live-exec-1", dto.getId());
        Assertions.assertEquals("Live Submit Order", dto.getTitle());
        Assertions.assertEquals("running", dto.getStatus());
        Assertions.assertEquals(1, dto.getBlocks().path("steps").size());
    }

    @Test
    public void testExecutionDetailsWithInteractiveSessionDoesNotHijackUnrelatedExecutions()
    {
        final AuraInteractiveService mockInteractiveService = new AuraInteractiveService();
        final InteractiveConsoleEngine mockEngine = Mockito.mock(InteractiveConsoleEngine.class);
        final String activeStateJson = """
            {
                "runId": "run-interactive-test",
                "id": "interactive-active-exec",
                "testClass": "com.xceptance.neodymium.aura.sandbox.LiveLoginTest",
                "testMethod": "testLogin",
                "title": "Default",
                "browser": "Chrome",
                "status": "running"
            }
            """;
        Mockito.when(mockEngine.getCurrentStateJson()).thenReturn(activeStateJson);
        mockInteractiveService.setCurrentConsoleEngine(mockEngine);

        final AuraReportDataService service = new AuraReportDataService(
            runRepository,
            null,
            variationRepository,
            bugRepository,
            storageService,
            null,
            mockInteractiveService
        );

        // 1. Matches when querying the interactive test directly via simple execution key
        final TestExecutionDto matchedDto = service.getExecutionDetails("run-interactive-test", "LiveLoginTest#testLogin#Default#Chrome");
        Assertions.assertNotNull(matchedDto, "Interactive execution must match its own composite key");
        Assertions.assertEquals("interactive-active-exec", matchedDto.getId());

        // 2. Matches when querying a generic console execution id
        final TestExecutionDto genericDto = service.getExecutionDetails("run-interactive-test", "console-execution");
        Assertions.assertNotNull(genericDto, "Generic execution ID must resolve to active interactive session");

        // 3. Does NOT hijack an unrelated execution ID when the session belongs to a different test
        final TestExecutionDto unrelatedDto = service.getExecutionDetails("run-interactive-test", "UnrelatedOrderTest#testOrder#Default#Chrome");
        Assertions.assertNull(unrelatedDto, "Interactive session must NOT match an unrelated execution key");
    }

    @Test
    public void testRunningExecutionClassification() throws IOException
    {
        final String runningRunId = "run-sync-running-test";
        final Path runDir = Paths.get("storage", "runs", runningRunId);
        final Path execDir = runDir.resolve("Checkout").resolve("CheckoutProcessTest");

        try
        {
            Files.createDirectories(execDir);

            final String execJson = """
                {
                    "id": "exec-running-1",
                    "title": "CheckoutProcessTest [US Chrome]",
                    "testClass": "CheckoutProcessTest",
                    "status": "running",
                    "location": "US",
                    "browser": "Chrome"
                }
                """;
            Files.writeString(execDir.resolve("exec-1.json"), execJson);

            syncService.importOrUpdateRunReport(runningRunId);

            final Optional<TestRunEntity> runOpt = runRepository.findById(runningRunId);
            Assertions.assertTrue(runOpt.isPresent(), "Running run should be ingested into database");
            final TestRunEntity entity = runOpt.get();
            Assertions.assertEquals("IN_PROGRESS", entity.getStatus(), "Run status should be IN_PROGRESS");
            Assertions.assertEquals(1, entity.getRunningCountSafe(), "runningCount should be 1");
            Assertions.assertEquals(0, entity.getFailedUnknownCount(), "failedUnknownCount should be 0");
        }
        finally
        {
            deleteRecursively(runDir);
            runRepository.findById(runningRunId).ifPresent(runRepository::delete);
        }
    }

    @Test
    public void testLiveBufferOverridesIntermediateDiskRunningStatusInGetRunReport() throws IOException
    {
        final String runId = "run-live-override-test";
        final Path runDir = Paths.get("storage", "runs", runId);
        final Path execDir = runDir.resolve("Checkout").resolve("CheckoutProcessTest");

        try
        {
            Files.createDirectories(execDir);

            final String intermediateExecJson = """
                {
                    "id": "exec-running-override-1",
                    "title": "Dataset US",
                    "testClass": "CheckoutProcessTest",
                    "testMethod": "testCheckout",
                    "status": "running",
                    "location": "US",
                    "browser": "Chrome"
                }
                """;
            Files.writeString(execDir.resolve("console-execution-1.json"), intermediateExecJson);

            final TestRunEntity initialRun = new TestRunEntity(
                runId, "Default", "IN_PROGRESS", "Aura", "Default", "US", "Chrome", "Recently", System.currentTimeMillis()
            );
            runRepository.save(initialRun);

            final Map<String, Object> finishedPayload = new HashMap<>();
            finishedPayload.put("id", "exec-running-override-1");
            finishedPayload.put("testClass", "CheckoutProcessTest");
            finishedPayload.put("testMethod", "testCheckout");
            finishedPayload.put("title", "Dataset US");
            finishedPayload.put("location", "US");
            finishedPayload.put("browser", "Chrome");
            finishedPayload.put("status", "passed");

            dataService.ingestExecution(runId, finishedPayload);

            final RunReportDto report = dataService.getRunReport(runId);
            Assertions.assertNotNull(report, "RunReportDto must not be null");
            Assertions.assertEquals(1, report.getTotalCount(), "Report total count should be 1");
            Assertions.assertEquals(1, report.getPassCount(), "Report pass count should be 1");
            Assertions.assertEquals(0, report.getRunningCount(), "Report running count should be 0");

            final List<TestExecutionDto> executions = report.getExecutions();
            Assertions.assertEquals(1, executions.size(), "Executions list should have 1 item");
            Assertions.assertEquals("passed-clean", executions.get(0).getStatus(), "Live completed status must override disk running status");
        }
        finally
        {
            deleteRecursively(runDir);
            runRepository.findById(runId).ifPresent(runRepository::delete);
        }
    }

    @Test
    public void testIngestExecutionMaintainsAndClearsRunningCount()
    {
        final String runId = "run-ingest-running-count-test";
        try
        {
            final TestRunEntity initialRun = new TestRunEntity(
                runId, "Default", "IN_PROGRESS", "Aura", "Default", "US", "Chrome", "Recently", System.currentTimeMillis()
            );
            runRepository.save(initialRun);

            final Map<String, Object> runningPayload = new HashMap<>();
            runningPayload.put("id", "exec-step-1");
            runningPayload.put("testClass", "PaymentProcessTest");
            runningPayload.put("testMethod", "testPay");
            runningPayload.put("title", "Dataset 1");
            runningPayload.put("location", "US");
            runningPayload.put("browser", "Chrome");
            runningPayload.put("status", "running");

            dataService.ingestExecution(runId, runningPayload);

            final TestRunEntity runningEntity = runRepository.findById(runId).orElseThrow();
            Assertions.assertEquals(1, runningEntity.getRunningCountSafe(), "Running count must be 1 while test is running");
            Assertions.assertEquals(0, runningEntity.getPassedCount(), "Passed count must be 0 while test is running");

            final Map<String, Object> passedPayload = new HashMap<>(runningPayload);
            passedPayload.put("status", "passed");

            dataService.ingestExecution(runId, passedPayload);

            final TestRunEntity completedEntity = runRepository.findById(runId).orElseThrow();
            Assertions.assertEquals(0, completedEntity.getRunningCountSafe(), "Running count must drop to 0 after test passes");
            Assertions.assertEquals(1, completedEntity.getPassedCount(), "Passed count must increase to 1");

            dataService.finishRun(runId);

            final TestRunEntity finishedEntity = runRepository.findById(runId).orElseThrow();
            Assertions.assertEquals(0, finishedEntity.getRunningCountSafe(), "Running count must be 0 after finishRun");
            Assertions.assertEquals("COMPLETED", finishedEntity.getStatus(), "Run status must be COMPLETED");
        }
        finally
        {
            runRepository.findById(runId).ifPresent(runRepository::delete);
        }
    }

    @Test
    public void testPopulateRunTagsSuppressesSyntheticBatchNames()
    {
        final TestRunEntity runWithDefault = new TestRunEntity();
        runWithDefault.setId("run_test_default");
        runWithDefault.setBatchName("Default");

        final TestRunEntity runWithSuite = new TestRunEntity();
        runWithSuite.setId("run_test_suite");
        runWithSuite.setBatchName("Suite");

        final TestRunEntity runWithQueue = new TestRunEntity();
        runWithQueue.setId("run_test_queue");
        runWithQueue.setBatchName("Queue");

        final TestRunEntity runWithRealBatch = new TestRunEntity();
        runWithRealBatch.setId("run_test_real");
        runWithRealBatch.setBatchName("BillingRegression");

        final List<TestRunEntity> runs = List.of(runWithDefault, runWithSuite, runWithQueue, runWithRealBatch);
        dataService.populateRunTags(runs);

        Assertions.assertTrue(
            runWithDefault.getTags() == null || runWithDefault.getTags().stream().noneMatch(t -> "Suite".equalsIgnoreCase(t.getTagKey())),
            "Run with batch name 'Default' must not receive synthetic Suite tag"
        );
        Assertions.assertTrue(
            runWithSuite.getTags() == null || runWithSuite.getTags().stream().noneMatch(t -> "Suite".equalsIgnoreCase(t.getTagKey())),
            "Run with batch name 'Suite' must not receive synthetic Suite tag"
        );
        Assertions.assertTrue(
            runWithQueue.getTags() == null || runWithQueue.getTags().stream().noneMatch(t -> "Suite".equalsIgnoreCase(t.getTagKey())),
            "Run with batch name 'Queue' must not receive synthetic Suite tag"
        );

        Assertions.assertNotNull(runWithRealBatch.getTags());
        Assertions.assertTrue(
            runWithRealBatch.getTags().stream().anyMatch(t -> "Suite".equalsIgnoreCase(t.getTagKey()) && "BillingRegression".equals(t.getTagValue())),
            "Run with legitimate batch name 'BillingRegression' must receive synthetic Suite tag"
        );

        final TestRunTagEntity legitTag = new TestRunTagEntity("run_1", "Suite", "BillingRegression");
        final TestRunTagEntity defaultTag = new TestRunTagEntity("run_2", "Suite", "Default");
        final TestRunTagEntity suiteTag = new TestRunTagEntity("run_3", "Suite", "Suite");
        final TestRunTagEntity queueTag = new TestRunTagEntity("run_4", "Batch", "Queue");

        Assertions.assertTrue(AuraReportDataService.isRealBatch("BillingRegression"));
        Assertions.assertFalse(AuraReportDataService.isRealBatch("Default"));
        Assertions.assertFalse(AuraReportDataService.isRealBatch("Suite"));
        Assertions.assertFalse(AuraReportDataService.isRealBatch("Queue"));

        Assertions.assertTrue(AuraReportDataService.isLegitimateTag(legitTag));
        Assertions.assertFalse(AuraReportDataService.isLegitimateTag(defaultTag));
        Assertions.assertFalse(AuraReportDataService.isLegitimateTag(suiteTag));
        Assertions.assertFalse(AuraReportDataService.isLegitimateTag(queueTag));
    }
}


