# [DEF-20261016-1830] Deprecation Cleanup of Incomplete AiMethod Migration and MetricsAsserter Tests

- **Status:** `Resolved`
- **Opened:** 2026-10-16 18:30
- **Closed:** 2026-10-16 18:46
- **Component:** `neodymium-core`
- **Scope:** `Framework & Test/Harness`
- **Symptom:** javac emitted 19+ `[WARNING] ... has been deprecated and marked for removal` warnings during compile of `neodymium-core`, specifically flagging `@AiMethod` in `AiAssertions`, `JavaMethodAction`, `JavaToolFactory`, and `JavaMethodActionTest`, as well as `hasStandardCalls(...)` in `MetricsAsserterTest`.
- **Root Cause:**
  1. When unified tooling was introduced, `@AiMethod` was marked `@Deprecated(since = "5.6.0", forRemoval = true)` in favor of `@Tool(description = "...")`, but `AiAssertions` was never migrated, retaining `@AiMethod` on 14 public assertion methods.
  2. `JavaMethodAction` only scanned for `@AiMethod`, lacking reflection support for `@Tool`.
  3. Reflection lookups in `JavaToolFactory` and tests in `JavaMethodActionTest` and `MetricsAsserterTest` exercising deprecated backwards-compatibility methods lacked targeted `@SuppressWarnings("removal")`.
- **Detection Gap ("What did we miss?"):** The build passed because deprecation warnings were not treated as fatal compiler errors by default, allowing terminal deprecations to linger without completing migration to `@Tool`.
- **Resolution:**
  1. Migrated all 14 assertion methods in `AiAssertions.java` from `@AiMethod("...")` to `@Tool(description = "...")`.
  2. Updated `JavaMethodAction.java` to support both `@Tool` and legacy `@AiMethod`, adding dedicated helper methods with `@SuppressWarnings("removal")` to localize suppression.
  3. Added `@SuppressWarnings("removal")` to `JavaToolFactory.java` on legacy `AiMethod` inspection methods.
  4. Added `@SuppressWarnings("removal")` to legacy fixtures in `JavaMethodActionTest.java` and `MetricsAsserterTest.java`.
- **Safety Net Added:** Added test `testInvokeToolStaticMethodSuccess` in `JavaMethodActionTest` to verify that methods annotated with `@Tool` are cleanly resolved and executed by `JavaMethodAction`.
