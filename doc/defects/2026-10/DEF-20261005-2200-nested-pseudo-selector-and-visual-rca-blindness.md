# [DEF-20261005-2200] Nested Pseudo-Selector Translation Failure and Visual RCA Blindness

- **Status:** `Resolved`
- **Opened:** 2026-10-05 21:17
- **Closed:** 2026-10-05 22:05
- **Component:** `neodymium-core`
- **Scope:** `Framework & AI/Prompt`
- **Symptom:** During automated checkout test execution (`CheckoutTest_live_tailwind-by-claude_20261005-203623.html`), step `assert_text({"expectedText":"$31.98", "selector":"div:has(> h3:text-is(\"Order Summary\"))"})` failed with element not found even though `$31.98` was visually displayed on the page twice within the Order Summary card. Subsequently, the automatic Visual RCA step generated an entirely confabulated diagnostic report claiming the cart contained "2 units of 'Classic Crewneck Tee' with Subtotal '$59.98'"—a completely fictitious cart state contradictory to the true page state.
- **Root Cause:**
  Two distinct, cascading bugs in `neodymium-core` caused the verification failure and the subsequent hallucinatory RCA:
  1. **Nested Extended Pseudo Extraction & Combinator Handling in `LocatorResolver`:**
     In `LocatorResolver.buildSegmentPredicate`, regex scanning for text pseudo-classes (`:text-is(...)`, `:has-text(...)`, `:exact-text(...)`, etc.) via `extractBalancedPseudoArgs` did not track parenthesis nesting depth (`parenDepth == 0`). When processing `div:has(> h3:text-is("Order Summary"))`, it matched `:text-is("Order Summary")` embedded within `:has(...)`, stripped it from `:has(...)`, and added `(normalize-space(.)='Order Summary' or normalize-space(text())='Order Summary')` as a condition on the outer `div` itself. Because the outer `div` contained the whole order summary card rather than just the title, the locator failed. In addition, `:has(...)` translation did not support the child combinator (`>`), and did not strip `:has` or `:not` before attribute/class matchers evaluated `cleanedRest`, causing inner classes and IDs to leak to outer element predicates.
  2. **Visual RCA Blindness in `StateMachineRunner`:**
     In `StateMachineRunner.runVisualRca`, line 859 invoked `executor.captureState()` using the default no-argument method, which captures `ContextLevel.LEAN` (DOM snapshot only, screenshot capture disabled). Because `state.getAttachments()` was null or empty, no image attachments were passed to `LlmRequest`. The runner dispatched the request to the `VISION` provider capability anyway. Forced to evaluate an instruction demanding visual analysis of a non-existent screenshot, the multimodal LLM hallucinated the DOM and visual cart state.
- **Detection Gap ("What did we miss?"):**
  1. `LocatorResolverTest` verified top-level extended pseudo-classes (e.g., `div:text-is("...")`) and simple `:has(td)` tags, but never tested nested combinations where an extended pseudo-class or child combinator was embedded inside `:has(...)`.
  2. Existing tests for `StateMachineRunner` did not verify that `ContextLevel.VISUAL_RICH` is captured when Visual RCA executes on step failure.
- **Resolution:**
  1. Updated `extractBalancedPseudoArgs` and `stripBalancedPseudo` in `LocatorResolver` to only match at top-level depth (`parenDepth == 0 && bracketDepth == 0 && !inQuote`), preserving nested pseudo-classes inside `:has(...)` and `:not(...)`.
  2. Enhanced `:has(...)` processing in `LocatorResolver` to detect leading child combinator `>` and map to `child::` axis instead of `descendant::`, supported by quote- and bracket-aware `splitSelectorList`.
  3. Stripped `:has` and `:not` from `cleanedRest` prior to regex matching for attributes, classes, and IDs, preventing inner selector leakage.
  4. Updated `toXPathSegment` to support leading `>` child combinators.
  5. In `StateMachineRunner.runVisualRca`, changed state capture to `executor.captureState(ContextLevel.VISUAL_RICH)` so failure screenshots are preserved.
  6. Filtered image attachments before creating `LlmRequest`, and selected `LlmCapability.VISION` only if image attachments exist, falling back to `LlmCapability.TEXT_ONLY` otherwise to eliminate vision confabulation.
- **Safety Net Added:**
  1. `LocatorResolverTest.testHasWithChildCombinatorAndTextIs`
  2. `LocatorResolverTest.testHasAndNotDoNotLeakInnerClassesOrIdsToOuterElement`
  3. `LocatorResolverTest.testHasWithMultipleCommaSeparatedSelectorsWithAttributes`
  4. `StateMachineRunnerTest.testStateMachineRunnerCapturesVisualRichOnFailureForVisualRca`
