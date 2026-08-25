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
        Assertions.assertFalse(Files.exists(runJsonPath), "run.json file should NOT be created on disk");
        Assertions.assertTrue(runJsonOpt.get().contains("exec-999-1"));
    }

    @Test
    public void testUnassignedCategoryDefaultsToBrowsingAndMovesFile() throws IOException
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
            Assertions.assertTrue(runJsonOpt.get().contains("Browsing (default)"), "Expected category/areaName to be assigned to Browsing (default)");

            final Path expectedMovedPath = runDir.resolve("Browsing (default)").resolve("UnassignedTest").resolve("unassigned-exec.json");
            Assertions.assertTrue(Files.exists(expectedMovedPath), "Expected execution JSON file to be moved to Browsing (default)/UnassignedTest/unassigned-exec.json");
            Assertions.assertFalse(Files.exists(flatExecFile), "Old flat file should no longer exist at run root");
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
            Assertions.assertTrue(runJsonOpt.get().contains("Browsing (default)"), "Expected category/areaName to be assigned to Browsing (default)");

            final Path expectedMovedPath = runDir.resolve("Browsing (default)").resolve("GoogleTest").resolve("console-execution-1.json");
            Assertions.assertTrue(Files.exists(expectedMovedPath), "Expected execution JSON file to be moved into Browsing (default)/GoogleTest/console-execution-1.json");
            Assertions.assertFalse(Files.exists(execFile), "Old 2-level execution file should no longer exist at GoogleTest/console-execution-1.json");
            Assertions.assertFalse(Files.exists(classDir), "Old class folder should be removed when empty");
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
    public void testEarliestStartTimeInRunJson() throws IOException
    {
        final String runId = "run-earliest-time-test";
        final Path runDir = Paths.get("storage", "runs", runId);
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
            final Optional<String> runJsonOpt = storageService.readRunJson(runId);
            Assertions.assertTrue(runJsonOpt.isPresent(), "readRunJson should return generated run.json");

            final String runJson = runJsonOpt.get();
            Assertions.assertTrue(runJson.contains("2026-08-20 06:15:00"),
                "Expected run.json to contain the earliest start time '2026-08-20 06:15:00' instead of later time");
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
            final Optional<String> runJsonOpt = storageService.readRunJson(runId);
            Assertions.assertTrue(runJsonOpt.isPresent(), "readRunJson should return generated run.json");

            final String runJson = runJsonOpt.get();
            Assertions.assertTrue(runJson.contains("\"locales\" : [ \"DE\", \"FR\" ]") || (runJson.contains("\"DE\"") && runJson.contains("\"FR\"")),
                "Expected run.json to contain locales array with DE and FR");
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
}
