# [DEF-20261104-1255] Disconnect in Passed Tests Metric and Scaling Between Text + Trend and Normal Trend Graph

- **Status:** `Resolved`
- **Opened:** 2026-11-04 12:55
- **Closed:** 2026-11-04 13:06
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** When placing a "Text + Trend" (`TEXT_WITH_SMALL_TREND`) widget side-by-side with a normal "Trend Graph" (`TREND_GRAPH`) widget for the `STATUS` metric (e.g. tracking passed tests over test suite evolution), the trend representations displayed conflicting patterns:
  1. The normal `TREND_GRAPH` displayed a monotonic upward curve from 110 towards 225 passed tests as the suite grew from 116 to 232 tests.
  2. The `TEXT_WITH_SMALL_TREND` sparkline graph displayed an extreme plunge and steep rebound (V/N-shaped cliff drops) over the same runs.
  3. When `valueFormat: ABSOLUTE` was selected, the `TREND_GRAPH` correctly graphed absolute test volume, while `TEXT_WITH_SMALL_TREND` continued plotting scalar percentage pass rates with raw unconstrained min-max normalization, stretching a minor 1.4% pass rate variation across 100% of the SVG canvas height.
- **Root Cause:**
  1. **Metric Value Disconnect on `WidgetTrendPointDto`:** In `WidgetAggregationService.buildTrendGraphWidget`, when populating `WidgetTrendPointDto` for the `STATUS` metric, the service previously hardcoded `v = pt.getPassRate()` across all format configurations. While `TREND_GRAPH` calculated stacked area paths directly from raw counts (`StatusPointBreakdown.passTotal() * baseScale`), `TEXT_WITH_SMALL_TREND` consumed the scalar `getValue()` on `WidgetTrendPointDto`. Thus, under `ABSOLUTE` format, the sparkline plotted pass rate percentages rather than passed test counts.
  2. **Unbounded Relative Scaling in `generateSparklineSvgPath`:** `generateSparklineSvgPath` computed `norm = (v - min) / (max - min)` using only the local minimum and maximum observed in the data slice. For high-performing suites where pass rates varied slightly (e.g., 96.0% to 97.4%), the negligible 1.4% delta was stretched across the full canvas height (from `y = 4.0` down to `y = 28.0`), creating the optical illusion of a severe failure collapse. Furthermore, flat datasets of 100% or 0% collapsed to a center line (`y = 16.0`) instead of hugging the top or bottom baselines.
- **Detection Gap ("What did we miss?"):**
  Existing unit tests in `WidgetAggregationServiceTest` only verified that `generateSparklineSvgPath` returned non-empty SVG path strings (`M ... L ... C ...`), without asserting normalized Y-coordinate bounds for realistic narrow-range percentage datasets (e.g. 96%–98%) or validating consistency across `PERCENT` vs `ABSOLUTE` value formats for `TEXT_WITH_SMALL_TREND`.
- **Resolution:**
  1. In `WidgetAggregationService.buildTrendGraphWidget`, updated point population across all trend modes (`LATEST_APPROACH`, `BY_MILESTONE`, and `BY_RUNS`) so that when `format == WidgetValueFormat.ABSOLUTE`, `WidgetTrendPointDto.getValue()` stores passed test counts (`pt.getPassedTotalCount()` or `r.getPassedCount()`), formats labels as `"X tests"`, sets `minVal = 0.0` and `maxVal = maxTests`, and formats delta as `"+/-X tests vs start"`.
  2. Overloaded `generateSparklineSvgPath(points, boundMin, boundMax)` to respect domain bounds:
     - For percentage metrics (`boundMin == 0.0 && boundMax == 100.0`), applied an adaptive minimum window of 20% centered around the observed range within `[0.0, 100.0]`. This dampens sub-5% micro-fluctuations and prevents cliff-drop distortions, while positioning near-perfect pass rates (>95%) stably in the top 25% zone of the sparkline canvas.
     - For volume count metrics, bounded `min` to `Math.min(boundMin, min)` and `max` to `Math.max(boundMax, max)`.
     - Ensured flat 100% baselines map to the top edge (`y = 4.0`) and flat 0% baselines map to the bottom edge (`y = 28.0`).
  3. In `buildTextWithSmallTrendWidget`, forwarded `trendWidget.getTrendMinVal()` and `trendWidget.getTrendMaxVal()` to `generateSparklineSvgPath`.
- **Safety Net Added:**
  Added permanent automated regression tests to `WidgetAggregationServiceTest`:
  - `testTextWithSmallTrendWidget_StatusPercent_NoArtificialCliffDrop`: Asserts that a high pass rate dataset fluctuating between 96.0% and 97.4% keeps all Y coordinates within the upper canvas band (`ys <= 12.0`) without plunging.
  - `testTextWithSmallTrendWidget_StatusAbsolute_MirrorsVolumeCurve`: Asserts that `TEXT_WITH_SMALL_TREND` with `WidgetValueFormat.ABSOLUTE` reflects passed test counts (110 and 225) and delta labels `"tests vs start"`.
  - `testGenerateSparklineSvgPath_DirectBoundedAndAdaptiveScale`: Asserts direct coordinate generation for flat 100% (`y = 4.0`) and flat 0% (`y = 28.0`) baselines.
