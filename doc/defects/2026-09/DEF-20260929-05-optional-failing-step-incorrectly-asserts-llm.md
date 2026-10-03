# [DEF-20260929-05] Optional Failing Step Incorrectly Asserts LLM Invocations in Healing Replay Mode

- **Status:** Resolved
- **Opened:** 2026-09-29
- **Closed:** 2026-09-29
- **Component:** `neodymium-core` (`ai-testing` / `live-integration` / `OptionalIntegrationTest`)
- **Scope:** `Test/Harness`
- **Symptom:** Running `OptionalIntegrationTest.testOptionalFailingStepBypassed` failed in `REPLAY_WITH_HEALING` mode with `org.opentest4j.AssertionFailedError: Expected at least 1 LLM call, but 0 calls were made.`
- **Root Cause:** In `OptionalIntegrationTest.java` line 81, the metrics assertion `.onHealing(m -> m.hasLlmCalls())` expected LLM calls during healing replay. However, for an optional failing step, 0 actions were recorded in the companion JSON during recording. In `REPLAY_WITH_HEALING`, there are no recorded actions to execute and fail, so self-healing is never triggered, resulting in 0 LLM calls.
- **Detection Gap ("What did we miss?"):** The metric assertion was copied from self-healing test cases where steps had broken actions that actively triggered the healing agent loop.
- **Resolution:** Updated the assertion in `OptionalIntegrationTest.java` from `.onHealing(m -> m.hasLlmCalls())` to `.onHealing(m -> m.hasNoLlmCalls())`.
- **Safety Net Added:** Verified `OptionalIntegrationTest` passes across all modes (`FORCE_RECORDING`, `REPLAY_STRICT`, `REPLAY_WITH_HEALING`).
