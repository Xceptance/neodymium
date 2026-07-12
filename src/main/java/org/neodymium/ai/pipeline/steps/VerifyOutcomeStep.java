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
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.executor.TargetExecutor;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.HealingRequiredException;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;
import org.neodymium.ai.prompt.VerificationPrompt;
import org.neodymium.ai.prompt.VerificationResult;
import org.neodymium.ai.session.AiSession;

/**
 * Pipeline step executed after ExecuteActionsStep. Evaluates step execution
 * outcome by performing a dual-state semantic validation comparison if enabled.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class VerifyOutcomeStep implements PipelineStep
{
    /**
     * Constructs a VerifyOutcomeStep.
     */
    public VerifyOutcomeStep()
    {
    }

    @Override
    public void execute(final ExecutionContext context) throws PipelineException
    {
        final AiConfiguration config = new AiConfiguration();
        if (!config.isSemanticVerificationEnabled())
        {
            return;
        }

        final AiSession session = (AiSession) context.getTransientData().get(ExecutionContext.KEY_SESSION);
        final TargetExecutor executor = (TargetExecutor) context.getTransientData().get(ExecutionContext.KEY_TARGET_EXECUTOR);

        if (session == null)
        {
            throw new ConclusiveFailureException("No active AiSession registered in ExecutionContext transient data");
        }
        if (executor == null)
        {
            throw new ConclusiveFailureException("No active TargetExecutor registered in ExecutionContext transient data");
        }

        try
        {
            // 1. Capture the final state dynamically after action execution
            final SutState finalState = executor.captureState();
            context.getTransientData().put("finalState", finalState);

            // 2. Query the LLM using VerificationPrompt
            final VerificationPrompt prompt = new VerificationPrompt();
            final String system = prompt.compileSystemMessage(context);
            final String user = prompt.compileUserMessage(context);

            List<SutAttachment> attachments = Collections.emptyList();
            if (finalState != null && finalState.getAttachments() != null)
            {
                attachments = finalState.getAttachments();
            }

            final LlmRequest request = new LlmRequest(
                system,
                user,
                attachments,
                prompt.getResponseSchema(),
                config.getTemperature("verification"),
                config.getTimeoutSeconds("verification")
            );

            final LlmProvider provider = session.getLlmRegistry().getProvider(LlmCapability.VERIFICATION);
            final LlmResponse response = provider.chat(request);

            // 3. Accumulate token usage stats separately for verification
            final TokenUsage newUsage = response.tokenUsage();
            if (newUsage != null)
            {
                final TokenUsage existing = (TokenUsage) context.getTransientData().get(ExecutionContext.KEY_VERIFICATION_TOKEN_USAGE);
                if (existing == null)
                {
                    context.getTransientData().put(ExecutionContext.KEY_VERIFICATION_TOKEN_USAGE, newUsage);
                }
                else
                {
                    context.getTransientData().put(ExecutionContext.KEY_VERIFICATION_TOKEN_USAGE, new TokenUsage(
                        existing.inputTokenCount() + newUsage.inputTokenCount(),
                        existing.outputTokenCount() + newUsage.outputTokenCount(),
                        existing.totalTokenCount() + newUsage.totalTokenCount()
                    ));
                }
            }

            // 4. Parse response and throw exception if check failed
            final VerificationResult result = prompt.parseResponse(response.content(), context);
            if (!result.passed())
            {
                throw new HealingRequiredException("Semantic outcome verification failed: " + result.reasoning());
            }

            // 5. Update lastState to the verified finalState
            context.getTransientData().put(ExecutionContext.KEY_LAST_STATE, finalState);
        }
        catch (final PipelineException e)
        {
            throw e;
        }
        catch (final Exception e)
        {
            throw new HealingRequiredException("Failed executing semantic outcome verification check", e);
        }
        finally
        {
            // Cleanup transient finalState from the context
            context.getTransientData().remove("finalState");
        }
    }
}
