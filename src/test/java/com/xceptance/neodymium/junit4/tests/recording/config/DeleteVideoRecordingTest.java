package com.xceptance.neodymium.junit4.tests.recording.config;

import com.xceptance.neodymium.common.recording.FilmTestExecution;
import com.xceptance.neodymium.common.recording.config.VideoRecordingConfigurations;
import com.xceptance.neodymium.junit4.tests.recording.AbstractRecordingDeletionTest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

import java.io.File;

public class DeleteVideoRecordingTest extends AbstractRecordingDeletionTest
{
    public DeleteVideoRecordingTest()
    {
        super(false);
    }

    @BeforeClass
    public static void form()
    {
        beforeClass("video", true);
        configurationsClass.put(Thread.currentThread(),VideoRecordingConfigurations.class);
    }

    @AfterClass
    public static void assertLogFileExists()
    {
        File logFile = new File(FilmTestExecution.getContextVideo().ffmpegLogFile());
        Assert.assertTrue("the logfile for the automatic video recording test exists", logFile.exists());
        logFile.delete();
        Assert.assertFalse("the logfile for the automatic video recording test wasn't deleted", logFile.exists());
    }
}
