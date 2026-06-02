## 1. Browser-Side Text Extraction

- [ ] 1.1 Implement browser-side JavaScript utility to traverse the DOM, extract visible text node contents, filter out purely numeric/date/whitespace tokens, and return a list of text nodes with their exact CSS selector paths.
- [ ] 1.2 Write unit/integration tests for the text extraction script using diverse mock HTML pages containing nested elements, hidden sections, and various content types.

## 2. Core Language Diagnostics Action & Routing

- [ ] 2.1 Add `ContextLevel.LANGUAGE` to Neodymium's `ContextLevel` enum and update `AiAgent.java` to route instructions containing the `(language)` hint tag directly to `ContextLevel.LANGUAGE` (skipping browser screenshot generation).
- [ ] 2.2 Create `LanguageDiagnosticsAction` class in `com.xceptance.neodymium.ai.action` implementing the AI Playbook action interface.
- [ ] 2.3 Register the action in the AI action registry to parse natural language instructions containing patterns like `Verify spelling`, `Verify casing`, and `Audit page copy`.
- [ ] 2.4 Implement scoped audits: parse standard inline hint tags (`(hint: ...)` or `(selector: ...)`) and resolve descriptive targets (e.g. "description fields") to active DOM selectors using the playbook engine's pre-existing element mapping service.
- [ ] 2.5 Implement hybrid locale detection: read the DOM `<html lang="...">` attribute inside the action, falling back to project configuration, and supporting inline step parameter overrides (e.g., `(locale: de-DE)`).
- [ ] 2.6 Implement a structured Markdown parser to load global allowed words, tone rules, and style guidelines from `config/language-rules.md`.
- [ ] 2.7 Implement parser to read test-specific scenario-level extensions under the `languageChecks` block in the active test case's YAML configuration.
- [ ] 2.8 Implement deterministic JVM-side casing style checks (e.g., sentence-case, Title Case, lowercase, UPPERCASE) via regular expressions.
- [ ] 2.9 Implement deterministic placeholder scanning to detect raw translation keys or templating patterns (e.g., `???key???`, `${key}`, `{{key}}`).

## 3. LLM Integration, Fallbacks & Token Optimization

- [ ] 3.1 Design specific AI system prompts for the existing `LlmClient` to perform contextual spelling, grammar, and tone consistency checks on extracted text blocks, returning structured correction recommendations.
- [ ] 3.2 Implement conditional context injection: load and merge the global `config/language-rules.md` file and scenario-specific YAML configurations to pass into the LLM system prompt **only** when a step is executed within `ContextLevel.LANGUAGE` (bypassing this context payload on standard functional/visual steps to save tokens).
- [ ] 3.3 Implement LLMless fallback logic: ensure local casing and placeholder validations execute 100% offline without requiring LLM connectivity, while spelling/grammar validations degrade gracefully to soft warnings in LLMless mode by default (unless strict mode is configured).
- [ ] 3.4 Implement SHA-256 hashing utilities for:
  - Global `config/language-rules.md` contents (`globalRulesHash`).
  - Active test scenario YAML `languageChecks` blocks (`scenarioConfigHash`).
  - Extracted browser DOM text elements (sorting by selector, serializing as `[selector]:[text]` lines, and hashing payload to compute `pageCopyHash`).
- [ ] 3.5 Implement the LLMless Replay Cache engine: on `(language)` steps, compare computed hashes with the cached replay record. If they match perfectly, replay the verdict offline (0 tokens); if any hash mismatches, invalidate the step cache and execute a live LLMfull turn to re-evaluate and record.
- [ ] 3.6 Integrate the LLM audit workflow into `LanguageDiagnosticsAction` for spelling/grammar steps, with flexible support for report-only warnings, strict failure triggers, and configurable threshold-based metrics (e.g., `neodymium.ai.language.strict` and `neodymium.ai.language.maxErrors`).

## 4. Reporting and Verification

- [ ] 4.1 Integrate the linguistic check results into Neodymium's test reporting and logging system to highlight failing elements, selectors, and suggestion details in Allure reports as prominent warnings or step failures depending on active strictness configurations.
- [ ] 4.2 Implement comprehensive integration tests verifying successful audits as well as correct detection and reporting of spelling errors, grammar mistakes, casing violations, and key leaks under different strictness, LLMless, and threshold configurations.
