## Purpose

Provides an upfront, single-batch linguistic pre-flight linter that validates playbook instructions across universal, domain-neutral semantic quality categories before test execution begins. Supports optional scenario description context, dual raw and data-substituted instruction representations with source line numbers, strict natural language universality, comprehensive token metrics accounting, and unified report generation (Markdown, HTML, JSON).

## ADDED Requirements

### Requirement: Upfront Pre-Flight Static Linting
The system SHALL support an upfront static linter that analyzes the full sequence of playbook scenario instructions in a single batch LLM call during session initialization, before step execution and per-step pre-flight analysis. The linter findings SHALL be purely advisory and SHALL NOT cause test execution to fail or abort.

#### Scenario: Pre-flight linter executes by default
- **WHEN** a test session starts and `neodymium.ai.linter.enabled` is not explicitly set or set to `true` (default: `true`)
- **THEN** the system SHALL send all scenario steps in a single batch request to the configured linter LLM and record any returned advisory findings without stopping test execution

#### Scenario: Pre-flight linter is bypassed when disabled via property or annotation
- **WHEN** a test session starts and `neodymium.ai.linter.enabled` is set to `false` or annotated with `@AiLinter(false)`
- **THEN** the system SHALL proceed directly to test execution without making a linter LLM call

#### Scenario: Graceful degradation on linter failure
- **WHEN** the linter LLM call fails, times out, or returns invalid JSON
- **THEN** the system SHALL log a warning and continue test execution without interrupting the test run

---

### Requirement: Explicit Scenario Description Context Grounding
The linter SHALL support receiving an optional scenario description to ground semantic evaluation without hardcoding domain assumptions. Context SHALL strictly be read from the playbook YAML `description:` header or a test case `@Description("...")` annotation.

#### Scenario: Grounding with playbook YAML description
- **WHEN** a playbook YAML specifies a top-level `description:` field
- **THEN** the linter SHALL include the description as high-level scenario context in the prompt

#### Scenario: Grounding with test annotation description
- **WHEN** a test method or class is annotated with `@Description("...")`
- **THEN** the linter SHALL supply the annotation description text to the linter prompt

#### Scenario: No description provided
- **WHEN** neither a playbook YAML `description:` nor a `@Description` annotation is present
- **THEN** the linter SHALL evaluate scenario steps based purely on intrinsic linguistic clarity without synthetic context

---

### Requirement: Dual Raw and Resolved Instruction Representation with Line Numbers
The linter finding data model and report presentation SHALL maintain both the raw template instruction (with `${...}` placeholders), the resolved instruction (with substituted test data), and the source line number and file path.

#### Scenario: Recording raw and resolved step representations
- **WHEN** a step containing test data placeholders is evaluated and flagged by the linter
- **THEN** the finding SHALL capture `stepIndex`, `lineNumber`, `sourceFile`, `rawInstruction`, `resolvedInstruction`, and suggested rewrite preserving placeholders

---

### Requirement: Strict Natural Language Universality
The linter SHALL support instructions written in any natural language (English, German, French, Japanese, Spanish, etc.) and SHALL generate rewrite suggestions in the exact same language as the original instruction.

#### Scenario: Non-English instruction evaluation
- **WHEN** scenario steps are written in German, French, Japanese, or any other natural language
- **THEN** the linter SHALL analyze the semantic structure and produce `suggestedRewrite` in the exact same language as the original step

---

### Requirement: Compound Step and Multi-Action Detection
The linter SHALL identify compound instructions combining two or more interactive operations or mixing an action with a post-condition verification, classifying them under `STEP_SPLITTING_CANDIDATE`.

#### Scenario: Detecting compound interactive actions
- **WHEN** an instruction combines two interactive operations, such as `Open the country selector and click "${country}".` or `Öffne das Menü und wähle "${option}"`
- **THEN** the linter SHALL generate a finding with category `STEP_SPLITTING_CANDIDATE` and suggest discrete sequential step splits in the original language

#### Scenario: Detecting mixed action and verification
- **WHEN** an instruction combines an action with a post-condition check, such as `Click submit and verify the confirmation modal appears`
- **THEN** the linter SHALL generate a finding with category `STEP_SPLITTING_CANDIDATE` suggesting an action step followed by a dedicated verification step

---

### Requirement: Missing Visual Tag and Scope Detection
The linter SHALL identify instructions describing visual appearance, layout, colors, badges, icons, typography, or spatial locations that lack explicit visual tags, classifying them under `MISSING_VISUAL_TAG` and recommending either `(visual)` for viewport-scoped observations or `(visual: full)` for whole-page or cross-section observations.

#### Scenario: Detecting visual description lacking visual tag
- **WHEN** an instruction describes viewport visual attributes such as `There is a green badge with a checkmark` without a visual tag
- **THEN** the linter SHALL generate a finding with category `MISSING_VISUAL_TAG` suggesting `(visual)`

#### Scenario: Detecting whole-page or footer visual description
- **WHEN** an instruction describes layout spanning above and below the fold (e.g. `The page has a top banner and a dark footer with copyright links at the bottom`) without a full-page visual tag
- **THEN** the linter SHALL generate a finding with category `MISSING_VISUAL_TAG` suggesting `(visual: full)`

