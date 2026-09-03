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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.client.SutAttachment;
import org.neodymium.ai.executor.MockSutState;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.executor.SutState;

/**
 * Unit tests for {@link VisualStabilityDetector} verifying temporal inter-frame visual settling
 * and stability detection behavior.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
final class VisualStabilityDetectorTest
{
    @Test
    void testCaptureSettledState_immediateStability() throws IOException
    {
        final MockTargetExecutor executor = new MockTargetExecutor();
        final String base64Png = createSolidColorPngBase64(Color.BLUE, 200, 200);

        final SutState state0 = new MockSutState("Frame 0", List.of(new SutAttachment("image/png", "screenshot0.png", base64Png)), "hash0");
        final SutState state1 = new MockSutState("Frame 1", List.of(new SutAttachment("image/png", "screenshot1.png", base64Png)), "hash1");

        executor.enqueueState(state0);
        executor.enqueueState(state1);

        final SutState settled = VisualStabilityDetector.captureSettledState(executor, false, 1000L, 5, 0.999);
        assertNotNull(settled);
        assertEquals("Frame 1", settled.getTextContent());
    }

    @Test
    void testCaptureSettledState_transitioningThenStabilized() throws IOException
    {
        final MockTargetExecutor executor = new MockTargetExecutor();
        final String base64Blue = createSolidColorPngBase64(Color.BLUE, 200, 200);
        final String base64Red = createSolidColorPngBase64(Color.RED, 200, 200);

        final SutState state0 = new MockSutState("Initial Frame 0", List.of(new SutAttachment("image/png", "s0.png", base64Blue)), "h0");
        final SutState state1 = new MockSutState("Transition Frame 1", List.of(new SutAttachment("image/png", "s1.png", base64Red)), "h1");
        final SutState state2 = new MockSutState("Stable Frame 2", List.of(new SutAttachment("image/png", "s2.png", base64Red)), "h2");

        executor.enqueueState(state0);
        executor.enqueueState(state1);
        executor.enqueueState(state2);

        final SutState settled = VisualStabilityDetector.captureSettledState(executor, false, 1000L, 5, 0.999);
        assertNotNull(settled);
        assertEquals("Stable Frame 2", settled.getTextContent());
    }

    @Test
    void testCaptureSettledState_maxAttemptsExceeded() throws IOException
    {
        final MockTargetExecutor executor = new MockTargetExecutor();
        final String base64Blue = createSolidColorPngBase64(Color.BLUE, 200, 200);
        final String base64Red = createSolidColorPngBase64(Color.RED, 200, 200);
        final String base64Green = createSolidColorPngBase64(Color.GREEN, 200, 200);

        // Frame 0: Blue, Frame 1: Red, Frame 2: Green (continuous change)
        executor.enqueueState(new MockSutState("Frame 0", List.of(new SutAttachment("image/png", "s0.png", base64Blue)), "h0"));
        executor.enqueueState(new MockSutState("Frame 1", List.of(new SutAttachment("image/png", "s1.png", base64Red)), "h1"));
        executor.enqueueState(new MockSutState("Frame 2", List.of(new SutAttachment("image/png", "s2.png", base64Green)), "h2"));

        final SutState settled = VisualStabilityDetector.captureSettledState(executor, false, 1000L, 2, 0.999);
        assertNotNull(settled);
        assertEquals("Frame 2", settled.getTextContent());
    }

    @Test
    void testCaptureSettledState_nullExecutorThrows()
    {
        assertThrows(IllegalArgumentException.class, () ->
            VisualStabilityDetector.captureSettledState(null, false, 1000L, 5, 0.999));
    }

    @Test
    void testExtractSsimMatrix() throws IOException
    {
        final String base64 = createSolidColorPngBase64(Color.BLACK, 100, 100);
        final SutState stateWithImage = new MockSutState("DOM", List.of(new SutAttachment("image/png", "shot.png", base64)), "h1");
        final SutState stateWithoutImage = new MockSutState("DOM", Collections.emptyList(), "h2");

        assertNotNull(VisualStabilityDetector.extractSsimMatrix(stateWithImage));
        assertNull(VisualStabilityDetector.extractSsimMatrix(stateWithoutImage));
        assertNull(VisualStabilityDetector.extractSsimMatrix(null));
    }

    private static String createSolidColorPngBase64(final Color color, final int width, final int height) throws IOException
    {
        final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();

        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream())
        {
            ImageIO.write(img, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        }
    }
}
