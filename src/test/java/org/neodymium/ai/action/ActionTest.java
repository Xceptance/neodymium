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
import java.util.List;
import org.junit.jupiter.api.Test;

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
}