---

### Requirement: Ambiguous Affordance Detection
The linter SHALL identify instructions describing element capability or affordance without an explicit action or verification verb, classifying them under `AMBIGUOUS_AFFORDANCE`.

#### Scenario: Detecting passive affordance phrasing
- **WHEN** an instruction describes element capability (e.g. `There is a link on the left side that allows to log out` or `Ermöglicht das Abmelden`) without an imperative command or verification verb
- **THEN** the linter SHALL generate a finding with category `AMBIGUOUS_AFFORDANCE` suggesting explicit verification or explicit execution

---

### Requirement: Vague Target Anchor Detection
The linter SHALL identify instructions containing under-specified element references or missing contextual anchors, classifying them under `VAGUE_TARGET`.

#### Scenario: Detecting under-specified element target
- **WHEN** an instruction specifies a vague target such as `Click the button` or `Klicke darauf` where multiple matching candidates could exist
- **THEN** the linter SHALL generate a finding with category `VAGUE_TARGET` recommending scoping the target with container, label, or section context

---

### Requirement: Vague or Subjective Verification Detection
The linter SHALL identify test assertions with vague, subjective, or non-verifiable criteria, classifying them under `VAGUE_VERIFICATION` and suggesting concrete verifiable state or element checks.

#### Scenario: Detecting subjective verification criteria
- **WHEN** an instruction uses subjective or non-verifiable criteria such as `Make sure everything looks good` or `Check that the page works correctly`
- **THEN** the linter SHALL generate a finding with category `VAGUE_VERIFICATION` suggesting concrete verifiable criteria

---

### Requirement: Dangling Anaphora and Ambiguous Reference Detection
The linter SHALL identify relative pronouns and ambiguous references (*"it"*, *"that one"*, *"the other option"*) where the antecedent is ambiguous or separated across steps, classifying them under `DANGLING_ANAPHORA`.

#### Scenario: Detecting dangling pronoun reference
- **WHEN** a step refers to an element with an ambiguous pronoun after intervening actions
- **THEN** the linter SHALL generate a finding with category `DANGLING_ANAPHORA` suggesting explicitly naming the target element

---

### Requirement: Temporal Flow and Missing Prerequisite Detection
The linter SHALL identify logical flow anomalies where an action depends on a container, modal, or entity prior to opening or navigating to it, classifying them under `TEMPORAL_FLOW_ANOMALY`.

#### Scenario: Detecting inverted action order
- **WHEN** a step attempts to interact with elements in a dialog or container before the step that triggers or opens it
- **THEN** the linter SHALL generate a finding with category `TEMPORAL_FLOW_ANOMALY`

---

### Requirement: Hardcoded Volatile Dynamic Data Detection
The linter SHALL identify hardcoded absolute execution dates or dynamic IDs in assertion steps, classifying them under `HARDCODED_VOLATILE_DATA`.

#### Scenario: Detecting hardcoded timestamp assertion
- **WHEN** an assertion checks for a hardcoded absolute date like `"2026-08-30"` instead of a parameterized variable `${...}`
- **THEN** the linter SHALL generate a finding with category `HARDCODED_VOLATILE_DATA` suggesting parameterization

---

### Requirement: Dedicated Linter Routing Configuration
The system SHALL support dedicated provider and model configuration for the pre-flight linter, allowing the use of fast and cost-effective LLM models while falling back to the default LLM provider if unspecified.

#### Scenario: Dedicated linter model configuration
- **WHEN** `neodymium.ai.llm.linter.provider` or `neodymium.ai.llm.linter.model` is configured
- **THEN** the linter SHALL route requests to the specified provider and model using `LlmCapability.LINTER`

---

### Requirement: Pre-Flight Linter Token Accounting & Metrics Integration
The system SHALL aggregate token usage (input, output, cached) and call counts resulting from pre-flight linting into `ReportMetrics.linter`, contributing to the scenario's total token volume and cost estimations.

#### Scenario: Token metrics aggregation
- **WHEN** pre-flight linting is executed
- **THEN** token usage and call count SHALL be tracked in `ExecutionContext` (`KEY_LINTER_TOKEN_USAGE`, `KEY_LINTER_CALL_COUNT`), recorded as an `LlmResponseReceivedEvent`, and aggregated into `ReportMetrics.linter` and `ReportMetrics.total`

---

### Requirement: Unified Advisory Report Presentation (Markdown, HTML, JSON)
The test execution report and its format generators (Markdown, HTML, JSON) SHALL present advisory findings and metrics in dedicated sections with line numbers, dual template/resolved views, and category badges.

#### Scenario: Markdown report generation
- **WHEN** a test completes with linter findings
- **THEN** the Markdown report SHALL include a `Playbook Quality & Pre-Flight Findings` table and a `Linter (Pre-Flight)` entry in the LLM Responsibility Breakdown

#### Scenario: HTML and JSON report generation
- **WHEN** a test completes with linter findings
- **THEN** the HTML report SHALL render a pre-flight findings card/table and the JSON report SHALL include serialized `linterFindings`
