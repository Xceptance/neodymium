# [DEF-20260923-02] Duplicate @Test Annotation on @AiPlaybook Methods Triggering ParameterResolutionException

- **Status:** Resolved
- **Opened:** 2026-09-23
- **Closed:** 2026-09-23
- **Component:** `neodymium-core` (`CheckIntegrationTest`, `ClickIntegrationTest`, `HoverIntegrationTest`, `ClearCookiesIntegrationTest`, `RefreshIntegrationTest`, `SelectOptionIntegrationTest`)
- **Scope:** `Test/Harness`
- **Symptom:** Running tests failed with `ParameterResolutionException: No ParameterResolver registered for parameter [org.neodymium.ai.session.AiSession arg0] in method [public void setupProperties(org.neodymium.ai.session.AiSession)]` when `@BeforeEach` or test method expected `AiSession`.
- **Root Cause:** `@AiPlaybook` is meta-annotated with `@TestTemplate`. Adding `@Test` to the same method caused JUnit 5 Jupiter engine to discover and execute the method twice: once as standard `@Test` (where `AiInvocationExtension` is not registered and cannot resolve `AiSession`) and once as `@TestTemplate`. In addition, `SelectOptionIntegrationTest` used `@AiDataSet` without YAML datasets.
- **Detection Gap ("What did we miss?"):** Compilation succeeds because both `@Test` and `@AiPlaybook` are valid annotations; duplicate discovery errors only surface at test execution time.
- **Resolution:** Removed redundant `@Test` annotations and unused imports across affected live integration test classes; removed inapplicable `@AiDataSet` filter from `SelectOptionIntegrationTest`.
- **Safety Net Added:** Clean test execution of `CheckIntegrationTest` and companion live integration suites in both offline replay and live execution modes.
