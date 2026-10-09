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

import org.junit.jupiter.api.Test;

/**
 * Unit tests verifying AI metrics, clean counts, and SVG dash calculations in {@link BatchSummaryDto}.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class BatchSummaryDtoTest
{
    @Test
    public void testAiMetricsAndDashCalculations()
    {
        final BatchSummaryDto batch = new BatchSummaryDto(
            "batch-1",
            "Checkout Batch",
            "Staging",
            "Test Description",
            "10:00",
            "100% Pass",
            "",
            java.util.List.of("en_US"),
            java.util.List.of("Chrome"),
            "run-100",
            4,
            2,
            2,
            2,
            0,
            10,
            1,
            1,
            0,
            0,
            1,
            0,
            1,
            1
        );

        assertEquals("batch-1", batch.getId());
        assertEquals("Checkout Batch", batch.getName());
        assertEquals(10, batch.getLatestTotalTests());
        assertEquals(4, batch.getLatestPassedCount());
        assertEquals(2, batch.getLatestFixedCount());
        assertEquals(2, batch.getLatestKnownCount());
        assertEquals(2, batch.getLatestUnknownCount());

        // Healed counts
        assertEquals(1, batch.getLatestPassHealedCount());
        assertEquals(1, batch.getLatestFixedHealedCount());

        // AI counts
        assertEquals(1, batch.getLatestPassAiCount());
        assertEquals(0, batch.getLatestFixedAiCount());
        assertEquals(1, batch.getLatestKnownAiCount());
        assertEquals(1, batch.getLatestUnknownAiCount());

        // Clean counts: clean = total - healed - ai
        // Passed clean: 4 - 1 (healed) - 1 (AI) = 2
        assertEquals(2, batch.getLatestPassCleanCount());
        // Fixed clean: 2 - 1 (healed) - 0 (AI) = 1
        assertEquals(1, batch.getLatestFixedCleanCount());
        // Known clean: 2 - 0 (healed) - 1 (AI) = 1
        assertEquals(1, batch.getLatestKnownCleanCount());
        // Unknown clean: 2 - 0 (healed) - 1 (AI) = 1
        assertEquals(1, batch.getLatestUnknownCleanCount());

        // Percentages (total = 10)
        assertEquals(20.0, batch.getLatestPassCleanPct());
        assertEquals(10.0, batch.getLatestPassHealedPct());
        assertEquals(10.0, batch.getLatestPassAiPct());

        // SVG Dash array & offsets
        assertEquals("20.00 80.00", batch.getPassCleanDashArray());
        assertEquals("25", batch.getPassCleanDashOffset());

        assertEquals("10.00 90.00", batch.getPassHealedDashArray());
        assertEquals("5.00", batch.getPassHealedDashOffset()); // 25 - 20 = 5

        assertEquals("10.00 90.00", batch.getPassAiDashArray());
        assertEquals("-5.00", batch.getPassAiDashOffset()); // 25 - 20 - 10 = -5
    }
}
