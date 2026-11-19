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
package org.neodymium.common.recording.writers;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.neodymium.common.recording.config.RecordingConfigurations;

/**
 * Interface for all the writers. Provides a static method to instantiate the writer and the methods every writer should
 * have (start, write and stop). It also implements the methods for image compression as this algorithm is the same for
 * every writer.
 * 
 * @author olha
 */
public interface Writer
{
    static final Logger LOGGER = LoggerFactory.getLogger(Writer.class);

    /**
     * Instantiates the writer.
     * 
     * @param writer
     *            {@link Class} of the {@link Writer} to instantiate
     * @param recordingConfigurations
     *            {@link RecordingConfigurations} for the {@link Writer}
     * @param fileName
     *            {@link String} name of the file writer shouls write into (with absolute or relative path)
     * @return created {@link Writer} instance
     */
    public static Writer instantiate(Class<? extends Writer> writer, RecordingConfigurations recordingConfigurations, String fileName) throws IOException,
        NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        Writer writerObject = null;

        Constructor<? extends Writer> constructor = writer.getDeclaredConstructor(RecordingConfigurations.class, String.class);
        try
        {
            writerObject = constructor.newInstance(recordingConfigurations, fileName);
        }
        catch (InvocationTargetException e)
        {
            if (e.getCause() instanceof FileNotFoundException)
            {
                throw (FileNotFoundException) e.getCause();
            }
            throw e;
        }
        return writerObject;
    }

    /**
     * Compresses image if it' required.
     * 
     * @param imageFile
     *            {@link File} of the image to compress
     * @param imageScaleFactor
     *            {@link Double} scale factor of the image (does nothing on 1.0 value)
     * @param imageQuality
     *            {@link Double} image quality value (does nothing on 1.0 value)
     * @return compressed image {@link File}
     */
    public default File compressImageIfNeeded(File imageFile, double imageScaleFactor, double imageQuality) throws IOException
    {
        // compress image if needed
        boolean isResizeNeeded = imageScaleFactor != 1.0;
        boolean isCompressionNeeded = imageQuality != 1.0;
        if (isResizeNeeded || isCompressionNeeded)
        {
            BufferedImage img = ImageIO.read(imageFile);
            if (isResizeNeeded)
            {
                img = resizeImage(img, imageScaleFactor);
            }
            if (isCompressionNeeded)
            {
                img = changeImageQuality(img, imageQuality);
            }
            ImageIO.write(img, "jpg", imageFile);
        }
        return imageFile;
    }

    /**
     * Changes quality of the image
     * 
     * @param image
     *            {@link BufferedImage} to compress
     * @param imageQuality
     *            {@link Double} value of desired quality percentage
     * @return {@link BufferedImage} with changed quality
     */
    public default BufferedImage changeImageQuality(BufferedImage image, double imageQuality)
    {
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();

        try (ImageOutputStream outputStream = ImageIO.createImageOutputStream(compressed))
        {
            // Obtain writer for JPG format
            // Needs to be png, else alpha isn't handled (bogus input colorspace)
            ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("png").next();
            ImageIO.setUseCache(false);

            // Configure JPG compression with specified image quality
            ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
            jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            jpgWriteParam.setCompressionQuality((float) imageQuality);

            // Set your in-memory stream as the output
            jpgWriter.setOutput(outputStream);

            // Write image as JPEG w/configured settings to the in-memory stream
            // (the IIOImage is just an aggregator object, allowing you to associate
            // thumbnails and metadata to the image, it "does" nothing)
            jpgWriter.write(null, new IIOImage(image, null, null), jpgWriteParam);

            // Dispose the writer to free resources
            jpgWriter.dispose();
            return ImageIO.read(new ByteArrayInputStream(compressed.toByteArray()));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Resizes the image
     * 
     * @param originalImage
     *            {@link BufferedImage} to resize
     * @param scaleFactor
     *            {@link Double} of scale factor
     * @return resized {@link BufferedImage}
     */
    public default BufferedImage resizeImage(BufferedImage originalImage, double scaleFactor)
    {
        int targetWidth = (int) Math.round(originalImage.getWidth() * scaleFactor);
        int targetHeight = (int) Math.round(originalImage.getHeight() * scaleFactor);
        Image resultingImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_DEFAULT);
        BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        outputImage.getGraphics().drawImage(resultingImage, 0, 0, null);
        return outputImage;
    }

    public void start() throws IOException;

    public void write(File image, long delay);

    public void stop();
}
