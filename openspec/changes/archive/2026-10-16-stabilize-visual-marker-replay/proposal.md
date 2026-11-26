## Why

When a web application's DOM is heavily degraded ("DOM soup" — nested anonymous `<div>`s, utility CSS hashes, textless SVG icon buttons, and repeating cards without unique IDs), the Neodymium AI engine relies on Set-of-Marks (SoM) visual element markers (`(marker)` directive / `mark_elements` tool / `marker:N` targeting) and falls back to container-anchored coordinates (`coord:<anchorSelector>@relX,relY`) paired with a `DomFeatureVector` for offline replay and self-healing.

Research into the current visual marker recording, re-anchoring, and offline replay pipeline identified six critical stability gaps:
1. `ReanchoringBridge` resolves `document.elementFromPoint(bx, by)` after finding `[data-m="N"]`, hitting inner `<path>` or `<svg>` nodes instead of the marked interactive `<button>`, and records a truncated `DomFeatureVector` missing `role`, `accessibleName`, `parentTag`, `siblingIndex`, and `visualHash` while leaving `class` unstripped in `attributes`.
2. Container anchoring in `BrowserToolProvider` only checks for `#id` or singleton semantic tags (`section`, `main`), walking past repeating anonymous cards/rows (`div._c9a2x`) and anchoring coordinates to page-wide containers that break under grid reflows or item insertions. Furthermore, `ClickAction.COORD_PATTERN` rejects colons and parentheses inside `<anchorSelector>`, breaking `:has(...)` and `:has-text(...)` container anchors.
3. `PlaybookToolReplayer.matchesRecordedIdentity` returns `true` for any live element with the same tag whenever `recordedVector.getText()` is blank, causing strict replay to blindly click the wrong textless icon button and self-healing to be skipped when sibling icons swap or shift.
4. `DomFeatureVector.visualHash` (dHash) and `tileSsim` are never populated anywhere in the codebase (leaving the 45% visual similarity weight in `LocatorCascadeResolver` permanently `0.0`), `VisualBaselineGateStep.executeGate` returns early on mutating steps before reaching its coordinate tile SSIM guard, and `VerifyOutcomeStep` either skips `(marker)` steps or crops post-click/marker-contaminated screenshots.
5. `PlaybookToolReplayer.attemptHealing` downgrades healed coordinate targets to unanchored `"coord: x,y"`, discarding `coord:<anchor>@relX,relY`.
6. Non-click interactive tools (`fill`, `type`, `select`, `check`, `hover`, `clear`, `press_key`) fail with `InvalidSelectorException` when given `marker:N`, contradicting `QualityJudgeToolInterceptor` guidance.

## What Changes

- **Full-Fidelity `[data-m="N"]` Re-anchoring & Graphic Fingerprinting**: Update `ReanchoringBridge` and `PageAnalyzer` to resolve the marked `[data-m="N"]` element (or nearest interactive ancestor on point lookups), populate all `DomFeatureVector` fields (`role`, `accessibleName`, `parentTag`, `siblingIndex`, normalized `attributes`), and compute a zero-latency deterministic graphic fingerprint (`data-ai-graphic-sig`) for child `<svg>`/`<img>`/icon geometry.
- **Heading/Landmark-Scoped Container Anchoring & Coordinate Pattern Support**: Enhance container anchor discovery in `BrowserToolProvider` and `PageAnalyzer` to anchor repeating anonymous cards/rows using `:has-text("...")` on non-volatile headings/landmarks, and update `ClickAction.COORD_PATTERN` to support pseudo-classes (`:`, `(`, `)`) inside `<anchorSelector>`.
- **Clean Pre-Click Visual Signature (`visualHash` dHash & 64x64 Tile SSIM)**: Capture the unmarked 64x64 target crop in `BrowserToolProvider` after `VisualBadgeInjector.removeMarkers(driver)` and before clicking, populating `DomFeatureVector.visualHash` (64-bit dHash) and caching the clean pre-click tile for `VerifyOutcomeStep`. Enable `VerifyOutcomeStep` baseline persistence on `(marker)` / `coord:` steps, execute pre-click coordinate tile gating in `VisualBaselineGateStep`, and populate `visualHash` on live healing candidates in `PlaybookToolReplayer`.
- **Strict & Healing Identity Verification for Textless Elements**: Harden `PlaybookToolReplayer.matchesRecordedIdentity` to verify graphic signatures, stable attributes, structural context, and feature similarity (`>= 0.80`) when `recordedText` is blank.
- **Preserved Container Anchoring on Coordinate Self-Healing**: Keep or re-compute `coord:<anchor>@relX,relY` when `PlaybookToolReplayer.attemptHealing` heals a coordinate target.
- **Universal `marker:N` Support Across Interactive Tools**: Support `marker:N` / `badge:N` resolution, re-anchoring, and marker cleanup in `SelenideElementFinder` and `BrowserToolProvider` (`fill`, `type`, `select`, `check`, `hover`, `clear`, `press_key`).

## Capabilities

### New Capabilities
- `visual-marker-replay`: End-to-end recording, semantic/heading-scoped container anchoring, graphic/visual fingerprinting, pre-click tile SSIM verification, cross-tool `marker:N` resolution, and self-healing for visual marker targets in degraded DOMs.

## Impact

- `neodymium-core/src/main/java/org/neodymium/ai/tool/browser/ReanchoringBridge.java`
- `neodymium-core/src/main/java/org/neodymium/ai/tool/browser/BrowserToolProvider.java`
- `neodymium-core/src/main/java/org/neodymium/ai/executor/selenide/PageAnalyzer.java`
- `neodymium-core/src/main/java/org/neodymium/ai/executor/selenide/SelenideElementFinder.java`
- `neodymium-core/src/main/java/org/neodymium/ai/executor/selenide/plugins/ClickAction.java`
- `neodymium-core/src/main/java/org/neodymium/ai/replay/PlaybookToolReplayer.java`
- `neodymium-core/src/main/java/org/neodymium/ai/pipeline/ExecutionContext.java`
- `neodymium-core/src/main/java/org/neodymium/ai/pipeline/steps/ExecuteActionsStep.java`
- `neodymium-core/src/main/java/org/neodymium/ai/pipeline/steps/VerifyOutcomeStep.java`
- `neodymium-core/src/main/java/org/neodymium/ai/pipeline/steps/VisualBaselineGateStep.java`
- `doc/DOCUMENTATION.md` & `doc/AI_VLM_VISUAL_GROUNDING.md`
