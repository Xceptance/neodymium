package com.xceptance.neodymium.junit4.tests.recording.writer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.xceptance.neodymium.common.recording.config.RecordingConfigurations;
import com.xceptance.neodymium.common.recording.writers.Writer;
import com.xceptance.neodymium.util.TestImageGenerator;

public abstract class AbstractWriterTest
{
    private RecordingConfigurations configurations;

    private Class<? extends Writer> writerClass;

    private String pathToFile;

    private Writer writer;

    protected AbstractWriterTest(RecordingConfigurations configurations, Class<? extends Writer> writerClass)
    {
        this.configurations = configurations;
        this.writerClass = writerClass;
    }

    @Before
    public void calculatePath() throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException,
        InvocationTargetException, IOException
    {
        if (!new File(configurations.tempFolderToStoreRecording()).exists())
        {
            new File(configurations.tempFolderToStoreRecording()).mkdir();
        }
        pathToFile = configurations.tempFolderToStoreRecording() + UUID.randomUUID().toString() + "." + configurations.format();
        writer = Writer.instantiate(writerClass, configurations, pathToFile);
    }

    @Test
    public void testWriting() throws IOException
    {
        File filePath = new File(pathToFile);

        writer.start();
        writer.write(TestImageGenerator.generateImage(), 100);
        writer.write(TestImageGenerator.generateImage(), 100);
        writer.write(TestImageGenerator.generateImage(), 100);
        writer.stop();

        Assert.assertTrue("writer haven't created a file", filePath.exists());
        filePath.delete();
        Assert.assertFalse("the file wasn't deleted", filePath.exists());
    }

    @Test
    public void testResizing() throws IOException
    {
        File fileCopy = new File("target/" + UUID.randomUUID().toString() + ".jpg");

        FileUtils.copyFile(TestImageGenerator.generateImage(), fileCopy);
        int originalLength = (int) fileCopy.length();
        BufferedImage fileCopyImg = ImageIO.read(fileCopy);

        fileCopyImg = writer.resizeImage(fileCopyImg, 0.2);
        ImageIO.write(fileCopyImg, "jpg", fileCopy);

        int compressedLength = (int) fileCopy.length();
        Assert.assertTrue("original length (" + originalLength + ") is not greater than compressed length (" + compressedLength + ")",
                          originalLength > compressedLength);

        Assert.assertTrue("writer haven't created a file", fileCopy.exists());
        fileCopy.delete();
        Assert.assertFalse("the file wasn't deleted", fileCopy.exists());
    }

    @Test
    public void testQualityChange() throws IOException
    {
        File fileCopy = new File("target/" + UUID.randomUUID().toString() + ".jpg");

        FileUtils.copyFile(TestImageGenerator.generateImage(), fileCopy);
        int originalLength = (int) fileCopy.length();
        BufferedImage fileCopyImg = ImageIO.read(fileCopy);

        fileCopyImg = writer.changeImageQuality(fileCopyImg, 0.2);
        ImageIO.write(fileCopyImg, "jpg", fileCopy);

        int compressedLength = (int) fileCopy.length();
        Assert.assertTrue("original length (" + originalLength + ") is not greater than compressed length (" + compressedLength + ")",
                          originalLength > compressedLength);

        Assert.assertTrue("writer haven't created a file", fileCopy.exists());
        fileCopy.delete();
        Assert.assertFalse("the file wasn't deleted", fileCopy.exists());
    }
}
