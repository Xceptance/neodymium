# [DEF-20260924-04] Agent Loop Misclassifies Plain-Text Action Failure Report as InvalidAgentResponseException

- **Status:** Resolved
- **Opened:** 2026-09-24
- **Closed:** 2026-09-24
- **Component:** `neodymium-core` (`AgentToolLoopStep`)
- **Scope:** `Framework`
- **Symptom:** `HoverIntegrationTest.testHoverNonExistentElementFailure` threw `InvalidAgentResponseException: Agent turn did not produce a valid tool call after warning` instead of failing conclusively with `ConclusiveFailureException` when hovering over a missing element (`Hover over 'Place Order'`).
- **Root Cause:** Operating Rule 3 in the agent prompt directs: *"If an action fails (e.g., target element is disabled, missing, or non-interactable), DO NOT substitute uncommanded assertions and DO NOT call 'complete_step'; report the failure."* In `HoverIntegrationTest`, after `hover` failed on Turn 1 and exploratory DOM queries confirmed the element was missing, the LLM faithfully followed Rule 3 by reporting the failure in plain text without proposing tools. However, `AgentToolLoopStep` blindly treated any turn without tool calls (`proposedCalls == null || proposedCalls.isEmpty()`) as an invalid response, sent a warning scolding the model to invoke `complete_step`, and threw `InvalidAgentResponseException` on repeat. This misclassified an expected SUT action failure as an agent communication glitch and bypassed Visual RCA failure classification.
- **Detection Gap ("What did we miss?"):** Previous tests (e.g. `ClickIntegrationTest`) worked by accident because the model gave in to the warning and called `complete_step`, which was intercepted at line 719. Unit tests for `AgentToolLoopStepTest` only tested action failure rejection when the agent explicitly called `complete_step`, never when the agent adhered to Rule 3 and reported failure via plain text.
- **Resolution:** Updated `AgentToolLoopStep` so that when `hasActionToolFailed` is true for an interactive action instruction and no tool calls are proposed, the runner recognizes that the agent has concluded failure and immediately throws `ConclusiveFailureException` with the action failure details instead of issuing an invalid response warning or throwing `InvalidAgentResponseException`.
- **Safety Net Added:** Unit test in `AgentToolLoopStepTest.testActionFailureReportedInPlainTextThrowsConclusiveFailureException`.
