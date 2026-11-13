# [DEF-20261110-1300] Unreached step status propagation edge cases and teardown clobber in interactive state and reports

- **Status:** `Resolved`
- **Opened:** 2026-11-10 12:35
- **Closed:** 2026-11-10 13:00
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:**
  1. In `InteractiveStateBuilder`, succeeding teardown steps (e.g. `after` steps executing after a main step failure) were clobbered and serialized as `"status": "not_run"` with an erroneous failure banner, because `precedingStepFailed && isRunFailed` unconditionally overwrote any status on steps evaluated after a failure.
  2. In `PreliminaryReportListener`, if a composite step preceding a failure contained both `SUCCESS` sub-steps and intentionally `SKIPPED` sub-steps (e.g. conditional skips), `resolveParentStepStatus()` erroneously marked the completed composite parent as `NOT RUN` instead of `SUCCESS`.
  3. In `PreliminaryReportListener.onEvent(StepFinishedEvent)`, `notRunReason` set on `PlaybookStep` was not copied over to `targetStep`, causing manually finalized or runner-dispatched `NOT_RUN` steps to lose runner-specific context reasons unless overwritten by `resolveUnfinishedSteps()`.
  4. In `StateMachineRunner.markRemainingStepsAsSkipped()`, composite sub-step failures formatted downstream unreached step labels as `#parent` rather than `#parent.sub` (e.g. `#1.2`).
- **Root Cause:**
  1. `InteractiveStateBuilder.java`: the status reassignment block `if (precedingStepFailed && isRunFailed) status = PlaybookStepStatus.NOT_RUN;` executed regardless of whether `step.getStatus()` was already a completed terminal state (`SUCCESS`, `FAILED`, `HEALED`, or `SKIPPED`).
  2. `PreliminaryReportListener.java`: `resolveParentStepStatus()` required `allSuccess` to be true to mark a composite parent `SUCCESS`. If some sub-steps were intentionally skipped and none failed, `allSuccess` was false, and the fallback marked the step `NOT RUN`.
  3. `PreliminaryReportListener.java`: `onEvent(StepFinishedEvent)` copied `failureReason`, `reasoning`, and `verificationResult`, but omitted copying `notRunReason` from `PlaybookStep`.
  4. `StateMachineRunner.java`: failed step resolution in `markRemainingStepsAsSkipped()` checked if `s.getSubSteps().contains(fs)` and assigned the root step index `#fi + 1` rather than appending the sub-step index `.sub`.
- **Detection Gap ("What did we miss?"):**
  Previous PR #525 tests tested single linear steps and unexecuted teardown steps, but did not test scenarios where an `after` block step executed successfully after a core step failed, nor composite steps containing intentionally skipped sub-steps preceding a downstream failure.
- **Resolution:**
  1. `InteractiveStateBuilder.java`: restricted `precedingStepFailed && isRunFailed` override to non-terminal statuses (`status == null || status == PlaybookStepStatus.PENDING || status == PlaybookStepStatus.SPLITTED`), preserving completed `SUCCESS`, `FAILED`, `HEALED`, and `SKIPPED` teardown and after steps. Added fallback message for `notRunReason` if `firstFailedStepLabel` is unavailable.
  2. `PreliminaryReportListener.java`: updated `resolveParentStepStatus()` to evaluate `else if (allSuccess || (firstNotRunReason == null && !anyFailed)) { step.setStatus("SUCCESS"); }` so composite steps whose sub-steps completed without failure or downstream unreached status retain `SUCCESS`.
  3. `PreliminaryReportListener.java`: updated `onEvent(StepFinishedEvent)` to copy `pbStep.getNotRunReason()` into `targetStep`, and updated sub-step iteration to skip sub-steps where `sub.isNotRun()` is true.
  4. `StateMachineRunner.java`: updated `markRemainingStepsAsSkipped()` to resolve composite sub-step failure labels as `#parent.sub` (e.g., `#1.2`).
- **Safety Net Added:** Added comprehensive automated regression tests:
  - `InteractiveConsoleEngineTest.testSuccessfulAfterStepPreservesPassedStatusWhenPrecedingStepFailed`: verifies successful after steps retain `passed` status when preceding core step failed.
  - `InteractiveConsoleEngineTest.testUnexecutedAfterStepSerializesAsNotRunWhenPrecedingStepFailed`: verifies unexecuted after steps are serialized as `not_run`.
  - `PreliminaryReportListenerTest.testCompositeStepWithPassedAndSkippedSubstepsRemainsSuccessWhenDownstreamStepFails`: verifies composite steps with mixed passed and skipped sub-steps remain `SUCCESS`.
  - `PreliminaryReportListenerTest.testStepFinishedEventWithNotRunStatusPreservesNotRunReason`: verifies `onEvent(StepFinishedEvent)` propagates `notRunReason` from `PlaybookStep` to report steps.
  - `StateMachineRunnerTest.testCompositeSubStepFailureSetsSubStepSpecificLabelInNotRunReason`: verifies `#parent.sub` formatting in downstream `notRunReason`.
