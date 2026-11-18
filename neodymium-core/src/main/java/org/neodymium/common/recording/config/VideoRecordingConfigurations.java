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
package org.neodymium.common.recording.config;

import org.aeonbits.owner.Config.Sources;

/**
 * Configuration interface, which contains the values needed for video recording. Contains the key and default values
 * for video.
 * 
 * @author Xceptance Software Technologies
 */
@Sources(
{
  "${recording.temporaryConfigFile}", "system:env", "system:properties",
  "file:config/dev-video-recording.properties", "file:../config/dev-video-recording.properties",
  "file:config/dev-neodymium.properties", "file:../config/dev-neodymium.properties",
  "file:config/video-recording.properties", "file:../config/video-recording.properties",
  "file:config/neodymium.properties", "file:../config/neodymium.properties"
})
public interface VideoRecordingConfigurations extends RecordingConfigurations
{
    @Override
    @Key("video.enableFilming")
    @DefaultValue("false")
    public boolean enableFilming();

    @Override
    @Key("video.filmAutomatically")
    @DefaultValue("true")
    public boolean filmAutomatically();

    @Override
    @Key("video.oneImagePerMilliseconds")
    @DefaultValue("100")
    public int oneImagePerMilliseconds();

    @Override
    @Key("video.tempFolderToStoreRecording")
    @DefaultValue("target/videos/")
    public String tempFolderToStoreRecording();

    @Override
    @Key("video.deleteRecordingsAfterAddingToAllureReport")
    @DefaultValue("true")
    public boolean deleteRecordingsAfterAddingToAllureReport();

    @Override
    @Key("video.deleteTempRecordings")
    @DefaultValue("true")
    public boolean deleteTempRecordings();

    @Override
    @Key("video.appendAllRecordingsToAllureReport")
    @DefaultValue("true")
    public boolean appendAllRecordingsToAllureReport();

    @Override
    @Key("video.imageQuality")
    @DefaultValue("1.0")
    public double imageQuality();

    @Override
    @Key("video.imageScaleFactor")
    @DefaultValue("1.0")
    public double imageScaleFactor();

    @Override
    @Key("video.format")
    @DefaultValue("mp4")
    public String format();

    @Key("video.ffmpegBinaryPath")
    @DefaultValue("ffmpeg")
    public String ffmpegBinaryPath();

    @Key("video.ffmpegLogFile")
    @DefaultValue("target/ffmpeg_output_msg.txt")
    public String ffmpegLogFile();

    @Override
    @Key("video.logInformationAboutRecording")
    @DefaultValue("false")
    public boolean logInformationAboutRecording();
}
