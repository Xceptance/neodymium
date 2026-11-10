# [DEF-20261005-1348] Visual tile full-page mismatch and container anchor coordinate extraction drift on replay

- **Status:** `Resolved`
- **Opened:** 2026-10-05 13:48
- **Closed:** 2026-10-05 13:52
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** When running with `neodymium.ai.dataAi.enabled = false` and marker clicks recorded as container-anchored coordinates (`coord:<anchor>@x,y`), playbook replay failed:
  1. `VisualBaselineGateStep` rejected the step baseline comparison with low SSIM (e.g. SSIM 0.2563 < 0.95), reporting visual mismatch and blocking or triggering false-positive healing.
  2. In `BrowserToolProvider`, replay of recorded tool calls containing both `target: "coord:section@133,403"` and `x: 133, y: 403` failed to extract the anchor selector `section`, resulting in an empty selector string `""`. Consequently, `executeCoordinateClick` performed a raw unanchored viewport click at (133, 403) rather than computing the relative offset from the container anchor (`anchor.left + x, anchor.top + y`), causing clicks to miss the intended button/input by the container's dynamic offset (e.g. ~114px offset).
  3. In `VerifyOutcomeStep`, `VisualBaselineGateStep`, and `ExecuteActionsStep`, when context escalation enabled `activeLevel.isFullPageScreenshot()`, `step.setFullPage(true)` was persisted onto the recorded step. On replay, the screenshot captured the full 2211px page document, while tile matrix cropping evaluated viewport coordinates (e.g. 138, 381), effectively comparing the top navigation/hero against the viewport content.

- **Root Cause:** 
  1. `VerifyOutcomeStep` prioritized `activeLevel.isFullPageScreenshot()` over the step's coordinate targets, calling `step.setFullPage(true)`. Full-page screenshots cannot be directly cropped using viewport/container-relative coordinate offsets.
  2. `VisualBaselineGateStep` did not check if the step had a `CoordinateTarget` when deciding `isFullPageReq`, inheriting `step.isFullPage() == true` from recording. Furthermore, when cropping the 64x64 SSIM tile, it did not scroll the anchor container into view or translate the crop center by `anchor.left, anchor.top`.
  3. In `BrowserToolProvider.createClickTool`, coordinate argument parsing only attempted to extract `anchorSelector` from `target` if `parsedX == MIN_VALUE || parsedY == MIN_VALUE`. Since recorded tool calls in playbooks had explicit `x` and `y` arguments deserialized alongside `target`, `parsedX` and `parsedY` were already non-sentinel values, causing the branch to be skipped and leaving `sel = ""`.
  4. In `ClickAction` and `TypeAction`, element-relative coordinate clicks used `actions.moveToElement(anchorElement, coord.x(), coord.y())` which measures from the center of the anchor element in Selenium WebDriver, whereas container-relative coordinate markers were measured relative to the top-left corner of the container (`ar.left, ar.top`).

- **Detection Gap ("What did we miss?"):** Prior sandbox tests for visual markers (`VisualMarkersSandboxLiveTest`) operated on short single-viewport pages without page scrolling, where `isFullPageScreenshot()` was not escalated by lean visual context or multi-turn escalations, and container anchors were coincidentally at (0,0) or had synthetic `data-ai` attributes enabled (`neodymium.ai.dataAi.enabled=true`).

- **Resolution:**
  1. In `VerifyOutcomeStep`, `VisualBaselineGateStep`, and `ExecuteActionsStep`, explicitly detect coordinate actions (`CoordinateTarget`) and enforce `isFullPageReq = false` and `step.setFullPage(false)`, pre-scrolling the anchor container before state capture and translating tile cropping by `anchor.left, anchor.top`.
  2. In `BrowserToolProvider.createClickTool`, unconditionally extract `coord.anchorSelector()` into `sel` if `sel.isBlank()` whenever `target` contains `coord:` or `@`, and serialize `res.put("selector", anchorSelector)`.
  3. In `ClickAction` and `TypeAction`, replace center-based `moveToElement` with pre-scrolling `SelenideElementFinder.scrollIntoViewIfNeeded(anchorElement)` and `actions.moveToLocation(anchor.left + coord.x(), anchor.top + coord.y())`.
  4. In `PlaybookToolReplayer.verifyStrictReplayGuards`, translate container-anchored coordinates `coord:<anchor>@x,y` into viewport coordinates after scrolling the anchor into view before verifying element identity.

- **Safety Net Added:** Added unit test `VerifyOutcomeStepTest.testCoordinateStepEnforcesViewportScreenshotAndClearsFullPage`, and verified live and replay suites on complex e-commerce flows without `data-ai` (`CheckoutTest` on `/verla-bad/` passing both live and replay with 21 steps, 0 LLM calls on replay) and Aura AI Sandbox suites (`VisualMarkersSandboxLiveTest`, `VisualMarkersComplexSandboxLiveTest` across `FORCE_RECORDING`, `REPLAY_STRICT`, and `REPLAY_WITH_HEALING`).
