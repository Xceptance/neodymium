## MODIFIED Requirements

### Requirement: Agent tool execution loop and stop criteria
The system SHALL execute an iterative agentic loop (`Think` → `ToolCall` → `Observe` → `Finish`) using a native multi-turn message protocol where tools are provided as structured schemas, assistant outputs contain structured tool calls, and tool execution results are appended as structured tool messages, evaluating six explicit stop criteria:
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

#### Scenario: Native tool call response and execution
- **WHEN** the agent returns a structured tool call for an available tool
- **THEN** the system executes the tool in-process and appends its structured result as a native tool role message to the conversation history without string parsing or markdown backtick extraction.

## ADDED Requirements

### Requirement: Turn-aware dynamic context resolution
The system SHALL resolve the context provided to the LLM dynamically based on the current turn and operational objective, providing zero DOM elements for navigation and metadata operations, supplying pierced DOM Light on the initial turn of interactive operations, and feeding structured tool results directly into tool messages on subsequent turns without re-extracting the full DOM tree.

#### Scenario: Zero DOM provided for navigation operations
- **WHEN** an instruction with navigation or metadata intent is executed, or immediately after a `browser_navigate` tool call
- **THEN** the system supplies only page URL and title metadata to the model without extracting or serializing DOM element trees.

#### Scenario: DOM Light provided for interactive step initiation
- **WHEN** an interactive instruction (such as click, type, select, or hover) initiates Turn 1
- **THEN** the system supplies a compact DOM Light representation including pierced Shadow DOMs, custom elements, and unique automation IDs.

#### Scenario: Intermediate turn receives tool result without full DOM re-dump
- **WHEN** a mutating browser action completes during a multi-turn step
- **THEN** the system returns the structured tool execution result in the tool message and updates page URL and title without re-dumping the full DOM tree.

### Requirement: Internal loop milestones for compound instructions
The system SHALL execute compound multi-action instructions within a single continuous tool loop conversation using an internal sequence of milestone sub-actions, preserving conversational context across turns and eliminating artificial external pipeline step splitting.

#### Scenario: Compound instruction executes sequentially in single step
- **WHEN** an instruction containing multiple distinct field entries or sequential actions is executed
- **THEN** the agent executes each action sequentially across conversational turns in a single step
- **AND** calls `complete_step` only after all milestone actions are accomplished
- **AND** records all tool calls under the original playbook step.
