# [DEF-20261015-1545] Run Button Enabled During Step Action Execution

- **Status:** `Resolved`
- **Opened:** 2026-10-15 15:45
- **Closed:** 2026-10-15 15:58
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** During interactive test execution, while step actions (clicks, keystrokes, form fills, assertions, and page navigations) are actively executing against the browser SUT, the "Run" toolbar button remains enabled instead of disabled across every step. The button is correctly disabled during AI thinking, but gets re-enabled as soon as actions begin executing.
- **Root Cause:**
  1. In `interactive_console.js`: `setButtonsEnabled(enabled)` unconditionally computed `runBtn.disabled = !enabled || isEditing; if (!enabled || isEditing) runBtn.setAttribute('disabled', 'true'); else runBtn.removeAttribute('disabled');` without checking whether a step was actively executing (`!currentPauseId || currentState.status === 'running' || isStructuralActionInFlight || isAutoMode`).
  2. In `checkActionApprovals()`: `isThinking` was guarded for the LLM thinking phase (`!hasPlannedActions`), which successfully kept the button disabled while the model was formulating thoughts/actions, but during the subsequent action execution phase (`hasPlannedActions === true`), `isThinking` evaluated to `false`. When any state update or UI event called `setButtonsEnabled(true)`, it bypassed `checkActionApprovals()` or re-enabled `runBtn`.
- **Detection Gap ("What did we miss?"):**
  Existing unit tests in `InteractiveConsoleServerTest` verified that `checkActionApprovals()` disabled `runBtn` when `isThinking` was `true`, but did not test that calling `setButtonsEnabled(true)` when `currentState.status === 'running'` or `currentPauseId === null` keeps `runBtn` strictly disabled while allowing other buttons to follow their standard enable/disable rules.
- **Resolution:**
  1. Updated `setButtonsEnabled(enabled)` in `interactive_console.js`:
     - Added execution guard: `const isExecuting = (!currentPauseId) || (currentState && currentState.status === 'running') || isStructuralActionInFlight;`
     - Bound `runBtn` disabled state: `const runDisabled = !enabled || isEditing || isExecuting;`
     - Ensured `runBtn` is only enabled when `enabled === true`, `isEditing === false`, and `isExecuting === false`.
  2. Refined `checkActionApprovals()` in `interactive_console.js`:
     - Preserved the user-approved disabled state during `isThinking` (`!s.reasoning && !hasPlannedActions && currentPauseId === null && !isStructuralActionInFlight`).
     - Added fallback guard ensuring `runBtn.disabled = true` if `!activeCard`.
     - Enforced `runBtn.disabled = true` whenever `isThinking || isExecuting`.
- **Safety Net Added:**
  - Automated unit test in `InteractiveConsoleServerTest.java` (`testInteractiveConsoleJsRunButtonDisabledDuringExecutionAndThinking`) verifying that `setButtonsEnabled(true)` in `interactive_console.js` strictly leaves `btnRun.disabled = true` whenever `currentPauseId === null` or `currentState.status === 'running'`, while maintaining `btnRun.disabled = true` during `isThinking`.
