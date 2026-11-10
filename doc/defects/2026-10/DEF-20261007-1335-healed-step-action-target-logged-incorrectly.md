# [DEF-20261007-1335] Healed Step Action Target Logged Incorrectly in console-execution.json and UI

- **Status:** `Resolved`
- **Opened:** 2026-10-07 13:35
- **Closed:** 2026-10-07 13:43
- **Component:** `neodymium-core`, `aura-manager`
- **Scope:** `Framework`
- **Symptom:** When a test step failed to locate an element using its original target (e.g., `#selectLanguage`) during replay/execution and was healed to a new target (e.g., `#searchLanguage`), `console-execution-*.json` and the UI action pills still logged the original unhealed target (`#selectLanguage`).
- **Root Cause:**
  1. `InteractiveStateBuilder.serializeReportActions()` serialized `action.getTarget()` directly without checking `action.getResolvedTarget()`, omitting `resolvedTarget`, `resolvedValue`, and `healed` properties from `console-execution-*.json`.
  2. `PlaybookToolReplayer.replayStep()` did not update the `Action` entry in `step.getActions()` when locator healing occurred.
  3. `LocalRunJsonStorageService.java` dropped `resolvedTarget`, `resolvedValue`, and `healed` properties when parsing action nodes into execution DTOs.
  4. `report-manager.js` and `interactive_console.js` rendered action targets strictly via `act.target` rather than checking `act.resolvedTarget || act.target`.
- **Detection Gap ("What did we miss?"):** Existing unit tests verified overall step status as `HEALED` but did not assert that action targets inside `console-execution-*.json` and rendered DTO/JS action pills reflected `resolvedTarget`.
- **Resolution:**
  1. Updated `InteractiveStateBuilder.serializeReportActions()` to use `action.getResolvedTarget()` when present for `target` and write `resolvedTarget`, `resolvedValue`, and `healed` properties.
  2. Updated `PlaybookToolReplayer.replayStep()` to update `step.getActions()` at the tool call index when healed.
  3. Updated `LocalRunJsonStorageService.java` to copy `resolvedTarget`, `resolvedValue`, and `healed` fields into step action JSON nodes.
  4. Updated `report-manager.js` and `interactive_console.js` to render `act.resolvedTarget || act.target` and `act.resolvedValue || act.value`.
- **Safety Net Added:** Added unit tests `InteractiveConsoleEngineTest.testStateJsonSerializesHealedActionTargetAndFlag` and `RunStorageSyncServiceTest.testHealedActionTargetPreservedInStorageService`.
