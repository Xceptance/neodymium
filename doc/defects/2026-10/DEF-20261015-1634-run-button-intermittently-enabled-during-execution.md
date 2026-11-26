# [DEF-20261015-1634] Run Button Intermittently Enabled During Step Execution on Specific Steps

- **Status:** `Resolved`
- **Opened:** 2026-10-15 16:34
- **Closed:** 2026-10-15 16:37
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** During interactive test execution, the "Run" toolbar button intermittently becomes enabled while step actions are actively executing against the browser for specific steps (e.g. only Step 2, Steps 1 & 2, or Step 1 & Last Step), while correctly remaining disabled during other steps.
- **Root Cause:**
  1. In `interactive_console.js`: `checkActionApprovals` and `setButtonsEnabled` only evaluated top-level `currentState.status === 'running'`, omitting a check for whether any individual step card has `step.status === 'running'`. When action events or sub-step transitions occurred, `activeCard` fell back to `document.querySelector('.step-card.selected-details')`, evaluating approval on a pending step with pre-recorded/template actions rather than the actively running step.
  2. In `ExecuteActionsStep.java`: Steps with pre-recorded actions (`hasRecordedCalls == true`) pause immediately before execution without an AI thinking phase, whereas dynamic steps enter `AgentToolLoopStep` where the AI thinking phase kept the button disabled.
- **Detection Gap ("What did we miss?"):** Existing unit tests verified that `btnRun` is disabled during the thinking phase and when `currentState.status === 'running'`, but did not test scenarios where another step is selected in `selectedStepIndexForDetails` while a step is executing, or where individual steps report `status: 'running'` during action replay.
- **Resolution:**
  1. Updated `interactive_console.js`:
     - In `applyState`: computed `hasAnyRunningStep = allSteps.some(s => s.status === 'running')` and guarded button enabling with `if (!currentPauseId || state.status === 'running' || hasAnyRunningStep) { setButtonsEnabled(false); }`.
     - In `checkActionApprovals`: computed `hasAnyRunningStep` and incorporated it into `isExecuting` and the disabled state check (`if (isThinking || isExecuting || hasAnyRunningStep)`).
     - Decoupled card targeting: ensured `targetCard` only falls back to `.step-card.selected-details` when `currentPauseId` is present (explicitly paused), never during active execution.
     - In `setButtonsEnabled`: added `hasAnyRunningStep` to `isExecuting` guard to prevent premature enabling.
- **Safety Net Added:**
  - Automated unit test in `InteractiveConsoleServerTest.java`: `testRunButtonRemainsDisabledWhenAnyStepStatusIsRunningEvenIfAnotherStepIsSelected`
  - Automated unit test in `InteractiveConsoleServerTest.java`: `testInteractiveConsoleJsRunButtonDisabledDuringExecutionAndThinking` updated with `hasAnyRunningStep` assertions
