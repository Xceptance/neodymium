## 1. Context Routing & Registry

- [ ] 1.1 Update `AiAgent.java`'s `getInitialContextLevel` method to recognize case-insensitive `(usability)` and `[usability]` tags, routing them directly to `ContextLevel.VISUAL`.
- [ ] 1.2 Add unit tests for routing logic to verify that `(usability)` and `[usability]` tags successfully map to `ContextLevel.VISUAL`.
- [ ] 1.3 Create `UsabilityDiagnosticsAction` class under `com.xceptance.neodymium.ai.action` implementing the AI playbook action interface.
- [ ] 1.4 Register the `UsabilityDiagnosticsAction` in the AI action registry, parsing instructions with tags like `(usability)` or `[usability]`.

## 2. Dead-End Interactivity Scan Heuristic

- [ ] 2.1 Implement browser-side JS helper to identify elements with interactive visual indicators (e.g. `cursor: pointer` computed style, `btn`, `nav-link` classes).
- [ ] 2.2 Implement browser-side check to verify presence of action mechanisms (inline `onclick`, active non-empty `href`, keyboard focusability, parent interactive container).
- [ ] 2.3 Ensure the JavaScript engine returns a clean JSON array detailing selectors, coordinates, text, and outerHTML of all flagged dead-ends.

## 3. Text Readability Score Evaluator

- [ ] 3.1 Implement a compact, regex-based syllable count heuristic algorithm in JavaScript.
- [ ] 3.2 Implement Flesch-Kincaid Grade Level index calculator in JS evaluating average sentence length and syllables per word on selected text nodes.
- [ ] 3.3 Implement locale-safe fallback checks that evaluate simple sentence length and word-count thresholds when non-English locales are active.

## 4. Empty State Diagnostics Auditing

- [ ] 4.1 Implement browser-side scan of designated data grids, lists, or tables to detect when container child items/rows count is zero.
- [ ] 4.2 Implement heuristic to verify empty containers display a visible, non-empty user-friendly description or helper message.
- [ ] 4.3 Raise assertion failure detailing container selector and missing empty state warning when empty grids render completely blank.

## 5. Viewport Usability Health Auditing

- [ ] 5.1 Implement natural language step parsing for full viewport usability health audits (e.g. `Verify viewport health (usability)`).
- [ ] 5.2 Implement optimized JavaScript helper to detect horizontal scrollbars and scan the DOM to identify the exact overflowing visible element(s).
- [ ] 5.3 Implement JavaScript helper to scan visible text leaf nodes and detect when text dimensions exceed their container bounds (clipped text without ellipsis truncation).
- [ ] 5.4 Implement JavaScript helper to scan visible active interactive controls (buttons, links, inputs) and verify they are not rendered entirely off-screen.

## 6. Execution Engine & Test Reporting

- [ ] 6.1 Implement Single-Injection JavaScript execution model via WebDriver to run usability, readability, empty state, and viewport health heuristics in a single network turn.
- [ ] 6.2 Format detailed `AssertionError` exceptions detailing selectors, computed metrics, threshold differences, and recommended UX fixes.
- [ ] 6.3 Integrate usability checks execution and violation outcomes directly into Neodymium's Allure reports.

## 7. Integration and Verification Tests

- [ ] 7.1 Create automated test layouts to verify correct detection of dead-end click targets vs valid interactive controls.
- [ ] 7.2 Write automated tests to verify text readability checks for different target grade levels, including non-English fallback behaviors.
- [ ] 7.3 Write automated tests to verify empty state checks (success when warning exists, failure on completely blank grid container).
- [ ] 7.4 Write automated tests to verify viewport usability health checks (correct detection of horizontal scroll/overflow, clipped text content, and off-screen interactive elements).
