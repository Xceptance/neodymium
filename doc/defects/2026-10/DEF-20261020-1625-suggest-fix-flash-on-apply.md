# [DEF-20261020-1625] Suggest Fix Button Flashes on Applying Suggestion in Interactive Console

- **Status:** `Resolved`
- **Opened:** 2026-10-20 16:25
- **Closed:** 2026-10-20 16:28
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In interactive mode, clicking "Apply" (`applySuggestionOnly`) or "Apply and Confirm" (`applySuggestionAndRun`) in the AI instruction fix suggestion dialog causes the UI to briefly re-display the failed step with red error styling and the "Suggest Fix" button for 1–2 seconds before replacing it with the revised step text.
- **Root Cause:**
  1. `executeApplySuggestion` in `interactive_console.js` did not traverse `subSteps` and relied on strict index equality (`===`), causing sub-step and type-divergent step lookups to fail optimistic updating.
  2. `sendAction('EDIT')` did not set `isRunActionInFlight = true` or register `runActionPauseId`, allowing rotated failure pause and state events dispatched by the server during token rotation after `SUGGEST_FIX` to overwrite the client's optimistic running state.
  3. `applyState` reset `userEditedFailedStep = false` whenever `failedIdx` cleared, re-enabling error mode and re-displaying the "Suggest Fix" button on subsequent state events.
  4. The backend `InteractiveConsoleListener.handleUserAction` for `EDIT` did not clean up `KEY_LAST_STEP_FAILURE`, `KEY_LAST_EXECUTION_ERROR`, and `lastFixSuggestion` transient context attributes.
- **Detection Gap ("What did we miss?"):** Existing interactive console unit and integration tests verified that `SUGGEST_FIX` generated a suggestion and that `EDIT` successfully updated the instruction on the server, but did not assert step card DOM states in the intermediate window between modal dismissal and server action execution.
- **Resolution:**
  - Updated `executeApplySuggestion` in `interactive_console.js` to locate steps across blocks and nested sub-steps using loose equality, optimistically set `targetStep.status = 'running'`, clear failure flags, and transition `currentState.status = 'running'`.
  - Equipped `sendAction` with in-flight execution guards (`isRunActionInFlight = true`, `runActionPauseId`) for `EDIT` actions to drop stale in-flight pause events.
  - Updated `applyState` to preserve optimistic blocks during in-flight run/edit actions and avoid wiping `userEditedFailedStep` when `failedIdx` clears.
  - Cleaned up transient failure attributes (`KEY_LAST_STEP_FAILURE`, `KEY_LAST_EXECUTION_ERROR`, `lastFixSuggestion`) in `InteractiveConsoleListener` upon editing the current failed step.
- **Safety Net Added:** Added `testPauseOnStepFailureEditClearsTransientFailureAndPushesRunningState` and updated `testPauseOnStepFailureSuggestFixAndApplyOnlyCarryingRotatedPrecedingTokenAccepted` in `InteractiveConsoleListenerTest` verifying transient error cleanup and state updates on `EDIT`.
