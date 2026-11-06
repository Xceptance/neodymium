# [DEF-20261029-1305] Trend Widget Monochromatic Status, Missing Bugs Legend, and Single-Day Cost Flattening

- **Status:** `Resolved`
- **Opened:** 2026-10-29 13:05
- **Closed:** 2026-10-29 13:13 
- **Component:** `aura-manager`
- **Scope:** `Framework & UI`
- **Symptom:** In dashboard trend widgets:
  1. Costs trend graph with DAILY mode collapsed all same-day runs into a single dot without generating a trend line.
  2. Status trend graph rendered only a single blue line for pass rate without showing distinct colors for statuses (passed, fixed, known fail, unknown fail, ignored).
  3. Bugs trend graph lacked a descriptive legend explaining the metric spikes and was styled with generic blue instead of bug-indicative orange.
- **Root Cause:**
  1. Time bucketing in `WidgetAggregationService` had no sub-day fallback when all test runs occurred on the same calendar day.
  2. `buildTrendGraphWidget` reduced status telemetry to a single scalar pass rate instead of generating multi-status stacked layer paths.
  3. `dashboard-widgets.html` hardcoded SVG stroke and fill to `#3b82f6` and omitted a legend block for trend widgets.
- **Detection Gap ("What did we miss?"):** Previous unit tests only asserted that SVG paths were non-empty and points existed, but did not assert color distinction, layer count for multi-status progression, or multi-point generation when runs are clustered on a single date.
- **Resolution:**
  1. Implemented sub-day granularity fallback (DAILY -> HOURLY -> BY_RUNS) when all runs in dataset share the same date.
  2. Implemented `WidgetTrendLayerDto` and generated stacked area layers for STATUS (green, blue, orange, red, gray).
  3. Added metric-specific theme colors and legend chips for STATUS, BUGS, and COSTS.
- **Safety Net Added:** Added unit tests in `WidgetAggregationServiceTest` verifying multi-layer status breakdown, bugs legend generation, and single-day cost trend preservation.
