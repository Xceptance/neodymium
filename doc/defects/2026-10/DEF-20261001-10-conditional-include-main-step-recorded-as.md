# [DEF-20261001-10] Conditional Include Main Step Recorded as Substep in Console Execution Reports

- **Status:** Resolved
- **Opened:** 2026-10-01
- **Closed:** 2026-10-01
- **Component:** `neodymium-core` (`PreliminaryReportListener` / `ExecuteActionsStep` / `InteractiveStateBuilder`)
- **Scope:** `Framework`
- **Symptom:** In `console-execution-*.json` and execution reports for steps with conditional includes (e.g. `Add product to cart:` with child `- If (condition) then _include: ...`), the main step's conditional instruction was recorded as `Substep 0` inside its own `subSteps` array alongside the actual included steps, and `subSteps` of included steps contained nested duplicates of the main step.
- **Root Cause:** When a parent step has a child step containing `_include:`, `PreliminaryReportListener` pre-populated `subSteps` with the conditional instruction as `Substep 0` prior to runtime include expansion. When `ExecuteActionsStep` and `IncludeAction` ran, `stepStats.getSubStats()` contained the container step, causing `mergeStepStats` to overwrite `Substep 0` with the conditional instruction.
- **Detection Gap ("What did we miss?"):** Existing tests for `IncludeAction` verified step execution order and execution results, but did not assert that the report's `subSteps` array excludes the conditional include step itself.
- **Resolution:** Updated `ExecuteActionsStep` to extract effective leaf sub-steps when populating sub-stats, updated `PreliminaryReportListener` to clean up intermediate container/include steps from `subSteps`, and updated `InteractiveStateBuilder` to filter out include container instructions during subStep serialization.
- **Safety Net Added:** Added unit test `testConditionalIncludeSubStepsExcludesMainStep` in `SubStepReportingAndScopingTest.java`.
