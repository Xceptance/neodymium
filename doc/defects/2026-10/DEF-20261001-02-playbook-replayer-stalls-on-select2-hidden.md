# [DEF-20261001-02] Playbook Replayer Stalls on Select2 Hidden Select Elements and Unrestricted Full-DOM Self-Healing on Optional/Async Steps

- **Status:** Resolved
- **Opened:** 2026-10-01
- **Closed:** 2026-10-01
- **Component:** `neodymium-core` (`BrowserToolProvider`, `SelenideElementFinder`, `PlaybookToolReplayer`)
- **Scope:** `Framework`
- **Symptom:** Playbook execution in `REPLAY_STRICT` mode experiences multi-second stalling (up to 20 seconds per step) on interactive elements, specifically when selecting options in dropdowns styled with Select2 (e.g. `#radius` in `SpaLocatorTest`), checking optional elements that may not be present (e.g. dismissible cookie consent modals), or selecting asynchronous autocomplete popups.
- **Root Cause:**
  1. `BrowserToolProvider.createSelectTool` strictly enforced `el = findElement(selector).shouldBe(Condition.visible).shouldBe(Condition.enabled)`. Modern UI widget libraries (like Select2, Chosen, and accessible form styling) hide standard HTML `<select>` elements (`display: none` or `.select2-hidden-accessible` with 1x1 dimensions) behind a customized `<div>` wrapper while delegating option values to the underlying `<select>`. Enforcing Selenide `Condition.visible` forced `findElement` to exhaust full polling timeouts (3,000ms - 5,000ms) before failing or resorting to expensive DOM scans.
  2. `SelenideElementFinder.findDirect` discarded attached `<select>` elements if they were not visible in the light DOM, causing `isDirectlyPresent("#radius")` to return `false` despite the `<select>` being present and interactive in the DOM.
  3. `PlaybookToolReplayer.attemptHealing` triggered heavy full-DOM feature vector extractions (`extractFeatureVectors(driver)`) unconditionally on absent targets, even for optional steps (`step.isOptional()`) like cookie banners or promotional modals where element absence is expected and harmless.
  4. For live pages, `attemptHealing` triggered full-DOM scans immediately without brief async polling for standard CSS/ID locators that were in the process of rendering (e.g. Google Maps dropdowns).
- **Detection Gap ("What did we miss?"):** `BrowserToolProviderStabilityTest` tested `createSelectTool` only with mock `WebElement`s where `isDisplayed() == true`. No unit tests evaluated hidden or Select2-styled `<select>` elements where the `<select>` has class `select2-hidden-accessible` and `isDisplayed() == false`. Integration tests in Verla demo store used native HTML5 `<select>` elements rather than Select2 or Chosen widgets.
- **Resolution:**
  1. Relaxed `createSelectTool` to check `if (found.is(Condition.visible)) { el = found.shouldBe(Condition.enabled); } else { el = found.shouldBe(Condition.exist).shouldBe(Condition.enabled); }`.
  2. Enhanced `SelenideElementFinder.findDirect` to fall back to attached `<select>` elements or elements with class `select2-hidden-accessible` if no visible element was matched.
  3. Added a guard in `PlaybookToolReplayer.attemptHealing` to skip expensive full-DOM extraction if `step != null && step.isOptional()`.
  4. Added brief polling before triggering full-page feature extraction on live drivers for standard CSS/ID locators, while ensuring context-provided `liveCandidates` bypass polling for sub-millisecond offline execution.
- **Safety Net Added:** Added `testSelectToolAllowsHiddenSelectElementsWithSelect2` in `BrowserToolProviderStabilityTest.java` and verified offline sub-millisecond replay in `PlaybookToolReplayTest.java`.
