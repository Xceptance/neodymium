# [DEF-20261020-1245] Suggest Fix Modal "Apply Only" Prematurely Executes Actions Due to Undisarmed Auto Mode and Missing Protocol Flag

- **Status:** `Resolved`
- **Opened:** 2026-10-20 12:45
- **Closed:** 2026-10-20 12:50
- **Component:** `neodymium-core`
- **Scope:** `Framework & Test/Harness`
- **Symptom:** Selecting "Apply only" on the AI Fix Suggestion modal updates the step instruction and triggers LLM planning, but then immediately executes the proposed actions on the SUT without waiting for user review and confirmation.
- **Root Cause:**
  1. `interactive_console.js` failed to disarm `isAutoMode` upon step failure pauses (as `state.status` is `"paused"` rather than `"failed"`), allowing `triggerAutoRunCheck()` to dispatch a background `RUN` action 500ms after the step was cleared of failure flags by `applySuggestionOnly()`.
  2. `applySuggestionOnly()` and `applySuggestionAndRun()` both dispatched generic `EDIT` actions without a deterministic `autoConfirm` flag, while `applySuggestionAndRun()` relied on a race-prone `setTimeout(..., 150)` client timer rather than an explicit server-side one-shot confirmation protocol.
- **Detection Gap ("What did we miss?"):** Prior tests only checked that `EDIT` updated the step instruction in memory or unpaused `pauseOnStepFailure()`, but did not test the subsequent transition through `pauseBeforeActionExecution()` to assert that "Apply only" keeps the runner halted awaiting explicit user confirmation.
- **Resolution:**
  1. Updated `interactive_console.js` to unconditionally disarm `isAutoMode`, `autoStartStepIndex`, and `pendingRunAfterSync` on step failure in `applyState()` and in `applySuggestionOnly()`.
  2. Introduced an explicit `autoConfirm: true/false` flag in the `EDIT` action payload in `interactive_console.js`, backed on the server by a one-shot `KEY_AUTO_CONFIRM_ONCE` transient flag consumed in `InteractiveConsoleListener.pauseBeforeActionExecution()`.
  3. Renamed the modal button from "Approve & Run" to "Apply and Confirm" in `interactive_console.html`.
- **Safety Net Added:**
  - `InteractiveConsoleListenerTest.testApplyOnlyPausesBeforeActionExecutionAwaitingUserConfirmation`: Verifies that an edit with `autoConfirm: false` (or omitted) halts at `pauseBeforeActionExecution()` and awaits explicit user confirmation.
  - `InteractiveConsoleListenerTest.testApplyAndConfirmAutoConfirmsExecutionOnce`: Verifies that an edit with `autoConfirm: true` auto-confirms execution of proposed actions exactly once.
