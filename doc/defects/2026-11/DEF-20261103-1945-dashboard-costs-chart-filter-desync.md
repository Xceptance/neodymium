# [DEF-20261103-1945] Dashboard Costs Pie Chart Filter Desynchronization and Hardcoded Filter Mockup Leak

- **Status:** `Resolved`
- **Opened:** 2026-11-03 19:45
- **Closed:** 2026-11-03 19:47
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:**
  1. On custom dashboard pages containing a linked widget group (e.g., Status Pie, Ratio Bars, Bugs, and Costs Pie Chart), the Costs Pie Chart displayed 2,772,000 tokens ($1.37) on initial server-side render, jumped to 68,000 tokens ($0.03) when interactive filtering was applied (e.g., clicking "Passed"), and recalculating to 92,400 tokens ($0.05) when the filter was cleared client-side.
  2. Reloading the page or performing a full server render jumped the metrics back up to 2,772,000 tokens ($1.37).
  3. In certain states or DOM re-renders, a static filter badge `Filter: Passed` with a `Clear all` button was visible in the container header while sibling widgets displayed unfiltered totals (232 tests, 1 bug).
  4. Server-rendered token metrics and breakdown slice counts were displayed as unformatted numbers without thousands grouping commas (e.g. `2772000 tokens`, `1940399 (70.0%)`), whereas client-side JavaScript formatted values with locale grouping.
- **Root Cause:**
  1. In `WidgetAggregationService` (`buildPieChartWidget`, `buildRatioBarWidget`, and `buildListWidget`), when `config.getMetric() == WidgetMetric.COSTS` with `WidgetPointScope.LATEST_TEST_STATUS`, metrics were read from `tokenCostTrend.getCurrentCost()` and `tokenCostTrend.getTotalTokens()`. In `AuraReportDataService`, `tokenCostTrend` aggregates all test runs across the latest calendar day bucket (in the test dataset: 60 runs across 6 suites = 2,772,000 tokens and $1.37). Sibling widgets in the same container evaluated their metrics strictly over `latestVariations` (the 232 active latest test variations = 92,400 tokens and $0.05). When filtering, client-side JS (`recalculateCostsPieChart`) correctly summed `v.c` and `v.tok` only over the 224 passed variations (68,000 tokens / $0.03), exposing the massive disconnect between server render and client calculation.
  2. In `dashboard-widgets.html` lines 91–100, the container filter strip statically contained a hardcoded mockup chip: `<span class="tag-container-filter ..."><span>Filter: <strong class="filter-val-text">Passed</strong></span>...</span>`. Whenever `.container-active-filter-strip` was displayed or styled before `renderContainerFilterStrip()` dynamically injected chips, this dummy chip was displayed.
  3. `summaryLabel = tokens + " tokens";` and `WidgetPieSliceDto.count` in `dashboard-widgets.html` rendered raw integer strings without thousands grouping separators.
- **Detection Gap ("What did we miss?"):**
  - Existing unit tests in `WidgetAggregationServiceTest` tested `COSTS` metric aggregation only with `WidgetPointScope.LAST_RUN`, leaving `WidgetPointScope.LATEST_TEST_STATUS` untested in conjunction with `latestVariations`.
  - Static HTML template tests did not assert that dynamic container filter chips are initially empty.
- **Resolution:**
  1. Updated `WidgetAggregationService.java` in `buildPieChartWidget()`, `buildRatioBarWidget()`, and `buildListWidget()`: when `config.getMetric() == WidgetMetric.COSTS` and `scope == WidgetPointScope.LATEST_TEST_STATUS`, compute cost and tokens directly by summing `v.getLlmCost()` and `v.getLlmTokens()` over `latestVariations`. Fall back to `tokenCostTrend` only if `latestVariations` is null or empty.
  2. Formatted token summary labels with thousands grouping commas via `String.format(Locale.ROOT, "%,d tokens", tokens)`.
  3. Added `getFormattedCount()` to `WidgetPieSliceDto` and `DistributionRatioDto` to format slice and segment counts with thousands grouping commas (`String.format(Locale.ROOT, "%,d", count)`), and updated `dashboard-widgets.html` breakdown and ratio views to use `formattedCount`.
  4. Removed the hardcoded dummy filter chip from `dashboard-widgets.html` lines 91–100, ensuring `.container-filter-chips` starts completely empty and `.btn-clear-all-container-filters` is initially hidden.
- **Safety Net Added:**
  - Added unit test `WidgetAggregationServiceTest.testPieChartCostsWithLatestTestStatusScope` verifying that `latestVariations` sums take precedence over whole-day `tokenCostTrend` and format tokens with comma grouping.
  - Added unit test `WidgetAggregationServiceTest.testRatioBarCostsWithLatestTestStatusScope` verifying ratio bar cost summation over `latestVariations`.
  - Added unit test `WidgetAggregationServiceTest.testListCostsWithLatestTestStatusScope` verifying list widget cost and token summation over `latestVariations`.
  - Updated `WidgetAggregationServiceTest.testPieChartCostsWithLastRunScope` to assert formatted summary labels and slice counts.
