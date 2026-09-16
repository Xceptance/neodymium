# Defect Log & Post-Mortem Registry

This document tracks diagnosed defects, regressions, and behavioral bugs encountered during development and testing across Neodymium, its test suites, and connected Systems Under Test (SUTs).

The objective is to maintain an actionable learning record: to understand **why** defects occurred, identify detection gaps (**"what did we miss?"**), and ensure safety nets (regression tests, linters, or architectural assertions) prevent recurrence.

---

## Logging Guidelines & Criteria

### When to Log a Defect (In Scope)
- **Framework Regressions & Logic Bugs**: Any semantic bug in Neodymium core, AI engine, state machine, runners, parsers, or reporters.
- **Test Harness / Fixture Failures**: Faulty assertions, broken test setups, incorrect wait conditions, or selector fragility that caused false positives or false negatives.
- **SUT Behavioral Defects**: Confirmed functional, visual, or layout defects detected in the System Under Test (e.g., Verla demo store).
- **Silent Failures or Masked Exceptions**: Situations where errors were swallowed or misclassified.

### When NOT to Log (Out of Scope)
- **Normal TDD Red Phase**: Expected test failures during active test-first development prior to implementing the feature.
- **In-Progress Compilation / Syntax Typos**: Errors resolved during the immediate editing cycle.
- **Transient External Outages**: Temporary network loss, upstream LLM provider 503/429 quota limits, or local OS process termination.

---

## Defect Entry Template

