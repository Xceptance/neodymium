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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.client.SutAttachment;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.event.structural.StepFinishedEvent;
import org.neodymium.ai.executor.MockSutState;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.PlaybookStepStatus;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.pipeline.DivergenceException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;
import org.neodymium.ai.report.DiskReportFormat;
import org.neodymium.ai.report.PreliminaryReportListener;
import org.neodymium.ai.runner.StateMachineRunner;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.tool.AiTool;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolContext;
import org.neodymium.ai.tool.ToolDefinition;
import org.neodymium.ai.tool.ToolRegistry;
import org.neodymium.ai.tool.ToolResult;
import org.neodymium.ai.util.ScreenshotHasher;

/**
 * Dedicated unit tests for {@link ExecuteActionsStep}.
 * Validates step mapping, instruction preparation, and replay execution paths.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class ExecuteActionsStepTest
{
    /**
     * Verifies that mapping and executing a step in REPLAY_STRICT mode throws ConclusiveFailureException
     * when the step has no recorded tool calls and is not a visual/composite step.
     */
    @Test
    public void testReplayStrictThrowsWhenStepHasNoRecordedToolCalls()
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
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.REPLAY_STRICT);

        final PlaybookStep emptyStep = new PlaybookStep();
        emptyStep.setInstruction("Click the checkout button");

        final PipelineStep pipelineStep = ExecuteActionsStep.mapPlaybookStepToPipelineStep(emptyStep, session, context);

        final ConclusiveFailureException ex = assertThrows(ConclusiveFailureException.class, () ->
        {
            pipelineStep.execute(context);
            // Execute any pushed sequence steps
            while (context.hasSteps())
            {
                context.popStep().execute(context);
            }
        });

        assertTrue(ex.getMessage().contains("No recorded tool calls found for step"));
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
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.REPLAY_STRICT);

        final PlaybookStep recordedStep = new PlaybookStep();
        recordedStep.setInstruction("When this string '' is not empty, enter '' as state.");
        recordedStep.setStatus(PlaybookStepStatus.SUCCESS);

        final PipelineStep pipelineStep = ExecuteActionsStep.mapPlaybookStepToPipelineStep(recordedStep, session, context);

        // Must succeed without throwing ConclusiveFailureException
        pipelineStep.execute(context);
        while (context.hasSteps())
        {
            context.popStep().execute(context);
        }

        assertEquals(PlaybookStepStatus.SUCCESS, recordedStep.getStatus());
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

        final SessionData sessionData = new SessionData();
        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final AtomicReference<StepFinishedEvent> finishedEventRef = new AtomicReference<>();
        eventBus.registerListener(event ->
        {
            if (event instanceof StepFinishedEvent sfe)
            {
                finishedEventRef.set(sfe);
            }
        });

        final AiSession session = AiSession.mock(sessionData, new LlmRegistry(), eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();

        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);
        context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.REPLAY_STRICT);

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

    @Test
    public void testPrepareInstructionStrippingControlTags()
    {
        final String raw = "Check cart header (layout) (hint: #cart-badge) (no-replay) (bug: BUG-123) (timeout: 5000ms) (visual:full)";
        final String prepared = ExecuteActionsStep.prepareInstruction(raw);
        assertEquals("Check cart header", prepared);

        final String raw2 = "Click submit (hint) (optional)";
        final String prepared2 = ExecuteActionsStep.prepareInstruction(raw2);
        assertEquals("Click submit", prepared2);
    }

    @Test
    public void testMapPlaybookStepToPipelineStepReplay() throws Exception
    {
        final MockTargetExecutor executor = new MockTargetExecutor();
        final MockLlmProvider mockProvider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockProvider);

        final SessionData sessionData = new SessionData();
        sessionData.set("testVar", "World");
        final ExecutionEventBus eventBus = new ExecutionEventBus();

        final AiSession session = AiSession.mock(sessionData, registry, eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();

        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);
        context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.REPLAY_STRICT);

        final ToolRegistry toolRegistry = new ToolRegistry();
        final AtomicBoolean toolExecuted = new AtomicBoolean(false);
        final AtomicReference<String> resolvedArg = new AtomicReference<>();
        toolRegistry.register(new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return new ToolDefinition("test_tool", "Test Tool", JsonNodeFactory.instance.objectNode());
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext toolContext)
            {
                toolExecuted.set(true);
                resolvedArg.set(call.arguments().path("text").asText());
                return ToolResult.success(call.callId(), "Executed");
            }
        });
        context.getTransientData().put("KEY_TOOL_REGISTRY", toolRegistry);

        final PlaybookStep step = new PlaybookStep("Greet user");
        final ObjectNode args = JsonNodeFactory.instance.objectNode();
        args.put("text", "Hello ${testVar}!");
        step.setToolCalls(List.of(new ToolCall("call-1", "test_tool", args)));

        final PipelineStep pipelineStep = ExecuteActionsStep.mapPlaybookStepToPipelineStep(step, session, context);
        context.pushStep(pipelineStep);

        final StateMachineRunner runner = new StateMachineRunner(session);
        runner.run();

        assertTrue(toolExecuted.get(), "Recorded tool call should have executed via PlaybookToolReplayer");
        assertEquals("Hello World!", resolvedArg.get(), "Variables should have been resolved during replay");
        assertNull(mockProvider.getLastRequest(), "Zero LLM calls should occur during replay");
        assertEquals(PlaybookStepStatus.SUCCESS, step.getStatus(), "Step status should be SUCCESS");
    }

    private static String encodeToBase64(final BufferedImage image) throws IOException
    {
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream())
        {
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        }
    }

    /**
     * Verifies that composite playbook steps with sub-steps schedule their sub-steps sequentially
     * onto the runner pipeline and record individual durations, actions, and status in the preliminary report.
     */
    @Test
    public void testCompositeStepSequentiallySchedulesSubStepsWithDataFidelity() throws Exception
    {
        final MockTargetExecutor executor = new MockTargetExecutor();
        final MockLlmProvider mockProvider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockProvider);

        final SessionData sessionData = new SessionData();
        final ExecutionEventBus eventBus = new ExecutionEventBus();

        final AiSession session = AiSession.mock(sessionData, registry, eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();

        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);
        context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.REPLAY_STRICT);

        final ToolRegistry toolRegistry = new ToolRegistry();
        final List<String> executedTools = new ArrayList<>();
        toolRegistry.register(new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return new ToolDefinition("test_action", "Test Action", JsonNodeFactory.instance.objectNode());
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext toolContext)
            {
                executedTools.add(call.arguments().path("name").asText());
                return ToolResult.success(call.callId(), "OK");
            }
        });
        context.getTransientData().put("KEY_TOOL_REGISTRY", toolRegistry);

        final PlaybookStep parent = new PlaybookStep("Locate the first product card:");
        final PlaybookStep sub1 = new PlaybookStep("Hover over it");
        final ObjectNode args1 = JsonNodeFactory.instance.objectNode();
        args1.put("name", "hover");
        sub1.setToolCalls(List.of(new ToolCall("call-1", "test_action", args1)));
        sub1.getActions().add(new Action("hover", "#card", "Hover card"));

        final PlaybookStep sub2 = new PlaybookStep("Click add to cart");
        final ObjectNode args2 = JsonNodeFactory.instance.objectNode();
        args2.put("name", "click");
        sub2.setToolCalls(List.of(new ToolCall("call-2", "test_action", args2)));
        sub2.getActions().add(new Action("click", "#btn", "Click button"));

        parent.getSubSteps().add(sub1);
        parent.getSubSteps().add(sub2);
        sub1.setParent(parent);
        sub2.setParent(parent);

        final List<PlaybookStep> flatSteps = List.of(sub1, sub2);
        context.getTransientData().put("playbook.flatSteps", flatSteps);
        context.getTransientData().put("playbook.steps", List.of(parent));

        final Path tempDir = Files.createTempDirectory("substep-test-");
        final PreliminaryReportListener listener = new PreliminaryReportListener(tempDir, EnumSet.of(DiskReportFormat.JSON, DiskReportFormat.HTML), true);
        listener.getReport().setTestClass("CompositeStepTest");
        listener.getReport().setTestMethod("testDataFidelity");
        eventBus.registerListener(listener);

        final PipelineStep pipelineStep = ExecuteActionsStep.mapPlaybookStepToPipelineStep(parent, session, context);
        context.pushStep(pipelineStep);

        final StateMachineRunner runner = new StateMachineRunner(session);
        runner.run();
        listener.flushReport();

        assertEquals(List.of("hover", "click"), executedTools, "Both sub-step tool actions must be executed in order");
        assertEquals(PlaybookStepStatus.SUCCESS, parent.getStatus());
        assertEquals(PlaybookStepStatus.SUCCESS, sub1.getStatus());
        assertEquals(PlaybookStepStatus.SUCCESS, sub2.getStatus());
        assertTrue(parent.getDurationMs() >= 0);
        assertTrue(sub1.getDurationMs() >= 0);
        assertTrue(sub2.getDurationMs() >= 0);

        final Path jsonPath = tempDir.resolve(listener.getLastBaseFileName() + ".json");
        assertTrue(Files.exists(jsonPath), "JSON report must exist");
        final JsonNode root = new ObjectMapper().readTree(Files.readString(jsonPath));
        final JsonNode parentNode = root.get("steps").get(0);
        assertEquals("SUCCESS", parentNode.get("status").asText());
        assertEquals(2, parentNode.get("subSteps").size());

        final JsonNode sub1Node = parentNode.get("subSteps").get(0);
        assertEquals("Hover over it", sub1Node.get("instruction").asText());
        assertEquals("SUCCESS", sub1Node.get("status").asText());
        assertEquals(1, sub1Node.get("actions").size());

        final JsonNode sub2Node = parentNode.get("subSteps").get(1);
        assertEquals("Click add to cart", sub2Node.get("instruction").asText());
        assertEquals("SUCCESS", sub2Node.get("status").asText());
        assertEquals(1, sub2Node.get("actions").size());

        final Path htmlPath = tempDir.resolve(listener.getLastBaseFileName() + ".html");
        assertTrue(Files.exists(htmlPath), "HTML report must exist");
        final String html = Files.readString(htmlPath);
        assertTrue(html.contains("substep-item-0-0"), "HTML report must contain sub-step 1 card");
        assertTrue(html.contains("substep-item-0-1"), "HTML report must contain sub-step 2 card");
        assertTrue(html.contains("2 sub-step(s)"), "Parent card must display sub-steps count");
    }

    @Test
    public void testLiveVisualStepRecordsScreenshotHashEvenWhenSemanticVerificationDisabled() throws Exception
    {
        final BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = img.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 200, 200);
        g.dispose();

        final String base64Png = encodeToBase64(img);
        final MockTargetExecutor executor = new MockTargetExecutor();
        final SutAttachment screenshot = new SutAttachment("image/png", "shot.png", base64Png);
        final MockSutState visualState = new MockSutState("<html><body>Order summary</body></html>", List.of(screenshot), "hash-vis-live");
        executor.enqueueState(visualState);

        final MockLlmProvider mockProvider = new MockLlmProvider();
        final ObjectNode args = JsonNodeFactory.instance.objectNode();
        args.put("summary", "Visual check passed");
        final ToolCall completeCall = new ToolCall("call-1", "complete_step", args);
        mockProvider.addResponse(new LlmResponse("", new TokenUsage(100, 20, 120, 0), "mock-model", List.of(completeCall)));

        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockProvider);

        final SessionData sessionData = new SessionData();
        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final AiSession session = AiSession.mock(sessionData, registry, eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();

        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);
        context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.FORCE_RECORDING);
        context.getTransientData().put("semanticVerification.enabled", false);

        final PlaybookStep visualStep = new PlaybookStep("Order summary box is on the right (visual)");

        final PipelineStep pipelineStep = ExecuteActionsStep.mapPlaybookStepToPipelineStep(visualStep, session, context);
        pipelineStep.execute(context);
        while (context.hasSteps())
        {
            context.popStep().execute(context);
        }

        assertEquals(PlaybookStepStatus.SUCCESS, visualStep.getStatus());
        assertNotNull(visualStep.getScreenshotHash(), "Screenshot hash must be recorded during live execution even when semantic verification is disabled");
        assertFalse(visualStep.getActions().isEmpty(), "Visual baseline Action('NONE') must be recorded");
        assertEquals("NONE", visualStep.getActions().get(0).getType());
    }

    @Test
    public void testReplayVisualStepWithActionExecutesActionThenVerifiesPostActionVisualBaseline() throws Exception
    {
        final BufferedImage whiteImg = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g1 = whiteImg.createGraphics();
        g1.setColor(Color.WHITE);
        g1.fillRect(0, 0, 200, 200);
        g1.dispose();

        final BufferedImage blueImg = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g2 = blueImg.createGraphics();
        g2.setColor(Color.BLUE);
        g2.fillRect(0, 0, 200, 200);
        g2.dispose();

        final String whiteBase64 = encodeToBase64(whiteImg);
        final String blueBase64 = encodeToBase64(blueImg);

        // Recorded baseline hash is blue image (post-action)
        final String baselineHash = ScreenshotHasher.computeSsimMatrix(blueBase64);

        final MockTargetExecutor executor = new MockTargetExecutor();
        // 1. State captured during action replay (white)
        executor.enqueueState(new MockSutState("<html></html>", List.of(new SutAttachment("image/png", "pre.png", whiteBase64)), "hash-pre"));
        // 2. Post-action state capture (blue)
        executor.enqueueState(new MockSutState("<html></html>", List.of(new SutAttachment("image/png", "post.png", blueBase64)), "hash-post"));

        final SessionData sessionData = new SessionData();
        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final AiSession session = AiSession.mock(sessionData, new LlmRegistry(), eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();

        final ToolRegistry toolRegistry = new ToolRegistry();
        final AtomicBoolean toolExecuted = new AtomicBoolean(false);
        toolRegistry.register(new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return new ToolDefinition("scroll", "scrolls page", JsonNodeFactory.instance.objectNode());
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                toolExecuted.set(true);
                return ToolResult.success(call.callId(), "scrolled");
            }
        });

        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);
        context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.REPLAY_STRICT);
        context.getTransientData().put("KEY_TOOL_REGISTRY", toolRegistry);
        context.getTransientData().put("semanticVerification.enabled", false);

        final PlaybookStep visualStep = new PlaybookStep("Scroll down to view filter box (visual)");
        visualStep.setScreenshotHash(baselineHash);
        final ObjectNode scrollArgs = JsonNodeFactory.instance.objectNode();
        scrollArgs.put("direction", "down");
        visualStep.setToolCalls(List.of(new ToolCall("call-scroll-1", "scroll", scrollArgs)));

        final PipelineStep pipelineStep = ExecuteActionsStep.mapPlaybookStepToPipelineStep(visualStep, session, context);
        pipelineStep.execute(context);
        while (context.hasSteps())
        {
            context.popStep().execute(context);
        }

        assertTrue(toolExecuted.get(), "Recorded scroll action must be executed during replay");
        assertEquals(PlaybookStepStatus.SUCCESS, visualStep.getStatus());
        assertNotNull(visualStep.getSsimScore());
        assertEquals(1.0, visualStep.getSsimScore(), 0.001);
    }

    @Test
    public void testReplayVisualStepWithActionThrowsDivergenceWhenPostActionVisualBaselineDiffers() throws Exception
    {
        final BufferedImage whiteImg = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g1 = whiteImg.createGraphics();
        g1.setColor(Color.WHITE);
        g1.fillRect(0, 0, 200, 200);
        g1.dispose();

        final BufferedImage blueImg = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g2 = blueImg.createGraphics();
        g2.setColor(Color.BLUE);
        g2.fillRect(0, 0, 200, 200);
        g2.dispose();

        final BufferedImage redImg = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g3 = redImg.createGraphics();
        g3.setColor(Color.RED);
        g3.fillRect(0, 0, 200, 200);
        g3.dispose();

        final String whiteBase64 = encodeToBase64(whiteImg);
        final String blueBase64 = encodeToBase64(blueImg);
        final String redBase64 = encodeToBase64(redImg);

        // Recorded baseline hash is blue image
        final String baselineHash = ScreenshotHasher.computeSsimMatrix(blueBase64);

        final MockTargetExecutor executor = new MockTargetExecutor();
        // 1. State captured during action replay (white)
        executor.enqueueState(new MockSutState("<html></html>", List.of(new SutAttachment("image/png", "pre.png", whiteBase64)), "hash-pre"));
        // 2. Post-action state capture (red != blue)
        executor.enqueueState(new MockSutState("<html></html>", List.of(new SutAttachment("image/png", "post.png", redBase64)), "hash-post"));

        final SessionData sessionData = new SessionData();
        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final AiSession session = AiSession.mock(sessionData, new LlmRegistry(), eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();

        final ToolRegistry toolRegistry = new ToolRegistry();
        final AtomicBoolean toolExecuted = new AtomicBoolean(false);
        toolRegistry.register(new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return new ToolDefinition("scroll", "scrolls page", JsonNodeFactory.instance.objectNode());
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                toolExecuted.set(true);
                return ToolResult.success(call.callId(), "scrolled");
            }
        });

        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);
        context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.REPLAY_STRICT);
        context.getTransientData().put("KEY_TOOL_REGISTRY", toolRegistry);
        context.getTransientData().put("semanticVerification.enabled", false);

        final PlaybookStep visualStep = new PlaybookStep("Scroll down to view filter box (visual)");
        visualStep.setScreenshotHash(baselineHash);
        final ObjectNode scrollArgs = JsonNodeFactory.instance.objectNode();
        scrollArgs.put("direction", "down");
        visualStep.setToolCalls(List.of(new ToolCall("call-scroll-1", "scroll", scrollArgs)));

        final PipelineStep pipelineStep = ExecuteActionsStep.mapPlaybookStepToPipelineStep(visualStep, session, context);
        pipelineStep.execute(context);

        assertThrows(DivergenceException.class, () ->
        {
            while (context.hasSteps())
            {
                context.popStep().execute(context);
            }
        });

        assertTrue(toolExecuted.get(), "Recorded scroll action must have executed before post-action visual check");
        assertNotNull(visualStep.getSsimScore());
        assertTrue(visualStep.getSsimScore() < 0.99);
    }

    @Test
    public void testCompoundTurnGroupMapsToSingleStepWithMilestonesInLiveMode() throws Exception
    {
        final MockTargetExecutor executor = new MockTargetExecutor();
        final SessionData sessionData = new SessionData();
        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final AiSession session = AiSession.mock(sessionData, new LlmRegistry(), eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.LLM_RECORDING);

        final PlaybookStep parent = new PlaybookStep("Locate the promo code input field:");
        final PlaybookStep sub1 = new PlaybookStep("type '10p-off' into it");
        final PlaybookStep sub2 = new PlaybookStep("Submit the form");
        final PlaybookStep sub3 = new PlaybookStep("Verify that the order summary now shows a 'Discount' line item");
        parent.getSubSteps().addAll(List.of(sub1, sub2, sub3));

        final PipelineStep pipelineStep = ExecuteActionsStep.mapPlaybookStepToPipelineStep(parent, session, context);
        pipelineStep.execute(context);

        @SuppressWarnings("unchecked")
        final List<String> milestones = (List<String>) context.getTransientData().get(ExecutionContext.KEY_INTERNAL_MILESTONES);
        assertNotNull(milestones, "Compound turn group must populate internal milestones");
        assertEquals(3, milestones.size());
        assertEquals("type '10p-off' into it", milestones.get(0));
        assertEquals("Submit the form", milestones.get(1));
        assertEquals("Verify that the order summary now shows a 'Discount' line item", milestones.get(2));

        final String activeInstruction = (String) context.getTransientData().get(ExecutionContext.KEY_CURRENT_INSTRUCTION);
        assertNotNull(activeInstruction);
        assertTrue(activeInstruction.contains("Locate the promo code input field:"));
        assertTrue(activeInstruction.contains("type '10p-off' into it"));
        assertTrue(activeInstruction.contains("Submit the form"));
    }

    @Test
    public void testIncludeStepUnrollsSubSteps() throws Exception
    {
        final MockTargetExecutor executor = new MockTargetExecutor();
        final SessionData sessionData = new SessionData();
        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final AiSession session = AiSession.mock(sessionData, new LlmRegistry(), eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.LLM_RECORDING);

        final PlaybookStep includeStep = new PlaybookStep("_include: login.yaml");
        final PlaybookStep child1 = new PlaybookStep("Enter username");
        final PlaybookStep child2 = new PlaybookStep("Enter password");
        includeStep.getSubSteps().addAll(List.of(child1, child2));

        final PipelineStep pipelineStep = ExecuteActionsStep.mapPlaybookStepToPipelineStep(includeStep, session, context);
        pipelineStep.execute(context);

        // Include step schedules children + finishParent on the context stack (3 items)
        assertTrue(context.hasSteps());
        assertNull(context.getTransientData().get(ExecutionContext.KEY_INTERNAL_MILESTONES),
            "Include steps must unroll into sequential pipeline steps rather than compound milestones");
    }

    /**
     * Verifies that compound turn groups resolve currently available variables (e.g. testId from dataset)
     * while preserving uncaptured runtime variables (e.g. lineItemCount) without throwing UnresolvableVariableException.
     */
    @Test
    public void testCompoundTurnGroupWithRuntimeVariablesPreservesPlaceholdersWithoutFailing() throws Exception
    {
        final MockTargetExecutor executor = new MockTargetExecutor();
        final SessionData sessionData = new SessionData();
        sessionData.set("testId", "bad");
        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final AiSession session = AiSession.mock(sessionData, new LlmRegistry(), eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.LLM_RECORDING);

        final PlaybookStep parent = new PlaybookStep("Locate the first product card:");
        final PlaybookStep sub1 = new PlaybookStep("Capture the cart line item count in 'lineItemCount'");
        final PlaybookStep sub2 = new PlaybookStep("When this string '${testId}' is not equal 'bad', click the size 'S'");
        final PlaybookStep sub3 = new PlaybookStep("Verify that the cart item count is higher than ${lineItemCount}.");
        parent.getSubSteps().addAll(List.of(sub1, sub2, sub3));

        final PipelineStep pipelineStep = ExecuteActionsStep.mapPlaybookStepToPipelineStep(parent, session, context);
        pipelineStep.execute(context);

        @SuppressWarnings("unchecked")
        final List<String> milestones = (List<String>) context.getTransientData().get(ExecutionContext.KEY_INTERNAL_MILESTONES);
        assertNotNull(milestones, "Compound turn group must populate internal milestones");
        assertEquals(3, milestones.size());
        assertEquals("Capture the cart line item count in 'lineItemCount'", milestones.get(0));
        assertEquals("When this string 'bad' is not equal 'bad', click the size 'S'", milestones.get(1));
        assertEquals("Verify that the cart item count is higher than ${lineItemCount}.", milestones.get(2));

        final String activeInstruction = (String) context.getTransientData().get(ExecutionContext.KEY_CURRENT_INSTRUCTION);
        assertNotNull(activeInstruction);
        assertTrue(activeInstruction.contains("Locate the first product card:"));
        assertTrue(activeInstruction.contains("When this string 'bad' is not equal 'bad'"));
        assertTrue(activeInstruction.contains("${lineItemCount}"));
    }

    /**
     * Verifies that single leaf steps resolve available variables and preserve uncaptured runtime variables
     * without throwing UnresolvableVariableException.
     */
    @Test
    public void testLeafStepWithRuntimeVariablesPreservesPlaceholdersWithoutFailing() throws Exception
    {
        final MockTargetExecutor executor = new MockTargetExecutor();
        final SessionData sessionData = new SessionData();
        sessionData.set("scope", "cart");
        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final AiSession session = AiSession.mock(sessionData, new LlmRegistry(), eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.LLM_RECORDING);

        final PlaybookStep leaf = new PlaybookStep("Verify that the ${scope} item count is higher than ${lineItemCount}.");

        final PipelineStep pipelineStep = ExecuteActionsStep.mapPlaybookStepToPipelineStep(leaf, session, context);
        pipelineStep.execute(context);

        final String activeInstruction = (String) context.getTransientData().get(ExecutionContext.KEY_CURRENT_INSTRUCTION);
        assertNotNull(activeInstruction);
        assertEquals("Verify that the cart item count is higher than ${lineItemCount}.", activeInstruction);
    }

    @Test
    public void testCompoundTurnGroupUnrollsSubStepsInReplayMode() throws Exception
    {
        final MockTargetExecutor executor = new MockTargetExecutor();
        final SessionData sessionData = new SessionData();
        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final AiSession session = AiSession.mock(sessionData, new LlmRegistry(), eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.REPLAY_STRICT);

        final PlaybookStep parent = new PlaybookStep("Locate the first product card:");
        parent.setToolCalls(List.of(
            new ToolCall("c1", "hover", JsonNodeFactory.instance.objectNode()),
            new ToolCall("c2", "click", JsonNodeFactory.instance.objectNode())
        ));
        final PlaybookStep sub1 = new PlaybookStep("Hover over it");
        sub1.setToolCalls(List.of(new ToolCall("c1", "hover", JsonNodeFactory.instance.objectNode())));
        final PlaybookStep sub2 = new PlaybookStep("Click it");
        sub2.setToolCalls(List.of(new ToolCall("c2", "click", JsonNodeFactory.instance.objectNode())));
        parent.getSubSteps().addAll(List.of(sub1, sub2));

        final PipelineStep pipelineStep = ExecuteActionsStep.mapPlaybookStepToPipelineStep(parent, session, context);
        pipelineStep.execute(context);

        assertTrue(context.hasSteps());
    }

    /**
     * Verifies that partitionToolCallsAndActions maps calls sequentially to matching sub-steps
     * without cloning the entire call list to each child when the tool count is asymmetric
     * (e.g. when conditional branches are skipped).
     */
    @Test
    public void testPartitionToolCallsAndActionsAsymmetricMatching()
    {
        final PlaybookStep sub1 = new PlaybookStep("Capture the cart line item count in 'lineItemCount'");
        final PlaybookStep sub2 = new PlaybookStep("Hover over the product card");
        final PlaybookStep sub3 = new PlaybookStep("Click the 'Add to Cart' button");
        final PlaybookStep sub4 = new PlaybookStep("When this string 'bad' is not equal 'bad', click the size 'S'");
        final PlaybookStep sub5 = new PlaybookStep("Verify that the cart item count is higher than ${lineItemCount}.");

        final List<PlaybookStep> subSteps = List.of(sub1, sub2, sub3, sub4, sub5);

        final ToolCall call1 = new ToolCall("1", "browser_store", JsonNodeFactory.instance.objectNode());
        final ToolCall call2 = new ToolCall("2", "browser_hover", JsonNodeFactory.instance.objectNode());
        final ToolCall call3 = new ToolCall("3", "browser_click", JsonNodeFactory.instance.objectNode());
        final ToolCall call4 = new ToolCall("4", "browser_assert_text", JsonNodeFactory.instance.objectNode());

        final Action act1 = new Action("STORE", "#badge", List.of("lineItemCount"), "Store count", "", false);
        final Action act2 = new Action("HOVER", "#card", List.of(), "Hover card", "", false);
        final Action act3 = new Action("CLICK", "#add-btn", List.of(), "Click Add", "", false);
        final Action act4 = new Action("ASSERT_TEXT", "#badge", List.of("1"), "Assert text", "", false);

        AgentToolLoopStep.partitionToolCallsAndActions(subSteps, List.of(call1, call2, call3, call4), List.of(act1, act2, act3, act4));

        assertEquals(1, sub1.getToolCalls().size());
        assertEquals("browser_store", sub1.getToolCalls().get(0).toolName());
        assertEquals(1, sub1.getActions().size());

        assertEquals(1, sub2.getToolCalls().size());
        assertEquals("browser_hover", sub2.getToolCalls().get(0).toolName());
        assertEquals(1, sub2.getActions().size());

        assertEquals(1, sub3.getToolCalls().size());
        assertEquals("browser_click", sub3.getToolCalls().get(0).toolName());
        assertEquals(1, sub3.getActions().size());

        // Substep 4 was skipped (conditional 'bad' != 'bad' is false)
        assertTrue(sub4.getToolCalls().isEmpty(), "Skipped conditional sub-step must not receive tool calls");
        assertTrue(sub4.getActions().isEmpty(), "Skipped conditional sub-step must not receive actions");

        assertEquals(1, sub5.getToolCalls().size());
        assertEquals("browser_assert_text", sub5.getToolCalls().get(0).toolName());
        assertEquals(1, sub5.getActions().size());
    }

    /**
     * Verifies that ExecuteActionsStep auto-heals corrupted legacy recordings during replay
     * where every sub-step received a cloned copy of all parent tool calls.
     */
    @Test
    public void testAutoHealsCorruptedClonedToolCallsInReplayMode() throws Exception
    {
        final MockTargetExecutor executor = new MockTargetExecutor();
        final SessionData sessionData = new SessionData();
        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final AiSession session = AiSession.mock(sessionData, new LlmRegistry(), eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.REPLAY_STRICT);

        final PlaybookStep parent = new PlaybookStep("Locate the first product card:");
        final ToolCall callHover = new ToolCall("c1", "hover", JsonNodeFactory.instance.objectNode());
        final ToolCall callClick = new ToolCall("c2", "click", JsonNodeFactory.instance.objectNode());
        parent.setToolCalls(List.of(callHover, callClick));

        final PlaybookStep sub1 = new PlaybookStep("Hover over it");
        // Corrupted recording: sub1 has both parent tool calls cloned
        sub1.setToolCalls(new ArrayList<>(List.of(callHover, callClick)));

        final PlaybookStep sub2 = new PlaybookStep("Click it");
        // Corrupted recording: sub2 also has both parent tool calls cloned
        sub2.setToolCalls(new ArrayList<>(List.of(callHover, callClick)));

        parent.getSubSteps().addAll(List.of(sub1, sub2));

        final PipelineStep pipelineStep = ExecuteActionsStep.mapPlaybookStepToPipelineStep(parent, session, context);
        pipelineStep.execute(context);

        // Auto-healing should have partitioned tool calls across sub-steps so each has only 1 call
        assertEquals(1, sub1.getToolCalls().size());
        assertEquals("hover", sub1.getToolCalls().get(0).toolName());
        assertEquals(1, sub2.getToolCalls().size());
        assertEquals("click", sub2.getToolCalls().get(0).toolName());
    }

    /**
     * Verifies that in REPLAY_STRICT mode, unrolled child sub-steps of a compound parent
     * that have 0 tool calls (due to action coalescing into sibling sub-steps) complete
     * cleanly as coalesced steps without throwing ConclusiveFailureException.
     */
    @Test
    public void testReplayStrictAllowsCoalescedSubStepsWithZeroToolCalls() throws Exception
    {
        final MockTargetExecutor executor = new MockTargetExecutor();
        final SessionData sessionData = new SessionData();
        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final AiSession session = AiSession.mock(sessionData, new LlmRegistry(), eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.REPLAY_STRICT);

        final ToolRegistry registry = new ToolRegistry();
        registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("mock_action", "Mock action", JsonNodeFactory.instance.objectNode());

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "OK");
            }
        });
        context.getTransientData().put("KEY_TOOL_REGISTRY", registry);

        final PlaybookStep parent = new PlaybookStep("Locate promo input:");
        final ToolCall mockCall = new ToolCall("c1", "mock_action", JsonNodeFactory.instance.objectNode().put("param", "val"));
        parent.setToolCalls(List.of(mockCall));

        final PlaybookStep sub1 = new PlaybookStep("Clear and type code");
        sub1.setParent(parent);
        sub1.setToolCalls(List.of(mockCall));
        sub1.setStatus(PlaybookStepStatus.SUCCESS);

        final PlaybookStep sub2 = new PlaybookStep("Verify input filled");
        sub2.setParent(parent);
        // Coalesced sub-step: 0 tool calls recorded
        sub2.setToolCalls(Collections.emptyList());

        parent.getSubSteps().addAll(List.of(sub1, sub2));

        final PipelineStep pipelineStep = ExecuteActionsStep.mapPlaybookStepToPipelineStep(parent, session, context);
        pipelineStep.execute(context);

        // Drain pushed child pipeline steps and finishParent
        while (context.hasSteps())
        {
            context.popStep().execute(context);
        }

        // Sub-step 2 had 0 tool calls, but since it is a child of a compound parent, it should not fail in REPLAY_STRICT!
        assertEquals(PlaybookStepStatus.SUCCESS, sub1.getStatus());
        assertEquals(PlaybookStepStatus.SUCCESS, sub2.getStatus());
        assertEquals(PlaybookStepStatus.SUCCESS, parent.getStatus());
    }
}

