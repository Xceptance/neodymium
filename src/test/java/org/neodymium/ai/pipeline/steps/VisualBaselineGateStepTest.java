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
package org.neodymium.ai.pipeline.steps;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.client.SutAttachment;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.executor.MockSutState;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.DivergenceException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.util.ScreenshotHasher;

/**
 * Unit tests for {@link VisualBaselineGateStep}.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class VisualBaselineGateStepTest
{
    @Test
    public void testNonReplayModeBypassesCheck()
    {
        final MockTargetExecutor executor = new MockTargetExecutor();
        final SessionData sessionData = new SessionData();
        final AiSession session = AiSession.mock(sessionData, null, new ExecutionEventBus(), executor);
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.LLM_ONLY);

        final PlaybookStep step = new PlaybookStep("Verify logo is visible");
        step.setScreenshotHash("a1b2c3d4e5f6");

        final VisualBaselineGateStep gateStep = new VisualBaselineGateStep(step, session);
        assertDoesNotThrow(() -> gateStep.execute(context));
    }

    @Test
    public void testReplayWithMatchingVisualBaseline() throws IOException
    {
        final BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = img.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 200, 200);
        g.dispose();

        final String base64Png = encodeToBase64(img);
        final MockTargetExecutor executor = new MockTargetExecutor();
        final MockSutState state = new MockSutState(
            "<html></html>",
            List.of(new SutAttachment("image/png", "screenshot.png", base64Png)),
            "content_hash");
        executor.enqueueState(state);
        executor.enqueueState(state);

        final SessionData sessionData = new SessionData();
        final AiSession session = AiSession.mock(sessionData, null, new ExecutionEventBus(), executor);
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.REPLAY_STRICT);
        context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);

        final String hash = ScreenshotHasher.computeSsimMatrix(base64Png);
        final PlaybookStep step = new PlaybookStep("Verify logo (visual: full)");
        step.setScreenshotHash(hash);

        final VisualBaselineGateStep gateStep = new VisualBaselineGateStep(step, session);
        assertDoesNotThrow(() -> gateStep.execute(context));

        assertNotNull(step.getSsimScore());
        assertEquals(1.0, step.getSsimScore(), 0.001);
        assertNotNull(step.getBaselineMatrixPng());
        assertNotNull(step.getReplayMatrixPng());
        assertEquals(128, step.getScreenshotHashDim());
    }

    @Test
    public void testReplayWithDivergentVisualBaseline_throwsDivergenceException() throws IOException
    {
        final BufferedImage img1 = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g1 = img1.createGraphics();
        g1.setColor(Color.WHITE);
        g1.fillRect(0, 0, 200, 200);
        g1.dispose();

        final BufferedImage img2 = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g2 = img2.createGraphics();
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, 200, 200);
        g2.dispose();

        final String base64Png1 = encodeToBase64(img1);
        final String base64Png2 = encodeToBase64(img2);

        final MockTargetExecutor executor = new MockTargetExecutor();
        final MockSutState state2 = new MockSutState(
            "<html></html>",
            List.of(new SutAttachment("image/png", "screenshot.png", base64Png2)),
            "content_hash");
        for (int i = 0; i < 6; i++)
        {
            executor.enqueueState(state2);
        }

        final SessionData sessionData = new SessionData();
        final AiSession session = AiSession.mock(sessionData, null, new ExecutionEventBus(), executor);
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.REPLAY_STRICT);
        context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);

        final String hash1 = ScreenshotHasher.computeSsimMatrix(base64Png1);
        final PlaybookStep step = new PlaybookStep("Verify logo (visual: full)");
        step.setScreenshotHash(hash1);

        final VisualBaselineGateStep gateStep = new VisualBaselineGateStep(step, session);
        assertThrows(DivergenceException.class, () -> gateStep.execute(context));

        assertNotNull(step.getSsimScore());
        assertTrue(step.getSsimScore() < 0.99);
        assertNotNull(step.getBaselineMatrixPng());
        assertNotNull(step.getReplayMatrixPng());
    }

    @Test
    public void testReplayWithDivergentVisualBaseline_withAssertAction_throwsDivergenceException() throws IOException
    {
        final BufferedImage img1 = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g1 = img1.createGraphics();
        g1.setColor(Color.WHITE);
        g1.fillRect(0, 0, 200, 200);
        g1.dispose();

        final BufferedImage img2 = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g2 = img2.createGraphics();
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, 200, 200);
        g2.dispose();

        final String base64Png1 = encodeToBase64(img1);
        final String base64Png2 = encodeToBase64(img2);

        final MockTargetExecutor executor = new MockTargetExecutor();
        final MockSutState state2 = new MockSutState(
            "<html></html>",
            List.of(new SutAttachment("image/png", "screenshot.png", base64Png2)),
            "content_hash");
        for (int i = 0; i < 6; i++)
        {
            executor.enqueueState(state2);
        }

        final SessionData sessionData = new SessionData();
        final AiSession session = AiSession.mock(sessionData, null, new ExecutionEventBus(), executor);
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.REPLAY_STRICT);
        context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);

        final String hash1 = ScreenshotHasher.computeSsimMatrix(base64Png1);
        final PlaybookStep step = new PlaybookStep("Verify logo (visual)");
        step.setScreenshotHash(hash1);
        final Action assertAction = new Action("ASSERT", "#logo", "visible");
        step.getActions().add(assertAction);

        final VisualBaselineGateStep gateStep = new VisualBaselineGateStep(step, session);
        assertThrows(DivergenceException.class, () -> gateStep.execute(context));

        assertNotNull(step.getSsimScore());
        assertTrue(step.getSsimScore() < 0.99);
    }

    @Test
    public void testReplayWithMatchingVisualBaseline_withAssertAction_shortCircuits() throws IOException
    {
        final BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = img.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 200, 200);
        g.dispose();

        final String base64Png = encodeToBase64(img);
        final MockTargetExecutor executor = new MockTargetExecutor();
        final MockSutState state = new MockSutState(
            "<html></html>",
            List.of(new SutAttachment("image/png", "screenshot.png", base64Png)),
            "content_hash");
        executor.enqueueState(state);
        executor.enqueueState(state);

        final SessionData sessionData = new SessionData();
        final AiSession session = AiSession.mock(sessionData, null, new ExecutionEventBus(), executor);
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.REPLAY_STRICT);
        context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);

        final String hash = ScreenshotHasher.computeSsimMatrix(base64Png);
        final PlaybookStep step = new PlaybookStep("Verify logo (visual)");
        step.setScreenshotHash(hash);
        final Action assertAction = new Action("ASSERT", "#logo", "visible");
        step.getActions().add(assertAction);

        final VisualBaselineGateStep gateStep = new VisualBaselineGateStep(step, session);
        assertDoesNotThrow(() -> gateStep.execute(context));

        assertNotNull(step.getSsimScore());
        assertEquals(1.0, step.getSsimScore(), 0.001);
    }

    @Test
    public void testReplayWithMatchingVisualBaseline_withFullPageProperty_withoutInstructionTag() throws IOException
    {
        final BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = img.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 200, 200);
        g.dispose();

        final String base64Png = encodeToBase64(img);
        final MockTargetExecutor executor = new MockTargetExecutor();
        final MockSutState state = new MockSutState(
            "<html></html>",
            List.of(new SutAttachment("image/png", "screenshot.png", base64Png)),
            "content_hash");
        executor.enqueueState(state);
        executor.enqueueState(state);

        final SessionData sessionData = new SessionData();
        final AiSession session = AiSession.mock(sessionData, null, new ExecutionEventBus(), executor);
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.REPLAY_STRICT);
        context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);

        final String hash = ScreenshotHasher.computeSsimMatrix(base64Png);
        final PlaybookStep step = new PlaybookStep("Verify logo");
        step.setFullPage(true);
        step.setScreenshotHash(hash);
        final Action assertAction = new Action("ASSERT", "#logo", "visible");
        step.getActions().add(assertAction);

        final VisualBaselineGateStep gateStep = new VisualBaselineGateStep(step, session);
        assertDoesNotThrow(() -> gateStep.execute(context));

        assertNotNull(step.getSsimScore());
        assertEquals(1.0, step.getSsimScore(), 0.001);
    }

    private static String encodeToBase64(final BufferedImage image) throws IOException
    {
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream())
        {
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        }
    }
}
