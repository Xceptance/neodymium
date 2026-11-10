# [DEF-20261014-2040] Visual Marker Replay Identity Bypass, Truncated Feature Vectors, and Unreachable Tile SSIM Guard

- **Status:** `Resolved`
- **Opened:** 2026-10-14 20:40
- **Closed:** 2026-10-14 21:20
- **Component:** `neodymium-core`
- **Scope:** `Framework & AI/Prompt`
- **Symptom:**
  1. When `mark_elements` / `(marker)` falls back to `coord:<anchor>@relX,relY` on textless icon buttons in degraded DOMs ("DOM soup"), offline replay in `PlaybookToolReplayer` fails to self-heal when elements shift (`LocatorCascadeResolver` similarity score stays below `0.70`), or silently clicks the wrong sibling element if another control occupies `(x, y)`.
  2. Pre-click 64×64 coordinate tile SSIM gating in `VisualBaselineGateStep` never executes on action steps, and `VerifyOutcomeStep` either skips `(marker)` steps or records post-click / marker-contaminated tiles.
  3. Non-click tools (`fill`, `type`, `select`, `check`, `hover`) fail with `InvalidSelectorException` when given `marker:N`.
- **Root Cause:**
  1. `BrowserToolProvider` calls `ReanchoringBridge.resolveElementAtPoint(driver, bx, by)` using `document.elementFromPoint(bx, by)` after finding `[data-m="N"]`, hitting inner `<path>`/`<svg>` children instead of the marked interactive `<button>`, and builds a partial `DomFeatureVector` missing `role`, `accessibleName`, `parentTag`, `siblingIndex`, and `visualHash` while leaving `class` unstripped in `attributes`.
  2. `DomFeatureVector.visualHash` and `tileSsim` are never populated anywhere in the codebase, leaving the 45% visual weight in `LocatorCascadeResolver.computeSimilarity` permanently `0.0` for textless elements.
  3. `PlaybookToolReplayer.matchesRecordedIdentity` returns `true` for any live element with the same tag whenever `recordedVector.getText()` is blank.
  4. `VisualBaselineGateStep.executeGate` returns `false` at line 193 when `!pureVerification`, making the coordinate tile SSIM guard at lines 384–398 unreachable dead code for click steps; `ExecuteActionsStep` also omits `VerifyOutcomeStep` when `step.isMarker()` is true but `isVisualOrLayoutStep()` is false.
  5. `PlaybookToolReplayer.attemptHealing` overwrites healed coordinate targets as unanchored `"coord: x,y"`, discarding `coord:<anchor>@relX,relY`.
- **Detection Gap ("What did we miss?"):**
  Existing sandbox tests for visual markers tested discovery and immediate coordinate clicks on static layouts (`DEF-20261005-1348`) or simple tag mismatches, but lacked offline replay tests where (a) textless SVG icon buttons swap or shift positions inside repeating anonymous cards, (b) `ReanchoringBridge` output is directly fed into `LocatorCascadeResolver.findBestMatch` against `PageAnalyzer.extractFeatureVectors`, and (c) non-click tools target `marker:N`.
- **Resolution:**
  1. Added `ReanchoringBridge.resolveMarkedElement(WebDriver, String)` and interactive ancestor normalization in `ReanchoringBridge`, extracting full `DomFeatureVector` parity (`role`, `accessibleName`, `parentTag`, `siblingIndex`, stripped `data-m`/`data-ai*`/`class`/`style`, and deterministic `data-graphic-sig` FNV-1a fingerprints for SVG/img/canvas) across both `ReanchoringBridge` and `PageAnalyzer`.
  2. Upgraded `PageAnalyzer.resolveAnchorCoordinate` and `ClickAction.COORD_PATTERN` to discover and parse heading-scoped repeating containers (`tag:has-text("...")`) and `[data-testid]`/`[data-test]`/`[data-qa]` before falling back to singleton page sections.
  3. Updated `BrowserToolProvider` to clear markers before capturing clean pre-click `visualHash` (dHash) and 64x64 `tileSsim` signatures (`ScreenshotHasher.computeElementDHash` and `ExecutionContext.KEY_PRE_CLICK_COORD_TILE_MATRIX`), scheduled `VerifyOutcomeStep` dynamically in `ExecuteActionsStep` for `(marker)`/`coord:` steps, and enabled pre-click coordinate tile gating in `VisualBaselineGateStep` and `PlaybookToolReplayer`.
  4. Hardened `PlaybookToolReplayer.matchesRecordedIdentity` for textless elements (verifying `data-graphic-sig`, `parentTag`, `siblingIndex`, `visualHash`, and `tileSsim`), enriched live healing candidates with cropped visual signatures, and preserved container anchors (`coord:<anchor>@newRelX,newRelY`) on coordinate relocation.
  5. Added universal `marker:N` and `badge:N` support across all interactive tools in `BrowserToolProvider`, `SelenideElementFinder`, and `AgentToolLoopStep`.
- **Safety Net Added:**
  - `LocatorCascadeResolverTest`: `findBestMatchUsesGraphicSignatureForTextlessSvgIconButtons`, `computeSimilarityIncorporatesGraphicSignatureAndVisualHash`
  - `CoordinateActionTest`: `testParseCoordinateTarget_withHasTextContainerAnchor`
  - `CoordinateReplayGuardTest`: `testCoordinateClickRejectsSwappedTextlessSvgIconViaGraphicSig`, `testCoordinateClickHealingPreservesContainerAnchor`, `testCoordinateClickAbortsWhenPreClickTileSsimDiverges`
  - `VisualBaselineGateStepTest`: `testReplayWithMarkerCoordinateAction_preActionGateExecutesOnCleanPreClickTile`, `testReplayWithMarkerCoordinateAction_preActionGateThrowsDivergenceWhenTileMismatches`
