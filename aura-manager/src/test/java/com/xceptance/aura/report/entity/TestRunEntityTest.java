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
package com.xceptance.aura.report.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests verifying AI metrics, clean subtraction, and status bar label formatting in {@link TestRunEntity}.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class TestRunEntityTest
{
    @Test
    public void testAiMetricsCleanSubtractionAndLabels()
    {
        final TestRunEntity entity = new TestRunEntity();
        entity.setTotalTests(20);
        entity.setPassedCount(10);
        entity.setPassedHealedCount(2);
        entity.setPassedAiCount(3);

        entity.setSucceededFixedCount(4);
        entity.setSucceededFixedHealedCount(1);
        entity.setSucceededFixedAiCount(2);

        entity.setFailedKnownCount(3);
        entity.setFailedKnownHealedCount(1);
        entity.setFailedKnownAiCount(1);

        entity.setFailedUnknownCount(3);
        entity.setFailedUnknownHealedCount(0);
        entity.setFailedUnknownAiCount(2);

        // Safe getters
        assertEquals(3, entity.getPassedAiCountSafe());
        assertEquals(2, entity.getFixedAiCountSafe());
        assertEquals(1, entity.getKnownAiCountSafe());
        assertEquals(2, entity.getUnknownAiCountSafe());

        // Clean count subtraction: clean = total - healed - ai
        // Passed: 10 - 2 - 3 = 5
        assertEquals(5, entity.getPassedCleanCount());
        // Fixed: 4 - 1 - 2 = 1
        assertEquals(1, entity.getFixedCleanCount());
        // Known: 3 - 1 - 1 = 1
        assertEquals(1, entity.getKnownCleanCount());
        // Unknown: 3 - 0 - 2 = 1
        assertEquals(1, entity.getUnknownCleanCount());

        // Percentages (total = 20)
        // Passed clean: 5/20 = 25%
        assertEquals(25.0, entity.getPassedCleanPct());
        // Passed healed: 2/20 = 10%
        assertEquals(10.0, entity.getPassedHealedPct());
        // Passed AI: 3/20 = 15%
        assertEquals(15.0, entity.getPassedAiPct());

        // Labels: >= 7% shows label
        // Passed AI pct is 15% (>= 7%), so label must be "3" (clean number only, NO robot emoji)
        assertEquals("3", entity.getPassedAiLabel());
        assertFalse(entity.getPassedAiLabel().contains("🤖"), "AI label must not contain robot emoji");

        // Tooltips: must mention AI count
        final String passedTooltip = entity.getPassedTooltip();
        assertTrue(passedTooltip.contains("3 AI-driven"), "Tooltip must detail AI tests");
        assertTrue(passedTooltip.contains("2 healed"), "Tooltip must detail healed tests");
        assertTrue(passedTooltip.contains("5 clean"), "Tooltip must detail clean tests");
    }

    @Test
    public void testAiLabelHiddenWhenUnderSevenPercent()
    {
        final TestRunEntity entity = new TestRunEntity();
        entity.setTotalTests(100);
        entity.setPassedAiCount(5); // 5% < 7% threshold

        assertEquals("", entity.getPassedAiLabel(), "Label should be empty string when under 7%");
    }
}
