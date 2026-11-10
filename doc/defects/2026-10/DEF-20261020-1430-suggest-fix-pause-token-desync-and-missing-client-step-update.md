# [DEF-20261020-1430] Suggest Fix Pause Token Desynchronization and Missing Client Step Update

- **Status:** `Resolved`
- **Opened:** 2026-10-20 14:15
- **Closed:** 2026-10-20 14:35
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** After the AI Suggest Fix modal presents a revised instruction, clicking "Apply only" or "Apply and Confirm" did not update the step text in the interactive web UI and failed to trigger the LLM replanning loop, leaving the runner paused or unresponsive.
- **Root Cause:**
  1. **Token Rotation Desynchronization:** `SUGGEST_FIX` was submitted as a structural action, causing `pauseOnStepFailure` to complete its cycle and generate a new pause token ($T_2$). The SSE `fixSuggestion` broadcast sent back to the browser previously did not carry the updated pause token, so the client retained the original token ($T_1$). When the user subsequently clicked "Apply only" or "Apply and Confirm", the resulting `EDIT` action carried $T_1$, which `InteractiveConsoleEngine.submitAction` rejected as a mismatched `pauseId` (`status: already-handled`), leaving the test runner thread blocked in `waitForAction(T_2)`.
  2. **Missing UI Optimistic State and Metadata:** `applySuggestionOnly()` and `applySuggestionAndRun()` in `interactive_console.js` failed to resolve block-specific indices (`block`, `blockIndex`, `flatIndex`), failed to update the step instruction and reset `failed` / `isLlmProcessing` flags on the local step object, and omitted calling `applyState(currentState)` to render the updated step card to the DOM before dispatching the `EDIT` action.
- **Detection Gap ("What did we miss?"):** Previous integration tests (`InteractiveConsoleListenerTest`) mocked and simulated `SUGGEST_FIX` and subsequent actions in isolation or injected simulated state objects without asserting the multi-step pause token rotation sequence that occurs across the live HTTP/SSE interface between `SUGGEST_FIX` and `EDIT`.
- **Resolution:**
  1. Extended `InteractiveConsoleEngine` to track `previousPauseId` alongside `currentPauseId`, providing a grace window where non-structural user actions carrying the immediately preceding token are accepted.
  2. Included the active `pauseId` in the `fixSuggestion` SSE event payload in `InteractiveConsoleListener.handleSuggestFix`, and updated the client-side SSE listener in `interactive_console.js` to synchronize `currentPauseId`.
  3. Re-architected suggestion application in `interactive_console.js` into `executeApplySuggestion(autoConfirm)` which resolves the block and step indices, optimistically updates `targetStep` properties (`instruction`, `status = 'running'`, `failed = false`, `isLlmProcessing = true`), immediately calls `applyState(currentState)`, and dispatches `EDIT` with full block metadata and `autoConfirm`.
- **Safety Net Added:**
  - `com.xceptance.neodymium.ai.console.InteractiveConsoleEngineTest#testSubmitActionAcceptsPrecedingPauseTokenGraceWindow`
  - `org.neodymium.ai.event.InteractiveConsoleListenerTest#testPauseOnStepFailureSuggestFixFollowedByApplyOnlyWithPrecedingToken`
