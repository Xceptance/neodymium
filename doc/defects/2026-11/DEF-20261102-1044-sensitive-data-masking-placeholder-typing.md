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
  2. In `AgentToolLoopStep.java`, resolved `executableCall = ToolCallVariableResolver.resolveVariables(effectiveCall, context.getSessionData())` immediately before `tool.execute(...)` and before legacy `executor.execute(...)`, while preserving `effectiveCall` with `${...}` in recorded tool calls.
  3. Sanitized `ToolResult` content using `sessionData.getRawSensitiveData()` via `LlmSanitizerHelper.maskSensitiveValues(...)` before storing in `conversation` (`ChatMessage.tool(...)`).
  4. In `LlmSanitizerHelper.toSanitizedRequest(...)`, sanitized all outbound messages and assistant `toolCalls()` arguments using recursive Jackson AST textual node traversal (`maskObjectNode` / `maskArrayNode`), avoiding raw string serialization or Jackson re-parsing.
  5. Implemented single-pass regex alternation `(\[MASKED_VAR_[^\]]+\])|(...)` with length-descending secrets in `LlmSanitizerHelper.maskSensitiveValues(...)`, protecting `[MASKED_VAR_...]` tokens from corruption without sentinel markers. Centralized `DefaultContextSanitizer` to delegate prompt and state text masking directly to `LlmSanitizerHelper.maskSensitiveValues(...)`.
  6. In `AgentToolLoopStep.java`, caught `UnresolvableVariableException` during single-shot action execution and gracefully fell back to the interactive agent loop with diagnostic feedback instead of fatal `ConclusiveFailureException`.
  7. In `ToolCallVariableResolver.java`, preserved uninitialized self-referential store variable placeholders while resolving available sibling placeholders via `sessionData.resolveAvailableVariables(...)`, validated that all non-target sibling variables are resolvable (throwing `UnresolvableVariableException` if an unknown/misspelled sibling is referenced), and added fast-path `${` checks before string replacement.
  8. Refactored `PlaybookToolReplayer.java` to reuse `ToolCallVariableResolver.java`, removing unused iterator imports.
  9. In `AgentToolLoopStep.java`, preserved preceding successfully executed tool calls when a subsequent action in a single-shot batch encounters an `UnresolvableVariableException` during fallback to interactive mode (avoiding `executedCalls.clear()`).
  10. In `AgentToolLoopStep.java`, ensured `ActionExecutedEvent` constructs canonical action from `effectiveCall` (retaining `${...}` placeholders) prior to sanitization rather than `mappedAction` (which contained raw decrypted secrets).
  11. In `LlmSanitizerHelper.java`, introduced `maskJsonOrTextContent(...)` which performs AST-aware masking on valid JSON string literals while preventing regex corruption of numeric and boolean literals, and applied it to tool results in `AgentToolLoopStep.java`.
- **Safety Net Added:**
  - `AgentToolLoopStepTest#testSensitiveVariablePlaceholderResolvedInToolExecutionWhilePreservingPlaceholderInRecording` asserting resolved execution, parameterized recording, and sanitized multi-turn conversation.
  - `AgentToolLoopStepTest#testSensitiveVariableMaskedOverWireAndUnmaskedInResponseThroughMockLlmProvider` asserting end-to-end wire masking over `MockLlmProvider`, inbound unmasking, resolved tool execution, and parameterized recording.
  - `AgentToolLoopStepTest#testSingleShotActionWithUnresolvableVariableFallsBackToInteractiveLoop` verifying graceful single-shot fallback to interactive agent loop upon encountering an unresolvable variable placeholder.
  - `AgentToolLoopStepTest#testSingleShotBatchPreservesPrecedingExecutedCallsWhenSubsequentActionFailsWithUnresolvableVariable` ensuring preceding actions are preserved in playback steps when falling back from a batch failure.
  - `AgentToolLoopStepTest#testSingleShotActionExecutedEventDispatchesMaskedRawAction` verifying `ActionExecutedEvent` dispatches sanitized action derived from placeholder-bearing call.
  - `ToolCallVariableResolverTest` verifying unit resolution across nested objects, arrays, fast path, and store / browser_store tool target preservation.
  - `ToolCallVariableResolverTest#testResolveVariablesSkipsUnresolvableCompositeSelfReferentialStoreValue` and `#testResolveVariablesResolvesSiblingWhilePreservingUnresolvableSelfReferentialStoreValue` verifying composite store value handling.
  - `ToolCallVariableResolverTest#testResolveVariablesStrictlyFailsOnMissingSiblingInCompositeStoreValue` asserting missing sibling placeholders in composite store expressions fail fast.
  - `LlmSanitizerHelperTest#testMaskSensitiveValuesPreventsTokenCorruptionWhenSecretIsSubstringOfAnotherKey` and `#testMaskSensitiveValuesWithSingleDigitAndPunctuationSecrets` validating collision-free and sentinel-free regex alternation masking.
  - `LlmSanitizerHelperTest#testMaskSensitiveValuesWithDuplicateSecretValuesAcrossKeys` validating duplicate secret handling.
  - `LlmSanitizerHelperTest#testToSanitizedRequestSanitizesToolCallArgumentsInConversation` and `#testToSanitizedRequestHandlesJsonEscapedCharactersInToolCallArguments` verifying AST-based wire sanitization of assistant tool call arguments and JSON escaping.
  - `LlmSanitizerHelperTest#testMaskJsonOrTextContentPreservesValidJsonWhenSecretIsNumericOrBoolean` verifying JSON structure and boolean/numeric tokens remain intact during tool result masking.
