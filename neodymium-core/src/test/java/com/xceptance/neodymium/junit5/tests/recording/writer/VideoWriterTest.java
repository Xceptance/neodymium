package com.xceptance.neodymium.junit5.tests.recording.writer;

import java.lang.reflect.Field;
import java.util.List;
import org.junit.Assert;
import org.neodymium.common.recording.FilmTestExecution;
import org.neodymium.common.recording.writers.VideoWriter;
import org.neodymium.junit5.NeodymiumTest;

public class VideoWriterTest extends AbstractWriterTest
{
    public VideoWriterTest()
    {
        super(FilmTestExecution.getContextVideo(), VideoWriter.class);
    }

    @NeodymiumTest
    public void testFfmpegCommandIncludesFaststartFlag() throws NoSuchFieldException, IllegalAccessException
    {
        final Field writerField = AbstractWriterTest.class.getDeclaredField("writer");
        writerField.setAccessible(true);
        final VideoWriter videoWriter = (VideoWriter) writerField.get(this);
        Assert.assertNotNull("VideoWriter instance should be initialized", videoWriter);

        final List<String> command = videoWriter.getProcessBuilder().command();
        Assert.assertTrue("ProcessBuilder command should contain -movflags", command.contains("-movflags"));
        final int index = command.indexOf("-movflags");
        Assert.assertTrue("Parameter following -movflags should be +faststart", command.size() > index + 1 && "+faststart".equals(command.get(index + 1)));
    }
}
