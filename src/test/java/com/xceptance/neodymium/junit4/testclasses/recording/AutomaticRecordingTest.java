package com.xceptance.neodymium.junit4.testclasses.recording;

import java.io.IOException;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.recording.FilmTestExecution;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@Browser("Chrome_headless")
@RunWith(NeodymiumRunner.class)
public class AutomaticRecordingTest
{
    public static String uuid;

    public static boolean isGif;

    @Test
    public void test() throws IOException
    {
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
