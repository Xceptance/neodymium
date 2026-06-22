## ADDED Requirements

### Requirement: Spelling and Grammar Validation
The system SHALL parse natural language commands to audit visible text nodes in specified DOM elements or the whole page for spelling and grammatical errors using a configured language locale, taking into account global and step-specific allowlists.

#### Scenario: Successful Spelling and Grammar Audit
- **WHEN** the AI step `Verify spelling on ".modal-body" (locale: en-US)` is executed on an element containing grammatically correct English text without typos
- **THEN** the language diagnostics engine extracts the text, validates it, and the test step passes successfully.

#### Scenario: Spelling Errors Detected
- **WHEN** the AI step `Verify spelling on ".modal-body" (locale: en-US)` is executed on an element containing "Welcome to our webiste"
- **THEN** the validation flags "webiste" as a misspelled word with "website" as a suggested correction inside the test report.

#### Scenario: Ignore Allowed Words Inline
- **WHEN** the AI step `Verify spelling on ".footer" (locale: en-US, ignore: "Xceptance", "Neo")` is executed on a page containing the text "Copyright Xceptance Neo"
- **THEN** the validation ignores "Xceptance" and "Neo" and passes successfully without flagging them as spelling errors.

### Requirement: Scoped Auditing and Target Resolution
The system SHALL support restricting language validation text extraction strictly to matching containers/elements specified in the playbook step. The system SHALL support Neodymium's standard inline hint tags (`(hint: ...)` or `(selector: ...)`) and natural descriptive targets (which the engine SHALL resolve dynamically to DOM selectors).

#### Scenario: Scoped Audit via Existing Standard Hint Tag
- **WHEN** the AI step `Verify spelling of the element (hint: .modal-body) (language)` is executed on a page where only `.modal-body` contains text
- **THEN** the engine extracts visible text nodes strictly from `.modal-body` and its descendants.

#### Scenario: Scoped Audit via Descriptive Natural Language Targeting
- **WHEN** the AI step `Check the language for the description fields (language)` is executed on a page containing a text field grouped by description labels
- **THEN** the engine dynamically resolves "description fields" to the active DOM selectors and extracts text nodes strictly from those target elements.

### Requirement: Casing Consistency Verification
The system SHALL support natural language assertions to verify that the visible text of specified UI elements conforms strictly to a declared casing standard, such as sentence-case, title-case, lowercase, or uppercase.

#### Scenario: Sentence Casing Verified Successfully
- **WHEN** the AI step `Verify casing on ".card-title" (casing: sentence-case)` is executed on elements containing "Select your product" and "Enter email address"
- **THEN** the validation passes successfully without raising warnings.

#### Scenario: Casing Violation Detected
- **WHEN** the AI step `Verify casing on ".card-title" (casing: sentence-case)` is executed on an element containing "Select Your Product"
- **THEN** the validation flags that the text "Select Your Product" violates the sentence-case casing standard in the test report.

### Requirement: Unresolved Translation Placeholder Auditing
The system SHALL support scanning the visible page DOM for typical patterns of unresolved translation keys or template placeholders (such as brackets, braces, triple question marks, or raw spring/thymeleaf property format strings).

#### Scenario: Unresolved Key Leak Detected
- **WHEN** the AI step `Audit page copy for leaks` is executed on a page containing a placeholder string "???button.label.submit???" or "${unresolved_key}"
- **THEN** the validation flags the specific unresolved placeholder key and its CSS selector path in the test report.

### Requirement: Playbook Routing and Context Level Selection
The system SHALL support the explicit step hint tag `(language)`. When present in the step text, the system SHALL route the step to `ContextLevel.LANGUAGE` to execute a text-only DOM extraction and skip rendering graphical screenshot files.

#### Scenario: Context Level LANGUAGE Triggered
- **WHEN** the AI playbook receives the step instruction `Verify spelling of the main text (language)`
- **THEN** the system executes the step within `ContextLevel.LANGUAGE`, skipping visual screenshot capture and extracting visible text nodes from the DOM.

### Requirement: Test-Specific Scenario Extensions (Extend and Amend)
The system SHALL support scenario-level dictionary lists and rules declared in the test case's YAML configuration under the `languageChecks` block. These properties SHALL **extend and amend** (merge with/append to) the global settings from `config/language-rules.md` instead of replacing them.

