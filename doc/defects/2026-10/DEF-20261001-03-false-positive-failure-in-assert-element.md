# [DEF-20261001-03] False-Positive Failure in `assert_element_state` on Multi-Candidate Selectors with Inactive Leading Elements (e.g. Slick Carousels)

- **Status:** Resolved
- **Opened:** 2026-10-01
- **Closed:** 2026-10-01
- **Component:** `neodymium-core` (`BrowserToolProvider`)
- **Scope:** `Framework`
- **Symptom:** AI validation step asserting visibility of elements using class selectors (e.g. `.c-product-recommendations .product-tile`) failed with `Element should be visible ... Actual value: hidden` even though multiple matching elements were prominently displayed on the page.
- **Root Cause:** `BrowserToolProvider.createAssertElementStateTool()` resolved targets via `resolveLazyElement(selector)` which maps strictly to `$(selector)` (the first element in DOM order). In carousel components such as Slick (`slick-slider`), element 0 (`#slick-slide00`) is often an inactive or cloned slide with `display: none` (`displayed:false`), while subsequent elements (`#slick-slide01` etc.) are actively displayed. Unlike `assert_text` which iterates over matching candidates, `assert_element_state` tested only element 0 without checking whether other candidates matched the asserted state.
- **Detection Gap ("What did we miss?"):** `BrowserToolProviderStabilityTest` tested `assert_element_state` only on single-element locators (`#readonly-input`), never on selectors matching multiple elements where index 0 is hidden or inactive.
- **Resolution:**
  1. Updated `BrowserToolProvider.createAssertElementStateTool()`: when the initial candidate is not visible, check `findElements(selector).filter(Condition.visible)` and reassign `el` to the visible candidate before assertion, or fall back to collection polling via `findBy(Condition.visible)` in `case "visible"`.
  2. For negative assertions (`state: "hidden"` / `absent`), verify that no candidate matching the selector is currently visible before passing.
  3. Added `"displayed"` normalization to `"visible"` in `BrowserToolProvider.normalizeElementState()`.
- **Safety Net Added:** Added `testAssertElementStateMultiCandidateVisibilityFallback` and `normalizeElementState` assertions in `BrowserToolProviderStabilityTest.java`.
