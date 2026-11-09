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
package com.xceptance.aura.report.dto.widget;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DashboardWidgetConfigDto}, verifying container support,
 * children manipulation, and JSON serialization/deserialization.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public class DashboardWidgetConfigDtoTest
{
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testCreateContainerFromTwoWidgets()
    {
        final DashboardWidgetConfigDto widget1 = new DashboardWidgetConfigDto(
            "w1", "Status Donut", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0, false, false, Collections.emptyList(), true, null
        );

        final DashboardWidgetConfigDto widget2 = new DashboardWidgetConfigDto(
            "w2", "Failure List", WidgetMetric.STATUS, null,
            WidgetPresentation.LIST, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 1, false, false, Collections.emptyList(), true, null
        );

        final DashboardWidgetConfigDto container = DashboardWidgetConfigDto.createContainer(widget1, widget2);

        Assertions.assertNotNull(container);
        Assertions.assertTrue(container.isContainer());
        Assertions.assertTrue(container.hasChildren());
        Assertions.assertEquals(WidgetPresentation.CONTAINER, container.getPresentation());
        Assertions.assertEquals("full", container.getWidth());
        Assertions.assertEquals(2, container.getChildren().size());
        Assertions.assertEquals("w1", container.getChildren().get(0).getId());
        Assertions.assertEquals("w2", container.getChildren().get(1).getId());
    }

    @Test
    public void testAddAndRemoveChild()
    {
        final DashboardWidgetConfigDto widget1 = new DashboardWidgetConfigDto(
            "w1", "Widget 1", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0, false, false, Collections.emptyList(), true, null
        );

        final DashboardWidgetConfigDto widget2 = new DashboardWidgetConfigDto(
            "w2", "Widget 2", WidgetMetric.STATUS, null,
            WidgetPresentation.LIST, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 1, false, false, Collections.emptyList(), true, null
        );

        final DashboardWidgetConfigDto widget3 = new DashboardWidgetConfigDto(
            "w3", "Widget 3", WidgetMetric.STATUS, null,
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 2, false, false, Collections.emptyList(), true, null
        );

        final DashboardWidgetConfigDto container = DashboardWidgetConfigDto.createContainer(widget1, widget2);
        Assertions.assertEquals(2, container.getChildren().size());

        final DashboardWidgetConfigDto updatedContainer = container.addChild(widget3);
        Assertions.assertEquals(3, updatedContainer.getChildren().size());
        Assertions.assertEquals("w3", updatedContainer.getChildren().get(2).getId());

        final DashboardWidgetConfigDto afterRemoval = updatedContainer.removeChild("w2");
        Assertions.assertEquals(2, afterRemoval.getChildren().size());
        Assertions.assertEquals("w1", afterRemoval.getChildren().get(0).getId());
        Assertions.assertEquals("w3", afterRemoval.getChildren().get(1).getId());
    }

    @Test
    public void testJsonSerializationAndDeserialization() throws Exception
    {
        final DashboardWidgetConfigDto child1 = new DashboardWidgetConfigDto(
            "child-1", "Donut", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0, false, false, Collections.emptyList(), true, null
        );

        final DashboardWidgetConfigDto child2 = new DashboardWidgetConfigDto(
            "child-2", "List", WidgetMetric.STATUS, null,
            WidgetPresentation.LIST, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 1, false, false, Collections.emptyList(), true, null
        );

        final DashboardWidgetConfigDto container = DashboardWidgetConfigDto.createContainer(child1, child2);

        final String json = objectMapper.writeValueAsString(container);
        Assertions.assertTrue(json.contains("\"presentation\":\"CONTAINER\""));
        Assertions.assertTrue(json.contains("\"children\":["));
        Assertions.assertTrue(json.contains("\"child-1\""));
        Assertions.assertTrue(json.contains("\"child-2\""));

        final DashboardWidgetConfigDto deserialized = objectMapper.readValue(json, DashboardWidgetConfigDto.class);
        Assertions.assertNotNull(deserialized);
        Assertions.assertTrue(deserialized.isContainer());
        Assertions.assertTrue(deserialized.hasChildren());
        Assertions.assertEquals(2, deserialized.getChildren().size());
        Assertions.assertEquals("child-1", deserialized.getChildren().get(0).getId());
        Assertions.assertEquals("child-2", deserialized.getChildren().get(1).getId());
    }

    @Test
    public void testCreateContainerPreservesCustomExecutionIdentity()
    {
        final DashboardWidgetConfigDto child1 = new DashboardWidgetConfigDto(
            "child-1", "Status Donut", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0, false, true, List.of("environment"), false, null
        );

        final DashboardWidgetConfigDto child2 = new DashboardWidgetConfigDto(
            "child-2", "Browser Ratio", WidgetMetric.TAG, "browser",
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 1, false, true, List.of("environment"), false, null
        );

        final DashboardWidgetConfigDto container = DashboardWidgetConfigDto.createContainer(child1, child2);

        Assertions.assertNotNull(container);
        Assertions.assertTrue(container.isContainer());
        Assertions.assertTrue(container.hasOwnExecutionIdentity(), "Container must inherit custom execution identity from children");
        Assertions.assertEquals(List.of("environment"), container.getExecutionIdentityDimensions());
    }

    @Test
    public void testAddChildPreservesCustomExecutionIdentity()
    {
        final DashboardWidgetConfigDto child1 = new DashboardWidgetConfigDto(
            "child-1", "Status Donut", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0, false, true, List.of("environment"), false, null
        );

        final DashboardWidgetConfigDto child2 = new DashboardWidgetConfigDto(
            "child-2", "Browser Ratio", WidgetMetric.TAG, "browser",
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 1, false, true, List.of("environment"), false, null
        );

        final DashboardWidgetConfigDto child3 = new DashboardWidgetConfigDto(
            "child-3", "Area Ratio", WidgetMetric.TAG, "area",
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 2, false, false, Collections.emptyList(), false, null
        );

        final DashboardWidgetConfigDto container = DashboardWidgetConfigDto.createContainer(child1, child2);
        Assertions.assertTrue(container.hasOwnExecutionIdentity());

        final DashboardWidgetConfigDto updatedContainer = container.addChild(child3);
        Assertions.assertTrue(updatedContainer.hasOwnExecutionIdentity(), "Adding a child must preserve the container's custom execution identity");
        Assertions.assertEquals(List.of("environment"), updatedContainer.getExecutionIdentityDimensions());
        Assertions.assertEquals(3, updatedContainer.getChildren().size());
    }

    @Test
    public void testNonTagMetricsClearTagKey()
    {
        final DashboardWidgetConfigDto statusWidget = new DashboardWidgetConfigDto(
            "w1", "Status Pie", WidgetMetric.STATUS, "Area",
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0, false, false, Collections.emptyList(), false, null
        );
        Assertions.assertEquals("", statusWidget.getTagKey(), "Status widget must clear tagKey");

        final DashboardWidgetConfigDto bugsWidget = new DashboardWidgetConfigDto(
            "w2", "Bugs List", WidgetMetric.BUGS, "Area",
            WidgetPresentation.LIST, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 1, false, false, Collections.emptyList(), false, null
        );
        Assertions.assertEquals("", bugsWidget.getTagKey(), "Bugs widget must clear tagKey");

        final DashboardWidgetConfigDto tagWidget = new DashboardWidgetConfigDto(
            "w3", "Area Ratio", WidgetMetric.TAG, "Area",
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 2, false, false, Collections.emptyList(), false, null
        );
        Assertions.assertEquals("Area", tagWidget.getTagKey(), "TAG metric widget must preserve tagKey");
    }

    @Test
    public void testAllowPerlenketteForTagMetric()
    {
        final DashboardWidgetConfigDto tagPerlenketteWidget = new DashboardWidgetConfigDto(
            "w-tag-perlenkette", "Browser String of Pearls", WidgetMetric.TAG, "Browser",
            WidgetPresentation.PERLENKETTE, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0, false, false, Collections.emptyList(), false, null
        );

        Assertions.assertEquals(WidgetMetric.TAG, tagPerlenketteWidget.getMetric());
        Assertions.assertEquals(WidgetPresentation.PERLENKETTE, tagPerlenketteWidget.getPresentation(),
            "TAG metric widget with PERLENKETTE presentation must be preserved and supported");
    }

    @Test
    public void testGenerateDefaultTitlePresentationAwareness()
    {
        // Status presentation variants
        Assertions.assertEquals("Status Pie Chart",
            DashboardWidgetConfigDto.generateDefaultTitle(WidgetMetric.STATUS, null, WidgetPresentation.PIE_CHART, null, WidgetPointScope.LATEST_TEST_STATUS));
        Assertions.assertEquals("Status Ratio Bar",
            DashboardWidgetConfigDto.generateDefaultTitle(WidgetMetric.STATUS, null, WidgetPresentation.RATIO_BAR, null, WidgetPointScope.LATEST_TEST_STATUS));
        Assertions.assertEquals("Status String of Pearls (By Runs)",
            DashboardWidgetConfigDto.generateDefaultTitle(WidgetMetric.STATUS, null, WidgetPresentation.PERLENKETTE, WidgetTrendMode.BY_RUNS, WidgetPointScope.LATEST_TEST_STATUS));
        Assertions.assertEquals("Status Trend Graph (Daily)",
            DashboardWidgetConfigDto.generateDefaultTitle(WidgetMetric.STATUS, null, WidgetPresentation.TREND_GRAPH, WidgetTrendMode.DAILY, WidgetPointScope.LATEST_TEST_STATUS));
        Assertions.assertEquals("Status KPI",
            DashboardWidgetConfigDto.generateDefaultTitle(WidgetMetric.STATUS, null, WidgetPresentation.TEXT, null, WidgetPointScope.LATEST_TEST_STATUS));
        Assertions.assertEquals("Status with Trend",
            DashboardWidgetConfigDto.generateDefaultTitle(WidgetMetric.STATUS, null, WidgetPresentation.TEXT_WITH_SMALL_TREND, null, WidgetPointScope.LATEST_TEST_STATUS));
        Assertions.assertEquals("Status Breakdown List",
            DashboardWidgetConfigDto.generateDefaultTitle(WidgetMetric.STATUS, null, WidgetPresentation.LIST, null, WidgetPointScope.LATEST_TEST_STATUS));

        // Cost presentation variants
        Assertions.assertEquals("Cost Pie Chart",
            DashboardWidgetConfigDto.generateDefaultTitle(WidgetMetric.COSTS, null, WidgetPresentation.PIE_CHART, null, WidgetPointScope.LATEST_TEST_STATUS));
        Assertions.assertEquals("Cost Trend Graph (By Runs)",
            DashboardWidgetConfigDto.generateDefaultTitle(WidgetMetric.COSTS, null, WidgetPresentation.TREND_GRAPH, WidgetTrendMode.BY_RUNS, WidgetPointScope.LATEST_TEST_STATUS));
        Assertions.assertEquals("Cost KPI",
            DashboardWidgetConfigDto.generateDefaultTitle(WidgetMetric.COSTS, null, WidgetPresentation.TEXT, null, WidgetPointScope.LATEST_TEST_STATUS));

        // Bugs presentation variants
        Assertions.assertEquals("Bug Severity Ratio Bar",
            DashboardWidgetConfigDto.generateDefaultTitle(WidgetMetric.BUGS, null, WidgetPresentation.RATIO_BAR, null, WidgetPointScope.LATEST_TEST_STATUS));
        Assertions.assertEquals("Active Bugs KPI",
            DashboardWidgetConfigDto.generateDefaultTitle(WidgetMetric.BUGS, null, WidgetPresentation.TEXT, null, WidgetPointScope.LATEST_TEST_STATUS));
        Assertions.assertEquals("Bug Breakdown List",
            DashboardWidgetConfigDto.generateDefaultTitle(WidgetMetric.BUGS, null, WidgetPresentation.LIST, null, WidgetPointScope.LATEST_TEST_STATUS));

        // Tag dimension presentation variants
        Assertions.assertEquals("Browser Ratio Bar",
            DashboardWidgetConfigDto.generateDefaultTitle(WidgetMetric.TAG, "Browser", WidgetPresentation.RATIO_BAR, null, WidgetPointScope.LATEST_TEST_STATUS));
        Assertions.assertEquals("Browser String of Pearls",
            DashboardWidgetConfigDto.generateDefaultTitle(WidgetMetric.TAG, "Browser", WidgetPresentation.PERLENKETTE, null, WidgetPointScope.LATEST_TEST_STATUS));
        Assertions.assertEquals("Browser String of Pearls (Last Run)",
            DashboardWidgetConfigDto.generateDefaultTitle(WidgetMetric.TAG, "Browser", WidgetPresentation.PERLENKETTE, null, WidgetPointScope.LAST_RUN));
        Assertions.assertEquals("Locale Trend Graph (Monthly)",
            DashboardWidgetConfigDto.generateDefaultTitle(WidgetMetric.TAG, "Locale", WidgetPresentation.TREND_GRAPH, WidgetTrendMode.MONTHLY, WidgetPointScope.LATEST_TEST_STATUS));
        Assertions.assertEquals("Area Pie Chart (Last Run)",
            DashboardWidgetConfigDto.generateDefaultTitle(WidgetMetric.TAG, "Area", WidgetPresentation.PIE_CHART, null, WidgetPointScope.LAST_RUN));

        // AI Usage and Flakiness
        Assertions.assertEquals("AI Usage Pie Chart",
            DashboardWidgetConfigDto.generateDefaultTitle(WidgetMetric.AI_USAGE, null, WidgetPresentation.PIE_CHART, null, WidgetPointScope.LATEST_TEST_STATUS));
        Assertions.assertEquals("AI Usage KPI",
            DashboardWidgetConfigDto.generateDefaultTitle(WidgetMetric.AI_USAGE, null, WidgetPresentation.TEXT, null, WidgetPointScope.LATEST_TEST_STATUS));
        Assertions.assertEquals("Flakiness KPI",
            DashboardWidgetConfigDto.generateDefaultTitle(WidgetMetric.FLAKINESS, null, WidgetPresentation.TEXT, null, WidgetPointScope.LATEST_TEST_STATUS));

        // Variations
        Assertions.assertEquals("Known Test Base Variations",
            DashboardWidgetConfigDto.generateDefaultTitle(WidgetMetric.TEST_VARIATIONS, null, WidgetPresentation.TEST_VARIATIONS, null, WidgetPointScope.LATEST_TEST_STATUS));
    }

    @Test
    public void testCloneWithNewIdForSingleWidget()
    {
        final DashboardWidgetConfigDto original = new DashboardWidgetConfigDto(
            "w-original", "Original Status", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 2, true, true, List.of("browser", "locale"), true, null
        );

        final DashboardWidgetConfigDto cloned = original.cloneWithNewId();

        Assertions.assertNotNull(cloned);
        Assertions.assertNotEquals(original.getId(), cloned.getId());
        Assertions.assertTrue(cloned.getId().startsWith("widget-"));
        Assertions.assertEquals(original.getTitle(), cloned.getTitle());
        Assertions.assertEquals(original.getMetric(), cloned.getMetric());
        Assertions.assertEquals(original.getPresentation(), cloned.getPresentation());
        Assertions.assertEquals(original.getTrendMode(), cloned.getTrendMode());
        Assertions.assertEquals(original.getValueFormat(), cloned.getValueFormat());
        Assertions.assertEquals(original.getPointScope(), cloned.getPointScope());
        Assertions.assertEquals(original.getWidth(), cloned.getWidth());
        Assertions.assertEquals(original.getSortOrder(), cloned.getSortOrder());
        Assertions.assertTrue(cloned.isShowHealedAi());
        Assertions.assertTrue(cloned.isOverrideExecutionIdentity());
        Assertions.assertEquals(List.of("browser", "locale"), cloned.getExecutionIdentityDimensions());
        Assertions.assertTrue(cloned.isOverrideRunFilters());
    }

    @Test
    public void testCloneWithNewIdForContainerWidget()
    {
        final DashboardWidgetConfigDto child1 = new DashboardWidgetConfigDto(
            "child-1", "Child 1", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0
        );
        final DashboardWidgetConfigDto child2 = new DashboardWidgetConfigDto(
            "child-2", "Child 2", WidgetMetric.COSTS, null,
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.ABSOLUTE, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 1
        );
        final DashboardWidgetConfigDto container = DashboardWidgetConfigDto.createContainer(child1, child2);

        final DashboardWidgetConfigDto clonedContainer = container.cloneWithNewId();

        Assertions.assertNotNull(clonedContainer);
        Assertions.assertTrue(clonedContainer.isContainer());
        Assertions.assertNotEquals(container.getId(), clonedContainer.getId());
        Assertions.assertTrue(clonedContainer.getId().startsWith("container-"));
        Assertions.assertEquals(2, clonedContainer.getChildren().size());

        final DashboardWidgetConfigDto clonedChild1 = clonedContainer.getChildren().get(0);
        final DashboardWidgetConfigDto clonedChild2 = clonedContainer.getChildren().get(1);

        Assertions.assertNotEquals("child-1", clonedChild1.getId());
        Assertions.assertNotEquals("child-2", clonedChild2.getId());
        Assertions.assertNotEquals(clonedChild1.getId(), clonedChild2.getId());
        Assertions.assertEquals("Child 1", clonedChild1.getTitle());
        Assertions.assertEquals("Child 2", clonedChild2.getTitle());
    }

    @Test
    public void testRunsWidgetDefaultsAndAliases()
    {
        // Aliases
        Assertions.assertEquals(WidgetMetric.RUNS, WidgetMetric.fromString("RUNS"));
        Assertions.assertEquals(WidgetMetric.RUNS, WidgetMetric.fromString("runs"));
        Assertions.assertEquals(WidgetMetric.RUNS, WidgetMetric.fromString("RUN_HISTORY"));
        Assertions.assertEquals(WidgetMetric.RUNS, WidgetMetric.fromString("RUNS_LIST"));
        Assertions.assertEquals(WidgetMetric.RUNS, WidgetMetric.fromString("LIST_OF_RUNS"));

        Assertions.assertEquals(WidgetPresentation.RUNS, WidgetPresentation.fromString("RUNS"));
        Assertions.assertEquals(WidgetPresentation.RUNS, WidgetPresentation.fromString("runs"));
        Assertions.assertEquals(WidgetPresentation.RUNS, WidgetPresentation.fromString("RUN_HISTORY"));
        Assertions.assertEquals(WidgetPresentation.RUNS, WidgetPresentation.fromString("RUNS_LIST"));
        Assertions.assertEquals(WidgetPresentation.RUNS, WidgetPresentation.fromString("LIST_OF_RUNS"));

        // Defaults
        Assertions.assertEquals("List of Runs",
            DashboardWidgetConfigDto.generateDefaultTitle(WidgetMetric.RUNS, null, WidgetPresentation.RUNS, null, null));
        final DashboardWidgetConfigDto w = new DashboardWidgetConfigDto(
            "w", null, WidgetMetric.RUNS, null, WidgetPresentation.RUNS, null, null, null, null, 0
        );
        Assertions.assertEquals("full", w.getWidth());
    }

    @Test
    public void testRunsWidgetJsonSerializationAndDeserialization() throws Exception
    {
        final DashboardWidgetConfigDto runsWidget = new DashboardWidgetConfigDto(
            "w-runs", "List of Runs", WidgetMetric.RUNS, null,
            WidgetPresentation.RUNS, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "full", 0, false, false, Collections.emptyList(), false, null
        );

        final String json = objectMapper.writeValueAsString(runsWidget);
        Assertions.assertTrue(json.contains("\"metric\":\"RUNS\""));
        Assertions.assertTrue(json.contains("\"presentation\":\"RUNS\""));
        Assertions.assertTrue(json.contains("\"width\":\"full\""));

        final DashboardWidgetConfigDto deserialized = objectMapper.readValue(json, DashboardWidgetConfigDto.class);
        Assertions.assertNotNull(deserialized);
        Assertions.assertEquals("w-runs", deserialized.getId());
        Assertions.assertEquals(WidgetMetric.RUNS, deserialized.getMetric());
        Assertions.assertEquals(WidgetPresentation.RUNS, deserialized.getPresentation());
        Assertions.assertEquals("full", deserialized.getWidth());
    }

    @Test
    public void testRunsWidgetCloning()
    {
        final DashboardWidgetConfigDto runsWidget = new DashboardWidgetConfigDto(
            "w-runs-orig", "List of Runs", WidgetMetric.RUNS, null,
            WidgetPresentation.RUNS, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "full", 0, false, false, Collections.emptyList(), false, null
        );

        final DashboardWidgetConfigDto cloned = runsWidget.cloneWithNewId();
        Assertions.assertNotNull(cloned);
        Assertions.assertNotEquals("w-runs-orig", cloned.getId());
        Assertions.assertTrue(cloned.getId().startsWith("widget-"));
        Assertions.assertEquals(WidgetMetric.RUNS, cloned.getMetric());
        Assertions.assertEquals(WidgetPresentation.RUNS, cloned.getPresentation());
        Assertions.assertEquals("full", cloned.getWidth());
    }

    @Test
    public void testRunsWidgetProhibitsNonFullWidth()
    {
        final DashboardWidgetConfigDto quarterRuns = new DashboardWidgetConfigDto(
            "w-quarter", "Quarter Runs", WidgetMetric.RUNS, null,
            WidgetPresentation.RUNS, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "quarter", 0
        );
        Assertions.assertEquals("full", quarterRuns.getWidth(), "RUNS widget must normalize 'quarter' width to 'full'");
        Assertions.assertTrue(quarterRuns.isFullWidth(), "RUNS widget must be full width");
        Assertions.assertFalse(quarterRuns.isQuarterWidth(), "RUNS widget must not be quarter width");
        Assertions.assertFalse(quarterRuns.isHalfWidth(), "RUNS widget must not be half width");

        final DashboardWidgetConfigDto halfRuns = new DashboardWidgetConfigDto(
            "w-half", "Half Runs", WidgetMetric.RUNS, null,
            WidgetPresentation.RUNS, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0
        );
        Assertions.assertEquals("full", halfRuns.getWidth(), "RUNS widget must normalize 'half' width to 'full'");
        Assertions.assertTrue(halfRuns.isFullWidth(), "RUNS widget must be full width");
        Assertions.assertFalse(halfRuns.isQuarterWidth(), "RUNS widget must not be quarter width");
        Assertions.assertFalse(halfRuns.isHalfWidth(), "RUNS widget must not be half width");
    }

    @Test
    public void testRunsWidgetProhibitsCustomExecutionIdentity()
    {
        final DashboardWidgetConfigDto runsWidget = new DashboardWidgetConfigDto(
            "w-runs-identity", "Identity Runs", WidgetMetric.RUNS, null,
            WidgetPresentation.RUNS, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "full", 0, false, true, List.of("browser", "locale"), false, null
        );

        Assertions.assertFalse(runsWidget.isOverrideExecutionIdentity(), "RUNS widget must prohibit custom execution identity");
        Assertions.assertFalse(runsWidget.hasOwnExecutionIdentity(), "RUNS widget must report false for hasOwnExecutionIdentity");
        Assertions.assertTrue(runsWidget.getExecutionIdentityDimensions().isEmpty(), "Identity dimensions must be empty for RUNS widget");
    }

    @Test
    public void testWithPointScope()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-scope-test", "Scope Test", WidgetMetric.STATUS, null,
            WidgetPresentation.TEXT_WITH_SMALL_TREND, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0
        );
        Assertions.assertEquals(WidgetPointScope.LATEST_TEST_STATUS, config.getPointScope());

        final DashboardWidgetConfigDto updated = config.withPointScope(WidgetPointScope.LAST_RUN);
        Assertions.assertNotNull(updated);
        Assertions.assertEquals("w-scope-test", updated.getId());
        Assertions.assertEquals(WidgetPointScope.LAST_RUN, updated.getPointScope());
        // Original remains unchanged (immutability)
        Assertions.assertEquals(WidgetPointScope.LATEST_TEST_STATUS, config.getPointScope());
    }

    @Test
    public void testNumberOfRunsMetricAliases()
    {
        Assertions.assertEquals(WidgetMetric.NUMBER_OF_RUNS, WidgetMetric.fromString("NUMBER_OF_RUNS"));
        Assertions.assertEquals(WidgetMetric.NUMBER_OF_RUNS, WidgetMetric.fromString("RUN_COUNT"));
        Assertions.assertEquals(WidgetMetric.NUMBER_OF_RUNS, WidgetMetric.fromString("RUNS_COUNT"));
        Assertions.assertEquals(WidgetMetric.NUMBER_OF_RUNS, WidgetMetric.fromString("EXECUTION_RUNS"));
        Assertions.assertEquals(WidgetMetric.NUMBER_OF_RUNS, WidgetMetric.fromString("RUN_VOLUME"));
        Assertions.assertEquals(WidgetMetric.NUMBER_OF_RUNS, WidgetMetric.fromString("RUN_FREQUENCY"));
        Assertions.assertEquals(WidgetMetric.NUMBER_OF_RUNS, WidgetMetric.fromString("NUMBER_OF_TEST_RUNS"));
    }

    @Test
    public void testNumberOfRunsDefaultTitleGenerationAcrossAllPresentations()
    {
        Assertions.assertEquals(
            "Number of Runs Distribution",
            DashboardWidgetConfigDto.generateDefaultTitle(WidgetMetric.NUMBER_OF_RUNS, null, WidgetPresentation.PIE_CHART)
        );
        Assertions.assertEquals(
            "Number of Runs Ratio Bar",
            DashboardWidgetConfigDto.generateDefaultTitle(WidgetMetric.NUMBER_OF_RUNS, null, WidgetPresentation.RATIO_BAR)
        );
        Assertions.assertEquals(
            "Number of Runs String of Pearls",
            DashboardWidgetConfigDto.generateDefaultTitle(WidgetMetric.NUMBER_OF_RUNS, null, WidgetPresentation.PERLENKETTE)
        );
        Assertions.assertEquals(
            "Number of Runs Trend Graph",
            DashboardWidgetConfigDto.generateDefaultTitle(WidgetMetric.NUMBER_OF_RUNS, null, WidgetPresentation.TREND_GRAPH)
        );
        Assertions.assertEquals(
            "Number of Runs KPI",
            DashboardWidgetConfigDto.generateDefaultTitle(WidgetMetric.NUMBER_OF_RUNS, null, WidgetPresentation.TEXT)
        );
        Assertions.assertEquals(
            "Number of Runs with Trend",
            DashboardWidgetConfigDto.generateDefaultTitle(WidgetMetric.NUMBER_OF_RUNS, null, WidgetPresentation.TEXT_WITH_SMALL_TREND)
        );
        Assertions.assertEquals(
            "Number of Runs Breakdown List",
            DashboardWidgetConfigDto.generateDefaultTitle(WidgetMetric.NUMBER_OF_RUNS, null, WidgetPresentation.LIST)
        );
    }

    @Test
    public void testNumberOfRunsDefaultValueFormatIsAbsolute()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-runs-count", "", WidgetMetric.NUMBER_OF_RUNS, null,
            WidgetPresentation.PIE_CHART, null, null, null,
            "half", 0
        );

        Assertions.assertEquals(WidgetValueFormat.ABSOLUTE, config.getValueFormat(), "NUMBER_OF_RUNS valueFormat must default to ABSOLUTE");
        Assertions.assertEquals("Number of Runs Distribution", config.getTitle(), "Default title should be generated when blank");
    }

    @Test
    public void testNumberOfRunsProhibitsCustomExecutionIdentity()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-runs-identity", "Runs", WidgetMetric.NUMBER_OF_RUNS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.ABSOLUTE, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0, false, true, List.of("browser", "locale"), false, null
        );

        Assertions.assertFalse(config.isOverrideExecutionIdentity(), "NUMBER_OF_RUNS must suppress custom execution identity");
        Assertions.assertFalse(config.hasOwnExecutionIdentity(), "NUMBER_OF_RUNS hasOwnExecutionIdentity must be false");
        Assertions.assertTrue(config.getExecutionIdentityDimensions().isEmpty(), "Identity dimensions must be empty for NUMBER_OF_RUNS");
    }

    @Test
    public void testNumberOfRunsPermitsAllWidths()
    {
        final DashboardWidgetConfigDto quarter = new DashboardWidgetConfigDto(
            "w-q", "Runs Q", WidgetMetric.NUMBER_OF_RUNS, null,
            WidgetPresentation.PIE_CHART, null, null, null,
            "quarter", 0
        );
        Assertions.assertEquals("quarter", quarter.getWidth());
        Assertions.assertTrue(quarter.isQuarterWidth());

        final DashboardWidgetConfigDto half = new DashboardWidgetConfigDto(
            "w-h", "Runs H", WidgetMetric.NUMBER_OF_RUNS, null,
            WidgetPresentation.PIE_CHART, null, null, null,
            "half", 0
        );
        Assertions.assertEquals("half", half.getWidth());
        Assertions.assertTrue(half.isHalfWidth());

        final DashboardWidgetConfigDto full = new DashboardWidgetConfigDto(
            "w-f", "Runs F", WidgetMetric.NUMBER_OF_RUNS, null,
            WidgetPresentation.PIE_CHART, null, null, null,
            "full", 0
        );
        Assertions.assertEquals("full", full.getWidth());
        Assertions.assertTrue(full.isFullWidth());
    }

    @Test
    public void testReorderChildren()
    {
        final DashboardWidgetConfigDto c1 = new DashboardWidgetConfigDto(
            "c1", "Child 1", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0, false, false, Collections.emptyList(), true, null
        );
        final DashboardWidgetConfigDto c2 = new DashboardWidgetConfigDto(
            "c2", "Child 2", WidgetMetric.STATUS, null,
            WidgetPresentation.LIST, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 1, false, false, Collections.emptyList(), true, null
        );
        final DashboardWidgetConfigDto c3 = new DashboardWidgetConfigDto(
            "c3", "Child 3", WidgetMetric.STATUS, null,
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 2, false, false, Collections.emptyList(), true, null
        );

        final DashboardWidgetConfigDto container = DashboardWidgetConfigDto.createContainer(
            "cont-1", "Container", List.of(c1, c2, c3), 0
        );
        Assertions.assertEquals(3, container.getChildren().size());
        Assertions.assertEquals("c1", container.getChildren().get(0).getId());
        Assertions.assertEquals("c2", container.getChildren().get(1).getId());
        Assertions.assertEquals("c3", container.getChildren().get(2).getId());

        // Reorder completely: c3, c1, c2
        final DashboardWidgetConfigDto reordered = container.reorderChildren(List.of("c3", "c1", "c2"));
        Assertions.assertEquals(3, reordered.getChildren().size());
        Assertions.assertEquals("c3", reordered.getChildren().get(0).getId());
        Assertions.assertEquals("c1", reordered.getChildren().get(1).getId());
        Assertions.assertEquals("c2", reordered.getChildren().get(2).getId());

        // Partial reorder: c2 first, remaining (c1, c3) appended in their previous relative order
        final DashboardWidgetConfigDto partial = container.reorderChildren(List.of("c2"));
        Assertions.assertEquals(3, partial.getChildren().size());
        Assertions.assertEquals("c2", partial.getChildren().get(0).getId());
        Assertions.assertEquals("c1", partial.getChildren().get(1).getId());
        Assertions.assertEquals("c3", partial.getChildren().get(2).getId());

        // Unknown IDs ignored
        final DashboardWidgetConfigDto withUnknown = container.reorderChildren(List.of("unknown-id", "c3"));
        Assertions.assertEquals(3, withUnknown.getChildren().size());
        Assertions.assertEquals("c3", withUnknown.getChildren().get(0).getId());
        Assertions.assertEquals("c1", withUnknown.getChildren().get(1).getId());
        Assertions.assertEquals("c2", withUnknown.getChildren().get(2).getId());

        // Null or empty list returns unchanged container
        Assertions.assertSame(container, container.reorderChildren(null));
        Assertions.assertSame(container, container.reorderChildren(Collections.emptyList()));

        // Non-container / empty children returns unchanged
        Assertions.assertSame(c1, c1.reorderChildren(List.of("c1")));
    }
}

