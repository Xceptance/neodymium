package com.xceptance.neodymium.junit5.testclasses.recording;

import java.io.IOException;
import java.util.UUID;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.recording.FilmTestExecution;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@Browser("Chrome_headless")
public class CustomRecordingTest
{
    public static String uuid;

    public static boolean isGif;

    @NeodymiumTest
    public void test() throws IOException
    {
        // open different page first to warm up browser
        // as the empty page on browser warm up may sometimes land in video/gif sequence and influence duration
        // we in return want to keep the executions as identical as possible to ensure duration is the same for
        // identical tests
        Selenide.open("https://www.google.com/");
        uuid = UUID.randomUUID().toString();
        if (isGif)
        {
            FilmTestExecution.startGifRecording(uuid);
        }
        else
        {
            FilmTestExecution.startVideoRecording(uuid);
        }
        Selenide.open("https://www.timeanddate.com/worldclock/germany/berlin");
        Selenide.sleep(30000);

        if (isGif)
        {
            FilmTestExecution.finishGifFilming(uuid, false);
        }
        else
        {
            FilmTestExecution.finishVideoFilming(uuid, false);
        }
    }
}
