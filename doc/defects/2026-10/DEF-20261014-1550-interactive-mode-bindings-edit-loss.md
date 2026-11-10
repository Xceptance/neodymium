# [DEF-20261014-1550] Interactive Mode Test Data and Properties Edits Reverted and Lost

- **Status:** `Resolved`
- **Opened:** 2026-10-14 15:50
- **Closed:** 2026-10-14 15:58
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In interactive console mode during step edit, the UI allows modifying test data bindings and runtime properties alongside the step instruction. However, after editing, modified values were not taken into account: the UI inputs reverted back to their initial values and the execution pipeline continued executing with stale data bindings.
- **Root Cause:** Multiple cascading disconnects between frontend edit dispatch and backend state/pipeline synchronization:
  1. Frontend payload omission & sync: `interactive_console.js` maintained `currentState.dataBindings` on input, but `sendAction()` did not systematically send `bindings` to `/api/console/action`. Furthermore, if an incoming SSE state update arrived while the user was editing, `applyState()` clobbered in-progress edits with stale backend state. In addition, `handlePrimaryAction()` ("Run" / "Auto") did not auto-save uncommitted step-card edits before dispatching execution actions.
  2. Backend event handler discard: `InteractiveConsoleListener.handleUserAction()` completely ignored `userAction.get("bindings")`. Because bindings were not propagated to `context.getSessionData()`, `Neodymium.getData()`, or `System.getProperties()`, subsequent state broadcasts (`pushCurrentState`) regenerated state snapshots from unchanged session data, which SSE pushed back down to the UI, reverting all values.
  3. Precedence in SessionData: `SessionData.getStaticDataVariables()` built test data from `this.staticData` followed by `Neodymium.getData()`, but never overlaid `this.dynamicData`. Consequently, dynamic variable overrides in the session were overshadowed by static dataset definitions when generating `localDataBindings`.
  4. Execution pipeline re-resolution: When bindings alone were updated during pause, `AgentToolLoopStep` did not detect binding updates, re-evaluated instructions against potentially pre-resolved text rather than `getRawInstruction()`, and did not update `step.setInstruction(...)` for downstream tool execution.
- **Detection Gap ("What did we miss?"):** Existing tests for `InteractiveConsoleListener` tested instruction text edits (`newInstruction`) and step statuses, but none tested sending modified data bindings in user action payloads. In addition, existing `SessionDataTest` assertions only verified dataset resolution and masking without verifying that dynamic runtime variable mutations override static dataset variables in `getStaticDataVariables()`.
- **Resolution:**
  1. Updated `SessionData.getStaticDataVariables()` to apply `this.dynamicData` entries over matching static dataset variables, ensuring runtime edits take precedence in local data bindings.
  2. Added `applyDataBindings()` to `InteractiveConsoleListener` to propagate edited bindings from `userAction.get("bindings")` to `SessionData`, `Neodymium.getData()`, and `System.setProperty()`, capture a snapshot for the current step index, and flag `KEY_BINDINGS_EDITED`.
  3. Extended `AgentToolLoopStep` to detect `KEY_BINDINGS_EDITED`, re-resolve instructions against `step.getRawInstruction()`, update `step.setInstruction(...)`, and re-query the LLM with fresh parameters.
  4. Enhanced `interactive_console.js`:
     - Included `bindings: currentState?.dataBindings` in `sendAction()` payloads.
     - Synchronized `currentState.localDataBindings` in `updateDataBinding()`.
     - Preserved in-flight edited bindings during SSE updates in `applyState()` when `.step-card.editing` is active.
     - Auto-committed open step card edits via `saveBtn.click()` in `handlePrimaryAction()`.
- **Safety Net Added:**
  - `org.neodymium.ai.model.SessionDataTest#testGetStaticDataVariablesReflectsDynamicOverrides`: Verifies that dynamically overridden variables take precedence over static dataset entries in `getStaticDataVariables()`.
  - `org.neodymium.ai.event.InteractiveConsoleListenerTest#testEditActionUpdatesDataBindingsAndSessionData`: Validates that user action payloads containing modified data bindings update `SessionData`, `Neodymium.getData()`, and the broadcasted console state JSON.
