# [DEF-20261105-1122] Maven Test Profile Scoping Flaws, Shadowing Leaks, and Missing Profiles

- **Status:** `Resolved`
- **Opened:** 2026-11-05 11:22
- **Closed:** 2026-11-05 11:44
- **Component:** `pom.xml`, `neodymium-core`, `neodymium-test-server`, `aura-manager`, `neodymium-e2e-tests`
- **Scope:** `Infra/Build & Test/Harness`
- **Symptom:** Running specific Maven profiles (e.g. `mvn test -Punit-tests` or `mvn test -Pneo-classic-junit4`) failed to isolate execution:
  1. In `unit-tests`, `neodymium-e2e-tests` executed all E2E browser tests instead of skipping, while `neodymium-core` executed hundreds of non-unit tests and browser tests (e.g. `AlertHandlingTest`, `InteractiveViewStateTest`), causing long 10-minute runtimes and test failures.
  2. In `smoke`, non-existent test references (`AiRoleTest`, `com/xceptance/neodymium/util/DataUtilsTest`, and `AuraSandboxBaselineMockTest`) caused dead-reference warnings or suite failures.
  3. In `verla-tests`, `ai-live-tests`, and `aura-sandbox`, modules intended to be skipped were not skipped because profile skip properties defined at root were shadowed by child module POM properties.
  4. In `aura-manager`, the `non-llm-tests` profile was completely missing, causing `WikipediaSearchTest` (which navigates over public internet to Wikipedia) and UI tests to execute during supposed deterministic offline test runs.
- **Root Cause:**
  1. *Maven POM Inheritance Shadowing:* The root aggregator POM defined skip properties (`skipCoreTests`, `skipServerTests`, `skipE2eTests`) in root profiles, but child POMs declared `<skipCoreTests>false</skipCoreTests>` etc. directly in their child `<properties>` blocks. In Maven property resolution order, properties declared in child POM `<properties>` take precedence over profile-injected properties from a parent POM. As a result, the tests were never skipped.
  2. *Sparse Profile Matrix:* Child modules lacked explicit `<profile>` definitions with `<skipTests>true</skipTests>` for profiles they were supposed to skip.
  3. *Unverified Class References:* The `smoke` profile included non-existent classes (`AiRoleTest`, `com/xceptance/neodymium/util/DataUtilsTest`, `AuraSandboxBaselineMockTest`).
  4. *Overly Broad Wildcard Patterns in Unit Profile:* `unit-tests` included `org/neodymium/ai/**/*Test.java` and `com/xceptance/neodymium/aura/**/unit/*Test.java` which pulled in full AI pipeline runners, configuration precedence tests, and interactive Selenide view tests (`InteractiveViewStateTest`), violating the zero-browser, pure in-memory contract of `unit-tests`.
- **Detection Gap ("What did we miss?"):** The initial implementation was verified primarily with class filters (`-Dtest=...`) which masked module skipping failures because Surefire's `-Dtest` filtered out all non-matching tests in unskipped modules. Full profile commands without `-Dtest` were not executed end-to-end across the aggregator reactor.
- **Resolution:**
  1. *Removed Shadowed Properties:* Removed `skipCoreTests`, `skipServerTests`, `skipE2eTests` from child POM properties and replaced root property toggles with explicit local profile declarations in child modules setting `<properties><skipTests>true</skipTests></properties>`.
  2. *Aligned Complete Profile Matrix:* Added `<skipTests>true</skipTests>` profiles to each submodule for every profile where that submodule must not execute. Added `non-llm-tests` profile to `aura-manager/pom.xml` excluding browser and external tests (`WikipediaSearchTest`).
  3. *Corrected Test Inclusions in `smoke`:* Replaced dead references with valid, deterministic tests: `org/neodymium/ai/client/TokenUsageTest.java`, `ExecutionMetricsTest.java`, and `com/xceptance/neodymium/junit5/tests/DataUtilsTest.java` in core; `AutocompleteSandboxMockTest.java` in e2e-tests.
  4. *Scoped Unit Tests Strictly In-Memory:* In `neodymium-core`, targeted `unit-tests` to `org/neodymium/ai/model/**`, `org/neodymium/ai/unit/**`, `TokenUsageTest`, `AuraManager` unit services, and utility tests while excluding interactive browser tests (`com/xceptance/neodymium/aura/interactive/**`).
  5. *Restored Classic Engine Alignment:* Separated `util` JUnit 4 and JUnit 5 tests between `neo-classic-junit4` and `neo-classic-junit5` to ensure Vintage and Jupiter engines run their respective tests.
- **Safety Net Added:** Verified full multi-module aggregator execution of `mvn test -Psmoke` (completed in 43 seconds, 100% green across all modules) and `mvn test -Punit-tests` (verified E2E and server module skipping).
