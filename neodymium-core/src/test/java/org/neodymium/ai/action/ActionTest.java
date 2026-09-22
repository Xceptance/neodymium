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
package org.neodymium.ai.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.tool.ToolCall;

/**
 * Unit tests for {@link Action} model serialization, getters, and pattern properties.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class ActionTest
{
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testIsRegexDefaultAndSetter()
    {
        final Action action = new Action("ASSERT", "#order", "Check order");
        assertFalse(action.isRegex());

        final Action regexAction = action.withIsRegex(true);
        assertTrue(regexAction.isRegex());
        assertEquals("ASSERT", regexAction.getType());
        assertEquals("#order", regexAction.getTarget());
    }

    @Test
    public void testIsRegexJsonDeserialization() throws Exception
    {
        final String json = """
            {
              "type": "ASSERT",
              "target": "#checkout-form-container",
              "value": "V-[0-9]+-US",
              "isRegex": true,
              "reasoning": "Matching 7-digit dynamic order number pattern"
            }
            """;

        final Action action = this.mapper.readValue(json, Action.class);
        assertEquals("ASSERT", action.getType());
        assertEquals("#checkout-form-container", action.getTarget());
        assertEquals("V-[0-9]+-US", action.getValue());
        assertTrue(action.isRegex());
    }

    @Test
    public void testIsRegexFullRoundTripSerialization() throws Exception
    {
        final Action original = new Action("ASSERT", "#order-summary", List.of("V-[0-9]+-US"),
                "Verify order pattern", "Regex assertion on order number", true);

        final String serializedJson = this.mapper.writeValueAsString(original);
        // Verify that the serialized JSON contains the exact key "isRegex": true (not "regex": true)
        assertTrue(serializedJson.contains("\"isRegex\":true") || serializedJson.contains("\"isRegex\" : true"),
                "Serialized JSON must use 'isRegex' property name, but was: " + serializedJson);
        assertFalse(serializedJson.contains("\"regex\":true") || serializedJson.contains("\"regex\" : true"),
                "Serialized JSON should not contain legacy/JavaBean 'regex' property name: " + serializedJson);

        final Action deserialized = this.mapper.readValue(serializedJson, Action.class);
        assertEquals("ASSERT", deserialized.getType());
        assertEquals("#order-summary", deserialized.getTarget());
        assertEquals("V-[0-9]+-US", deserialized.getValue());
        assertTrue(deserialized.isRegex());
    }

    @Test
    public void testIsRegexAliasCompatibility() throws Exception
    {
        // Support legacy or alternative JSON payloads containing "regex": true
        final String legacyJson = """
            {
              "type": "ASSERT",
              "target": "[data-ai='xcuulzml']",
              "value": "V-[0-9]+-US",
              "regex": true,
              "reasoning": "Legacy property key"
            }
            """;

        final Action action = this.mapper.readValue(legacyJson, Action.class);
        assertEquals("ASSERT", action.getType());
        assertEquals("[data-ai='xcuulzml']", action.getTarget());
        assertEquals("V-[0-9]+-US", action.getValue());
        assertTrue(action.isRegex(), "Action should recognize 'regex' as an alias for 'isRegex'");
    }

    @Test
    public void testDurationAndDelayFields() throws Exception
    {
        final Action action = new Action("CLICK", "#submit", "Click submit");
        action.setDurationMs(250L);
        action.setDelayMs(500L);

        assertEquals(250L, action.getDurationMs());
        assertEquals(500L, action.getDelayMs());

        final Action copied = action.withTarget("#submit-btn");
        assertEquals("#submit-btn", copied.getTarget());
        assertEquals(250L, copied.getDurationMs());
        assertEquals(500L, copied.getDelayMs());

        final String json = this.mapper.writeValueAsString(action);
        assertTrue(json.contains("\"durationMs\":250") || json.contains("\"durationMs\" : 250"));
        assertTrue(json.contains("\"delayMs\":500") || json.contains("\"delayMs\" : 500"));

        final Action deserialized = this.mapper.readValue(json, Action.class);
        assertEquals(250L, deserialized.getDurationMs());
        assertEquals(500L, deserialized.getDelayMs());
    }

    @Test
    public void testCandidateLocatorsResolution()
    {
        final Action action = new Action("CLICK", "#primary-btn", "Click primary");
        assertEquals("#primary-btn", action.getBestCandidateLocator());
        assertEquals(List.of("#primary-btn"), action.getAllCandidateLocators());

        action.setCandidateLocators(List.of(
            new LocatorCandidate("button.fallback-btn", "CSS", 0.6, "Class selector fallback"),
            new LocatorCandidate("button[data-testid='order-submit']", "TEST_ID", 0.95, "Gold standard test ID"),
            new LocatorCandidate("button.low-score", "CSS", 0.3, "Low score fallback")
        ));

        // Best candidate locator should resolve to the highest score
        assertEquals("button[data-testid='order-submit']", action.getBestCandidateLocator());

        // All candidate locators should start with primary target, followed by sorted candidates
        final List<String> all = action.getAllCandidateLocators();
        assertEquals(4, all.size());
        assertEquals("#primary-btn", all.get(0));
        assertEquals("button[data-testid='order-submit']", all.get(1));
        assertEquals("button.fallback-btn", all.get(2));
        assertEquals("button.low-score", all.get(3));
    }

    @Test
    public void testFromToolCallBrowserClick()
    {
        final ObjectNode args = this.mapper.createObjectNode();
        args.put("selector", "[data-ai='xcz0f8a5']");
        final ToolCall call = new ToolCall("call-1", "browser_click", args);

        final Action action = Action.fromToolCall(call);
        assertEquals("CLICK", action.getType());
        assertEquals("[data-ai='xcz0f8a5']", action.getTarget());
        assertEquals("Click [data-ai='xcz0f8a5']", action.getDescription());
    }

    @Test
    public void testFromToolCallBrowserType()
    {
        final ObjectNode args = this.mapper.createObjectNode();
        args.put("selector", "#couponCode");
        args.put("text", "10p-off");
        final ToolCall call = new ToolCall("call-2", "browser_type", args);

        final Action action = Action.fromToolCall(call);
        assertEquals("TYPE", action.getType());
        assertEquals("#couponCode", action.getTarget());
        assertEquals("10p-off", action.getValue());
        assertEquals("Type '10p-off' into #couponCode", action.getDescription());
    }

    @Test
    public void testFromToolCallBrowserNavigate()
    {
        final ObjectNode args = this.mapper.createObjectNode();
        args.put("url", "https://localhost:8543/verla-normal/index.html");
        final ToolCall call = new ToolCall("call-3", "browser_navigate", args);

        final Action action = Action.fromToolCall(call);
        assertEquals("NAVIGATE", action.getType());
        assertEquals("https://localhost:8543/verla-normal/index.html", action.getTarget());
        assertEquals("Navigate to https://localhost:8543/verla-normal/index.html", action.getDescription());
    }

    @Test
    public void testFromToolCallBrowserAssertText()
    {
        final ObjectNode args = this.mapper.createObjectNode();
        args.put("selector", ".order-summary");
        args.put("expectedText", "Discount (10P-OFF)");
        final ToolCall call = new ToolCall("call-4", "browser_assert_text", args);

        final Action action = Action.fromToolCall(call);
        assertEquals("ASSERT_TEXT", action.getType());
        assertEquals(".order-summary", action.getTarget());
        assertEquals("Discount (10P-OFF)", action.getValue());
        assertEquals("Assert text 'Discount (10P-OFF)' on .order-summary", action.getDescription());
    }

    @Test
    public void testFromToolCallCoordinateClick()
    {
        final ObjectNode args = this.mapper.createObjectNode();
        args.put("x", 150);
        args.put("y", 300);
        final ToolCall call = new ToolCall("call-5", "browser_click", args);

        final Action action = Action.fromToolCall(call);
        assertEquals("CLICK", action.getType());
        assertEquals("coord: 150,300", action.getTarget());
    }

    @Test
    public void testDeserializationWithAliases() throws Exception
    {
        final String json = """
            {
              "type": "CLICK",
              "selector": "#submit-order",
              "thought": "Submit customer order"
            }
            """;

        final Action action = this.mapper.readValue(json, Action.class);
        assertEquals("CLICK", action.getType());
        assertEquals("#submit-order", action.getTarget());
        assertEquals("Submit customer order", action.getReasoning());
    }

    @Test
    public void testFromToolCallBrowserAssertCount()
    {
        final ObjectNode args = this.mapper.createObjectNode();
        args.put("selector", ".search-suggestion-item");
        args.put("minCount", 6);
        final ToolCall call = new ToolCall("call-cnt", "browser_assert_count", args);

        final Action action = Action.fromToolCall(call);
        assertEquals("ASSERT_COUNT", action.getType());
        assertEquals(".search-suggestion-item", action.getTarget());
        assertEquals(">=6", action.getValue());
    }

    @Test
    public void testFromToolCallCleanAssertCount()
    {
        final ObjectNode args = this.mapper.createObjectNode();
        args.put("selector", ".search-suggestion-item");
        args.put("count", 6);
        args.put("operator", "MIN");
        final ToolCall call = new ToolCall("call-cnt-clean", "assert_count", args);

        final Action action = Action.fromToolCall(call);
        assertEquals("ASSERT_COUNT", action.getType());
        assertEquals(".search-suggestion-item", action.getTarget());
        assertEquals(">=6", action.getValue());
    }

    @Test
    public void testToToolCallBrowserAssertCount()
    {
        final Action action = new Action("ASSERT_COUNT", ".search-suggestion-item", List.of(">=6"), "Assert count", "Verify at least 6 items", false);
        final ToolCall call = action.toToolCall();

        assertEquals("assert_count", call.toolName());
        assertEquals(".search-suggestion-item", call.arguments().path("selector").asText());
        assertEquals(6, call.arguments().path("minCount").asInt());
        assertEquals(6, call.arguments().path("count").asInt());
        assertEquals("MIN", call.arguments().path("operator").asText());
    }

    @Test
    public void testNegatedDefaultAndSetter()
    {
        final Action action = new Action("ASSERT_URL", "url", "Check URL");
        assertFalse(action.isNegated());

        final Action negatedAction = action.withNegated(true);
        assertTrue(negatedAction.isNegated());
        assertEquals("ASSERT_URL", negatedAction.getType());
        assertEquals("url", negatedAction.getTarget());
    }

    @Test
    public void testNegatedJsonRoundTrip() throws Exception
    {
        final Action original = new Action("ASSERT_URL", "url", List.of("#"), "Verify URL does not contain #", "Negative assertion", false, true);

        final String serializedJson = this.mapper.writeValueAsString(original);
        assertTrue(serializedJson.contains("\"negated\":true") || serializedJson.contains("\"negated\" : true"),
                "Serialized JSON must contain 'negated': true, but was: " + serializedJson);

        final Action deserialized = this.mapper.readValue(serializedJson, Action.class);
        assertEquals("ASSERT_URL", deserialized.getType());
        assertEquals("url", deserialized.getTarget());
        assertEquals("#", deserialized.getValue());
        assertTrue(deserialized.isNegated());
    }

    @Test
    public void testFromToolCallWithNegatedFlag()
    {
        final ObjectNode args = this.mapper.createObjectNode();
        args.put("expectedUrl", "#");
        args.put("negated", true);
        final ToolCall call = new ToolCall("call-neg", "assert_url", args);

        final Action action = Action.fromToolCall(call);
        assertEquals("ASSERT_URL", action.getType());
        assertEquals("url", action.getTarget());
        assertEquals("#", action.getValue());
        assertTrue(action.isNegated());

        final ToolCall roundTripCall = action.toToolCall();
        assertEquals("assert_url", roundTripCall.toolName());
        assertTrue(roundTripCall.arguments().path("negated").asBoolean());
    }

    @Test
    public void testFromToolCallAssertElementStateInversion()
    {
        final ObjectNode args = this.mapper.createObjectNode();
        args.put("selector", "#btn");
        args.put("state", "visible");
        args.put("negated", true);
        final ToolCall call = new ToolCall("call-state-inv", "assert_element_state", args);

        final Action action = Action.fromToolCall(call);
        assertEquals("ASSERT_HIDDEN", action.getType());
        assertEquals("#btn", action.getTarget());
    }

    @Test
    public void testFromToolCallAssertElementStateUnfocused()
    {
        final ObjectNode args = this.mapper.createObjectNode();
        args.put("selector", "#input");
        args.put("state", "unfocused");
        final ToolCall call = new ToolCall("call-unfocused", "assert_element_state", args);

        final Action action = Action.fromToolCall(call);
        assertEquals("ASSERT_UNFOCUSED", action.getType());
        assertEquals("#input", action.getTarget());
    }

    @Test
    public void testFromToolCallAssertCountNotEquals()
    {
        final ObjectNode args = this.mapper.createObjectNode();
        args.put("selector", ".item");
        args.put("count", 3);
        args.put("operator", "NOT_EQUALS");
        final ToolCall call = new ToolCall("call-cnt-neq", "assert_count", args);

        final Action action = Action.fromToolCall(call);
        assertEquals("ASSERT_COUNT", action.getType());
        assertEquals(".item", action.getTarget());
        assertEquals("!=3", action.getValue());
    }
}
