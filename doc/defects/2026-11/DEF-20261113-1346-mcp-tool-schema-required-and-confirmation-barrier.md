# [DEF-20261113-1346] MCP Tool Optional Parameters Marked Required, Same-Turn Confirmation Bypass, and Substring Action Detection

- **Status:** `Resolved`
- **Opened:** 2026-11-13 13:15
- **Closed:** 2026-11-13 13:47
- **Component:** `aura-manager`
- **Scope:** `AI/Prompt & Framework`
- **Symptom:**
  1. Generated JSON schemas for Spring AI chat tool callbacks listed optional `@McpToolParam` arguments (such as `list_tests.filter`, `run_test.browserProfile`/`dataset`, `delete_test.confirmationToken`, `cancel_run.confirmationToken`) in the JSON Schema `"required"` array, causing strict LLM tool-call validators (Gemini/OpenAI/Mistral) to require or hallucinate dummy values for optional fields.
  2. When a user asked to delete a test or cancel a run, the two-step `TwoStepConfirmationManager` confirmation token returned by `delete_test` / `cancel_run` appeared in the Spring AI tool response and `thinking` trace, allowing the LLM to immediately invoke the tool a second time in the exact same turn and execute destructive operations without waiting for user confirmation.
  3. Concurrent chat requests could overwrite a global `ThreadLocal<ToolExecutionObserver>` fallback if observer registration raced or callbacks were reused across threads.
  4. `AuraChatService.detectUiAction` used substring checks (`contains("SUCCESS")`) and checked stale `"delete"` / `"run_tests"` / `"execute"` names that no longer matched the 11 canonical MCP tool names (`create_test`, `delete_test`, `run_test`, `run_suite`, `cancel_run`), causing error messages containing `"SUCCESS"` to trigger UI refreshes while `ALREADY_COMPLETED` or `NOT_FOUND` on `cancel_run` was unhandled cleanly.

- **Root Cause:**
  1. `@McpToolParam` annotations in `AuraMcpToolService` relied on Spring AI MCP's default `required = true` value, and `AuraMcpToolCallbackProvider.enrichInputSchemaWithMcpParams` only copied `description` from `@McpToolParam` without regenerating the top-level JSON Schema `"required"` array.
  2. `AuraChatService` relied solely on system prompt instructions to respect `CONFIRMATION_REQUIRED`, rather than enforcing a session-scoped server barrier across turns.
  3. `AuraMcpToolCallbackProvider` cached a single `ToolCallback[]` backed by a `static ThreadLocal<ToolExecutionObserver>`.
  4. `AuraChatService.detectUiAction` inspected raw response strings with substring matching instead of parsing the JSON `status` field.

- **Detection Gap ("What did we miss?"):**
  - `AuraMcpToolCallbackProviderTest` previously asserted only that `testPath` was present under `properties` in `run_test`'s schema, never inspecting the `"required"` array, and no multi-turn test simulated an LLM attempting same-turn confirmation token reuse or error text containing `"SUCCESS"`.

- **Resolution:**
  1. Annotated all `@McpToolParam` parameters explicitly with `required = true` or `required = false` in `AuraMcpToolService`, and updated `AuraMcpToolCallbackProvider.enrichInputSchemaWithMcpParams` to rebuild the JSON Schema `"required"` array exclusively from parameters marked `required = true`.
  2. Added per-turn `AuraMcpToolCallbackProvider.createToolCallbacks(ToolExecutionObserver)` wrapping each callback invocation directly (removing the mutable static `ThreadLocal` and wrapping delegate exceptions with `[ERROR]` observer reporting).
  3. Added session-scoped `ConfirmationBarrierToolCallback` in `AuraChatService` that captures `confirmationToken` from `CONFIRMATION_REQUIRED` responses, redacts raw tokens from chat `thinking` traces, blocks same-turn retries (`CONFIRMATION_PENDING_USER`), and transparently injects valid tokens only in the subsequent user turn.
  4. Replaced substring matching in `AuraChatService.detectUiAction` with strict JSON `status` parsing for `create_test`, `delete_test`, `run_test`, `run_suite`, and `cancel_run`.

- **Safety Net Added:**
  - `AuraMcpToolCallbackProviderTest.testAllElevenMcpToolsRegisteredWithEnrichedSchemasAndRequiredFields`: Verifies exact `"required"` arrays for `run_test`, `list_tests`, and `delete_test`.
  - `AuraMcpToolCallbackProviderTest.testCallbackObserverReceivesErrorSummaryWhenDelegateThrows`: Verifies delegate `ToolExecutionException` propagation and `[ERROR]` observer notification.
  - `AuraChatServiceTest.testSameTurnConfirmationBarrierBlocksAutoExecutionUntilNextUserTurn`: Verifies same-turn retry blocking, token redaction in `thinking`, and second-turn execution.
  - `AuraChatServiceTest.testDetectUiActionParsesJsonStatusWithoutFalsePositives`: Verifies strict JSON status classification against error/already-completed false positives.
