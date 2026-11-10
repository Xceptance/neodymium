# [DEF-20261007-1335] PreliminaryReportListener Single-Use Flush Latch Ignores Failures in Multi-Execute Sessions

- **Status:** `Resolved`
- **Opened:** 2026-10-07 13:35
- **Closed:** 2026-10-07 13:50
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In programmatic test cases invoking `AiSession.execute(...)` multiple times within a single test method, only the steps and outcome of the first execution block were persisted to the generated HTML, JSON, and Markdown reports. If a subsequent `execute()` block failed, the test method failed JUnit, but the disk reports remained frozen in the state of the first block (falsely reporting `PASSED` with a truncated step list), completely masking the terminal failure and all subsequent steps.
- **Root Cause:** `PreliminaryReportListener` utilized a single-use latch (`reportFlushed = new AtomicBoolean(false)`). When Phase 1 completed successfully, `SessionFinishedEvent` triggered `flushReport()`, setting `reportFlushed` to `true` and writing the disk reports. When Phase 2 executed and failed, `StateMachineRunner` fired a subsequent `SessionFinishedEvent(success=false)`. However, `flushReport()` executed `if (!this.reportFlushed.compareAndSet(false, true)) return;`, silently discarding the update. Furthermore, `PreliminaryReportListener` did not preserve the base file name across updates or accumulate cumulative status, duration, and metrics across sequential session lifecycles.
- **Detection Gap ("What did we miss?"):** When programmatic multi-execute step accumulation was hardened in `DEF-20261004-1410`, unit tests verified that `AiSession` accumulates steps in memory and replays them sequentially, but did not assert disk report generation or listener behavior across multiple sequential `SessionFinishedEvent` emissions where later blocks fail.
- **Resolution:**
  1. Updated `PreliminaryReportListener` to allow re-flushing on subsequent `SessionFinishedEvent`s, ensuring that reports are rewritten with updated cumulative data when later blocks finish or fail.
  2. Preserved the initial base file name in `lastBaseFileName` so that subsequent flushes overwrite and update the exact same report artifacts (`.html`, `.json`, `.md`) and refresh `index.html`.
  3. Cumulative session status tracking: `this.report.setSuccess(cumulativeSuccess)` ensures that if any execution block fails, the overall test report status transitions to `FAILED`.
  4. Cumulative duration calculation: total test duration is computed from `this.report.getStartTimeMs()` up to the final session finish event.
- **Safety Net Added:** Added automated unit test `PreliminaryReportListenerTest.testMultiSessionExecuteAccumulatesStepsAndFlushesFinalFailure` simulating sequential `SessionFinishedEvent`s (first passing, second failing) and asserting the resulting report file reflects cumulative steps and `status = FAILED`.
