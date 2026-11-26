# Proposal: Native Tool Calling, Turn-Aware Context & Internal Loop Milestones

## Why

Neodymium's agent tool loop (`AgentToolLoopStep`) currently simulates tool calling through plain-text prompt concatenation (ReAct pattern), serializing schemas as markdown text, manually building markdown sections for observations, and regex-parsing JSON out of markdown backticks.

Furthermore, because context level was designed for legacy single-shot prompts, the loop naively re-captures SUT state after simple actions (such as `browser_navigate`) using static pre-step context levels (`MINIMAL`), dumping over 26,000 characters of irrelevant links and headers into subsequent turns (e.g., Call 3 in `BlogTest`). Compound multi-action instructions also still trigger legacy PESAP external sub-step splitting, creating artificial pipeline steps that fracture reporting and wipe conversation memory.

Now that unified tooling is established, we can discard these legacy workarounds and adopt first-class Native Tool Calling (`role: "tool"`, `role: "assistant"` with `tool_calls`), Turn-Aware Context (zero DOM for navigation/metadata, battle-tested DOM Light on Turn 1 for interactions), and Internal Loop Milestones.

## What Changes

- **Native Tool-Calling Protocol**: Transition `LlmRequest`, `LlmResponse`, and `AgentToolLoopStep` to structured multi-turn message objects (`role: "system"`, `role: "user"`, `role: "assistant"` with native `tool_calls`, and `role: "tool"` with structured JSON `ToolResult`), passing native function schemas to provider APIs.
- **Turn-Aware Dynamic Context**:
  - `NAVIGATE`, `ASSERT_METADATA`, focused `KEY_PRESS`, and simple `WAIT` steps receive **zero DOM elements** (URL and Title only), eliminating prompt bloat on navigation.
  - Interactive steps (`CLICK`, `TYPE`, `SELECT`, `HOVER_SCROLL`, `CLEAR`, `CHECK`, `ASSERT`, `STORE`, `BRANCH`) receive battle-tested **DOM Light (LEAN)** on Turn 1 with full Shadow DOM piercing and `data-ai` stamping.
  - Post-action in-loop turns feed structured `ToolResult` JSON directly into `role: "tool"` without re-dumping the full DOM tree.
- **In-Flight Context Escalation**: The model dynamically requests deeper subtrees (`browser_query_dom`, `browser_inspect`) or visual snapshots (`browser_take_screenshot`) on demand, eliminating rigid framework restart ladders.
- **Internal Loop Milestones**: Compound multi-action instructions execute as sequential checklist milestones inside the single tool loop conversation, preserving memory and eliminating artificial external pipeline sub-steps.
- **Structured Tool Results**: Standardize all local browser tools in `BrowserToolProvider` to return structured JSON payloads.
- **Provider Native Function Calling**: Wire native function declarations and multi-turn message formatting to `GeminiLlmProvider` and `OpenAiLlmProvider`.

## Capabilities

### Modified Capabilities
- `unified-tooling-architecture`: Evolve the tool execution loop from prompt-simulated ReAct to native multi-turn tool calling, turn-aware context resolution, and internal loop milestones.

## Impact

- **Affected Components**:
  - `org.neodymium.ai.client`: `LlmRequest`, `LlmResponse`, `ChatMessage`, `Role`, `GeminiLlmProvider`, `OpenAiLlmProvider`.
  - `org.neodymium.ai.pipeline.steps`: `AgentToolLoopStep`, `ExecuteActionsStep`.
  - `org.neodymium.ai.tool.browser`: `BrowserToolProvider`.
- **API / Behavioral Impact**:
  - Eliminates markdown string parsing in the agent loop.
  - Eliminates 27KB DOM dumps on navigation steps.
  - Compound instructions execute within a single step without creating artificial sub-steps.
  - Clean-slate architectural improvement without legacy backward-compatibility compromises.
