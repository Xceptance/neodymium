## 1. Setup and Resource Packaging

- [ ] 1.1 Add the minified `axe.min.js` script to `src/main/resources/js/axe.min.js`
- [ ] 1.2 Add the Axe configuration properties with default values to `src/main/resources/default-neodymium.properties` (e.g. `neodymium.axe.failOnViolations = true`, `neodymium.axe.tags = wcag2a wcag2aa`, `neodymium.axe.rules = `)

## 2. Configuration API

- [ ] 2.1 Update `com.xceptance.neodymium.util.NeodymiumConfiguration.java` to define keys and getter methods for Axe-core properties
- [ ] 2.2 Mark properties with `@Key` and default values using Owner framework annotations

## 3. Core Axe Execution Engine

- [ ] 3.1 Implement class `com.xceptance.neodymium.util.AxeOptions` to build execution contexts (includes/excludes), tags, and threshold limits (`maxViolations`, `recordOnly`)
- [ ] 3.2 Implement class `com.xceptance.neodymium.util.AxeUtils` with thread-safe cached classpath resource loading of `axe.min.js`
- [ ] 3.3 Implement the two-phase injection protocol in `AxeUtils` (checking `typeof axe !== 'undefined'` and injecting/executing the cached script)
- [ ] 3.4 Implement asynchronous `axe.run()` execution in browser context via `executeAsyncScript()` returning JSON results

## 4. AI Playbook Accessibility Action Plugin

- [ ] 4.1 Implement class `com.xceptance.neodymium.ai.action.plugins.AccessibilityAction` implementing `AiActionPlugin`
- [ ] 4.2 Add natural language direct instruction parsing using regular expressions in `AccessibilityAction.parseDirectInstruction`
- [ ] 4.3 Expose system prompt instructions in `AccessibilityAction.getPromptInstructions`
- [ ] 4.4 Register the `AccessibilityAction` plugin in `com.xceptance.neodymium.ai.action.ActionRegistry.java`

## 5. Reporting and Assertion Logic

- [ ] 5.1 Implement JSON parsing of Axe results using Gson in `AxeUtils` to extract violations list
- [ ] 5.2 Implement human-readable text formatter to compile a detailed list of violations (including element selectors, HTML snippets, failure reasons, and WCAG remediation links)
- [ ] 5.3 Implement Allure report attachment integration to automatically attach structured accessibility report logs
- [ ] 5.4 Implement execution mode evaluation in `AccessibilityAction` and `AxeUtils` supporting `recordOnly` (skip throwing error) and `maxViolations` (fail only if violations count exceeds limit)
- [ ] 5.5 Implement assertion logic to throw a clean `AssertionError` with the formatted violation logs when failures are present and threshold limits are exceeded

## 6. Testing and Verification

- [ ] 6.1 Create unit tests in `src/test/java/com/xceptance/neodymium/util/AxeUtilsTest.java` to verify classpath loading, option builder parsing, and configuration properties
- [ ] 6.2 Create unit tests in `src/test/java/com/xceptance/neodymium/ai/action/plugins/AccessibilityActionTest.java` to verify natural language regex parsing and action mapping
- [ ] 6.3 Create integration tests running on a sample local HTML file to verify dynamic injection, correct parsing of both passing and violating states, execution modes (fail vs. record only), and Allure reporting
