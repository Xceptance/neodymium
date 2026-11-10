# [DEF-20261013-1305] Skipped Downstream Composite Step Substeps Leaking Into Prior Failed Step in Report and Console Execution Log

- **Status:** `Resolved`
- **Opened:** 2026-10-13 13:05
- **Closed:** 2026-10-13 13:16
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** When a scenario step fails (e.g. on timeout), substeps belonging to an unexecuted downstream composite step (such as an arrow block or include block) are erroneously attached to the failed step in `console-execution.json` and reports. The first substep of the unrelated step is marked `FAILED` with the parent step's timeout failure message, and remaining substeps are marked `SKIPPED`.
- **Root Cause:** In `PreliminaryReportListener.onEvent(StepFinishedEvent)`, when `targetStep` was not found for a skipped unexecuted step dispatched by `StateMachineRunner.markRemainingStepsAsSkipped`, an unconditional fallback assigned `targetStep = this.currentStep`. Since `this.currentStep` was still pointing to the previously failed step, `pbStep.getSubSteps()` from the skipped step were copied directly into the failed step's `ReportStepEntry`, and the first substep was marked `FAILED` with the parent's timeout failure reason.
- **Detection Gap ("What did we miss?"):** Existing `PreliminaryReportListener` unit tests only tested isolated composite step executions where the composite step itself was started, rather than scenarios where a prior step fails and downstream composite steps are skipped via `markRemainingStepsAsSkipped`.
- **Resolution:**
  1. Updated `PreliminaryReportListener.java` to restrict the `targetStep` fallback to `this.currentStep` (or the last step in the report) exclusively to cases where `pbStep == null`. When `pbStep != null` is not found in the report, `targetStep` remains `null`, preventing mutation of unrelated prior steps.
  2. Enhanced `PreliminaryReportListener.java` to match on both `pbRaw` (raw instruction) and `pbInstr` (resolved instruction) when searching existing report entries.
  3. Added defense-in-depth instruction verification in `InteractiveStateBuilder.java` (`serializeStep`) when associating `ReportStepEntry` with playbook steps, and verified that a flat non-include step never accepts substeps from report entries.
- **Safety Net Added:**
  - `PreliminaryReportListenerTest.testSkippedRemainingCompositeStepDoesNotCorruptPreviousFailedStep`
  - `InteractiveConsoleEngineTest.testSkippedCompositeSubstepsDoNotLeakIntoPriorFailedStep`
