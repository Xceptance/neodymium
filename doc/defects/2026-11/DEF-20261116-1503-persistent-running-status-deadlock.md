# [DEF-20261116-1503] Persistent Running Status Deadlock in Aura Reporting and Storage Sync

- **Status:** `Resolved`
- **Opened:** 2026-11-16 15:03
- **Closed:** 2026-11-16 16:08
- **Component:** `aura-manager` & `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** Completed or terminated tests and test runs remained stuck in the "RUNNING" / "IN_PROGRESS" state indefinitely in the reporting UI, dashboard banner, and API responses long after the test process had stopped.
- **Root Cause:**
  1. Circular deadlock in `RunStorageSyncService.java` and `AuraReportDataService.java` where `isRunActive` and `isInProgress` checked `runRepository.findById(runId)`'s existing database status (`status.equalsIgnoreCase("IN_PROGRESS")`). Once a run was saved as `IN_PROGRESS`, subsequent syncs and report views treated it as actively running forever, perpetually re-evaluating test executions as `"running"` and resetting `runStatus` to `IN_PROGRESS`.
  2. `AuraGlobalModelAdvice.java` and `AuraTestQueueController.java` flagged `running = true` across the entire web UI whenever any DB entity had status `IN_PROGRESS`, even when the queue service was completely idle and disk was inactive. Furthermore, fallback active run detection scanned all historical runs on disk without short-circuit filtering, inducing high latency.
  3. `LocalRunJsonStorageService.buildRunJsonContent` enriched execution JSON files and wrote them back to disk without restoring original file modification timestamps, resetting `f.lastModified()` to current time on every report access and perpetually satisfying `now - f.lastModified() <= 120_000ms`.
  4. `InteractiveConsoleEngine.reconcileTestOutcome` only updated status when `wasFailed` or `wasPassed` was already set, ignoring tests where `currentStatus` was `"running"`, in-progress, or blank.
  5. Aborting an active run via "Stop Execution" left disk files in a recent timestamp window, causing `isRunCurrentlyActiveOnDisk()` to falsely keep the UI banner in "RUNNING" for up to 120 seconds post-abortion.
- **Detection Gap ("What did we miss?"):** Unit tests verified live run buffering and completed test executions independently with mocked static statuses, but lacked an end-to-end integration scenario where an execution process stopped while in the `running` state and was subsequently synced by `RunStorageSyncService`.
- **Resolution:**
  - Removed circular `status == IN_PROGRESS` database check in `RunStorageSyncService.java`, `AuraReportDataService.java`, `AuraGlobalModelAdvice.java`, and `AuraTestQueueController.java`.
  - Optimized active run lookup in `AuraGlobalModelAdvice` and `AuraTestQueueController` with short-circuit filtering (`IN_PROGRESS && isRunCurrentlyActiveOnDisk`, then `.limit(5)` fallback), eliminating unbounded disk I/O scans on idle requests.
  - Handled manual abortion (`queueService.isManuallyStopped()`): immediately suppressed `running = true` and invoked `reportDataService.cancelRun(curRunId)` to transition the entity to `CANCELLED` and clear running counters.
  - Added self-healing in `AuraReportDataService.java` so viewing a finished run whose DB record was left as `IN_PROGRESS` transitions it to `COMPLETED` in the database, recalculating pass rates and syncing healed counts, browser, and locale metrics.
  - Preserved original file `lastModified` timestamps in `LocalRunJsonStorageService.java` during execution JSON metadata enrichment.
  - Updated `InteractiveConsoleEngine.reconcileTestOutcome` to cleanly resolve in-progress states (`running`, `in_progress`, `executing`, `pending`, and blank) to `passed` or `failed` upon test completion.
- **Safety Net Added:**
  - `RunStorageSyncServiceTest#testStaleInProgressRunTransitionsToCompletedWhenProcessNotActive`
  - `InteractiveConsoleEngineTest#testReconcileTestOutcomeTransitionsRunningStatus`
  - `AuraTestQueueControllerTest#testManuallyStoppedSuppressesRunningInStatusStream`
  - `AuraTestQueueControllerTest#testStopExecutionCancelsActiveRun`
