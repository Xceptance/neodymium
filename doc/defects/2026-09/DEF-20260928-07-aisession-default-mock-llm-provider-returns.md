# [DEF-20260928-07] AiSession Default Mock LLM Provider Returns Empty Tool Calls Breaking LLM-Mode Unit Tests

- **Status:** Resolved
- **Opened:** 2026-09-28
- **Closed:** 2026-09-28
- **Component:** `neodymium-core` (`ai-session` / `mock-testing` / `agent-loop`)
- **Scope:** `Framework`
- **Symptom:** `AiSessionTest.testExecuteInlineStepsString`, `testExecutePlaybook`, and `testExecuteInlineYamlAutoSelectsFirstDataSet` fail with `InvalidAgentResponseException: Agent turn did not produce a valid tool call after warning`.
- **Root Cause:** `AiSession.createMockLlmProvider()` returned `new LlmResponse("[]", ...)` with empty tool calls. Under the tightened agent loop contract (`AgentToolLoopStep`), turns must produce a tool call (such as `complete_step`) or be rejected.
- **Detection Gap ("What did we miss?"):** When `AgentToolLoopStep` added mandatory tool call enforcement in commit 87651a09a, `createMockLlmProvider()` in `AiSession.java` was not updated to return a default `complete_step` tool call.
- **Resolution:** Updated `AiSession.createMockLlmProvider()` to return an `LlmResponse` populated with a `complete_step` `ToolCall`.
- **Safety Net Added:** `AiSessionTest` suite execution verifying all 17 unit tests pass cleanly.
