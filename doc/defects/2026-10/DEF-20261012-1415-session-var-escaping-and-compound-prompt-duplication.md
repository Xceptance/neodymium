# [DEF-20261012-1415] Unescaped Session Variables and Compound Instruction Prompt Duplication

- **Status:** `Resolved`
- **Opened:** 2026-10-12 14:15
- **Closed:** 2026-10-12 14:25
- **Component:** `neodymium-core` (`ai-pipeline`, `ai-prompt`)
- **Scope:** `Framework & AI/Prompt`
- **Symptom:**
  1. In `CartTest_liveNormal_normal_*.html`, active session variables containing layout line breaks (such as `lineItemCount: "CART\n1"`) were injected into the LLM prompt with raw, unescaped newlines. This broke Markdown bullet formatting, introduced prompt injection risks, and corrupted subsequent variable substitution (e.g. turning `Verify that the cart item count is higher than ${lineItemCount}.` into `Verify that the cart item count is higher than CART\n1.`, which flattened to `... CART 1.` and corrupted numerical comparison).
  2. For compound turn groups, sub-steps were rendered twice in conflicting formats: first squashed into a single run-on sentence separated by hyphens under `### Test Instruction:`, and immediately repeated as a numbered checklist under `### Compound Instruction Milestones:`.
- **Root Cause:**
  1. `AgentToolLoopStep.java` appended `guardedData.get(key)` directly inside double quotes without escaping control characters (`\n`, `\r`, `\t`, `"`, `\`).
  2. `ExecuteActionsStep.java` passed `step.getFullInstruction()` to `prepareInstruction()`, which executed `replaceAll("\\s+", " ").trim()`. This collapsed all newlines and indentation into a single line with hyphens. Simultaneously, `KEY_INTERNAL_MILESTONES` was populated with the child steps, causing `AgentToolLoopStep` to append a second, redundant section (`### Compound Instruction Milestones:`).
- **Detection Gap ("What did we miss?"):**
  1. Existing session variable unit tests in `AgentToolLoopStepTest` only tested single-word alphanumeric values (e.g. `"normal"`, `"test123"`), never verifying multiline text, quotes, or whitespace captured from DOM elements.
  2. `ExecuteActionsStepTest` tested `prepareInstruction` solely with single-line control tags, never validating multiline composite instructions.
- **Resolution:**
  1. Implemented `escapeSessionVariableValue` in `AgentToolLoopStep.java` to safely escape special characters (`\n`, `\r`, `\t`, `"`, `\`) for all session variable values rendered into prompt bullets.
  2. In `ExecuteActionsStep.java`, set `rawInstruction` to `step.getRawInstruction()` / `step.getInstruction()` for compound steps so child sub-steps remain exclusively in `KEY_INTERNAL_MILESTONES`.
  3. Updated `ExecuteActionsStep.prepareInstruction` to preserve newline boundaries, collapsing only horizontal whitespace (`[ \t]+`) per line.
  4. In `AgentToolLoopStep.java`, formatted compound instructions as a single unified hierarchical block under `### Test Instruction:` with the parent header immediately introducing the numbered milestones (1..N), eliminating the duplicate `### Compound Instruction Milestones:` section header.
- **Safety Net Added:**
  - `ExecuteActionsStepTest.testPrepareInstructionPreservesNewlines()`
  - `ExecuteActionsStepTest.testCompoundTurnGroupInstructionDoesNotPreFlattenSubSteps()`
  - `AgentToolLoopStepTest.testSessionVariablesEscapingMultilineAndQuotes()`
  - `AgentToolLoopStepTest.testCompoundInstructionPromptUnifiedFormattingWithoutDuplication()`
