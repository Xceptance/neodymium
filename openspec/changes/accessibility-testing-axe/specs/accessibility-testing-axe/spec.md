## ADDED Requirements

### Requirement: Axe-core Engine Resource Loading
Neodymium SHALL embed the minified Axe-core JavaScript engine as a classpath resource at `js/axe.min.js`. The `AxeUtils` library SHALL read this file from the classpath once and cache its content in memory to avoid redundant disk I/O.

#### Scenario: Successful loading and caching of Axe-core JS
- **WHEN** `AxeUtils` is invoked for the first time
- **THEN** the `axe.min.js` resource is read from the classpath and cached in memory
- **WHEN** `AxeUtils` is invoked subsequently
- **THEN** the cached Javascript string is used instead of re-reading from the classpath

### Requirement: Axe-core Injection and Execution
The `AxeUtils` class SHALL expose a method to execute accessibility audits on the active page. The execution engine SHALL verify if the `axe` object is already defined in the active browser context; if not, it SHALL inject the cached `axe.min.js` content. It SHALL then execute `axe.run()` asynchronously and return the JSON audit results.

#### Scenario: Injection on a clean page
- **WHEN** `AxeUtils.checkAccessibility()` is called on a page that has not had Axe injected
- **THEN** Neodymium injects the `axe.min.js` script into the page context and executes the accessibility scan successfully

#### Scenario: Running on a page where Axe is already present
- **WHEN** `AxeUtils.checkAccessibility()` is called on a page that already has the `axe` object defined
- **THEN** Neodymium skips script injection and executes the accessibility scan immediately

### Requirement: Audit Options and Customization
The `AxeUtils` execution API SHALL allow users to customize the accessibility audit by passing:
1. Contexts/Selectors: Specific elements or areas to include in or exclude from the scan.
2. Options: Custom Axe run options, such as specific tags to run (e.g., `wcag2aa`) or rules to execute.

#### Scenario: Auditing a specific page section
- **WHEN** `AxeUtils.checkAccessibility()` is called with a specific CSS selector (e.g., `#main-content`)
- **THEN** Axe only scans elements within that container, ignoring violations outside of it

#### Scenario: Auditing with specific WCAG tags
- **WHEN** `AxeUtils.checkAccessibility()` is called with specific tags (e.g., `["wcag2a", "wcag2aa"]`)
- **THEN** Axe only executes rules associated with those standard tags

### Requirement: Assertion and Failure Reporting
The accessibility check SHALL automatically validate the audit results. If any violations are found, and the failure-on-violation configuration is enabled, it SHALL throw a detailed `AssertionError`. This error message SHALL contain a structured list of all violations, including their ID, impact level, description, target CSS selectors, and help URL.

#### Scenario: Throwing assertions on accessibility violations
- **WHEN** `AxeUtils.checkAccessibility()` is executed and finds 2 violations, and `failOnViolations` is set to `true`
- **THEN** the check throws an `AssertionError` detailing the violations
- **AND** the error message contains the target elements, HTML snippets, and help links for each violation

#### Scenario: Suppressing assertions on accessibility violations
- **WHEN** `AxeUtils.checkAccessibility()` is executed and finds violations, and `failOnViolations` is set to `false`
- **THEN** the check does not throw an error and completes successfully

### Requirement: Allure Report Attachment
Neodymium SHALL automatically format the accessibility violations and attach them to the active Allure test run as a readable text or HTML attachment. This attachment SHALL list all violations, their descriptions, impact levels, affected HTML elements, and the recommended solutions.

#### Scenario: Generating Allure attachments for audits
- **WHEN** `AxeUtils.checkAccessibility()` completes an audit
- **THEN** it automatically attaches a structured summary of the results to the current Allure test context

### Requirement: Configuration Properties
Neodymium SHALL support the following properties in `neodymium.properties` to configure Axe behavior:
1. `neodymium.axe.failOnViolations`: Boolean to control whether audits throw assertions on violations (default: `true`).
2. `neodymium.axe.tags`: Space-separated list of default Axe-core tags to execute (default: `wcag2a wcag2aa`).
3. `neodymium.axe.rules`: Space-separated list of specific rules to enable.

#### Scenario: Reading configuration properties
- **WHEN** Neodymium initializes the Axe engine
- **THEN** it reads default tags, fail-on-violation settings, and rule overrides from `neodymium.properties`

### Requirement: AI Playbook Accessibility Script Action
Neodymium SHALL register a new core AI action plugin `AccessibilityAction` that parses natural language script steps related to accessibility validation, and maps them to a native `ACCESSIBILITY` action.
The action SHALL support parsing statements with keywords: `"accessibility"`, `"a11y"`, `"wcag"`, `"axe"`.

#### Scenario: Parsing a general accessibility validation step
- **WHEN** the AI test script contains: `Verify accessibility of page`
- **THEN** `AccessibilityAction` parses it into an `Action` with type `ACCESSIBILITY`, target `page`, and value `null`

#### Scenario: Parsing a focused area validation step
- **WHEN** the AI test script contains: `Verify accessibility focusing on #main-content`
- **THEN** `AccessibilityAction` parses it into an `Action` with type `ACCESSIBILITY`, target `#main-content`, and value `null`

#### Scenario: Parsing custom tags and rules validation step
- **WHEN** the AI test script contains: `Audit accessibility of page using tags wcag2aa`
- **THEN** `AccessibilityAction` parses it into an `Action` with type `ACCESSIBILITY`, target `page`, and value containing `tags=wcag2aa`

### Requirement: Playbook Script Audit Execution Modes (Fail vs Record)
The `AccessibilityAction` execution engine SHALL support different execution modes parsed from the natural language script step or properties:
1. **Default Fail Mode**: Throws an `AssertionError` if accessibility violations are found.
2. **Record-Only Mode**: Executes the audit and writes the detailed violation report to Allure/outcome log but does not fail the test run.
3. **Threshold Score Fail Mode**: Fails the test if the calculated score/number of violations exceeds a specified threshold.

#### Scenario: Execution in record-only mode
- **WHEN** the AI script step contains: `Verify accessibility of page and record only`
- **THEN** the execution engine parses `recordOnly=true`
- **AND** it executes the full Axe audit, records results to Allure and logs
- **AND** it does not throw an AssertionError even if violations are present

#### Scenario: Execution in threshold fail mode
- **WHEN** the AI script step contains: `Audit accessibility of page and fail if violations > 5`
- **THEN** the execution engine parses `maxViolations=5`
- **AND** it throws an AssertionError only if more than 5 distinct accessibility violations are detected
