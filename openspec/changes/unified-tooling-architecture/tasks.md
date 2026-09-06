## 1. Core Tool Abstractions

- [ ] 1.1 Create `AiTool`, `ToolDefinition`, `ToolCall`, and `ToolResult` interfaces and records in `org.neodymium.ai.tool` and verify unit tests for immutability and schema access pass.
- [ ] 1.2 Implement `ToolContext` interface providing `invokeTool`, session variable access, and artifact attachment, and verify mock invocation tests pass.
- [ ] 1.3 Implement thread-safe `ToolRegistry` with registration, discovery, and lookup capabilities, and verify concurrent lookup tests pass.

## 2. Java Tool Reflection & Schema Generation

- [ ] 2.1 Create `@AiTool` and `@ToolParam` annotations with proper retention and targets in `org.neodymium.ai.tool`.
- [ ] 2.2 Implement `JavaToolFactory` to reflectively generate OpenAPI/JSON Schema from annotated Java methods using Jackson and verify schema generation unit tests pass.
- [ ] 2.3 Implement argument deserialization and return value variable binding in `JavaTool` and verify multi-parameter typed invocations (`BigDecimal`, `int`, `String`, POJOs) pass.
- [ ] 2.4 Migrate utility methods in `AiAssertions` directly to `@AiTool` and retire `@AiMethod` and `JavaMethodAction`, verifying assertion execution tests pass.

## 3. Browser Tools Implementation

- [ ] 3.1 Implement core `BrowserToolProvider` wrapping Selenide actions (`click`, `type`, `navigate`, `select`, `hover`, `assert_text`) as `AiTool`s with strict JSON Schemas.
- [ ] 3.2 Ensure browser action tools capture and preserve `DomFeatureVector`, candidate locators, and screenshot visual hashes, and verify vector capture tests pass.
- [ ] 3.3 Implement `browser_execute_script` and `browser_get_dom` tools to provide script execution and inspection for composite plugins.

## 4. Composite Plugin Tools

- [ ] 4.1 Implement `WcagAccessibilityTool` implementing `AiTool`, utilizing `context.invokeTool("browser_execute_script")` to execute Axe-core without direct `WebDriverRunner` dependencies.
- [ ] 4.2 Verify `WcagAccessibilityTool` unit tests validate violation detection, formatting, and Allure report attachment.

## 5. Playbook Recording & Replay Engine

- [ ] 5.1 Update `PlaybookStep` and `Action` JSON serialization to support `toolCalls` while transparently reading legacy recorded `actions` arrays.
- [ ] 5.2 Update the playback execution loop to replay `toolCalls` directly via `toolRegistry.getTool(name).execute(...)` without LLM calls.
- [ ] 5.3 Verify offline replay preserves sub-millisecond similarity healing using recorded `DomFeatureVector` data when selectors change.

## 6. Full Verification

- [ ] 6.1 Execute the test suite (`mvn clean test -Dtest=org.neodymium.ai.tool.**`) and verify all new tooling tests pass.
- [ ] 6.2 Execute existing regression tests (`SearchTest`, `AiAssertionsTest`) and verify zero regressions.
