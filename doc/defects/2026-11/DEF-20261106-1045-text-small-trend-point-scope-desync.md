# [DEF-20261106-1045] Text with Small Trend KPI Desynchronization from Trend Mode

- **Status:** `Resolved`
- **Opened:** 2026-11-06 10:45
- **Closed:** 2026-11-06 10:52
- **Component:** `aura-manager`
- **Scope:** `Framework & UI`
- **Symptom:**
  In `TEXT_WITH_SMALL_TREND` widgets configured with `BY_RUNS` (or time-bucketed trend modes such as `DAILY` or `HOURLY`),
  the sparkline graph plots historical runs and drops down to the latest run's pass rate (e.g., dropping to 50% PASS on the last run of 2 tests),
  while the adjacent KPI text and count display the entire test base variation numbers (e.g., "86% PASS", "118 Tests") from `LATEST_TEST_STATUS`.
  Concurrently, standalone `TEXT` widgets configured with `LAST_RUN` correctly display "50% PASS" and "2 Tests", creating a direct contradiction on the dashboard.
- **Root Cause:**
  1. In `WidgetAggregationService.buildTextWithSmallTrendWidget(...)`, text evaluation was unconditionally delegated to `buildTextWidget(config, ...)`
     without adapting the target point scope to match `config.getTrendMode()`.
  2. In the widget configurator modal (`widget-configurator-modal.html` and `report-manager.js`), the "Target Point / Time Scope" (`pointScope`)
     section is hidden for trend-based widgets, leaving `config.getPointScope()` defaulted to `LATEST_TEST_STATUS`.
  3. `buildTextWidget(...)` only evaluates `pointScope` and ignores `trendMode`, always evaluating `latestVariations` across the suite for `TEXT_WITH_SMALL_TREND`.
  4. In `WidgetAggregationService.resolveUniqueBugs(...)`, bug inclusion for non-`LATEST_TEST_STATUS` point scopes only checked `scopedRunIds.contains(b.getLinkedRunId())`
     instead of verifying historical activity via `isBugActiveAtRun(...)`, causing active defects introduced in earlier runs to drop out of `LAST_RUN` card scopes.
- **Detection Gap ("What did we miss?"):**
  Prior unit tests for `TEXT_WITH_SMALL_TREND` in `WidgetAggregationServiceTest` tested fixtures where `latestVariations` was empty or matched run outcomes,
  masking the divergence between `trendWidget`'s latest point and `textWidget`'s test base snapshot.
- **Resolution:**
  1. Updated `WidgetAggregationService.buildTextWithSmallTrendWidget(...)` to derive the effective `WidgetPointScope` from `config.getTrendMode()`:
     - `BY_RUNS` -> `WidgetPointScope.LAST_RUN`
     - `DAILY` -> `WidgetPointScope.TODAY` (with fallback to `LAST_RUN` if no runs executed today)
     - `HOURLY` -> `WidgetPointScope.LAST_HOUR` (with fallback to `LAST_RUN` if no runs in the last hour)
     - `MONTHLY` -> `WidgetPointScope.LAST_MONTH` (with fallback to `LAST_RUN` if no runs in the last month)
     - `LATEST_APPROACH` -> `WidgetPointScope.LATEST_TEST_STATUS`
  2. Updated `WidgetAggregationService.resolveUniqueBugs(...)` to evaluate bug activity across scoped runs using `isBugActiveAtRun(...)`, ensuring defects introduced in earlier runs remain active in point-scoped widgets until removed.
- **Safety Net Added:**
  Added unit test `testTextWithSmallTrendWidget_PointScopeAlignsWithTrendMode` in `WidgetAggregationServiceTest.java` verifying that in `BY_RUNS` mode with 118 test variations at 86% and a 2-test final run at 50%, the widget's primary and secondary text display `"50% PASS"` and `"2 Tests (Run #run_20261105_113326)"`, matching the sparkline's ending value.
