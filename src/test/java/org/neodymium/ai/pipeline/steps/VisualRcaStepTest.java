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
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.executor.MockSutState;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.session.AiSession;

/**
 * Unit test suite for {@link VisualRcaStep}.
 * Verifies multimodal vision RCA execution with screenshots and fallback to text-only analysis when no images are present.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class VisualRcaStepTest
{
    private ExecutionContext context;
    private MockLlmProvider mockLlmProvider;
    private AiSession session;

    @BeforeEach
    public void setUp()
    {
        final SessionData sessionData = new SessionData(new HashMap<>());
        mockLlmProvider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockLlmProvider);
        registry.registerProvider(mockLlmProvider);
        final ExecutionEventBus eventBus = new ExecutionEventBus();

        session = AiSession.mock(sessionData, registry, eventBus, new MockTargetExecutor());
        context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);
    }

    /**
     * Goal: Verifies that when a screenshot attachment is present in SUT state,
     * {@link VisualRcaStep} dispatches a multimodal vision request and saves the returned RCA diagnosis.
     */
    @Test
    public void testVisualRcaGeneratesDiagnosisWithImageAttachment() throws Exception
    {
        final PlaybookStep step = new PlaybookStep("Click checkout button");
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, step);

        final SutAttachment screenshot = new SutAttachment("screenshot.png", "image/png", "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");
        final MockSutState lastState = new MockSutState("Checkout Page", List.of(screenshot), "hash-123");
        context.getTransientData().put(ExecutionContext.KEY_LAST_STATE, lastState);

        mockLlmProvider.addResponse(new LlmResponse("Checkout button was blocked by an unaccepted cookie consent overlay.", new TokenUsage(10, 10, 20), "mock-model"));

        final VisualRcaStep rcaStep = new VisualRcaStep("ElementNotInteractableException");
        rcaStep.execute(context);

        final String summary = (String) context.getTransientData().get(ExecutionContext.KEY_VISUAL_RCA_SUMMARY);
        // Assert expected multimodal RCA summary is retrieved and saved in transient context
        Assertions.assertNotNull(summary);
        Assertions.assertEquals("Checkout button was blocked by an unaccepted cookie consent overlay.", summary);
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
}
