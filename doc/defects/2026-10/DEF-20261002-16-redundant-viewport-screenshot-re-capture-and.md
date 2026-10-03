# [DEF-20261002-16] Redundant Viewport Screenshot Re-Capture and Context Inflation on Read-Only Discovery Tools

- **Status:** Resolved
- **Opened:** 2026-10-02
- **Closed:** 2026-10-02
- **Component:** `neodymium-core` (`AgentToolLoopStep`)
- **Scope:** `Framework`
- **Symptom:** Multi-turn discovery steps (e.g. `query_dom`, `inspect`, `store`) in standard non-visual instructions consumed tens of thousands of redundant tokens (e.g. ~100k tokens in `CheckoutTest_live_tailwind-by-claude` Step #11, ~81k in Step #1, ~63k in Step #4), rapidly exhausting the test step token budget.
- **Root Cause:**
  1. `AgentToolLoopStep` unconditionally invoked `executor.captureState(ContextLevel.VISUAL, isFullPage)` after every turn and attached the resulting viewport screenshot to subsequent observation turns, even when only read-only discovery/inspection tools (`query_dom`, `inspect`, `request_context`, `store`) were executed.
  2. On multimodal models (Gemini), each unneeded PNG image attachment adds ~2,000–2,500 input tokens per turn, inflating cumulative context by 60k–100k tokens over 5–10 discovery turns on an unchanged page.
  3. `isMutatingTool()` omitted `"scroll"`, causing viewport repositioning actions to be misclassified as non-mutating.
- **Detection Gap ("What did we miss?"):** Existing multi-turn unit tests in `AgentToolLoopStepTest` only verified turn count and mock responses without asserting that non-visual discovery turns omit intermediate screenshot capture and attachments.
- **Resolution:**
  1. Added `"scroll"` to `AgentToolLoopStep.isMutatingTool()`, while ensuring `filterTools()` permits `scroll` for visual assertion repositioning.
  2. Tracked `turnHadMutatingAction` per turn in `executeStep()`.
  3. Gated intermediate `captureState(ContextLevel.VISUAL)` so that viewport screenshots are captured and attached only when the step is visual (`isVisualStep`) or when a mutating action actually executed in the turn (`turnHadMutatingAction`).
  4. Updated turn prompts to inform the model when visual state is unchanged: `"Note: Page visual state is unchanged after discovery/inspection tool execution (viewport screenshot omitted)."`.
- **Safety Net Added:** Added regression tests `testDiscoveryToolOmitsVisualStateCaptureAndAttachments`, `testMutatingActionRetainsVisualStateCaptureAndAttachments`, and `testScrollToolTreatedAsMutatingActionCapturesVisualState` in `AgentToolLoopStepTest.java`.
