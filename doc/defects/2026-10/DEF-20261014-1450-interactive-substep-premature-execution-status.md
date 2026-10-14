# [DEF-20261014-1450] Interactive Mode Prematurely Marks Sub-Step as Executed Before Action Execution

- **Status:** `Resolved`
- **Opened:** 2026-10-14 14:50
- **Closed:** 2026-10-14 14:52
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In interactive execution of compound steps with sub-steps, when the runner pauses before action execution, the first sub-step is prematurely marked as executed/passed (`check_circle`, tab `✓`, badge `1/2 Sub-steps`), and the right-hand details panel states "ACTIONS PERFORMED", before the user has clicked "Run" or any action has executed.
- **Root Cause:**
  1. `AgentToolLoopStep.applySubStepStatuses` marked all sub-steps prior to `lastActiveIdx` as `SUCCESS` using combined `liveCalls`, without checking whether their actions actually originated from `executedCalls` or were merely unexecuted `sanitizedCalls` (proposed actions). When the LLM proposed multiple actions in turn 1 covering multiple sub-steps, the first sub-step was marked `SUCCESS` before execution began.
  2. `interactive_console.js` called `buildStepDetailsHtml` without active pause context, displaying "Actions performed" instead of "Proposed Actions", and displayed a progress spinner rather than a pause indicator on the active sub-step.
- **Detection Gap ("What did we miss?"):** Previous tests in `AgentToolLoopStepTest` tested sequential turn partitioning where actions arrived one per turn, but omitted the multi-shot turn scenario where multiple proposed actions are partitioned to sub-steps during an initial live pause prior to any action execution.
- **Resolution:**
  1. Updated `AgentToolLoopStep.partitionToolCallsAndActions` and `applySubStepStatuses` to track executed actions vs proposed actions. A sub-step is only marked `SUCCESS` if its actions originate from `executedActions`. If its actions are in `proposedActions`, it is marked `RUNNING` (active proposed sub-step) or `PENDING` (subsequent proposed sub-step).
  2. Updated `interactive_console.js` to pass pause context (`isStepPaused`) to `buildStepDetailsHtml` (showing "Proposed Actions") and render `pause_circle` / `⏸` for active sub-steps when `currentPauseId !== null`.
- **Safety Net Added:** Unit tests in `AgentToolLoopStepTest.testPartitionToolCallsAndActionsLiveTurnSetsRunningAndPendingAndSuccess` verifying status isolation when proposed calls span multiple sub-steps with zero executed actions.
