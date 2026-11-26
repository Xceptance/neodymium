# [DEF-20261016-1005] Premature Run Button Enablement and Toolbar Control Coupling During Execution and AI Thinking

- **Status:** `Resolved`
- **Opened:** 2026-10-16 10:05
- **Closed:** 2026-10-16 10:15
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In the Aura interactive web console, the `#btnRun` ("Run") toolbar button became enabled while step actions were executing or while the AI was still in its reasoning/planning phase (before proposed actions or thoughts were returned). This caused user confusion about whether the step was finished or awaiting another confirmation. Simultaneously, other action buttons (`#btnSkip`, `#btnAuto`, and `#btnCancel`) were disabled during execution and AI thinking, preventing users from skipping steps, arming auto-run mode ahead of time, or cancelling the test while an action or LLM call was in flight.
- **Root Cause:**
  1. Monolithic button state coupling in `setButtonsEnabled(enabled)`: calling `setButtonsEnabled(false)` when `!currentPauseId || state.status === 'running'` unconditionally disabled `skipBtn` and `autoBtn` (`!enabled || isEditing`).
  2. Incomplete thinking guard in `checkActionApprovals()`: `isThinking` only verified that `s.actions` and `s.reasoning` were empty, failing to guard against top-level thinking state (`currentState.reasoning` containing "thinking") or step-level thinking messages.
  3. Lack of queued skip handling: `sendAction('SKIP')` immediately returned if `currentPauseId` was null, preventing users from clicking Skip during step execution or thinking.
  4. Bypassing `checkActionApprovals()`: multiple call sites of `setButtonsEnabled(true)` enabled `runBtn` directly via `runDisabled = !enabled || isEditing || isExecuting;` without validating whether AI thinking was active.
- **Detection Gap ("What did we miss?"):** Previous unit tests in `InteractiveConsoleServerTest.java` verified that `setButtonsEnabled(false)` was called when `!currentPauseId || state.status === 'running'`, but did not assert that `btnSkip`, `btnAuto`, and `btnCancel` must remain enabled independently of `btnRun`. Tests also did not verify queued skip handling (`pendingSkipAfterSync`) or thinking indicators in `state.reasoning`.
- **Resolution:**
  1. Decoupled toolbar controls in `interactive_console.js`:
     - `#btnSkip`, `#btnAuto`, and `#btnCancel` remain enabled during step execution and AI thinking (only disabled when inline editing `isEditing` is active).
     - Added `pendingSkipAfterSync` support in `sendAction('SKIP')` to arm a queued skip when clicked during execution/thinking, automatically dispatching `SKIP` as soon as the runner pauses for the next step.
     - Strengthened `checkActionApprovals()` to evaluate `isTopThinking` and `isStepThinking` so `#btnRun` stays disabled while AI reasoning is in flight.
     - Ensured `setButtonsEnabled` delegates `#btnRun` validation to `checkActionApprovals()` when enabling controls.
     - Removed initial static `disabled` attribute from `btnSkip` and `btnAuto` in `interactive_console.html`.
  2. Preserved Step 1 confirmation: Step 1 arrives in `"paused"` state with `currentPauseId` and planned actions ready, enabling `#btnRun` for initial user confirmation.
  3. Added regression test `testToolbarButtonsIndependentOfRunExecutionState` in `InteractiveConsoleServerTest.java` and updated existing assertions.
- **Safety Net Added:** `InteractiveConsoleServerTest#testToolbarButtonsIndependentOfRunExecutionState` and `InteractiveConsoleServerTest#testInteractiveConsoleJsRunButtonDisabledDuringExecutionAndThinking`.
