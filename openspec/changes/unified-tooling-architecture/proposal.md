## Why

Currently, Neodymium executes custom Java logic through an ad-hoc `JAVA_METHOD` action plugin that relies on brittle string-splitting heuristics, enforces 0-to-1 string parameter limits, discards return values, and lacks a JSON Schema contract. Furthermore, browser interactions are handled as hardcoded procedural actions in `SelenideTargetExecutor`, creating an artificial architectural split between "browser actions" and "helper methods". 

By establishing a unified in-process Tooling Architecture inspired by modern tool-use standards (such as MCP and LLM function calling), every capability—from browser interactions (click, type, navigate) to Java helper methods, math assertions, and complex domain plugins (such as WCAG accessibility audits)—is modeled under a single, typed, schema-driven `AiTool` contract.

## What Changes

- **Unified `AiTool` Abstraction**: Introduce a standard `AiTool` interface with `ToolDefinition` (JSON Schema contract), `ToolCall`, and `ToolResult` (multimodal content, variables export, and Allure attachments).
- **Java Tool Provider (`@AiTool`, `@ToolParam`)**: Replace `JavaMethodAction` and `@AiMethod` with a reflective, Jackson-backed schema generator supporting multi-typed parameters, typed deserialization, and return value variable binding (`store: varName`). Migrate `AiAssertions` directly to `@AiTool`.
- **Browser Tools (`BrowserToolProvider`)**: Wrap Selenide browser operations (`browser_click`, `browser_type`, `browser_navigate`, `browser_select`, `browser_assert_text`, `browser_execute_script`, etc.) as standard `AiTool` implementations, exposing structured JSON schemas to LLM agents.
- **Element Healing & Replay Fidelity**: Preserve all `DomFeatureVector`, candidate locators, and screenshot visual hash metadata within recorded JSON playbooks so that offline, sub-millisecond similarity healing and fast playback remain 100% deterministic and free of LLM calls during regression runs.
- **ToolContext & Composite Plugins**: Introduce a `ToolContext` enabling tools to invoke other tools (e.g., WCAG auditing calling `browser_execute_script`), mutate session variables, attach report artifacts, and optionally run scoped diagnostic subloops without leaking raw `WebDriver` handles.
- **Supersede `generic-java-method-support`**: Formally supersede the narrow `generic-java-method-support` change with this comprehensive architecture.

## Capabilities

### New Capabilities
- `unified-tooling-architecture`: Establishes the unified in-process `AiTool` contract, `ToolRegistry`, Java reflection tooling (`@AiTool`), browser tool wrapping, `ToolContext`, and deterministic JSON playbook recording and playback.

### Modified Capabilities
<!-- None -->

## Impact

- **New Packages & Interfaces**: `org.neodymium.ai.tool` (`AiTool`, `ToolDefinition`, `ToolCall`, `ToolResult`, `ToolContext`, `ToolRegistry`, `JavaToolFactory`, `@AiTool`, `@ToolParam`).
- **Browser Execution**: `SelenideTargetExecutor` transitions to register and dispatch via `BrowserToolProvider`.
- **Playbook Model**: `PlaybookStep` and recorded JSON playbooks serialize structured `toolCalls` alongside existing `DomFeatureVector` and locator candidate metadata.
- **Legacy Removal**: Retires `JAVA_METHOD` action and `@AiMethod` annotation in favor of direct `@AiTool` registration.
- **Dependencies**: No new external dependencies required; leverages existing Jackson and Selenide libraries.
