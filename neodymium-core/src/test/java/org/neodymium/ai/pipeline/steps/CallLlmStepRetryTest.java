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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.prompt.ActionExtractionPrompt;
import org.neodymium.ai.session.AiSession;

/**
 * Unit tests verifying single-shot retry behavior in {@link CallLlmStep} on parse failures.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class CallLlmStepRetryTest
{
    @Test
    public void testRetriesOnceOnMalformedJsonAndSucceeds() throws PipelineException
    {
        final MockLlmProvider provider = new MockLlmProvider();
        // Attempt 1: Malformed JSON
        provider.addResponse(new LlmResponse("This is not valid JSON { malformed ...", new TokenUsage(10, 5, 15, 0), "mock"));
        // Attempt 2: Valid JSON
        provider.addResponse(new LlmResponse(
            "{\"status\":\"SUCCESS\",\"actions\":[{\"action\":\"CLICK\",\"locator\":\"#checkout-btn\"}]}",
            new TokenUsage(10, 10, 20, 0),
            "mock"
        ));

        final LlmRegistry registry = new LlmRegistry();
        registry.registerProvider(LlmCapability.TEXT_ONLY, provider);
        registry.setDefaultProvider(provider);

        final SessionData sessionData = new SessionData();
        final AiSession session = AiSession.mock(sessionData, registry, new ExecutionEventBus(), new MockTargetExecutor());
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, new PlaybookStep("Click checkout button."));

        final CallLlmStep<List<Action>> step = new CallLlmStep<>(new ActionExtractionPrompt(), LlmCapability.TEXT_ONLY);
        step.execute(context);

        assertEquals(2, (Integer) context.getTransientData().get(ExecutionContext.KEY_TOTAL_LLM_CALLS));
        assertFalse(provider.hasQueuedResponses());
        @SuppressWarnings("unchecked")
        final List<Action> actions = (List<Action>) context.getTransientData().get(ExecutionContext.KEY_LAST_LLM_RESULT);
        assertNotNull(actions);
        assertEquals(1, actions.size());
        assertEquals("CLICK", actions.get(0).getType());
    }

    @Test
    public void testFailsConclusivelyAfterTwoFailedAttempts()
    {
        final MockLlmProvider provider = new MockLlmProvider();
        // Attempt 1 & 2: Malformed JSON
        provider.addResponse(new LlmResponse("Invalid JSON 1", new TokenUsage(10, 5, 15, 0), "mock"));
        provider.addResponse(new LlmResponse("Invalid JSON 2", new TokenUsage(10, 5, 15, 0), "mock"));

        final LlmRegistry registry = new LlmRegistry();
        registry.registerProvider(LlmCapability.TEXT_ONLY, provider);
        registry.setDefaultProvider(provider);

        final SessionData sessionData = new SessionData();
        final AiSession session = AiSession.mock(sessionData, registry, new ExecutionEventBus(), new MockTargetExecutor());
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, new PlaybookStep("Click checkout button."));

        final CallLlmStep<List<Action>> step = new CallLlmStep<>(new ActionExtractionPrompt(), LlmCapability.TEXT_ONLY);

        assertThrows(ConclusiveFailureException.class, () -> step.execute(context));
        assertEquals(2, (Integer) context.getTransientData().get(ExecutionContext.KEY_TOTAL_LLM_CALLS));
        assertFalse(provider.hasQueuedResponses());
    }
}
