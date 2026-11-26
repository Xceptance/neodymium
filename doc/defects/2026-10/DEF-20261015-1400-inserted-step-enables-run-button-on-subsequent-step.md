# [DEF-20261015-1400] Inserted Step Enables Run Button on Subsequent Step During Execution

- **Status:** `Resolved`
- **Opened:** 2026-10-15 14:00
- **Closed:** 2026-10-15 14:23
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** During interactive test execution, after inserting a new step before or at the current active step, the step after the inserted step has the "Run" toolbar button enabled while step execution is actively in progress or while waiting for execution. Furthermore, the newly inserted step was prematurely coerced to "passed" in the console UI before executing, and the shifted subsequent step executed instead of the newly inserted step.
- **Root Cause:**
  1. In `InteractiveConsoleListener.java`: `handleAddStep` added the new step into `playbook.steps`, but because `currentStep` was the step paused before insertion (shifted to $N+1$), `pauseBeforeActionExecution` continued pausing on the shifted step ($N+1$). Furthermore, `rescheduleRemainingSteps` only added steps starting from `currentIdx + 1`, completely omitting the newly inserted step at index $\le \text{currentIdx}$ from `remaining` and `runStack`.
  2. In `InteractiveStateBuilder.java`: Lines 728–735 coerced any step with status `PENDING` into `PlaybookStepStatus.SUCCESS` whenever `stepIndex < activeStepIndex`, causing newly inserted pending steps located before `activeStepIndex` to be falsely serialized and rendered as "passed" (green checkmark).
  3. In `interactive_console.js`: In `checkActionApprovals`, `isThinking` was calculated as `s && s.status === 'running' && !s.reasoning && !hasPlannedActions && currentPauseId === null && !isStructuralActionInFlight`. Because the shifted step retained planned actions from before insertion, `isThinking` evaluated to `false`, falling into the `else` branch which explicitly invoked `runBtn.disabled = false` even while `currentPauseId === null` or during execution. In addition, `saveEdit` synchronously called `setButtonsEnabled(true)` while an asynchronous `ADD` action was in flight.
- **Detection Gap ("What did we miss?"):**
  Previous unit test `testAddStepDuringPauseKeepsPauseAndUpdatesExecutionStack` in `InteractiveConsoleListenerTest` only tested inserting a step *after* the currently executing step (`index = 1` while paused on `step1` at `index = 0`), which masked the scheduling and pause-token retargeting failure that occurs when inserting *before* or *at* the active step. Additionally, tests for `InteractiveStateBuilder` did not verify serialization of pending steps positioned before `activeStepIndex`.
- **Resolution:**
  1. Updated `InteractiveConsoleListener.java`:
     - In `handleAddStep`: When `targetIndex <= currentIdx`, cleared stale actions, tool calls, and reasoning on `currentStep`, reset its status to `PlaybookStepStatus.PENDING`, and marked `KEY_STEP_RESCHEDULED = true`. Returned `false` so the pause loop does not re-pause on `currentStep`.
     - In `pauseBeforeActionExecution`: If `KEY_STEP_RESCHEDULED` is set, returned `"RESCHEDULE"` immediately without executing or marking `currentStep` as `RUNNING`.
     - In `rescheduleRemainingSteps`: When `KEY_STEP_RESCHEDULED` is set, collected all pending steps from the earliest pending index onwards, ensuring the newly inserted step is placed on top of `runStack` followed by `currentStep` and subsequent steps.
     - Handled `KEY_STEP_RESCHEDULED` in `AgentToolLoopStep`, `VerifyOutcomeStep`, and `ExecuteActionsStep.endHookStep` to cleanly yield execution so the newly inserted step executes first.
  2. Updated `InteractiveStateBuilder.java`:
     - In `serializeStep`: Prevented `PlaybookStepStatus.PENDING` from being coerced to `SUCCESS` when `stepIndex < activeStepIndex`.
  3. Updated `interactive_console.js`:
     - In `checkActionApprovals`: Guarded `runBtn` to be strictly disabled whenever `!currentPauseId`, `currentState.status === 'running'`, `isStructuralActionInFlight`, or `isAutoMode` is true.
     - In `saveEdit`: Removed premature `setButtonsEnabled(true)` for `isTemp` insertions and reset `selectedStepIndexForDetails = null`.
- **Safety Net Added:**
  - Automated unit test `InteractiveConsoleListenerTest.testAddStepBeforeCurrentReschedulesExecutionStackAndClearsStaleStateOnShiftedStep` verifying that inserting before the active step reschedules the execution stack with the new step on top, returns `"RESCHEDULE"`, and resets stale proposals on the shifted step.
  - Automated unit test `InteractiveConsoleEngineTest.testPendingStepBeforeActiveStepIndexIsNotCoercedToSuccess` verifying that pending steps before `activeStepIndex` serialize as `"pending"`.
  - Automated unit test `InteractiveConsoleServerTest.testInteractiveConsoleJsSaveEditDoesNotPrematurelyEnableButtons` verifying that `saveEdit` does not prematurely enable buttons when `ADD` is in flight.
