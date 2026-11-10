# [DEF-20261019-1712] Interactive Console Current Step Edit Skips Execution and Prematurely Confirms Step

- **Status:** `Resolved`
- **Opened:** 2026-10-19 17:10
- **Closed:** 2026-10-19 17:20
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In interactive console mode, editing the currently active step's instruction and saving causes the UI to briefly show "AI is thinking..." before immediately transitioning to "confirmed" (green checkmark / passed) without executing any browser action on the SUT.
- **Root Cause:**
  1. In `ExecuteActionsStep.java`, when the user edited the active step during `pauseBeforeActionExecution`, `InteractiveConsoleListener.handleUserAction` cleared `step.getActions()` and `step.getToolCalls()`, but left `step.getRecordedStatus()`, `step.getScreenshotHash()`, and `initialStepStatus` intact. Consequently, `isReplay` evaluated to `true`, dispatching `PlaybookToolReplayer.replayStep` rather than `AgentToolLoopStep`.
  2. `PlaybookToolReplayer.replayStep` found an empty `toolCalls` list, immediately returned success, and `endHookStep` marked the step as `PlaybookStepStatus.SUCCESS`.
  3. In `AgentToolLoopStep.java`, local variables `instruction` and `rawInstruction` were immutable and never refreshed upon edit, and SUT attachments were not refreshed when continuing the loop.
  4. In `InteractiveConsoleServer.java`, standalone simulation mode fell through without pausing on current-step edit.
- **Detection Gap ("What did we miss?"):** Existing test `testInteractiveModePromptEditUpdatesInstructionAndReEvaluatesInPipeline` in `ExecuteActionsStepTest` only asserted string equality on `step.getInstruction()`, without verifying that `isReplay` was disabled, recorded data invalidated, or that `AgentToolLoopStep` was chosen over `PlaybookToolReplayer`.
- **Resolution:**
  1. In `ExecuteActionsStep.java`, detected current step edits during `pauseBeforeActionExecution`. Invalidated `recordedStatus` and `screenshotHash`, set `step.setNoReplay(true)`, and set `KEY_CURRENT_STEP_NO_REPLAY` and `KEY_IS_HEALED_STEP` in transient data so `isReplay` evaluates to `false` and dispatches `AgentToolLoopStep`.
  2. In `InteractiveConsoleListener.java`, ensured `currentStep.setRecordedStatus(null)`, `currentStep.setScreenshotHash(null)`, `currentStep.setNoReplay(true)`, and transient flags are populated whenever the current step is edited.
  3. In `AgentToolLoopStep.java`, updated the active `instruction` and `rawInstruction` variables, refreshed SUT state/attachments, and reset loop guards upon edit.
  4. In `InteractiveConsoleServer.java`, ensured simulation mode continues and re-pauses on current-step edit.
- **Safety Net Added:**
  - `ExecuteActionsStepTest.testInteractiveModeCurrentStepEditDispatchesAgentToolLoopAndInvalidatesReplay`
  - `ExecuteActionsStepTest.testInteractiveModePromptEditUpdatesInstructionAndReEvaluatesInPipeline` (strengthened assertions)
  - `InteractiveConsoleListenerTest.testCurrentStepEditInvalidatesRecordedStatusAndSetsNoReplay`
