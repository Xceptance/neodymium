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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.neodymium.ai.client.ChatMessage.Role;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.prompt.LlmSanitizerHelper;
import org.neodymium.ai.prompt.SanitizedPayload;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolDefinition;

/**
 * Native OpenAI provider backed by LangChain4j OpenAiChatModel.
 * Supports native tool calling, multi-turn messages, reasoning effort, and multimodal images.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class OpenAiLlmProvider implements LlmProvider
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AiConfiguration config;
    private final String apiKey;
    private final String modelName;
    private final String baseUrl;
    private final ChatModel defaultModel;
    private final ConcurrentHashMap<String, ChatModel> modelCache = new ConcurrentHashMap<>();

    /**
     * Constructs an OpenAiLlmProvider with dynamic configuration resolution.
     */
    public OpenAiLlmProvider()
    {
        this.config = AiConfiguration.getInstance();

        final String resolvedKey = this.config.getProperty("neodymium.ai.openai.apiKey", this.config.getApiKey("openai"));
        this.apiKey = resolvedKey != null ? resolvedKey : System.getenv("OPENAI_API_KEY");

        if (this.apiKey == null || this.apiKey.isBlank())
        {
            throw new IllegalArgumentException("OpenAI API key is missing. Please configure neodymium.ai.openai.apiKey or set OPENAI_API_KEY.");
        }

        this.modelName = this.config.getProperty("neodymium.ai.openai.model", this.config.getModel("openai") != null ? this.config.getModel("openai") : "gpt-4o");
        this.baseUrl = this.config.getProperty("neodymium.ai.openai.baseUrl", "https://api.openai.com/v1");

        this.defaultModel = buildChatModel(0.0, 180, ResponseSchema.TEXT, ReasoningEffort.MEDIUM);
    }

    /**
     * Builds an OpenAiChatModel instance with specified temperature, timeout, response schema, and reasoning effort.
     */
    private ChatModel buildChatModel(
        final double temperature,
        final int timeoutSeconds,
        final ResponseSchema responseSchema,
        final ReasoningEffort reasoningEffort
    )
    {
        final int maxTokens = ResponseSchema.resolveMaxOutputTokens(responseSchema);
        final ReasoningEffort effort = reasoningEffort != null ? reasoningEffort : ResponseSchema.resolveReasoningEffort(responseSchema);

        final String effortString = switch (effort)
        {
            case OFF -> null;
            case LOW -> "low";
            case MEDIUM -> "medium";
            case HIGH -> "high";
        };

        final OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
            .baseUrl(this.baseUrl)
            .apiKey(this.apiKey)
            .modelName(this.modelName)
            .temperature(temperature)
            .maxTokens(maxTokens)
            .timeout(Duration.ofSeconds(timeoutSeconds > 0 ? timeoutSeconds : 180));

        if (effortString != null)
        {
            builder.reasoningEffort(effortString);
        }

        return builder.build();
    }

    /**
     * Resolves appropriate ChatModel for the given temperature, timeout, response schema, and reasoning effort settings.
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

        if (temp == 0.0 && timeout == 180 && schema == ResponseSchema.TEXT && effort == ReasoningEffort.MEDIUM)
        {
            return this.defaultModel;
        }

        final String cacheKey = String.format("%s:%.2f:%d:%s:%s", this.modelName, temp, timeout, schema.name(), effort.name());
        return this.modelCache.computeIfAbsent(cacheKey, k -> buildChatModel(temp, timeout, schema, effort));
    }

    /**
     * Maps an internal ChatMessage to LangChain4j ChatMessage representation.
     */
    private static dev.langchain4j.data.message.ChatMessage toLangChainMessage(
        final ChatMessage msg,
        final List<SutAttachment> attachments,
        final boolean isLastUserMessage
    )
    {
        return switch (msg.role())
        {
            case SYSTEM -> SystemMessage.from(msg.content() != null ? msg.content() : "");
            case USER -> {
                if (isLastUserMessage && attachments != null && !attachments.isEmpty())
                {
                    final List<Content> contents = new ArrayList<>();
                    if (msg.content() != null && !msg.content().isBlank())
                    {
                        contents.add(TextContent.from(msg.content()));
                    }
                    for (final SutAttachment attachment : attachments)
                    {
                        if (attachment.base64Data() != null)
                        {
                            final boolean isImage = attachment.mediaType() != null &&
                                (attachment.mediaType().startsWith("image/") || "screenshot".equalsIgnoreCase(attachment.mediaType()));
                            if (isImage)
                            {
                                final String mimeType = (attachment.mediaType() != null && attachment.mediaType().startsWith("image/"))
                                    ? attachment.mediaType() : "image/png";
                                contents.add(ImageContent.from(attachment.base64Data(), mimeType));
                            }
                            else
                            {
                                try
                                {
                                    final String decoded = new String(Base64.getDecoder().decode(attachment.base64Data()), StandardCharsets.UTF_8);
                                    contents.add(TextContent.from("\n--- Attachment: " + attachment.mediaType() + " ---\n" + decoded));
                                }
                                catch (final Exception e)
                                {
                                    contents.add(TextContent.from("\n--- [Unreadable Attachment: " + attachment.mediaType() + "] ---\n"));
                                }
                            }
                        }
                    }
                    yield UserMessage.from(contents);
                }
                else
                {
                    yield UserMessage.from(msg.content() != null ? msg.content() : "");
                }
            }
            case ASSISTANT -> {
                if (msg.hasToolCalls())
                {
                    final List<ToolExecutionRequest> requests = new ArrayList<>();
                    for (final ToolCall tc : msg.toolCalls())
                    {
                        final String id = tc.callId() != null ? tc.callId() : tc.toolName();
                        final String args = tc.arguments() != null ? tc.arguments().toString() : "{}";
                        requests.add(ToolExecutionRequest.builder()
                            .id(id)
                            .name(tc.toolName())
                            .arguments(args)
                            .build());
                    }
                    if (msg.content() != null && !msg.content().isBlank())
                    {
                        yield AiMessage.from(msg.content(), requests);
                    }
                    else
                    {
                        yield AiMessage.from(requests);
                    }
                }
                else
                {
                    yield AiMessage.from(msg.content() != null ? msg.content() : "");
                }
            }
            case TOOL -> {
                final String id = msg.toolCallId() != null ? msg.toolCallId() : (msg.toolName() != null ? msg.toolName() : "call_1");
                final String name = msg.toolName() != null ? msg.toolName() : "tool";
                final String content = msg.content() != null ? msg.content() : "";
                yield ToolExecutionResultMessage.from(id, name, content);
            }
        };
    }

    @Override
    public LlmResponse chat(final LlmRequest rawRequest) throws IOException
    {
        final SanitizedPayload sanitizedPayload = LlmSanitizerHelper.sanitizeRequest(rawRequest);
        final LlmRequest request = LlmSanitizerHelper.toSanitizedRequest(rawRequest, sanitizedPayload);

        final List<dev.langchain4j.data.message.ChatMessage> lcMessages = new ArrayList<>();
        final List<ChatMessage> sourceMessages = request.messages();

        int lastUserIndex = -1;
        for (int i = sourceMessages.size() - 1; i >= 0; i--)
        {
            if (sourceMessages.get(i).role() == Role.USER)
            {
                lastUserIndex = i;
                break;
            }
        }

        for (int i = 0; i < sourceMessages.size(); i++)
        {
            final ChatMessage msg = sourceMessages.get(i);
            final boolean isLastUser = (i == lastUserIndex);
            lcMessages.add(toLangChainMessage(msg, request.attachments(), isLastUser));
        }

        final List<ToolSpecification> toolSpecifications = new ArrayList<>();
        if (request.tools() != null && !request.tools().isEmpty())
        {
            for (final ToolDefinition td : request.tools())
            {
                final ObjectNode specJson = MAPPER.createObjectNode();
                specJson.put("name", td.name());
                specJson.put("description", td.description());
                specJson.set("parameters", td.parametersSchema());
                toolSpecifications.add(ToolSpecification.fromJson(specJson.toString()));
            }
        }

        final ChatRequest.Builder requestBuilder = ChatRequest.builder().messages(lcMessages);
        if (!toolSpecifications.isEmpty())
        {
            requestBuilder.toolSpecifications(toolSpecifications);
        }
        final ChatRequest chatRequest = requestBuilder.build();

        return LlmRetryHelper.executeWithRetry(() -> {
            try
            {
                final ChatModel activeModel = getChatModel(
                    request.temperature(),
                    request.timeoutSeconds(),
                    request.responseSchema(),
                    request.reasoningEffort()
                );
                final ChatResponse response = activeModel.chat(chatRequest);

                final dev.langchain4j.model.output.TokenUsage usage = response.tokenUsage();

                TokenUsage mappedUsage = null;
                if (usage != null)
                {
                    mappedUsage = new TokenUsage(
                        usage.inputTokenCount(),
                        usage.outputTokenCount(),
                        usage.totalTokenCount(),
                        0
                    );
                }

                final AiMessage aiMsg = response.aiMessage();
                final List<ToolCall> toolCalls = new ArrayList<>();
                if (aiMsg != null && aiMsg.hasToolExecutionRequests())
                {
                    for (final ToolExecutionRequest ter : aiMsg.toolExecutionRequests())
                    {
                        final JsonNode argsNode;
                        if (ter.arguments() != null && !ter.arguments().isBlank())
                        {
                            argsNode = MAPPER.readTree(ter.arguments());
                        }
                        else
                        {
                            argsNode = MAPPER.createObjectNode();
                        }
                        final String callId = (ter.id() != null && !ter.id().isBlank())
                            ? ter.id()
                            : (ter.name() + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
                        toolCalls.add(new ToolCall(callId, ter.name(), argsNode));
                    }
                }

                final String content = (aiMsg != null && aiMsg.text() != null) ? aiMsg.text() : "";
                final LlmResponse rawResponse = new LlmResponse(content, mappedUsage, this.modelName, toolCalls);
                return LlmSanitizerHelper.unmaskResponse(rawResponse, sanitizedPayload.maskToVariableMap());
            }
            catch (final Exception e)
            {
                throw new IOException("Failed to execute OpenAI chat request: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public Set<LlmCapability> getCapabilities()
    {
        return EnumSet.of(
            LlmCapability.TEXT_ONLY,
            LlmCapability.VISION,
            LlmCapability.EXECUTION,
            LlmCapability.PESAP,
            LlmCapability.VERIFICATION,
            LlmCapability.LINTER
        );
    }
}
