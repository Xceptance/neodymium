package com.xceptance.neodymium.junit4.tests.recording.writer;

import org.neodymium.common.recording.FilmTestExecution;
import org.neodymium.common.recording.writers.GifSequenceWriter;

public class GifWriterTest extends AbstractWriterTest
{
    public GifWriterTest()
    {
        super(FilmTestExecution.getContextGif(), GifSequenceWriter.class);
    }
}
