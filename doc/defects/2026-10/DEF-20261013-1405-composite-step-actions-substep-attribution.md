# [DEF-20261013-1405] Composite Step Actions Not Attributed to Substeps in Execution Console

- **Status:** `Resolved`
- **Opened:** 2026-10-13 13:55
- **Closed:** 2026-10-13 14:05
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In `console-execution.json`, parent composite steps (such as arrow blocks `- >-` or `_include:` fragments) listed executed actions solely in the parent's flat `actions` list with empty `actions: []` on the nested `subSteps` entries. Furthermore, parent actions did not indicate which child substep was responsible for their execution.
- **Root Cause:** In `ExecuteActionsStep.java`, when a composite step completed in `endHookStep`, it updated the duration and status of child substeps, but never invoked `AgentToolLoopStep.partitionToolCallsAndActions(...)`. Consequently, child `PlaybookStep` instances never received their partitioned action lists. Additionally, `ReportActionEntry` and `InteractiveStateBuilder` lacked attribution fields (`subStepIndex` and `subStepInstruction`) to tag parent actions with their source substep.
- **Detection Gap ("What did we miss?"):** Previous unit tests verified that composite steps executed without error and that `partitionToolCallsAndActions` functioned in isolation during replay healing, but no test verified that the live `endHookStep` partitioned actions to child substeps or that serialized console logs contained both substep-level actions and parent action attribution metadata.
- **Resolution:**
  1. Enhanced `Action.java` and `TestExecutionReport.ReportActionEntry` with `subStepIndex` (Integer) and `subStepInstruction` (String) metadata fields.
  2. Updated `AgentToolLoopStep.partitionToolCallsAndActions(...)` to tag each action with its corresponding `subStepIndex` and `subStepInstruction`, and extended it to support partitioning actions even when `toolCalls` list is empty.
  3. In `ExecuteActionsStep.java`, invoked `partitionToolCallsAndActions` in `endHookStep` and aggregated child actions in `finishParent`.
  4. In `PreliminaryReportListener.java`, synchronized `subStepIndex` and `subStepInstruction` to `targetStep.getActions()` and distributed actions to child `ReportStepEntry` substeps.
  5. In `InteractiveStateBuilder.java`, serialized `subStepIndex` and `subStepInstruction` in parent actions and populated `actions` in both report-backed and fallback substep serializations.
- **Safety Net Added:** Added automated unit tests in `ExecuteActionsStepTest.testPartitionToolCallsAndActionsWithoutToolCalls` and `InteractiveConsoleEngineTest.testCompositeStepActionsAttributedToSubstepsInConsoleExecutionJson`.
