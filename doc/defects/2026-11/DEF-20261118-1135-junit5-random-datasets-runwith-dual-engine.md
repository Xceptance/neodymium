# [DEF-20261118-1135] Erroneous JUnit 4 @RunWith Annotation on JUnit 5 Test Class Causing Dual-Engine Execution

- **Status:** `Resolved`
- **Opened:** 2026-11-18 11:35
- **Closed:** 2026-11-18 11:38
- **Component:** `neodymium-classic` & `neodymium-core`
- **Scope:** `Test/Harness`
- **Symptom:** Running `TestDataStatementTest#testClassRandomDataSetSelectedFromRange` failed with `Assertion failed. Method run count ==> expected: <4> but was: <5>` in `neodymium-classic`, and `InvalidTestClassError: No runnable methods` in `neodymium-core`.
- **Root Cause:** In the JUnit 5 test fixture `ClassRandomDataSetsFromRange`, the class was erroneously annotated with JUnit 4's `@RunWith(NeodymiumRunner.class)`. During test discovery via JUnit Platform `Launcher`, both the JUnit Jupiter and JUnit Vintage engines claimed the class. In `neodymium-classic`, Vintage added an unwanted 5th execution to Jupiter's 4 random dataset runs; in `neodymium-core`, Vintage failed because Jupiter's `@NeodymiumTest` was not recognized as a runnable JUnit 4 method.
- **Detection Gap ("What did we miss?"):** The test fixture was ported from the JUnit 4 suite without removing the `@RunWith(NeodymiumRunner.class)` runner annotation, which only fails when JUnit Platform evaluates both engines in a single discovery pass with exact execution count assertions.
- **Resolution:** Removed `@RunWith(NeodymiumRunner.class)` and its unreferenced imports (`RunWith`, `NeodymiumRunner`) from `ClassRandomDataSetsFromRange.java` in both `neodymium-classic` and `neodymium-core`.
- **Safety Net Added:** Automated regression tests verified via `mvn test -pl neodymium-classic -Dtest=TestDataStatementTest#testClassRandomDataSetSelectedFromRange` and `mvn test -pl neodymium-core -Dtest=TestDataStatementTest#testClassRandomDataSetSelectedFromRange`.
