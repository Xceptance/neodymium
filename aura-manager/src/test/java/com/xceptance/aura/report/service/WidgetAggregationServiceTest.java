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
package com.xceptance.aura.report.service;

import com.xceptance.aura.report.dto.BatchLatestExecutionsDto;
import com.xceptance.aura.report.dto.BatchLatestVariationDto;
import com.xceptance.aura.report.dto.BatchSuiteEvolutionDto;
import com.xceptance.aura.report.dto.BatchSummaryDto;
import com.xceptance.aura.report.dto.BatchSuiteEvolutionPointDto;
import com.xceptance.aura.report.dto.DistributionRatioDto;
import com.xceptance.aura.report.dto.RunReportDto;
import com.xceptance.aura.report.dto.TagRatioCardDto;
import com.xceptance.aura.report.dto.TestExecutionDto;
import com.xceptance.aura.report.dto.TestBaseAreaDto;
import com.xceptance.aura.report.dto.TestBaseClassDto;
import com.xceptance.aura.report.dto.TokenCostPointGroupDto;
import com.xceptance.aura.report.dto.TokenCostTrendDto;
import com.xceptance.aura.report.dto.widget.DashboardWidgetConfigDto;
import com.xceptance.aura.report.dto.widget.DashboardWidgetViewDto;
import com.xceptance.aura.report.dto.widget.WidgetAxisTickDto;
import com.xceptance.aura.report.dto.widget.PerlenketteBeadDto;
import com.xceptance.aura.report.dto.widget.PerlenketteDto;
import com.xceptance.aura.report.dto.widget.PerlenketteMinorDotDto;
import com.xceptance.aura.report.dto.widget.SparklineGeometryDto;
import com.xceptance.aura.report.dto.widget.WidgetColorPolarity;
import com.xceptance.aura.report.dto.widget.WidgetContributingExecutionsDto;
import com.xceptance.aura.report.dto.widget.WidgetFilterConfigDto;
import com.xceptance.aura.report.dto.widget.WidgetListItemDto;
import com.xceptance.aura.report.dto.widget.WidgetMetric;
import com.xceptance.aura.report.dto.widget.WidgetPieSliceDto;
import com.xceptance.aura.report.dto.widget.WidgetPointScope;
import com.xceptance.aura.report.dto.widget.WidgetPresentation;
import com.xceptance.aura.report.dto.widget.WidgetRunExecutionsGroupDto;
import com.xceptance.aura.report.dto.widget.WidgetTrendLayerDto;
import com.xceptance.aura.report.dto.widget.WidgetTrendMode;
import com.xceptance.aura.report.dto.widget.WidgetTrendPointDto;
import com.xceptance.aura.report.dto.widget.WidgetValueFormat;
import com.xceptance.aura.report.entity.TestBaseBugEntity;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.entity.TestRunTagEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for {@link WidgetAggregationService}.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public class WidgetAggregationServiceTest
{
    private WidgetAggregationService service;

    @BeforeEach
    public void setUp()
    {
        service = new WidgetAggregationService();
    }

    @Test
    public void testStatusPieChartAggregation()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w1", "Status Donut", WidgetMetric.STATUS, "", WidgetPresentation.PIE_CHART,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, "half", 0
        );

        final List<BatchLatestVariationDto> variations = List.of(
            new BatchLatestVariationDto("k1", "passed-clean", false, "JAVA", "run-1", "Default", "Chrome", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L),
            new BatchLatestVariationDto("k2", "succeeded-fixed", true, "PLAYBOOK", "run-1", "Default", "Chrome", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L),
            new BatchLatestVariationDto("k3", "failed-unknown", false, "AI", "run-1", "Default", "Firefox", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L)
        );

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, Collections.emptyList(), variations, Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertEquals("67%", view.getPieCenterRate()); // 2 of 3 passed/fixed
        Assertions.assertEquals("PASS", view.getPieCenterLabel());
        Assertions.assertEquals(3, view.getPieSlices().size()); // 1 passed, 1 succeeded fixed, 1 unknown
        Assertions.assertEquals("Passed", view.getPieSlices().get(0).getLabel());
        Assertions.assertEquals(1, view.getPieSlices().get(0).getCount());
        Assertions.assertEquals("Succeeded Fixed", view.getPieSlices().get(1).getLabel());
        Assertions.assertEquals(1, view.getPieSlices().get(1).getCount());
        Assertions.assertEquals("Unknown Fail", view.getPieSlices().get(2).getLabel());
        Assertions.assertEquals(1, view.getPieSlices().get(2).getCount());
    }

    @Test
    public void testStatusPieChartFourBucketAggregation()
    {
        final DashboardWidgetConfigDto pieConfig = new DashboardWidgetConfigDto(
            "w-pie-4", "Status Donut 4 Buckets", WidgetMetric.STATUS, "", WidgetPresentation.PIE_CHART,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, "half", 0
        );
        final DashboardWidgetConfigDto listConfig = new DashboardWidgetConfigDto(
            "w-list-4", "Status List 4 Buckets", WidgetMetric.STATUS, "", WidgetPresentation.LIST,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, "half", 1
        );

        final List<BatchLatestVariationDto> variations = List.of(
            new BatchLatestVariationDto("k1", "passed-clean", false, "JAVA", "run-1", "Default", "Chrome", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L),
            new BatchLatestVariationDto("k2", "succeeded-fixed", true, "PLAYBOOK", "run-1", "Default", "Chrome", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L),
            new BatchLatestVariationDto("k3", "failed-known", false, "AI", "run-1", "Default", "Chrome", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L),
            new BatchLatestVariationDto("k4", "failed-unknown", false, "JAVA", "run-1", "Default", "Firefox", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L),
            new BatchLatestVariationDto("k5", "ignored", false, "JAVA", "run-1", "Default", "Safari", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L)
        );

        final DashboardWidgetViewDto pieView = service.aggregateWidget(
            pieConfig, Collections.emptyList(), variations, Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(pieView);
        Assertions.assertEquals(5, pieView.getPieSlices().size());
        Assertions.assertEquals("Passed", pieView.getPieSlices().get(0).getLabel());
        Assertions.assertEquals(1, pieView.getPieSlices().get(0).getCount());
        Assertions.assertEquals("Succeeded Fixed", pieView.getPieSlices().get(1).getLabel());
        Assertions.assertEquals(1, pieView.getPieSlices().get(1).getCount());
        Assertions.assertEquals("Known Fail", pieView.getPieSlices().get(2).getLabel());
        Assertions.assertEquals(1, pieView.getPieSlices().get(2).getCount());
        Assertions.assertEquals("Unknown Fail", pieView.getPieSlices().get(3).getLabel());
        Assertions.assertEquals(1, pieView.getPieSlices().get(3).getCount());
        Assertions.assertEquals("Ignored", pieView.getPieSlices().get(4).getLabel());
        Assertions.assertEquals(1, pieView.getPieSlices().get(4).getCount());
        Assertions.assertEquals("40%", pieView.getPieCenterRate()); // 2 of 5 passed/fixed

        final DashboardWidgetViewDto listView = service.aggregateWidget(
            listConfig, Collections.emptyList(), variations, Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );
        Assertions.assertNotNull(listView);
        Assertions.assertEquals(5, listView.getListItems().size());
        final List<String> groupNames = listView.getListItems().stream().map(WidgetListItemDto::getLabel).toList();
        Assertions.assertTrue(groupNames.contains("Passed"));
        Assertions.assertTrue(groupNames.contains("Succeeded Fixed"));
        Assertions.assertTrue(groupNames.contains("Known Fail"));
        Assertions.assertTrue(groupNames.contains("Unknown Fail"));
        Assertions.assertTrue(groupNames.contains("Ignored"));
    }

    @Test
    public void testCostsRatioBarAggregation()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w2", "Costs Ratio", WidgetMetric.COSTS, "", WidgetPresentation.RATIO_BAR,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, "half", 1
        );

        final TokenCostTrendDto trend = new TokenCostTrendDto(1.50, 1.20, 15000L, List.of(1.0, 1.2, 1.5));

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), trend, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertEquals(2, view.getRatioSegments().size());
        Assertions.assertEquals("Prompt Tokens", view.getRatioSegments().get(0).getName());
    }

    @Test
    public void testBugsPieChartAggregation()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w3", "Bugs Distribution", WidgetMetric.BUGS, "", WidgetPresentation.PIE_CHART,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE, "half", 2
        );

        final List<TestBaseBugEntity> bugs = List.of(
            new TestBaseBugEntity("v1", "BUG-101", "ALL", "ALL", System.currentTimeMillis(), "run-1", "CRITICAL", null),
            new TestBaseBugEntity("v2", "BUG-102", "ALL", "ALL", System.currentTimeMillis(), "run-1", "MAJOR", null),
            new TestBaseBugEntity("v3", "BUG-103", "ALL", "ALL", System.currentTimeMillis(), "run-1", "MAJOR", null)
        );

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, bugs
        );

        Assertions.assertNotNull(view);
        Assertions.assertEquals("3", view.getPieCenterRate());
        Assertions.assertEquals("BUGS", view.getPieCenterLabel());
        Assertions.assertEquals(2, view.getPieSlices().size()); // CRITICAL, MAJOR
    }

    @Test
    public void testTagRatioBarAggregation()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w4", "Browser Ratio", WidgetMetric.TAG, "browser", WidgetPresentation.RATIO_BAR,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, "half", 3
        );

        final List<DistributionRatioDto> browserRatios = List.of(
            new DistributionRatioDto("Chrome", 6, 60.0, "#10b981"),
            new DistributionRatioDto("Firefox", 4, 40.0, "#3b82f6")
        );

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, Collections.emptyList(), Collections.emptyList(), browserRatios, Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertEquals(2, view.getRatioSegments().size());
        Assertions.assertEquals("Chrome", view.getRatioSegments().get(0).getName());
    }

    @Test
    public void testPerlenketteByRunsPercent()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w5", "Pass Rate Perlenkette", WidgetMetric.STATUS, "", WidgetPresentation.PERLENKETTE,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, "full", 4
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-101");
        r1.setStartTimeMs(1000L);
        r1.setTotalTests(10);
        r1.setPassedCount(10);
        r1.setPassRate(100.0);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-102");
        r2.setStartTimeMs(2000L);
        r2.setTotalTests(10);
        r2.setPassedCount(8);
        r2.setFailedUnknownCount(2);
        r2.setPassRate(80.0);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1, r2), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertNotNull(view.getPerlenkette());
        Assertions.assertEquals(2, view.getPerlenkette().getTotalBeads());
        Assertions.assertEquals("100%", view.getPerlenkette().getBeads().get(0).getValueFormatted());
        Assertions.assertEquals("80%", view.getPerlenkette().getBeads().get(1).getValueFormatted());
        Assertions.assertEquals("bead-pass", view.getPerlenkette().getBeads().get(0).getStatusClass());
        Assertions.assertEquals("bead-unknown", view.getPerlenkette().getBeads().get(1).getStatusClass());
    }

    @Test
    public void testPerlenketteLatestApproachAbsolute()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w6", "Suite Evolution Bead Chain", WidgetMetric.STATUS, "", WidgetPresentation.PERLENKETTE,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.ABSOLUTE, "full", 5
        );

        final BatchSuiteEvolutionPointDto pt1 = new BatchSuiteEvolutionPointDto(
            "run-1", 0, "2026-10-21 10:00", 5, 5, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 100.0
        );
        final BatchSuiteEvolutionPointDto pt2 = new BatchSuiteEvolutionPointDto(
            "run-2", 1, "2026-10-21 11:00", 8, 3, 7, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 87.5
        );
        final BatchSuiteEvolutionDto suiteEvolution = new BatchSuiteEvolutionDto(List.of(pt1, pt2));

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, suiteEvolution, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertEquals(2, view.getPerlenkette().getTotalBeads());
        Assertions.assertEquals("5", view.getPerlenkette().getBeads().get(0).getValueFormatted());
        Assertions.assertEquals("8", view.getPerlenkette().getBeads().get(1).getValueFormatted());
    }

    @Test
    public void testTrendGraphGeneration()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w7", "Cost Trend Graph", WidgetMetric.COSTS, "", WidgetPresentation.TREND_GRAPH,
            WidgetTrendMode.DAILY, WidgetValueFormat.ABSOLUTE, "full", 6
        );

        final TokenCostTrendDto trend = new TokenCostTrendDto(2.50, 1.00, 25000L, List.of(1.0, 1.5, 2.5));

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), trend, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertFalse(view.getTrendSvgPath().isBlank());
        Assertions.assertFalse(view.getTrendSvgAreaPath().isBlank());
        Assertions.assertEquals(3, view.getTrendPoints().size());
    }

    @Test
    public void testTrendGraphHourlyBucketingWithCosts()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w8", "Hourly Cost Trend", WidgetMetric.COSTS, "", WidgetPresentation.TREND_GRAPH,
            WidgetTrendMode.HOURLY, WidgetValueFormat.ABSOLUTE, "full", 7
        );

        final long baseTime = 1730000000000L; // fixed epoch
        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-h1");
        r1.setStartTimeMs(baseTime);
        r1.setTotalLlmCost(0.0525);
        r1.setTotalTests(10);
        r1.setPassedCount(10);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-h2");
        r2.setStartTimeMs(baseTime + 3600 * 1000L * 2); // 2 hours later
        r2.setTotalLlmCost(0.1250);
        r2.setTotalTests(10);
        r2.setPassedCount(10);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1, r2), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertEquals(2, view.getTrendPoints().size());
        Assertions.assertEquals(0.0525, view.getTrendPoints().get(0).getValue(), 0.0001);
        Assertions.assertEquals("$0.0525", view.getTrendPoints().get(0).getValueFormatted());
        Assertions.assertEquals(0.1250, view.getTrendPoints().get(1).getValue(), 0.0001);
        Assertions.assertEquals("$0.1250", view.getTrendPoints().get(1).getValueFormatted());
        Assertions.assertTrue(view.getTrendDeltaLabel().contains("+$0.0725"));
    }

    @Test
    public void testTrendGraphDailyBucketingWithBugs()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w9", "Daily Bugs Trend", WidgetMetric.BUGS, "", WidgetPresentation.TREND_GRAPH,
            WidgetTrendMode.DAILY, WidgetValueFormat.ABSOLUTE, "full", 8
        );

        final long baseTime = 1730000000000L;
        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-d1");
        r1.setStartTimeMs(baseTime);
        r1.setFailedKnownCount(2);
        r1.setTotalTests(10);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-d2");
        r2.setStartTimeMs(baseTime + 86400 * 1000L * 2); // 2 days later
        r2.setFailedKnownCount(5);
        r2.setTotalTests(10);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1, r2), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertEquals(2, view.getTrendPoints().size());
        Assertions.assertEquals(2.0, view.getTrendPoints().get(0).getValue(), 0.001);
        Assertions.assertEquals("2 bugs", view.getTrendPoints().get(0).getValueFormatted());
        Assertions.assertEquals(5.0, view.getTrendPoints().get(1).getValue(), 0.001);
        Assertions.assertEquals("5 bugs", view.getTrendPoints().get(1).getValueFormatted());
        Assertions.assertEquals("+3 bugs vs start", view.getTrendDeltaLabel());
    }

    @Test
    public void testPerlenketteCostsByRuns()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w10", "Cost By Runs Chain", WidgetMetric.COSTS, "", WidgetPresentation.PERLENKETTE,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE, "full", 9
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-c1");
        r1.setStartTimeMs(1000L);
        r1.setTotalLlmCost(0.0245);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-c2");
        r2.setStartTimeMs(2000L);
        r2.setTotalLlmCost(0.0812);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1, r2), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertNotNull(view.getPerlenkette());
        Assertions.assertEquals(2, view.getPerlenkette().getTotalBeads());
        Assertions.assertEquals("$0.0245", view.getPerlenkette().getBeads().get(0).getValueFormatted());
        Assertions.assertEquals("$0.0812", view.getPerlenkette().getBeads().get(1).getValueFormatted());
        Assertions.assertEquals("bead-cost", view.getPerlenkette().getBeads().get(0).getStatusClass());
        Assertions.assertEquals("bead-cost", view.getPerlenkette().getBeads().get(1).getStatusClass());
        Assertions.assertEquals("#3b82f6", view.getPerlenkette().getBeads().get(0).getColor(), "Cost under $0.05 must be blue");
        Assertions.assertEquals("#1d4ed8", view.getPerlenkette().getBeads().get(0).getDarkColor());
        Assertions.assertEquals("#eab308", view.getPerlenkette().getBeads().get(1).getColor(), "Cost between $0.05 and $0.15 must be yellow/amber");
    }

    @Test
    public void testPerlenketteCostsLatestApproach_WithMultipleRuns_GeneratesRunProgression()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-cost-latest", "Cost String of Pearls", WidgetMetric.COSTS, "", WidgetPresentation.PERLENKETTE,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.ABSOLUTE, "full", 9
        );

        final TestRunEntity r128 = new TestRunEntity();
        r128.setId("run-128");
        r128.setStartTimeMs(1000L);
        r128.setTimestampLabel("Oct 28 10:39");
        r128.setTotalLlmCost(0.50);
        r128.setTotalLlmTokens(1500L);

        final TestRunEntity r129 = new TestRunEntity();
        r129.setId("run-129");
        r129.setStartTimeMs(2000L);
        r129.setTimestampLabel("Oct 28 10:39");
        r129.setTotalLlmCost(0.80);
        r129.setTotalLlmTokens(2400L);

        final TestRunEntity r130 = new TestRunEntity();
        r130.setId("run-130");
        r130.setStartTimeMs(3000L);
        r130.setTimestampLabel("Oct 28 10:39");
        r130.setTotalLlmCost(0.60);
        r130.setTotalLlmTokens(1800L);

        final TestRunEntity r131 = new TestRunEntity();
        r131.setId("run-131");
        r131.setStartTimeMs(4000L);
        r131.setTimestampLabel("Oct 28 10:39");
        r131.setTotalLlmCost(1.20);
        r131.setTotalLlmTokens(3600L);

        final TestRunEntity r132 = new TestRunEntity();
        r132.setId("run-132");
        r132.setStartTimeMs(5000L);
        r132.setTimestampLabel("Nov 05 10:38");
        r132.setTotalLlmCost(0.10);
        r132.setTotalLlmTokens(300L);

        final TestRunEntity r133 = new TestRunEntity();
        r133.setId("run-133");
        r133.setStartTimeMs(6000L);
        r133.setTimestampLabel("Nov 05 11:34");
        r133.setTotalLlmCost(0.08);
        r133.setTotalLlmTokens(250L);

        final List<TestRunEntity> runs = List.of(r128, r129, r130, r131, r132, r133);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, runs, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertNotNull(view.getPerlenkette());
        Assertions.assertEquals(6, view.getPerlenkette().getTotalBeads(), "Must evaluate all 6 runs instead of collapsing into daily buckets");

        final List<PerlenketteBeadDto> beads = view.getPerlenkette().getBeads();
        Assertions.assertEquals("run-128", beads.get(0).getId());
        Assertions.assertEquals("$0.5000", beads.get(0).getValueFormatted());
        Assertions.assertNotNull(beads.get(0).getTimestampLabel());
        Assertions.assertFalse(beads.get(0).getTimestampLabel().isBlank());
        Assertions.assertEquals("$0.5000", beads.get(0).getDetails().get("Cost"));
        Assertions.assertEquals("1500", beads.get(0).getDetails().get("Total Tokens"));

        Assertions.assertEquals("run-129", beads.get(1).getId());
        Assertions.assertEquals("$0.8000", beads.get(1).getValueFormatted());

        Assertions.assertEquals("run-132", beads.get(4).getId());
        Assertions.assertEquals("$0.1000", beads.get(4).getValueFormatted());

        Assertions.assertEquals("run-133", beads.get(5).getId());
        Assertions.assertEquals("$0.0800", beads.get(5).getValueFormatted());
        Assertions.assertNotNull(beads.get(5).getTimestampLabel());
    }

    @Test
    public void testPerlenketteCostsLatestApproach_FallbackToTokenCostTrendWhenNoRuns()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-cost-fallback", "Cost Fallback Chain", WidgetMetric.COSTS, "", WidgetPresentation.PERLENKETTE,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.ABSOLUTE, "full", 9
        );

        final TokenCostPointGroupDto g1 = new TokenCostPointGroupDto(
            0, "Day 1", "run-oct21", "2026-10-21", 3.10, 10000L, 0.0, 0.0, Collections.emptyList()
        );
        final TokenCostPointGroupDto g2 = new TokenCostPointGroupDto(
            1, "Day 2", "run-nov05", "2026-11-05", 0.18, 500L, -2.92, -94.19, Collections.emptyList()
        );

        final TokenCostTrendDto trend = new TokenCostTrendDto(
            0.18, 3.10, 10500L, List.of(3.10, 0.18), List.of(g1, g2)
        );

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), trend, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertNotNull(view.getPerlenkette());
        Assertions.assertEquals(2, view.getPerlenkette().getTotalBeads(), "Must fall back to tokenCostTrend point groups when runs are empty");
        Assertions.assertEquals("$3.1000", view.getPerlenkette().getBeads().get(0).getValueFormatted());
        Assertions.assertEquals("$0.1800", view.getPerlenkette().getBeads().get(1).getValueFormatted());
    }

    @Test
    public void testPerlenketteCostsBudgetAlertSpectrum()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-cost-spectrum", "Cost Spectrum Chain", WidgetMetric.COSTS, "", WidgetPresentation.PERLENKETTE,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE, "full", 10
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-green");
        r1.setStartTimeMs(1000L);
        r1.setTotalLlmCost(0.005);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-blue");
        r2.setStartTimeMs(2000L);
        r2.setTotalLlmCost(0.02);

        final TestRunEntity r3 = new TestRunEntity();
        r3.setId("run-yellow");
        r3.setStartTimeMs(3000L);
        r3.setTotalLlmCost(0.12);

        final TestRunEntity r4 = new TestRunEntity();
        r4.setId("run-orange");
        r4.setStartTimeMs(4000L);
        r4.setTotalLlmCost(0.35);

        final TestRunEntity r5 = new TestRunEntity();
        r5.setId("run-red");
        r5.setStartTimeMs(5000L);
        r5.setTotalLlmCost(3.10);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1, r2, r3, r4, r5), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertEquals(5, view.getPerlenkette().getTotalBeads());

        final List<PerlenketteBeadDto> beads = view.getPerlenkette().getBeads();
        // Tier 1: <= $0.01 -> Green
        Assertions.assertEquals("#22c55e", beads.get(0).getColor());
        Assertions.assertEquals("#15803d", beads.get(0).getDarkColor());

        // Tier 2: < $0.05 -> Blue
        Assertions.assertEquals("#3b82f6", beads.get(1).getColor());
        Assertions.assertEquals("#1d4ed8", beads.get(1).getDarkColor());

        // Tier 3: < $0.15 -> Yellow/Amber
        Assertions.assertEquals("#eab308", beads.get(2).getColor());
        Assertions.assertEquals("#ca8a04", beads.get(2).getDarkColor());

        // Tier 4: < $0.50 -> Orange
        Assertions.assertEquals("#f97316", beads.get(3).getColor());
        Assertions.assertEquals("#c2410c", beads.get(3).getDarkColor());

        // Tier 5: >= $0.50 -> Red
        Assertions.assertEquals("#ef4444", beads.get(4).getColor());
        Assertions.assertEquals("#b91c1c", beads.get(4).getDarkColor());
    }

    @Test
    public void testPerlenketteBugsDailyBucketing()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w11", "Bugs Daily Chain", WidgetMetric.BUGS, "", WidgetPresentation.PERLENKETTE,
            WidgetTrendMode.DAILY, WidgetValueFormat.ABSOLUTE, "full", 10
        );

        final long baseTime = 1730000000000L;
        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-b1");
        r1.setStartTimeMs(baseTime);
        r1.setFailedKnownCount(3);
        r1.setTotalTests(10);
        r1.setPassedCount(7);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-b2");
        r2.setStartTimeMs(baseTime + 86400 * 1000L);
        r2.setFailedKnownCount(1);
        r2.setTotalTests(10);
        r2.setPassedCount(9);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1, r2), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertNotNull(view.getPerlenkette());
        Assertions.assertEquals(2, view.getPerlenkette().getTotalBeads());
        Assertions.assertEquals("3 bugs", view.getPerlenkette().getBeads().get(0).getValueFormatted());
        Assertions.assertEquals("1 bug", view.getPerlenkette().getBeads().get(1).getValueFormatted());
        Assertions.assertEquals("bead-known", view.getPerlenkette().getBeads().get(0).getStatusClass());
        Assertions.assertEquals("bead-known", view.getPerlenkette().getBeads().get(1).getStatusClass());
    }

    @Test
    public void testStatusTrendGraphMultiColorStackedLayers()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w12", "Status Trend Stacked", WidgetMetric.STATUS, "", WidgetPresentation.TREND_GRAPH,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, "half", 11
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-s1");
        r1.setStartTimeMs(1000L);
        r1.setTotalTests(10);
        r1.setPassedCount(7);
        r1.setSucceededFixedCount(1);
        r1.setFailedKnownCount(1);
        r1.setFailedUnknownCount(1);
        r1.setPassRate(80.0);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-s2");
        r2.setStartTimeMs(2000L);
        r2.setTotalTests(10);
        r2.setPassedCount(9);
        r2.setSucceededFixedCount(0);
        r2.setFailedKnownCount(1);
        r2.setFailedUnknownCount(0);
        r2.setPassRate(90.0);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1, r2), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertTrue(view.hasTrendLayers());
        Assertions.assertEquals("#10b981", view.getTrendColor());
        Assertions.assertFalse(view.getTrendLayers().isEmpty());

        final boolean hasPassedLayer = view.getTrendLayers().stream()
            .anyMatch(l -> "Passed Clean".equals(l.getName()) && l.hasLinePath());
        Assertions.assertTrue(hasPassedLayer);

        final boolean hasKnownLayer = view.getTrendLayers().stream()
            .anyMatch(l -> "Failed Known".equals(l.getName()) && l.hasLinePath());
        Assertions.assertTrue(hasKnownLayer);
    }

    @Test
    public void testBugsTrendGraphLegendAndThemeColor()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w13", "Bugs Trend Orange", WidgetMetric.BUGS, "", WidgetPresentation.TREND_GRAPH,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE, "half", 12
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-bg1");
        r1.setStartTimeMs(1000L);
        r1.setFailedKnownCount(3);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-bg2");
        r2.setStartTimeMs(2000L);
        r2.setFailedKnownCount(1);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1, r2), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertEquals("#f97316", view.getTrendColor());
        Assertions.assertFalse(view.hasTrendLayers());
        Assertions.assertEquals(1, view.getTrendLayers().size());
        Assertions.assertEquals("Active Known Bugs", view.getTrendLayers().get(0).getName());
        Assertions.assertEquals("", view.getTrendLayers().get(0).getFormattedValue(), "Trend legend layers must omit point-in-time values");
    }

    @Test
    public void testCostsTrendGraphDailySubDayGranularityFallback()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w14", "Costs Daily Single Day Fallback", WidgetMetric.COSTS, "", WidgetPresentation.TREND_GRAPH,
            WidgetTrendMode.DAILY, WidgetValueFormat.ABSOLUTE, "half", 13
        );

        final long sameDayMs = 1726300000000L;
        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-c1");
        r1.setStartTimeMs(sameDayMs);
        r1.setTotalLlmCost(0.0120);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-c2");
        r2.setStartTimeMs(sameDayMs + 3600 * 1000L);
        r2.setTotalLlmCost(0.0340);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1, r2), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertEquals("#be123c", view.getTrendColor());
        Assertions.assertEquals(2, view.getTrendPoints().size());
        Assertions.assertEquals("$0.0120", view.getTrendPoints().get(0).getValueFormatted());
        Assertions.assertEquals("$0.0340", view.getTrendPoints().get(1).getValueFormatted());
        Assertions.assertFalse(view.hasTrendLayers());
        Assertions.assertEquals(1, view.getTrendLayers().size());
        Assertions.assertEquals("LLM Token Cost", view.getTrendLayers().get(0).getName());
        Assertions.assertEquals("", view.getTrendLayers().get(0).getFormattedValue(), "Trend legend layers must omit point-in-time values");
    }

    @Test
    public void testPieChartStatusWithLastRunScope()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-last-run", "Status Last Run", WidgetMetric.STATUS, "", WidgetPresentation.PIE_CHART,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LAST_RUN, "half", 0
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-1");
        r1.setStartTimeMs(1000L);
        r1.setPassedCount(5);
        r1.setFailedUnknownCount(5);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-2");
        r2.setStartTimeMs(2000L);
        r2.setPassedCount(8);
        r2.setFailedUnknownCount(2);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1, r2), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertEquals("80%", view.getPieCenterRate());
        Assertions.assertEquals("PASS", view.getPieCenterLabel());
        Assertions.assertEquals("10 Tests (Run #run-2)", view.getSummaryLabel());
        Assertions.assertEquals(WidgetPointScope.LAST_RUN, view.getPointScope());
        Assertions.assertEquals(2, view.getPieSlices().size());
        Assertions.assertEquals("Passed", view.getPieSlices().get(0).getLabel());
        Assertions.assertEquals(8, view.getPieSlices().get(0).getCount());
    }

    @Test
    public void testPieChartStatusWithTodayScope()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-today", "Status Today", WidgetMetric.STATUS, "", WidgetPresentation.PIE_CHART,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.TODAY, "half", 0
        );

        final long now = System.currentTimeMillis();
        final TestRunEntity rOld = new TestRunEntity();
        rOld.setId("run-old");
        rOld.setStartTimeMs(now - (3L * 24L * 60L * 60L * 1000L)); // 3 days ago
        rOld.setPassedCount(10);

        final TestRunEntity rToday = new TestRunEntity();
        rToday.setId("run-today");
        rToday.setStartTimeMs(now - 1000L);
        rToday.setPassedCount(4);
        rToday.setFailedUnknownCount(1);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(rOld, rToday), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertEquals("80%", view.getPieCenterRate());
        Assertions.assertEquals("5 Tests (1 Run)", view.getSummaryLabel());
        Assertions.assertEquals(WidgetPointScope.TODAY, view.getPointScope());
        Assertions.assertEquals(2, view.getPieSlices().size());
    }

    @Test
    public void testPieChartStatusWithLastHourScope()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-last-hour", "Status Last Hour", WidgetMetric.STATUS, "", WidgetPresentation.PIE_CHART,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LAST_HOUR, "half", 0
        );

        final long now = System.currentTimeMillis();
        final TestRunEntity r2HoursAgo = new TestRunEntity();
        r2HoursAgo.setId("run-2h");
        r2HoursAgo.setStartTimeMs(now - (2L * 60L * 60L * 1000L));
        r2HoursAgo.setPassedCount(20);

        final TestRunEntity r30MinsAgo = new TestRunEntity();
        r30MinsAgo.setId("run-30m");
        r30MinsAgo.setStartTimeMs(now - (30L * 60L * 1000L));
        r30MinsAgo.setPassedCount(3);
        r30MinsAgo.setFailedUnknownCount(1);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r2HoursAgo, r30MinsAgo), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertEquals("75%", view.getPieCenterRate());
        Assertions.assertEquals("4 Tests (1 Run)", view.getSummaryLabel());
        Assertions.assertEquals(WidgetPointScope.LAST_HOUR, view.getPointScope());
    }

    @Test
    public void testPieChartCostsWithLastRunScope()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-costs-last-run", "Costs Last Run", WidgetMetric.COSTS, "", WidgetPresentation.PIE_CHART,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.ABSOLUTE, WidgetPointScope.LAST_RUN, "half", 0
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-1");
        r1.setStartTimeMs(1000L);
        r1.setTotalLlmCost(0.0200);
        r1.setTotalLlmTokens(2500L);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-2");
        r2.setStartTimeMs(2000L);
        r2.setTotalLlmCost(0.0450);
        r2.setTotalLlmTokens(5000L);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1, r2), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertEquals("$0.05", view.getPieCenterRate());
        Assertions.assertEquals("COST", view.getPieCenterLabel());
        Assertions.assertEquals("5,000 tokens (Run #run-2)", view.getSummaryLabel());
        Assertions.assertEquals(2, view.getPieSlices().size());
        Assertions.assertEquals("Prompt Tokens (70%)", view.getPieSlices().get(0).getLabel());
        Assertions.assertEquals(3500L, view.getPieSlices().get(0).getCount());
        Assertions.assertEquals("3,500", view.getPieSlices().get(0).getFormattedCount());
    }

    @Test
    public void testPieChartCostsWithLatestTestStatusScope()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-costs-latest", "Costs Latest Status", WidgetMetric.COSTS, "", WidgetPresentation.PIE_CHART,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.ABSOLUTE, WidgetPointScope.LATEST_TEST_STATUS, "half", 0
        );

        final BatchLatestVariationDto v1 = new BatchLatestVariationDto(
            "k1", "passed-clean", false, "JAVA", "run-1", "Default", "Chrome", "en-US", "@Browsing", "Default",
            Collections.emptyMap(), 0.0300, 1500L, "exec-1"
        );
        final BatchLatestVariationDto v2 = new BatchLatestVariationDto(
            "k2", "passed-clean", false, "JAVA", "run-1", "Default", "Chrome", "en-US", "@Browsing", "Default",
            Collections.emptyMap(), 0.0200, 2500L, "exec-2"
        );

        // Daily trend has higher aggregate numbers to verify that latestVariations takes precedence for LATEST_TEST_STATUS
        final TokenCostTrendDto legacyTrend = new TokenCostTrendDto(
            25.50, 10.00, 500000L, List.of(10.00, 25.50), Collections.emptyList()
        );

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, Collections.emptyList(), List.of(v1, v2), Collections.emptyList(), Collections.emptyList(), legacyTrend, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertEquals("$0.05", view.getPieCenterRate());
        Assertions.assertEquals("COST", view.getPieCenterLabel());
        Assertions.assertEquals("4,000 tokens", view.getSummaryLabel());
        Assertions.assertEquals(2, view.getPieSlices().size());
        Assertions.assertEquals("Prompt Tokens (70%)", view.getPieSlices().get(0).getLabel());
        Assertions.assertEquals(2800, view.getPieSlices().get(0).getCount());
        Assertions.assertEquals("2,800", view.getPieSlices().get(0).getFormattedCount());
        Assertions.assertEquals("Completion Tokens (30%)", view.getPieSlices().get(1).getLabel());
        Assertions.assertEquals(1200, view.getPieSlices().get(1).getCount());
        Assertions.assertEquals("1,200", view.getPieSlices().get(1).getFormattedCount());
    }

    @Test
    public void testRatioBarCostsWithLatestTestStatusScope()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-ratio-cost-latest", "Ratio Cost Latest Status", WidgetMetric.COSTS, "", WidgetPresentation.RATIO_BAR,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS, "half", 0
        );

        final BatchLatestVariationDto v1 = new BatchLatestVariationDto(
            "k1", "passed-clean", false, "JAVA", "run-1", "Default", "Chrome", "en-US", "@Browsing", "Default",
            Collections.emptyMap(), 0.0125, 500L, "exec-1"
        );
        final BatchLatestVariationDto v2 = new BatchLatestVariationDto(
            "k2", "passed-clean", false, "JAVA", "run-1", "Default", "Chrome", "en-US", "@Browsing", "Default",
            Collections.emptyMap(), 0.0375, 1500L, "exec-2"
        );

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, Collections.emptyList(), List.of(v1, v2), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertEquals("$0.0500 Total Cost", view.getSummaryLabel());
        Assertions.assertEquals(2, view.getRatioSegments().size());
        Assertions.assertEquals("70", view.getRatioSegments().get(0).getFormattedCount());
        Assertions.assertEquals("30", view.getRatioSegments().get(1).getFormattedCount());
    }

    @Test
    public void testListCostsWithLatestTestStatusScope()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-list-cost-latest", "List Cost Latest Status", WidgetMetric.COSTS, "", WidgetPresentation.LIST,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS, "half", 0
        );

        final BatchLatestVariationDto v1 = new BatchLatestVariationDto(
            "k1", "passed-clean", false, "JAVA", "run-1", "Default", "Chrome", "en-US", "@Browsing", "Default",
            Collections.emptyMap(), 0.0200, 2000L, "exec-1"
        );
        final BatchLatestVariationDto v2 = new BatchLatestVariationDto(
            "k2", "passed-clean", false, "JAVA", "run-1", "Default", "Chrome", "en-US", "@Browsing", "Default",
            Collections.emptyMap(), 0.0300, 3000L, "exec-2"
        );

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, Collections.emptyList(), List.of(v1, v2), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertFalse(view.getListItems().isEmpty());
        Assertions.assertEquals("Current Cost", view.getListItems().get(0).getLabel());
        Assertions.assertEquals("$0.05", view.getListItems().get(0).getValueFormatted());
        Assertions.assertEquals("5,000 tokens", view.getListItems().get(0).getSecondaryLabel());
    }

    @Test
    public void testRatioBarStatusWithPointScope()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-ratio-last-run", "Ratio Last Run", WidgetMetric.STATUS, "", WidgetPresentation.RATIO_BAR,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LAST_RUN, "half", 0
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-1");
        r1.setStartTimeMs(1000L);
        r1.setPassedCount(5);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-2");
        r2.setStartTimeMs(2000L);
        r2.setPassedCount(6);
        r2.setFailedUnknownCount(4);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1, r2), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertEquals(2, view.getRatioSegments().size());
        Assertions.assertEquals("Passed", view.getRatioSegments().get(0).getName());
        Assertions.assertEquals(60.0, view.getRatioSegments().get(0).getPercentage(), 0.01);
        Assertions.assertEquals("Unknown Fail", view.getRatioSegments().get(1).getName());
        Assertions.assertEquals(40.0, view.getRatioSegments().get(1).getPercentage(), 0.01);
    }

    @Test
    public void testEmptyPointScopeWindowReturnsEmptyState()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-empty-hour", "Empty Hour", WidgetMetric.STATUS, "", WidgetPresentation.PIE_CHART,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LAST_HOUR, "half", 0
        );

        final TestRunEntity rOld = new TestRunEntity();
        rOld.setId("run-old");
        rOld.setStartTimeMs(System.currentTimeMillis() - (5L * 60L * 60L * 1000L)); // 5 hours ago
        rOld.setPassedCount(5);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(rOld), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertFalse(view.hasData());
        Assertions.assertEquals("0 Tests", view.getSummaryLabel());
        Assertions.assertEquals("No test runs found for runs of last hour", view.getEmptyMessage());
    }

    @Test
    public void testResolveContributingExecutions_LastRun()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-last-run", "Last Run Status", WidgetMetric.STATUS, "", WidgetPresentation.PIE_CHART,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LAST_RUN, "half", 0
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-1");
        r1.setStartTimeMs(1000L);
        r1.setPassedCount(2);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-2");
        r2.setStartTimeMs(2000L);
        r2.setPassedCount(4);
        r2.setFailedUnknownCount(1);

        final WidgetContributingExecutionsDto result = service.resolveContributingExecutions(
            config, List.of(r1, r2), Collections.emptyList(), null
        );

        Assertions.assertNotNull(result);
        Assertions.assertEquals("w-last-run", result.getWidgetId());
        Assertions.assertEquals(1, result.getTotalRunsCount());
        Assertions.assertEquals("run-2", result.getRunGroups().get(0).getRunId());
        Assertions.assertEquals(5, result.getTotalExecutionsCount());
        Assertions.assertEquals(4, result.getOverallPassCount());
        Assertions.assertEquals(1, result.getOverallUnknownCount());
    }

    @Test
    public void testResolveContributingExecutions_WithDataService()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-ds", "Status with Executions", WidgetMetric.STATUS, "", WidgetPresentation.PIE_CHART,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LAST_RUN, "half", 0
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-10");
        r1.setStartTimeMs(5000L);

        final TestExecutionDto e1 = new TestExecutionDto(
            "row-1", "run-10", "com.example.AuthTest", "testLogin", "testLogin", "PASSED", "en-US", "Chrome", Collections.emptyMap()
        );

        final TestExecutionDto e2 = new TestExecutionDto(
            "row-2", "run-10", "com.example.CartTest", "testCheckout", "testCheckout", "FAILED", "de-DE", "Firefox", Collections.emptyMap()
        );

        final RunReportDto mockReport = Mockito.mock(RunReportDto.class);
        Mockito.when(mockReport.getExecutions()).thenReturn(List.of(e1, e2));

        final AuraReportDataService mockDataService = Mockito.mock(AuraReportDataService.class);
        Mockito.when(mockDataService.getRunReport("run-10")).thenReturn(mockReport);

        final WidgetContributingExecutionsDto result = service.resolveContributingExecutions(
            config, List.of(r1), Collections.emptyList(), mockDataService
        );

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getTotalRunsCount());
        Assertions.assertEquals(2, result.getTotalExecutionsCount());
        Assertions.assertEquals(1, result.getOverallPassCount());
        Assertions.assertEquals(1, result.getOverallUnknownCount());
        Assertions.assertEquals(2, result.getRunGroups().get(0).getExecutions().size());
        Assertions.assertEquals("com.example.AuthTest testLogin", result.getRunGroups().get(0).getExecutions().get(0).getTestName());
    }

    @Test
    public void testResolveContributingExecutions_CostsMetricFiltersCostsOnly()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-cost", "Cost Executions", WidgetMetric.COSTS, "", WidgetPresentation.RATIO_BAR,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.ABSOLUTE, WidgetPointScope.LAST_RUN, "half", 0
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-cost-1");
        r1.setStartTimeMs(9000L);

        final TestExecutionDto eFree = new TestExecutionDto(
            "row-free", "run-cost-1", "com.example.AuthTest", "testFree", "testFree", "PASSED", "US", "Chrome", 0.0, 0L, Collections.emptyMap()
        );

        final TestExecutionDto eCost = new TestExecutionDto(
            "row-cost", "run-cost-1", "com.example.CostTest", "testCostly", "testCostly", "PASSED", "US", "Chrome", 0.0450, 1500L, Collections.emptyMap()
        );

        final RunReportDto mockReport = Mockito.mock(RunReportDto.class);
        Mockito.when(mockReport.getExecutions()).thenReturn(List.of(eFree, eCost));

        final AuraReportDataService mockDataService = Mockito.mock(AuraReportDataService.class);
        Mockito.when(mockDataService.getRunReport("run-cost-1")).thenReturn(mockReport);

        final WidgetContributingExecutionsDto result = service.resolveContributingExecutions(
            config, List.of(r1), Collections.emptyList(), mockDataService
        );

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getTotalExecutionsCount());
        Assertions.assertEquals("com.example.CostTest testCostly", result.getRunGroups().get(0).getExecutions().get(0).getTestName());
        Assertions.assertEquals(0.0450, result.getOverallTotalCost(), 0.0001);
        Assertions.assertEquals(1500L, result.getOverallTotalTokens());
    }

    @Test
    public void testResolveContributingExecutions_LatestTestStatus_PopulatesExecutionsViaLogicalKey()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-latest", "Latest Status Pie", WidgetMetric.STATUS, "", WidgetPresentation.PIE_CHART,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS, "half", 0
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-emea");
        r1.setStartTimeMs(10000L);

        final TestExecutionDto exec1 = new TestExecutionDto(
            "exec-101", "run-emea", "com.example.HomeTest", "testHome", "testHome", "PASSED", "en-US", "Chrome", Collections.emptyMap()
        );
        final TestExecutionDto exec2 = new TestExecutionDto(
            "exec-102", "run-emea", "com.example.CartTest", "testCart", "testCart", "FAILED", "en-US", "Chrome", Collections.emptyMap()
        );

        final RunReportDto mockReport = Mockito.mock(RunReportDto.class);
        Mockito.when(mockReport.getExecutions()).thenReturn(List.of(exec1, exec2));

        final AuraReportDataService mockDataService = Mockito.mock(AuraReportDataService.class);
        Mockito.when(mockDataService.getRunReport("run-emea")).thenReturn(mockReport);
        Mockito.when(mockDataService.buildLogicalKey(r1, exec1)).thenReturn("com.example.HomeTest|testHome|Default|br:Chrome|loc:en-US|env:Default");
        Mockito.when(mockDataService.buildLogicalKey(r1, exec2)).thenReturn("com.example.CartTest|testCart|Default|br:Chrome|loc:en-US|env:Default");

        final BatchLatestVariationDto v1 = new BatchLatestVariationDto(
            "com.example.HomeTest|testHome|Default|br:Chrome|loc:en-US|env:Default",
            "passed-clean", false, "JAVA", "run-emea", "Default", "Chrome", "en-US", "@Browsing (default)", "Default",
            Collections.emptyMap(), 0.0, 0L, "exec-101"
        );
        final BatchLatestVariationDto v2 = new BatchLatestVariationDto(
            "com.example.CartTest|testCart|Default|br:Chrome|loc:en-US|env:Default",
            "failed-unknown", false, "JAVA", "run-emea", "Default", "Chrome", "en-US", "@Browsing (default)", "Default",
            Collections.emptyMap(), 0.0, 0L, "exec-102"
        );

        final WidgetContributingExecutionsDto result = service.resolveContributingExecutions(
            config, List.of(r1), List.of(v1, v2), mockDataService
        );

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getTotalRunsCount());
        Assertions.assertEquals(2, result.getTotalExecutionsCount());
        Assertions.assertEquals(1, result.getOverallPassCount());
        Assertions.assertEquals(1, result.getOverallUnknownCount());
        Assertions.assertEquals(1, result.getRunGroups().size());

        final WidgetRunExecutionsGroupDto group = result.getRunGroups().get(0);
        Assertions.assertEquals("run-emea", group.getRunId());
        Assertions.assertEquals(2, group.getExecutions().size());
        Assertions.assertEquals("exec-101", group.getExecutions().get(0).getId());
        Assertions.assertEquals("exec-102", group.getExecutions().get(1).getId());
    }

    @Test
    public void testResolveContributingExecutions_CostsMetric_ZeroCostRunShowsExecutions()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-cost-zero", "Costs Pie Chart", WidgetMetric.COSTS, "", WidgetPresentation.PIE_CHART,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.ABSOLUTE, WidgetPointScope.LATEST_TEST_STATUS, "half", 0
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-stg-zero");
        r1.setStartTimeMs(12000L);

        final TestExecutionDto exec1 = new TestExecutionDto(
            "exec-z1", "run-stg-zero", "com.example.SmokeTest", "testOne", "testOne", "PASSED", "en-US", "Chrome", 0.0, 0L, Collections.emptyMap()
        );
        final TestExecutionDto exec2 = new TestExecutionDto(
            "exec-z2", "run-stg-zero", "com.example.SmokeTest", "testTwo", "testTwo", "PASSED", "en-US", "Chrome", 0.0, 0L, Collections.emptyMap()
        );

        final RunReportDto mockReport = Mockito.mock(RunReportDto.class);
        Mockito.when(mockReport.getExecutions()).thenReturn(List.of(exec1, exec2));

        final AuraReportDataService mockDataService = Mockito.mock(AuraReportDataService.class);
        Mockito.when(mockDataService.getRunReport("run-stg-zero")).thenReturn(mockReport);
        Mockito.when(mockDataService.buildLogicalKey(r1, exec1)).thenReturn("key-z1");
        Mockito.when(mockDataService.buildLogicalKey(r1, exec2)).thenReturn("key-z2");

        final BatchLatestVariationDto v1 = new BatchLatestVariationDto(
            "key-z1", "passed-clean", false, "JAVA", "run-stg-zero", "Default", "Chrome", "en-US", "@Browsing (default)", "Default",
            Collections.emptyMap(), 0.0, 0L, "exec-z1"
        );
        final BatchLatestVariationDto v2 = new BatchLatestVariationDto(
            "key-z2", "passed-clean", false, "JAVA", "run-stg-zero", "Default", "Chrome", "en-US", "@Browsing (default)", "Default",
            Collections.emptyMap(), 0.0, 0L, "exec-z2"
        );

        final WidgetContributingExecutionsDto result = service.resolveContributingExecutions(
            config, List.of(r1), List.of(v1, v2), mockDataService
        );

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getTotalRunsCount());
        Assertions.assertEquals(2, result.getTotalExecutionsCount());
        Assertions.assertEquals(2, result.getRunGroups().get(0).getExecutions().size());
        Assertions.assertEquals(0.0, result.getOverallTotalCost(), 0.0001);
        Assertions.assertEquals(0L, result.getOverallTotalTokens());
    }

    @Test
    public void testSortRunsChronologically_NullTimestampSafety()
    {
        final TestRunEntity rNull = new TestRunEntity();
        rNull.setId("run-null");
        rNull.setStartTimeMs(null);

        final TestRunEntity rOld = new TestRunEntity();
        rOld.setId("run-old");
        rOld.setStartTimeMs(1000L);

        final TestRunEntity rNew = new TestRunEntity();
        rNew.setId("run-new");
        rNew.setStartTimeMs(5000L);

        final List<TestRunEntity> sorted = service.sortRunsChronologically(List.of(rNew, rNull, rOld));

        Assertions.assertEquals(3, sorted.size());
        Assertions.assertEquals("run-null", sorted.get(0).getId(), "Run with null timestamp must be placed at the start");
        Assertions.assertEquals("run-old", sorted.get(1).getId());
        Assertions.assertEquals("run-new", sorted.get(2).getId(), "Newest run must be placed at the end for LAST_RUN");
    }

    @Test
    public void testResolveContributingExecutions_LastRunWithNullTimestampRun()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-last-null", "Last Run with Nulls", WidgetMetric.STATUS, "", WidgetPresentation.PIE_CHART,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LAST_RUN, "half", 0
        );

        final TestRunEntity rNull = new TestRunEntity();
        rNull.setId("run-corrupt");
        rNull.setStartTimeMs(null);
        rNull.setPassedCount(10);

        final TestRunEntity rValid = new TestRunEntity();
        rValid.setId("run-valid");
        rValid.setStartTimeMs(8000L);
        rValid.setPassedCount(5);

        final WidgetContributingExecutionsDto result = service.resolveContributingExecutions(
            config, List.of(rNull, rValid), Collections.emptyList(), null
        );

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getTotalRunsCount());
        Assertions.assertEquals("run-valid", result.getRunGroups().get(0).getRunId());
        Assertions.assertEquals(5, result.getTotalExecutionsCount());
    }

    @Test
    public void testLatestApproachCostTrendWithMultipleSameDayRuns_GeneratesRunSeries()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-cost-evo", "Cost Suite Evolution", WidgetMetric.COSTS, "", WidgetPresentation.TREND_GRAPH,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.ABSOLUTE, "full", 0
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-1");
        r1.setStartTimeMs(1000L);
        r1.setTotalLlmCost(0.50);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-2");
        r2.setStartTimeMs(2000L);
        r2.setTotalLlmCost(1.20);

        final TokenCostPointGroupDto singleDayGroup = new TokenCostPointGroupDto(
            0, "Day 1 of 1 — 2026-10-29 (2 runs)", "run-2", "2026-10-29", 1.70, 1000L, 0.0, 0.0, Collections.emptyList()
        );
        final TokenCostTrendDto singleDayTrend = new TokenCostTrendDto(1.70, -1.0, 1000L, List.of(1.70), List.of(singleDayGroup));

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1, r2), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            singleDayTrend, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertEquals(2, view.getTrendPoints().size(), "Must generate 2 points for each run instead of collapsing into single day");
        Assertions.assertEquals("#run-1", view.getTrendPoints().get(0).getLabel());
        Assertions.assertEquals(0.50, view.getTrendPoints().get(0).getValue(), 0.0001);
        Assertions.assertEquals("#run-2", view.getTrendPoints().get(1).getLabel());
        Assertions.assertEquals(1.20, view.getTrendPoints().get(1).getValue(), 0.0001);
        Assertions.assertTrue(view.getTrendDeltaLabel().contains("+$0.7000 vs start"));
        Assertions.assertTrue(view.getTrendSvgPath().contains(" L "));
    }

    @Test
    public void testSinglePointTrend_GeneratesBaselineLineAndBaselineLabel()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-single", "Single Point Cost", WidgetMetric.COSTS, "", WidgetPresentation.TREND_GRAPH,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.ABSOLUTE, "half", 0
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-single-1");
        r1.setStartTimeMs(1000L);
        r1.setTotalLlmCost(3.1032);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertEquals(1, view.getTrendPoints().size());
        Assertions.assertEquals("Baseline", view.getTrendDeltaLabel(), "Single point must show Baseline rather than +$0.0000 vs start");
        Assertions.assertTrue(view.getTrendSvgPath().startsWith("M 70.0 "), "Must draw horizontal baseline across viewBox");
        Assertions.assertTrue(view.getTrendSvgPath().contains(" L 780.0 "));
        Assertions.assertTrue(view.getTrendSvgAreaPath().endsWith(" Z"));
    }

    @Test
    public void testAggregateWidget_StatusPieChartAndRatioBar_ShowHealedAi()
    {
        final DashboardWidgetConfigDto pieConfig = new DashboardWidgetConfigDto(
            "w-pie-status-healed", "Status Pie with Healed", WidgetMetric.STATUS, "",
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, true
        );

        final BatchLatestVariationDto v1 = new BatchLatestVariationDto("TestA", "PASSED", true, null, "run-1", "env", "chrome", "loc");
        final BatchLatestVariationDto v2 = new BatchLatestVariationDto("TestB", "PASSED", false, "AI", "run-1", "env", "chrome", "loc");
        final BatchLatestVariationDto v3 = new BatchLatestVariationDto("TestC", "PASSED", false, null, "run-1", "env", "chrome", "loc");
        final BatchLatestVariationDto v4 = new BatchLatestVariationDto("TestD", "FAILED_UNKNOWN", false, null, "run-1", "env", "chrome", "loc");

        final List<BatchLatestVariationDto> variations = List.of(v1, v2, v3, v4);

        final DashboardWidgetViewDto pieView = service.aggregateWidget(
            pieConfig, Collections.emptyList(), variations, Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(pieView);
        Assertions.assertTrue(pieView.isShowHealedAi());
        Assertions.assertEquals(2, pieView.getPieSlices().size());

        final WidgetPieSliceDto passedSlice = pieView.getPieSlices().stream()
            .filter(s -> "Passed".equals(s.getLabel()))
            .findFirst()
            .orElseThrow();

        Assertions.assertEquals(3, passedSlice.getCount());
        Assertions.assertEquals(75.0, passedSlice.getPercentage());
        Assertions.assertEquals(1, passedSlice.getHealedCount());
        Assertions.assertEquals(1, passedSlice.getAiCount());
        Assertions.assertTrue(passedSlice.hasHealedOrAi());
        Assertions.assertEquals("25.00 75.00", passedSlice.getHealedDashArray());

        // Also test ratio bar presentation with showHealedAi
        final DashboardWidgetConfigDto ratioConfig = new DashboardWidgetConfigDto(
            "w-ratio-status-healed", "Status Ratio with Healed", WidgetMetric.STATUS, "",
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 1, true
        );

        final DashboardWidgetViewDto ratioView = service.aggregateWidget(
            ratioConfig, Collections.emptyList(), variations, Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(ratioView);
        Assertions.assertTrue(ratioView.isShowHealedAi());
        Assertions.assertEquals(2, ratioView.getRatioSegments().size());

        final DistributionRatioDto passSeg = ratioView.getRatioSegments().stream()
            .filter(s -> "Passed".equals(s.getName()))
            .findFirst()
            .orElseThrow();

        Assertions.assertEquals(1, passSeg.getHealedCount());
        Assertions.assertEquals(1, passSeg.getAiCount());
        Assertions.assertTrue(passSeg.hasHealedOrAi());
    }

    @Test
    public void testStatusTrend_ProportionalMaxTestsScaling_AvoidsFlatBrick()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-trend-status-scaling", "Status Trend Scaling", WidgetMetric.STATUS, "",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-1");
        r1.setStartTimeMs(1000L);
        r1.setPassedCount(4);
        r1.setFailedUnknownCount(1); // 5 tests total

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-2");
        r2.setStartTimeMs(2000L);
        r2.setPassedCount(8);
        r2.setFailedUnknownCount(2); // 10 tests total

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1, r2), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertEquals(2, view.getTrendPoints().size());

        // Max tests = 10, graphHeight = 150 (padTop=15, height=200, baselineY=165)
        // Point 0 (4 passed clean tests) should reach y = 165 - (4/10 * 150) = 105.0
        // Point 1 (8 passed clean tests) should reach y = 165 - (8/10 * 150) = 45.0
        final double y0 = view.getTrendPoints().get(0).getY();
        final double y1 = view.getTrendPoints().get(1).getY();

        Assertions.assertEquals(105.0, y0, 0.1, "Point 0 primary passed line must scale to 40% height");
        Assertions.assertEquals(45.0, y1, 0.1, "Point 1 primary passed line must scale to 80% height");
        Assertions.assertNotEquals(y0, y1, "Passed line must naturally contour test volumes instead of flat brick");

        final WidgetTrendLayerDto passLayer = view.getTrendLayers().stream()
            .filter(l -> "Passed Clean".equals(l.getName()))
            .findFirst()
            .orElseThrow();
        Assertions.assertTrue(passLayer.hasLinePath());
        Assertions.assertTrue(passLayer.getSvgLinePath().contains("M 70.0 105.0 L 780.0 45.0"));

        final WidgetTrendLayerDto failLayer = view.getTrendLayers().stream()
            .filter(l -> "Failed Unknown".equals(l.getName()))
            .findFirst()
            .orElseThrow();
        Assertions.assertTrue(failLayer.hasLinePath());
        Assertions.assertTrue(failLayer.getSvgLinePath().contains("M 70.0 150.0 L 780.0 135.0"));
        Assertions.assertTrue(view.hasTrendLayers());
        Assertions.assertEquals(2, view.getTrendLayers().size(), "Should have Failed Unknown and Passed Clean layers");
        Assertions.assertTrue(view.getTrendLayers().stream().allMatch(l -> l.getFormattedValue().isEmpty()),
            "Trend legend layers must omit point-in-time values");
        Assertions.assertFalse(view.isFullWidth(), "Trend graph configured as half must stay half width");
        Assertions.assertTrue(view.hasYAxisTicks(), "Must produce Y-axis ticks");
        Assertions.assertEquals(3, view.getYAxisTicks().size());
        Assertions.assertEquals("0 Tests", view.getYAxisTicks().get(0).getLabel());
        Assertions.assertEquals("5 Tests", view.getYAxisTicks().get(1).getLabel());
        Assertions.assertEquals("10 Tests", view.getYAxisTicks().get(2).getLabel());
    }

    @Test
    public void testStatusTrend_ShowHealedAi_GeneratesPatternLayers()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-trend-status-patterns", "Status Trend Patterns", WidgetMetric.STATUS, "",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, true
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-patterns-1");
        r1.setStartTimeMs(1000L);
        r1.setPassedCount(4);
        r1.setPassedHealedCount(1);
        r1.setPassedAiCount(1); // passedClean = 2, passHealed = 1, passAi = 1
        r1.setFailedUnknownCount(1); // 5 tests total

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertTrue(view.isShowHealedAi());
        Assertions.assertTrue(view.hasTrendLayers());

        final List<WidgetTrendLayerDto> layers = view.getTrendLayers();
        final WidgetTrendLayerDto healedLayer = layers.stream()
            .filter(l -> "Passed Healed".equals(l.getName()))
            .findFirst()
            .orElseThrow();
        Assertions.assertEquals("url(#sparkle-pat-widget-w-trend-status-patterns)", healedLayer.getPatternUrl());
        Assertions.assertEquals("seg-healed-print", healedLayer.getPatternClass());
        Assertions.assertTrue(healedLayer.hasPattern());

        final WidgetTrendLayerDto aiLayer = layers.stream()
            .filter(l -> "Passed AI".equals(l.getName()))
            .findFirst()
            .orElseThrow();
        Assertions.assertEquals("url(#ai-pat-widget-w-trend-status-patterns)", aiLayer.getPatternUrl());
        Assertions.assertEquals("seg-ai-print", aiLayer.getPatternClass());
        Assertions.assertTrue(aiLayer.hasPattern());

        final WidgetTrendLayerDto cleanLayer = layers.stream()
            .filter(l -> "Passed Clean".equals(l.getName()))
            .findFirst()
            .orElseThrow();
        Assertions.assertFalse(cleanLayer.hasPattern());
        Assertions.assertTrue(layers.stream().allMatch(l -> l.getFormattedValue().isEmpty()),
            "Trend legend layers with healed/AI must omit point-in-time values");
        Assertions.assertTrue(layers.stream().allMatch(WidgetTrendLayerDto::hasLinePath),
            "All status trend layers must have standalone line paths for legend selection");
    }

    @Test
    public void testStatusPieChart_HealedAndAiSubArcsPreserveCategoryStatusColors()
    {
        final DashboardWidgetConfigDto pieConfig = new DashboardWidgetConfigDto(
            "w-pie-colors", "Status Pie Colors", WidgetMetric.STATUS, "",
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, true
        );

        final BatchLatestVariationDto vPass = new BatchLatestVariationDto("TestPass", "PASSED", true, null, "run-1", "env", "chrome", "loc");
        final BatchLatestVariationDto vKnown = new BatchLatestVariationDto("TestKnown", "FAILED_KNOWN", false, "AI", "run-1", "env", "chrome", "loc");
        final BatchLatestVariationDto vUnknown = new BatchLatestVariationDto("TestUnknown", "FAILED_UNKNOWN", false, "AI", "run-1", "env", "chrome", "loc");

        final DashboardWidgetViewDto view = service.aggregateWidget(
            pieConfig, Collections.emptyList(), List.of(vPass, vKnown, vUnknown), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertEquals(3, view.getPieSlices().size());

        final WidgetPieSliceDto passSlice = view.getPieSlices().stream().filter(s -> s.getLabel().contains("Pass")).findFirst().orElseThrow();
        Assertions.assertEquals("var(--status-pass, #10b981)", passSlice.getColor());
        Assertions.assertEquals("var(--status-pass-healed, #34d399)", passSlice.getHealedColor());

        final WidgetPieSliceDto knownSlice = view.getPieSlices().stream().filter(s -> s.getLabel().contains("Known")).findFirst().orElseThrow();
        Assertions.assertEquals("var(--status-known-fail, #f97316)", knownSlice.getColor(), "Known Fail AI base stroke must stay orange");
        Assertions.assertEquals("var(--status-known-healed, #fb923c)", knownSlice.getHealedColor());
        Assertions.assertEquals(1, knownSlice.getAiCount());

        final WidgetPieSliceDto unknownSlice = view.getPieSlices().stream().filter(s -> s.getLabel().contains("Unknown")).findFirst().orElseThrow();
        Assertions.assertEquals("var(--status-unknown-fail, #ef4444)", unknownSlice.getColor(), "Unknown Fail AI base stroke must stay red");
        Assertions.assertEquals("var(--status-unknown-healed, #f87171)", unknownSlice.getHealedColor());
        Assertions.assertEquals(1, unknownSlice.getAiCount());
    }

    @Test
    public void testTrendGraph_HonorsSelectedWidthAndGeneratesAxes()
    {
        final DashboardWidgetConfigDto configQuarter = new DashboardWidgetConfigDto(
            "w-trend-quarter", "Trend Quarter Width", WidgetMetric.BUGS, "",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "quarter", 0, false
        );

        final DashboardWidgetConfigDto configHalf = new DashboardWidgetConfigDto(
            "w-trend-half", "Trend Half Width", WidgetMetric.BUGS, "",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );

        final DashboardWidgetConfigDto configFull = new DashboardWidgetConfigDto(
            "w-trend-full", "Trend Full Width", WidgetMetric.BUGS, "",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "full", 0, false
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-101");
        r1.setStartTimeMs(1000L);
        r1.setFailedKnownCount(5);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-102");
        r2.setStartTimeMs(2000L);
        r2.setFailedKnownCount(2);

        final DashboardWidgetViewDto viewQuarter = service.aggregateWidget(
            configQuarter, List.of(r1, r2), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        final DashboardWidgetViewDto viewHalf = service.aggregateWidget(
            configHalf, List.of(r1, r2), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        final DashboardWidgetViewDto viewFull = service.aggregateWidget(
            configFull, List.of(r1, r2), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(viewQuarter);
        Assertions.assertTrue(viewQuarter.isQuarterWidth(), "Quarter width widget must report isQuarterWidth()=true");
        Assertions.assertFalse(viewQuarter.isHalfWidth());
        Assertions.assertFalse(viewQuarter.isFullWidth());
        Assertions.assertEquals("widget-card-quarter widget-quarter-width", viewQuarter.getWidthClass());

        Assertions.assertNotNull(viewHalf);
        Assertions.assertFalse(viewHalf.isQuarterWidth());
        Assertions.assertTrue(viewHalf.isHalfWidth(), "Half width widget must report isHalfWidth()=true");
        Assertions.assertFalse(viewHalf.isFullWidth(), "Trend graph with width=half must remain half-width");
        Assertions.assertEquals("widget-card-half widget-half-width", viewHalf.getWidthClass());

        Assertions.assertNotNull(viewFull);
        Assertions.assertFalse(viewFull.isQuarterWidth());
        Assertions.assertFalse(viewFull.isHalfWidth());
        Assertions.assertTrue(viewFull.isFullWidth(), "Trend graph with width=full must be full-width");
        Assertions.assertEquals("widget-card-full widget-full-width", viewFull.getWidthClass());

        Assertions.assertEquals(800.0, viewHalf.getTrendWidth());
        Assertions.assertEquals(200.0, viewHalf.getTrendHeight());
        Assertions.assertEquals(70.0, viewHalf.getTrendPadLeft());
        Assertions.assertEquals(165.0, viewHalf.getTrendBaselineY());

        // Y-axis ticks for bugs: 0 bugs, mid bugs, 5 bugs
        Assertions.assertTrue(viewHalf.hasYAxisTicks());
        Assertions.assertEquals(3, viewHalf.getYAxisTicks().size());
        Assertions.assertEquals("0 bugs", viewHalf.getYAxisTicks().get(0).getLabel());
        Assertions.assertEquals("3 bugs", viewHalf.getYAxisTicks().get(1).getLabel());
        Assertions.assertEquals("5 bugs", viewHalf.getYAxisTicks().get(2).getLabel());

        // X-axis ticks: run-101, run-102 (<= 7 values -> direct labels)
        Assertions.assertTrue(viewHalf.hasXAxisTicks());
        Assertions.assertEquals(2, viewHalf.getXAxisTicks().size());
        Assertions.assertEquals("#run-101", viewHalf.getXAxisTicks().get(0).getLabel());
        Assertions.assertEquals("#run-102", viewHalf.getXAxisTicks().get(1).getLabel());
    }

    @Test
    public void testTrendGraph_DenseRuns_GroupsXAxisInMilestones()
    {
        final DashboardWidgetConfigDto configFull = new DashboardWidgetConfigDto(
            "w-trend-dense-full", "Trend Dense Runs Full", WidgetMetric.BUGS, "",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "full", 0, false
        );

        final DashboardWidgetConfigDto configHalf = new DashboardWidgetConfigDto(
            "w-trend-dense-half", "Trend Dense Runs Half", WidgetMetric.BUGS, "",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );

        final DashboardWidgetConfigDto configQuarter = new DashboardWidgetConfigDto(
            "w-trend-dense-quarter", "Trend Dense Runs Quarter", WidgetMetric.BUGS, "",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "quarter", 0, false
        );

        final List<TestRunEntity> runs = new ArrayList<>();
        for (int i = 1; i <= 25; i++)
        {
            final TestRunEntity r = new TestRunEntity();
            r.setId(String.format("run_custom_stg_20261022_%06d", i * 100));
            r.setStartTimeMs(1000L * i);
            r.setFailedKnownCount(i % 5);
            runs.add(r);
        }

        final DashboardWidgetViewDto viewFull = service.aggregateWidget(
            configFull, runs, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        final DashboardWidgetViewDto viewHalf = service.aggregateWidget(
            configHalf, runs, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        final DashboardWidgetViewDto viewQuarter = service.aggregateWidget(
            configQuarter, runs, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(viewFull);
        Assertions.assertTrue(viewFull.hasXAxisTicks());
        // For full width, dense runs group into 10 milestones
        Assertions.assertEquals(10, viewFull.getXAxisTicks().size());
        Assertions.assertEquals("Run 1", viewFull.getXAxisTicks().get(0).getLabel());
        Assertions.assertEquals("Run 4", viewFull.getXAxisTicks().get(1).getLabel());
        Assertions.assertEquals("Latest", viewFull.getXAxisTicks().get(9).getLabel());
        Assertions.assertTrue(viewFull.getXAxisTicks().stream().noneMatch(t -> t.getLabel().startsWith("M") && Character.isDigit(t.getLabel().charAt(1))),
            "Milestone labels must not use ambiguous M1..M10 markers");
        Assertions.assertTrue(viewFull.getXAxisTicks().stream().allMatch(t -> t.getLabel().length() <= 10),
            "Milestone labels must remain compact to prevent collision");
        Assertions.assertTrue(viewFull.getXAxisTicks().get(0).getTooltip().contains("run_custom_stg_20261022_000100"),
            "Tooltip must contain the full run identifier");

        Assertions.assertNotNull(viewHalf);
        Assertions.assertTrue(viewHalf.hasXAxisTicks());
        // For half width, dense runs group into 8 milestones
        Assertions.assertEquals(8, viewHalf.getXAxisTicks().size());
        Assertions.assertEquals("Run 1", viewHalf.getXAxisTicks().get(0).getLabel());
        Assertions.assertEquals("Run 4", viewHalf.getXAxisTicks().get(1).getLabel());
        Assertions.assertEquals("Latest", viewHalf.getXAxisTicks().get(7).getLabel());
        Assertions.assertTrue(viewHalf.getXAxisTicks().stream().noneMatch(t -> t.getLabel().startsWith("M") && Character.isDigit(t.getLabel().charAt(1))),
            "Milestone labels must not use ambiguous M1..M10 markers");
        Assertions.assertTrue(viewHalf.getXAxisTicks().stream().allMatch(t -> t.getLabel().length() <= 10));

        Assertions.assertNotNull(viewQuarter);
        Assertions.assertTrue(viewQuarter.hasXAxisTicks());
        // For quarter width, dense runs group into 5 milestones to avoid cramped labels
        Assertions.assertEquals(5, viewQuarter.getXAxisTicks().size());
        Assertions.assertEquals("Run 1", viewQuarter.getXAxisTicks().get(0).getLabel());
        Assertions.assertEquals("Run 7", viewQuarter.getXAxisTicks().get(1).getLabel());
        Assertions.assertEquals("Latest", viewQuarter.getXAxisTicks().get(4).getLabel());
        Assertions.assertTrue(viewQuarter.getXAxisTicks().stream().noneMatch(t -> t.getLabel().startsWith("M") && Character.isDigit(t.getLabel().charAt(1))),
            "Milestone labels must not use ambiguous M1..M10 markers");
        Assertions.assertTrue(viewQuarter.getXAxisTicks().stream().allMatch(t -> t.getLabel().length() <= 10));
    }

    @Test
    public void testTrendGraph_DenseRunsAcrossDays_ProducesHumanReadableDateMilestones()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-trend-dates", "Daily Cost Trend Dates", WidgetMetric.COSTS, "",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.DAILY, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "full", 0, false
        );

        final List<TestRunEntity> runs = new ArrayList<>();
        for (int day = 10; day <= 25; day++)
        {
            final TestRunEntity r = new TestRunEntity();
            r.setId(String.format("run_202610%02d_120000", day));
            r.setTotalLlmCost(0.01 * (day - 9));
            runs.add(r);
        }

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, runs, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasXAxisTicks());
        // 16 daily points grouped into 10 milestones on full width
        Assertions.assertEquals(10, view.getXAxisTicks().size());
        Assertions.assertEquals("Oct 10", view.getXAxisTicks().get(0).getLabel(), "First milestone must be starting date Oct 10");
        Assertions.assertTrue(view.getXAxisTicks().stream().noneMatch(t -> t.getLabel().startsWith("M") && Character.isDigit(t.getLabel().charAt(1))),
            "Milestone labels must display real calendar dates instead of ambiguous M1..M10 markers");
        Assertions.assertTrue(view.getXAxisTicks().stream().allMatch(t -> t.getLabel().length() <= 10),
            "Date milestone labels must stay compact");
        Assertions.assertTrue(view.getXAxisTicks().get(0).getTooltip().contains("run_20261010_120000"),
            "Tooltip must contain the associated run id and details");
    }

    @Test
    public void testTrendGraph_ChronologicalOrderingFromRunIdTimestamp()
    {
        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run_20261028_094020"); // Oct 28
        r1.setStartTimeMs(null);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run_custom_stg_20261022_173500"); // Oct 22
        r2.setStartTimeMs(null);

        final TestRunEntity r3 = new TestRunEntity();
        r3.setId("run_dev_emea_20261017_091500"); // Oct 17
        r3.setStartTimeMs(null);

        final List<TestRunEntity> sorted = service.sortRunsChronologically(List.of(r1, r2, r3));
        Assertions.assertEquals(3, sorted.size());
        Assertions.assertEquals("run_dev_emea_20261017_091500", sorted.get(0).getId(), "Oct 17 must be first");
        Assertions.assertEquals("run_custom_stg_20261022_173500", sorted.get(1).getId(), "Oct 22 must be second");
        Assertions.assertEquals("run_20261028_094020", sorted.get(2).getId(), "Oct 28 must be third");

        final var buckets = service.groupRunsIntoTimeBuckets(List.of(r1, r2, r3), WidgetTrendMode.DAILY);
        Assertions.assertEquals(3, buckets.size(), "Each distinct date must form its own daily bucket");
        Assertions.assertEquals("Oct 17", buckets.get(0).getLabel());
        Assertions.assertEquals("Oct 22", buckets.get(1).getLabel());
        Assertions.assertEquals("Oct 28", buckets.get(2).getLabel());
    }

    @Test
    public void testTrendGraph_CostsMode_GeneratesCurrencyYAxisTicks()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-trend-cost", "Cost Trend Axes", WidgetMetric.COSTS, "",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-c1");
        r1.setStartTimeMs(1000L);
        r1.setTotalLlmCost(0.0100);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-c2");
        r2.setStartTimeMs(2000L);
        r2.setTotalLlmCost(0.0500);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1, r2), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasYAxisTicks());
        Assertions.assertEquals(3, view.getYAxisTicks().size());
        Assertions.assertEquals("$0.0000", view.getYAxisTicks().get(0).getLabel());
        Assertions.assertEquals("$0.0250", view.getYAxisTicks().get(1).getLabel());
        Assertions.assertEquals("$0.0500", view.getYAxisTicks().get(2).getLabel());
    }

    @Test
    public void testStatusTrend_TripleDigitVolume_AccommodatesInPadLeftWithoutClipping()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-trend-status-3digit", "Status Trend 3-Digit", WidgetMetric.STATUS, "",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS, "full", 0, false
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-1");
        r1.setStartTimeMs(1000L);
        r1.setPassedCount(100);
        r1.setFailedUnknownCount(16); // 116 tests total

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-2");
        r2.setStartTimeMs(2000L);
        r2.setPassedCount(110);
        r2.setFailedUnknownCount(6); // 116 tests total

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1, r2), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertEquals(70.0, view.getTrendPadLeft(), "Pad left must be 70.0 to prevent 3-digit label clipping");

        // Y-axis ticks should contain 0 Tests, 58 Tests, 116 Tests
        Assertions.assertTrue(view.hasYAxisTicks());
        Assertions.assertEquals(3, view.getYAxisTicks().size());
        Assertions.assertEquals("0 Tests", view.getYAxisTicks().get(0).getLabel());
        Assertions.assertEquals("58 Tests", view.getYAxisTicks().get(1).getLabel());
        Assertions.assertEquals("116 Tests", view.getYAxisTicks().get(2).getLabel());

        // Verify label length fits within the 62.0px margin (padLeft 70.0 - 8.0 = 62.0px)
        final double textAnchorX = view.getTrendPadLeft() - 8.0;
        final double maxCharWidth = 6.2; // monospace JetBrains Mono 10px character width
        for (final var tick : view.getYAxisTicks())
        {
            final double textWidth = tick.getLabel().length() * maxCharWidth;
            Assertions.assertTrue(textAnchorX - textWidth >= 0.0,
                "Tick label '" + tick.getLabel() + "' must not start before x=0 inside the SVG viewBox");
        }
    }

    @Test
    public void testStatusTrend_LargeSuiteVolume_FormatsCompactKiloTests()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-trend-status-large", "Status Trend Large", WidgetMetric.STATUS, "",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS, "full", 0, false
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-1");
        r1.setStartTimeMs(1000L);
        r1.setPassedCount(15000); // 15,000 tests total

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasYAxisTicks());
        Assertions.assertEquals(3, view.getYAxisTicks().size());
        Assertions.assertEquals("0 Tests", view.getYAxisTicks().get(0).getLabel());
        Assertions.assertEquals("7500 Tests", view.getYAxisTicks().get(1).getLabel());
        Assertions.assertEquals("15k Tests", view.getYAxisTicks().get(2).getLabel());
    }

    @Test
    public void testAggregateWidgets_WidgetWithOwnRunFilters_UnaffectedByGlobalFilters()
    {
        final AuraReportDataService dataService = Mockito.mock(AuraReportDataService.class);

        // Global dashboard filter is Chrome, so displayedRuns only contains runChrome
        final TestRunEntity runChrome = new TestRunEntity();
        runChrome.setId("run-chrome");
        runChrome.setPassedCount(10);
        runChrome.setTotalTests(10);

        final TestRunEntity runFirefox = new TestRunEntity();
        runFirefox.setId("run-firefox");
        runFirefox.setPassedCount(5);
        runFirefox.setTotalTests(5);

        final List<TestRunEntity> displayedRuns = List.of(runChrome);
        final List<TestRunEntity> baseRuns = List.of(runChrome, runFirefox);

        // Widget configured to only look at Firefox (unaffected by global Chrome filter)
        final WidgetFilterConfigDto ffFilters = new WidgetFilterConfigDto(
            Set.of("Firefox"), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptyMap(), Collections.emptyMap()
        );

        final DashboardWidgetConfigDto widgetConfig = new DashboardWidgetConfigDto(
            "w-firefox-only",
            "Firefox Pass Rate",
            WidgetMetric.STATUS,
            "",
            WidgetPresentation.PIE_CHART,
            WidgetTrendMode.LATEST_APPROACH,
            WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS,
            "half",
            0,
            false,
            false,
            Collections.emptyList(),
            true,
            ffFilters
        );

        // Mock scoping: runChrome has 0 tests for Firefox, runFirefox has 5 tests
        Mockito.when(dataService.scopeRunToCriteria(
            Mockito.eq(runChrome), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.isNull(), Mockito.any(), Mockito.any()
        )).thenReturn(null);

        Mockito.when(dataService.scopeRunToCriteria(
            Mockito.eq(runFirefox), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.isNull(), Mockito.any(), Mockito.any()
        )).thenReturn(runFirefox);

        // Mock latest executions for Firefox
        final BatchLatestVariationDto ffVar = new BatchLatestVariationDto(
            "k-ff", "passed-clean", false, "JAVA", "run-firefox", "Default", "Firefox", "US", "Checkout", "Default",
            Collections.emptyMap(), 0.0, 0L
        );
        final BatchSummaryDto ffBatchSummary = Mockito.mock(BatchSummaryDto.class);
        final BatchLatestExecutionsDto ffExecDto = new BatchLatestExecutionsDto(
            ffBatchSummary,
            baseRuns,
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(ffVar),
            1
        );

        Mockito.when(dataService.getBatchLatestTestExecutionsSummary(
            Mockito.eq("Nightly"),
            Mockito.eq(baseRuns),
            Mockito.any(),
            Mockito.isNull(), Mockito.isNull(),
            Mockito.eq(Set.of("Firefox")), Mockito.any(),
            Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any(),
            Mockito.any(),
            Mockito.any(), Mockito.any(),
            Mockito.eq(List.of("browser"))
        )).thenReturn(ffExecDto);

        final List<DashboardWidgetViewDto> views = service.aggregateWidgets(
            List.of(widgetConfig),
            displayedRuns,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            null,
            null,
            Collections.emptyList(),
            dataService,
            baseRuns,
            "Nightly",
            List.of("browser"),
            Collections.emptyMap(),
            Collections.emptyList()
        );

        Assertions.assertEquals(1, views.size());
        final DashboardWidgetViewDto view = views.get(0);
        Assertions.assertTrue(view.hasOwnRunFilters());
        Assertions.assertFalse(view.hasOwnExecutionIdentity());
        Assertions.assertEquals("Browsers: Firefox", view.getFilterSummary());
        Assertions.assertEquals("Filtered: Firefox", view.getFilterBadgeLabel());
        Assertions.assertEquals(1, view.getActiveFiltersCount());
        Assertions.assertEquals(1, view.getFilterChips().size());
        Assertions.assertEquals("browser", view.getFilterChips().get(0).getCategory());
        Assertions.assertEquals("web", view.getFilterChips().get(0).getIcon());
        Assertions.assertEquals("Firefox", view.getFilterChips().get(0).getLabel());
        Assertions.assertFalse(view.getFilterChips().get(0).isExcluded());
        Assertions.assertEquals("100%", view.getPieCenterRate());
        Assertions.assertEquals("PASS", view.getPieCenterLabel());
    }

    @Test
    public void testAggregateWidgets_WidgetWithOwnExecutionIdentity()
    {
        final AuraReportDataService dataService = Mockito.mock(AuraReportDataService.class);

        final TestRunEntity run = new TestRunEntity();
        run.setId("run-1");
        run.setPassedCount(10);
        run.setTotalTests(10);
        final List<TestRunEntity> runs = List.of(run);

        final DashboardWidgetConfigDto widgetConfig = new DashboardWidgetConfigDto(
            "w-custom-ident",
            "Multi-dim Donut",
            WidgetMetric.STATUS,
            "",
            WidgetPresentation.PIE_CHART,
            WidgetTrendMode.LATEST_APPROACH,
            WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS,
            "half",
            0,
            false,
            true,
            List.of("browser", "locale", "tag:device"),
            false,
            null
        );

        final BatchLatestVariationDto v1 = new BatchLatestVariationDto(
            "k-1", "passed-clean", false, "JAVA", "run-1", "Default", "Chrome", "US", "Checkout", "Default",
            Collections.emptyMap(), 0.0, 0L
        );
        final BatchSummaryDto batchSummary = Mockito.mock(BatchSummaryDto.class);
        final BatchLatestExecutionsDto execDto = new BatchLatestExecutionsDto(
            batchSummary,
            runs,
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(v1),
            1
        );

        Mockito.when(dataService.getBatchLatestTestExecutionsSummary(
            Mockito.eq("Nightly"),
            Mockito.eq(runs),
            Mockito.any(),
            Mockito.isNull(), Mockito.isNull(),
            Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any(),
            Mockito.any(),
            Mockito.any(), Mockito.any(),
            Mockito.eq(List.of("browser", "locale", "tag:device"))
        )).thenReturn(execDto);

        final List<DashboardWidgetViewDto> views = service.aggregateWidgets(
            List.of(widgetConfig),
            runs,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            null,
            null,
            Collections.emptyList(),
            dataService,
            runs,
            "Nightly",
            List.of("browser"),
            Collections.emptyMap(),
            Collections.emptyList()
        );

        Assertions.assertEquals(1, views.size());
        final DashboardWidgetViewDto view = views.get(0);
        Assertions.assertTrue(view.hasOwnExecutionIdentity());
        Assertions.assertFalse(view.hasOwnRunFilters());
        Assertions.assertEquals("Test + browser, locale, tag:device", view.getIdentityDimensionsSummary());
        Assertions.assertEquals("100%", view.getPieCenterRate());
    }

    @Test
    public void testAggregateWidgets_WidgetDefault_InheritsGlobalFiltersAndIdentity()
    {
        final AuraReportDataService dataService = Mockito.mock(AuraReportDataService.class);

        final TestRunEntity run = new TestRunEntity();
        run.setId("run-1");
        final List<TestRunEntity> runs = List.of(run);

        final DashboardWidgetConfigDto widgetConfig = new DashboardWidgetConfigDto(
            "w-default",
            "Default Donut",
            WidgetMetric.STATUS,
            "",
            WidgetPresentation.PIE_CHART,
            WidgetTrendMode.LATEST_APPROACH,
            WidgetValueFormat.PERCENT,
            "half",
            0
        );

        final BatchLatestVariationDto v1 = new BatchLatestVariationDto(
            "k-1", "passed-clean", false, "JAVA", "run-1", "Default", "Chrome", "US", "Checkout", "Default",
            Collections.emptyMap(), 0.0, 0L
        );

        final List<DashboardWidgetViewDto> views = service.aggregateWidgets(
            List.of(widgetConfig),
            runs,
            List.of(v1),
            Collections.emptyList(),
            Collections.emptyList(),
            null,
            null,
            Collections.emptyList(),
            dataService,
            runs,
            "Nightly",
            List.of("browser"),
            Collections.emptyMap(),
            Collections.emptyList()
        );

        Assertions.assertEquals(1, views.size());
        final DashboardWidgetViewDto view = views.get(0);
        Assertions.assertFalse(view.hasOwnExecutionIdentity());
        Assertions.assertFalse(view.hasOwnRunFilters());
        Assertions.assertEquals("Global Identity", view.getIdentityDimensionsSummary());
        Assertions.assertEquals("", view.getFilterSummary());
        Assertions.assertEquals("100%", view.getPieCenterRate());

        // Verify dataService was NOT invoked because widget inherits global data
        Mockito.verifyNoInteractions(dataService);
    }

    @Test
    public void testAggregateWidgets_CacheReuse_MultipleWidgetsWithSameCustomCriteria()
    {
        final AuraReportDataService dataService = Mockito.mock(AuraReportDataService.class);

        final TestRunEntity run = new TestRunEntity();
        run.setId("run-1");
        run.setPassedCount(10);
        run.setTotalTests(10);
        final List<TestRunEntity> runs = List.of(run);

        final WidgetFilterConfigDto filters = new WidgetFilterConfigDto(
            Set.of("Chrome"), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptyMap(), Collections.emptyMap()
        );

        final DashboardWidgetConfigDto w1 = new DashboardWidgetConfigDto(
            "w-1", "Widget 1", WidgetMetric.STATUS, "", WidgetPresentation.PIE_CHART,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0, false, true, List.of("browser", "locale"), true, filters
        );

        final DashboardWidgetConfigDto w2 = new DashboardWidgetConfigDto(
            "w-2", "Widget 2", WidgetMetric.STATUS, "", WidgetPresentation.RATIO_BAR,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 1, false, true, List.of("browser", "locale"), true, filters
        );

        Mockito.when(dataService.scopeRunToCriteria(
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.isNull(), Mockito.any(), Mockito.any()
        )).thenReturn(run);

        final BatchSummaryDto batchSummary = Mockito.mock(BatchSummaryDto.class);
        final BatchLatestExecutionsDto execDto = new BatchLatestExecutionsDto(
            batchSummary,
            runs,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            0
        );
        Mockito.when(dataService.getBatchLatestTestExecutionsSummary(
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any()
        )).thenReturn(execDto);

        final List<DashboardWidgetViewDto> views = service.aggregateWidgets(
            List.of(w1, w2),
            runs,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            null,
            null,
            Collections.emptyList(),
            dataService,
            runs,
            "Nightly",
            List.of("browser"),
            Collections.emptyMap(),
            Collections.emptyList()
        );

        Assertions.assertEquals(2, views.size());
        // Verify getBatchLatestTestExecutionsSummary was called ONLY ONCE because cache key was identical
        Mockito.verify(dataService, Mockito.times(1)).getBatchLatestTestExecutionsSummary(
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any()
        );
    }

    @Test
    public void testStatusTrend_SinglePoint_RendersProminentStackedColumnWithoutSpuriousFullWidthLine()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-single-status", "Single Point Status Trend", WidgetMetric.STATUS, "",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, "half", 0
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-20261028-100000");
        r1.setStartTimeMs(1000L);
        r1.setTotalTests(2);
        r1.setPassedCount(1);
        r1.setFailedUnknownCount(1);
        r1.setPassRate(50.0);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertTrue(view.hasTrendLayers());
        Assertions.assertEquals(1, view.getTrendPoints().size());
        Assertions.assertEquals(425.0, view.getTrendPoints().get(0).getX(), 0.001);

        // Standalone line width should be 100px (half-width 50px around 425.0, i.e. 375.0 to 475.0)
        final WidgetTrendLayerDto unknownLayer = view.getTrendLayers().stream()
            .filter(l -> "Failed Unknown".equals(l.getName()))
            .findFirst()
            .orElseThrow();
        Assertions.assertTrue(unknownLayer.hasLinePath());
        Assertions.assertTrue(unknownLayer.getSvgLinePath().startsWith("M 375.0 "),
            "Standalone line left bound must be 375.0 instead of 4px needle: " + unknownLayer.getSvgLinePath());
        Assertions.assertTrue(unknownLayer.getSvgLinePath().contains(" L 475.0 "),
            "Standalone line right bound must be 475.0 instead of 4px needle: " + unknownLayer.getSvgLinePath());
        Assertions.assertFalse(unknownLayer.getSvgLinePath().contains("780.0"),
            "Standalone line must NOT span across the entire SVG canvas: " + unknownLayer.getSvgLinePath());
    }

    @Test
    public void testTrendGraph_QuarterWidth_GeneratesProportionalAspectCanvasAndSizing()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-quarter-trend", "Quarter Trend", WidgetMetric.STATUS, "",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, "quarter", 0
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-20261028-100000");
        r1.setStartTimeMs(1000L);
        r1.setTotalTests(4);
        r1.setPassedCount(3);
        r1.setFailedUnknownCount(1);
        r1.setPassRate(75.0);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.isQuarterWidth());
        Assertions.assertEquals(360.0, view.getTrendWidth(), 0.001);
        Assertions.assertEquals(170.0, view.getTrendHeight(), 0.001);
        Assertions.assertEquals(140.0, view.getTrendBaselineY(), 0.001);
        Assertions.assertEquals(50.0, view.getTrendPadLeft(), 0.001);
        Assertions.assertEquals(15.0, view.getTrendPadRight(), 0.001);

        Assertions.assertEquals(1, view.getTrendPoints().size());
        // Center x = 50 + (360 - 50 - 15) / 2 = 197.5
        final double expectedCenterX = 197.5;
        Assertions.assertEquals(expectedCenterX, view.getTrendPoints().get(0).getX(), 0.001);

        // Single column half width is 25.0 -> spans 172.5 to 222.5
        final WidgetTrendLayerDto unknownLayer = view.getTrendLayers().stream()
            .filter(l -> "Failed Unknown".equals(l.getName()))
            .findFirst()
            .orElseThrow();
        Assertions.assertTrue(unknownLayer.hasLinePath());
        Assertions.assertTrue(unknownLayer.getSvgLinePath().startsWith("M 172.5 "),
            "Standalone line left bound must be 172.5: " + unknownLayer.getSvgLinePath());
        Assertions.assertTrue(unknownLayer.getSvgLinePath().contains(" L 222.5 "),
            "Standalone line right bound must be 222.5: " + unknownLayer.getSvgLinePath());
        Assertions.assertFalse(unknownLayer.getSvgLinePath().contains("780.0"),
            "Standalone line must NOT span across the entire SVG canvas: " + unknownLayer.getSvgLinePath());
    }

    @Test
    public void testAggregateWidgets_WidgetWithLimitFilter_EvaluatesOnlyTopNNewestRuns()
    {
        final AuraReportDataService dataService = Mockito.mock(AuraReportDataService.class);

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-1");
        r1.setStartTimeMs(1000L);
        r1.setTotalTests(10);
        r1.setPassedCount(10);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-2");
        r2.setStartTimeMs(2000L);
        r2.setTotalTests(10);
        r2.setPassedCount(10);

        final TestRunEntity r3 = new TestRunEntity();
        r3.setId("run-3");
        r3.setStartTimeMs(3000L);
        r3.setTotalTests(10);
        r3.setPassedCount(10);

        final TestRunEntity r4 = new TestRunEntity();
        r4.setId("run-4");
        r4.setStartTimeMs(4000L);
        r4.setTotalTests(10);
        r4.setPassedCount(10);

        final TestRunEntity r5 = new TestRunEntity();
        r5.setId("run-5");
        r5.setStartTimeMs(5000L);
        r5.setTotalTests(10);
        r5.setPassedCount(10);

        final List<TestRunEntity> allRuns = List.of(r5, r4, r3, r2, r1);

        // mock scopeRunToCriteria to return the run itself
        Mockito.when(dataService.scopeRunToCriteria(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenAnswer(inv -> inv.getArgument(0));

        final WidgetFilterConfigDto filterConfig = new WidgetFilterConfigDto(
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptyMap(), Collections.emptyMap(),
            2, null, null
        );

        final DashboardWidgetConfigDto widgetConfig = new DashboardWidgetConfigDto(
            "w-limit-2", "Last 2 Runs Trend", WidgetMetric.STATUS, "",
            WidgetPresentation.PERLENKETTE, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0,
            false, false, Collections.emptyList(), true, filterConfig
        );

        final BatchSummaryDto batchSummary = Mockito.mock(BatchSummaryDto.class);
        final BatchLatestExecutionsDto execDto = new BatchLatestExecutionsDto(
            batchSummary, List.of(r5, r4), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), 0
        );
        Mockito.when(dataService.getBatchLatestTestExecutionsSummary(
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any()
        )).thenReturn(execDto);

        final List<DashboardWidgetViewDto> views = service.aggregateWidgets(
            List.of(widgetConfig),
            allRuns,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            null,
            null,
            Collections.emptyList(),
            dataService,
            allRuns,
            "Nightly",
            List.of("browser"),
            Collections.emptyMap(),
            Collections.emptyList()
        );

        Assertions.assertEquals(1, views.size());
        final DashboardWidgetViewDto view = views.get(0);
        Assertions.assertTrue(view.hasOwnRunFilters());
        Assertions.assertEquals(1, view.getFilterChips().size());
        Assertions.assertEquals("limit", view.getFilterChips().get(0).getCategory());
        Assertions.assertEquals("Last 2 Runs", view.getFilterChips().get(0).getLabel());
        // Perlenkette should only have 2 beads for the 2 newest runs in chronological order!
        Assertions.assertNotNull(view.getPerlenkette());
        Assertions.assertEquals(2, view.getPerlenkette().getBeads().size());
        Assertions.assertEquals("run-4", view.getPerlenkette().getBeads().get(0).getId());
        Assertions.assertEquals("run-5", view.getPerlenkette().getBeads().get(1).getId());
    }

    @Test
    public void testAggregateWidgets_WidgetWithDateRangeFilter_EvaluatesRunsWithinRange()
    {
        final AuraReportDataService dataService = Mockito.mock(AuraReportDataService.class);

        // Sept 15, 2026: ~1789430400000L
        final TestRunEntity rSept = new TestRunEntity();
        rSept.setId("run-sept");
        rSept.setStartTimeMs(1789430400000L);
        rSept.setTotalTests(5);
        rSept.setPassedCount(5);

        // Oct 15, 2026: ~1792022400000L
        final TestRunEntity rOct = new TestRunEntity();
        rOct.setId("run-oct");
        rOct.setStartTimeMs(1792022400000L);
        rOct.setTotalTests(10);
        rOct.setPassedCount(10);

        // Nov 15, 2026: ~1794700800000L
        final TestRunEntity rNov = new TestRunEntity();
        rNov.setId("run-nov");
        rNov.setStartTimeMs(1794700800000L);
        rNov.setTotalTests(8);
        rNov.setPassedCount(8);

        final List<TestRunEntity> allRuns = List.of(rNov, rOct, rSept);

        Mockito.when(dataService.scopeRunToCriteria(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenAnswer(inv -> inv.getArgument(0));

        final WidgetFilterConfigDto filterConfig = new WidgetFilterConfigDto(
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptyMap(), Collections.emptyMap(),
            null, "2026-10-01", "2026-10-31"
        );

        final DashboardWidgetConfigDto widgetConfig = new DashboardWidgetConfigDto(
            "w-oct-only", "October Runs", WidgetMetric.STATUS, "",
            WidgetPresentation.PERLENKETTE, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0,
            false, false, Collections.emptyList(), true, filterConfig
        );

        final BatchSummaryDto batchSummary = Mockito.mock(BatchSummaryDto.class);
        final BatchLatestExecutionsDto execDto = new BatchLatestExecutionsDto(
            batchSummary, List.of(rOct), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), 0
        );
        Mockito.when(dataService.getBatchLatestTestExecutionsSummary(
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any()
        )).thenReturn(execDto);

        final List<DashboardWidgetViewDto> views = service.aggregateWidgets(
            List.of(widgetConfig),
            allRuns,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            null,
            null,
            Collections.emptyList(),
            dataService,
            allRuns,
            "Nightly",
            List.of("browser"),
            Collections.emptyMap(),
            Collections.emptyList()
        );

        Assertions.assertEquals(1, views.size());
        final DashboardWidgetViewDto view = views.get(0);
        Assertions.assertTrue(view.hasOwnRunFilters());
        Assertions.assertEquals(1, view.getFilterChips().size());
        Assertions.assertEquals("date", view.getFilterChips().get(0).getCategory());
        Assertions.assertEquals("2026-10-01 \u2013 2026-10-31", view.getFilterChips().get(0).getLabel());
        // Only rOct matched the date range!
        Assertions.assertNotNull(view.getPerlenkette());
        Assertions.assertEquals(1, view.getPerlenkette().getBeads().size());
        Assertions.assertEquals("run-oct", view.getPerlenkette().getBeads().get(0).getId());
    }

    @Test
    public void testWidgetPresentationStringOfPearlsNaming()
    {
        Assertions.assertEquals("String of Pearls", WidgetPresentation.PERLENKETTE.getDisplayName());
        Assertions.assertEquals(WidgetPresentation.PERLENKETTE, WidgetPresentation.fromString("PERLENKETTE"));
        Assertions.assertEquals(WidgetPresentation.PERLENKETTE, WidgetPresentation.fromString("STRING_OF_PEARLS"));
        Assertions.assertEquals(WidgetPresentation.PERLENKETTE, WidgetPresentation.fromString("string-of-pearls"));
        Assertions.assertEquals(WidgetPresentation.PERLENKETTE, WidgetPresentation.fromString("pearl_chain"));
    }

    @Test
    public void testAggregateWidgetTagMetricPerlenketteProducesBeadsPerTagValue()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-tag-pk", "Browser String of Pearls", WidgetMetric.TAG, "browser",
            WidgetPresentation.PERLENKETTE, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );

        final List<BatchLatestVariationDto> variations = new ArrayList<>();
        // 10 Chrome variations: 9 passed, 1 unknown fail -> 90%
        for (int i = 0; i < 9; i++)
        {
            variations.add(new BatchLatestVariationDto("c" + i, "passed-clean", false, "JAVA", "run-1", "Default", "Chrome", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L));
        }
        variations.add(new BatchLatestVariationDto("c9", "failed-unknown", false, "JAVA", "run-1", "Default", "Chrome", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L));

        // 6 Firefox variations: 6 passed -> 100%
        for (int i = 0; i < 6; i++)
        {
            variations.add(new BatchLatestVariationDto("f" + i, "passed-clean", false, "JAVA", "run-1", "Default", "Firefox", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L));
        }

        // 4 Safari variations: 2 passed, 2 fixed -> 100%
        variations.add(new BatchLatestVariationDto("s0", "passed-clean", false, "JAVA", "run-1", "Default", "Safari", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L));
        variations.add(new BatchLatestVariationDto("s1", "passed-clean", false, "JAVA", "run-1", "Default", "Safari", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L));
        variations.add(new BatchLatestVariationDto("s2", "succeeded-fixed", false, "JAVA", "run-1", "Default", "Safari", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L));
        variations.add(new BatchLatestVariationDto("s3", "succeeded-fixed", false, "JAVA", "run-1", "Default", "Safari", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L));

        // 2 Edge variations: 1 passed, 1 known fail -> 50%
        variations.add(new BatchLatestVariationDto("e0", "passed-clean", false, "JAVA", "run-1", "Default", "Edge", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L));
        variations.add(new BatchLatestVariationDto("e1", "failed-known", false, "JAVA", "run-1", "Default", "Edge", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L));

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, Collections.emptyList(), variations,
            Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList(), null
        );

        Assertions.assertNotNull(view);
        final PerlenketteDto pearls = view.getPerlenkette();
        Assertions.assertNotNull(pearls, "Perlenkette model must be populated for TAG metric");
        Assertions.assertEquals(4, pearls.getTotalBeads(), "Must create one bead per distinct tag value");
        Assertions.assertEquals("4 Values", view.getSummaryLabel());
        Assertions.assertEquals("4 Values", pearls.getTrendSummary());

        final List<PerlenketteBeadDto> beads = pearls.getBeads();
        // Sorted by volume descending: Chrome (10) -> Firefox (6) -> Safari (4) -> Edge (2)
        final PerlenketteBeadDto chromeBead = beads.get(0);
        Assertions.assertEquals("Chrome", chromeBead.getLabel());
        Assertions.assertEquals("90%", chromeBead.getValueFormatted());
        Assertions.assertEquals("10 tests", chromeBead.getTimestampLabel());
        Assertions.assertEquals(90.0, chromeBead.getPercentValue(), 0.01);
        Assertions.assertEquals("bead-unknown", chromeBead.getStatusClass());
        Assertions.assertEquals("browser: Chrome", chromeBead.getDetails().get("Tag"));
        Assertions.assertEquals("90.0%", chromeBead.getDetails().get("Pass Rate"));
        Assertions.assertEquals("10", chromeBead.getDetails().get("Total Tests"));
        Assertions.assertEquals("9", chromeBead.getDetails().get("Passed"));
        Assertions.assertEquals("1", chromeBead.getDetails().get("Unknown Fail"));
        Assertions.assertTrue(chromeBead.getPillWidth() >= chromeBead.getRadius() * 1.9);

        final PerlenketteBeadDto firefoxBead = beads.get(1);
        Assertions.assertEquals("Firefox", firefoxBead.getLabel());
        Assertions.assertEquals("100%", firefoxBead.getValueFormatted());
        Assertions.assertEquals("6 tests", firefoxBead.getTimestampLabel());
        Assertions.assertEquals(100.0, firefoxBead.getPercentValue(), 0.01);
        Assertions.assertEquals("bead-pass", firefoxBead.getStatusClass());
        Assertions.assertEquals("6", firefoxBead.getDetails().get("Passed"));

        final PerlenketteBeadDto safariBead = beads.get(2);
        Assertions.assertEquals("Safari", safariBead.getLabel());
        Assertions.assertEquals("100%", safariBead.getValueFormatted());
        Assertions.assertEquals("4 tests", safariBead.getTimestampLabel());
        Assertions.assertEquals(100.0, safariBead.getPercentValue(), 0.01);
        Assertions.assertEquals("bead-fixed", safariBead.getStatusClass());
        Assertions.assertEquals("2", safariBead.getDetails().get("Passed"));
        Assertions.assertEquals("2", safariBead.getDetails().get("Fixed"));

        final PerlenketteBeadDto edgeBead = beads.get(3);
        Assertions.assertEquals("Edge", edgeBead.getLabel());
        Assertions.assertEquals("50%", edgeBead.getValueFormatted());
        Assertions.assertEquals("2 tests", edgeBead.getTimestampLabel());
        Assertions.assertEquals(50.0, edgeBead.getPercentValue(), 0.01);
        Assertions.assertEquals("bead-known", edgeBead.getStatusClass());
        Assertions.assertEquals("1", edgeBead.getDetails().get("Known Fail"));
    }

    @Test
    public void testAggregateWidgetTagMetricPerlenkette_AbsoluteFormatAndFallbackRatios()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-tag-pk-abs", "Device String of Pearls", WidgetMetric.TAG, "device",
            WidgetPresentation.PERLENKETTE, WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );

        final TagRatioCardDto deviceCard = new TagRatioCardDto(
            "device", List.of(
                new DistributionRatioDto("Desktop", 15, 60.0, "#3b82f6"),
                new DistributionRatioDto("Mobile", 10, 40.0, "#10b981")
            )
        );

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), List.of(deviceCard), null, null, Collections.emptyList(), null
        );

        Assertions.assertNotNull(view);
        final PerlenketteDto pearls = view.getPerlenkette();
        Assertions.assertNotNull(pearls);
        Assertions.assertEquals(2, pearls.getTotalBeads());
        Assertions.assertEquals("2 Values", view.getSummaryLabel());
        Assertions.assertEquals("2 Values", pearls.getTrendSummary());

        final PerlenketteBeadDto bead0 = pearls.getBeads().get(0);
        Assertions.assertEquals("Desktop", bead0.getLabel());
        Assertions.assertEquals("15/15", bead0.getValueFormatted());
        Assertions.assertEquals("15 tests", bead0.getTimestampLabel());

        final PerlenketteBeadDto bead1 = pearls.getBeads().get(1);
        Assertions.assertEquals("Mobile", bead1.getLabel());
        Assertions.assertEquals("10/10", bead1.getValueFormatted());
        Assertions.assertEquals("10 tests", bead1.getTimestampLabel());
    }

    @Test
    public void testAggregateWidgetTagMetricPerlenkette_EmptyDataShowsNoTagDataMessage()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-tag-pk-empty", "Env Pearls", WidgetMetric.TAG, "environment",
            WidgetPresentation.PERLENKETTE, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList(), null
        );

        Assertions.assertNotNull(view);
        final PerlenketteDto pearls = view.getPerlenkette();
        Assertions.assertNotNull(pearls);
        Assertions.assertEquals(0, pearls.getTotalBeads());
        Assertions.assertEquals("No tag data available", view.getEmptyMessage());
    }

    @Test
    public void testTagPerlenketteChronologicalMilestonesAcrossRuns()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-rel-pk", "Release Milestone Stabilization", WidgetMetric.TAG, "Release",
            WidgetPresentation.PERLENKETTE, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS, "full", 0, false
        );

        // Run 1: 2026.09 (Sep 15) -> 90 passed, 10 known fail, 16 unknown fail = 116 total -> 90/116 = 77.58% -> 78%
        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run_20260915_100000");
        r1.setStartTimeMs(1726387200000L);
        r1.setPassedCount(90);
        r1.setFailedKnownCount(10);
        r1.setFailedUnknownCount(16);
        r1.setTotalTests(116);
        r1.setTags(List.of(new TestRunTagEntity("run_20260915_100000", "Release", "2026.09")));

        // Run 2: 2026.10 (Oct 15) -> 100 passed, 5 fixed, 5 known fail, 6 unknown fail = 116 total -> 105/116 = 90.51% -> 91%
        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run_20261015_100000");
        r2.setStartTimeMs(1728979200000L);
        r2.setPassedCount(100);
        r2.setSucceededFixedCount(5);
        r2.setFailedKnownCount(5);
        r2.setFailedUnknownCount(6);
        r2.setTotalTests(116);
        r2.setTags(List.of(new TestRunTagEntity("run_20261015_100000", "Release", "2026.10")));

        // Run 3: 2026.11 (Nov 15) -> 110 passed, 4 fixed, 2 known fail, 0 unknown fail = 116 total -> 114/116 = 98.27% -> 98%
        final TestRunEntity r3 = new TestRunEntity();
        r3.setId("run_20261115_100000");
        r3.setStartTimeMs(1731657600000L);
        r3.setPassedCount(110);
        r3.setSucceededFixedCount(4);
        r3.setFailedKnownCount(2);
        r3.setFailedUnknownCount(0);
        r3.setTotalTests(116);
        r3.setTags(List.of(new TestRunTagEntity("run_20261115_100000", "Release", "2026.11")));

        // Provide runs in reverse order (r3, r2, r1) to verify chronological re-ordering
        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r3, r2, r1), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList(), null
        );

        Assertions.assertNotNull(view);
        final PerlenketteDto pearls = view.getPerlenkette();
        Assertions.assertNotNull(pearls, "Perlenkette model must be populated for release milestone TAG widget");
        Assertions.assertEquals(3, pearls.getTotalBeads(), "Must create one bead per chronological release milestone");

        final List<PerlenketteBeadDto> beads = pearls.getBeads();
        // Bead 0: 2026.09
        final PerlenketteBeadDto b0 = beads.get(0);
        Assertions.assertEquals("2026.09", b0.getLabel());
        Assertions.assertEquals("78%", b0.getValueFormatted());
        Assertions.assertEquals("116 tests", b0.getTimestampLabel());
        Assertions.assertEquals(77.58, b0.getPercentValue(), 0.1);
        Assertions.assertEquals("bead-unknown", b0.getStatusClass());
        Assertions.assertEquals("/run-report?runId=run_20260915_100000", b0.getLinkUrl());
        Assertions.assertEquals("Suite Evolution (Deduplicated)", b0.getDetails().get("Aggregation Mode"));
        Assertions.assertEquals("116", b0.getDetails().get("Total Tests"));
        Assertions.assertEquals("#run_20260915_100000", b0.getDetails().get("Latest Run"));

        // Bead 1: 2026.10
        final PerlenketteBeadDto b1 = beads.get(1);
        Assertions.assertEquals("2026.10", b1.getLabel());
        Assertions.assertEquals("91%", b1.getValueFormatted());
        Assertions.assertEquals("116 tests", b1.getTimestampLabel());
        Assertions.assertEquals(90.51, b1.getPercentValue(), 0.1);
        Assertions.assertEquals("bead-unknown", b1.getStatusClass());
        Assertions.assertEquals("/run-report?runId=run_20261015_100000", b1.getLinkUrl());
        Assertions.assertEquals("Suite Evolution (Deduplicated)", b1.getDetails().get("Aggregation Mode"));
        Assertions.assertEquals("116", b1.getDetails().get("Total Tests"));
        Assertions.assertEquals("#run_20261015_100000", b1.getDetails().get("Latest Run"));

        // Bead 2: 2026.11
        final PerlenketteBeadDto b2 = beads.get(2);
        Assertions.assertEquals("2026.11", b2.getLabel());
        Assertions.assertEquals("98%", b2.getValueFormatted());
        Assertions.assertEquals("116 tests", b2.getTimestampLabel());
        Assertions.assertEquals(98.27, b2.getPercentValue(), 0.1);
        Assertions.assertEquals("bead-known", b2.getStatusClass());
        Assertions.assertEquals("/run-report?runId=run_20261115_100000", b2.getLinkUrl());
        Assertions.assertEquals("Suite Evolution (Deduplicated)", b2.getDetails().get("Aggregation Mode"));
        Assertions.assertEquals("116", b2.getDetails().get("Total Tests"));
        Assertions.assertEquals("#run_20261115_100000", b2.getDetails().get("Latest Run"));
    }

    @Test
    public void testTagPerlenketteTrendMode_ByRunsVsLatestApproach()
    {
        final AuraReportDataService dataService = Mockito.mock(AuraReportDataService.class);

        // Run 1: 2026.10 (Oct 10)
        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run_20261010_100000");
        r1.setStartTimeMs(1728556800000L);
        r1.setTags(List.of(new TestRunTagEntity("run_20261010_100000", "Release", "2026.10")));

        final TestExecutionDto exec1A = new TestExecutionDto("e1", "run_20261010_100000", "TestClassA", "testA", "testA", "passed-clean", "Chrome", Collections.emptyMap());
        final TestExecutionDto exec1B = new TestExecutionDto("e2", "run_20261010_100000", "TestClassB", "testB", "testB", "failed-unknown", "Chrome", Collections.emptyMap());

        final RunReportDto rep1 = new RunReportDto("run_20261010_100000", "Batch", "2026-10-10 10:00", "10s", 2, 1, 0, 0, 1, 0, List.of(exec1A, exec1B));
        Mockito.when(dataService.getRunReport("run_20261010_100000")).thenReturn(rep1);

        // Run 2: 2026.10 (Oct 15)
        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run_20261015_100000");
        r2.setStartTimeMs(1728988800000L);
        r2.setTags(List.of(new TestRunTagEntity("run_20261015_100000", "Release", "2026.10")));

        final TestExecutionDto exec2A = new TestExecutionDto("e3", "run_20261015_100000", "TestClassA", "testA", "testA", "passed-clean", "Chrome", Collections.emptyMap());
        final TestExecutionDto exec2B = new TestExecutionDto("e4", "run_20261015_100000", "TestClassB", "testB", "testB", "passed-clean", "Chrome", Collections.emptyMap()); // Retried and passed in run 2!
        final TestExecutionDto exec2C = new TestExecutionDto("e5", "run_20261015_100000", "TestClassC", "testC", "testC", "passed-clean", "Chrome", Collections.emptyMap());

        final RunReportDto rep2 = new RunReportDto("run_20261015_100000", "Batch", "2026-10-15 10:00", "10s", 3, 3, 0, 0, 0, 0, List.of(exec2A, exec2B, exec2C));
        Mockito.when(dataService.getRunReport("run_20261015_100000")).thenReturn(rep2);

        final List<TestRunEntity> runs = List.of(r1, r2);

        // Case 1: LATEST_APPROACH (Suite Evolution / Deduplicated per milestone)
        final DashboardWidgetConfigDto latestApproachConfig = new DashboardWidgetConfigDto(
            "w-latest", "Release Suite Evolution", WidgetMetric.TAG, "Release",
            WidgetPresentation.PERLENKETTE, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS, "full", 0, false
        );

        final DashboardWidgetViewDto latestView = service.aggregateWidget(
            latestApproachConfig, runs, Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList(), dataService
        );

        Assertions.assertNotNull(latestView);
        Assertions.assertNotNull(latestView.getPerlenkette());
        Assertions.assertEquals(1, latestView.getPerlenkette().getTotalBeads());
        final PerlenketteBeadDto latestBead = latestView.getPerlenkette().getBeads().get(0);
        Assertions.assertEquals("2026.10", latestBead.getLabel());
        Assertions.assertEquals("3 tests", latestBead.getTimestampLabel());
        Assertions.assertEquals("100%", latestBead.getValueFormatted());
        Assertions.assertEquals(100.0, latestBead.getPercentValue(), 0.1);
        Assertions.assertEquals("Suite Evolution (Deduplicated)", latestBead.getDetails().get("Aggregation Mode"));
        Assertions.assertEquals("3", latestBead.getDetails().get("Total Tests"));
        Assertions.assertEquals("3", latestBead.getDetails().get("Passed"));

        // Case 2: BY_RUNS (All Run Executions / Total Volume)
        final DashboardWidgetConfigDto byRunsConfig = new DashboardWidgetConfigDto(
            "w-by-runs", "Release Execution Volume", WidgetMetric.TAG, "Release",
            WidgetPresentation.PERLENKETTE, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS, "full", 0, false
        );

        final DashboardWidgetViewDto volumeView = service.aggregateWidget(
            byRunsConfig, runs, Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList(), dataService
        );

        Assertions.assertNotNull(volumeView);
        Assertions.assertNotNull(volumeView.getPerlenkette());
        Assertions.assertEquals(1, volumeView.getPerlenkette().getTotalBeads());
        final PerlenketteBeadDto volumeBead = volumeView.getPerlenkette().getBeads().get(0);
        Assertions.assertEquals("5 tests", volumeBead.getTimestampLabel());
        // 4 passed out of 5 total = 80.0%
        Assertions.assertEquals("80%", volumeBead.getValueFormatted());
        Assertions.assertEquals(80.0, volumeBead.getPercentValue(), 0.1);
        Assertions.assertEquals("All Run Executions (Volume)", volumeBead.getDetails().get("Aggregation Mode"));
        Assertions.assertEquals("5", volumeBead.getDetails().get("Total Tests"));
        Assertions.assertEquals("5", volumeBead.getDetails().get("Total Executions"));
        Assertions.assertEquals("4", volumeBead.getDetails().get("Passed"));
    }

    @Test
    public void testStringOfPearlsReferenceImageSequence()
    {
        // 5 milestone runs matching the reference image: 90, 75, 60, 45, 30
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-pearls-ref", "String of Pearls", WidgetMetric.STATUS, "",
            WidgetPresentation.PERLENKETTE, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, "full", 1
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-90");
        r1.setStartTimeMs(1000L);
        r1.setPassRate(90.0);
        r1.setTotalTests(100);
        r1.setPassedCount(90);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-75");
        r2.setStartTimeMs(2000L);
        r2.setPassRate(75.0);
        r2.setTotalTests(100);
        r2.setPassedCount(75);

        final TestRunEntity r3 = new TestRunEntity();
        r3.setId("run-60");
        r3.setStartTimeMs(3000L);
        r3.setPassRate(60.0);
        r3.setTotalTests(100);
        r3.setPassedCount(60);

        final TestRunEntity r4 = new TestRunEntity();
        r4.setId("run-45");
        r4.setStartTimeMs(4000L);
        r4.setPassRate(45.0);
        r4.setTotalTests(100);
        r4.setPassedCount(45);

        final TestRunEntity r5 = new TestRunEntity();
        r5.setId("run-30");
        r5.setStartTimeMs(5000L);
        r5.setPassRate(30.0);
        r5.setTotalTests(100);
        r5.setPassedCount(30);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1, r2, r3, r4, r5),
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        final PerlenketteDto pearls = view.getPerlenkette();
        Assertions.assertNotNull(pearls);
        Assertions.assertEquals(5, pearls.getTotalBeads());

        // Validate SVG Wave Path and Minor Dots
        Assertions.assertFalse(pearls.getSvgWavePath().isBlank());
        Assertions.assertTrue(pearls.getSvgWavePath().startsWith("M 0.0"));
        Assertions.assertTrue(pearls.getMinorDots().size() >= 5);
        for (final PerlenketteMinorDotDto dot : pearls.getMinorDots())
        {
            Assertions.assertTrue(dot.getRadius() > 0.0);
            Assertions.assertNotNull(dot.getColor());
        }

        // Validate 5 beads matching reference image colors, centered numbers, and 2.5D dark crescent tones
        final List<PerlenketteBeadDto> beads = pearls.getBeads();

        // 1. Green (90)
        Assertions.assertEquals("90", beads.get(0).getInnerValue());
        Assertions.assertEquals("#22c55e", beads.get(0).getColor());
        Assertions.assertEquals("#15803d", beads.get(0).getDarkColor());

        // 2. Blue (75)
        Assertions.assertEquals("75", beads.get(1).getInnerValue());
        Assertions.assertEquals("#2563eb", beads.get(1).getColor());
        Assertions.assertEquals("#1d4ed8", beads.get(1).getDarkColor());

        // 3. Yellow/Gold (60)
        Assertions.assertEquals("60", beads.get(2).getInnerValue());
        Assertions.assertEquals("#eab308", beads.get(2).getColor());
        Assertions.assertEquals("#ca8a04", beads.get(2).getDarkColor());

        // 4. Orange (45)
        Assertions.assertEquals("45", beads.get(3).getInnerValue());
        Assertions.assertEquals("#f97316", beads.get(3).getColor());
        Assertions.assertEquals("#c2410c", beads.get(3).getDarkColor());

        // 5. Red (30)
        Assertions.assertEquals("30", beads.get(4).getInnerValue());
        Assertions.assertEquals("#ef4444", beads.get(4).getColor());
        Assertions.assertEquals("#b91c1c", beads.get(4).getDarkColor());

        // All beads share the exact same midline cy (90.0)
        for (final PerlenketteBeadDto b : beads)
        {
            Assertions.assertEquals(90.0, b.getCy(), 0.01);
            Assertions.assertTrue(b.getRadius() > 0.0);
        }
        // cx positions are strictly ascending
        for (int i = 0; i < beads.size() - 1; i++)
        {
            Assertions.assertTrue(beads.get(i).getCx() < beads.get(i + 1).getCx());
        }
    }

    @Test
    public void testPerlenketteLimitsShellsByWidgetWidth()
    {
        final List<TestRunEntity> runs = new ArrayList<>();
        for (int i = 1; i <= 12; i++)
        {
            final TestRunEntity r = new TestRunEntity();
            r.setId("run-" + i);
            r.setStartTimeMs(1000L * i);
            r.setTotalTests(10);
            r.setPassedCount(8);
            r.setPassRate(80.0);
            runs.add(r);
        }

        // 1. Quarter Width: limit = 4 shells (retaining runs 9, 10, 11, 12)
        final DashboardWidgetConfigDto quarterConfig = new DashboardWidgetConfigDto(
            "w-quarter", "Quarter Pearls", WidgetMetric.STATUS, "", WidgetPresentation.PERLENKETTE,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, "quarter", 1
        );
        final DashboardWidgetViewDto quarterView = service.aggregateWidget(
            quarterConfig, runs, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );
        Assertions.assertNotNull(quarterView);
        final PerlenketteDto quarterPearls = quarterView.getPerlenkette();
        Assertions.assertNotNull(quarterPearls);
        Assertions.assertEquals(4, quarterPearls.getTotalBeads());
        Assertions.assertEquals("run-9", quarterPearls.getBeads().get(0).getId());
        Assertions.assertEquals("run-10", quarterPearls.getBeads().get(1).getId());
        Assertions.assertEquals("run-11", quarterPearls.getBeads().get(2).getId());
        Assertions.assertEquals("run-12", quarterPearls.getBeads().get(3).getId());
        Assertions.assertEquals(360.0, quarterPearls.getSvgWidth(), 0.01);
        Assertions.assertEquals(160.0, quarterPearls.getSvgHeight(), 0.01);

        // 2. Half Width (Default): limit = 6 shells (retaining runs 7..12)
        final DashboardWidgetConfigDto halfConfig = new DashboardWidgetConfigDto(
            "w-half", "Half Pearls", WidgetMetric.STATUS, "", WidgetPresentation.PERLENKETTE,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, "half", 2
        );
        final DashboardWidgetViewDto halfView = service.aggregateWidget(
            halfConfig, runs, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );
        Assertions.assertNotNull(halfView);
        final PerlenketteDto halfPearls = halfView.getPerlenkette();
        Assertions.assertNotNull(halfPearls);
        Assertions.assertEquals(6, halfPearls.getTotalBeads());
        Assertions.assertEquals("run-7", halfPearls.getBeads().get(0).getId());
        Assertions.assertEquals("run-12", halfPearls.getBeads().get(5).getId());
        Assertions.assertEquals(800.0, halfPearls.getSvgWidth(), 0.01);
        Assertions.assertEquals(180.0, halfPearls.getSvgHeight(), 0.01);

        // 3. Full Width: limit = 10 shells (retaining runs 3..12)
        final DashboardWidgetConfigDto fullConfig = new DashboardWidgetConfigDto(
            "w-full", "Full Pearls", WidgetMetric.STATUS, "", WidgetPresentation.PERLENKETTE,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, "full", 3
        );
        final DashboardWidgetViewDto fullView = service.aggregateWidget(
            fullConfig, runs, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );
        Assertions.assertNotNull(fullView);
        final PerlenketteDto fullPearls = fullView.getPerlenkette();
        Assertions.assertNotNull(fullPearls);
        Assertions.assertEquals(10, fullPearls.getTotalBeads());
        Assertions.assertEquals("run-3", fullPearls.getBeads().get(0).getId());
        Assertions.assertEquals("run-12", fullPearls.getBeads().get(9).getId());
        Assertions.assertEquals(1000.0, fullPearls.getSvgWidth(), 0.01);
        Assertions.assertEquals(180.0, fullPearls.getSvgHeight(), 0.01);
    }

    @Test
    public void testStringOfPearlsHigherIsRedPolarity()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-pearls-inverted", "Inverted Polarity Strand", WidgetMetric.STATUS, "", WidgetPresentation.PERLENKETTE,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, "full", 1, WidgetColorPolarity.HIGHER_IS_RED
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-90");
        r1.setStartTimeMs(1000L);
        r1.setPassRate(90.0);
        r1.setTotalTests(100);
        r1.setPassedCount(90);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-75");
        r2.setStartTimeMs(2000L);
        r2.setPassRate(75.0);
        r2.setTotalTests(100);
        r2.setPassedCount(75);

        final TestRunEntity r3 = new TestRunEntity();
        r3.setId("run-60");
        r3.setStartTimeMs(3000L);
        r3.setPassRate(60.0);
        r3.setTotalTests(100);
        r3.setPassedCount(60);

        final TestRunEntity r4 = new TestRunEntity();
        r4.setId("run-45");
        r4.setStartTimeMs(4000L);
        r4.setPassRate(45.0);
        r4.setTotalTests(100);
        r4.setPassedCount(45);

        final TestRunEntity r5 = new TestRunEntity();
        r5.setId("run-30");
        r5.setStartTimeMs(5000L);
        r5.setPassRate(30.0);
        r5.setTotalTests(100);
        r5.setPassedCount(30);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1, r2, r3, r4, r5),
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        final PerlenketteDto pearls = view.getPerlenkette();
        Assertions.assertNotNull(pearls);
        Assertions.assertEquals(5, pearls.getTotalBeads());

        final List<PerlenketteBeadDto> beads = pearls.getBeads();

        // Under HIGHER_IS_RED:
        // 1. Red (90)
        Assertions.assertEquals("90", beads.get(0).getInnerValue());
        Assertions.assertEquals("#ef4444", beads.get(0).getColor());
        Assertions.assertEquals("#b91c1c", beads.get(0).getDarkColor());

        // 2. Orange (75)
        Assertions.assertEquals("75", beads.get(1).getInnerValue());
        Assertions.assertEquals("#f97316", beads.get(1).getColor());
        Assertions.assertEquals("#c2410c", beads.get(1).getDarkColor());

        // 3. Yellow/Amber (60)
        Assertions.assertEquals("60", beads.get(2).getInnerValue());
        Assertions.assertEquals("#eab308", beads.get(2).getColor());
        Assertions.assertEquals("#ca8a04", beads.get(2).getDarkColor());

        // 4. Blue (45)
        Assertions.assertEquals("45", beads.get(3).getInnerValue());
        Assertions.assertEquals("#2563eb", beads.get(3).getColor());
        Assertions.assertEquals("#1d4ed8", beads.get(3).getDarkColor());

        // 5. Green (30)
        Assertions.assertEquals("30", beads.get(4).getInnerValue());
        Assertions.assertEquals("#22c55e", beads.get(4).getColor());
        Assertions.assertEquals("#15803d", beads.get(4).getDarkColor());
    }

    @Test
    public void testStringOfPearlsPolarityWithUnknownFailures()
    {
        // 1. Higher is Green: Run with 97% pass rate and 3 unknown failures must be GREEN (#22c55e),
        // not overridden to RED by statusClass
        final DashboardWidgetConfigDto greenConfig = new DashboardWidgetConfigDto(
            "w-green", "High is Green", WidgetMetric.STATUS, "", WidgetPresentation.PERLENKETTE,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, "half", 1, WidgetColorPolarity.HIGHER_IS_GREEN
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-97");
        r1.setStartTimeMs(1000L);
        r1.setPassRate(97.0);
        r1.setTotalTests(116);
        r1.setPassedCount(113);
        r1.setFailedUnknownCount(3);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-35");
        r2.setStartTimeMs(2000L);
        r2.setPassRate(35.0);
        r2.setTotalTests(100);
        r2.setPassedCount(35);
        r2.setFailedUnknownCount(65);

        final DashboardWidgetViewDto greenView = service.aggregateWidget(
            greenConfig, List.of(r1, r2),
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(greenView);
        final PerlenketteDto greenPearls = greenView.getPerlenkette();
        Assertions.assertNotNull(greenPearls);
        Assertions.assertEquals(2, greenPearls.getTotalBeads());
        // r1 (97%) -> GREEN
        Assertions.assertEquals("97", greenPearls.getBeads().get(0).getInnerValue());
        Assertions.assertEquals("#22c55e", greenPearls.getBeads().get(0).getColor());
        Assertions.assertEquals("#15803d", greenPearls.getBeads().get(0).getDarkColor());
        Assertions.assertEquals("bead-unknown", greenPearls.getBeads().get(0).getStatusClass());
        // r2 (35%) -> RED
        Assertions.assertEquals("35", greenPearls.getBeads().get(1).getInnerValue());
        Assertions.assertEquals("#ef4444", greenPearls.getBeads().get(1).getColor());
        Assertions.assertEquals("#b91c1c", greenPearls.getBeads().get(1).getDarkColor());
        Assertions.assertEquals("bead-unknown", greenPearls.getBeads().get(1).getStatusClass());

        // 2. Higher is Red: Same runs inverted
        final DashboardWidgetConfigDto redConfig = new DashboardWidgetConfigDto(
            "w-red", "High is Red", WidgetMetric.STATUS, "", WidgetPresentation.PERLENKETTE,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, "half", 2, WidgetColorPolarity.HIGHER_IS_RED
        );

        final DashboardWidgetViewDto redView = service.aggregateWidget(
            redConfig, List.of(r1, r2),
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(redView);
        final PerlenketteDto redPearls = redView.getPerlenkette();
        Assertions.assertNotNull(redPearls);
        // r1 (97%) -> RED
        Assertions.assertEquals("97", redPearls.getBeads().get(0).getInnerValue());
        Assertions.assertEquals("#ef4444", redPearls.getBeads().get(0).getColor());
        Assertions.assertEquals("#b91c1c", redPearls.getBeads().get(0).getDarkColor());
        // r2 (35%) -> GREEN
        Assertions.assertEquals("35", redPearls.getBeads().get(1).getInnerValue());
        Assertions.assertEquals("#22c55e", redPearls.getBeads().get(1).getColor());
        Assertions.assertEquals("#15803d", redPearls.getBeads().get(1).getDarkColor());
    }

    @Test
    public void testTrendGraph_ShowDots_PopulatesGranularStateBreakdown()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-trend-dots", "Status Trend with Dots", WidgetMetric.STATUS, "",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, true
        ).withShowDots(true);

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-101");
        r1.setStartTimeMs(1000L);
        r1.setPassedCount(5);
        r1.setPassedHealedCount(2);
        r1.setPassedAiCount(1); // passTotal = 5, passClean = 2, passHealed = 2, passAi = 1
        r1.setSucceededFixedCount(2);
        r1.setSucceededFixedHealedCount(1); // fixedTotal = 2, fixedClean = 1, fixedHealed = 1, fixedAi = 0
        r1.setFailedKnownCount(1); // knownTotal = 1, knownClean = 1
        r1.setFailedUnknownCount(2); // unknownTotal = 2, unknownClean = 2
        r1.setIgnoredCount(3);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertTrue(view.isShowDots());
        Assertions.assertEquals(1, view.getTrendPoints().size());

        final WidgetTrendPointDto pt = view.getTrendPoints().get(0);
        Assertions.assertEquals("run-101", pt.getRunId());
        Assertions.assertTrue(pt.hasDetailedStatus());
        Assertions.assertEquals(13, pt.getTotalTests()); // 5 passed + 2 fixed + 1 known + 2 unknown + 3 ignored = 13
        Assertions.assertEquals(5, pt.getPassTotal());
        Assertions.assertEquals(2, pt.getPassClean());
        Assertions.assertEquals(2, pt.getPassHealed());
        Assertions.assertEquals(1, pt.getPassAi());

        Assertions.assertEquals(2, pt.getFixedTotal());
        Assertions.assertEquals(1, pt.getFixedClean());
        Assertions.assertEquals(1, pt.getFixedHealed());
        Assertions.assertEquals(0, pt.getFixedAi());

        Assertions.assertEquals(1, pt.getKnownTotal());
        Assertions.assertEquals(1, pt.getKnownClean());

        Assertions.assertEquals(2, pt.getUnknownTotal());
        Assertions.assertEquals(2, pt.getUnknownClean());

        Assertions.assertEquals(3, pt.getIgnoredCount());
        Assertions.assertTrue(pt.getStateBadgeText().contains("PASS") || pt.getStateBadgeText().contains("%"));
        Assertions.assertNotNull(pt.getStateBadgeClass());
    }

    @Test
    public void testTrendGraph_ShowDots_DefaultFalseAndNonStatusMetric()
    {
        final DashboardWidgetConfigDto defaultConfig = new DashboardWidgetConfigDto(
            "w-trend-bugs", "Bugs Trend", WidgetMetric.BUGS, "",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );

        Assertions.assertFalse(defaultConfig.isShowDots(), "showDots must default to false");

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-b1");
        r1.setStartTimeMs(1000L);
        r1.setTotalTests(20);

        final DashboardWidgetViewDto viewDefault = service.aggregateWidget(
            defaultConfig, List.of(r1), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(viewDefault);
        Assertions.assertFalse(viewDefault.isShowDots());
        Assertions.assertEquals(1, viewDefault.getTrendPoints().size());
        Assertions.assertFalse(viewDefault.getTrendPoints().get(0).hasDetailedStatus());

        final DashboardWidgetConfigDto enabledConfig = defaultConfig.withShowDots(true);
        Assertions.assertTrue(enabledConfig.isShowDots());

        final DashboardWidgetViewDto viewEnabled = service.aggregateWidget(
            enabledConfig, List.of(r1), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(viewEnabled);
        Assertions.assertTrue(viewEnabled.isShowDots());
    }

    @Test
    public void testWidgetPresentationNewOptionsNamingAndProperties()
    {
        // TEXT
        Assertions.assertEquals("Text", WidgetPresentation.TEXT.getDisplayName());
        Assertions.assertEquals(WidgetPresentation.TEXT, WidgetPresentation.fromString("TEXT"));
        Assertions.assertEquals(WidgetPresentation.TEXT, WidgetPresentation.fromString("text"));
        Assertions.assertTrue(WidgetPresentation.TEXT.isStaticPoint());
        Assertions.assertFalse(WidgetPresentation.TEXT.isTrendBased());

        // TEXT_WITH_SMALL_TREND
        Assertions.assertEquals("Text with Small Trend", WidgetPresentation.TEXT_WITH_SMALL_TREND.getDisplayName());
        Assertions.assertEquals(WidgetPresentation.TEXT_WITH_SMALL_TREND, WidgetPresentation.fromString("TEXT_WITH_SMALL_TREND"));
        Assertions.assertEquals(WidgetPresentation.TEXT_WITH_SMALL_TREND, WidgetPresentation.fromString("text_with_trend"));
        Assertions.assertEquals(WidgetPresentation.TEXT_WITH_SMALL_TREND, WidgetPresentation.fromString("sparkline"));
        Assertions.assertEquals(WidgetPresentation.TEXT_WITH_SMALL_TREND, WidgetPresentation.fromString("text-small-trend"));
        Assertions.assertTrue(WidgetPresentation.TEXT_WITH_SMALL_TREND.isTrendBased());

        // LIST
        Assertions.assertEquals("List", WidgetPresentation.LIST.getDisplayName());
        Assertions.assertEquals(WidgetPresentation.LIST, WidgetPresentation.fromString("LIST"));
        Assertions.assertEquals(WidgetPresentation.LIST, WidgetPresentation.fromString("breakdown_list"));
        Assertions.assertEquals(WidgetPresentation.LIST, WidgetPresentation.fromString("item-list"));
        Assertions.assertTrue(WidgetPresentation.LIST.isStaticPoint());
        Assertions.assertFalse(WidgetPresentation.LIST.isTrendBased());
    }

    @Test
    public void testTextWidgetPresentationStatus()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-text-status", "Status KPI", WidgetMetric.STATUS, "",
            WidgetPresentation.TEXT, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );

        final BatchLatestVariationDto v1 = new BatchLatestVariationDto("k1", "passed-clean", false, "JAVA", "run-1", "Default", "Chrome", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L);
        final BatchLatestVariationDto v2 = new BatchLatestVariationDto("k2", "passed-clean", false, "JAVA", "run-1", "Default", "Chrome", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, Collections.emptyList(), List.of(v1, v2), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertEquals(WidgetPresentation.TEXT, view.getPresentation());
        Assertions.assertTrue(view.hasData());
        Assertions.assertTrue(view.getPrimaryTextValue().contains("100%"));
        Assertions.assertEquals("2 Tests", view.getSecondaryTextValue());
    }

    @Test
    public void testTextWithSmallTrendWidget()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-text-trend", "Pass Rate Sparkline", WidgetMetric.STATUS, "",
            WidgetPresentation.TEXT_WITH_SMALL_TREND, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-1");
        r1.setStartTimeMs(1000L);
        r1.setTotalTests(10);
        r1.setPassedCount(8);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-2");
        r2.setStartTimeMs(2000L);
        r2.setTotalTests(10);
        r2.setPassedCount(10);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1, r2), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertEquals(WidgetPresentation.TEXT_WITH_SMALL_TREND, view.getPresentation());
        Assertions.assertTrue(view.hasData());
        Assertions.assertFalse(view.getSparklineSvgPath().isEmpty());
        Assertions.assertTrue(view.getSparklineSvgPath().startsWith("M "));
        Assertions.assertEquals(2, view.getTrendPoints().size());
    }

    @Test
    public void testListWidgetPresentationStatusAndCosts()
    {
        final DashboardWidgetConfigDto statusListConfig = new DashboardWidgetConfigDto(
            "w-list-status", "Status Breakdown List", WidgetMetric.STATUS, "",
            WidgetPresentation.LIST, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );

        final BatchLatestVariationDto v1 = new BatchLatestVariationDto(
            "CurrencySwitchTest|verifyPriceDisplayPerMarket|UK|br:Chrome", "passed-clean", false, "JAVA", "run-1",
            "STG", "Chrome", "UK", "Checkout", "Default",
            Map.of("locale", "UK", "Area", "Checkout", "Branch", "main", "Custom", "Not Custom"), 0.0, 0L
        );
        final BatchLatestVariationDto v2 = new BatchLatestVariationDto("k2", "failed-known", false, "JAVA", "run-1", "Default", "Chrome", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L);

        final DashboardWidgetViewDto statusView = service.aggregateWidget(
            statusListConfig, Collections.emptyList(), List.of(v1, v2), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(statusView);
        Assertions.assertEquals(WidgetPresentation.LIST, statusView.getPresentation());
        Assertions.assertTrue(statusView.hasData());
        Assertions.assertTrue(statusView.hasListItems());
        Assertions.assertEquals(2, statusView.getListItems().size());

        final WidgetListItemDto passedItem = statusView.getListItems().stream()
            .filter(i -> i.getLabel().contains("Passed"))
            .findFirst()
            .orElseThrow();
        Assertions.assertTrue(passedItem.hasSubItems());
        Assertions.assertEquals(1, passedItem.getSubItems().size());
        final WidgetListItemDto subPass = passedItem.getSubItems().get(0);
        Assertions.assertEquals("verifyPriceDisplayPerMarket [UK]", subPass.getLabel());
        Assertions.assertEquals("CurrencySwitchTest", subPass.getSecondaryLabel());
        Assertions.assertEquals(v1.getKey(), subPass.getVariationKey());
        Assertions.assertEquals(v1.getVariationId(), subPass.getVariationId());
        Assertions.assertTrue(subPass.hasTags());
        Assertions.assertTrue(subPass.getTags().contains("Chrome"));
        Assertions.assertTrue(subPass.getTags().contains("UK"));
        Assertions.assertTrue(subPass.getTags().contains("@Checkout"));
        Assertions.assertTrue(subPass.getTags().contains("STG"));
        Assertions.assertTrue(subPass.getTags().contains("Branch:main"));
        // Negative boolean 'Not Custom' and duplicate 'locale:UK' must be excluded
        Assertions.assertFalse(subPass.getTags().contains("Custom:Not Custom"));
        Assertions.assertFalse(subPass.getTags().contains("locale:UK"));
        Assertions.assertEquals("run-1", subPass.getRunId());
        Assertions.assertTrue(subPass.hasRunId());
        Assertions.assertTrue(subPass.hasReportUrl());
        Assertions.assertTrue(subPass.getReportUrl().startsWith("/run-report?runId=run-1"));

        final DashboardWidgetConfigDto costListConfig = new DashboardWidgetConfigDto(
            "w-list-costs", "Costs Breakdown List", WidgetMetric.COSTS, "",
            WidgetPresentation.LIST, WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );

        final TokenCostTrendDto costTrend = new TokenCostTrendDto(100.0, 50.0, 1000L, List.of(50.0, 100.0));
        final TestRunEntity runEntry = new TestRunEntity();
        runEntry.setId("run-cost-42");
        runEntry.setTotalLlmCost(0.1234);
        runEntry.setTotalTests(8);
        runEntry.setStartTimeMs(5000L);

        final DashboardWidgetViewDto costView = service.aggregateWidget(
            costListConfig, List.of(runEntry), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            costTrend, null, Collections.emptyList()
        );

        Assertions.assertNotNull(costView);
        Assertions.assertTrue(costView.hasListItems());
        Assertions.assertTrue(costView.getListItems().get(0).hasSubItems());
        final WidgetListItemDto runItem = costView.getListItems().stream()
            .filter(i -> i.getLabel().contains("Run #run-cost-42"))
            .findFirst()
            .orElseThrow();
        Assertions.assertEquals("run-cost-42", runItem.getRunId());
        Assertions.assertTrue(runItem.hasReportUrl());
        Assertions.assertEquals("/run-report?runId=run-cost-42", runItem.getReportUrl());
    }

    @Test
    public void testListWidgetPresentationBugsWithSubItemsAndTags()
    {
        final DashboardWidgetConfigDto bugListConfig = new DashboardWidgetConfigDto(
            "w-list-bugs", "Bugs List", WidgetMetric.BUGS, "",
            WidgetPresentation.LIST, WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );

        final TestBaseBugEntity bug1 = new TestBaseBugEntity(
            "CheckoutTest#testPayment", "BUG-12345", "DefaultBatch", "staging",
            System.currentTimeMillis(), "run-104", "MAJOR", "browser:Chrome, locale:en-US"
        );
        final TestBaseBugEntity bug2 = new TestBaseBugEntity(
            "LoginTest#testInvalidAuth", "BUG-99999", "ALL", "ALL",
            System.currentTimeMillis(), "run-104", "CRITICAL", "browser:Firefox"
        );

        final DashboardWidgetViewDto bugView = service.aggregateWidget(
            bugListConfig, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, List.of(bug1, bug2)
        );

        Assertions.assertNotNull(bugView);
        Assertions.assertEquals(WidgetPresentation.LIST, bugView.getPresentation());
        Assertions.assertTrue(bugView.hasListItems());
        Assertions.assertEquals(2, bugView.getListItems().size());

        final WidgetListItemDto criticalItem = bugView.getListItems().stream()
            .filter(i -> "CRITICAL".equalsIgnoreCase(i.getLabel()))
            .findFirst()
            .orElseThrow();
        Assertions.assertTrue(criticalItem.hasSubItems());
        Assertions.assertEquals(1, criticalItem.getSubItems().size());
        final WidgetListItemDto subBug = criticalItem.getSubItems().get(0);
        Assertions.assertEquals("run-104", subBug.getRunId());
        Assertions.assertTrue(subBug.hasReportUrl());
        Assertions.assertTrue(subBug.getReportUrl().startsWith("/run-report?runId=run-104"));
        final WidgetListItemDto subCritical = criticalItem.getSubItems().get(0);
        Assertions.assertEquals("BUG-99999", subCritical.getLabel());
        Assertions.assertEquals("LoginTest#testInvalidAuth", subCritical.getSecondaryLabel());
        Assertions.assertEquals("LoginTest#testInvalidAuth", subCritical.getVariationId());
        Assertions.assertTrue(subCritical.hasTags());
        Assertions.assertTrue(subCritical.getTags().contains("browser:Firefox"));

        final WidgetListItemDto majorItem = bugView.getListItems().stream()
            .filter(i -> "MAJOR".equalsIgnoreCase(i.getLabel()))
            .findFirst()
            .orElseThrow();
        Assertions.assertTrue(majorItem.hasSubItems());
        Assertions.assertEquals(1, majorItem.getSubItems().size());
        final WidgetListItemDto subMajor = majorItem.getSubItems().get(0);
        Assertions.assertEquals("BUG-12345", subMajor.getLabel());
        Assertions.assertEquals("CheckoutTest#testPayment", subMajor.getSecondaryLabel());
        Assertions.assertEquals("CheckoutTest#testPayment", subMajor.getVariationId());
        Assertions.assertTrue(subMajor.hasTags());
        Assertions.assertTrue(subMajor.getTags().contains("browser:Chrome"));
        Assertions.assertTrue(subMajor.getTags().contains("locale:en-US"));
        Assertions.assertTrue(subMajor.getTags().contains("staging"));
        Assertions.assertTrue(subMajor.getTags().contains("DefaultBatch"));
    }

    @Test
    public void testBugsPointWidgetsDeduplicateByTicketWithAllReferencesInList()
    {
        final DashboardWidgetConfigDto pieConfig = new DashboardWidgetConfigDto(
            "w-bugs-pie", "Bugs Donut", WidgetMetric.BUGS, "",
            WidgetPresentation.PIE_CHART, WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );
        final DashboardWidgetConfigDto ratioConfig = new DashboardWidgetConfigDto(
            "w-bugs-ratio", "Bugs Ratio", WidgetMetric.BUGS, "",
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );
        final DashboardWidgetConfigDto textConfig = new DashboardWidgetConfigDto(
            "w-bugs-text", "Bugs Text", WidgetMetric.BUGS, "",
            WidgetPresentation.TEXT, WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );
        final DashboardWidgetConfigDto listConfig = new DashboardWidgetConfigDto(
            "w-bugs-list", "Bugs List", WidgetMetric.BUGS, "",
            WidgetPresentation.LIST, WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );

        // 3 assignments for 2 unique bug tickets:
        // BUG-12345 (MAJOR) appears on 2 variations: testPayment and testShipping
        // BUG-99999 (CRITICAL) appears on 1 variation: testInvalidAuth
        final TestBaseBugEntity bug1 = new TestBaseBugEntity(
            "CheckoutTest#testPayment", "BUG-12345", "DefaultBatch", "staging",
            System.currentTimeMillis(), "run-104", "MAJOR", "browser:Chrome"
        );
        final TestBaseBugEntity bug2 = new TestBaseBugEntity(
            "CheckoutTest#testShipping", "BUG-12345", "DefaultBatch", "staging",
            System.currentTimeMillis(), "run-104", "MAJOR", "browser:Firefox"
        );
        final TestBaseBugEntity bug3 = new TestBaseBugEntity(
            "LoginTest#testInvalidAuth", "BUG-99999", "ALL", "ALL",
            System.currentTimeMillis(), "run-104", "CRITICAL", "browser:Edge"
        );
        final List<TestBaseBugEntity> bugList = List.of(bug1, bug2, bug3);

        // 1. PIE CHART: Displays unique bug count in center and unique slice counts
        final DashboardWidgetViewDto pieView = service.aggregateWidget(
            pieConfig, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, bugList
        );
        Assertions.assertNotNull(pieView);
        Assertions.assertEquals("2", pieView.getPieCenterRate());
        Assertions.assertEquals("2 Active Bugs", pieView.getSummaryLabel());
        Assertions.assertEquals(2, pieView.getPieSlices().size());
        final WidgetPieSliceDto pieCritical = pieView.getPieSlices().stream()
            .filter(s -> "CRITICAL".equalsIgnoreCase(s.getLabel())).findFirst().orElseThrow();
        Assertions.assertEquals(1, pieCritical.getCount());
        Assertions.assertEquals(50.0, pieCritical.getPercentage(), 0.01);
        final WidgetPieSliceDto pieMajor = pieView.getPieSlices().stream()
            .filter(s -> "MAJOR".equalsIgnoreCase(s.getLabel())).findFirst().orElseThrow();
        Assertions.assertEquals(1, pieMajor.getCount());
        Assertions.assertEquals(50.0, pieMajor.getPercentage(), 0.01);

        // 2. RATIO BAR: Segments display unique bug count
        final DashboardWidgetViewDto ratioView = service.aggregateWidget(
            ratioConfig, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, bugList
        );
        Assertions.assertNotNull(ratioView);
        Assertions.assertEquals(2, ratioView.getRatioSegments().size());
        final DistributionRatioDto ratioMajor = ratioView.getRatioSegments().stream()
            .filter(s -> "MAJOR".equalsIgnoreCase(s.getName())).findFirst().orElseThrow();
        Assertions.assertEquals(1, ratioMajor.getCount());

        // 3. TEXT (KPI): Displays unique bug count
        final DashboardWidgetViewDto textView = service.aggregateWidget(
            textConfig, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, bugList
        );
        Assertions.assertNotNull(textView);
        Assertions.assertEquals("2 Bugs", textView.getPrimaryTextValue());

        // 4. LIST: Severity group shows unique count, but subItems retain all references
        final DashboardWidgetViewDto listView = service.aggregateWidget(
            listConfig, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, bugList
        );
        Assertions.assertNotNull(listView);
        Assertions.assertEquals(2, listView.getListItems().size());

        final WidgetListItemDto listMajor = listView.getListItems().stream()
            .filter(i -> "MAJOR".equalsIgnoreCase(i.getLabel())).findFirst().orElseThrow();
        // Group count must be 1 unique bug
        Assertions.assertEquals(1, listMajor.getCount());
        Assertions.assertEquals("1 (50.0%)", listMajor.getValueFormatted());
        // Sub-items must preserve both occurrences/references
        Assertions.assertEquals(2, listMajor.getSubItems().size());
        Assertions.assertEquals("BUG-12345", listMajor.getSubItems().get(0).getLabel());
        Assertions.assertEquals("CheckoutTest#testPayment", listMajor.getSubItems().get(0).getSecondaryLabel());
        Assertions.assertEquals("BUG-12345", listMajor.getSubItems().get(1).getLabel());
        Assertions.assertEquals("CheckoutTest#testShipping", listMajor.getSubItems().get(1).getSecondaryLabel());
    }

    @Test
    public void testAiUsagePieChartAggregation()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-ai-pie", "AI Usage Donut", WidgetMetric.AI_USAGE, "", WidgetPresentation.PIE_CHART,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, "half", 0
        );

        final List<BatchLatestVariationDto> variations = List.of(
            new BatchLatestVariationDto("v1", "passed", false, "AI", "run-1", "Default", "Chrome", "US", "Search", "Default", Collections.emptyMap(), 0.0, 0L),
            new BatchLatestVariationDto("v2", "passed", true, "JAVA", "run-1", "Default", "Chrome", "US", "Search", "Default", Collections.emptyMap(), 0.0, 0L),
            new BatchLatestVariationDto("v3", "passed", false, "PLAYBOOK", "run-1", "Default", "Chrome", "US", "Search", "Default", Collections.emptyMap(), 0.0, 0L),
            new BatchLatestVariationDto("v4", "passed", false, "JAVA", "run-1", "Default", "Chrome", "US", "Search", "Default", Collections.emptyMap(), 0.0, 0L)
        );

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, Collections.emptyList(), variations, Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertEquals("50%", view.getPieCenterRate()); // 2 out of 4 tests use AI (AI, Healed)
        Assertions.assertEquals("AI", view.getPieCenterLabel());
        Assertions.assertEquals(4, view.getPieSlices().size());

        final WidgetPieSliceDto aiSlice = view.getPieSlices().stream().filter(s -> "AI-Driven".equals(s.getLabel())).findFirst().orElseThrow();
        Assertions.assertEquals(1, aiSlice.getCount());
        Assertions.assertEquals("#8b5cf6", aiSlice.getColor());

        final WidgetPieSliceDto healedSlice = view.getPieSlices().stream().filter(s -> "Healed".equals(s.getLabel())).findFirst().orElseThrow();
        Assertions.assertEquals(1, healedSlice.getCount());
        Assertions.assertEquals("#10b981", healedSlice.getColor());

        final WidgetPieSliceDto playbookSlice = view.getPieSlices().stream().filter(s -> "Playbook".equals(s.getLabel())).findFirst().orElseThrow();
        Assertions.assertEquals(1, playbookSlice.getCount());
        Assertions.assertEquals("#0284c7", playbookSlice.getColor());

        final WidgetPieSliceDto classicSlice = view.getPieSlices().stream().filter(s -> "Classic".equals(s.getLabel())).findFirst().orElseThrow();
        Assertions.assertEquals(1, classicSlice.getCount());
        Assertions.assertEquals("#64748b", classicSlice.getColor());
    }

    @Test
    public void testAiUsageRatioBarAggregation()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-ai-ratio", "AI Usage Ratio", WidgetMetric.AI_USAGE, "", WidgetPresentation.RATIO_BAR,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, "half", 0
        );

        final List<BatchLatestVariationDto> variations = List.of(
            new BatchLatestVariationDto("v1", "passed", false, "AI", "run-1", "Default", "Chrome", "US"),
            new BatchLatestVariationDto("v2", "passed", true, "JAVA", "run-1", "Default", "Chrome", "US")
        );

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, Collections.emptyList(), variations, Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertEquals(WidgetPresentation.RATIO_BAR, view.getPresentation());
        Assertions.assertFalse(view.getRatioSegments().isEmpty());
        Assertions.assertEquals(2, view.getRatioSegments().size());
        Assertions.assertTrue(view.getRatioSegments().stream().anyMatch(s -> "AI-Driven".equals(s.getName())));
        Assertions.assertTrue(view.getRatioSegments().stream().anyMatch(s -> "Healed".equals(s.getName())));
    }

    @Test
    public void testAiUsagePerlenketteAggregation()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-ai-perle", "AI Milestone", WidgetMetric.AI_USAGE, "", WidgetPresentation.PERLENKETTE,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, "half", 0
        );

        final BatchSuiteEvolutionPointDto pt1 = new BatchSuiteEvolutionPointDto(
            "run-10", 0, "2026-11-01 10:00", 10, 0, 5, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 80.0
        );
        final BatchSuiteEvolutionPointDto pt2 = new BatchSuiteEvolutionPointDto(
            "run-11", 1, "2026-11-02 10:00", 10, 0, 4, 3, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 90.0
        );
        final BatchSuiteEvolutionDto evo = new BatchSuiteEvolutionDto(List.of(pt1, pt2));

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, evo, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertEquals(WidgetPresentation.PERLENKETTE, view.getPresentation());
        Assertions.assertNotNull(view.getPerlenkette());
        Assertions.assertEquals(2, view.getPerlenkette().getBeads().size());
        final PerlenketteBeadDto bead = view.getPerlenkette().getBeads().get(0);
        Assertions.assertEquals("bead-ai", bead.getStatusClass());
        Assertions.assertEquals("#8b5cf6", bead.getColor(), "AI pearls must use designated AI purple");
        Assertions.assertEquals("#6442b1", bead.getDarkColor());
        Assertions.assertEquals("30", bead.getInnerValue(), "Must show 30% without Playbooks in AI count");
        Assertions.assertEquals("30%", bead.getValueFormatted());
        Assertions.assertEquals("30.0%", bead.getDetails().get("AI Usage Rate"));
        Assertions.assertEquals("3", bead.getDetails().get("Total AI Tests"));
        Assertions.assertEquals("2", bead.getDetails().get("Playbook"));

        final PerlenketteBeadDto bead2 = view.getPerlenkette().getBeads().get(1);
        Assertions.assertEquals("bead-ai", bead2.getStatusClass());
        Assertions.assertEquals("#581c87", bead2.getColor(), "50% AI usage must use royal dark violet");
        Assertions.assertEquals("#3b0764", bead2.getDarkColor());
        Assertions.assertEquals("50", bead2.getInnerValue());
        Assertions.assertEquals("50%", bead2.getValueFormatted());
        Assertions.assertEquals("50.0%", bead2.getDetails().get("AI Usage Rate"));
        Assertions.assertEquals("5", bead2.getDetails().get("Total AI Tests"));
    }

    @Test
    public void testPerlenketteAiUsagePurpleIntensityGradient()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-ai-gradient", "AI Gradient Chain", WidgetMetric.AI_USAGE, "", WidgetPresentation.PERLENKETTE,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, "full", 10
        );

        // Tier 1: 0% AI -> Pale lavender (#c4b5fd / #8b5cf6)
        final BatchSuiteEvolutionPointDto pt1 = new BatchSuiteEvolutionPointDto(
            "run-0", 0, "2026-11-01 10:00", 10, 0, 10, 0, 0, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 100.0
        );

        // Tier 2: 10% AI -> Soft purple/lavender (#a78bfa / #7c3aed)
        final BatchSuiteEvolutionPointDto pt2 = new BatchSuiteEvolutionPointDto(
            "run-10", 1, "2026-11-02 10:00", 10, 0, 9, 1, 0, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 90.0
        );

        // Tier 3: 25% AI -> Vivid purple (#8b5cf6 / #6442b1)
        final BatchSuiteEvolutionPointDto pt3 = new BatchSuiteEvolutionPointDto(
            "run-25", 2, "2026-11-03 10:00", 20, 0, 15, 3, 2, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 75.0
        );

        // Tier 4: 40% AI -> Deep purple (#7c3aed / #5b21b6)
        final BatchSuiteEvolutionPointDto pt4 = new BatchSuiteEvolutionPointDto(
            "run-40", 3, "2026-11-04 10:00", 10, 0, 6, 3, 1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 60.0
        );

        // Tier 5: 60% AI -> Royal dark violet (#581c87 / #3b0764)
        final BatchSuiteEvolutionPointDto pt5 = new BatchSuiteEvolutionPointDto(
            "run-60", 4, "2026-11-05 10:00", 10, 0, 4, 4, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 40.0
        );

        final BatchSuiteEvolutionDto evo = new BatchSuiteEvolutionDto(List.of(pt1, pt2, pt3, pt4, pt5));

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, evo, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertEquals(5, view.getPerlenkette().getTotalBeads());

        final List<PerlenketteBeadDto> beads = view.getPerlenkette().getBeads();

        // Tier 1: 0% -> Pale lavender
        Assertions.assertEquals("#c4b5fd", beads.get(0).getColor());
        Assertions.assertEquals("#8b5cf6", beads.get(0).getDarkColor());

        // Tier 2: 10% -> Soft lavender
        Assertions.assertEquals("#a78bfa", beads.get(1).getColor());
        Assertions.assertEquals("#7c3aed", beads.get(1).getDarkColor());

        // Tier 3: 25% -> Vivid purple
        Assertions.assertEquals("#8b5cf6", beads.get(2).getColor());
        Assertions.assertEquals("#6442b1", beads.get(2).getDarkColor());

        // Tier 4: 40% -> Deep purple
        Assertions.assertEquals("#7c3aed", beads.get(3).getColor());
        Assertions.assertEquals("#5b21b6", beads.get(3).getDarkColor());

        // Tier 5: 60% -> Royal dark violet
        Assertions.assertEquals("#581c87", beads.get(4).getColor());
        Assertions.assertEquals("#3b0764", beads.get(4).getDarkColor());
    }

    @Test
    public void testAiUsageTrendGraphAggregation()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-ai-trend", "AI Usage Trend", WidgetMetric.AI_USAGE, "", WidgetPresentation.TREND_GRAPH,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, "full", 0
        );

        final BatchSuiteEvolutionPointDto pt1 = new BatchSuiteEvolutionPointDto(
            "run-1", 0, "2026-11-01 10:00", 10, 0, 5, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 60.0
        );
        final BatchSuiteEvolutionPointDto pt2 = new BatchSuiteEvolutionPointDto(
            "run-2", 1, "2026-11-02 10:00", 10, 0, 4, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 70.0
        );
        final BatchSuiteEvolutionDto evo = new BatchSuiteEvolutionDto(List.of(pt1, pt2));

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, evo, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertEquals(WidgetPresentation.TREND_GRAPH, view.getPresentation());
        Assertions.assertEquals("#8b5cf6", view.getTrendColor());
        Assertions.assertEquals(2, view.getTrendPoints().size());
        Assertions.assertFalse(view.getTrendLayers().isEmpty());
        Assertions.assertTrue(view.getTrendLayers().stream().anyMatch(l -> "AI-Driven".equals(l.getName())));
        Assertions.assertTrue(view.getTrendLayers().stream().anyMatch(l -> "Playbook".equals(l.getName())));
        Assertions.assertTrue(view.getTrendLayers().stream().allMatch(WidgetTrendLayerDto::hasLinePath),
            "All automation trend layers must have standalone line paths for legend selection");
    }

    @Test
    public void testAiUsageTextWidgetAggregation()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-ai-text", "AI Usage KPI", WidgetMetric.AI_USAGE, "", WidgetPresentation.TEXT,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, "quarter", 0
        );

        final List<BatchLatestVariationDto> variations = List.of(
            new BatchLatestVariationDto("v1", "passed", false, "AI", "run-1", "Default", "Chrome", "US"),
            new BatchLatestVariationDto("v2", "passed", true, "JAVA", "run-1", "Default", "Chrome", "US"),
            new BatchLatestVariationDto("v3", "passed", false, "JAVA", "run-1", "Default", "Chrome", "US")
        );

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, Collections.emptyList(), variations, Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertEquals(WidgetPresentation.TEXT, view.getPresentation());
        Assertions.assertEquals("33% AI · 33% Healed · 0% Playbook", view.getPrimaryTextValue());
        Assertions.assertEquals("#8b5cf6", view.getTextValueColor());
        Assertions.assertEquals(3, view.getRatioSegments().size());
        Assertions.assertEquals("AI-Driven", view.getRatioSegments().get(0).getName());
        Assertions.assertEquals(1, view.getRatioSegments().get(0).getCount());
        Assertions.assertEquals("#8b5cf6", view.getRatioSegments().get(0).getColor());
        Assertions.assertEquals("Healed", view.getRatioSegments().get(1).getName());
        Assertions.assertEquals(1, view.getRatioSegments().get(1).getCount());
        Assertions.assertEquals("#10b981", view.getRatioSegments().get(1).getColor());
        Assertions.assertEquals("Playbook", view.getRatioSegments().get(2).getName());
        Assertions.assertEquals(0, view.getRatioSegments().get(2).getCount());
        Assertions.assertEquals("#0284c7", view.getRatioSegments().get(2).getColor());

        final DashboardWidgetConfigDto absoluteConfig = new DashboardWidgetConfigDto(
            "w-ai-text-abs", "AI Usage Absolute", WidgetMetric.AI_USAGE, "", WidgetPresentation.TEXT,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.ABSOLUTE, "quarter", 0
        );
        final DashboardWidgetViewDto absView = service.aggregateWidget(
            absoluteConfig, Collections.emptyList(), variations, Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );
        Assertions.assertEquals("1 AI · 1 Healed · 0 Playbook", absView.getPrimaryTextValue());
        Assertions.assertEquals(3, absView.getRatioSegments().size());

        final DashboardWidgetConfigDto trendConfig = new DashboardWidgetConfigDto(
            "w-ai-trend", "AI Usage Sparkline", WidgetMetric.AI_USAGE, "", WidgetPresentation.TEXT_WITH_SMALL_TREND,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, "quarter", 0
        );
        final DashboardWidgetViewDto trendView = service.aggregateWidget(
            trendConfig, Collections.emptyList(), variations, Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );
        Assertions.assertEquals(3, trendView.getRatioSegments().size());
        Assertions.assertEquals("33% AI · 33% Healed · 0% Playbook", trendView.getPrimaryTextValue());
    }

    @Test
    public void testTagTextWidgetAbsoluteAggregation()
    {
        final DashboardWidgetConfigDto pctConfig = new DashboardWidgetConfigDto(
            "w-tag-pct", "Tag KPI Percent", WidgetMetric.TAG, "browser", WidgetPresentation.TEXT,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, "quarter", 0
        );
        final List<DistributionRatioDto> browserRatios = List.of(
            new DistributionRatioDto("Chrome", 6, 60.0, "#10b981"),
            new DistributionRatioDto("Firefox", 4, 40.0, "#3b82f6")
        );
        final DashboardWidgetViewDto pctView = service.aggregateWidget(
            pctConfig, Collections.emptyList(), Collections.emptyList(), browserRatios, Collections.emptyList(), null, null, Collections.emptyList()
        );
        Assertions.assertNotNull(pctView);
        Assertions.assertEquals("Chrome (60.0%)", pctView.getPrimaryTextValue());

        final DashboardWidgetConfigDto absConfig = new DashboardWidgetConfigDto(
            "w-tag-abs", "Tag KPI Absolute", WidgetMetric.TAG, "browser", WidgetPresentation.TEXT,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.ABSOLUTE, "quarter", 0
        );
        final DashboardWidgetViewDto absView = service.aggregateWidget(
            absConfig, Collections.emptyList(), Collections.emptyList(), browserRatios, Collections.emptyList(), null, null, Collections.emptyList()
        );
        Assertions.assertNotNull(absView);
        Assertions.assertEquals("6 Chrome", absView.getPrimaryTextValue());
    }

    @Test
    public void testAiUsageListWidgetAggregation()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-ai-list", "AI Usage Breakdown", WidgetMetric.AI_USAGE, "", WidgetPresentation.LIST,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE, "half", 0
        );

        final List<BatchLatestVariationDto> variations = List.of(
            new BatchLatestVariationDto("CheckoutTest#testAiSearch", "passed", false, "AI", "run-10", "staging", "Chrome", "US"),
            new BatchLatestVariationDto("CheckoutTest#testHealedButton", "passed", true, "JAVA", "run-10", "staging", "Chrome", "US"),
            new BatchLatestVariationDto("CheckoutTest#testPlaybookYaml", "passed", false, "PLAYBOOK", "run-10", "staging", "Chrome", "US"),
            new BatchLatestVariationDto("CheckoutTest#testClassicUnit", "passed", false, "JAVA", "run-10", "staging", "Chrome", "US")
        );

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, Collections.emptyList(), variations, Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertEquals(WidgetPresentation.LIST, view.getPresentation());
        Assertions.assertTrue(view.hasListItems());
        Assertions.assertEquals(4, view.getListItems().size());

        final WidgetListItemDto aiGroup = view.getListItems().stream().filter(i -> "AI-Driven".equals(i.getLabel())).findFirst().orElseThrow();
        Assertions.assertEquals("psychology", aiGroup.getIcon());
        Assertions.assertTrue(aiGroup.hasSubItems());
        Assertions.assertEquals("testAiSearch", aiGroup.getSubItems().get(0).getLabel());

        final WidgetListItemDto healedGroup = view.getListItems().stream().filter(i -> "Healed".equals(i.getLabel())).findFirst().orElseThrow();
        Assertions.assertEquals("auto_fix_high", healedGroup.getIcon());

        final WidgetListItemDto playbookGroup = view.getListItems().stream().filter(i -> "Playbook".equals(i.getLabel())).findFirst().orElseThrow();
        Assertions.assertEquals("menu_book", playbookGroup.getIcon());

        final WidgetListItemDto classicGroup = view.getListItems().stream().filter(i -> "Classic".equals(i.getLabel())).findFirst().orElseThrow();
        Assertions.assertEquals("code", classicGroup.getIcon());
    }

    @Test
    public void testAiUsageContributingExecutions()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-ai-contrib", "AI Contribs", WidgetMetric.AI_USAGE, "", WidgetPresentation.PIE_CHART,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, "half", 0
        );

        final TestRunEntity run = new TestRunEntity("run-1", "DefaultBatch", "COMPLETED", "CI", "staging", "en-US", "Chrome", "2026-11-02 10:00", 1730541600000L);
        run.setTotalTests(3);

        final TestExecutionDto execAi = new TestExecutionDto("e1", "run-1", "com.xceptance.TestClass", "testAi", "testAi", "passed-clean", "Chrome", Collections.emptyMap());
        execAi.setExecutionMode("LLM_ONLY");

        final TestExecutionDto execHealed = new TestExecutionDto("e2", "run-1", "com.xceptance.TestClass", "testHealed", "testHealed", "passed-clean", "Chrome", Collections.emptyMap());
        execHealed.setHealed(true);

        final TestExecutionDto execClassic = new TestExecutionDto("e3", "run-1", "com.xceptance.TestClass", "testClassic", "testClassic", "passed-clean", "Chrome", Collections.emptyMap());

        final RunReportDto report = new RunReportDto("run-1", "DefaultBatch", "2026-11-02 10:00", "10s", 3, 3, 0, 0, 0, 0, List.of(execAi, execHealed, execClassic));

        final AuraReportDataService mockDataService = Mockito.mock(AuraReportDataService.class);
        Mockito.when(mockDataService.getRunReport("run-1")).thenReturn(report);

        final WidgetContributingExecutionsDto contribs = service.resolveContributingExecutions(
            config, List.of(run), Collections.emptyList(), mockDataService
        );

        Assertions.assertNotNull(contribs);
        Assertions.assertEquals(1, contribs.getRunGroups().size());
        final WidgetRunExecutionsGroupDto group = contribs.getRunGroups().get(0);
        Assertions.assertEquals(2, group.getExecutions().size()); // only execAi and execHealed, classic excluded
        Assertions.assertTrue(group.getExecutions().stream().anyMatch(e -> "e1".equals(e.getId())));
        Assertions.assertTrue(group.getExecutions().stream().anyMatch(e -> "e2".equals(e.getId())));
    }

    @Test
    public void testAiUsageTrendGraphDecouplesCategoryCurves()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-ai-trend", "AI Adoption Trend", WidgetMetric.AI_USAGE, "", WidgetPresentation.TREND_GRAPH,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, "full", 0
        );

        final TestRunEntity run1 = new TestRunEntity("run-1", "DefaultBatch", "COMPLETED", "CI", "staging", "en-US", "Chrome", "2026-11-01 10:00", 1730455200000L);
        run1.setTotalTests(100);
        run1.setPassedCount(70);
        run1.setPassedHealedCount(15);
        run1.setPassedAiCount(15);

        final TestRunEntity run2 = new TestRunEntity("run-2", "DefaultBatch", "COMPLETED", "CI", "staging", "en-US", "Chrome", "2026-11-02 10:00", 1730541600000L);
        run2.setTotalTests(50); // Total tests drop by 50, Classic drops from 70 to 20, but AI tests stay constant at 15
        run2.setPassedCount(20);
        run2.setPassedHealedCount(15);
        run2.setPassedAiCount(15);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(run1, run2), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasTrendLayers(), "AI Usage trend widget must have trend layers");
        Assertions.assertEquals(3, view.getTrendLayers().size(), "Must have Classic, Healed, and AI-Driven layers");

        final WidgetTrendLayerDto aiLayer = view.getTrendLayers().stream()
            .filter(l -> "AI-Driven".equals(l.getName()))
            .findFirst()
            .orElseThrow();

        Assertions.assertTrue(aiLayer.hasPath(), "AI-Driven layer must have a stacked area path for filled mode");
        Assertions.assertTrue(aiLayer.hasLinePath(), "AI-Driven layer must have a standalone line path for not-filled mode");

        // Since AI count was 15 in run-1 and 15 in run-2, its standalone line path must have equal Y coordinates
        final String linePath = aiLayer.getSvgLinePath();
        Assertions.assertTrue(linePath.startsWith("M "), "Line path must start with M");
        final String[] segments = linePath.split(" L ");
        Assertions.assertEquals(2, segments.length, "Must contain two point coordinates for two runs");

        final double yPoint1 = Double.parseDouble(segments[0].substring(2).trim().split(" ")[1]);
        final double yPoint2 = Double.parseDouble(segments[1].trim().split(" ")[1]);
        Assertions.assertEquals(yPoint1, yPoint2, 0.01, "AI-Driven curve must remain completely flat despite 50-test drop in Classic test volume");
    }

    @Test
    public void testStatusTrendGraphDecouplesCategoryCurves()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-status-trend", "Status Trend Decoupled", WidgetMetric.STATUS, "", WidgetPresentation.TREND_GRAPH,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, "full", 0
        );

        final TestRunEntity run1 = new TestRunEntity("run-1", "DefaultBatch", "COMPLETED", "CI", "staging", "en-US", "Chrome", "2026-11-01 10:00", 1730455200000L);
        run1.setTotalTests(100);
        run1.setPassedCount(50);
        run1.setFailedUnknownCount(50);

        final TestRunEntity run2 = new TestRunEntity("run-2", "DefaultBatch", "COMPLETED", "CI", "staging", "en-US", "Chrome", "2026-11-02 10:00", 1730541600000L);
        run2.setTotalTests(60); // Total tests drop by 40, Failed Unknown drops from 50 to 10, but Passed Clean stays constant at 50
        run2.setPassedCount(50);
        run2.setFailedUnknownCount(10);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(run1, run2), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasTrendLayers(), "Status trend widget must have trend layers");

        final WidgetTrendLayerDto passedLayer = view.getTrendLayers().stream()
            .filter(l -> "Passed Clean".equals(l.getName()))
            .findFirst()
            .orElseThrow();

        Assertions.assertTrue(passedLayer.hasPath(), "Passed Clean layer must have a stacked area path for filled mode");
        Assertions.assertTrue(passedLayer.hasLinePath(), "Passed Clean layer must have a standalone line path for not-filled mode");

        // Since Passed Clean count was 50 in run-1 and 50 in run-2, its standalone line path must have equal Y coordinates
        final String linePath = passedLayer.getSvgLinePath();
        Assertions.assertTrue(linePath.startsWith("M "), "Line path must start with M");
        final String[] segments = linePath.split(" L ");
        Assertions.assertEquals(2, segments.length, "Must contain two point coordinates for two runs");

        final double yPoint1 = Double.parseDouble(segments[0].substring(2).trim().split(" ")[1]);
        final double yPoint2 = Double.parseDouble(segments[1].trim().split(" ")[1]);
        Assertions.assertEquals(yPoint1, yPoint2, 0.01, "Passed Clean curve must remain completely flat despite 40-test drop in Failed Unknown test volume");
    }

    @Test
    public void testTrendGraphSupportsBothFilledAndNotFilledVariants()
    {
        final DashboardWidgetConfigDto notFilledConfig = new DashboardWidgetConfigDto(
            "w-trend-toggle", "Status Trend", WidgetMetric.STATUS, "", WidgetPresentation.TREND_GRAPH,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, "full", 0
        ).withFilled(false);

        final DashboardWidgetConfigDto filledConfig = notFilledConfig.withFilled(true);

        final TestRunEntity run1 = new TestRunEntity("run-1", "DefaultBatch", "COMPLETED", "CI", "staging", "en-US", "Chrome", "2026-11-01 10:00", 1730455200000L);
        run1.setTotalTests(100);
        run1.setPassedCount(80);
        run1.setFailedUnknownCount(20);

        final DashboardWidgetViewDto notFilledView = service.aggregateWidget(
            notFilledConfig, List.of(run1), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );
        Assertions.assertFalse(notFilledView.isFilled(), "View must report not filled");
        Assertions.assertTrue(notFilledView.hasTrendLayers());
        final WidgetTrendLayerDto notFilledLayer = notFilledView.getTrendLayers().get(0);
        Assertions.assertTrue(notFilledLayer.hasPath(), "Must have area path available");
        Assertions.assertTrue(notFilledLayer.hasLinePath(), "Must have line path available");

        final DashboardWidgetViewDto filledView = service.aggregateWidget(
            filledConfig, List.of(run1), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );
        Assertions.assertTrue(filledView.isFilled(), "View must report filled");
        Assertions.assertTrue(filledView.hasTrendLayers());
        final WidgetTrendLayerDto filledLayer = filledView.getTrendLayers().get(0);
        Assertions.assertTrue(filledLayer.hasPath());
        Assertions.assertTrue(filledLayer.hasLinePath());
    }

    @Test
    public void testResolveContributingRunIds()
    {
        final TestRunEntity run1 = new TestRunEntity("run-1", "DefaultBatch", "COMPLETED", "CI", "staging", "en-US", "Chrome", "2026-11-01 10:00", 1730455200000L);
        final TestRunEntity run2 = new TestRunEntity("run-2", "DefaultBatch", "COMPLETED", "CI", "staging", "en-US", "Firefox", "2026-11-01 11:00", 1730458800000L);

        final List<BatchLatestVariationDto> vars = List.of(
            new BatchLatestVariationDto("k1", "passed-clean", false, "JAVA", "run-1", "Default", "Chrome", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L),
            new BatchLatestVariationDto("k2", "passed-clean", false, "JAVA", "run-2", "Default", "Firefox", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L)
        );

        final DashboardWidgetConfigDto pointWidget = new DashboardWidgetConfigDto(
            "w-point", "Status Donut", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0, false, false, Collections.emptyList(), true, null
        );

        final Set<String> pointRuns = service.resolveContributingRunIds(pointWidget, List.of(run1, run2), vars, null);
        Assertions.assertEquals(Set.of("run-1", "run-2"), pointRuns);

        final DashboardWidgetConfigDto trendWidget = new DashboardWidgetConfigDto(
            "w-trend", "Status Trend", WidgetMetric.STATUS, null,
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "full", 1, false, false, Collections.emptyList(), true, null
        );

        final Set<String> trendRuns = service.resolveContributingRunIds(trendWidget, List.of(run1, run2), vars, null);
        Assertions.assertEquals(Set.of("run-1", "run-2"), trendRuns);
    }

    @Test
    public void testAreBasedOnSameContributingRuns()
    {
        final TestRunEntity run1 = new TestRunEntity("run-1", "DefaultBatch", "COMPLETED", "CI", "staging", "en-US", "Chrome", "2026-11-01 10:00", 1730455200000L);
        final TestRunEntity run2 = new TestRunEntity("run-2", "DefaultBatch", "COMPLETED", "CI", "staging", "en-US", "Firefox", "2026-11-01 11:00", 1730458800000L);

        final List<BatchLatestVariationDto> vars = List.of(
            new BatchLatestVariationDto("k1", "passed-clean", false, "JAVA", "run-1", "Default", "Chrome", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L)
        );

        final DashboardWidgetConfigDto w1 = new DashboardWidgetConfigDto(
            "w1", "Widget 1", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0, false, false, Collections.emptyList(), true, null
        );

        final DashboardWidgetConfigDto w2 = new DashboardWidgetConfigDto(
            "w2", "Widget 2", WidgetMetric.STATUS, null,
            WidgetPresentation.LIST, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 1, false, false, Collections.emptyList(), true, null
        );

        final boolean same = service.areBasedOnSameContributingRuns(w1, w2, List.of(run1), vars, null);
        Assertions.assertTrue(same, "Both widgets resolve to run-1, so they must be recognized as having the same contributing runs");

        // Widget 3 with trend mode resolves to both run1 and run2
        final DashboardWidgetConfigDto w3 = new DashboardWidgetConfigDto(
            "w3", "Widget 3", WidgetMetric.STATUS, null,
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "full", 2, false, false, Collections.emptyList(), true, null
        );

        final boolean different = service.areBasedOnSameContributingRuns(w1, w3, List.of(run1, run2), vars, null);
        Assertions.assertFalse(different, "Point widget (run-1) and trend widget (run-1, run-2) do not share the same runs");
    }

    @Test
    public void testContainerWidgetAggregationAggregatesChildViews()
    {
        final DashboardWidgetConfigDto child1 = new DashboardWidgetConfigDto(
            "child1", "Donut", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0, false, false, Collections.emptyList(), true, null
        );
        final DashboardWidgetConfigDto child2 = new DashboardWidgetConfigDto(
            "child2", "List", WidgetMetric.STATUS, null,
            WidgetPresentation.LIST, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 1, false, false, Collections.emptyList(), true, null
        );
        final DashboardWidgetConfigDto containerConfig = DashboardWidgetConfigDto.createContainer(child1, child2);

        final List<BatchLatestVariationDto> variations = List.of(
            new BatchLatestVariationDto("k1", "passed-clean", false, "JAVA", "run-1", "Default", "Chrome", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L)
        );

        final DashboardWidgetViewDto containerView = service.aggregateWidget(
            containerConfig, Collections.emptyList(), variations, Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(containerView);
        Assertions.assertTrue(containerView.isContainer());
        Assertions.assertEquals(WidgetPresentation.CONTAINER, containerView.getPresentation());
        Assertions.assertEquals(2, containerView.getChildViews().size());
        Assertions.assertEquals("child1", containerView.getChildViews().get(0).getId());
        Assertions.assertEquals("child2", containerView.getChildViews().get(1).getId());
        Assertions.assertEquals("run-1", containerView.getContributingRunIdsCsv());
        Assertions.assertNotNull(containerView.getContainerVariationsJson());
        Assertions.assertTrue(containerView.getContainerVariationsJson().contains("\"k\":\"k1\""));
        Assertions.assertTrue(containerView.getContainerVariationsJson().contains("\"b\":\"Chrome\""));
        Assertions.assertTrue(containerView.getContainerVariationsJson().contains("\"s\":\"passed-clean\""));
        Assertions.assertTrue(containerView.getContainerVariationsJson().contains("\"c\":"));
        Assertions.assertTrue(containerView.getContainerVariationsJson().contains("\"tok\":"));
    }

    @Test
    public void testContainerWidgetAggregationClearsTagKeyForNonTagChildren()
    {
        final DashboardWidgetConfigDto statusChild = new DashboardWidgetConfigDto(
            "c1", "Status Donut", WidgetMetric.STATUS, "Area",
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0, false, false, Collections.emptyList(), true, null
        );
        final DashboardWidgetConfigDto tagChild = new DashboardWidgetConfigDto(
            "c2", "Area Ratio", WidgetMetric.TAG, "Area",
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 1, false, false, Collections.emptyList(), true, null
        );
        final DashboardWidgetConfigDto containerConfig = DashboardWidgetConfigDto.createContainer(statusChild, tagChild);

        final DashboardWidgetViewDto containerView = service.aggregateWidget(
            containerConfig, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertEquals("", containerView.getChildViews().get(0).getTagKey(), "Status child must have empty tagKey");
        Assertions.assertEquals("Area", containerView.getChildViews().get(1).getTagKey(), "TAG child must preserve tagKey");
    }

    @Test
    public void testAggregateWidgetsWithContainerHavingCustomExecutionIdentity()
    {
        final DashboardWidgetConfigDto child1 = new DashboardWidgetConfigDto(
            "child1", "Status Donut", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0, false, true, List.of("environment"), false, null
        );
        final DashboardWidgetConfigDto child2 = new DashboardWidgetConfigDto(
            "child2", "Browser Ratio", WidgetMetric.TAG, "browser",
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 1, false, true, List.of("environment"), false, null
        );
        final DashboardWidgetConfigDto containerConfig = DashboardWidgetConfigDto.createContainer(child1, child2);
        Assertions.assertTrue(containerConfig.hasOwnExecutionIdentity());

        final List<BatchLatestVariationDto> variations = List.of(
            new BatchLatestVariationDto("k1", "passed-clean", false, "JAVA", "run-1", "Default", "Chrome", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L),
            new BatchLatestVariationDto("k2", "failed-known", false, "JAVA", "run-1", "Default", "Firefox", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L)
        );

        final List<DashboardWidgetViewDto> views = service.aggregateWidgets(
            List.of(containerConfig),
            Collections.emptyList(),
            variations,
            Collections.emptyList(),
            Collections.emptyList(),
            null,
            null,
            Collections.emptyList()
        );

        Assertions.assertNotNull(views);
        Assertions.assertEquals(1, views.size());
        final DashboardWidgetViewDto containerView = views.get(0);
        Assertions.assertTrue(containerView.isContainer());
        Assertions.assertTrue(containerView.hasOwnExecutionIdentity(), "Container view must report custom execution identity");
        Assertions.assertEquals("Test + environment", containerView.getIdentityDimensionsSummary());
        Assertions.assertEquals(2, containerView.getChildViews().size());
        Assertions.assertNotNull(containerView.getContainerVariationsJson());
        Assertions.assertTrue(containerView.getContainerVariationsJson().contains("\"b\":\"Chrome\""));
        Assertions.assertTrue(containerView.getContainerVariationsJson().contains("\"b\":\"Firefox\""));
    }

    @Test
    public void testResolveContributingExecutions_ForContainerWidget()
    {
        final DashboardWidgetConfigDto child1 = new DashboardWidgetConfigDto(
            "c1", "Status Donut", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0
        );
        final DashboardWidgetConfigDto child2 = new DashboardWidgetConfigDto(
            "c2", "Browser Ratio", WidgetMetric.TAG, "browser",
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 1
        );
        final DashboardWidgetConfigDto container = DashboardWidgetConfigDto.createContainer("container-1", "Status & Browser", List.of(child1, child2), 0);

        final TestRunEntity run1 = new TestRunEntity();
        run1.setId("run-c1");
        run1.setStartTimeMs(2000L);
        run1.setPassedCount(2);

        final TestRunEntity runOld = new TestRunEntity();
        runOld.setId("run-old");
        runOld.setStartTimeMs(1000L);
        runOld.setPassedCount(1);

        final BatchLatestVariationDto var1 = new BatchLatestVariationDto(
            "key1", "passed-clean", false, "JAVA", "run-c1", "Default", "Chrome", "US", "AuthTest", "testLogin", Collections.emptyMap(), 0.0, 0L, "exec-1"
        );

        final TestExecutionDto exec1 = new TestExecutionDto(
            "exec-1", "run-c1", "com.example.AuthTest", "testLogin", "testLogin", "PASSED", "en-US", "Chrome", Collections.emptyMap()
        );
        final RunReportDto report = Mockito.mock(RunReportDto.class);
        Mockito.when(report.getExecutions()).thenReturn(List.of(exec1));

        final AuraReportDataService dataService = Mockito.mock(AuraReportDataService.class);
        Mockito.when(dataService.getRunReport("run-c1")).thenReturn(report);
        Mockito.when(dataService.buildLogicalKey(Mockito.any(), Mockito.any())).thenReturn("key1");

        final WidgetContributingExecutionsDto result = service.resolveContributingExecutions(
            container, List.of(run1, runOld), List.of(var1), dataService
        );

        Assertions.assertNotNull(result);
        Assertions.assertEquals("container-1", result.getWidgetId());
        Assertions.assertEquals("Status & Browser", result.getWidgetTitle());
        Assertions.assertEquals(WidgetPresentation.CONTAINER, result.getPresentation());
        Assertions.assertEquals(1, result.getTotalRunsCount(), "Container must only include contributing runs, not older runs");
        Assertions.assertEquals("run-c1", result.getRunGroups().get(0).getRunId());
        Assertions.assertEquals(1, result.getTotalExecutionsCount());
        Assertions.assertEquals(1, result.getOverallPassCount());
    }

    @Test
    public void testTestVariationsWidget_DefaultGlobalIdentity()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-variations", "Known Test Base Variations", WidgetMetric.TEST_VARIATIONS, null,
            WidgetPresentation.TEST_VARIATIONS, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "full", 0
        );

        final AuraReportDataService dataService = Mockito.mock(AuraReportDataService.class);
        final TestBaseClassDto classDto = new TestBaseClassDto("CheckoutProcessTest", "container-1", 2, Collections.emptyList());
        final List<TestBaseClassDto> classes = List.of(classDto);
        final TestBaseAreaDto areaDto = new TestBaseAreaDto("@Browsing", "area-1", "tab-1", "folder_open", 1, classes);
        final List<TestBaseAreaDto> areas = List.of(areaDto);

        final TestRunEntity run1 = new TestRunEntity();
        run1.setId("run-1");
        final List<TestRunEntity> runs = List.of(run1);

        Mockito.when(dataService.getBatchTestBaseClasses(
            Mockito.any(), Mockito.eq(runs), Mockito.anySet(), Mockito.anySet(),
            Mockito.anySet(), Mockito.anySet(), Mockito.anySet(), Mockito.anySet(),
            Mockito.anySet(), Mockito.anySet(), Mockito.anySet(), Mockito.anySet(),
            Mockito.eq(List.of("browser"))
        )).thenReturn(classes);

        Mockito.when(dataService.groupBatchTestClassesByArea(classes)).thenReturn(areas);

        final List<DashboardWidgetViewDto> views = service.aggregateWidgets(
            List.of(config), runs, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList(), dataService, runs, "DefaultBatch", List.of("browser"),
            Collections.emptyMap(), List.of("Browser", "Locale")
        );

        Assertions.assertNotNull(views);
        Assertions.assertEquals(1, views.size());
        final DashboardWidgetViewDto view = views.get(0);
        Assertions.assertEquals(WidgetMetric.TEST_VARIATIONS, view.getMetric());
        Assertions.assertEquals(WidgetPresentation.TEST_VARIATIONS, view.getPresentation());
        Assertions.assertEquals(2, view.getTotalVariationsCount());
        Assertions.assertEquals("2 Variations", view.getSummaryLabel());
        Assertions.assertEquals(1, view.getTestAreas().size());
        Assertions.assertEquals(1, view.getTestClasses().size());
        Assertions.assertFalse(view.hasOwnExecutionIdentity());
        Assertions.assertFalse(view.isApplyIdentityToVariations());
        Assertions.assertTrue(view.getActiveIdentityCustomTags().isEmpty());
    }

    @Test
    public void testTestVariationsWidget_CustomExecutionIdentity()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-variations-custom-id", "Custom ID Variations", WidgetMetric.TEST_VARIATIONS, null,
            WidgetPresentation.TEST_VARIATIONS, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "full", 0, false, true, List.of("browser", "environment", "device"), false, null
        );

        final AuraReportDataService dataService = Mockito.mock(AuraReportDataService.class);
        final TestBaseClassDto classDto = new TestBaseClassDto("LoginTest", "container-2", 3, Collections.emptyList());
        final List<TestBaseClassDto> classes = List.of(classDto);
        final TestBaseAreaDto areaDto = new TestBaseAreaDto("@Auth", "area-2", "tab-2", "lock", 1, classes);
        final List<TestBaseAreaDto> areas = List.of(areaDto);

        final TestRunEntity run1 = new TestRunEntity();
        run1.setId("run-1");
        final List<TestRunEntity> runs = List.of(run1);

        Mockito.when(dataService.getBatchTestBaseClasses(
            Mockito.any(), Mockito.eq(runs), Mockito.anySet(), Mockito.anySet(),
            Mockito.anySet(), Mockito.anySet(), Mockito.anySet(), Mockito.anySet(),
            Mockito.anySet(), Mockito.anySet(), Mockito.anySet(), Mockito.anySet(),
            Mockito.eq(List.of("browser", "environment", "device"))
        )).thenReturn(classes);

        Mockito.when(dataService.groupBatchTestClassesByArea(classes)).thenReturn(areas);

        final List<DashboardWidgetViewDto> views = service.aggregateWidgets(
            List.of(config), runs, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList(), dataService, runs, "DefaultBatch", List.of("browser"),
            Collections.emptyMap(), List.of("Browser", "Environment", "Device")
        );

        Assertions.assertNotNull(views);
        Assertions.assertEquals(1, views.size());
        final DashboardWidgetViewDto view = views.get(0);
        Assertions.assertTrue(view.hasOwnExecutionIdentity());
        Assertions.assertTrue(view.isApplyIdentityToVariations());
        Assertions.assertEquals(2, view.getActiveIdentityCustomTags().size());
        Assertions.assertTrue(view.getActiveIdentityCustomTags().contains("Environment"));
        Assertions.assertTrue(view.getActiveIdentityCustomTags().contains("Device"));
    }

    @Test
    public void testTestVariationsWidget_CustomRunFilters()
    {
        final WidgetFilterConfigDto filter = new WidgetFilterConfigDto(
            Set.of("Chrome"), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptyMap(), Collections.emptyMap(),
            1, null, null
        );

        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-variations-filters", "Filtered Variations", WidgetMetric.TEST_VARIATIONS, null,
            WidgetPresentation.TEST_VARIATIONS, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "full", 0, false, false, Collections.emptyList(), true, filter
        );

        final TestRunEntity run1 = new TestRunEntity();
        run1.setId("run-chrome");
        run1.setStartTimeMs(2000L);
        run1.setTotalTests(5);

        final TestRunEntity run2 = new TestRunEntity();
        run2.setId("run-firefox");
        run2.setStartTimeMs(1000L);
        run2.setTotalTests(5);

        final AuraReportDataService dataService = Mockito.mock(AuraReportDataService.class);
        Mockito.when(dataService.scopeRunToCriteria(Mockito.eq(run1), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(run1);
        Mockito.when(dataService.scopeRunToCriteria(Mockito.eq(run2), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(null);

        final TestBaseClassDto classDto = new TestBaseClassDto("SearchTest", "container-3", 1, Collections.emptyList());
        Mockito.when(dataService.getBatchTestBaseClasses(
            Mockito.any(), Mockito.eq(List.of(run1)), Mockito.eq(Set.of("Chrome")), Mockito.anySet(),
            Mockito.anySet(), Mockito.anySet(), Mockito.anySet(), Mockito.anySet(),
            Mockito.anySet(), Mockito.anySet(), Mockito.anySet(), Mockito.anySet(),
            Mockito.anyList()
        )).thenReturn(List.of(classDto));

        final List<DashboardWidgetViewDto> views = service.aggregateWidgets(
            List.of(config), List.of(run1, run2), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList(), dataService, List.of(run1, run2), "DefaultBatch", List.of("browser"),
            Collections.emptyMap(), List.of("Browser")
        );

        Assertions.assertNotNull(views);
        Assertions.assertEquals(1, views.size());
        final DashboardWidgetViewDto view = views.get(0);
        Assertions.assertTrue(view.hasOwnRunFilters());
        Assertions.assertEquals(1, view.getTotalVariationsCount());
        Mockito.verify(dataService).getBatchTestBaseClasses(
            Mockito.any(), Mockito.eq(List.of(run1)), Mockito.eq(Set.of("Chrome")), Mockito.anySet(),
            Mockito.anySet(), Mockito.anySet(), Mockito.anySet(), Mockito.anySet(),
            Mockito.anySet(), Mockito.anySet(), Mockito.anySet(), Mockito.anySet(),
            Mockito.anyList()
        );
    }

    @Test
    public void testTrendGraph_TagMetric_LatestApproach_ShowsExecutionCountTrend()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-tag-evo", "Area Trend", WidgetMetric.TAG, "Area",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "full", 0, false
        );

        final BatchSuiteEvolutionPointDto pt1 = new BatchSuiteEvolutionPointDto(
            "run-1", 0, "2026-11-01 10:00", 50, 50,
            50, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 100.0
        );
        final BatchSuiteEvolutionPointDto pt2 = new BatchSuiteEvolutionPointDto(
            "run-2", 1, "2026-11-02 10:00", 75, 25,
            75, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 100.0
        );
        final BatchSuiteEvolutionDto suiteEvolution = new BatchSuiteEvolutionDto(List.of(pt1, pt2));

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), null, suiteEvolution, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertEquals(2, view.getTrendPoints().size());
        Assertions.assertEquals(50.0, view.getTrendPoints().get(0).getValue(), 0.001);
        Assertions.assertEquals("50 tests", view.getTrendPoints().get(0).getValueFormatted());
        Assertions.assertEquals(75.0, view.getTrendPoints().get(1).getValue(), 0.001);
        Assertions.assertEquals("75 tests", view.getTrendPoints().get(1).getValueFormatted());
        Assertions.assertEquals("+25 tests vs start", view.getTrendDeltaLabel());
        Assertions.assertEquals(1, view.getTrendLayers().size(), "Tag trend must have exactly 1 summarized layer, not multiple status layers");
        Assertions.assertEquals("Area Tests", view.getTrendLayers().get(0).getName());
        Assertions.assertTrue(view.hasYAxisTicks());
        Assertions.assertEquals("0 Tests", view.getYAxisTicks().get(0).getLabel());
        Assertions.assertEquals("75 Tests", view.getYAxisTicks().get(view.getYAxisTicks().size() - 1).getLabel());
    }

    @Test
    public void testTrendGraph_TagMetric_ByRuns_ShowsPerRunExecutionCounts()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-tag-runs", "Browser Trend", WidgetMetric.TAG, "Browser",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "full", 0, false
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-1");
        r1.setStartTimeMs(1000L);
        r1.setTotalTests(20);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-2");
        r2.setStartTimeMs(2000L);
        r2.setTotalTests(35);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1, r2), Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertEquals(2, view.getTrendPoints().size());
        Assertions.assertEquals(20.0, view.getTrendPoints().get(0).getValue(), 0.001);
        Assertions.assertEquals("20 tests", view.getTrendPoints().get(0).getValueFormatted());
        Assertions.assertEquals(35.0, view.getTrendPoints().get(1).getValue(), 0.001);
        Assertions.assertEquals("35 tests", view.getTrendPoints().get(1).getValueFormatted());
        Assertions.assertEquals("+15 tests vs start", view.getTrendDeltaLabel());
        Assertions.assertEquals(1, view.getTrendLayers().size());
        Assertions.assertEquals("Browser Tests", view.getTrendLayers().get(0).getName());
    }

    @Test
    public void testTrendGraph_TagMetric_DailyBucketing()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-tag-daily", "Daily Area Trend", WidgetMetric.TAG, "Area",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.DAILY, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "full", 0, false
        );

        final long baseTime = 1730000000000L;
        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-d1");
        r1.setStartTimeMs(baseTime);
        r1.setTotalTests(10);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-d2");
        r2.setStartTimeMs(baseTime + 3600 * 1000L); // same day
        r2.setTotalTests(15);

        final TestRunEntity r3 = new TestRunEntity();
        r3.setId("run-d3");
        r3.setStartTimeMs(baseTime + 86400 * 1000L * 2); // 2 days later
        r3.setTotalTests(40);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1, r2, r3), Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertEquals(2, view.getTrendPoints().size());
        Assertions.assertEquals(25.0, view.getTrendPoints().get(0).getValue(), 0.001);
        Assertions.assertEquals("25 tests", view.getTrendPoints().get(0).getValueFormatted());
        Assertions.assertEquals(40.0, view.getTrendPoints().get(1).getValue(), 0.001);
        Assertions.assertEquals("40 tests", view.getTrendPoints().get(1).getValueFormatted());
        Assertions.assertEquals("+15 tests vs start", view.getTrendDeltaLabel());
        Assertions.assertEquals(1, view.getTrendLayers().size());
        Assertions.assertEquals("Area Tests", view.getTrendLayers().get(0).getName());
    }

    @Test
    public void testTrendGraph_TagMetric_HourlyBucketing()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-tag-hourly", "Hourly Area Trend", WidgetMetric.TAG, "Area",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.HOURLY, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "full", 0, false
        );

        final long baseTime = 1730000000000L;
        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-h1");
        r1.setStartTimeMs(baseTime);
        r1.setTotalTests(15);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-h2");
        r2.setStartTimeMs(baseTime + 3600 * 1000L * 2); // 2 hours later
        r2.setTotalTests(25);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1, r2), Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertEquals(2, view.getTrendPoints().size());
        Assertions.assertEquals(15.0, view.getTrendPoints().get(0).getValue(), 0.001);
        Assertions.assertEquals("15 tests", view.getTrendPoints().get(0).getValueFormatted());
        Assertions.assertEquals(25.0, view.getTrendPoints().get(1).getValue(), 0.001);
        Assertions.assertEquals("25 tests", view.getTrendPoints().get(1).getValueFormatted());
        Assertions.assertEquals("+10 tests vs start", view.getTrendDeltaLabel());
    }

    @Test
    public void testTrendGraph_TagMetric_MonthlyBucketing()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-tag-monthly", "Monthly Area Trend", WidgetMetric.TAG, "Area",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.MONTHLY, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "full", 0, false
        );

        final long baseTime = 1730000000000L; // late Oct 2024
        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-m1");
        r1.setStartTimeMs(baseTime);
        r1.setTotalTests(100);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-m2");
        r2.setStartTimeMs(baseTime + 86400 * 1000L * 40); // following month
        r2.setTotalTests(150);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1, r2), Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertEquals(2, view.getTrendPoints().size());
        Assertions.assertEquals(100.0, view.getTrendPoints().get(0).getValue(), 0.001);
        Assertions.assertEquals("100 tests", view.getTrendPoints().get(0).getValueFormatted());
        Assertions.assertEquals(150.0, view.getTrendPoints().get(1).getValue(), 0.001);
        Assertions.assertEquals("150 tests", view.getTrendPoints().get(1).getValueFormatted());
        Assertions.assertEquals("+50 tests vs start", view.getTrendDeltaLabel());
    }

    @Test
    public void testTrendGraph_TagMetric_PercentFormat_ShowsPercentageGrowth()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-tag-pct", "Area Growth Rate", WidgetMetric.TAG, "Area",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS, "full", 0, false
        );

        final BatchSuiteEvolutionPointDto pt1 = new BatchSuiteEvolutionPointDto(
            "run-1", 0, "2026-11-01 10:00", 50, 50,
            50, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 100.0
        );
        final BatchSuiteEvolutionPointDto pt2 = new BatchSuiteEvolutionPointDto(
            "run-2", 1, "2026-11-02 10:00", 75, 25,
            75, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 100.0
        );
        final BatchSuiteEvolutionDto suiteEvolution = new BatchSuiteEvolutionDto(List.of(pt1, pt2));

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), null, suiteEvolution, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertEquals("+50.0% vs start", view.getTrendDeltaLabel());
    }

    @Test
    public void testTrendGraph_TagMetric_MultiLine_SuiteEvolution()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-tag-multiline-evo", "Area Suite Trend", WidgetMetric.TAG, "Area",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "full", 0, false
        );

        final Map<String, Integer> pt1Areas = Map.of(
            "Browsing (default)", 10,
            "Checkout", 5
        );
        final Map<String, Integer> pt2Areas = Map.of(
            "Browsing (default)", 12,
            "Checkout", 8,
            "Cart", 4
        );

        final BatchSuiteEvolutionPointDto pt1 = new BatchSuiteEvolutionPointDto(
            "run-1", 0, "2026-11-01 10:00", 15, 15,
            15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 100.0, 0,
            Map.of("area", pt1Areas)
        );
        final BatchSuiteEvolutionPointDto pt2 = new BatchSuiteEvolutionPointDto(
            "run-2", 1, "2026-11-02 10:00", 24, 9,
            24, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 100.0, 0,
            Map.of("area", pt2Areas)
        );
        final BatchSuiteEvolutionDto suiteEvolution = new BatchSuiteEvolutionDto(List.of(pt1, pt2));

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), null, suiteEvolution, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertEquals(2, view.getTrendPoints().size());
        Assertions.assertEquals(15.0, view.getTrendPoints().get(0).getValue(), 0.001);
        Assertions.assertEquals("15 tests", view.getTrendPoints().get(0).getValueFormatted());
        Assertions.assertEquals(24.0, view.getTrendPoints().get(1).getValue(), 0.001);
        Assertions.assertEquals("24 tests", view.getTrendPoints().get(1).getValueFormatted());
        Assertions.assertEquals("+9 tests vs start", view.getTrendDeltaLabel());

        final List<WidgetTrendLayerDto> layers = view.getTrendLayers();
        Assertions.assertEquals(3, layers.size());

        final List<String> layerNames = layers.stream().map(WidgetTrendLayerDto::getName).toList();
        Assertions.assertTrue(layerNames.contains("Browsing (default)"));
        Assertions.assertTrue(layerNames.contains("Checkout"));
        Assertions.assertTrue(layerNames.contains("Cart"));

        for (final WidgetTrendLayerDto layer : layers)
        {
            Assertions.assertFalse(layer.getColor().isBlank());
            Assertions.assertFalse(layer.getSvgLinePath().isBlank());
            Assertions.assertFalse(layer.getSvgAreaPath().isBlank());
            if ("Browsing (default)".equals(layer.getName()))
            {
                Assertions.assertEquals(12, layer.getCount());
            }
            else if ("Checkout".equals(layer.getName()))
            {
                Assertions.assertEquals(8, layer.getCount());
            }
            else if ("Cart".equals(layer.getName()))
            {
                Assertions.assertEquals(4, layer.getCount());
            }
        }
    }

    @Test
    public void testTrendGraph_TagMetric_UnstackedVsStacked_YAxisScaling()
    {
        final Map<String, Integer> pt1Areas = Map.of(
            "Browsing (default)", 10,
            "Checkout", 5
        );
        final Map<String, Integer> pt2Areas = Map.of(
            "Browsing (default)", 12,
            "Checkout", 8,
            "Cart", 4
        );

        final BatchSuiteEvolutionPointDto pt1 = new BatchSuiteEvolutionPointDto(
            "run-1", 0, "2026-11-01 10:00", 15, 15,
            15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 100.0, 0,
            Map.of("area", pt1Areas)
        );
        final BatchSuiteEvolutionPointDto pt2 = new BatchSuiteEvolutionPointDto(
            "run-2", 1, "2026-11-02 10:00", 24, 9,
            24, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 100.0, 0,
            Map.of("area", pt2Areas)
        );
        final BatchSuiteEvolutionDto suiteEvolution = new BatchSuiteEvolutionDto(List.of(pt1, pt2));

        // 1. Unstacked Line mode (isFilled == false)
        final DashboardWidgetConfigDto unstackedConfig = new DashboardWidgetConfigDto(
            "w-tag-unstacked", "Unstacked Area Trend", WidgetMetric.TAG, "Area",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "full", 0, false
        ).withFilled(false);

        final DashboardWidgetViewDto unstackedView = service.aggregateWidget(
            unstackedConfig, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), null, suiteEvolution, Collections.emptyList()
        );

        Assertions.assertNotNull(unstackedView);
        final List<WidgetAxisTickDto> unstackedTicks = unstackedView.getYAxisTicks();
        Assertions.assertFalse(unstackedTicks.isEmpty());
        // In unstacked mode, top Y-axis tick must equal the maximal single tag value across all points (12 Tests)
        final WidgetAxisTickDto unstackedTopTick = unstackedTicks.get(unstackedTicks.size() - 1);
        Assertions.assertEquals("12 Tests", unstackedTopTick.getLabel());

        // Verify per-layer telemetry
        for (final WidgetTrendLayerDto layer : unstackedView.getTrendLayers())
        {
            if ("Browsing (default)".equals(layer.getName()))
            {
                Assertions.assertEquals(12, layer.getPeakCount());
                Assertions.assertEquals("10,12", layer.getRawValuesCsv());
            }
            else if ("Checkout".equals(layer.getName()))
            {
                Assertions.assertEquals(8, layer.getPeakCount());
                Assertions.assertEquals("5,8", layer.getRawValuesCsv());
            }
            else if ("Cart".equals(layer.getName()))
            {
                Assertions.assertEquals(4, layer.getPeakCount());
                Assertions.assertEquals("0,4", layer.getRawValuesCsv());
            }
        }

        // 2. Stacked Area mode (isFilled == true)
        final DashboardWidgetConfigDto stackedConfig = new DashboardWidgetConfigDto(
            "w-tag-stacked", "Stacked Area Trend", WidgetMetric.TAG, "Area",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "full", 0, false
        ).withFilled(true);

        final DashboardWidgetViewDto stackedView = service.aggregateWidget(
            stackedConfig, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), null, suiteEvolution, Collections.emptyList()
        );

        Assertions.assertNotNull(stackedView);
        final List<WidgetAxisTickDto> stackedTicks = stackedView.getYAxisTicks();
        Assertions.assertFalse(stackedTicks.isEmpty());
        // In stacked mode, top Y-axis tick must equal the maximal stacked sum across all points (24 Tests)
        final WidgetAxisTickDto stackedTopTick = stackedTicks.get(stackedTicks.size() - 1);
        Assertions.assertEquals("24 Tests", stackedTopTick.getLabel());
    }

    @Test
    public void testTrendGraph_TagMetric_MultiLine_ByRuns_WithDataService()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-tag-multiline-runs", "Runs Area Trend", WidgetMetric.TAG, "Area",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "full", 0, false
        );

        final AuraReportDataService mockDataService = Mockito.mock(AuraReportDataService.class);

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-r1");
        r1.setStartTimeMs(1000L);
        r1.setTotalTests(6);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-r2");
        r2.setStartTimeMs(2000L);
        r2.setTotalTests(8);

        final TestExecutionDto e1 = Mockito.mock(TestExecutionDto.class);
        Mockito.when(e1.getAreaName()).thenReturn("Browsing (default)");
        final TestExecutionDto e2 = Mockito.mock(TestExecutionDto.class);
        Mockito.when(e2.getAreaName()).thenReturn("Checkout");

        final RunReportDto rep1 = new RunReportDto(
            "run-r1", "Batch", "2026-11-01 10:00", "10s", 6, 6, 0, 0, 0, 0,
            List.of(e1, e1, e1, e1, e2, e2)
        );

        final RunReportDto rep2 = new RunReportDto(
            "run-r2", "Batch", "2026-11-01 11:00", "10s", 8, 8, 0, 0, 0, 0,
            List.of(e1, e1, e1, e2, e2, e2, e2, e2)
        );

        Mockito.when(mockDataService.getRunReport("run-r1")).thenReturn(rep1);
        Mockito.when(mockDataService.getRunReport("run-r2")).thenReturn(rep2);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1, r2), Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), null, null, Collections.emptyList(), mockDataService
        );

        Assertions.assertNotNull(view);
        Assertions.assertEquals(2, view.getTrendPoints().size());
        Assertions.assertEquals(6.0, view.getTrendPoints().get(0).getValue(), 0.001);
        Assertions.assertEquals(8.0, view.getTrendPoints().get(1).getValue(), 0.001);
        Assertions.assertEquals("+2 tests vs start", view.getTrendDeltaLabel());

        final List<WidgetTrendLayerDto> layers = view.getTrendLayers();
        Assertions.assertEquals(2, layers.size());
        final List<String> layerNames = layers.stream().map(WidgetTrendLayerDto::getName).toList();
        Assertions.assertTrue(layerNames.contains("@Browsing (default)"));
        Assertions.assertTrue(layerNames.contains("@Checkout"));

        for (final WidgetTrendLayerDto layer : layers)
        {
            if ("@Browsing (default)".equals(layer.getName()))
            {
                Assertions.assertEquals(3, layer.getCount());
            }
            else if ("@Checkout".equals(layer.getName()))
            {
                Assertions.assertEquals(5, layer.getCount());
            }
        }
    }

    @Test
    public void testTrendGraph_TagMetric_MultiLine_DailyBucketing_WithRunCsv()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-tag-daily-browser", "Daily Browser Trend", WidgetMetric.TAG, "browser",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.DAILY, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "full", 0, false
        );

        final long day1 = 1730000000000L;
        final long day2 = day1 + (86400 * 1000L * 2);

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-b1");
        r1.setStartTimeMs(day1);
        r1.setBrowsersCsv("Chrome");
        r1.setTotalTests(10);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-b2");
        r2.setStartTimeMs(day1 + 3600000L);
        r2.setBrowsersCsv("Firefox");
        r2.setTotalTests(15);

        final TestRunEntity r3 = new TestRunEntity();
        r3.setId("run-b3");
        r3.setStartTimeMs(day2);
        r3.setBrowsersCsv("Chrome");
        r3.setTotalTests(25);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1, r2, r3), Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertEquals(2, view.getTrendPoints().size());
        Assertions.assertEquals(25.0, view.getTrendPoints().get(0).getValue(), 0.001);
        Assertions.assertEquals(25.0, view.getTrendPoints().get(1).getValue(), 0.001);

        final List<WidgetTrendLayerDto> layers = view.getTrendLayers();
        Assertions.assertEquals(2, layers.size());
        final List<String> layerNames = layers.stream().map(WidgetTrendLayerDto::getName).toList();
        Assertions.assertTrue(layerNames.contains("Chrome"));
        Assertions.assertTrue(layerNames.contains("Firefox"));

        for (final WidgetTrendLayerDto layer : layers)
        {
            if ("Chrome".equals(layer.getName()))
            {
                Assertions.assertEquals(25, layer.getCount());
            }
            else if ("Firefox".equals(layer.getName()))
            {
                Assertions.assertEquals(0, layer.getCount());
            }
        }
    }

    @Test
    public void testFlakinessPieChartAggregation()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-flaky-pie", "Flakiness Pie", WidgetMetric.FLAKINESS, "", WidgetPresentation.PIE_CHART,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, "half", 0
        );

        final List<BatchLatestVariationDto> variations = List.of(
            new BatchLatestVariationDto(
                "k1", "k1", "failed-unknown", false, "JAVA", "run-1", "Default", "Chrome", "US", "Checkout", "Default",
                Collections.emptyMap(), 0.0, 0L, "exec-1", Collections.emptyList(), Collections.emptyList(),
                true, 2, 5, "Intermittent failure"
            ),
            new BatchLatestVariationDto(
                "k2", "k2", "passed-clean", false, "JAVA", "run-1", "Default", "Chrome", "US", "Checkout", "Default",
                Collections.emptyMap(), 0.0, 0L, "exec-2", Collections.emptyList(), Collections.emptyList(),
                false, 0, 5, ""
            ),
            new BatchLatestVariationDto(
                "k3", "k3", "passed-clean", false, "JAVA", "run-1", "Default", "Firefox", "US", "Checkout", "Default",
                Collections.emptyMap(), 0.0, 0L, "exec-3", Collections.emptyList(), Collections.emptyList(),
                false, 0, 5, ""
            ),
            new BatchLatestVariationDto(
                "k4", "k4", "passed-clean", false, "JAVA", "run-1", "Default", "Safari", "US", "Checkout", "Default",
                Collections.emptyMap(), 0.0, 0L, "exec-4", Collections.emptyList(), Collections.emptyList(),
                false, 0, 5, ""
            )
        );

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, Collections.emptyList(), variations, Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertEquals(2, view.getPieSlices().size());
        Assertions.assertEquals("Flaky Tests", view.getPieSlices().get(0).getLabel());
        Assertions.assertEquals(1, view.getPieSlices().get(0).getCount());
        Assertions.assertEquals(25.0, view.getPieSlices().get(0).getPercentage(), 0.01);
        Assertions.assertEquals("Stable Tests", view.getPieSlices().get(1).getLabel());
        Assertions.assertEquals(3, view.getPieSlices().get(1).getCount());
        Assertions.assertEquals(75.0, view.getPieSlices().get(1).getPercentage(), 0.01);
        Assertions.assertEquals("25%", view.getPieCenterRate());
        Assertions.assertEquals("FLAKY", view.getPieCenterLabel());
        Assertions.assertEquals("4 Tests", view.getSummaryLabel());
    }

    @Test
    public void testFlakinessRatioBarAggregation()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-flaky-bar", "Flakiness Ratio", WidgetMetric.FLAKINESS, "", WidgetPresentation.RATIO_BAR,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, "full", 0
        );

        final List<BatchLatestVariationDto> variations = List.of(
            new BatchLatestVariationDto(
                "k1", "k1", "failed-unknown", false, "JAVA", "run-1", "Default", "Chrome", "US", "Checkout", "Default",
                Collections.emptyMap(), 0.0, 0L, "exec-1", Collections.emptyList(), Collections.emptyList(),
                true, 2, 5, "Intermittent failure"
            ),
            new BatchLatestVariationDto(
                "k2", "k2", "passed-clean", false, "JAVA", "run-1", "Default", "Chrome", "US", "Checkout", "Default",
                Collections.emptyMap(), 0.0, 0L, "exec-2", Collections.emptyList(), Collections.emptyList(),
                false, 0, 5, ""
            )
        );

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, Collections.emptyList(), variations, Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.hasData());
        Assertions.assertEquals(2, view.getRatioSegments().size());
        Assertions.assertEquals("Flaky Tests", view.getRatioSegments().get(0).getName());
        Assertions.assertEquals(1, view.getRatioSegments().get(0).getCount());
        Assertions.assertEquals(50.0, view.getRatioSegments().get(0).getPercentage(), 0.01);
        Assertions.assertEquals("Stable Tests", view.getRatioSegments().get(1).getName());
        Assertions.assertEquals(1, view.getRatioSegments().get(1).getCount());
        Assertions.assertEquals(50.0, view.getRatioSegments().get(1).getPercentage(), 0.01);
    }

    @Test
    public void testFlakinessPerlenketteAggregation_LatestApproach()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-flaky-pk", "Flakiness Evolution", WidgetMetric.FLAKINESS, "", WidgetPresentation.PERLENKETTE,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, "full", 0, WidgetColorPolarity.HIGHER_IS_RED
        );

        final BatchSuiteEvolutionPointDto pt1 = new BatchSuiteEvolutionPointDto(
            "run-1", 0, "2026-11-01 10:00", 10, 10,
            8, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 80.0, 0
        );
        final BatchSuiteEvolutionPointDto pt2 = new BatchSuiteEvolutionPointDto(
            "run-2", 1, "2026-11-02 10:00", 10, 0,
            7, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 70.0, 3
        );
        final BatchSuiteEvolutionDto suiteEvolution = new BatchSuiteEvolutionDto(List.of(pt1, pt2));

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), null, suiteEvolution, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertNotNull(view.getPerlenkette());
        Assertions.assertTrue(view.getPerlenkette().hasBeads());
        Assertions.assertEquals(2, view.getPerlenkette().getBeads().size());

        final PerlenketteBeadDto bead1 = view.getPerlenkette().getBeads().get(0);
        Assertions.assertEquals(0.0, bead1.getPercentValue(), 0.01);
        Assertions.assertEquals("0%", bead1.getValueFormatted());

        final PerlenketteBeadDto bead2 = view.getPerlenkette().getBeads().get(1);
        Assertions.assertEquals(30.0, bead2.getPercentValue(), 0.01);
        Assertions.assertEquals("30%", bead2.getValueFormatted());
    }

    @Test
    public void testFlakinessTrendGraphAggregation()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-flaky-trend", "Flakiness Trend", WidgetMetric.FLAKINESS, "", WidgetPresentation.TREND_GRAPH,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, "full", 0, WidgetColorPolarity.HIGHER_IS_RED
        );

        final BatchSuiteEvolutionPointDto pt1 = new BatchSuiteEvolutionPointDto(
            "run-1", 0, "2026-11-01 10:00", 10, 10,
            8, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 80.0, 1
        );
        final BatchSuiteEvolutionPointDto pt2 = new BatchSuiteEvolutionPointDto(
            "run-2", 1, "2026-11-02 10:00", 10, 0,
            7, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 70.0, 2
        );
        final BatchSuiteEvolutionDto suiteEvolution = new BatchSuiteEvolutionDto(List.of(pt1, pt2));

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), null, suiteEvolution, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertEquals(2, view.getTrendPoints().size());
        Assertions.assertEquals(10.0, view.getTrendPoints().get(0).getValue(), 0.01);
        Assertions.assertEquals(20.0, view.getTrendPoints().get(1).getValue(), 0.01);
        Assertions.assertNotNull(view.getTrendLayers());
        Assertions.assertEquals(1, view.getTrendLayers().size());
        Assertions.assertEquals("Flaky Tests", view.getTrendLayers().get(0).getName());
        Assertions.assertTrue(view.getTrendDeltaLabel().contains("vs start"));
    }

    @Test
    public void testFlakinessTextAndListAggregation()
    {
        final DashboardWidgetConfigDto textConfig = new DashboardWidgetConfigDto(
            "w-flaky-text", "Flakiness KPI", WidgetMetric.FLAKINESS, "", WidgetPresentation.TEXT,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, "half", 0
        );

        final DashboardWidgetConfigDto listConfig = new DashboardWidgetConfigDto(
            "w-flaky-list", "Flakiness List", WidgetMetric.FLAKINESS, "", WidgetPresentation.LIST,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, "half", 0
        );

        final List<BatchLatestVariationDto> variations = List.of(
            new BatchLatestVariationDto(
                "com.example.TestClass#testFlakyMethod", "com.example.TestClass#testFlakyMethod", "failed-unknown", false, "JAVA", "run-1", "Default", "Chrome", "US", "Checkout", "Default",
                Collections.emptyMap(), 0.0, 0L, "exec-1", Collections.emptyList(), Collections.emptyList(),
                true, 3, 10, "Erratic timeout"
            ),
            new BatchLatestVariationDto(
                "com.example.TestClass#testStableMethod", "com.example.TestClass#testStableMethod", "passed-clean", false, "JAVA", "run-1", "Default", "Chrome", "US", "Checkout", "Default",
                Collections.emptyMap(), 0.0, 0L, "exec-2", Collections.emptyList(), Collections.emptyList(),
                false, 0, 10, ""
            )
        );

        final DashboardWidgetViewDto textView = service.aggregateWidget(
            textConfig, Collections.emptyList(), variations, Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(textView);
        Assertions.assertEquals("50.0% FLAKY", textView.getPrimaryTextValue());
        Assertions.assertEquals("1 of 2 Tests Flaky", textView.getSecondaryTextValue());

        final DashboardWidgetViewDto listView = service.aggregateWidget(
            listConfig, Collections.emptyList(), variations, Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(listView);
        Assertions.assertEquals(2, listView.getListItems().size());
        Assertions.assertEquals("Flaky Tests", listView.getListItems().get(0).getLabel());
        Assertions.assertEquals("shuffle", listView.getListItems().get(0).getIcon());
        Assertions.assertEquals(1, listView.getListItems().get(0).getSubItems().size());
        Assertions.assertEquals("Stable Tests", listView.getListItems().get(1).getLabel());
        Assertions.assertEquals("check_circle", listView.getListItems().get(1).getIcon());
    }

    @Test
    public void testFlakinessContributingExecutionsResolution()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-flaky-contrib", "Flaky Executions", WidgetMetric.FLAKINESS, "", WidgetPresentation.PIE_CHART,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, "full", 0
        );

        final TestRunEntity run = new TestRunEntity();
        run.setId("run-flaky-1");
        run.setStartTimeMs(1000L);
        run.setFlakyCount(2);

        final TestExecutionDto execFlaky1 = new TestExecutionDto(
            "e-flaky-1", "run-flaky-1", "com.example.FlakyTestA", "testA", "testA", "FAILED_UNKNOWN", "en-US", "Chrome", Collections.emptyMap()
        );
        execFlaky1.setFlaky(true);
        execFlaky1.setFlakyFailureCount(5);
        execFlaky1.setFlakyTotalRuns(10);

        final TestExecutionDto execFlaky2 = new TestExecutionDto(
            "e-flaky-2", "run-flaky-1", "com.example.FlakyTestB", "testB", "testB", "PASSED", "en-US", "Chrome", Collections.emptyMap()
        );
        execFlaky2.setFlaky(true);
        execFlaky2.setFlakyFailureCount(2);
        execFlaky2.setFlakyTotalRuns(10);

        final TestExecutionDto execStable = new TestExecutionDto(
            "e-stable", "run-flaky-1", "com.example.StableTest", "testStable", "testStable", "PASSED", "en-US", "Chrome", Collections.emptyMap()
        );
        execStable.setFlaky(false);

        final RunReportDto report = new RunReportDto("run-flaky-1", "DefaultBatch", "2026-11-03 12:00", "5s", 3, 2, 0, 0, 1, 0, List.of(execStable, execFlaky2, execFlaky1));

        final AuraReportDataService mockDataService = Mockito.mock(AuraReportDataService.class);
        Mockito.when(mockDataService.getRunReport("run-flaky-1")).thenReturn(report);

        final WidgetContributingExecutionsDto contribs = service.resolveContributingExecutions(
            config, List.of(run), Collections.emptyList(), mockDataService
        );

        Assertions.assertNotNull(contribs);
        Assertions.assertEquals(1, contribs.getRunGroups().size());
        final WidgetRunExecutionsGroupDto group = contribs.getRunGroups().get(0);
        Assertions.assertEquals(2, group.getFlakyCount());
        Assertions.assertEquals(2, group.getExecutions().size());
        // Verify sorted descending by failure count: execFlaky1 (5 fails) then execFlaky2 (2 fails)
        Assertions.assertEquals("e-flaky-1", group.getExecutions().get(0).getId());
        Assertions.assertEquals("e-flaky-2", group.getExecutions().get(1).getId());
        Assertions.assertEquals(2, contribs.getOverallFlakyCount());
    }

    @Test
    public void testTrendGraph_Bugs_SingleBugMultipleVariations_ReportsConsistentCount()
    {
        final DashboardWidgetConfigDto trendConfig = new DashboardWidgetConfigDto(
            "w-trend-bugs-unique", "Bugs Trend Unique", WidgetMetric.BUGS, "",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );

        final DashboardWidgetConfigDto cardConfig = new DashboardWidgetConfigDto(
            "w-card-bugs-unique", "Bugs Card Unique", WidgetMetric.BUGS, "",
            WidgetPresentation.TEXT_WITH_SMALL_TREND, WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "quarter", 0, false
        );

        // 1 unique bug ticket assigned across 2 different test variations
        final TestBaseBugEntity bugVar1 = new TestBaseBugEntity(
            "CheckoutTest#testPayment", "BUG-100", "DefaultBatch", "ALL",
            1000L, "run-1", "MAJOR", "browser:Chrome"
        );
        final TestBaseBugEntity bugVar2 = new TestBaseBugEntity(
            "CheckoutTest#testShipping", "BUG-100", "DefaultBatch", "ALL",
            1000L, "run-1", "MAJOR", "browser:Firefox"
        );
        final List<TestBaseBugEntity> bugs = List.of(bugVar1, bugVar2);

        // Run 1: both variations fail with known bug -> failedKnownCount = 2
        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-1");
        r1.setStartTimeMs(1000L);
        r1.setFailedKnownCount(2);
        r1.setTotalTests(10);

        // Run 2: both variations fail with known bug -> failedKnownCount = 2
        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-2");
        r2.setStartTimeMs(2000L);
        r2.setFailedKnownCount(2);
        r2.setTotalTests(10);

        // Run 3: only 1 variation fails with known bug -> failedKnownCount = 1
        final TestRunEntity r3 = new TestRunEntity();
        r3.setId("run-3");
        r3.setStartTimeMs(3000L);
        r3.setFailedKnownCount(1);
        r3.setTotalTests(10);

        final List<TestRunEntity> runs = List.of(r1, r2, r3);

        // 1. Trend Graph View
        final DashboardWidgetViewDto trendView = service.aggregateWidget(
            trendConfig, runs, Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), null, null, bugs
        );

        Assertions.assertNotNull(trendView);
        Assertions.assertEquals(3, trendView.getTrendPoints().size());
        // Since both variations belong to BUG-100, each point must resolve to 1 unique active bug!
        Assertions.assertEquals(1.0, trendView.getTrendPoints().get(0).getValue());
        Assertions.assertEquals("1 bug", trendView.getTrendPoints().get(0).getValueFormatted());
        Assertions.assertEquals(1.0, trendView.getTrendPoints().get(1).getValue());
        Assertions.assertEquals("1 bug", trendView.getTrendPoints().get(1).getValueFormatted());
        Assertions.assertEquals(1.0, trendView.getTrendPoints().get(2).getValue());
        Assertions.assertEquals("1 bug", trendView.getTrendPoints().get(2).getValueFormatted());

        // Delta between run-1 (1 bug) and run-3 (1 bug) must be 0 bugs vs start
        Assertions.assertEquals("+0 bugs vs start", trendView.getTrendDeltaLabel());

        // Trend layer must show 1 active bug
        Assertions.assertFalse(trendView.getTrendLayers().isEmpty());
        Assertions.assertEquals("Active Known Bugs", trendView.getTrendLayers().get(0).getName());
        Assertions.assertEquals(1, trendView.getTrendLayers().get(0).getCount());

        // Y-axis tick must use singular "1 bug"
        Assertions.assertTrue(trendView.getYAxisTicks().stream().anyMatch(t -> "1 bug".equals(t.getLabel())));

        // 2. Card Widget with Sparkline View
        final DashboardWidgetViewDto cardView = service.aggregateWidget(
            cardConfig, runs, Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), null, null, bugs
        );
        Assertions.assertNotNull(cardView);
        Assertions.assertEquals("1 Bug", cardView.getPrimaryTextValue());
        Assertions.assertEquals("+0 bugs vs start", cardView.getTrendDeltaLabel());
    }

    @Test
    public void testTrendGraph_Bugs_LifecycleIntroductionAndRemoval()
    {
        final DashboardWidgetConfigDto trendConfig = new DashboardWidgetConfigDto(
            "w-trend-bugs-lifecycle", "Bugs Lifecycle", WidgetMetric.BUGS, "",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );

        // Bug introduced at run-2 and removed at run-4
        final TestBaseBugEntity bug = new TestBaseBugEntity(
            "SearchTest#testQuery", "BUG-200", "DefaultBatch", "ALL",
            2000L, "run-2", "CRITICAL", null
        );
        bug.setRemovedRunId("run-4");
        bug.setRemovedAtMs(4000L);
        final List<TestBaseBugEntity> bugs = List.of(bug);

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-1");
        r1.setStartTimeMs(1000L);
        r1.setFailedKnownCount(0);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-2");
        r2.setStartTimeMs(2000L);
        r2.setFailedKnownCount(1);

        final TestRunEntity r3 = new TestRunEntity();
        r3.setId("run-3");
        r3.setStartTimeMs(3000L);
        r3.setFailedKnownCount(1);

        final TestRunEntity r4 = new TestRunEntity();
        r4.setId("run-4");
        r4.setStartTimeMs(4000L);
        r4.setFailedKnownCount(0);

        final List<TestRunEntity> runs = List.of(r1, r2, r3, r4);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            trendConfig, runs, Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), null, null, bugs
        );

        Assertions.assertNotNull(view);
        Assertions.assertEquals(4, view.getTrendPoints().size());
        // run-1: not yet introduced
        Assertions.assertEquals(0.0, view.getTrendPoints().get(0).getValue());
        Assertions.assertEquals("0 bugs", view.getTrendPoints().get(0).getValueFormatted());
        // run-2: active
        Assertions.assertEquals(1.0, view.getTrendPoints().get(1).getValue());
        Assertions.assertEquals("1 bug", view.getTrendPoints().get(1).getValueFormatted());
        // run-3: active
        Assertions.assertEquals(1.0, view.getTrendPoints().get(2).getValue());
        Assertions.assertEquals("1 bug", view.getTrendPoints().get(2).getValueFormatted());
        // run-4: removed
        Assertions.assertEquals(0.0, view.getTrendPoints().get(3).getValue());
        Assertions.assertEquals("0 bugs", view.getTrendPoints().get(3).getValueFormatted());
    }

    @Test
    public void testTextWithSmallTrendWidget_StatusPercent_NoArtificialCliffDrop()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-status-sparkline-pct", "Status Sparkline", WidgetMetric.STATUS, "",
            WidgetPresentation.TEXT_WITH_SMALL_TREND, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );

        final BatchSuiteEvolutionPointDto pt1 = new BatchSuiteEvolutionPointDto(
            "run-1", 0, "Run 1", 116, 116, 113, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 97.4, 0, Collections.emptyMap()
        );
        final BatchSuiteEvolutionPointDto pt2 = new BatchSuiteEvolutionPointDto(
            "run-2", 1, "Run 2", 126, 10, 121, 0, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 96.0, 0, Collections.emptyMap()
        );
        final BatchSuiteEvolutionPointDto pt3 = new BatchSuiteEvolutionPointDto(
            "run-3", 2, "Run 3", 232, 106, 226, 0, 0, 0, 0, 0, 0, 0, 0, 6, 0, 0, 0, 0, 97.4, 0, Collections.emptyMap()
        );
        final BatchSuiteEvolutionDto evo = new BatchSuiteEvolutionDto(List.of(pt1, pt2, pt3), "run-3");

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, evo, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertEquals(WidgetPresentation.TEXT_WITH_SMALL_TREND, view.getPresentation());
        Assertions.assertTrue(view.getSparklineSvgPath().startsWith("M "));
        // In the bounded percentage sparkline (0-100% with min 20% window),
        // 96.0% and 97.4% map to the top band (ys <= 10.0), never plunging to the floor (~28.0)
        final String path = view.getSparklineSvgPath();
        final String[] parts = path.split("[MLC ,]+");
        for (int i = 2; i < parts.length; i += 2)
        {
            if (!parts[i].isBlank())
            {
                final double y = Double.parseDouble(parts[i]);
                Assertions.assertTrue(y <= 25.0, "Y coordinate " + y + " should stay in the upper band, not plunge");
            }
        }
    }

    @Test
    public void testTextWithSmallTrendWidget_StatusAbsolute_MirrorsVolumeCurve()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-status-sparkline-abs", "Status Sparkline Absolute", WidgetMetric.STATUS, "",
            WidgetPresentation.TEXT_WITH_SMALL_TREND, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );

        final BatchSuiteEvolutionPointDto pt1 = new BatchSuiteEvolutionPointDto(
            "run-1", 0, "Run 1", 116, 116, 110, 0, 0, 0, 0, 0, 0, 0, 0, 6, 0, 0, 0, 0, 94.8, 0, Collections.emptyMap()
        );
        final BatchSuiteEvolutionPointDto pt2 = new BatchSuiteEvolutionPointDto(
            "run-2", 1, "Run 2", 232, 116, 225, 0, 0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 97.0, 0, Collections.emptyMap()
        );
        final BatchSuiteEvolutionDto evo = new BatchSuiteEvolutionDto(List.of(pt1, pt2), "run-2");

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, evo, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertEquals(WidgetPresentation.TEXT_WITH_SMALL_TREND, view.getPresentation());
        // For ABSOLUTE, points reflect passed test volume (110 and 225)
        Assertions.assertEquals(2, view.getTrendPoints().size());
        Assertions.assertEquals(110.0, view.getTrendPoints().get(0).getValue());
        Assertions.assertEquals(225.0, view.getTrendPoints().get(1).getValue());
        Assertions.assertTrue(view.getTrendDeltaLabel().contains("tests vs start"), "Delta should be in tests: " + view.getTrendDeltaLabel());
        Assertions.assertTrue(view.getSparklineSvgPath().startsWith("M "));
    }

    @Test
    public void testGenerateSparklineSvgPath_DirectBoundedAndAdaptiveScale()
    {
        // 1. Single or empty points
        Assertions.assertEquals("M 6.0 35.0 L 294.0 35.0", WidgetAggregationService.generateSparklineSvgPath(null));
        Assertions.assertEquals("M 6.0 35.0 L 294.0 35.0", WidgetAggregationService.generateSparklineSvgPath(Collections.emptyList()));

        // 2. 100% all runs on percentage metric: line must be at the top padding (y = 6.0), not middle (35.0)
        final WidgetTrendPointDto p100a = new WidgetTrendPointDto(0, 0.0, 0.0, "r1", "", 100.0, "100%", "r1");
        final WidgetTrendPointDto p100b = new WidgetTrendPointDto(1, 0.0, 0.0, "r2", "", 100.0, "100%", "r2");
        final String path100 = WidgetAggregationService.generateSparklineSvgPath(List.of(p100a, p100b), 0.0, 100.0);
        Assertions.assertEquals("M 6.0 6.0 L 294.0 6.0", path100);

        // 3. 0% all runs on percentage metric: line must be at the bottom padding (y = 64.0)
        final WidgetTrendPointDto p0a = new WidgetTrendPointDto(0, 0.0, 0.0, "r1", "", 0.0, "0%", "r1");
        final WidgetTrendPointDto p0b = new WidgetTrendPointDto(1, 0.0, 0.0, "r2", "", 0.0, "0%", "r2");
        final String path0 = WidgetAggregationService.generateSparklineSvgPath(List.of(p0a, p0b), 0.0, 100.0);
        Assertions.assertEquals("M 6.0 64.0 L 294.0 64.0", path0);
    }

    @Test
    public void testGenerateSparklineGeometry_CalculatesStrokeAreaAndEndpoint()
    {
        // 1. Empty / null cases
        final SparklineGeometryDto emptyGeo = WidgetAggregationService.generateSparklineGeometry(null, 0.0, 100.0);
        Assertions.assertNotNull(emptyGeo);
        Assertions.assertEquals("M 6.0 35.0 L 294.0 35.0", emptyGeo.getStrokePath());
        Assertions.assertEquals("", emptyGeo.getAreaPath());
        Assertions.assertFalse(emptyGeo.isHasEndpoint());

        // 2. Single point produces flat line and closed rectangular area
        final WidgetTrendPointDto ptSingle = new WidgetTrendPointDto(0, 0.0, 0.0, "r1", "", 90.0, "90%", "r1");
        final SparklineGeometryDto singleGeo = WidgetAggregationService.generateSparklineGeometry(List.of(ptSingle), 0.0, 100.0);
        Assertions.assertNotNull(singleGeo);
        Assertions.assertTrue(singleGeo.getStrokePath().startsWith("M "));
        Assertions.assertTrue(singleGeo.getAreaPath().endsWith(" Z"));
        Assertions.assertTrue(singleGeo.isHasEndpoint());
        Assertions.assertEquals(294.0, singleGeo.getLastX(), 0.01);

        // 3. Multi-point series produces smooth Bezier curve and closed area
        final WidgetTrendPointDto p1 = new WidgetTrendPointDto(0, 0.0, 0.0, "r1", "", 95.0, "95%", "r1");
        final WidgetTrendPointDto p2 = new WidgetTrendPointDto(1, 0.0, 0.0, "r2", "", 97.0, "97%", "r2");
        final WidgetTrendPointDto p3 = new WidgetTrendPointDto(2, 0.0, 0.0, "r3", "", 98.0, "98%", "r3");
        final SparklineGeometryDto multiGeo = WidgetAggregationService.generateSparklineGeometry(List.of(p1, p2, p3), 0.0, 100.0);
        Assertions.assertNotNull(multiGeo);
        Assertions.assertTrue(multiGeo.getStrokePath().contains(" C "));
        Assertions.assertTrue(multiGeo.getAreaPath().endsWith(" L 294.0 70.0 L 6.0 70.0 Z"));
        Assertions.assertEquals(294.0, multiGeo.getLastX(), 0.01);
        Assertions.assertTrue(multiGeo.getLastY() > 0.0 && multiGeo.getLastY() < 70.0);
        Assertions.assertTrue(multiGeo.isHasEndpoint());
    }

    @Test
    public void testTextWithSmallTrendWidget_BugTrendPolarityAndGeometry()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-bugs-trend", "Active Defects Trend", WidgetMetric.BUGS, "",
            WidgetPresentation.TEXT_WITH_SMALL_TREND, WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );

        final TestRunEntity run1 = new TestRunEntity("r1", "Batch", "COMPLETED", "Main", "Default", "Java", "Chrome", "10:00", 1000L);
        final TestRunEntity run2 = new TestRunEntity("r2", "Batch", "COMPLETED", "Main", "Default", "Java", "Chrome", "11:00", 2000L);

        // Bug added in run 2 (regression: 0 bugs -> 1 bug)
        final TestBaseBugEntity bug = new TestBaseBugEntity("var-1", "BUG-101", "Batch", "Default", 2000L, "r2", "CRITICAL", null);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(run1, run2), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, List.of(bug)
        );

        Assertions.assertNotNull(view);
        Assertions.assertEquals(WidgetPresentation.TEXT_WITH_SMALL_TREND, view.getPresentation());
        // 1. Math delta is "+1 bug vs start"
        Assertions.assertTrue(view.getTrendDeltaLabel().contains("+1 bug vs start"), "Delta should be +1: " + view.getTrendDeltaLabel());
        // 2. Favorable polarity: an increase in bugs MUST be unfavorable (isTrendPositive == false -> red styling)
        Assertions.assertFalse(view.isTrendPositive(), "Bug increase should NOT be considered a positive trend");
        // 3. Sparkline area and endpoint must be populated
        Assertions.assertFalse(view.getSparklineSvgPath().isEmpty());
        Assertions.assertFalse(view.getSparklineAreaSvgPath().isEmpty());
        Assertions.assertTrue(view.isSparklineHasEndpoint());
        Assertions.assertEquals(294.0, view.getSparklineLastX(), 0.01);
    }

    @Test
    public void testPerlenketteTimelineRailAndNoOccludedDots()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-pk-timeline", "Execution Timeline", WidgetMetric.STATUS, "",
            WidgetPresentation.PERLENKETTE, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT,
            "half", 1
        );

        final List<TestRunEntity> runs = new ArrayList<>();
        for (int i = 1; i <= 5; i++)
        {
            final TestRunEntity r = new TestRunEntity();
            r.setId("run-" + i);
            r.setStartTimeMs(1000L * i);
            r.setTimestampLabel("10:0" + i + " AM");
            r.setPassRate(90.0 + i);
            r.setTotalTests(100);
            r.setPassedCount(90 + i);
            runs.add(r);
        }

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, runs, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        final PerlenketteDto pearls = view.getPerlenkette();
        Assertions.assertNotNull(pearls);
        Assertions.assertEquals(5, pearls.getTotalBeads());

        // 1. Timeline rail path has straight horizontal segments and directional chevrons
        final String path = pearls.getSvgWavePath();
        Assertions.assertNotNull(path);
        Assertions.assertTrue(path.startsWith("M 0.0 90.0 L 800.0 90.0"));
        Assertions.assertTrue(path.contains(" M "), "Should contain directional chevron segments");

        // 2. Bead geometry and label assertions
        final List<PerlenketteBeadDto> beads = pearls.getBeads();
        for (int i = 0; i < beads.size(); i++)
        {
            final PerlenketteBeadDto b = beads.get(i);
            Assertions.assertEquals(90.0, b.getCy(), 0.01);
            Assertions.assertEquals(36.0, b.getRadius(), 0.01);
            Assertions.assertEquals("#run-" + (i + 1), b.getLabel());
            Assertions.assertEquals("01:00", b.getTimestampLabel());
            Assertions.assertEquals(runs.get(i).getTimestampLabel(), b.getDetails().get("Timestamp"));
            Assertions.assertFalse(b.getInnerValue().isBlank());
        }

        // 3. Minor dots must lie strictly outside every pearl radius (zero occluded blemish artifacts)
        final List<PerlenketteMinorDotDto> dots = pearls.getMinorDots();
        Assertions.assertTrue(dots.size() >= 5);
        for (final PerlenketteMinorDotDto dot : dots)
        {
            Assertions.assertEquals(90.0, dot.getCy(), 0.01);
            Assertions.assertTrue(dot.getRadius() > 0.0);
            Assertions.assertNotNull(dot.getColor());

            // Mathematical clearance: distance from dot to each pearl center must be >= radius
            for (final PerlenketteBeadDto b : beads)
            {
                final double distance = Math.abs(dot.getCx() - b.getCx());
                Assertions.assertTrue(distance >= b.getRadius(),
                    "Minor dot at " + dot.getCx() + " must not be occluded by pearl at " + b.getCx() + " (radius " + b.getRadius() + ")");
            }
        }
    }

    @Test
    public void testPerlenkette_LongRunIds_ProducesCompactMilestoneLabels()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-pk-long-ids", "Production Timeline", WidgetMetric.STATUS, "",
            WidgetPresentation.PERLENKETTE, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT,
            "half", 1
        );

        final List<TestRunEntity> runs = new ArrayList<>();
        final String[] runIds = new String[] {
            "run_dev_na_20261026_104200",
            "run_dev_na_20261026_104500",
            "run_dev_na_20261026_104800",
            "run_dev_na_20261026_105100",
            "run_dev_na_20261026_105400"
        };
        final String[] timestamps = new String[] {
            "2026-10-26 10:42:00",
            "2026-10-26 10:45:00",
            "2026-10-26 10:48:00",
            "2026-10-26 10:51:00",
            "2026-10-26 10:54:00"
        };
        final String[] expectedTimes = new String[] {
            "10:42", "10:45", "10:48", "10:51", "10:54"
        };

        for (int i = 0; i < runIds.length; i++)
        {
            final TestRunEntity r = new TestRunEntity();
            r.setId(runIds[i]);
            r.setTimestampLabel(timestamps[i]);
            r.setPassRate(96.0 + i);
            r.setTotalTests(100);
            r.setPassedCount(96 + i);
            runs.add(r);
        }

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, runs, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        final PerlenketteDto pearls = view.getPerlenkette();
        Assertions.assertNotNull(pearls);
        Assertions.assertEquals(5, pearls.getTotalBeads());

        final List<PerlenketteBeadDto> beads = pearls.getBeads();
        for (int i = 0; i < beads.size(); i++)
        {
            final PerlenketteBeadDto b = beads.get(i);
            // Must have clean, non-overlapping sequential milestone label
            Assertions.assertEquals("Run " + (i + 1), b.getLabel());
            // Same-day runs must render compact HH:mm timestamp
            Assertions.assertEquals(expectedTimes[i], b.getTimestampLabel());
            // Full raw run ID must be preserved in id and details for tooltip
            Assertions.assertEquals(runIds[i], b.getId());
            Assertions.assertEquals("#" + runIds[i], b.getDetails().get("Run ID"));
            Assertions.assertEquals(timestamps[i], b.getDetails().get("Timestamp"));
        }
    }

    @Test
    public void testPerlenkette_MultiDayRuns_ProducesFormattedDateTimestamps()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-pk-multi-day", "Multi-day Progression", WidgetMetric.STATUS, "",
            WidgetPresentation.PERLENKETTE, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT,
            "half", 1
        );

        final List<TestRunEntity> runs = new ArrayList<>();
        final String[] runIds = new String[] { "run-101", "run-102", "run-103" };
        final String[] timestamps = new String[] {
            "2026-10-24 14:00:00",
            "2026-10-25 15:30:00",
            "2026-10-26 09:15:00"
        };
        final String[] expectedDates = new String[] { "Oct 24", "Oct 25", "Oct 26" };

        for (int i = 0; i < runIds.length; i++)
        {
            final TestRunEntity r = new TestRunEntity();
            r.setId(runIds[i]);
            r.setTimestampLabel(timestamps[i]);
            r.setPassRate(90.0 + i);
            r.setTotalTests(50);
            r.setPassedCount(45 + i);
            runs.add(r);
        }

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, runs, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        final PerlenketteDto pearls = view.getPerlenkette();
        Assertions.assertNotNull(pearls);
        Assertions.assertEquals(3, pearls.getTotalBeads());

        final List<PerlenketteBeadDto> beads = pearls.getBeads();
        for (int i = 0; i < beads.size(); i++)
        {
            final PerlenketteBeadDto b = beads.get(i);
            // Short IDs retain short hash label
            Assertions.assertEquals("#" + runIds[i], b.getLabel());
            // Distinct multi-day runs must render compact MMM dd date
            Assertions.assertEquals(expectedDates[i], b.getTimestampLabel());
        }
    }

    @Test
    public void testPerlenkette_BugsMetricWithPercentFormat_DoesNotLeakPassRate()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-pk-bugs-percent", "Bug Rate Chain", WidgetMetric.BUGS, "",
            WidgetPresentation.PERLENKETTE, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT,
            "half", 1
        );

        final TestRunEntity r = new TestRunEntity();
        r.setId("run_qa_20261026_090000");
        r.setStartTimeMs(1730000000000L);
        r.setTimestampLabel("2026-10-26 09:00:00");
        r.setPassRate(97.0);
        r.setTotalTests(100);
        r.setFailedKnownCount(3);
        r.setPassedCount(97);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        final PerlenketteDto pearls = view.getPerlenkette();
        Assertions.assertNotNull(pearls);
        Assertions.assertEquals(1, pearls.getTotalBeads());

        final PerlenketteBeadDto b = pearls.getBeads().get(0);
        // Must show Bug Rate (3.0%), NEVER suite pass rate (97.0%)
        Assertions.assertEquals("3.0%", b.getValueFormatted());
        Assertions.assertEquals(3.0, b.getPercentValue(), 0.01);
        Assertions.assertEquals("3", b.getInnerValue());
        Assertions.assertEquals("3", b.getDetails().get("Active Bugs"));
        Assertions.assertEquals("100", b.getDetails().get("Total Tests"));
        Assertions.assertEquals("3.0%", b.getDetails().get("Bug Rate"));
        Assertions.assertFalse(b.getDetails().containsKey("Pass Rate"), "Must not leak Pass Rate into Bugs details");
    }

    @Test
    public void testPerlenkette_CostsMetric_NeverLeaksPassRate()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-pk-costs-percent", "LLM Costs Chain", WidgetMetric.COSTS, "",
            WidgetPresentation.PERLENKETTE, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT,
            "half", 1
        );

        final TestRunEntity r = new TestRunEntity();
        r.setId("run_qa_20261026_090000");
        r.setStartTimeMs(1730000000000L);
        r.setTimestampLabel("2026-10-26 09:00:00");
        r.setPassRate(98.5);
        r.setTotalTests(100);
        r.setTotalLlmCost(0.0420);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        final PerlenketteDto pearls = view.getPerlenkette();
        Assertions.assertNotNull(pearls);
        Assertions.assertEquals(1, pearls.getTotalBeads());

        final PerlenketteBeadDto b = pearls.getBeads().get(0);
        // Costs must always format as currency ($0.0420) and innerValue ($0.04), NEVER pass rate
        Assertions.assertEquals("$0.0420", b.getValueFormatted());
        Assertions.assertEquals("$0.04", b.getInnerValue());
        Assertions.assertEquals("$0.0420", b.getDetails().get("Cost"));
        Assertions.assertFalse(b.getDetails().containsKey("Pass Rate"), "Must not leak Pass Rate into Costs details");
    }

    @Test
    public final void testPerlenketteBugsDetailsBugCountInBuckets()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-bugs-details", "Bugs Hourly Chain", WidgetMetric.BUGS, "", WidgetPresentation.PERLENKETTE,
            WidgetTrendMode.HOURLY, WidgetValueFormat.ABSOLUTE, "full", 10
        );

        final long baseTime = 1730000000000L;
        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-b-hourly");
        r1.setStartTimeMs(baseTime);
        r1.setFailedKnownCount(4);
        r1.setTotalTests(20);
        r1.setPassedCount(16);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertNotNull(view.getPerlenkette());
        Assertions.assertEquals(1, view.getPerlenkette().getTotalBeads());
        final PerlenketteBeadDto bead = view.getPerlenkette().getBeads().get(0);
        Assertions.assertEquals("4", bead.getDetails().get("Active Bugs"));
        Assertions.assertEquals("20", bead.getDetails().get("Total Tests"));
        Assertions.assertEquals("20.0%", bead.getDetails().get("Bug Rate"));
    }

    @Test
    public final void testCombineWidgetsContributingRunsEquivalence()
    {
        final DashboardWidgetConfigDto w1 = new DashboardWidgetConfigDto(
            "w1", "Status Pie", WidgetMetric.STATUS, "", WidgetPresentation.PIE_CHART,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, WidgetPointScope.LAST_RUN, "half", 0
        );
        final DashboardWidgetConfigDto w2 = new DashboardWidgetConfigDto(
            "w2", "Cost Ratio", WidgetMetric.COSTS, "", WidgetPresentation.RATIO_BAR,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, WidgetPointScope.LAST_RUN, "half", 0
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-1");
        r1.setStartTimeMs(1000L);
        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-2");
        r2.setStartTimeMs(2000L);

        final List<TestRunEntity> runs = List.of(r1, r2);

        // Both evaluate LAST_RUN (run-2) -> should share identical contributing runs
        final boolean sameRuns = service.areBasedOnSameContributingRuns(w1, w2, runs, Collections.emptyList());
        Assertions.assertTrue(sameRuns, "Widgets with matching LAST_RUN scope must share identical contributing runs");

        // Now create w3 with custom run filter limiting to 1 run
        final WidgetFilterConfigDto filter1 = new WidgetFilterConfigDto(null, null, null, null, null, null, null, null, null, null, null, null, 1, null, null);
        final DashboardWidgetConfigDto w3 = new DashboardWidgetConfigDto(
            "w3", "Filtered 1", WidgetMetric.STATUS, null, WidgetPresentation.PIE_CHART,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, WidgetPointScope.TODAY, "half", 0,
            false, false, null, true, filter1, null, false, false
        );

        final boolean different = service.areBasedOnSameContributingRuns(w1, w3, runs, Collections.emptyList());
        Assertions.assertFalse(different, "Widgets with differing scopes or filters must not match contributing runs");

        Assertions.assertFalse(service.areBasedOnSameContributingRuns(null, w2, runs, Collections.emptyList()));
        Assertions.assertFalse(service.areBasedOnSameContributingRuns(w1, null, runs, Collections.emptyList()));
    }

    @Test
    public final void testCombineWidgetsContributingRunsWithEmptyRunsListGracefulHandling()
    {
        final DashboardWidgetConfigDto w1 = new DashboardWidgetConfigDto(
            "w1", "Status Pie", WidgetMetric.STATUS, "", WidgetPresentation.PIE_CHART,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, WidgetPointScope.LAST_RUN, "half", 0
        );
        final DashboardWidgetConfigDto w2 = new DashboardWidgetConfigDto(
            "w2", "Cost Ratio", WidgetMetric.COSTS, "", WidgetPresentation.RATIO_BAR,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, WidgetPointScope.LAST_RUN, "half", 0
        );

        final boolean sameRuns = service.areBasedOnSameContributingRuns(w1, w2, Collections.emptyList(), Collections.emptyList());
        Assertions.assertFalse(sameRuns, "Empty runs list must be handled gracefully without throwing exceptions and return false as there are no contributing runs");
    }

    @Test
    public final void testCombineWidgetsPointScopeMismatchRejection()
    {
        final DashboardWidgetConfigDto wLatest = new DashboardWidgetConfigDto(
            "w-latest", "Status Latest", WidgetMetric.STATUS, "", WidgetPresentation.PIE_CHART,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS, "half", 0
        );
        final DashboardWidgetConfigDto wLastRun = new DashboardWidgetConfigDto(
            "w-lastrun", "Status Last Run", WidgetMetric.STATUS, "", WidgetPresentation.PIE_CHART,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, WidgetPointScope.LAST_RUN, "half", 0
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-1");
        r1.setStartTimeMs(1000L);
        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-2");
        r2.setStartTimeMs(2000L);

        final List<TestRunEntity> runs = List.of(r1, r2);

        final boolean matches = service.areBasedOnSameContributingRuns(wLatest, wLastRun, runs, Collections.emptyList());
        Assertions.assertFalse(matches, "Widgets with differing scopes (LATEST_TEST_STATUS vs LAST_RUN) must not match");
    }

    @Test
    public final void testCombineWidgetsBrowserFilterMismatchRejection()
    {
        final WidgetFilterConfigDto chromeFilter = new WidgetFilterConfigDto(
            Set.of("Chrome"), Collections.emptySet(), Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(), Collections.emptyMap(), Collections.emptyMap(),
            null, null, null
        );
        final WidgetFilterConfigDto firefoxFilter = new WidgetFilterConfigDto(
            Set.of("Firefox"), Collections.emptySet(), Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(), Collections.emptyMap(), Collections.emptyMap(),
            null, null, null
        );

        final DashboardWidgetConfigDto wChrome = new DashboardWidgetConfigDto(
            "w-chrome", "Chrome Status", WidgetMetric.STATUS, null, WidgetPresentation.PIE_CHART,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS, "half", 0,
            false, false, null, true, chromeFilter, null, false, false
        );
        final DashboardWidgetConfigDto wFirefox = new DashboardWidgetConfigDto(
            "w-firefox", "Firefox Status", WidgetMetric.STATUS, null, WidgetPresentation.PIE_CHART,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS, "half", 0,
            false, false, null, true, firefoxFilter, null, false, false
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-1");
        r1.setStartTimeMs(1000L);
        final List<TestRunEntity> runs = List.of(r1);

        final boolean matches = service.areBasedOnSameContributingRuns(wChrome, wFirefox, runs, Collections.emptyList());
        Assertions.assertFalse(matches, "Widgets with conflicting browser filters must not match contributing runs");
    }

    @Test
    public void testAggregateRunsWidgetWithoutFilter()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-runs-list", "List of Runs", WidgetMetric.RUNS, null,
            WidgetPresentation.RUNS, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "full", 0
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-1");
        r1.setStartTimeMs(1000L);
        r1.setTotalTests(10);
        r1.setPassedCount(10);
        r1.setPassRate(100.0);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-2");
        r2.setStartTimeMs(2000L);
        r2.setTotalTests(20);
        r2.setPassedCount(15);
        r2.setPassRate(75.0);

        final List<TestRunEntity> runs = List.of(r1, r2);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, runs, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.isRuns());
        Assertions.assertTrue(view.hasData());
        Assertions.assertEquals(2, view.getTotalRunsCount());
        Assertions.assertEquals(2, view.getShowingRunsCount());
        Assertions.assertNotNull(view.getRuns());
        Assertions.assertEquals(2, view.getRuns().size());
        Assertions.assertEquals("run-2", view.getRuns().get(0).getId());
        Assertions.assertEquals("run-1", view.getRuns().get(1).getId());
    }

    @Test
    public void testAggregateRunsWidgetWithLimitFilter()
    {
        final WidgetFilterConfigDto runFilter = new WidgetFilterConfigDto(
            Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(), Collections.emptyMap(), Collections.emptyMap(),
            1, null, null
        );

        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-runs-limited", "Limited Runs", WidgetMetric.RUNS, null,
            WidgetPresentation.RUNS, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "full", 0, false, false, Collections.emptyList(), true, runFilter, null, false, false
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-1");
        r1.setStartTimeMs(1000L);
        r1.setTotalTests(10);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-2");
        r2.setStartTimeMs(2000L);
        r2.setTotalTests(10);

        final List<TestRunEntity> runs = List.of(r1, r2);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, runs, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertTrue(view.isRuns());
        Assertions.assertTrue(view.hasData());
        Assertions.assertEquals(1, view.getRuns().size());
        Assertions.assertEquals(1, view.getShowingRunsCount());
        Assertions.assertEquals("run-2", view.getRuns().get(0).getId());
    }

    @Test
    public void testResolveContributingRunIdsForRunsWidget()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-runs", "List of Runs", WidgetMetric.RUNS, null,
            WidgetPresentation.RUNS, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "full", 0
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-101");
        r1.setStartTimeMs(1000L);
        r1.setTotalTests(5);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-102");
        r2.setStartTimeMs(2000L);
        r2.setTotalTests(5);

        final Set<String> runIds = service.resolveContributingRunIds(config, List.of(r1, r2), Collections.emptyList(), null);
        Assertions.assertNotNull(runIds);
        Assertions.assertEquals(2, runIds.size());
        Assertions.assertTrue(runIds.contains("run-101"));
        Assertions.assertTrue(runIds.contains("run-102"));
    }

    @Test
    public void testTrendGraph_TagMetric_RunLevelTags_ByRunsMode()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-tag-release-runs", "Release Trend", WidgetMetric.TAG, "Release",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "full", 0, false
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-1");
        r1.setStartTimeMs(1000L);
        r1.setTotalTests(10);
        r1.setTags(List.of(new TestRunTagEntity("run-1", "Release", "2026.09")));

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-2");
        r2.setStartTimeMs(2000L);
        r2.setTotalTests(10);
        r2.setTags(List.of(new TestRunTagEntity("run-2", "Release", "2026.10")));

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, List.of(r1, r2), Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertEquals(2, view.getTrendPoints().size());

        final List<WidgetTrendLayerDto> layers = view.getTrendLayers();
        Assertions.assertEquals(2, layers.size());

        final List<String> layerNames = layers.stream().map(WidgetTrendLayerDto::getName).toList();
        Assertions.assertTrue(layerNames.contains("2026.09"), "Should contain 2026.09 layer instead of Default");
        Assertions.assertTrue(layerNames.contains("2026.10"), "Should contain 2026.10 layer instead of Default");
        Assertions.assertFalse(layerNames.contains("Default"), "Should not contain Default when run has tag");
    }

    @Test
    public void testTrendGraph_TagMetric_RunLevelTags_WithDataServiceExecutionReport()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-tag-release-execs", "Release Trend Execs", WidgetMetric.TAG, "Release",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "full", 0, false
        );

        final AuraReportDataService mockDataService = Mockito.mock(AuraReportDataService.class);

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-1");
        r1.setStartTimeMs(1000L);
        r1.setTotalTests(2);
        r1.setTags(List.of(new TestRunTagEntity("run-1", "Release", "2026.09")));

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-2");
        r2.setStartTimeMs(2000L);
        r2.setTotalTests(2);
        r2.setTags(List.of(new TestRunTagEntity("run-2", "Release", "2026.10")));

        final TestExecutionDto e1 = new TestExecutionDto(
            "exec-1", "run-1", "com.example.TestA", "testA", "testA", "PASSED", "en-US", "Chrome",
            Map.of("Area", List.of("Browsing"))
        );
        final TestExecutionDto e2 = new TestExecutionDto(
            "exec-2", "run-2", "com.example.TestB", "testB", "testB", "PASSED", "en-US", "Chrome",
            Map.of("Area", List.of("Checkout"))
        );

        final RunReportDto report1 = Mockito.mock(RunReportDto.class);
        Mockito.when(report1.getExecutions()).thenReturn(List.of(e1));

        final RunReportDto report2 = Mockito.mock(RunReportDto.class);
        Mockito.when(report2.getExecutions()).thenReturn(List.of(e2));

        Mockito.when(mockDataService.getRunReport("run-1")).thenReturn(report1);
        Mockito.when(mockDataService.getRunReport("run-2")).thenReturn(report2);
        Mockito.when(mockDataService.getRunTagValues(r1, "release")).thenReturn(List.of("2026.09"));
        Mockito.when(mockDataService.getRunTagValues(r2, "release")).thenReturn(List.of("2026.10"));

        final List<DashboardWidgetViewDto> views = service.aggregateWidgets(
            List.of(config), List.of(r1, r2), Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), null, null, Collections.emptyList(),
            mockDataService, List.of(r1, r2), "all", Collections.emptyList(), Collections.emptyMap(), Collections.emptyList()
        );

        Assertions.assertNotNull(views);
        Assertions.assertEquals(1, views.size());
        final DashboardWidgetViewDto view = views.get(0);

        final List<WidgetTrendLayerDto> layers = view.getTrendLayers();
        Assertions.assertEquals(2, layers.size());

        final List<String> layerNames = layers.stream().map(WidgetTrendLayerDto::getName).toList();
        Assertions.assertTrue(layerNames.contains("2026.09"));
        Assertions.assertTrue(layerNames.contains("2026.10"));
        Assertions.assertFalse(layerNames.contains("Default"));
    }

    @Test
    public void testTrendGraph_TagMetric_SuiteEvolution_WithRunLevelTags()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-tag-release-evo", "Release Suite Evolution", WidgetMetric.TAG, "Release",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "full", 0, false
        );

        final BatchSuiteEvolutionPointDto pt1 = new BatchSuiteEvolutionPointDto(
            "run-1", 0, "2026-10-20 10:00", 10, 10,
            10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 100.0, 0,
            Map.of("release", Map.of("2026.09", 10))
        );
        final BatchSuiteEvolutionPointDto pt2 = new BatchSuiteEvolutionPointDto(
            "run-2", 1, "2026-10-26 10:00", 20, 10,
            20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 100.0, 0,
            Map.of("release", Map.of("2026.09", 5, "2026.10", 15))
        );
        final BatchSuiteEvolutionDto suiteEvolution = new BatchSuiteEvolutionDto(List.of(pt1, pt2));

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), null, suiteEvolution, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertEquals(2, view.getTrendPoints().size());

        final List<WidgetTrendLayerDto> layers = view.getTrendLayers();
        Assertions.assertEquals(2, layers.size());

        final List<String> layerNames = layers.stream().map(WidgetTrendLayerDto::getName).toList();
        Assertions.assertTrue(layerNames.contains("2026.09"));
        Assertions.assertTrue(layerNames.contains("2026.10"));
        Assertions.assertFalse(layerNames.contains("Default"));
    }

    @Test
    public void testPieChart_TagMetric_Browser_LastRunScope_DoesNotReturnBatchWideRatios()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-pie-browser-last-run", "Browser Pie Chart (Last Run)", WidgetMetric.TAG, "browser",
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT,
            WidgetPointScope.LAST_RUN, "half", 0, false
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run_20261105_100000");
        r1.setStartTimeMs(1000L);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run_20261105_113326");
        r2.setStartTimeMs(2000L);

        final List<TestRunEntity> runs = List.of(r1, r2);

        final List<BatchLatestVariationDto> variations = new ArrayList<>();
        for (int i = 0; i < 79; i++)
        {
            variations.add(new BatchLatestVariationDto("k-ch-" + i, "passed-clean", false, "JAVA", "run_20261105_100000", "Default", "Chrome_headless", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L));
        }
        for (int i = 0; i < 14; i++)
        {
            variations.add(new BatchLatestVariationDto("k-ip-" + i, "passed-clean", false, "JAVA", "run_20261105_100000", "Default", "Chrome_iPhoneX", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L));
        }
        for (int i = 0; i < 12; i++)
        {
            variations.add(new BatchLatestVariationDto("k-ff-" + i, "passed-clean", false, "JAVA", "run_20261105_100000", "Default", "Firefox_1920x1080", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L));
        }
        for (int i = 0; i < 11; i++)
        {
            variations.add(new BatchLatestVariationDto("k-sf-" + i, "passed-clean", false, "JAVA", "run_20261105_100000", "Default", "Safari_1920x1080", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L));
        }
        variations.add(new BatchLatestVariationDto("k-c1", "passed-clean", false, "JAVA", "run_20261105_113326", "Default", "Chrome_1920x1080", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L));
        variations.add(new BatchLatestVariationDto("k-c2", "failed-unknown", false, "JAVA", "run_20261105_113326", "Default", "Chrome_1920x1080", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L));

        final List<DistributionRatioDto> browserRatios = List.of(
            new DistributionRatioDto("Chrome_headless", 79, 66.9, "#3b82f6"),
            new DistributionRatioDto("Chrome_iPhoneX", 14, 11.9, "#8b5cf6"),
            new DistributionRatioDto("Firefox_1920x1080", 12, 10.2, "#ec4899"),
            new DistributionRatioDto("Safari_1920x1080", 11, 9.3, "#f59e0b"),
            new DistributionRatioDto("Chrome_1920x1080", 2, 1.7, "#10b981")
        );

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, runs, variations, browserRatios, Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertEquals("2 Items (Run #run_20261105_113326)", view.getSummaryLabel());
        Assertions.assertEquals("2", view.getPieCenterRate());
        Assertions.assertEquals("ITEMS", view.getPieCenterLabel());
        Assertions.assertEquals(1, view.getPieSlices().size());

        final WidgetPieSliceDto slice = view.getPieSlices().get(0);
        Assertions.assertEquals("Chrome_1920x1080", slice.getLabel());
        Assertions.assertEquals(2, slice.getCount());
        Assertions.assertEquals(100.0, slice.getPercentage(), 0.01);
        Assertions.assertEquals("#10b981", slice.getColor());
    }

    @Test
    public void testRatioBar_TagMetric_Browser_LastRunScope()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-ratio-browser-last-run", "Browser Ratio Bar (Last Run)", WidgetMetric.TAG, "browser",
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT,
            WidgetPointScope.LAST_RUN, "half", 0, false
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-old");
        r1.setStartTimeMs(1000L);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-new");
        r2.setStartTimeMs(2000L);

        final List<TestRunEntity> runs = List.of(r1, r2);

        final List<BatchLatestVariationDto> variations = List.of(
            new BatchLatestVariationDto("k-old-1", "passed-clean", false, "JAVA", "run-old", "Default", "Firefox", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L),
            new BatchLatestVariationDto("k-new-1", "passed-clean", false, "JAVA", "run-new", "Default", "Chrome", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L),
            new BatchLatestVariationDto("k-new-2", "passed-clean", false, "JAVA", "run-new", "Default", "Chrome", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L)
        );

        final List<DistributionRatioDto> browserRatios = List.of(
            new DistributionRatioDto("Chrome", 2, 66.7, "#10b981"),
            new DistributionRatioDto("Firefox", 1, 33.3, "#3b82f6")
        );

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, runs, variations, browserRatios, Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertEquals("2 Items (Run #run-new)", view.getSummaryLabel());
        Assertions.assertEquals(1, view.getRatioSegments().size());
        Assertions.assertEquals("Chrome", view.getRatioSegments().get(0).getName());
        Assertions.assertEquals(2, view.getRatioSegments().get(0).getCount());
        Assertions.assertEquals(100.0, view.getRatioSegments().get(0).getPercentage(), 0.01);
    }

    @Test
    public void testListWidget_TagMetric_Browser_LastRunScope()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-list-browser-last-run", "Browser List (Last Run)", WidgetMetric.TAG, "browser",
            WidgetPresentation.LIST, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT,
            WidgetPointScope.LAST_RUN, "half", 0, false
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-old");
        r1.setStartTimeMs(1000L);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-new");
        r2.setStartTimeMs(2000L);

        final List<TestRunEntity> runs = List.of(r1, r2);

        final List<BatchLatestVariationDto> variations = List.of(
            new BatchLatestVariationDto("k-old-1", "passed-clean", false, "JAVA", "run-old", "Default", "Firefox", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L),
            new BatchLatestVariationDto("k-new-1", "passed-clean", false, "JAVA", "run-new", "Default", "Chrome", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L),
            new BatchLatestVariationDto("k-new-2", "passed-clean", false, "JAVA", "run-new", "Default", "Chrome", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L)
        );

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, runs, variations, Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertEquals(1, view.getListItems().size());
        final WidgetListItemDto item = view.getListItems().get(0);
        Assertions.assertEquals("Chrome", item.getLabel());
        Assertions.assertEquals(2, item.getCount());
        Assertions.assertEquals(2, item.getSubItems().size());
        Assertions.assertEquals("#run-new", item.getSubItems().get(0).getValueFormatted());
        Assertions.assertEquals("#run-new", item.getSubItems().get(1).getValueFormatted());
        Assertions.assertEquals("run-new", item.getSubItems().get(0).getRunId());
        Assertions.assertEquals("run-new", item.getSubItems().get(1).getRunId());
    }

    @Test
    public void testPieChart_TagMetric_Area_LastRunScope()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-pie-area-last-run", "Area Pie Chart (Last Run)", WidgetMetric.TAG, "Area",
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT,
            WidgetPointScope.LAST_RUN, "half", 0, false
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-old");
        r1.setStartTimeMs(1000L);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-new");
        r2.setStartTimeMs(2000L);

        final List<TestRunEntity> runs = List.of(r1, r2);

        final List<BatchLatestVariationDto> variations = List.of(
            new BatchLatestVariationDto("k-old-1", "passed-clean", false, "JAVA", "run-old", "Default", "Chrome", "US", "Catalog", "Default", Collections.emptyMap(), 0.0, 0L),
            new BatchLatestVariationDto("k-new-1", "passed-clean", false, "JAVA", "run-new", "Default", "Chrome", "US", "Checkout", "Default", Collections.emptyMap(), 0.0, 0L),
            new BatchLatestVariationDto("k-new-2", "passed-clean", false, "JAVA", "run-new", "Default", "Chrome", "US", "Cart", "Default", Collections.emptyMap(), 0.0, 0L)
        );

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, runs, variations, Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertEquals("2 Items (Run #run-new)", view.getSummaryLabel());
        Assertions.assertEquals(2, view.getPieSlices().size());
        final List<String> sliceLabels = view.getPieSlices().stream().map(WidgetPieSliceDto::getLabel).toList();
        Assertions.assertTrue(sliceLabels.contains("@Checkout"));
        Assertions.assertTrue(sliceLabels.contains("@Cart"));
        Assertions.assertFalse(sliceLabels.contains("@Catalog"));
    }

    @Test
    public void testTextWithSmallTrendWidget_PointScopeAlignsWithTrendMode()
    {
        // 1. By Runs: should reflect the last run (50% PASS, 2 Tests), not latest variations
        final DashboardWidgetConfigDto byRunsConfig = new DashboardWidgetConfigDto(
            "w-trend-by-runs", "Status with Trend (By Runs)", WidgetMetric.STATUS, "",
            WidgetPresentation.TEXT_WITH_SMALL_TREND, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run_20261105_100000");
        r1.setStartTimeMs(1000L);
        r1.setPassedCount(10);
        r1.setTotalTests(10);
        r1.setPassRate(100.0);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run_20261105_113326");
        r2.setStartTimeMs(2000L);
        r2.setPassedCount(1);
        r2.setFailedUnknownCount(1);
        r2.setTotalTests(2);
        r2.setPassRate(50.0);

        final List<TestRunEntity> runs = List.of(r1, r2);

        // Variations represent suite evolution across test base: 10 variations, 8 passed = 80%
        final List<BatchLatestVariationDto> variations = new ArrayList<>();
        for (int i = 1; i <= 8; i++)
        {
            variations.add(new BatchLatestVariationDto("k-" + i, "passed-clean", false, "JAVA", "run_20261105_100000", "Default", "Chrome", "US", "Catalog", "Default", Collections.emptyMap(), 0.0, 0L));
        }
        for (int i = 9; i <= 10; i++)
        {
            variations.add(new BatchLatestVariationDto("k-" + i, "failed-unknown", false, "JAVA", "run_20261105_100000", "Default", "Chrome", "US", "Catalog", "Default", Collections.emptyMap(), 0.0, 0L));
        }

        final DashboardWidgetViewDto byRunsView = service.aggregateWidget(
            byRunsConfig, runs, variations, Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(byRunsView);
        Assertions.assertEquals("50% PASS", byRunsView.getPrimaryTextValue());
        Assertions.assertEquals("2 Tests (Run #run_20261105_113326)", byRunsView.getSecondaryTextValue());
        Assertions.assertEquals(2, byRunsView.getTrendPoints().size());
        Assertions.assertEquals(50.0, byRunsView.getTrendPoints().get(1).getValue());

        // 2. Latest Approach: should reflect latest test variations (80% PASS, 10 Tests)
        final DashboardWidgetConfigDto latestApproachConfig = new DashboardWidgetConfigDto(
            "w-trend-latest-approach", "Status with Trend (Latest Approach)", WidgetMetric.STATUS, "",
            WidgetPresentation.TEXT_WITH_SMALL_TREND, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );

        final DashboardWidgetViewDto latestView = service.aggregateWidget(
            latestApproachConfig, runs, variations, Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(latestView);
        Assertions.assertEquals("80% PASS", latestView.getPrimaryTextValue());
        Assertions.assertEquals("10 Tests", latestView.getSecondaryTextValue());
    }

    @Test
    public void testNumberOfRuns_PieChart_Aggregation()
    {
        final DashboardWidgetConfigDto absConfig = new DashboardWidgetConfigDto(
            "w-runs-pie-abs", "Number of Runs Distribution", WidgetMetric.NUMBER_OF_RUNS, "",
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-1");
        r1.setStartTimeMs(1000L);
        r1.setPassedCount(5);
        r1.setTotalTests(5);
        r1.setPassRate(100.0);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-2");
        r2.setStartTimeMs(2000L);
        r2.setPassedCount(3);
        r2.setFailedKnownCount(2);
        r2.setTotalTests(5);
        r2.setPassRate(60.0);

        final TestRunEntity r3 = new TestRunEntity();
        r3.setId("run-3");
        r3.setStartTimeMs(3000L);
        r3.setPassedCount(4);
        r3.setFailedUnknownCount(1);
        r3.setTotalTests(5);
        r3.setPassRate(80.0);

        final TestRunEntity r4 = new TestRunEntity();
        r4.setId("run-4");
        r4.setStartTimeMs(4000L);
        r4.setIgnoredCount(2);
        r4.setTotalTests(2);
        r4.setPassRate(0.0);

        final List<TestRunEntity> runs = List.of(r1, r2, r3, r4);

        final DashboardWidgetViewDto absView = service.aggregateWidget(
            absConfig, runs, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(absView);
        Assertions.assertEquals("4 Runs", absView.getSummaryLabel());
        Assertions.assertEquals("4", absView.getPieCenterRate());
        Assertions.assertEquals("RUNS", absView.getPieCenterLabel());
        Assertions.assertEquals(4, absView.getPieSlices().size());
        Assertions.assertEquals("Passed Runs", absView.getPieSlices().get(0).getLabel());
        Assertions.assertEquals(1, absView.getPieSlices().get(0).getCount());
        Assertions.assertEquals(25.0, absView.getPieSlices().get(0).getPercentage());
        Assertions.assertEquals("Failed Known Runs", absView.getPieSlices().get(1).getLabel());
        Assertions.assertEquals(1, absView.getPieSlices().get(1).getCount());
        Assertions.assertEquals("Failed Unknown Runs", absView.getPieSlices().get(2).getLabel());
        Assertions.assertEquals(1, absView.getPieSlices().get(2).getCount());
        Assertions.assertEquals("Ignored / Empty Runs", absView.getPieSlices().get(3).getLabel());
        Assertions.assertEquals(1, absView.getPieSlices().get(3).getCount());

        // Also test PERCENT value format
        final DashboardWidgetConfigDto pctConfig = new DashboardWidgetConfigDto(
            "w-runs-pie-pct", "Number of Runs Distribution", WidgetMetric.NUMBER_OF_RUNS, "",
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );
        final DashboardWidgetViewDto pctView = service.aggregateWidget(
            pctConfig, runs, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );
        Assertions.assertEquals("25%", pctView.getPieCenterRate());
        Assertions.assertEquals("RUN PASS", pctView.getPieCenterLabel());
    }

    @Test
    public void testNumberOfRuns_RatioBar_Aggregation()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-runs-bar", "Number of Runs Ratio Bar", WidgetMetric.NUMBER_OF_RUNS, "",
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-1");
        r1.setStartTimeMs(1000L);
        r1.setPassedCount(5);
        r1.setTotalTests(5);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-2");
        r2.setStartTimeMs(2000L);
        r2.setFailedUnknownCount(1);
        r2.setTotalTests(1);

        final List<TestRunEntity> runs = List.of(r1, r2);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            config, runs, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertEquals(2, view.getRatioSegments().size());
        Assertions.assertEquals("Passed Runs", view.getRatioSegments().get(0).getName());
        Assertions.assertEquals(1, view.getRatioSegments().get(0).getCount());
        Assertions.assertEquals(50.0, view.getRatioSegments().get(0).getPercentage());
        Assertions.assertEquals("Failed Unknown Runs", view.getRatioSegments().get(1).getName());
        Assertions.assertEquals(1, view.getRatioSegments().get(1).getCount());
        Assertions.assertEquals(50.0, view.getRatioSegments().get(1).getPercentage());
    }

    @Test
    public void testNumberOfRuns_Perlenkette_ByRuns()
    {
        final DashboardWidgetConfigDto byRunsConfig = new DashboardWidgetConfigDto(
            "w-runs-pk-runs", "Number of Runs String of Pearls", WidgetMetric.NUMBER_OF_RUNS, "",
            WidgetPresentation.PERLENKETTE, WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-1");
        r1.setStartTimeMs(1000L);
        r1.setPassedCount(10);
        r1.setTotalTests(10);
        r1.setPassRate(100.0);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-2");
        r2.setStartTimeMs(2000L);
        r2.setPassedCount(2);
        r2.setFailedUnknownCount(2);
        r2.setTotalTests(4);
        r2.setPassRate(50.0);

        final List<TestRunEntity> runs = List.of(r1, r2);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            byRunsConfig, runs, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertNotNull(view.getPerlenkette());
        Assertions.assertEquals(2, view.getPerlenkette().getBeads().size());
        final PerlenketteBeadDto b1 = view.getPerlenkette().getBeads().get(0);
        final PerlenketteBeadDto b2 = view.getPerlenkette().getBeads().get(1);
        Assertions.assertEquals("Run #1", b1.getValueFormatted());
        Assertions.assertEquals("Run #2", b2.getValueFormatted());
        Assertions.assertEquals("100.0%", b1.getDetails().get("Run Pass Rate"));
        Assertions.assertEquals("50.0%", b2.getDetails().get("Run Pass Rate"));
    }

    @Test
    public void testNumberOfRuns_TrendGraph_Aggregation()
    {
        final DashboardWidgetConfigDto trendConfig = new DashboardWidgetConfigDto(
            "w-runs-trend", "Number of Runs Trend Graph", WidgetMetric.NUMBER_OF_RUNS, "",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-1");
        r1.setStartTimeMs(1000L);
        r1.setPassedCount(5);
        r1.setTotalTests(5);
        r1.setPassRate(100.0);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-2");
        r2.setStartTimeMs(2000L);
        r2.setPassedCount(5);
        r2.setTotalTests(5);
        r2.setPassRate(100.0);

        final TestRunEntity r3 = new TestRunEntity();
        r3.setId("run-3");
        r3.setStartTimeMs(3000L);
        r3.setPassedCount(5);
        r3.setTotalTests(5);
        r3.setPassRate(100.0);

        final List<TestRunEntity> runs = List.of(r1, r2, r3);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            trendConfig, runs, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertEquals(3, view.getTrendPoints().size());
        Assertions.assertEquals(1.0, view.getTrendPoints().get(0).getValue());
        Assertions.assertEquals(2.0, view.getTrendPoints().get(1).getValue());
        Assertions.assertEquals(3.0, view.getTrendPoints().get(2).getValue());
        Assertions.assertEquals("+2 runs vs start", view.getTrendDeltaLabel());
        Assertions.assertFalse(view.getTrendLayers().isEmpty());
        Assertions.assertEquals("Total Runs", view.getTrendLayers().get(0).getName());
        Assertions.assertEquals(3, view.getTrendLayers().get(0).getCount());
    }

    @Test
    public void testNumberOfRuns_TextAndSmallTrend_Aggregation()
    {
        final DashboardWidgetConfigDto textAbsConfig = new DashboardWidgetConfigDto(
            "w-runs-text-abs", "Number of Runs KPI", WidgetMetric.NUMBER_OF_RUNS, "",
            WidgetPresentation.TEXT, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );

        final DashboardWidgetConfigDto textPctConfig = new DashboardWidgetConfigDto(
            "w-runs-text-pct", "Number of Runs KPI", WidgetMetric.NUMBER_OF_RUNS, "",
            WidgetPresentation.TEXT, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-1");
        r1.setStartTimeMs(1000L);
        r1.setPassedCount(5);
        r1.setTotalTests(5);
        r1.setPassRate(100.0);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-2");
        r2.setStartTimeMs(2000L);
        r2.setPassedCount(2);
        r2.setFailedUnknownCount(2);
        r2.setTotalTests(4);
        r2.setPassRate(50.0);

        final List<TestRunEntity> runs = List.of(r1, r2);

        final DashboardWidgetViewDto absView = service.aggregateWidget(
            textAbsConfig, runs, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );
        Assertions.assertEquals("2 Runs", absView.getPrimaryTextValue());
        Assertions.assertEquals("2 Runs", absView.getSecondaryTextValue());

        final DashboardWidgetViewDto pctView = service.aggregateWidget(
            textPctConfig, runs, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );
        Assertions.assertEquals("50% RUN PASS", pctView.getPrimaryTextValue());
        Assertions.assertEquals("1 of 2 Runs Passed", pctView.getSecondaryTextValue());

        final DashboardWidgetConfigDto smallTrendConfig = new DashboardWidgetConfigDto(
            "w-runs-small-trend", "Number of Runs with Trend", WidgetMetric.NUMBER_OF_RUNS, "",
            WidgetPresentation.TEXT_WITH_SMALL_TREND, WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );
        final DashboardWidgetViewDto smallTrendView = service.aggregateWidget(
            smallTrendConfig, runs, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );
        Assertions.assertEquals("1 Run", smallTrendView.getPrimaryTextValue());
        Assertions.assertEquals(2, smallTrendView.getTrendPoints().size());

        final DashboardWidgetConfigDto smallTrendLatestConfig = new DashboardWidgetConfigDto(
            "w-runs-small-trend-latest", "Number of Runs with Trend (Latest Approach)", WidgetMetric.NUMBER_OF_RUNS, "",
            WidgetPresentation.TEXT_WITH_SMALL_TREND, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );
        final DashboardWidgetViewDto smallTrendLatestView = service.aggregateWidget(
            smallTrendLatestConfig, runs, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );
        Assertions.assertEquals("2 Runs", smallTrendLatestView.getPrimaryTextValue());
    }

    @Test
    public void testNumberOfRuns_List_Aggregation()
    {
        final DashboardWidgetConfigDto listConfig = new DashboardWidgetConfigDto(
            "w-runs-list", "Number of Runs Breakdown List", WidgetMetric.NUMBER_OF_RUNS, "",
            WidgetPresentation.LIST, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-101");
        r1.setStartTimeMs(1000L);
        r1.setPassedCount(5);
        r1.setTotalTests(5);
        r1.setPassRate(100.0);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-102");
        r2.setStartTimeMs(2000L);
        r2.setFailedUnknownCount(3);
        r2.setTotalTests(3);
        r2.setPassRate(0.0);

        final List<TestRunEntity> runs = List.of(r1, r2);

        final DashboardWidgetViewDto view = service.aggregateWidget(
            listConfig, runs, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );

        Assertions.assertNotNull(view);
        Assertions.assertEquals(2, view.getListItems().size());
        final WidgetListItemDto passedGroup = view.getListItems().stream()
            .filter(i -> "Passed Runs".equalsIgnoreCase(i.getLabel()))
            .findFirst()
            .orElse(null);
        Assertions.assertNotNull(passedGroup);
        Assertions.assertEquals(1, passedGroup.getCount());
        Assertions.assertEquals(1, passedGroup.getSubItems().size());
        Assertions.assertEquals("Run #run-101", passedGroup.getSubItems().get(0).getLabel());

        final WidgetListItemDto failedUnknownGroup = view.getListItems().stream()
            .filter(i -> "Failed Unknown Runs".equalsIgnoreCase(i.getLabel()))
            .findFirst()
            .orElse(null);
        Assertions.assertNotNull(failedUnknownGroup);
        Assertions.assertEquals(1, failedUnknownGroup.getCount());
        Assertions.assertEquals(1, failedUnknownGroup.getSubItems().size());
        Assertions.assertEquals("Run #run-102", failedUnknownGroup.getSubItems().get(0).getLabel());
    }

    @Test
    public void testNumberOfRuns_ContributingRunsAndContainerCompatibility()
    {
        final DashboardWidgetConfigDto c1 = new DashboardWidgetConfigDto(
            "c1", "Runs Pie", WidgetMetric.NUMBER_OF_RUNS, "",
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false
        );
        final DashboardWidgetConfigDto c2 = new DashboardWidgetConfigDto(
            "c2", "Runs Bar", WidgetMetric.NUMBER_OF_RUNS, "",
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "half", 1, false
        );

        final TestRunEntity r1 = new TestRunEntity();
        r1.setId("run-a");
        r1.setStartTimeMs(1000L);

        final TestRunEntity r2 = new TestRunEntity();
        r2.setId("run-b");
        r2.setStartTimeMs(2000L);

        final List<TestRunEntity> runs = List.of(r1, r2);

        final Set<String> contribC1 = service.resolveContributingRunIds(c1, runs, Collections.emptyList());
        final Set<String> contribC2 = service.resolveContributingRunIds(c2, runs, Collections.emptyList());

        Assertions.assertEquals(Set.of("run-a", "run-b"), contribC1);
        Assertions.assertEquals(Set.of("run-a", "run-b"), contribC2);

        Assertions.assertTrue(service.areBasedOnSameContributingRuns(c1, c2, runs, Collections.emptyList()));

        final DashboardWidgetConfigDto containerConfig = DashboardWidgetConfigDto.createContainer(c1, c2);
        final DashboardWidgetViewDto containerView = service.aggregateWidget(
            containerConfig, runs, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null, Collections.emptyList()
        );
        Assertions.assertNotNull(containerView);
        Assertions.assertEquals(2, containerView.getChildViews().size());
    }
}



