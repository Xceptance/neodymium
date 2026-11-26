# [DEF-20261004-0040] Core unit test depended on an e2e-only resource and failed

- **Status:** `Resolved`
- **Opened:** 2026-10-04 00:25
- **Closed:** 2026-10-04 00:40
- **Component:** `neodymium-core`
- **Scope:** `Test/Harness`
- **Symptom:** `YamlPlaybookParserTest#testParseSearchTestGermanYaml` failed with `FileNotFoundException: Classpath resource not found: verla/SearchTest_German.yaml`, so `mvn test -pl neodymium-core` was red.
- **Root Cause:** The test read `verla/SearchTest_German.yaml` from the core test classpath, but the file now only exists under `neodymium-e2e-tests/src/test/resources/verla/`. A core module cannot see resources of a downstream module.
- **Detection Gap ("What did we miss?"):** The resource was moved to the e2e module without running the core suite afterwards, and there is no CI in the repository that would run it on every change.
- **Resolution:** The test now parses an inline copy of the same YAML (anchor and alias reuse, three site variants, three data sets) through `InMemoryResourceManager`, with the same assertions: 27 steps, 3 data sets, first `testId` is `US`. The file stays in the e2e module because e2e tests still use it. The test also lost its inline fully qualified class name.
- **Safety Net Added:** `YamlPlaybookParserTest.testParseSearchTestGermanYaml` itself, now hermetic; the class passes (27 tests).
