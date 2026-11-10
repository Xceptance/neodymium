# [DEF-20261015-1100] Interactive Console Spurious AI Thinking and Button Lockout on Step Add/Reorder

- **Status:** `Resolved`
- **Opened:** 2026-10-15 11:00
- **Closed:** 2026-10-15 11:12
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** Adding a step or moving/reordering steps during an interactive console pause triggered a false "AI Thinking" bubble with a spinning progress icon on the active step and disabled or hid the "Run" / confirm button for 1.5 to 2.5 seconds.
- **Root Cause:**
  1. Frontend Client State Invalidation: `interactive_console.js`'s `sendAction()` function unconditionally wiped `currentPauseId = null` and `currentState.pauseId = null` for all actions, including structural in-pause operations (`ADD`, `REORDER`). Step rendering evaluated `const isThinking = step.status === 'running' && !step.reasoning && currentPauseId === null && step.source !== 'recording';`. Because `currentPauseId` was wiped, `isThinking` evaluated to `true`, causing the card to replace its planned actions with the purple "AI Thinking" bubble.
  2. Button Visibility Inadvertent Hide: In `setButtonsEnabled(false)`, `if (!enabled && currentState && currentState.status !== 'running')` checked whether status was not `'running'`. Since paused tests have `status: 'paused'`, this condition evaluated to `true` and completely hid all toolbar controls (`display = 'none'`) during pauses.
  3. Redundant Screenshot Capture: In `InteractiveConsoleListener.pauseBeforeActionExecution()`, every loop iteration of the pause `while (true)` loop invoked `Selenide.screenshot(OutputType.BASE64)` and base64-encoded the image. Because structural operations (`ADD`, `REORDER`, non-current step `EDIT`) only modify the test playbook without changing the browser page DOM, re-taking full-page screenshots was redundant and blocked the runner thread for 500ms–1500ms before rotating the pause token and emitting SSE updates.
- **Detection Gap ("What did we miss?"):** Previous tests verified execution context step mutation on `REORDER` and `ADD`, but did not simulate frontend client-side pause token lifecycles, `isThinking` predicate guards, or screenshot idempotency across in-pause loop iterations.
- **Resolution:**
  1. Frontend: Added `isStructuralAction()` in `interactive_console.js` to distinguish playbook structural mutations (`ADD`, `REORDER`, `TOGGLE_BREAKPOINT`, `SUGGEST_FIX`, non-current step `EDIT`) from execution-resuming actions (`RUN`, `SKIP`, `AUTO`, `HEAL`, `ABORT`, current step `EDIT`). Structural actions no longer wipe `currentPauseId` or `currentState.pauseId` and track an `isStructuralActionInFlight` flag.
  2. Frontend: Guarded `isThinking` across step cards, accordions, and action approval checks by checking `!hasPlannedActions && !isStructuralActionInFlight`, ensuring cards with planned actions never flicker into an "AI Thinking" state.
  3. Frontend: Fixed `setButtonsEnabled()` to only hide toolbar buttons when test execution is finished (`passed`, `failed`, or `aborted`). Added a buffered confirm handshake (`pendingRunAfterSync`) so rapid Run clicks during structural turnaround automatically execute when the rotated pause token arrives.
  4. Backend: Optimized `InteractiveConsoleListener.pauseBeforeActionExecution()` with an `isInitialPause` flag, capturing `Selenide.screenshot()` only on initial pause entry and skipping redundant capture on structural loop iterations.
- **Safety Net Added:** `InteractiveConsoleListenerTest.testStructuralActionsDoNotRecaptureScreenshotAndRotatePauseId`
