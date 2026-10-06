# [DEF-20261006-1325] Composite Step Modifier Bubbling and Optional Sub-Step Failure Leakage

- **Status:** `Resolved`
- **Opened:** 2026-10-06 13:25
- **Closed:** 2026-10-06 15:12
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:**
  1. In HTML/JSON execution reports containing composite/include steps, an `(optional)` modifier on an inner step (such as a cookie banner dismissal) causes the entire parent include step card to be branded with `[OPTIONAL]`.
  2. When an optional inner step fails and is tolerated, the composite step's final sub-step (e.g. Step 1.9) is falsely marked as `FAILED`.
  3. When an independent failure occurs later in the playbook (e.g. Step 6 visual SSIM comparison failure), the session finalizer stamps Step 6's visual error message onto Step 1.9 and Step 1.
- **Root Cause:**
  1. `PlaybookStep.isOptional()` passed `checkDescendants = true`, propagating child attributes upward to parent steps. Additionally, `TestExecutionReport.ReportStepEntry.isOptional()` and `isMarker()` looped over `this.subSteps` returning `true` if any sub-step possessed the flag.
  2. In `StateMachineRunner.java`, the exception handler for optional steps executed `playbookStep.getParent().setStatus(PlaybookStepStatus.FAILED)`. Furthermore, `ExecuteActionsStep.finishParent()` determined parent step status by checking if any child had `status == FAILED`, without filtering out optional sub-steps (`!sub.isOptional()`).
  3. In `PreliminaryReportListener.onStepFinished()`, a fallback heuristic assigned `lastSub.setStatus("FAILED")` whenever a parent step finished with failure but no child report entry was marked `FAILED` (due to the tolerated optional step reporting success).
  4. In `PreliminaryReportListener.resolveUnfinishedSteps()`, `failedIndex` was determined by selecting the first leaf step with status `"FAILED"`. It did not check `!leaf.isOptional()`, so it falsely matched earlier soft-failed or corrupted leaf steps (such as Step 1.9) instead of the actual fatal failing step (Step 6) and stamped `report.getFailureReason()` onto it. Finally, `resolveParentStepStatus()` unconditionally set the parent composite step to `"FAILED"` when any sub-step had `"FAILED"`, without excluding optional sub-steps.
- **Detection Gap ("What did we miss?"):**
  1. Unit tests in `PlaybookStepTest` previously asserted upward modifier bubbling (`assertTrue(parent.isOptional())`), codifying the incorrect behavior into the test suite.
  2. Existing integration tests (`OptionalIntegrationTest`) evaluated optional steps as standalone root steps without parent composite containers, and lacked multi-step test sequences where an optional child step failed inside an include followed by an independent failure later in the playbook.
- **Resolution:**
  1. Updated `PlaybookStep.java`: `isOptional()` now invokes `hasOptionalRecursive(true, false)` (ancestors only, no upward bubbling from children).
  2. Updated `TestExecutionReport.java`: In `ReportStepEntry.isOptional()` and `isMarker()`, removed the upward `subSteps` loop; downward inheritance from parent entries is preserved.
  3. Updated `StateMachineRunner.java`: Removed parent status mutation (`playbookStep.getParent().setStatus(...)`) upon optional step failures.
  4. Updated `ExecuteActionsStep.java`: In `finishParent()`, excluded optional sub-steps (`!sub.isOptional()`) when computing parent composite status.
  5. Updated `PreliminaryReportListener.java`:
     - In `onStepFinished()`, ensured `lastSub` is not falsely marked `FAILED` if it completed successfully with valid actions.
     - In `StepStartedEvent` handling, improved sub-step matching to check both raw and cleaned instructions, preventing duplicate sub-step creation.
     - In `resolveUnfinishedSteps()`, filtered out optional leaf steps (`!leaf.isOptional()`) when identifying the session-terminating failure index.
     - In `resolveParentStepStatus()`, ignored soft failures on optional sub-steps when calculating the parent composite step status.
- **Safety Net Added:**
  - `PlaybookStepTest#testSubStepModifierInheritance`: verified parent composite step does not inherit optional from child sub-steps, while child steps inherit optional from parent containers.
  - `PlaybookStepTest#testCompositeStepModifierSerializationRoundTrip`: verified deserialized parent does not inherit optional from child.
  - `SubStepReportingAndScopingTest#testOptionalChildStepNotPromotedToParent`: verified parent include step is not marked optional in PlaybookStep or report entries when an inner step is optional.
  - `SubStepReportingAndScopingTest#testToleratedOptionalSubStepFailureDoesNotMarkParentOrSubsequentSubStepsAsFailed`: verified that a tolerated optional child failure does not fail the parent composite step or corrupt subsequent sub-steps.
  - `SubStepReportingAndScopingTest#testSubsequentStepFailureDoesNotLeakOntoPrecedingCompositeStepOrItsSubSteps`: verified that a subsequent step failure (e.g. visual divergence) does not leak error messages or failure status onto earlier composite steps or their sub-steps.
