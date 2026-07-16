## 1. Investigation & Preparation

- [x] 1.1 Locate the DOM element serialization code (likely a JavaScript snippet injected into the page or a Java component parsing the DOM).

## 2. Core Implementation

- [x] 2.1 Update the DOM element serialization logic to explicitly check for and extract the `value` attribute/property ONLY for non user-editable input elements (like `<input type="button">`, `<input type="submit">`, `<input type="hidden">`, etc.).
- [x] 2.2 Ensure the extracted `value` is included in the attributes map sent to the LLM context alongside other standard attributes.

## 3. Testing & Verification

- [x] 3.1 Run existing tests to ensure no regressions in serialization behavior.
- [x] 3.2 Add or update tests to verify that `value` attributes are correctly serialized for non-editable input elements, and ensure they are STILL OMITTED for editable inputs (like `type="text"` or `textarea`).
