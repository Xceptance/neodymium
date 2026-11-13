# [DEF-20261028-1445] Ratio Filter Selection Erroneously Filters History Runs and History Trend Chart

- **Status:** `Resolved`
- **Opened:** 2026-10-28 14:45
- **Closed:** 2026-10-28 14:46
- **Component:** `aura-manager`
- **Scope:** `Framework & Test/Harness`
- **Symptom:** Selecting a segment or legend chip in any ratio card in Batch History (e.g. Browser, Locale, Area, or Dynamic Tag ratio) hides rows in `#batchRunsHistoryTableBody` and recalculates `#mainTrendSvg` and area trend charts, resulting in empty/flat trend charts and an empty runs history table.
- **Root Cause:** `applyBatchDirectoryFilters()` in `report-manager.js` applied `batchActiveRatioFilters` to `#batchRunsHistoryTableBody .run-history-row` and `.area-trend-card`, followed by invoking `renderDynamicTrendChart()` and `renderDynamicAreaTrendCharts()`. Because `renderDynamicTrendChart()` constructs its time-series exclusively from visible run rows (`r.style.display !== 'none'`), filtering historical runs dynamically corrupted or wiped the trend chart.
- **Detection Gap ("What did we miss?"):** Early automated syntax tests (`testBatchHistoryAreaRatioFiltering`, `testDynamicTagRatioBarFilteringSyntax`) actively asserted that ratio filter clicks set `row.style.display = 'none'` on `#batchRunsHistoryTableBody .run-history-row`, codifying the erroneous architectural coupling instead of enforcing separation between the latest-variation ratio inspector and the historical runs timeline.
- **Resolution:**
  1. Removed `#batchRunsHistoryTableBody .run-history-row` filtering from `applyBatchDirectoryFilters()`.
  2. Removed `.area-trend-grid .area-trend-card` filtering from `applyBatchDirectoryFilters()`.
  3. Removed `renderDynamicTrendChart()` and `renderDynamicAreaTrendCharts()` calls from `applyBatchDirectoryFilters()`.
  4. Preserved ratio filtering strictly for `recalculateBatchLatestStatusPie()`, `#batchTestBaseVariationsPanel .tb-entry-row`, variations badge counters, and `syncBatchRatioBarsVisualState()`.
- **Safety Net Added:**
  - `com.xceptance.aura.report.staticassets.StaticResourceSyntaxTest.testBatchHistoryAreaRatioFiltering` (updated to assert runs and trend cards remain unaffected)
  - `com.xceptance.aura.report.staticassets.StaticResourceSyntaxTest.testDynamicTagRatioBarFilteringSyntax` (updated to assert runs remain unaffected)
  - `com.xceptance.aura.report.staticassets.StaticResourceSyntaxTest.testBatchHistoryMultiValueTagRatioFiltering` (updated to assert runs remain unaffected)
  - `com.xceptance.aura.report.staticassets.StaticResourceSyntaxTest.testRatioFilterDecoupledFromRunsAndTrends` (new dedicated regression test)
