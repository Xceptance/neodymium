package com.xceptance.neodymium.junit5.tests.recording.config;

import java.io.File;

import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import com.xceptance.neodymium.common.recording.FilmTestExecution;
import com.xceptance.neodymium.common.recording.config.VideoRecordingConfigurations;
import com.xceptance.neodymium.junit4.tests.recording.AbstractRecordingTest;

public class AutomaticVideoRecordingTest extends AbstractRecordingTest
{
    public AutomaticVideoRecordingTest()
    {
        super(false);
    }

    @BeforeAll
    public static void form()
    {
        beforeClass("video", true);
        configurationsClass = VideoRecordingConfigurations.class;
    }

    @AfterAll
    public static void assertLogFileExists()
    {
        File logFile = new File(FilmTestExecution.getContextVideo().ffmpegLogFile());
        Assert.assertTrue("the logfile for the automatic video recording test exists", logFile.exists());
        logFile.delete();
        Assert.assertFalse("the logfile for the automatic video recording test wasn't deleted", logFile.exists());
    }
}
