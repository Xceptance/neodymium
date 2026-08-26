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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.client.SutAttachment;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.event.structural.StateCapturedEvent;
import org.neodymium.ai.event.structural.StepFinishedEvent;
import org.neodymium.ai.executor.MockSutState;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.PlaybookStepStatus;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.util.ScreenshotHasher;

/**
 * Dedicated unit tests for {@link ExecuteActionsStep}.
 * Validates execution of actions, custom timeout parsing, transient context validation,
 * and error propagation.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class ExecuteActionsStepTest
{
    @Test
    public void testExecuteThrowsConclusiveFailureWhenSessionMissing()
    {
        final ExecutionContext context = new ExecutionContext(new SessionData());
        final ExecuteActionsStep step = new ExecuteActionsStep();

        final ConclusiveFailureException ex = assertThrows(ConclusiveFailureException.class, () -> {
            step.execute(context);
        });

        assertTrue(ex.getMessage().contains("No active AiSession registered"));
    }

    @Test
    public void testExecuteThrowsConclusiveFailureWhenExecutorMissing()
    {
        final ExecutionContext context = new ExecutionContext(new SessionData());
        final MockLlmProvider mockProvider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockProvider);
        final AiSession session = AiSession.mock(new SessionData(), registry, new ExecutionEventBus(), new MockTargetExecutor());

        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);

        final ExecuteActionsStep step = new ExecuteActionsStep();

        final ConclusiveFailureException ex = assertThrows(ConclusiveFailureException.class, () -> {
            step.execute(context);
        });

        assertTrue(ex.getMessage().contains("No active TargetExecutor registered"));
    }

    @Test
    public void testExecuteActionsSuccessfully() throws PipelineException
    {
        final MockTargetExecutor executor = new MockTargetExecutor();
        final MockLlmProvider mockProvider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockProvider);

        final SessionData sessionData = new SessionData();
        final AiSession session = AiSession.mock(sessionData, registry, new ExecutionEventBus(), executor);
        final ExecutionContext context = session.getExecutionContext();

        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);
        context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);

        final List<Action> actions = List.of(
            new Action("NAVIGATE", "http://localhost:8080", null, "Open page", "Reason 1"),
            new Action("CLICK", "#submit-btn", null, "Click submit", "Reason 2")
        );
        context.getTransientData().put("KEY_CURRENT_STEP_ACTIONS", actions);

        final ExecuteActionsStep step = new ExecuteActionsStep();
        step.execute(context);

        assertNotNull(context);
        assertEquals(actions, context.getTransientData().get("KEY_CURRENT_STEP_ACTIONS"));
    }

    /**
     * Verifies that mapping and executing a step in REPLAY_STRICT mode throws ConclusiveFailureException
     * when the step has no recorded actions and is not a visual/composite step.
     */
    @Test
    public void testReplayStrictThrowsWhenStepHasNoRecordedActions()
    {
        final MockTargetExecutor executor = new MockTargetExecutor();
        final MockLlmProvider mockProvider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockProvider);

        final SessionData sessionData = new SessionData();
        final AiSession session = AiSession.mock(sessionData, registry, new ExecutionEventBus(), executor);
        final ExecutionContext context = session.getExecutionContext();

        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);
        context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, org.neodymium.ai.config.ExecutionMode.REPLAY_STRICT);
        context.getTransientData().put(ExecutionContext.KEY_ACTIVE_PROMPT, new org.neodymium.ai.prompt.ActionExtractionPrompt());

        final org.neodymium.ai.model.PlaybookStep emptyStep = new org.neodymium.ai.model.PlaybookStep();
        emptyStep.setInstruction("Click the checkout button");

        final org.neodymium.ai.pipeline.PipelineStep pipelineStep = ExecuteActionsStep.mapPlaybookStepToPipelineStep(emptyStep, session, context);

        final ConclusiveFailureException ex = assertThrows(ConclusiveFailureException.class, () -> {
            pipelineStep.execute(context);
            // Execute any pushed sequence steps
            while (context.hasSteps())
            {
                context.popStep().execute(context);
            }
        });

        assertTrue(ex.getMessage().contains("No recorded actions found for step"));
    }

    @Test
    public void testReplayStrictSucceedsWhenRecordedStepCompletedWithZeroActions() throws PipelineException
    {
        final MockTargetExecutor executor = new MockTargetExecutor();
        final MockLlmProvider mockProvider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockProvider);

        final SessionData sessionData = new SessionData();
        final AiSession session = AiSession.mock(sessionData, registry, new ExecutionEventBus(), executor);
        final ExecutionContext context = session.getExecutionContext();

        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);
        context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, org.neodymium.ai.config.ExecutionMode.REPLAY_STRICT);
        context.getTransientData().put(ExecutionContext.KEY_ACTIVE_PROMPT, new org.neodymium.ai.prompt.ActionExtractionPrompt());

        final org.neodymium.ai.model.PlaybookStep recordedStep = new org.neodymium.ai.model.PlaybookStep();
        recordedStep.setInstruction("When this string '' is not empty, enter '' as state.");
        recordedStep.setStatus(org.neodymium.ai.model.PlaybookStepStatus.SUCCESS);

        final org.neodymium.ai.pipeline.PipelineStep pipelineStep = ExecuteActionsStep.mapPlaybookStepToPipelineStep(recordedStep, session, context);

        // Must succeed without throwing ConclusiveFailureException
        pipelineStep.execute(context);
        while (context.hasSteps())
        {
            context.popStep().execute(context);
        }

        assertEquals(org.neodymium.ai.model.PlaybookStepStatus.SUCCESS, recordedStep.getStatus());
    }

    @Test
    public void testExecuteActionsInscribesTargetFramework() throws PipelineException
    {
        final MockTargetExecutor executor = new MockTargetExecutor();
        final MockLlmProvider mockProvider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockProvider);

        final SessionData sessionData = new SessionData();
        final AiSession session = AiSession.mock(sessionData, registry, new ExecutionEventBus(), executor);
        final ExecutionContext context = session.getExecutionContext();

        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);
        context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, org.neodymium.ai.config.ExecutionMode.FORCE_RECORDING);

        final org.neodymium.ai.model.PlaybookStep step = new org.neodymium.ai.model.PlaybookStep();
        step.setInstruction("Click submit");
        context.getTransientData().put("KEY_CURRENT_PLAYBOOK_STEP", step);

        final List<Action> actions = List.of(
            new Action("CLICK", "#submit-btn", null, "Click submit", "Reason")
        );
        context.getTransientData().put("KEY_CURRENT_STEP_ACTIONS", actions);

        final ExecuteActionsStep executeStep = new ExecuteActionsStep();
        executeStep.execute(context);

        assertEquals("SELENIUM_SELENIDE", step.getTargetFramework());
    }

    @Test
    public void testVisualStepExecutionDispatchesStateCapturedEvent() throws PipelineException
    {
        final MockTargetExecutor executor = new MockTargetExecutor();
        final String base64Png = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
        final SutAttachment screenshot = new SutAttachment("image/png", "shot.png", base64Png);
        final MockSutState visualState = new MockSutState("<html><body>Visual Layout</body></html>", List.of(screenshot), "hash-vis-1");
        executor.enqueueState(visualState);

        final MockLlmProvider mockProvider = new MockLlmProvider();
        // 1. PESAP response predicting VISUAL context
        mockProvider.addResponse(new LlmResponse("{\"c\":\"VISUAL\"}", new TokenUsage(100, 20, 120, 0), "mock-model"));
        // 2. Action extraction response returning a NONE action (pure visual verification passing)
        mockProvider.addResponse(new LlmResponse("[{\"action\": \"NONE\", \"target\": \"\", \"value\": \"Visual layout verified\"}]", new TokenUsage(200, 30, 230, 0), "mock-model"));

        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockProvider);

        final SessionData sessionData = new SessionData();
        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final AtomicBoolean stateCapturedReceived = new AtomicBoolean(false);
        eventBus.registerListener(event -> {
            if (event instanceof StateCapturedEvent sce)
            {
                if (sce.getState() != null && sce.getState().getAttachments() != null && !sce.getState().getAttachments().isEmpty())
                {
                    stateCapturedReceived.set(true);
                }
            }
        });

        final AiSession session = AiSession.mock(sessionData, registry, eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();

        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);
        context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, org.neodymium.ai.config.ExecutionMode.LLM_RECORDING);

        final PlaybookStep visualStep = new PlaybookStep();
        visualStep.setInstruction("There are data input forms on the left and order summary on the right (visual).");

        final PipelineStep pipelineStep = ExecuteActionsStep.mapPlaybookStepToPipelineStep(visualStep, session, context);
        pipelineStep.execute(context);
        while (context.hasSteps())
        {
            context.popStep().execute(context);
        }

        assertTrue(stateCapturedReceived.get(), "StateCapturedEvent with screenshot attachment must be dispatched during visual step execution");
    }

    @Test
    public void testReplayVisualBypassSetsSuccessAndDispatchesStepFinishedEvent() throws PipelineException, IOException
    {
        final BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 200, 200);
        g.dispose();

        final String base64Png = encodeToBase64(img);
        final String recordedMatrix = ScreenshotHasher.computeSsimMatrix(base64Png);

        final MockTargetExecutor executor = new MockTargetExecutor();
        final SutAttachment screenshot = new SutAttachment("image/png", "shot.png", base64Png);
        final MockSutState visualState = new MockSutState("<html><body>Confirmed</body></html>", List.of(screenshot), "hash-vis-replay");
        executor.enqueueState(visualState);

        final SessionData sessionData = new org.neodymium.ai.model.SessionData();
        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final AtomicReference<StepFinishedEvent> finishedEventRef = new AtomicReference<>();
        eventBus.registerListener(event -> {
            if (event instanceof StepFinishedEvent sfe)
            {
                finishedEventRef.set(sfe);
            }
        });

        final AiSession session = AiSession.mock(sessionData, new LlmRegistry(), eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();

        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);
        context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, org.neodymium.ai.config.ExecutionMode.REPLAY_STRICT);

        final PlaybookStep visualStep = new PlaybookStep("Green checkmark is displayed (visual: full)");
        visualStep.setScreenshotHash(recordedMatrix);

        final PipelineStep pipelineStep = ExecuteActionsStep.mapPlaybookStepToPipelineStep(visualStep, session, context);
        pipelineStep.execute(context);
        while (context.hasSteps())
        {
            context.popStep().execute(context);
        }

        assertEquals(PlaybookStepStatus.SUCCESS, visualStep.getStatus());
        assertNotNull(visualStep.getSsimScore());
        assertTrue(visualStep.getSsimScore() >= 0.99);
        assertNotNull(finishedEventRef.get(), "StepFinishedEvent must be dispatched when visual gate bypasses step");
        assertEquals(PlaybookStepStatus.SUCCESS, finishedEventRef.get().getStatus());
        assertEquals(visualStep, finishedEventRef.get().getStep());
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
