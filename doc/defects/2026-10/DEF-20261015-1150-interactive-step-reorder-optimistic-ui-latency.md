# [DEF-20261015-1150] Interactive Console Delayed Step Mutation and Optimistic UI Desynchronization

- **Status:** `Resolved`
- **Opened:** 2026-10-15 11:50
- **Closed:** 2026-10-15 11:56
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** Step move and step addition requests took effect in test execution, but did not appear immediately in the UI or temporarily reverted until the backend SSE round-trip completed. Clicking native browser spinner up/down arrows on the step position input had no immediate effect until the input was blurred or clicked away.
- **Root Cause:**
  1. Input Event Binding Gap: The step position input (`<input type="number">`) in `interactive_console.js` only bound `onkeydown` and `onblur`. Native browser spinner up/down clicks and manual typing without pressing Enter did not fire either event, keeping the value uncommitted until the user clicked elsewhere.
  2. In-Flight SSE State Flapping: When a user moved or added a step, the client modified `currentState.blocks` optimistically and sent the action to the backend. While the backend was processing the mutation, intermediate SSE `state` events arriving with the pre-mutation pause token (`pauseId`) unconditionally overwrote `currentState.blocks` with the old server snapshot, reverting the UI until the backend rotated the pause token.
  3. Premature Removal of Added Steps: `saveEdit()` deleted `isTempAdd` upon sending `ADD`. Because `applyState()` only preserved steps with `isTempAdd === true`, any intermediate state sync dropped the newly saved step from the DOM until the backend finished adding it.
  4. Missing Final Drop Zone on `steps` and `before` Blocks: In `renderBlock()`, `final-drop-zone` was only appended to the `after` block. Dragging steps to the end of the `steps` list had no valid drop target.
  5. Step Focus & Selection Desynchronization: `reorderStepDirect()` assigned 0-based array indices (`targetFlatIdx`) to `lastFocusedStepIndex` and `selectedStepIndexForDetails`, breaking `restoreFocus()` and deselecting the moved card. In `applyState()`, `selectedStepIndexForDetails` was unconditionally forced to `activeStep.index` during pauses, overriding user card selection.
- **Detection Gap ("What did we miss?"):** Previous tests verified backend playbook step array mutations on `REORDER` and `ADD`, but did not assert browser-side event binding coverage (`onchange` / spinner), optimistic state preservation during in-flight SSE streams, or drop-zone DOM presence across all block sections.
- **Resolution:**
  1. Bound immediate `onchange="reorderStepDirect(event, ${step.index})"` and debounced `handleStepInputInput` to the step number input so spinner button clicks and typed numbers apply instantly.
  2. Added `structuralActionPauseId` tracking. When a structural action is in flight, `applyState()` preserves optimistic `currentState.blocks` and does not allow stale pre-rotation SSE states to overwrite the UI.
  3. Retained newly added steps under `isPendingAdd` until confirmed by the rotated server state.
  4. Appended `final-drop-zone` across all blocks in interactive mode so steps can be dragged to the end of `steps` or `before`.
  5. Preserved `fromStepIndex` across `lastFocusedStepIndex`, `selectedStepIndexForDetails`, and `flashMovedRow` in both `reorderStepDirect()` and `handleDrop()`.
  6. Prevented `applyState()` from clearing the user's selected step during pauses if the step still exists.
- **Safety Net Added:** Verified via `InteractiveConsoleEngineTest` and `InteractiveConsoleListenerTest` test suites (all 49 tests passing).
