# [DEF-20261004-1425] Assertion Tool Healing Overwrites Test Oracle and Masks Functional Regressions

- **Status:** `Resolved`
- **Opened:** 2026-10-04 14:25
- **Closed:** 2026-10-04 14:46
- **Component:** `neodymium-core`
- **Scope:** `Framework & AI/Prompt`
- **Symptom:** When assertion tools (`assert_text`, `assert_attribute`, `assert_element_state`) or assertion steps failed during replay (such as when SUT data changed or a price regression occurred), the replay pipeline either:
  1. Permitted vector-based locator healing to bind to candidate elements whose actual values differed from the assertion expectation, or
  2. Threw `HealingRequiredException` during `REPLAY_WITH_HEALING`, escalating the failed assertion to `AgentToolLoopStep`. The LLM agent loop then re-inspected the DOM, called tools, and rewrote the playbook with the new incorrect values, masking genuine SUT functional/data bugs and corrupting test oracles.
- **Root Cause:**
  1. In `PlaybookToolReplayer.attemptHealing`, Tier-2 similarity healing was invoked uniformly across all tool types, including assertion tools. Candidate selection evaluated structural/CSS/tag similarity without verifying that candidate attributes or text satisfied the expected assertion value.
  2. In `ExecuteActionsStep`, all step execution failures under `mode.supportsHealing()` threw `HealingRequiredException` without distinguishing action steps (e.g. `click`, `type`) from assertion/verification steps (`isAssertionStep()`). This triggered online LLM healing, allowing the LLM to inspect the changed page and overwrite assertion expectations.
- **Detection Gap ("What did we miss?"):** Existing replay healing tests validated that action steps (such as `click` on moved buttons or links) successfully healed locators and escalated to LLM when needed, but lacked negative tests asserting that assertion tools must reject healing and fail conclusively when SUT data mismatches the test oracle.
- **Resolution:**
  1. Added `PlaybookStep.isAssertionStep()` and `PlaybookStep.isAssertionToolName(String)` to identify verification steps and assertion tools.
  2. Updated `PlaybookToolReplayer.attemptHealing` to strictly validate candidate elements against expected assertion criteria via `candidateMatchesAssertionExpectation` (`assert_text`, `assert_attribute`, `assert_element_state`). If a candidate does not satisfy the expected text, attribute, or state, locator healing is rejected.
  3. Bypassed locator healing entirely for page-level assertion tools (`assert_count`, `assert_url`, `assert_title`).
  4. Updated `PlaybookToolReplayer.replayStep` catch blocks to wrap assertion tool/step failures in `ConclusiveFailureException`.
  5. Updated `ExecuteActionsStep` to immediately throw `ConclusiveFailureException` when `step.isAssertionStep()` fails, completely preventing escalation to `AgentToolLoopStep` or LLM invocation.
- **Safety Net Added:** Added permanent automated regression tests:
  1. `org.neodymium.ai.replay.PlaybookToolReplayTest`:
     - `testAssertionLocatorHealsWhenElementTextMatchesExpected`: verifies moving locator heals when live candidate text matches expected value.
     - `testAssertionLocatorHealingRejectedWhenElementTextDoesNotMatchExpected`: verifies healing is rejected when candidate text mismatches expected value, failing conclusively with `ConclusiveFailureException`.
     - `testPageLevelAssertionBypassesLocatorHealing`: verifies page-level assertions (`assert_count`) bypass element locator healing.
  2. `org.neodymium.ai.pipeline.steps.ExecuteActionsStepTest`:
     - `testReplayWithHealingFailsConclusivelyOnAssertionFailureWithoutEscalatingToAgentLoop`: verifies failed assertion step throws `ConclusiveFailureException` with 0 LLM calls in `REPLAY_WITH_HEALING` mode.
     - `testReplayWithHealingEscalatesActionFailureToAgentLoop`: verifies failing action step (`click`) still properly escalates to online LLM healing.
