# [DEF-20261030-1256] Trend Chart Severe Aspect Ratio Distortion on Quarter-Width (25%) Dashboard Cards

- **Status:** `Resolved`
- **Opened:** 2026-10-30 12:56
- **Closed:** 2026-10-30 13:05
- **Component:** `aura-manager`
- **Scope:** `Framework & SUT`
- **Symptom:** When a trend graph widget was configured with Quarter Width (25% width, `grid-column: span 3`), the trend chart suffered severe horizontal compression and vertical elongation (distortion factor > 3.1x). Typography (axis ticks, monospace labels) and single-run stacked status bars were squashed into thin, needle-like vertical pillars, making the chart illegible.
- **Root Cause:** 
  1. The SVG container had a hardcoded `style="width: 100%; height: 220px;"` with `viewBox="0 0 800 200" preserveAspectRatio="none"`.
  2. On a 12-column grid, a quarter-width card has an actual physical width of only ~280px–315px. When stretched into a 220px tall container, its physical aspect ratio is ~1.27:1 to 1.4:1.
  3. Forcing the wide 800x200 (4:1) coordinate system into this compact 1.27:1 container with non-uniform scaling (`preserveAspectRatio="none"`) compressed horizontal dimensions by ~2.85x while expanding vertical dimensions by ~1.1x, compounding into a 3.14x vertical elongation distortion. Single-run bars (configured with `TREND_SINGLE_COLUMN_HALF_WIDTH = 50.0`, i.e., 100px wide in 800px coordinate space = 12.5% canvas width) shrunk to ~35px in screen pixels, while text labels were squashed horizontally.
- **Detection Gap ("What did we miss?"):** Tests in `WidgetAggregationServiceTest` only asserted SVG path generation for the default half-width (`800x200`) canvas. No test validated the geometric aspect ratio, canvas coordinate adaptability, or single-column bar widths when widgets were configured with quarter width (`"quarter"` or `"25%"`).
- **Resolution:**
  1. Added dynamic coordinate space resolution in `DashboardWidgetViewDto`: quarter-width widgets dynamically use `trendWidth = 360.0`, `trendHeight = 170.0` (aspect ratio 2.12:1), `trendBaselineY = 140.0`, `trendPadLeft = 50.0`, and `trendPadRight = 15.0`, while half/full-width widgets retain `800x200`.
  2. Updated `WidgetAggregationService`: adapted milestone tick density (`targetMilestones = 5` for quarter, 8 for half, 10 for full) and parameterized `buildStackedAreaPolygon(...)` with `singleColumnHalfWidth = 25.0` for quarter width, ensuring single-point status columns span 50 coordinate units (~14% of canvas width) with balanced proportions.
  3. Updated `dashboard-widgets.html`: replaced static `viewBox="0 0 800 200"` with dynamic Thymeleaf attribute `th:attr="viewBox='0 0 ' + ${#numbers.formatInteger(w.trendWidth, 1)} + ' ' + ${#numbers.formatInteger(w.trendHeight, 1)}"`, removed inline 220px height style, and parameterized point overlay dots to use `w.trendWidth` and `w.trendHeight`.
  4. Updated `dashboard-styles.css`: introduced `.widget-trend-svg-container` (default 220px height) and `.widget-card-quarter .widget-trend-svg-container { height: 150px; }`, harmonizing physical container aspect ratio (280–315px : 150px $\approx 1.87:1$ to $2.1:1$) with SVG canvas aspect ratio (360 : 170 $\approx 2.12:1$), reducing scaling distortion to $\le 6\%$.
- **Safety Net Added:** Added unit test `WidgetAggregationServiceTest.testTrendGraph_QuarterWidth_GeneratesProportionalAspectCanvasAndSizing` asserting dynamic 360x170 canvas properties, proportional column coordinate bounds, and single-run top line width bounding.
