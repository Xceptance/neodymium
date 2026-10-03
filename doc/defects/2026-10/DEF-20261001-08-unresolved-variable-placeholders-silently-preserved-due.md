# [DEF-20261001-08] Unresolved Variable Placeholders Silently Preserved Due to Lenient Step-Level Resolution

- **Status:** Resolved
- **Opened:** 2026-10-01
- **Closed:** 2026-10-01
- **Component:** `neodymium-core` (`ExecuteActionsStep`, `VisualBaselineGateStep`, `SessionData`)
- **Scope:** `Framework`
- **Symptom:** A playbook step referencing an unmapped variable (`${cartName}` instead of `${cartHeadline}`) passed silently in replay mode because lenient variable resolution left the placeholder intact and visual baseline dHash bypassed validation.
- **Root Cause:** PlaybookStep instructions were resolved with lenient `resolveAvailableVariables()` at step execution dispatch, preserving unresolvable placeholders rather than invoking strict `resolveVariables()`. Visual baseline comparison subsequently matched the rendered page and short-circuited execution without checking variable integrity.
- **Detection Gap ("What did we miss?"):** Existing test `testLeafStepWithRuntimeVariablesPreservesPlaceholdersWithoutFailing` explicitly encoded and tested the lenient behavior, and no test verified that executing a leaf step with an unresolvable variable throws `UnresolvableVariableException` prior to visual baseline gating.
- **Resolution:**
  1. Enforced strict resolution (`sessionData.resolveVariables()`) at leaf step execution dispatch in `ExecuteActionsStep`.
  2. Preserved template immutability on `PlaybookStep.instruction` (stored resolved text strictly in `ExecutionContext.KEY_CURRENT_INSTRUCTION`), ensuring JSON recordings and multi-dataset iterations are not corrupted.
  3. Simplified downstream helpers (`VisualBaselineGateStep`) to consume `ExecutionContext.KEY_CURRENT_INSTRUCTION` directly instead of redundantly re-resolving variables.
- **Safety Net Added:** Comprehensive unit tests in `ExecuteActionsStepTest`:
  - `testLeafStepWithUnresolvableVariableThrowsExceptionAndPreservesTemplate`: Asserts that missing variables throw `UnresolvableVariableException` and keep the original template pristine.
  - `testLeafStepWithUnresolvableVariableFailsBeforeVisualBaselineGating`: Asserts that visual steps with unresolvable variables fail before visual baseline gating, preventing false-positive test bypass.
  - `testLeafStepWithMissingRuntimeVariableFailsStrictly`: Asserts strict failure on missing runtime variables, succeeded once runtime variable is provided.
  - `testMultiDataSetSequentialExecutionPreservesTemplateIsolation`: Asserts that executing the exact same `PlaybookStep` across sequential dataset iterations preserves template immutability and isolates resolved instructions without cross-contamination.
  - `testUnrolledSubStepWithUnresolvableVariableFailsStrictlyOnChild`: Asserts that when a parent step with recorded sub-steps is unrolled in replay mode, an unmapped placeholder on child 2 strictly fails with `UnresolvableVariableException` after child 1 executes.
  - `testStateMachineRunnerFailsConclusivelyOnUnresolvableVariable`: Asserts that `StateMachineRunner` propagates `UnresolvableVariableException` directly and marks failing playbook step status as `FAILED`.
