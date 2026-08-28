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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ScreenshotHasher} verifying perceptual dHash computation
 * and Hamming distance calculations across identical, similar, and different images.
 */
final class ScreenshotHasherTest
{


    @Test
    void testComputeSsimMatrix_andCalculateSsim_identicalImages() throws IOException
    {
        final BufferedImage img = createSolidColorImage(Color.BLUE, 200, 200);
        final String base64 = encodeToBase64Png(img);

        final String matrix1 = ScreenshotHasher.computeSsimMatrix(base64);
        final String matrix2 = ScreenshotHasher.computeSsimMatrix(base64);

        assertNotNull(matrix1, "Matrix should not be null.");
        assertEquals(matrix1, matrix2);
        
        final double score = ScreenshotHasher.calculateSsim(matrix1, matrix2);
        assertEquals(1.0, score, 0.001, "Identical images must have SSIM score 1.0.");
    }

    @Test
    void testComputeSsimMatrix_andCalculateSsim_similarImages() throws IOException
    {
        final BufferedImage img1 = createSolidColorImage(Color.WHITE, 200, 200);
        final BufferedImage img2 = createSolidColorImage(Color.WHITE, 200, 200);
        final Graphics2D g = img2.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(95, 95, 10, 10);
        g.dispose();

        final String base64_1 = encodeToBase64Png(img1);
        final String base64_2 = encodeToBase64Png(img2);

        final String matrix1 = ScreenshotHasher.computeSsimMatrix(base64_1);
        final String matrix2 = ScreenshotHasher.computeSsimMatrix(base64_2);

        final double score = ScreenshotHasher.calculateSsim(matrix1, matrix2);
        assertTrue(score >= 0.90, "Minor visual change should yield high SSIM score (>= 0.90), got: " + score);
    }


    @Test
    void testComputeSsimMatrix_andCalculateSsim_differentImages() throws IOException
    {
        final BufferedImage img1 = createSolidColorImage(Color.BLACK, 200, 200);
        final BufferedImage img2 = createCheckerboardImage(200, 200);

        final String base64_1 = encodeToBase64Png(img1);
        final String base64_2 = encodeToBase64Png(img2);

        final String matrix1 = ScreenshotHasher.computeSsimMatrix(base64_1);
        final String matrix2 = ScreenshotHasher.computeSsimMatrix(base64_2);

        final double score = ScreenshotHasher.calculateSsim(matrix1, matrix2);
        assertTrue(score < 0.80, "Visually different images should have low SSIM score, got: " + score);
    }

    @Test
    void testComputeTileSsimMatrix_andCalculateSsim() throws IOException
    {
        final BufferedImage img = createCheckerboardImage(200, 200);
        final String base64 = encodeToBase64Png(img);

        final String tile1 = ScreenshotHasher.computeTileSsimMatrix(base64, 50, 50, 32);
        final String tile2 = ScreenshotHasher.computeTileSsimMatrix(base64, 50, 50, 32);
        final String tileDiff = ScreenshotHasher.computeTileSsimMatrix(base64, 150, 150, 32);

        assertNotNull(tile1);
        assertEquals(tile1, tile2);
        assertEquals(1.0, ScreenshotHasher.calculateSsim(tile1, tile2), 0.001);
        assertEquals(64, ScreenshotHasher.getMatrixDimension(tile1));
    }

    @Test
    void testMatrixDimension_andDataUri() throws IOException
    {
        final BufferedImage img = createSolidColorImage(Color.GREEN, 500, 500);
        final String base64 = encodeToBase64Png(img);

        final String matrix = ScreenshotHasher.computeSsimMatrix(base64);
        assertNotNull(matrix);
        assertEquals(128, ScreenshotHasher.getMatrixDimension(matrix));

        final String dataUri = ScreenshotHasher.matrixToDataUri(matrix);
        assertNotNull(dataUri);
        assertTrue(dataUri.startsWith("data:image/png;base64,"));

        assertEquals(0, ScreenshotHasher.getMatrixDimension(null));
        assertEquals(0, ScreenshotHasher.getMatrixDimension(""));
        assertEquals(0, ScreenshotHasher.getMatrixDimension("invalid_base64"));
        assertNull(ScreenshotHasher.matrixToDataUri(null));
        assertNull(ScreenshotHasher.matrixToDataUri(""));
    }

    @Test
    void testCalculateSsim_mixedDimensions() throws IOException
    {
        final BufferedImage img = createCheckerboardImage(300, 300);
        final String base64 = encodeToBase64Png(img);

        final String fullMatrix = ScreenshotHasher.computeSsimMatrix(base64);
        final String tileMatrix = ScreenshotHasher.computeTileSsimMatrix(base64, 50, 50, 32);

        // Comparing two matrices with different dimensions must return 0.0
        final double score = ScreenshotHasher.calculateSsim(fullMatrix, tileMatrix);
        assertEquals(0.0, score, 0.001);
    }

    @Test
    void testDownsampleProgressive_largeCanvas()
    {
        final BufferedImage large = createSolidColorImage(Color.RED, 1920, 1080);
        final BufferedImage downscaled = ScreenshotHasher.downsampleProgressive(large, 128);

        assertNotNull(downscaled);
        assertEquals(128, downscaled.getWidth());
        assertEquals(128, downscaled.getHeight());
        assertEquals(BufferedImage.TYPE_BYTE_GRAY, downscaled.getType());
    }

    private BufferedImage createSolidColorImage(final Color color, final int width, final int height)

    {
        final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return img;
    }

    private BufferedImage createCheckerboardImage(final int width, final int height)
    {
        final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = img.createGraphics();
        final int size = 10;
        for (int y = 0; y < height; y += size)
        {
            for (int x = 0; x < width; x += size)
            {
                if (((x / size) + (y / size)) % 2 == 0)
                {
                    g.setColor(Color.WHITE);
                }
                else
                {
                    g.setColor(Color.BLACK);
                }
                g.fillRect(x, y, size, size);
            }
        }
        g.dispose();
        return img;
    }

    private String encodeToBase64Png(final BufferedImage image) throws IOException
    {
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream())
        {
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        }
    }
}
