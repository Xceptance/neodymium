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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RunReportDto} duration calculations.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class RunReportDtoTest
{
    @Test
    public void testGetTotalDurationMs_emptyExecutions()
    {
        final RunReportDto report = new RunReportDto("run1", "batch1", "2026-08-20", "0s", 0, 0, 0, 0, 0, 0, new ArrayList<>());
        Assertions.assertEquals(0L, report.getTotalDurationMs());
        Assertions.assertEquals("0 min 0 s", report.getTotalDurationFormatted());
    }

    @Test
    public void testGetTotalDurationMs_singleExecution()
    {
        final TestExecutionDto exec = new TestExecutionDto(
            "id1", "run1", "TestClass", "Title", "test1", "playbook", "test.json",
            "passed-clean", "Java", "US", "en_US", "Chrome", "NONE", new ArrayList<>(),
            "", "Browsing", new ArrayList<>(), null, null, null, null,
            "t1", "d1", "RECORDING", "RECORDING", "10:00:00", "2026-08-20",
            "10:00:00", 1000L, 5, 0, 5000L, "5.0s", 0, 0L, 0.0, ""
        );

        final List<TestExecutionDto> execs = new ArrayList<>();
        execs.add(exec);

        final RunReportDto report = new RunReportDto("run1", "batch1", "2026-08-20", "5.0s", 1, 1, 0, 0, 0, 0, execs);
        Assertions.assertEquals(5000L, report.getTotalDurationMs());
        Assertions.assertEquals("5.0s", report.getTotalDurationFormatted());
    }

    @Test
    public void testGetTotalDurationMs_parallelExecutions()
    {
        // Test A: start timestamp = 1000ms, duration = 5000ms
        final TestExecutionDto execA = new TestExecutionDto(
            "id1", "run1", "TestClassA", "TitleA", "testA", "playbookA", "testA.json",
            "passed-clean", "Java", "US", "en_US", "Chrome", "NONE", new ArrayList<>(),
            "", "Browsing", new ArrayList<>(), null, null, null, null,
            "t1", "d1", "RECORDING", "RECORDING", "10:00:00", "2026-08-20",
            "10:00:00", 1000L, 5, 0, 5000L, "5.0s", 0, 0L, 0.0, ""
        );

        // Test B: start timestamp = 2000ms (latest start), duration = 3000ms (latest duration)
        final TestExecutionDto execB = new TestExecutionDto(
            "id2", "run1", "TestClassB", "TitleB", "testB", "playbookB", "testB.json",
            "passed-clean", "Java", "US", "en_US", "Chrome", "NONE", new ArrayList<>(),
            "", "Browsing", new ArrayList<>(), null, null, null, null,
            "t2", "d2", "RECORDING", "RECORDING", "10:00:01", "2026-08-20",
            "10:00:01", 2000L, 5, 0, 3000L, "3.0s", 0, 0L, 0.0, ""
        );

        final List<TestExecutionDto> execs = new ArrayList<>();
        execs.add(execA);
        execs.add(execB);

        final RunReportDto report = new RunReportDto("run1", "batch1", "2026-08-20", "0s", 2, 2, 0, 0, 0, 0, execs);

        // Latest start (2000ms) - Earliest start (1000ms) + Latest start duration (3000ms) = 4000ms
        Assertions.assertEquals(4000L, report.getTotalDurationMs());
        Assertions.assertEquals("0 min 4 s", report.getTotalDurationFormatted());
    }

    @Test
    public void testGetTotalDurationFormatted_usesStoredDurationFromTestRun()
    {
        final TestExecutionDto exec = new TestExecutionDto(
            "id1", "run1", "TestClass", "Title", "test1", "playbook", "test.json",
            "passed-clean", "Java", "US", "en_US", "Chrome", "NONE", new ArrayList<>(),
            "", "Browsing", new ArrayList<>(), null, null, null, null,
            "t1", "d1", "RECORDING", "RECORDING", "10:00:00", "2026-08-20",
            "10:00:00", 0L, 5, 0, 52617L, "52.6s", 0, 0L, 0.0, ""
        );

        final List<TestExecutionDto> execs = List.of(exec);

        // Stored duration from TEST_RUN table is "4 min 20 s" (260,657 ms)
        final RunReportDto report = new RunReportDto("run1", "batch1", "2026-08-20", "4 min 20 s", 1, 1, 0, 0, 0, 0, execs);
        Assertions.assertEquals("4 min 20 s", report.getTotalDurationFormatted());
    }

    @Test
    public void testGetTotalDurationMs_ignoresZeroTimestampAndFallsBackToSum()
    {
        // Execution A: timestampMs = 0L (uninitialized/missing start time), duration = 120,000 ms (2 min)
        final TestExecutionDto execA = new TestExecutionDto(
            "id1", "run1", "TestClassA", "TitleA", "testA", "playbookA", "testA.json",
            "passed-clean", "Java", "US", "en_US", "Chrome", "NONE", new ArrayList<>(),
            "", "Browsing", new ArrayList<>(), null, null, null, null,
            "t1", "d1", "RECORDING", "RECORDING", "", "",
            "", 0L, 5, 0, 120000L, "2 min 0 s", 0, 0L, 0.0, ""
        );

        // Execution B: timestampMs = 1,790,852,520,000L, duration = 180,000 ms (3 min)
        final TestExecutionDto execB = new TestExecutionDto(
            "id2", "run1", "TestClassB", "TitleB", "testB", "playbookB", "testB.json",
            "passed-clean", "Java", "US", "en_US", "Chrome", "NONE", new ArrayList<>(),
            "", "Browsing", new ArrayList<>(), null, null, null, null,
            "t2", "d2", "RECORDING", "RECORDING", "12:56:40", "2026-10-01",
            "12:56:40", 1790852520000L, 5, 0, 180000L, "3 min 0 s", 0, 0L, 0.0, ""
        );

        final List<TestExecutionDto> execs = new ArrayList<>();
        execs.add(execA);
        execs.add(execB);

        final RunReportDto report = new RunReportDto("run_20261001_12564", "batch1", "2026-10-01", "0s", 2, 2, 0, 0, 0, 0, execs);

        // Must NOT produce ~29.8 million minutes (1,790,852,520,000ms - 0ms).
        // Should fall back to sum of durations: 120,000 + 180,000 = 300,000 ms (5 min)
        Assertions.assertEquals(300000L, report.getTotalDurationMs());
        Assertions.assertEquals("5 min 0 s", report.getTotalDurationFormatted());
    }

    @Test
    public void testFilterMethods_emptyExecutions()
    {
        final RunReportDto report = new RunReportDto("run1", "batch1", "2026-08-20", "0s", 0, 0, 0, 0, 0, 0, new ArrayList<>());
        Assertions.assertTrue(report.getFilterModes().isEmpty());
        Assertions.assertTrue(report.getFilterBrowsers().isEmpty());
        Assertions.assertTrue(report.getFilterLocales().isEmpty());
        Assertions.assertTrue(report.getFilterLocations().isEmpty());
    }

    @Test
    public void testFilterMethods_withValidAndFallbackValues()
    {
        final TestExecutionDto exec1 = new TestExecutionDto(
            "id1", "run1", "TestClassA", "TitleA", "testA", "playbookA", "testA.json",
            "passed-clean", "Java", "US", "en_US", "Chrome", "NONE", new ArrayList<>(),
            "", "Browsing", new ArrayList<>(), null, null, null, null,
            "t1", "d1", "RECORDING", "RECORDING", "10:00:00", "2026-08-20",
            "10:00:00", 1000L, 5, 0, 5000L, "5.0s", 0, 0L, 0.0, ""
        );
        final TestExecutionDto exec2 = new TestExecutionDto(
            "id2", "run1", "TestClassB", "TitleB", "testB", "playbookB", "testB.json",
            "passed-clean", "Java", null, "", "Firefox", "NONE", new ArrayList<>(),
            "", "Browsing", new ArrayList<>(), null, null, null, null,
            "t2", "d2", "HEALING", "HEALING", "10:00:01", "2026-08-20",
            "10:00:01", 2000L, 5, 0, 3000L, "3.0s", 0, 0L, 0.0, ""
        );

        final List<TestExecutionDto> execs = List.of(exec1, exec2);
        final RunReportDto report = new RunReportDto("run1", "batch1", "2026-08-20", "8.0s", 2, 2, 0, 0, 0, 0, execs);

        Assertions.assertEquals(List.of("HEALING", "RECORDING"), report.getFilterModes());
        Assertions.assertEquals(List.of("Chrome", "Firefox"), report.getFilterBrowsers());
        Assertions.assertEquals(List.of("Default", "en_US"), report.getFilterLocales());
        Assertions.assertEquals(List.of("Default", "en_US"), report.getFilterLocations());
    }

    @Test
    public void testPassRateAndCleanCountCalculations()
    {
        final RunReportDto emptyReport = new RunReportDto("run0", "batch0", "2026-08-20", "0s", 0, 0, 0, 0, 0, 0, List.of());
        Assertions.assertEquals("0%", emptyReport.getPassRateInt());
        Assertions.assertEquals("N/A", emptyReport.getPassRateText());

        // 3 executions: 2 passed (clean), 1 unknown fail
        final RunReportDto report = new RunReportDto("run1", "batch1", "2026-08-20", "8m 34s", 3, 2, 0, 0, 1, 0, List.of());
        Assertions.assertEquals("67%", report.getPassRateInt());
        Assertions.assertEquals("67% Pass", report.getPassRateText());
        Assertions.assertEquals(2, report.getPassCleanCount());
        Assertions.assertEquals(1, report.getUnknownCleanCount());
        Assertions.assertEquals(0, report.getFixedCleanCount());
        Assertions.assertEquals(0, report.getKnownCleanCount());

        // Dash arrays & offsets
        Assertions.assertEquals("25", report.getPassCleanDashOffset());
        Assertions.assertTrue(report.getPassCleanDashArray().startsWith("66.67"));
        Assertions.assertTrue(report.getUnknownCleanDashArray().startsWith("33.33"));
    }

    @Test
    public void testBrowserAndLocaleRatios()
    {
        final TestExecutionDto exec1 = new TestExecutionDto(
            "id1", "run1", "TestClassA", "TitleA", "testA", "playbookA", "testA.json",
            "passed-clean", "Java", "US", "en_US", "Chrome", "NONE", new ArrayList<>(),
            "", "Browsing", new ArrayList<>(), null, null, null, null,
            "t1", "d1", "RECORDING", "RECORDING", "10:00:00", "2026-08-20",
            "10:00:00", 1000L, 5, 0, 5000L, "5.0s", 0, 0L, 0.0, ""
        );
        final TestExecutionDto exec2 = new TestExecutionDto(
            "id2", "run1", "TestClassB", "TitleB", "testB", "playbookB", "testB.json",
            "passed-clean", "Java", "US", "en_US", "Chrome", "NONE", new ArrayList<>(),
            "", "Browsing", new ArrayList<>(), null, null, null, null,
            "t2", "d2", "RECORDING", "RECORDING", "10:00:01", "2026-08-20",
            "10:00:01", 2000L, 5, 0, 3000L, "3.0s", 0, 0L, 0.0, ""
        );
        final TestExecutionDto exec3 = new TestExecutionDto(
            "id3", "run1", "TestClassC", "TitleC", "testC", "playbookC", "testC.json",
            "failed-unknown", "Java", "US", "en_US", "Firefox", "NONE", new ArrayList<>(),
            "", "Browsing", new ArrayList<>(), null, null, null, null,
            "t3", "d3", "RECORDING", "RECORDING", "10:00:02", "2026-08-20",
            "10:00:02", 3000L, 5, 0, 2000L, "2.0s", 0, 0L, 0.0, ""
        );

        final List<TestExecutionDto> execs = List.of(exec1, exec2, exec3);
        final RunReportDto report = new RunReportDto("run1", "batch1", "2026-08-20", "8m 34s", 3, 2, 0, 0, 1, 0, execs);

        final List<DistributionRatioDto> browserRatios = report.getBrowserRatios();
        Assertions.assertEquals(2, browserRatios.size());
        Assertions.assertEquals("Chrome", browserRatios.get(0).getName());
        Assertions.assertEquals(2, browserRatios.get(0).getCount());
        Assertions.assertEquals("67%", browserRatios.get(0).getFormattedPercentage());
        Assertions.assertEquals("Firefox", browserRatios.get(1).getName());
        Assertions.assertEquals(1, browserRatios.get(1).getCount());
        Assertions.assertEquals("33%", browserRatios.get(1).getFormattedPercentage());

        final List<DistributionRatioDto> localeRatios = report.getLocaleRatios();
        Assertions.assertEquals(1, localeRatios.size());
        Assertions.assertEquals("en_US", localeRatios.get(0).getName());
        Assertions.assertEquals(3, localeRatios.get(0).getCount());
        Assertions.assertEquals("100%", localeRatios.get(0).getFormattedPercentage());
    }

    @Test
    public void testHasRealAreas()
    {
        // 1. Empty area summaries -> false
        final RunReportDto emptyReport = new RunReportDto("run1", "batch1", "2026-11-10", "10s", 0, 0, 0, 0, 0, 0, List.of());
        Assertions.assertFalse(emptyReport.hasRealAreas());

        // 2. Only fallback default area -> false
        final AreaSummaryDto defaultArea = new AreaSummaryDto("@Browsing (default)", "areaBrowsing", List.of(), 1, 0, 0, 0, 0);
        final RunReportDto defaultReport = new RunReportDto("run2", "batch1", "2026-11-10", "10s", 1, 1, 0, 0, 0, 0, 0, List.of(), List.of(defaultArea));
        Assertions.assertFalse(defaultReport.hasRealAreas());

        // 3. General fallback area -> false
        final AreaSummaryDto generalArea = new AreaSummaryDto("General", "areaGeneral", List.of(), 1, 0, 0, 0, 0);
        final RunReportDto generalReport = new RunReportDto("run3", "batch1", "2026-11-10", "10s", 1, 1, 0, 0, 0, 0, 0, List.of(), List.of(generalArea));
        Assertions.assertFalse(generalReport.hasRealAreas());

        // 4. Real custom area -> true
        final AreaSummaryDto realArea = new AreaSummaryDto("@Checkout", "areaCheckout", List.of(), 1, 0, 0, 0, 0);
        final RunReportDto realReport = new RunReportDto("run4", "batch1", "2026-11-10", "10s", 1, 1, 0, 0, 0, 0, 0, List.of(), List.of(realArea));
        Assertions.assertTrue(realReport.hasRealAreas());

        // 5. TestBaseDataDto with only fallback -> false
        final TestBaseDataDto testBaseDefault = new TestBaseDataDto(
            List.of(), List.of(), List.of(),
            List.of(new TestBaseAreaDto("@Browsing (default)", "tb1", "btn1", "folder", 1, List.of())),
            Map.of(), 1, 1
        );
        Assertions.assertFalse(testBaseDefault.hasRealAreas());

        // 6. TestBaseDataDto with real area -> true
        final TestBaseDataDto testBaseReal = new TestBaseDataDto(
            List.of(), List.of(), List.of(),
            List.of(new TestBaseAreaDto("@Search", "tb2", "btn2", "folder", 1, List.of())),
            Map.of(), 1, 1
        );
        Assertions.assertTrue(testBaseReal.hasRealAreas());
    }
}
