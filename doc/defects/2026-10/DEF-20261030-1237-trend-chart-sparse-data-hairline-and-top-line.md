# [DEF-20261030-1237] Single-Point Trend Graph Status Needle and Spurious Full-Width Top Stroke

- **Status:** `Resolved`
- **Opened:** 2026-10-30 12:37
- **Closed:** 2026-10-30 12:41
- **Component:** `aura-manager`
- **Scope:** `Framework & UI / Styles`
- **Symptom:** When a trend graph widget configured with metric `STATUS` or `TAG` contains only a single data point (e.g. a newly created batch, filtered run, or single-point evolution), the chart displays a microscopic 4-pixel vertical hairline needle in the center, and a solid green horizontal line cutting across the entire canvas from edge to edge at the top Y-axis level.
- **Root Cause:**
  1. `WidgetAggregationService.buildStackedAreaPolygon()` used `xs[0] - 2.0` and `xs[0] + 2.0` (4px width) when `n == 1` instead of rendering a well-proportioned stacked column (such as 100px width in an 800px viewBox).
  2. `WidgetAggregationService` lines 1742–1749 reconstructed `linePath` as `M padLeft topY L width - padRight topY`, drawing a 710px horizontal line across the entire SVG canvas rather than capping only the top boundary of the single column (`M x0 topY L x1 topY`).
- **Detection Gap ("What did we miss?"):** Existing unit tests for single-point trends (`testSinglePointTrend_GeneratesBaselineLineAndBaselineLabel`) only exercised `COSTS` metric, and status trend tests (`testStatusTrendGraphMultiColorStackedLayers`) only tested multi-run series (`n >= 2`), leaving single-point status trends untested.
- **Resolution:**
  1. In `WidgetAggregationService.java`, defined `TREND_SINGLE_COLUMN_HALF_WIDTH = 50.0` (100px total column width) for single-point stacked columns in `buildStackedAreaPolygon`.
  2. For `metric == STATUS` and `metric == TAG` when `n == 1`, capped `linePath` and `areaPath` strictly to `[x0, x1]` (`M x0 topY L x1 topY`), eliminating the full-width horizontal line.
  3. Ensured overlay dot, X-axis tick, and tooltips align cleanly to the center of the 100px column (`x = 425.0`).
- **Safety Net Added:** Added automated regression test `WidgetAggregationServiceTest.testStatusTrend_SinglePoint_RendersProminentStackedColumnWithoutSpuriousFullWidthLine` asserting that stacked polygon paths have width 100px, `linePath` does not stretch to `width - padRight`, and axes ticks align properly.
