# [DEF-20261001-11] Queue Execution Report Total Duration Displays Multimillion Minutes Due to Unfiltered Zero Timestamps

- **Status:** Resolved
- **Opened:** 2026-10-01
- **Closed:** 2026-10-01
- **Component:** `aura-manager` (`RunReportDto`)
- **Scope:** `Framework`
- **Symptom:** Total execution duration for queue runs (`run_20261001_12564`) showed invalid values such as `29847542 min 38 s` instead of actual wall-clock execution time (~4-5 min).
- **Root Cause:** `RunReportDto.getTotalDurationMs()` evaluated `(maxStartMs - minStartMs) + latestExec.getDurationMs()` without filtering out uninitialized or missing start timestamps (`startMs = 0L`, Jan 1 1970). When an uninitialized execution stub or execution snapshot with `0L` timestamp was present, `minStartMs` was set to `0L`, causing the subtraction `maxStartMs - 0L` to evaluate to the current Epoch timestamp (~1.79x10^12 ms = 29,847,542 minutes).
- **Detection Gap ("What did we miss?"):** Existing unit tests for `RunReportDto` verified total duration only with valid mock timestamps or single executions, missing test coverage for queue runs where some execution snapshots have `0L` start timestamps.
- **Resolution:** Updated `RunReportDto.getTotalDurationMs()` to filter out invalid start timestamps (`startMs <= 0L`) when calculating wall-clock spans and fall back to the sum of test execution durations when valid start timestamps are missing or insufficient.
- **Safety Net Added:** Added unit test `testGetTotalDurationMs_ignoresZeroTimestampAndFallsBackToSum` in `RunReportDtoTest.java`.
