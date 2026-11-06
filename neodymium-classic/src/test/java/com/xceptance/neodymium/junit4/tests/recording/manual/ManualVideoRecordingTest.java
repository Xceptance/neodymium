package com.xceptance.neodymium.junit4.tests.recording.manual;

import java.io.File;
import java.util.UUID;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

import com.xceptance.neodymium.common.recording.FilmTestExecution;
import com.xceptance.neodymium.common.recording.config.VideoRecordingConfigurations;
import com.xceptance.neodymium.junit4.tests.recording.AbstractRecordingTest;

public class ManualVideoRecordingTest extends AbstractRecordingTest
{
    public ManualVideoRecordingTest()
    {
        super(false);
    }

    @BeforeClass
    public static void form()
    {
        beforeClass("video", false);
        configurationsClass.put(Thread.currentThread(),VideoRecordingConfigurations.class);
    }

    @Before
    public void startFilming()
    {
        FilmTestExecution.startVideoRecording(UUID.randomUUID().toString());
    }

    @After
    public void finishFilming()
    {
        FilmTestExecution.finishVideoFilming(uuid.get(Thread.currentThread()), false);
    }

    @AfterClass
    public static void assertLogFileExists()
    {
        File logFile = new File(FilmTestExecution.getContextVideo().ffmpegLogFile());
        Assert.assertTrue("the logfile for the manual video recording test exists", logFile.exists());
        logFile.delete();
        Assert.assertFalse("the logfile for the manual video recording test wasn't deleted", logFile.exists());
    }
}
