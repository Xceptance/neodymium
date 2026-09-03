package com.xceptance.neodymium.junit4.tests.recording.writer;

import org.neodymium.common.recording.FilmTestExecution;
import org.neodymium.common.recording.writers.VideoWriter;

public class VideoWriterTest extends AbstractWriterTest
{
    public VideoWriterTest()
    {
        super(FilmTestExecution.getContextVideo(), VideoWriter.class);
    }
}
