# [DEF-20260928-02] Premature Runtime Initialization Failure and Metric Deficit in TypeIntegrationTest

- **Status:** Resolved
- **Opened:** 2026-09-28
- **Closed:** 2026-09-28
- **Component:** `neodymium-core` (`live-integration` / `action-plugins`)
- **Scope:** `Test/Harness`
- **Symptom:** `TypeIntegrationTest` crashed immediately upon test startup with `IllegalArgumentException: No datasets defined in playbook 'programmatic', but @AiDataSet filter [typeData] was specified`. In addition, execution metrics were discarded without validation, pre-conditions were unverified, and the test suite lacked coverage for multiline textareas, sequential multi-field submission, natural language semantic locators, value overwriting, and negative failure handling on disabled, readonly, and non-existent elements.
- **Root Cause:** Programmatic playbooks without external static YAML datasets cannot use `@AiDataSet`; the test harness contained an orphaned `@AiDataSet("typeData")` annotation and inline `data:` block. The test had not been modernized to adopt the `@BeforeEach` setup, `@AiLinter(false)`, `REPLAY_WITH_HEALING`, or `.verifyMetrics()` standards established across other action integration suites.
- **Detection Gap ("What did we miss?"):** Test suite sweeps did not execute `TypeIntegrationTest` individually in CI; linting and metric chaining requirements were not enforced on legacy live action tests.
- **Resolution:** Removed orphaned dataset annotations and unused imports, added `@AiLinter(false)`, isolated URL setup into `@BeforeEach`, enabled full mode verification (`FORCE_RECORDING`, `REPLAY_STRICT`, `REPLAY_WITH_HEALING`) with `.verifyMetrics()`, and expanded coverage across single-line input, multiline textareas, sequential form submission, natural language semantic locators, value overwriting, and negative failure handling for disabled, readonly, and missing inputs.
- **Safety Net Added:** Modernized live integration test suite in `TypeIntegrationTest.java` with 8 comprehensive scenarios and strict metric assertions.
