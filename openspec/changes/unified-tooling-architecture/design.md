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
- Migrate existing assertions in `AiAssertions` directly to `@AiTool` and retire `@AiMethod` and `JavaMethodAction`.
- Wrap Selenide browser operations as standard `AiTool` implementations without altering underlying driver behavior.
- Provide active discovery tools (`browser_query_dom`, `browser_inspect`, `browser_scroll`, `browser_take_screenshot`) so the agent queries page state on demand, completely eliminating the legacy brute-force context escalation ladder.
- Replace the monolithic single-shot `CallLlmStep` + `ExecuteActionsStep` pipeline with an iterative `AgentToolLoopStep` (`Think` → `ToolCall` → `Observe` → `Finish`).
- Enforce strict loop stop criteria: Goal Accomplished (`complete_step`), Assertion Defect (`AssertionError`), Turn Budget, Thrashing / Stagnation, Wall-Clock Timeout, and Fatal Environment Failures.
- Adapt the Quality Judge into a pre-invocation guard (`QualityJudgeToolInterceptor`) that intercepts proposed browser tool locators to verify confidence and resolve ambiguity.
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

### 5. Active Discovery Replacing Passive Context Escalation
- **Decision**: Provide active discovery tools (`browser_query_dom`, `browser_inspect`, `browser_scroll`, `browser_take_screenshot`) that allow the agent to explore DOM elements, viewport coordinates, and visual layouts on demand. Retire `ToLevelEscalationException`, `HealingRequiredException`, and the multi-tier context escalation ladder.
- **Rationale**: Context escalation was an artificial workaround for a blind, single-shot LLM. With tools, the agent queries for what it needs interactively, resulting in cleaner code, reduced latency, and fewer redundant LLM calls.
- **Alternatives Considered**: Retaining the context ladder alongside tools. Rejected as redundant and conflicting with agent autonomy.

### 6. `AgentToolLoopStep` and Six Stop Criteria
- **Decision**: Replace `CallLlmStep<List<Action>>` and `ExecuteActionsStep` with `AgentToolLoopStep`. The loop terminates upon encountering any of six explicit stop criteria:
  1. **Goal Accomplished (`SUCCESS`)**: Agent calls `complete_step(summary)` or returns with no further tool calls needed.
  2. **Assertion Failure (`FAILED`)**: An assertion tool (`assert_text_equals`, `assert_element_exists`, `@AiTool` custom assertion) fails with `AssertionError`.
  3. **Turn Budget Ceiling (`FAILED`)**: Step reaches maximum allowed tool turns (default 10, configurable via `neodymium.ai.agent.maxTurnsPerStep`).
  4. **Thrashing / Stagnation (`FAILED`)**: Agent issues identical tool calls with identical arguments 3 times consecutively with no state change.
  5. **Wall-Clock Timeout (`FAILED`)**: Step duration exceeds maximum configured step timeout.
  6. **Fatal Environment Failure (`FAILED`)**: WebDriver disconnect, browser crash, or user interactive abort (`SKIP`/`ABORT`).
- **Rationale**: Prevents infinite loops and runaway token usage while guaranteeing immediate, decisive failure on actual application bugs.

### 7. `QualityJudgeToolInterceptor` for Locator Guarding
- **Decision**: Adapt `QualityJudgeStep` into `QualityJudgeToolInterceptor`. When an agent issues a `browser_*` tool call with proposed locators:
  - If top candidate locator confidence is $\ge 0.95$, it passes through immediately with zero overhead.
  - If ambiguous ($< 0.85$ or close runner-up), the Judge deliberates or requests clarification before browser dispatch.
- **Rationale**: Retains Neodymium's locator stability protection while integrating seamlessly into the agent's pre-execution dispatch cycle.

## Risks / Trade-offs

- **[Risk] Reflection and schema generation overhead at startup** → *Mitigation*: Lazily inspect and cache `ToolDefinition`s in `ToolRegistry` on first access.
- **[Risk] Overloaded Java method collision** → *Mitigation*: Enforce unique tool names per class via `@AiTool(name = "...")` or append parameter signatures when names collide.
- **[Risk] Existing recorded JSON playbooks compatibility** → *Mitigation*: The playbook parser transparently maps existing `actions` records into `toolCalls` during playback.
- **[Risk] Agent turn budget exhaustion on complex tasks** → *Mitigation*: PESAP continues to split compound tasks into atomic steps upfront so each step executes in 1–5 tool calls.
