# [DEF-20261021-1358] Phantom aura-manager/storage/runs Folder Created During Test Execution

- **Status:** `Resolved`
- **Opened:** 2026-10-21 13:58
- **Closed:** 2026-10-21 14:00
- **Component:** `neodymium-core` (`InteractiveConsoleEngine.java`)
- **Scope:** `Framework & Config/Environment`
- **Symptom:** During test execution (especially when `batchName` or `neodymium.batchName` is set), a rogue directory `/aura-manager/storage/runs/<runId>/` containing solely a `batch.json` file is created in the project root directory.
- **Root Cause:** `InteractiveConsoleEngine.ensureBatchJson` included hardcoded `"aura-manager/storage/runs"` in `storageRoots` and called `runDir.mkdirs()` unconditionally. In consumer projects without an `aura-manager` subproject, this created dummy directory structures. Furthermore, `InteractiveConsoleEngine` writing `batch.json` is redundant because `AuraQueueService` already pre-creates `storage/runs/<runId>/batch.json` prior to spawning test runner subprocesses.
- **Detection Gap ("What did we miss?"):** Prior unit tests checked that `batch.json` existed in the logs directory, but did not assert that external unconfigured directories like `"aura-manager/storage/runs"` were not created on disk.
- **Resolution:** 
  1. Removed `ensureBatchJson` from `InteractiveConsoleEngine.java` (reverting `initializeRunFolder()` and removing `ensureBatchJson` calls from `pushState`).
  2. Replaced `testEnsureBatchJsonWithBatchNameSystemProperty` with a safety net test asserting that `InteractiveConsoleEngine` never creates phantom `aura-manager/storage/runs/` directories.
  3. Cleaned up rogue test directories on disk.
- **Safety Net Added:** Added unit test `InteractiveConsoleEngineTest.testInteractiveConsoleEngineDoesNotCreatePhantomAuraManagerStorageDirectory` asserting no `aura-manager/storage/runs/` directory is created when executing with a custom `batchName`.
