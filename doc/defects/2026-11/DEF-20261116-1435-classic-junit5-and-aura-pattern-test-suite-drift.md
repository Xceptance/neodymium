# [DEF-20261116-1435] Classic JUnit5 Download, Recording Regressions, and Aura Manager Pattern Test Leak

- **Status:** `Resolved`
- **Opened:** 2026-11-16 14:35
- **Closed:** 2026-11-16 14:45
- **Component:** `neodymium-classic`, `aura-manager`
- **Scope:** `Framework & Test/Harness & Config/Environment`
- **Symptom:**
  1. `DownloadFilesExecutorTest.test` failed in JUnit5 (`ElementNotFound: {downloads-manager -> #downloadsList downloads-item -> #title-area.findBy(exact text "sample.pdf")}`) and in Firefox (`UnsupportedCommandException: Navigation to "about:downloads" is not allowed in this context`).
  2. `ManualGifRecordingTest>AbstractRecordingTest` failed with `expected:<1> but was:<0>` and `assertRecordingFileExists: the recording file doesn't exist`.
  3. `CustomDashboardViewControllerIntegrationTest.testGroupDashboardCardsRenderHealedAndAiPatternSvg` failed with `Must contain pie-slice-ai circle ==> expected: <true> but was: <false>`.
- **Root Cause:**
  1. In `DownloadFilesExecutorTest`, `browserprofile.firefox_download.arguments` was configured with `--headless=new` (a Chromium flag rejected by Firefox) instead of `browserprofile.firefox_download.headless = true`. Furthermore, `DownloadFilesInDifferentWays.downloadViaLink` did not delete preexisting `target/sample.pdf` before clicking the link, causing `waitForFileDownloading` to return immediately and query `chrome://downloads/` before download completion. In containerized/headless environments, querying `chrome://downloads/` shadow DOM can also time out or be restricted.
  2. In `FilmTestExecution.java` in `neodymium-classic`, `GIF_THREADS` and `VIDEO_THREADS` used `Collections.synchronizedMap(new WeakHashMap<>())` with dynamic String keys (`UUID.randomUUID().toString()`). Under JVM GC pressure during extended test suite runs, these unanchored String keys were garbage collected and evicted, clearing active threads and causing `null.gif` / 0 recording files.
  3. In `CustomDashboardViewControllerIntegrationTest.testGroupDashboardCardsRenderHealedAndAiPatternSvg`:
     a. In `LocalRunJsonStorageService.updateExecutionInRun()`, the test-case-matching predicate contained `|| (jsonFiles.size() == 1)`. When saving a multi-execution report via `dataService.saveRunReportToDisk()`, the first execution created `console-execution-1.json`. When saving the second execution, `jsonFiles.size() == 1` matched `console-execution-1.json` even though the row ID did not match, overwriting execution 1 with execution 2 instead of creating `console-execution-2.json`. As a result, the saved report retained only 1 execution rather than 2, leaving `passAi` as 0 and omitting the AI slice from the SVG chart.
     b. Additionally, the group dashboard was created with `runFilters = null`. Consequently, `AuraReportViewController` fell back to `pageEffectiveRuns = allRuns`, aggregating across all test runs in the database rather than isolating the test run's specific metrics.
- **Detection Gap ("What did we miss?"):**
  1. `DownloadFilesExecutorTest` was verified in isolation where preexisting target artifacts were absent, masking the race condition on `target/sample.pdf` and container shadow DOM restrictions.
  2. `FilmTestExecution` was refactored to `ConcurrentHashMap` in `neodymium-core` but the classic submodule was overlooked during the architecture migration.
  3. `LocalRunJsonStorageService.updateExecutionInRun()`'s single-file heuristic was designed for legacy single-test console runs, but erroneously triggered during iterative multi-execution saves whenever exactly 1 execution file existed on disk at that moment.
  4. `CustomDashboardViewControllerIntegrationTest` passed when executed alone on an empty database, but when executed as part of the full suite, residual runs in H2 contaminated group card aggregation.
- **Resolution:**
  1. In `DownloadFilesExecutorTest.java` (both JUnit5 and JUnit4 in `neodymium-classic`), replaced `--headless=new` with `browserprofile.firefox_download.headless=true`.
  2. In `DownloadFilesInDifferentWays.java` (JUnit5 and JUnit4), ensured `fileName.delete()` is executed before clicking the download link in `downloadViaLink()`, and added a fallback to on-disk file verification if the Chrome shadow DOM downloads query fails.
  3. In `FilmTestExecution.java` (`neodymium-classic`), migrated `GIF_THREADS` and `VIDEO_THREADS` from `WeakHashMap` to `ConcurrentHashMap`, aligning with `neodymium-core`.
  4. In `LocalRunJsonStorageService.java`, restricted the single-file fallback `(jsonFiles.size() == 1)` so that when `rowId != null && !rowId.isEmpty()`, it only matches if the execution is a fallback placeholder (`nodeId.isEmpty() && nodeTestId.isEmpty() && (tClass.isEmpty() || "DefaultClass".equalsIgnoreCase(tClass))`), preventing distinct test executions from clobbering each other during report disk persistence.
  5. In `CustomDashboardViewControllerIntegrationTest.java`, provided an explicit `WidgetFilterConfigDto` scoped to the test's `browser` when creating `groupPage`, guaranteeing isolated card aggregation against any residual database state.
- **Safety Net Added:**
  - Automated tests verified in `neodymium-classic`:
    - `mvn test -Pneo-classic-junit5 -pl neodymium-classic -Dtest=DownloadFilesExecutorTest,ManualGifRecordingTest`
    - `mvn test -Pneo-classic-junit4 -pl neodymium-classic -Dtest=DownloadFilesExecutorTest,ManualGifRecordingTest`
  - Automated test verified in `aura-manager`:
    - `mvn test -Paura-manager -Dtest=CustomDashboardViewControllerIntegrationTest`
