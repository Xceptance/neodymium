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
