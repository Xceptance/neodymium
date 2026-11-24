# [DEF-20261123-0937] Phantom Timestamped Run Directory Created Due to Static RUN_FOLDER in InteractiveConsoleEngine

- **Status:** `Resolved`
- **Opened:** 2026-11-23 09:37
- **Closed:** 2026-11-23 10:15
- **Component:** `neodymium-core` (`InteractiveConsoleEngine.java`, `AuraInteractiveService.java`)
- **Scope:** `Framework`
- **Symptom:** When running automated tests with an explicit manager run ID (e.g., `-Dneodymium.managerRunId=run_from_console_5158 -Dneodymium.runId=run_from_console_5158`), two run folders are created in storage: the expected configured run folder (e.g. `storage/runs/run_from_console_5158`) and a phantom timestamped run folder (e.g. `storage/runs/run_20261123_092205`) containing mirrored execution state JSON files.
- **Root Cause:**
  1. `InteractiveConsoleEngine.RUN_FOLDER` was declared as a `static final String` initialized once at class-loading time via `initializeRunFolder()`. In long-running JVM processes or server daemons (such as Aura Manager running in `neodymium-ai-testcase-hub`), `RUN_FOLDER` froze to the timestamp of the very first request received by the server.
  2. In `InteractiveConsoleEngine.saveLocalExecutionLog()`, files were saved under `getRunFolder()` (the frozen static timestamp) rather than using the instance's active `runId`.
  3. `AuraInteractiveService.pushState()` attempted to mitigate this by implementing a redundant secondary write loop that wrote states to `engine.getRunId()`. Consequently, both `engine.pushState(body)` and `AuraInteractiveService.pushState()` wrote to disk, resulting in two separate run directories (`run_<timestamp>` and `run_from_console_5158`).
- **Detection Gap ("What did we miss?"):**
  1. Unit tests in `InteractiveConsoleEngineTest` did not dynamically vary `runId` on an already-loaded class while inspecting the resulting storage root.
  2. `InteractiveConsoleEngineTest.setUp()` did not isolate `neodymium.ai.report.disk.directory` and `neodymium.ai.storageRuns.directory` into temporary directories, allowing execution logs to leak into host storage.
  3. `testInteractiveConsoleEngineDoesNotCreatePhantomAuraManagerStorageDirectory` asserted `assertFalse(new File("aura-manager/storage/runs/" + testRunId).exists())`, which previously passed as a false negative only because `testRunId` was ignored by `RUN_FOLDER`.
- **Resolution:**
  1. Made `RUN_FOLDER` dynamic in `InteractiveConsoleEngine`: replaced `static final String RUN_FOLDER` with `activeRunFolder`, added `normalizeRunFolder(String)`, `setRunFolder(String)`, `resetRunFolder()`, and instance method `getEffectiveRunFolder()`.
  2. Updated `InteractiveConsoleEngine(final String runId)` and `setRunId(final String runId)` to dynamically update `activeRunFolder`.
  3. Updated `InteractiveConsoleEngine.saveLocalExecutionLog()` to resolve `runFolder = getEffectiveRunFolder()`.
  4. Removed the redundant secondary disk-write loop in `AuraInteractiveService.pushState()`, letting `InteractiveConsoleEngine.pushState()` remain the single source of truth for execution persistence.
  5. Isolated directory configurations in `InteractiveConsoleEngineTest.setUp()` / `tearDown()`.
- **Safety Net Added:**
  1. Added unit test `InteractiveConsoleEngineTest.testPushStateWithDynamicRunIdDoesNotCreatePhantomTimestampFolder` asserting that pushing state with a dynamic custom run ID creates only the custom directory and no phantom timestamped folder under `storage-runs/`.
  2. Added unit test `AuraInteractiveServiceTest.testPushStateWritesSingleExecutionFolderWithoutPhantomDirectory` asserting that `AuraInteractiveService.pushState` writes execution state to exactly one directory in `storage-runs/` without creating any phantom timestamped folder.
