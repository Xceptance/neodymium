## 1. ReanchoringBridge & PageAnalyzer Feature Vector Parity + Graphic Signature

- [x] 1.1 Update `ReanchoringBridge` to walk up to the nearest interactive element when `elementFromPoint` hits an inner SVG/icon child (`path`, `svg`, `circle`, `rect`, `line`, `polygon`, `polyline`, `use`, `g`) and add `resolveMarkedElement(WebDriver, String)` for direct `[data-m="N"]` lookup
- [x] 1.2 Align `ReanchoringBridge` `DomFeatureVector` extraction with `PageAnalyzer.extractFeatureVectors()` (`role`, `accessibleName`, `parentTag`, `siblingIndex`, stripped `class`/`style`/`data-m` from `attributes`) and extract deterministic `data-graphic-sig` in both `ReanchoringBridge` and `PageAnalyzer`
- [x] 1.3 Add unit tests in `LocatorCascadeResolverTest` verifying full `DomFeatureVector` field parity and `data-graphic-sig` differentiation

## 2. Heading/Landmark-Scoped Container Anchoring & Coordinate Pattern Support

- [x] 2.1 Fix `ClickAction.COORD_PATTERN` to support colons, parentheses, and quotes inside `<anchorSelector>` before `@` (e.g. `coord:div._c9a2x:has-text("Stealth Cyber Deck")@40,50`)
- [x] 2.2 Enhance container anchor discovery in `BrowserToolProvider` and `PageAnalyzer.resolveAnchorCoordinate` to anchor to repeating card/row containers with unique non-volatile heading landmarks (`:has-text(...)`) before falling back to singleton page sections
- [x] 2.3 Add unit tests in `CoordinateActionTest` for `ClickAction.parseCoordinateTarget` with pseudo-class anchor selectors and container anchoring

## 3. Clean Pre-Click Visual Signature (`visualHash` dHash) & Coordinate Tile SSIM Gating

- [x] 3.1 Capture clean pre-click 64-bit `dHash` (`DomFeatureVector.visualHash`) and 64x64 tile SSIM matrix (`ExecutionContext.KEY_PRE_CLICK_COORD_TILE_MATRIX`) in `BrowserToolProvider` after `VisualBadgeInjector.clearMarkers(driver)` and before clicking
- [x] 3.2 Update `ExecuteActionsStep` and `VerifyOutcomeStep` to persist `screenshotHash` from the clean pre-click tile for `coord:` / `(marker)` steps even when semantic verification is disabled
- [x] 3.3 Enable the pre-click coordinate tile SSIM guard in `VisualBaselineGateStep.executeGate` for `(marker)` coordinate steps and in `PlaybookToolReplayer` so pre-click tile divergence is caught on coordinate action steps
- [x] 3.4 Populate `visualHash` and `tileSsim` on visible live candidate `DomFeatureVector`s in `PlaybookToolReplayer.attemptHealing` from the live viewport screenshot so `LocatorCascadeResolver`'s 45% visual similarity weight is active

## 4. Textless Element Identity Verification & Anchored Coordinate Self-Healing

- [x] 4.1 Harden `PlaybookToolReplayer.matchesRecordedIdentity` for textless elements to verify `data-graphic-sig`, stable attributes, structural context (`parentTag`, `siblingIndex`), and `visualHash` / `tileSsim` similarity
- [x] 4.2 Update `PlaybookToolReplayer.attemptHealing` to preserve container-relative anchoring (`coord:<anchorSelector>@newRelX,newRelY`) when healing a coordinate target whose best selector is a bare generic tag
- [x] 4.3 Add unit tests in `CoordinateReplayGuardTest` and `VisualBaselineGateStepTest` covering textless SVG icon swap detection, `visualHash` + `data-graphic-sig` healing via `LocatorCascadeResolver`, preserved container anchors, and pre-click tile SSIM gating

## 5. Universal `marker:N` Support Across Interactive Tools

- [x] 5.1 Support `marker:N` and `badge:N` in `SelenideElementFinder`, `BrowserToolProvider` (`fill`, `type`, `enter`, `select`, `check`, `hover`, `clear`, `press_key`), and `AgentToolLoopStep` with automatic re-anchoring and marker cleanup
- [x] 5.2 Add unit tests verifying `marker:N` resolution and container-anchored coordinate handling

## 6. Documentation & Defect Record Finalization

- [x] 6.1 Update `doc/DOCUMENTATION.md` and `doc/AI_VLM_VISUAL_GROUNDING.md` with heading-scoped container anchoring, graphic/visual signatures, and universal `marker:N` tool support
- [x] 6.2 Mark `doc/defects/2026-10/DEF-20261014-2040-visual-marker-replay-healing-gaps.md` as `Resolved` with complete resolution and safety net references
