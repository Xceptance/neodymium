## Purpose

Enables just-in-time semantic intent classification during pre-step PESAP analysis to categorize instruction goals, constrain downstream action extraction to relevant element types, enforce Java execution guardrails against mutating actions on assertions, and route browser metadata assertions to fast paths.

## ADDED Requirements

### Requirement: JIT Semantic Intent Classification
The system SHALL classify each playbook step instruction into an explicit semantic intent during the Pre-Execution Step Analysis Phase (PESAP) prior to DOM capture and action extraction.

#### Scenario: Classifying page and element assertion intent
- **WHEN** the active instruction expresses text validation, message appearance, element state/presence, visibility, counts, or wait-for-text conditions (e.g. `Wait for "Order Placed" to appear`, `Verify button is enabled`, `Check that the modal is visible`, `Attendre que le message apparaisse`)
- **THEN** PESAP SHALL assign the semantic intent `ASSERT`

#### Scenario: Classifying browser metadata assertion intent
- **WHEN** the active instruction asserts page URL, page title, or browser metadata (e.g. `Verify URL contains '/cart.html'` or `Page title is 'Checkout'`)
- **THEN** PESAP SHALL assign the semantic intent `ASSERT_METADATA`

#### Scenario: Classifying click interaction intent
- **WHEN** the active instruction directs clicking on buttons, links, icons, checkboxes, or tabs (e.g. `Click 'Add to Cart'`, `Click on the search icon`)
- **THEN** PESAP SHALL assign the semantic intent `CLICK`

#### Scenario: Classifying typing data-entry intent
- **WHEN** the active instruction directs typing, entering, or filling input fields and textareas (e.g. `Type 'john@example.com' into email`, `Enter password`)
- **THEN** PESAP SHALL assign the semantic intent `TYPE`

#### Scenario: Classifying dropdown selection intent
- **WHEN** the active instruction directs selecting options from dropdowns, radio groups, or list pickers (e.g. `Select 'United States' from country`, `Choose size 'Medium'`)
- **THEN** PESAP SHALL assign the semantic intent `SELECT`

#### Scenario: Classifying hover or scroll intent
- **WHEN** the active instruction directs hovering over elements or scrolling the viewport (e.g. `Hover over 'Categories'`, `Scroll down to footer`)
- **THEN** PESAP SHALL assign the semantic intent `HOVER_SCROLL`

#### Scenario: Classifying browser navigation intent
- **WHEN** the active instruction directs browser navigation (e.g. `Open https://example.com/login`, `Go back`, `Refresh page`)
- **THEN** PESAP SHALL assign the semantic intent `NAVIGATE`

#### Scenario: Classifying delay and wait intent
- **WHEN** the active instruction directs a temporal pause or delay (e.g. `Wait 3 seconds`, `Pause for animation`)
- **THEN** PESAP SHALL assign the semantic intent `WAIT`

#### Scenario: Classifying data extraction intent
- **WHEN** the active instruction directs extracting or saving on-screen values to session variables (e.g. `Store order ID into $orderId`, `Save price to $itemPrice`)
- **THEN** PESAP SHALL assign the semantic intent `STORE`

#### Scenario: Classifying conditional branching intent
- **WHEN** the active instruction directs conditional execution (e.g. `If cookie banner is visible then click Accept`)
- **THEN** PESAP SHALL assign the semantic intent `BRANCH`

### Requirement: Java Execution Guard Against Mutating Actions on Assertions
The system SHALL enforce a deterministic execution boundary preventing mutating or state-changing actions from being emitted or executed when the step intent is an assertion type.

#### Scenario: Mutating action rejected on assertion intent
- **WHEN** a step has semantic intent `ASSERT` or `ASSERT_METADATA` and the Action Extractor LLM emits a `CLICK`, `TYPE`, `CLEAR`, or `SELECT` action
- **THEN** the system SHALL reject the mutating action, override or escalate the status, and enforce that only verification actions or state assertions are executed

#### Scenario: Speculative actions prevented on wait-for-text steps
- **WHEN** an instruction waits for a confirmation message to appear and the text is not yet present
- **THEN** the system SHALL execute an assertion against the expected text rather than attempting to click submit or purchase buttons

### Requirement: Downstream Intent Context Injection
The system SHALL inject the classified semantic intent into the Action Extractor prompt to focus the model's action extraction scope to relevant element types.

#### Scenario: Intent passed to action extraction prompt
- **WHEN** PESAP predicts a valid semantic intent for the active step
- **THEN** the compiled action extraction prompt SHALL explicitly indicate the active semantic intent (`[SEMANTIC_INTENT]`) and its allowed action/element constraints

### Requirement: Metadata Fast-Path Assertion Routing
The system SHALL support fast-path assertion evaluation for URL and Title verification steps without requiring heavy DOM captures.

#### Scenario: Fast-path execution for metadata assertions
- **WHEN** PESAP classifies a step as `ASSERT_METADATA`
- **THEN** the system SHALL use `MINIMAL` context or evaluate browser state directly via native WebDriver calls, bypassing full DOM capture

### Requirement: 360° LLM Taming & Safety Lifecycle Documentation
The system documentation in `doc/DOCUMENTATION.md` SHALL include a dedicated section detailing all defense-in-depth safety guardrails, execution invariants, and verification mechanisms used to tame LLM non-determinism.

#### Scenario: Documenting comprehensive LLM safety mechanisms
- **WHEN** the technical reference manual `doc/DOCUMENTATION.md` is compiled or updated
- **THEN** it SHALL include Section 4.4 detailing:
  1. Pre-execution guardrails (JIT semantic intent routing, volatile ID stripping, outbound credential masking)
  2. In-flight execution boundaries (Java-level mutating action rejection on assertions, action scope focusing, timeout guards)
  3. Post-execution verification (semantic outcome verification, second-opinion Quality Judge auditing, temporal visual stability settling, and visual RCA diagnostics)
