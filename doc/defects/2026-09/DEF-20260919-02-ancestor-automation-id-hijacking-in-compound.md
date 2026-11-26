# [DEF-20260919-02] Ancestor Automation ID Hijacking in Compound Selectors and Missing Sub-Step Activities/Screenshots in Replay

- **Status:** Resolved
- **Opened:** 2026-09-19
- **Closed:** 2026-09-19
- **Component:** `neodymium-core` (`SelenideElementFinder`, `ClickAction`, `ExecuteActionsStep`, `PreliminaryReportListener`, `PlaybookToolReplayer`)
- **Scope:** `Framework`
- **Symptom:** In `CartTest.replayNormal`, Step #3 fails at substep 3.5 with `Expected text/pattern "CART 1" was not found on selector "#cart-btn-anchor" nor anywhere on the page within 3000ms`. Cart badge remains 0 because the size button in the dynamic quick-add dropdown was never clicked. Furthermore, in the HTML report, all sub-steps of the compound step displayed 0 activities and 0 screenshots.
- **Root Cause:**
  1. **Selector Hijacking:** In `SelenideElementFinder.tryResolveAutomationId`, regex matching extracted the first automation ID token in the selector (`xcboo7um`, belonging to the ancestor `<article>`). When the un-stamped dynamic size button failed to match, line 724 queried `[data-ai='xcboo7um']`, returned the visible `<article>`, and bypassed `PageAnalyzer.captureSimplifiedDom`. The replayer clicked the product card container instead of the size button.
  2. **Monolithic Replay Execution:** In `ExecuteActionsStep`, `isLegacySubStepReplay` was guarded by `!parentHasToolCalls`. Because the parent step had recorded tool calls, compound steps were executed as a single monolithic block in replay mode. Sub-steps were never scheduled in the pipeline, so no sub-step lifecycle events (`StepStartedEvent`, `StepFinishedEvent`), sub-step screenshots, or sub-step actions were recorded.
  3. **Missing Sub-Step Action Population:** `PreliminaryReportListener` failed to transfer `childStep.getActions()` into `childEntry` when populating sub-steps.
- **Detection Gap ("What did we miss?"):** Prior unit tests validated compound turn groups in live mode with static instructions, but did not test sequential sub-step unrolling during replay mode, nor did they verify that `ReportStepEntry` sub-steps received their corresponding actions and screenshots.
- **Resolution:**
  1. Updated `SelenideElementFinder.tryResolveAutomationId` to transform all `#xc...` tokens, prioritize `PageAnalyzer.captureSimplifiedDom` on missing elements, and strictly forbid bare `[data-ai='neoId']` fallbacks on compound selectors.
  2. Added `element.shouldBe(Condition.visible)` in `ClickAction` before clicking to ensure dynamic elements are ready for interaction.
  3. Updated `ExecuteActionsStep` to schedule sub-steps in sequence during replay mode (`executionMode.isReplay()`), capturing individual sub-step screenshots, statuses, and durations.
  4. Updated `PlaybookToolReplayer` to dispatch `ActionExecutedEvent` during replay.
  5. Updated `PreliminaryReportListener` to copy actions and screenshots to sub-step report entries.
- **Safety Net Added:** Added regression tests in `SelenideElementFinderTest`, `ExecuteActionsStepTest`, and `PreliminaryReportListenerTest`, and verified `CartTest.replayNormal` passes end-to-end with full sub-step activities and screenshots in the report.
