# [DEF-20260929-02] Legacy Action Format STORE Parameter Mapping Discrepancy Causes Missing variableName Tool Error

- **Status:** Resolved
- **Opened:** 2026-09-29
- **Closed:** 2026-09-29
- **Component:** `neodymium-core` (`action-mapping` / `browser-tool-provider` / `AgentToolLoopStep`)
- **Scope:** `Framework`
- **Symptom:** Executing `StoreIntegrationTest.testStoreMock` failed with `ConclusiveFailure: Action execution failed: {"status":"ERROR","message":"store requires a non-empty 'variableName'","error":"store requires a non-empty 'variableName'"}` during recording, and subsequently failed `REPLAY_STRICT` due to the voided recording file.
- **Root Cause:** Legacy `Action` objects and mock LLM actions defined element text store as `locator: "#id", value: "varName"`. When converted to tool calls in `AgentToolLoopStep`, `storedOrderId` remained in `value` and was not mapped to `variableName`. Similarly, `Action.toToolCall()` omitted an explicit case for `STORE`, defaulting to storing the variable name in `args.value`. `BrowserToolProvider` strictly mandated `args.hasNonNull("variableName")`, causing execution rejection.
- **Detection Gap ("What did we miss?"):** Unit tests for `BrowserStoreToolTest` tested native tool call structures (`variableName: "..."`), while `StoreActionTest` tested legacy `Action` executions via `SelenideTargetExecutor`. The bridge conversion between legacy action JSON candidates and `BrowserToolProvider`'s `store` tool was not covered in unit isolation.
- **Resolution:** Added `store` argument normalization in `AgentToolLoopStep.parseToolCallFromCandidate()` and `Action.toToolCall()`, and added fallback extraction in `BrowserToolProvider` when `value` is supplied alongside a selector without explicit `variableName`.
- **Safety Net Added:** Verified `StoreIntegrationTest` in both `FORCE_RECORDING` and `REPLAY_STRICT`, along with unit tests in `BrowserStoreToolTest` and `ActionTest`.
