# [DEF-20260930-21] Successful Executions Overwritten to Failed and SLF4J Warnings Extracted as Process Errors

- **Status:** Resolved
- **Opened:** 2026-09-30
- **Closed:** 2026-09-30
- **Component:** `neodymium-core` (`AuraQueueService`)
- **Scope:** `Framework`
- **Symptom:** Successful test executions with completed steps (`totalSteps > 0`) in a multi-dataset batch were overwritten to status `"failed"` with SLF4J warning messages (e.g., `WARN ... BiDiException`) listed as `failureReason`.
- **Root Cause:**
  1. `AuraQueueService.executeQueue` updated all non-failed execution JSONs to `"failed"` when `isFailedRun` was true, missing the `isZeroStep` check to distinguish unexecuted tests from completed successful ones.
  2. `extractSubprocessErrorMessage` checked `line.contains("WARN:")` with a required colon, failing to match SLF4J log lines (`[main] WARN ...`) which omit the colon, allowing lines containing `BiDiException:` to be parsed as error messages.
- **Detection Gap ("What did we miss?"):** Tests did not assert that multi-dataset execution batches containing both a failing test and a passing test retain `"passed"` status for the completed test, nor did tests cover SLF4J `WARN` log formats without trailing colons.
- **Resolution:** Re-enforced `isZeroStep` check in `AuraQueueService` when updating non-failed execution states on `isFailedRun`, and updated `extractSubprocessErrorMessage` to inspect `WARN` and `WARNING` without requiring trailing colons.
- **Safety Net Added:** Added unit tests in `AuraQueueServiceTest.java` for SLF4J `WARN` filtering and zero-step status update bounds.
