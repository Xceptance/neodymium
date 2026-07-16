## 1. Clarification & Review

- [x] 1.1 Review the open questions in `design.md` to decide between formatting options as a quoted JSON array inside an attribute or extracting them as separate indented `<option>` elements.

## 2. Core Implementation

- [x] 2.1 Update `PageAnalyzer.java` to extract `<option>` elements losslessly, preserving special spaces like non-breaking spaces (`nbsp`).
- [x] 2.2 Adjust formatting logic in `PageAnalyzer` to render the options according to the chosen approach (attribute string vs. child elements).

## 3. Testing & Verification

- [x] 3.1 Update related DOM checking tests in `PageAnalyzerCheckableTest.java` to match the new output format for `<select>`.
- [x] 3.2 Update AI test pages / `AuraGlanceTest` cases that rely on specific select formatting, if affected.
