# [DEF-20261022-1525] Cumulative ChatMessage History With Embedded DOM Trees Bypasses Turn Extraction

- **Status:** `Resolved`
- **Opened:** 2026-10-22 15:25
- **Closed:** 2026-10-22 15:32
- **Component:** `aura-manager`
- **Scope:** `Framework & AI/Prompt`
- **Symptom:** In multi-turn test steps (Turns 2 to 11), turn cards and clipboard exports dump the entire multi-turn conversation history and 1,500-line `<app-root>` DOM trees into an unformatted wall of text.
- **Root Cause:** Neodymium AI stores the cumulative conversation history in `userPrompt`. The parser lacked multi-message tokenization (`splitChatMessages`) to isolate the latest tool return (`lastToolMsg`) from continuation DOM trees (`lastUserMsg`) and previous turns.
- **Detection Gap ("What did we miss?"):** Previous test cases only tested isolated single tool responses without preceding `ChatMessage[USER]` and `ChatMessage[ASSISTANT]` segments or embedded multi-kilobyte DOM trees.
- **Resolution:** Tokenize accumulated history into discrete `ChatMessage` blocks; extract `lastToolMsg` to generate a concise oneliner for `TURN INPUT`; tuck the full DOM tree and raw tool payloads into the collapsed details drawer; remove per-card copy buttons in favor of the full step copy button.
- **Safety Net Added:** `StaticResourceSyntaxTest.testAccumulatedChatMessageHistoryParsingAndDomCollapsing()`
