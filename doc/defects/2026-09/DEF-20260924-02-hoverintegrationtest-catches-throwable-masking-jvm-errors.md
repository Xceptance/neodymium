# [DEF-20260924-02] HoverIntegrationTest Catches Throwable Masking JVM Errors and Lacks State Assertion

- **Status:** Resolved
- **Opened:** 2026-09-24
- **Closed:** 2026-09-24
- **Component:** `neodymium-core` (`HoverIntegrationTest`, `live-integration-tests`)
- **Scope:** `Test/Harness`
- **Symptom:** `HoverIntegrationTest.testHoverNonExistentElementFailure` used `assertThrows(Throwable.class, ...)`. If a fatal JVM Error (such as `OutOfMemoryError`, `StackOverflowError`, or linkage failure) occurred during execution, the test would catch it and falsely report success. Additionally, no post-condition DOM state was asserted after failure.
- **Root Cause:** Incomplete test harness exception targeting. Catching `Throwable` violates framework test guidelines, which mandate catching `Exception.class` to prevent swallowing VM-level errors while verifying DOM stability after failure.
- **Detection Gap ("What did we miss?"):** The negative test passed during green runs because the framework properly threw an exception, but code analysis and linters did not flag `Throwable.class` usage in test assertions.
- **Resolution:** Replaced `Throwable.class` with `Exception.class` in `testHoverNonExistentElementFailure` and added post-condition oracle assertion `$(".dropdown-content").shouldNotBe(visible)`.
- **Safety Net Added:** Clean code and exception audit rule enforced across live integration test suite.
