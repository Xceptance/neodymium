package com.xceptance.neodymium.junit5.tests.recording;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.aeonbits.owner.ConfigFactory;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.recording.FilmTestExecution;
import com.xceptance.neodymium.common.recording.config.RecordingConfigurations;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.junit5.tests.AbstractNeodymiumTest;

@Browser("Chrome_headless")
public abstract class AbstractRecordingTest extends AbstractNeodymiumTest
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
        logFilePath.put(Thread.currentThread(),"target/ffmpeg_output_msg"+UUID.randomUUID()+".txt");
        if("video".equals(format)) {
            properties1.put("video.ffmpegLogFile",logFilePath.get(Thread.currentThread()));
        }
        properties1.put(format + ".deleteRecordingsAfterAddingToAllureReport", "false");
        final String fileLocation = "config/temp-" + format + "-" + filmAutomatically +UUID.randomUUID()+ ".properties";
        File tempConfigFile1 = new File("./" + fileLocation);
        writeMapToPropertiesFile(properties1, tempConfigFile1);
        ConfigFactory.setProperty(FilmTestExecution.TEMPORARY_CONFIG_FILE_PROPERTY_NAME, "file:" + fileLocation);
        tempFiles.add(tempConfigFile1);
    }

    @NeodymiumTest
    public void test()
    {
        List<String> uuids = isGif ? FilmTestExecution.getNamesOfAllCurrentGifRecordings() : FilmTestExecution.getNamesOfAllCurrentVideoRecordings();
        Assert.assertEquals(1, uuids.size());
        uuid.put(Thread.currentThread(), uuids.get(0));
        Selenide.open("https://www.xceptance.com/en/");
        Selenide.sleep(FilmTestExecution.getContext(configurationsClass.get(Thread.currentThread())).oneImagePerMilliseconds());
    }

    @AfterAll
    public static void assertRecordingFileExists()
    {
        File recordingFile = new File(FilmTestExecution.getContext(configurationsClass.get(Thread.currentThread())).tempFolderToStoreRecording() + uuid.get(Thread.currentThread()) + "."
                                      + FilmTestExecution.getContext(configurationsClass.get(Thread.currentThread())).format());
        Assert.assertTrue("the recording file doesn't exist", recordingFile.exists());
        recordingFile.delete();
        Assert.assertFalse("the recording file wasn't deleted", recordingFile.exists());
    }
}
