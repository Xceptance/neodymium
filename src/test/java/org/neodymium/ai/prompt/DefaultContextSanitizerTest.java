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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.neodymium.ai.executor.MockSutState;
import org.neodymium.ai.model.SessionData;

/**
 * Unit tests for {@link DefaultContextSanitizer}.
 * Validates sensitive secret masking in prompt strings and SUT DOM state.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class DefaultContextSanitizerTest
{
    @Test
    public void testSanitizeSensitiveData()
    {
        final DefaultContextSanitizer sanitizer = new DefaultContextSanitizer();
        final SessionData sessionData = new SessionData();
        sessionData.putDynamic("password", "SuperSecret123!", true);

        final String rawPrompt = "Type 'SuperSecret123!' into input field #pass";
        final MockSutState sutState = new MockSutState("<input id='pass' value='SuperSecret123!'/>", "hash123");

        final SanitizedPayload sanitizedPayload = sanitizer.sanitize(rawPrompt, sutState, sessionData);

        assertNotNull(sanitizedPayload, "Sanitized payload should not be null.");
        assertFalse(sanitizedPayload.sanitizedPrompt().contains("SuperSecret123!"), "Prompt should not contain raw secret.");
        assertFalse(sanitizedPayload.sanitizedStateText().contains("SuperSecret123!"), "State text should not contain raw secret.");
        assertTrue(sanitizedPayload.sanitizedPrompt().contains("[MASKED_VAR_password]"), "Prompt should contain masked placeholder.");
        assertEquals("${password}", sanitizedPayload.maskToVariableMap().get("[MASKED_VAR_password]"), "Mask map should resolve back to variable placeholder.");
    }
}
