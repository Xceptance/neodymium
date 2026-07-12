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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.client.SutAttachment;
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.model.SessionData;

/**
 * TDD test suite validating the pre-LLM context sanitization, reverse-mapping
 * of placeholder values, and action parameterization.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class SanitizersTest
{
    /**
     * Mock SUT state implementation for testing.
     */
    private static final class MockSutState implements SutState
    {
        private final String textContent;

        MockSutState(final String textContent)
        {
            this.textContent = textContent;
        }

        @Override
        public String getTextContent()
        {
            return this.textContent;
        }

        @Override
        public List<SutAttachment> getAttachments()
        {
            return List.of();
        }

        @Override
        public String getContentHash()
        {
            return "mock-hash";
        }
    }

    /**
     * Constructs a default test instance.
     */
    public SanitizersTest()
    {
    }

    /**
     * Verifies that the ContextSanitizer masks sensitive variables and compiles
     * the reverse mapping linking placeholders back to their variable references.
     */
    @Test
    public void testContextSanitizerMaskingAndReverseMapping()
    {
        final Map<String, SessionData.DataEntry> staticMap = new HashMap<>();
        staticMap.put("apiKey", new SessionData.DataEntry("AI_KEY_12345", true));
        staticMap.put("password", new SessionData.DataEntry("AliceSecret99", true));

        final SessionData session = new SessionData(staticMap);

        final String rawPrompt = "Use apiKey AI_KEY_12345 to authenticate.";
        final String rawStateDom = "<html><input value=\"AliceSecret99\"></html>";
        final SutState rawState = new MockSutState(rawStateDom);

        final ContextSanitizer sanitizer = new DefaultContextSanitizer();
        final SanitizedPayload payload = sanitizer.sanitize(rawPrompt, rawState, session);

        // 1. Verify masking occurred
        assertTrue(payload.sanitizedPrompt().contains("[MASKED_VAR_apiKey]"));
        assertTrue(payload.sanitizedStateText().contains("[MASKED_VAR_password]"));
        
        // Assert actual secrets are no longer visible in outbound payloads
        assertTrue(!payload.sanitizedPrompt().contains("AI_KEY_12345"));
        assertTrue(!payload.sanitizedStateText().contains("AliceSecret99"));

        // 2. Verify reverse mapping links placeholders back to variable references
        final Map<String, String> reverseMap = payload.maskToVariableMap();
        assertEquals(2, reverseMap.size());
        assertEquals("${apiKey}", reverseMap.get("[MASKED_VAR_apiKey]"));
        assertEquals("${password}", reverseMap.get("[MASKED_VAR_password]"));
    }

    /**
     * Verifies that the ActionSanitizer parameterizes executed actions, replacing
     * raw input secrets with their variable references.
     */
    @Test
    public void testActionSanitizerParameterization()
    {
        final Map<String, SessionData.DataEntry> staticMap = new HashMap<>();
        staticMap.put("password", new SessionData.DataEntry("AliceSecret99", true));

        final SessionData session = new SessionData(staticMap);

        // Raw action typing the secret value
        final Action rawAction = new Action(
            "TYPE",
            "//input[@id='pass']",
            List.of("AliceSecret99"),
            "Type AliceSecret99 into password field",
            "typed password"
        );

        final ActionSanitizer sanitizer = new DefaultActionSanitizer();
        final Action sanitizedAction = sanitizer.sanitize(rawAction, session);

        assertNotNull(sanitizedAction);
        
        // Assert value, target, and description are parameterized
        assertEquals("${password}", sanitizedAction.getValue());
        assertEquals("//input[@id='pass']", sanitizedAction.getTarget());
        assertEquals("Type ${password} into password field", sanitizedAction.getDescription());
    }
}
