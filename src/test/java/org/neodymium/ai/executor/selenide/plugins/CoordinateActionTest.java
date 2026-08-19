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
package org.neodymium.ai.executor.selenide.plugins;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.executor.selenide.plugins.ClickAction.CoordinateTarget;

/**
 * Unit tests for coordinate parsing and anchor-relative spatial pinning logic in {@link ClickAction} and {@link TypeAction}.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class CoordinateActionTest
{
    @Test
    public void testParseDirectCoordinates()
    {
        final CoordinateTarget target1 = ClickAction.parseCoordinateTarget("coord:100,200");
        Assertions.assertNotNull(target1);
        Assertions.assertNull(target1.anchorSelector());
        Assertions.assertEquals(100, target1.x());
        Assertions.assertEquals(200, target1.y());

        final CoordinateTarget target2 = ClickAction.parseCoordinateTarget("coordinates: 350 x 420");
        Assertions.assertNotNull(target2);
        Assertions.assertNull(target2.anchorSelector());
        Assertions.assertEquals(350, target2.x());
        Assertions.assertEquals(420, target2.y());

        final CoordinateTarget target3 = ClickAction.parseCoordinateTarget("point(50, 75)");
        Assertions.assertNotNull(target3);
        Assertions.assertNull(target3.anchorSelector());
        Assertions.assertEquals(50, target3.x());
        Assertions.assertEquals(75, target3.y());
    }

    @Test
    public void testParseAnchorRelativeCoordinates()
    {
        final CoordinateTarget target1 = ClickAction.parseCoordinateTarget("coord:#signature-canvas@150,75");
        Assertions.assertNotNull(target1);
        Assertions.assertEquals("#signature-canvas", target1.anchorSelector());
        Assertions.assertEquals(150, target1.x());
        Assertions.assertEquals(75, target1.y());

        final CoordinateTarget target2 = ClickAction.parseCoordinateTarget("coord:form.checkout:20,40");
        Assertions.assertNotNull(target2);
        Assertions.assertEquals("form.checkout", target2.anchorSelector());
        Assertions.assertEquals(20, target2.x());
        Assertions.assertEquals(40, target2.y());

        final CoordinateTarget target3 = ClickAction.parseCoordinateTarget("#canvas@100,200");
        Assertions.assertNotNull(target3);
        Assertions.assertEquals("#canvas", target3.anchorSelector());
        Assertions.assertEquals(100, target3.x());
        Assertions.assertEquals(200, target3.y());
    }

    @Test
    public void testNonCoordinateStringsDoNotMatch()
    {
        Assertions.assertNull(ClickAction.parseCoordinateTarget("10x20"));
        Assertions.assertNull(ClickAction.parseCoordinateTarget("Search 10, 20"));
        Assertions.assertNull(ClickAction.parseCoordinateTarget("#submit-btn"));
        Assertions.assertNull(ClickAction.parseCoordinateTarget("button.btn-primary"));
        Assertions.assertNull(ClickAction.parseCoordinateTarget(null));
        Assertions.assertNull(ClickAction.parseCoordinateTarget(""));
        Assertions.assertNull(ClickAction.parseCoordinateTarget("   "));
    }
}
