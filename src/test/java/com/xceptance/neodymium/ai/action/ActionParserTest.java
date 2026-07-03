/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance GmbH
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link ActionParser} class.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class ActionParserTest
{
    @Test
    public void testParseNestedJsonInValue()
    {
        final String llmResponse = """
            {
              "s": true,
              "d": true,
              "a": [
                {
                  "t": "STORE",
                  "v": [
                    "productsAddedInTheRound",
                    {
                      "Tripp Trapp® Stuhl": 1,
                      "Baby Set": 1,
                      "Tray": 1
                    }
                  ],
                  "desc": "Store the names of the product items in the bundle with a value of 1 in the productsAddedInTheRound map.",
                  "ed": "productsAddedInTheRound map"
                }
              ],
              "r": "The instruction specifies a conditional action..."
            }
            """;

        final ActionParser parser = new ActionParser();
        final List<Action> actions = parser.parse(llmResponse);

        assertNotNull(actions);
        assertEquals(1, actions.size());

        final Action action = actions.get(0);
        assertEquals("STORE", action.getType());
        assertNotNull(action.getValues());
        assertEquals(2, action.getValues().size());
        assertEquals("productsAddedInTheRound", action.getValues().get(0));
        // The second element should be stringified JSON
        assertTrue(action.getValues().get(1).contains("Tripp Trapp® Stuhl"));
    }
}
