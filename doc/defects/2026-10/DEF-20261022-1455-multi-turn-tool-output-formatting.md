# [DEF-20261022-1455] Multi-Turn LLM Tool Result and Continuation Prompt Bleed Into Lean Turn Card

- **Status:** `Resolved`
- **Opened:** 2026-10-22 14:55
- **Closed:** 2026-10-22 14:58
- **Component:** `aura-manager`
- **Scope:** `Framework & AI/Prompt`
- **Symptom:** In multi-turn LLM calls, the lean TURN INPUT card displays massive unformatted JSON and framework continuation guidance instead of a concise summary, breaking the lean diagnostic view.
- **Root Cause:** `parseLlmPromptContent` fails to isolate `ChatMessage[USER]` from tool execution responses and dumps raw tool JSON and continuation boilerplate directly into `turnInput`.
- **Detection Gap ("What did we miss?"):** Previous tests only asserted single-turn prompts with standard markdown headers (`### Active Session Variables:`, `### Test Instruction:`), lacking multi-turn tool roundtrips containing `TOOL (...)` and `ChatMessage[USER]` continuation blocks.
- **Resolution:** Isolate tool execution JSON from continuation messages; summarize tool status/matches into a clean oneliner for `TURN INPUT`; format raw JSON with indentation and place page context and continuation guidance inside the `[Details ▼]` drawer. Add a one-click clipboard copy button for AI diagnostics.
- **Safety Net Added:** `StaticResourceSyntaxTest.testMultiTurnToolOutputParsingAndClipboardExport()`
