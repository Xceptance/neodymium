# [DEF-20261009-2340] Assert Text Stale Collection Snapshot Timeout & Selenide Screenshot Storm

- **Status:** `Resolved`
- **Opened:** 2026-10-09 23:40
- **Closed:** 2026-10-09 23:42
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** During test replay of `org.neodymium.ai.integration.verla.cart.judge.CartTest.replayBad` (dataset `bad`), Step 7 (`assert_text` on selector `.cart-sidebar` looking for `"Discount (10P-OFF)"`) failed with `Assertion failed during replay: Expected text/pattern "Discount (10P-OFF)" was not found on selector ".cart-sidebar" within 3000ms.` The test hung for ~24 seconds while Selenide dumped 8 successive screenshot attachments (`*.0` through `*.7`), even though the discount item was visibly present on the rendered cart sidebar.
- **Root Cause:**
  1. `BrowserToolProvider.assert_text` invoked `findElements(selector)`, which returns an `ElementsCollection`. The polling loop iterated over this collection using `for (final SelenideElement el : elements)`. In Selenide, iterating an `ElementsCollection` produces fixed `CollectionSnapshot` proxies holding raw `WebElement` references.
  2. When the promo code form was submitted, HTMX asynchronously swapped `#cart-content-wrapper` via `outerHTML`, detaching the old `.cart-sidebar` DOM element.
  3. `matchesElementText` unconditionally evaluated 8 distinct element properties (`getText()`, `getValue()`, `placeholder`, `aria-label`, `data-parent-text`, `innerText`, `getTagName()`, `textContent`) before comparing matches. On a detached `CollectionSnapshot` proxy, each attribute invocation triggered Selenide's internal `dispatchAndRetry` loop for 3000ms and produced a failure screenshot attachment before failing with `ElementNotFound`.
  4. `isStaleException` only checked for `StaleElementReferenceException` and missed Selenide's `ElementNotFound` (which wraps stale references or element disappearance). Consequently, the catch block swallowed `ElementNotFound` and continued to the next attribute, burning 8 * 3000ms = 24,000ms and consuming the entire retry budget before the outer loop could re-query the DOM.
- **Detection Gap ("What did we miss?"):**
  1. Existing unit tests (`BrowserToolsTest`) evaluated `matchesElementText` with static proxy mocks where `getText()` and `getAttribute()` return immediately without testing dynamic DOM replacements or Selenide `CollectionSnapshot` retry behaviors.
  2. Fast E2E tests using static HTML fixtures did not trigger asynchronous HTMX `outerHTML` element replacement during an active `assert_text` assertion.
- **Resolution:**
  1. Updated `BrowserToolProvider.assert_text` to poll direct `WebElement` references via `WebDriverRunner.getWebDriver().findElements(safeResolveLocator(selector))`. Calling methods on raw `WebElement` instances raises `StaleElementReferenceException` instantly (<1ms) upon an HTMX swap without triggering Selenide's 3000ms retry or screenshot attachments.
  2. Extracted `matchesCandidate` and refactored `matchesElementText` to short-circuit: if `el.getText()` matches, it returns `true` immediately without executing 7 redundant CDP calls.
  3. If a stale exception occurs on any property check, `matchesElementText` immediately aborts and rethrows the stale exception instead of continuing to inspect a dead element.
  4. Updated `isStaleException` to recognize `ElementNotFound` and `"element not found"` messages alongside `StaleElementReferenceException`.
  5. Unified locator resolution across `resolveLazyElement` and `findElements` via `safeResolveLocator`.
- **Safety Net Added:**
  1. `BrowserToolsTest.testMatchesElementTextWithInnerTextFallback` and `BrowserToolsTest.testMatchesElementTextMultiSpaceNormalization` verify short-circuiting and candidate matching.
  2. E2E regression validated end-to-end via `org.neodymium.ai.integration.verla.cart.judge.CartTest#replayBad` covering Step 7 promo code HTMX swap and assertion.
