# [DEF-20261104-0545] Step Details Load Failure Due to Polling Storm, Duplicate DTO Serialization, and Status Mismatch

- **Status:** `Resolved`
- **Opened:** 2026-11-04 05:45
- **Closed:** 2026-11-04 06:15
- **Component:** `aura-manager` / `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** Side panel displays initial test steps briefly before suddenly failing with "Failed to load step details for this test execution." Also occurs when inspecting completed tests in the reporting section.
- **Root Cause:**
  1. `report-manager.js` invokes `startLiveStepRefreshTimer` without an `isStepFetchInFlight` mutex guard, firing overlapping 1-second polling requests for 15-35MB JSON payloads, exhausting browser HTTP connections.
  2. The polling timer's terminal condition (`isTerminal`) checks exact string equality against `'PASSED'`/`'FAILED'`, failing to recognize Neodymium's hyphenated status taxonomy (`passed-clean`, `failed-assertion`, etc.), so polling continues indefinitely even on finished runs.
  3. `TestExecutionDto.java` serializes both `blocks` and `blocksJson` (and `steps`/`stepsJson`) due to missing `@JsonIgnore`, doubling the serialized HTTP response size to 40-60MB.
  4. The frontend `.catch()` error handler destructively clears the DOM (`stepListEl.innerHTML = ...`), wiping out previously rendered steps upon any transient network abort or timeout.
  5. `AuraReportDataService.getExecutionDetails` misses file matches when `console-execution-*.json` lacks stamped top-level metadata, falling back to summary-only `run.json` entries (`steps: null`).
- **Detection Gap ("What did we miss?"):** Unit tests verified DTO conversion and single-call step fetching with small 1-step mock payloads, missing the multi-step multi-megabyte payload sizes and concurrent polling behavior present in real live runs.
- **Resolution:**
  1. Added in-flight request lock (`isLiveStepFetchInFlight`) and increased poll interval to 2.5s in `report-manager.js`.
  2. Updated `isTerminal` check with `isTerminalStatus` helper to handle hyphenated statuses (`passed-*`, `failed-*`, `succeeded-*`, etc.) and skipped starting the timer altogether for terminal rows.
  3. Preserved rendered steps on fetch failure in `.catch()`; only displaying error message if the container has no existing step content.
  4. Added `@JsonIgnore` to duplicate JSON getters `getBlocksJson()` and `getStepsJson()` in `TestExecutionDto.java`.
  5. Ensured top-level execution attributes (`testClass`, `testMethod`, `title`) are stamped into `console-execution-*.json` in `InteractiveStateBuilder` and added directory/file fallback matching in `AuraReportDataService.getExecutionDetails`.
- **Safety Net Added:** Unit tests in `TestExecutionDtoTest` verifying `@JsonIgnore` excludes `blocksJson` and `stepsJson` from serialization, and tests in `AuraReportDataServiceTest` verifying fallback execution resolution.
