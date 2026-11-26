# [IDEA-20261004-0045] Replay guard for coordinate-based marker clicks

- **Status:** `Implemented`
- **Proposed:** 2026-10-04 00:45
- **Resolved:** 2026-10-04 01:31
- **Component:** `neodymium-core`
- **Category:** `AI & VLM`
- **Author:** AI-generated: Claude Sonnet 5.5 / Xceptance GmbH 2026

## 1. Problem Statement & Motivation
Since commit `dc09be33`, a click on a visual marker (`marker:N` / `badge:N`) is recorded as `target: "coord: x,y"` with the selector removed (`BrowserToolProvider` marker branch, `AgentToolLoopStep` ~L803-816). On replay this click has no element identity at all:

- `BrowserToolProvider` Case 2 (selector) is skipped for `coord:` targets, and `PlaybookToolReplayer.attemptHealing` returns early for them, so vector healing never runs.
- `VisualBaselineGateStep` only guards when a `screenshotHash` was recorded, and `VerifyOutcomeStep` only runs for `(visual)` / `(layout)` steps or with semantic verification on. The run artifact `VisualMarkersSandboxMockTest_testVisualMarkersMock_20261003-224030.json` shows `CLICK target "coord: 165,521"` and an empty `screenshotHash`.

If the layout shifts, the click lands on whatever is at those coordinates and replay can pass without noticing. The coordinate form was a deliberate choice ("prevent unrecorded scroll in marker coordinate clicks to maintain replay precision"), so this is a design gap, not a simple regression.

## 2. Proposed Architecture & Design
Keep the coordinate click and add an identity check:

1. At record time, resolve the element under the marker centre with `ReanchoringBridge.resolveElementAtPoint` **before** the click (the overlay has `pointer-events: none`, so `elementFromPoint` sees the real element) and record its `domFeatureVector` on the action.
2. At replay time, before a `coord:` click, call `elementFromPoint` at the recorded point and compare tag and text with the vector. On mismatch, search the live candidates with `LocatorCascadeResolver.findBestMatch` (healing modes) or fail (`REPLAY_STRICT`).

## 3. Expected Impact & Trade-offs
- **Benefits:** Silent mis-clicks after layout shifts become either a heal or a clear failure; no change to the click mechanics.
- **Risks & Complexity:** Touches recording and replay code paths; needs a mock-tier test that moves the target between record and replay. Re-anchoring after the click, as the pre-`dc09be33` code did, is not equivalent: the click may navigate first.

## 4. Open Questions & Alternatives Considered
- **Alternative: record the re-anchored selector as the target again.** The selector from `ReanchoringBridge` can be as weak as `a.nav-link` or a bare tag, which always exists, so healing would never trigger and replay could click the first match. Probably worse than coordinates.
- Observed side note: the badge script in `BrowserToolProvider` queries `#_neo_som_badges_` while `VisualBadgeInjector` creates `__neo_som_badges__`, so the badge lookup never matches and the `[data-m]` fallback does the work. Fixing the typo would click the label tag instead of the element, so it should not be fixed in isolation.
- Open: should `REPLAY_STRICT` fail on a mismatch or only warn?
