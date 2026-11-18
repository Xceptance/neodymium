# [DEF-20261116-1530] ConcurrentHashMap Null Key NPE during BrowserRunner Teardown

- **Status:** `Resolved`
- **Opened:** 2026-11-16 15:30
- **Closed:** 2026-11-16 15:47
- **Component:** `neodymium-classic`
- **Scope:** `Framework & Test/Harness`
- **Symptom:**
  Multiple test suites (`AllureSelenideListenerTest`, `BrowserStatementTest`, `CucumberTest`, `NeodymiumContextTest`, `NeodymiumWebDriverTest`, `RepeatOnFailureAnnotationTest`) failed with:
  `java.lang.NullPointerException: Cannot invoke "Object.hashCode()" because "key" is null at ConcurrentHashMap.get() -> FilmTestExecution.popThread(FilmTestExecution.java:180) -> FilmTestExecution.finishGifFilming -> BrowserRunner.teardown`.
- **Root Cause:**
  When `GIF_THREADS` and `VIDEO_THREADS` in `FilmTestExecution.java` were migrated from `Collections.synchronizedMap(new WeakHashMap<>())` to `ConcurrentHashMap` to avoid GC key eviction, the null-key handling changed. `WeakHashMap` permitted `null` keys, whereas `ConcurrentHashMap` strictly prohibits null keys (`get(null)` and `remove(null)` throw `NullPointerException`). When tests teardown without an active recording session or when `setUpTest()` was not invoked (such as cucumber runners, browser statements before/after hooks, or context teardowns), `BrowserRunner.recordingID` remains `null`. Invoking `FilmTestExecution.finishGifFilming(recordingID, testFailed)` dereferenced `ConcurrentHashMap.get(null)`, throwing NPE.
- **Detection Gap ("What did we miss?"):**
  Targeted regression testing in the previous fix focused on `ManualGifRecordingTest` and `DownloadFilesExecutorTest`, which both establish active sessions with non-null recording UUIDs, failing to exercise teardowns of uninitialized recording IDs across the broader suite.
- **Resolution:**
  1. In `FilmTestExecution.java` (`neodymium-classic`), added early null guards `if (testName == null) { return null; }` to `popThread()` and `createThread()`, aligning with `neodymium-core`.
  2. In `BrowserRunner.java` (`neodymium-classic`), guarded `teardown()` with `if (recordingID != null && ...)` for defensive lifecycle cleanup.
- **Safety Net Added:**
  - Automated tests verified in `neodymium-classic`:
    - `mvn test -pl neodymium-classic -Dtest=AllureSelenideListenerTest,BrowserStatementTest,CucumberTest,NeodymiumContextTest,NeodymiumWebDriverTest,RepeatOnFailureAnnotationTest,ManualGifRecordingTest,DownloadFilesExecutorTest`
