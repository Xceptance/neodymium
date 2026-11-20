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
package com.xceptance.neodymium.common.recording.config;

import org.aeonbits.owner.Config.Sources;

/**
 * Configuration interface, which contains the values needed for gif recording. Contains the key and default values for
 * gif.
 * 
 * @author Xceptance Software Technologies
 */
@Sources(
{
  "${recording.temporaryConfigFile}", "system:env", "system:properties", "file:config/dev-gif-recording.properties", "file:config/dev-neodymium.properties",
  "file:config/gif-recording.properties", "file:config/neodyium.properties"
})
public interface GifRecordingConfigurations extends RecordingConfigurations
{
    @Override
    @Key("gif.enableFilming")
    @DefaultValue("false")
    public boolean enableFilming();

    @Override
    @Key("gif.filmAutomatically")
    @DefaultValue("true")
    public boolean filmAutomatically();

    @Override
    @Key("gif.oneImagePerMilliseconds")
    @DefaultValue("500")
    public int oneImagePerMilliseconds();

    @Override
    @Key("gif.tempFolderToStoreRecording")
    @DefaultValue("target/gifs/")
    public String tempFolderToStoreRecording();

    @Override
    @Key("gif.deleteRecordingsAfterAddingToAllureReport")
    @DefaultValue("true")
    public boolean deleteRecordingsAfterAddingToAllureReport();

    @Override
    @Key("gif.deleteTempRecordings")
    @DefaultValue("true")
    public boolean deleteTempRecordings();

    @Override
    @Key("gif.appendAllRecordingsToAllureReport")
    @DefaultValue("false")
    public boolean appendAllRecordingsToAllureReport();

    @Override
    @Key("gif.attachOnlyOnFailure")
    @DefaultValue("false")
    public boolean attachOnlyOnFailure();

    @Override
    @Key("gif.imageQuality")
    @DefaultValue("0.2")
    public double imageQuality();

    @Override
    @Key("gif.imageScaleFactor")
    @DefaultValue("1.0")
    public double imageScaleFactor();

    @Override
    @Key("gif.format")
    @DefaultValue("gif")
    public String format();

    @Key("gif.loop")
    @DefaultValue("false")
    public boolean loop();

    @Override
    @Key("gif.logInformationAboutRecording")
    @DefaultValue("false")
    public boolean logInformationAboutRecording();
}
