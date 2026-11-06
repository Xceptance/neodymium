# [DEF-20261105-1445] Text with Trend Widget By Runs Disconnects From Last Run KPI

- **Status:** `Resolved`
- **Opened:** 2026-11-05 14:45
- **Closed:** 2026-11-05 14:55
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In a "Status with Trend (By Runs)" (`TEXT_WITH_SMALL_TREND`) dashboard widget, the inline sparkline chart plots individual historical run outcomes and drops to the terminal point of the latest run (e.g. 50% PASS, 2 Tests), but the prominent KPI text and subtitle directly above the sparkline display the cumulative test base status across all known test variations (e.g. 97% PASS, 118 Tests).
- **Root Cause:** `WidgetAggregationService.buildTextWithSmallTrendWidget` delegated the KPI text card evaluation to `buildTextWidget` using the widget configuration directly. In `widget-configurator-modal.html`, selecting `TEXT_WITH_SMALL_TREND` concealed the **Target Point / Time Scope** selector (`pointScope`), which caused `config.getPointScope()` to always default to `WidgetPointScope.LATEST_TEST_STATUS`. Consequently, `buildTextWidget` evaluated the entire test base variation snapshot (`latestVariations`, 118 tests) instead of scoping to the last run (`LAST_RUN`, 2 tests), creating a direct semantic mismatch between the sparkline endpoint and the KPI text.
- **Detection Gap ("What did we miss?"):** Existing unit tests for `TEXT_WITH_SMALL_TREND` (`testTextWithSmallTrendWidget`) validated sparkline non-emptiness and point array lengths, but never asserted that `primaryTextValue` and `secondaryTextValue` align with the sparkline's terminal run point when `trendMode == BY_RUNS`.
- **Resolution:**
  1. Added immutable copy method `withPointScope(WidgetPointScope)` to `DashboardWidgetConfigDto`.
  2. In `WidgetAggregationService.buildTextWithSmallTrendWidget`, when `metric == WidgetMetric.STATUS` and `trendMode == WidgetTrendMode.BY_RUNS` with default/unspecified `pointScope`, adapt the effective configuration passed to `buildTextWidget` to use `WidgetPointScope.LAST_RUN`.
  3. When `trendMode == WidgetTrendMode.LATEST_APPROACH`, preserved `WidgetPointScope.LATEST_TEST_STATUS` so that Suite Evolution sparklines and text continue to evaluate the entire test base.
- **Safety Net Added:** Added automated unit tests in `WidgetAggregationServiceTest`:
  - `testTextWithSmallTrendWidget_ByRuns_EvaluatesLastRunKpi`: Verifies that for `BY_RUNS`, the primary KPI reflects the last run's pass rate (e.g. `50% PASS`) and subtitle `2 Tests (Run #...)`.
  - `testTextWithSmallTrendWidget_LatestApproach_PreservesLatestTestStatus`: Verifies that for `LATEST_APPROACH`, the primary KPI preserves `97% PASS` and `118 Tests`.
  - Added `testWithPointScope` in `DashboardWidgetConfigDtoTest` verifying copy immutability.
