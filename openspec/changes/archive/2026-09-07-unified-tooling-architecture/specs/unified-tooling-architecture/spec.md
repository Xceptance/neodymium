## Purpose

Provides a unified in-process tooling architecture for Neodymium that models browser interactions, Java helper methods, assertions, and domain plugins as typed, schema-validated tools with deterministic playback recording.

## ADDED Requirements

### Requirement: Standard in-process tool contract
The system SHALL define a standard tool contract where every executable capability exposes a unique name, a description, and an OpenAPI/JSON Schema defining its input parameters, and executes to produce a structured result containing text, data, and execution status.

#### Scenario: Discover and inspect tool definition
- **WHEN** a tool is registered in the tool registry
- **THEN** the system provides its tool definition containing name, description, and valid JSON Schema describing expected parameters and required fields.

### Requirement: Java tool schema generation and typed invocation
The system SHALL reflectively inspect public Java methods annotated with tool annotations, automatically generate their parameter JSON Schema using standard types, deserialize incoming structured arguments into the target Java types, and execute the method.

#### Scenario: Execute multi-argument Java tool with typed conversion
- **WHEN** a tool call for an annotated Java method is received with structured arguments
- **THEN** the system converts the arguments into the declared method parameter types and executes the method without requiring manual string parsing.

### Requirement: Return value capture and context variable assignment
The system SHALL capture the return value of an executed tool and, when a target variable name is configured, store the result in the active test execution session context.

#### Scenario: Assign tool return value to session variable
- **WHEN** a tool call specifies a target variable name and executes successfully
- **THEN** the return value is saved into the session data context under the specified variable key for subsequent step resolution.


### Requirement: Browser tools encapsulation
The system SHALL expose standard browser automation operations (including click, type, navigate, select, hover, assert text, and execute script) as standard tools, isolating browser driver interactions behind the tool contract.

#### Scenario: Execute browser action via tool contract
- **WHEN** a browser click tool call is dispatched with a target element selector
- **THEN** the system executes the click interaction on the matching element in the active browser session and returns a success status.

### Requirement: Element metadata and offline similarity healing in playbooks
The system SHALL record tool calls in the recorded JSON playbook and preserve target element metadata (including DOM feature vectors, candidate locators, and visual hashes) to enable deterministic offline execution and sub-millisecond similarity healing without invoking LLMs during replay.

#### Scenario: Replay recorded browser tool call offline
- **WHEN** a recorded playbook containing browser tool calls is executed in replay mode
- **THEN** the system executes the recorded tool calls directly without LLM invocation, applying DOM feature vector similarity matching if the target selector has shifted.

#### Scenario: Healed locator executes through native condition polling
- **WHEN** a recorded tool call fails to find an element with the primary selector
- **AND** similarity healing resolves a replacement element candidate from DOM feature vectors
- **THEN** the system dispatches the interaction with the healed locator through native driver condition polling
- **AND** marks the step complete only after the action execution succeeds.

### Requirement: Composite plugin execution via tool context
The system SHALL provide an execution context to tools allowing them to invoke other registered tools, access or set session variables, and attach structured artifacts to the test report without accessing global driver singletons.

#### Scenario: Composite WCAG tool executes script and attaches report
- **WHEN** a composite accessibility tool is executed
- **THEN** it invokes the browser script execution tool via its tool context, evaluates compliance, and attaches the resulting report artifact to the test outcome.

### Requirement: Active discovery tools
The system SHALL provide interactive discovery tools (including `browser_query_dom`, `browser_inspect`, `browser_scroll`, and `browser_take_screenshot`) allowing the agent to actively inspect DOM elements, visibility, and layout on demand, eliminating passive context escalation ladders.

#### Scenario: Agent queries DOM for element details
- **WHEN** an agent issues a `browser_query_dom` call with search text and tag name
- **THEN** the system returns matching element candidates, attributes, and viewport visibility states without restarting the pipeline step.

### Requirement: Agent tool execution loop and stop criteria
The system SHALL execute an iterative agentic loop (`Think` → `ToolCall` → `Observe` → `Finish`) for atomic playbook steps, evaluating six explicit stop criteria:
1. **Goal Completion**: Agent calls `complete_step` or requests no further tools (`SUCCESS`).
2. **Assertion Failure**: An assertion tool fails with `AssertionError` (`FAILED`).
3. **Thrashing / Stagnation**: Identical consecutive tool calls detected with no progress (`FAILED`).
4. **Token Budget Limit**: Cumulative input/output tokens breach configured limits via `TokenBudgetGuard` (`FAILED`).
5. **Liberal Wall-Clock Timeout**: Execution duration exceeds the configured liberal step timeout (default 180s) (`FAILED`).
6. **Fatal Environment Failure**: Unrecoverable target crash or user abort (`FAILED`).

#### Scenario: Agent completes step successfully
- **WHEN** the agent performs browser interactions and calls `complete_step` within budget
- **THEN** the loop terminates immediately and marks the step as `SUCCESS`.

#### Scenario: Token budget guard halts runaway execution
- **WHEN** cumulative token consumption for the step breaches the configured token ceiling
- **THEN** the loop terminates immediately with a token budget exceeded failure without burning further tokens.

#### Scenario: Agent fails immediately on assertion defect
- **WHEN** an assertion tool is executed and detects a factual mismatch
- **THEN** the loop terminates immediately with `FAILED` without retrying or context escalation.

