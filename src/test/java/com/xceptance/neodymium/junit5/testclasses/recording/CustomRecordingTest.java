package com.xceptance.neodymium.junit5.testclasses.recording;

import java.io.IOException;
import java.util.UUID;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.recording.FilmTestExecution;
import com.xceptance.neodymium.common.recording.config.GifRecordingConfigurations;
import com.xceptance.neodymium.common.recording.config.VideoRecordingConfigurations;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@Browser("Chrome_headless")
public class CustomRecordingTest
{
    public static String uuid_100;

    public static String uuid_1000;

    public static boolean isGif;

    public static boolean isMixed;

    @NeodymiumTest
    public void test() throws IOException
    {
        uuid_100 = UUID.randomUUID().toString();
        uuid_1000 = UUID.randomUUID().toString();
        if (isGif)
        {
            FilmTestExecution.getContext(GifRecordingConfigurations.class)
                             .setProperty("gif.oneImagePerMilliseconds", "100");
            FilmTestExecution.startGifRecording(uuid_100);
            FilmTestExecution.getContext(GifRecordingConfigurations.class)
                             .setProperty("gif.oneImagePerMilliseconds", "1000");
            FilmTestExecution.startGifRecording(uuid_1000);
        }
        else if (isMixed)
        {
            FilmTestExecution.getContext(GifRecordingConfigurations.class)
                             .setProperty("gif.oneImagePerMilliseconds", "100");
            FilmTestExecution.startGifRecording(uuid_100);
            FilmTestExecution.getContext(VideoRecordingConfigurations.class)
                             .setProperty("video.oneImagePerMilliseconds", "1000");
            FilmTestExecution.startVideoRecording(uuid_1000);
        }
        else
        {
            FilmTestExecution.getContext(VideoRecordingConfigurations.class)
                             .setProperty("video.oneImagePerMilliseconds", "100");
            FilmTestExecution.startVideoRecording(uuid_100);
            FilmTestExecution.getContext(VideoRecordingConfigurations.class)
                             .setProperty("video.oneImagePerMilliseconds", "1000");
            FilmTestExecution.startVideoRecording(uuid_1000);
        }
        Selenide.open("https://www.timeanddate.com/worldclock/germany/berlin");
        Selenide.sleep(30000);

        if (isGif)
        {
            FilmTestExecution.finishGifFilming(uuid_100, false);
            FilmTestExecution.finishGifFilming(uuid_1000, false);
        }
        else if (isMixed)
        {
            FilmTestExecution.finishGifFilming(uuid_100, false);
            FilmTestExecution.finishVideoFilming(uuid_1000, false);
        }
        else
        {
            FilmTestExecution.finishVideoFilming(uuid_100, false);
            FilmTestExecution.finishVideoFilming(uuid_1000, false);
        }
    }
}
