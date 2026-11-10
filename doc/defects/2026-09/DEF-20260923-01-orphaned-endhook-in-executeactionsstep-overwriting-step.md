# [DEF-20260923-01] Orphaned endHook in ExecuteActionsStep Overwriting Step Status on Expected Bug with Continue-On-Error

- **Status:** Resolved
- **Opened:** 2026-09-23
- **Closed:** 2026-09-23
- **Component:** `neodymium-core` (`ExecuteActionsStep`, `BugIntegrationTest`)
- **Scope:** `Framework`
- **Symptom:** In tests combining `(bug)` and `(continue-on-error)`, expected step failures are falsely logged as `❌ Expected bug but step succeeded` and persisted in recorded playbooks with status `SUCCESS`. In `BugIntegrationTest.testBugContinueOnError`, the test also failed due to an invalid assertion on non-existent element `#result`.
- **Root Cause:**
  1. `ExecuteActionsStep` pushed an `endHook` lambda onto `ExecutionContext.runStack` prior to `TryCatchStep`. When `StateMachineRunner` handled an expected bug failure on a step with `continue-on-error`, it discarded steps up to `EndTryStep` and continued the loop, popping the orphaned `endHook`. The hook unconditionally set `step.setStatus(SUCCESS)` and `step.setFailed(false)`.
  2. `BugIntegrationTest` asserted `$("#result").shouldHave(exactText("Click Me Triggered!"))` instead of targeting `<span id="click-status">` on `AllActionsTest/test.html`.
- **Detection Gap ("What did we miss?"):** Previous mock tests for `(bug) (continue-on-error)` only validated that the playbook run completed without uncaught exceptions, but did not assert that the failed step retained `PlaybookStepStatus.FAILED` in the recorded companion model.
- **Resolution:** Guarded status assignments and bug validation in `ExecuteActionsStep.endHook` when `step.isFailed()` is true; corrected the element locator and expected text in `BugIntegrationTest.testBugContinueOnError`.
- **Safety Net Added:** `BugIntegrationTest.testBugContinueOnError` verifying end-to-end recording and offline replay, plus assertion on recorded companion step status.
