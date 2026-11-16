# [DEF-20261113-1630] Surefire Error Log Accumulation and Boilerplate Bloat in Queue Execution

- **Status:** `Resolved`
- **Opened:** 2026-11-13 16:30
- **Closed:** 2026-11-13 16:32
- **Component:** `neodymium-core`
- **Scope:** `Framework & Test/Harness`
- **Symptom:** Test execution failure details in Aura Manager cards were excessively verbose, displaying repeated Surefire test lifecycle headers, entire Maven build failure diagnostics (`Failed to execute goal...`), dump file paths, debug CLI switches, and errors accumulated from all previously failed test files in the queue batch.
- **Root Cause:**
  1. `AuraQueueService.java` passed the shared, cumulative `currentRunLogs` buffer into `extractSubprocessErrorMessage(...)` after each test file in a batch finished, causing later tests to inherit and display the failure logs of earlier tests.
  2. `extractSubprocessErrorMessage` collected all lines matching `Error:`, `Caused by:`, `Exception:`, or `[ERROR]` without filtering out Maven build lifecycle errors, Surefire report pointers, or CLI debug suggestions, and joined all matches into an unstructured space-delimited string without deduplicating identical exception lines.
- **Detection Gap:** `AuraQueueServiceTest.testExtractSubprocessErrorMessageOnPlaybookParseException` tested only a synthetic 3-line log snippet with no Maven / Surefire CLI boilerplate or multi-file batch execution simulation.
- **Resolution:**
  1. Isolated subprocess stream logs into a per-file list (`fileSubprocessLogs`) in `AuraQueueService.java` so failures remain strictly scoped to the test file that produced them.
  2. Filtered out Maven and Surefire boilerplate (goal execution failures, report directory pointers, dump stream advice, CLI switch recommendations, test summary lines, and header lines) in `extractSubprocessErrorMessage`.
  3. Added deduplication and redundant summary line filtering so only the legitimate root cause exception messages are extracted and formatted cleanly.
- **Safety Net Added:** Added `@Test` `AuraQueueServiceTest.testExtractSubprocessErrorMessageFiltersMavenSurefireBoilerplateAndDeduplicates` and `testExtractSubprocessErrorMessagePreservesDirectSummaryWhenNoPriorExceptionLogged`.
