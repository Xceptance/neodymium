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

@Browser("Chrome_headless")
public abstract class AbstractRecordingDeletionTest extends AbstractNeodymiumTest
{
    protected static String uuid;

    public static Class<? extends RecordingConfigurations> configurationsClass;

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

        final String fileLocation = "config/temp-" + format + "-" + filmAutomatically + ".properties";
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
        uuid = uuids.get(0);
        Selenide.open("https://www.xceptance.com/en/");
        Selenide.sleep(FilmTestExecution.getContext(configurationsClass).oneImagePerMilliseconds());
    }

    @AfterAll
    public static void assertRecordingFileWasDeleted()
    {
        File recordingFile = new File(FilmTestExecution.getContext(configurationsClass).tempFolderToStoreRecording() + uuid + "."
                                          + FilmTestExecution.getContext(configurationsClass).format());
        Assert.assertFalse("the recording file wasn't deleted", recordingFile.exists());
    }
}
