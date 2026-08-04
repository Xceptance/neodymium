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
package org.neodymium.ai.executor.selenide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests verifying ContextLevel behavior, escalation paths, screenshot inclusion, and text content inclusion.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class ContextLevelTest
{
    /**
     * Default constructor.
     */
    public ContextLevelTest()
    {
    }

    /**
     * Verifies escalation paths across all context levels, including VISUAL_MINIMAL -> VISUAL_LEAN -> VISUAL.
     */
    @Test
    public void testEscalate()
    {
        assertEquals(ContextLevel.AXTREE, ContextLevel.HINT.escalate());
        assertEquals(ContextLevel.STANDARD, ContextLevel.AXTREE.escalate());
        assertEquals(ContextLevel.STANDARD, ContextLevel.LEAN.escalate());
        assertEquals(ContextLevel.VISUAL, ContextLevel.STANDARD.escalate());
        assertEquals(ContextLevel.VISUAL_LEAN, ContextLevel.VISUAL_MINIMAL.escalate());
        assertEquals(ContextLevel.VISUAL, ContextLevel.VISUAL_LEAN.escalate());
        assertNull(ContextLevel.VISUAL.escalate());
    }

    /**
     * Verifies screenshot inclusion flags for visual context levels.
     */
    @Test
    public void testIncludesScreenshot()
    {
        assertFalse(ContextLevel.HINT.includesScreenshot());
        assertFalse(ContextLevel.AXTREE.includesScreenshot());
        assertFalse(ContextLevel.LEAN.includesScreenshot());
        assertFalse(ContextLevel.STANDARD.includesScreenshot());
        assertTrue(ContextLevel.VISUAL_MINIMAL.includesScreenshot());
        assertTrue(ContextLevel.VISUAL_LEAN.includesScreenshot());
        assertTrue(ContextLevel.VISUAL.includesScreenshot());
    }

    /**
     * Verifies text content inclusion flags.
     */
    @Test
    public void testIncludesTextContent()
    {
        assertFalse(ContextLevel.HINT.includesTextContent());
        assertFalse(ContextLevel.AXTREE.includesTextContent());
        assertFalse(ContextLevel.LEAN.includesTextContent());
        assertFalse(ContextLevel.VISUAL_MINIMAL.includesTextContent());
        assertFalse(ContextLevel.VISUAL_LEAN.includesTextContent());
        assertTrue(ContextLevel.STANDARD.includesTextContent());
        assertTrue(ContextLevel.VISUAL.includesTextContent());
    }
}
