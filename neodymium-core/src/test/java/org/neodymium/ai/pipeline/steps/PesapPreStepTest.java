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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.model.ContextLevel;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.SemanticIntent;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PesapClassificationException;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.session.AiSession;

/**
 * Unit tests for {@link PesapPreStep} verifying intent classification, context level cleaning,
 * 1-retry handling, and {@link PesapClassificationException} throwing on unresolvable intents.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class PesapPreStepTest
{
    @Test
    public void testPesapPredictionUpdatesContextLevel() throws PipelineException
    {
        final MockTargetExecutor executor = new MockTargetExecutor();
        final MockLlmProvider provider = new MockLlmProvider();
        provider.addResponse(new LlmResponse("{\"c\": \"RICH\", \"i\": \"CLICK\"}", null, "mock-model"));

        final LlmRegistry registry = new LlmRegistry();
        registry.registerProvider(LlmCapability.PESAP, provider);
        registry.setDefaultProvider(provider);

        final SessionData sessionData = new SessionData();
        final AiSession session = AiSession.mock(sessionData, registry, new ExecutionEventBus(), executor);
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.LLM_ONLY);

        final PlaybookStep step = new PlaybookStep("Click the submit button");
        final PesapPreStep pesapStep = new PesapPreStep(step, session);
        final boolean isSplit = pesapStep.executePreStep(context);

        assertFalse(isSplit);
        assertEquals(ContextLevel.RICH, context.getTransientData().get(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL));
        assertEquals("RICH", step.getContextLevel());
        assertEquals(SemanticIntent.CLICK, step.getSemanticIntent());
    }

    @Test
    public void testPesapStepSplitting() throws PipelineException
    {
        final MockTargetExecutor executor = new MockTargetExecutor();
        final MockLlmProvider provider = new MockLlmProvider();
        provider.addResponse(new LlmResponse(
            "{\"c\": \"MINIMAL\", \"i\": \"TYPE\", \"sp\": [\"Click username\", \"Type admin\"]}",
            null,
            "mock-model"
        ));

        final LlmRegistry registry = new LlmRegistry();
        registry.registerProvider(LlmCapability.PESAP, provider);
        registry.setDefaultProvider(provider);

        final SessionData sessionData = new SessionData();
        final AiSession session = AiSession.mock(sessionData, registry, new ExecutionEventBus(), executor);
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.LLM_ONLY);

        final PlaybookStep parentStep = new PlaybookStep("Fill username with admin");
        final PesapPreStep pesapStep = new PesapPreStep(parentStep, session);
        final boolean isSplit = pesapStep.executePreStep(context);

        assertTrue(isSplit);
        assertEquals(2, parentStep.getSubSteps().size());
        assertEquals("Click username", parentStep.getSubSteps().get(0).getInstruction());
        assertEquals("Type admin", parentStep.getSubSteps().get(1).getInstruction());
    }

    @Test
    public void testPesapStepSplittingSanitizesVariables() throws PipelineException
    {
        final MockTargetExecutor executor = new MockTargetExecutor();
        final MockLlmProvider provider = new MockLlmProvider();
        provider.addResponse(new LlmResponse(
            "{\"c\": \"MINIMAL\", \"i\": \"TYPE\", \"sp\": [\"Type \\\"john.doe.us.123456@example.com\\\" into email\", \"Type \\\"SecretPass1!\\\" into password\"]}",
            null,
            "mock-model"
        ));

        final LlmRegistry registry = new LlmRegistry();
        registry.registerProvider(LlmCapability.PESAP, provider);
        registry.setDefaultProvider(provider);

        final SessionData sessionData = new SessionData();
        sessionData.putDynamic("random", "123456", false);
        sessionData.putDynamic("email", "john.doe.us.${random}@example.com", false);
        sessionData.putDynamic("password", "SecretPass1!", true);

        final AiSession session = AiSession.mock(sessionData, registry, new ExecutionEventBus(), executor);
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.LLM_RECORDING);

        final PlaybookStep parentStep = new PlaybookStep("Type \"${email}\" into email and \"${password}\" into password");
        final PesapPreStep pesapStep = new PesapPreStep(parentStep, session);
        final boolean isSplit = pesapStep.executePreStep(context);

        assertTrue(isSplit);
        assertEquals(2, parentStep.getSubSteps().size());
        assertEquals("Type \"${email}\" into email", parentStep.getSubSteps().get(0).getInstruction());
        assertEquals("Type \"${password}\" into password", parentStep.getSubSteps().get(1).getInstruction());
    }

    @Test
    public void testPesapRetriesOnVoidIntentAndSucceeds() throws PipelineException
    {
        final MockTargetExecutor executor = new MockTargetExecutor();
        final MockLlmProvider provider = new MockLlmProvider();
        // Attempt 1: void/empty JSON
        provider.addResponse(new LlmResponse("{}", null, "mock-model"));
        // Attempt 2: valid JSON with CLICK intent
        provider.addResponse(new LlmResponse("{\"c\": \"LEAN\", \"i\": \"CLICK\"}", null, "mock-model"));

        final LlmRegistry registry = new LlmRegistry();
        registry.registerProvider(LlmCapability.PESAP, provider);
        registry.setDefaultProvider(provider);

        final SessionData sessionData = new SessionData();
        final AiSession session = AiSession.mock(sessionData, registry, new ExecutionEventBus(), executor);
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.LLM_ONLY);

        final PlaybookStep step = new PlaybookStep("Click Submit");
        final PesapPreStep pesapStep = new PesapPreStep(step, session);
        final boolean isSplit = pesapStep.executePreStep(context);

        assertFalse(isSplit);
        assertEquals(SemanticIntent.CLICK, step.getSemanticIntent());
        assertEquals(ContextLevel.LEAN, context.getTransientData().get(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL));
    }

    @Test
    public void testPesapThrowsPesapClassificationExceptionAfterExhaustingRetries()
    {
        final MockTargetExecutor executor = new MockTargetExecutor();
        final MockLlmProvider provider = new MockLlmProvider();
        // Attempt 1: invalid/unrecognized intent
        provider.addResponse(new LlmResponse("{\"c\": \"LEAN\", \"i\": \"UNKNOWN_INTENT\"}", null, "mock-model"));
        // Attempt 2: empty response
        provider.addResponse(new LlmResponse("{}", null, "mock-model"));

        final LlmRegistry registry = new LlmRegistry();
        registry.registerProvider(LlmCapability.PESAP, provider);
        registry.setDefaultProvider(provider);

        final SessionData sessionData = new SessionData();
        final AiSession session = AiSession.mock(sessionData, registry, new ExecutionEventBus(), executor);
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.LLM_ONLY);

        final PlaybookStep step = new PlaybookStep("Unclassifiable instruction");
        final PesapPreStep pesapStep = new PesapPreStep(step, session);

        assertThrows(PesapClassificationException.class, () -> pesapStep.executePreStep(context));
    }

    @Test
    public void testPesapAssertPromotesContextLevelToStandard() throws PipelineException
    {
        final MockTargetExecutor executor = new MockTargetExecutor();
        final MockLlmProvider provider = new MockLlmProvider();
        // LLM returns MINIMAL for ASSERT intent -> should be promoted to STANDARD
        provider.addResponse(new LlmResponse("{\"c\": \"MINIMAL\", \"i\": \"ASSERT\"}", null, "mock-model"));

        final LlmRegistry registry = new LlmRegistry();
        registry.registerProvider(LlmCapability.PESAP, provider);
        registry.setDefaultProvider(provider);

        final SessionData sessionData = new SessionData();
        final AiSession session = AiSession.mock(sessionData, registry, new ExecutionEventBus(), executor);
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.LLM_ONLY);

        final PlaybookStep step = new PlaybookStep("An order number matching regex 'V-[0-9]+-DE' is displayed.");
        final PesapPreStep pesapStep = new PesapPreStep(step, session);
        final boolean isSplit = pesapStep.executePreStep(context);

        assertFalse(isSplit);
        assertEquals(SemanticIntent.ASSERT, step.getSemanticIntent());
        assertEquals(ContextLevel.STANDARD, context.getTransientData().get(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL));
        assertEquals("STANDARD", step.getContextLevel());
    }
}
