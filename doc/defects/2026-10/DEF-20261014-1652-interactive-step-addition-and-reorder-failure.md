# [DEF-20261014-1652] Interactive Mode Step Addition Append-Only and Step Reorder Sync Invalidation

- **Status:** `Resolved`
- **Opened:** 2026-10-14 16:52
- **Closed:** 2026-10-14 17:25
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In interactive console mode:
  1. Steps could only be appended at the end of the test; attempting to add a step in the middle was unsupported or defaulted to the end.
  2. Upon appending a step, the currently paused step was automatically confirmed and executed without user intent.
  3. The newly added step and the subsequent step both displayed with "running" status (dual active cards with blue borders).
  4. Appended steps were never executed by the pipeline runner.
  5. Shifting/reordering a step temporarily rendered it at the new position, but upon server sync it shifted back or was placed incorrectly.
- **Root Cause:**
  1. In `interactive_console.js`, `saveEdit()` always calculated `targetRelIdx = (currentState.blocks.steps || []).length` when saving newly added steps without respecting the target insertion index, and `openAddStepOverlay()` only appended to the end of the step list.
  2. When saving an added step, `saveEdit()` marked the step as `running` and submitted an `"EDIT"` action, which the backend `InteractiveConsoleListener` interpreted as modifying the currently paused step, thereby exiting the pause loop (`return "RUN"`) and resuming execution.
  3. The backend execution engine did not support dynamic step mutation on `ExecutionContext.runStack`: pending steps pre-scheduled on the execution stack were immutable and unmapped, so newly added or reordered steps in `playbook.steps` were never updated on the runner stack.
  4. In `InteractiveConsoleListener.handleReorderStep()`, list lookups did not fall back to `ExecutionContext.KEY_PLAYBOOK` when `playbook.steps` was not explicitly populated in transient data, and index calculations double-decremented target indices when moving downward.
  5. In `InteractiveStateBuilder.java`, steps serialization checked `playbook.steps` directly without checking `ExecutionContext.KEY_PLAYBOOK`, causing state sync desynchronization when only the playbook container was present.
- **Detection Gap ("What did we miss?"):** Previous interactive console tests focused on pause-and-resume actions (`RUN`, `AUTO`, `SKIP`, `HEAL`, `FINISH`) on existing static steps, without testing live playbook mutations (`ADD`, `REORDER`, non-current `EDIT`) during active pauses or verifying execution stack re-synchronization.
- **Resolution:**
  1. Introduced `org.neodymium.ai.pipeline.PlaybookPipelineStep` interface to identify pipeline steps bound to logical `PlaybookStep` instances and distinguish them from intra-step boundary markers or exception handlers.
  2. Added `ExecutionContext.updateRemainingPlaybookSteps(remainingSteps, session)` to preserve intra-step boundary hooks and active sub-steps, discard obsolete pending top-level steps, and schedule newly inserted or reordered steps onto `runStack`.
  3. Wrapped `InteractiveConsoleListener.pauseBeforeActionExecution()` and `pauseOnStepFailure()` in interactive loops that continue pausing after handling structural actions (`ADD`, `REORDER`, `SUGGEST_FIX`, non-current `EDIT`).
  4. Updated `InteractiveConsoleListener` with `handleAddStep()`, `handleReorderStep()`, `updateFlatSteps()`, and `rescheduleRemainingSteps()`, integrating seamless fallbacks to `ExecutionContext.KEY_PLAYBOOK` and immediate state broadcasting via `pushCurrentState()`.
  5. Enhanced `interactive_console.js`:
     - Added `.step-add-after-btn` ("Add step after this") on step cards and updated `openAddStepOverlay(blockName, targetIndex)` to insert at arbitrary indices.
     - Updated `saveEdit()` to mark added steps with status `'pending'`, preserve runner pause, and send `index` to the backend.
     - Fixed `reorderStepDirect()` and `handleDrop()` to keep client and server insertion index math consistent without double decrements.
  6. Added styling in `interactive_console.css` for `.step-add-after-btn`.
- **Safety Net Added:**
  - `org.neodymium.ai.event.InteractiveConsoleListenerTest#testAddStepDuringPauseKeepsPauseAndUpdatesExecutionStack`: Verifies step insertion at arbitrary index, preservation of pause state, Playbook steps update, and pipeline `runStack` synchronization.
  - `org.neodymium.ai.event.InteractiveConsoleListenerTest#testReorderStepsDuringPauseKeepsPauseAndUpdatesExecutionStack`: Verifies step reordering during pause, preservation of pause state, Playbook steps update, and pipeline `runStack` re-scheduling.
  - `org.neodymium.ai.event.InteractiveConsoleListenerTest#testEditNonCurrentStepDuringPauseKeepsPauseAndUpdatesTargetStep`: Verifies editing a non-current pending step while paused on an active step, keeping the active step paused and broadcasting updated instructions.
