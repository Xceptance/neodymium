/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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
package com.xceptance.aura.report.dto.widget;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WidgetTrendLayerDto}.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public class WidgetTrendLayerDtoTest
{
    @Test
    public void testConstructorsAndDefaults()
    {
        final WidgetTrendLayerDto layer = new WidgetTrendLayerDto("Passed Clean", "var(--status-pass)", "M 0 0 L 10 10 Z", "M 0 0 L 10 10", 5, "");

        Assertions.assertEquals("Passed Clean", layer.getName());
        Assertions.assertEquals("var(--status-pass)", layer.getColor());
        Assertions.assertEquals("M 0 0 L 10 10 Z", layer.getSvgAreaPath());
        Assertions.assertEquals("M 0 0 L 10 10", layer.getSvgLinePath());
        Assertions.assertEquals(5, layer.getCount());
        Assertions.assertEquals("", layer.getFormattedValue());
        Assertions.assertTrue(layer.hasPath());
        Assertions.assertTrue(layer.hasLinePath());
        Assertions.assertFalse(layer.hasPattern());
    }

    @Test
    public void testNullSafety()
    {
        final WidgetTrendLayerDto layer = new WidgetTrendLayerDto(null, null, null, null, -3, null, null, null);

        Assertions.assertEquals("", layer.getName());
        Assertions.assertEquals("#3b82f6", layer.getColor());
        Assertions.assertEquals("", layer.getSvgAreaPath());
        Assertions.assertEquals("", layer.getSvgLinePath());
        Assertions.assertEquals(0, layer.getCount());
        Assertions.assertEquals("", layer.getFormattedValue());
        Assertions.assertFalse(layer.hasPath());
        Assertions.assertFalse(layer.hasLinePath());
        Assertions.assertFalse(layer.hasPattern());
    }
}
