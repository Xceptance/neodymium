# [DEF-20261002-12] Interactive Console Displays Empty Step Details Without Proposed Tools or LLM Reasoning

- **Status:** Resolved
- **Opened:** 2026-10-02
- **Closed:** 2026-10-02
- **Component:** `neodymium-core` (`AgentToolLoopStep.java`, `ExecuteActionsStep.java`)
- **Scope:** `Framework`
- **Symptom:** In interactive debugging mode, when execution paused at a step, the "Step Details" panel displayed "No actions recorded yet" and contained no LLM reasoning text or proposed tool calls.
- **Root Cause:** `AgentToolLoopStep.java` only populated `PlaybookStep.setActions()` and `setToolCalls()` at the very end of step execution after tools were already executed on SUT, and `ExecuteActionsStep.java` triggered `pauseBeforeActionExecution` before LLM invocation occurred.
- **Detection Gap ("What did we miss?"):** Unit tests tested `pauseBeforeActionExecution` in isolation without verifying that `PlaybookStep` contained mapped proposed actions and reasoning before tool execution.
- **Resolution:** Updated `AgentToolLoopStep` to map proposed LLM tool calls and reasoning onto `PlaybookStep` immediately upon receiving the LLM response, and call `pauseBeforeActionExecution` before executing tools on SUT. Prompt edits trigger LLM re-invocation to generate updated proposals.
- **Safety Net Added:** Added unit test `testAgentToolLoopPausesWithProposedActionsAndReasoningInInteractiveMode` in `AgentToolLoopStepTest.java`.
