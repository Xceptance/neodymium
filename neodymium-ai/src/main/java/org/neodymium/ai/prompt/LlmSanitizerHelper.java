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
package org.neodymium.ai.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.neodymium.ai.client.ChatMessage;
import org.neodymium.ai.client.ChatMessage.Role;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.SutAttachment;
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.util.WireImageOptimizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper utility for central outbound LLM request secret masking and response unmasking.
 * Applied centrally at provider dispatch time across all LLM capabilities and call sites.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class LlmSanitizerHelper
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LlmSanitizerHelper.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern MASKED_TOKEN_PATTERN = Pattern.compile("\\[MASKED_VAR_[^\\]]+\\]");

    private LlmSanitizerHelper()
    {
    }

    /**
     * Sanitizes an outbound LLM request by masking sensitive session credentials in the user prompt and SUT DOM state.
     *
     * @param request the raw LLM request
     * @return a SanitizedPayload holding the sanitized prompt text and reverse variable mappings
     */
    public static SanitizedPayload sanitizeRequest(final LlmRequest request)
    {
        if (request == null)
        {
            return new SanitizedPayload(null, null, Map.of());
        }

        final ExecutionContext ctx = ExecutionContext.getActiveContext();
        if (ctx == null || ctx.getSessionData() == null)
        {
            LOGGER.warn("⚠️ [Security Warning] Outbound LLM request dispatched without active ExecutionContext bound to thread. Secret masking skipped.");
            return new SanitizedPayload(request.userMessage(), null, Map.of());
        }

        final SutState lastState = (SutState) ctx.getTransientData().get(ExecutionContext.KEY_LAST_STATE);
        final ContextSanitizer sanitizer = new DefaultContextSanitizer();
        return sanitizer.sanitize(request.userMessage(), lastState, ctx.getSessionData());
    }

    /**
     * Creates a new LlmRequest replacing the user prompt with the sanitized prompt text,
     * preserving multi-turn messages, tools, and attachments while optimizing binary images for the wire.
     *
     * @param original the original raw LlmRequest
     * @param payload the sanitized payload
     * @return the sanitized LlmRequest
     */
    public static LlmRequest toSanitizedRequest(final LlmRequest original, final SanitizedPayload payload)
    {
        if (original == null || payload == null || payload.sanitizedPrompt() == null)
        {
            return original;
        }

        final ExecutionContext ctx = ExecutionContext.getActiveContext();
        final Map<String, String> sensitiveMap = (ctx != null && ctx.getSessionData() != null)
            ? ctx.getSessionData().getRawSensitiveData()
            : Map.of();
        final MaskingContext maskingContext = !sensitiveMap.isEmpty() ? createMaskingContext(sensitiveMap) : null;

        List<SutAttachment> sanitizedAttachments = original.attachments();
        if (sanitizedAttachments != null && !sanitizedAttachments.isEmpty() && payload.maskToVariableMap() != null && !payload.maskToVariableMap().isEmpty())
        {
            final List<SutAttachment> updated = new ArrayList<>();
            for (final SutAttachment att : sanitizedAttachments)
            {
                if (att.base64Data() != null && att.mediaType() != null && !att.mediaType().startsWith("image/"))
                {
                    try
                    {
                        String decoded = new String(Base64.getDecoder().decode(att.base64Data()), StandardCharsets.UTF_8);
                        if (maskingContext != null)
                        {
                            decoded = maskingContext.mask(decoded);
                        }
                        else
                        {
                            for (final Map.Entry<String, String> entry : payload.maskToVariableMap().entrySet())
                            {
                                decoded = decoded.replace(entry.getValue(), entry.getKey());
                            }
                        }
                        final String encoded = Base64.getEncoder().encodeToString(decoded.getBytes(StandardCharsets.UTF_8));
                        updated.add(new SutAttachment(att.mediaType(), att.filePath(), encoded));
                    }
                    catch (final Exception e)
                    {
                        updated.add(att);
                    }
                }
                else
                {
                    updated.add(att);
                }
            }
            sanitizedAttachments = updated;
        }

        final List<SutAttachment> wireAttachments = WireImageOptimizer.optimize(sanitizedAttachments);

        final List<ChatMessage> updatedMessages;
        if (original.messages() != null && !original.messages().isEmpty())
        {
            updatedMessages = new ArrayList<>();
            for (final ChatMessage msg : original.messages())
            {
                final List<SutAttachment> optimizedMsgAtts = (msg.attachments() != null && !msg.attachments().isEmpty())
                    ? WireImageOptimizer.optimize(msg.attachments())
                    : msg.attachments();

                String sanitizedContent = msg.content();
                if (msg.role() == Role.USER && sanitizedContent != null && sanitizedContent.equals(original.userMessage()))
                {
                    sanitizedContent = payload.sanitizedPrompt();
                }
                else if (sanitizedContent != null && maskingContext != null)
                {
                    sanitizedContent = maskJsonOrTextContent(sanitizedContent, maskingContext);
                }

                List<ToolCall> sanitizedToolCalls = msg.toolCalls();
                if (sanitizedToolCalls != null && !sanitizedToolCalls.isEmpty() && maskingContext != null)
                {
                    List<ToolCall> updatedCalls = null;
                    for (int i = 0; i < sanitizedToolCalls.size(); i++)
                    {
                        final ToolCall tc = sanitizedToolCalls.get(i);
                        if (tc.arguments() != null && tc.arguments().isObject())
                        {
                            final ObjectNode argsCopy = ((ObjectNode) tc.arguments()).deepCopy();
                            final boolean changed = maskObjectNode(argsCopy, maskingContext);
                            if (changed)
                            {
                                if (updatedCalls == null)
                                {
                                    updatedCalls = new ArrayList<>(sanitizedToolCalls);
                                }
                                updatedCalls.set(i, new ToolCall(tc.callId(), tc.toolName(), argsCopy));
                            }
                        }
                    }
                    if (updatedCalls != null)
                    {
                        sanitizedToolCalls = updatedCalls;
                    }
                }

                if (!Objects.equals(sanitizedContent, msg.content())
                    || sanitizedToolCalls != msg.toolCalls()
                    || optimizedMsgAtts != msg.attachments())
                {
                    updatedMessages.add(new ChatMessage(
                        msg.role(),
                        sanitizedContent,
                        sanitizedToolCalls,
                        msg.toolCallId(),
                        msg.toolName(),
                        optimizedMsgAtts
                    ));
                }
                else
                {
                    updatedMessages.add(msg);
                }
            }
        }
        else
        {
            updatedMessages = List.of();
        }

        return new LlmRequest(
            updatedMessages,
            original.tools(),
            wireAttachments,
            original.responseSchema(),
            original.temperature(),
            original.timeoutSeconds(),
            original.reasoningEffort()
        );
    }

    /**
     * Unmasks returned LLM response content and tool call arguments by mapping format-preserving placeholders back to variable references.
     *
     * @param response the raw LLM response
     * @param maskMap the reverse variable map ([MASKED_VAR_key] -> ${key})
     * @return the unmasked LlmResponse
     */
    public static LlmResponse unmaskResponse(final LlmResponse response, final Map<String, String> maskMap)
    {
        if (response == null || maskMap == null || maskMap.isEmpty())
        {
            return response;
        }

        String content = response.content();
        if (content != null)
        {
            for (final Map.Entry<String, String> entry : maskMap.entrySet())
            {
                content = content.replace(entry.getKey(), entry.getValue());
            }
        }

        final List<ToolCall> unmaskedToolCalls;
        if (response.hasToolCalls())
        {
            unmaskedToolCalls = new ArrayList<>();
            for (final ToolCall tc : response.toolCalls())
            {
                if (tc.arguments() != null)
                {
                    String argsStr = tc.arguments().toString();
                    for (final Map.Entry<String, String> entry : maskMap.entrySet())
                    {
                        argsStr = argsStr.replace(entry.getKey(), entry.getValue());
                    }
                    try
                    {
                        final JsonNode unmaskedNode = MAPPER.readTree(argsStr);
                        unmaskedToolCalls.add(new ToolCall(tc.callId(), tc.toolName(), unmaskedNode));
                    }
                    catch (final Exception e)
                    {
                        unmaskedToolCalls.add(tc);
                    }
                }
                else
                {
                    unmaskedToolCalls.add(tc);
                }
            }
        }
        else
        {
            unmaskedToolCalls = List.of();
        }

        String thinking = response.thinking();
        if (thinking != null)
        {
            for (final Map.Entry<String, String> entry : maskMap.entrySet())
            {
                thinking = thinking.replace(entry.getKey(), entry.getValue());
            }
        }

        return new LlmResponse(content, response.tokenUsage(), response.modelName(), unmaskedToolCalls, thinking);
    }

    /**
     * Masks sensitive values in the provided content using format-preserving placeholders {@code [MASKED_VAR_key]}.
     * Sorts secrets by length descending to prevent partial substring leaks, and protects existing
     * {@code [MASKED_VAR_...]} tokens from recursive self-masking when secret values are substrings of variable keys.
     *
     * @param content the raw text or JSON string to sanitize
     * @param sensitiveMap map of variable keys to raw secret values
     * @return sanitized string with secrets masked, or the original content if null or empty
     */
    public static String maskSensitiveValues(final String content, final Map<String, String> sensitiveMap)
    {
        if (content == null || content.isEmpty() || sensitiveMap == null || sensitiveMap.isEmpty())
        {
            return content;
        }

        final MaskingContext context = createMaskingContext(sensitiveMap);
        return context != null ? context.mask(content) : content;
    }

    /**
     * Immutable compilation context holding the pre-compiled regex pattern and secret-to-token map
     * for masking sensitive values across multiple calls and JSON AST traversal without recompiling.
     *
     * @param pattern compiled regex pattern alternating existing tokens and quoted secrets
     * @param secretToToken mapping from plaintext secret string to masked token e.g. {@code [MASKED_VAR_key]}
     */
    private record MaskingContext(Pattern pattern, Map<String, String> secretToToken)
    {
        /**
         * Replaces occurrences of secrets in the given content with their corresponding masked tokens,
         * preserving any pre-existing masked tokens without re-masking them.
         *
         * @param content text content to mask
         * @return masked content
         */
        String mask(final String content)
        {
            if (content == null || content.isEmpty())
            {
                return content;
            }

            final Matcher matcher = this.pattern.matcher(content);
            final StringBuilder sb = new StringBuilder();
            while (matcher.find())
            {
                if (matcher.group(1) != null)
                {
                    // Group 1: Pre-existing [MASKED_VAR_...] token - preserve as-is without re-masking substrings
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(1)));
                }
                else
                {
                    // Group 2: Secret value match
                    final String matchedSecret = matcher.group(2);
                    final String token = this.secretToToken.get(matchedSecret);
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(token != null ? token : matchedSecret));
                }
            }
            matcher.appendTail(sb);
            return sb.toString();
        }
    }

    /**
     * Builds an immutable {@link MaskingContext} from the given sensitive map.
     * Secrets are sorted length-descending to ensure longer prefixes match before shorter substrings,
     * with deterministic alphabetical tie-breaking across variable keys.
     *
     * @param sensitiveMap map of variable names to secret values
     * @return compiled masking context, or {@code null} if no non-empty secrets exist
     */
    private static MaskingContext createMaskingContext(final Map<String, String> sensitiveMap)
    {
        if (sensitiveMap == null || sensitiveMap.isEmpty())
        {
            return null;
        }

        final List<Map.Entry<String, String>> sortedEntries = new ArrayList<>();
        for (final Map.Entry<String, String> entry : sensitiveMap.entrySet())
        {
            if (entry.getValue() != null && !entry.getValue().isEmpty())
            {
                sortedEntries.add(entry);
            }
        }

        if (sortedEntries.isEmpty())
        {
            return null;
        }

        // Sort by secret length descending so longer substrings match before shorter ones,
        // with deterministic secondary sorting by variable key to break ties identically across JVM runs.
        sortedEntries.sort((a, b) -> {
            final int cmp = Integer.compare(b.getValue().length(), a.getValue().length());
            return cmp != 0 ? cmp : a.getKey().compareTo(b.getKey());
        });

        final Map<String, String> secretToToken = new HashMap<>();
        final List<String> distinctSecrets = new ArrayList<>();
        for (final Map.Entry<String, String> entry : sortedEntries)
        {
            final String secret = entry.getValue();
            if (!secretToToken.containsKey(secret))
            {
                secretToToken.put(secret, "[MASKED_VAR_" + entry.getKey() + "]");
                distinctSecrets.add(secret);
            }
        }

        final StringBuilder patternBuilder = new StringBuilder();
        patternBuilder.append('(').append(MASKED_TOKEN_PATTERN.pattern()).append(")|(");
        for (int i = 0; i < distinctSecrets.size(); i++)
        {
            if (i > 0)
            {
                patternBuilder.append('|');
            }
            patternBuilder.append(Pattern.quote(distinctSecrets.get(i)));
        }
        patternBuilder.append(')');

        final Pattern pattern = Pattern.compile(patternBuilder.toString());
        return new MaskingContext(pattern, secretToToken);
    }

    /**
     * Sanitizes sensitive values within text or structured JSON content.
     * If the content is valid JSON (object or array), values inside JSON string literals are masked
     * while preserving JSON syntax without corrupting boolean, numeric, or structural literals.
     * Non-JSON or malformed content falls back to pattern-based text masking.
     *
     * @param content the raw string or JSON payload
     * @param sensitiveMap map of variable names to secret values
     * @return masked content with valid JSON preserved where applicable
     */
    public static String maskJsonOrTextContent(final String content, final Map<String, String> sensitiveMap)
    {
        if (content == null || content.isBlank() || sensitiveMap == null || sensitiveMap.isEmpty())
        {
            return content;
        }

        final MaskingContext maskingContext = createMaskingContext(sensitiveMap);
        return maskJsonOrTextContent(content, maskingContext);
    }

    private static String maskJsonOrTextContent(final String content, final MaskingContext maskingContext)
    {
        if (content == null || content.isBlank() || maskingContext == null)
        {
            return content;
        }

        int sepIndex = content.lastIndexOf("\n\n");
        while (sepIndex != -1)
        {
            final String leadingPart = content.substring(0, sepIndex);
            final String trimmedLeading = leadingPart.trim();
            if ((trimmedLeading.startsWith("{") && trimmedLeading.endsWith("}")) || (trimmedLeading.startsWith("[") && trimmedLeading.endsWith("]")))
            {
                final String maskedLeading = maskJsonString(leadingPart, maskingContext);
                if (maskedLeading != null)
                {
                    final String trailingPart = content.substring(sepIndex + 2);
                    return maskedLeading + "\n\n" + maskingContext.mask(trailingPart);
                }
            }
            sepIndex = content.lastIndexOf("\n\n", sepIndex - 1);
        }

        final String trimmed = content.trim();
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]")))
        {
            final String masked = maskJsonString(content, maskingContext);
            if (masked != null)
            {
                return masked;
            }
        }

        return maskingContext.mask(content);
    }

    private static String maskJsonString(final String jsonContent, final MaskingContext maskingContext)
    {
        try
        {
            // In-place AST mutation is safe here because readTree produces a freshly parsed,
            // locally scoped JsonNode tree, avoiding an unnecessary deepCopy allocation.
            final JsonNode tree = MAPPER.readTree(jsonContent);
            if (tree.isObject())
            {
                final ObjectNode obj = (ObjectNode) tree;
                final boolean modified = maskObjectNode(obj, maskingContext);
                return modified ? MAPPER.writeValueAsString(obj) : jsonContent;
            }
            else if (tree.isArray())
            {
                final ArrayNode arr = (ArrayNode) tree;
                final boolean modified = maskArrayNode(arr, maskingContext);
                return modified ? MAPPER.writeValueAsString(arr) : jsonContent;
            }
        }
        catch (final Exception ignored)
        {
            // Fall back to text masking if JSON parsing or serialization fails
        }
        return null;
    }

    private static boolean maskObjectNode(final ObjectNode objectNode, final MaskingContext context)
    {
        boolean changed = false;
        final List<String> fieldNames = new ArrayList<>();
        final Iterator<String> it = objectNode.fieldNames();
        while (it.hasNext())
        {
            fieldNames.add(it.next());
        }

        for (final String key : fieldNames)
        {
            final JsonNode child = objectNode.get(key);
            if (child.isTextual())
            {
                final String original = child.asText();
                final String masked = context.mask(original);
                if (!masked.equals(original))
                {
                    objectNode.put(key, masked);
                    changed = true;
                }
            }
            else if (child.isObject())
            {
                if (maskObjectNode((ObjectNode) child, context))
                {
                    changed = true;
                }
            }
            else if (child.isArray())
            {
                if (maskArrayNode((ArrayNode) child, context))
                {
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static boolean maskArrayNode(final ArrayNode arrayNode, final MaskingContext context)
    {
        boolean changed = false;
        for (int i = 0; i < arrayNode.size(); i++)
        {
            final JsonNode element = arrayNode.get(i);
            if (element.isTextual())
            {
                final String original = element.asText();
                final String masked = context.mask(original);
                if (!masked.equals(original))
                {
                    arrayNode.set(i, arrayNode.textNode(masked));
                    changed = true;
                }
            }
            else if (element.isObject())
            {
                if (maskObjectNode((ObjectNode) element, context))
                {
                    changed = true;
                }
            }
            else if (element.isArray())
            {
                if (maskArrayNode((ArrayNode) element, context))
                {
                    changed = true;
                }
            }
        }
        return changed;
    }
}
