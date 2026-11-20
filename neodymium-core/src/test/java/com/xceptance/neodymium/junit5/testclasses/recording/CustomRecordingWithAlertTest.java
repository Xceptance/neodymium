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
package com.xceptance.neodymium.junit5.testclasses.recording;

import java.io.IOException;
import java.util.UUID;

import com.codeborne.selenide.Selenide;
import org.neodymium.common.browser.Browser;
import org.neodymium.common.recording.FilmTestExecution;
import org.neodymium.common.recording.config.GifRecordingConfigurations;
import org.neodymium.common.recording.config.VideoRecordingConfigurations;
import org.neodymium.junit5.NeodymiumTest;

@Browser("Chrome_headless")
public class CustomRecordingWithAlertTest
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
        Selenide.sleep(10000);

        Selenide.executeJavaScript("alert('Hello! I am an alert box!');");
        Selenide.sleep(10000);

        Selenide.switchTo().alert().accept();
        Selenide.sleep(10000);

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
