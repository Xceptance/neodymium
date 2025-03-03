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
    @Key("gif.appendAllRecordingsToAllureReport")
    @DefaultValue("true")
    public boolean appendAllRecordingsToAllureReport();

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
