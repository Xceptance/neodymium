# [DEF-20261103-1351] Tag Metric Trend Graph Multi-Status Breakdown Fallback

- **Status:** `Resolved`
- **Opened:** 2026-11-03 13:51
- **Closed:** 2026-11-03 14:15
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In trend widgets configured with metric `TAG` (e.g. "Area Trend", tagKey="Area"), the trend graph rendered the multi-layer pass/fail status breakdown ("Passed Clean", "Failed Known", "Failed Unknown", "Succeeded Fixed") and pass rate delta ("+46.6% vs start") across all 118 suite tests, rather than showing the evolution of tests tagged with that tagKey.
- **Root Cause:** In `WidgetAggregationService.java` (`buildTrendGraphWidget`), `WidgetMetric.TAG` fell through to `else // STATUS or TAG` across all trend modes (`LATEST_APPROACH`, `HOURLY`, `DAILY`, `MONTHLY`, `BY_RUNS`). It extracted `pt.getPassRate()` or `r.getPassRate()` and generated 4 stacked area layers from `StatusPointBreakdown`, completely ignoring tag execution volume.
- **Detection Gap ("What did we miss?"):** Previous widget tests focused on `WidgetMetric.STATUS`, `AI_USAGE`, `BUGS`, and `COSTS` for trend graphs. Metric `TAG` was tested only in `RATIO_BAR` and `LIST` presentations, leaving `TREND_GRAPH` with `TAG` untested and falling into the legacy pass-rate branch.
- **Resolution:**
  1. Updated `WidgetAggregationService.java` to handle `WidgetMetric.TAG` separately across all 5 trend modes (`LATEST_APPROACH`, `BY_RUNS`, `HOURLY`, `DAILY`, `MONTHLY`), extracting total test execution counts ($v = \text{totalTests}$) instead of pass rates.
  2. Replaced the multi-layer status breakdown for `TAG` with a single unified trend layer and curve titled `"<TagKey> Tests"` (e.g. `"Area Tests"` or `"Total Tests"`).
  3. Formatted trend values as integer counts (`"X tests"`) and delta badges as `"+X tests vs start"` (or percentage growth when `valueFormat = PERCENT`).
  4. Updated Y-axis tick generation for `TAG` to scale integer test counts from `0 Tests` to peak execution volume.
- **Safety Net Added:** Added 6 dedicated automated regression tests in `WidgetAggregationServiceTest.java`:
  - `testTrendGraph_TagMetric_LatestApproach_ShowsExecutionCountTrend`
  - `testTrendGraph_TagMetric_ByRuns_ShowsPerRunExecutionCounts`
  - `testTrendGraph_TagMetric_DailyBucketing`
  - `testTrendGraph_TagMetric_HourlyBucketing`
  - `testTrendGraph_TagMetric_MonthlyBucketing`
  - `testTrendGraph_TagMetric_PercentFormat_ShowsPercentageGrowth`
