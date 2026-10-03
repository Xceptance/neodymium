# [DEF-20261001-04] Strict Replay Suffers Massive Latency Penalty Due to Redundant DOM Serialization and Routine Visual Captures

- **Status:** Resolved
- **Opened:** 2026-10-01
- **Closed:** 2026-10-01
- **Component:** `neodymium-core` (`ExecuteActionsStep`, `PlaybookStep`, `AiConfiguration`)
- **Scope:** `Framework`
- **Symptom:** Replaying test playbooks in `REPLAY_STRICT` mode took nearly as long as initial LLM recording runs (e.g. 125s vs 171s), despite 100% LLM bypass ($0.00 cost, 0 LLM calls).
- **Root Cause:**
  1. `ExecuteActionsStep` unconditionally executed `executor.captureState(ContextLevel.STANDARD)` before every step (originally intended for data-ai stamping and healing fallback). In `REPLAY_STRICT`, healing is disabled (`mode.supportsHealing() == false`), and locators targeting automation IDs (`[data-ai="..."]`) are stamped dynamically on-demand by `SelenideElementFinder.tryResolveAutomationId` if absent.
  2. `ExecuteActionsStep` unconditionally executed `executor.captureState(ContextLevel.VISUAL)` after every single step (for preliminary HTML report screenshot attachments). Routine action steps during strict replay do not perform visual regression gating, wasting 1.5s–2.0s per step on DOM tree serialization and DevTools screenshot fallbacks.
  3. `PlaybookStep.isVisualStep()` only checked instruction text patterns, omitting `Boolean.TRUE.equals(this.fullPage)`. This created architectural inconsistency where `isFullPageVisualStep()` was true but `isVisualStep()` was false, forcing call-sites to defensively write `isVisualStep() || isFullPageVisualStep()`.
- **Detection Gap ("What did we miss?"):** Unit tests for `ExecuteActionsStep` used in-memory `MockTargetExecutor` where `captureState` completes instantaneously without WebDriver screenshotting, DevTools fallback, or full DOM tree serialization.
- **Resolution:**
  1. Updated `PlaybookStep.isVisualStep()` to check `Boolean.TRUE.equals(this.fullPage)` first, ensuring every full-page visual step is guaranteed to be a visual step and centralizing visual classification.
  2. Added `neodymium.ai.replay.leanStateCapture` (default: `true`) and `neodymium.ai.replay.captureScreenshots` (default: `false` in `REPLAY_STRICT`) in `AiConfiguration`.
  3. Updated `ExecuteActionsStep` to bypass pre-step DOM traversal during replay when lean state capture is active or healing is unsupported, while lazily capturing state in `HealingRequiredException` if healing is ever triggered.
  4. Conditioned post-action visual capture on `isVisualRequired` (`step.isVisualStep() || isSemanticVerificationEnabled() || !isReplay || isReplayScreenshotCaptureEnabled(mode)`), eliminating routine screenshots on passing non-visual replay steps while preserving visual baseline gating and failure state capture in `StateMachineRunner`.
- **Safety Net Added:** Added `testReplayStrictNonVisualStepBypassesStateCapture` and `testReplayStrictVisualStepCapturesVisualState` in `ExecuteActionsStepTest.java`, and verified `isVisualStep` consistency in `PlaybookStepFullPagePersistenceTest.java`.
