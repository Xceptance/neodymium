# [DEF-20260930-07] Unpruned DOM in Multi-Turn Verification Observation Causing Token Doubling and Visual Call Bloat

- **Status:** Resolved
- **Opened:** 2026-09-30
- **Closed:** 2026-09-30
- **Component:** `neodymium-core` (`ai-pipeline`)
- **Scope:** `Framework`
- **Symptom:** In execution reports, non-mutating verification steps that required 2 turns (Turn 1 executing `assert_text` or `assert_element_state` and Turn 2 executing `complete_step`) consumed 19,000–35,000 input tokens on Turn 2 instead of ~2,000–5,000 tokens, doubling the token consumption of every verification step.
- **Root Cause:** In `AgentToolLoopStep.java:1132`, `pruneExpiredDomFromConversation` was guarded by `hasFreshDomIncoming = (requireDomForNextTurn || hasMutated)`. When an assertion succeeded without remaining milestones, `hasMutated == false` and `requireDomForNextTurn == false`, causing `pruneExpiredDomFromConversation` to preserve the entire 18,000–34,000 token Turn 1 DOM in conversation history alongside the viewport screenshot attached for Turn 2 visual observation.
- **Detection Gap ("What did we miss?"):** [DEF-20260930-04] prevented Turn 1 DOM pruning when `hasFreshDomIncoming == false` to avoid multi-turn selector amnesia, and unit test `testTurn2PreservesDomWhenNonMutatingToolExecutedAndNoFreshDomIncoming` explicitly enforced retaining the DOM without verifying token efficiency or supporting on-demand DOM queries (`query_dom` / `request_context`).
- **Resolution:**
  1. Updated `AgentToolLoopStep.java` to unconditionally prune expired DOM snapshots from prior turns before entering visual observation turns, reducing Turn 1 user message content to `[Initial page state omitted after Turn 1 — use browser tools for current page state]`.
  2. Updated the Turn 2 observation prompt and System Prompt rules to explicitly notify the agent that prior DOM snapshots are omitted to minimize context, and instructed it to call `request_context` (for full fresh DOM) or `query_dom` (for specific elements) if DOM targeting is needed.
- **Safety Net Added:** Updated `AgentToolLoopStepTest.java` (`testTurn2PrunesDomOnVisualObservationTurnAndAllowsContextRequest`) asserting that Turn 2 prunes the Turn 1 DOM on visual observation turns, and added unit test `testTurn2ContextEscalationViaRequestContext` verifying that calling `request_context` in Turn 2 delivers a fresh DOM snapshot in Turn 3.
