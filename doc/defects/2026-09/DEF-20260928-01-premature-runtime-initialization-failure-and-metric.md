# [DEF-20260928-01] Premature Runtime Initialization Failure and Metric Deficit in WaitIntegrationTest

- **Status:** Resolved
- **Opened:** 2026-09-28
- **Closed:** 2026-09-28
- **Component:** `neodymium-core` (`live-integration` / `action-plugins`)
- **Scope:** `Test/Harness`
- **Symptom:** `WaitIntegrationTest` previously failed during test template setup with `IllegalArgumentException: No datasets defined in playbook 'programmatic', but @AiDataSet filter [waitData] was specified`, discarded the execution result without metric validations (`verifyMetrics()`) allowing silent drops to go undetected, suffered from a weak assertion oracle (no pre-condition check), and lacked reach into static duration pauses (`wait` tool), dynamic DOM pop-in, content mutations, and loader disappearance.
- **Root Cause:** Programmatic playbooks without static YAML dataset definitions cannot use `@AiDataSet`; the test harness only contained a single happy-path method that triggered `assert_element_state` rather than exercising duration-based wait tools or multiple asynchronous DOM transition states provided in `testWaitHappyPath.html`.
- **Detection Gap ("What did we miss?"):** Test suite reviews did not enforce metric chaining or full fixture scenario reach for `WaitIntegrationTest`, leaving orphaned annotations, unused imports, and narrow reach undetected.
- **Resolution:** Removed orphaned dataset annotations and unused imports, added `@AiLinter(false)`, isolated URL setup into `@BeforeEach`, enabled full mode verification (`FORCE_RECORDING`, `REPLAY_STRICT`, `REPLAY_WITH_HEALING`) with `.verifyMetrics()`, and expanded coverage across pre-condition checks, hidden element reveals, dynamic pop-ins, content mutations, loader disappearance, and static duration pauses.
- **Safety Net Added:** Modernized live integration test suite in `WaitIntegrationTest.java` with comprehensive multi-scenario execution and strict metric assertions.
