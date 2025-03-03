package com.xceptance.neodymium.junit5.tests.recording.manual;

import java.io.File;
import java.util.UUID;

import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import com.xceptance.neodymium.common.recording.FilmTestExecution;
import com.xceptance.neodymium.common.recording.config.VideoRecordingConfigurations;
import com.xceptance.neodymium.junit4.tests.recording.AbstractRecordingTest;

public class ManualVideoRecordingTest extends AbstractRecordingTest
{
    public ManualVideoRecordingTest()
    {
        super(false);
    }

    @BeforeAll
    public static void form()
    {
        beforeClass("video", false);
        configurationsClass = VideoRecordingConfigurations.class;
    }

    @BeforeEach
    public void startFilming()
    {
        FilmTestExecution.startVideoRecording(UUID.randomUUID().toString());
    }

    @AfterEach
    public void finishFilming()
    {
        FilmTestExecution.finishVideoFilming(uuid, false);
    }

    @AfterAll
    public static void assertLogFileExists()
    {
        File logFile = new File(FilmTestExecution.getContextVideo().ffmpegLogFile());
        Assert.assertTrue("the logfile for the manual video recording test exists", logFile.exists());
        logFile.delete();
        Assert.assertFalse("the logfile for the manual video recording test wasn't deleted", logFile.exists());
    }
}
