package com.xceptance.neodymium.junit4.tests.recording.automatic;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.recording.FilmTestExecution;
import com.xceptance.neodymium.common.recording.config.RecordingConfigurations;
import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.testclasses.recording.AutomaticRecordingTest;
import com.xceptance.neodymium.junit4.tests.NeodymiumTest;

@RunWith(NeodymiumRunner.class)
@Retry(maxNumberOfRetries = 5, exceptions =
{
  "Invalid data found when processing input"
})
public class RecordingDurationTest extends NeodymiumTest
{
    private File recordingFile100;

    private File recordingFile1000;

    public void runTest(boolean isGif, boolean isMixed) throws IOException, InterruptedException
    {
        AutomaticRecordingTest.isGif = isGif;
        AutomaticRecordingTest.isMixed = isMixed;
        String format = isGif || isMixed ? "gif" : "video";
        FilmTestExecution.clearThreadContexts();
        Map<String, String> properties1 = new HashMap<>();
        properties1.put(format + ".filmAutomatically", "false");
        properties1.put(format + ".enableFilming", "true");
        properties1.put(format + ".deleteRecordingsAfterAddingToAllureReport", "false");
        if (isMixed)
        {
            properties1.put("video.filmAutomatically", "false");
            properties1.put("video.enableFilming", "true");
            properties1.put("video.deleteRecordingsAfterAddingToAllureReport", "false");
        }
        final String fileLocation = "config/temp-RecordingDurationTest" + (isMixed ? "mixed" : format) + ".properties";
        File tempConfigFile1 = new File("./" + fileLocation);
        writeMapToPropertiesFile(properties1, tempConfigFile1);
        ConfigFactory.setProperty(FilmTestExecution.TEMPORARY_CONFIG_FILE_PROPERTY_NAME, "file:" + fileLocation);
        tempFiles.add(tempConfigFile1);
        run(AutomaticRecordingTest.class);
        RecordingConfigurations config = isGif || isMixed ? FilmTestExecution.getContextGif() : FilmTestExecution.getContextVideo();
        recordingFile100 = new File(config.tempFolderToStoreRecording() + AutomaticRecordingTest.uuid_100 + "." + config.format());
        recordingFile1000 = new File((isMixed ? FilmTestExecution.getContextVideo().tempFolderToStoreRecording() : config.tempFolderToStoreRecording())
                                     + AutomaticRecordingTest.uuid_1000 + "." + (isMixed ? FilmTestExecution.getContextVideo().format() : config.format()));
        recordingFile100.deleteOnExit();
        recordingFile1000.deleteOnExit();
        for (int i = 0; i < 6 && !(recordingFile100.exists() && recordingFile1000.exists()); i++)
        {
            Thread.sleep(1000);
        }
        Assert.assertTrue("the recording file doesn't exist", recordingFile100.exists() && recordingFile1000.exists());
    }

    double mesureDuration(File recordingFile) throws IOException
    {
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
    public void testVideoRecording() throws IOException, InterruptedException
    {
        runTest(false, false);
        double duration100 = mesureDuration(recordingFile100);
        double duration1000 = mesureDuration(recordingFile1000);
        Assert.assertEquals("Videos with different oneImagePerMilliseconds value should have approximaty the same length (1/100 = " + duration100
                            + ", 1/1000 = " + duration1000 + ")", duration100, duration1000, 5.0);
    }

    @Test
    public void testGifRecording() throws IOException, InterruptedException
    {
        runTest(true, false);
        double duration100 = mesureDuration(recordingFile100);
        double duration1000 = mesureDuration(recordingFile1000);
        Assert.assertEquals("Gifs with different oneImagePerMilliseconds value should have approximaty the same length (1/100 = " + duration100
                            + ", 1/1000 = " + duration1000 + ")", duration100, duration1000, 5.0);
    }

    @Test
    public void testMixedRecording() throws IOException, InterruptedException
    {
        runTest(false, true);
        double durationGif1000 = mesureDuration(recordingFile100);
        double durationVideo1000 = mesureDuration(recordingFile1000);
        Assert.assertEquals("Gif and video with different oneImagePerMilliseconds value should have approximaty the same length (video = " + durationVideo1000
                            + ", gif = "
                            + durationGif1000 + ")", durationVideo1000, durationGif1000, 5.0);
    }
}