#### Scenario: Optional step exhausts up to 3 candidate options before skipping
- **WHEN** a step marked with the optional flag does not resolve its target on the initial attempt
- **THEN** the system explores up to 3 viable candidate options or locators to resolve the step
- **AND** only logs a warning and marks the step as skipped after all 3 candidate options are exhausted without failing the overall test run.

### Requirement: Quality judge locator guard
The system SHALL intercept proposed `browser_*` tool calls before browser dispatch to evaluate candidate locator stability, passing high-confidence locators through without latency and triggering deliberation when ambiguity is detected.

#### Scenario: High-confidence locator bypasses judge deliberation
- **WHEN** a proposed browser click specifies a candidate locator with confidence $\ge 0.95$
- **THEN** the interceptor approves the tool call immediately without triggering multi-turn deliberation.

### Requirement: Backward compatibility with legacy recorded playbooks
The system SHALL transparently deserialize recorded JSON playbooks containing legacy `actions` arrays into equivalent `toolCalls` during playback, preserving existing recorded test suites without manual migration.

#### Scenario: Load legacy JSON playbook containing actions
- **WHEN** a recorded playbook JSON file containing an `actions` array is loaded
- **THEN** the system maps each action to an equivalent `browser_*` tool call with preserved DOM feature vectors and candidate locators.

### Requirement: Tool error feedback and agent self-correction
The system SHALL capture non-fatal tool execution failures (such as element obscured, element not found in viewport, or invalid selection value) and return them as diagnostic error `ToolResult`s to the agent context, allowing the agent to self-correct in subsequent turns within the turn budget.

#### Scenario: Agent self-corrects after obscured element error
- **WHEN** a browser click fails because the element is outside the viewport or obscured
- **THEN** the tool returns an error status with diagnostic guidance in `ToolResult`
- **AND** the agent issues a `browser_scroll` tool call to bring the element into view on the next turn.

### Requirement: Tool execution telemetry and event dispatch
The system SHALL dispatch execution events to the active `ExecutionEventBus` upon tool start, success, and failure, allowing real-time consoles, preliminary reporting, and Allure listeners to record tool activity.

#### Scenario: Tool execution dispatches event to session bus
- **WHEN** any `AiTool` completes execution
- **THEN** a tool executed event is dispatched to the session event bus containing tool name, parameters, execution duration, and success status.

### Requirement: Journey fidelity and intent-based tool scoping
The system SHALL dynamically constrain active tool definitions based on the step's semantic intent and enforce journey fidelity policies, preventing agents from bypassing realistic UI user flows via direct URL navigation or location scripts during interactive steps.

#### Scenario: Interaction step excludes browser navigation tool
- **WHEN** a playbook step has an interactive semantic intent such as click or type
- **THEN** the system excludes `browser_navigate` from the tool definitions exposed to the agent.

#### Scenario: Quality judge rejects direct navigation on interaction step
- **WHEN** an agent emits a tool call or script that mutates the browser URL during an interactive step
- **THEN** the quality judge interceptor rejects the tool call with a journey fidelity policy violation
- **AND** instructs the agent to reach the destination via on-screen UI elements.

### Requirement: Visual discovery and Set-of-Marks tools
The system SHALL expose targeted visual inspection (`browser_inspect_visual`) and Set-of-Marks badge rendering (`browser_take_screenshot(mark_interactive=true)`), allowing the agent to visually inspect cropped bounding boxes or resolve ambiguous and non-DOM interactive candidates using visual numeric overlays.

#### Scenario: Agent inspects targeted element visual crop
- **WHEN** an agent issues a `browser_inspect_visual` call for a specific selector or container
- **THEN** the system returns a cropped base64 thumbnail of the target bounding box with dimension metadata.

#### Scenario: Agent captures screenshot with Set-of-Marks visual badges
- **WHEN** an agent calls `browser_take_screenshot` with interactive marking enabled
- **THEN** the system injects temporary visual numeric badges over interactive element candidate centers and returns the annotated screenshot.

### Requirement: Visual coordinate re-anchoring to DOM
The system SHALL intercept visual badge or coordinate-based interactions, resolve the underlying DOM element via `document.elementFromPoint`, extract its hierarchical selector and `DomFeatureVector`, and record them into the playbook step to ensure future test runs replay deterministically at native machine speed.

#### Scenario: Visual coordinate click re-anchors to underlying DOM element
- **WHEN** an interaction is performed using viewport coordinates or a Set-of-Marks badge
- **THEN** the system resolves the underlying element at those coordinates via `document.elementFromPoint`
- **AND** generates a resilient DOM selector and `DomFeatureVector` stored in the recorded playbook step.

### Requirement: Tier 2 perceptual visual similarity matching
The system SHALL evaluate perceptual visual dHash and tile SSIM similarity in `LocatorCascadeResolver` during offline replay when candidate elements lack distinct text or classes, resolving matching icon-only elements in $< 1\text{ms}$ on CPU without invoking LLMs.

#### Scenario: Heal icon button via visual dHash when text is absent
- **WHEN** a recorded button with an icon and no inner text cannot be located by its primary selector
- **AND** candidate elements in the container match the recorded bounding box aspect ratio and perceptual visual dHash with similarity $\ge 0.85$
- **THEN** the system heals the locator and passes the candidate element directly to native driver condition polling without LLM invocation.




