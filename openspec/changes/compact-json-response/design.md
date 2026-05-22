## Context

Neodymium's AI agent features (specifically exploratory test generation and playbook healing) interact with LLMs by providing HTML/DOM states and instructions and parsing structured JSON replies containing actions, reasoning, and status fields. By mapping these keys to extremely compact 1- or 2-letter equivalents in the prompts and responses, we can significantly reduce LLM token usage and execution costs.

Following direct user feedback, we do not need to maintain dual-format parsing for legacy keys in the LLM response parser. However, the recorded playbooks on disk must remain strictly in their standard long-key format (e.g. `type`, `target`, etc.) for readability, and the debug log output showing the compact response from the LLM must be pretty-printed.

## Goals / Non-Goals

**Goals:**
- Implement a compact JSON key mapping for all key fields used in LLM prompts and responses.
- Update system prompt templates to require and output the compact schema.
- Implement strict compact-key parsing in `ActionParser.java` and `AiPromptGenerator.java`.
- Ensure playbooks saved to disk remain in the standard long-key format.
- Pretty-print the raw JSON response in logs under the updated header "   📄 --- LLM Response (Pretty-Printed) ---".

**Non-Goals:**
- Supporting legacy/long keys in the LLM response parser (deprecated per user request).
- Changing the playbook format on disk (must remain standard long keys).

## Decisions

### 1. Key Mapping Schema

**Root-Level Keys (LLM response):**
- `success` $\rightarrow$ `s`
- `actions` $\rightarrow$ `a`
- `done` $\rightarrow$ `d`
- `error` $\rightarrow$ `e`
- `reasoning` $\rightarrow$ `r`
- `status` $\rightarrow$ `st`
- `targetContext` $\rightarrow$ `tc`
- `currentSubgoal` $\rightarrow$ `cs`
- `overallIntentAchieved` $\rightarrow$ `oia`
- `dropLastNActions` $\rightarrow$ `dl`

**Action-Level Keys:**
- `type` $\rightarrow$ `t`
- `target` $\rightarrow$ `tg`
- `value` $\rightarrow$ `v`
- `description` $\rightarrow$ `d`
- `elementDetails` $\rightarrow$ `ed`
- `condition` $\rightarrow$ `c`
- `then` $\rightarrow$ `th`
- `else` $\rightarrow$ `el`
- `adjust` $\rightarrow$ `ad`
- `elementContext` $\rightarrow$ `ec`
- `reasoning` $\rightarrow$ `r`

### 2. Playbook Immutability

Since the `Action` class's private fields are defined with long names (`type`, `target`, `value`, `description`), when GSON serializes the playbook steps to disk (`PlaybookManager.savePlaybook`), it automatically writes the long names. In `ActionParser.java`, we will parse the compact JSON fields from the LLM and construct `Action` instances with the standard setters. This perfectly preserves the playbook JSON file structure on disk while using compact syntax for LLM calls.

### 3. Pretty-Printed Debug Logging

In `AiAgent.java` (and/or `ActionParser.java`), the header for logging the response will be changed from `   📄 --- Raw LLM Response ---` to `   📄 --- LLM Response (Pretty-Printed) ---`. When logging this response, we will check if it's a valid JSON string. If so, we will deserialize it using GSON and serialize it back to a pretty-printed string using `new GsonBuilder().setPrettyPrinting().create()`. This ensures the debug/trace log output is highly readable while displaying the compact keys.

## Risks / Trade-offs

- **[Risk]**: GSON deserialization overhead for logging.
- **[Mitigation]**: Only executed when debug logging is enabled.
