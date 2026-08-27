## Purpose

Enables just-in-time semantic intent classification during pre-step PESAP analysis to categorize instruction goals, constrain downstream action extraction, enforce Java execution guardrails against mutating actions on assertions, and route URL/Title assertions to fast-paths.

## ADDED Requirements

### Requirement: JIT Semantic Intent Classification
The system SHALL classify each playbook step instruction into an explicit semantic intent during the Pre-Execution Step Analysis Phase (PESAP) prior to DOM capture and action extraction.

#### Scenario: Classifying text assertion intent
- **WHEN** the active instruction expresses text validation, message appearance, or wait-for-text conditions (e.g. `Wait for "Order Placed" to appear` or `Attendre que le texte "Merci pour votre achat !" apparaisse`)
- **THEN** PESAP SHALL assign the semantic intent `ASSERT_TEXT`

#### Scenario: Classifying element state assertion intent
- **WHEN** the active instruction expresses element state, presence, visibility, or boolean attribute checks (e.g. `Verify button is enabled` or `Check that the modal is visible`)
- **THEN** PESAP SHALL assign the semantic intent `ASSERT_STATE`

#### Scenario: Classifying browser URL or Title assertion intent
- **WHEN** the active instruction asserts page URL or page title (e.g. `Verify URL contains '/cart.html'` or `Page title is 'Checkout'`)
- **THEN** PESAP SHALL assign the semantic intent `ASSERT_URL` or `ASSERT_TITLE` respectively

#### Scenario: Classifying user interaction intent
- **WHEN** the active instruction expresses UI actions like clicking, typing, selecting, clearing, hovering, or scrolling (e.g. `Click 'Add to Cart'`, `Type 'John' into name`)
- **THEN** PESAP SHALL assign the semantic intent `INTERACT`

#### Scenario: Classifying browser navigation intent
- **WHEN** the active instruction directs browser navigation (e.g. `Open https://example.com/login`, `Go back`, `Refresh page`)
- **THEN** PESAP SHALL assign the semantic intent `NAVIGATE`

### Requirement: Java Execution Guard Against Mutating Actions on Assertions
The system SHALL enforce a deterministic execution boundary preventing mutating or state-changing actions from being emitted or executed when the step intent is an assertion type.

#### Scenario: Mutating action rejected on assertion intent
- **WHEN** a step has semantic intent `ASSERT_TEXT`, `ASSERT_STATE`, `ASSERT_URL`, or `ASSERT_TITLE` and the Action Extractor LLM emits a `CLICK`, `TYPE`, `CLEAR`, or `SELECT` action
- **THEN** the system SHALL reject the mutating action, override or escalate the status, and enforce that only verification actions or state assertions are executed

#### Scenario: Speculative actions prevented on wait-for-text steps
- **WHEN** an instruction waits for a confirmation message to appear and the text is not yet present
- **THEN** the system SHALL execute an assertion against the expected text rather than attempting to click submit or purchase buttons

### Requirement: Downstream Intent Context Injection
The system SHALL inject the classified semantic intent into the Action Extractor prompt to focus the model's action extraction scope.

#### Scenario: Intent passed to action extraction prompt
- **WHEN** PESAP predicts a valid semantic intent for the active step
- **THEN** the compiled action extraction prompt SHALL explicitly indicate the active semantic intent and its allowed action constraints

### Requirement: URL and Title Fast-Path Assertion Routing
The system SHALL support fast-path assertion evaluation for URL and Title verification steps without requiring heavy DOM captures.

#### Scenario: Fast-path execution for title and URL assertions
- **WHEN** PESAP classifies a step as `ASSERT_URL` or `ASSERT_TITLE`
- **THEN** the system SHALL use `MINIMAL` context or evaluate browser state directly via native WebDriver calls, bypassing full DOM capture
