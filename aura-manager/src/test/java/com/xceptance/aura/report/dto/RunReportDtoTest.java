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
}
