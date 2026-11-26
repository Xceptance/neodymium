# [DEF-20261020-1130] Interactive Suggest Fix Modal Spinner Lock and Disabled Heal Button

- **Status:** `Resolved`
- **Opened:** 2026-10-20 11:30
- **Closed:** 2026-10-20 11:49
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In interactive test execution when a step fails:
  1. Clicking "Suggest Fix" opens a dialog with a spinner that never resolves even though the server finishes processing and pauses waiting for user action.
  2. Closing the dialog causes it to never open again when clicking "Suggest Fix" on a step.
  3. The "Heal" button remains permanently disabled after closing the dialog or during step failure pauses.
- **Root Cause:**
  1. `InteractiveConsoleListener.handleSuggestFix()` resolved step instructions solely from `currentStep` without checking `userAction.get("index")`, while `InteractiveStateBuilder` lacked state persistence fallback for generated fix suggestions.
  2. `closeOverlay()` sets inline `display: none` on the overlay, which `requestFixSuggestion()` failed to clear because it only toggled `.active` class without calling `openOverlay()` or clearing the inline style.
  3. `checkActionApprovals()` in `interactive_console.js` evaluated `!s.reasoning && !hasPlannedActions` as `isThinking = true` even for failed steps, disabling `btnRun` (which represents the "Heal" button in error mode).
- **Detection Gap ("What did we miss?"):**
  1. Existing UI unit test `InteractiveViewErrorRecoveryAndSuggestFixTest.java` directly invoked `Selenide.executeJavaScript("handleFixSuggestionReceived(...)")`, bypassing live SSE delivery and state synchronization.
  2. Dialog re-opening after dismissal was never asserted in test flows.
  3. Tests only checked the presence of the Heal button in error mode, not its disabled/enabled state across structural action pauses.
- **Resolution:**
  1. In `InteractiveConsoleListener.java`: Resolved step instruction using `userAction.get("index")` via `findStepByIndex()` and stored latest suggestion in context transient data for state embedding.
  2. In `InteractiveStateBuilder.java`: Included `fixSuggestion` in the state JSON payload when available in context transient data.
  3. In `interactive_console.js`:
     - Called `openOverlay('suggestionOverlay')` in `requestFixSuggestion()` to clear inline `display: none`.
     - Guarded `isThinking` in `checkActionApprovals()` to exclude failed steps (`!isFailedStep && ...`).
     - Added state fallback in `applyState()` to resolve active fix suggestion overlays and wrapped the SSE listener in safe JSON parsing.
- **Safety Net Added:**
  - `InteractiveConsoleListenerTest.testPauseOnStepFailureSuggestFixResolvesStepByIndexAndStoresState`
  - `InteractiveConsoleServerTest.testSuggestFixModalReopenAndHealButtonState`
  - `InteractiveViewErrorRecoveryAndSuggestFixTest.testSuggestFixModalWorkflow` (enhanced to verify Heal button enablement, dialog re-opening after dismissal, and live SSE event delivery without manual JS execution).
