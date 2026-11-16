# [DEF-20261116-0635] Replay Mode Captures Routine Passing Screenshots Causing Multi-Megabyte Report Bloat and Execution Slowdown

- **Status:** `Resolved`
- **Opened:** 2026-11-16 06:35
- **Closed:** 2026-11-16 06:40
- **Component:** `neodymium-core`
- **Scope:** `Framework & Config/Environment`
- **Symptom:** Recorded replay runs (such as `Aura_tests_posters_homepage_yaml_Test`) with mostly assertion/validation steps took over 8 minutes to execute instead of seconds. In addition, memory and disk report outputs ballooned to over 73MB JSON / 110MB HTML, resulting in 30+ seconds spent repeatedly re-serializing massive JSON/HTML files at test completion.
- **Root Cause:** In `AiConfiguration.isReplayScreenshotCaptureEnabled(mode)`, default screenshot capture during replay was enabled for any mode except `ExecutionMode.REPLAY_STRICT`. When running in the default `ExecutionMode.REPLAY_WITH_HEALING` (the mode automatically selected by `AUTO` when a companion recording is detected), the check returned `true`. Consequently, `ExecuteActionsStep` executed `executor.captureState(ContextLevel.VISUAL)` after every single routine passing replay step, taking a full browser screenshot, serializing DOM structures, and base64-encoding images into `ExecutionContext` and the playbook report model.
- **Detection Gap ("What did we miss?"):** Previous replay optimizations assumed only `REPLAY_STRICT` was lean, assuming `REPLAY_WITH_HEALING` needed full visual captures after every step in case downstream healing required historical states. However, only visual/layout verification steps, semantic verifications, failing steps, or healed steps require visual state. Routine passing steps in all replay modes should default to lean execution unless explicitly configured via `neodymium.ai.replay.captureScreenshots=true`.
- **Resolution:**
  1. Updated `AiConfiguration.isReplayScreenshotCaptureEnabled(ExecutionMode mode)` so all replay modes (`mode != null && !mode.isReplay()`) default to `false`. Replay screenshot capture can still be toggled globally via `neodymium.ai.replay.captureScreenshots=true`.
  2. Updated `ExecuteActionsStep.java` so that post-step visual state capture (`executor.captureState(ContextLevel.VISUAL)`) is only performed if `step.isVisualOrLayoutStep()`, semantic verification is enabled, execution is in live mode (`!isReplay`), the step required healing (`ExecutionContext.KEY_IS_HEALED_STEP`), or `isReplayScreenshotCaptureEnabled(mode)` is explicitly true.
- **Safety Net Added:**
  - Added unit test `AiConfigurationTest.testIsReplayScreenshotCaptureEnabledDefaultsAndOverrides` validating default `false` for `REPLAY_STRICT` and `REPLAY_WITH_HEALING`, default `true` for live/force-recording modes, and explicit override behavior.
  - Added unit tests `ExecuteActionsStepTest.testReplayPassingNonVisualStepDoesNotCaptureVisualStateByDefault` and `ExecuteActionsStepTest.testReplayPassingVisualStepCapturesVisualState` validating that routine passing steps skip visual state capture while visual steps capture post-action state and dispatch `StateCapturedEvent`.
