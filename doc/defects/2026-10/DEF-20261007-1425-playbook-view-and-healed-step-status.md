# [DEF-20261007-1425] Playbook View SpEL Evaluation Failure and Healed Step/Test Status Displayed as PASSED

- **Status:** `Resolved`
- **Opened:** 2026-10-07 14:20
- **Closed:** 2026-10-07 14:35
- **Component:** `aura-manager`, `neodymium-core`
- **Scope:** `Framework`
- **Symptom:**
  1. Opening the Playbook View drawer in Aura Manager failed with a 500 Internal Server Error when viewing playbooks where actions lacked healed targets.
  2. Steps that were self-healed during execution were marked as `SUCCESS` or `PASSED` in execution reports and console execution logs instead of `HEALED`.
  3. Tests containing healed steps were marked and displayed as `PASSED` instead of `HEALED` (`SUCCEEDED_FIXED`).
- **Root Cause:**
  1. `side-panel-playbook.html` used `!act.expectedTarget.isEmpty()` in a Thymeleaf SpEL expression. Because Jackson deserializes JSON objects into generic `Map<String, Object>` models, invoking `.isEmpty()` on null keys caused `SpelEvaluationException: EL1011E: Method call: Attempted to call method isEmpty() on null context object`.
  2. `InteractiveStateBuilder.java` collapsed `StepStatus.HEALED` to `"passed"` in its switch-case statement, discarding the healed status in interactive console JSON output.
  3. `LocalRunJsonStorageService.java` unconditionally assigned `"status": "passed"` when building block steps, overwriting healed step statuses, and did not inspect `blocks` for healed steps when evaluating execution summary metrics.
  4. `TestExecutionDto.java` only returned `"SUCCEEDED_FIXED"` when bugs were linked, ignoring `healedStepsCount > 0` or healed steps in `blocks`.
  5. `run-report.html` and `report-manager.js` displayed badge text as `SUCCEEDED-FIXED` instead of `HEALED`, and the side panel details header fell back to `PASSED` instead of checking for healed steps.
- **Detection Gap ("What did we miss?"):**
  - Unit tests for `side-panel-playbook.html` did not render actions with null `expectedTarget` attributes.
  - Serialization tests in `InteractiveConsoleEngineTest` did not assert that `StepStatus.HEALED` serialized as `"healed"` with `"healed": true`.
  - Storage tests in `RunStorageSyncServiceTest` did not assert that `dto.getDisplayStatusKey()` transitions to `"SUCCEEDED_FIXED"` and report summaries count `fixed++` when steps are healed without attached bug IDs.
- **Resolution:**
  1. Refactored `side-panel-playbook.html` to perform null-safe bracket property checks (`th:with="oldTarget=${act['expectedTarget'] != null and act['expectedTarget'] != '' ? act['expectedTarget'] : ...}"`) without calling methods on potentially null context objects.
  2. Updated `InteractiveStateBuilder.java` to serialize `StepStatus.HEALED` as `"healed"`, set `"healed": true` on step objects, and promote top-level status to `"healed"` if any step in `before`, `steps`, or `after` is healed.
  3. Updated `InteractiveConsoleListener.java` to set overall status to `"healed"` on `SessionFinishedEvent` when `metrics.getHealedSteps() > 0`.
  4. Updated `LocalRunJsonStorageService.java` to preserve `"status": "healed"` and `"healed": true` on step JSON objects, inspect `blocks` for healed steps when computing execution counts, and aggregate healed tests into `fixed++`.
  5. Updated `TestExecutionDto.java` `getDisplayStatusKey()` to return `"SUCCEEDED_FIXED"` when `healedStepsCount > 0` or when any step in `blocks` is healed.
  6. Updated `run-report.html` and `report-manager.js` to render badge and header labels as `HEALED` with `badge-healed` and `status-fixed` styling.
- **Safety Net Added:**
  - `RunStorageSyncServiceTest.testHealedStepAndExecutionStatusInStorageAndDto`: asserts that `console-execution-*.json` with healed steps resolves `TestExecutionDto.getDisplayStatusKey() == "SUCCEEDED_FIXED"`, `report.getFixedCount() == 1`, and `report.getHealedCount() == 1`.
  - `InteractiveConsoleEngineTest.testInteractiveConsoleStateJsonReflectsHealedActionTarget`: asserts healed step attributes in console execution state.
