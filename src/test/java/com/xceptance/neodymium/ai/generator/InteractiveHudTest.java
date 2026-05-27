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
package com.xceptance.neodymium.ai.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link InteractiveHud} class, verifying the breakpoint parsing logic.
 *
 * // AI-generated: Antigravity and Gemini 3.5 Sonnet
 */
final class InteractiveHudTest
{
    @Test
    void getBreakpoints_defaultEmpty_returnsEmptyList()
    {
        final InteractiveHud hud = new InteractiveHud();
        final List<Integer> breakpoints = hud.getBreakpoints();
        assertTrue(breakpoints.isEmpty());
    }

    @Test
    void getBreakpoints_validJsonArray_returnsCorrectParsedIndices() throws Exception
    {
        final InteractiveHud hud = new InteractiveHud();
        setPrivateField(hud, "lastBreakpointsStr", "[0, 2, 5]");

        final List<Integer> breakpoints = hud.getBreakpoints();
        assertEquals(3, breakpoints.size());
        assertEquals(0, breakpoints.get(0));
        assertEquals(2, breakpoints.get(1));
        assertEquals(5, breakpoints.get(2));
    }

    @Test
    void getBreakpoints_invalidJson_returnsEmptyList() throws Exception
    {
        final InteractiveHud hud = new InteractiveHud();
        setPrivateField(hud, "lastBreakpointsStr", "[invalid, json}");

        final List<Integer> breakpoints = hud.getBreakpoints();
        assertTrue(breakpoints.isEmpty());
    }

    @Test
    void getBreakpoints_nullValue_returnsEmptyList() throws Exception
    {
        final InteractiveHud hud = new InteractiveHud();
        setPrivateField(hud, "lastBreakpointsStr", null);

        final List<Integer> breakpoints = hud.getBreakpoints();
        assertTrue(breakpoints.isEmpty());
    }

    private void setPrivateField(final Object obj, final String fieldName, final Object value) throws Exception
    {
        final Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
