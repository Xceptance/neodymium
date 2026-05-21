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
package com.xceptance.neodymium.ai.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests the case-insensitive support for the (visual) tag and context-level
 * resolution in {@link AiAgent}.
 *
 * // AI-generated: Antigravity
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
        assertEquals(ContextLevel.LEAN, invokeGetInitialContextLevel("Type 'text' in input"));
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
            Integer.class,
            String.class,
            String.class
        );
        formatFailureMessageMethod.setAccessible(true);

        // Case 1: both present
        final String res1 = (String) formatFailureMessageMethod.invoke(
            null,
            "Click button",
            42,
            "test.yaml",
            ": click failed"
        );
        assertEquals("Instruction 'Click button' failed at line 42 in test.yaml: click failed", res1);

        // Case 2: only line number
        final String res2 = (String) formatFailureMessageMethod.invoke(
            null,
            "Click button",
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
            ": click failed"
        );
        assertEquals("Instruction 'Click button' failed: click failed", res4);
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
