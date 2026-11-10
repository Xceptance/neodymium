# [IDEA-20260803] Visual Root Cause Analysis (Visual RCA) Enhancements

- **Status:** `Implemented`
- **Proposed:** 2026-08-03
- **Resolved:** 2026-10-02
- **Component:** `neodymium-core (VisualRcaPrompt, VisualRcaStep)`
- **Category:** `AI & VLM`
- **Author:** Neodymium Core Team

---

When a test execution fails conclusively, Neodymium AI triggers `VisualRcaStep` (guarded by `neodymium.ai.visualRca.enabled`). Currently, Visual RCA inspects a single failure screenshot along with the failed step instruction and exception message to generate a high-level text explanation.

While effective for basic failure analysis, diagnosing complex UI failures (such as subtle layout shifts, element occlusion, or multi-step cascading errors) requires richer visual and structural context.

### Proposed Enhancements

#### 1. Baseline vs. Failure Screenshot Delta Analysis
* **Concept**: Feed both the golden/recorded baseline screenshot and the live failure screenshot (plus optional SSIM heatmap diff) side-by-side to the Vision LLM.
* **Benefit**: Allows the LLM to perform precise visual regression diagnosis—pinpointing missing components, altered layouts, styling glitches, or unintended text changes relative to expected state.

#### 2. Set-of-Mark (SOM) Bounding Box Overlays
* **Concept**: Superimpose numbered bounding box overlays (`[1]`, `[2]`, etc.) or DOM element outlines onto the failure screenshot before sending it to the Vision model.
* **Benefit**: Enables Visual RCA to cite specific visual element markers (e.g., *"Element [12] (`#submit-btn`) is occluded by Modal overlay [3] (`#cookie-consent`)"*) rather than vague visual descriptions.

#### 3. Multi-Step Trajectory Timeline (Failure Filmstrip)
* **Concept**: Attach a filmstrip sequence of screenshots from the last $N$ executed steps leading up to the failure.
* **Benefit**: Helps diagnose cascading failures where the true root cause occurred several steps prior to the failed assertion (e.g. an unhandled popup appeared 2 steps ago).

#### 4. Structured Diagnostic Schema & Report Integration
* **Concept**: Standardize Visual RCA response output into a typed JSON schema:
  * `category`: `ELEMENT_OCCLUDED`, `VISUAL_REGRESSION`, `DOM_CHANGE`, `ASSERTION_MISMATCH`, or `NETWORK_ERROR`
  * `confidence`: Score from `0.0` to `1.0`
  * `suggestedRemediation`: Actionable fix (e.g., *"Update selector from `#btn` to `#btn-primary` or add overlay dismissal step"*)
  * `summary` / `explanation`: Human-readable summary
* **Benefit**: Enables automated embedding of visual diagnostic cards directly into JUnit XML reports, Allure reports, and CI/CD dashboards.
