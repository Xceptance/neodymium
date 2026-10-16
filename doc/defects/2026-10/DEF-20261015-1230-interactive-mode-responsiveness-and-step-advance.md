# [DEF-20261015-1230] Interactive Mode UI Responsiveness and Step Advance Desynchronization

- **Status:** `Resolved`
- **Opened:** 2026-10-15 12:30
- **Closed:** 2026-10-15 12:45
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In interactive mode, clicking "Run" or "Heal" on a paused step left the user uncertain whether step execution had actually begun because the step card remained static, the "Run" button remained or became re-enabled before the step completed, and upon step completion the "Run" button became enabled while the UI still highlighted the already-passed step as current, misleading the user into thinking the passed step still needed confirmation.
- **Root Cause:**
  1. `InteractiveConsoleListener.pauseBeforeActionExecution()` unblocked from `consoleEngine.waitForAction()` and returned `"RUN"` without broadcasting an updated state (`pushCurrentState(context, step)`), leaving the backend silent and the SSE console holding the previous paused state snapshot until the next step event or action completed seconds later.
  2. `interactive_console.js` had missing optimistic UI feedback in `sendAction()`: when the user confirmed with "Run", "Heal", or "Auto", the client disabled buttons but did not optimistically mark the active step as running and re-render the card, delaying spinner feedback.
  3. In `interactive_console.js:applyState()`, block rendering (`renderBlock()`) was executed *before* computing step auto-advance. Furthermore, lines preserving `selectedStepIndexForDetails` during pause (`if (currentPauseId !== null && selStepExists)`) unconditionally preserved the selection even if the selected step had already completed (`status === 'passed' | 'skipped' | 'healed'`), keeping the passed step highlighted.
  4. Compound step sub-step tracking in `interactive_console.js` lacked auto-advance in both `applyState()` and `updateBigScreenDetails()`, leaving `selectedSubStepIndex` locked on previously completed sub-steps.
  5. `checkActionApprovals()` in `interactive_console.js` lacked a guard checking step completion, enabling the "Run" button if `activeCard` pointed to a completed step during transient state syncs.
- **Detection Gap ("What did we miss?"):** Existing tests in `InteractiveConsoleListenerTest` tested pause entry and user action returns, but did not assert that `InteractiveConsoleEngine.getCurrentStateJson()` transitioned from `"paused"` with a valid `pauseId` to `"running"` with `pauseId` cleared immediately upon unpausing. End-to-end tests ran without verifying UI selection advance prior to action button re-enablement.
- **Resolution:**
  1. Updated `InteractiveConsoleListener.pauseBeforeActionExecution()` and `pauseOnStepFailure()` to set `step.setStatus(PlaybookStepStatus.RUNNING)` and immediately invoke `pushCurrentState(context, step)` upon unpausing for execution actions (`RUN`, `STEP`, `AUTO`, `RUN_ALL`, `CONTINUE`, `HEAL`, `RETRY`).
  2. Updated `sendAction()` in `interactive_console.js` to optimistically clear `currentPauseId`, set `currentState.status = 'running'`, mark the active step and sub-step as running, and re-render blocks immediately with 0ms visual latency.
  3. Reordered `applyState()` in `interactive_console.js` so that step and sub-step selection auto-advance occurs *before* `renderBlock()`, ensuring that completed steps (`passed`, `skipped`, `healed`) immediately release selection to the active or next pending step, and added auto-scrolling to the active step.
  4. Extended `updateBigScreenDetails()` to auto-advance step details and sub-step tab selection to the active running sub-step when previously selected items are completed.
  5. Added an explicit guard in `checkActionApprovals()` ensuring the "Run" button is never enabled if the active step is already completed.
- **Safety Net Added:** Added automated regression unit tests in `InteractiveConsoleListenerTest.java`:
  - `testPauseBeforeActionExecutionBroadcastsRunningStateUponUnpausing`: verifies that `RUN` unpausing immediately marks the step as `RUNNING` and broadcasts state with `status="running"` and cleared `pauseId`.
  - `testPauseOnStepFailureBroadcastsRunningStateUponRecovery`: verifies that `HEAL` unpausing immediately marks the step as `RUNNING` and broadcasts state with `status="running"` and cleared `pauseId`.
  - Updated `testReorderDuringPauseMaintainsPausedStateWithoutSpuriousRunningBroadcast` to assert intermediate paused state after `REORDER` and running state transition after `RUN`.
