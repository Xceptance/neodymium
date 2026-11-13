# [DEF-20261023-0950] Batch History Filters and Exclusions Disconnected from Latest Status Donut Chart

- **Status:** `Resolved`
- **Opened:** 2026-10-23 09:50
- **Closed:** 2026-10-23 09:52
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** Selecting batch run limit presets (e.g. Last 5, Last 10), toggling Directory Filters (Environment, Locales, Browsers), or excluding all contributing runs fails to update the "Latest Test Status" donut pie chart, center KPI pass rate, breakdown list, or causes the entire top overview card (and Reset button) to disappear from the DOM.
- **Root Cause:** 
  1. `AuraReportViewController.batchHistory` passed `allRuns` instead of limited `runs` to `getBatchLatestTestExecutionsSummary` and `getBatchTestBaseClasses`.
  2. `batch-history.html` guarded `.batch-top-overview-panel` with `th:if="${batchSummary != null and batchSummary.hasLatestRun()}"`, which collapses when all runs are excluded (`latestTotalTests == 0`), hiding the cutoff filter and Reset button.
  3. `report-manager.js:applyBatchDirectoryFilters()` filtered the table rows and trend charts but lacked live recalculation of the SVG donut sub-arcs, center KPI rate, test count badge, and contributing runs list.
  4. `TestBaseVariationEntity` and `.tb-entry-row` lacked `lastRunId` and `lastEnvironment` attributes needed for accurate client-side variation-to-run correlation.
- **Detection Gap ("What did we miss?"):** Previous controller unit tests verified pagination of the run table independently from `getBatchLatestTestExecutionsSummary`, and client-side directory filter tests only checked row display styles without validating SVG donut sub-arcs or top card summary updates.
- **Resolution:** 
  1. Updated `AuraReportViewController.batchHistory` to pass the effective limited `runs` list to `getBatchLatestTestExecutionsSummary` and `getBatchTestBaseClasses`.
  2. Updated `batch-history.html` to guard `.batch-top-overview-panel` with `th:if="${batchSummary != null}"`, gracefully handling `latestTotalTests == 0` with 0% empty donut and keeping the Reset button accessible.
  3. Added `lastRunId` and `lastEnvironment` fields to `TestBaseVariationEntity`, populated them in `AuraReportDataService.getBatchTestBaseClasses`, and exposed them as DOM attributes (`data-run-id`, `data-env`) on `.tb-entry-row`.
  4. Implemented `recalculateBatchLatestStatusPie()` in `report-manager.js` to dynamically synchronize SVG donut sub-arcs, center KPI rate, breakdown counters, top test count badge, and contributing runs list on filter changes and view load.
- **Safety Net Added:** 
  - `AuraReportViewControllerTest.testBatchHistoryWithLimit`
  - `AuraReportViewControllerTest.testBatchHistoryRenderingFragmentWithEmptySummary`
  - `TestBaseVariationHistoryTest.testBatchTestBaseClassesSetsLastRunIdAndEnvironment`
