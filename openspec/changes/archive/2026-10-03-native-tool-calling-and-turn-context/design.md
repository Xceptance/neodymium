# Design: Native Tool Calling, Turn-Aware Context & Internal Loop Milestones

## Context

See `proposal.md` for motivation.

Neodymium currently executes agent tool loops (`AgentToolLoopStep`) via string-concatenated prompt engineering:
- Tool definitions are formatted as markdown text into the system prompt.
- SUT DOM state is appended as markdown into the user prompt.
- Previous tool results are concatenated into a `### Previous Tool Observations:` markdown section.
- The assistant's JSON output is extracted from markdown code fences (` ```json `).
- Context level resolution relies on pre-step PESAP classification, which causes `PageAnalyzer` to re-extract full interactive DOM trees (27KB on content-heavy pages) even after simple `browser_navigate` operations.

Now that unified tooling is established, we can implement a clean-slate architecture that eliminates text scaffolding in favor of native tool-calling protocols.

## Goals / Non-Goals

**Goals:**
- Implement a native multi-turn message protocol (`SYSTEM`, `USER`, `ASSISTANT`, `TOOL`) in `LlmRequest` and LLM client providers.
- Implement Turn-Aware Dynamic Context in `AgentToolLoopStep` (zero DOM for navigation/metadata, pierced DOM Light on Turn 1 for interactive steps, structured `ToolResult` on intermediate turns).
- Evolve step splitting from external framework sub-steps into internal loop milestone tracking.
- Standardize all browser tools in `BrowserToolProvider` to emit structured JSON execution payloads.
- Eliminate markdown backtick stripping and prompt-concatenated observation sections.

**Non-Goals:**
- Modifying the underlying Selenide browser execution or Playbook similarity healing algorithms.
- Maintaining backward compatibility with legacy single-shot prompt parsing or deprecated `"jm"` reflection flags.

## Decisions

### Decision 1: First-Class `ChatMessage` and Native `tools` in `LlmRequest`
Introduce a structured message abstraction:
```java
public record ChatMessage(Role role, String content, List<ToolCall> toolCalls, String toolCallId) {}
public enum Role { SYSTEM, USER, ASSISTANT, TOOL }
```
`LlmRequest` directly carries `List<ChatMessage> messages` and `List<ToolDefinition> tools`.
- **Rationale**: Direct mapping to Gemini's `functionDeclarations` / `functionCall` and OpenAI's `tools` / `tool_calls` schemas.
- **Alternatives Considered**: Continuing text-based prompt simulation (rejected due to token waste, string-escaping bugs, and prompt drift).

### Decision 2: Turn-Aware Dynamic Context Resolution
In `AgentToolLoopStep`:
1. **Turn 1**:
   - If intent is `NAVIGATE`, `ASSERT_METADATA`, or `WAIT`: capture only Page URL and Title (zero DOM nodes).
   - If intent is interactive (`CLICK`, `TYPE`, `SELECT`, `HOVER_SCROLL`, `ASSERT`, `STORE`): capture our battle-tested DOM Light (LEAN) with shadow roots pierced and `data-ai` stamped.
2. **Intermediate Turns (Turn 2+)**:
   - When a tool executes (e.g. `browser_navigate` or `browser_type`), its structured `ToolResult` JSON is appended as a `role: "TOOL"` message.
   - SUT state refresh updates only lightweight page status (URL, Title) without re-dumping the full DOM tree.
3. **Escalation**:
   - If an element is hidden, obscure, or visual layout is needed, the model calls `browser_query_dom`, `browser_inspect`, or `browser_take_screenshot`.
- **Rationale**: Drops Call 3 on navigation from 27,000 characters to ~150 characters while preserving full robustness for complex shadow DOM and custom element interactions.
- **Alternatives Considered**: Complete on-demand query without initial DOM Light (rejected because it forces an extra roundtrip on every interactive step).

### Decision 3: Internal Loop Milestones Replacing External Step Splitting
Compound instructions (e.g. *"Gib 'Mario' als Vorname, 'Meier' als Nachname..."* or *"Type search and press enter"*):
- Are recognized as a sequence of milestone sub-actions at the start of the tool loop.
- The single tool loop executes turns until all milestones are satisfied.
- **Rationale**: Maintains conversational memory across sub-actions, prevents artificial sub-step proliferation in test suites and reports, and establishes crisp boundaries against over-reaching.
- **Alternatives Considered**: External pipeline step splitting (rejected because it wipes LLM session memory and breaks test structure).

### Decision 4: Structured JSON Payloads for Local Browser Tools
All `BrowserToolProvider` tools return structured JSON strings in `ToolResult`:
- `browser_navigate`: `{"status": "SUCCESS", "url": "...", "title": "..."}`
- `browser_click`: `{"status": "SUCCESS", "target": "...", "url": "...", "title": "..."}`
- `browser_type`: `{"status": "SUCCESS", "target": "...", "value": "..."}`
- `browser_press_key`: `{"status": "SUCCESS", "key": "..."}`
- `browser_query_dom`: `{"matches": [...]}`
- **Rationale**: Provides structured state directly to `role: "TOOL"` messages without human-oriented prose.

## Risks / Trade-offs

- **[Risk] Compound instruction terminates prematurely**  
  → *Mitigation*: The loop checks milestone checklist progress before accepting `complete_step`. If milestones remain, the loop issues an observation detailing the remaining required actions.
- **[Risk] Interactive element not present in initial DOM Light**  
  → *Mitigation*: The agent has active discovery tools (`browser_query_dom`, `browser_inspect`, `browser_take_screenshot`) to fetch targeted containers or visual layouts without restarting the step.
