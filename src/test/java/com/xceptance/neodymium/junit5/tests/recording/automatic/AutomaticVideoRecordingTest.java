package com.xceptance.neodymium.junit5.tests.recording.automatic;

import com.xceptance.neodymium.common.recording.config.VideoRecordingConfigurations;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.junit5.tests.recording.AbstractRecordingTest;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.File;
import java.io.IOException;

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
        configurationsClass.put(Thread.currentThread(), VideoRecordingConfigurations.class);
    }

    @Override
    @NeodymiumTest
    public void test()
    {
        super.test();
    }

    @AfterAll
    public static void assertLogFileExists() throws IOException
    {
        File logFile = new File(logFilePath.get(Thread.currentThread()));
        Assert.assertTrue("the logfile for the automatic video recording test exists", logFile.exists());
        logFile.delete();
        Assert.assertFalse("the logfile for the automatic video recording test wasn't deleted", logFile.exists());
    }
}
