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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.SutAttachment;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;

/**
 * Dedicated unit tests for {@link LlmSanitizerHelper}.
 * Validates secret masking on outbound prompts and attachments, response unmasking,
 * and fail-closed context state handling.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class LlmSanitizerHelperTest
{
    private ExecutionContext previousContext;

    @BeforeEach
    public void setUp()
    {
        this.previousContext = ExecutionContext.getActiveContext();
        ExecutionContext.setActiveContext(null);
    }

    @AfterEach
    public void tearDown()
    {
        ExecutionContext.setActiveContext(this.previousContext);
    }

    @Test
    public void testSanitizeRequestWithNullRequest()
    {
        final SanitizedPayload payload = LlmSanitizerHelper.sanitizeRequest(null);
        assertNotNull(payload);
        assertNull(payload.sanitizedPrompt());
        assertNull(payload.sanitizedStateText());
    }

    @Test
    public void testSanitizeRequestWithoutActiveContextEmitsWarningAndPreservesPrompt()
    {
        final LlmRequest request = new LlmRequest("system", "User message with secret SecretValue123", List.of(), null, 0.0, 30);
        final SanitizedPayload payload = LlmSanitizerHelper.sanitizeRequest(request);

        assertNotNull(payload);
        assertEquals("User message with secret SecretValue123", payload.sanitizedPrompt());
        assertNull(payload.sanitizedStateText());
    }

    @Test
    public void testSanitizeRequestWithActiveContextMasksPromptSecrets()
    {
        final SessionData sessionData = new SessionData();
        sessionData.putDynamic("password", "SecretValue123", true);

        final ExecutionContext context = new ExecutionContext(sessionData);
        ExecutionContext.setActiveContext(context);

        final LlmRequest request = new LlmRequest("system", "Login using SecretValue123 in prompt", List.of(), null, 0.0, 30);
        final SanitizedPayload payload = LlmSanitizerHelper.sanitizeRequest(request);

        assertNotNull(payload);
        assertEquals("Login using [MASKED_VAR_password] in prompt", payload.sanitizedPrompt());
        assertNotNull(payload.maskToVariableMap());
        assertEquals("${password}", payload.maskToVariableMap().get("[MASKED_VAR_password]"));
    }

    @Test
    public void testToSanitizedRequestWithNullsReturnsOriginal()
    {
        final LlmRequest request = new LlmRequest("sys", "prompt", List.of(), null, 0.0, 30);
        assertSame(request, LlmSanitizerHelper.toSanitizedRequest(request, null));
        assertSame(null, LlmSanitizerHelper.toSanitizedRequest(null, new SanitizedPayload("clean", null, Map.of())));
    }

    @Test
    public void testToSanitizedRequestReplacesPromptAndTextAttachments()
    {
        final String rawText = "Secret payload: SecretValue123";
        final String encodedRawText = Base64.getEncoder().encodeToString(rawText.getBytes(StandardCharsets.UTF_8));
        final SutAttachment textAtt = new SutAttachment("text/plain", "log.txt", encodedRawText);

        final LlmRequest request = new LlmRequest("sys", "Original raw message: SecretValue123", List.of(textAtt), null, 0.0, 30);
        final SanitizedPayload payload = new SanitizedPayload(
            "Original raw message: [MASKED_VAR_password]",
            null,
            Map.of("[MASKED_VAR_password]", "SecretValue123")
        );

        final LlmRequest sanitized = LlmSanitizerHelper.toSanitizedRequest(request, payload);

        assertNotNull(sanitized);
        assertEquals("Original raw message: [MASKED_VAR_password]", sanitized.userMessage());
        assertEquals(1, sanitized.attachments().size());

        final SutAttachment sanitizedAtt = sanitized.attachments().get(0);
        final String decodedAtt = new String(Base64.getDecoder().decode(sanitizedAtt.base64Data()), StandardCharsets.UTF_8);
        assertEquals("Secret payload: [MASKED_VAR_password]", decodedAtt);
    }

    @Test
    public void testUnmaskResponseRestoresPlaceholders()
    {
        final LlmResponse response = new LlmResponse("Click element with [MASKED_VAR_password]", null, "mock-model");
        final Map<String, String> maskMap = Map.of("[MASKED_VAR_password]", "${password}");

        final LlmResponse unmasked = LlmSanitizerHelper.unmaskResponse(response, maskMap);

        assertNotNull(unmasked);
        assertEquals("Click element with ${password}", unmasked.content());
    }

    @Test
    public void testUnmaskResponseWithNullsReturnsOriginal()
    {
        final LlmResponse response = new LlmResponse("content", null, "mock-model");
        assertSame(response, LlmSanitizerHelper.unmaskResponse(response, null));
        assertSame(response, LlmSanitizerHelper.unmaskResponse(response, Map.of()));
        assertNull(LlmSanitizerHelper.unmaskResponse(null, Map.of("[A]", "B")));
    }
}
