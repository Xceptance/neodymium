# [DEF-20260930-03] BrowserToolProvider Uncaught ElementNotFound/AssertionError in Retry Loops

- **Status:** Resolved
- **Opened:** 2026-09-30
- **Closed:** 2026-09-30
- **Component:** `neodymium-core` (`tool/browser/BrowserToolProvider`)
- **Scope:** `Framework`
- **Symptom:** Replay tests failed with `ElementNotFound {#cart-btn-anchor.snapshot(1 elements)[0]} Expected: exist` caused by `StaleElementReferenceException` during `assert_text` evaluation.
- **Root Cause:** `matchesElementText`, `matchesElementOrAssociatedLabel`, `safeGetText`, and the `assert_text` retry loop caught only `java.lang.Exception`. In Selenide, `ElementNotFound` and `UIAssertionError` inherit from `java.lang.AssertionError` (subclass of `Error`), allowing stale/detached elements in dynamic collections to escape unhandled and prematurely abort the retry loop before the timeout.
- **Detection Gap ("What did we miss?"):** Previous unit tests inspected static DOMs without concurrent DOM mutations or stale collection snapshots during assertion retries.
- **Resolution:** Updated `matchesElementText`, `matchesElementOrAssociatedLabel`, `safeGetText`, `resolveElementBySelectorAndText`, and the `assert_text` retry loop in `BrowserToolProvider` to catch `(final Exception | AssertionError ignored)` per the `java_test_exception_handling` knowledge pattern.
- **Safety Net Added:** Verified via `CartTest.livePerfect` and `CartTest.replayPerfect` with dynamic cart badge DOM updates passing end-to-end.
