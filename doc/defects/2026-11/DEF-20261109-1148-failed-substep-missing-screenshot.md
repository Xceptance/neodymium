# [DEF-20261109-1148] Missing Screenshot in Report on Failed Playbook Sub-step

- **Status:** `Resolved`
- **Opened:** 2026-11-09 11:06
- **Closed:** 2026-11-09 11:48
- **Component:** `neodymium-core`, `aura-manager`
- **Scope:** `Framework`
- **Symptom:** When a playbook step containing sub-steps failed during sub-step execution (e.g. an assertion failure in a nested sub-step action), the step card in Aura Manager's test report displayed no screenshot evidence in its Overview tab.
- **Root Cause:**
  1. Failure in sub-step action execution threw an exception, bypassing the normal post-step state capture in `ExecuteActionsStep.java`.
  2. While `StateMachineRunner.java` caught this failure and dispatched `StateCapturedEvent(failureState)`, `PreliminaryReportListener.java` associated the screenshot strictly with `currentStep` (the child sub-step) when `currentParentStep != null` without attaching it to `currentParentStep` (the parent step).
  3. In `PreliminaryReportListener.java`, `resolveParentStepStatus()` resolved status and failure reasons from failed sub-steps but omitted rolling up captured failure screenshots onto the parent step.
  4. In `InteractiveStateBuilder.java`, the parent step's serialized `screenshots` list remained empty because it did not aggregate or fall back to child sub-step screenshots when `reportStep.getScreenshots().isEmpty()`.
  5. In `report-manager.js`, the UI viewer explicitly ignored the transient screenshot (`s.screenshot`) and lacked fallback to `s.subSteps[].screenshots` when evaluating `s.screenshots`.
- **Detection Gap ("What did we miss?"):** Existing test cases in `PreliminaryReportListenerTest` validated top-level step failure captures or successful multi-step executions, but never asserted that a parent step's screenshot list reflects screenshots captured during sub-step failure events in both memory, disk reports, and interactive state serialization.
- **Resolution:**
  1. Updated `PreliminaryReportListener.java` on `StateCapturedEvent` to register captured screenshots on `currentParentStep` when a failure capture occurs, and in `resolveParentStepStatus()` to roll up failure screenshots from failed sub-steps onto `step.getScreenshots()` when empty.
  2. Enhanced `InteractiveStateBuilder.java` to aggregate screenshots from sub-steps if the parent step's `screenshots` list is empty.
  3. Hardened `report-manager.js` to fall back to `s.subSteps[].screenshots` and `s.screenshot` when `s.screenshots` is empty.
- **Safety Net Added:** Added `@Test` `PreliminaryReportListenerTest.testFailedPlaybookSubStepScreenshotRegisteredOnParentStep` asserting that sub-step failure screenshots are registered on both the sub-step and parent step entries in `TestExecutionReport`, in the generated JSON report, and in `InteractiveStateBuilder.buildStateJson()`.
