# [DEF-20261116-1620] Static Retry Counter Pollution in OverwriteRetryTest

- **Status:** `Resolved`
- **Opened:** 2026-11-16 16:20
- **Closed:** 2026-11-16 16:28
- **Component:** `neodymium-classic`
- **Scope:** `Test/Harness`
- **Symptom:**
  `RepeatOnFailureAnnotationTest.testOverwriteRetryTest` failed under JUnit 5:
  `Assertion failed. Failure message ==> expected: <java.lang.AssertionError: Fail Child 3> but was: <java.lang.AssertionError: Fail Child 6>`.
- **Root Cause:**
  `OverwriteRetryTest` and `RetryParent` tracked retry attempts using `static AtomicInteger i = new AtomicInteger()`. Because static fields are classloader-scoped and survive across test executions in the same forked JVM process, any prior execution in the same JVM fork (such as an execution by Maven Surefire, a retry in `AbstractNeodymiumTest.run` on intermittent `SessionNotCreatedException`, or test re-runs) left `i` at 3. The subsequent execution incremented `i` from 3 to 6 (`Fail Child 6`) rather than resetting to 0.
- **Detection Gap ("What did we miss?"):**
  Running `RepeatOnFailureAnnotationTest#testOverwriteRetryTest` in isolation starts a fresh forked JVM where `i` initializes to 0, masking the static counter state pollution that occurs when tests run sequentially within shared Surefire forks or when test executions are retried.
- **Resolution:**
  1. In `OverwriteRetryTest.java` and `RetryParent.java` (JUnit 5 and JUnit 4), added `@BeforeAll` / `@BeforeClass` lifecycle hooks and public `reset()` methods to reset `i.set(0)`.
  2. In `RepeatOnFailureAnnotationTest.java` (JUnit 5 and JUnit 4), explicitly invoke `reset()` prior to `run(OverwriteRetryTest.class)`.
- **Safety Net Added:**
  - Automated tests verified in `neodymium-classic`:
    - `mvn test -Pneo-classic-junit5 -pl neodymium-classic -Dtest=RepeatOnFailureAnnotationTest#testOverwriteRetryTest`
    - `mvn test -Pneo-classic-junit5 -pl neodymium-classic -Dtest=RepeatOnFailureAnnotationTest`
    - `mvn test -Pneo-classic-junit4 -pl neodymium-classic -Dtest=RepeatOnFailureAnnotationTest`
