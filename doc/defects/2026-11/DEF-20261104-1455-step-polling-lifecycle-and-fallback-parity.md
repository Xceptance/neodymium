# [DEF-20261104-1455] Step Polling Lifecycle Race Conditions and Fallback Key Parity

- **Status:** `Resolved`
- **Opened:** 2026-11-04 14:00
- **Closed:** 2026-11-04 15:05
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** Step details in side panel intermittently fail or degrade under slow networks: rapid test switching or re-rendering could trigger timer proliferation (fork bombing) and overlapping fetches, `isTerminalStatus` did not recognize `cancelled`/`canceled`/`stopped` runs causing indefinite polling on terminated executions, fetch failures on initial lazy load poisoned the cache attribute `data-steps-fetched="true"` preventing subsequent retries, and `run.json` / memory fallback in `AuraReportDataService.getExecutionDetails` did not match short class names (`SimpleClassName#testMethod#...`) leading to step details failure when individual execution JSON files are missing.
- **Root Cause:**
  1. `renderStepsForExecution` called `startLiveStepRefreshTimer` synchronously on every render while `startLiveStepRefreshTimer`'s `poll()` also re-scheduled itself in `.finally()`, causing exponential timer proliferation whenever step updates occurred during polling.
  2. Fixed-interval `setInterval` left insufficient idle breathing room under heavy payloads (~2.4s transfer on 2.5s interval), causing near-continuous network utilization.
  3. `isTerminalStatus` omitted `cancelled`, `canceled`, and `stopped` lifecycle terminal states, causing polling to continue indefinitely on aborted runs.
  4. Initial lazy load fetch in `renderStepsForExecution` set `activeRow.dataset.stepsFetched = 'true'` before network resolution completed, permanently blocking retry on transient fetch error.
  5. `AuraReportDataService.getExecutionDetails` fallback paths (`run.json` `executionMetrics`, `runReportCache`, and `liveRunBuffer`) matched only against fully-qualified class names, failing when requested with short class composite keys (`SimpleClass#method#...`).
- **Detection Gap ("What did we miss?"):** Previous fix for DEF-20261104-0545 introduced `startLiveStepRefreshTimer` and `isTerminalStatus` but lacked chained `setTimeout` scheduling, missed abort controller cleanup on row change, and did not test fallback matching for simple class keys against cached and live execution buffers.
- **Resolution:**
  1. Refactored `startLiveStepRefreshTimer` to use self-scheduling chained `setTimeout(poll, 2500)` in `.finally()` with monotonic fetch IDs (`activeStepFetchId`) and `AbortController` cancellation.
  2. Decoupled `renderStepsForExecution` from timer scheduling; timers are only started upon initial panel opening or successful initial lazy-load fetch.
  3. Stamped `data-steps-fetched="true"` exclusively within `.then()` upon verified payload receipt.
  4. Extended `isTerminalStatus` to recognize `success`, `cancel`, and `stop` prefix variants.
  5. Enhanced `AuraReportDataService.getExecutionDetails` with `extractSimpleExecutionKey` and `matchesExecution` supporting `simpleFullKey`, `simpleExecKey`, `simpleExecKeyWithId`, and `simpleClassMethodBrowser`.
- **Safety Net Added:** `StaticResourceSyntaxTest.testIsTerminalStatusRecognizesNormalizedStatusesAndHandlesAbort` (validating status taxonomy and abort logic via Node.js VM) and `RunStorageSyncServiceTest.testExecutionDetailsResolvesViaRunJsonFallbackWithSimpleKey` & `testExecutionDetailsResolvesViaLiveBufferWithSimpleKey`.
