# [DEF-20260930-04] DOM Amnesia in Multi-Turn Verification and Incomplete Single-Turn Assertion Batching

- **Status:** Resolved
- **Opened:** 2026-09-30
- **Closed:** 2026-09-30
- **Component:** `neodymium-core` (`ai-pipeline`)
- **Scope:** `Framework`
- **Symptom:** In natural language verification steps (e.g. `Validate that United States as country is selected`), tests on inaccessible storefronts failed with `Stop Criterion 2: Assertion failure` due to hallucinated selectors (`img[alt*="United States"]`, `img[src*="us.svg"]`).
- **Root Cause:**
  1. In `AgentToolLoopStep.java:1131`, `pruneExpiredDomFromConversation(conversation)` unconditionally hardcoded `hasFreshDomIncoming = true`, wiping the Turn 1 DOM even when `requireDomForNextTurn` was `false`. Turn 2 was left with `[Initial page state omitted after Turn 1]`, completely blinding the model.
  2. Rule 4 in `AgentToolLoopStep` did not instruct the model to propose `[assertion, complete_step]` in the same turn for single verification checks, leading the LLM to execute partial container visibility checks in Turn 1 and attempt content verification in a blind Turn 2.
- **Detection Gap ("What did we miss?"):** Tests in `AgentToolLoopStepTest` only verified DOM pruning when mutating tools (`mock_click`) were executed, but did not test non-mutating assertion turns without incoming fresh DOM.
- **Resolution:**
  1. Updated `AgentToolLoopStep.java:1131` to preserve the latest DOM snapshot across turns when a non-mutating tool executes without fresh DOM incoming (`pruneExpiredDomFromConversation(conversation, requireDomForNextTurn || hasMutated)`).
  2. Added `[assertion, complete_step]` single-turn completion and direct assertion guidance to Rule 4 in `AgentToolLoopStep`.
- **Safety Net Added:** Added unit test `testTurn2PreservesDomWhenNonMutatingToolExecutedAndNoFreshDomIncoming` in `AgentToolLoopStepTest` verifying that Turn 2 retains the Turn 1 DOM when a non-mutating assertion executes without a fresh DOM snapshot incoming.
