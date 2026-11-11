# [DEF-20261110-1605] Report Page Finished Test Status and Phantom Running Count

- **Status:** `Resolved`
- **Opened:** 2026-11-10 16:05
- **Closed:** 2026-11-10 16:18
- **Component:** `neodymium-core`, `aura-manager`
- **Scope:** `Framework`
- **Symptom:**
  1. On the test run report page (`/run-report?runId=...`), completed tests failed to display their passed/failed status badges and remained marked as `RUNNING`, leaving only the last executed test marked with status.
  2. In the runs history table and active run context, phantom running test counts appeared or lingered after batch execution finished.
- **Root Cause:**
  1. `AuraQueueService.updateExecutionSnapshotsAndFallbacks` only updated existing execution snapshots if `isFailedRun || testWasCancelled`. For successful runs, intermediate snapshots written during test execution with `"status": "running"` were never finalized to `"passed"`, causing all but the last test to remain unfinalized on disk.
  2. `AuraReportDataService.getRunReport` checked `liveRunBuffer` only `if (rawExecutions.isEmpty())`. Because `localRunJsonStorageService.buildRunJsonContent` read the intermediate disk snapshots, `rawExecutions` was populated and `liveRunBuffer` was ignored.
  3. `AuraReportDataService.ingestExecution` mapped `"running"` to `"ignored"` and omitted `runningCount` accumulation and `run.setRunningCount(runningCount)`.
  4. `AuraReportDataService.getBatchOverviewData` synchronized counts between `report` and `TestRunEntity` but omitted `runningCount`.
  5. `AuraReportDataService.finishRun` and `cancelRun` never reset `run.setRunningCount(0)` on completion.
- **Detection Gap ("What did we miss?"):** Existing unit tests verified completed static `run.json` files or batch failure paths where execution files were either pre-finalized or triggered failure fallbacks, missing the live transition where intermediate execution snapshots contain `"running"` while live ingestion receives completed status.
- **Resolution:**
  1. Updated `AuraQueueService.updateExecutionSnapshotsAndFallbacks` to finalize any non-final execution status (`!isFinalExecutionStatus(currentStatus)`) or `isZeroStep` to the batch's outcome (`passed`, `failed`, or `cancelled`).
  2. Updated `AuraQueueService.ingestBatchExecutionFiles` to normalize `"finished"` and `"succeeded"` statuses to `"passed"` regardless of `onlyFinalStatus`.
  3. Updated `AuraReportDataService.getRunReport` to merge `liveRunBuffer` into `rawExecutions` (matching by execution ID or variation ID), ensuring live finished executions override intermediate running disk snapshots.
  4. In `AuraReportDataService.ingestExecution`, mapped `"running"` to `"running"`, computed `runningCount`, and set `run.setRunningCount(runningCount)`.
  5. In `AuraReportDataService.finishRun` and `cancelRun`, explicitly reset `run.setRunningCount(0)`.
  6. In `AuraReportDataService.getBatchOverviewData`, synchronized `r.setRunningCount(report.getRunningCount())`.
  7. In `AuraReportDataService.buildRunReportDto`, added fallback to `runEntity.getRunningCount()` when executions are empty.
- **Safety Net Added:**
  - `com.xceptance.neodymium.aura.manager.unit.AuraQueueServiceTest.testUpdateExecutionSnapshotsFinalizesRunningExecutionsOnSuccess`
  - `com.xceptance.aura.report.service.RunStorageSyncServiceTest.testLiveBufferOverridesIntermediateDiskRunningStatusInGetRunReport`
  - `com.xceptance.aura.report.service.RunStorageSyncServiceTest.testIngestExecutionMaintainsAndClearsRunningCount`
