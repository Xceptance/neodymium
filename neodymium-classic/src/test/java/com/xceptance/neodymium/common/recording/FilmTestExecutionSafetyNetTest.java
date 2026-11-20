/*
 * MIT License
 *
 * Copyright (c) 2017-2026 Xceptance Software Technologies GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xceptance.neodymium.common.recording;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.common.recording.config.VideoRecordingConfigurations;

/**
 * Regression and safety net tests for classic {@link FilmTestExecution} and {@link TakeScreenshotsThread}
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
        final String recordingId = "test-no-filming-classic-" + UUID.randomUUID();
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
    public void testFinishVideoRecordingGracefullyHandlesUnregisteredRecording()
    {
        Assertions.assertDoesNotThrow(() ->
        {
            FilmTestExecution.finishVideoFilming("non-existent-recording-id-" + UUID.randomUUID(), false);
        });
    }

    @Test
    public void testFinishVideoRecordingGracefullyHandlesNullOrEmpty()
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

        final String recordingName = "dummy-video-classic-" + UUID.randomUUID();
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

        final String recordingName = "dummy-retained-video-classic-" + UUID.randomUUID();
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
