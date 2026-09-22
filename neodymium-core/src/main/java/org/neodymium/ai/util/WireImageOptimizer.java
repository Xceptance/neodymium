/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.ai.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.neodymium.ai.client.SutAttachment;
import org.neodymium.ai.config.AiConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Optimizes binary screenshot attachments exclusively for outbound LLM wire communication.
 * Applies JPEG compression (80-85% quality) and resolution downscaling (clamped to 1280px)
 * while preserving original local artifacts, report screenshots, and baseline perceptual hashes.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class WireImageOptimizer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(WireImageOptimizer.class);

    private WireImageOptimizer()
    {
        // Prevent instantiation of utility class
    }

    /**
     * Checks whether the given MIME type represents a visual image attachment.
     *
     * @param mediaType the MIME type string
     * @return true if mediaType is an image or screenshot, false otherwise
     */
    public static boolean isImage(final String mediaType)
    {
        if (mediaType == null || mediaType.isBlank())
        {
            return false;
        }
        return mediaType.startsWith("image/") || "screenshot".equalsIgnoreCase(mediaType);
    }

    /**
     * Optimizes an image attachment for outbound LLM wire transmission by converting to JPEG
     * and downscaling if dimensions exceed the configured maximum.
     *
     * @param attachment the raw attachment
     * @return the optimized attachment or original if not an image or optimization is disabled
     */
    public static SutAttachment optimize(final SutAttachment attachment)
    {
        if (attachment == null || attachment.base64Data() == null || attachment.base64Data().isBlank())
        {
            return attachment;
        }

        if (!isImage(attachment.mediaType()))
        {
            return attachment;
        }

        final AiConfiguration config = AiConfiguration.getInstance();
        if (!config.isWireImageOptimizationEnabled())
        {
            return attachment;
        }

        try
        {
            final String rawBase64 = attachment.base64Data();
            final String cleanBase64 = rawBase64.startsWith("data:")
                ? rawBase64.substring(rawBase64.indexOf(',') + 1)
                : rawBase64;

            final byte[] decodedBytes = Base64.getDecoder().decode(cleanBase64);
            final BufferedImage src = ImageIO.read(new ByteArrayInputStream(decodedBytes));
            if (src == null)
            {
                return attachment;
            }

            final int width = src.getWidth();
            final int height = src.getHeight();
            final int maxDim = config.getWireImageOptimizationMaxDimension();

            final double scale = (width > maxDim || height > maxDim)
                ? Math.min((double) maxDim / width, (double) maxDim / height)
                : 1.0;

            final int targetWidth = Math.max(1, (int) Math.round(width * scale));
            final int targetHeight = Math.max(1, (int) Math.round(height * scale));

            final BufferedImage rgbImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            final Graphics2D g2d = rgbImage.createGraphics();
            try
            {
                // Fill with white background so transparent PNGs do not render black in JPEG
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, targetWidth, targetHeight);
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.drawImage(src, 0, 0, targetWidth, targetHeight, null);
            }
            finally
            {
                g2d.dispose();
            }

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext())
            {
                return attachment;
            }

            final ImageWriter writer = writers.next();
            try
            {
                final ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(config.getWireImageOptimizationJpegQuality());

                try (final ImageOutputStream ios = ImageIO.createImageOutputStream(baos))
                {
                    writer.setOutput(ios);
                    writer.write(null, new IIOImage(rgbImage, null, null), param);
                }
            }
            finally
            {
                writer.dispose();
            }

            final byte[] compressedBytes = baos.toByteArray();
            final String compressedBase64 = Base64.getEncoder().encodeToString(compressedBytes);
            final double reductionPct = decodedBytes.length > 0
                ? (1.0 - ((double) compressedBytes.length / decodedBytes.length)) * 100.0
                : 0.0;
            LOGGER.debug("Optimized wire image [{}]: {}x{} -> {}x{}, bytes: {} -> {} ({}% reduction)",
                attachment.filePath(), width, height, targetWidth, targetHeight,
                decodedBytes.length, compressedBytes.length,
                String.format("%.1f", reductionPct));
            return new SutAttachment("image/jpeg", attachment.filePath(), compressedBase64);
        }
        catch (final Throwable e)
        {
            LOGGER.debug("Wire image optimization skipped due to error: {}", e.getMessage());
            return attachment;
        }
    }

    /**
     * Batch optimizes a list of attachments for outbound LLM wire transmission.
     *
     * @param attachments the list of attachments
     * @return unmodifiable list with image attachments optimized
     */
    public static List<SutAttachment> optimize(final List<SutAttachment> attachments)
    {
        if (attachments == null || attachments.isEmpty())
        {
            return attachments != null ? attachments : Collections.emptyList();
        }

        final List<SutAttachment> optimizedList = new ArrayList<>(attachments.size());
        for (final SutAttachment att : attachments)
        {
            optimizedList.add(optimize(att));
        }
        return Collections.unmodifiableList(optimizedList);
    }
}
