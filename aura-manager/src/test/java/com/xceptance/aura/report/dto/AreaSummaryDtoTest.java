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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit tests verifying granular per-status healing aggregation and total consistency
 * across {@link AreaSummaryDto}, {@link TestClassSummaryDto}, and {@link TestExecutionDto}.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public class AreaSummaryDtoTest
{
    @Test
    public void testGranularHealingPerStatusInExecutionsAndSummaries()
    {
        // 1. Passed + Healed
        final TestExecutionDto exec1 = new TestExecutionDto();
        exec1.setStatus("passed");
        exec1.setHealed(true);

        // 2. Succeeded-Fixed (clean, not healed)
        final TestExecutionDto exec2 = new TestExecutionDto();
        exec2.setStatus("succeeded-fixed");
        exec2.setHealed(false);

        // 3. Succeeded-Fixed + Healed
        final TestExecutionDto exec3 = new TestExecutionDto();
        exec3.setStatus("succeeded-fixed");
        exec3.setHealed(true);

        // 4. Known Failed + Healed
        final TestExecutionDto exec4 = new TestExecutionDto();
        exec4.setStatus("failed-known");
        exec4.setHealed(true);

        // 5. Unknown Failed + Clean
        final TestExecutionDto exec5 = new TestExecutionDto();
        exec5.setStatus("failed-unknown");
        exec5.setHealed(false);

        // Verify TestExecutionDto predicates
        assertTrue(exec1.isPassHealed());
        assertFalse(exec1.isFixedHealed());
        assertTrue(exec1.isPassedHealed());

        assertFalse(exec2.isFixedHealed());
        assertFalse(exec2.isPassedHealed());

        assertTrue(exec3.isFixedHealed());
        assertFalse(exec3.isPassHealed());
        assertTrue(exec3.isPassedHealed());

        assertTrue(exec4.isKnownHealed());
        assertFalse(exec4.isUnknownHealed());
        assertTrue(exec4.isFailedHealed());

        assertFalse(exec5.isUnknownHealed());
        assertFalse(exec5.isFailedHealed());

        // TestClassSummaryDto aggregation
        final TestClassSummaryDto classSummary = new TestClassSummaryDto(
            "BrowsingTest",
            "class-browsing",
            "Browsing",
            5,
            1,
            2,
            1,
            1,
            0,
            0,
            List.of(exec1, exec2, exec3, exec4, exec5)
        );

        assertEquals(1, classSummary.getPassHealedCount());
        assertEquals(1, classSummary.getFixedHealedCount());
        assertEquals(2, classSummary.getPassedHealedCount());
        assertEquals(1, classSummary.getKnownHealedCount());
        assertEquals(0, classSummary.getUnknownHealedCount());
        assertEquals(1, classSummary.getFailedHealedCount());

        // AreaSummaryDto aggregation
        final AreaSummaryDto areaSummary = new AreaSummaryDto(
            "Browsing",
            "area-browsing",
            "Browsing (default)",
            5,
            1,
            2,
            1,
            1,
            0,
            0,
            List.of(classSummary)
        );

        assertEquals(1, areaSummary.getPassHealedCount());
        assertEquals(1, areaSummary.getFixedHealedCount());
        assertEquals(2, areaSummary.getPassedHealedCount());
        assertEquals(1, areaSummary.getKnownHealedCount());
        assertEquals(0, areaSummary.getUnknownHealedCount());
        assertEquals(1, areaSummary.getFailedHealedCount());

        // Verify total arithmetic: top-level statuses must sum to totalCount
        final int sumOfStatuses = areaSummary.getPassCount()
            + areaSummary.getFixedCount()
            + areaSummary.getKnownCount()
            + areaSummary.getUnknownCount()
            + areaSummary.getIgnoredCount()
            + areaSummary.getRunningCount();

        assertEquals(areaSummary.getTotalCount(), sumOfStatuses, "Top-level status counts must sum to totalCount");
    }

    @Test
    public void testTwoExecutionsOnePassedHealedOneSucceededFixedClean()
    {
        // Matches user's exact case: 2 tests total
        final TestExecutionDto exec1 = new TestExecutionDto();
        exec1.setStatus("passed");
        exec1.setHealed(true);

        final TestExecutionDto exec2 = new TestExecutionDto();
        exec2.setStatus("succeeded-fixed");
        exec2.setHealed(false);

        final TestClassSummaryDto classSummary = new TestClassSummaryDto(
            "QuickCheckoutTest",
            "class-qc",
            "Checkout",
            2,
            1,
            1,
            0,
            0,
            0,
            0,
            List.of(exec1, exec2)
        );

        final AreaSummaryDto areaSummary = new AreaSummaryDto(
            "Checkout",
            "area-checkout",
            "Checkout",
            2,
            1,
            1,
            0,
            0,
            0,
            0,
            List.of(classSummary)
        );

        assertEquals(2, areaSummary.getTotalCount());
        assertEquals(1, areaSummary.getPassCount());
        assertEquals(1, areaSummary.getPassHealedCount());
        assertEquals(1, areaSummary.getFixedCount());
        assertEquals(0, areaSummary.getFixedHealedCount());

        // Status sum is exactly 2, not 3
        assertEquals(2, areaSummary.getPassCount() + areaSummary.getFixedCount());

        // Clean vs Healed counts
        assertEquals(0, areaSummary.getPassCleanCount());
        assertEquals(1, areaSummary.getPassHealedCount());
        assertEquals(1, areaSummary.getFixedCleanCount());
        assertEquals(0, areaSummary.getFixedHealedCount());

        // Dash calculations
        assertEquals("0.00 100.00", areaSummary.getPassCleanDashArray());
        assertEquals("50.00 50.00", areaSummary.getPassHealedDashArray());
        assertEquals("25.00", areaSummary.getPassHealedDashOffset());
        assertEquals("50.00 50.00", areaSummary.getFixedCleanDashArray());
    }

    @Test
    public void testAiDrivenTestsAggregationAndDashCalculations()
    {
        // 1. Pass clean (not healed, not AI)
        final TestExecutionDto exec1 = new TestExecutionDto();
        exec1.setStatus("passed");
        exec1.setExecutionMode("REPLAY_STRICT");

        // 2. Pass AI
        final TestExecutionDto exec2 = new TestExecutionDto();
        exec2.setStatus("passed");
        exec2.setExecutionMode("LLM_RECORDING");

        // 3. Fixed AI
        final TestExecutionDto exec3 = new TestExecutionDto();
        exec3.setStatus("succeeded-fixed");
        exec3.setExecutionMode("FORCE_RECORDING");

        // 4. Unknown AI
        final TestExecutionDto exec4 = new TestExecutionDto();
        exec4.setStatus("failed-unknown");
        exec4.setExecutionMode("LLM_ONLY");

        final TestClassSummaryDto classSummary = new TestClassSummaryDto(
            "AiDrivenTestClass",
            "class-ai",
            "Checkout",
            4,
            2,
            1,
            0,
            1,
            0,
            0,
            List.of(exec1, exec2, exec3, exec4)
        );

        assertEquals(1, classSummary.getPassAiCount());
        assertEquals(1, classSummary.getFixedAiCount());
        assertEquals(0, classSummary.getKnownAiCount());
        assertEquals(1, classSummary.getUnknownAiCount());

        final AreaSummaryDto areaSummary = new AreaSummaryDto(
            "Checkout",
            "area-checkout",
            "Checkout Area",
            4,
            2,
            1,
            0,
            1,
            0,
            0,
            List.of(classSummary)
        );

        assertEquals(4, areaSummary.getTotalCount());
        assertEquals(2, areaSummary.getPassCount());
        assertEquals(1, areaSummary.getPassCleanCount());
        assertEquals(1, areaSummary.getPassAiCount());
        assertEquals(0, areaSummary.getPassHealedCount());

        assertEquals(1, areaSummary.getFixedCount());
        assertEquals(0, areaSummary.getFixedCleanCount());
        assertEquals(1, areaSummary.getFixedAiCount());

        assertEquals(1, areaSummary.getUnknownCount());
        assertEquals(0, areaSummary.getUnknownCleanCount());
        assertEquals(1, areaSummary.getUnknownAiCount());

        // Pass clean: 1/4 = 25%
        assertEquals("25.00 75.00", areaSummary.getPassCleanDashArray());
        assertEquals("25", areaSummary.getPassCleanDashOffset());

        // Pass AI: 1/4 = 25%, offset = 25 - 25 = 0
        assertEquals("25.00 75.00", areaSummary.getPassAiDashArray());
        assertEquals("0.00", areaSummary.getPassAiDashOffset());

        // Fixed AI: 1/4 = 25%, offset = 25 - 50 - 0 = -25
        assertEquals("25.00 75.00", areaSummary.getFixedAiDashArray());
        assertEquals("-25.00", areaSummary.getFixedAiDashOffset());
    }
}
