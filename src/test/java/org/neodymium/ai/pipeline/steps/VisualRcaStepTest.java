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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.client.SutAttachment;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.event.ExecutionEvent;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.event.llm.LlmRequestSentEvent;
import org.neodymium.ai.event.llm.LlmResponseReceivedEvent;
import org.neodymium.ai.executor.MockSutState;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.StepStats;
import org.neodymium.ai.session.AiSession;

/**
 * Unit test suite for {@link VisualRcaStep}.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class VisualRcaStepTest
{
    private ExecutionContext context;
    private MockLlmProvider mockLlmProvider;
    private AiSession session;
    private ExecutionEventBus eventBus;

    @BeforeEach
    public void setUp()
    {
        final SessionData sessionData = new SessionData();
        final LlmRegistry registry = new LlmRegistry();
        mockLlmProvider = new MockLlmProvider();
        registry.registerProvider(mockLlmProvider);
        eventBus = new ExecutionEventBus();

        session = AiSession.mock(sessionData, registry, eventBus, new MockTargetExecutor());
        context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);
    }

    /**
     * Goal: Verifies that when a screenshot attachment is present in SUT state,
     * {@link VisualRcaStep} dispatches a multimodal vision request, tracks tokens,
     * records events, and saves the returned RCA diagnosis.
     */
    @Test
    public void testVisualRcaGeneratesDiagnosisWithImageAttachment() throws Exception
    {
        final List<ExecutionEvent> events = new ArrayList<>();
        eventBus.registerListener(events::add);

        final PlaybookStep step = new PlaybookStep("Click checkout button");
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, step);

        final StepStats stepStats = new StepStats("Click checkout button", System.currentTimeMillis());
        context.getTransientData().put("KEY_CURRENT_STEP_STATS", stepStats);

        final SutAttachment screenshot = new SutAttachment("screenshot.png", "image/png", "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");
        final MockSutState lastState = new MockSutState("Checkout Page", List.of(screenshot), "hash-123");
        context.getTransientData().put(ExecutionContext.KEY_LAST_STATE, lastState);

        mockLlmProvider.addResponse(new LlmResponse("Checkout button was blocked by an unaccepted cookie consent overlay.", new TokenUsage(100, 50, 150), "mock-model"));

        final VisualRcaStep rcaStep = new VisualRcaStep("ElementNotInteractableException");
        rcaStep.execute(context);

        final String summary = (String) context.getTransientData().get(ExecutionContext.KEY_VISUAL_RCA_SUMMARY);
        final String explanation = (String) context.getTransientData().get(ExecutionContext.KEY_VISUAL_RCA_EXPLANATION);
        // Assert expected multimodal RCA summary is retrieved and saved in transient context
        Assertions.assertNotNull(summary);
        Assertions.assertEquals("Checkout button was blocked by an unaccepted cookie consent overlay.", summary);
        Assertions.assertEquals(summary, explanation);

        // Assert events were dispatched
        Assertions.assertTrue(events.stream().anyMatch(e -> e instanceof LlmRequestSentEvent sent && "VISUAL_RCA".equals(sent.getCapability())));
        Assertions.assertTrue(events.stream().anyMatch(e -> e instanceof LlmResponseReceivedEvent recv && "VISUAL_RCA".equals(recv.getCapability())));

        // Assert token tracking and call counts
        Assertions.assertEquals(1, context.getTransientData().get(ExecutionContext.KEY_TOTAL_LLM_CALLS));
        Assertions.assertEquals(1, context.getTransientData().get(ExecutionContext.KEY_RCA_CALL_COUNT));
        final TokenUsage rcaUsage = (TokenUsage) context.getTransientData().get(ExecutionContext.KEY_RCA_TOKEN_USAGE);
        Assertions.assertNotNull(rcaUsage);
        Assertions.assertEquals(100, rcaUsage.inputTokenCount());
        Assertions.assertEquals(50, rcaUsage.outputTokenCount());

        // Assert StepStats
        Assertions.assertEquals(1, stepStats.getRcaCalls());
        Assertions.assertEquals(100, stepStats.getRcaInputTokens());
        Assertions.assertEquals(50, stepStats.getRcaOutputTokens());
    }

    /**
     * Goal: Verifies that when no screenshot attachment is present,
     * {@link VisualRcaStep} gracefully falls back to text-only analysis and records the diagnosis.
     */
    @Test
    public void testVisualRcaFallbackToTextOnlyWhenNoImage() throws Exception
    {
        final PlaybookStep step = new PlaybookStep("Type user email");
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, step);

        final MockSutState lastState = new MockSutState("Login Page DOM", "hash-456");
        context.getTransientData().put(ExecutionContext.KEY_LAST_STATE, lastState);

        mockLlmProvider.addResponse(new LlmResponse("Input field #email was disabled in DOM.", new TokenUsage(10, 10, 20), "mock-model"));

        final VisualRcaStep rcaStep = new VisualRcaStep("InvalidElementStateException");
        rcaStep.execute(context);

        final String summary = (String) context.getTransientData().get(ExecutionContext.KEY_VISUAL_RCA_SUMMARY);
        // Assert text-only fallback diagnosis summary is retrieved and stored
        Assertions.assertNotNull(summary);
        Assertions.assertEquals("Input field #email was disabled in DOM.", summary);
    }

    /**
     * Goal: Verifies that when {@link PlaybookStep} has no prior failure reason,
     * {@link VisualRcaStep} enriches the step with the RCA diagnosis string.
     */
    @Test
    public void testVisualRcaEnrichesPlaybookStepFailureReasonWhenNull() throws Exception
    {
        final PlaybookStep step = new PlaybookStep("Select shipping method");
        Assertions.assertNull(step.getFailureReason());
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, step);

        mockLlmProvider.addResponse(new LlmResponse("Radio button #express was obscured by sticky footer.", new TokenUsage(20, 10, 30), "mock-model"));

        final VisualRcaStep rcaStep = new VisualRcaStep("ElementClickInterceptedException");
        rcaStep.execute(context);

        Assertions.assertEquals("Radio button #express was obscured by sticky footer.", step.getFailureReason());
    }

    /**
     * Goal: Verifies that when KEY_LAST_STATE is omitted/null, {@link VisualRcaStep}
     * performs dynamic fallback state capture via TargetExecutor and extracts screenshots.
     */
    @Test
    public void testVisualRcaDynamicFallbackCaptureViaTargetExecutorWhenLastStateMissing() throws Exception
    {
        final PlaybookStep step = new PlaybookStep("Submit payment form");
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, step);

        // Do NOT put KEY_LAST_STATE in context; instead configure MockTargetExecutor with a captured state
        final MockTargetExecutor executor = new MockTargetExecutor();
        final SutAttachment screenshot = new SutAttachment("dynamic-screenshot.png", "image/png", "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");
        executor.enqueueState(new MockSutState("Payment Form", List.of(screenshot), "hash-dynamic"));
        context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);

        mockLlmProvider.addResponse(new LlmResponse("CVV input field was marked with validation error border.", new TokenUsage(50, 25, 75), "mock-model"));

        final VisualRcaStep rcaStep = new VisualRcaStep("PaymentValidationException");
        rcaStep.execute(context);

        final String summary = (String) context.getTransientData().get(ExecutionContext.KEY_VISUAL_RCA_SUMMARY);
        Assertions.assertNotNull(summary);
        Assertions.assertEquals("CVV input field was marked with validation error border.", summary);
        Assertions.assertEquals("CVV input field was marked with validation error border.", step.getFailureReason());
    }
}
