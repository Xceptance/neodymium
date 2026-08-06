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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ContextLevel} enum escalation sequence and capability boolean checks.
 *
 * @author AI-generated: Gemini 3.6 Flash (High)
 * @author Xceptance GmbH 2026
 */
public class ContextLevelTest
{
    @Test
    public void testEscalationSequence()
    {
        Assertions.assertEquals(ContextLevel.LEAN, ContextLevel.HINT.escalate());
        Assertions.assertEquals(ContextLevel.STANDARD, ContextLevel.LEAN.escalate());
        Assertions.assertEquals(ContextLevel.RICH, ContextLevel.STANDARD.escalate());
        Assertions.assertEquals(ContextLevel.VISUAL, ContextLevel.RICH.escalate());
        Assertions.assertEquals(ContextLevel.VISUAL_LEAN, ContextLevel.VISUAL.escalate());
        Assertions.assertEquals(ContextLevel.VISUAL_RICH, ContextLevel.VISUAL_LEAN.escalate());
        Assertions.assertNull(ContextLevel.VISUAL_RICH.escalate());
    }

    @Test
    public void testIncludesScreenshot()
    {
        Assertions.assertFalse(ContextLevel.HINT.includesScreenshot());
        Assertions.assertFalse(ContextLevel.LEAN.includesScreenshot());
        Assertions.assertFalse(ContextLevel.STANDARD.includesScreenshot());
        Assertions.assertFalse(ContextLevel.RICH.includesScreenshot());

        Assertions.assertTrue(ContextLevel.VISUAL.includesScreenshot());
        Assertions.assertTrue(ContextLevel.VISUAL_LEAN.includesScreenshot());
        Assertions.assertTrue(ContextLevel.VISUAL_RICH.includesScreenshot());
    }

    @Test
    public void testIncludesTextContent()
    {
        Assertions.assertFalse(ContextLevel.HINT.includesTextContent());
        Assertions.assertFalse(ContextLevel.LEAN.includesTextContent());
        Assertions.assertFalse(ContextLevel.VISUAL.includesTextContent());
        Assertions.assertFalse(ContextLevel.VISUAL_LEAN.includesTextContent());

        Assertions.assertTrue(ContextLevel.STANDARD.includesTextContent());
        Assertions.assertTrue(ContextLevel.RICH.includesTextContent());
        Assertions.assertTrue(ContextLevel.VISUAL_RICH.includesTextContent());
    }

    @Test
    public void testIncludesRichMetadata()
    {
        Assertions.assertFalse(ContextLevel.HINT.includesRichMetadata());
        Assertions.assertFalse(ContextLevel.LEAN.includesRichMetadata());
        Assertions.assertFalse(ContextLevel.STANDARD.includesRichMetadata());
        Assertions.assertFalse(ContextLevel.VISUAL.includesRichMetadata());
        Assertions.assertFalse(ContextLevel.VISUAL_LEAN.includesRichMetadata());

        Assertions.assertTrue(ContextLevel.RICH.includesRichMetadata());
        Assertions.assertTrue(ContextLevel.VISUAL_RICH.includesRichMetadata());
    }
}
