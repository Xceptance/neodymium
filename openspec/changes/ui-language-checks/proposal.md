## Why

Manual content review is slow, expensive, and highly error-prone. While automated UI tests verify functional behavior and layout structure, they rarely detect typos, spelling mistakes, grammatical issues, or brand terminology inconsistencies across pages. Enabling automated, LLM-powered spelling, grammar, and consistency checks on visible UI text directly within Neodymium's AI Playbooks allows continuous editorial quality control to run seamlessly as part of functional regression testing.

## What Changes

- **AI Playbook Copy/Language Action**: Register a pluggable AI Playbook action (`LanguageDiagnosticsAction`) to parse and handle language/copy verification steps in AI test scripts.
- **AI Playbook Routing & Hints**: Support the explicit step hint tag `(language)` (e.g., `Verify spelling of page header (language)`). When parsed, it routes the execution directly to a lightweight `ContextLevel.LANGUAGE` mode, bypassing screenshot generation and extracting only structured text nodes to minimize execution time and LLM token costs.
- **Scoped Language Audits**: Allow restricting language checks to specific target sections or elements to optimize performance and prevent false positives on unrelated page elements. Support both:
  - **Existing Standard Hint Tags**: Explicitly target elements using Neodymium's pre-existing inline hint tags directly in the step syntax, such as `(hint: .description-fields)` or `(selector: .description-fields)`.
  - **Descriptive Natural Language Targeting**: E.g., `Check the language for the description fields (language)`. The playbook engine resolves natural descriptors (like "description fields") to active DOM selectors before extraction.
  - The engine extracts text nodes **only** from the resolved target elements/containers.
- **Global Markdown Copy Rules**: Support a single, human-readable config file at `config/language-rules.md`. This file combines the dictionary (allowed words), tone guidelines, and text probes (approved/disapproved few-shot examples) in structured Markdown, serving as ideal input context for the LLM.
- **Test-Specific YAML Configurations**: Allow test authors to define scenario-level language checks directly inside their test case YAML files under a `languageChecks` block. These properties SHALL **extend and amend** the global configuration (concatenating lists and guidelines) rather than replacing them:
  - `locale`: Overrides target locale (e.g., `locale: de-DE`).
  - `ignore`: Extends the global dictionary with test-specific allowed words or brand names.
  - `rules`: Appends custom test-specific style guidelines or text probes.
- **Auto-detected & Override Locales**: Auto-detect the target language from the DOM's `<html lang="...">` attribute by default, while supporting explicit step-level overrides (e.g., `(locale: de-DE)`) or test YAML overrides.
- **Conditional Context & Token Optimization**: To save tokens, the global `config/language-rules.md` file and test-level `languageChecks` YAML configurations SHALL only be loaded and passed to the LLM during playbook steps explicitly routed to `ContextLevel.LANGUAGE`. All other standard functional or visual playbook steps completely bypass this context overhead.
- **LLMless Playbook & Replay Support**: Support standard offline and LLMless playbooks gracefully:
  - **Deterministic local audits** (casing consistency and translation key leak scans) SHALL run fully offline on the JVM without requiring any LLM connection.
  - **LLMless Replay Cache**: Support recording and replaying LLM-based language audits. The engine computes SHA-256 hashes of the global Markdown rules (`globalRulesHash`), the test scenario YAML configurations (`scenarioConfigHash`), and the deterministic visible page DOM text (`pageCopyHash`). If all three hashes match a cached replay record, the engine replays the cached LLM verdict offline, costing 0 tokens. If any hash mismatches (copy, rules, or test configs change), the cache is invalidated, and the engine triggers an LLMfull "stateful" recording run to re-validate.
- **Flexible Verification & Failure Metrics**: Record all copy issues (spelling, grammar, casing, key leaks) and LLM network timeouts inside the Allure test report as warning diagnostics. Support configuring strict test failure behavior or setting threshold-based error metrics via project-level properties (e.g., `neodymium.ai.language.strict = true` or `neodymium.ai.language.maxErrors = 3`) to serve as customized CI quality gates.
- **Language Diagnostics Engine**: A subsystem integrated with Neodymium's AI capability (LLM client) to extract visible text from the page, filter out system/dynamic text if needed, segment the copy, and perform linguistic analysis based on global markdown rules and test-specific YAML guidelines.
- **Localization Key Leak Detection**: Scan the DOM automatically for unresolved translation placeholders or raw property keys (e.g., `???button.label???`, `{{missing}}`, `${unresolved}`).

## Capabilities

### New Capabilities
- `ui-language-diagnostics`: Introduces natural language copy audit actions, spelling and grammar checks, tone/style consistency validation, and localization placeholder detection within AI Playbooks.

### Modified Capabilities

## Impact

- **AI Core Routing**: Update Neodymium's `AiAgent.java` to support the custom `(language)` tag, routing it directly to `ContextLevel.LANGUAGE` to ensure the agent uses an optimized, text-only extraction payload.
- **AI Action Plugins**: Register `LanguageDiagnosticsAction` in Neodymium's AI Action Registry to parse and execute language validation steps.
- **Text Extraction Helper**: A browser-side helper to traverse the DOM, extract visible text node contents, and map them to their corresponding selectors/elements.
- **Test Reporting**: Integrate with Neodymium reporting/logging to output detailed spelling, grammar, and casing violations, complete with the original text, offending selectors, and recommended corrections in Allure reports.
