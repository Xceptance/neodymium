# Decomposed and Optimized AI System Prompts

This document outlines the architectural changes, implementation details, and token savings achieved by decomposing and optimizing the AI system prompts for the Neodymium test automation agent.

## Background

Previously, Neodymium sent monolithic system prompts (`system-prompt.md` and `system-healing-prompt.md`) to the LLM. In `HINT` context (when the user provides an inline locator hint like `(hint: #myId)`), the agent does not receive the page DOM or accessibility tree, making detailed capability instructions, DOM analysis rules, and visual validation parameters irrelevant.

These monolithic prompts consumed excessive tokens (approx. **2,877 input tokens** per HINT step), leading to slower response times and higher API costs.

---

## Architectural Improvements

### 1. Prompt Decomposition
The monolithic system instructions were split into 8 highly focused Markdown snippets located in `src/main/resources/ai-prompts/`:
* `snippet-role.md` - Defins the core agent role/persona.
* `snippet-capabilities.md` - Template container `{actionDescriptions}` for dynamic action plugins.
* `snippet-response-format.md` - Complete JSON schema definition for LLM responses.
* `snippet-rules-success-failure.md` - General rules for verifying step outcome success.
* `snippet-rules-general.md` - Common mapping rules (e.g. click -> CLICK, type -> TYPE).
* `snippet-rules-dom-analysis.md` - DOM and layout structure analysis rules.
* `snippet-rules-element-selection.md` - Guidance for selecting CSS, XPath, or ID selectors.
* `snippet-rules-visual.md` - Visual validation/screenshot audit specifications.
* `system-healing-instruction.md` - Self-healing logic specifically for step replay failures.

### 2. Context-Level Tailoring
We modified `AiAgentPrompts.java` to dynamically assemble the system prompt based on the requested `ContextLevel`:
* **`HINT`:** Dynamically compiles an extremely minimal prompt containing:
  * Minimal role persona (`snippet-role.md`).
  * A target-oriented inline capabilities list (`CLICK`, `TYPE`, `CLEAR`, `SELECT`, `HOVER`, `ASSERT`, `CHECK`, `SCROLL`, `KEY_PRESS`), bypassing verbose descriptions.
  * A stripped-down JSON schema containing only the required keys (removing frameId, branching/logic trees).
  * A focused 4-rule execution instruction (omitting BACK, FORWARD, REFRESH, etc., which have no locator targets).
  * A brief guidance instructions block for HINT context.
* **`AXTREE` / `LEAN` / `STANDARD`:** Compiles standard DOM analysis and element selection snippets.
* **`VISUAL` / `VISUAL_LEAN`:** Appends full DOM, element selection, and visual comparison snippets.

### 3. Centralized Prompt Tracing
To improve debugging visibility without polluting execution logs:
* Centrally log the final combined `systemPrompt` and `userPrompt` inside `LlmClient.java` under the `TRACE` log level.
* Removed duplicate user prompt logs from `AiAgent.java`.

---

## Token Savings Analysis

Through three iterative phases of prompt minimization, we achieved the following input token counts for HINT mode calls:

| Phase | Description | Token Count | Savings % |
|---|---|---|---|
| **Baseline** | Original monolithic system prompt | **2,877** | *Reference* |
| **Phase 1** | Dynamic context-level snippet assembly | **1,993** | 37.3% |
| **Phase 2** | Tailored plugin metadata (names list only) | **907** | 68.4% |
| **Phase 3** | Full minimal capabilities, format schema & rules | **421** | **85.3%** |

*Happy-path `HINT` context calls now consume only **421 input tokens** instead of **2,877**, resulting in a **85.3% total reduction** in prompt tokens.*
