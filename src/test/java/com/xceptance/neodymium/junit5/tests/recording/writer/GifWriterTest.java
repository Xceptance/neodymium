package com.xceptance.neodymium.junit5.tests.recording.writer;

import com.xceptance.neodymium.common.recording.FilmTestExecution;
import com.xceptance.neodymium.common.recording.writers.GifSequenceWriter;

public class GifWriterTest extends AbstractWriterTest
{
    public GifWriterTest()
    {
        super(FilmTestExecution.getContextGif(), GifSequenceWriter.class);
    }
}