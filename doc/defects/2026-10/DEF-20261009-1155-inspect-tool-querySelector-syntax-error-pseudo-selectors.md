# [DEF-20261009-1155] `inspect` and `inspect_visual` browser tools throw SyntaxError on flexible selectors due to direct `document.querySelector`

- **Status:** `Resolved`
- **Opened:** 2026-10-09 11:55
- **Closed:** 2026-10-09 14:32
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In live test executions (e.g., `CartTest_liveAllDataSets_normal_20261009-110425.html`), when an agent executed `inspect(selector: "article.product-card:has-text(\"Modern Sage Sweaters\")")`, the browser threw `JavascriptException: javascript error: SyntaxError: Failed to execute 'querySelector' on 'Document': 'article.product-card:has-text("Modern Sage Sweaters")' is not a valid selector`. This unexpected exception triggered an automatic escalation to `VISION` context level, which stripped DOM snapshots from subsequent turns and caused the vision model to hallucinate selectors (`#cart-count` instead of `#cart-btn-anchor`), leading to timeout failure on Stop Criterion 2.
- **Root Cause:** In `BrowserToolProvider.java`, `createInspectTool()`, `createInspectVisualTool()`, and `resolveViewportCoordinates()` passed the raw caller-provided selector directly to native browser `document.querySelector(arguments[0])`. Standard W3C `document.querySelector` only accepts strict CSS syntax and throws a `DOMException / SyntaxError` when encountering Neodymium flexible selectors (`:has-text(...)`, `:text-is(...)`, `:contains(...)`), Playwright chained locators (`>> nth=N`), or XPath. While interaction actions (such as `CLICK`, `TYPE`, `ASSERT`) resolve targets through `SelenideElementFinder.findElement(...)`, inspection tools bypassed the framework locator pipeline.
- **Detection Gap ("What did we miss?"):** Existing browser tool stability unit tests exercised `assert_text`, `click`, and `type`, but did not assert execution of `inspect` or `inspect_visual` with non-CSS pseudo-classes (`:has-text(...)`).
- **Resolution:**
  1. Updated `createInspectTool()` in `BrowserToolProvider.java` to resolve the selector via `findElement(selector)`. Upon existence verification, `el.getWrappedElement()` is passed directly to the JavaScript closure as `arguments[0]`, completely eliminating `document.querySelector(sel)`.
  2. Updated `createInspectVisualTool()` in `BrowserToolProvider.java` to resolve the target selector via `findElement(selector)` and pass the live `WebElement` to the bounding box extraction script.
  3. Updated `resolveViewportCoordinates()` in `BrowserToolProvider.java` to locate the target via `findElement(selector)` and compute bounding box offsets from the resolved `WebElement`.
  4. Updated `createScrollTool()` container scroll resolution in `BrowserToolProvider.java` to look up container elements via `findElement(targetContainer)` instead of `document.querySelector(target)`.
- **Safety Net Added:** Added automated unit regression tests to `BrowserToolProviderStabilityTest.java`:
  - `testInspectToolResolvesFlexibleSelectorAndPassesElementToJavaScript()`: Verifies that `:has-text(...)` pseudo-selectors resolve to a live `WebElement` passed into the JavaScript execution context without native syntax errors.
  - `testInspectToolReturnsErrorWhenElementNotFound()`: Verifies proper error reporting when an inspection target cannot be located.
  - `testInspectVisualToolResolvesFlexibleSelectorAndCapturesBoundingBox()`: Verifies that `inspect_visual` resolves flexible pseudo-selectors and captures bounding box crops.
