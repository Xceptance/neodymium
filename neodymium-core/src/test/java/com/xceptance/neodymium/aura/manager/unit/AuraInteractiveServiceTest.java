/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xceptance.neodymium.aura.manager.unit;

import com.xceptance.neodymium.ai.console.InteractiveConsoleEngine;
import com.xceptance.neodymium.ai.console.InteractiveConsoleEngine.ActionResult;
import com.xceptance.neodymium.aura.AuraInteractiveService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.neodymium.ai.config.AiConfiguration;

/**
 * Unit tests for {@link AuraInteractiveService} verifying state management,
 * action synchronization, execution indices, and screenshot traversal safety.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Tag("unit")
@Tag("aura-manager")
public final class AuraInteractiveServiceTest
{
    private AuraInteractiveService interactiveService;

    @BeforeEach
    public void setUp()
    {
        interactiveService = new AuraInteractiveService();
    }

    @Test
    public void testThemePreferenceManagement()
    {
        Assertions.assertEquals("system", interactiveService.getActiveTheme());

        interactiveService.setActiveTheme("dark");
        Assertions.assertEquals("dark", interactiveService.getActiveTheme());

        interactiveService.setActiveTheme("light");
        Assertions.assertEquals("light", interactiveService.getActiveTheme());
    }

    @Test
    public void testExecutionIndexManagement()
    {
        interactiveService.resetExecutionIndexes();

        final int idx1 = interactiveService.getExecutionIndex("TestKey1");
        final int idx2 = interactiveService.getExecutionIndex("TestKey2");
        final int idx1Again = interactiveService.getExecutionIndex("TestKey1");

        Assertions.assertEquals(1, idx1);
        Assertions.assertEquals(2, idx2);
        Assertions.assertEquals(1, idx1Again);

        interactiveService.resetExecutionIndexes();
        final int idxAfterReset = interactiveService.getExecutionIndex("TestKey1");
        Assertions.assertEquals(1, idxAfterReset);
    }

    @Test
    public void testPushStateStatus()
    {
        final String validStateJson = "{\"runId\":\"run_123\",\"testName\":\"TestLogin\",\"status\":\"running\"}";

        final String okStatus = interactiveService.pushState(validStateJson, false);
        Assertions.assertEquals("ok", okStatus);
        Assertions.assertEquals("run_123", interactiveService.getLastProcessedRunId());

        final String stoppedStatus = interactiveService.pushState(validStateJson, true);
        Assertions.assertEquals("stopped", stoppedStatus);
    }

    @Test
    public void testWaitForActionWhenManuallyStopped() throws InterruptedException
    {
        final String response = interactiveService.waitForAction("pause_999", true);
        Assertions.assertNotNull(response);
        Assertions.assertTrue(response.contains("\"action\":\"ABORT\""), "Should return ABORT action payload when stopped");
    }

    @Test
    public void testSubmitActionValidation()
    {
        // Engine is initialized when needed or created
        interactiveService.getOrCreateConsoleEngine();

        final ActionResult invalidJsonResult = interactiveService.submitAction("not-valid-json");
        Assertions.assertEquals(400, invalidJsonResult.statusCode());
        Assertions.assertTrue(invalidJsonResult.responseBody().contains("Invalid JSON body"));
    }

    @Test
    public void testGetScreenshotFileDirectoryTraversalProtection()
    {
        Assertions.assertTrue(interactiveService.getScreenshotFile(null, null).isEmpty());
        Assertions.assertTrue(interactiveService.getScreenshotFile("../secret.png", null).isEmpty());
        Assertions.assertTrue(interactiveService.getScreenshotFile("sub/file.png", null).isEmpty());
        Assertions.assertTrue(interactiveService.getScreenshotFile("sub\\file.png", null).isEmpty());
    }

    @Test
    public void testGetScreenshotFileFromActiveDirectory(@TempDir final File tempDir) throws IOException
    {
        final File testScreenshot = new File(tempDir, "sample.png");
        Files.writeString(testScreenshot.toPath(), "test-image-data");

        System.setProperty("neodymium.ai.console.screenshotsDir", tempDir.getAbsolutePath());
        try
        {
            final Optional<File> resolved = interactiveService.getScreenshotFile("sample.png", null);
            Assertions.assertTrue(resolved.isPresent());
            Assertions.assertEquals(testScreenshot.getCanonicalPath(), resolved.get().getCanonicalPath());

            final Optional<File> notFound = interactiveService.getScreenshotFile("missing.png", null);
            Assertions.assertTrue(notFound.isEmpty());
        }
        finally
        {
            System.clearProperty("neodymium.ai.console.screenshotsDir");
        }
    }

    @Test
    public void testPushStateAcrossDistinctRunIdsIsolatesRunFoldersAndPreservesEarlierRun(@TempDir final Path tempDir) throws IOException
    {
        final Path allureDir = tempDir.resolve("allure-results");
        final Path logsDir = tempDir.resolve("console-logs");
        final Path reportsDir = tempDir.resolve("ai-reports");
        final Path storageDir = tempDir.resolve("storage-runs");

        final String origAllure = System.getProperty("allure.results.directory");
        final String origEnabled = System.getProperty("neodymium.ai.consoleExecutionLogs");
        final String origLogsDir = System.getProperty("neodymium.ai.consoleExecutionLogs.directory");
        final String origReportsDir = System.getProperty("neodymium.ai.report.disk.directory");
        final String origStorageDir = System.getProperty("neodymium.ai.storageRuns.directory");

        System.setProperty("allure.results.directory", allureDir.toString());
        System.setProperty("neodymium.ai.consoleExecutionLogs", "true");
        System.setProperty("neodymium.ai.consoleExecutionLogs.directory", logsDir.toString());
        System.setProperty("neodymium.ai.report.disk.directory", reportsDir.toString());
        System.setProperty("neodymium.ai.storageRuns.directory", storageDir.toString());
        InteractiveConsoleEngine.resetExecutionIndexes();
        AiConfiguration.resetInstance();

        try
        {
            final String runAState = "{"
                + "\"runId\":\"run_20261124_200001\","
                + "\"testClass\":\"com.xceptance.WikipediaSearchTest\","
                + "\"testName\":\"WikipediaSearchTest :: search · ds1\","
                + "\"status\":\"healed\","
                + "\"llmCalls\":[{\"id\":\"heal-call-a\"}],"
                + "\"blocks\":{\"steps\":[{\"instruction\":\"Type query\",\"status\":\"healed\",\"llmCalls\":[{\"id\":\"heal-call-a\"}]}]}"
                + "}";
            final String runBState = "{"
                + "\"runId\":\"run_20261124_200002\","
                + "\"testClass\":\"com.xceptance.WikipediaSearchTest\","
                + "\"testName\":\"WikipediaSearchTest :: search · ds1\","
                + "\"status\":\"healed\","
                + "\"llmCalls\":[],"
                + "\"blocks\":{\"steps\":[{\"instruction\":\"Type query\",\"status\":\"healed\",\"llmCalls\":[]}]}"
                + "}";

            Assertions.assertEquals("ok", interactiveService.pushState(runAState, false));
            Assertions.assertEquals("ok", interactiveService.pushState(runBState, false));
            Assertions.assertEquals("run_20261124_200002", interactiveService.getLastProcessedRunId());

            for (final Path root : List.of(storageDir, logsDir, reportsDir))
            {
                final Path runAFile = root.resolve("run_20261124_200001/WikipediaSearchTest/console-execution-1.json");
                final Path runBFile = root.resolve("run_20261124_200002/WikipediaSearchTest/console-execution-1.json");
                Assertions.assertTrue(Files.exists(runAFile), "Run A JSON must remain in its own folder under " + root);
                Assertions.assertTrue(Files.exists(runBFile), "Run B JSON must be written to its own folder under " + root);

                final String runAContent = Files.readString(runAFile);
                final String runBContent = Files.readString(runBFile);
                Assertions.assertTrue(runAContent.contains("heal-call-a"), "Run A LLM call must remain intact");
                Assertions.assertFalse(runAContent.contains("run_20261124_200002"), "Run A file must not be overwritten by Run B");
                Assertions.assertTrue(runBContent.contains("run_20261124_200002"), "Run B file must contain Run B state");
                Assertions.assertFalse(runBContent.contains("heal-call-a"), "Run B file must not inherit Run A LLM calls");
            }
        }
        finally
        {
            InteractiveConsoleEngine.resetExecutionIndexes();
            restoreOrClearProperty("allure.results.directory", origAllure);
            restoreOrClearProperty("neodymium.ai.consoleExecutionLogs", origEnabled);
            restoreOrClearProperty("neodymium.ai.consoleExecutionLogs.directory", origLogsDir);
            restoreOrClearProperty("neodymium.ai.report.disk.directory", origReportsDir);
            restoreOrClearProperty("neodymium.ai.storageRuns.directory", origStorageDir);
            AiConfiguration.resetInstance();
        }
    }

    private static void restoreOrClearProperty(final String key, final String originalValue)
    {
        if (originalValue != null)
        {
            System.setProperty(key, originalValue);
        }
        else
        {
            System.clearProperty(key);
        }
    }
}
