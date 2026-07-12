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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.SutAttachment;
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;
import org.neodymium.ai.prompt.AiPrompt;
import org.neodymium.ai.session.AiSession;

/**
 * Concrete pipeline step compiling user/system instructions, executing LLM chat queries
 * via capability provider routing, and parsing the response content into context.
 *
 * @param <T> the type of parsed target output object
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class CallLlmStep<T> implements PipelineStep
{
    /**
     * The AI prompt template.
     */
    private final AiPrompt<T> prompt;

    /**
     * The capability required for provider routing.
     */
    private final LlmCapability capability;

    /**
     * Constructs a CallLlmStep.
     *
     * @param prompt the prompt compiler
     * @param capability the capability routing key
     */
    public CallLlmStep(final AiPrompt<T> prompt, final LlmCapability capability)
    {
        this.prompt = prompt;
        this.capability = capability;
    }

    /**
     * Compiles system/user instructions, queries the routed LLM provider,
     * parses the response, and places the parsed result in the transient data map.
     *
     * @param context the thread-isolated execution context
     * @throws PipelineException if the LLM query or response parsing fails
     */
    @Override
    public void execute(final ExecutionContext context) throws PipelineException
    {
        final AiSession session = (AiSession) context.getTransientData().get(ExecutionContext.KEY_SESSION);
        if (session == null)
        {
            return;
        }

        final String system = this.prompt.compileSystemMessage(context);
        final String user = this.prompt.compileUserMessage(context);

        List<SutAttachment> attachments = Collections.emptyList();
        final SutState lastState = (SutState) context.getTransientData().get(ExecutionContext.KEY_LAST_STATE);
        if (lastState != null && lastState.getAttachments() != null)
        {
            attachments = lastState.getAttachments();
        }

        final LlmRequest request = new LlmRequest(
            system,
            user,
            attachments,
            this.prompt.getResponseSchema(),
            0.0,
            60
        );

        final LlmProvider provider = session.getLlmRegistry().getProvider(this.capability);
        final LlmResponse response;
        try
        {
            response = provider.chat(request);
        }
        catch (final IOException e)
        {
            throw new ConclusiveFailureException("LLM provider communication failed", e);
        }

        try
        {
            final T parsedResult = this.prompt.parseResponse(response.content(), context);
            context.getTransientData().put(ExecutionContext.KEY_LAST_LLM_RESULT, parsedResult);
        }
        catch (final Exception e)
        {
            throw new ConclusiveFailureException("Failed to parse LLM response into target signature", e);
        }
    }
}
