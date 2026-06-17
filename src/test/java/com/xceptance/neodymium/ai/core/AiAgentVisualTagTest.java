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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests the case-insensitive support for the (visual) tag and context-level
 * resolution in {@link AiAgent}.
 *
 * @author AI-generated: Gemini 2.5 Flash
 * @author Xceptance GmbH 2026
 */
final class AiAgentVisualTagTest
{
    private static Method getInitialContextLevelMethod;

    @BeforeAll
    static void setup() throws Exception
    {
        getInitialContextLevelMethod = AiAgent.class.getDeclaredMethod("getInitialContextLevel", String.class);
        getInitialContextLevelMethod.setAccessible(true);
    }

    private ContextLevel invokeGetInitialContextLevel(final String instruction) throws Exception
    {
        return (ContextLevel) getInitialContextLevelMethod.invoke(null, instruction);
    }

    @Test
    void testGetInitialContextLevel_withVisualTag() throws Exception
    {
        assertEquals(ContextLevel.VISUAL_LEAN, invokeGetInitialContextLevel("Check page matches template (visual)"));
        assertEquals(ContextLevel.VISUAL_LEAN, invokeGetInitialContextLevel("Check page matches template (VISUAL)"));
        assertEquals(ContextLevel.VISUAL_LEAN, invokeGetInitialContextLevel("Check page matches template (ViSuAl)"));
        assertEquals(ContextLevel.VISUAL_LEAN, invokeGetInitialContextLevel("(visual) check logo"));
    }

    @Test
    void testGetInitialContextLevel_withHintTag() throws Exception
    {
        assertEquals(ContextLevel.HINT, invokeGetInitialContextLevel("Click the search button (hint: .search)"));
    }

    @Test
    void testGetInitialContextLevel_withoutTags() throws Exception
    {
        assertEquals(ContextLevel.AXTREE, invokeGetInitialContextLevel("Type 'text' in input"));
    }

    @Test
    void testGetInitialContextLevel_bothTags_precedence() throws Exception
    {
        // Visual tag should take precedence over hint tags
        assertEquals(ContextLevel.VISUAL_LEAN, invokeGetInitialContextLevel("Check that visual displays (VISUAL) and has (hint: .img)"));
    }

    @Test
    void testFormatFailureMessage() throws Exception
    {
        final Method formatFailureMessageMethod = AiAgent.class.getDeclaredMethod(
            "formatFailureMessage",
            String.class,
            String.class,
            Integer.class,
            String.class,
            String.class
        );
        formatFailureMessageMethod.setAccessible(true);

        // Case 1: both present
        final String res1 = (String) formatFailureMessageMethod.invoke(
            null,
            "Click button",
            null,
            42,
            "test.yaml",
            ": click failed"
        );
        assertEquals("Instruction 'Click button' failed at line 42 in test.yaml: click failed", res1);

        // Case 2: only line number
        final String res2 = (String) formatFailureMessageMethod.invoke(
            null,
            "Click button",
            null,
            42,
            null,
            ": click failed"
        );
        assertEquals("Instruction 'Click button' failed at line 42: click failed", res2);

        // Case 3: only source file
        final String res3 = (String) formatFailureMessageMethod.invoke(
            null,
            "Click button",
            null,
            null,
            "test.yaml",
            ": click failed"
        );
        assertEquals("Instruction 'Click button' failed in test.yaml: click failed", res3);

        // Case 4: neither
        final String res4 = (String) formatFailureMessageMethod.invoke(
            null,
            "Click button",
            null,
            null,
            null,
            ": click failed"
        );
        assertEquals("Instruction 'Click button' failed: click failed", res4);

        // Case 5: split step with original instruction present
        final String res5 = (String) formatFailureMessageMethod.invoke(
            null,
            "Click button",
            "Click button and verify text",
            42,
            "test.yaml",
            ": click failed"
        );
        assertEquals("Instruction 'Click button' (virtual step split from: 'Click button and verify text') failed at line 42 in test.yaml: click failed", res5);
    }

    @Test
    void testFormatFailureLogContext() throws Exception
    {
        final Method formatFailureLogContextMethod = AiAgent.class.getDeclaredMethod(
            "formatFailureLogContext",
            Integer.class,
            String.class
        );
        formatFailureLogContextMethod.setAccessible(true);

        // Case 1: both present
        final String res1 = (String) formatFailureLogContextMethod.invoke(null, 42, "path/to/test.yaml");
        assertEquals(" (test.yaml:42)", res1);

        // Case 2: only line number
        final String res2 = (String) formatFailureLogContextMethod.invoke(null, 42, null);
        assertEquals(" (line 42)", res2);

        // Case 3: only source file
        final String res3 = (String) formatFailureLogContextMethod.invoke(null, null, "path/to/test.yaml");
        assertEquals(" (test.yaml)", res3);

        // Case 4: neither
        final String res4 = (String) formatFailureLogContextMethod.invoke(null, null, null);
        assertEquals("", res4);
    }
}
