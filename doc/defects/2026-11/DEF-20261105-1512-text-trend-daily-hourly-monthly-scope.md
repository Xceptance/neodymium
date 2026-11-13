# [DEF-20261105-1512] Text with Trend Widget Daily, Hourly, and Monthly Trend Modes Disconnect From Periodic KPI Scope

- **Status:** `Resolved`
- **Opened:** 2026-11-05 15:05
- **Closed:** 2026-11-05 15:20
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In a "Status with Trend (Daily)" (`TEXT_WITH_SMALL_TREND` with `WidgetTrendMode.DAILY`) dashboard widget, the sparkline plots daily aggregated pass rate buckets with the terminal point falling to the day's pass rate (e.g. 50% PASS, 4 Tests (2 Runs)), but the primary KPI and secondary count above the sparkline display the cumulative suite evolution status across all historical test variations (e.g. 97% PASS, 118 Tests). Similar disconnects exist for `HOURLY` and `MONTHLY` trend modes.
- **Root Cause:** When configuring a `TEXT_WITH_SMALL_TREND` widget in `widget-configurator-modal.html`, the `pointScope` field is hidden from the UI, defaulting `config.getPointScope()` to `LATEST_TEST_STATUS`. While `WidgetTrendMode.BY_RUNS` was mapped to `WidgetPointScope.LAST_RUN`, `DAILY`, `HOURLY`, and `MONTHLY` were unhandled in `WidgetAggregationService.buildTextWithSmallTrendWidget`. As a result, `buildTextWidget` evaluated all 118 test variations instead of the corresponding temporal window (`TODAY`, `LAST_HOUR`, or `LAST_MONTH`). Furthermore, for historical datasets where no runs occurred in the current real-world wall-clock window, `buildTextWidget` would evaluate to `0 Tests` unless a graceful fallback to `LAST_RUN` was provided.
- **Detection Gap ("What did we miss?"):** Previous fix DEF-20261105-1445 specifically targeted `WidgetTrendMode.BY_RUNS` without extending temporal symmetry to periodic aggregation modes (`DAILY`, `HOURLY`, `MONTHLY`). Existing tests did not assert KPI text values for periodic trend modes.
- **Resolution:**
  1. In `WidgetAggregationService.buildTextWithSmallTrendWidget`, extended automatic point scope adaptation for `metric == WidgetMetric.STATUS` when `pointScope` is default/unspecified:
     - `WidgetTrendMode.DAILY` -> `WidgetPointScope.TODAY`
     - `WidgetTrendMode.HOURLY` -> `WidgetPointScope.LAST_HOUR`
     - `WidgetTrendMode.MONTHLY` -> `WidgetPointScope.LAST_MONTH`
  2. Implemented graceful historical fallback: if the adapted temporal scope yields empty data (e.g., historical datasets or test fixtures where no runs occurred during the current wall-clock window) and runs are present, `buildTextWithSmallTrendWidget` falls back to `WidgetPointScope.LAST_RUN`, guaranteeing that the KPI evaluates the latest available run rather than reporting an artificial "0 Tests".
  3. Preserved strict wall-clock filtering in `filterRunsByPointScope` to ensure standalone explicit point scope widgets (e.g. pie charts targeting `LAST_HOUR`) accurately report empty state when no runs match the window.
- **Safety Net Added:** Added automated regression tests in `WidgetAggregationServiceTest`:
  - `testTextWithSmallTrendWidget_Daily_EvaluatesTodayRunsKpi`: Verifies that `DAILY` evaluates today's runs (`50% PASS`, `4 Tests (2 Runs)`) rather than all 118 test variations.
  - `testTextWithSmallTrendWidget_Daily_HistoricalFallback`: Verifies that `DAILY` on past runs falls back to `LAST_RUN` (`50% PASS`, `2 Tests (Run #run-past-2)`).
  - `testTextWithSmallTrendWidget_HourlyAndMonthly_EvaluatesScopedRunsKpi`: Verifies that `HOURLY` and `MONTHLY` evaluate their respective scoped runs (`50% PASS`, `4 Tests (2 Runs)`).
