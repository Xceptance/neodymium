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
import org.neodymium.ai.config.AiConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(CallLlmStep.class);
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
        final ExecutionContext previousContext = ExecutionContext.getActiveContext();
        try
        {
            ExecutionContext.setActiveContext(context);
            executeInternal(context);
        }
        finally
        {
            ExecutionContext.setActiveContext(previousContext);
        }
    }

    private void executeInternal(final ExecutionContext context) throws PipelineException
    {
        final AiSession session = (AiSession) context.getTransientData().get(ExecutionContext.KEY_SESSION);
        if (session == null)
        {
            return;
        }

        LOGGER.debug("Compiling prompt: {}", this.prompt.getClass().getSimpleName());
        final String system = this.prompt.compileSystemMessage(context);
        final String rawUser = this.prompt.compileUserMessage(context);

        List<SutAttachment> attachments = Collections.emptyList();
        final SutState lastState = (SutState) context.getTransientData().get(ExecutionContext.KEY_LAST_STATE);
        if (lastState != null && lastState.getAttachments() != null)
        {
            attachments = lastState.getAttachments();
            if (LOGGER.isTraceEnabled())
            {
                LOGGER.trace("Attachments count: {} (Target Provider Capability: {})", attachments.size(), this.capability);
                for (final SutAttachment attachment : attachments)
                {
                    LOGGER.trace("  - MimeType: {}, Path: {}", attachment.mediaType(), attachment.filePath());
                }
            }
        }

        final String user = rawUser;

        if (LOGGER.isTraceEnabled())
        {
            LOGGER.trace("System Prompt:\n{}", system);
            LOGGER.trace("User Prompt:\n{}", user);
        }

        final AiConfiguration config = AiConfiguration.getInstance();
        final double temp = config.getTemperature("action");
        final int timeoutSeconds = config.getTimeoutSeconds("action");

        final LlmRequest request = new LlmRequest(
            system,
            user,
            attachments,
            this.prompt.getResponseSchema(),
            temp,
            timeoutSeconds
        );

        final LlmProvider provider = session.getLlmRegistry().getProvider(this.capability);
        LOGGER.debug("Calling LLM provider '{}' via capability: {}", provider.getClass().getSimpleName(), this.capability);
        final String capName = this.capability != null ? this.capability.name() : "DEFAULT";
        session.getEventBus().dispatch(new org.neodymium.ai.event.llm.LlmRequestSentEvent(request, capName));
        final long startTime = System.currentTimeMillis();
        final LlmResponse response;
        try
        {
            response = provider.chat(request);
            final long durationMs = System.currentTimeMillis() - startTime;

            session.getEventBus().dispatch(new org.neodymium.ai.event.llm.LlmResponseReceivedEvent(request, response, durationMs, capName));
            LOGGER.debug("LLM response received. Length: {} chars (duration: {} ms)", response.content() != null ? response.content().length() : 0, durationMs);
            if (LOGGER.isTraceEnabled())
            {
                LOGGER.trace("Raw response content:\n{}", formatJsonForLogging(response.content()));
            }

            // Track stats
            final boolean isInternalCacheHit = response.modelName() != null && response.modelName().contains("(Cached)");
            if (isInternalCacheHit)
            {
                final Integer cacheHits = (Integer) context.getTransientData().getOrDefault(ExecutionContext.KEY_INTERNAL_CACHE_HITS, 0);
                context.getTransientData().put(ExecutionContext.KEY_INTERNAL_CACHE_HITS, cacheHits + 1);
                LOGGER.info("⚡ CallLlmStep replayed response from internal LLM cache for instruction: {}", context.getTransientData().get(ExecutionContext.KEY_CURRENT_INSTRUCTION));
            }

            final Integer calls = (Integer) context.getTransientData().getOrDefault(ExecutionContext.KEY_TOTAL_LLM_CALLS, 0);
            LOGGER.info("📞 CallLlmStep incrementing totalLlmCalls from {} to {} for instruction: {}", calls, calls + 1, context.getTransientData().get(ExecutionContext.KEY_CURRENT_INSTRUCTION));
            context.getTransientData().put(ExecutionContext.KEY_TOTAL_LLM_CALLS, calls + 1);
            final Integer stdCalls = (Integer) context.getTransientData().getOrDefault(ExecutionContext.KEY_STANDARD_CALL_COUNT, 0);
            context.getTransientData().put(ExecutionContext.KEY_STANDARD_CALL_COUNT, stdCalls + 1);

            final org.neodymium.ai.client.TokenUsage newUsage = response.tokenUsage();
            if (newUsage != null)
            {
                final Object statsObj = context.getTransientData().get("KEY_CURRENT_STEP_STATS");
                if (statsObj instanceof org.neodymium.ai.pipeline.StepStats stats)
                {
                    stats.addStandardCall(newUsage.inputTokenCount(), newUsage.outputTokenCount(), newUsage.cachedTokenCount());
                }

                LOGGER.debug("   📊 Call Tokens: {} in ({} cached) → {} out (total: {})",
                    newUsage.inputTokenCount(), newUsage.cachedTokenCount(), newUsage.outputTokenCount(), newUsage.totalTokenCount());

                final org.neodymium.ai.client.TokenUsage existing = (org.neodymium.ai.client.TokenUsage) context.getTransientData().get(ExecutionContext.KEY_STANDARD_TOKEN_USAGE);
                if (existing == null)
                {
                    context.getTransientData().put(ExecutionContext.KEY_STANDARD_TOKEN_USAGE, newUsage);
                }
                else
                {
                    context.getTransientData().put(ExecutionContext.KEY_STANDARD_TOKEN_USAGE, new org.neodymium.ai.client.TokenUsage(
                        existing.inputTokenCount() + newUsage.inputTokenCount(),
                        existing.outputTokenCount() + newUsage.outputTokenCount(),
                        existing.totalTokenCount() + newUsage.totalTokenCount(),
                        existing.cachedTokenCount() + newUsage.cachedTokenCount()
                    ));
                }
            }
        }
        catch (final IOException e)
        {
            LOGGER.error("LLM provider communication failed", e);
            throw new ConclusiveFailureException("LLM provider communication failed", e);
        }

        try
        {
            final T parsedResult = this.prompt.parseResponse(response.content(), context);
            if (parsedResult instanceof java.util.List<?> list)
            {
                LOGGER.debug("Successfully parsed response: {} actions extracted", list.size());
                int index = 1;
                for (final Object obj : list)
                {
                    if (obj instanceof org.neodymium.ai.action.Action action)
                    {
                        LOGGER.debug("   ┌─ Action #{} Details ───────────────────────────────────────", index++);
                        LOGGER.debug("   │ Type:      {}", action.getType());
                        LOGGER.debug("   │ Target:    {}", action.getTarget());
                        LOGGER.debug("   │ Value:     {}", action.getValues());
                        LOGGER.debug("   │ Reasoning: {}", action.getReasoning());
                        LOGGER.debug("   └────────────────────────────────────────────────────────");
                    }
                }
            }
            else if (parsedResult instanceof org.neodymium.ai.prompt.VerificationResult vr)
            {
                LOGGER.debug("Successfully parsed response: VerificationResult (passed={}, actionReasoning='{}', visualReasoning='{}')", vr.passed(), vr.actionReasoning(), vr.visualReasoning());
            }
            else
            {
                LOGGER.debug("Successfully parsed response signature: {}", parsedResult != null ? parsedResult.getClass().getSimpleName() : "null");
            }
            context.getTransientData().put(ExecutionContext.KEY_LAST_LLM_RESULT, parsedResult);
        }
        catch (final PipelineException e)
        {
            throw e;
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to parse LLM response into target signature. Raw response: {}", response.content(), e);
            throw new ConclusiveFailureException("Failed to parse LLM response into target signature", e);
        }
    }

    static String formatJsonForLogging(final String rawContent)
    {
        if (rawContent == null || rawContent.trim().isEmpty())
        {
            return rawContent;
        }
        try
        {
            String jsonContent = rawContent.trim();
            if (jsonContent.startsWith("```json"))
            {
                jsonContent = jsonContent.substring(7);
                if (jsonContent.endsWith("```"))
                {
                    jsonContent = jsonContent.substring(0, jsonContent.length() - 3);
                }
            }
            else if (jsonContent.startsWith("```"))
            {
                jsonContent = jsonContent.substring(3);
                if (jsonContent.endsWith("```"))
                {
                    jsonContent = jsonContent.substring(0, jsonContent.length() - 3);
                }
            }
            
            final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            final Object json = mapper.readValue(jsonContent.trim(), Object.class);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        }
        catch (final Exception e)
        {
            return rawContent;
        }
    }
}