#### Scenario: Scenario-Specific Dictionary Extension
- **WHEN** the global `config/language-rules.md` allows "GlobalTerm", the test's YAML configuration contains `languageChecks.ignore: ["DynamicMockTerm"]`, and the AI step `Verify spelling (language)` runs on a page containing both "GlobalTerm" and "DynamicMockTerm"
- **THEN** the LLM successfully ignores both terms based on the merged global and scenario-specific context.

### Requirement: Conditional Context and Token Optimization
The system SHALL only load and inject the global Markdown rules from `config/language-rules.md` and test-level YAML configurations into the LLM system prompt when executing a playbook step under `ContextLevel.LANGUAGE`. Standard functional or visual playbook steps SHALL completely bypass this context.

#### Scenario: Bypassing Copy Context on Functional Step
- **WHEN** the AI playbook executes a standard functional step `Click the "Login" button`
- **THEN** the execution context contains no style guidelines, dictionary terms, or copy rules, keeping the token footprint minimal.

### Requirement: LLMless Playbook Support & Graceful Degradation
The system SHALL support standard offline and LLMless playbooks gracefully. Deterministic audits SHALL execute fully offline, whereas LLM-dependent spelling/grammar audits SHALL degrade gracefully to warnings by default without failing the test runner.

#### Scenario: Offline Deterministic Audits Execute Successfully
- **WHEN** the AI step `Verify casing on ".card-title" (casing: sentence-case)` is executed in an LLMless/offline playbook run
- **THEN** the system executes the validation locally using regular expressions and reports results successfully without requiring LLM access.

#### Scenario: Spelling Audit Bypassed Gracefully in LLMless Run
- **WHEN** the AI step `Verify spelling (language)` is executed in an LLMless/offline playbook run while `neodymium.ai.language.strict` is set to `false`
- **THEN** the system records a warning in the test report explaining that spelling validation requires an active LLM and passes the step successfully.

#### Scenario: Spelling Audit Fails in LLMless Run with Strict Mode
- **WHEN** the AI step `Verify spelling (language)` is executed in an LLMless/offline playbook run while `neodymium.ai.language.strict` is set to `true`
- **THEN** the test runner aborts execution immediately and marks the step as failed.

### Requirement: LLMless Replay Caching and Hash-Based Invalidation
The system SHALL support recording and replaying LLM-based language audits. The engine SHALL cache the verdict along with `globalRulesHash`, `scenarioConfigHash`, and `pageCopyHash`. If all three hashes match perfectly during replay, the engine SHALL output the cached verdict offline. If any hash mismatches, the engine SHALL invalidate the replay and execute a live LLMfull run.

#### Scenario: Offline Language Replay Successful
- **WHEN** a playbook runs a `Verify spelling (language)` step, and the current computed `globalRulesHash`, `scenarioConfigHash`, and `pageCopyHash` match the cached record exactly
- **THEN** the engine replays the cached verdict offline immediately without calling the LLM client.

#### Scenario: Replay Invalidated by Copy Change
- **WHEN** a playbook runs a `Verify spelling (language)` step, and the page's visible text copy changes (resulting in a mismatched `pageCopyHash` compared to the cache)
- **THEN** the engine invalidates the replay cache, executes a live LLM step to re-validate, and saves the new hashes to the cache.

#### Scenario: Replay Invalidated by Rule Change
- **WHEN** a playbook runs a `Verify spelling (language)` step, and the global `config/language-rules.md` is modified (resulting in a mismatched `globalRulesHash` compared to the cache)
- **THEN** the engine invalidates the replay cache, executes a live LLM step to re-validate under the new rules, and saves the new hashes to the cache.

### Requirement: Flexible and Metric-Based Failure Mode
The system SHALL support configurable test validation failure modes based on project properties, allowing language violations and API errors to either be logged strictly as warnings in the diagnostic report or fail the functional test step when strictness or metric error thresholds are exceeded.

#### Scenario: Report-Only Soft Warning
- **WHEN** the AI step `Verify spelling of page content (language)` is executed and spelling errors are found while `neodymium.ai.language.strict` is set to `false`
- **THEN** the errors are logged in the test report as diagnostic warnings and the test step passes successfully.

#### Scenario: Strict Failure Mode Enabled
- **WHEN** the AI step `Verify spelling of page content (language)` is executed and spelling errors are found while `neodymium.ai.language.strict` is set to `true`
- **THEN** the test runner aborts execution immediately and marks the step as failed.

#### Scenario: Threshold Metric Exceeded
- **WHEN** the AI step `Verify spelling of page content (language)` detects 5 spelling errors while `neodymium.ai.language.maxErrors` is set to `3`
- **THEN** the test runner aborts execution immediately and marks the step as failed.
