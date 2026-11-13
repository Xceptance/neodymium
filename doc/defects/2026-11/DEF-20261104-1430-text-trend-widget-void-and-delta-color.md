# [DEF-20261104-1430] Text with Trend Widget Expansive Void, Micro-Sparkline Constriction, and CSS Trend-Down Green Defect

- **Status:** `Resolved`
- **Opened:** 2026-11-04 14:25
- **Closed:** 2026-11-04 15:06
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:**
  1. On wide cards (half-width or full-width), the sparkline was hardcoded to a tiny `120x32px` box inside a `max-width: 160px` container pinned to the bottom-right corner, leaving over 80% of the horizontal card body as a vacant empty white void.
  2. On quarter-width cards, the metric was pushed to the top and delta/sparkline to the bottom, leaving a 120px cavernous gap in the center. Moreover, long widget titles wrapped `.widget-card-header` into 3 separate lines because of `flex-wrap: wrap`.
  3. Defect regressions (e.g. `+1 bug vs start`) were rendered with green text instead of red, despite `isTrendPositive` correctly returning `false`, because `.token-cost-delta-group.trend-down .token-cost-delta` was hardcoded to `#059669` (emerald green).
- **Root Cause:**
  1. `dashboard-widgets.html` hardcoded `.widget-sparkline-container` to `max-width: 160px; height: 32px` in an end-anchored footer row, divorcing the sparkline from the available card width.
  2. `WidgetAggregationService.generateSparklineGeometry` hardcoded coordinate space bounds to `width: 120.0` and `height: 32.0`, preventing SVG fluid scaling.
  3. `dashboard-styles.css` set `flex-wrap: wrap` on `.widget-card-quarter .widget-card-header` and `.widget-card-title` without text truncation (`text-overflow: ellipsis`).
  4. `report-manager.css` mistakenly assigned `#059669` (green) to `.token-cost-delta-group.trend-down .token-cost-delta`.
- **Detection Gap ("What did we miss?"):**
  Previous unit tests only checked that `isTrendPositive == false` in the Java DTO and that the sparkline SVG string was non-empty. Tests never asserted CSS color inheritance for `.trend-down`, nor did they check responsive sparkline layout classes or header non-wrapping styling.
- **Resolution:**
  1. Scaled `WidgetAggregationService.generateSparklineGeometry` to a normalized `300.0 x 70.0` coordinate space with `padX = 6.0` and `padY = 6.0`.
  2. Refactored `textSmallTrendBody(w)` in `dashboard-widgets.html` and `dashboard-styles.css`:
     - Wide cards (half / full width): Use a responsive side-by-side layout with the KPI group on the left (~35% width) and an expansive SVG sparkline spanning the remaining 65% width with gradient area fill and glowing endpoint.
     - Quarter cards: Top-grouped KPI/delta with a full-width bottom sparkline (`width: 100%; height: 50px;`) filling the lower half of the card.
  3. Added `flex-wrap: nowrap` and `text-overflow: ellipsis` on quarter-card headers in `dashboard-styles.css`.
  4. Corrected `.token-cost-delta-group.trend-down .token-cost-delta` to `var(--status-unknown-fail, #ef4444)` in both `dashboard-styles.css` and `report-manager.css`.
- **Safety Net Added:**
  - `WidgetAggregationServiceTest.testGenerateSparklineSvgPath_DirectBoundedAndAdaptiveScale`: Verifies 300x70 bounded stroke paths.
  - `WidgetAggregationServiceTest.testGenerateSparklineGeometry_CalculatesStrokeAreaAndEndpoint`: Verifies 300x70 stroke and area polygon closure (`L 294.0 70.0 L 6.0 70.0 Z`) and endpoint `lastX = 294.0`.
  - `WidgetAggregationServiceTest.testTextWithSmallTrendWidget_BugTrendPolarityAndGeometry`: Asserts `+1 bug vs start` sets `isTrendPositive == false` and populates 300x70 geometry.
  - `StaticResourceSyntaxTest.testTextWithSmallTrendWidget_CompactStylesAndMarkup`: Asserts CSS contracts (`flex-direction: row` for wide cards, `flex-direction: column` for quarter cards, non-wrapping header, red trend-down color) and template bindings (`widget-text-trend-metrics`, `viewBox="0 0 300 70"`, `vector-effect="non-scaling-stroke"`).
