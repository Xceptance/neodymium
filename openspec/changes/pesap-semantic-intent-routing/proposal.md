## Why

During test execution, natural language instructions exhibit distinct semantic objectives: verifying text, asserting element state, checking page title or URL, interacting with forms/buttons, navigating the browser, or branching logic. When the Action Extractor LLM receives a generic prompt without prior semantic intent guidance, it can suffer from category confusion—such as emitting speculative mutating clicks (`CLICK #submit` or `CLICK #purchase-btn`) on passive wait/assertion steps (e.g. *"Wait for 'Thank you' to appear"*). Furthermore, assertions on page titles or URLs unnecessarily incur full DOM captures and heavy token payloads.

An upfront JIT semantic intent classifier in PESAP categorizes each step's primary intent (`ASSERT_TEXT`, `ASSERT_STATE`, `ASSERT_URL`, `ASSERT_TITLE`, `INTERACT`, `NAVIGATE`, `FLOW`, `JAVA_METHOD`) before DOM capture and action extraction. This narrows the downstream action search space, enforces hard Java-level execution guards against mutating actions during assertions, and unlocks token-free fast-path assertions for titles and URLs.

## What Changes

- **PESAP Intent Classification (`i`)**: Extend the pre-step PESAP analyzer prompt and response parser to predict the semantic intent alongside context level (`c`) and step splitting (`sp`).
- **Semantic Intent Taxonomy**:
  - `ASSERT_TEXT`: Text content, numbers, pattern strings, badges, messages, or wait-for-text instructions.
  - `ASSERT_STATE`: Element state/presence (visible, hidden, enabled, disabled, checked, unchecked, focused, selected).
  - `ASSERT_URL`: Browser URL verification.
  - `ASSERT_TITLE`: Page title verification.
  - `INTERACT`: User interactions (click, type, clear, select, hover, scroll, press key).
  - `NAVIGATE`: Browser navigation (open, back, forward, refresh).
  - `BRANCH` / `LOOP` / `WAIT` / `STORE` / `JAVA_METHOD`: Control flow and programmatic execution.
- **Java Guard Enforcer**: In `ActionExtractionPrompt` and `ExecuteActionsStep`, strictly reject and prevent mutating actions (`CLICK`, `TYPE`, `CLEAR`, `SELECT`) when the step intent is classified under `ASSERT_*`.
- **Fast-Path & Context Level Synergy**: Skip redundant DOM dumps and action extraction calls for pure URL and Title assertions by routing directly to native Java assertion evaluators.
- **Diagnostics & Reporting**: Expose `pesapIntent` in `ExecutionContext`, logging, and test execution reports.

## Capabilities

### New Capabilities
- `pesap-semantic-intent-routing`: Covers JIT semantic intent classification in PESAP, downstream action prompt constraint injection, Java execution guardrails against mutating actions on assertions, and title/URL fast-path routing.

### Modified Capabilities
<!-- None: purely additive feature with no existing capability spec requirement modifications -->

## Impact

- **Affected Components**: `org.neodymium.ai.prompt` (`PesapPrompt`, `ActionExtractionPrompt`), `org.neodymium.ai.pipeline` (`ExecutionContext`, `PesapPreStep`, `ExecuteActionsStep`), `src/main/resources/ai-prompts/` (`pesap-pre-step-prompt.md`, `action-extraction-prompt.md`), `org.neodymium.ai.report` (`TestExecutionReport`, `MarkdownReportGenerator`).
- **Dependencies**: Uses existing Flash Lite model via `LlmCapability.PESAP`.
- **Compatibility**: Fully backward-compatible; non-blocking fallback if intent classification fails.
