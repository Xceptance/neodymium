## Why

While Google Lighthouse is currently supported in Neodymium for accessibility testing, it is heavyweight, works only with Chrome, requires a local Node.js CLI installation, and runs in a separate process that reloads the page. This makes it impossible to perform accessibility audits on dynamic, stateful page interactions (e.g., opened modals, expanded menus, form validation states, and single-page application views) without losing the session or state.

Furthermore, our test automation suite utilizes natural language **AI Playbooks** to define test scripts. QA engineers need a clean way to declare accessibility validation steps directly in these AI test scripts—specifying a full-page review, focusing on a specific element area, and choosing whether to fail on violations, record results to a document, or both.

To solve this, we will introduce:
1. An integrated, zero-maven-dependency **Axe-core** accessibility engine (`AxeUtils`).
2. A pluggable AI Playbook action (`AccessibilityAction`) to support natural language script statements like `Verify accessibility of #main-content` or `Audit accessibility of page and record results`.

## What Changes

- **Axe-core Integration**: Embed the minified Axe-core JavaScript engine (`axe.min.js`) into Neodymium's classpath resources so it can be injected on demand into any active browser session.
- **AxeUtils API**: Introduce a fluent utility class `AxeUtils` to execute accessibility checks on the active page.
  - Support auditing the entire page or specific target elements (selectors).
  - Support customizing Axe options (rules, tags, contexts).
  - Support a non-failing "record only" mode that outputs accessibility reports without stopping the test.
- **AI Playbook Action Integration**: Register a new core AI action plugin `AccessibilityAction` supporting the action type `"ACCESSIBILITY"`.
  - Parse natural language instruction steps from AI scripts (e.g. `Verify accessibility focusing on .modal-content` or `Audit accessibility and record only`).
  - Pass custom selectors (focus areas) and options (tags, failOnViolations, recordOnly) directly to the execution engine.
- **Assertion and Failures**: Automatically assert on accessibility violations when configured. Throw a detailed `AssertionError` listing the violations, their impact, descriptions, target elements, and resolution guidance.
- **Allure Reporting & Recording**: Automatically attach a structured, human-readable summary of violations to Allure reports, and write summaries to a record log file to document outcomes.
- **Neodymium Configuration**: Add configuration properties (`neodymium.properties`) to set default Axe tags (e.g., `wcag2a`, `wcag2aa`, `wcag21aa`), toggle assertions on/off, and specify reporting options.

## Capabilities

### New Capabilities

- `accessibility-testing-axe`: Provides embedded Axe-core accessibility auditing on the active browser session, supporting customizable contexts/options, natural language AI playbook integration, detailed assertion errors, and Allure reporting.

### Modified Capabilities

None.

## Impact

- **New Files**:
  - `src/main/java/com/xceptance/neodymium/util/AxeUtils.java` (main API and logic).
  - `src/main/java/com/xceptance/neodymium/ai/action/plugins/AccessibilityAction.java` (AI Action Plugin).
  - `src/main/resources/js/axe.min.js` (embedded Axe-core JS engine).
- **Modified Files**:
  - `src/main/java/com/xceptance/neodymium/ai/action/ActionRegistry.java` (register the new AI Action plugin).
- **Configuration**:
  - Extension of `NeodymiumConfiguration.java` with new property methods for Axe configuration.
  - New default properties in `config/neodymium.properties`.
- **Dependencies**: No new maven/external dependencies are introduced! We package the MIT-licensed `axe.min.js` file internally in resources, ensuring zero setup overhead and complete offline portability.
