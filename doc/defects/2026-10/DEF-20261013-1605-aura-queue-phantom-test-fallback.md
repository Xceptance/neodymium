# [DEF-20261013-1605] Phantom Second Test Execution Generated on Single-Variation Run Failure

- **Status:** `Resolved`
- **Opened:** 2026-10-13 16:05
- **Closed:** 2026-10-13 16:08
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In test runs with only one test variation triggered (e.g. `DEU`), if the run failed or aborted, a second phantom test execution (`console-execution-2.json`) appeared in the test logs folder and in the Aura HTML report, doubling the reported execution count.
- **Root Cause:** In `AuraQueueService.java`, the fallback generation logic executed:
  ```java
  int targetCount = Math.max(1, ids.size());
  if (isFailedRun && !hasFailureOrCancel)
  {
      targetCount = Math.max(targetCount, existingCount + 1);
  }
  ```
  When a test was started, `console-execution-1.json` was already created (`existingCount >= 1`). If the test failed or its JSON was not marked with a final status (`"passed"`, `"failed"`, `"cancelled"`) prior to subprocess termination, the condition `targetCount = Math.max(targetCount, existingCount + 1)` bumped `targetCount` from `1` to `2`. Then the loop `for (int k = 1; k <= targetCount; k++)` saw that `console-execution-2.json` did not exist and synthesized a phantom second failed execution.
- **Detection Gap ("What did we miss?"):** Previous tests in `AuraQueueServiceTest` focused exclusively on process stop commands and log error message extraction from console outputs. Fallback execution generation when Maven subprocesses exit with failure was not tested against non-empty output directories containing active executions.
- **Resolution:**
  1. Removed `targetCount = Math.max(targetCount, existingCount + 1)` in `AuraQueueService.java` and strictly bounded `targetCount` to `Math.max(1, ids.size())`.
  2. Refactored snapshot and fallback logic into `updateExecutionSnapshotsAndFallbacks(...)`. For existing `console-execution-*.json` files that are not in a final state (`passed`, `failed`, `cancelled`), update them in-place with `failed`/`cancelled` status, timestamp, and subprocess error. Only synthesize missing files for index `k <= targetCount` if no file exists for index `k` and total existing executions is zero or below the configured variation count.
  3. Cleaned up the phantom `console-execution-2.json` from `storage/runs/run_20261013_152856/Aura_tests_stokke_self_include_yaml_Test/` and resynced `run.json`.
- **Safety Net Added:** Added unit tests in `AuraQueueServiceTest`:
  - `testUpdateExecutionSnapshotsAndFallbacksDoesNotCreatePhantomSecondFileWhenFirstExecutionExists`
  - `testUpdateExecutionSnapshotsAndFallbacksCreatesFallbackWhenZeroExecutionsExist`
