## Context

See [proposal.md](proposal.md) for background and motivation.

Currently, Neodymium has two disjoint execution concepts:
1. Procedural browser actions hardcoded inside `SelenideTargetExecutor` (e.g. `CLICK`, `TYPE`, `NAVIGATE`).
2. Reflection-based Java method execution inside `JavaMethodAction`, which is constrained to single string arguments, lacks JSON Schema contracts, and cannot return values to context.

This design unifies both into an in-process, schema-driven Tooling Architecture modeled after modern agentic tool protocols.

## Goals / Non-Goals

**Goals:**
- Provide a clean, extensible `AiTool` interface in `org.neodymium.ai.tool`.
- Automatically generate JSON Schema for Java methods annotated with `@AiTool` and `@ToolParam` using Jackson.
- Automatically deserialize incoming JSON arguments into typed Java parameters (`BigDecimal`, `int`, `String`, POJOs) and capture return values.
- Adapt legacy `@AiMethod` utility classes (e.g. `AiAssertions`) into the tool registry transparently.
- Wrap Selenide browser operations as standard `AiTool` implementations without altering underlying driver behavior.
- Preserve all recorded metadata (`DomFeatureVector`, candidate locators, visual dHash baselines) in JSON playbooks to maintain sub-millisecond offline healing and deterministic replay.
- Provide `ToolContext` for composite plugin tools (like WCAG accessibility audits) to invoke browser tools and attach Allure artifacts without leaking raw `WebDriver` handles.

**Non-Goals:**
- External out-of-process MCP server spawning over `stdio` or HTTP/SSE (postponed to a dedicated follow-up phase as agreed).
- Rewriting or replacing the underlying Selenide/WebDriver automation engine.

## Decisions

### 1. Unified `AiTool` Interface for Browser and Java Logic
- **Decision**: All capabilities implement or adapt to `AiTool`:
  ```java
  public interface AiTool
  {
      ToolDefinition getDefinition();
      ToolResult execute(ToolCall call, ToolContext context) throws Exception;
  }
  ```
- **Rationale**: Eliminates the artificial split between "browser actions" and "helper methods". The LLM receives a single unified list of tools with standard schemas, and the runner executes all steps through a single `execute(call, context)` contract.
- **Alternatives Considered**: Keeping a dual pipeline (Browser Actions in `TargetExecutor` and Tools in `ToolRegistry`). Rejected because it leaks implementation details into the runner, complicates playbook schemas, and confuses LLM prompts.

### 2. Jackson-Backed In-Process JSON Schema Generation
- **Decision**: `JavaToolFactory` reflectively inspects `@AiTool` methods and their `@ToolParam` parameters, constructing an OpenAPI/JSON Schema `ObjectNode` describing parameter types, descriptions, and required fields. Arguments are deserialized via `ObjectMapper.convertValue(...)`.
- **Rationale**: Replaces fragile string splitting with robust, industry-standard JSON deserialization. Supports complex and numeric types cleanly.
- **Alternatives Considered**: Manual regex parsing or requiring users to write raw JSON schemas by hand. Rejected due to developer friction and error-proneness.

### 3. Preservation of Element Metadata in JSON Playbooks
- **Decision**: When browser tools execute during recording or healing, they record their invocation and attach target element metadata (`domFeatureVector`, candidate locators, screenshot hash) to the recorded JSON step.
- **Rationale**: Ensures that during offline replay, Neodymium's `LocatorCascadeResolver` can heal shifted elements locally in 0.5ms with zero LLM calls.
- **Alternatives Considered**: Storing only raw tool call arguments without DOM features. Rejected because it would destroy Neodymium's core offline self-healing capability.

### 4. `ToolContext` Abstraction for Composite Plugin Tools
- **Decision**: Tools receive a `ToolContext` providing `invokeTool(name, arguments)`, `setVariable(key, val)`, and `attachArtifact(name, mime, data)`.
- **Rationale**: Enables complex plugins (e.g., `WcagAccessibilityTool`) to run scripts via `browser_execute_script`, inspect the DOM, and push Allure reports without referencing static `WebDriverRunner` singletons or breaking encapsulation.
- **Alternatives Considered**: Giving tools raw `WebDriver` access. Rejected because it breaks test isolation and leaks browser engine internals into domain tools.

## Risks / Trade-offs

- **[Risk] Reflection and schema generation overhead at startup** → *Mitigation*: Lazily inspect and cache `ToolDefinition`s in `ToolRegistry` on first access.
- **[Risk] Overloaded Java method collision** → *Mitigation*: Enforce unique tool names per class via `@AiTool(name = "...")` or append parameter signatures when names collide.
- **[Risk] Existing recorded JSON playbooks compatibility** → *Mitigation*: The playbook parser transparently maps existing `actions` records into `toolCalls` during playback.
