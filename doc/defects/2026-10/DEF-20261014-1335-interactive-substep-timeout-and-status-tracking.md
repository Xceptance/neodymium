# [DEF-20261014-1335] Interactive Pause Wall-Clock Timeout Leak and Unpartitioned Sub-Step Status Tracking

- **Status:** `Resolved`
- **Opened:** 2026-10-14 13:35
- **Closed:** 2026-10-14 13:45
- **Component:** `neodymium-core`
- **Scope:** `Framework & AI/Prompt`
- **Symptom:** In interactive debugging mode, spending more than 60 seconds reviewing a step or compound sub-steps causes the step to immediately fail with "Step timeout of 60s exceeded (elapsed: 80s)" when clicking Run/Next. Additionally, sub-steps in compound turn groups remain in PENDING status (0/2 Sub-steps) without showing running or completed status markers while the step executes.
- **Root Cause:**
  1. `AgentToolLoopStep` evaluated step wall-clock timeout against an immutable `startTimeMs` that continuously accumulated time while the runner was parked in `InteractiveConsoleListener.pauseBeforeActionExecution`.
  2. `partitionToolCallsAndActions` was only executed upon step conclusion in `finishLoop` or `ExecuteActionsStep`, leaving `subSteps` in `PENDING` status without actions during intermediate interactive pauses.
- **Detection Gap ("What did we miss?"):** Automated tests executed interactive pauses synchronously with immediate mock actions (<300ms), failing to simulate human inspection time (>60s) during intermediate compound turns.
- **Resolution:**
  1. Added centralized interactive pause tracking to `ExecutionContext` and compensated `startTimeMs` in `AgentToolLoopStep` and `ExecuteActionsStep` by subtracting all pause durations from wall-clock timers.
  2. Scaled compound step timeouts proportionally to `step.getSubSteps().size()`.
  3. Extended `partitionToolCallsAndActions` with live turn awareness (`isLiveTurn`) and invoked it before `pauseBeforeActionExecution` and after turn executions, setting finished sub-steps to `SUCCESS`, in-progress sub-steps to `RUNNING`, and future sub-steps to `PENDING`.
- **Safety Net Added:** `AgentToolLoopStepTest.testInteractivePauseCompensatesWallClockTimeout`, `AgentToolLoopStepTest.testPartitionToolCallsAndActionsLiveTurnSetsRunningAndPendingAndSuccess`, `InteractiveConsoleListenerTest.testPauseBeforeActionExecutionRecordsPauseDurationInExecutionContext`, `InteractiveConsoleListenerTest.testActionExecutedEventPushesRunningStateToConsole`, and `InteractiveReasoningAndActionsUiTest.testSubStepsTreeRenderingAndTogglingInInteractiveView`.
