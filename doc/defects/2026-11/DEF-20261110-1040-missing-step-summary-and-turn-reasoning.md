# [DEF-20261110-1040] Step Goal Summary and LLM Turn Reasoning Missing in Reports

- **Status:** `Resolved`
- **Opened:** 2026-11-10 10:40
- **Closed:** 2026-11-10 12:30
- **Component:** `neodymium-core` & `aura-manager`
- **Scope:** `Framework & AI/Prompt`
- **Symptom:** 
  1. In Aura Manager and HTML test reports, the `💡 AI Reasoning & Decisioning` card in the step Overview was absent for native tool-calling models (e.g. Gemini 3.5 Flash Lite), and individual LLM turn cards in the LLM Details tab showed only tool call code snippets without any turn-level thought/reasoning.
  2. Multi-turn compound steps had fine-grained per-action thoughts overwritten by the step completion summary in `finalizeStepExecution`.
  3. LLM turns with markdown-fenced JSON or action-array payloads had their thoughts omitted from report call entries.
  4. When `complete_step` was called without a `summary` argument (valid in tool schema), prior rich turn reasoning in `step.getReasoning()` was clobbered by the hardcoded string `"Goal completed"`.
  5. In Aura Manager, clicking "Copy Conversation" or "Copy Turn" exported Markdown that omitted model thought/reasoning traces.
  6. In dark mode, `.llm-thinking-text` and `.llm-tag-thinking` rendered with light-mode hex colors causing a high-glare contrast break.
- **Root Cause:**
  1. `complete_step` tool calls pass a `summary` argument (e.g. `{"summary":"Navigated to www.google.de (redirected to google.com)"}`), but `AgentToolLoopStep` only returned it as `ToolResult.success(...)` and never invoked `step.setReasoning(summary)`.
  2. With native function calling, `response.content()` is empty and `response.hasThinking()` is false by default (`neodymium.ai.gemini.includeThoughts=false`), causing `extractThought()` to return `null`.
  3. `ReportLlmCallEntry` did not store per-turn thoughts or reasoning (`thinking` from `LlmResponse` was dropped in `PreliminaryReportListener`), and `LocalRunJsonStorageService.populateBlockSteps` omitted `reasoning` and `reasonings` when constructing `stepObj`.
  4. Aura Manager's `renderStreamlinedLlmCallCard` in `report-manager.js` only extracted `formattedToolCall` and provided no slot for turn reasoning or model thoughts.
  5. `finalizeStepExecution` rebuilt actions via `Action.fromToolCall(call)` and unconditionally applied `currentStep.getReasoning()` without checking if `currentStep.getActions()` already held explicit per-turn action thoughts.
  6. `PreliminaryReportListener` relied on `startsWith("{")` rather than `LlmResponseSanitizer.extractJson(content)` and did not inspect `actions[0].reasoning`.
  7. In `AgentToolLoopStep`, `summary` defaulted to `"Goal completed"` when omitted from `complete_step` arguments, and `step.setReasoning(sanitizedThought)` was skipped when `proposedActions` was empty on concluding turns.
  8. `generateTurnConversationMarkdown` in `report-manager.js` omitted `call.thinking` / `call.reasoning`.
  9. `report-manager.css` lacked `[data-theme="dark"]` styles for `.llm-tag-thinking` and `.llm-thinking-text`.
- **Detection Gap ("What did we miss?"):** Existing integration tests checked that tool calls executed successfully and parameter substitution occurred, but did not assert that `step.getReasoning()` or `ReportLlmCallEntry.getThinking()` contained the `complete_step` summary or turn-level thought, nor did they assert multi-turn retention of per-action reasoning across subsequent completing turns, omission of `summary` argument in `complete_step`, clipboard Markdown generation, or dark-mode CSS classes.
- **Resolution:**
  1. In `AgentToolLoopStep`, captured `complete_step`'s `summary` argument as nullable, avoided clobbering existing reasoning with `"Goal completed"`, preserved final-turn thoughts on `complete_step` invocations, and preserved existing per-action reasonings on `currentStep.getActions()`.
  2. Added `thinking` / `reasoning` fields to `ReportLlmCallEntry` and preserved `LlmResponse.thinking()`, `complete_step` summary fallback, `LlmResponseSanitizer.extractJson(...)`, and `actions[0].reasoning` extraction in `PreliminaryReportListener` and `InteractiveStateBuilder`.
  3. Updated `LocalRunJsonStorageService.populateBlockSteps` to pass `reasoning` and validated `reasonings` array through to `stepObj` in `run.json` generation.
  4. In `report-manager.js`, `report-manager.css`, and `HtmlReportGenerator.java`, rendered a dedicated `💭 Thought / Turn Reasoning` section in the LLM Details call cards with support for string and array thoughts, added thought export to `generateTurnConversationMarkdown`, and added dark-mode CSS rules for thought tags and containers.
- **Safety Net Added:**
  - `PreliminaryReportListenerTest.testLlmCallThinkingRecordedFromResponseThinking`
  - `PreliminaryReportListenerTest.testLlmCallThinkingRecordedFromCompleteStepSummary`
  - `PreliminaryReportListenerTest.testLlmCallThinkingRecordedFromJsonContent`
  - `PreliminaryReportListenerTest.testLlmCallThinkingRecordedFromMarkdownFencedJsonContent`
  - `PreliminaryReportListenerTest.testLlmCallThinkingRecordedFromActionArrayReasoning`
  - `AgentToolLoopStepTest.testStepReasoningAndActionReasoningPopulatedFromCompleteStepSummary`
  - `AgentToolLoopStepTest.testTurnThoughtsRecordedInPlaybookStepReasonings`
  - `AgentToolLoopStepTest.testMultiTurnActionsRetainIndividualTurnThoughtsWhenStepCompletesWithSummary`
  - `AgentToolLoopStepTest.testCompleteStepWithoutSummaryPreservesPriorTurnReasoning`
  - `AgentToolLoopStepTest.testFinalTurnAssistantThoughtHonoredOnCompleteStep`
  - `RunStorageSyncServiceTest.testSyncLocalRunStoragePopulatesStepReasoningAndReasonings`
