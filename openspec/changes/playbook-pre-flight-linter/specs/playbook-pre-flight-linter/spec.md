## Purpose

Provides an upfront, single-batch linguistic pre-flight linter that validates playbook instructions across four quality categories (compound steps, missing visual tags, ambiguous affordances, vague target anchors) before test execution begins.

## ADDED Requirements

### Requirement: Upfront Pre-Flight Static Linting
The system SHALL support an upfront static linter that analyzes the full sequence of playbook scenario instructions in a single batch LLM call during session initialization, before step execution and per-step pre-flight analysis. The linter findings SHALL be purely advisory and SHALL NOT cause test execution to fail or abort.

#### Scenario: Pre-flight linter executes on enabled session
- **WHEN** a test session starts and `neodymium.ai.linter.enabled` is set to `true`
- **THEN** the system SHALL send all scenario steps in a single batch request to the configured linter LLM and record any returned advisory findings without stopping test execution

#### Scenario: Pre-flight linter is bypassed when disabled
- **WHEN** a test session starts and `neodymium.ai.linter.enabled` is `false` (default)
- **THEN** the system SHALL proceed directly to test execution without making a linter LLM call

#### Scenario: Graceful degradation on linter failure
- **WHEN** the linter LLM call fails, times out, or returns invalid JSON
- **THEN** the system SHALL log a warning and continue test execution without interrupting the test run

### Requirement: Compound Step and Multi-Action Detection
The linter SHALL identify compound instructions that combine two or more distinct interactive operations or mix an interactive operation with a post-condition verification, classifying them under `STEP_SPLITTING_CANDIDATE` with actionable suggestions to split them into discrete atomic steps.

#### Scenario: Detecting compound interactive actions
- **WHEN** an instruction combines two interactive operations, such as `Open the country selector and click "${country}".` or `Click button X and type Y into input Z`
- **THEN** the linter SHALL generate a finding with category `STEP_SPLITTING_CANDIDATE`, warning severity, and suggest discrete sequential step splits

#### Scenario: Detecting mixed action and verification
- **WHEN** an instruction combines an action with a post-condition check, such as `Click submit and verify the confirmation modal appears`
- **THEN** the linter SHALL generate a finding with category `STEP_SPLITTING_CANDIDATE` suggesting an action step followed by a dedicated verification step

### Requirement: Missing Visual Tag Detection
The linter SHALL identify instructions describing visual appearance, layout, colors, badges, icons, typography, or spatial locations that lack explicit visual tags, classifying them under `MISSING_VISUAL_TAG`.

#### Scenario: Detecting visual description without visual tag
- **WHEN** an instruction describes visual attributes such as `There is a green badge with a checkmark` or `Auf der rechten Seite ist ein rotes Warnsymbol` without a `(visual)` or `(layout)` tag
- **THEN** the linter SHALL generate a finding with category `MISSING_VISUAL_TAG` suggesting the addition of `(visual)` or `(layout)`

### Requirement: Ambiguous Affordance Detection
The linter SHALL identify instructions describing element capability or affordance without an explicit action or verification verb, classifying them under `AMBIGUOUS_AFFORDANCE`.

#### Scenario: Detecting passive affordance phrasing
- **WHEN** an instruction describes element capability (e.g. `There is a link on the left side that allows to log out` or `Ermöglicht das Abmelden`) without an imperative command or verification verb
- **THEN** the linter SHALL generate a finding with category `AMBIGUOUS_AFFORDANCE` suggesting explicit verification (`Verify that the logout link is visible...`) or explicit execution (`Click the logout link...`)

### Requirement: Vague Target Anchor Detection
The linter SHALL identify instructions containing under-specified element references or missing contextual anchors, classifying them under `VAGUE_TARGET`.

#### Scenario: Detecting under-specified element target
- **WHEN** an instruction specifies a vague target such as `Click the button` or `Klicke darauf` where multiple matching candidates could exist
- **THEN** the linter SHALL generate a finding with category `VAGUE_TARGET` recommending scoping the target with container, label, or section context

### Requirement: Dedicated Linter Routing Configuration
The system SHALL support dedicated provider and model configuration for the pre-flight linter, allowing the use of fast and cost-effective LLM models while falling back to the default LLM provider if unspecified.

#### Scenario: Dedicated linter model configuration
- **WHEN** `neodymium.ai.llm.linter.provider` or `neodymium.ai.llm.linter.model` is configured in `ai.properties` or system properties
- **THEN** the linter SHALL route requests to the specified provider and model using `LlmCapability.LINTER`

#### Scenario: Fallback to general LLM provider
- **WHEN** no dedicated linter provider or model is configured
- **THEN** the linter SHALL fall back to the general default LLM provider

### Requirement: Advisory Test Report Presentation
The test execution report and Markdown report generator SHALL present any advisory findings produced by the pre-flight linter in a dedicated report section.

#### Scenario: Presenting findings in Markdown report
- **WHEN** the test run completes with one or more linter advisory findings
- **THEN** the Markdown report SHALL include a `Playbook Quality & Pre-Flight Findings` section detailing step index, original instruction, category, severity, message, and suggested rewrite