When recording a defect, add a new entry directly under the [Active Defect Records](#active-defect-records) section in **reverse chronological order** (newest entries first).

```markdown
### [DEF-YYYYMMDD-01] Concise Description of Defect
- **Date:** YYYY-MM-DD
- **Component:** `neodymium-core` / `aura-visual` / `playbook-engine` / `verla-fixture` / etc.
- **Scope:** `Framework` | `Test/Harness` | `SUT`
- **Symptom:** Observed failure, error message, or unexpected behavior.
- **Root Cause:** Technical explanation of why the defect occurred.
- **Detection Gap ("What did we miss?"):** Why existing unit tests, linters, or type systems failed to catch this earlier.
- **Resolution:** Summary of code changes made to resolve the issue.
- **Safety Net Added:** Reference to the regression test, assertion, or linter rule preventing recurrence.
```

---

## Active Defect Records

### [DEF-20260916-02] Pre-Action Visual Baseline Check Prematurely Aborted Replay for Visual Steps with Mutating Actions
- **Date:** 2026-09-16
- **Component:** `neodymium-core` (`ai-pipeline`)
- **Scope:** `Framework`
- **Symptom:** In `SearchTest_German_testSearchDeReplayDe_DE`, step 11 (`Auf der linken Seite wird eine Box mit Kategorien, Farben, Preis und Angebot angezeigt (visual).`) failed during replay with `DivergenceException: Visual SSIM score below threshold (score: 0.5352 < 0.99)` because the page was not scrolled down.
- **Root Cause:**
  1. During live recording, the LLM executed an interactive/viewport action `scroll(direction: "down", yOffset: 300)`. `VerifyOutcomeStep` captured the post-action screenshot at `scrollY = 300` and saved its SSIM matrix as `step.screenshotHash`.
  2. During replay, `VisualBaselineGateStep.executeGate()` was invoked before replaying step actions (at `scrollY = 0`).
  3. `VisualBaselineGateStep` assumed all visual steps (`step.isVisualStep()`) were pure verification steps and evaluated live pre-action screen state against `step.screenshotHash`. When the pre-action screen did not match the post-action baseline (SSIM 0.5352 < 0.99), line 235 threw a `DivergenceException` immediately, preventing `PlaybookToolReplayer.replayStep()` from ever executing the recorded `SCROLL` action.
- **Detection Gap ("What did we miss?"):**
  - Existing unit tests for `VisualBaselineGateStep` only tested visual steps with zero actions or with `ASSERT` actions (`Action("ASSERT", ...)`); none tested steps containing mutating or viewport actions like `SCROLL` or `CLICK`.
  - The pipeline assumed `step.isVisualStep()` implied a pure verification step with no mutating actions, ignoring cases where an LLM calls non-mutating tools or scroll actions during visual assertions.
- **Resolution:**
  1. Added `VisualBaselineGateStep.isPureVerification()` to distinguish steps that only contain assertions/NO-OPs from steps that contain recorded mutating actions or tool calls (`SCROLL`, `CLICK`, etc.).
  2. Updated `VisualBaselineGateStep.executeGate()` to defer visual baseline comparison when `!isPureVerification() && coordinateTarget == null`, allowing recorded actions to execute first without throwing pre-action divergence.
  3. Added `VisualBaselineGateStep.executePostActionCheck()` and wired it into `ExecuteActionsStep`'s `standardFlow` after action replay and post-step state settling/capture to verify the post-action visual outcome against the recorded baseline.
- **Safety Net Added:**
  - Added 4 unit tests in `VisualBaselineGateStepTest`:
    - `testReplayWithMutatingAction_preActionGateDoesNotThrowDivergence()`
    - `testReplayWithMutatingAction_postActionCheckWithMatchingBaseline_succeeds()`
    - `testReplayWithMutatingAction_postActionCheckWithDivergentBaseline_throwsDivergenceException()`
    - `testReplayWithMutatingAction_postActionCheck_supportsHealing_throwsHealingRequiredException()`
  - Added 2 integration tests in `ExecuteActionsStepTest`:
    - `testReplayVisualStepWithActionExecutesActionThenVerifiesPostActionVisualBaseline()`
    - `testReplayVisualStepWithActionThrowsDivergenceWhenPostActionVisualBaselineDiffers()`

### [DEF-20260916-01] Full-Page Visual Assertion Baseline Recorded As Viewport Capture During Multi-Turn Tool Loops
- **Date:** 2026-09-16
- **Component:** `neodymium-core` (`ai-pipeline`)
- **Scope:** `Framework`
- **Symptom:** In `VerlaGuestCheckout_Pl_Polish_testCheckoutReplayPerfect`, step 19 (`Na środku ekranu znajduje się zielony znacznik wyboru (visual: full).`) failed during replay with SSIM 0.6878 against the recorded baseline (required >= 0.99), causing a `DivergenceException`.
- **Root Cause:**
  1. In `AgentToolLoopStep`, for visual assertions, `activeContextLevel` is `ContextLevel.VISUAL`, where `ContextLevel.isFullPageScreenshot()` evaluates to `false`. When an agent loop required multiple turns (e.g. recovering from an invalid tool call), subsequent turns refreshed page state using `executor.captureState(activeContextLevel, activeContextLevel.isFullPageScreenshot())`. This captured a 1500x857 viewport screenshot and overwrote `KEY_LAST_STATE`.
  2. In `VerifyOutcomeStep`, for steps without mutating DOM actions (`step.getActions().isEmpty()`), the pipeline previously discarded `KEY_POST_ACTION_STATE` (which had been freshly captured as full-page by `ExecuteActionsStep`) and fell back to `KEY_LAST_STATE` (overwritten with the viewport capture). It hashed that viewport capture while marking `step.setFullPage(true)`.
  3. During replay, `VisualBaselineGateStep` checked `step.isFullPageVisualStep()`, correctly captured a 1500x1122 full-page screenshot, and compared it against the recorded 1500x857 viewport hash, causing SSIM 0.6878 < 0.99 (`DivergenceException`).
- **Detection Gap ("What did we miss?"):**
  - Existing test `testVisualStepDoesNotConsumeStalePostActionState` in `VerifyOutcomeStepTest` specifically tested that viewport visual steps without actions discard `KEY_POST_ACTION_STATE` in favor of viewport `KEY_LAST_STATE`, but did not test full-page visual steps (`(visual: full)` or `step.isFullPageVisualStep()`).
  - Unit tests for `AgentToolLoopStep` did not verify the `isFullPage` flag passed to `captureState` during intermediate turns.
- **Resolution:**
  1. Updated `AgentToolLoopStep` lines 357, 1086, and 1195 so that initial, intermediate, and visual observation state captures evaluate `isFullPage` considering `(step != null && step.isFullPageVisualStep()) || Boolean.TRUE.equals(context.getTransientData().get("KEY_IS_FULL_PAGE_SCREENSHOT")) || activeContextLevel.isFullPageScreenshot()`.
  2. Updated `VerifyOutcomeStep` lines 116-124 to preserve and consume `KEY_POST_ACTION_STATE` whenever `isFullPageReq` is true, even when `step.getActions()` is empty.
- **Safety Net Added:**
  - Added unit test `testVisualStepWithFullPagePreservesPostActionState` in `VerifyOutcomeStepTest`.
  - Added unit test `testMultiTurnFullPageVisualStepPreservesFullPageCapture` in `AgentToolLoopStepTest`.
  - Added full-page capture tracking (`capturedFullPageFlags`) in `MockTargetExecutor`.
