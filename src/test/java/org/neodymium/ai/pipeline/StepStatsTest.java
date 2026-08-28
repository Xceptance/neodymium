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
package org.neodymium.ai.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link StepStats} validating context level deduplication,
 * timing, and metric aggregation.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class StepStatsTest
{
    @Test
    public void testAddContextLevelDeduplicatesConsecutiveEntries()
    {
        final StepStats stats = new StepStats("Click button", System.currentTimeMillis());

        stats.addContextLevel("LEAN");
        stats.addContextLevel("LEAN");
        stats.addContextLevel("STANDARD");
        stats.addContextLevel("STANDARD");
        stats.addContextLevel("RICH");

        final List<String> levels = stats.getContextLevels();
        assertEquals(3, levels.size());
        assertEquals("LEAN", levels.get(0));
        assertEquals("STANDARD", levels.get(1));
        assertEquals("RICH", levels.get(2));
    }

    @Test
    public void testAddContextLevelHandlesNull()
    {
        final StepStats stats = new StepStats("Click button", System.currentTimeMillis());

        stats.addContextLevel(null);
        assertTrue(stats.getContextLevels().isEmpty());
    }

    @Test
    public void testAddRcaCallAggregatesMetricsAndFlagsExecuted()
    {
        final StepStats stats = new StepStats("Verify product grid", System.currentTimeMillis());
        assertEquals(0, stats.getRcaCalls());
        assertEquals(0, stats.getRcaInputTokens());
        assertEquals(0, stats.getRcaOutputTokens());
        assertEquals(0, stats.getRcaCachedTokens());

        stats.addRcaCall(1200, 300, 400);
        assertEquals(1, stats.getRcaCalls());
        assertEquals(1200, stats.getRcaInputTokens());
        assertEquals(300, stats.getRcaOutputTokens());
        assertEquals(400, stats.getRcaCachedTokens());
        assertTrue(stats.isExecuted());

        stats.addRcaCall(800, 150, 100);
        assertEquals(2, stats.getRcaCalls());
        assertEquals(2000, stats.getRcaInputTokens());
        assertEquals(450, stats.getRcaOutputTokens());
        assertEquals(500, stats.getRcaCachedTokens());
    }
}
