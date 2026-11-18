/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xceptance.neodymium.junit5.tests.recording.writer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;

import org.neodymium.common.recording.config.RecordingConfigurations;
import org.neodymium.common.recording.writers.Writer;
import org.neodymium.junit5.NeodymiumTest;
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

    @BeforeEach
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

    @NeodymiumTest
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

    @NeodymiumTest
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

    @NeodymiumTest
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
