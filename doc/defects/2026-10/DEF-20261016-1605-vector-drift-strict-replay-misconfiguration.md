# [DEF-20261016-1605] REPLAY_STRICT Mode Served Attribute Drift URL Breaking Playbook Verification

- **Status:** `Resolved`
- **Opened:** 2026-10-16 14:05
- **Closed:** 2026-10-16 16:05
- **Component:** `neodymium-e2e-tests`
- **Scope:** `Test/Harness`
- **Symptom:** In `VectorHealingSandboxLiveTest.testVectorAttributeDriftHealingLive`, the `REPLAY_STRICT` execution mode failed with `NoSuchElementException: Cannot locate an element using PlaywrightBy: #coupon-input (Timeout: 3s)`.
- **Root Cause:** In `VectorHealingSandboxLiveTest.setupProperties`, the test checked `session.getExecutionMode().isReplay()` to decide whether to append `?drift=attribute` to the base URL. While `REPLAY_WITH_HEALING` is intended to encounter drifted DOM attributes (`#input_dyn_84920`) and exercise vector self-healing, `REPLAY_STRICT` explicitly disables self-healing by specification to ensure that recorded locators match verbatim. Injecting `?drift=attribute` during `REPLAY_STRICT` inherently violated the strict replay contract on an intentionally drifted page.
- **Detection Gap ("What did we miss?"):** Sibling mock tests (`VectorHealingSandboxMockTest`) and assertion drift live tests only ran `@AiMode({FORCE_RECORDING, REPLAY_WITH_HEALING})`, omitting `REPLAY_STRICT`. `testVectorAttributeDriftHealingLive` was configured with all three modes without distinguishing between strict replay (which requires the recorded baseline DOM) and healing replay (which tests drift adaptation).
- **Resolution:** Modified `VectorHealingSandboxLiveTest.setupProperties` to evaluate `session.getExecutionMode().supportsHealing()` rather than `isReplay()`. When running in `FORCE_RECORDING` or `REPLAY_STRICT`, `supportsHealing()` returns `false`, serving the clean `baseUrl`. When running in `REPLAY_WITH_HEALING`, `supportsHealing()` returns `true`, serving `baseUrl + "?drift=attribute"`.
- **Safety Net Added:** Verified via `mvn test-compile -pl neodymium-e2e-tests` and verified the clean DOM / drifted DOM execution matrix across `FORCE_RECORDING`, `REPLAY_STRICT`, and `REPLAY_WITH_HEALING` in `VectorHealingSandboxLiveTest`.
