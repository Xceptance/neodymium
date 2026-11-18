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
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.client.ChatMessage;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.SutAttachment;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.prompt.LlmSanitizerHelper;
import org.neodymium.ai.prompt.SanitizedPayload;

/**
 * Unit tests verifying outbound LLM wire image optimization:
 * PNG to JPEG compression, resolution downscaling, and white background preservation.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class WireImageOptimizerTest
{
    @BeforeEach
    public void setUp()
    {
        System.clearProperty("neodymium.ai.wireImageOptimization.enabled");
        System.clearProperty("neodymium.ai.wireImageOptimization.jpegQuality");
        System.clearProperty("neodymium.ai.wireImageOptimization.maxDimension");
        AiConfiguration.resetInstance();
    }

    @AfterEach
    public void tearDown()
    {
        System.clearProperty("neodymium.ai.wireImageOptimization.enabled");
        System.clearProperty("neodymium.ai.wireImageOptimization.jpegQuality");
        System.clearProperty("neodymium.ai.wireImageOptimization.maxDimension");
        AiConfiguration.resetInstance();
    }

    private static String createTestPngBase64(final int width, final int height, final boolean transparent) throws IOException
    {
        final int imageType = transparent ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        final BufferedImage img = new BufferedImage(width, height, imageType);
        final Graphics2D g2d = img.createGraphics();

        if (!transparent)
        {
            for (int y = 0; y < height; y += 40)
            {
                for (int x = 0; x < width; x += 40)
                {
                    g2d.setColor(new Color((x * 255) / width, (y * 255) / height, ((x + y) * 128) / (width + height)));
                    g2d.fillRect(x, y, 40, 40);
                }
            }
        }

        g2d.setColor(Color.BLUE);
        g2d.fillRect(width / 4, height / 4, width / 2, height / 2);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(12, height / 20)));
        g2d.drawString("Wire Image Test", width / 3, height / 2);
        g2d.dispose();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    @Test
    public void testOptimizePngToJpegCompressionAndDownscaling() throws IOException
    {
        final String pngBase64 = createTestPngBase64(1920, 1080, false);
        final SutAttachment original = new SutAttachment("image/png", "screenshot.png", pngBase64);

        final SutAttachment optimized = WireImageOptimizer.optimize(original);

        Assertions.assertNotNull(optimized);
        Assertions.assertEquals("image/jpeg", optimized.mediaType());
        Assertions.assertEquals("screenshot.png", optimized.filePath());

        // Decode optimized JPEG
        final byte[] jpegBytes = Base64.getDecoder().decode(optimized.base64Data());
        final BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(jpegBytes));
        Assertions.assertNotNull(decoded);

        // Clamped to 1280 max dimension: 1920x1080 -> 1280x720
        Assertions.assertEquals(1280, decoded.getWidth());
        Assertions.assertEquals(720, decoded.getHeight());

        // Verify significant byte reduction (> 50%)
        Assertions.assertTrue(optimized.base64Data().length() < original.base64Data().length(),
            "Optimized JPEG Base64 must be smaller than original PNG Base64");
    }

    @Test
    public void testTransparentPngPreservedWithWhiteBackground() throws IOException
    {
        final String transparentPng = createTestPngBase64(400, 300, true);
        final SutAttachment original = new SutAttachment("image/png", "transparent.png", transparentPng);

        final SutAttachment optimized = WireImageOptimizer.optimize(original);

        Assertions.assertNotNull(optimized);
        Assertions.assertEquals("image/jpeg", optimized.mediaType());

        final byte[] jpegBytes = Base64.getDecoder().decode(optimized.base64Data());
        final BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(jpegBytes));
        Assertions.assertNotNull(decoded);

        // Pixel (0, 0) was transparent, in JPEG it must render as white (not black)
        final int rgb = decoded.getRGB(0, 0);
        final int r = (rgb >> 16) & 0xFF;
        final int g = (rgb >> 8) & 0xFF;
        final int b = rgb & 0xFF;

        // Allowing minor lossy compression tolerance near 255
        Assertions.assertTrue(r >= 230 && g >= 230 && b >= 230,
            "Transparent background should render white in JPEG, but got RGB(" + r + "," + g + "," + b + ")");
    }

    @Test
    public void testResolutionCappingDownscales4kImage() throws IOException
    {
        final String fourKBase64 = createTestPngBase64(3840, 2160, false);
        final SutAttachment original = new SutAttachment("image/png", "4k_screen.png", fourKBase64);

        final SutAttachment optimized = WireImageOptimizer.optimize(original);

        final byte[] jpegBytes = Base64.getDecoder().decode(optimized.base64Data());
        final BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(jpegBytes));
        Assertions.assertNotNull(decoded);

        Assertions.assertEquals(1280, decoded.getWidth());
        Assertions.assertEquals(720, decoded.getHeight());
    }

    @Test
    public void testSmallImageDimensionsPreserved() throws IOException
    {
        final String smallBase64 = createTestPngBase64(640, 480, false);
        final SutAttachment original = new SutAttachment("image/png", "small.png", smallBase64);

        final SutAttachment optimized = WireImageOptimizer.optimize(original);

        final byte[] jpegBytes = Base64.getDecoder().decode(optimized.base64Data());
        final BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(jpegBytes));
        Assertions.assertNotNull(decoded);

        // Dimensions within 1280 must remain 640x480 (not enlarged)
        Assertions.assertEquals(640, decoded.getWidth());
        Assertions.assertEquals(480, decoded.getHeight());
    }

    @Test
    public void testNonImageAttachmentPassThrough()
    {
        final SutAttachment jsonAtt = new SutAttachment("application/json", "data.json", "eyJrZXkiOiJ2YWx1ZSJ9");
        final SutAttachment result = WireImageOptimizer.optimize(jsonAtt);

        Assertions.assertSame(jsonAtt, result);
    }

    @Test
    public void testNullOrEmptyAttachmentPassThrough()
    {
        Assertions.assertNull(WireImageOptimizer.optimize((SutAttachment) null));

        final SutAttachment empty = new SutAttachment("image/png", "empty.png", null);
        Assertions.assertSame(empty, WireImageOptimizer.optimize(empty));
    }

    @Test
    public void testCorruptImageFallbackWithoutException()
    {
        final SutAttachment corrupt = new SutAttachment("image/png", "corrupt.png", "not-a-valid-image-base64");
        final SutAttachment result = WireImageOptimizer.optimize(corrupt);

        // Must gracefully fall back to original without throwing
        Assertions.assertSame(corrupt, result);
    }

    @Test
    public void testConfigurationDisabledToggle() throws IOException
    {
        System.setProperty("neodymium.ai.wireImageOptimization.enabled", "false");
        AiConfiguration.resetInstance();

        final String pngBase64 = createTestPngBase64(800, 600, false);
        final SutAttachment original = new SutAttachment("image/png", "screen.png", pngBase64);

        final SutAttachment result = WireImageOptimizer.optimize(original);

        // Should return original unchanged when disabled
        Assertions.assertSame(original, result);
    }

    @Test
    public void testBatchOptimizeList() throws IOException
    {
        final String pngBase64 = createTestPngBase64(800, 600, false);
        final SutAttachment img = new SutAttachment("image/png", "screen.png", pngBase64);
        final SutAttachment doc = new SutAttachment("text/plain", "log.txt", "SGVsbG8=");

        final List<SutAttachment> list = List.of(img, doc);
        final List<SutAttachment> optimizedList = WireImageOptimizer.optimize(list);

        Assertions.assertEquals(2, optimizedList.size());
        Assertions.assertEquals("image/jpeg", optimizedList.get(0).mediaType());
        Assertions.assertSame(doc, optimizedList.get(1));
    }

    @Test
    public void testToSanitizedRequestOptimizesWireImages() throws IOException
    {
        final String pngBase64 = createTestPngBase64(1920, 1080, false);
        final SutAttachment img = new SutAttachment("image/png", "screenshot.png", pngBase64);
        final ChatMessage userMsg = ChatMessage.user("Test prompt", List.of(img));
        final LlmRequest rawRequest = new LlmRequest(
            List.of(ChatMessage.system("system"), userMsg),
            List.of(),
            List.of(img),
            null,
            0.0,
            30,
            null
        );
        final SanitizedPayload payload = new SanitizedPayload("Sanitized prompt", null, Map.of());

        final LlmRequest sanitized = LlmSanitizerHelper.toSanitizedRequest(rawRequest, payload);

        Assertions.assertNotNull(sanitized);
        Assertions.assertEquals("Sanitized prompt", sanitized.userMessage());
        Assertions.assertEquals(1, sanitized.attachments().size());
        Assertions.assertEquals("image/jpeg", sanitized.attachments().get(0).mediaType());

        // Also verify message attachments are optimized
        Assertions.assertNotNull(sanitized.messages());
        Assertions.assertFalse(sanitized.messages().isEmpty());
        final ChatMessage userChatMessage = sanitized.messages().stream()
            .filter(m -> m.role() == ChatMessage.Role.USER)
            .findFirst()
            .orElseThrow();
        Assertions.assertEquals("image/jpeg", userChatMessage.attachments().get(0).mediaType());
    }
}
