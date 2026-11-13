# [DEF-20261111-0947] External Run Sidebar Reaction Stalling and Synthetic Batch Name Leakage

- **Status:** `Resolved`
- **Opened:** 2026-11-11 09:47
- **Closed:** 2026-11-11 10:15
- **Component:** `neodymium-core`, `aura-manager`
- **Scope:** `Framework & Test/Harness`
- **Symptom:** When a test run was initiated from the console or an external process, the "Active Run Context" sidebar card did not update or display the active run, despite the run appearing in the runs table. Additionally, the card displayed the redundant text prefix "Run " before the run ID, and synthetic batch placeholders such as "Default" or "Suite" were shown as batch names and tagged onto runs without explicit batch definitions.
- **Root Cause:**
  1. `AuraQueueService.currentRunId` and `currentBatchName` were not cleared in the `finally` block of `runQueue()`, leaving stale completed queue runs in memory after queue completion.
  2. `AuraGlobalModelAdvice` and `AuraTestQueueController` checked `queueService.getCurrentRunId()` before checking for `IN_PROGRESS` database runs. Because `queueService.getCurrentRunId()` was non-null, external runs from `reportDataService.getAllRuns()` / `runRepository` were skipped.
  3. `AuraTestDashboardController.renderDashboard()` unconditionally overwritten `model.addAttribute("activeRunId", ...)` with `queueController.getCurrentRunId()`, clobbering the active console run computed by `AuraGlobalModelAdvice`.
  4. In `dashboard-runner.js`, `sidebarBadge.innerHTML` accessed `statusData.tests.length` without checking if `tests` was defined, throwing a `TypeError` on console runs (which do not emit an array of tests in `statusObj`) and halting subsequent UI polling updates.
  5. `dashboard-core.js` only started `startPolling()` if `auraTestManagerWorkspace` or `dashboardView` existed on the page, leaving `/dashboard-pages/*` views dormant.
  6. `sidebar.html`, `dashboard-core.js`, and `dashboard-runner.js` hardcoded `'Run #' + runId`, and lacked suppression for synthetic fallback batch names (`"Default"`, `"Suite"`, `"Queue"`, `"Unknown"`, `"ALL"`, `"N/A"`).
- **Detection Gap ("What did we miss?"):** Prior unit tests for `AuraGlobalModelAdviceTest` and `AuraQueueServiceTest` tested queue runs and idle states in isolation, but did not test an external console run occurring while the queue service held leftover state from a previously executed queue run. Furthermore, `dashboard-runner.js` polling error recovery swallowed the `TypeError` in `statusData.tests.length`, hiding the failure during manual browser testing.
- **Resolution:**
  1. Updated `AuraQueueService.runQueue()` `finally` block to reset `currentRunId` and `currentBatchName` to `""`.
  2. Updated `AuraGlobalModelAdvice` and `AuraTestQueueController.getStatusStream` to only consult `queueService` when `queueService.isRunningQueue()` is true; when idle, any `IN_PROGRESS` run from the database or storage takes precedence.
  3. In `AuraTestDashboardController.renderDashboard()`, guarded `activeRunId` population so it does not overwrite values already assigned by `AuraGlobalModelAdvice`.
  4. In `AuraReportDataService`, added `isRealBatch()` to identify non-synthetic batch names, updated `isLegitimateTag()` to reject synthetic suite/batch tags, and updated `populateRunTags()` to only attach `Suite` tags when `isRealBatch(batchName)` is true.
  5. In `sidebar.html`, `dashboard-core.js`, and `dashboard-runner.js`, removed `"Run "` prefix, rendering `#${runId}`, and conditionally hid the batch name element when empty or matching synthetic placeholders.
  6. In `dashboard-runner.js`, safely resolved `testsCount` for `sidebarRunBadge` via `(statusData.tests ? statusData.tests.length : (statusData.scheduledCount || statusData.total || 0))` preventing `TypeError`.
  7. In `dashboard-core.js:init()`, ensured `startPolling()` is invoked on any page displaying `sidebarContextCard`.
- **Safety Net Added:**
  - `AuraGlobalModelAdviceTest.testExternalRunContextWithStaleQueueStateAndDefaultBatchSuppression` verifying priority of active external database runs over idle queue service state and verifying `"Default"` batch name suppression.
  - `AuraGlobalModelAdviceTest.testAddGlobalAttributesWhenNoRunActive` asserting `activeBatchName` defaults to `""`.
  - `RunStorageSyncServiceTest.testPopulateRunTagsSuppressesSyntheticBatchNames` verifying that runs with `"Default"`, `"Suite"`, or `"Queue"` batch names do not receive synthetic `Suite` tags and that `isRealBatch` / `isLegitimateTag` reject placeholders.
