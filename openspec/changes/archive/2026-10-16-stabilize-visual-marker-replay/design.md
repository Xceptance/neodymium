## Context

When a web page has a degraded DOM ("DOM soup"), Neodymium's `QualityJudgeToolInterceptor` or `(marker)` directive overlays Set-of-Marks (`[data-m="N"]`) badges via `VisualBadgeInjector`. When the agent interacts with `marker:N`, `BrowserToolProvider` attempts to re-anchor the marked element to a clean CSS selector via `ReanchoringBridge`. If no unique, non-volatile CSS selector exists, it records a container-relative coordinate (`coord:<anchorSelector>@relX,relY`) and a `DomFeatureVector` on the playbook's `ToolCallRecord` and `Action`.

During offline replay (`REPLAY_STRICT` or `REPLAY_WITH_HEALING`), `PlaybookToolReplayer` resolves the coordinate, checks whether the live element at `(x, y)` still matches the recorded `DomFeatureVector`, and if drifted in `REPLAY_WITH_HEALING`, runs `LocatorCascadeResolver.findBestMatch(recordedVector, liveCandidates, 0.70)` to self-heal the target.

## Goals / Non-Goals

**Goals:**
- Achieve 1:1 schema and semantic parity between `ReanchoringBridge` feature vectors and `PageAnalyzer.extractFeatureVectors()`.
- Prevent `document.elementFromPoint(x, y)` from capturing inner `<svg>` / `<path>` nodes when an interactive ancestor (`<button>`, `<a>`, `[role="button"]`) owns the control.
- Introduce a zero-latency DOM graphic fingerprint (`data-ai-graphic-sig`) plus clean pre-click 64-bit `dHash` (`visualHash`) so textless icon buttons can be distinguished and self-healed above the `0.70` similarity threshold.
- Anchor `coord:<anchor>@relX,relY` to repeating card/row containers using non-volatile heading landmarks (`:has-text("...")`) and fix `ClickAction.COORD_PATTERN` to parse complex anchor selectors.
- Fix unreachable coordinate tile SSIM gating in `VisualBaselineGateStep` and ensure `VerifyOutcomeStep` records clean pre-click tiles for marker/coordinate steps.
- Preserve container anchors when persisting self-healed coordinate targets.
- Support `marker:N` / `badge:N` across all selector-based browser tools (`fill`, `type`, `select`, `check`, `hover`, `clear`, `press_key`).

**Non-Goals:**
- Changing VLM prompt syntax or introducing external OCR libraries.
- Adding third-party Maven dependencies.

## Decisions

### 1. Direct `[data-m="N"]` Resolution & Interactive Ancestor Normalization in `ReanchoringBridge`
- **Choice**: Add `ReanchoringBridge.resolveMarkedElement(WebDriver driver, int markerId)` (and update `resolveElementAtPoint` to walk up `el.closest('button, a, input, select, textarea, summary, label, [role="button"], [role="link"], [role="checkbox"], [role="menuitem"], [role="tab"], [data-m]')` when `elementFromPoint` lands on a non-interactive graphic child like `<path>`, `<svg>`, `<span>`, or `<i>`).
- **Rationale**: `PageAnalyzer.extractFeatureVectors()` only indexes interactive elements. If `ReanchoringBridge` records `<path>` or `<svg>`, tag similarity (`0.35` weight) is `0.0` against `<button>` candidates during healing.
- **Full Vector Parity**: Populate `role`, `accessibleName`, `parentTag`, `siblingIndex`, and strip `class` and `style` from `attributes` in `ReanchoringBridge`, matching `PageAnalyzer` exactly.

