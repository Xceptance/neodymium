# [DEF-20261012-1729] Transient Visual Escalation Sets Persistent fullPage Flag Causing Viewport vs Full-Page SSIM Mismatch on Replay

- **Status:** Resolved
- **Opened:** 2026-10-12 17:29
- **Closed:** 2026-10-12 17:32
- **Component:** `neodymium-core` (`ai-pipeline`)
- **Scope:** `Framework`
- **Symptom:** In `CartTest.replayAllDataSets` with dataset `normal`, step 2 (`Locate the first product card`) replayed all 5 recorded DOM actions (`STORE`, `HOVER`, `CLICK`, `CLICK`, `ASSERT_TEXT`) successfully, but then failed during replay with `DivergenceException: Visual SSIM score below threshold (score: 0.4908 < 0.99)` for visual instruction.
- **Root Cause:**
  1. In `VerifyOutcomeStep.java`, visual baseline hashing was triggered whenever `activeLevel.includesScreenshot()` was true, treating non-visual interactive DOM steps that escalated to `VISUAL_LEAN` during live execution as visual steps.
  2. In `VerifyOutcomeStep.java:265`, the step was flagged with `step.setFullPage(true)` because `activeLevel.isFullPageScreenshot()` evaluated to `true` for `VISUAL_LEAN`, even though `KEY_POST_ACTION_STATE` had been captured by `ExecuteActionsStep` as a 1500x857 viewport screenshot (`isFullPageReq` was `false`) and the recorded `screenshotHash` was a viewport hash.
  3. Setting `step.setFullPage(true)` caused `PlaybookStep.isVisualStep()` and `PlaybookStep.isFullPageVisualStep()` to both return `true` on replay.
  4. On replay, `ExecuteActionsStep` captured a 1500x2117 full-page screenshot and registered `VisualBaselineGateStep::executePostActionCheck`.
  5. `VisualBaselineGateStep.executePostActionCheck` compared the 1500x2117 full-page replay screenshot against the recorded 1500x857 viewport SSIM matrix, yielding SSIM 0.4908 < 0.99 and failing replay.
- **Detection Gap:**
  Prior unit tests (`VerifyOutcomeStepTest.testCoordinateStepEnforcesViewportScreenshotAndClearsFullPage`) only guarded coordinate targets (`coordinateTarget != null`) from receiving `fullPage=true`, but did not test that regular interactive DOM steps with selector targets that escalate to `VISUAL_LEAN` do not receive `fullPage=true` or record visual baseline hashes.
- **Resolution:**
  1. In `VerifyOutcomeStep.java`, restricted visual execution baseline hashing to steps that are genuinely visual/layout steps or have coordinate actions: `final boolean isVisualExecution = (step != null && step.isVisualOrLayoutStep()) || coordinateTarget != null;`.
  2. In `VerifyOutcomeStep.java`, set `step.setFullPage(isFullPageReq);`, directly tying `fullPage` persistence to whether full-page capture was actually requested and taken, eliminating the conflation with transient `activeLevel.isFullPageScreenshot()`.
- **Safety Net Added:**
  Added unit tests in `VerifyOutcomeStepTest.java`:
  - `testInteractiveStepEscalatingToVisualLeanDoesNotRecordBaselineOrSetFullPage`: Verifies that selector-based interactive steps escalating to `VISUAL_LEAN` do not record a baseline hash or set `fullPage=true`.
  - `testViewportVisualStepEscalatingToVisualLeanEnforcesViewportFullPageFalse`: Verifies that viewport-scoped visual steps (`(visual)`) evaluated while context level is `VISUAL_LEAN` record a viewport hash and explicitly enforce `fullPage=false`.
