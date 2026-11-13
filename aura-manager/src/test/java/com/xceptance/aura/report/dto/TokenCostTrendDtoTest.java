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
package com.xceptance.aura.report.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests verifying cost formatting, percentage delta, and SVG sparkline path generation in {@link TokenCostTrendDto}.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public class TokenCostTrendDtoTest
{
    @Test
    public void testEmptyAndSinglePointTrend()
    {
        final TokenCostTrendDto emptyDto = new TokenCostTrendDto(0.0, -1.0, 0L, Collections.emptyList());
        assertEquals(0.0, emptyDto.getCurrentCost());
        assertEquals("$0.0000", emptyDto.getCurrentCostFormatted());
        assertEquals("0 tokens", emptyDto.getTotalTokensFormatted());
        assertEquals("+$0.0000 vs prev day", emptyDto.getDeltaLabel());
        assertTrue(emptyDto.isPositive());
        assertEquals("M 4.0 16.0 L 116.0 16.0", emptyDto.getSparklineSvgPath());

        final TokenCostTrendDto singleDto = new TokenCostTrendDto(0.0042, 0.0, 1200L, List.of(0.0042));
        assertEquals("$0.0042", singleDto.getCurrentCostFormatted());
        assertEquals("1,200 tokens", singleDto.getTotalTokensFormatted());
        assertEquals("M 4.0 16.0 L 116.0 16.0", singleDto.getSparklineSvgPath());
    }

    @Test
    public void testTwoPointsTrend()
    {
        final TokenCostTrendDto twoPointsDto = new TokenCostTrendDto(0.0050, 0.0025, 3000L, List.of(0.0025, 0.0050));
        assertEquals(0.0050, twoPointsDto.getCurrentCost());
        assertEquals(0.0025, twoPointsDto.getPreviousCost());
        assertEquals(0.0025, twoPointsDto.getDeltaCost());
        assertEquals(100.0, twoPointsDto.getDeltaPercentage(), 0.001);
        assertEquals("$0.0050", twoPointsDto.getCurrentCostFormatted());
        assertEquals("+$0.0025 (100%) vs prev day", twoPointsDto.getDeltaLabel());
        assertTrue(twoPointsDto.isPositive());
        assertNotNull(twoPointsDto.getSparklineSvgPath());
        assertTrue(twoPointsDto.getSparklineSvgPath().startsWith("M 4.0"));
        assertTrue(twoPointsDto.getSparklineSvgPath().contains("L 116.0"));
    }

    @Test
    public void testMultiplePointsCurveAndFormatting()
    {
        final List<Double> history = List.of(0.0010, 0.0015, 0.0012, 0.0020, 0.0018, 0.0025, 0.0030);
        final TokenCostTrendDto multiDto = new TokenCostTrendDto(0.0030, 0.0025, 15420L, history);

        assertEquals("$0.0030", multiDto.getCurrentCostFormatted());
        assertEquals("15,420 tokens", multiDto.getTotalTokensFormatted());
        assertEquals(0.0005, multiDto.getDeltaCost(), 0.00001);
        assertEquals(20.0, multiDto.getDeltaPercentage(), 0.001);
        assertEquals("+$0.0005 (20%) vs prev day", multiDto.getDeltaLabel());
        assertTrue(multiDto.isPositive());

        final String path = multiDto.getSparklineSvgPath();
        assertNotNull(path);
        assertTrue(path.startsWith("M 4.0"));
        assertTrue(path.contains(" C "));
    }

    @Test
    public void testNegativeDeltaAndLargeMagnitudes()
    {
        final TokenCostTrendDto downDto = new TokenCostTrendDto(0.0010, 0.0020, 800L, List.of(0.0030, 0.0020, 0.0010));
        assertEquals(-0.0010, downDto.getDeltaCost(), 0.00001);
        assertEquals(-50.0, downDto.getDeltaPercentage(), 0.001);
        assertFalse(downDto.isPositive());
        assertEquals("-$0.0010 (-50%) vs prev day", downDto.getDeltaLabel());

        final TokenCostTrendDto largeDto = new TokenCostTrendDto(8_420_000.0, 8_208_000.0, 999_999L, List.of(8_000_000.0, 8_208_000.0, 8_420_000.0));
        assertEquals("$8.42M", largeDto.getCurrentCostFormatted());
        assertEquals("+$212.0K (3%) vs prev day", largeDto.getDeltaLabel());
    }

    @Test
    public void testPointGroupsAndJsonSerialization()
    {
        final ContributingRunCostDto r1 = new ContributingRunCostDto("run-1", "2026-10-28 12:00", "QA", "Chrome", "Default", 2, 0.0015, 3000L, 60.0);
        final ContributingRunCostDto r2 = new ContributingRunCostDto("run-2", "2026-10-28 12:05", "QA", "Firefox", "Default", 1, 0.0010, 2000L, 40.0);

        final TokenCostPointGroupDto g1 = new TokenCostPointGroupDto(0, "Point 1 of 2", "run-1", "2026-10-28 12:00", 0.0015, 3000L, 0.0, 0.0, List.of(r1));
        final TokenCostPointGroupDto g2 = new TokenCostPointGroupDto(1, "Point 2 of 2 (Latest)", "run-2", "2026-10-28 12:05", 0.0025, 5000L, 0.0010, 66.67, List.of(r1, r2));

        final TokenCostTrendDto trendDto = new TokenCostTrendDto(0.0025, 0.0015, 5000L, List.of(0.0015, 0.0025), List.of(g1, g2));

        assertEquals(2, trendDto.getPointGroups().size());
        assertEquals("Point 1 of 2", trendDto.getPointGroups().get(0).getPointLabel());
        assertEquals(0.0025, trendDto.getPointGroups().get(1).getTotalCost());
        assertEquals("5,000 tokens", trendDto.getPointGroups().get(1).getTotalTokensFormatted());

        final String json = trendDto.getPointGroupsJson();
        assertNotNull(json);
        assertTrue(json.contains("\"pointIndex\":0"));
        assertTrue(json.contains("\"pointIndex\":1"));
        assertTrue(json.contains("\"runId\":\"run-1\""));
        assertTrue(json.contains("\"costPercentageFormatted\":\"60.0%\""));
    }
}
