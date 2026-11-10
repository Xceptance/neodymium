# [DEF-20261020-0945] Interactive Console Reschedule Flag Leak on Rewind & Incomplete Return Button Availability

- **Status:** `Verified`
- **Opened:** 2026-10-20 09:45
- **Closed:** 2026-10-20 10:00
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:**
  1. On the interactive console HUD, the return/rewind button was missing on active steps, failed steps, and pending steps.
  2. After rewinding back to a step, the LLM generated a proposed action, but upon confirming execution (`RUN`), the action was not executed and the runner advanced immediately to the next step.
- **Root Cause:**
  1. `KEY_STEP_RESCHEDULED` flag was set in `InteractiveConsoleListener.handleRewindStep()` but never cleared for the rewound target step. When `AgentToolLoopStep.java` paused and resumed with `"RUN"`, line 782 evaluated `Boolean.TRUE.equals(context.getTransientData().get("KEY_STEP_RESCHEDULED"))` as true. This caused `AgentToolLoopStep` to invoke `finishLoop(..., "Rescheduled"); return;`, abandoning the proposed actions.
  2. In `interactive_console.js`, `.step-rewind-btn` was guarded behind `(isPassed || isFailed || isSkipped) && !showEdit`. Because `showEdit` is defined as `step.status !== 'passed' && step.status !== 'skipped'`, `!showEdit` is false for failed steps, hiding the button on failed steps. Active (`running`) and pending steps were also excluded, preventing rewinding/retrying the active step or jumping directly to any step card.
- **Detection Gap ("What did we miss?"):** Prior rewind unit tests verified that the listener yielded `"REWIND"` and repopulated the stack, but did not execute a subsequent `AgentToolLoopStep` with `"RUN"` to verify that actions were executed.
- **Resolution:**
  1. Replaced `context.getTransientData().put("KEY_STEP_RESCHEDULED", Boolean.TRUE);` with `context.getTransientData().remove("KEY_STEP_RESCHEDULED");` in `InteractiveConsoleListener.handleRewindStep()`.
  2. Changed `Boolean.TRUE.equals(context.getTransientData().get("KEY_STEP_RESCHEDULED"))` to `.remove("KEY_STEP_RESCHEDULED")` in `AgentToolLoopStep.java` and `ExecuteActionsStep.java` so any reschedule flag is consumed immediately.
  3. Made `.step-rewind-btn` available strictly on steps that have passed (`status === 'passed' || status === 'healed'`), ensuring rewind buttons are not offered on steps that have not yet passed.
- **Safety Net Added:** Added unit test `testRewindExecutionFollowedByAgentToolLoopActionExecutionRunsActions` in `InteractiveConsoleListenerTest.java`.
