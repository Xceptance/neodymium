package com.xceptance.neodymium.junit5.tests.recording.automatic;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.common.recording.FilmTestExecution;
import com.xceptance.neodymium.common.recording.config.RecordingConfigurations;
import com.xceptance.neodymium.junit5.testclasses.recording.AutomaticRecordingTest;
import com.xceptance.neodymium.junit5.tests.AbstractNeodymiumTest;

public class RecordingDurationTest extends AbstractNeodymiumTest
{
    public double runTest(boolean isGif, String oneImagePerMilliseconds) throws IOException
    {
        AutomaticRecordingTest.isGif = isGif;
        String format = isGif ? "gif" : "video";
        FilmTestExecution.clearThreadContexts();
        Map<String, String> properties1 = new HashMap<>();
        properties1.put(format + ".filmAutomatically", "false");
        properties1.put(format + ".enableFilming", "true");
        properties1.put(format + ".deleteRecordingsAfterAddingToAllureReport", "false");
        properties1.put(format + ".oneImagePerMilliseconds", oneImagePerMilliseconds);
        final String fileLocation = "config/temp-" + format + "-" + oneImagePerMilliseconds + ".properties";
        File tempConfigFile1 = new File("./" + fileLocation);
        writeMapToPropertiesFile(properties1, tempConfigFile1);
        ConfigFactory.setProperty(FilmTestExecution.TEMPORARY_CONFIG_FILE_PROPERTY_NAME, "file:" + fileLocation);
        tempFiles.add(tempConfigFile1);
        run(AutomaticRecordingTest.class);
        RecordingConfigurations config = isGif ? FilmTestExecution.getContextGif() : FilmTestExecution.getContextVideo();
        File recordingFile = new File(config.tempFolderToStoreRecording() + AutomaticRecordingTest.uuid + "." + config.format());
        // recordingFile.deleteOnExit();
        Assert.assertTrue("the recording file doesn't exist", recordingFile.exists());
        ProcessBuilder pb = new ProcessBuilder("ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "default=noprint_wrappers=1:nokey=1", recordingFile.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String recordDuration = null;
        for (String line = r.readLine(); StringUtils.isNotBlank(line); line = r.readLine())
        {
            recordDuration = line;
            continue;
        }
        return Double.parseDouble(recordDuration);
    }

    @Test
    public void testVideoRecording() throws IOException
    {
        double run100 = runTest(false, "100");
        // double run1000 = runTest(false, "1000");
        System.out.println(run100);
        // System.out.println(run1000);

        //
        // Assert.assertEquals("Videos with different oneImagePerMilliseconds value should have approximaty the same
        // length (1/100 = " + run1000
        // + ", 1/1000 = " + run100 + ")", run100, run1000, 5.0);
    }

    @Test
    public void testGifRecording() throws IOException
    {
        double run100 = runTest(true, "100");
        double run1500 = runTest(true, "1000");
        Assert.assertEquals("Gifs with different oneImagePerMilliseconds value should have approximaty the same length (1/100 = " + run100 + ", 1/1500 = "
                            + run1500 + ")", run100, run1500, 5.0);
    }

    @Test
    public void testMixedRecording() throws IOException
    {
        double runVideo1000 = runTest(false, "1000");
        double runGif1000 = runTest(true, "1000");
        Assert.assertEquals("Gifs with different oneImagePerMilliseconds value should have approximaty the same length (video = " + runVideo1000 + ", gif = "
                            + runGif1000 + ")", runVideo1000, runGif1000, 5.0);
    }
}
