# [DEF-20261111-2145] Queue Test Run Count Desync, Refresh Batch Flash, and Suite Tag Resolution

- **Status:** `Resolved`
- **Opened:** 2026-11-11 21:45
- **Closed:** 2026-11-11 22:45
- **Component:** `neodymium-core` / `aura-manager`
- **Scope:** `Framework`
- **Symptom:**
  1. When running tests from Aura Manager queue, the test run counts (`executedCount`, `passed`, `failed`, `skipped`) and progress text ("X of Y tests") on the active run context card and runner badge (`X/Y`) did not update as individual variations/datasets completed, remaining frozen until the entire batch subprocess terminated.
  2. Clicking the Refresh button on the report page or reloading caused the word `"Queue"` to briefly flash on the active run context card before disappearing.
  3. Specifying a `"Suite"` or `"Batch"` tag (e.g. `Suite: CheckoutRegression`) on an Aura Manager queue run or console run did not display its value on the active run context card; the card either showed nothing or defaulted to placeholder text.
- **Root Cause:**
  1. **Test Run Count Stalling:** Maven Surefire aggregates and prints `Tests run: X, Failures: Y, ...` once per test class JVM termination, not per dataset/variation. While `ingestBatchExecutionFiles()` already ingested each completed execution JSON into the database in real time during the mid-run scan, `queueService.globalTestsRun` was only updated when `STATS_PATTERN` matched Surefire stdout at process exit. Furthermore, `AuraTestQueueController.getStatusStream()` (`/api/status`) and `AuraGlobalModelAdvice` checked `queueService.getGlobalTestsRun()` exclusively whenever `isQueueRunning == true`, completely ignoring real-time database counts from `TestRunEntity`.
  2. **"Queue" Flash on Refresh:** `AuraQueueService.java` defaulted `currentBatchName` to `"Queue"` when `req.batchName` was null/empty. That string was saved to `batch.json` and persisted on `TestRunEntity.batchName`. In `AuraReportViewController.java:runReport()`, `model.addAttribute("activeBatchName", effectiveBatchName)` unconditionally assigned `report.getBatchName()` (`"Queue"`). On clicking `/runs/refresh`, `runReport()` returned an HTMX OOB swap rendering `<div id="sidebarActiveBatchName">Queue</div>`. Moments later, client-side JS polled `/api/status`, which sanitized the batch name with `AuraReportDataService.isRealBatch()`, hiding the element and creating a visible flicker.
  3. **Suite Tag Value Ignored:** `req.batchName` had no tag fallback for `"Suite"` or `"Batch"` tags, defaulting to `"Queue"` (or blank). For console/remote runs, `TestRunEntity.getBatchName()` was `"Default"`, and neither `AuraGlobalModelAdvice` nor `AuraTestQueueController` checked `activeEntity.getTags()` for user-defined `"Suite"` or `"Batch"` tags.
- **Detection Gap ("What did we miss?"):**
  Existing queue tests (`AuraQueueServiceTest`) asserted final state after full batch process termination. Advice unit tests used pre-configured non-default batch names or tested when the queue was inactive without testing dataset-level mid-run progression.
- **Resolution:**
  1. Updated `AuraQueueService.java` to resolve `effectiveBatchName` from `req.batchName` or `req.tags` (`"Suite"` / `"Batch"` tag), strictly rejecting synthetic placeholders.
  2. Updated `ingestBatchExecutionFiles()` in `AuraQueueService.java` to immediately increment `globalTestsRun`, `globalPassed`, `globalFailed`, `globalSkipped` in real time as each execution JSON is detected, while tracking `batchIngestedRuns` and `batchReportedSurefireRuns` to prevent double-counting when Surefire prints stdout.
  3. Added `resolveEffectiveBatchName` helper methods in `AuraReportDataService.java` supporting `Collection<TestRunTagEntity>`, `Map<String, String>`, and `Map<String, List<String>>`.
  4. Updated `AuraGlobalModelAdvice.java` and `AuraTestQueueController.java` to look up `activeEntity` in real time for both queue and external runs, merging database counts into `executedCount`, `scheduledCount`, and `progressText`, and resolving `activeBatchName` through `resolveEffectiveBatchName`.
  5. Updated `AuraReportViewController.java:runReport()` to pass `report.getBatchName()` through `AuraReportDataService.resolveEffectiveBatchName()`, ensuring synthetic names like `"Queue"` or `"Default"` are never rendered in the OOB swap.
- **Safety Net Added:**
  - `AuraGlobalModelAdviceTest.testActiveRunContextResolvesSuiteTagWhenBatchNameDefault`: verifies that a run with `batchName = "Default"` and tag `Suite: CheckoutRegression` yields `activeBatchName = "CheckoutRegression"`.
  - `AuraGlobalModelAdviceTest.testQueueExecutionRealtimeProgressCountSync`: verifies that real-time counts from `activeEntity` update `executedCount` and `progressText` during active queue runs.
  - `RunStorageSyncServiceTest.testResolveEffectiveBatchNameFromTagsAndFallbacks`: verifies batch name resolution across single/multi tags, maps, and rejection of synthetic placeholders.
