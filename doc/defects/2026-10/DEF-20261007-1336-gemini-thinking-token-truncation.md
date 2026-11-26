# [DEF-20261007-1336] Gemini Thinking Mode Exhausts Max Output Tokens Causing Truncation and JSON Parsing Failures

- **Status:** `Resolved`
- **Opened:** 2026-10-07 13:36
- **Closed:** 2026-10-07 13:50
- **Component:** `neodymium-core`
- **Scope:** `AI/Prompt & Framework`
- **Symptom:** On complex instructions (such as multi-branch conditional rules evaluated by `gemini-3.8-flash` on `ReasoningEffort.HIGH`), the LLM call fails after retries with:
  `JsonParseException: Unrecognized token 'is': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')`
  The raw response contains truncated sentences starting mid-phrase (e.g. `is already there! ...`) rather than a valid JSON structure.
- **Root Cause:** In Google Gemini API, `maxOutputTokens` limits the cumulative sum of internal thinking/reasoning tokens and response content. `ResponseSchema.resolveMaxOutputTokens()` allocated only `4096` tokens for `ACTIONS`. When processing complex conditional instructions, `gemini-3.8-flash` burned ~3,928 tokens on internal thinking, leaving only ~164 tokens before reaching the hard 4,096 ceiling. The API terminated generation mid-stream (`finishReason: LENGTH` / `MAX_TOKENS`), returning cut-off text. Because no valid JSON object was complete, `LlmResponseSanitizer` fell back to returning raw truncated content, which was then fed into Jackson's parser.
- **Detection Gap ("What did we miss?"):** Existing test cases for action extraction used zero or low reasoning budgets (<500 tokens), never coming close to the 4096 combined token ceiling. Complex branching prompts with high reasoning effort were never tested with constrained output token limits.
- **Resolution:**
  1. Increased `ResponseSchema.resolveMaxOutputTokens()` for `ACTIONS`, `ASSERTION`, and `LINTER` from `4096` to `8192` (and scaled other tiers proportionally: `STEP_SPLITS` to 512, `JUDGE` to 2048, `TEXT` to 4096).
  2. In `GeminiLlmProvider`, added detection for `response.finishReason() == FinishReason.LENGTH` to log an explicit, actionable warning pointing directly to token exhaustion and model/token metrics.
  3. In `LlmResponseSanitizer`, reject non-JSON raw strings that contain no JSON delimiters (`{` or `[`), returning an empty string rather than passing conversational thinking text to Jackson.
- **Safety Net Added:**
  1. Updated `ResponseSchemaTest.testResolveMaxOutputTokens` to assert the increased token ceilings.
  2. Added `LlmResponseSanitizerTest.testNonJsonTextWithoutDelimitersReturnsEmpty` verifying that plain conversational text without JSON delimiters returns an empty string.
