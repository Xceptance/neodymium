package com.xceptance.neodymium.junit5.testclasses.recording;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.recording.FilmTestExecution;
import com.xceptance.neodymium.junit5.NeodymiumTest;

import java.io.IOException;
import java.util.UUID;

@Browser("Chrome_headless")
public class CustomRecordingWithAlertTest
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
        Selenide.sleep(10000);

        Selenide.executeJavaScript("alert('Hello! I am an alert box!');");
        Selenide.sleep(10000);

        Selenide.switchTo().alert().accept();
        Selenide.sleep(10000);

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
