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
        final ObjectNode pesapArgs = JsonNodeFactory.instance.objectNode();
        pesapArgs.put("intent", "ASSERT");
        pesapArgs.put("contextLevel", "VISUAL");
        final ToolCall pesapCall = new ToolCall("pesap-1", "classify_step", pesapArgs);
        mockProvider.addResponse(new LlmResponse("", new TokenUsage(50, 10, 60, 0), "mock-model", List.of(pesapCall)));

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
}
