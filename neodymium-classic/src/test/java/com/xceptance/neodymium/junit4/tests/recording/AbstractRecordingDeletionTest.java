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
package com.xceptance.neodymium.junit4.tests.recording;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.recording.FilmTestExecution;
import com.xceptance.neodymium.common.recording.config.RecordingConfigurations;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.tests.NeodymiumTest;
import org.aeonbits.owner.ConfigFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Browser("Chrome_headless")
@RunWith(NeodymiumRunner.class)
public abstract class AbstractRecordingDeletionTest extends NeodymiumTest
{
    protected static Map<Thread, String> uuid = new HashMap<>();

    protected static Map<Thread, String> logFilePath = new HashMap<>();

    public static  Map<Thread,Class<? extends RecordingConfigurations>> configurationsClass = new HashMap<>();

    private boolean isGif;

    protected AbstractRecordingDeletionTest(boolean isGif)
    {
        this.isGif = isGif;
    }

    public static void beforeClass(String format, boolean filmAutomatically)
    {
        FilmTestExecution.clearThreadContexts();

        Map<String, String> properties1 = new HashMap<>();
        properties1.put(format + ".filmAutomatically", Boolean.toString(filmAutomatically));
        properties1.put(format + ".enableFilming", "true");
        properties1.put(format + ".deleteRecordingsAfterAddingToAllureReport", "false");
        properties1.put(format + ".deleteTempRecordings", "true");

        final String fileLocation = "config/temp-" + format + "-delete-" + filmAutomatically +UUID.randomUUID()+ ".properties";
        File tempConfigFile1 = new File("./" + fileLocation);

        writeMapToPropertiesFile(properties1, tempConfigFile1);
        ConfigFactory.setProperty(FilmTestExecution.TEMPORARY_CONFIG_FILE_PROPERTY_NAME, "file:" + fileLocation);
        tempFiles.add(tempConfigFile1);
    }

    @Test
    public void test()
    {
        List<String> uuids = isGif ? FilmTestExecution.getNamesOfAllCurrentGifRecordings() : FilmTestExecution.getNamesOfAllCurrentVideoRecordings();
        Assert.assertEquals(1, uuids.size());
        uuid.put(Thread.currentThread(), uuids.get(0));
        Selenide.open("https://www.xceptance.com/en/");
        Selenide.sleep(FilmTestExecution.getContext(configurationsClass.get(Thread.currentThread())).oneImagePerMilliseconds());
    }

    @AfterClass
    public static void assertRecordingFileWasDeleted()
    {
        File recordingFile = new File(FilmTestExecution.getContext(configurationsClass.get(Thread.currentThread())).tempFolderToStoreRecording() + uuid + "."
            + FilmTestExecution.getContext(configurationsClass.get(Thread.currentThread())).format());
        Assert.assertFalse("the recording file wasn't deleted", recordingFile.exists());
    }
}
