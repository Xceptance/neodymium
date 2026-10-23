# [DEF-20261023-1020] False-Negative Timeout in assert_element_state on Elements Below Viewport Fold

- **Status:** `Resolved`
- **Opened:** 2026-10-23 10:20
- **Closed:** 2026-10-23 10:25
- **Component:** `neodymium-core`
- **Scope:** `Framework & AI/Prompt`
- **Symptom:** During test execution (e.g. `PhotoGiftAddToCart.yaml` Step #14), `assert_element_state` fails with `Element not found {PlaywrightBy: button[data-testid="EDIT_NEXT"].findBy(visible)} Expected: visible (Timeout: 3s)`. The test immediately aborts in Turn 1 under Stop Criterion 2 ("Immediate fail on real defects") even though the element exists in the DOM and is functionally valid.
- **Root Cause:** In `BrowserToolProvider.createAssertElementStateTool()`, elements were resolved via `resolveLazyElement(selector)` and asserted directly against Selenide's `Condition.visible`. While action tools (`click`, `fill`) automatically invoke `SelenideElementFinder.scrollIntoViewIfNeeded`, assertion tools did not scroll candidate elements into view. When an element was positioned outside the viewport fold or clipped by scrollable parent containers, Selenium's `WebElement.isDisplayed()` evaluated to `false`, causing Selenide's 3,000ms polling condition to time out.
- **Detection Gap ("What did we miss?"):** `BrowserToolProviderStabilityTest` tested `assert_element_state` against compact fixture pages where all target elements fit inside the initial 1500x1000 viewport, never testing elements positioned below the viewport fold or inside scrollable containers.
- **Resolution:**
  1. Updated `BrowserToolProvider.createAssertElementStateTool()` to invoke `SelenideElementFinder.scrollIntoViewIfNeeded(el)` before evaluating visibility/interactability.
  2. For selectors matching multiple candidates, iterated and scrolled candidates into view if the primary candidate was not currently visible.
  3. In `case "visible"`, added fallback candidate scrolling before Selenide's `findBy(Condition.visible)` polling.
- **Safety Net Added:** Added `@Test` `BrowserToolProviderStabilityTest.testAssertElementStateAutoScrollsOffscreenElement` verifying that initially off-screen elements below the viewport fold are automatically scrolled and asserted visible and enabled without timing out.
