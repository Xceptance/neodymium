# [DEF-20261104-1520] Stale Polling Timer Aborts Lazy Step Fetch on Row Switch, Write-Only Mutex Guard, and Interactive Over-Matching

- **Status:** `Resolved`
- **Opened:** 2026-11-04 15:15
- **Closed:** 2026-11-04 15:43
- **Component:** `aura-manager`
- **Scope:** `Framework & Test/Harness`
- **Symptom:** Switching from an active polling test row to an uncached test row intermittently freezes the side drawer on "Loading steps and screenshots..." without recovering. Furthermore, rapid clicks dispatch duplicate concurrent fetches because the in-flight mutex was never evaluated, and inspecting completed executions in a run with an active interactive session returns the interactive session's DTO for unrelated tests. In addition, Maven builds fail on CI environments lacking Node.js due to unhandled `IOException: Cannot run program "node"`.
- **Root Cause:**
  1. `openTestSidePagePanel` omitted calling `stopLiveStepRefreshTimer()`. When opening an uncached row, any pending `setTimeout(poll, 2500)` from the prior row remained active. When it triggered, its row switch check invoked `stopLiveStepRefreshTimer()`, which aborted the newly created `activeStepAbortController` of the incoming row and bumped `activeStepFetchId`, aborting the in-flight lazy load and leaving the loading spinner active.
  2. `isLiveStepFetchInFlight` was written to but never read, allowing duplicate lazy-load fetches to be dispatched before the first completed.
  3. In `AuraReportDataService.getExecutionDetails`, the condition `|| liveRunId.equalsIgnoreCase(effectiveRunId)` matched any execution query when an interactive session was active for that run ID, hijacking unrelated completed tests.
  4. `StaticResourceSyntaxTest.testSubStepLlmInvocationRenderingDoesNotReferenceUndeclaredSidx` and `testIsTerminalStatusRecognizesNormalizedStatusesAndHandlesAbort` invoked `ProcessBuilder("node")` without checking `isNodeAvailable()`, failing on environments without a global Node binary.
- **Detection Gap ("What did we miss?"):** Previous tests tested single-panel opening or headless status evaluation without testing rapid switching between an active polling row and an uncached row. In addition, mock tests for `AuraReportDataService` did not evaluate fallback behavior when `interactiveService` was populated alongside unrelated execution queries.
- **Resolution:**
  1. In `report-manager.js`, invoked `stopLiveStepRefreshTimer()` at the start of `openTestSidePagePanel()`.
  2. In `report-manager.js`, added active read guards on `isLiveStepFetchInFlight` in `renderStepsForExecution` and `poll()` to drop duplicate requests and reschedule.
  3. In `AuraReportDataService.java`, restricted `interactiveService` matching to `matchesTarget || isGenericId` (`console-execution`, `default`, `live`, or matching `effectiveRunId`).
  4. In `StaticResourceSyntaxTest.java`, added `if (!isNodeAvailable()) return;` guards to both Node VM tests.
- **Safety Net Added:** Added `StaticResourceSyntaxTest.testOpenTestSidePagePanelInvokesStopLiveStepRefreshTimer`, `StaticResourceSyntaxTest.testLiveStepFetchInFlightGuardsArePresent`, and `RunStorageSyncServiceTest.testExecutionDetailsWithInteractiveSessionDoesNotHijackUnrelatedExecutions`. All 47 tests pass under `mvn test -pl aura-manager`.
