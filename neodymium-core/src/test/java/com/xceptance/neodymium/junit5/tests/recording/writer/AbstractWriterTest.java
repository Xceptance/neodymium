/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
