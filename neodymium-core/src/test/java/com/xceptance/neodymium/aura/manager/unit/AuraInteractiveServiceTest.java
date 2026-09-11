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
package com.xceptance.neodymium.aura.manager.unit;

import com.xceptance.neodymium.ai.console.InteractiveConsoleEngine.ActionResult;
import com.xceptance.neodymium.aura.AuraInteractiveService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
}
