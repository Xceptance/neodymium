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
package org.neodymium.ai.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.model.UnresolvableVariableException;

/**
 * Dedicated unit tests for {@link ToolCallVariableResolver}.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class ToolCallVariableResolverTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void testResolveVariablesReturnsOriginalWhenNullInputs()
    {
        final ToolCall call = new ToolCall("call1", "fill", MAPPER.createObjectNode());
        Assertions.assertNull(ToolCallVariableResolver.resolveVariables(null, new SessionData()));
        Assertions.assertSame(call, ToolCallVariableResolver.resolveVariables(call, null));
    }

    @Test
    public void testResolveVariablesReplacesTextPlaceholders()
    {
        final SessionData sessionData = new SessionData();
        sessionData.putDynamic("user.password", "secretP@ss123", true);
        sessionData.putDynamic("user.name", "john_doe", false);

        final ObjectNode args = MAPPER.createObjectNode();
        args.put("selector", "#password");
        args.put("text", "${user.password}");
        args.put("extra", "User is ${user.name}");

        final ToolCall call = new ToolCall("call123", "fill", args);
        final ToolCall resolved = ToolCallVariableResolver.resolveVariables(call, sessionData);

        Assertions.assertNotSame(call, resolved);
        Assertions.assertEquals("secretP@ss123", resolved.arguments().path("text").asText());
        Assertions.assertEquals("User is john_doe", resolved.arguments().path("extra").asText());
        Assertions.assertEquals("#password", resolved.arguments().path("selector").asText());
    }

    @Test
    public void testResolveVariablesHandlesNestedObjectsAndArrays()
    {
        final SessionData sessionData = new SessionData();
        sessionData.putDynamic("token", "xyz-789", true);

        final ObjectNode args = MAPPER.createObjectNode();
        final ObjectNode nested = args.putObject("credentials");
        nested.put("auth", "Bearer ${token}");

        final ArrayNode array = args.putArray("items");
        array.add("${token}");
        final ObjectNode arrayObj = array.addObject();
        arrayObj.put("sub", "${token}");

        final ToolCall call = new ToolCall("call1", "custom", args);
        final ToolCall resolved = ToolCallVariableResolver.resolveVariables(call, sessionData);

        Assertions.assertEquals("Bearer xyz-789", resolved.arguments().path("credentials").path("auth").asText());
        Assertions.assertEquals("xyz-789", resolved.arguments().path("items").get(0).asText());
        Assertions.assertEquals("xyz-789", resolved.arguments().path("items").get(1).path("sub").asText());
    }

    @Test
    public void testResolveVariablesPreservesStoreToolTargetVariable()
    {
        final SessionData sessionData = new SessionData();
        sessionData.putDynamic("extractedValue", "realVal", false);

        final ObjectNode args = MAPPER.createObjectNode();
        args.put("variableName", "extractedValue");
        args.put("value", "ConstantText");

        final ToolCall call = new ToolCall("callStore", "store", args);
        final ToolCall resolved = ToolCallVariableResolver.resolveVariables(call, sessionData);

        // variableName should NOT be resolved even if a variable with that name exists in SessionData
        Assertions.assertEquals("extractedValue", resolved.arguments().path("variableName").asText());
    }

    @Test
    public void testResolveVariablesFastPathReturnsSameCallWhenNoPlaceholderPresent()
    {
        final SessionData sessionData = new SessionData();
        sessionData.putDynamic("var", "val", false);

        final ObjectNode args = MAPPER.createObjectNode();
        args.put("selector", "#btn");
        args.put("text", "plain text without placeholders");

        final ToolCall call = new ToolCall("callFast", "click", args);
        final ToolCall resolved = ToolCallVariableResolver.resolveVariables(call, sessionData);

        Assertions.assertSame(call, resolved);
    }

    @Test
    public void testResolveVariablesPreservesBrowserStoreAliasTargetVariable()
    {
        final SessionData sessionData = new SessionData();
        sessionData.putDynamic("orderId", "12345", false);

        final ObjectNode args = MAPPER.createObjectNode();
        args.put("variableName", "orderId");
        args.put("value", "ConstantText");

        final ToolCall call = new ToolCall("callBrowserStore", "browser_store", args);
        final ToolCall resolved = ToolCallVariableResolver.resolveVariables(call, sessionData);

        Assertions.assertEquals("orderId", resolved.arguments().path("variableName").asText());
    }

    @Test
    public void testResolveVariablesSkipsUnresolvableCompositeSelfReferentialStoreValue()
    {
        final SessionData sessionData = new SessionData();

        final ObjectNode args = MAPPER.createObjectNode();
        args.put("variableName", "tempKey");
        args.put("value", "prefix_${tempKey}_suffix");

        final ToolCall call = new ToolCall("callStoreComposite", "store", args);
        final ToolCall resolved = ToolCallVariableResolver.resolveVariables(call, sessionData);

        Assertions.assertEquals("tempKey", resolved.arguments().path("variableName").asText());
        Assertions.assertEquals("prefix_${tempKey}_suffix", resolved.arguments().path("value").asText());
    }

    @Test
    public void testResolveVariablesResolvesSiblingWhilePreservingUnresolvableSelfReferentialStoreValue()
    {
        final SessionData sessionData = new SessionData();
        sessionData.putDynamic("validVar", "resolvedVal", false);

        final ObjectNode args = MAPPER.createObjectNode();
        args.put("variableName", "tempKey");
        args.put("value", "prefix_${validVar}_${tempKey}_suffix");

        final ToolCall call = new ToolCall("callStoreCompositeSibling", "store", args);
        final ToolCall resolved = ToolCallVariableResolver.resolveVariables(call, sessionData);

        Assertions.assertEquals("tempKey", resolved.arguments().path("variableName").asText());
        Assertions.assertEquals("prefix_resolvedVal_${tempKey}_suffix", resolved.arguments().path("value").asText());
    }

    @Test
    public void testResolveVariablesStrictlyFailsOnMissingSiblingInCompositeStoreValue()
    {
        final SessionData sessionData = new SessionData();

        final ObjectNode args = MAPPER.createObjectNode();
        args.put("variableName", "tempKey");
        args.put("value", "prefix_${tempKey}_${missingVar}_suffix");

        final ToolCall call = new ToolCall("callStoreCompositeMissingSibling", "store", args);

        final UnresolvableVariableException ex = Assertions.assertThrows(
                UnresolvableVariableException.class,
                () -> ToolCallVariableResolver.resolveVariables(call, sessionData)
        );
        Assertions.assertEquals("missingVar", ex.getVariableName());
    }

    @Test
    public void testResolveVariablesNormalizesAndResolvesUnbracketedVariables()
    {
        final SessionData sessionData = new SessionData();
        sessionData.putDynamic("password", "SecretP@ss123!", true);
        sessionData.putDynamic("user.email", "neo@test.de", false);

        final ObjectNode args = MAPPER.createObjectNode();
        args.put("selector", "#password");
        args.put("text", "$password");
        args.put("email", "$user.email");

        final ToolCall call = new ToolCall("callUnbracketed", "fill", args);
        final ToolCall resolved = ToolCallVariableResolver.resolveVariables(call, sessionData);

        Assertions.assertNotSame(call, resolved);
        Assertions.assertEquals("SecretP@ss123!", resolved.arguments().path("text").asText());
        Assertions.assertEquals("neo@test.de", resolved.arguments().path("email").asText());
        Assertions.assertEquals("#password", resolved.arguments().path("selector").asText());
    }

    @Test
    public void testResolveVariablesPreservesMonetaryAndCurrencyAmounts()
    {
        final SessionData sessionData = new SessionData();
        sessionData.putDynamic("password", "SecretP@ss123!", true);

        final ObjectNode args = MAPPER.createObjectNode();
        args.put("price", "$100");
        args.put("decimal", "$19.99");
        args.put("code", "$25OFF");

        final ToolCall call = new ToolCall("callCurrency", "fill", args);
        final ToolCall resolved = ToolCallVariableResolver.resolveVariables(call, sessionData);

        Assertions.assertEquals("$100", resolved.arguments().path("price").asText());
        Assertions.assertEquals("$19.99", resolved.arguments().path("decimal").asText());
        Assertions.assertEquals("$25OFF", resolved.arguments().path("code").asText());
    }

    @Test
    public void testResolveVariablesPreservesUnknownDollarIdentifiers()
    {
        final SessionData sessionData = new SessionData();
        sessionData.putDynamic("password", "SecretP@ss123!", true);

        final ObjectNode args = MAPPER.createObjectNode();
        args.put("text", "$unknownVar");
        args.put("selector", "$('#btn')");

        final ToolCall call = new ToolCall("callUnknown", "click", args);
        final ToolCall resolved = ToolCallVariableResolver.resolveVariables(call, sessionData);

        Assertions.assertEquals("$unknownVar", resolved.arguments().path("text").asText());
        Assertions.assertEquals("$('#btn')", resolved.arguments().path("selector").asText());
    }

    @Test
    public void testResolveVariablesResolvesUnbracketedWithTrailingPunctuation()
    {
        final SessionData sessionData = new SessionData();
        sessionData.putDynamic("password", "SecretP@ss123!", true);

        final ObjectNode args = MAPPER.createObjectNode();
        args.put("text", "Your password is $password.");

        final ToolCall call = new ToolCall("callPunctuation", "fill", args);
        final ToolCall resolved = ToolCallVariableResolver.resolveVariables(call, sessionData);

        Assertions.assertEquals("Your password is SecretP@ss123!.", resolved.arguments().path("text").asText());
    }

    @Test
    public void testNormalizePlaceholdersNormalizesUnbracketedVariables()
    {
        final SessionData sessionData = new SessionData();
        sessionData.putDynamic("password", "SecretP@ss123!", true);
        sessionData.putDynamic("email", "neo@test.de", false);

        final ObjectNode args = MAPPER.createObjectNode();
        args.put("text", "$password");
        args.put("login", "User: $email");

        final ToolCall call = new ToolCall("callNorm", "fill", args);
        final ToolCall normalized = ToolCallVariableResolver.normalizePlaceholders(call, sessionData);

        Assertions.assertNotSame(call, normalized);
        Assertions.assertEquals("${password}", normalized.arguments().path("text").asText());
        Assertions.assertEquals("User: ${email}", normalized.arguments().path("login").asText());
    }

    @Test
    public void testNormalizePlaceholdersHandlesPrefixCollisionCleanly()
    {
        final SessionData sessionData = new SessionData();
        sessionData.putDynamic("user", "john", false);
        sessionData.putDynamic("user.name", "john_doe", false);

        final ObjectNode args = MAPPER.createObjectNode();
        args.put("full", "$user.name");
        args.put("partial", "$user.other");

        final ToolCall call = new ToolCall("callCollision", "fill", args);
        final ToolCall normalized = ToolCallVariableResolver.normalizePlaceholders(call, sessionData);

        Assertions.assertEquals("${user.name}", normalized.arguments().path("full").asText());
        Assertions.assertEquals("${user}.other", normalized.arguments().path("partial").asText());
    }

    @Test
    public void testResolveVariablesNormalizesAndResolvesInFillFormStructure()
    {
        final SessionData sessionData = new SessionData();
        sessionData.putDynamic("user.email", "test@varmail.de", false);
        sessionData.putDynamic("password", "Secret123!", true);

        final ObjectNode args = MAPPER.createObjectNode();
        final ArrayNode fields = args.putArray("fields");
        final ObjectNode f1 = fields.addObject();
        f1.put("selector", "#email");
        f1.put("value", "$user.email");
        final ObjectNode f2 = fields.addObject();
        f2.put("selector", "#password");
        f2.put("value", "$password");

        final ToolCall call = new ToolCall("callForm", "fill_form", args);
        final ToolCall resolved = ToolCallVariableResolver.resolveVariables(call, sessionData);

        Assertions.assertEquals("test@varmail.de", resolved.arguments().path("fields").get(0).path("value").asText());
        Assertions.assertEquals("Secret123!", resolved.arguments().path("fields").get(1).path("value").asText());

        final ToolCall normalized = ToolCallVariableResolver.normalizePlaceholders(call, sessionData);
        Assertions.assertEquals("${user.email}", normalized.arguments().path("fields").get(0).path("value").asText());
        Assertions.assertEquals("${password}", normalized.arguments().path("fields").get(1).path("value").asText());
    }

    @Test
    public void testNormalizeAndResolvePreservesJavaScriptVariablesInScriptTool()
    {
        final SessionData sessionData = new SessionData();
        sessionData.putDynamic("btn", "button#submit", false);
        sessionData.putDynamic("token", "secretToken", true);

        final ObjectNode args = MAPPER.createObjectNode();
        args.put("script", "var $btn = document.querySelector('button'); return $btn.textContent + ' ' + '${token}';");

        final ToolCall call = new ToolCall("callJs", "execute_script", args);
        final ToolCall normalized = ToolCallVariableResolver.normalizePlaceholders(call, sessionData);
        Assertions.assertEquals("var $btn = document.querySelector('button'); return $btn.textContent + ' ' + '${token}';", normalized.arguments().path("script").asText());

        final ToolCall resolved = ToolCallVariableResolver.resolveVariables(call, sessionData);
        Assertions.assertEquals("var $btn = document.querySelector('button'); return $btn.textContent + ' ' + 'secretToken';", resolved.arguments().path("script").asText());
    }
}
