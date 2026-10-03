# [DEF-20260922-01] Missing Replayed Step Count Tracking in ExecuteActionsStep Causing hasAllStepsReplayed Assertion Failure

- **Status:** Resolved
- **Opened:** 2026-09-22
- **Closed:** 2026-09-22
- **Component:** `neodymium-core` (`ai-pipeline`, `ai-replay`, `metrics`)
- **Scope:** `Framework`
- **Symptom:** In `AssertIntegrationTest` and any tests asserting `hasAllStepsReplayed()` on replay, `hasAllStepsReplayed()` failed with `AssertionError: Not all steps were replayed from cache. ==> expected: <N> but was: <0>`.
- **Root Cause:**
  `AiSession.getMetrics()` retrieves `replayedStepCount` from `ExecutionContext.KEY_TOTAL_REPLAYS`. In `ExecuteActionsStep`, replaying steps via `PlaybookToolReplayer.replayStep(...)` or bypassing them via `VisualBaselineGateStep` executed the actions against the SUT but never incremented `ExecutionContext.KEY_TOTAL_REPLAYS`. Consequently, `KEY_TOTAL_REPLAYS` remained 0 across all successful replay steps.
- **Detection Gap ("What did we miss?"):**
  Unit tests for `MetricsAsserter` and `ExecutionMetrics` used manually constructed instances with mocked non-zero `replayedStepCount` values. Pipeline integration tests focused on step completion rather than asserting that the runtime metrics asserter counted real pipeline replay steps.
- **Resolution:**
  1. In `ExecuteActionsStep.java`, increment `ExecutionContext.KEY_TOTAL_REPLAYS` upon successful completion of `PlaybookToolReplayer.replayStep(...)`.
  2. In `ExecuteActionsStep.java`, increment `ExecutionContext.KEY_TOTAL_REPLAYS` when a step is visually verified and bypassed via `VisualBaselineGateStep` in replay mode.
- **Safety Net Added:**
  Regression test `ExecuteActionsStepTest#testReplayedStepCountIncrementedOnReplay` and `AssertIntegrationTest#testAssertUrl` verifying that `hasAllStepsReplayed()` passes with exact step count equality on replay runs.
