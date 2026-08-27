## Purpose

Standardizes declarative condition-driven waiting, dynamic stale element resilience, viewport visibility handling, and robust diagnostic reporting across all Selenide action plugins in Neodymium.

## ADDED Requirements

### Requirement: Standardized Condition-Driven Action Execution
All Selenide action plugins SHALL verify appropriate element readiness conditions using Selenide's declarative condition polling before performing interactive or state-mutating operations.

#### Scenario: Condition-driven click execution
- **WHEN** a `CLICK` action is executed on a selector
- **THEN** the action plugin SHALL ensure the target element is `visible` and `interactable` before invoking the click, scrolling it into view if needed

#### Scenario: Condition-driven text input execution
- **WHEN** a `TYPE` or `CLEAR` action is executed on an input element
- **THEN** the action plugin SHALL ensure the target element is `visible` and `editable` before clearing or setting values

#### Scenario: Condition-driven dropdown option selection
- **WHEN** a `SELECT` action is executed on a select element
- **THEN** the action plugin SHALL ensure the select element is `visible` and `enabled` before selecting the target option

### Requirement: Dynamic Stale Element Resilience
Action execution SHALL NOT cache raw `WebElement` references across condition evaluations or retries, ensuring that DOM node replacements during AJAX/SPA updates automatically re-query the target selector.

#### Scenario: Element re-rendered during execution
- **WHEN** a target element is detached and re-rendered by a client-side JavaScript framework during action execution
- **THEN** Selenide's proxy element SHALL re-query the DOM dynamically without throwing an unhandled `StaleElementReferenceException`

### Requirement: Declarative Assertion Waiting
The `AssertAction` plugin SHALL leverage Selenide's built-in condition evaluation and timeout polling for all text, state, and attribute checks rather than evaluating static snapshots.

#### Scenario: Asynchronously appearing text assertion
- **WHEN** an `ASSERT` action verifies text that appears after an asynchronous delay
- **THEN** `AssertAction` SHALL poll the target element with `shouldHave(text(...))` up to the configured timeout duration before reporting a failure

### Requirement: Diagnostic Action Timeout Reporting
When an action or assertion wait condition fails to be satisfied within the configured timeout, the resulting exception SHALL provide detailed diagnostic context including selector, expected condition, actual element state, and duration.

#### Scenario: Action timeout exception formatting
- **WHEN** an action wait condition times out
- **THEN** the thrown exception SHALL detail the target selector, the failed condition, and the observed DOM element attributes/text
