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

import java.util.List;
import java.util.Map;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.pipeline.ExecutionContext;

/**
 * Helper utility for central outbound LLM request secret masking and response unmasking.
 * Applied centrally at provider dispatch time across all LLM capabilities and call sites.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class LlmSanitizerHelper
{
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
            org.slf4j.LoggerFactory.getLogger(LlmSanitizerHelper.class)
                .warn("⚠️ [Security Warning] Outbound LLM request dispatched without active ExecutionContext bound to thread. Secret masking skipped.");
            return new SanitizedPayload(request.userMessage(), null, Map.of());
        }

        final SutState lastState = (SutState) ctx.getTransientData().get(ExecutionContext.KEY_LAST_STATE);
        final ContextSanitizer sanitizer = new DefaultContextSanitizer();
        return sanitizer.sanitize(request.userMessage(), lastState, ctx.getSessionData());
    }

    /**
     * Creates a new LlmRequest replacing the user prompt with the sanitized prompt text.
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

        List<org.neodymium.ai.client.SutAttachment> sanitizedAttachments = original.attachments();
        if (sanitizedAttachments != null && !sanitizedAttachments.isEmpty() && payload.maskToVariableMap() != null && !payload.maskToVariableMap().isEmpty())
        {
            final List<org.neodymium.ai.client.SutAttachment> updated = new java.util.ArrayList<>();
            for (final org.neodymium.ai.client.SutAttachment att : sanitizedAttachments)
            {
                if (att.base64Data() != null && att.mediaType() != null && !att.mediaType().startsWith("image/"))
                {
                    try
                    {
                        String decoded = new String(java.util.Base64.getDecoder().decode(att.base64Data()), java.nio.charset.StandardCharsets.UTF_8);
                        for (final Map.Entry<String, String> entry : payload.maskToVariableMap().entrySet())
                        {
                            decoded = decoded.replace(entry.getValue(), entry.getKey());
                        }
                        final String encoded = java.util.Base64.getEncoder().encodeToString(decoded.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        updated.add(new org.neodymium.ai.client.SutAttachment(att.filePath(), encoded, att.mediaType()));
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

        return new LlmRequest(
            original.systemMessage(),
            payload.sanitizedPrompt(),
            sanitizedAttachments,
            original.responseSchema(),
            original.temperature(),
            original.timeoutSeconds()
        );
    }

    /**
     * Unmasks returned LLM response content by mapping format-preserving placeholders back to variable references.
     *
     * @param response the raw LLM response
     * @param maskMap the reverse variable map ([MASKED_VAR_key] -> ${key})
     * @return the unmasked LlmResponse
     */
    public static LlmResponse unmaskResponse(final LlmResponse response, final Map<String, String> maskMap)
    {
        if (response == null || response.content() == null || maskMap == null || maskMap.isEmpty())
        {
            return response;
        }

        String content = response.content();
        for (final Map.Entry<String, String> entry : maskMap.entrySet())
        {
            content = content.replace(entry.getKey(), entry.getValue());
        }

        return new LlmResponse(content, response.tokenUsage(), response.modelName());
    }
}
