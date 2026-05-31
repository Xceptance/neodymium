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
package com.xceptance.neodymium.ai.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link ActionParser} compact key format parsing and validation.
 *
 * @author AI-generated: Gemini 2.5 Flash
 */
class ActionParserEscalateTest
{
    private final ActionParser parser = new ActionParser();

    @Test
    void isEscalateRequested_withEscalateStatus_returnsTrue()
    {
        final String response = """
                {
                  "s": false,
                  "st": "ESCALATE",
                  "r": "I see 5 'View Details' links but cannot distinguish them.",
                  "a": []
                }
                """;
        assertTrue(parser.isEscalateRequested(response));
    }

    @Test
    void isEscalateRequested_caseInsensitive_returnsTrue()
    {
        final String response = """
                {
                  "s": false,
                  "st": "escalate",
                  "r": "Need more context",
                  "a": []
                }
                """;
        assertTrue(parser.isEscalateRequested(response));
    }

    @Test
    void isEscalateRequested_mixedCase_returnsTrue()
    {
        final String response = """
                {
                  "s": false,
                  "st": "Escalate",
                  "r": "Need more context",
                  "a": []
                }
                """;
        assertTrue(parser.isEscalateRequested(response));
    }

    @Test
    void isEscalateRequested_withBugStatus_returnsFalse()
    {
        final String response = """
                {
                  "s": false,
                  "st": "BUG",
                  "r": "Element not found",
                  "a": []
                }
                """;
        assertFalse(parser.isEscalateRequested(response));
    }

    @Test
    void isEscalateRequested_withNoStatus_returnsFalse()
    {
        final String response = """
                {
                  "s": true,
                  "a": [{"t": "CLICK", "tg": "#btn"}]
                }
                """;
        assertFalse(parser.isEscalateRequested(response));
    }

    @Test
    void isEscalateRequested_withEmptyResponse_returnsFalse()
    {
        assertFalse(parser.isEscalateRequested(""));
        assertFalse(parser.isEscalateRequested(null));
    }

    @Test
    void isEscalateRequested_withMalformedJson_returnsFalse()
    {
        assertFalse(parser.isEscalateRequested("not json at all"));
    }

    @Test
    void isEscalateRequested_wrappedInCodeFences_returnsTrue()
    {
        final String response = """
                ```json
                {
                  "s": false,
                  "st": "ESCALATE",
                  "r": "Need text content to find the price",
                  "a": []
                }
                ```
                """;
        assertTrue(parser.isEscalateRequested(response));
    }

    @Test
    void parse_withCompactKeys_correctlyMapsToActions()
    {
        final String response = """
                {
                  "s": true,
                  "r": "Test compact key parsing logic",
                  "a": [
                    {
                      "t": "TYPE",
                      "tg": "#username",
                      "v": "testuser",
                      "d": "type username",
                      "ed": "Username input field",
                      "ad": true
                    }
                  ]
                }
                """;
        final List<Action> actions = parser.parse(response);
        assertNotNull(actions);
        assertEquals(1, actions.size());

        final Action action = actions.get(0);
        assertEquals("TYPE", action.getType());
        assertEquals("#username", action.getTarget());
        assertNotNull(action.getValues());
        assertEquals("testuser", action.getValues().get(0));
        assertEquals("type username", action.getDescription());
        assertEquals("Username input field", action.getElementDetails());
        assertTrue(action.getAdjust());
    }

    @Test
    void actionSerialization_producesStandardLongKeys()
    {
        final Action action = new Action("TYPE", "#username", List.of("testuser"), "type username");
        action.setElementDetails("Username input field");
        action.setAdjust(true);

        final Gson gson = new Gson();
        final String json = gson.toJson(action);

        // Standard serializations must use the long names to prevent breaking recorded playbooks on disk
        assertTrue(json.contains("\"type\":\"TYPE\""));
        assertTrue(json.contains("\"target\":\"#username\""));
        assertTrue(json.contains("\"description\":\"type username\""));
        assertTrue(json.contains("\"elementDetails\":\"Username input field\""));
        assertTrue(json.contains("\"adjust\":true"));

        // Confirm that the serialized JSON does NOT contain compact keys
        assertFalse(json.contains("\"t\":"));
        assertFalse(json.contains("\"tg\":"));
        assertFalse(json.contains("\"ed\":"));
    }
}
