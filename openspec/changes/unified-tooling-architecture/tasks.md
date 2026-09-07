## 1. Core Tool Abstractions

- [x] 1.1 Create `AiTool`, `ToolDefinition`, `ToolCall`, and `ToolResult` interfaces and records in `org.neodymium.ai.tool` and verify unit tests for immutability and schema access pass.
- [x] 1.2 Implement `ToolContext` interface providing `invokeTool`, session variable access, and artifact attachment, and verify mock invocation tests pass.
- [x] 1.3 Implement thread-safe `ToolRegistry` with registration, discovery, and lookup capabilities, and verify concurrent lookup tests pass.

## 2. Java Tool Reflection & Schema Generation

- [x] 2.1 Create `@AiTool` and `@ToolParam` annotations with proper retention and targets in `org.neodymium.ai.tool`.
- [x] 2.2 Implement `JavaToolFactory` to reflectively generate OpenAPI/JSON Schema from annotated Java methods using Jackson and verify schema generation unit tests pass.
- [x] 2.3 Implement argument deserialization and return value variable binding in `JavaTool` and verify multi-parameter typed invocations (`BigDecimal`, `int`, `String`, POJOs) pass.
- [x] 2.4 Migrate utility methods in `AiAssertions` directly to `@AiTool` and retire `@AiMethod` and `JavaMethodAction`, verifying assertion execution tests pass.

## 3. Browser & Active Discovery Tools

- [x] 3.1 Implement core `BrowserToolProvider` wrapping Selenide actions (`click`, `type`, `navigate`, `select`, `hover`, `assert_text`) as `AiTool`s with strict JSON Schemas.
- [x] 3.2 Implement active discovery tools (`browser_query_dom`, `browser_inspect`, `browser_scroll`, `browser_take_screenshot`, `browser_execute_script`) so the agent actively inspects page state on demand.
- [x] 3.3 Implement `browser_inspect_visual` (targeted bounding-box crop) and Set-of-Marks visual numeric badge overlay in `browser_take_screenshot(mark_interactive=true)`.
- [x] 3.4 Implement visual coordinate re-anchoring bridge via `document.elementFromPoint(x, y)` to resolve underlying DOM elements and generate resilient locators and `DomFeatureVector`s.
- [x] 3.5 Ensure browser action tools capture and preserve `DomFeatureVector`, candidate locators, and screenshot visual hashes, and verify vector capture tests pass.

## 4. Quality Judge Tool Guard

- [x] 4.1 Implement `QualityJudgeToolInterceptor` intercepting proposed `browser_*` tool calls before browser dispatch.
- [x] 4.2 Verify candidate locator scoring passes high-confidence ($\ge 0.95$) locators immediately and triggers deliberation for ambiguous candidates.
- [x] 4.3 Implement Journey Fidelity policy checks in `QualityJudgeToolInterceptor` to reject direct URL mutations or script redirects during interaction steps.

## 5. Agent Tool Loop Step & Stop Criteria

- [x] 5.1 Implement `AgentToolLoopStep` driving iterative tool execution (`Think` → `ToolCall` → `Observe` → `Finish`) in place of monolithic `CallLlmStep` + `ExecuteActionsStep`.
- [x] 5.2 Implement and unit test the six stop criteria: Goal Completion (`complete_step`), Assertion Failure (`AssertionError`), Thrashing / Stagnation detection, Token Budget Guard limits, Liberal Step Timeout (180s), and Fatal Environment Failure.
- [x] 5.3 Implement dynamic intent-based tool filtering in `AgentToolLoopStep` based on `SemanticIntent` (omitting `browser_navigate` for interactive steps).
- [x] 5.4 Retire legacy `ToLevelEscalationException`, `HealingRequiredException`, and brute-force context escalation ladders from step execution.

## 6. Composite Plugin Tools

- [x] 6.1 Implement `WcagAccessibilityTool` implementing `AiTool`, utilizing `context.invokeTool("browser_execute_script")` to execute Axe-core without direct `WebDriverRunner` dependencies.
- [x] 6.2 Verify `WcagAccessibilityTool` unit tests validate violation detection, formatting, and Allure report attachment.

## 7. Playbook Recording & Replay Engine

- [ ] 7.1 Update `PlaybookStep` and `Action` JSON serialization to support `toolCalls` while transparently reading legacy recorded `actions` arrays.
- [ ] 7.2 Update the playback execution loop to replay `toolCalls` directly via `toolRegistry.getTool(name).execute(...)` without LLM calls.
- [ ] 7.3 Verify offline replay preserves sub-millisecond similarity healing using recorded `DomFeatureVector` data when selectors change.
- [ ] 7.4 Verify Tier 2 offline visual dHash / tile SSIM matching for icon-only and textless elements without LLM calls.

## 8. Full Verification

- [ ] 8.1 Execute the test suite (`mvn clean test -Dtest=org.neodymium.ai.tool.**,org.neodymium.ai.pipeline.**`) and verify all new tooling and loop tests pass.
- [ ] 8.2 Execute existing regression tests (`SearchTest`, `AiAssertionsTest`) and verify zero regressions.
