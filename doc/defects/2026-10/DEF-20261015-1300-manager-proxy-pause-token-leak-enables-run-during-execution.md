# [DEF-20261015-1300] Manager Proxy Pause Token Leak Re-enables Run Button and Shows Paused Icon During Step Execution

- **Status:** `Resolved`
- **Opened:** 2026-10-15 13:00
- **Closed:** 2026-10-15 13:10
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** During interactive test execution connected to Aura Manager (`neodymium.managerUrl`), while a playbook step was actively executing in the test runner, the Aura Manager dashboard iframe displayed the "Run", "Skip", and "Auto" action buttons enabled instead of disabled. Furthermore, the active step card rendered the `⏸` (`pause_circle`) icon and "Proposed Actions" header rather than the spinning progress indicator (`progress_activity`) and "Executing Actions...". This gave the misleading impression that the step had not started or required re-confirmation.
- **Root Cause:** Two interrelated mechanisms caused the defect:
  1. In `InteractiveConsoleEngine.waitForAction(customPauseId, timeoutMs)`: When `AiConfiguration.getInstance().isManagerActive()` was `true`, the runner thread proxied action retrieval via HTTP GET to `/api/console/internal/waitForAction`. When Aura Manager returned HTTP 200 with the user action (e.g. `{"action":"RUN"}`), the method directly returned without clearing `this.currentPauseId` (which was only cleared at the end of the standalone lock block on line 781).
  2. In `InteractiveConsoleEngine.pushState(stateJson)`: When the test runner subsequently unpaused and began executing the step, `InteractiveConsoleListener.pushCurrentState` broadcast a state snapshot with `status: "running"` and `pauseId: null`. However, `pushState` had a fallback `else if (this.currentPauseId.get() != null) { state.addProperty("pauseId", this.currentPauseId.get()); }`. Because `this.currentPauseId` leaked from `waitForAction`, `pushState` re-injected the stale `pauseId` into the `"running"` state payload and sent it to Manager, which broadcast it over SSE to the frontend.
  3. In `interactive_console.js`: `applyState(state)` checked `if (state.pauseId)` prior to checking `state.status === 'running'`, re-arming `currentPauseId` in the browser, calling `setButtonsEnabled(true)`, and rendering `pause_circle` on the running step card.
- **Detection Gap ("What did we miss?"):** Existing unit tests for `InteractiveConsoleEngine` and `InteractiveConsoleListener` tested standalone mode without `isManagerActive()=true` and did not verify that calling `pushState` with a `"running"` status purged any pre-registered or internal `currentPauseId` from both the engine reference and the resulting state JSON.
- **Resolution:**
  1. Wrapped `InteractiveConsoleEngine.waitForAction` in a `try { ... } finally { this.currentPauseId.set(null); }` block, ensuring that `currentPauseId` is unconditionally invalidated on all exit paths in both standalone and Manager proxy modes.
  2. Updated `InteractiveConsoleEngine.pushState` to check `isRunning` (`running`); when true, it explicitly clears `this.currentPauseId.set(null)` and removes any `pauseId` property from the state JSON before broadcasting. Furthermore, when state lacks an explicit `pauseId`, stale tokens are never re-injected.
  3. Updated `interactive_console.js`:
     - In `applyState`: nullified `currentPauseId` whenever `state.status === 'running'`.
     - In `applyState`: guarded button enabling with `if (!currentPauseId || state.status === 'running') { setButtonsEnabled(false); }`.
     - In `checkActionApprovals`: immediately disabled `runBtn` if `!currentPauseId || (currentState && currentState.status === 'running')` or `s.status === 'running'`.
     - In `buildStepDetailsHtml`: rendered spinning progress indicator with "Executing Actions..." label when the step is actively running.
     - In `renderStepCard`: rendered `progress_activity` spinner whenever `step.status === 'running'` and guarded `isPausedRunning` against active running states.
- **Safety Net Added:** Added unit tests in `InteractiveConsoleEngineTest`:
  - `testPushStateRunningStatusPurgesPauseIdAndClearsCurrentPauseId`: verifies that `pushState` with `"running"` status clears `currentPauseId` in the engine and removes the `pauseId` property from the generated state JSON.
  - `testPushStateCompletedStatusWithoutPauseIdPurgesPauseId`: verifies that `pushState` with completed statuses lacking a pause token clears `currentPauseId` and strips `pauseId`.
  - `testPushStateCompletedStatusWithExplicitPauseIdPreservesPauseId`: verifies that `pushState` with explicit completion pause tokens (e.g. `pause-final-...`) preserves `currentPauseId` for user review.
  - `testWaitForActionClearsCurrentPauseIdOnCompletion`: verifies that `waitForAction` invalidates `currentPauseId` upon completion.
