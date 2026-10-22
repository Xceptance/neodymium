# [DEF-20261021-1240] Static "Default" Batch Name in Run Report Breadcrumbs and Active Run Context Card

- **Status:** `Resolved`
- **Opened:** 2026-10-21 12:40
- **Closed:** 2026-10-21 12:48
- **Component:** `neodymium-core`, `aura-manager`
- **Scope:** `Framework`
- **Symptom:** When executing or viewing a run report for a custom batch (such as "Wikipedia"), the report page header, breadcrumbs (/ Known Batches / Default / Run #...), and sidebar "Active Run Context" card display "Default" instead of the actual batch name.
- **Root Cause:** 
  1. `AuraQueueService` maintained `effectiveBatchName` purely as a local thread execution variable, omitting an atomic reference and accessor for the active/recent batch.
  2. `AuraGlobalModelAdvice` and `/api/status` omitted `activeBatchName` / `batchName` in model advice and status JSON payloads.
  3. `run-report.html` lacked a `data-batch-name` attribute, and `report-manager.js` stamped an uninitialized global variable (`activeBatchName = 'Default'`) over the page title and breadcrumbs.
  4. `dashboard-runner.js` polling loop never synchronized `sidebarActiveBatchName`.
- **Detection Gap ("What did we miss?"):** Prior automated tests verified batch metadata persistence into `batch.json` and database entities directly, but did not assert the `/api/status` JSON contract or the Thymeleaf model/DOM data-attribute propagation to the view layer.
- **Resolution:**
  1. Added `currentBatchName` atomic reference and `getCurrentBatchName()` getter to `AuraQueueService`, updated during `runQueue()`.
  2. Exposed `activeBatchName` in `AuraGlobalModelAdvice` and added `"batchName"` to the `/api/status` payload in `AuraTestQueueController`.
  3. Bound `batchName` and `activeBatchName` in `AuraReportViewController.runReport()`, updated `pageTitle` with dynamic batch name, and added `th:data-batch-name="${report?.batchName ?: (batchName ?: 'Default')}"` to `run-report.html`.
  4. Updated `report-manager.js` to extract the dynamic batch name from the DOM attribute before updating breadcrumbs and titles.
  5. Updated `dashboard-runner.js` to synchronize `sidebarActiveBatchName` during `/api/status` polling and immediate batch execution triggers.
- **Safety Net Added:** Unit tests in `AuraTestQueueControllerTest.testGetStatusStreamIncludesBatchName` verifying `/api/status` returns the dynamic batch name, `AuraGlobalModelAdviceTest.testAddGlobalAttributesWhenRunActiveWithBatchName` verifying `activeBatchName` is populated into the global view model, `AuraReportViewControllerTest.testRunReportPreservesSubTab` verifying `batchName`, `activeBatchName`, and `pageTitle` in the run report view, and `AuraQueueServiceTest.testGetCurrentBatchNameDefaultAndSet`.
