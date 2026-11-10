# [DEF-20261002-01] Premature TokenBudgetExceededException on Completing Turns & Dead includeAncestors in query_dom

- **Status:** Resolved
- **Opened:** 2026-10-02
- **Closed:** 2026-10-02
- **Component:** `neodymium-core` (`AgentToolLoopStep`, `BrowserToolProvider`)
- **Scope:** `Framework`
- **Symptom:**
  In `CheckoutTest.live()`, step #2 ("Validate that United States as country is selected.") failed with `TokenBudgetExceededException` (104,443 tokens consumed vs 100,000 budget limit) at turn 11 right after the agent had successfully verified the country and issued `complete_step`. Additionally, the agent struggled over multiple turns to locate the country picker container because `query_dom`'s `includeAncestors` parameter was dead code.
- **Root Cause:**
  1. `AgentToolLoopStep` evaluated `stepCumulativeTokens > this.maxTokens` immediately upon receiving the LLM response without inspecting whether the response proposed completing the step (`complete_step`, `include`, or single-shot action). Even though turn 11 achieved the goal and issued `complete_step`, it was aborted before executing the completion tool call.
  2. In `BrowserToolProvider`, `query_dom` declared `includeAncestors` in its JSON schema, but never passed the parameter to the client-side JavaScript `queryScript` and never traversed ancestors in DOM query results.
- **Detection Gap ("What did we miss?"):**
  Unit tests in `AgentToolLoopStepTest` tested token budget exhaustion with an ongoing action (`token_action`) rather than testing a turn that proposes `complete_step` slightly above the budget. `BrowserToolsTest` verified basic `query_dom` results without testing the `includeAncestors` parameter.
- **Resolution:**
  1. In `AgentToolLoopStep`, deferred immediate token budget exceptions for turns proposing completion (`complete_step`, `include`, or single-shot action), granting a 20% grace limit (e.g. up to 120k for a 100k budget) to execute completion. If the step fails to complete or is rejected, the budget exception is enforced at the end of the turn before proceeding to any subsequent turns.
  2. In `BrowserToolProvider`, wired `includeAncestors` into `queryScript`, traversing up parent nodes up to `ancestorLevels` with deduplication and 500-char `outerHtml` output.
- **Safety Net Added:**
  Added unit tests in `AgentToolLoopStepTest`: `testStepTokenBudgetGraceAllowedForCompletingTurn`, `testStepTokenBudgetGraceExceededEvenIfProposesCompletion`, and `testStepTokenBudgetGraceEnforcedIfCompletingTurnFailsToComplete`. Added unit test in `BrowserToolsTest` verifying `includeAncestors: 1` hierarchy traversal.
