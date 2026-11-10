# [DEF-20261003-06] VectorHealingSandboxMockTest Replay Failure Due to REPLAY_STRICT Healing Bypass and Variable Sanitization Collision

- **Status:** Resolved
- **Opened:** 2026-10-03
- **Closed:** 2026-10-03
- **Component:** `neodymium-core` (`VectorHealingSandboxMockTest`)
- **Scope:** `Test/Harness`
- **Symptom:**
  Executing `VectorHealingSandboxMockTest` fails 4 out of 8 executions during replay with `Element not found {#coupon-input} Expected: exist (Timeout: 3s)`.
- **Root Cause:**
  1. `VectorHealingSandboxMockTest` specified `@AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT})`. Following DEF-20261001-05, `REPLAY_STRICT` strictly disables all locator self-healing (`supportsHealing() == false`). Because the test page intentionally mutates IDs, classes, and DOM hierarchy between recording and replay, `REPLAY_STRICT` caused immediate selector lookup failures instead of exercising Tier-4 DOM Feature Vector healing.
  2. In `setupPropertiesAndMock`, all four drift URLs were assigned to `session.data()` before each test. During recording, all four variables held the identical `baseUrl` value, causing `DefaultActionSanitizer` to arbitrarily sanitize navigation targets to `${drift.wrapper.url}` for all four test playbooks.
- **Detection Gap ("What did we miss?"):**
  `VectorHealingSandboxMockTest` is tagged with `@Tag("AuraIntegration")` and was not executed as part of standard unit tests when `REPLAY_STRICT` healing bypass was implemented in DEF-20261001-05.
- **Resolution:**
  1. Updated `@AiMode` on all four test methods to `{ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_WITH_HEALING}`.
  2. Isolated URL parameterization so only the relevant test URL is injected into `session.data()` for each scenario, preventing reverse variable sanitization collisions.
  3. Cleaned up inline FQCNs (`Neodymium.getData()`).
- **Safety Net Added:**
  Verified all 8 executions (recording and replay across 4 drift scenarios: attribute drift, structural wrapper drift, tag migration, and sibling disambiguation) pass cleanly via `mvn test -pl neodymium-core -Dtest=VectorHealingSandboxMockTest`.
