package com.xceptance.neodymium.junit4.tests.recording.writer;

import com.xceptance.neodymium.common.recording.FilmTestExecution;
import com.xceptance.neodymium.common.recording.writers.VideoWriter;

public class VideoWriterTest extends AbstractWriterTest
{
    public VideoWriterTest()
    {
        super(FilmTestExecution.getContextVideo(), VideoWriter.class);
    }
}
