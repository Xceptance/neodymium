# [DEF-20261004-1320] Agent tool loop mutated PlaybookStep with unexecuted proposed actions before tool execution

- **Status:** `Resolved`
- **Opened:** 2026-10-04 12:45
- **Closed:** 2026-10-04 13:20
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** When an LLM turn proposed actions and the subsequent tool call failed, was rejected, or aborted (such as via thrashing detection, pause, or runtime exception), `PlaybookStep` was left containing the unexecuted proposed actions in `step.getActions()` and `step.getToolCalls()`. In recording modes, this risked committing bogus or unexecuted actions to test artifacts.
- **Root Cause:** In `AgentToolLoopStep.java`, lines 679-683 directly called `step.setToolCalls(sanitizedCalls)` and `step.setActions(proposedActions)` during each turn immediately after parsing the LLM response, prior to tool execution. Actual verified actions are properly sanitized and committed to the step only upon loop completion (`step.setActions(actions)` at line 1780).
- **Detection Gap ("What did we miss?"):** Tests in `AgentToolLoopStepTest` previously validated happy-path execution or focused on token budget exhaustion without verifying that `step.getActions()` remained unpolluted when a tool failed or raised an exception during execution.
- **Resolution:** Removed the premature mutation of `step.setToolCalls(...)` and `step.setActions(...)` from the turn loop in `AgentToolLoopStep.java`. For interactive debugging preview in `interactiveListener.pauseBeforeActionExecution(...)`, wrapped the preview assignment in a `try`/`finally` block that restores the step's previous actions and tool calls so interactive inspection does not leak unexecuted proposals into the step.
- **Safety Net Added:** Added `@Test` `AgentToolLoopStepTest.testAgentToolLoopDoesNotCommitUnexecutedProposalsOnFailure`, verifying that when a tool explodes during turn execution, unexecuted proposals are not retained in `step.getActions()`.
