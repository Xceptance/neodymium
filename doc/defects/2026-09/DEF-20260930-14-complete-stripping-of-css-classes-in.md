# [DEF-20260930-14] Complete Stripping of CSS Classes in Non-RICH Context Levels Due to Synthetic `autoId` Checked in `hasSemanticLocator`

- **Status:** Resolved
- **Opened:** 2026-09-30
- **Closed:** 2026-09-30
- **Component:** `neodymium-core` (`PageAnalyzer`)
- **Scope:** `Framework`
- **Symptom:** AI verification steps targeting structural containers without IDs or text (e.g. `Verify cart items section is displayed`) fail on Turn 1 because container CSS classes (such as `class="b-basket-content-items"`) are completely omitted from the simplified DOM, forcing the agent into expensive 9-turn discovery loops (`query_dom`, `inspect_element`) taking 22+ seconds.
- **Root Cause:** In commit `af748f3b`, `PageAnalyzer.formatElementNode` and `formatElement` introduced a token optimization to omit presentation classes when an element had a semantic locator (`hasSemanticLocator`). However, `autoId != null` was included in the condition. Because `PageAnalyzer` automatically assigns a synthetic `automationId` (`data-ai="xc..."`) to 100% of all extracted elements in the DOM tree, `autoId != null` evaluated to true for every single container and leaf element. Since `level.includesRichMetadata()` is false for `MINIMAL`, `LEAN`, and `STANDARD`, `appendSanitizedClassAttribute` was never called, stripping CSS classes entirely across all non-RICH context levels.
- **Detection Gap ("What did we miss?"):** `PageAnalyzerTest` asserted element tags (`<table`, `<tr`, `<td`, `<form`), interactive attributes (`data-testid`, `role`, `aria-label`, `href`, `data-ai`), and text strings (`"Company Brand Logo"`), but contained zero assertions validating the presence of `class` attributes on containers. Integration tests asserted overall step success without asserting turn-count efficiency (`turnCount == 1`).
- **Resolution:** Removed `autoId != null` from `hasSemanticLocator` in both `formatElementNode` (line 1237) and `formatElement` (line 1297) of `PageAnalyzer.java`, restoring CSS classes on all elements that lack semantic identifiers (`id`, `name`, `data-testid`, `role`, `aria-label`).
- **Safety Net Added:** Added unit regression test `testContainerClassPreservationWhenAutomationIdPresent` in `PageAnalyzerTest.java` verifying that containers with `automationId` present retain their `class` attribute in `STANDARD` and `LEAN` modes.
