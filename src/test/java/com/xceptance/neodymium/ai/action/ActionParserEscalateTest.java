/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xceptance.neodymium.ai.action;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link ActionParser#isEscalateRequested(String)} method,
 * verifying detection of the ESCALATE response status used in the
 * escalating context protocol.
 *
 * // AI-generated: Gemini 3.1 Pro
 */
class ActionParserEscalateTest
{
    private final ActionParser parser = new ActionParser();

    @Test
    void isEscalateRequested_withEscalateStatus_returnsTrue()
    {
        final String response = """
                {
                  "success": false,
                  "status": "ESCALATE",
                  "reasoning": "I see 5 'View Details' links but cannot distinguish them.",
                  "actions": []
                }
                """;
        assertTrue(parser.isEscalateRequested(response));
    }

    @Test
    void isEscalateRequested_caseInsensitive_returnsTrue()
    {
        final String response = """
                {
                  "success": false,
                  "status": "escalate",
                  "reasoning": "Need more context",
                  "actions": []
                }
                """;
        assertTrue(parser.isEscalateRequested(response));
    }

    @Test
    void isEscalateRequested_mixedCase_returnsTrue()
    {
        final String response = """
                {
                  "success": false,
                  "status": "Escalate",
                  "reasoning": "Need more context",
                  "actions": []
                }
                """;
        assertTrue(parser.isEscalateRequested(response));
    }

    @Test
    void isEscalateRequested_withBugStatus_returnsFalse()
    {
        final String response = """
                {
                  "success": false,
                  "status": "BUG",
                  "reasoning": "Element not found",
                  "actions": []
                }
                """;
        assertFalse(parser.isEscalateRequested(response));
    }

    @Test
    void isEscalateRequested_withNoStatus_returnsFalse()
    {
        final String response = """
                {
                  "success": true,
                  "actions": [{"type": "CLICK", "target": "#btn"}]
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
                  "success": false,
                  "status": "ESCALATE",
                  "reasoning": "Need text content to find the price",
                  "actions": []
                }
                ```
                """;
        assertTrue(parser.isEscalateRequested(response));
    }
}
