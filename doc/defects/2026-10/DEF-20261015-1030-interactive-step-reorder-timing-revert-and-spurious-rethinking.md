# [DEF-20261015-1030] Interactive Step Reorder Timing Revert and Spurious Rethinking

- **Status:** `Resolved`
- **Opened:** 2026-10-15 10:30
- **Closed:** 2026-10-15 10:31
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** Moving or reordering a step in the interactive console while a step is executing or before the pause token is registered causes the UI to optimistically show the step moved, but the LLM continues thinking and the step is reverted to its original position once the server broadcasts the paused state. Moving a step during an active pause also triggers a brief spurious "AI Thinking" bubble and spinner.
- **Root Cause:**
  1. `interactive_console.js` enabled drag handles and number inputs without checking `currentPauseId !== null`, allowing optimistic local mutations when execution was active, while `sendAction` silently dropped the reorder action due to `!currentPauseId`. When the LLM inference finished and the server entered pause, its state broadcast reverted the client's optimistic mutation.
  2. `InteractiveConsoleListener.handleReorderStep`, `handleAddStep`, and non-current `EDIT` invoked `pushCurrentState(context, currentStep)`, which broadcasted `status="running"` and `pauseId=null` to SSE clients even though execution was paused, causing a momentary "AI Thinking" UI flicker and wiping out the client's pause token right before the pause loop's second broadcast.
  3. `handleReorderStep` created separate `ArrayList` instances for `fromList` and `toList` when reordering within the same block on non-`ArrayList` collections (such as `List.of(...)`), desynchronizing the remove and add operations and corrupting the list stored in `playbook.steps`.
- **Detection Gap ("What did we miss?"):** Existing tests in `InteractiveConsoleListenerTest` tested sequential asynchronous submission of `REORDER` followed immediately by `RUN`, but did not simulate client interactions when `pauseId` was null or inspect the intermediate SSE runner status.
- **Resolution:**
  1. Locked drag handles (`draggable="false"`), drop targets, and step number inputs in `interactive_console.js` when `currentPauseId === null` (active running or auto-run mode), adding disabled styling (`.reorder-disabled` / `pointer-events: none`) and informative tooltips.
  2. In `handleDrop` and `reorderStepDirect`, added guards aborting local DOM mutation if `!currentPauseId`.
  3. Removed premature `pushCurrentState(..., "running", null)` calls from `handleReorderStep`, `handleAddStep`, and non-current `EDIT` in `InteractiveConsoleListener.java`, allowing the pause loop to broadcast the updated `"paused"` state directly.
  4. Unified `fromList` and `toList` in `handleReorderStep` when `fromBlock.equals(toBlock)`.
- **Safety Net Added:** Added `@Test` `testInBlockReorderOnUnmodifiableListDoesNotDuplicateOrDesynchronize` and `testReorderDuringPauseMaintainsPausedStateWithoutSpuriousRunningBroadcast` in `InteractiveConsoleListenerTest.java`.
