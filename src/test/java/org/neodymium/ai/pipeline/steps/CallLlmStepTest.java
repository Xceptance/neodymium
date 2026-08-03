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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.client.ResponseSchema;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.prompt.AiPrompt;
import org.neodymium.ai.session.AiSession;

/**
 * Unit tests for {@link CallLlmStep}.
 * Validates prompt compilation and LLM response execution routing.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class CallLlmStepTest
{
    @Test
    public void testCallLlmExecution() throws PipelineException
    {
        final MockTargetExecutor executor = new MockTargetExecutor();
        final MockLlmProvider provider = new MockLlmProvider();
        provider.addResponse(new LlmResponse("Test Response", null, "mock-model"));

        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(provider);
        registry.registerProvider(provider);

        final SessionData sessionData = new SessionData();
        final AiSession session = AiSession.mock(sessionData, registry, new ExecutionEventBus(), executor);

        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);
        context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);

        final AiPrompt<String> dummyPrompt = new AiPrompt<>()
        {
            @Override
            public ResponseSchema getResponseSchema()
            {
                return ResponseSchema.TEXT;
            }

            @Override
            public String compileSystemMessage(final ExecutionContext ctx)
            {
                return "System";
            }

            @Override
            public String compileUserMessage(final ExecutionContext ctx)
            {
                return "User";
            }

            @Override
            public String parseResponse(final String rawContent, final ExecutionContext ctx)
            {
                return rawContent;
            }
        };

        final CallLlmStep<String> callLlmStep = new CallLlmStep<>(dummyPrompt, LlmCapability.TEXT_ONLY);
        callLlmStep.execute(context);

        assertNotNull(context.getTransientData().get(ExecutionContext.KEY_LAST_LLM_RESULT), "LLM response result should be placed in context transient storage.");
    }

    @Test
    public void testCallLlmMasksSensitiveDataAndUnmasksResponse() throws PipelineException
    {
        final MockTargetExecutor executor = new MockTargetExecutor();
        final MockLlmProvider provider = new MockLlmProvider();
        provider.addResponse(new LlmResponse("Selected element with value [MASKED_VAR_password]", null, "mock-model"));

        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(provider);
        registry.registerProvider(provider);

        final SessionData sessionData = new SessionData();
        sessionData.putDynamic("password", "SuperSecret123!", true);
        final AiSession session = AiSession.mock(sessionData, registry, new ExecutionEventBus(), executor);

        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);
        context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);

        final AiPrompt<String> prompt = new AiPrompt<>()
        {
            @Override
            public ResponseSchema getResponseSchema()
            {
                return ResponseSchema.TEXT;
            }

            @Override
            public String compileSystemMessage(final ExecutionContext ctx)
            {
                return "System";
            }

            @Override
            public String compileUserMessage(final ExecutionContext ctx)
            {
                return "Enter password SuperSecret123! into login form";
            }

            @Override
            public String parseResponse(final String rawContent, final ExecutionContext ctx)
            {
                return rawContent;
            }
        };

        final CallLlmStep<String> callLlmStep = new CallLlmStep<>(prompt, LlmCapability.TEXT_ONLY);
        callLlmStep.execute(context);

        org.junit.jupiter.api.Assertions.assertNotNull(provider.getLastRequest(), "LlmRequest should be recorded in mock provider.");
        final String dispatchedPrompt = provider.getLastRequest().userMessage();
        org.junit.jupiter.api.Assertions.assertTrue(dispatchedPrompt.contains("[MASKED_VAR_password]"), "Dispatched prompt to LLM should contain masked variable placeholder.");
        org.junit.jupiter.api.Assertions.assertFalse(dispatchedPrompt.contains("SuperSecret123!"), "Dispatched prompt to LLM should NOT contain raw secret password!");

        final Object result = context.getTransientData().get(ExecutionContext.KEY_LAST_LLM_RESULT);
        org.junit.jupiter.api.Assertions.assertEquals("Selected element with value ${password}", result, "Response content should be unmasked back to variable reference syntax.");
    }
}
