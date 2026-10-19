# Visual Marker Replay

## Purpose

Ensures resilient offline replay, self-healing, and visual baseline verification when the Neodymium AI engine targets elements via Set-of-Marks visual markers (`(marker)`, `mark_elements`, `marker:N`) and container-anchored coordinates (`coord:<anchorSelector>@relX,relY`) on degraded DOM structures.

## Requirements

### Requirement: Full-fidelity interactive element re-anchoring and graphic fingerprinting
When resolving a visual marker (`marker:N` or `badge:N`) or a viewport coordinate (`resolveElementAtPoint`), `ReanchoringBridge` and `PageAnalyzer` SHALL resolve the target interactive element rather than inner non-interactive SVG/icon primitives (`<path>`, `<circle>`, `<svg>`, `<use>`), and SHALL populate a complete `DomFeatureVector` matching the schema of `PageAnalyzer.extractFeatureVectors` (`tag`, `id`, `text`, `classes`, `attributes` with `class`/`style` stripped, `boundingBox`, `role`, `accessibleName`, `parentTag`, `siblingIndex`, and a deterministic `data-ai-graphic-sig` attribute for child SVG/image/icon geometry).

#### Scenario: Re-anchor textless SVG icon button marked with data-m
- **WHEN** `click(target: "marker:N")` targets a `<button>` containing an nested `<svg><path d="..."/></svg>`
- **THEN** `ReanchoringBridge` SHALL extract the `DomFeatureVector` from the `<button>` element (not `<path>` or `<svg>`) including `role`, `accessibleName`, `parentTag`, `siblingIndex`, and `data-ai-graphic-sig`

#### Scenario: Distinguish sibling textless SVG icon buttons during healing
- **WHEN** two textless `<button>` elements share identical tags and obfuscated classes but contain different `<svg>` path geometry
- **THEN** their `DomFeatureVector` instances SHALL carry distinct `data-ai-graphic-sig` values, enabling `LocatorCascadeResolver` and `matchesRecordedIdentity` to differentiate them

### Requirement: Heading and landmark-scoped container anchoring for repeating DOM structures
When a marked element cannot be uniquely identified by a stable CSS selector and resides inside a repeating container without a unique `#id`, the engine SHALL anchor relative coordinates (`coord:<anchorSelector>@relX,relY`) to the nearest enclosing card/row/item container that contains a unique non-volatile heading or landmark text (`:has-text("...")`), and `ClickAction.parseCoordinateTarget` SHALL parse `<anchorSelector>` expressions containing colons, parentheses, and quotes.

#### Scenario: Anchor button inside anonymous product card with unique heading
- **WHEN** `click(target: "marker:N")` targets an anonymous button inside a repeating `div` card that contains a unique `<h3>Stealth Cyber Deck</h3>` heading
- **THEN** the recorded coordinate target SHALL anchor to the card container (e.g. `coord:div._c9a2x:has-text("Stealth Cyber Deck")@relX,relY`) rather than the page-wide `<section>` or `<main>`

#### Scenario: Parse coordinate target with pseudo-class anchor selector
- **WHEN** `ClickAction.parseCoordinateTarget` parses `coord:div._c9a2x:has-text("Stealth Cyber Deck")@42,18`
- **THEN** it SHALL return `anchorSelector = "div._c9a2x:has-text(\"Stealth Cyber Deck\")"`, `x = 42`, and `y = 18`

### Requirement: Clean pre-click visual hash and coordinate tile SSIM gating
Before executing a marker or coordinate click, `BrowserToolProvider` SHALL remove visual marker overlays (`#__neo_som_badges__`), capture a clean pre-click visual signature (`DomFeatureVector.visualHash` 64-bit dHash and 64x64 luminance tile matrix), and cache it for `VerifyOutcomeStep`. During offline replay, `VisualBaselineGateStep` and `PlaybookToolReplayer` SHALL evaluate pre-click visual/structural identity before dispatching coordinate clicks and populate `visualHash` on visible live candidates during self-healing.

#### Scenario: Pre-click tile baseline captured without marker overlay contamination
- **WHEN** a step uses `(marker)` or `mark_elements` and clicks `marker:N` resulting in a `coord:` target
- **THEN** the recorded `DomFeatureVector.visualHash` and `coordinateTileHashes` baseline SHALL be computed from the unmarked pre-click viewport rather than the red-badged marker screenshot or post-click mutated state

#### Scenario: Coordinate tile SSIM gate executes on action steps with coordinate targets
- **WHEN** a recorded step containing a `coord:` click action is replayed with a saved `coordinateTileHashes` baseline and the live 64x64 tile SSIM falls below threshold
- **THEN** `VisualBaselineGateStep` SHALL detect the coordinate tile mismatch even though the step is not a pure verification step

### Requirement: Strict and healing identity verification for textless coordinate targets
During offline replay of a `coord:` target, `PlaybookToolReplayer.matchesRecordedIdentity` SHALL NOT treat bare tag equality as sufficient when `recordedVector.getText()` is blank; it SHALL verify `data-ai-graphic-sig`, stable attributes (`aria-label`, `title`, `name`, `type`, `role`, `data-action`), and structural/visual similarity before accepting the live element at `(x, y)`.

#### Scenario: Detect swapped textless icon buttons at recorded coordinates
- **WHEN** a textless `<button>` with SVG icon A was recorded at `(x, y)` and on replay a different textless `<button>` with SVG icon B occupies `(x, y)`
- **THEN** `matchesRecordedIdentity` SHALL return `false`, causing `REPLAY_STRICT` to fail fast and `REPLAY_WITH_HEALING` to invoke `attemptHealing`

### Requirement: Container anchor preservation during coordinate self-healing
When `PlaybookToolReplayer.attemptHealing` relocates a drifted `coord:<anchorSelector>@relX,relY` target to a live candidate whose selector is a bare generic tag, it SHALL preserve container-relative anchoring (`coord:<anchorSelector>@newRelX,newRelY`) instead of downgrading the playbook to unanchored viewport coordinates.

#### Scenario: Heal drifted coordinate button inside anchored container
- **WHEN** `coord:#product-grid@120,80` (or `coord:div._c9a2x:has-text("...")@120,80`) drifts to a new offset within the anchor container and self-heals
- **THEN** the updated playbook target SHALL retain the anchor prefix `coord:<anchorSelector>@newRelX,newRelY`

### Requirement: Universal marker:N resolution across interactive browser tools
All selector-accepting interactive browser tools (`click`, `fill`, `type`, `select`, `check`, `hover`, `clear`, `press_key`) and `SelenideElementFinder` SHALL support `marker:N` and `badge:N` targets by resolving `[data-m="N"]`, re-anchoring to a stable selector when available, and removing marker overlays cleanly.

#### Scenario: Fill input field targeted via marker:N
- **WHEN** `fill(selector: "marker:4", text: "test@example.com")` is invoked while `[data-m="4"]` is present on an `<input>` element
- **THEN** `BrowserToolProvider` SHALL resolve the marked input, enter the text, remove the visual markers, and record the re-anchored selector and `DomFeatureVector` in the tool result
