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
}
