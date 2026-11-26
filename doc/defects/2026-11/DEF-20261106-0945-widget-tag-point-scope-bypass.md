# [DEF-20261106-0945] Tag and Browser Widgets in Non-Latest Point Scope Return Batch-Wide Totals

- **Status:** `Resolved`
- **Opened:** 2026-11-06 09:45
- **Closed:** 2026-11-06 09:53
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** When a dashboard widget with metric `TAG` (such as `browser`, `area`, `locale`, `environment`, or custom tags) is configured with a non-latest point scope (e.g. `LAST_RUN`, `TODAY`, `LAST_HOUR`, `LAST_MONTH`), the widget displays counts and breakdown slices for the entire historical test suite (e.g. 118 items) rather than the scoped test run(s) (e.g. 2 items). In contrast, the status widget for the same run correctly displays 2 tests.
- **Root Cause:** `resolveTagRatios()` in `WidgetAggregationService` unconditionally returned the batch-level pre-aggregated `browserRatios` (and matching `tagRatioCards`) whenever `tagKey` equaled `"browser"` or existed in `tagRatioCards`, bypassing the scoped `targetVariations` computed for the widget's `pointScope`. Additionally, fallback variation tag extraction called `v.getTagValue()` (custom tag map only) instead of `extractVariationTagValue()` which handles built-in dimensions (`browser`, `area`, `location`/`locale`, `environment`/`env`). Furthermore, `summaryLabel` for `TAG` widgets did not format the run ID when `scope == LAST_RUN`.
- **Detection Gap ("What did we miss?"):** Existing unit tests for `WidgetMetric.TAG` in `WidgetAggregationServiceTest` only tested the default `LATEST_TEST_STATUS` point scope with pre-aggregated batch ratios, leaving non-default point scopes (`LAST_RUN`, `TODAY`, `LAST_HOUR`, `LAST_MONTH`) on `PIE_CHART`, `RATIO_BAR`, and `LIST` presentations uncovered.
- **Resolution:**
  1. Updated `buildPieChartWidget` and `buildRatioBarWidget` to suppress batch-level `browserRatios` and `tagRatioCards` when `scope != WidgetPointScope.LATEST_TEST_STATUS`.
  2. Updated `resolveTagRatios` to evaluate tag distributions dynamically from `latestVariations` when present, while preserving palette colors for known values from pre-aggregates and falling back to pre-aggregates only when variations are absent.
  3. Replaced raw `v.getTagValue(tagKey)` in `resolveTagRatios` with `extractVariationTagValue(v, tagKey)` with case-insensitive tag key normalization so built-in dimensions (`browser`, `area`, `locale`, `environment`) as well as custom tags are properly extracted.
  4. Formatted `summaryLabel` for `TAG` widgets to include `(Run #<id>)` when `scope == WidgetPointScope.LAST_RUN` for visual parity with `STATUS`.
  5. Scoped `buildListWidget` variation iterations by `scopedRunIds` when `scope != WidgetPointScope.LATEST_TEST_STATUS`.
- **Safety Net Added:** Added automated regression unit tests in `WidgetAggregationServiceTest`:
  - `testPieChart_TagMetric_Browser_LastRunScope_DoesNotReturnBatchWideRatios`: Verifies a Last Run browser pie chart only counts the 2 tests from the latest run and assigns 100% to `Chrome_1920x1080` instead of returning 118 items from batch history.
  - `testRatioBar_TagMetric_Browser_LastRunScope`: Verifies ratio bar presentation scopes browser segments to the last run.
  - `testListWidget_TagMetric_Browser_LastRunScope`: Verifies list widget sub-items only display executions from the scoped run.
  - `testPieChart_TagMetric_Area_LastRunScope`: Verifies built-in area dimension evaluation scopes correctly to the last run.
