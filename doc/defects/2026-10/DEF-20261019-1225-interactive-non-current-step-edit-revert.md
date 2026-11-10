# [DEF-20261019-1225] Interactive Non-Current Step Edit Marked Active and Reverted

- **Status:** `Resolved`
- **Opened:** 2026-10-19 12:25
- **Closed:** 2026-10-19 12:35
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In interactive console mode, editing any non-current (pending) step card immediately marks that step as active/running with an LLM processing spinner. The runner unpauses and initiates LLM thinking on the active paused step using the instruction typed into the pending step. Once LLM reasoning concludes and a fresh state snapshot is emitted by the backend, the non-current step reverts back to its original instruction.
- **Root Cause:**
  1. In `interactive_console.js`, `saveEdit` mutated `targetStep.status = 'running'` and `targetStep.isLlmProcessing = true` before calculating `isCurrentStep`, forcing `isCurrentStep` to evaluate to `true` for all step edits.
  2. `isStructuralAction` did not check `extra.isCurrentStep`, failing to treat non-current step edits as structural pause-preserving mutations.
  3. `InteractiveConsoleListener.java` received `isCurrentStep: true`, causing it to mutate `currentStep` instead of `targetStep` and unpause execution (`return false`).
  4. Step index lookups in `saveEdit`, `enableEdit`, and `cancelEdit` did not scope by `data-step-block`, risking multi-block index collisions.
  5. `InteractiveConsoleServer.runSimulationInternal` unconditionally set `status = "running"` on edited steps without respecting `isCurrentStep: false`.
- **Detection Gap ("What did we miss?"):** Existing unit tests (`testEditNonCurrentStepDuringPauseKeepsPauseAndUpdatesTargetStep`, `testEditPendingStepUsingNormalizedZeroBasedIndex`) tested backend event processing with synthetic JSON payloads where `isCurrentStep` was explicitly set to `false`. All existing UI/HUD tests (`InteractiveViewSaveTest`) called `uiAdvanceToStep()` prior to `uiEditStep()`, only ever editing the currently active step and never testing inline editing of a pending step.
- **Resolution:**
  1. Refactored `saveEdit` in `interactive_console.js` to determine `isCurrentStep` prior to modifying `targetStep`, preserving `targetStep.status = 'pending'` and skipping LLM processing flags when editing non-current steps.
  2. Updated `isStructuralAction` in `interactive_console.js` to return `!extra.isCurrentStep` when present.
  3. Scoped step lookups in `saveEdit`, `enableEdit`, and `cancelEdit` by `data-step-block` to avoid multi-block collisions.
  4. Transmitted normalized `blockIndex`, `flatIndex`, `rawIndex`, and explicit `isCurrentStep: false` to the backend.
  5. Added `blockIndex` and `flatIndex` resolution fallbacks in `InteractiveConsoleListener.java`.
  6. Updated `InteractiveConsoleServer.runSimulationInternal` to scope block searches by `block`, match steps using `flatIndex`/`rawIndex`/`blockIndex`, and preserve `pending` status and pause state on non-current step edits.
- **Safety Net Added:** Added unit regression tests `testEditPendingStepInMultiBlockPlaybookPreservesPendingStatusAndPause` in `InteractiveConsoleListenerTest.java`, `testInteractiveConsoleJsSaveEditPreservesPendingStatusForNonCurrentSteps` in `InteractiveConsoleServerTest.java`, and full UI simulation test `testEditPendingStepWithoutAdvancing_Global` in `InteractiveViewSaveTest.java`.
