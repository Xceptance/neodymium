/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xceptance.neodymium.junit5.tests.recording.takescreenshot;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import org.junit.Assert;

import com.codeborne.selenide.Selenide;
import org.neodymium.common.browser.Browser;
import org.neodymium.common.recording.FilmTestExecution;
import org.neodymium.common.recording.TakeScreenshotsThread;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;

@Browser("Chrome_headless")
public class TakeScreenshotTest
{
    @NeodymiumTest
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
