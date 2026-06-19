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
package com.xceptance.neodymium.ai.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link InteractiveHud} class, verifying the breakpoint parsing logic.
 *
 * @author AI-generated: Gemini 2.5 Flash
 * @author Xceptance GmbH 2026
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

    @Test
    void calculateStateSignature_updatesOnStateChanges() throws Exception
    {
        final InteractiveHud hud = new InteractiveHud();
        
        final String sig1 = getPrivateField(hud, "lastStateSignature").toString();
        assertEquals("", sig1);

        hud.injectOrUpdateHud(List.of("Step 1"), List.of(), false, false, false, "Step 1", "Reason 1", false);
        final String sig2 = getPrivateField(hud, "lastStateSignature").toString();
        assertTrue(!sig2.isEmpty());

        hud.injectOrUpdateHud(List.of("Step 2"), List.of(), false, false, false, "Step 2", "Reason 1", false);
        final String sig3 = getPrivateField(hud, "lastStateSignature").toString();
        assertTrue(!sig3.isEmpty());
        assertTrue(!sig2.equals(sig3));

        hud.injectOrUpdateHud(List.of("Step 2"), List.of(), false, false, false, "Step 2", "Reason 2", false);
        final String sig4 = getPrivateField(hud, "lastStateSignature").toString();
        assertTrue(!sig4.equals(sig3));
    }

    private void setPrivateField(final Object obj, final String fieldName, final Object value) throws Exception
    {
        final Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    private Object getPrivateField(final Object obj, final String fieldName) throws Exception
    {
        final Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}
