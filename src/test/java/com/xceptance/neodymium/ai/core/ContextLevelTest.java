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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link ContextLevel} enum, verifying the escalation chain
 * and capability flags.
 *
 * // AI-generated: Gemini 3.1 Pro
 */
class ContextLevelTest
{
    @Test
    void escalate_fromLean_returnsStandard()
    {
        assertEquals(ContextLevel.STANDARD, ContextLevel.LEAN.escalate());
    }

    @Test
    void escalate_fromStandard_returnsVisual()
    {
        assertEquals(ContextLevel.VISUAL, ContextLevel.STANDARD.escalate());
    }

    @Test
    void escalate_fromVisual_returnsNull()
    {
        assertNull(ContextLevel.VISUAL.escalate());
    }

    @Test
    void includesScreenshot_onlyVisual()
    {
        assertFalse(ContextLevel.LEAN.includesScreenshot());
        assertFalse(ContextLevel.STANDARD.includesScreenshot());
        assertTrue(ContextLevel.VISUAL.includesScreenshot());
    }

    @Test
    void includesTextContent_standardAndVisual()
    {
        assertFalse(ContextLevel.LEAN.includesTextContent());
        assertTrue(ContextLevel.STANDARD.includesTextContent());
        assertTrue(ContextLevel.VISUAL.includesTextContent());
    }

    @Test
    void escalationChain_coversAllLevels()
    {
        // Verify the full escalation chain: LEAN -> STANDARD -> VISUAL -> null
        ContextLevel current = ContextLevel.LEAN;
        assertEquals(ContextLevel.LEAN, current);

        current = current.escalate();
        assertEquals(ContextLevel.STANDARD, current);

        current = current.escalate();
        assertEquals(ContextLevel.VISUAL, current);

        final ContextLevel terminal = current.escalate();
        assertNull(terminal);
    }
}
