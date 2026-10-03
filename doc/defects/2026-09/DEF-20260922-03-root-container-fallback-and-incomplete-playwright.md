# [DEF-20260922-03] Root Container Fallback and Incomplete Playwright Selector Support Causing False Element Matching on Hidden Elements

- **Status:** Resolved
- **Opened:** 2026-09-22
- **Closed:** 2026-09-22
- **Component:** `neodymium-core` (`LocatorResolver`, `QualityJudgeToolInterceptor`, `SelenideElementFinder`, `BrowserToolProvider`)
- **Scope:** `Framework`
- **Symptom:** In `AssertIntegrationTest.testAssertVisibility`, asserting that a hidden element is absent (`Assert that the hidden 'Secret Button' is absent`) failed. The selector resolved to `<body>` instead of the absent/hidden element, or failed on `assert_text(negated=true)` with `Expected text/pattern "Secret Button" was still present anywhere on the page within 3000ms.`.
- **Root Cause:**
  1. `QualityJudgeToolInterceptor` called `driver.findElements(By.cssSelector(selector))` directly, throwing `InvalidSelectorException` when Playwright selectors (e.g. `text=Secret Button`, `role=...`, or chained `>>`) were used, silently bypassing quality deliberation.
  2. `SelenideElementFinder` duplicated pseudo-resolution with an ad-hoc XPath expression `contains(normalize-space(.), '...')`. In XPath, `.` matches the string-value of all descendants, matching `<html>` and `<body>`.
  3. When the targeted button was hidden (`display: none`), `findFirstVisible` filtered it out, and Chromium's `visibleEls.find(Condition.focused)` defaulted to `<body>` (since `document.activeElement` is `document.body` when no element has focus), returning `<body>` as the matched element.
  4. In `BrowserToolProvider.matchesElementText`, `el.getAttribute("textContent")` was evaluated unconditionally on `$("body")`, dumping all text in the entire DOM (including hidden `display: none` elements). When the LLM attempted `assert_text(expectedText="Secret Button", negated=true)`, `isTextPresentOnPage` always reported the text present.
- **Detection Gap ("What did we miss?"):**
  1. `LocatorResolver` lacked browserless unit test coverage for Playwright selector extensions (`role=`, chained `>>`, attribute shorthands, test IDs).
  2. Element finding tests focused on finding present and visible elements; negative assertions on hidden/absent elements were not guarded against root container (`html`/`body`) fallback.
  3. Page-level text presence assertions lacked tests verifying that text inside `display: none` elements is correctly recognized as not present on the rendered page.
- **Resolution:**
  1. Expanded `LocatorResolver` into a comprehensive translation layer supporting all Playwright selectors (`role=`, attribute shorthands, test IDs, exact vs substring text matching, chained `>>` combinators, Shadow DOM).
  2. Excluded root containers (`html`, `body`, `head`) from wildcard text match expressions in `LocatorResolver.buildPseudoSelectorXpath`.
  3. Updated `QualityJudgeToolInterceptor` to query live DOM using `LocatorResolver.resolveLocator(selector)`.
  4. Streamlined `SelenideElementFinder.findDirect` to delegate directly to `LocatorResolver`, excised flawed `tryResolvePlaywrightPseudo`, and updated `findFirstVisible` to filter out `html` and `body` unless explicitly requested.
  5. In `BrowserToolProvider`, guarded `matchesElementText` so `textContent` is never evaluated on root containers (`body`/`html`), and filtered for visible inputs in `isTextPresentOnPage`.
- **Safety Net Added:**
  - Added unit test suite `LocatorResolverTest` (10 tests) verifying locator resolution without requiring a browser.
  - End-to-end multi-cycle regression test in `AssertIntegrationTest#testAssertVisibility` covering live recording and strict/healing replay cycles.
