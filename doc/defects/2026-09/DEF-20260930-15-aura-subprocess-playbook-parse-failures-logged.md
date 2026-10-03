# [DEF-20260930-15] Aura Subprocess Playbook Parse Failures Logged as Passed in Console Execution Reports

- **Status:** Resolved
- **Opened:** 2026-09-30
- **Closed:** 2026-09-30
- **Component:** `neodymium-core` (`AuraQueueService`)
- **Scope:** `Framework`
- **Symptom:** When a test subprocess fails due to playbook parsing errors (e.g., `Failed to parse playbook`), `console-execution-1.json` was retained with status `"passed"` and missing failure message.
- **Root Cause:** `isFailedRun` in `AuraQueueService` did not account for `fileErrors`/`fileFailures` counters, and the execution JSON updater required `isZeroStep` (stepIndex == 0 && totalSteps == 0) to overwrite existing `"passed"` execution states.
- **Detection Gap ("What did we miss?"):** Tests did not assert that existing `console-execution-*.json` files with non-zero step metrics are overridden to `"failed"` with `failureReason` when the batch subprocess fails.
- **Resolution:** Updated `AuraQueueService` to include `fileErrors`/`fileFailures` in `isFailedRun`, relaxed `isZeroStep` restriction when `isFailedRun` is true to force-update non-failed execution states with `failureReason`, and expanded `extractSubprocessErrorMessage` trace parsing.
- **Safety Net Added:** Added unit test `testExtractSubprocessErrorMessageAndExecutionStatusUpdateOnParseError` in `AuraQueueServiceTest.java`.
