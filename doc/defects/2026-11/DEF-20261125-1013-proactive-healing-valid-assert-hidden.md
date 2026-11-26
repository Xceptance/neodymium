# [DEF-20261125-1013] Proactive Self-Healing Triggered on Valid `ASSERT_HIDDEN` Locators

- **Status:** `Resolved`
- **Opened:** 2026-11-25 09:36
- **Closed:** 2026-11-25 10:13
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:**
  During `REPLAY_WITH_HEALING`, `PlaybookToolReplayer.attemptHealing()` proactively triggers `DomFeatureVector` similarity healing on already-valid `ASSERT_HIDDEN` (`assert_element_state`) assertions even when the page DOM is completely unchanged. On `AssertIntegrationTest#testAssertHiddenState`, Step 2 rewrote `button#hidden-btn` to `#hidden-btn`, added ~1 s of wait and DOM-scan overhead, and failed `MetricsAsserter.wasNotHealed()` while marking the step `HEALED`.
- **Root Cause:**
  1. `PlaybookToolReplayer.attemptHealing()` checked `SelenideElementFinder.isDirectlyPresent(currentTarget)` both before and after the `400 ms` `DomQuiescenceWatcher.waitForDomQuiet()` pause without inspecting the `assert_element_state` target state.
  2. `SelenideElementFinder.isDirectlyPresent()` delegates to `findDirect()`, which requires the matched element to pass `findFirstVisible()` (returning `false` for `display:none` elements).
  3. Because `BrowserToolProvider` records a `DomFeatureVector` for hidden elements when `el.exists()` is `true` and `PageAnalyzer.extractFeatureVectors()` includes hidden elements, `attemptHealing()` fell through to a full-DOM scan, matched the same element via `LocatorCascadeResolver`, derived `#hidden-btn` via `resolveSelectorForCandidate()`, saw `"button#hidden-btn" != "#hidden-btn"`, and rewrote the working locator.
- **Detection Gap ("What did we miss?"):**
  Existing `assert_element_state` replay tests in `PlaybookToolReplayTest` and `BrowserToolsTest` only mocked `liveCandidates` without a live DOM or only exercised `visible`/`enabled` assertions; no test executed `PlaybookToolReplayer.replayStep()` under `REPLAY_WITH_HEALING` against an active DOM containing a legitimately hidden element (`display:none`).
- **Resolution:**
  1. Added non-blocking DOM attachment lookup `SelenideElementFinder.isDirectlyAttached(String)` (via parameterized `findDirect(String, boolean requireVisible)`) while preserving `isDirectlyPresent(String)` visibility semantics.
  2. Added `PlaybookToolReplayer.resolveAssertElementStates(JsonNode)` reusing `BrowserToolProvider.normalizeElementState(String)` and `negated`/`not` inversion, and updated `candidateMatchesAssertionExpectation()` to use it.
  3. Updated `PlaybookToolReplayer.attemptHealing()` for `assert_element_state` (`assert_element_state` / `browser_assert_element_state` / namespaced variants) to:
     - skip proactive healing immediately for pure `hidden` / `absent` assertions (preventing both false-positive healing on valid hidden locators and sibling misbinding when an element is unmounted);
     - use `isDirectlyAttached()` instead of `isDirectlyPresent()` for non-visibility-requiring state assertions (`exists`, `disabled`, `readonly`, `not_readonly`, `not_editable`, `checked`, `unchecked`, `selected`, `unselected`, `unfocused`, `empty`, `not_empty`, and compound states such as `["hidden", "disabled"]`);
     - retain `isDirectlyPresent()` for visibility-requiring states (`visible`, `editable`, `focused`).
- **Safety Net Added:**
  - Added unit tests in `PlaybookToolReplayTest`: `testAssertElementStateHiddenSkipsProactiveHealing`, `testAssertElementStateAbsentSkipsProactiveHealing`, `testAssertElementStateNegatedHiddenStillAllowsVisibleHealing`, `testAssertElementStateExistsAndCompoundHiddenDisabledStillHealWhenMissing`, and `testResolveAssertElementStatesNormalizationAndNegation`.
  - Added unit tests in `SelenideElementFinderTest`: `testIsDirectlyAttachedSafelyReturnsFalseOnNullBlankOrNoDriver` and `testIsDirectlyAttachedReturnsTrueForHiddenElementWhileIsDirectlyPresentReturnsFalse`.
  - Added headless Chrome replay regression tests in `BrowserToolsTest`: `testAssertElementStateHiddenDoesNotProactivelyHealOnValidHiddenElement`, `testAssertElementStateHiddenFailsConclusivelyWhenElementIsVisible`, and `testAssertElementStateExistsDoesNotProactivelyHealWhenHiddenElementIsAttached`.
