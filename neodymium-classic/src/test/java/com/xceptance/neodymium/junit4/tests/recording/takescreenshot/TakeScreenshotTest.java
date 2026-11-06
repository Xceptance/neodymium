package com.xceptance.neodymium.junit4.tests.recording.takescreenshot;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.recording.FilmTestExecution;
import com.xceptance.neodymium.common.recording.TakeScreenshotsThread;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@Browser("Chrome_headless")
@RunWith(NeodymiumRunner.class)
public class TakeScreenshotTest
{
    @Test
    public void test() throws IOException, InterruptedException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException,
        IllegalArgumentException, InvocationTargetException
    {
        TakeScreenshotsThread takeScreenshotsThread = new TakeScreenshotsThread(Neodymium.getDriver(), MockWriter.class, FilmTestExecution.getContextGif(), UUID.randomUUID()
                                                                                                                                                                .toString());
        Selenide.open("https://www.xceptance.com/en/");
        takeScreenshotsThread.start();
        Selenide.sleep(FilmTestExecution.getContextGif().oneImagePerMilliseconds());
        takeScreenshotsThread.stopRun(false);
        takeScreenshotsThread.join();
        Assert.assertNotNull("neither start nor stop method of writer were called by " + TakeScreenshotsThread.class.getSimpleName(), MockWriter.screenshots);
        Assert.assertTrue(TakeScreenshotsThread.class.getSimpleName() + " should only take one or two screenshots in this time",
                          MockWriter.screenshots.size() > 0 && MockWriter.screenshots.size() < 3);
    }
}
