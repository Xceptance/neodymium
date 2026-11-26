# [DEF-20260928-03] Premature Runtime Initialization Failures and Scope Deficits in Live Integration Tests (Scroll, Store, Timeout)

- **Status:** Resolved
- **Opened:** 2026-09-28
- **Closed:** 2026-09-28
- **Component:** `neodymium-core` (`live-integration` / `action-plugins`)
- **Scope:** `Test/Harness`
- **Symptom:** `ScrollIntegrationTest`, `StoreIntegrationTest`, and `TimeoutIntegrationTest` failed immediately on invocation with `IllegalArgumentException: No datasets defined in playbook 'programmatic', but @AiDataSet filter [...] was specified`, preventing execution. In addition, `RefreshIntegrationTest`, `ScrollIntegrationTest`, `SelectOptionIntegrationTest`, `StoreIntegrationTest`, and `TimeoutIntegrationTest` lacked `@AiLinter(false)`, omitted `.verifyMetrics()` chaining, lacked `@BeforeEach` lifecycle isolation, suffered from tagging inconsistencies (`@Tag("integration")`), and left major action scenarios unverified.
- **Root Cause:** Programmatic playbooks without external static YAML datasets cannot use `@AiDataSet`; legacy test classes contained orphaned dataset annotations and inline `data:` blocks. The test classes were never upgraded to the modern lifecycle, metric verification, and multi-scenario standards.
- **Detection Gap ("What did we miss?"):** The live integration test suite was not run with full test discovery across all live action classes in CI; individual test runs masked runner initialization failures.
- **Resolution:** Removed orphaned dataset annotations and dead data blocks, added `@AiLinter(false)`, isolated URL initialization into `@BeforeEach`, aligned JUnit tags to `@Tag("AuraIntegration")` and `@Tag("LiveAPI")`, enabled full mode verification (`FORCE_RECORDING`, `REPLAY_STRICT`, `REPLAY_WITH_HEALING`) with strict `.verifyMetrics()` chaining, and expanded scenario coverage across all 5 test classes.
- **Safety Net Added:** Modernized live integration test suites in `RefreshIntegrationTest.java`, `ScrollIntegrationTest.java`, `SelectOptionIntegrationTest.java`, `StoreIntegrationTest.java`, and `TimeoutIntegrationTest.java` with comprehensive multi-scenario execution and strict metric assertions.