### 2. Dual-Tier Graphic & Perceptual Signature (`data-ai-graphic-sig` + `visualHash` dHash)
- **Choice**:
  1. In both `ReanchoringBridge` and `PageAnalyzer`, compute a compact deterministic graphic signature stored in `attributes.get("data-ai-graphic-sig")` for any element containing `<svg>` paths/shapes, `<img>` `src`, or icon classes (e.g., `svg:d=<hash>`).
  2. After `VisualBadgeInjector.removeMarkers(driver)` and before clicking in `BrowserToolProvider`, capture a clean viewport screenshot crop around `(bx, by)` to compute the 64-bit hex `dHash` (`DomFeatureVector.visualHash`) and 64x64 tile SSIM matrix (`ExecutionContext.KEY_PRE_CLICK_COORD_TILE_MATRIX`).
  3. During `PlaybookToolReplayer.attemptHealing()`, if `recordedVector.getVisualHash()` is non-null, capture the current viewport image once and compute `visualHash` for each visible candidate bounding box before invoking `LocatorCascadeResolver.findBestMatch()`.
- **Rationale**: `data-ai-graphic-sig` is zero-latency and works off-viewport; `visualHash` activates the 45% visual similarity weight (`0.45 * computeVisualSimilarity`) in `LocatorCascadeResolver` for canvas/custom controls.

### 3. Heading-Scoped Card/Row Container Anchors & `ClickAction.COORD_PATTERN` Fix
- **Choice**:
  1. When walking ancestors for `coord:<anchor>@relX,relY`, before falling back to a singleton `<section>`/`<main>`, check if an ancestor container (`article`, `tr`, `li`, `[role="listitem"]`, `[role="row"]`, or a `div` card with a stable/single class) contains a unique non-volatile heading (`h1`–`h6`, `[role="heading"]`, `th`, `legend`) whose text passes volatility checks (not purely numeric/currency/timestamp) and uniquely matches on the page via `<tag><class>:has-text("<heading>")`.
  2. Update `ClickAction.COORD_PATTERN` so that when `@` separates the anchor from `x,y`, the anchor group permits colons, parentheses, and quotes:
     `^(?:(?:coord:|coordinates:|point\()\s*(?:([^@]+?)@|([^@:\)]+?):)?\s*([0-9]+)\s*[,x]\s*([0-9]+)\)?|([^@]+)[@]([0-9]+)\s*[,x]\s*([0-9]+))$`

### 4. Hardened `matchesRecordedIdentity` & Pre-Click Coordinate Tile Gating
- **Choice**:
  1. In `PlaybookToolReplayer.matchesRecordedIdentity`, when `recordedText` is blank, compare `data-ai-graphic-sig`, non-volatile identifying attributes (`aria-label`, `title`, `name`, `type`, `role`, `data-action`, `href`), `parentTag`, and `siblingIndex`. If a graphic signature or identifying attribute was recorded and differs on the live element, return `false`.
  2. In `VisualBaselineGateStep.executeGate`, evaluate the coordinate tile SSIM guard (`coordinateTileHashes`) **before** the `if (!pureVerification) return false;` early exit so action steps with `coord:` targets are protected before clicking.
  3. In `ExecuteActionsStep`, schedule `VerifyOutcomeStep` whenever the step has a `coord:` action or `step.isMarker()` so `coordinateTileHashes` are persisted even when `semanticVerification` is disabled.

### 5. Universal `marker:N` Support & Preserved Container Anchoring on Heal
- **Choice**:
  1. Add a helper `BrowserToolProvider.resolveMarkerSelectorIfPresent(WebDriver driver, String rawSelector)` and support `marker:N` / `badge:N` in `SelenideElementFinder.findDirect` so `fill`, `type`, `select`, `check`, `hover`, `clear`, and `press_key` seamlessly resolve `[data-m="N"]`, re-anchor to a storable selector, and strip markers.
  2. In `PlaybookToolReplayer.attemptHealing`, when a coordinate target heals to a bare generic tag, compute the new relative offset against the existing `coordTarget.anchorSelector()` (or `PageAnalyzer.resolveAnchorCoordinate`) so `coord:<anchor>@newRelX,newRelY` is preserved.
