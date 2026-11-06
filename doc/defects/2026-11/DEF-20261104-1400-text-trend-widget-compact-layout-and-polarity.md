# [DEF-20261104-1400] Text with Trend Widget 60% Barren Void, Inverted Defect Polarity, and Sparkline Disconnection

- **Status:** `Resolved`
- **Opened:** 2026-11-04 14:00
- **Closed:** 2026-11-04 14:15
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** The "Text with Trend" (`TEXT_WITH_SMALL_TREND`) dashboard metric card presented severe visual and semantic defects:
  1. The card body enforced a rigid `min-height: 220px`, but its top-aligned KPI number and 38px sparkline consumed only ~70px, leaving more than 60% of the card area as a barren empty void.
  2. The sparkline was an isolated horizontal stroke floating in the upper right with no baseline, no area gradient, and no endpoint marker.
  3. When tracking defect trends, a bug increase (e.g. 0 -> 1 bug) was rendered with green text (`trend-up`), incorrectly signaling an adverse regression as a positive improvement.
  4. On narrow quarter-width cards, header action buttons wrapped onto a second line, displacing card elements.
- **Root Cause:**
  1. `dashboard-styles.css` set `min-height: 220px` on `.widget-text-trend-card .widget-text-trend-card-body`. Combined with Thymeleaf's `d-flex flex-column`, all content was shoved to the top.
  2. `WidgetAggregationService.buildTrendGraphWidget` conflated the mathematical delta sign with favorable polarity (`isPos = delta >= 0.0`), failing to invert polarity for defects, flakiness, and costs where increases are negative.
  3. `generateSparklineSvgPath` only produced a raw stroke string without area fill polygon or endpoint coordinates.
  4. `.widget-card-actions` lacked `flex-shrink: 0; white-space: nowrap;`.
- **Detection Gap ("What did we miss?"):**
  Existing test suites (`StaticResourceSyntaxTest` and `WidgetAggregationServiceTest`) verified that sparkline SVG strings were non-empty and that widget templates compiled, but never verified height constraints, non-wrapping header action styles, area gradient polygon generation, or polarity semantics for defect metrics.
- **Resolution:**
  1. Created `SparklineGeometryDto` and `WidgetAggregationService.generateSparklineGeometry(...)` to compute smooth Bezier stroke paths, closed area paths (`sparklineAreaSvgPath`), and endpoint dot coordinates (`lastX`, `lastY`).
  2. Updated `DashboardWidgetViewDto` with `sparklineAreaSvgPath`, `sparklineLastX`, `sparklineLastY`, and `sparklineHasEndpoint`, maintaining full constructor backward compatibility.
  3. Decoupled mathematical delta sign (`isIncrease = delta >= 0.0`) from favorable polarity (`isPos = delta <= 0.0` for BUGS, FLAKINESS, COSTS) in `WidgetAggregationService.buildTrendGraphWidget`.
  4. Redesigned `textSmallTrendBody` in `dashboard-widgets.html` into a balanced KPI layout: top section hosts the hero value and secondary context; anchored footer duo pairs the delta tag on the left with a sparkline featuring SVG linear gradient area fill and glowing live endpoint circle on the right.
  5. Updated `dashboard-styles.css` to reduce `min-height` to 125px (120px for quarter-width), and added `flex-shrink: 0; white-space: nowrap;` to `.widget-card-header .widget-card-actions`.
- **Safety Net Added:**
  - `WidgetAggregationServiceTest.testGenerateSparklineGeometry_CalculatesStrokeAreaAndEndpoint`: Verifies empty, single-point, and multi-point sparkline stroke, area polygon, and endpoint coordinates.
  - `WidgetAggregationServiceTest.testTextWithSmallTrendWidget_BugTrendPolarityAndGeometry`: Asserts `+1 bug vs start` sets `isTrendPositive == false` (red styling) and populates sparkline area and endpoint coordinates.
  - `StaticResourceSyntaxTest.testTextWithSmallTrendWidget_CompactStylesAndMarkup`: Asserts CSS contracts (`min-height: 125px`, `min-height: 120px`, non-wrapping actions) and template bindings (`sparklineAreaSvgPath`, endpoint circle coordinates).
