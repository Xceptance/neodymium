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
package org.neodymium.ai.client;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel.MistralAiChatModelBuilder;
import dev.langchain4j.model.output.TokenUsage;
import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.prompt.LlmSanitizerHelper;
import org.neodymium.ai.prompt.SanitizedPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Native Mistral AI provider backed by LangChain4j.
 * Translates Neodymium requests into standard LangChain4j Mistral interactions.
 * 
 * Note: This provider does not declare the VISION capability as standard Mistral endpoints
 * are primarily text-based in this implementation layer.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class MistralLlmProvider implements LlmProvider
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MistralLlmProvider.class);

    private final AiConfiguration config;
    private final String apiKey;
    private final String modelName;
    private final String baseUrl;
    private final ConcurrentHashMap<String, ChatModel> modelCache = new ConcurrentHashMap<>();

    /**
     * Constructs a MistralLlmProvider and dynamically resolves its configuration.
     */
    public MistralLlmProvider()
    {
        this.config = AiConfiguration.getInstance();
        
        // Resolve Mistral specific properties or fallback to globals
        final String resolvedKey = this.config.getProperty("neodymium.ai.mistral.apiKey", this.config.getApiKey("mistral"));
        this.apiKey = resolvedKey != null ? resolvedKey : System.getenv("MISTRAL_API_KEY");
        
        if (this.apiKey == null || this.apiKey.isBlank())
        {
            throw new IllegalArgumentException("Mistral API key is missing. Please configure neodymium.ai.mistral.apiKey or set MISTRAL_API_KEY.");
        }

        this.modelName = this.config.getProperty("neodymium.ai.mistral.model", this.config.getModel("mistral"));

        this.baseUrl = this.config.getProperty("neodymium.ai.mistral.baseUrl", "https://api.mistral.ai/v1");
    }

    /**
     * Builds a MistralAiChatModel with specified temperature, timeout, and response schema.
     */
    private ChatModel buildChatModel(
        final double temperature,
        final int timeoutSeconds,
        final ResponseSchema responseSchema,
        final ReasoningEffort reasoningEffort
    )
    {
        final int maxTokens = ResponseSchema.resolveMaxOutputTokens(responseSchema);

        final MistralAiChatModelBuilder builder = MistralAiChatModel.builder()
            .baseUrl(this.baseUrl)
            .apiKey(this.apiKey)
            .modelName(this.modelName != null ? this.modelName : "mistral-large-latest")
            .temperature(temperature)
            .maxTokens(maxTokens);

        if (timeoutSeconds > 0)
        {
            builder.timeout(Duration.ofSeconds(timeoutSeconds));
        }

        if (LlmCommunicationLogger.isLoggingActive())
        {
            builder.logRequests(true);
            builder.logResponses(true);
            builder.logger(LlmCommunicationLogger.getLogger());
        }

        return builder.build();
    }

    /**
     * Resolves appropriate ChatModel for the given temperature, timeout, and response schema settings.
     */
    private ChatModel getChatModel(
        final double temperature,
        final int timeoutSeconds,
        final ResponseSchema responseSchema,
        final ReasoningEffort reasoningEffort
    )
    {
        final double temp = temperature >= 0.0 ? temperature : 0.0;
        final int timeout = timeoutSeconds > 0 ? timeoutSeconds : 180;
        final ResponseSchema schema = responseSchema != null ? responseSchema : ResponseSchema.TEXT;
        final ReasoningEffort effort = reasoningEffort != null ? reasoningEffort : ResponseSchema.resolveReasoningEffort(schema);
        final boolean isLoggingActive = LlmCommunicationLogger.isLoggingActive();

        final String cacheKey = String.format("%s:%.2f:%d:%s:%s:%b", this.modelName, temp, timeout, schema.name(), effort.name(), isLoggingActive);
        return this.modelCache.computeIfAbsent(cacheKey, k -> buildChatModel(temp, timeout, schema, effort));
    }

    @Override
    public LlmResponse chat(final LlmRequest rawRequest) throws IOException
    {
        final SanitizedPayload sanitizedPayload = LlmSanitizerHelper.sanitizeRequest(rawRequest);
        final LlmRequest request = LlmSanitizerHelper.toSanitizedRequest(rawRequest, sanitizedPayload);

        final List<ChatMessage> messages = new ArrayList<>();
        if (request.systemMessage() != null && !request.systemMessage().isBlank())
        {
            messages.add(SystemMessage.from(request.systemMessage()));
        }

        if (request.attachments() != null && !request.attachments().isEmpty())
        {
            LOGGER.warn("MistralLlmProvider does not support image attachments. Attachments will be ignored.");
        }

        messages.add(UserMessage.from(request.userMessage()));

        return LlmRetryHelper.executeWithRetry(() -> {
            try
            {
                final ChatModel activeModel = getChatModel(
                    request.temperature(),
                    request.timeoutSeconds(),
                    request.responseSchema(),
                    request.reasoningEffort()
                );
                final ChatResponse response = activeModel.chat(messages);

                final TokenUsage usage = response.tokenUsage();

                org.neodymium.ai.client.TokenUsage mappedUsage = null;
                if (usage != null)
                {
                    mappedUsage = new org.neodymium.ai.client.TokenUsage(
                        usage.inputTokenCount(),
                        usage.outputTokenCount(),
                        usage.totalTokenCount(),
                        extractCachedTokens(usage)
                    );
                }

                final LlmResponse rawResponse = new LlmResponse(response.aiMessage().text(), mappedUsage, this.modelName);
                return LlmSanitizerHelper.unmaskResponse(rawResponse, sanitizedPayload.maskToVariableMap());
            }
            catch (final Exception e)
            {
                throw new IOException("Failed to execute Mistral chat request: " + e.getMessage(), e);
            }
        });
    }

    private int extractCachedTokens(final TokenUsage usage)
    {
        if (usage == null)
        {
            return 0;
        }
        try
        {
            final Method detailsMethod = usage.getClass().getMethod("promptTokensDetails");
            final Object details = detailsMethod.invoke(usage);
            if (details != null)
            {
                final Method cachedMethod = details.getClass().getMethod("cachedTokens");
                final Object cached = cachedMethod.invoke(details);
                if (cached instanceof Integer)
                {
                    return (Integer) cached;
                }
            }
        }
        catch (final Exception e)
        {
            // Silent catch
        }
        return 0;
    }

    @Override
    public Set<LlmCapability> getCapabilities()
    {
        // Excludes LlmCapability.VISION
        return EnumSet.of(
            LlmCapability.TEXT_ONLY,
            LlmCapability.EXECUTION,
            LlmCapability.VERIFICATION,
            LlmCapability.LINTER
        );
    }
}
