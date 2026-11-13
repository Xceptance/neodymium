# [DEF-20261026-1318] Rogue storage/batches Manifest Folder Created During Batch Operations

- **Status:** `Resolved`
- **Opened:** 2026-10-26 13:18
- **Closed:** 2026-10-26 13:22
- **Component:** `aura-manager` (`AuraTestQueueController.java`)
- **Scope:** `Framework & Test/Harness`
- **Symptom:** Saving or executing batch queues via `AuraTestQueueController` created an untracked, unconfigured `storage/batches/` folder on disk (or `aura-manager/storage/batches/` when invoked from subproject working directories), leaving dangling JSON manifests like `SmokeBatch.json` in the filesystem.
- **Root Cause:** `AuraTestQueueController.saveQueueAsBatch` contained a redundant secondary disk fallback that unconditionally wrote JSON files to `Paths.get("storage/batches")`, and `runBatch` attempted to read from this path if missing in the repository. Because `TestBatchEntity` already persists `testsJson` in the database, writing loose JSON files to disk was redundant and polluted local directories depending on the process CWD.
- **Detection Gap ("What did we miss?"):** Prior controller unit tests (`AuraTestQueueControllerTest.testSaveQueueAsBatchSuccess`) invoked `saveQueueAsBatch` with a mocked database repository without asserting that no unintended filesystem side effects or rogue directories were created under `storage/batches`.
- **Resolution:**
  1. Removed the disk file write logic from `saveQueueAsBatch` in `AuraTestQueueController.java`.
  2. Removed the disk fallback lookup from `runBatch` in `AuraTestQueueController.java`, making `batchRepository` the sole authoritative persistence mechanism.
  3. Cleaned up unneeded imports (`Path`, `Paths`, `Files`, `JsonNode`).
  4. Deleted the rogue `aura-manager/storage/batches/` directory from the filesystem.
  5. Added an automated regression test in `AuraTestQueueControllerTest` verifying no `storage/batches` directory is created when saving a batch.
- **Safety Net Added:** Unit test `AuraTestQueueControllerTest.testSaveQueueAsBatchSuccess` asserting that saving a batch does not create `storage/batches` or `aura-manager/storage/batches` directories on disk.
