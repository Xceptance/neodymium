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
package org.neodymium.ai.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xceptance.neodymium.ai.console.InteractiveStateBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.executor.ActionDefinition;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.executor.TargetExecutor;
import org.neodymium.ai.executor.probe.LocatorProbeResult;
import org.neodymium.ai.model.ContextLevel;
import org.neodymium.ai.model.Playbook;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.PlaybookStepStatus;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.report.HtmlReportGenerator;
import org.neodymium.ai.report.MarkdownReportGenerator;
import org.neodymium.ai.report.PreliminaryReportListener;
import org.neodymium.ai.report.TestExecutionReport;
import org.neodymium.ai.session.AiSession;

/**
 * Unit tests verifying lifecycle separation and failure semantics for before, core, and after blocks.
 * Validates that before failure aborts core execution, after steps always execute during test failures,
 * exceptions are properly suppressed, and report/interactive states preserve block separation.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class PlaybookLifecycleExecutionTest
{
    private ExecutionContext outerContext;

    @BeforeEach
    public void setUp()
    {
        this.outerContext = new ExecutionContext(new SessionData());
        ExecutionContext.setActiveContext(this.outerContext);
    }

    @AfterEach
    public void tearDown()
    {
        ExecutionContext.setActiveContext(null);
    }

    @Test
    public void testBeforeStepFailureAbortsCoreStepsAndExecutesAfterSteps()
    {
        final MockLlmProvider mockProvider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockProvider);
        final ExecutionEventBus eventBus = new ExecutionEventBus();

        final List<String> executedInstructions = new ArrayList<>();
        final ActionTrackingTargetExecutor targetExecutor = new ActionTrackingTargetExecutor(action -> {
            if ("FAIL_ACTION".equals(action.getType()))
            {
                throw new RuntimeException("Simulated setup failure in before block");
            }
            executedInstructions.add(action.getTarget());
        });

        final AiSession session = AiSession.mock(ExecutionMode.REPLAY_STRICT, new SessionData(), registry, eventBus, targetExecutor);

        final PlaybookStep beforeStep = new PlaybookStep("Setup environment");
        beforeStep.setBlock("before");
        beforeStep.setActions(List.of(new Action("FAIL_ACTION", "env_setup", null)));

        final PlaybookStep coreStep = new PlaybookStep("Perform core user task");
        coreStep.setBlock("steps");
        coreStep.setActions(List.of(new Action("SUCCESS_ACTION", "core_task", null)));

        final PlaybookStep afterStep = new PlaybookStep("Teardown environment");
        afterStep.setBlock("after");
        afterStep.setActions(List.of(new Action("SUCCESS_ACTION", "teardown_task", null)));

        final Playbook playbook = Playbook.builder()
            .beforeStep(beforeStep)
            .step(coreStep)
            .afterStep(afterStep)
            .build();

        final Throwable thrown = assertThrows(Throwable.class, () -> session.execute(playbook));
        assertTrue(thrown.getMessage().contains("Simulated setup failure") || (thrown.getCause() != null && thrown.getCause().getMessage().contains("Simulated setup failure")));

        assertEquals(PlaybookStepStatus.FAILED, beforeStep.getStatus(), "Before step should be marked FAILED");
        assertEquals(PlaybookStepStatus.NOT_RUN, coreStep.getStatus(), "Core step should be marked NOT_RUN and not executed");
        assertEquals(PlaybookStepStatus.SUCCESS, afterStep.getStatus(), "After step should be executed and marked SUCCESS");

        assertFalse(executedInstructions.contains("core_task"), "Core step action must not have executed");
        assertTrue(executedInstructions.contains("teardown_task"), "After step action must have executed");
    }

    @Test
    public void testCoreStepFailureExecutesAfterSteps()
    {
        final MockLlmProvider mockProvider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockProvider);
        final ExecutionEventBus eventBus = new ExecutionEventBus();

        final List<String> executedInstructions = new ArrayList<>();
        final ActionTrackingTargetExecutor targetExecutor = new ActionTrackingTargetExecutor(action -> {
            if ("FAIL_CORE".equals(action.getType()))
            {
                throw new RuntimeException("Simulated core step failure");
            }
            executedInstructions.add(action.getTarget());
        });

        final AiSession session = AiSession.mock(ExecutionMode.REPLAY_STRICT, new SessionData(), registry, eventBus, targetExecutor);

        final PlaybookStep beforeStep = new PlaybookStep("Prepare test state");
        beforeStep.setBlock("before");
        beforeStep.setActions(List.of(new Action("SUCCESS_ACTION", "prepare_state", null)));

        final PlaybookStep coreStep = new PlaybookStep("Submit checkout order");
        coreStep.setBlock("steps");
        coreStep.setActions(List.of(new Action("FAIL_CORE", "checkout_order", null)));

        final PlaybookStep afterStep = new PlaybookStep("Clean up cart cookies");
        afterStep.setBlock("after");
        afterStep.setActions(List.of(new Action("SUCCESS_ACTION", "clean_cookies", null)));

        final Playbook playbook = Playbook.builder()
            .beforeStep(beforeStep)
            .step(coreStep)
            .afterStep(afterStep)
            .build();

        final Throwable thrown = assertThrows(Throwable.class, () -> session.execute(playbook));
        assertTrue(thrown.getMessage().contains("Simulated core step failure") || (thrown.getCause() != null && thrown.getCause().getMessage().contains("Simulated core step failure")));

        assertEquals(PlaybookStepStatus.SUCCESS, beforeStep.getStatus(), "Before step should be marked SUCCESS");
        assertEquals(PlaybookStepStatus.FAILED, coreStep.getStatus(), "Core step should be marked FAILED");
        assertEquals(PlaybookStepStatus.SUCCESS, afterStep.getStatus(), "After step should be executed and marked SUCCESS");

        assertTrue(executedInstructions.contains("prepare_state"), "Before step action should execute");
        assertTrue(executedInstructions.contains("clean_cookies"), "After step action should execute even though core failed");
    }

    @Test
    public void testSuppressedExceptionHandlingWhenBothCoreAndAfterFail()
    {
        final MockLlmProvider mockProvider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockProvider);
        final ExecutionEventBus eventBus = new ExecutionEventBus();

        final ActionTrackingTargetExecutor targetExecutor = new ActionTrackingTargetExecutor(action -> {
            if ("FAIL_CORE".equals(action.getType()))
            {
                throw new RuntimeException("Primary failure in core execution");
            }
            if ("FAIL_AFTER".equals(action.getType()))
            {
                throw new RuntimeException("Secondary failure in teardown after execution");
            }
        });

        final AiSession session = AiSession.mock(ExecutionMode.REPLAY_STRICT, new SessionData(), registry, eventBus, targetExecutor);

        final PlaybookStep coreStep = new PlaybookStep("Execute core logic");
        coreStep.setBlock("steps");
        coreStep.setActions(List.of(new Action("FAIL_CORE", "core_logic", null)));

        final PlaybookStep afterStep = new PlaybookStep("Execute teardown logic");
        afterStep.setBlock("after");
        afterStep.setActions(List.of(new Action("FAIL_AFTER", "teardown_logic", null)));

        final Playbook playbook = Playbook.builder()
            .step(coreStep)
            .afterStep(afterStep)
            .build();

        final Throwable thrown = assertThrows(Throwable.class, () -> session.execute(playbook));
        assertTrue(thrown.getMessage().contains("Primary failure in core execution")
            || (thrown.getCause() != null && thrown.getCause().getMessage().contains("Primary failure in core execution")));

        assertEquals(1, thrown.getSuppressed().length, "After block failure should be added as suppressed exception to core failure");
        final Throwable suppressed = thrown.getSuppressed()[0];
        assertTrue(suppressed.getMessage().contains("Secondary failure in teardown after execution")
            || (suppressed.getCause() != null && suppressed.getCause().getMessage().contains("Secondary failure in teardown after execution")));
    }

    @Test
    public void testReportAndInteractiveStatePreserveBlockSeparation() throws PipelineException, IOException
    {
        final MockLlmProvider mockProvider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockProvider);
        final ExecutionEventBus eventBus = new ExecutionEventBus();

        final PreliminaryReportListener reportListener = new PreliminaryReportListener();
        eventBus.registerListener(reportListener);

        final MockTargetExecutor targetExecutor = new MockTargetExecutor();
        final AiSession session = AiSession.mock(ExecutionMode.REPLAY_STRICT, new SessionData(), registry, eventBus, targetExecutor);

        final PlaybookStep beforeStep = new PlaybookStep("Init test session");
        beforeStep.setBlock("before");
        beforeStep.setActions(List.of(new Action("MOCK_NAVIGATE", "https://example.com/init", null)));

        final PlaybookStep coreStep = new PlaybookStep("Perform user search");
        coreStep.setBlock("steps");
        coreStep.setActions(List.of(new Action("MOCK_TYPE", "#search-input", "shoes")));

        final PlaybookStep afterStep = new PlaybookStep("Purge user session");
        afterStep.setBlock("after");
        afterStep.setActions(List.of(new Action("MOCK_CLICK", "#logout-button", null)));

        final Playbook playbook = Playbook.builder()
            .beforeStep(beforeStep)
            .step(coreStep)
            .afterStep(afterStep)
            .build();

        session.execute(playbook);

        final TestExecutionReport report = reportListener.getReport();
        assertNotNull(report, "Execution report should be populated");
        final List<TestExecutionReport.ReportStepEntry> reportSteps = report.getSteps();
        assertEquals(3, reportSteps.size(), "Report should contain all 3 steps");
        assertEquals("before", reportSteps.get(0).getBlock(), "First report step should have 'before' block");
        assertEquals("steps", reportSteps.get(1).getBlock(), "Second report step should have 'steps' block");
        assertEquals("after", reportSteps.get(2).getBlock(), "Third report step should have 'after' block");

        final String stateJson = InteractiveStateBuilder.buildStateJson(
            session,
            session.getExecutionContext(),
            "run_test_123",
            0,
            "running",
            null,
            report
        );

        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode interactiveState = mapper.readTree(stateJson);

        assertNotNull(interactiveState, "Interactive state JSON should not be null");
        final JsonNode blocksNode = interactiveState.get("blocks");
        assertNotNull(blocksNode, "Interactive state must have 'blocks' structure");
        assertTrue(blocksNode.has("before"), "Interactive state must contain 'before' block");
        assertTrue(blocksNode.has("steps"), "Interactive state must contain 'steps' block");
        assertTrue(blocksNode.has("after"), "Interactive state must contain 'after' block");

        assertEquals(1, blocksNode.get("before").size(), "Interactive state before block should contain 1 step");
        assertEquals("Init test session", blocksNode.get("before").get(0).get("instruction").asText());

        assertEquals(1, blocksNode.get("steps").size(), "Interactive state steps block should contain 1 step");
        assertEquals("Perform user search", blocksNode.get("steps").get(0).get("instruction").asText());

        assertEquals(1, blocksNode.get("after").size(), "Interactive state after block should contain 1 step");
        assertEquals("Purge user session", blocksNode.get("after").get(0).get("instruction").asText());
    }

    @Test
    public void testCoreStepFailureWithSuccessfulAfterBlockMarksReportAndConsoleAsFailed() throws IOException
    {
        final MockLlmProvider mockProvider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockProvider);
        final ExecutionEventBus eventBus = new ExecutionEventBus();

        final PreliminaryReportListener reportListener = new PreliminaryReportListener();
        eventBus.registerListener(reportListener);

        final ActionTrackingTargetExecutor targetExecutor = new ActionTrackingTargetExecutor(action -> {
            if ("FAIL_CORE".equals(action.getType()))
            {
                throw new RuntimeException("Simulated core step failure");
            }
        });

        final AiSession session = AiSession.mock(ExecutionMode.REPLAY_STRICT, new SessionData(), registry, eventBus, targetExecutor);

        final PlaybookStep beforeStep = new PlaybookStep("Prepare test state");
        beforeStep.setBlock("before");
        beforeStep.setActions(List.of(new Action("SUCCESS_ACTION", "prepare_state", null)));

        final PlaybookStep coreStep = new PlaybookStep("Submit checkout order");
        coreStep.setBlock("steps");
        coreStep.setActions(List.of(new Action("FAIL_CORE", "checkout_order", null)));

        final PlaybookStep afterStep = new PlaybookStep("Clean up cart cookies");
        afterStep.setBlock("after");
        afterStep.setActions(List.of(new Action("SUCCESS_ACTION", "clean_cookies", null)));

        final Playbook playbook = Playbook.builder()
            .beforeStep(beforeStep)
            .step(coreStep)
            .afterStep(afterStep)
            .build();

        final Throwable thrown = assertThrows(Throwable.class, () -> session.execute(playbook));
        assertNotNull(thrown);

        // Assert step execution statuses
        assertEquals(PlaybookStepStatus.SUCCESS, beforeStep.getStatus(), "Before step should be marked SUCCESS");
        assertEquals(PlaybookStepStatus.FAILED, coreStep.getStatus(), "Core step should be marked FAILED");
        assertEquals(PlaybookStepStatus.SUCCESS, afterStep.getStatus(), "After step should be executed and marked SUCCESS");

        // Assert report status and metrics
        final TestExecutionReport report = reportListener.getReport();
        assertNotNull(report, "Execution report must exist");
        assertFalse(report.isSuccess(), "Report must be marked failed when core step fails, even if after succeeds");
        assertEquals("FAILED", report.getStatus(), "Report status must be FAILED");
        assertNotNull(report.getFailureReason(), "Report must retain the failure reason");
        assertTrue(report.getFailureReason().contains("Simulated core step failure"), "Failure reason should match core failure");

        // Assert HTML and Markdown reports reflect failure
        final String html = new HtmlReportGenerator().generate(report);
        assertTrue(html.contains("status-fail"), "HTML report must include failed status class");
        assertTrue(html.contains("FAILED"), "HTML report must display FAILED");

        final String markdown = new MarkdownReportGenerator().generate(report);
        assertTrue(markdown.contains("**FAILED**"), "Markdown report must display **FAILED**");
        assertTrue(markdown.contains("❌"), "Markdown report must include failure emoji");

        // Assert interactive console state is failed
        final String stateJson = InteractiveStateBuilder.buildStateJson(
            session,
            session.getExecutionContext(),
            "run_failure_test",
            1,
            "running",
            null,
            report
        );
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode interactiveState = mapper.readTree(stateJson);
        assertEquals("failed", interactiveState.get("status").asText(), "Interactive state status must be failed");
    }

    @Test
    public void testBeforeStepFailureWithSuccessfulAfterBlockMarksReportAndConsoleAsFailed() throws IOException
    {
        final MockLlmProvider mockProvider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockProvider);
        final ExecutionEventBus eventBus = new ExecutionEventBus();

        final PreliminaryReportListener reportListener = new PreliminaryReportListener();
        eventBus.registerListener(reportListener);

        final ActionTrackingTargetExecutor targetExecutor = new ActionTrackingTargetExecutor(action -> {
            if ("FAIL_BEFORE".equals(action.getType()))
            {
                throw new RuntimeException("Simulated before step setup failure");
            }
        });

        final AiSession session = AiSession.mock(ExecutionMode.REPLAY_STRICT, new SessionData(), registry, eventBus, targetExecutor);

        final PlaybookStep beforeStep = new PlaybookStep("Setup test dependencies");
        beforeStep.setBlock("before");
        beforeStep.setActions(List.of(new Action("FAIL_BEFORE", "setup_dependencies", null)));

        final PlaybookStep coreStep = new PlaybookStep("Process transaction");
        coreStep.setBlock("steps");
        coreStep.setActions(List.of(new Action("SUCCESS_ACTION", "process_transaction", null)));

        final PlaybookStep afterStep = new PlaybookStep("Release resources");
        afterStep.setBlock("after");
        afterStep.setActions(List.of(new Action("SUCCESS_ACTION", "release_resources", null)));

        final Playbook playbook = Playbook.builder()
            .beforeStep(beforeStep)
            .step(coreStep)
            .afterStep(afterStep)
            .build();

        final Throwable thrown = assertThrows(Throwable.class, () -> session.execute(playbook));
        assertNotNull(thrown);

        assertEquals(PlaybookStepStatus.FAILED, beforeStep.getStatus(), "Before step should be marked FAILED");
        assertEquals(PlaybookStepStatus.NOT_RUN, coreStep.getStatus(), "Core step should be marked NOT_RUN");
        assertEquals(PlaybookStepStatus.SUCCESS, afterStep.getStatus(), "After step should execute and be marked SUCCESS");

        final TestExecutionReport report = reportListener.getReport();
        assertNotNull(report, "Execution report must exist");
        assertFalse(report.isSuccess(), "Report must be marked failed when before step fails");
        assertEquals("FAILED", report.getStatus(), "Report status must be FAILED");
        assertNotNull(report.getFailureReason(), "Report must retain before failure reason");
        assertTrue(report.getFailureReason().contains("Simulated before step setup failure"));
    }

    @Test
    public void testBothCoreAndAfterFailureMarksReportAsFailedWithSuppressedFailure() throws IOException
    {
        final MockLlmProvider mockProvider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockProvider);
        final ExecutionEventBus eventBus = new ExecutionEventBus();

        final PreliminaryReportListener reportListener = new PreliminaryReportListener();
        eventBus.registerListener(reportListener);

        final ActionTrackingTargetExecutor targetExecutor = new ActionTrackingTargetExecutor(action -> {
            if ("FAIL_CORE".equals(action.getType()))
            {
                throw new RuntimeException("Primary failure in core step");
            }
            if ("FAIL_AFTER".equals(action.getType()))
            {
                throw new RuntimeException("Secondary failure in teardown step");
            }
        });

        final AiSession session = AiSession.mock(ExecutionMode.REPLAY_STRICT, new SessionData(), registry, eventBus, targetExecutor);

        final PlaybookStep coreStep = new PlaybookStep("Execute core operation");
        coreStep.setBlock("steps");
        coreStep.setActions(List.of(new Action("FAIL_CORE", "core_operation", null)));

        final PlaybookStep afterStep = new PlaybookStep("Cleanup environment");
        afterStep.setBlock("after");
        afterStep.setActions(List.of(new Action("FAIL_AFTER", "cleanup_env", null)));

        final Playbook playbook = Playbook.builder()
            .step(coreStep)
            .afterStep(afterStep)
            .build();

        final Throwable thrown = assertThrows(Throwable.class, () -> session.execute(playbook));
        assertNotNull(thrown);
        assertTrue(thrown.getMessage().contains("Primary failure in core step")
            || (thrown.getCause() != null && thrown.getCause().getMessage().contains("Primary failure in core step")));
        assertEquals(1, thrown.getSuppressed().length, "After block failure must be chained as suppressed exception");

        final TestExecutionReport report = reportListener.getReport();
        assertNotNull(report, "Execution report must exist");
        assertFalse(report.isSuccess(), "Report must be marked failed when both core and after fail");
        assertEquals("FAILED", report.getStatus(), "Report status must be FAILED");
        assertEquals(PlaybookStepStatus.FAILED, coreStep.getStatus());
        assertEquals(PlaybookStepStatus.FAILED, afterStep.getStatus());
    }

    @Test
    public void testInteractiveStateBuilderGlobalContinuousStepIndexingAcrossBlocks() throws IOException
    {
        final ExecutionContext context = new ExecutionContext(new SessionData());
        final PlaybookStep before1 = new PlaybookStep("Navigate to homepage");
        before1.setBlock("before");
        before1.setStatus(PlaybookStepStatus.SUCCESS);

        final PlaybookStep before2 = new PlaybookStep("Accept cookie banner");
        before2.setBlock("before");
        before2.setStatus(PlaybookStepStatus.SUCCESS);

        final PlaybookStep core1 = new PlaybookStep("Click search button");
        core1.setBlock("steps");
        core1.setStatus(PlaybookStepStatus.RUNNING);

        final PlaybookStep core2 = new PlaybookStep("Enter search query");
        core2.setBlock("steps");
        core2.setStatus(PlaybookStepStatus.PENDING);

        final PlaybookStep after1 = new PlaybookStep("Clean up session");
        after1.setBlock("after");
        after1.setStatus(PlaybookStepStatus.PENDING);

        final List<PlaybookStep> beforeSteps = List.of(before1, before2);
        final List<PlaybookStep> coreSteps = List.of(core1, core2);
        final List<PlaybookStep> afterSteps = List.of(after1);

        context.getTransientData().put("playbook.beforeSteps", beforeSteps);
        context.getTransientData().put("playbook.steps", coreSteps);
        context.getTransientData().put("playbook.afterSteps", afterSteps);
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, core1);

        final String stateJson = InteractiveStateBuilder.buildStateJson(
            null,
            context,
            "multi-block-indexing-run",
            0,
            "paused",
            "pause-token-xyz"
        );

        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode state = mapper.readTree(stateJson);

        assertEquals("paused", state.get("status").asText());
        assertEquals("pause-token-xyz", state.get("pauseId").asText());

        final JsonNode blocks = state.get("blocks");
        assertNotNull(blocks);

        // Before block steps: indices 1, 2
        final JsonNode beforeArr = blocks.get("before");
        assertEquals(2, beforeArr.size());
        assertEquals(1, beforeArr.get(0).get("index").asInt());
        assertEquals(0, beforeArr.get(0).get("blockIndex").asInt());
        assertEquals("before", beforeArr.get(0).get("block").asText());
        assertEquals("passed", beforeArr.get(0).get("status").asText());

        assertEquals(2, beforeArr.get(1).get("index").asInt());
        assertEquals(1, beforeArr.get(1).get("blockIndex").asInt());
        assertEquals("before", beforeArr.get(1).get("block").asText());
        assertEquals("passed", beforeArr.get(1).get("status").asText());

        // Core steps: indices 3, 4 (continuous sequential numbering, NOT restarting at 1)
        final JsonNode stepsArr = blocks.get("steps");
        assertEquals(2, stepsArr.size());
        assertEquals(3, stepsArr.get(0).get("index").asInt(), "First core step must have index 3 following before steps");
        assertEquals(0, stepsArr.get(0).get("blockIndex").asInt());
        assertEquals("steps", stepsArr.get(0).get("block").asText());
        assertEquals("running", stepsArr.get(0).get("status").asText());

        assertEquals(4, stepsArr.get(1).get("index").asInt(), "Second core step must have index 4");
        assertEquals(1, stepsArr.get(1).get("blockIndex").asInt());
        assertEquals("steps", stepsArr.get(1).get("block").asText());
        assertEquals("pending", stepsArr.get(1).get("status").asText());

        // After steps: index 5
        final JsonNode afterArr = blocks.get("after");
        assertEquals(1, afterArr.size());
        assertEquals(5, afterArr.get(0).get("index").asInt(), "After step must have index 5");
        assertEquals(0, afterArr.get(0).get("blockIndex").asInt());
        assertEquals("after", afterArr.get(0).get("block").asText());
        assertEquals("pending", afterArr.get(0).get("status").asText());
    }

    /**
     * Goal: Verifies that InteractiveStateBuilder resolves pending steps from playbook.mainSteps
     * when playbook.steps is initially empty, and ensures that before and after steps accumulated
     * into playbook.steps are not duplicated inside blocks.steps.
     */
    @Test
    public void testInteractiveStateBuilderResolvesMainStepsWhenStepsEmptyAndFiltersDuplicates() throws IOException
    {
        final ExecutionContext context = new ExecutionContext(new SessionData());
        final PlaybookStep before1 = new PlaybookStep("Navigate to homepage");
        before1.setBlock("before");
        before1.setStatus(PlaybookStepStatus.SUCCESS);

        final PlaybookStep core1 = new PlaybookStep("Click search button");
        core1.setBlock("steps");
        core1.setStatus(PlaybookStepStatus.PENDING);

        final PlaybookStep core2 = new PlaybookStep("Enter search query");
        core2.setBlock("steps");
        core2.setStatus(PlaybookStepStatus.PENDING);

        final PlaybookStep after1 = new PlaybookStep("Clean up session");
        after1.setBlock("after");
        after1.setStatus(PlaybookStepStatus.PENDING);

        final List<PlaybookStep> beforeList = List.of(before1);
        final List<PlaybookStep> coreList = List.of(core1, core2);
        final List<PlaybookStep> afterList = List.of(after1);

        // Case 1: playbook.steps is empty list (prior to test execution), but playbook.mainSteps is present
        context.getTransientData().put("playbook.beforeSteps", beforeList);
        context.getTransientData().put("playbook.steps", new ArrayList<PlaybookStep>());
        context.getTransientData().put("playbook.mainSteps", new ArrayList<>(coreList));
        context.getTransientData().put("playbook.afterSteps", afterList);

        final ObjectMapper mapper = new ObjectMapper();
        final String stateJsonInitial = InteractiveStateBuilder.buildStateJson(null, context, "run-mainsteps", 0, "running");
        final JsonNode stateInitial = mapper.readTree(stateJsonInitial);
        final JsonNode blocksInitial = stateInitial.get("blocks");
        assertNotNull(blocksInitial);

        final JsonNode beforeArrInitial = blocksInitial.get("before");
        assertEquals(1, beforeArrInitial.size());
        assertEquals("Navigate to homepage", beforeArrInitial.get(0).get("instruction").asText());

        final JsonNode stepsArrInitial = blocksInitial.get("steps");
        assertEquals(2, stepsArrInitial.size(), "blocks.steps must resolve mainSteps when playbook.steps is empty");
        assertEquals("Click search button", stepsArrInitial.get(0).get("instruction").asText());
        assertEquals("Enter search query", stepsArrInitial.get(1).get("instruction").asText());

        final JsonNode afterArrInitial = blocksInitial.get("after");
        assertEquals(1, afterArrInitial.size());
        assertEquals("Clean up session", afterArrInitial.get(0).get("instruction").asText());

        // Case 2: playbook.steps has accumulated all steps across lifecycle execution (before, core, after)
        final List<PlaybookStep> accumulatedSteps = new ArrayList<>();
        accumulatedSteps.addAll(beforeList);
        accumulatedSteps.addAll(coreList);
        accumulatedSteps.addAll(afterList);
        context.getTransientData().put("playbook.steps", accumulatedSteps);
        context.getTransientData().remove("playbook.mainSteps");

        final String stateJsonAccumulated = InteractiveStateBuilder.buildStateJson(null, context, "run-accumulated", 0, "running");
        final JsonNode stateAccumulated = mapper.readTree(stateJsonAccumulated);
        final JsonNode blocksAccumulated = stateAccumulated.get("blocks");
        assertNotNull(blocksAccumulated);

        final JsonNode stepsArrAccumulated = blocksAccumulated.get("steps");
        assertEquals(2, stepsArrAccumulated.size(), "blocks.steps must filter out before and after steps from accumulated playbook.steps");
        assertEquals("Click search button", stepsArrAccumulated.get(0).get("instruction").asText());
        assertEquals("Enter search query", stepsArrAccumulated.get(1).get("instruction").asText());

        // Case 3: Hybrid setup pause: programmatic setup step has executed into playbook.steps,
        // while playbook.mainSteps is still pending and awaiting test method execution.
        final ExecutionContext contextCase3 = new ExecutionContext(new SessionData());
        final PlaybookStep setupStep = new PlaybookStep("Programmatic setup step in BeforeEach");
        setupStep.setStatus(PlaybookStepStatus.SUCCESS);

        contextCase3.getTransientData().put("playbook.beforeSteps", beforeList);
        contextCase3.getTransientData().put("playbook.steps", new ArrayList<>(List.of(setupStep)));
        contextCase3.getTransientData().put("playbook.mainSteps", new ArrayList<>(coreList));
        contextCase3.getTransientData().put("playbook.afterSteps", afterList);

        final String stateJsonHybrid = InteractiveStateBuilder.buildStateJson(null, contextCase3, "run-hybrid", 0, "running");
        final JsonNode stateHybrid = mapper.readTree(stateJsonHybrid);
        final JsonNode blocksHybrid = stateHybrid.get("blocks");
        assertNotNull(blocksHybrid);

        final JsonNode stepsArrHybrid = blocksHybrid.get("steps");
        assertEquals(3, stepsArrHybrid.size(), "blocks.steps must include executed setup steps ahead of pending mainSteps");
        assertEquals("Programmatic setup step in BeforeEach", stepsArrHybrid.get(0).get("instruction").asText());
        assertEquals("passed", stepsArrHybrid.get(0).get("status").asText());
        assertEquals("Click search button", stepsArrHybrid.get(1).get("instruction").asText());
        assertEquals("Enter search query", stepsArrHybrid.get(2).get("instruction").asText());

        // Case 4: Null step safety: steps collection containing a null element is safely skipped
        final ExecutionContext contextCase4 = new ExecutionContext(new SessionData());
        final List<PlaybookStep> listWithNull = new ArrayList<>();
        listWithNull.add(null);
        listWithNull.add(core1);
        listWithNull.add(null);
        contextCase4.getTransientData().put("playbook.steps", listWithNull);

        final String stateJsonNullSafety = InteractiveStateBuilder.buildStateJson(null, contextCase4, "run-null-safety", 0, "running");
        final JsonNode stateNullSafety = mapper.readTree(stateJsonNullSafety);
        final JsonNode blocksNullSafety = stateNullSafety.get("blocks");
        assertNotNull(blocksNullSafety);
        final JsonNode stepsArrNullSafety = blocksNullSafety.get("steps");
        assertEquals(1, stepsArrNullSafety.size(), "blocks.steps must safely skip null step entries without NPE");
        assertEquals("Click search button", stepsArrNullSafety.get(0).get("instruction").asText());
    }

    /**
     * Goal: Verifies that in hybrid tests, programmatic inline steps executed after a YAML step
     * appear strictly in chronological sequence after that YAML step in the serialized state JSON.
     */
    @Test
    public void testHybridTestChronologicalOrderingInStateBuilder() throws IOException
    {
        final ExecutionContext context = new ExecutionContext(new SessionData());

        final PlaybookStep yamlStep1 = new PlaybookStep("YAML step 1");
        yamlStep1.setStatus(PlaybookStepStatus.SUCCESS);
        yamlStep1.setBlock("steps");

        final PlaybookStep inlineStep = new PlaybookStep("Inline programmatic step");
        inlineStep.setStatus(PlaybookStepStatus.SUCCESS);
        inlineStep.setBlock("steps");

        final PlaybookStep yamlStep2 = new PlaybookStep("YAML step 2");
        yamlStep2.setStatus(PlaybookStepStatus.PENDING);
        yamlStep2.setBlock("steps");

        // Executed so far: yamlStep1, followed by inlineStep
        final List<PlaybookStep> sessionSteps = new ArrayList<>(List.of(yamlStep1, inlineStep));
        // Remaining/configured main steps from YAML: yamlStep1, yamlStep2
        final List<PlaybookStep> mainSteps = new ArrayList<>(List.of(yamlStep1, yamlStep2));

        context.getTransientData().put("playbook.steps", sessionSteps);
        context.getTransientData().put("playbook.mainSteps", mainSteps);

        final ObjectMapper mapper = new ObjectMapper();
        final String stateJson = InteractiveStateBuilder.buildStateJson(null, context, "run-hybrid-order", 0, "running");
        final JsonNode root = mapper.readTree(stateJson);
        final JsonNode stepsArr = root.get("blocks").get("steps");
        assertNotNull(stepsArr);
        assertEquals(3, stepsArr.size(), "All 3 steps must be rendered in blocks.steps");
        assertEquals("YAML step 1", stepsArr.get(0).get("instruction").asText());
        assertEquals("Inline programmatic step", stepsArr.get(1).get("instruction").asText());
        assertEquals("YAML step 2", stepsArr.get(2).get("instruction").asText());
    }

    private static class ActionTrackingTargetExecutor implements TargetExecutor
    {
        private final MockTargetExecutor delegate = new MockTargetExecutor();
        private final Consumer<Action> actionConsumer;

        ActionTrackingTargetExecutor(final Consumer<Action> actionConsumer)
        {
            this.actionConsumer = actionConsumer;
        }

        @Override
        public void execute(final Action action) throws IOException
        {
            if (this.actionConsumer != null)
            {
                this.actionConsumer.accept(action);
            }
            this.delegate.execute(action);
        }

        @Override
        public SutState captureState(final ContextLevel level) throws IOException
        {
            return this.delegate.captureState(level);
        }

        @Override
        public SutState captureState(final ContextLevel level, final boolean isFullPage) throws IOException
        {
            return this.delegate.captureState(level, isFullPage);
        }

        @Override
        public Set<ActionDefinition> getSupportedActions()
        {
            return this.delegate.getSupportedActions();
        }

        @Override
        public boolean supportsLocatorProbing()
        {
            return this.delegate.supportsLocatorProbing();
        }

        @Override
        public List<LocatorProbeResult> probeLocators(final List<String> candidateLocators, final int maxDepth)
        {
            return this.delegate.probeLocators(candidateLocators, maxDepth);
        }
    }
}
