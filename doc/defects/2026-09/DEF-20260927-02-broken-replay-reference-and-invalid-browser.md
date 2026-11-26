# [DEF-20260927-02] Broken Replay Reference and Invalid Browser Agent Dispatch in PrelinterChallengeIntegrationTest

- **Status:** Resolved
- **Opened:** 2026-09-27
- **Closed:** 2026-09-27
- **Component:** `neodymium-core` (`ai-testing` / `live-integration`)
- **Scope:** `Test/Harness`
- **Symptom:** `PrelinterChallengeIntegrationTest` fails when executed in full: Method 3 (`testPrelinterBypassedInReplayStrict`) fails with `FileNotFoundException` during runner setup because no recording JSON file exists; Method 2 (`testPrelinterDisabledBypassesExecution`) triggers live browser LLM agent loops against intentionally flawed challenge steps.
- **Root Cause:** Method 2 configured `@AiMode(ExecutionMode.LLM_ONLY)` on a class pointing to `PrelinterChallengeTest.yaml`. Because it was not `LINTER_ONLY`, `NeodymiumAiRunner` dispatched `AgentToolLoopStep` to execute defective steps in the browser, while `LLM_ONLY` omitted recording generation (`isRecording() == false`). Method 3 attempted `REPLAY_STRICT` from Method 2's non-existent recording, causing immediate failure.
- **Detection Gap ("What did we miss?"):** Previous manual test runs and verification scripts executed only `-Dtest=PrelinterChallengeIntegrationTest#testPrelinterEnabledChallengesAllRules`, leaving Methods 2 and 3 unverified in CI.
- **Resolution:** Retired the broken `PrelinterChallengeIntegrationTest` and its orphaned fixtures (`PrelinterChallengeTest.yaml`, `PrelinterChallengeTest/index.html`).
- **Safety Net Added:** Upfront linter configuration, toggles, replay bypass, and fail-on-findings behavior remain fully guarded by deterministic unit tests in `PlaybookLinterTest`, and all 9 quality rules remain verified in `ExecutionMode.LINTER_ONLY` by `PrelinterRuleMatrixLiveTest`.
