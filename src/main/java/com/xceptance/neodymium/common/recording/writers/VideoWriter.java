package com.xceptance.neodymium.common.recording.writers;

/**
 * @deprecated Use {@link org.neodymium.common.recording.writers.VideoWriter} instead.
 */
@Deprecated
public class VideoWriter extends org.neodymium.common.recording.writers.VideoWriter
{
    protected VideoWriter(org.neodymium.common.recording.config.RecordingConfigurations recordingConfigurations, java.lang.String videoFileName) throws java.io.FileNotFoundException
    {
        super(recordingConfigurations, videoFileName);
    }
}
