# [DEF-20261002-15] Visual Assertion Step Derailment via DOM Context Escalation and Runaway Scrolling

- **Status:** Resolved
- **Opened:** 2026-10-02
- **Closed:** 2026-10-02
- **Component:** `neodymium-core` (`AgentToolLoopStep`)
- **Scope:** `Framework`
- **Symptom:** In live test runs with visual verification steps (e.g. `SearchGermanTest_live_DE` Step 19), an instruction marked `(visual)` executed 9 scroll actions and invoked `request_context({"level": "STANDARD"})`, escalating context to DOM. Prompt tokens spiked from ~5,500 to >21,000 per turn, burning 115,804 tokens and failing with `TokenBudgetExceededException`.
- **Root Cause:**
  1. `AgentToolLoopStep.filterTools()` omitted mutating tools and DOM assertion tools for pure visual assertions (`isVisualAssertion && !hasInteractive`), but left `request_context` in `availableTools`.
  2. The target element (*Angebot* filter) was cut off below the viewport fold and pinned inside a CSS `position: sticky; top: 100px;` sidebar. Window scrolling (`window.scrollBy`) moved the page content but left the sticky sidebar pinned and cut off in the viewport.
  3. Unable to reveal the element via scrolling, the agent invoked `request_context(level="STANDARD")`, injecting the full DOM tree into a visual verification turn in violation of Rule 5 ("Do not query DOM for visual checks").
  4. Consecutive identical call detection was bypassed by varying `yOffset` on each turn (`300`, `250`, `200`, `350`), permitting runaway scrolling loops.
- **Detection Gap ("What did we miss?"):** `AgentToolLoopStepTest.testVisualAssertionFiltersDomAndMutatingTools` explicitly asserted that `request_context` was present in visual step tools without checking whether requesting DOM levels was prohibited, and no test bounded consecutive scrolling in visual steps.
- **Resolution:**
  1. Excluded `request_context` from `filterTools()` when `isVisualAssertion && !hasInteractive`.
  2. Added a defensive guard against non-screenshot context level escalation via tool requests in pure visual steps.
  3. Clarified the `Visual Inspection Directive` and Rule 5 so the agent concludes the visual evaluation instead of attempting DOM recovery.
  4. Capped consecutive `scroll` interactions during visual assertion steps to 3 attempts, warning at attempt 3 and failing with `ConclusiveFailureException` at attempt 4.
- **Safety Net Added:** Updated `AgentToolLoopStepTest.testVisualAssertionFiltersDomAndMutatingTools` to assert `request_context` is excluded from visual assertion tools, and added regression test `testVisualAssertionExcessiveScrollingTerminatesConclusively`.
