# [DEF-20261023-1005] Batch Latest Status Pie Chart Clobbered by Historical Variations Panel

- **Status:** `Resolved`
- **Opened:** 2026-10-23 10:05
- **Closed:** 2026-10-23 10:16
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** Setting date cutoffs (e.g., "Exclude older than: 10/20/2026") or excluding runs in Contributing Runs list updates the contributing runs list, but the Latest Test Status pie chart, center pass rate %, and header badges remain stuck showing the entire historical test catalog (e.g., "26 Tests across 2 Runs", 38% Pass) rather than the actual tests executed across the filtered contributing runs.
- **Root Cause:** 
  1. `bindGlobalListeners()` in `report-manager.js` ran `recalculateBatchLatestStatusPie()` unconditionally on page load and HTMX swap, reading from `#batchTestBaseVariationsPanel .tb-entry-row` which contains the historical catalog of variations (all 26 tests across 343 runs) rather than the latest test executions.
  2. The Latest Test Status component lacked a dedicated DOM container of actual latest test execution entries, causing client-side recalculation to overwrite the server's calculations with the full historical catalog.
  3. `AuraReportViewController` passed un-excluded `runs` to `getBatchTestBaseClasses` instead of `eligibleRuns`.
  4. Browser and Locale ratio card header badges in `batch-history.html` referenced `totalBatchVariationsCount` instead of `latestExecResult.totalVariationsCount`.
  5. The external application runner (`run-aura.sh`) loads `aura-manager-1.0.0-SNAPSHOT.jar` from `~/.m2/repository`, which was not updated via `mvn install` during the earlier run.
- **Detection Gap ("What did we miss?"):** Controller tests validated DTO math and template rendering in isolation without asserting HTMX swap client re-calculation behavior when historical variations exceed active contributing run executions.
- **Resolution:**
  1. Created `BatchLatestVariationDto` to carry the actual test variation executions of the latest status (key, status, healed, mode, runId, environment, browser, location) in `BatchLatestExecutionsDto`.
  2. Rendered these latest test execution entries in a hidden container `#batchLatestVariationsData` inside `batch-history.html`.
  3. Updated `recalculateBatchLatestStatusPie()` in `report-manager.js` to source exclusively from `#batchLatestVariationsData`, filtering by active directory dropdowns (Environment, Browser, Locale) and updating both test count badges and ratio headers.
  4. Bound ratio card header badges to `latestExecResult.totalVariationsCount`.
  5. Passed `eligibleRuns` to `dataService.getBatchTestBaseClasses(batchName, eligibleRuns)` in `AuraReportViewController`.
  6. Installed updated `aura-manager-1.0.0-SNAPSHOT.jar` to local repository via `mvn install -DskipTests -pl aura-manager`.
- **Safety Net Added:**
  - `AuraReportViewControllerTest.testBatchHistoryTemplateRendersWithoutSpelErrorsWhenAiPassedTestsPresent` (validates `batchLatestVariationsData` DOM store and `batchBrowserRatioCountBadge`).
  - `AuraReportViewControllerTest.testBatchHistoryWithExclusionsAndDateCutoff` (validates `latestExecResult`, `latestVariations`, and `eligibleRuns` propagation).
  - `TestBaseVariationHistoryTest.testBatchLatestExecutionsPopulatesLatestVariationsWithEnvironmentAndStatus` (validates `BatchLatestVariationDto` field extraction).
