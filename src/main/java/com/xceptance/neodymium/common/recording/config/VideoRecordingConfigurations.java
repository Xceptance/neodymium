package com.xceptance.neodymium.common.recording.config;

import org.aeonbits.owner.Config.Sources;

/**
 * Configuration interface, which contains the values needed for video recording. Contains the key and default values
 * for video.
 * 
 * @author Xceptance Software Technologies
 */
@Sources(
{
  "${recording.temporaryConfigFile}", "system:env", "system:properties", "file:config/dev-video-recording.properties", "file:config/dev-neodymium.properties",
  "file:config/video-recording.properties", "file:config/neodyium.properties"
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
