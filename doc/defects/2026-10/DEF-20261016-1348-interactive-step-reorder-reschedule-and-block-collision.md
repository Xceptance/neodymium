# [DEF-20261016-1348] Interactive Step Reorder Reschedule Desynchronization and Section Block Index Collision

- **Status:** `Resolved`
- **Opened:** 2026-10-16 13:48
- **Closed:** 2026-10-16 13:55
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In interactive mode, reordering steps in the web console appeared to succeed visually initially, but when confirming or advancing execution, the move was reverted or the step executed in the wrong order. Additionally, in playbooks with `before`/`after` blocks, dragging or editing step indices in `steps` targeted steps in `before` due to duplicate 1-based indices (`step.index = 1`).
- **Root Cause:**
  1. **Pipeline Desynchronization & Staged Action Execution:** When a step was reordered before or at the currently active step (or when moving the active step downwards), `InteractiveConsoleListener.handleReorderStep` did not yield or reschedule the pipeline. Consequently, `pauseBeforeActionExecution` resumed execution with the *previously planned* actions of the old active step, immediately executing actions for the wrong step and marking pending steps as passed without running them.
  2. **Multi-Block Index Collision:** `InteractiveStateBuilder` assigns `step.index = stepIndex + 1` independently per section block (`before`, `steps`, `after`). In `interactive_console.js`, `handleDrop`, `handleDragStart`, and `reorderStepDirect` searched across blocks using only the duplicate integer index, resolving steps in `steps` to `before`.
  3. **Unprotected In-Flight Reorders:** Number input spinner clicks triggered `onchange` followed by `onblur` when clicking away, firing `reorderStepDirect` a second time with modified indices while the first reorder was in flight (`isStructuralActionInFlight` was not guarded).
- **Detection Gap ("What did we miss?"):** Existing test `testReorderStepsDuringPauseKeepsPauseAndUpdatesExecutionStack` only tested reordering between subsequent *future* steps (indices 1 and 2 while active step was 0), which did not require pipeline rescheduling. No test exercised moving a step before or at the active step, moving the active step downwards, or reordering steps across multi-block playbooks.
- **Resolution:**
  1. Updated `handleReorderStep` in `InteractiveConsoleListener` to detect when a reorder affects the active step (`isFromCurrent || isToBeforeOrAtCurrent || isTargetBefore`). When true, clears staged actions/tool-calls/reasoning on the active step, resets its status to `PlaybookStepStatus.PENDING`, marks `KEY_STEP_RESCHEDULED`, recalculates remaining steps on `runStack` from `firstPendingIdx`, and yields execution via `"RESCHEDULE"`.
  2. Scoped all frontend reordering operations (`renderBlock`, `renderStepCard`, `handleDragStart`, `handleDrop`, `reorderStepDirect`, `handleStepInputKeyDown`, `handleStepInputInput`) with `blockName` to eliminate multi-block index collisions.
  3. Guarded `reorderStepDirect` and `handleDrop` against `isStructuralActionInFlight || !currentPauseId`.
- **Safety Net Added:** Added regression unit tests in `InteractiveConsoleListenerTest.java`: `testReorderStepBeforeCurrentStepTriggersRescheduleAndYieldsExecution` and `testReorderActiveCurrentStepDownwardsTriggersReschedule`.
