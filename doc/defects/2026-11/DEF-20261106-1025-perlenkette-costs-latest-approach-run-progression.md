# [DEF-20261106-1025] String of Pearls Costs Metric Collapses Runs into Daily Buckets under Latest Approach

- **Status:** `Resolved`
- **Opened:** 2026-11-06 10:25
- **Closed:** 2026-11-06 10:26
- **Component:** `aura-manager`
- **Scope:** `Framework & UI`
- **Symptom:** On dashboards displaying a String of Pearls (`PERLENKETTE`) widget with Trend Mode: `LATEST_APPROACH` ("Latest Approach (Suite Evolution)") and Metric: `COSTS`, multiple test runs were collapsed into daily calendar buckets (e.g. 2 milestones) instead of showing the full chronological test run progression (e.g. 6 milestones), diverging from the behavior of `STATUS` and other suite evolution metrics.
- **Root Cause:** In `WidgetAggregationService.buildPerlenketteWidget()`, `trendMode == WidgetTrendMode.LATEST_APPROACH` for `WidgetMetric.COSTS` exclusively read from `tokenCostTrend.getPointGroups()`, which aggregates test runs by calendar day (`LocalDate`). While `buildTrendGraphWidget()` was previously updated under `DEF-20261029-1542` to evaluate chronological runs (`runs`) whenever multiple runs were present, `buildPerlenketteWidget()` was omitted and continued routing all `LATEST_APPROACH` cost milestones to daily buckets.
- **Detection Gap ("What did we miss?"):** Existing unit tests for `PERLENKETTE` with `WidgetMetric.COSTS` (`testPerlenketteCostsByRuns`, `testPerlenketteCostsBudgetAlertSpectrum`) exclusively tested `WidgetTrendMode.BY_RUNS`, leaving `WidgetTrendMode.LATEST_APPROACH` with costs unverified in the test harness.
- **Resolution:** Updated `WidgetAggregationService.buildPerlenketteWidget()` under `LATEST_APPROACH` for `WidgetMetric.COSTS` to evaluate chronological runs (`runs`) whenever runs are available, extracting each run's `totalLlmCost`, timestamp, and milestone label, with fallback to `tokenCostTrend.getPointGroups()` only when runs are absent.
- **Safety Net Added:** Added unit tests `WidgetAggregationServiceTest.testPerlenketteCostsLatestApproach_WithMultipleRuns_GeneratesRunProgression` and `WidgetAggregationServiceTest.testPerlenketteCostsLatestApproach_FallbackToTokenCostTrendWhenNoRuns`.
