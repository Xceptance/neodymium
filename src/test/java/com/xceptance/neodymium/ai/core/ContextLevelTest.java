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
    void escalate_fromHint_returnsAxTree()
    {
        assertEquals(ContextLevel.AXTREE, ContextLevel.HINT.escalate());
    }

    @Test
    void escalate_fromAxTree_returnsLean()
    {
        assertEquals(ContextLevel.LEAN, ContextLevel.AXTREE.escalate());
    }

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
    void escalate_fromVisualLean_returnsVisual()
    {
        assertEquals(ContextLevel.VISUAL, ContextLevel.VISUAL_LEAN.escalate());
    }

    @Test
    void escalate_fromVisual_returnsNull()
    {
        assertNull(ContextLevel.VISUAL.escalate());
    }

    @Test
    void includesScreenshot_visualLevels()
    {
        assertFalse(ContextLevel.HINT.includesScreenshot());
        assertFalse(ContextLevel.AXTREE.includesScreenshot());
        assertFalse(ContextLevel.LEAN.includesScreenshot());
        assertFalse(ContextLevel.STANDARD.includesScreenshot());
        assertTrue(ContextLevel.VISUAL_LEAN.includesScreenshot());
        assertTrue(ContextLevel.VISUAL.includesScreenshot());
    }

    @Test
    void includesTextContent_standardAndVisual()
    {
        assertFalse(ContextLevel.HINT.includesTextContent());
        assertFalse(ContextLevel.AXTREE.includesTextContent());
        assertFalse(ContextLevel.LEAN.includesTextContent());
        assertTrue(ContextLevel.STANDARD.includesTextContent());
        assertFalse(ContextLevel.VISUAL_LEAN.includesTextContent());
        assertTrue(ContextLevel.VISUAL.includesTextContent());
    }

    @Test
    void escalationChain_coversAllLevels()
    {
        // Verify the standard escalation chain: HINT -> AXTREE -> LEAN -> STANDARD -> VISUAL -> null
        ContextLevel current = ContextLevel.HINT;
        assertEquals(ContextLevel.HINT, current);

        current = current.escalate();
        assertEquals(ContextLevel.AXTREE, current);

        current = current.escalate();
        assertEquals(ContextLevel.LEAN, current);

        current = current.escalate();
        assertEquals(ContextLevel.STANDARD, current);

        current = current.escalate();
        assertEquals(ContextLevel.VISUAL, current);

        final ContextLevel terminal = current.escalate();
        assertNull(terminal);
    }

    @Test
    void escalationChain_fromVisualLean()
    {
        // Verify the visual tag escalation chain: VISUAL_LEAN -> VISUAL -> null
        ContextLevel current = ContextLevel.VISUAL_LEAN;
        assertEquals(ContextLevel.VISUAL_LEAN, current);

        current = current.escalate();
        assertEquals(ContextLevel.VISUAL, current);

        final ContextLevel terminal = current.escalate();
        assertNull(terminal);
    }
}
