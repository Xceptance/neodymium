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
 *
 * // AI-generated: Antigravity (Gemini 2.5 Pro)
 */
package com.xceptance.neodymium.ai.util;

import java.awt.Graphics;
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
     * Computes a 256-bit perceptual difference hash (dHash) for a Base64-encoded PNG screenshot.
     *
     * @param base64Png the Base64-encoded PNG image string
     * @return the computed dHash as a 64-character hexadecimal string, or {@code null} if an error occurs
     */
    public static String computeHash(final String base64Png)
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
                return computeDHash(image);
            }
        }
        catch (final IllegalArgumentException | IOException e)
        {
            LOG.error("Failed to decode screenshot or compute hash", e);
            return null;
        }
    }

    /**
     * Internal implementation of 256-bit dHash (16x16 output resolution).
     * Resizes the image to 17x16 gray, compares adjacent pixels, and converts to a hex string.
     */
    private static String computeDHash(final BufferedImage img)
    {
        final int width = 17;
        final int height = 16;

        final BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        final Graphics g = resized.createGraphics();
        g.drawImage(img, 0, 0, width, height, null);
        g.dispose();

        final StringBuilder sb = new StringBuilder();
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width - 1; x++)
            {
                final int p1 = resized.getRaster().getSample(x, y, 0);
                final int p2 = resized.getRaster().getSample(x + 1, y, 0);
                sb.append(p1 > p2 ? "1" : "0");
            }
        }

        final String binaryStr = sb.toString();
        final StringBuilder hex = new StringBuilder();
        for (int i = 0; i < binaryStr.length(); i += 4)
        {
            final String part = binaryStr.substring(i, i + 4);
            hex.append(Integer.toHexString(Integer.parseInt(part, 2)));
        }
        return hex.toString();
    }

    /**
     * Calculates the Hamming distance (number of differing bits) between two dHashes.
     *
     * @param hash1 the first hex hash
     * @param hash2 the second hex hash
     * @return the number of differing bits, or {@link Integer#MAX_VALUE} if hashes are invalid/mismatched
     */
    public static int getHammingDistance(final String hash1, final String hash2)
    {
        if (hash1 == null || hash2 == null || hash1.length() != hash2.length())
        {
            return Integer.MAX_VALUE;
        }

        int distance = 0;
        for (int i = 0; i < hash1.length(); i++)
        {
            final char c1 = hash1.charAt(i);
            final char c2 = hash2.charAt(i);
            if (c1 != c2)
            {
                final int val1 = Character.digit(c1, 16);
                final int val2 = Character.digit(c2, 16);
                if (val1 < 0 || val2 < 0)
                {
                    return Integer.MAX_VALUE;
                }
                final int xor = val1 ^ val2;
                distance += Integer.bitCount(xor);
            }
        }
        return distance;
    }
}
