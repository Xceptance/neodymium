# [DEF-20261003-02] Visual Marker Click Causes Unrecorded Scroll Resulting in Coordinate Replay Drift

- **Status:** Resolved
- **Opened:** 2026-10-03
- **Closed:** 2026-10-03
- **Component:** `neodymium-core` (`BrowserToolProvider`, `VisualBadgeInjector`)
- **Scope:** `Framework`
- **Symptom:**
  Replaying recorded playbook steps that use visual markers (`marker:N` or `badge:N`) on elements below the fold failed because coordinate clicks missed the target element during `REPLAY_STRICT` and `REPLAY_WITH_HEALING` modes, leading to assertion timeouts on subsequent steps.
- **Root Cause:**
  `BrowserToolProvider.clickBadgeScript` invoked `el.scrollIntoView({ block: 'center', inline: 'center' })` when calculating marker coordinates. This performed an unrecorded page scroll during recording, capturing post-scroll coordinates. During replay, only recorded actions ran (e.g. `scroll` on parent card container), so the unrecorded element scroll never occurred, causing the recorded coordinate click to land off-target (e.g. y=313 instead of y=426).
- **Detection Gap ("What did we miss?"):**
  Earlier visual marker tests (`VisualMarkersSandboxLiveTest`) targeted controls at the top of the page (`#card-hoodie`) which were already inside the initial viewport, where `scrollIntoView` caused zero scroll offset.
- **Resolution:**
  Removed `scrollIntoView` calls from `clickBadgeScript` in `BrowserToolProvider.java`. Visual markers are already verified visible in the viewport when injected by `VisualBadgeInjector`. The element's current `getBoundingClientRect()` accurately represents its location without altering page scroll state.
- **Safety Net Added:**
  Dense layout multi-mode live test `VisualMarkersComplexSandboxLiveTest` exercising `FORCE_RECORDING`, `REPLAY_STRICT`, and `REPLAY_WITH_HEALING` across below-the-fold controls.
