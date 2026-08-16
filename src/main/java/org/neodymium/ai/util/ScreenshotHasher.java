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

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for computing perceptual image hashes (dHash) from screenshots,
 * and calculating Hamming distances between hashes to verify visual similarity.
 */
public final class ScreenshotHasher
{
    private static final Logger LOG = LoggerFactory.getLogger(ScreenshotHasher.class);

    private ScreenshotHasher()
    {
        // Prevent instantiation
    }



    /**
     * Computes a 64x64 luminance matrix (4,096 bytes) for a Base64-encoded PNG screenshot,
     * downscaled using bilinear interpolation, and encoded as a Base64 string.
     *
     * @param base64Png the Base64-encoded PNG screenshot string
     * @return the Base64-encoded 64x64 luminance matrix, or {@code null} if input is invalid
     */
    public static String computeSsimMatrix(final String base64Png)
    {
        if (base64Png == null || base64Png.isEmpty())
        {
            return null;
        }

        try
        {
            final byte[] imageBytes = Base64.getDecoder().decode(base64Png);
            try (final ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes))
            {
                final BufferedImage image = ImageIO.read(bais);
                if (image == null)
                {
                    return null;
                }

                final int matrixDim = 64;
                final BufferedImage resized = new BufferedImage(matrixDim, matrixDim, BufferedImage.TYPE_BYTE_GRAY);
                final Graphics2D g = resized.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(image, 0, 0, matrixDim, matrixDim, null);
                g.dispose();

                final byte[] matrixBytes = new byte[matrixDim * matrixDim];
                int idx = 0;
                for (int y = 0; y < matrixDim; y++)
                {
                    for (int x = 0; x < matrixDim; x++)
                    {
                        matrixBytes[idx++] = (byte) (resized.getRaster().getSample(x, y, 0) & 0xFF);
                    }
                }
                return Base64.getEncoder().encodeToString(matrixBytes);
            }
        }
        catch (final IllegalArgumentException | IOException e)
        {
            LOG.error("Failed to decode screenshot or compute SSIM matrix", e);
            return null;
        }
    }

    /**
     * Computes a 64x64 SSIM luminance matrix for a micro-cropped region around (x, y) with radius r.
     *
     * @param base64Png the Base64-encoded PNG screenshot string
     * @param x center X coordinate
     * @param y center Y coordinate
     * @param radius crop radius (half-width / half-height)
     * @return the Base64-encoded 64x64 luminance matrix of the cropped tile, or null
     */
    public static String computeTileSsimMatrix(final String base64Png, final int x, final int y, final int radius)
    {
        if (base64Png == null || base64Png.isEmpty())
        {
            return null;
        }

        try
        {
            final byte[] imageBytes = Base64.getDecoder().decode(base64Png);
            try (final ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes))
            {
                final BufferedImage image = ImageIO.read(bais);
                if (image == null)
                {
                    return null;
                }

                final int imgW = image.getWidth();
                final int imgH = image.getHeight();
                final int cropX = Math.max(0, Math.min(imgW - 1, x - radius));
                final int cropY = Math.max(0, Math.min(imgH - 1, y - radius));
                final int cropW = Math.max(1, Math.min(imgW - cropX, radius * 2));
                final int cropH = Math.max(1, Math.min(imgH - cropY, radius * 2));

                final BufferedImage subImage = image.getSubimage(cropX, cropY, cropW, cropH);

                final int matrixDim = 64;
                final BufferedImage resized = new BufferedImage(matrixDim, matrixDim, BufferedImage.TYPE_BYTE_GRAY);
                final Graphics2D g = resized.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(subImage, 0, 0, matrixDim, matrixDim, null);
                g.dispose();

                final byte[] matrixBytes = new byte[matrixDim * matrixDim];
                int idx = 0;
                for (int ty = 0; ty < matrixDim; ty++)
                {
                    for (int tx = 0; tx < matrixDim; tx++)
                    {
                        matrixBytes[idx++] = (byte) (resized.getRaster().getSample(tx, ty, 0) & 0xFF);
                    }
                }
                return Base64.getEncoder().encodeToString(matrixBytes);
            }
        }
        catch (final Exception e)
        {
            LOG.error("Failed to compute tile SSIM matrix", e);
            return null;
        }
    }

    /**
     * Calculates the Mean SSIM score (0.0 to 1.0) between two Base64-encoded 64x64 luminance matrices.
     *
     * @param base64Matrix1 the first Base64 luminance matrix
     * @param base64Matrix2 the second Base64 luminance matrix
     * @return SSIM score between 0.0 and 1.0 (1.0 = identical), or 0.0 if invalid
     */
    public static double calculateSsim(final String base64Matrix1, final String base64Matrix2)
    {
        if (base64Matrix1 == null || base64Matrix2 == null)
        {
            return 0.0;
        }

        try
        {
            final byte[] bytes1 = Base64.getDecoder().decode(base64Matrix1);
            final byte[] bytes2 = Base64.getDecoder().decode(base64Matrix2);

            if (bytes1.length != bytes2.length || bytes1.length != 64 * 64)
            {
                return 0.0;
            }

            final int width = 64;
            final int height = 64;
            final int windowSize = 8;
            final double k1 = 0.01;
            final double k2 = 0.03;
            final double L = 255.0;
            final double c1 = Math.pow(k1 * L, 2);
            final double c2 = Math.pow(k2 * L, 2);

            double totalSsim = 0;
            int blockCount = 0;

            for (int y = 0; y < height; y += windowSize)
            {
                for (int x = 0; x < width; x += windowSize)
                {
                    final int w = Math.min(windowSize, width - x);
                    final int h = Math.min(windowSize, height - y);
                    final int numPixels = w * h;

                    double sumX = 0;
                    double sumY = 0;

                    for (int j = 0; j < h; j++)
                    {
                        for (int i = 0; i < w; i++)
                        {
                            final int idx = (y + j) * width + (x + i);
                            final int valX = bytes1[idx] & 0xFF;
                            final int valY = bytes2[idx] & 0xFF;
                            sumX += valX;
                            sumY += valY;
                        }
                    }

                    final double muX = sumX / numPixels;
                    final double muY = sumY / numPixels;

                    double varX = 0;
                    double varY = 0;
                    double covXY = 0;

                    for (int j = 0; j < h; j++)
                    {
                        for (int i = 0; i < w; i++)
                        {
                            final int idx = (y + j) * width + (x + i);
                            final double valX = (bytes1[idx] & 0xFF) - muX;
                            final double valY = (bytes2[idx] & 0xFF) - muY;
                            varX += valX * valX;
                            varY += valY * valY;
                            covXY += valX * valY;
                        }
                    }

                    varX /= numPixels;
                    varY /= numPixels;
                    covXY /= numPixels;

                    final double numerator = (2 * muX * muY + c1) * (2 * covXY + c2);
                    final double denominator = (muX * muX + muY * muY + c1) * (varX + varY + c2);
                    final double blockSsim = numerator / denominator;

                    totalSsim += blockSsim;
                    blockCount++;
                }
            }

            return blockCount > 0 ? Math.max(0.0, Math.min(1.0, totalSsim / blockCount)) : 0.0;
        }
        catch (final IllegalArgumentException e)
        {
            LOG.error("Failed to decode Base64 matrix for SSIM calculation", e);
            return 0.0;
        }
    }
}

