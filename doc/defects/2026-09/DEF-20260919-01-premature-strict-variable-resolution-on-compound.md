# [DEF-20260919-01] Premature Strict Variable Resolution on Compound Steps with Runtime Placeholders

- **Status:** Resolved
- **Opened:** 2026-09-19
- **Closed:** 2026-09-19
- **Component:** `neodymium-core` (`ai-pipeline`, `playbook-engine`, `ai-runner`)
- **Scope:** `Framework`
- **Symptom:** In `CartTest.liveAllDataSets` (and any playbook step containing dynamic runtime placeholders to be captured on the fly, such as `${lineItemCount}`), step execution fails immediately with `UnresolvableVariableException: Unresolvable variable placeholder '${lineItemCount}' in template: ...` before any browser actions or capture tools can execute.
- **Root Cause:** In `ExecuteActionsStep.mapPlaybookStepToPipelineStep`, compound turn groups and leaf steps invoked strict `contextState.getSessionData().resolveVariables(...)` when resolving instructions and milestones. Because dynamic variables captured during the step (e.g. via `store`) do not exist in `SessionData` at step start, strict resolution threw an `UnresolvableVariableException`. `SessionData.resolveAvailableVariables(...)` was designed specifically to leniently resolve known variables (like `${testId}`) while leaving dynamic placeholders intact, but `ExecuteActionsStep` invoked strict `resolveVariables`. In addition, `StateMachineRunner` used strict resolution in optional and bug step failure logging, risking secondary unhandled exceptions.
- **Detection Gap ("What did we miss?"):** Existing unit tests for compound turn groups in `ExecuteActionsStepTest` (`testCompoundTurnGroupMapsToSingleStepWithMilestonesInLiveMode`) tested instructions with static strings only, without dataset variables or dynamic placeholders.
- **Resolution:**
  1. Updated `ExecuteActionsStep.mapPlaybookStepToPipelineStep` to use `contextState.getSessionData().resolveAvailableVariables(...)` for both the main instruction and internal milestone sub-steps.
  2. Updated `StateMachineRunner` to use `resolveAvailableVariables(...)` when logging bug and optional step failures.
  3. Updated report and linting listeners (`PreliminaryReportListener`, `PostFlightPlaybookLinter`, `PlaybookLinterPrompt`) to use `resolveAvailableVariables(...)` so known variables are resolved cleanly without aborting on uncaptured placeholders.
- **Safety Net Added:** Added unit regression tests in `ExecuteActionsStepTest` (`testCompoundTurnGroupWithRuntimeVariablesPreservesPlaceholdersWithoutFailing`, `testLeafStepWithRuntimeVariablesPreservesPlaceholdersWithoutFailing`) asserting that available variables resolve while runtime placeholders are preserved without throwing.
