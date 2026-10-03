# [DEF-20260929-07] Shadow DOM Test Step Inadvertently Scopes Status Assertion to Component Host

- **Status:** Resolved
- **Opened:** 2026-09-29
- **Closed:** 2026-09-29
- **Component:** `neodymium-core` (`sandbox-tests` / `live-integration` / `ShadowDomSandboxLiveTest`)
- **Scope:** `Test/Harness`
- **Symptom:** Running `ShadowDomSandboxLiveTest.testShadowDomLive` failed with `Expected text/pattern "Login successful for: admin" was not found on selector "#login-form-host #shadow-status" within 3000ms.`
- **Root Cause:** Test step instruction `Verify that #shadow-status shows "Login successful for: admin"` directly followed three steps targeting elements "inside the login form", causing the LLM to scope `#shadow-status` inside `#login-form-host` rather than querying the top-level status span in the main document.
- **Detection Gap ("What did we miss?"):** Mock tests explicitly hardcoded the top-level selector `#shadow-status` in queued responses, masking the contextual bias introduced by the phrasing in the live test prompt.
- **Resolution:** Clarified step instruction in `ShadowDomSandboxLiveTest.java` to `Verify that the page status #shadow-status shows "Login successful for: admin"`.
- **Safety Net Added:** Verified `ShadowDomSandboxLiveTest` passes across all modes (`FORCE_RECORDING`, `REPLAY_STRICT`, `REPLAY_WITH_HEALING`).
