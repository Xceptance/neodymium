/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xceptance.neodymium.junit4.tests.recording.takescreenshot;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.codeborne.selenide.Selenide;
import org.neodymium.common.browser.Browser;
import org.neodymium.common.recording.FilmTestExecution;
import org.neodymium.common.recording.TakeScreenshotsThread;
import org.neodymium.junit4.NeodymiumRunner;
import org.neodymium.util.Neodymium;

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
