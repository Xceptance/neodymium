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
     * Default matrix dimension for full-page and viewport perceptual SSIM luminance matrices (128x128 = 16,384 bytes).
     */
    public static final int DEFAULT_SSIM_MATRIX_DIM = 128;

    /**
     * Tile matrix dimension for coordinate click perceptual SSIM luminance matrices (64x64 = 4,096 bytes).
     */
    public static final int TILE_SSIM_MATRIX_DIM = 64;

    /**
     * Progressively downsamples an image to the target dimension using multi-pass half-stepping
     * to eliminate single-pass point-sampling aliasing on high-contrast text and UI elements.
     *
     * @param image the source BufferedImage
     * @param targetDim the target square dimension (e.g. 128 or 64)
     * @return a grayscale BufferedImage of size targetDim x targetDim
     */
    public static BufferedImage downsampleProgressive(final BufferedImage image, final int targetDim)
    {
        if (image == null)
        {
            return null;
        }

        BufferedImage current = image;
        int w = image.getWidth();
        int h = image.getHeight();

        while (w > targetDim * 2 || h > targetDim * 2)
        {
            w = Math.max(targetDim, w / 2);
            h = Math.max(targetDim, h / 2);
            final BufferedImage step = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
            final Graphics2D g = step.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(current, 0, 0, w, h, null);
            g.dispose();
            current = step;
        }

        final BufferedImage finalImg = new BufferedImage(targetDim, targetDim, BufferedImage.TYPE_BYTE_GRAY);
        final Graphics2D g = finalImg.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(current, 0, 0, targetDim, targetDim, null);
        g.dispose();

        return finalImg;
    }

    /**
     * Computes a 128x128 luminance matrix (16,384 bytes) for a Base64-encoded PNG screenshot,
     * downscaled using progressive multi-pass bilinear half-stepping, and encoded as a Base64 string.
     *
     * @param base64Png the Base64-encoded PNG screenshot string
     * @return the Base64-encoded 128x128 luminance matrix, or {@code null} if input is invalid
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

                final BufferedImage resized = downsampleProgressive(image, DEFAULT_SSIM_MATRIX_DIM);
                if (resized == null)
                {
                    return null;
                }

                final byte[] matrixBytes = new byte[DEFAULT_SSIM_MATRIX_DIM * DEFAULT_SSIM_MATRIX_DIM];
                int idx = 0;
                for (int y = 0; y < DEFAULT_SSIM_MATRIX_DIM; y++)
                {
                    for (int x = 0; x < DEFAULT_SSIM_MATRIX_DIM; x++)
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
                final BufferedImage resized = downsampleProgressive(subImage, TILE_SSIM_MATRIX_DIM);
                if (resized == null)
                {
                    return null;
                }

                final byte[] matrixBytes = new byte[TILE_SSIM_MATRIX_DIM * TILE_SSIM_MATRIX_DIM];
                int idx = 0;
                for (int ty = 0; ty < TILE_SSIM_MATRIX_DIM; ty++)
                {
                    for (int tx = 0; tx < TILE_SSIM_MATRIX_DIM; tx++)
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
     * Determines the square dimension (e.g. 128 or 64) of a Base64-encoded SSIM luminance matrix.
     *
     * @param base64Matrix the Base64-encoded luminance matrix string
     * @return the square matrix dimension, or 0 if invalid
     */
    public static int getMatrixDimension(final String base64Matrix)
    {
        if (base64Matrix == null || base64Matrix.isBlank())
        {
            return 0;
        }

        try
        {
            final byte[] bytes = Base64.getDecoder().decode(base64Matrix);
            final int len = bytes.length;
            final int dim = (int) Math.sqrt(len);
            return (dim * dim == len) ? dim : 0;
        }
        catch (final Exception e)
        {
            return 0;
        }
    }

    /**
     * Converts a Base64-encoded SSIM luminance matrix into a viewable PNG data URI (e.g. data:image/png;base64,...).
     *
     * @param base64Matrix the Base64-encoded luminance matrix
     * @return the PNG data URI string, or {@code null} if invalid
     */
    public static String matrixToDataUri(final String base64Matrix)
    {
        if (base64Matrix == null || base64Matrix.isBlank())
        {
            return null;
        }

        try
        {
            final byte[] bytes = Base64.getDecoder().decode(base64Matrix);
            final int len = bytes.length;
            final int dim = (int) Math.sqrt(len);
            if (dim * dim != len || dim == 0)
            {
                return null;
            }

            final BufferedImage img = new BufferedImage(dim, dim, BufferedImage.TYPE_BYTE_GRAY);
            final byte[] imgData = ((java.awt.image.DataBufferByte) img.getRaster().getDataBuffer()).getData();
            System.arraycopy(bytes, 0, imgData, 0, len);

            try (final java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream())
            {
                ImageIO.write(img, "png", baos);
                return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
            }
        }
        catch (final Exception e)
        {
            LOG.warn("Failed to convert SSIM matrix to PNG data URI", e);
            return null;
        }
    }

    /**
     * Calculates the Mean SSIM score (0.0 to 1.0) between two Base64-encoded luminance matrices.
     * Evaluates across 8x8 sliding blocks dynamically matching the square matrix dimension.
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

            if (bytes1.length != bytes2.length)
            {
                return 0.0;
            }

            final int totalLen = bytes1.length;
            final int dim = (int) Math.sqrt(totalLen);
            if (dim * dim != totalLen || dim < 8)
            {
                return 0.0;
            }

            final int width = dim;
            final int height = dim;
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

