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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.client.ChatMessage;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.ResponseSchema;
import org.neodymium.ai.client.SutAttachment;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.tool.ToolCall;

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
        final SessionData data = new SessionData();
        data.putDynamic("password", "SecretValue123", true);
        final ExecutionContext ctx = new ExecutionContext(data);
        ExecutionContext.setActiveContext(ctx);
        try
        {
            final String rawText = "Secret payload: SecretValue123";
            final String encodedRawText = Base64.getEncoder().encodeToString(rawText.getBytes(StandardCharsets.UTF_8));
            final SutAttachment textAtt = new SutAttachment("text/plain", "log.txt", encodedRawText);

            final LlmRequest request = new LlmRequest("sys", "Original raw message: SecretValue123", List.of(textAtt), null, 0.0, 30);
            final ContextSanitizer sanitizer = new DefaultContextSanitizer();
            final SanitizedPayload payload = sanitizer.sanitize(request.userMessage(), null, data);

            final LlmRequest sanitized = LlmSanitizerHelper.toSanitizedRequest(request, payload);

            assertNotNull(sanitized);
            assertEquals("Original raw message: [MASKED_VAR_password]", sanitized.userMessage());
            assertEquals(1, sanitized.attachments().size());

            final SutAttachment sanitizedAtt = sanitized.attachments().get(0);
            final String decodedAtt = new String(Base64.getDecoder().decode(sanitizedAtt.base64Data()), StandardCharsets.UTF_8);
            assertEquals("Secret payload: [MASKED_VAR_password]", decodedAtt);
        }
        finally
        {
            ExecutionContext.setActiveContext(null);
        }
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

    @Test
    public void testMaskSensitiveValuesPreventsRecursiveSelfMaskingWhenSecretIsSubstringOfKey()
    {
        final Map<String, String> sensitiveMap = Map.of("user.password", "password");
        final String inputWithExistingToken = "The token is [MASKED_VAR_user.password] and plain password.";

        final String masked = LlmSanitizerHelper.maskSensitiveValues(inputWithExistingToken, sensitiveMap);

        assertEquals("The token is [MASKED_VAR_user.password] and plain [MASKED_VAR_user.password].", masked);
        assertFalse(masked.contains("[MASKED_VAR_user.[MASKED_VAR_"), "Existing token must not be recursively masked");
    }

    @Test
    public void testMaskSensitiveValuesReplacesLongerSecretBeforeShorterSubstring()
    {
        // Unordered map where short secret could precede long secret
        final Map<String, String> sensitiveMap = Map.of(
            "shortSecret", "secret",
            "longSecret", "secret123"
        );
        final String input = "Sensitive payload: secret123";

        final String masked = LlmSanitizerHelper.maskSensitiveValues(input, sensitiveMap);

        assertEquals("Sensitive payload: [MASKED_VAR_longSecret]", masked);
        assertFalse(masked.contains("[MASKED_VAR_shortSecret]123"), "Long secret must not be partially masked by short substring");
    }

    @Test
    public void testMaskSensitiveValuesReturnsOriginalWhenNullOrEmpty()
    {
        assertNull(LlmSanitizerHelper.maskSensitiveValues(null, Map.of("k", "v")));
        assertEquals("", LlmSanitizerHelper.maskSensitiveValues("", Map.of("k", "v")));
        assertEquals("content", LlmSanitizerHelper.maskSensitiveValues("content", null));
        assertEquals("content", LlmSanitizerHelper.maskSensitiveValues("content", Map.of()));
    }

    @Test
    public void testMaskSensitiveValuesPreventsTokenCorruptionWhenSecretIsSubstringOfAnotherKey()
    {
        final Map<String, String> sensitiveMap = Map.of(
            "user.password", "SecretP@ss123",
            "user", "demoUser"
        );
        final String input = "Login demoUser with password SecretP@ss123 now.";

        final String masked = LlmSanitizerHelper.maskSensitiveValues(input, sensitiveMap);

        assertEquals("Login [MASKED_VAR_user] with password [MASKED_VAR_user.password] now.", masked);
        assertFalse(masked.contains("[MASKED_VAR_[MASKED_VAR_"), "Newly generated token must not be corrupted by subsequent secret");
    }

    @Test
    public void testToSanitizedRequestSanitizesToolCallArgumentsInConversation()
    {
        final ObjectMapper mapper = new ObjectMapper();
        final SessionData sessionData = new SessionData();
        sessionData.putDynamic("db.password", "superSecretPass", true);

        final ExecutionContext ctx = new ExecutionContext(sessionData);
        ExecutionContext.setActiveContext(ctx);
        try
        {
            final ObjectNode args = mapper.createObjectNode();
            args.put("query", "CONNECT superSecretPass");
            final ToolCall tc = new ToolCall("call1", "db_exec", args);

            final ChatMessage assistantMsg = ChatMessage.assistant("Executing query", List.of(tc));
            final LlmRequest rawRequest = new LlmRequest(
                List.of(assistantMsg),
                List.of(),
                List.of(),
                ResponseSchema.TEXT,
                0.0,
                30,
                null
            );
            final SanitizedPayload payload = new SanitizedPayload("clean user", null, Map.of());

            final LlmRequest sanitizedRequest = LlmSanitizerHelper.toSanitizedRequest(rawRequest, payload);

            final ChatMessage sanitizedMsg = sanitizedRequest.messages().get(0);
            assertNotNull(sanitizedMsg.toolCalls());
            assertEquals(1, sanitizedMsg.toolCalls().size());
            final ToolCall sanitizedCall = sanitizedMsg.toolCalls().get(0);
            assertEquals("CONNECT [MASKED_VAR_db.password]", sanitizedCall.arguments().path("query").asText());
        }
        finally
        {
            ExecutionContext.setActiveContext(null);
        }
    }

    @Test
    public void testMaskSensitiveValuesWithSingleDigitAndPunctuationSecrets()
    {
        final Map<String, String> sensitiveMap = Map.of(
            "digitZero", "0",
            "digitOne", "1",
            "semicolon", ";",
            "subVar", "VAR_"
        );
        final String input = "Code: 01; [MASKED_VAR_existing]; VAR_end";

        final String masked = LlmSanitizerHelper.maskSensitiveValues(input, sensitiveMap);

        assertEquals("Code: [MASKED_VAR_digitZero][MASKED_VAR_digitOne][MASKED_VAR_semicolon] [MASKED_VAR_existing][MASKED_VAR_semicolon] [MASKED_VAR_subVar]end", masked);
        assertFalse(masked.contains("[MASKED_VAR_[MASKED_VAR_"), "Single-digit and substring secrets must not corrupt existing or newly generated mask tokens");
    }

    @Test
    public void testMaskSensitiveValuesWithDuplicateSecretValuesAcrossKeys()
    {
        final Map<String, String> sensitiveMap = Map.of(
            "user.password", "commonSecret",
            "confirm.password", "commonSecret"
        );
        final String input = "Values are commonSecret and commonSecret.";

        final String masked = LlmSanitizerHelper.maskSensitiveValues(input, sensitiveMap);

        // Deterministic secondary sorting selects alphabetically earliest key "confirm.password"
        assertEquals("Values are [MASKED_VAR_confirm.password] and [MASKED_VAR_confirm.password].", masked);
        assertFalse(masked.contains("commonSecret"));
    }

    @Test
    public void testMaskSensitiveValuesDeterministicTieBreaking()
    {
        final Map<String, String> sensitiveMap = Map.of(
            "z_key", "sameVal",
            "a_key", "sameVal"
        );
        final String input = "Value: sameVal";
        final String masked = LlmSanitizerHelper.maskSensitiveValues(input, sensitiveMap);
        assertEquals("Value: [MASKED_VAR_a_key]", masked,
            "When secrets have identical length, deterministic secondary sorting by key selects alphabetically first key");
    }

    @Test
    public void testToSanitizedRequestHandlesJsonEscapedCharactersInToolCallArguments()
    {
        final ObjectMapper mapper = new ObjectMapper();
        final SessionData sessionData = new SessionData();
        sessionData.putDynamic("auth.token", "tok\"en\\123", true);

        final ExecutionContext ctx = new ExecutionContext(sessionData);
        ExecutionContext.setActiveContext(ctx);
        try
        {
            final ObjectNode args = mapper.createObjectNode();
            args.put("tokenHeader", "Bearer tok\"en\\123");
            final ToolCall tc = new ToolCall("callAuth", "http_request", args);

            final ChatMessage assistantMsg = ChatMessage.assistant("Calling API", List.of(tc));
            final LlmRequest rawRequest = new LlmRequest(
                List.of(assistantMsg),
                List.of(),
                List.of(),
                ResponseSchema.TEXT,
                0.0,
                30,
                null
            );
            final SanitizedPayload payload = new SanitizedPayload("clean prompt", null, Map.of());

            final LlmRequest sanitizedRequest = LlmSanitizerHelper.toSanitizedRequest(rawRequest, payload);

            final ChatMessage sanitizedMsg = sanitizedRequest.messages().get(0);
            assertNotNull(sanitizedMsg.toolCalls());
            final ToolCall sanitizedCall = sanitizedMsg.toolCalls().get(0);
            assertEquals("Bearer [MASKED_VAR_auth.token]", sanitizedCall.arguments().path("tokenHeader").asText());
        }
        finally
        {
            ExecutionContext.setActiveContext(null);
        }
    }

    @Test
    public void testMaskJsonOrTextContentPreservesValidJsonWhenSecretIsNumericOrBoolean() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        final Map<String, String> sensitiveMap = Map.of("pin", "1234", "flag", "true");

        // Valid JSON containing numeric 1234 and boolean true, plus a string "my pin is 1234"
        final String rawJson = "{\"pinCode\":1234,\"active\":true,\"message\":\"User pin is 1234\"}";

        // If raw string regex mask is performed on rawJson:
        // "{\"pinCode\":1234" -> "{\"pinCode\":[MASKED_VAR_pin]" which produces syntax-invalid JSON!
        // maskSensitiveValues raw produces:
        final String rawMasked = LlmSanitizerHelper.maskSensitiveValues(rawJson, sensitiveMap);
        // Assert that raw masking breaks JSON syntax
        Assertions.assertThrows(Exception.class, () -> mapper.readTree(rawMasked),
                "Direct string masking over JSON with numeric/boolean secrets corrupts JSON syntax");

        // The safe helper method maskJsonOrTextContent must preserve valid parseable JSON:
        final String safeMasked = LlmSanitizerHelper.maskJsonOrTextContent(rawJson, sensitiveMap);
        final JsonNode parsed = mapper.readTree(safeMasked);
        Assertions.assertTrue(parsed.isObject());
        Assertions.assertEquals(1234, parsed.path("pinCode").asInt());
        Assertions.assertTrue(parsed.path("active").asBoolean());
        Assertions.assertEquals("User pin is [MASKED_VAR_pin]", parsed.path("message").asText());

        // Plain text content without JSON delimiters should be masked as normal text
        final String plainText = "My pin is 1234 and active status is true.";
        final String maskedText = LlmSanitizerHelper.maskJsonOrTextContent(plainText, sensitiveMap);
        Assertions.assertEquals("My pin is [MASKED_VAR_pin] and active status is [MASKED_VAR_flag].", maskedText);
    }

    @Test
    public void testToSanitizedRequestPreservesValidJsonInMessagesWhenSecretIsNumericOrBoolean() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        final SessionData sessionData = new SessionData();
        sessionData.putDynamic("pin", "1234", true);
        sessionData.putDynamic("flag", "true", true);

        final ExecutionContext ctx = new ExecutionContext(sessionData);
        ExecutionContext.setActiveContext(ctx);
        try
        {
            final String rawJsonToolContent = "{\"pinCode\":1234,\"active\":true,\"message\":\"User pin is 1234\"}";
            final List<ChatMessage> messages = List.of(
                ChatMessage.system("System prompt"),
                ChatMessage.user("Initial user prompt"),
                ChatMessage.assistant("{\"thought\":\"running tool\",\"tool_call\":{\"name\":\"inspect\",\"arguments\":{}}}"),
                ChatMessage.tool("call-1", "inspect", rawJsonToolContent)
            );

            final LlmRequest request = new LlmRequest(messages, List.of(), List.of(), ResponseSchema.TEXT, 0.0, 30, null);
            final SanitizedPayload payload = new SanitizedPayload("Initial user prompt", "State", Map.of());

            final LlmRequest sanitizedRequest = LlmSanitizerHelper.toSanitizedRequest(request, payload);

            Assertions.assertNotNull(sanitizedRequest);
            final ChatMessage sanitizedToolMsg = sanitizedRequest.messages().get(3);
            Assertions.assertEquals(ChatMessage.Role.TOOL, sanitizedToolMsg.role());

            // Must parse as valid JSON without throwing syntax error
            final JsonNode parsed = mapper.readTree(sanitizedToolMsg.content());
            Assertions.assertTrue(parsed.isObject(), "Sanitized tool message content must remain valid JSON object");
            Assertions.assertEquals(1234, parsed.path("pinCode").asInt(), "Numeric literal 1234 must not be corrupted by masking");
            Assertions.assertTrue(parsed.path("active").asBoolean(), "Boolean literal true must not be corrupted by masking");
            Assertions.assertEquals("User pin is [MASKED_VAR_pin]", parsed.path("message").asText(), "String literal must be masked");
        }
        finally
        {
            ExecutionContext.setActiveContext(null);
        }
    }

    @Test
    public void testMaskJsonOrTextContentWithHybridJsonAndDiagnosticAlert() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        final Map<String, String> sensitiveMap = Map.of(
            "pin", "1234",
            "secretToken", "s3cr3t"
        );

        final String hybridContent = "{\"status\":\"ERROR\",\"pinCode\":1234,\"reason\":\"Auth failed\"}\n\n⚠️ Passive Diagnostic Alert: Token s3cr3t was rejected!";
        final String masked = LlmSanitizerHelper.maskJsonOrTextContent(hybridContent, sensitiveMap);

        Assertions.assertTrue(masked.contains("\n\n"), "Hybrid payload must retain delimiter");
        final String[] parts = masked.split("\n\n", 2);
        final JsonNode jsonPart = mapper.readTree(parts[0]);
        Assertions.assertEquals(1234, jsonPart.path("pinCode").asInt(), "Numeric pinCode must remain valid number in JSON portion");
        Assertions.assertEquals("Auth failed", jsonPart.path("reason").asText());

        Assertions.assertEquals("⚠️ Passive Diagnostic Alert: Token [MASKED_VAR_secretToken] was rejected!", parts[1], "Trailing alert must be masked via text replacement");
    }

    @Test
    public void testMaskJsonStringReturnsOriginalStringReferenceWhenNoSecretsModified()
    {
        final Map<String, String> sensitiveMap = Map.of(
            "pin", "1234",
            "password", "secretPassword"
        );
        final String json = "{\"status\":\"SUCCESS\",\"data\":{\"code\":9999,\"message\":\"Operation finished cleanly\"}}";

        final String result = LlmSanitizerHelper.maskJsonOrTextContent(json, sensitiveMap);

        assertSame(json, result, "When no sensitive values are present, the original string reference must be returned without re-serialization");
    }

    @Test
    public void testMaskJsonOrTextContentWithPrettyPrintedJsonAndDiagnosticAlert() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        final Map<String, String> sensitiveMap = Map.of(
            "pin", "1234",
            "token", "myToken"
        );

        final String prettyJsonWithAlert = "{\n  \"status\": \"FAILED\",\n\n  \"pinCode\": 1234,\n  \"note\": \"Check pin 1234\"\n}\n\n⚠️ Passive Diagnostic Alert: Token myToken failed.";
        final String masked = LlmSanitizerHelper.maskJsonOrTextContent(prettyJsonWithAlert, sensitiveMap);

        assertTrue(masked.contains("\n\n⚠️ Passive Diagnostic Alert:"), "Trailing alert delimiter must be preserved");
        final int lastSep = masked.lastIndexOf("\n\n");
        final String jsonPart = masked.substring(0, lastSep);
        final String alertPart = masked.substring(lastSep + 2);

        final JsonNode parsed = mapper.readTree(jsonPart);
        assertEquals(1234, parsed.path("pinCode").asInt(), "Numeric literal 1234 must not be corrupted by masking");
        assertEquals("Check pin [MASKED_VAR_pin]", parsed.path("note").asText(), "String literal must be masked");
        assertEquals("⚠️ Passive Diagnostic Alert: Token [MASKED_VAR_token] failed.", alertPart, "Alert must be masked");
    }
}

