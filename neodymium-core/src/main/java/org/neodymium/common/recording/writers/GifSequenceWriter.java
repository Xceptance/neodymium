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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;

import org.neodymium.common.recording.config.GifRecordingConfigurations;
import org.neodymium.common.recording.config.RecordingConfigurations;

/**
 * Writer to create gif using pure java
 * 
 * @author olha
 */
public class GifSequenceWriter implements Writer
{
    protected ImageWriter writer;

    protected ImageWriteParam params;

    protected IIOMetadata metadata;

    private String gifFileName;

    private RecordingConfigurations recordingConfigurations;

    private ImageOutputStream out;

    /**
     * Required to instantiate the object in the {@link Writer#instantiate(Class, RecordingConfigurations, String)}
     * method.
     * <p>
     * 
     * @param recordingConfigurations
     *            {@link VideoRecordingConfigurations} for the writer
     * @param gifFileName
     *            {@link String} gif file name ( including the path)
     * @throws IOException
     */
    protected GifSequenceWriter(RecordingConfigurations recordingConfigurations, String gifFileName) throws IOException
    {
        this.recordingConfigurations = recordingConfigurations;
        this.gifFileName = gifFileName;
    }

    /**
     * Configures metadata, which will be used for every gif sequence
     * 
     * @param duration
     *            {@link Integer} value of milliseconds between the neighbor gif sequences
     * @param loop
     *            {@link Boolean} value if the gif should be looped
     * @throws IIOInvalidTreeException
     */
    private void configureRootMetadata(long duration, boolean loop) throws IIOInvalidTreeException
    {
        String metaFormatName = metadata.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metaFormatName);

        IIOMetadataNode graphicsControlExtensionNode = getNode(root, "GraphicControlExtension");
        graphicsControlExtensionNode.setAttribute("disposalMethod", "none");
        graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
        graphicsControlExtensionNode.setAttribute("transparentColorFlag", "FALSE");
        graphicsControlExtensionNode.setAttribute("delayTime", duration / 10 + "");
        graphicsControlExtensionNode.setAttribute("transparentColorIndex", "0");

        IIOMetadataNode commentsNode = getNode(root, "CommentExtensions");
        commentsNode.setAttribute("CommentExtension", "Created by: Xceptance");

        IIOMetadataNode appExtensionsNode = getNode(root, "ApplicationExtensions");
        IIOMetadataNode child = new IIOMetadataNode("ApplicationExtension");
        child.setAttribute("applicationID", "NETSCAPE");
        child.setAttribute("authenticationCode", "2.0");

        int loopContinuously = loop ? 1 : 0;
        child.setUserObject(new byte[]
        {
          (byte) loopContinuously, 0x0, 0x0
        });
        appExtensionsNode.appendChild(child);
        metadata.setFromTree(metaFormatName, root);
    }

    /**
     * Method to dive into the metadata configurations and find the specific {@link IIOMetadataNode}
     * 
     * @param rootNode
     *            {@link IIOMetadataNode} the parent node
     * @param nodeName
     *            {@link String} name of the desired node
     * @return found {@link IIOMetadataNode}
     */
    private static IIOMetadataNode getNode(IIOMetadataNode rootNode, String nodeName)
    {
        int nNodes = rootNode.getLength();
        for (int i = 0; i < nNodes; i++)
        {
            if (rootNode.item(i).getNodeName().equalsIgnoreCase(nodeName))
            {
                return (IIOMetadataNode) rootNode.item(i);
            }
        }
        IIOMetadataNode node = new IIOMetadataNode(nodeName);
        rootNode.appendChild(node);
        return node;
    }

    /**
     * Starts gif generation.
     * <p>
     * Creates the required streams and configures the {@link ImageWriter}
     * 
     * @throws IOException
     *             to stop screenshots loop on {@link TakeScreenshotsThread} in case of start failure
     */
    @Override
    public void start() throws IOException
    {
        out = new FileImageOutputStream(new File(gifFileName));
        writer = ImageIO.getImageWritersBySuffix("gif").next();
        params = writer.getDefaultWriteParam();

        ImageTypeSpecifier imageTypeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_4BYTE_ABGR);
        metadata = writer.getDefaultImageMetadata(imageTypeSpecifier, params);

        configureRootMetadata(recordingConfigurations.oneImagePerMilliseconds(), ((GifRecordingConfigurations) recordingConfigurations).loop());
        writer.setOutput(out);
        writer.prepareWriteSequence(null);
    }

    /**
     * Writes a gif sequence into gif file
     */
    @Override
    public void write(File image, long duration)
    {
        try
        {
            BufferedImage img = ImageIO.read(image);
            configureRootMetadata(duration, false);
            writer.writeToSequence(new IIOImage(img, null, metadata), params);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Closes the {@link ImageWriter}
     */
    @Override
    public void stop()
    {
        try
        {
            writer.endWriteSequence();
            out.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
