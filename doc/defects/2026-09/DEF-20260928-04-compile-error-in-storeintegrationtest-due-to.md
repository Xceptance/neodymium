# [DEF-20260928-04] Compile Error in StoreIntegrationTest Due to Undefined SessionData Method

- **Status:** Resolved
- **Opened:** 2026-09-28
- **Closed:** 2026-09-28
- **Component:** `neodymium-core` (`live-integration` / `StoreIntegrationTest`)
- **Scope:** `Test/Harness`
- **Symptom:** `StoreIntegrationTest.java` lines 136-137 failed compilation in the IDE with `The method getDynamic(String) is undefined for the type SessionData`.
- **Root Cause:** In `testStoreMultipleVariables()`, `session.data().getDynamic(...)` was mistakenly used instead of `session.data().get(...)`. Incremental compilation in `mvn test-compile` masked the compiler error by treating the previously built `.class` file as up to date.
- **Detection Gap ("What did we miss?"):** Incremental Maven builds do not always recompile every test when target timestamps precede source edits; full clean test compilation was needed to uncover the syntax discrepancy.
- **Resolution:** Replaced `session.data().getDynamic(...)` calls with `session.data().get(...)`.
- **Safety Net Added:** Verified clean test compilation (`mvn clean test-compile`) ensuring 0 compilation errors across all test sources.
