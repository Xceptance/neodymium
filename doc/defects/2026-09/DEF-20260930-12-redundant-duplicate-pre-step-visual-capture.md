# [DEF-20260930-12] Redundant Duplicate Pre-Step Visual Capture and Blind Settle Sleep in `ExecuteActionsStep`

- **Status:** Resolved
- **Opened:** 2026-09-30
- **Closed:** 2026-09-30
- **Component:** `neodymium-core` (`ExecuteActionsStep`)
- **Scope:** `Framework`
- **Symptom:** Every test step transition incurred an average 3.95s dead gap (~90.8s total overhead across 23 transitions) between goal completion and starting the next step.
- **Root Cause:**
  1. `ExecuteActionsStep` captured back-to-back screenshots: post-step visual state capture at the end of Step N (~1.2s) followed immediately by pre-step visual state capture at the start of Step N+1 (~1.0s) across identical, unchanged browser states.
  2. `ExecuteActionsStep` executed an unconditional `Thread.sleep(1000)` post-action settle pause on all mutating steps regardless of whether visual baselines were requested or whether DOM quiescence had already settled.
- **Detection Gap ("What did we miss?"):** End-to-end timing tests only evaluated step-internal durations, failing to track inter-step lifecycle transitions and screenshot redundancy.
- **Resolution:**
  1. Updated `ExecuteActionsStep` to reuse the preceding step's `POST_ACTION_STATE` as the current step's `PRE_ACTION_STATE` when not in full-page capture mode, eliminating duplicate screenshot capture.
  2. Conditioned the full post-action settle sleep on `step.isVisualStep()`, delegating non-visual mutating steps to `DomQuiescenceWatcher.waitForDomQuiet(Duration.ofMillis(200), Duration.ofMillis(50))` to confirm stability without blind multi-second sleep.
- **Safety Net Added:** Unit tests in `ExecuteActionsStepTest` asserting state reuse across step boundaries and quiescence integration.
