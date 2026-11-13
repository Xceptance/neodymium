# [DEF-20261029-1635] Status Trend Flat Normalization and Missing Healed/AI Patterns

- **Status:** `Resolved`
- **Opened:** 2026-10-29 16:35
- **Closed:** 2026-10-29 16:42
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** Status trend widgets rendered as a completely flat horizontal rectangle (brick) across all historical runs rather than mirroring the natural rising and falling stacked area contour seen on the History Trend chart. Furthermore, AI and Healed executions were displayed as plain solid color blocks instead of being marked with dots/sparkles (`trendSparklePattern` / `.seg-healed-print`) and 45° diagonal pinstripes (`trendAiPinstripePattern` / `.seg-ai-print`), making them visually indistinguishable from standard statuses.
- **Root Cause:**
  1. In `WidgetAggregationService.buildTrendGraphWidget`, stacked area heights were computed by dividing each run point's category counts by that run's own total (`sb.total()`), i.e. `yPass[i] = yFixed[i] - (((double) sb.pass / tot) * graphHeight)`. Because the sum of all status categories always equals `tot`, the top boundary of every run always reached exactly 100% of graph height (`y0 - 1.0 * graphHeight`), completely eliminating test volume differences across runs and flattening the chart into a solid block.
  2. The widget trend and pie chart SVGs did not include SVG `<pattern>` definitions for sparkle dots (`#sparkle-pat-widget-...`) and diagonal pinstripes (`#ai-pat-widget-...`), nor did `WidgetTrendLayerDto` carry pattern URLs or CSS print pattern classes to layer atop the SVG fill or chip badges.
- **Detection Gap ("What did we miss?"):** Existing widget tests asserted that `view.hasTrendLayers()` was true and that layers existed, but did not assert relative Y-coordinate scaling across runs with different total test volumes (e.g. 5 tests vs 10 tests), nor did they verify that healed and AI sub-layers carried SVG pattern references.
- **Resolution:**
  1. Updated `WidgetAggregationService.buildTrendGraphWidget` to compute `maxTests = Math.max(1, max(sb.total()))` across the series and scale stacked area boundaries via `baseScale = graphHeight / maxTests`.
  2. When `showHealedAi` is active, broke down each status category into Clean, Healed, and AI sub-layers (13 possible layers), applying `url(#sparkle-pat-widget-...)` / `seg-healed-print` to healed layers and `url(#ai-pat-widget-...)` / `seg-ai-print` to AI layers.
  3. Aligned the top stroke path (`trendSvgPath`) and trend node dots (`trendPoints`) to the peak curve of the stacked area (`yPassAi` / `yPass`).
  4. Added pattern definitions and pattern overlay strokes in `dashboard-widgets.html` for both Pie Chart and Trend Graph SVGs, and updated `report-manager.css` to render dot and badge textures for `.dot.seg-healed-print` and `.dot.seg-ai-print`.
- **Safety Net Added:** Added unit tests `WidgetAggregationServiceTest.testStatusTrend_ProportionalMaxTestsScaling_AvoidsFlatBrick` and `WidgetAggregationServiceTest.testStatusTrend_ShowHealedAi_GeneratesPatternLayers`.
