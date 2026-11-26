# [DEF-20261019-1438] Test Failure Overwritten as Passed in Report When Teardown After Block Executes Successfully

- **Status:** `Resolved`
- **Opened:** 2026-10-19 14:38
- **Closed:** 2026-10-19 14:45
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** If a test step fails in a before or core steps block and the playbook defines an after block, the after block executes, but the generated HTML/Markdown/JSON test reports and interactive console state display the test run as "PASSED" with failure reasons wiped out.
- **Root Cause:**
  1. `AiSession` and `NeodymiumAiRunner` removed `playbook.deferSessionFinished` prior to invoking `StateMachineRunner` on `afterSteps`, causing `StateMachineRunner` to treat teardown as the final session run and dispatch `SessionFinishedEvent(..., success=true)` when teardown succeeded.
  2. `StateMachineRunner` removed `KEY_LAST_EXECUTION_ERROR` upon step success, clearing previous failure markers.
  3. `PreliminaryReportListener` computed `wasAlreadyFailed` against `this.report.getStatus()` (which remained `"UNKNOWN"` until session finish) rather than checking step failure states, and unconditionally wiped out `failureReason` and `failureStackTrace` when `SessionFinishedEvent.isSuccess()` was true.
  4. `InteractiveStateBuilder` did not guard overall JSON status against failed non-optional steps in `blocks.before`, `blocks.steps`, or `blocks.after`.
- **Detection Gap ("What did we miss?"):**
  `PlaybookLifecycleExecutionTest` asserted step execution status (`beforeStep.getStatus()`, `coreStep.getStatus()`, `afterStep.getStatus()`) and exception suppression, but did not register `PreliminaryReportListener` or assert `TestExecutionReport.isSuccess()` / `getStatus()` under failure conditions.
- **Resolution:**
  1. Preserved `playbook.deferSessionFinished = Boolean.TRUE` across all intermediate and teardown blocks in `AiSession` and `NeodymiumAiRunner`, dispatching a single authoritative `SessionFinishedEvent` with `overallSuccess = (primaryFailure == null)` in the outermost lifecycle `finally` block.
  2. Prevented `StateMachineRunner` from clearing `KEY_LAST_EXECUTION_ERROR` when `deferSessionFinished` is active.
  3. Updated `PreliminaryReportListener` to immediately flag `this.report.setStatus("FAILED")` and `setSuccess(false)` on non-optional `StepFinishedEvent(FAILED)`, check `hasNonOptionalFailedSteps()`, and enforce that any step failure permanently prevents the report from becoming `PASSED`.
  4. Updated `InteractiveStateBuilder` to ensure any failed non-optional step forces overall JSON status to `"failed"`.
- **Safety Net Added:**
  `PlaybookLifecycleExecutionTest`:
  - `testCoreStepFailureWithSuccessfulAfterBlockMarksReportAndConsoleAsFailed`
  - `testBeforeStepFailureWithSuccessfulAfterBlockMarksReportAndConsoleAsFailed`
  - `testBothCoreAndAfterFailureMarksReportAsFailedWithSuppressedFailure`
