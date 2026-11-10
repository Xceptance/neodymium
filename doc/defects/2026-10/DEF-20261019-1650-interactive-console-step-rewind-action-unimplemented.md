# [DEF-20261019-1650] Interactive Console Step Rewind Action Unimplemented

- **Status:** `Verified`
- **Opened:** 2026-10-19 16:50
- **Closed:** 2026-10-19 17:15
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In interactive console mode, clicking the return/rewind button on an executed step card ("Rewind execution back here") or clicking the toolbar Back button does not rewind execution to that step. Instead, the currently paused active step is immediately confirmed and executed.
- **Root Cause:** In `InteractiveConsoleListener.java`, `handleUserAction()` completely lacked a `case "REWIND":` branch. The action fell through to `default:`, which logged a single-step execution message and returned `false`. This signaled `pauseBeforeActionExecution()` to unpause and execute the current step. Furthermore, `InteractiveConsoleListener` lacked rewind step orchestration (resetting target and subsequent steps to `PENDING`, clearing executed actions and tool calls, purging outdated report step entries, resetting the execution stack with the target step at the top, and yielding the current step).
- **Detection Gap ("What did we miss?"):** Prior interactive rewind tests (`InteractiveViewExecutionEdgeCasesTest.java`) executed against `InteractiveConsoleServer.runSimulation()` rather than live pipeline execution driven by `InteractiveConsoleListener` and `StateMachineRunner`.
- **Resolution:**
  1. Added `case "REWIND": return handleRewindStep(userAction, currentStep, context);` to `InteractiveConsoleListener.handleUserAction()`.
  2. Implemented `handleRewindStep()`:
     - Locates the target step by `block` and `blockIndex` or 1-based `targetIndex` in `playbook.flatSteps` (or fallback to preceding step for `btnBack`).
     - Resets target step and all subsequent steps to `PlaybookStepStatus.PENDING`, clearing their actions, tool calls, reasoning, and duration/error fields.
     - Cleans up `TestExecutionReport` step entries, LLM calls, and screenshots at and after the target step index.
     - Updates `ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP` to `targetStep`, sets `KEY_STEP_RESCHEDULED = true`, and re-populates `context.runStack` with the target step at the top.
     - Returns `false` so `pauseBeforeActionExecution()` yields with `"REWIND"`.
  3. In `AgentToolLoopStep.java` and `ExecuteActionsStep.java`, handled `"REWIND"` alongside `"RESCHEDULE"` by yielding immediately without executing actions.
  4. In `StateMachineRunner.java`'s `pauseOnStepFailure()`, handled `"REWIND"` and `"RESCHEDULE"` to `continue mainLoop;`.
  5. In `interactive_console.js`, enriched `triggerRewind()` with `block` and `blockIndex` while preserving existing function signatures, and updated `btnBack` disabled guard for 1-based indexing (`s.index <= 1`).
- **Safety Net Added:** Added `@Test` methods `testRewindExecutionToPreviousStepTriggersYieldAndReschedulesTargetStep`, `testRewindExecutionFromCoreStepToBeforeBlockStep`, and `testRewindExecutionOnStepFailure` in `InteractiveConsoleListenerTest.java`.
