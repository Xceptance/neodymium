package com.xceptance.neodymium.junit4.tests.recording.writer;

import com.xceptance.neodymium.common.recording.FilmTestExecution;
import com.xceptance.neodymium.common.recording.writers.GifSequenceWriter;

public class GifWriterTest extends AbstractWriterTest
{
    public GifWriterTest()
    {
        super(FilmTestExecution.getContextGif(), GifSequenceWriter.class);
    }
}