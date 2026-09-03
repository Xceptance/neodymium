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
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xceptance.aura.report.dto.AreaSummaryDto;
import com.xceptance.aura.report.dto.RunReportDto;
import com.xceptance.aura.report.dto.TestClassSummaryDto;
import com.xceptance.aura.report.entity.TestBaseBugEntity;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.repository.TestBaseBugRepository;
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
    public void testSyncLocalRunStorageWithSampleRuns()
    {
        final int syncedCount = syncService.syncLocalRunStorage();
        Assertions.assertTrue(syncedCount > 0, "Expected at least 1 run report synced into DB");

        final var allRuns = runRepository.findAll();
        Assertions.assertFalse(allRuns.isEmpty(), "Expected runs to be synced in repository");
        final TestRunEntity firstRun = allRuns.get(0);
        Assertions.assertTrue(firstRun.getTotalTests() > 0, "Expected total tests to be greater than 0");
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

            Assertions.assertEquals("Browsing (default)", findAreaForClass(dataService.getRunReport(unassignedRunId), "UnassignedTest"),
                "Flat execution without a category should be grouped under the default category in the run report");

            final JsonNode enrichedExec = new ObjectMapper().readTree(flatExecFile.toFile());
            Assertions.assertEquals("Browsing (default)", enrichedExec.path("areaName").asText(),
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
            Assertions.assertEquals("Browsing (default)", metrics.fields().next().getValue().path("areaName").asText(),
                "Expected run.json executionMetrics to carry the default category for the uncategorized class");
            Assertions.assertEquals("GoogleTest", metrics.fields().next().getValue().path("testClass").asText(),
                "Expected run.json executionMetrics to map the execution to the class folder name");

            Assertions.assertEquals("Browsing (default)", findAreaForClass(dataService.getRunReport(twoLevelRunId), "GoogleTest"),
                "Class folder without a category should be grouped under the default category in the run report");
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
            Assertions.assertEquals("Checkout", findAreaForClass(report, "CheckoutProcessTest"),
                "Class folder inside a category folder should keep the category from its folder");
            Assertions.assertEquals("Browsing (default)", findAreaForClass(report, "StandaloneTest"),
                "Class folder without subfolders at the run root should be grouped under the default category");

            final JsonNode enrichedExec = new ObjectMapper().readTree(uncategorizedExecFile.toFile());
            Assertions.assertEquals("Browsing (default)", enrichedExec.path("areaName").asText(),
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

            final Optional<com.xceptance.aura.report.entity.TestRunEntity> entityOpt = runRepository.findById(runId);
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

            final Optional<com.xceptance.aura.report.entity.TestRunEntity> entityOpt = runRepository.findById(runId);
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
        final TestBaseBugEntity bug = new TestBaseBugEntity(varId, "BUG-SYNC-999", "Unknown", "Unknown", System.currentTimeMillis(), runId);
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

            final com.xceptance.aura.report.dto.TestExecutionDto exec3Details = dataService.getExecutionDetails(runId, "VerlaProgrammaticDemoTest#test3_AnnotationDrivenInlinePlaybookTextBlocks#Default#Chrome");
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
}

