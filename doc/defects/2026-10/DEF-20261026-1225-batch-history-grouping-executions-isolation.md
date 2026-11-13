# [DEF-20261026-1225] Batch History Grouping Executions Isolation

- **Status:** `Resolved`
- **Opened:** 2026-10-26 12:25
- **Closed:** 2026-10-26 12:35
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In the grouped history overview (e.g. `/batch-history?groupType=area&groupValue=@Search`), contributing runs in `.contributing-runs-list`, the runs history table (`#batchRunsHistoryTableBody`), and the main history trend SVG (`#mainTrendSvg`) displayed whole-run totals across all areas (e.g. 7 executions including 5 from `@Browsing`) rather than isolating executions matching the active grouping feature (`@Search`). Non-selected area trends were also included in `areaTrends`.
- **Root Cause:**
  1. In `AuraReportViewController.batchHistory(...)`, grouping dimension criteria (e.g. `groupType=area&groupValue=@Search`) were stored in `groupDimensions` and `parts` but never merged into the effective criteria sets (`effectiveRunBrowsers`, `effectiveRunLocales`, `effectiveRunEnvs`, `effectiveRunAreas`, `effectiveRunTags`) passed into `dataService.getBatchLatestTestExecutionsSummary(...)` and `dataService.getBatchTestBaseClasses(...)`.
  2. In `AuraReportDataService.getBatchLatestTestExecutionsSummary(...)`, `contributingRuns` was populated using the raw database `TestRunEntity` instances from `runRepository`, which contained run-wide metrics across all test classes, areas, browsers, and locales.
  3. In `AuraReportViewController`, `runs` passed to the model and rendered in `#batchRunsHistoryTableBody` was not scoped to the active grouping criteria, causing the table and the client-side dynamic trend chart (`renderDynamicTrendChart`) to render whole-run numbers. Furthermore, `areaPointsMap` accumulated data points across all areas present in the runs without filtering by the active area dimension.
- **Detection Gap ("What did we miss?"):** Existing controller tests verified that `allRuns` and `runs` contained the runs that had at least one matching execution for the group dimension, but did not assert that the execution counts inside each run and contributing run were isolated to only the selected grouping feature.
- **Resolution:**
  1. In `AuraReportDataService`, added `scopeRunToCriteria(...)` to construct transient in-memory scoped copies of `TestRunEntity` that isolate execution counts (`passedCount`, `succeededFixedCount`, `failedKnownCount`, `failedUnknownCount`, `ignoredCount`, `totalTests`, `passRate`, `browsersCsv`, `localesCsv`) to only executions matching the criteria, without mutating database entities.
  2. In `AuraReportDataService.getBatchLatestTestExecutionsSummary(...)`, introduced `ScopedRunStats` aggregation per contributing run ID during latest variation processing, emitting scoped `TestRunEntity` items in `contributingRuns`.
  3. In `AuraReportViewController.batchHistory(...)`, merged `groupDimensions` and `parts` into `effectiveRun*` filter sets, filtered `areaPointsMap` so that `areaTrends` only includes the active area when area grouping is active, passed effective criteria to data service methods, and scoped `displayedRuns` for `#batchRunsHistoryTableBody` and `#mainTrendSvg`.
- **Safety Net Added:**
  1. `com.xceptance.aura.report.service.TestBaseVariationHistoryTest#testBatchLatestContributingRunsScopedToAreaGrouping`
  2. `com.xceptance.aura.report.controller.AuraReportViewControllerTest#testBatchHistoryAreaGroupingExecutionsIsolation`
