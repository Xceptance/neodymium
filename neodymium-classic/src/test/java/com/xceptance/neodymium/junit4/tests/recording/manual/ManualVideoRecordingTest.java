/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
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
        final String trackedLogFilePath = logFilePath.get(Thread.currentThread());
        Assert.assertNotNull("the logfile path for the manual video recording test must be configured", trackedLogFilePath);
        final File logFile = new File(trackedLogFilePath);
        Assert.assertTrue("the logfile for the manual video recording test exists", logFile.exists());
        logFile.delete();
        Assert.assertFalse("the logfile for the manual video recording test wasn't deleted", logFile.exists());
    }
}
