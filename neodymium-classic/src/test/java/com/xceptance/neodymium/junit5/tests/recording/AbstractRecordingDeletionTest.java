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
package com.xceptance.neodymium.junit5.tests.recording;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.recording.FilmTestExecution;
import com.xceptance.neodymium.common.recording.config.RecordingConfigurations;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.junit5.tests.AbstractNeodymiumTest;
import org.aeonbits.owner.ConfigFactory;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Browser("Chrome_headless")
public abstract class AbstractRecordingDeletionTest extends AbstractNeodymiumTest
{
    protected static final Map<Thread, String> uuid = new ConcurrentHashMap<>();

    protected static final Map<Thread, String> logFilePath = new ConcurrentHashMap<>();

    public static final Map<Thread, Class<? extends RecordingConfigurations>> configurationsClass = new ConcurrentHashMap<>();

    private final boolean isGif;

    protected AbstractRecordingDeletionTest(final boolean isGif)
    {
        this.isGif = isGif;
    }

    public static void beforeClass(final String format, final boolean filmAutomatically)
    {
        FilmTestExecution.clearThreadContexts();

        final Map<String, String> properties1 = new HashMap<>();
        properties1.put(format + ".filmAutomatically", Boolean.toString(filmAutomatically));
        properties1.put(format + ".enableFilming", "true");
        properties1.put(format + ".deleteRecordingsAfterAddingToAllureReport", "false");
        properties1.put(format + ".deleteTempRecordings", "true");
        logFilePath.put(Thread.currentThread(), "target/ffmpeg_output_msg" + UUID.randomUUID() + ".txt");
        if ("video".equals(format))
        {
            properties1.put("video.ffmpegLogFile", logFilePath.get(Thread.currentThread()));
        }
        final String fileLocation = "config/temp-" + format + "-delete-" + filmAutomatically + UUID.randomUUID() + ".properties";
        final File tempConfigFile1 = new File("./" + fileLocation);

        writeMapToPropertiesFile(properties1, tempConfigFile1);

        ConfigFactory.setProperty(FilmTestExecution.TEMPORARY_CONFIG_FILE_PROPERTY_NAME, "file:" + fileLocation);

        tempFiles.add(tempConfigFile1);
    }

    @NeodymiumTest
    public void test()
    {
        final List<String> uuids = isGif ? FilmTestExecution.getNamesOfAllCurrentGifRecordings() : FilmTestExecution.getNamesOfAllCurrentVideoRecordings();
        Assert.assertEquals(1, uuids.size());
        uuid.put(Thread.currentThread(), uuids.get(0));
        Selenide.open("https://www.xceptance.com/en/");
        Selenide.sleep(FilmTestExecution.getContext(configurationsClass.get(Thread.currentThread())).oneImagePerMilliseconds());
    }

    @AfterAll
    public static void assertRecordingFileWasDeleted()
    {
        final String recordingUuid = uuid.get(Thread.currentThread());
        Assert.assertNotNull("the recording uuid must be recorded before cleanup verification", recordingUuid);
        final RecordingConfigurations config = FilmTestExecution.getContext(configurationsClass.get(Thread.currentThread()));
        final File recordingFile = new File(config.tempFolderToStoreRecording() + recordingUuid + "." + config.format());
        Assert.assertFalse("the recording file wasn't deleted", recordingFile.exists());
    }
}
