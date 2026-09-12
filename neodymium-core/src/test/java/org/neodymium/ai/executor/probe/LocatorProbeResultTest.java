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
package org.neodymium.ai.executor.probe;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests verifying locator probing DTOs.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class LocatorProbeResultTest
{
    @Test
    public void testProbeBoundingRect()
    {
        final ProbeBoundingRect rect1 = new ProbeBoundingRect(10.0, 20.0, 100.0, 50.0);
        final ProbeBoundingRect rect2 = new ProbeBoundingRect(10.0, 20.0, 100.0, 50.0);

        Assertions.assertEquals(10.0, rect1.getX());
        Assertions.assertEquals(20.0, rect1.getY());
        Assertions.assertEquals(100.0, rect1.getWidth());
        Assertions.assertEquals(50.0, rect1.getHeight());
        Assertions.assertEquals(rect1, rect2);
        Assertions.assertEquals("[10, 20, 100x50]", rect1.toString());
    }

    @Test
    public void testProbeElementSummary()
    {
        final ProbeBoundingRect rect = new ProbeBoundingRect(5, 5, 30, 30);
        final ProbeElementSummary summary = new ProbeElementSummary(
                0, "button", "S", Map.of("class", "size-btn"), true, true, false, rect, "<button class=\"size-btn\">S</button>");

        Assertions.assertEquals(0, summary.getIndex());
        Assertions.assertEquals("button", summary.getTagName());
        Assertions.assertEquals("S", summary.getText());
        Assertions.assertEquals("size-btn", summary.getAttributes().get("class"));
        Assertions.assertTrue(summary.isVisible());
        Assertions.assertTrue(summary.isEnabled());
        Assertions.assertFalse(summary.isSelected());
        Assertions.assertEquals(rect, summary.getRect());
        Assertions.assertTrue(summary.toString().contains("visible=true"));
    }

    @Test
    public void testLocatorProbeResultStatusAndUniqueness()
    {
        final ProbeElementSummary s1 = new ProbeElementSummary(0, "button", "S", Map.of(), true, true, false, null, "");
        final LocatorProbeResult uniqueResult = LocatorProbeResult.supported("#submit", 1, List.of(s1));

        Assertions.assertTrue(uniqueResult.isSupported());
        Assertions.assertTrue(uniqueResult.isUnique());
        Assertions.assertEquals(1, uniqueResult.getMatchCount());
        Assertions.assertNull(uniqueResult.getErrorMessage());

        final LocatorProbeResult ambiguousResult = LocatorProbeResult.supported(".btn", 3, List.of(s1));
        Assertions.assertFalse(ambiguousResult.isUnique());
        Assertions.assertEquals(3, ambiguousResult.getMatchCount());

        final LocatorProbeResult errorResult = LocatorProbeResult.error("::invalid", "Syntax error");
        Assertions.assertFalse(errorResult.isUnique());
        Assertions.assertEquals("Syntax error", errorResult.getErrorMessage());

        final LocatorProbeResult unsupportedResult = LocatorProbeResult.unsupported("#btn");
        Assertions.assertFalse(unsupportedResult.isSupported());
    }
}
