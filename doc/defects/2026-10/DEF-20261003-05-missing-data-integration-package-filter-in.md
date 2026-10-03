# [DEF-20261003-05] Missing Data Integration Package Filter in NeodymiumAiRunner Leading to ClassCastException in Mock Tests

- **Status:** Resolved
- **Opened:** 2026-10-03
- **Closed:** 2026-10-03
- **Component:** `neodymium-core` (`NeodymiumAiRunner`, `ProgrammaticTestDataYamlTest`, `TestdataSubstitutionTest`)
- **Scope:** `Framework` / `Test/Harness`
- **Symptom:**
  Executing `ProgrammaticTestDataYamlTest` or `TestdataSubstitutionTest` fails during test step execution with `ClassCastException: class org.neodymium.ai.client.GeminiLlmProvider cannot be cast to class org.neodymium.ai.client.MockLlmProvider`.
- **Root Cause:**
  `NeodymiumAiRunner$AiInvocationExtension.executeBeforeEach` purges thread-local context (`Neodymium.clearThreadContext()`) at the start of each test invocation. It re-injected `neodymium.ai.global.provider = mock` into thread data only if the test class FQCN contained `.integration.mock.` or `.sandbox.mock.`. Tests located in `org.neodymium.ai.integration.data.*` had their `@BeforeAll` data configuration wiped, falling back to the default `gemini` provider which cannot be cast to `MockLlmProvider`.
- **Detection Gap ("What did we miss?"):**
  Integration data tests were previously executed in environments where `neodymium.ai.global.provider` or mock configuration was set globally via system properties or config files rather than relying solely on test package naming heuristics.
- **Resolution:**
  Updated `NeodymiumAiRunner` to recognize `.integration.data.` packages alongside `.integration.mock.` and `.sandbox.mock.`. In addition, updated `ProgrammaticTestDataYamlTest` and `TestdataSubstitutionTest` to set and clear `System.setProperty("neodymium.ai.global.provider", "mock")` in `@BeforeAll` and `@AfterAll` as a robust fallback.
- **Safety Net Added:**
  Ran `mvn test -Dtest=ProgrammaticTestDataYamlTest,TestdataSubstitutionTest,AiDataFileProgrammaticIntegrationTest` verifying all mock data integration tests pass cleanly.
