# [DEF-20261109-1328] Reconcile JUnit Test Success with Aura Report Status for Expected Failures

- **Status:** `Resolved`
- **Opened:** 2026-11-09 13:28
- **Closed:** 2026-11-09 13:50
- **Component:** `neodymium-core` / `aura-manager`
- **Scope:** `Framework & Test/Harness`
- **Symptom:** Intentional negative tests (e.g., verifying that invalid assertions or illegal interactions fail as expected via `Assertions.assertThrows(...)`) pass 100% green in JUnit and Maven Surefire, but are marked as `FAILED` (`UNKNOWN FAIL` or `FAILED_ASSERTION`) in Aura Manager and serialized execution JSON logs (`console-execution-*.json` and `test-execution-report.json`). In full test suite runs, 51 out of 99 reported failures were actually passing negative tests.
- **Root Cause:**
  1. `PreliminaryReportListener` and `InteractiveConsoleListener` hook into internal state machine events (`StepExecutionFailedEvent`, `SessionFailedEvent`). When a negative test intentionally triggers a failure inside `session.execute(...)`, listeners record the step and session status as `"FAILED"` and flush the JSON reports to disk before rethrowing the exception.
  2. When JUnit catches the exception via `Assertions.assertThrows(...)`, JUnit marks the test method as green (`PASSED`).
  3. `NeodymiumAiRunner`'s `afterEach` lifecycle never reconciled the session report status with JUnit's outcome (`context.getExecutionException().isEmpty()`), leaving the written JSON reports permanently flagged as failed.
  4. Conversely, if an AI session executed cleanly but a trailing JUnit assertion failed, the report remained marked as `PASSED`.
  5. `InteractiveStateBuilder` scanned all steps in the blocks array and unconditionally overwrote top-level status to `"failed"` whenever any non-optional step failed, trampling runner-level success verdicts.
  6. `PreliminaryReportListener.recalculateMetrics()` re-failed reports on subsequent flushes if `failedSteps > 0`, ignoring expected failure semantics.
  7. Aura Manager's `LocalRunJsonStorageService` ingested `console-execution-*.json` directly from disk without honoring expected failure flags, resulting in contradictory red "1 failed" step counters on green test cards.
- **Detection Gap:** Unit and integration tests for negative scenarios only verified that `assertThrows` caught the expected exception in JUnit, without asserting that the generated `test-execution-report.json` or `console-execution-*.json` reflected a `PASSED` outcome with `expectedFailure=true` upon clean method completion.
- **Resolution:**
  1. Added `expectedFailure` boolean property and getter/setter in `TestExecutionReport`.
  2. Added bidirectional `reconcileTestOutcome(boolean testPassed, Throwable uncaughtException)` methods to `PreliminaryReportListener`, `InteractiveConsoleEngine`, and `InteractiveConsoleListener`.
  3. Updated `InteractiveStateBuilder` to preserve `status: "passed"` and inject `"expectedFailure": true` when `execReport.isExpectedFailure()` or `report.isSuccess()` is true, retaining step-level diagnostic failure messages and stack traces.
  4. Guarded `PreliminaryReportListener.recalculateMetrics()` against re-failing reports when `report.isExpectedFailure()` is true.
  5. Wired bidirectional reconciliation into `NeodymiumAiRunner.finalizeManagedSession`: if JUnit passed without error, session failures are reconciled to `PASSED` (`expectedFailure: true`); if JUnit failed, session is reconciled to `FAILED`.
  6. Updated `LocalRunJsonStorageService` in `aura-manager` to recognize `expectedFailure` in `extractStepsFailed` and skip non-executable seeded test fixtures.
- **Safety Net Added:** Automated regression tests in `NeodymiumAiRunnerTest`, `PreliminaryReportListenerTest`, and `RunStorageSyncServiceTest` verifying bidirectional reconciliation, diagnostic retention, expected-failure report persistence, and synthetic fixture exclusion in Aura Manager.
