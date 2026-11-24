# [DEF-20261123-1211] Stale Mid-Run JSON Freezes Finished Test in Running State

- **Status:** `Resolved`
- **Opened:** 2026-11-23 11:45
- **Closed:** 2026-11-23 12:47
- **Component:** `aura-manager` (`AuraServiceConfiguration.java`, `LocalRunJsonStorageService.java`, `AuraReportDataService.java`, `RunStorageSyncService.java`) & `neodymium-core` (`InteractiveConsoleEngine.java`)
- **Scope:** `Framework & Infra/Build`
- **Symptom:** When a test run was inspected in Aura Manager while tests were actively executing (or pushed via `/api/console/internal/pushState` during interactive console runs), the last test remained permanently frozen in `running` state with `0 min 0 s` duration (e.g. `run_from_console_51765`, `run_from_console_51767`), even after the test finished and wrote its final failed/passed execution JSON to disk. Additionally, external test hubs depending on legacy `com.xceptance.aura:aura-manager` coordinates continued loading stale pre-rebrand JARs from `~/.m2`.
- **Root Cause:**
  1. `AuraReportDataService#getRunReport` generated `run.json` during execution via `LocalRunJsonStorageService#buildRunJsonContent(..., isRunActive=true)`. This persisted a snapshot on disk with `summary.running: 1` and test status `running`.
  2. Once the test completed and the test runner wrote `console-execution-2.json` with final status and duration, subsequent calls to `getRunReport` evaluated `(isInProgress || runJsonFile == null || !Files.exists(runJsonFile))`. Because `isInProgress` became `false` and `run.json` already existed on disk, `buildRunJsonContent` was skipped.
  3. `LocalRunJsonStorageService#readRunJson` returned the stale `run.json` from disk, which `AuraReportDataService` cached in memory. Because the run was no longer active, `AuraReportDataService#computeEffectiveStatus` mapped the stale `running` status to `ignored` or kept duration at `0s`.
  4. `RunStorageSyncService#syncLocalRunStorage` skipped any run where `hasRunJsonFile` and `existingRunOpt.isPresent()` were true, preventing user refresh (`/runs/refresh`) from repairing the stale run or un-deleting soft-deleted runs on disk.
  5. `AuraServiceConfiguration` did not wire `AuraInteractiveService#setStatePushListener`, so incoming `/api/console/internal/pushState` snapshots wrote `console-execution-N.json` to disk without evicting `AuraReportDataService` cache or syncing `run.json` and `TestRunEntity` upon test completion.
  6. `InteractiveConsoleEngine#reconcileTestOutcome` persisted reconciled state only locally instead of forwarding changed states through `pushState(updatedJson)` when `isManagerActive()` is `true`.
  7. Rebranding `aura-manager` to `com.xceptance:neodymium-manager` left external hub projects resolving legacy `com.xceptance.aura:aura-manager:6.0.0-BETA` artifacts from `~/.m2`, while concurrent `mvn install` builds from another branch overwrote `neodymium-core:6.0.0-BETA`.
- **Detection Gap ("What did we miss?"):** Existing test cases either generated `run.json` only after all executions were completely written or invoked `getRunReport` / `syncLocalRunStorage` directly against storage services, rather than driving the full `/api/console/internal/pushState` -> `AuraInteractiveService` -> `AuraServiceConfiguration` listener pipeline, and did not verify active JVM JAR descriptors in `neodymium-ai-testcase-hub`.
- **Resolution:**
  1. Implemented `LocalRunJsonStorageService#isRunJsonStale(final String runId)` to detect when `run.json` is missing, older than any test execution JSON on disk, or contains in-progress markers when the run is no longer active.
  2. Integrated `isRunJsonStale` into `LocalRunJsonStorageService#readRunJson` and `AuraReportDataService#getRunReport`, bypassing stale memory cache and rebuilding `run.json` when test results have updated.
  3. Updated `AuraReportDataService#getRunReport` to update and persist `TestRunEntity` in `runRepository` to `COMPLETED` with accurate counters and `runningCount = 0`.
  4. Updated `RunStorageSyncService#syncLocalRunStorage` to re-import runs if `isRunJsonStale` is true or `isDeleted` is true, and ensured `runEntity.setIsDeleted(false)` on import/sync.
  5. Wired `interactiveService.setStatePushListener(...)` in `AuraServiceConfiguration#auraQueueService` to evict `AuraReportDataService` cache on every push and invoke `runStorageSyncService.importOrUpdateRunReport(runId)` on new runs or terminal execution states.
  6. Updated `InteractiveConsoleEngine#reconcileTestOutcome` to forward reconciled state changes via `pushState(updatedJson)` when `isManagerActive()` is true.
  7. Reinstalled both `neodymium-core` and `aura-manager` from worktree and synced the manager artifact into legacy `~/.m2/repository/com/xceptance/aura/aura-manager/6.0.0-BETA/`.
- **Safety Net Added:** Added permanent automated integration tests in `com.xceptance.aura.report.service.AuraReportStalenessRecoveryTest`:
  - `testStaleMidRunJsonIsDetectedAndRebuiltOnGetRunReport`: Simulates mid-run `run.json` generation followed by newer completed test execution, verifying staleness detection, automatic report regeneration, and database entity status update.
  - `testSyncLocalRunStorageRepairsStaleRunAndUndeletes`: Verifies that `syncLocalRunStorage` detects stale in-progress runs, repairs them, and un-deletes soft-deleted runs present on disk.
  - `testInteractivePushStateAutomaticallyFinalizesMidRunReportAndDatabaseEntity`: Drives the full `AuraInteractiveService#pushState` -> `AuraServiceConfiguration` `statePushListener` -> `RunStorageSyncService` pipeline from an active `"running"` push (with mid-run `getRunReport` fetch) through a terminal `"failed"` push, asserting automatic `COMPLETED` database transition, `runningCount == 0`, cache eviction, and finalized report duration.
