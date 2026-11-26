# [DEF-20260929-01] Multi-Module Working Directory Discrepancy Causes Premature Playbook Deletion and Replay Failure

- **Status:** Resolved
- **Opened:** 2026-09-29
- **Closed:** 2026-09-29
- **Component:** `neodymium-core` (`ai-testing` / `mock-integration` / `BaseAiTest`)
- **Scope:** `Test/Harness`
- **Symptom:** Mock integration tests (`ClickIntegrationTest`, `AssertIntegrationTest`, `HoverIntegrationTest`, etc.) executing with `@AiExecutionMode({AiExecutionMode.Type.FORCE_RECORDING, AiExecutionMode.Type.REPLAY_STRICT})` passed the recording step but failed on assertions like `assertTrue(new File("src/test/resources/...").exists())`. This caused `NeodymiumAiRunner.afterEach` to void/delete the newly recorded companion JSON file, causing the subsequent `REPLAY_STRICT` iteration to fail with `FileNotFoundException: No recorded companion JSON file found`.
- **Root Cause:** When running Maven from the aggregator reactor root (`neodymium-library`), Surefire executed tests with `user.dir` set to the repository root where `src/test/resources` does not exist (the test resources reside in `neodymium-core/src/test/resources`). Tests constructing `new File("src/test/resources/...")` resolved against the repository root instead of the module directory or classpath.
- **Detection Gap ("What did we miss?"):** Tests previously ran in IDEs or directly within the `neodymium-core` submodule directory where `user.dir` was set to `neodymium-core`. When executed from the reactor root in Maven, the relative file paths failed silently.
- **Resolution:** Added `getTestResourceFile(final String relativePath)` helper to `BaseAiTest.java` that inspects both submodule (`src/test/resources/...`) and multi-module aggregator (`neodymium-core/src/test/resources/...`) paths. Refactored all 18 mock integration test suites to use `getTestResourceFile(...)` and standardized deprecated `@AiPlaybook(name = ...)` usages to `@AiPlaybook(recordingFileName = ...)`.
- **Safety Net Added:** Verified all 18 mock integration test suites (`mvn test -pl neodymium-core -Dtest="org.neodymium.ai.integration.mock.*Test"`), passing 68/70 tests cleanly (remaining 2 errors isolated to `StoreIntegrationTest` validator in Issue #2).
