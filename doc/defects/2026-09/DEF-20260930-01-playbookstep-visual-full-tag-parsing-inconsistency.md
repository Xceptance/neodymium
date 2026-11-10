# [DEF-20260930-01] PlaybookStep (visual: full) Tag Parsing Inconsistency & VerifyOutcomeStep Unit Test Misconfiguration

- **Status:** Resolved
- **Opened:** 2026-09-30
- **Closed:** 2026-09-30
- **Component:** `neodymium-core` (`model` / `runner` / `test-fixtures`)
- **Scope:** `Framework` & `Test/Harness`
- **Symptom:** `PlaybookStepFullPagePersistenceTest` failed with `expected: <null> but was: <true>`; `RunnerIntegrationTest.testVerifyOutcomeStepFailure` failed with `Expected VerificationFailureException to be thrown, but nothing was thrown`; and `ProgrammaticDemoTest.test7` threw `Failed to parse playbook: ...ProgrammaticDemoTest_test7_...yaml`.
- **Root Cause:** 1) `PlaybookStep.setInstruction` automatically parses `(visual: full)` into `this.fullPage = true`, which contradicted an obsolete unit test assertion expecting `null`. 2) `VerifyOutcomeStep` requires `failOnError=true` (or transient configuration `neodymium.ai.semanticVerification.failOnError=true`) to throw `VerificationFailureException` rather than logging a soft warning. 3) The test class was refactored from `VerlaProgrammaticDemoTest` to `ProgrammaticDemoTest`, leaving the convention-based classpath YAML and JSON fixtures mismatched.
- **Detection Gap ("What did we miss?"):** Unit tests and convention-based integration fixtures were not verified in an end-to-end reactor run following the class rename and model tag parser enhancements.
- **Resolution:** Updated assertion in `PlaybookStepFullPagePersistenceTest` to expect `Boolean.TRUE`, configured `neodymium.ai.semanticVerification.failOnError=true` in `RunnerIntegrationTest`, and aligned convention-based YAML and JSON fixture filenames for `ProgrammaticDemoTest`.
- **Safety Net Added:** Verified unit suite passes cleanly with zero failures via `mvn test -pl neodymium-core -Dtest="PlaybookStepFullPagePersistenceTest,RunnerIntegrationTest,ProgrammaticDemoTest#test7*"`.
