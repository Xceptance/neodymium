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
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.HttpClientBuilderLoader;
import dev.langchain4j.http.client.HttpRequest;
import dev.langchain4j.http.client.SuccessfulHttpResponse;
import dev.langchain4j.http.client.sse.ServerSentEventListener;
import dev.langchain4j.http.client.sse.ServerSentEventParser;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GeminiThinkingConfig;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.neodymium.ai.client.ChatMessage.Role;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.prompt.LlmSanitizerHelper;
import org.neodymium.ai.prompt.SanitizedPayload;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolDefinition;

/**
 * Native Google Gemini AI provider backed by LangChain4j.
 * Translates Neodymium requests into standard LangChain4j Gemini interactions,
 * supporting native tool calling, multi-turn messages, and multimodal image attachments.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class GeminiLlmProvider implements LlmProvider
{
    private static final Logger LOGGER = LoggerFactory.getLogger(GeminiLlmProvider.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AiConfiguration config;
    private final String apiKey;
    private final String modelName;
    private final boolean includeThoughts;
    private final ChatModel defaultModel;
    private final ConcurrentHashMap<String, ChatModel> modelCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> thinkingSignatureCache = new ConcurrentHashMap<>();

    /**
     * Constructs a GeminiLlmProvider with global configuration settings.
     */
    public GeminiLlmProvider()
    {
        this.config = AiConfiguration.getInstance();

        // Resolve Gemini specific properties or fallback to globals
        final String resolvedKey = this.config.getProperty("neodymium.ai.gemini.apiKey", this.config.getApiKey("gemini"));
        this.apiKey = resolvedKey != null ? resolvedKey : System.getenv("GEMINI_API_KEY");

        if (this.apiKey == null || this.apiKey.isBlank())
        {
            throw new IllegalArgumentException("Gemini API key is missing. Please configure neodymium.ai.gemini.apiKey or set GEMINI_API_KEY.");
        }

        this.modelName = this.config.getProperty("neodymium.ai.gemini.model", this.config.getModel("gemini"));
        this.includeThoughts = this.config.isIncludeThoughts();

        this.defaultModel = buildChatModel(0.0, 180, ResponseSchema.TEXT, ReasoningEffort.MEDIUM);
    }

    /**
     * Builds a GeminiThinkingConfig instance mapped from the specified ReasoningEffort tier.
     */
    private static GeminiThinkingConfig buildThinkingConfig(final ReasoningEffort effort, final boolean includeThoughts)
    {
        final ReasoningEffort effectiveEffort = effort != null ? effort : ReasoningEffort.MEDIUM;
        return switch (effectiveEffort)
        {
            case OFF -> GeminiThinkingConfig.builder().thinkingLevel("MINIMAL").includeThoughts(false).build();
            case LOW -> GeminiThinkingConfig.builder().thinkingLevel("LOW").includeThoughts(includeThoughts).build();
            case MEDIUM -> GeminiThinkingConfig.builder().thinkingLevel("MEDIUM").includeThoughts(includeThoughts).build();
            case HIGH -> GeminiThinkingConfig.builder().thinkingLevel("HIGH").includeThoughts(includeThoughts).build();
        };
    }

    /**
     * Builds a GoogleAiGeminiChatModel with specified temperature, timeout, response schema, and reasoning effort.
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

        return GoogleAiGeminiChatModel.builder()
            .apiKey(this.apiKey)
            .modelName(this.modelName != null ? this.modelName : "gemini-3.5-flash-lite")
            .temperature(temperature)
            .maxOutputTokens(maxTokens)
            .thinkingConfig(buildThinkingConfig(effort, this.includeThoughts))
            .returnThinking(true)
            .sendThinking(true)
            .timeout(Duration.ofSeconds(timeoutSeconds > 0 ? timeoutSeconds : 180))
            .httpClientBuilder(createDecoratedHttpClientBuilder())
            .build();
    }

    /**
     * Creates an HttpClientBuilder that intercepts outgoing requests to Gemini and ensures all
     * functionCall parts contain a valid thought_signature, working around LangChain4j's
     * parallel tool call signature omission.
     */
    private HttpClientBuilder createDecoratedHttpClientBuilder()
    {
        final HttpClientBuilder delegateBuilder = HttpClientBuilderLoader.loadHttpClientBuilder();
        return new HttpClientBuilder()
        {
            @Override
            public Duration connectTimeout()
            {
                return delegateBuilder.connectTimeout();
            }

            @Override
            public HttpClientBuilder connectTimeout(final Duration timeout)
            {
                delegateBuilder.connectTimeout(timeout);
                return this;
            }

            @Override
            public Duration readTimeout()
            {
                return delegateBuilder.readTimeout();
            }

            @Override
            public HttpClientBuilder readTimeout(final Duration timeout)
            {
                delegateBuilder.readTimeout(timeout);
                return this;
            }

            @Override
            public HttpClient build()
            {
                final HttpClient delegate = delegateBuilder.build();
                return new HttpClient()
                {
                    @Override
                    public SuccessfulHttpResponse execute(final HttpRequest request)
                    {
                        final HttpRequest repairedRequest = repairThoughtSignatures(request);
                        return delegate.execute(repairedRequest);
                    }

                    @Override
                    public void execute(final HttpRequest request, final ServerSentEventListener listener)
                    {
                        final HttpRequest repairedRequest = repairThoughtSignatures(request);
                        delegate.execute(repairedRequest, listener);
                    }

                    @Override
                    public void execute(final HttpRequest request, final ServerSentEventParser parser, final ServerSentEventListener listener)
                    {
                        final HttpRequest repairedRequest = repairThoughtSignatures(request);
                        delegate.execute(repairedRequest, parser, listener);
                    }
                };
            }
        };
    }

    /**
     * Inspects outgoing Gemini HTTP request payloads and ensures that every part containing a functionCall
     * has a valid thoughtSignature. If LangChain4j dropped the signature on parallel calls (position > 0),
     * this propagates the turn's existing signature or injects the official Google sentinel
     * "skip_thought_signature_validator".
     */
    static HttpRequest repairThoughtSignatures(final HttpRequest request)
    {
        if (request == null || request.body() == null || request.body().isBlank())
        {
            return request;
        }

        if (!request.body().contains("functionCall"))
        {
            return request;
        }

        try
        {
            final JsonNode rootNode = MAPPER.readTree(request.body());
            if (!(rootNode instanceof final ObjectNode rootObj))
            {
                return request;
            }

            final JsonNode contentsNode = rootObj.get("contents");
            if (contentsNode == null || !contentsNode.isArray())
            {
                return request;
            }

            boolean modified = false;
            for (final JsonNode contentNode : contentsNode)
            {
                if (contentNode instanceof final ObjectNode contentObj)
                {
                    final JsonNode partsNode = contentObj.get("parts");
                    if (partsNode != null && partsNode.isArray())
                    {
                        // 1. Locate any existing thoughtSignature in this content turn
                        String turnSignature = null;
                        for (final JsonNode partNode : partsNode)
                        {
                            if (partNode instanceof final ObjectNode partObj)
                            {
                                final JsonNode sigNode = partObj.has("thoughtSignature")
                                    ? partObj.get("thoughtSignature")
                                    : partObj.get("thought_signature");
                                if (sigNode != null && !sigNode.isNull() && !sigNode.asText().isBlank())
                                {
                                    turnSignature = sigNode.asText();
                                    break;
                                }
                            }
                        }

                        final String fallbackSignature = (turnSignature != null && !turnSignature.isBlank())
                            ? turnSignature
                            : "skip_thought_signature_validator";

                        // 2. Ensure every functionCall part has thoughtSignature populated
                        for (final JsonNode partNode : partsNode)
                        {
                            if (partNode instanceof final ObjectNode partObj && partObj.has("functionCall"))
                            {
                                final JsonNode sigNode = partObj.has("thoughtSignature")
                                    ? partObj.get("thoughtSignature")
                                    : partObj.get("thought_signature");
                                if (sigNode == null || sigNode.isNull() || sigNode.asText().isBlank())
                                {
                                    partObj.put("thoughtSignature", fallbackSignature);
                                    modified = true;
                                }
                            }
                        }
                    }
                }
            }

            if (!modified)
            {
                return request;
            }

            final String updatedBody = rootObj.toString();
            return HttpRequest.builder()
                .method(request.method())
                .url(request.url())
                .headers(request.headers())
                .formDataFields(request.formDataFields())
                .formDataFiles(request.formDataFiles())
                .body(updatedBody)
                .build();
        }
        catch (final Exception e)
        {
            LOGGER.warn("Failed to inspect/repair thought signatures in Gemini request payload: {}", e.getMessage());
            return request;
        }
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
    private dev.langchain4j.data.message.ChatMessage toLangChainMessage(
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
                    String thinkingSig = null;
                    for (final ToolCall tc : msg.toolCalls())
                    {
                        final String id = tc.callId() != null ? tc.callId() : tc.toolName();
                        final String args = tc.arguments() != null ? tc.arguments().toString() : "{}";
                        requests.add(ToolExecutionRequest.builder()
                            .id(id)
                            .name(tc.toolName())
                            .arguments(args)
                            .build());
                        if (thinkingSig == null && tc.callId() != null)
                        {
                            thinkingSig = this.thinkingSignatureCache.get(tc.callId());
                        }
                        if (thinkingSig == null && tc.toolName() != null)
                        {
                            thinkingSig = this.thinkingSignatureCache.get(tc.toolName());
                        }
                    }
                    final AiMessage.Builder builder = AiMessage.builder()
                        .toolExecutionRequests(requests);
                    if (msg.content() != null && !msg.content().isBlank())
                    {
                        builder.text(msg.content());
                    }
                    final String effectiveSig = (thinkingSig != null && !thinkingSig.isBlank())
                        ? thinkingSig
                        : "skip_thought_signature_validator";
                    builder.attributes(Map.of("thinking_signature", effectiveSig));
                    yield builder.build();
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
                specJson.set("parameters", sanitizeSchema(td.parametersSchema().deepCopy()));
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
                        extractCachedTokens(usage)
                    );
                }

                final AiMessage aiMsg = response.aiMessage();
                final List<ToolCall> toolCalls = new ArrayList<>();
                if (aiMsg != null && aiMsg.hasToolExecutionRequests())
                {
                    final String thinkingSig = (String) aiMsg.attribute("thinking_signature", String.class);
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
                        if (thinkingSig != null)
                        {
                            this.thinkingSignatureCache.put(callId, thinkingSig);
                            this.thinkingSignatureCache.put(ter.name(), thinkingSig);
                        }
                        toolCalls.add(new ToolCall(callId, ter.name(), argsNode));
                    }
                }

                final String thinking = (aiMsg != null && aiMsg.thinking() != null && !aiMsg.thinking().isBlank())
                    ? aiMsg.thinking()
                    : null;
                final String content = (aiMsg != null && aiMsg.text() != null) ? aiMsg.text() : "";
                final LlmResponse rawResponse = new LlmResponse(content, mappedUsage, this.modelName, toolCalls, thinking);
                return LlmSanitizerHelper.unmaskResponse(rawResponse, sanitizedPayload.maskToVariableMap());
            }
            catch (final Exception e)
            {
                throw new IOException("Failed to execute Gemini chat request: " + e.getMessage(), e);
            }
        });
    }

    private int extractCachedTokens(final dev.langchain4j.model.output.TokenUsage usage)
    {
        if (usage == null)
        {
            return 0;
        }
        try
        {
            final Method method = usage.getClass().getMethod("cachedContentTokenCount");
            final Object result = method.invoke(usage);
            if (result instanceof Number)
            {
                return ((Number) result).intValue();
            }
        }
        catch (final Exception e)
        {
            // Silent catch
        }
        return 0;
    }

    private static JsonNode sanitizeSchema(final JsonNode node)
    {
        if (node == null)
        {
            return null;
        }
        if (node.isObject())
        {
            final ObjectNode obj = (ObjectNode) node;
            if ("array".equalsIgnoreCase(obj.path("type").asText()) && !obj.has("items"))
            {
                obj.putObject("items").put("type", "string");
            }
            obj.fields().forEachRemaining(entry -> sanitizeSchema(entry.getValue()));
        }
        else if (node.isArray())
        {
            for (final JsonNode child : node)
            {
                sanitizeSchema(child);
            }
        }
        return node;
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
