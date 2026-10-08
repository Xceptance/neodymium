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
 * Unit tests verifying granular per-status healing metrics and clean counts in {@link AreaRunPointDto}.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class AreaRunPointDtoTest
{
    @Test
    public void testEightParameterConstructorDefaultsHealedCountsToZero()
    {
        final AreaRunPointDto point = new AreaRunPointDto(
            "run_001",
            "2026-10-08 12:00:00",
            5,
            3,
            2,
            1,
            1,
            12
        );

        assertEquals("run_001", point.getRunId());
        assertEquals("2026-10-08 12:00:00", point.getTimestampLabel());
        assertEquals(5, point.getPassCount());
        assertEquals(3, point.getFixedCount());
        assertEquals(2, point.getKnownCount());
        assertEquals(1, point.getUnknownCount());
        assertEquals(1, point.getIgnoredCount());
        assertEquals(12, point.getTotalCount());

        assertEquals(0, point.getPassHealedCount());
        assertEquals(0, point.getFixedHealedCount());
        assertEquals(0, point.getKnownHealedCount());
        assertEquals(0, point.getUnknownHealedCount());

        assertEquals(5, point.getPassCleanCount());
        assertEquals(3, point.getFixedCleanCount());
        assertEquals(2, point.getKnownCleanCount());
        assertEquals(1, point.getUnknownCleanCount());
    }

    @Test
    public void testTwelveParameterConstructorPreservesGranularHealedCountsAndCalculatesCleanMetrics()
    {
        final AreaRunPointDto point = new AreaRunPointDto(
            "run_002",
            "2026-10-08 14:00:00",
            8,
            4,
            3,
            2,
            0,
            17,
            3,
            2,
            1,
            1
        );

        assertEquals("run_002", point.getRunId());
        assertEquals("2026-10-08 14:00:00", point.getTimestampLabel());
        assertEquals(8, point.getPassCount());
        assertEquals(4, point.getFixedCount());
        assertEquals(3, point.getKnownCount());
        assertEquals(2, point.getUnknownCount());
        assertEquals(0, point.getIgnoredCount());
        assertEquals(17, point.getTotalCount());

        assertEquals(3, point.getPassHealedCount());
        assertEquals(2, point.getFixedHealedCount());
        assertEquals(1, point.getKnownHealedCount());
        assertEquals(1, point.getUnknownHealedCount());

        assertEquals(5, point.getPassCleanCount());
        assertEquals(2, point.getFixedCleanCount());
        assertEquals(2, point.getKnownCleanCount());
        assertEquals(1, point.getUnknownCleanCount());
    }
}
