/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xceptance.neodymium.junit4.tests.recording;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.aeonbits.owner.ConfigFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.console.InteractiveConsoleEngine;
import org.neodymium.common.browser.Browser;
import org.neodymium.common.recording.FilmTestExecution;
import org.neodymium.common.recording.config.RecordingConfigurations;
import org.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.tests.NeodymiumTest;

@Browser("Chrome_headless")
@RunWith(NeodymiumRunner.class)
public abstract class AbstractRecordingTest extends NeodymiumTest
{
    protected static final Map<Thread, String> uuid = new ConcurrentHashMap<>();

    protected static final Map<Thread, String> logFilePath = new ConcurrentHashMap<>();

    public static final Map<Thread, Class<? extends RecordingConfigurations>> configurationsClass = new ConcurrentHashMap<>();

    private final boolean isGif;

    protected AbstractRecordingTest(final boolean isGif)
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
        properties1.put(format + ".deleteTempRecordings", "false");
        if ("video".equals(format))
        {
            final String uniqueLogFile = "target/ffmpeg_output_msg_" + UUID.randomUUID() + ".txt";
            logFilePath.put(Thread.currentThread(), uniqueLogFile);
            properties1.put("video.ffmpegLogFile", uniqueLogFile);
        }
        final String fileLocation = "config/temp-" + format + "-" + filmAutomatically + UUID.randomUUID() + ".properties";
        final File tempConfigFile1 = new File("./" + fileLocation);
        writeMapToPropertiesFile(properties1, tempConfigFile1);
        ConfigFactory.setProperty(FilmTestExecution.TEMPORARY_CONFIG_FILE_PROPERTY_NAME, "file:" + fileLocation);
        tempFiles.add(tempConfigFile1);
    }

    @Test
    public void test()
    {
        final List<String> uuids = isGif ? FilmTestExecution.getNamesOfAllCurrentGifRecordings() : FilmTestExecution.getNamesOfAllCurrentVideoRecordings();
        Assert.assertEquals(1, uuids.size());
        uuid.put(Thread.currentThread(), uuids.get(0));
        Selenide.open("https://www.xceptance.com/en/");
        Selenide.sleep(FilmTestExecution.getContext(configurationsClass.get(Thread.currentThread())).oneImagePerMilliseconds());
    }

    @AfterClass
    public static void assertRecordingFileExists()
    {
        final String runFolder = InteractiveConsoleEngine.getRunFolder();
        final RecordingConfigurations config = FilmTestExecution.getContext(configurationsClass.get(Thread.currentThread()));
        final String recordingUuid = uuid.get(Thread.currentThread());
        File recordingFile = new File(config.tempFolderToStoreRecording() + runFolder + "/" + recordingUuid + "." + config.format());
        if (!recordingFile.exists())
        {
            recordingFile = new File(config.tempFolderToStoreRecording() + recordingUuid + "." + config.format());
        }
        Assert.assertTrue("the recording file doesn't exist", recordingFile.exists());
        recordingFile.delete();
        Assert.assertFalse("the recording file wasn't deleted", recordingFile.exists());
    }
}
