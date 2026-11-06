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
package com.xceptance.aura.mcp.service;

import com.xceptance.aura.mcp.dto.DistilledFailureDto;
import com.xceptance.aura.mcp.dto.RunStatusResponseDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for AdaptivePayloadReporter 3-tier output formatting.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class AdaptivePayloadReporterTest
{
    private AdaptivePayloadReporter reporter;

    @BeforeEach
    public void setUp()
    {
        reporter = new AdaptivePayloadReporter();
    }

    private DistilledFailureDto createFailure(final String testName, final String category, final String locator)
    {
        final DistilledFailureDto dto = new DistilledFailureDto();
        dto.setTestName(testName);
        dto.setClusterCategory(category);
        dto.setLocator(locator);
        dto.setCleanedStackTrace("at com.xceptance.test()");
        return dto;
    }

    @Test
    public void testTier1InlineFailuresWhenFiveOrFewer()
    {
        final List<DistilledFailureDto> failures = List.of(
            createFailure("Test1", "ELEMENT_NOT_FOUND", "button#a"),
            createFailure("Test2", "TIMEOUT", "input#b")
        );

        final RunStatusResponseDto dto = reporter.buildReport(
            "run_001", "FAILED", 2, 0, 2, 0, 5000L, failures
        );

        Assertions.assertEquals("run_001", dto.getRunId());
        Assertions.assertEquals("FAILED", dto.getStatus());
        Assertions.assertEquals(2, dto.getMetrics().getTotal());
        Assertions.assertEquals(5L, dto.getMetrics().getDuration().getTotalSeconds());
        Assertions.assertEquals("5s", dto.getMetrics().getDuration().getFormatted());
        Assertions.assertEquals(2, dto.getInlineFailures().size(), "Tier 1: Up to 5 failures returned inline");
        Assertions.assertNull(dto.getFailureClusters(), "Clusters should be omitted in Tier 1");
    }

    @Test
    public void testTier2ClusteredFailuresWhenMoreThanFiveFailuresAndFiveOrFewerClusters()
    {
        final List<DistilledFailureDto> failures = new ArrayList<>();
        // 6 failures across 2 categories
        for (int i = 0; i < 4; i++)
        {
            failures.add(createFailure("TestElement" + i, "ELEMENT_NOT_FOUND", "button#a"));
        }
        for (int i = 0; i < 2; i++)
        {
            failures.add(createFailure("TestTimeout" + i, "TIMEOUT", "input#b"));
        }

        final RunStatusResponseDto dto = reporter.buildReport(
            "run_002", "FAILED", 6, 0, 6, 0, 12000L, failures
        );

        Assertions.assertNull(dto.getInlineFailures(), "Tier 2: Detailed failures omitted from top envelope");
        Assertions.assertEquals(2, dto.getFailureClusters().size(), "Tier 2: 2 clusters returned");
        Assertions.assertTrue(dto.getNextSteps().contains("get_execution_failure"));
        Assertions.assertTrue(dto.getNextSteps().contains("get_run_failures"));
    }

    @Test
    public void testClusteringByRootCategoryPreventsEntropyExplosion()
    {
        final List<DistilledFailureDto> failures = new ArrayList<>();
        // 6 failures with same root category ELEMENT_NOT_FOUND but different locators
        for (int i = 0; i < 6; i++)
        {
            failures.add(createFailure("TestElement" + i, "ELEMENT_NOT_FOUND: button#" + i, "button#" + i));
        }

        final RunStatusResponseDto dto = reporter.buildReport(
            "run_cluster_root", "FAILED", 6, 0, 6, 0, 10000L, failures
        );

        Assertions.assertNull(dto.getHighEntropyNotice(), "Should NOT trip high-entropy notice when root cause is uniform");
        Assertions.assertNotNull(dto.getFailureClusters());
        Assertions.assertEquals(1, dto.getFailureClusters().size(), "Must cluster all 6 failures under single root category");
        Assertions.assertEquals("ELEMENT_NOT_FOUND", dto.getFailureClusters().getFirst().getCategory());
        Assertions.assertEquals(6, dto.getFailureClusters().getFirst().getCount());
    }

    @Test
    public void testTier3HighEntropySpilloverWhenMoreThanFiveClusters()
    {
        final List<DistilledFailureDto> failures = List.of(
            createFailure("T1", "CAT_1", "l1"),
            createFailure("T2", "CAT_2", "l2"),
            createFailure("T3", "CAT_3", "l3"),
            createFailure("T4", "CAT_4", "l4"),
            createFailure("T5", "CAT_5", "l5"),
            createFailure("T6", "CAT_6", "l6")
        );

        final RunStatusResponseDto dto = reporter.buildReport(
            "run_003", "FAILED", 6, 0, 6, 0, 15000L, failures
        );

        Assertions.assertNull(dto.getInlineFailures(), "Tier 3: Inline failures omitted");
        Assertions.assertNull(dto.getFailureClusters(), "Tier 3: Clusters omitted due to high entropy");
        Assertions.assertTrue(dto.getSummary().contains("6 failures detected across 6 distinct root causes"));
        Assertions.assertNotNull(dto.getHighEntropyNotice());
        Assertions.assertEquals(6, dto.getHighEntropyNotice().getClusterCount());
        Assertions.assertTrue(dto.getNextSteps().contains("get_run_failures(runId=\"run_003\", cursor=\"0\")"));
    }

    @Test
    public void testSubSecondAndZeroDuration()
    {
        final RunStatusResponseDto zeroDurationDto = reporter.buildReport(
            "run_zero", "PASSED", 1, 1, 0, 0, 0L, List.of()
        );
        Assertions.assertEquals(0L, zeroDurationDto.getMetrics().getDuration().getTotalSeconds());
        Assertions.assertEquals("0s", zeroDurationDto.getMetrics().getDuration().getFormatted());

        final RunStatusResponseDto subSecondDto = reporter.buildReport(
            "run_sub", "PASSED", 1, 1, 0, 0, 450L, List.of()
        );
        Assertions.assertEquals(0L, subSecondDto.getMetrics().getDuration().getTotalSeconds());
        Assertions.assertEquals("450ms", subSecondDto.getMetrics().getDuration().getFormatted());
    }

    @Test
    public void testMultiMinuteDuration()
    {
        final RunStatusResponseDto multiMinuteDto = reporter.buildReport(
            "run_long", "PASSED", 10, 10, 0, 0, 125000L, List.of()
        );
        Assertions.assertEquals(125L, multiMinuteDto.getMetrics().getDuration().getTotalSeconds());
        Assertions.assertEquals("2m 05s", multiMinuteDto.getMetrics().getDuration().getFormatted());
    }
}

