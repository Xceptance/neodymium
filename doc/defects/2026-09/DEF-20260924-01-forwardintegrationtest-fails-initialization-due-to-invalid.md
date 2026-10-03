# [DEF-20260924-01] ForwardIntegrationTest Fails Initialization Due to Invalid @AiDataSet on Programmatic Playbook

- **Status:** Resolved
- **Opened:** 2026-09-24
- **Closed:** 2026-09-24
- **Component:** `neodymium-core` (`NeodymiumAiRunner`, `live-integration-tests`)
- **Scope:** `Test/Harness`
- **Symptom:** Running `ForwardIntegrationTest` immediately crashed during JUnit Jupiter test template parameterization with `java.lang.IllegalArgumentException: No datasets defined in playbook 'programmatic', but @AiDataSet filter [forwardData] was specified.`
- **Root Cause:** `ForwardIntegrationTest` was annotated with `@AiDataSet("forwardData")` while using `@AiPlaybook("programmatic")`. `NeodymiumAiRunner` attempts to resolve and filter dataset names against pre-parsed YAML playbook tables when building invocation contexts before test execution begins. Because `"programmatic"` playbooks do not define static YAML dataset tables upfront, filtering against an undeclared dataset is illegal. The inline `data:` block passed to `session.execute(...)` was only evaluated at runtime, long after test invocation discovery had already aborted.
- **Detection Gap ("What did we miss?"):** The test was tagged with `@Tag("LiveAPI")` and `@Tag("AuraIntegration")`, which are frequently excluded during routine offline CI or unit test runs without LLM credentials, masking the fact that the test method failed at JUnit discovery/parameterization time before any browser or LLM connection was initiated.
- **Resolution:** Removed the `@AiDataSet` annotation and inline `data:` block from `ForwardIntegrationTest`. Standardized fixture parameter injection in `@BeforeEach` via `session.data().putDynamic(...)`. Modernized the test suite to use real link click navigation across dedicated test fixture pages (`ForwardActionTest/page1.html`, `page2.html`, `page3.html`), and added synonym phrasing (`testForwardSynonyms`), sequential multi-step history traversals (`testMultipleForward`), and fluent metric assertions (`verifyMetrics()`).
- **Safety Net Added:** Verified test parameterization across all execution modes (`FORCE_RECORDING`, `REPLAY_STRICT`, `REPLAY_WITH_HEALING`) via `mvn test -Dtest=ForwardIntegrationTest`.
