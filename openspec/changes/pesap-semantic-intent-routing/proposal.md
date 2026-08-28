## Why

During test execution, natural language instructions exhibit distinct semantic objectives: entering form inputs, clicking action triggers, selecting dropdown options, verifying text and element states, asserting browser metadata (URL / page title), navigating the browser, waiting, extracting variables, or branching logic. When the Action Extractor LLM receives a generic prompt without prior semantic intent guidance, it can suffer from category confusion—such as emitting speculative mutating clicks (`CLICK #submit` or `CLICK #purchase-btn`) on passive wait/assertion steps (e.g. *"Wait for 'Thank you' to appear"*), or selecting incorrect element tags when filling forms. Furthermore, assertions on page titles or URLs unnecessarily incur full DOM captures and heavy token payloads.

An upfront JIT semantic intent classifier in PESAP categorizes each step's primary operational intent (`ASSERT`, `ASSERT_METADATA`, `CLICK`, `TYPE`, `SELECT`, `HOVER_SCROLL`, `NAVIGATE`, `WAIT`, `STORE`, `BRANCH`) before DOM capture and action extraction. This narrows the downstream action search space to relevant HTML element types, enforces hard Java-level execution guards against mutating actions during assertions, and unlocks token-free fast-path assertions for titles and URLs.

## What Changes

- **PESAP Intent Classification (`i`)**: Extend the pre-step PESAP analyzer prompt and response parser to predict the semantic intent alongside context level (`c`), Java methods (`jm`), and step splitting (`sp`).
- **Balanced Operational & Verification Intent Taxonomy**:
  - `ASSERT`: Page and element verifications (text content, numbers, pattern strings, badges, messages, presence, visibility, enabled/disabled, checked/unchecked, focused, counts, wait-for-text).
  - `ASSERT_METADATA`: Browser metadata verifications (page title, current URL, HTTP status) for instant, token-free fast paths.
  - `CLICK`: User clicking buttons, links, checkboxes, icons, tabs, or action triggers.
  - `TYPE`: Form data entry into input fields, textareas, contenteditable elements.
  - `SELECT`: Dropdowns, radio button groups, and option list pickers.
  - `HOVER_SCROLL`: Mouse hover, scrolling to element or viewport position, revealing hidden hover menus.
  - `NAVIGATE`: Browser navigation (open URL, refresh, back, forward).
  - `WAIT`: Explicit pauses, sleeps, or waiting for spinners and animation settling.
  - `STORE`: Extracting/reading values from the page into session variables.
  - `BRANCH`: Conditional logic (If / Else execution branches).
- **Action Extractor Scope Focusing**: Inject active `[SEMANTIC_INTENT]` into the Action Extractor user prompt, narrowing element search (e.g. `TYPE` $\rightarrow$ inputs/textareas, `CLICK` $\rightarrow$ buttons/links, `SELECT` $\rightarrow$ dropdowns/radios).
- **Java Guard Enforcer**: In `ActionExtractionPrompt` and `ExecuteActionsStep`, strictly reject and prevent mutating actions (`CLICK`, `TYPE`, `CLEAR`, `SELECT`) when the step intent is classified as `ASSERT` or `ASSERT_METADATA`.
- **Fast-Path & Context Level Synergy**: Skip redundant DOM dumps and action extraction LLM calls for `ASSERT_METADATA` by routing directly to native Java assertion evaluators.
- **Diagnostics & Reporting**: Expose `semanticIntent` in `ExecutionContext`, `PlaybookStep`, logging, and test execution reports (Markdown and HTML).

## Capabilities

### New Capabilities
- `pesap-semantic-intent-routing`: Covers JIT semantic intent classification in PESAP, downstream action prompt constraint injection, Java execution guardrails against mutating actions on assertions, and title/URL metadata fast-path routing.

### Modified Capabilities
<!-- None: purely additive feature with no existing capability spec requirement modifications -->

## Impact

- **Affected Components**: `org.neodymium.ai.model` (`PlaybookStep`, `SemanticIntent`), `org.neodymium.ai.prompt` (`PesapPrompt`, `ActionExtractionPrompt`), `org.neodymium.ai.pipeline` (`ExecutionContext`, `PesapPreStep`, `ExecuteActionsStep`), `src/main/resources/ai-prompts/` (`pesap-pre-step-prompt.md`, `action-extraction-prompt.md`), `org.neodymium.ai.report` (`TestExecutionReport`, `MarkdownReportGenerator`, `HtmlReportGenerator`).
- **Dependencies**: Uses existing Flash Lite model via `LlmCapability.PESAP`.
- **Compatibility**: Fully backward-compatible; non-blocking fallback if intent classification fails.
