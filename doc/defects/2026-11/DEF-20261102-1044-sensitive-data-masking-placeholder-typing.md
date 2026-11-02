# [DEF-20261102-1044] Sensitive Data Masking Causes Unresolved Variable Placeholder to be Executed in Browser Tools

- **Status:** `Resolved`
- **Opened:** 2026-11-02 10:44
- **Closed:** 2026-11-02 12:02
- **Component:** `neodymium-core`
- **Scope:** `Framework & AI/Prompt`
- **Symptom:** During live AI execution, when input text references sensitive variables (e.g. `${user.password}`), browser tools type the literal string `"${user.password}"` into web elements instead of the actual credential, failing authentication.
- **Root Cause:** In `AgentToolLoopStep`, `LlmSanitizerHelper.unmaskResponse(...)` converts AI masked tokens `[MASKED_VAR_key]` back to `${key}`. While `PlaybookToolReplayer` resolved `${...}` against `SessionData` before executing replayed calls, `AgentToolLoopStep` passed `effectiveCall` directly to `tool.execute(...)` without just-in-time variable resolution. Furthermore, tool execution results returned to the agent loop were not sanitized before being appended as `ChatMessage.tool(...)` messages, creating a secondary leak risk across multi-turn interactions.
- **Detection Gap ("What did we miss?"):** Unit tests in `AgentToolLoopStepTest` only tested plain non-masked tool arguments or verified `effectiveCall` contents without validating the actual string received inside `tool.execute(...)` with dynamic `SessionData`. Replay tests in `PlaybookToolReplayerTest` covered resolution, but replay and live loop did not share a common execution variable resolver.
- **Resolution:**
  1. Extracted common tool call variable resolution logic into `ToolCallVariableResolver.java` (`resolveVariables(ToolCall, SessionData)`).
  2. In `AgentToolLoopStep.java`, resolved `executableCall = ToolCallVariableResolver.resolveVariables(effectiveCall, context.getSessionData())` immediately before `tool.execute(...)`, while preserving `effectiveCall` with `${...}` in recorded tool calls.
  3. Sanitized `ToolResult` content using `sessionData.getRawSensitiveData()` before storing in `conversation` (`ChatMessage.tool(...)`).
  4. In `LlmSanitizerHelper.toSanitizedRequest(...)`, ensured all messages in the conversation are sanitized against sensitive data map.
  5. Refactored `PlaybookToolReplayer.java` to reuse `ToolCallVariableResolver.java`.
- **Safety Net Added:**
  - `AgentToolLoopStepTest#testSensitiveVariablePlaceholderResolvedInToolExecutionWhilePreservingPlaceholderInRecording` asserting resolved execution, parameterized recording, and sanitized multi-turn conversation.
  - `AgentToolLoopStepTest#testSensitiveVariableMaskedOverWireAndUnmaskedInResponseThroughMockLlmProvider` asserting end-to-end wire masking over `MockLlmProvider`, inbound unmasking, resolved tool execution, and parameterized recording.
  - `ToolCallVariableResolverTest` verifying unit resolution across nested objects, arrays, and store tool target preservation.
