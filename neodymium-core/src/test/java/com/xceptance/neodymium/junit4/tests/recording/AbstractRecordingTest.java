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

import org.aeonbits.owner.ConfigFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.codeborne.selenide.Selenide;
import org.neodymium.common.browser.Browser;
import org.neodymium.common.recording.FilmTestExecution;
import org.neodymium.common.recording.config.RecordingConfigurations;
import org.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.tests.NeodymiumTest;

@Browser("Chrome_headless")
@RunWith(NeodymiumRunner.class)
public abstract class AbstractRecordingTest extends NeodymiumTest
{
    protected static Map<Thread, String> uuid = new HashMap<>();

    protected static Map<Thread, String> logFilePath = new HashMap<>();

    public static  Map<Thread,Class<? extends RecordingConfigurations>> configurationsClass = new HashMap<>();

    private boolean isGif;

    protected AbstractRecordingTest(boolean isGif)
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
        properties1.put(format + ".deleteTempRecordings", "false");
        properties1.put(format + ".ffmpegLogFile", "target/ffmpeg_output_msg_" + UUID.randomUUID() + ".txt");
        final String fileLocation = "config/temp-" + format + "-" + filmAutomatically +UUID.randomUUID()+ ".properties";
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

    private static String resolveRunFolder()
    {
        try
        {
            final Class<?> engineClass = Class.forName("com.xceptance.neodymium.ai.console.InteractiveConsoleEngine");
            final Object folder = engineClass.getMethod("getRunFolder").invoke(null);
            return folder != null ? folder.toString() : "";
        }
        catch (final Throwable ignored)
        {
            return "";
        }
    }

    @AfterClass
    public static void assertRecordingFileExists()
    {
        final String runFolder = resolveRunFolder();
        File recordingFile = null;
        if (!runFolder.isEmpty())
        {
            recordingFile = new File(FilmTestExecution.getContext(configurationsClass.get(Thread.currentThread())).tempFolderToStoreRecording() + runFolder + "/" + uuid.get(Thread.currentThread()) + "."
                + FilmTestExecution.getContext(configurationsClass.get(Thread.currentThread())).format());
        }
        if (recordingFile == null || !recordingFile.exists())
        {
            recordingFile = new File(FilmTestExecution.getContext(configurationsClass.get(Thread.currentThread())).tempFolderToStoreRecording() + uuid.get(Thread.currentThread()) + "."
                + FilmTestExecution.getContext(configurationsClass.get(Thread.currentThread())).format());
        }
        Assert.assertTrue("the recording file doesn't exist", recordingFile.exists());
        recordingFile.delete();
        Assert.assertFalse("the recording file wasn't deleted", recordingFile.exists());
    }
}
