## Context

Currently, the Neodymium AI executor relies on a single brittle DOM structural hash (`data-ai`) or isolated CSS selectors, and delegates all replay element failures directly to expensive remote LLM calls. In addition, the pipeline tightly couples Selenide mechanics inside `ExecuteActionsStep`, visual-only components (like canvas elements) cause failures when the execution context is abruptly stripped of all DOM nodes (the `VISUAL` tier), and cross-framework replay leads to confusing traversal failures.

## Goals / Non-Goals

**Goals:**
- Implement the Unified Perception Model (UPM) in JavaScript DOM extraction capturing AOM, Test-IDs, DOM, and Bounding Boxes.
- Introduce the 5-Tier Locator Cascade for deterministic replay without LLM intervention.
- Store dual representation in companion JSON: concrete selectors (Tiers 1–3) and structured DOM Feature Vectors (Tier 4).
- Implement local Feature Proximity Search (Jaccard + Levenshtein + Tag Equivalence Bucketing) for $<1\text{ ms}$, 0-token local self-healing.
- Safely handle visual-only elements (canvas, SVG) with anchor-relative coordinate clicks, SSIM tile micro-crop gating, and raw keyboard event dispatch.
- Enforce strict execution framework metadata matching (e.g. `SELENIUM_SELENIDE` vs `PLAYWRIGHT`).
- Standardize all prompts (including `QualityJudgePrompt`) on `AiPrompt<T>`.
- Maintain strict **Non-Destructive Backward Compatibility** for existing single-target companion recordings and core Neodymium browser automation APIs.

**Non-Goals:**
- Completely rewriting the underlying Selenide or Playwright WebDriver bridges.
- Replacing the SSIM 64x64 algorithm (it is reused for micro-cropping).
- Breaking or modifying existing non-AI Neodymium core utilities and runner lifecycles.

## Decisions

### 1. Dual Selector + Feature Vector Storage in Companion Recordings
*Rationale:* Concrete selectors (Tiers 1–3) allow $95\%$ of test steps to execute in $<0.1\text{ ms}$ under native browser engines on unchanged UIs. Storing the rich DOM Feature Vector alongside them gives the runner an in-memory safety net (Tier 4) when frontend refactorings break the static selectors.
*Backward Compatibility:* The parser will detect legacy flat-action or single-selector `.json` companions and adapt them transparently into Tier 3 entries without failing old test runs.

### 2. Feature Proximity Scoring (Jaccard + Levenshtein + Tag Bucketing)
*Rationale:* Exact hash matching (`data-ai`) is binary and brittle. The composite similarity score calculates:
$$\text{Score} = 0.35 \times \text{TagScore} + 0.30 \times \text{AttrJaccard} + 0.25 \times \text{TextLevenshtein} + 0.10 \times \text{ClassJaccard}$$
- Interactive elements are grouped into semantic equivalence buckets ($\{\texttt{button}, \texttt{a}, \texttt{div[role=button]}, \texttt{input[type=submit]}\}$), granting partial credit ($0.70$) when element tags change while preserving text and intent.
- Elements scoring $\ge 0.80$ are selected without triggering remote LLM calls.

### 3. Anchor-Relative Coordinate Grounding with 64x64 SSIM Tile Gating
*Rationale:* Absolute viewport coordinates break under responsive reflows or injected banners. Tier 5 anchors coordinates relative to the nearest stable parent DOM container and validates the local $64 \times 64$ luminance crop ($\ge 0.95$ SSIM) before clicking. If the tile has changed, the runner safely halts and escalates to multimodal LLM healing.

### 4. Decoupled Visual Form Inputs
*Rationale:* Canvas/WebGL inputs lack native DOM `<input>` handles. The runner issues an anchor-relative coordinate click to activate browser focus, then dispatches keystrokes via the WebDriver `Actions` API.

### 5. Strict Target Framework Validation
*Rationale:* Playwright and Selenide locator syntax (especially pseudo-selectors like `text=`) are not strictly interoperable. Companion JSON recordings record `targetFramework` in metadata and fail fast on engine mismatches with `IncompatibleFrameworkException`.

### 6. Retention and Elevation of the Quality Judge
*Rationale:* The Quality Judge (`QualityJudgePrompt`) acts as a quality gatekeeper during recording. It grades the complete 5-tier bundle to ensure non-volatile selectors and rich feature vectors before writing to companion JSON. It is unified under `AiPrompt<QualityJudgeResult>`.

### 7. Non-Destructive Quality Assurance Protocol
*Rationale:* Every vertical slice implemented must be accompanied by an instant, blunt code review auditing:
- No accidental removal or breakage of existing features, comments, or edge-case handlers.
- Line-by-line diff validation to ensure strict containment of changes.
- Compilation and full unit/integration test validation.

## Risks / Trade-offs

- **[Risk]** The Feature Proximity Search (Tier 4) could select the wrong element if multiple identical generic elements exist (e.g., two "Save" buttons).
  - *Mitigation:* Ensure the Feature Vector includes the `siblingIndex`, `parentTag`, and `relativeXPath` from the nearest semantic parent, providing structural disambiguation.
- **[Risk]** Visual Focus via Clicks on forms may trigger unwanted UI events if clicked in the wrong spot.
  - *Mitigation:* The anchor-relative bounding boxes and SSIM validation act as a safety gate. If the SSIM tile score drops below 0.95, the click is aborted and the framework escalates to LLM healing.
