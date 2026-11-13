# [DEF-20261102-1235] AI Usage Trend Graph Stacked Curves Mutual Influence

- **Status:** `Resolved`
- **Opened:** 2026-11-02 12:35
- **Closed:** 2026-11-02 12:37
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In trend widgets configured with metric `AI_USAGE`, the curves of `Classic`, `Playbook`, `Healed`, and `AI-Driven` were rendered as a cumulative stacked area chart. This placed the `AI-Driven` layer on top of `Classic` and `Healed`, causing run-to-run variations in `Classic` test volume to violently distort the upper contour of `AI-Driven`, making a steady 12-test volume appear jagged and curvy in the multi-legend view while appearing completely flat when isolated.
- **Root Cause:** In `WidgetAggregationService.java` (`buildTrendGraphWidget`), the `AI_USAGE` metric branch adapted the cumulative stacking algorithm from the Status breakdown (`y0 -> yClassic -> yPlaybook -> yHealed -> yAiDriven`), stacking categories on top of each other rather than plotting each category independently from baseline zero ($Y = 0$).
- **Detection Gap ("What did we miss?"):** Existing unit tests verified that `WidgetTrendLayerDto` instances were generated and non-empty, but did not assert that `AI_USAGE` category paths are independently referenced from the zero baseline without vertical accumulation.
- **Resolution:**
  1. Updated `WidgetAggregationService.java` for `WidgetMetric.AI_USAGE` to compute each category's vertical coordinate directly from baseline zero (`baselineY - (categoryCount * baseScale)`), producing independent standalone line paths (`svgLinePath`) without cumulative vertical accumulation or stacked area polygons.
  2. Updated `dashboard-widgets.html` so that when a trend layer has no stacked area path (`!layer.hasPath()`), `.widget-trend-layer-line` paths are rendered visible by default with `stroke-width="2.2"`, displaying all category curves simultaneously without stacking.
  3. Updated `report-manager.js` (`toggleWidgetTrendLayerSelect`) to check `hasAreas = areaPaths.length > 0` and properly restore visibility of all category lines upon deselecting a legend chip in an unstacked multi-line trend graph.
- **Safety Net Added:** Added `@Test testAiUsageTrendGraphDecouplesCategoryCurves()` to `WidgetAggregationServiceTest`, asserting that a 50-test drop in `Classic` test volume produces zero vertical displacement in the `AI-Driven` line path when AI test count is constant.
