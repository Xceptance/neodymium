# [DEF-20261030-0930] Trend Chart Squashed Aspect Ratio and Missing Y and X Axes Values

- **Status:** `Resolved`
- **Opened:** 2026-10-30 09:30
- **Closed:** 2026-10-30 09:40
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** On the dashboard, trend graph widgets (such as "Status Trend", "Token Cost Trend", and "Bugs Trend") rendered as an extremely flat, squished horizontal strip (~90px height) without Y-axis values/gridlines or X-axis milestone/time labels. Furthermore, trend graph widgets could be placed into half-width card slots (50% grid width) instead of spanning full width.
- **Root Cause:**
  1. `dashboard-widgets.html` constrained `.widget-trend-svg-container` to `height: 90px` with `viewBox="0 0 300 80"`, while `WidgetAggregationService.java` computed coordinates within a 300x80 box (`graphHeight = 60.0`). When stretched across a wide monitor (1200px+), the 90px vertical height produced a disproportionate 14:1 aspect ratio that squashed all stacked area contours into a flat ribbon.
  2. `WidgetAggregationService.java` and `dashboard-widgets.html` completely lacked Y-axis and X-axis tick generation and rendering logic. The SVG only rendered a single horizontal border line at `y = 70`.
  3. `DashboardWidgetViewDto.isFullWidth()` strictly mirrored the configured width without enforcing full width for `WidgetPresentation.TREND_GRAPH`, and default configurations like `default-token-cost` defaulted to `"half"` width.
- **Detection Gap ("What did we miss?"):** Previous tests only asserted that SVG paths (`d` attributes) were non-empty and that trend layers existed, without verifying SVG canvas dimensions, aspect ratio, or axes ticks and labels.
- **Resolution:**
  1. In `DashboardWidgetViewDto.java`, enforced `isFullWidth()` to return `true` whenever `config.getPresentation() == WidgetPresentation.TREND_GRAPH`, and added getters for trend canvas dimensions (`trendWidth = 800`, `trendHeight = 200`, `trendPadLeft = 55`, `trendPadRight = 20`, `trendPadTop = 15`, `trendBaselineY = 165`).
  2. Created immutable `WidgetAxisTickDto.java` (`coordinate`, `label`, `showGridLine`) and added `yAxisTicks` and `xAxisTicks` to `DashboardWidgetViewDto`.
  3. In `WidgetAggregationService.java`, expanded the SVG plotting canvas to `800 x 200` (`graphWidth = 725.0`, `graphHeight = 150.0`, `baselineY = 165.0`). Generated clean Y-axis ticks with gridlines (test volume steps for stacked areas, percentage steps for pass rate, formatted currency for costs, bug counts for bugs) and intelligently spaced, non-overlapping X-axis milestone ticks.
  4. In `dashboard-widgets.html`, updated container height to `220px`, viewBox to `0 0 800 200`, rendered Y-axis vertical line, dashed horizontal gridlines, and text labels, rendered X-axis baseline, tick lines, and text labels, and updated dot overlay coordinates to the 800x200 space.
  5. In `ReportDashboardSettingService.java`, updated default trend widgets (`default-token-cost`) to `"full"` width.
  6. In `widget-configurator-modal.html` and `report-manager.js`, auto-selected full width radio when `TREND_GRAPH` presentation is selected.
- **Safety Net Added:** Added unit tests `WidgetAggregationServiceTest.testTrendGraph_EnforcesFullWidthAndGeneratesAxes` and `WidgetAggregationServiceTest.testTrendGraph_CostsMode_GeneratesCurrencyYAxisTicks`, and updated `testSinglePointTrend_GeneratesBaselineLineAndBaselineLabel` and `testStatusTrend_ProportionalMaxTestsScaling_AvoidsFlatBrick` for the 800x200 coordinate space. Verified via `StaticResourceSyntaxTest`.
