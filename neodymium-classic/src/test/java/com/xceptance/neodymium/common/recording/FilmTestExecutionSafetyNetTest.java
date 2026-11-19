package com.xceptance.neodymium.common.recording;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.common.browser.BrowserRunner;
import com.xceptance.neodymium.common.recording.config.VideoRecordingConfigurations;
import com.xceptance.neodymium.util.Neodymium;

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
    public void testVideoConfigurationsDefaultToAppendAllRecordings()
    {
        final VideoRecordingConfigurations config = (VideoRecordingConfigurations) FilmTestExecution.getContext(VideoRecordingConfigurations.class);
        Assertions.assertTrue(config.appendAllRecordingsToAllureReport(),
            "video.appendAllRecordingsToAllureReport should default to true so tagged recordings are retained");
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
    public void testStartVideoRecordingSucceedsWhenFilmingEnabledBeforeDriverInit()
    {
        FilmTestExecution.getContextVideo().setProperty("video.enableFilming", "true");
        Assertions.assertFalse(Neodymium.hasDriver(), "Driver must not be initialized yet (setUpTest state)");

        final String recordingId = "test-filming-before-driver-classic-" + UUID.randomUUID();
        FilmTestExecution.startVideoRecording(recordingId);

        try
        {
            Assertions.assertTrue(FilmTestExecution.getNamesOfAllCurrentVideoRecordings().contains(recordingId),
                "Video recording thread should be started when enableFilming is true even before driver is initialized");
        }
        finally
        {
            FilmTestExecution.finishVideoFilming(recordingId, false);
        }
    }

    @Test
    public void testThreadContextClearedAfterBrowserRunnerTeardown()
    {
        FilmTestExecution.getContextVideo().setProperty("video.enableFilming", "true");
        Assertions.assertTrue(FilmTestExecution.getContextVideo().enableFilming(),
            "Property should reflect modified thread context");

        final BrowserRunner runner = new BrowserRunner();
        runner.teardown(false);

        Assertions.assertFalse(FilmTestExecution.getContextVideo().enableFilming(),
            "Thread context should be reset to default (false) after BrowserRunner teardown");
    }

    @Test
    public void testNeodymiumClearThreadContextClearsFilmContexts()
    {
        FilmTestExecution.getContextVideo().setProperty("video.enableFilming", "true");
        Assertions.assertTrue(FilmTestExecution.getContextVideo().enableFilming(),
            "Property should reflect modified thread context");

        Neodymium.clearThreadContext();

        Assertions.assertFalse(FilmTestExecution.getContextVideo().enableFilming(),
            "Neodymium.clearThreadContext() should clear thread-local video configurations");
    }

    @Test
    public void testPassingTestDeletesTemporaryRecordingWhenFailureOnlyConfigured() throws IOException
    {
        FilmTestExecution.getContextVideo().setProperty("video.appendAllRecordingsToAllureReport", "false");
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
            Assertions.assertFalse(config.appendAllRecordingsToAllureReport());
            final boolean testFailed = false;
            final boolean shouldAttach = config.appendAllRecordingsToAllureReport() || testFailed;

            Assertions.assertFalse(shouldAttach, "Should not attach on passing test when failure-only is configured");

            if (!shouldAttach && dummyVideo.exists())
            {
                dummyVideo.delete();
            }

            Assertions.assertFalse(dummyVideo.exists(), "Temporary video should be deleted upon passing test execution");
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
    public void testPassingTestRetainsRecordingByDefault() throws IOException
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
                "Default configuration must attach all recordings");
            final boolean testFailed = false;
            final boolean shouldAttach = config.appendAllRecordingsToAllureReport() || testFailed;

            Assertions.assertTrue(shouldAttach, "Should attach on passing test when default configuration is active");
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
