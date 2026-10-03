# [DEF-20260924-05] Suite-Wide Underchecked Exception Types in Live Integration Tests

- **Status:** Resolved
- **Opened:** 2026-09-24
- **Closed:** 2026-09-24
- **Component:** `neodymium-core` (`live-integration-tests`)
- **Scope:** `Test/Harness`
- **Symptom:** 28 negative test cases across 6 live integration test classes (`AssertIntegrationTest`, `CheckIntegrationTest`, `ClearIntegrationTest`, `ClickIntegrationTest`, `HoverIntegrationTest`, `TimeoutIntegrationTest`) used overly broad exception assertions (`Throwable.class`, `Exception.class`, or untyped `catch (final Exception e)` blocks).
- **Root Cause:** Historical use of generic exception catch-alls during initial test harness scaffolding. This violates test isolation and robustness principles:
  1. Catching `Throwable.class` masks fatal JVM Errors (`OutOfMemoryError`, `StackOverflowError`, `LinkageError`).
  2. Catching broad `Exception.class` allows arbitrary syntax errors, timeouts, or configuration glitches to falsely satisfy tests.
  3. Action steps (e.g. clicking/clearing/checking/hovering missing or disabled elements) have a distinct contract from verification steps (e.g. asserting text, URL, visibility, count, or attributes): action failures conclusively throw `ConclusiveFailureException`, while verification failures throw `AssertionError`.
- **Detection Gap ("What did we miss?"):** Linters and CI only checked whether tests passed green, without validating exception specificity against the framework's execution pipeline contracts.
- **Resolution:** Refactored all 28 negative test methods across the 6 live test classes to assert the exact expected exception type:
  1. `AssertIntegrationTest` (19 methods) & `TimeoutIntegrationTest` (1 method): migrated to `assertThrows(AssertionError.class, ...)`.
  2. `CheckIntegrationTest` (2 methods), `ClearIntegrationTest` (3 methods), `ClickIntegrationTest` (2 methods), and `HoverIntegrationTest` (1 method): migrated to `assertThrows(ConclusiveFailureException.class, ...)`.
- **Safety Net Added:** Exact type contracts enforced across all live suite negative tests, preventing false passes on pipeline or environmental crashes.
