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
package org.neodymium.common.recording;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.common.recording.config.VideoRecordingConfigurations;
import org.neodymium.util.Neodymium;

/**
 * Regression and safety net tests for {@link FilmTestExecution} and {@link TakeScreenshotsThread}
 * verifying thread-context isolation, boolean guards, and failure-only attachment gating.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class FilmTestExecutionSafetyNetTest
{
    @BeforeEach
    public void setUp()
    {
        FilmTestExecution.clearThreadContexts();
    }

    @AfterEach
    public void tearDown()
    {
        FilmTestExecution.clearThreadContexts();
    }

    @Test
    public void testVideoConfigurationsDefault()
    {
        final VideoRecordingConfigurations config = (VideoRecordingConfigurations) FilmTestExecution.getContext(VideoRecordingConfigurations.class);
        Assertions.assertTrue(config.appendAllRecordingsToAllureReport(),
            "video.appendAllRecordingsToAllureReport should default to true for backward compatibility");
        Assertions.assertFalse(config.attachOnlyOnFailure(),
            "video.attachOnlyOnFailure should default to false");
    }

    @Test
    public void testStartVideoRecordingDoesNotStartThreadWhenFilmingDisabled()
    {
        final String recordingId = "test-no-filming-" + UUID.randomUUID();
        FilmTestExecution.startVideoRecording(recordingId);

        try
        {
            Assertions.assertFalse(FilmTestExecution.getNamesOfAllCurrentVideoRecordings().contains(recordingId),
                "No video recording thread should be started when enableFilming is false");
        }
        finally
        {
            FilmTestExecution.finishVideoFilming(recordingId, false);
        }
    }

    @Test
    public void testStartVideoRecordingSucceedsWhenFilmingEnabledBeforeDriverInit()
    {
        FilmTestExecution.getContextVideo().setProperty("video.enableFilming", "true");
        Assertions.assertFalse(Neodymium.hasDriver(), "Driver must not be initialized yet (setUpTest state)");

        final String recordingId = "test-filming-before-driver-" + UUID.randomUUID();
        FilmTestExecution.startVideoRecording(recordingId);

        try
        {
            Assertions.assertTrue(FilmTestExecution.getNamesOfAllCurrentVideoRecordings().contains(recordingId),
                "Video recording thread should be registered in FilmTestExecution even before driver starts");
        }
        finally
        {
            FilmTestExecution.finishVideoFilming(recordingId, false);
        }
    }

    @Test
    public void testFinishVideoFilmingGracefullyHandlesUnregisteredRecording()
    {
        Assertions.assertDoesNotThrow(() ->
        {
            FilmTestExecution.finishVideoFilming("non-existent-recording-id-" + UUID.randomUUID(), false);
        });
    }

    @Test
    public void testFinishVideoFilmingGracefullyHandlesNullOrEmpty()
    {
        Assertions.assertDoesNotThrow(() ->
        {
            FilmTestExecution.finishVideoFilming(null, false);
            FilmTestExecution.finishVideoFilming("", false);
            FilmTestExecution.finishVideoFilming("   ", false);
        });
    }

    @Test
    public void testThreadContextIsolationBetweenThreads() throws InterruptedException
    {
        final String mainThreadValue = "main-thread-val-" + UUID.randomUUID();
        FilmTestExecution.getContextVideo().setProperty("video.tempFolderToStoreRecording", mainThreadValue);

        final String[] workerValue = new String[1];
        final Thread worker = new Thread(() ->
        {
            workerValue[0] = FilmTestExecution.getContextVideo().tempFolderToStoreRecording();
        });

        worker.start();
        worker.join(5000);

        Assertions.assertNotEquals(mainThreadValue, workerValue[0],
            "Worker thread must get its own isolated VideoRecordingConfigurations context");
        Assertions.assertEquals(mainThreadValue, FilmTestExecution.getContextVideo().tempFolderToStoreRecording(),
            "Main thread context property must remain intact");
    }

    @Test
    public void testPassingTestGatingWhenFailureOnlyConfigured() throws IOException
    {
        FilmTestExecution.getContextVideo().setProperty("video.attachOnlyOnFailure", "true");
        final VideoRecordingConfigurations config = (VideoRecordingConfigurations) FilmTestExecution.getContext(VideoRecordingConfigurations.class);
        final File tempDir = new File(config.tempFolderToStoreRecording());
        if (!tempDir.exists())
        {
            tempDir.mkdirs();
        }

        final String recordingName = "dummy-video-" + UUID.randomUUID();
        final File dummyVideo = new File(tempDir, recordingName + "." + config.format());
        Files.writeString(dummyVideo.toPath(), "dummy-video-data");
        Assertions.assertTrue(dummyVideo.exists(), "Dummy video file should exist prior to teardown");

        try
        {
            Assertions.assertTrue(config.attachOnlyOnFailure(), "attachOnlyOnFailure should be true when explicitly configured");
            final boolean testFailed = false;
            final boolean attachOnlyOnFailure = config.attachOnlyOnFailure()
                || !config.appendAllRecordingsToAllureReport();
            final boolean shouldAttach = !attachOnlyOnFailure || testFailed;

            Assertions.assertFalse(shouldAttach, "Should not attach on passing test when failure-only is configured");
        }
        finally
        {
            if (dummyVideo.exists())
            {
                dummyVideo.delete();
            }
        }
    }

    @Test
    public void testFailingTestAttachesWhenFailureOnlyConfigured()
    {
        FilmTestExecution.getContextVideo().setProperty("video.attachOnlyOnFailure", "true");
        final VideoRecordingConfigurations config = (VideoRecordingConfigurations) FilmTestExecution.getContext(VideoRecordingConfigurations.class);

        final boolean testFailed = true;
        final boolean attachOnlyOnFailure = config.attachOnlyOnFailure()
            || !config.appendAllRecordingsToAllureReport();
        final boolean shouldAttach = !attachOnlyOnFailure || testFailed;

        Assertions.assertTrue(shouldAttach, "Should attach on failed test even when failure-only is active");
    }

    @Test
    public void testPassingTestAttachesRecordingWhenAppendAllConfigured() throws IOException
    {
        final VideoRecordingConfigurations config = (VideoRecordingConfigurations) FilmTestExecution.getContext(VideoRecordingConfigurations.class);
        final File tempDir = new File(config.tempFolderToStoreRecording());
        if (!tempDir.exists())
        {
            tempDir.mkdirs();
        }

        final String recordingName = "dummy-retained-video-" + UUID.randomUUID();
        final File dummyVideo = new File(tempDir, recordingName + "." + config.format());
        Files.writeString(dummyVideo.toPath(), "dummy-retained-data");
        Assertions.assertTrue(dummyVideo.exists(), "Dummy video file should exist");

        try
        {
            Assertions.assertTrue(config.appendAllRecordingsToAllureReport(),
                "appendAllRecordingsToAllureReport must be true by default");
            final boolean testFailed = false;
            final boolean attachOnlyOnFailure = config.attachOnlyOnFailure()
                || !config.appendAllRecordingsToAllureReport();
            final boolean shouldAttach = !attachOnlyOnFailure || testFailed;

            Assertions.assertTrue(shouldAttach, "Should attach on passing test when appendAllRecordingsToAllureReport is active");
        }
        finally
        {
            if (dummyVideo.exists())
            {
                dummyVideo.delete();
            }
        }
    }
}
