## Context

Neodymium's AI Playbook (`AiAgent`) uses an LLM to interact with and validate web pages. While it currently has page analysis and layout diagnostics capabilities, it does not have a dedicated mechanism for linguistic validation of visible text (such as checking for spelling errors, grammar mistakes, tone consistency, and localized string leaks). By providing a lightweight, pluggable validation action, we can leverage Neodymium's existing LLM architecture to automatically audit page copy during regression test runs.

## Goals / Non-Goals

**Goals:**
- Provide a new pluggable AI Playbook action (`LanguageDiagnosticsAction`) to execute copy and language verification steps.
- Leverage Neodymium's existing `LlmClient` for flexible, context-aware spelling and grammar checks in any language.
- Implement efficient browser-side DOM text extraction that maps text fragments to CSS selectors while ignoring hidden elements, scripts, and styling tags.
- Support deterministic, token-free checking of casing standards (e.g., sentence-case, Title Case) and localized property leaks (e.g., `???key???`) directly on the JVM/Browser side.

**Non-Goals:**
- Integrating heavy offline dictionaries or language validation libraries (like LanguageTool) into the JVM, which would bloat the framework's dependency footprint.
- Translating text automatically (this tool is strictly for quality assurance of existing page copy).

## Decisions

### 1. AI Playbook Routing via `(language)` hint
- **Decision**: Update playbook step parser in `AiAgent.java` to detect the `(language)` hint tag and route the execution context directly to `ContextLevel.LANGUAGE`.
- **Rationale**: Bypassing screenshots and full visual layout tree computations minimizes browser execution time and reduces the token footprint sent to the LLM by up to 90%, since only structured visible text node fragments are needed. The keyword `(language)` and the enum `ContextLevel.LANGUAGE` provide clean, unambiguous semantic intent for text copy audits, avoiding any confusion with clipboard operations.

### 2. Scoped Audits via Existing Hints & Natural Descriptive Targeting
- **Decision**: Allow spelling, grammar, and casing checks to restrict text extraction only to specified DOM containers/elements. The engine SHALL resolve targets in two ways:
  1. **Existing Standard Hint Tags**: Extract CSS selectors directly using Neodymium's pre-existing inline hint tags within the step text, such as `(hint: .description-fields)` or `(selector: .description-fields)`.
  2. **Descriptive Natural Language Targeting**: Formulated as descriptive names within the step (e.g., `Check the language for the description fields (language)`). The AI playbook engine leverages its pre-existing DOM element matching capability to resolve "description fields" to active CSS selectors (e.g. `textarea.description` or `.description-fields`) before text extraction.
- **Rationale**: Reusing the standard `(hint: ...)` and `(selector: ...)` tags simplifies parsing code, reduces development overhead, and maintains absolute consistency with Neodymium's existing playbook step targeting patterns. Limiting audits to specific sections dramatically decreases the token footprint sent to the LLM and keeps execution costs minimal.

### 3. Hybrid Language Locale Resolution
- **Decision**: Auto-detect the target locale by reading the DOM's `<html lang="...">` attribute. If it's missing, fall back to the project default. Provide step-level overrides via inline hints (e.g., `(locale: de-DE)` or `(locale: fr-FR)`), or via test-level YAML configurations.
- **Rationale**: This guarantees zero-configuration multi-lingual validation for typical localized websites by default, while giving test authors precise control when checking specific language switchers or multi-lingual pages.

### 4. Global Markdown Language Rules
- **Decision**: Store all global language rules, dictionaries, brand tone guidelines, and text probes in a single, rich structured Markdown file located at `config/language-rules.md`.
- **Rationale**: LLMs are exceptionally well-suited for processing structured Markdown context (headings, lists, bold text). Merging dictionaries and guidelines into a single human-editable file simplifies management and improves LLM parsing performance compared to flat plain-text files.

### 5. Test-Specific YAML Configurations (Extend and Amend)
- **Decision**: Support scenario-level extensions and amendments defined in the test case's YAML configuration under a `languageChecks` block. These properties **shall extend and amend** the global settings, merging dictionaries and appending style rules instead of replacing them:
  ```yaml
  languageChecks:
    locale: fr-FR      # Explicit override
    ignore:            # Extends global Allowed Words
      - "TestSpecificBrand"
    rules: |           # Appends to global guidelines
      Test-specific tone rules...
  ```
- **Rationale**: Functional test scenarios often interact with dynamic test data, specific accounts, or special locales. Enabling test-level declarations ensures authors can dynamically expand the global allowlist or append specific style constraints for particular scenarios without erasing the primary project-level guidelines.

