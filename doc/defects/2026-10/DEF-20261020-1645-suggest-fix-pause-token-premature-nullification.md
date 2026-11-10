# [DEF-20261020-1645] Suggest Fix Cannot Be Applied Due to Premature Pause Token Nullification in Frontend

- **Status:** `Resolved`
- **Opened:** 2026-10-20 16:45
- **Closed:** 2026-10-20 16:48
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** Clicking "Apply" or "Apply and Confirm" in the fix suggestion dialog temporarily updates step text and shows LLM thinking, but the step text reverts back to the original failed step and "Suggest Fix" reappears after a couple of seconds.
- **Root Cause:**
  1. `executeApplySuggestion` in `interactive_console.js` explicitly cleared `currentPauseId = null` and `window.currentPauseId = null` before calling `sendAction('EDIT', ...)`.
  2. Inside `sendAction(action, extra)`, a strict guard checked `if (!currentPauseId || !currentRunId) return;` without checking `extra?.pauseId` or other fallbacks. As a result, `sendAction` dropped the request immediately and never sent the POST request to `/api/console/action`, nor did it set `isRunActionInFlight = true`.
  3. The client's optimistic update (`applyState(currentState)`) set `targetStep.isLlmProcessing = true`, displaying the revised text and the thinking spinner.
  4. The server remained paused on the failed step and continued broadcasting state snapshots containing the un-edited step and `status: "failed"`. Because `isRunActionInFlight` was `false`, `applyState(data)` accepted the server's snapshot and replaced the optimistic UI, reverting the step text and re-rendering "Suggest Fix".
- **Detection Gap ("What did we miss?"):** Previous unit tests in `InteractiveConsoleListenerTest` directly submitted `EDIT` action JSON payloads to `consoleEngine.submitAction(...)` on the backend, bypassing the client-side JavaScript dispatch logic in `sendAction` and `executeApplySuggestion`.
- **Resolution:**
  1. In `sendAction(action, extra)` in `interactive_console.js`, robustly resolved the pause token via `const token = (extra && extra.pauseId) || currentPauseId || window.currentPauseId || currentState?.pauseId;`, guarding on `!token || !currentRunId` and using `token` for `runActionPauseId`, `structuralActionPauseId`, and `payload.pauseId`.
  2. In `executeApplySuggestion` in `interactive_console.js`, removed the premature nullification of `currentPauseId` and `window.currentPauseId` prior to calling `sendAction`, and explicitly forwarded `pauseId: activePauseId` in the `sendAction('EDIT', ...)` payload.
- **Safety Net Added:** Verified client action dispatch flow and backend handling in `InteractiveConsoleListenerTest` and `InteractiveConsoleServerTest`.
