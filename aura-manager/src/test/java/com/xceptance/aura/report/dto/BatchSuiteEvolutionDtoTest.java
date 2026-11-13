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

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BatchSuiteEvolutionDto} and {@link BatchSuiteEvolutionPointDto}.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public class BatchSuiteEvolutionDtoTest
{
    @Test
    public void testEmptyEvolution()
    {
        final BatchSuiteEvolutionDto dto = new BatchSuiteEvolutionDto(Collections.emptyList());

        Assertions.assertTrue(dto.getPoints().isEmpty());
        Assertions.assertEquals(0, dto.getInitialTestsCount());
        Assertions.assertEquals(0, dto.getFinalTestsCount());
        Assertions.assertEquals(0, dto.getNetTestsGrowth());
        Assertions.assertEquals(0.0, dto.getInitialPassRate());
        Assertions.assertEquals(0.0, dto.getFinalPassRate());
        Assertions.assertEquals(0.0, dto.getPassRateDelta());
        Assertions.assertTrue(dto.isPassRatePositive());
        Assertions.assertFalse(dto.hasEvolution());
        Assertions.assertFalse(dto.hasPoints());
        Assertions.assertEquals("0 new", dto.getGrowthLabel());
        Assertions.assertEquals("+0.0%", dto.getPassRateDeltaFormatted());
        Assertions.assertEquals("[]", dto.getPointsJson());
    }

    @Test
    public void testSinglePointEvolution()
    {
        final BatchSuiteEvolutionPointDto point = new BatchSuiteEvolutionPointDto(
            "run_1", 0, "2026-10-28 10:00", 10, 10,
            8, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 90.0
        );
        final BatchSuiteEvolutionDto dto = new BatchSuiteEvolutionDto(List.of(point));

        Assertions.assertEquals(1, dto.getPoints().size());
        Assertions.assertEquals(10, dto.getInitialTestsCount());
        Assertions.assertEquals(10, dto.getFinalTestsCount());
        Assertions.assertEquals(0, dto.getNetTestsGrowth());
        Assertions.assertEquals(90.0, dto.getInitialPassRate());
        Assertions.assertEquals(90.0, dto.getFinalPassRate());
        Assertions.assertEquals(0.0, dto.getPassRateDelta());
        Assertions.assertTrue(dto.isPassRatePositive());
        Assertions.assertFalse(dto.hasEvolution());
        Assertions.assertTrue(dto.hasPoints());
        Assertions.assertEquals("0 new", dto.getGrowthLabel());
        Assertions.assertEquals("90.0%", point.getPassRateFormatted());
        Assertions.assertEquals(9, point.getPassedTotalCount());
        Assertions.assertEquals(1, point.getUnknownTotalCount());
    }

    @Test
    public void testMultiPointEvolutionGrowthAndRegression()
    {
        final BatchSuiteEvolutionPointDto p1 = new BatchSuiteEvolutionPointDto(
            "run_1", 0, "2026-10-26 10:00", 10, 10,
            10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 100.0
        );
        final BatchSuiteEvolutionPointDto p2 = new BatchSuiteEvolutionPointDto(
            "run_2", 1, "2026-10-27 10:00", 18, 8,
            15, 1, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 88.9
        );
        final BatchSuiteEvolutionPointDto p3 = new BatchSuiteEvolutionPointDto(
            "run_3", 2, "2026-10-28 10:00", 25, 7,
            20, 3, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 96.0
        );

        final BatchSuiteEvolutionDto dto = new BatchSuiteEvolutionDto(List.of(p1, p2, p3));

        Assertions.assertEquals(3, dto.getPoints().size());
        Assertions.assertEquals(10, dto.getInitialTestsCount());
        Assertions.assertEquals(25, dto.getFinalTestsCount());
        Assertions.assertEquals(15, dto.getNetTestsGrowth());
        Assertions.assertEquals(100.0, dto.getInitialPassRate());
        Assertions.assertEquals(96.0, dto.getFinalPassRate());
        Assertions.assertEquals(-4.0, dto.getPassRateDelta(), 0.001);
        Assertions.assertFalse(dto.isPassRatePositive());
        Assertions.assertTrue(dto.hasEvolution());
        Assertions.assertTrue(dto.hasPoints());
        Assertions.assertEquals("+15 tests", dto.getGrowthLabel());
        Assertions.assertEquals("-4.0%", dto.getPassRateDeltaFormatted());

        final String json = dto.getPointsJson();
        Assertions.assertTrue(json.contains("\"runId\":\"run_1\""));
        Assertions.assertTrue(json.contains("\"totalTestsKnown\":25"));
        Assertions.assertTrue(json.contains("\"newTestsCount\":7"));
    }

    @Test
    public void testActiveRunIdDefaultAndHistorical()
    {
        final BatchSuiteEvolutionPointDto p1 = new BatchSuiteEvolutionPointDto(
            "run_1", 0, "2026-10-26 10:00", 10, 10,
            10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 100.0
        );
        final BatchSuiteEvolutionPointDto p2 = new BatchSuiteEvolutionPointDto(
            "run_2", 1, "2026-10-27 10:00", 18, 8,
            15, 1, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 88.9
        );
        final BatchSuiteEvolutionPointDto p3 = new BatchSuiteEvolutionPointDto(
            "run_3", 2, "2026-10-28 10:00", 25, 7,
            20, 3, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 96.0
        );

        // Default constructor sets activeRunId to the latest run (run_3)
        final BatchSuiteEvolutionDto defaultDto = new BatchSuiteEvolutionDto(List.of(p1, p2, p3));
        Assertions.assertEquals("run_3", defaultDto.getActiveRunId());
        Assertions.assertFalse(defaultDto.isHistoricalActive());
        Assertions.assertEquals("run_3", defaultDto.getActivePoint().getRunId());

        // Overloaded constructor with historical activeRunId (run_2)
        final BatchSuiteEvolutionDto historicalDto = new BatchSuiteEvolutionDto(List.of(p1, p2, p3), "run_2");
        Assertions.assertEquals("run_2", historicalDto.getActiveRunId());
        Assertions.assertTrue(historicalDto.isHistoricalActive());
        Assertions.assertEquals("run_2", historicalDto.getActivePoint().getRunId());
        Assertions.assertEquals(18, historicalDto.getActivePoint().getTotalTestsKnown());
    }

    @Test
    public void testAiUsageStrictExcludesPlaybookReplays()
    {
        // 100 tests: 8 clean classic, 1 healed, 1 AI-driven, 90 playbooks
        final BatchSuiteEvolutionPointDto point = new BatchSuiteEvolutionPointDto(
            "run_ai", 0, "2026-11-05 10:00", 100, 100,
            8, 1, 1,
            0, 0, 0,
            0, 0, 0,
            0, 0, 0,
            0, 90, 10.0, 0
        );

        Assertions.assertEquals(1, point.getTotalAiCount());
        Assertions.assertEquals(1, point.getTotalHealedCount());
        Assertions.assertEquals(90, point.getPlaybookCount());
        Assertions.assertEquals(8, point.getClassicCount());
        Assertions.assertEquals(2, point.getTotalAiUsageCount(), "AI usage must strictly be AI-driven + Healed, excluding Playbook replays");
        Assertions.assertEquals(2.0, point.getAiUsageRate(), 0.001, "AI usage rate must be 2.0% (2 out of 100 tests)");
    }
}