### 6. LLM-Based Ignore and Style Filtering (No JVM Regex Parsing)
- **Decision**: Do not implement complex, brittle Java regex compilation or string comparison logic for word list ignores on the JVM. Instead, the Java action simply extracts the allowed words and style rules from both `config/language-rules.md` and the test-level YAML, parses them, and injects them directly into the LLM system prompt (e.g., *"The following terms are allowed: ..."*).
- **Rationale**: LLMs naturally understand morphological and grammatical variations of brand names and words contextually (e.g., ignoring *"Xceptance's"* when *"Xceptance"* is listed). Offloading ignore verification to the LLM simplifies the codebase and provides a far more robust filtering experience.

### 7. Token Optimization & Conditional Context Loading
- **Decision**: Ensure that the structured Markdown language rules and the test YAML configurations are loaded and injected into the LLM system context **ONLY** when a playbook step is being executed under `ContextLevel.LANGUAGE`. 
- **Rationale**: Passing style guidelines, text probes, and dictionaries on every standard functional step (like element interactions, assertions, and navigation) would create massive, unnecessary token overhead. Bypassing this context on standard steps keeps operational costs low and guarantees maximum token efficiency across standard execution runs.

### 8. LLMless Playbook Support & Graceful Degradation
- **Decision**: Design `LanguageDiagnosticsAction` to gracefully degrade when running in local development or offline test environments where LLM access is completely disabled/unavailable:
  1. **Deterministic Audits**: Casing checks and translation key leak scans SHALL execute 100% offline, remaining fully operational in LLMless mode.
  2. **AI-dependent Audits**: Spelling and grammar validations detect the lack of LLM connectivity, record a descriptive soft warning in the test report (e.g., *"Spelling check skipped: running in LLMless mode"*), and allow the test step to pass by default.
  3. **Strict Enforcement**: If `neodymium.ai.language.strict = true` is configured, LLM-dependent steps will fail in LLMless mode.
- **Rationale**: This prevents local developer runs or offline pipelines from breaking unnecessarily while still letting teams enforce strict verification on staging/CI pipelines where LLM integrations are required.

### 9. LLMless Replay Caching & Hash-Based Invalidation
- **Decision**: Extend Neodymium's offline replay mechanism to language checks by caching the LLM's `cachedVerdict` along with three dynamic SHA-256 hashes to verify state consistency:
  1. `globalRulesHash`: Computes the hash of the global `config/language-rules.md` file.
  2. `scenarioConfigHash`: Computes the hash of the active test's YAML `languageChecks` block.
  3. `pageCopyHash`: Extends DOM extraction to sort visible text elements by selector, serialize them as standard `[selector]:[text]` lines, and hash the resulting text payload.
- **Workflow**:
  * **Replay Match**: During a replay run, if the computed current `globalRulesHash`, `scenarioConfigHash`, and `pageCopyHash` **match perfectly** with the cached record, the engine immediately outputs the `cachedVerdict` offline (0 tokens, instant speed).
  * **Invalidation**: If any hash mismatches (rules, test configs, or page text changed), the replay is invalidated, and the engine automatically switches to a live `LLMfull` (stateful recording) turn to re-validate and update the cache.
- **Rationale**: This maintains perfect cost efficiency, guaranteeing zero LLM API calls on subsequent regression runs as long as the page copy, style guidelines, and test parameters remain unchanged.

### 10. Flexible Verification, Report-Only, and Metric-Based Failures
- **Decision**: By default, validation findings (spelling/grammar errors, casing errors, key leaks) and external LLM client connection timeouts/failures SHALL be recorded as warnings inside the Allure test report and NOT fail the functional test step. Support configuring strict failures or metric-based failure thresholds using project-level properties in `neodymium.properties`:
  * `neodymium.ai.language.strict = true/false` (defaults to `false` for soft/diagnostic reporting)
  * `neodymium.ai.language.maxErrors = <N>` (fails the step only if total detected issues exceed `<N>`, defaults to unbounded)
- **Rationale**: Copy spelling or grammar issues should not silently disrupt critical functional test suites or break CI pipelines due to transient LLM API hiccups by default. Recording them as rich diagnostic feedback in reports while supporting customizable quality gates offers optimal flexibility.

## Risks / Trade-offs

- **Risk**: Dynamic user content (e.g., usernames, brand names, product titles) triggering false positive spelling errors.
  - **Mitigation**: Encourage scoping audits to specific target containers using selectors (e.g., `Verify spelling on ".product-description"`). Rely on the dual-tiered allowlist (global + inline/YAML `ignore` overrides) to easily bypass false positives.
- **Risk**: Dynamic text shifts triggering LLM latency on large pages.
  - **Mitigation**: Using `ContextLevel.LANGUAGE` ensures the payload is lightweight. The LLM prompt is heavily optimized for fast, single-turn text audit classification.
