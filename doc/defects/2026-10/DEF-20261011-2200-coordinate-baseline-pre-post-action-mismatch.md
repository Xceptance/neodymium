# [DEF-20261011-2200] Coordinate SSIM Baseline Gate Pre-vs-Post Action Mismatch and Reporter Leakage

- **Status:** `Resolved`
- **Opened:** 2026-10-11 22:00
- **Closed:** 2026-10-11 22:25
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In strict replay mode (`REPLAY_STRICT`), steps containing coordinate actions fail prematurely with `Coordinate SSIM tile mismatch (score: 0.79 < 0.95). Aborting coordinate click to prevent blind misclick.` The HTML report simultaneously displays conflicting information: a red `FAILED` step pill alongside a green `🔍 VERIFIED` badge, while blaming the last sub-step with phantom live-run durations.
- **Root Cause:** 
  1. `VerifyOutcomeStep` computes the tile SSIM matrix from the post-action state after all step mutations have completed, storing it into `step.getScreenshotHash()`. On replay, `VisualBaselineGateStep.executeGate()` attempted to validate this hash pre-action for any step where `coordinateTarget != null` (due to an erroneous `&& coordinateTarget == null` check when deferring mutating steps), comparing pre-mutation browser pixels to post-mutation baseline pixels.
  2. In `VisualBaselineGateStep.executePostActionCheck()`, coordinate tile cropping was missing; it extracted a full-page SSIM matrix instead of the 64x64 coordinate tile, causing dimension and score divergence against tile baselines.
  3. In `PreliminaryReportListener`, `targetStep.setVerificationResult(pbStep.getVerificationResult())` unconditionally copied over the live recording's successful verification result into replay reports even when the step failed (`!isStepSuccess`), causing `HtmlReportGenerator` to render a green `🔍 VERIFIED` badge next to the red `FAILED` pill.
  4. In `PreliminaryReportListener`, pending sub-steps of a failed parent step were erroneously marked as `SUCCESS` (line 693), unexecuted sub-steps had recorded live durations copied into them, and line 699 arbitrarily blamed the last sub-step with the parent step's gate failure reason even when no actions ran.
- **Detection Gap ("What did we miss?"):** Existing `VisualBaselineGateStepTest` unit tests only tested non-coordinate mutating steps (e.g. `Action("SCROLL", "", "down")`) or pure verification coordinate steps, missing mutating steps that contain coordinate click actions. Existing `PreliminaryReportListenerTest` unit tests only asserted successful replay runs or steps with action-level failures, never verifying step-level pre-action gate aborts where zero actions ran.
- **Resolution:** 
  1. In `VisualBaselineGateStep.java`, unconditionally defer visual baseline checking for all mutating steps (`!pureVerification`), allowing actions to execute before verifying visual outcome.
  2. In `VisualBaselineGateStep.java`, add coordinate tile cropping support to `executePostActionCheck()` using the recorded coordinate target and `0.95` minimum threshold.
  3. In `PreliminaryReportListener.java`, suppress `verificationResult` if `!isStepSuccess`, mark unexecuted sub-steps of failed parent steps as `SKIPPED` with 0 ms duration, and avoid misattributing parent gate failures to trailing child sub-steps when zero actions executed.
  4. In `HtmlReportGenerator.java`, guard against rendering `verification-badge-pass` on steps with status `FAILED`.
- **Safety Net Added:** 
  - Added `VisualBaselineGateStepTest.testReplayWithMutatingCoordinateAction_preActionGateDefersAndDoesNotThrowDivergence()` and `testReplayWithMutatingCoordinateAction_postActionCheckWithMatchingBaseline_succeeds()`.
  - Added `PreliminaryReportListenerTest.testStepGateFailureSuppressesVerificationResultAndMarksSubStepsAborted()`.
