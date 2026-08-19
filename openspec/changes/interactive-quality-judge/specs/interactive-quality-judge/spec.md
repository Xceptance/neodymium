## ADDED Requirements

### Requirement: Driver-Agnostic Locator Probing
The system SHALL provide a generic, read-only locator inspection contract on `TargetExecutor` allowing execution engines to probe candidate selectors on the live SUT without performing state-changing actions.

`TargetExecutor` implementations supporting probing SHALL return a list of `LocatorProbeResult` objects containing:
1. Total elements matched in the DOM (`matchCount`).
2. Detailed element summaries (tag, text, attributes, visibility, interactability, bounding coordinates) for up to `maxElementsPerCandidate` matching elements.
3. Syntax or resolution errors if the selector string is invalid.

#### Scenario: Probing a unique valid selector
- **WHEN** `probeLocators` is called with candidate `"#submit-order"` and exactly one visible button exists with that ID
- **THEN** `LocatorProbeResult` SHALL report `matchCount = 1`, `matches[0].visible = true`, and `matches[0].tagName = "button"`.

#### Scenario: Probing an ambiguous selector
- **WHEN** `probeLocators` is called with candidate `".btn-primary"` and 3 buttons share that class
- **THEN** `LocatorProbeResult` SHALL report `matchCount = 3` and include element summaries for each matching button.

---

### Requirement: Pure W3C Telemetry (No Hardcoded Domain Knowledge)
The live SUT prober SHALL extract only standard W3C DOM attributes (e.g. `id`, `name`, `class`, `aria-*`, `data-*`) and W3C rendering properties (`visible`, `enabled`, `x`, `y`, `width`, `height`).
The Java codebase SHALL NOT hardcode any HTML/CSS framework-specific magic strings (e.g. `.loading`, `#spinner`, `.fade-in`) to ensure 100% domain-agnostic behavior.

---

### Requirement: Playwright & Appium Compatible Geometry
The probing DTOs (`ProbeBoundingRect`) SHALL use a normalized, cross-engine coordinate representation (`double x, double y, double width, double height`) capable of seamlessly bridging Selenide (`WebElement.getRect()`), Playwright (`Locator.boundingBox()`), and Appium.

---

### Requirement: Interactive Deliberation Discussion Round
When Quality Judge is enabled (`neodymium.ai.judge.enabled=true`) and mode is set to `DISCUSSION` (default), the `QualityJudgeStep` SHALL execute an interactive deliberation loop between the LLM Judge and the active `TargetExecutor`.

1. The deliberation loop SHALL execute up to `neodymium.ai.judge.discussion.maxTurns` (default: 3).
2. In each turn, the step SHALL probe current candidate selectors on the live SUT, format the results into a cumulative deliberation history prompt, and query the LLM.
3. If the LLM returns `APPROVED` or `REFINED`, the step SHALL update the action's target/value, log consensus, and terminate the discussion loop.
4. If the LLM returns `NEED_REFINEMENT` and turns remain, the step SHALL probe the LLM's `refinedProposal` in the subsequent turn.
5. If max turns are reached without consensus, the step SHALL pick the candidate with the highest probe quality score.

#### Scenario: Consensus reached in Turn 1
- **WHEN** the primary candidate selector is verified by live SUT probing as unique and visible with matching text
- **THEN** the Judge SHALL return `status = "APPROVED"` and the action target SHALL be finalized.

#### Scenario: Multi-turn refinement on ambiguous selector
- **WHEN** candidate 1 is absent (`matchCount = 0`) and candidate 2 matches multiple buttons
- **THEN** the Judge SHALL return `status = "NEED_REFINEMENT"` with `refinedProposal = ".cart-pane .btn-primary"`, and the engine SHALL probe `.cart-pane .btn-primary` in Turn 2 before finalizing.

---

### Requirement: Fast-Path Probing Optimization
If Candidate 1 is a gold-standard selector (`score == 10`) and live SUT probing confirms `matchCount == 1`, `visible == true`, and text matches the instruction keywords, the step SHALL fast-path the selector in Turn 1 without redundant LLM round-trips.

#### Scenario: Fast-path approval on unique data-testid
- **WHEN** Candidate 1 is `[data-testid='checkout-button']` and live SUT probe confirms 1 visible match
- **THEN** the engine SHALL auto-approve without querying additional LLM discussion turns.

---

### Requirement: Verification Against Extremes (Best and Worst SUTs)
The system SHALL provide integration tests validating the Judge Modes against embedded VÉRLA SUT variants:
1. **Best Quality (`/verla-perfect/`)**: Validating Fast-Path bypass on clean, standard HTML5 with unique IDs.
2. **Worst Quality (`/verla-pwa-chaos/` and `/verla-bad/`)**: Validating multi-turn deliberation, recovery from ambiguous selectors, and volatile hash IDs.

---

### Requirement: Discussion Observability and Token Metrics Tracking
The AI execution engine SHALL track and display discussion metrics:
1. Total LLM discussion calls and per-turn durations.
2. Accumulated token usage (`inputTokenCount`, `outputTokenCount`, `cachedTokenCount`) under `KEY_JUDGE_TOKEN_USAGE`.
3. Structured turn-by-turn console logs.
