## Why

Currently, Neodymium executes custom Java logic through an ad-hoc `JAVA_METHOD` action plugin that relies on brittle string-splitting heuristics, enforces 0-to-1 string parameter limits, discards return values, and lacks a JSON Schema contract. Furthermore, browser interactions are handled as hardcoded procedural actions in `SelenideTargetExecutor`, and the entire step execution pipeline couples `CallLlmStep` and `ExecuteActionsStep` to a rigid, single-shot batch `Action` model. In failure scenarios, the framework relies on a brute-force "context escalation ladder" (MINIMAL → INTERACTIVE → VISUAL → VISUAL_RICH) because the LLM lacks tools to inspect or navigate the page actively.

By establishing a unified in-process Tooling Architecture inspired by modern tool-use standards (such as MCP and LLM function calling), every capability—from browser interactions (click, type, navigate) to active discovery (query DOM, scroll, inspect, screenshot), Java helper methods, math assertions, and complex domain plugins (such as WCAG accessibility audits)—is modeled under a single, typed, schema-driven `AiTool` contract. The entire pipeline chain is modernized into an iterative agentic tool-use loop with rigorous stop criteria and pre-invocation quality judge guarding.

## What Changes

- **Unified `AiTool` Abstraction**: Introduce a standard `AiTool` interface with `ToolDefinition` (JSON Schema contract), `ToolCall`, and `ToolResult` (multimodal content, variables export, and Allure attachments).
- **Java Tool Provider (`@AiTool`, `@ToolParam`)**: Replace `JavaMethodAction` and `@AiMethod` with a reflective, Jackson-backed schema generator supporting multi-typed parameters, typed deserialization, and return value variable binding (`store: varName`). Migrate `AiAssertions` directly to `@AiTool`.
- **Browser Tools (`BrowserToolProvider`)**: Wrap Selenide browser operations (`browser_click`, `browser_type`, `browser_navigate`, `browser_select`, `browser_assert_text`, `browser_execute_script`, etc.) as standard `AiTool` implementations, exposing structured JSON schemas to LLM agents.
- **Active Discovery Over Passive Escalation**: Replace the legacy brute-force context escalation ladder (`ContextLevel` exceptions and nested retry try-catch blocks) with active, agent-driven discovery tools (`browser_query_dom`, `browser_inspect`, `browser_scroll`, `browser_take_screenshot`). The agent queries for what it needs on demand.
- **Agentic Tool Loop (`AgentToolLoopStep`)**: Replace the static single-shot batch action flow with an interactive tool execution loop (`Think` → `ToolCall` → `Observe ToolResult` → `Finish`), guided by explicit stop criteria (Goal Achieved, Assertion Defect, Turn Budget, Thrashing Breaker, Timeout, Fatal Error).
- **Quality Judge Tool Guard (`QualityJudgeToolInterceptor`)**: Transition `QualityJudgeStep` into a pre-invocation guard that intercepts proposed `browser_*` tool calls, validating locator stability and candidate scores before browser dispatch.
- **Element Healing & Replay Fidelity**: Preserve all `DomFeatureVector`, candidate locators, and screenshot visual hash metadata within recorded JSON playbooks so that offline, sub-millisecond similarity healing and fast playback remain 100% deterministic and free of LLM calls during regression runs.
- **ToolContext & Composite Plugins**: Introduce a `ToolContext` enabling tools to invoke other tools (e.g., WCAG auditing calling `browser_execute_script`), mutate session variables, attach report artifacts, and optionally run scoped diagnostic subloops without leaking raw `WebDriver` handles.
- **Supersede `generic-java-method-support`**: Formally supersede the narrow `generic-java-method-support` change with this comprehensive architecture.

## Capabilities

### New Capabilities
- `unified-tooling-architecture`: Establishes the unified in-process `AiTool` contract, `ToolRegistry`, Java reflection tooling (`@AiTool`), browser tool wrapping, active discovery tools, the `AgentToolLoopStep` with circuit breakers, the `QualityJudgeToolInterceptor`, and deterministic JSON playbook recording and playback.

### Modified Capabilities
<!-- None -->

## Impact

- **New Packages & Interfaces**: `org.neodymium.ai.tool` (`AiTool`, `ToolDefinition`, `ToolCall`, `ToolResult`, `ToolContext`, `ToolRegistry`, `JavaToolFactory`, `@AiTool`, `@ToolParam`, `BrowserToolProvider`, `ActiveDiscoveryTools`).
- **Pipeline Overhaul**: `ExecuteActionsStep` transitions to `AgentToolLoopStep`, driving iterative tool execution; `QualityJudgeStep` adapts to `QualityJudgeToolInterceptor`.
- **Context Escalation Retirement**: Eliminates `ToLevelEscalationException`, `HealingRequiredException` retry ladders, and brute-force multi-tier context escalation.
- **Playbook Model**: `PlaybookStep` and recorded JSON playbooks serialize structured `toolCalls` alongside existing `DomFeatureVector` and locator candidate metadata.
- **Legacy Removal**: Retires `JAVA_METHOD` action and `@AiMethod` annotation in favor of direct `@AiTool` registration.
- **Dependencies**: No new external dependencies required; leverages existing Jackson and Selenide libraries.
