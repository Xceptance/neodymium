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

import com.xceptance.aura.report.entity.TestRunEntity;
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
        Assertions.assertFalse(Files.exists(runJsonPath), "run.json should not exist prior to read/generate");

        final Optional<String> runJsonOpt = storageService.readRunJson(TEST_RUN_ID);
        Assertions.assertTrue(runJsonOpt.isPresent(), "readRunJson should dynamically generate and return run.json");
        Assertions.assertTrue(Files.exists(runJsonPath), "run.json file should have been generated on disk");
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
}
