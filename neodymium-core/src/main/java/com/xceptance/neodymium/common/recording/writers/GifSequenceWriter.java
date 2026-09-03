package com.xceptance.neodymium.common.recording.writers;

/**
 * @deprecated Use {@link org.neodymium.common.recording.writers.GifSequenceWriter} instead.
 */
@Deprecated
public class GifSequenceWriter extends org.neodymium.common.recording.writers.GifSequenceWriter
{
    protected GifSequenceWriter(org.neodymium.common.recording.config.RecordingConfigurations recordingConfigurations, java.lang.String gifFileName) throws java.io.IOException
    {
        super(recordingConfigurations, gifFileName);
    }
}
