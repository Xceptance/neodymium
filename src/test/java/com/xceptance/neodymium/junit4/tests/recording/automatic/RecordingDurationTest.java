package com.xceptance.neodymium.junit4.tests.recording.automatic;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.JUnitCore;

import com.xceptance.neodymium.common.recording.FilmTestExecution;
import com.xceptance.neodymium.common.recording.config.RecordingConfigurations;
import com.xceptance.neodymium.junit4.testclasses.recording.AutomaticRecordingTest;
import com.xceptance.neodymium.junit4.tests.NeodymiumTest;

public class RecordingDurationTest extends NeodymiumTest
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
        JUnitCore.runClasses(AutomaticRecordingTest.class);
        RecordingConfigurations config = isGif ? FilmTestExecution.getContextGif() : FilmTestExecution.getContextVideo();
        File recordingFile = new File(config.tempFolderToStoreRecording() + AutomaticRecordingTest.uuid + "." + config.format());
        recordingFile.deleteOnExit();
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
        double run1000 = runTest(false, "1000");
        double run1500 = runTest(false, "1500");
        Assert.assertEquals("Videos with different oneImagePerMilliseconds value should have approximaty the same length (1/1000 = " + run1000 + ", 1/1500 = "
                            + run1500 + ")", run1000, run1500, 5.0);
    }

    @Test
    public void testGifRecording() throws IOException
    {
        Instant start = Instant.now();
        double run100 = runTest(true, "100");
        double run100Duration = Duration.between(start, Instant.now()).toMillis() / 1000.0;
        start = Instant.now();
        Assert.assertEquals(
                            "Gif 1/100 for the test run of " + run100Duration
                            + " has duration " + run100,
                            run100, run100Duration, 5.0);
        double run1000 = runTest(true, "1000");
        double run1000Duration = Duration.between(start, Instant.now()).toMillis() / 1000.0;
        Assert.assertEquals(
                            "Gif 1/1000 for the test run of " + run100Duration
                            + " has duration " + run1000,
                            run1000, run1000Duration, 5.0);
    }

    @Test
    public void testMixedRecording() throws IOException, InterruptedException
    {
        double runVideo1000 = runTest(false, "1000");
        double runGif1000 = runTest(true, "1000");
        Assert.assertEquals(
                            "Gifs with different oneImagePerMilliseconds value should have approximaty the same length (video = "
                            + runVideo1000 + ", gif = " + runGif1000 + ")",
                            runVideo1000, runGif1000, 5.0);
    }
}
