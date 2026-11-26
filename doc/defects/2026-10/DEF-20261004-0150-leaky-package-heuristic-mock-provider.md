# [DEF-20261004-0150] Leaky Package Substring Heuristic for Mock Provider in NeodymiumAiRunner

- **Status:** Resolved
- **Opened:** 2026-10-04 01:50
- **Closed:** 2026-10-04 01:51
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** Production test runner `NeodymiumAiRunner` inspected fully qualified test class names for package substrings `.integration.mock.`, `.sandbox.mock.`, and `.integration.data.` to inject `neodymium.ai.global.provider=mock`, inadvertently hijacking any customer tests matching those names.
- **Root Cause:** Absence of a declarative `@AiProvider` annotation led to string checks against test package names in the core test runner after clearing thread-local contexts.
- **Detection Gap ("What did we miss?"):** Tests in this repository resided in those exact packages, masking the hazard for external library consumers.
- **Resolution:**
  1. Introduced declarative `@AiProvider` annotation in `org.neodymium.ai.junit` supporting `TYPE`, `METHOD`, and `PACKAGE` targets.
  2. Updated `NeodymiumAiRunner` to resolve `@AiProvider` from method, class hierarchy, and package annotations, falling back to `System.getProperty("neodymium.ai.global.provider")`, and completely removed `fqcn.contains(...)` package string checks.
  3. Added `package-info.java` with `@AiProvider("mock")` for `org.neodymium.ai.integration.sandbox.mock`, `org.neodymium.ai.integration.mock`, and `org.neodymium.ai.integration.data`.
- **Safety Net Added:** Added `NeodymiumAiRunnerProviderTest` regression test suite (5 tests) verifying method-level override, class-level annotation, inheritance, system property fallback, and unannotated defaults.
