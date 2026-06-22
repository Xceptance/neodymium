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
package com.xceptance.neodymium.ai.util;

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
    void testComputeHash_nullOrEmptyInput()
    {
        assertNull(ScreenshotHasher.computeHash(null));
        assertNull(ScreenshotHasher.computeHash(""));
    }

    @Test
    void testComputeHash_invalidBase64()
    {
        assertNull(ScreenshotHasher.computeHash("not-a-valid-base64-string!!!"));
    }

    @Test
    void testComputeHash_andHammingDistance_identicalImages() throws IOException
    {
        final BufferedImage img = createSolidColorImage(Color.BLUE, 200, 200);
        final String base64 = encodeToBase64Png(img);

        final String hash1 = ScreenshotHasher.computeHash(base64);
        final String hash2 = ScreenshotHasher.computeHash(base64);

        assertNotNull(hash1);
        assertEquals(64, hash1.length());
        assertEquals(hash1, hash2);
        assertEquals(0, ScreenshotHasher.getHammingDistance(hash1, hash2));
    }

    @Test
    void testComputeHash_andHammingDistance_similarImages() throws IOException
    {
        final BufferedImage img1 = createSolidColorImage(Color.WHITE, 100, 100);
        // Slightly modified image (a few pixels in the middle changed to black)
        final BufferedImage img2 = createSolidColorImage(Color.WHITE, 100, 100);
        final Graphics2D g = img2.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(45, 45, 10, 10);
        g.dispose();

        final String base64_1 = encodeToBase64Png(img1);
        final String base64_2 = encodeToBase64Png(img2);

        final String hash1 = ScreenshotHasher.computeHash(base64_1);
        final String hash2 = ScreenshotHasher.computeHash(base64_2);

        assertNotNull(hash1);
        assertNotNull(hash2);

        final int distance = ScreenshotHasher.getHammingDistance(hash1, hash2);
        // Perceptually extremely similar, distance should be very low (e.g. <= 8)
        assertTrue(distance <= 8, "Expected low Hamming distance for minor visual changes, but got: " + distance);
    }

    @Test
    void testComputeHash_andHammingDistance_completelyDifferentImages() throws IOException
    {
        final BufferedImage img1 = createSolidColorImage(Color.BLACK, 100, 100);
        final BufferedImage img2 = createCheckerboardImage(100, 100);

        final String base64_1 = encodeToBase64Png(img1);
        final String base64_2 = encodeToBase64Png(img2);

        final String hash1 = ScreenshotHasher.computeHash(base64_1);
        final String hash2 = ScreenshotHasher.computeHash(base64_2);

        assertNotNull(hash1);
        assertNotNull(hash2);

        final int distance = ScreenshotHasher.getHammingDistance(hash1, hash2);
        // Completely different visual appearance, distance should be substantial
        assertTrue(distance > 20, "Expected high Hamming distance for visually different images, but got: " + distance);
    }

    @Test
    void testGetHammingDistance_invalidInputs()
    {
        assertEquals(Integer.MAX_VALUE, ScreenshotHasher.getHammingDistance(null, "some-hash"));
        assertEquals(Integer.MAX_VALUE, ScreenshotHasher.getHammingDistance("some-hash", null));
        // Mismatched lengths
        assertEquals(Integer.MAX_VALUE, ScreenshotHasher.getHammingDistance("abc", "abcd"));
        // Invalid hex characters
        assertEquals(Integer.MAX_VALUE, ScreenshotHasher.getHammingDistance("g123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"));
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
