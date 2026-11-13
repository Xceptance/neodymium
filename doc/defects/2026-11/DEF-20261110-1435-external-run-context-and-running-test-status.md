# [DEF-20261110-1435] External Run Context Disconnection and Premature Running Test Status in Widgets and Tables

- **Status:** `Resolved`
- **Opened:** 2026-11-10 14:35
- **Closed:** 2026-11-10 15:05
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:**
  1. The active run context widget on the side panel (`#sidebarContextCard`) was not updated when test execution was triggered outside Aura Manager (e.g., via Maven CLI `mvn test` or IDE), even though the run was detected and ingested into the runs history table.
  2. Tests that had just started (and thus had an empty or in-progress status) were prematurely displayed in dashboard widgets as passed (`passed-clean`), skewing pass rate and distribution metrics.
  3. In the runs history table status breakdown bar, these just-started tests were displayed as failed (`failed-unknown`), showing a 0% pass rate and a red bar segment before test completion.
  4. The active run context widget provided no feedback on how many tests had completed or were currently running/scheduled.
- **Root Cause:**
  1. `AuraGlobalModelAdvice` and `AuraTestQueueController` exclusively inspected `AuraQueueService.getCurrentRunId()` and `isRunningQueue()`. For runs triggered externally, `AuraQueueService` remained idle, leaving `activeRunId = ""` and `hasRunContext = false`.
  2. `TestExecutionDto` and `AuraReportDataService` defaulted missing or blank execution statuses to `"passed-clean"`. Furthermore, `WidgetAggregationService` had fallback clauses across multiple metric calculations (stacked bar, tag stats, status pie, metric breakdown) that mapped any unrecognized non-failure status to `passClean`.
  3. `LocalRunJsonStorageService.buildRunJsonContent` and `RunStorageSyncService.importOrUpdateRunReport` defaulted unassigned execution status to `"failed-unknown"` and incremented `unknown` / `calcUnknown`. Neither tracked a running execution state or marked the parent `TestRunEntity` status as `IN_PROGRESS`.
- **Detection Gap ("What did we miss?"):**
  Existing integration tests for `AuraTestQueueController` and `AuraGlobalModelAdvice` tested queue runs initiated directly via `executeQueue()`, with pre-set `queueService` mocks. No test verified external CLI/IDE test execution ingestion where `AuraQueueService` is idle and `runRepository` holds an active `IN_PROGRESS` entity.
- **Resolution:**
  1. In `TestExecutionDto` and `BatchLatestVariationDto`, added `isFinished()` logic to reliably identify terminal statuses versus `running` / `in_progress`.
  2. Updated `AuraReportDataService` and `WidgetAggregationService` to omit unfinished executions from `variationStatus` and widget aggregation loops so tests only appear in widgets after completion.
  3. Added `runningCount` tracking to `TestRunEntity`, `LocalRunJsonStorageService`, and `RunStorageSyncService`. When running executions are present or candidate files on disk are actively being written, the run entity is assigned status `IN_PROGRESS` and its running segment is tracked via `runningCount` rather than `unknown` failure.
  4. In `AuraGlobalModelAdvice` and `AuraTestQueueController.getStatusStream`, implemented fallback to `reportDataService.getAllRuns()` / `runRepository` when `queueService` is inactive. Added calculation of executed count, scheduled count, and `progressText`.
  5. Updated `sidebar.html`, `dashboard-runner.js`, and `dashboard-core.js` to render `#sidebarActiveProgress` and dynamically reflect executed/running counts.
  6. Added `.status-bar-seg.seg-running` in `report-manager.css`, `batch-history.html`, and `dashboard-widgets.html` to display a blue running segment in status breakdown bars.
- **Safety Net Added:**
  - `AuraGlobalModelAdviceTest.testExternalRunContextFallbackAndProgress`: verifies that an active external run in `TestRunRepository` populates `activeRunId`, `hasRunContext = true`, `activeRunStatus = "running"`, and `activeRunProgress`.
  - `WidgetAggregationServiceTest.testUnfinishedTestsExcludedFromWidgets`: verifies that running/unfinished executions are excluded from widget aggregations.
  - `RunStorageSyncServiceTest.testRunningExecutionClassification`: verifies that running executions increment `runningCount` instead of `failedUnknown` and mark run status as `IN_PROGRESS`.
