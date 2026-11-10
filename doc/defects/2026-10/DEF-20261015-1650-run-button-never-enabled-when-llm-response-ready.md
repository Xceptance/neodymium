# [DEF-20261015-1650] Run Button Stays Disabled When LLM Response Is Ready Due to Step Running Status Guard

- **Status:** `Resolved`
- **Opened:** 2026-10-15 16:50
- **Closed:** 2026-10-15 16:52
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** During interactive test execution, after the AI completes its reasoning and proposes actions for a step, the "Run" button remains permanently disabled even though the runner is paused and waiting for user input.
- **Root Cause:** In `interactive_console.js`, `hasAnyRunningStep = allSteps.some(s => s.status === 'running')` was added to `applyState`, `setButtonsEnabled`, and `checkActionApprovals`. Because the active step undergoing execution retains `step.status === 'running'` throughout the interactive pause (prior to completion), `hasAnyRunningStep` evaluated to `true`, incorrectly treating the paused runner as actively executing and permanently disabling `btnRun`.
- **Detection Gap ("What did we miss?"):** Previous unit tests asserted the literal presence of `hasAnyRunningStep` string in JS function bodies rather than evaluating the state transition logic where `currentPauseId` is present simultaneously with `step.status === 'running'`.
- **Resolution:** Removed the unconditional `hasAnyRunningStep` checks from `applyState`, `setButtonsEnabled`, and `checkActionApprovals`, relying on `!currentPauseId || (currentState && currentState.status === 'running') || isStructuralActionInFlight` to govern execution state while preserving targetCard isolation (`activeCard || (currentPauseId ? document.querySelector('.step-card.selected-details') : null)`).
- **Safety Net Added:**
  - Automated unit test in `InteractiveConsoleServerTest.java`: `testInteractiveConsoleJsRunButtonDisabledDuringExecutionAndThinking`
  - Automated unit test in `InteractiveConsoleServerTest.java`: `testRunButtonTargetCardIsolationAndPauseGuard`
