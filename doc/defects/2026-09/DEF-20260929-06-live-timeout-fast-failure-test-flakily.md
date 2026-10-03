# [DEF-20260929-06] Live Timeout Fast-Failure Test Flakily Asserts Wall-Clock Network Latency

- **Status:** Resolved
- **Opened:** 2026-09-29
- **Closed:** 2026-09-29
- **Component:** `neodymium-core` (`ai-testing` / `live-integration` / `TimeoutIntegrationTest`)
- **Scope:** `Test/Harness`
- **Symptom:** Running `TimeoutIntegrationTest.testTimeoutFastFailureOnNonExistentElement` failed with `Test should fail fast (under 3 seconds) due to (timeout:50ms) tag, but took 13359 ms ==> expected: <true> but was: <false>`.
- **Root Cause:** The test asserted `duration < 3000ms` on `session.execute` across an entire multi-step scenario communicating with live cloud Gemini APIs over the internet. When the 50ms element lookup timed out as designed, the engine executed Visual RCA and multi-turn error recovery over the network, totaling ~13 seconds. Wall-clock latency under 3 seconds is unrealistic and unstable for live cloud LLM calls.
- **Detection Gap ("What did we miss?"):** The duration check was ported from mock tests where mock responses return in 0ms without network roundtrips.
- **Resolution:** Disabled Visual RCA for the fast-failure test method, asserted `AssertionError` on the missing selector, and adjusted the wall-clock guardrail to a realistic non-hanging limit.
- **Safety Net Added:** Verified `TimeoutIntegrationTest` passes across all live modes.
