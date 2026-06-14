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
package com.xceptance.neodymium.ai.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link LlmCallDetails} verification helper methods.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
class LlmCallDetailsTest
{
    @Test
    void isSuccess_and_isDone_returnsExpectedValues()
    {
        final String successDoneResponse = """
                {
                  "s": true,
                  "d": true,
                  "r": "Everything is perfect.",
                  "a": []
                }
                """;
        final LlmCallDetails details1 = createDetails(successDoneResponse);
        assertTrue(details1.isSuccess());
        assertTrue(details1.isDone());

        final String successNotDoneResponse = """
                {
                  "s": true,
                  "d": false,
                  "r": "Partial success, more actions needed.",
                  "a": [{"t": "CLICK", "tg": "#btn"}]
                }
                """;
        final LlmCallDetails details2 = createDetails(successNotDoneResponse);
        assertTrue(details2.isSuccess());
        assertFalse(details2.isDone());

        final String failureDoneResponse = """
                {
                  "s": false,
                  "d": true,
                  "e": "Visual verification failed.",
                  "r": "Bear not found",
                  "a": []
                }
                """;
        final LlmCallDetails details3 = createDetails(failureDoneResponse);
        assertFalse(details3.isSuccess());
        assertTrue(details3.isDone());
    }

    private final LlmCallDetails createDetails(final String rawResponse)
    {
        return new LlmCallDetails(
                "systemPrompt",
                "userPrompt",
                "base64Screenshot",
                "htmlDomContext",
                100,
                ContextLevel.LEAN,
                rawResponse,
                rawResponse,
                10L,
                20L,
                0L,
                30L,
                null,
                200,
                LlmMode.AGENT
        );
    }
}
