# [DEF-20261004-0036] `assert_url` / `assert_title` with `empty:false` and an expected value asserted nothing

- **Status:** `Resolved`
- **Opened:** 2026-10-04 00:05
- **Closed:** 2026-10-04 00:36
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** An `assert_url` or `assert_title` call such as `{ expectedUrl: "/checkout", empty: false }` passed on any non-empty URL or title, even when the actual value did not match the expected one.
- **Root Cause:** In `BrowserToolProvider` both tools computed `notEmpty` from the `empty` argument alone and returned early as soon as it was set, before the expected value was read. An LLM that sends `empty:false` next to an expected value therefore turned a content assertion into a "not empty" check.
- **Detection Gap ("What did we miss?"):** `BrowserToolsTest` covered `empty:false` on its own and an expected value on its own, but not both in one call, which is the shape an LLM tends to produce.
- **Resolution:** `notEmpty` is now evaluated after the expected value has been read. `empty:false` is treated as "must not be empty" only when no expected value is present; otherwise the expected-value comparison runs. Behaviour for `empty:false` alone is unchanged.
- **Safety Net Added:** Cases 6 and 7 in `BrowserToolsTest.testBrowserAssertUrlExecutionAndSchema` and `BrowserToolsTest.testBrowserAssertTitleExecutionAndSchema` (case 6: `empty:false` plus a mismatching expected value must throw `AssertionError`; case 7: `empty:false` alone succeeds). Both tests failed before the fix. The whole class passes (57 tests).
