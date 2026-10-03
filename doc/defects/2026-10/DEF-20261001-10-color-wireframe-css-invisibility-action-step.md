# [DEF-20261001-10] Color Wireframe CSS Invisibility & Action Step Replay State

- **Status:** Resolved
- **Opened:** 2026-10-01
- **Closed:** 2026-10-01
- **Component:** `neodymium-core` (`PageAnalyzer`, `ExecuteActionsStep`, `VisualBaselineGateStep`, `PlaybookStep`)
- **Scope:** `Framework`
- **Symptom:**
  1. Live browser wireframe stylesheet rendered invisible text and transparent image cutouts rather than brand-colored skeleton bars and gray media boxes.
  2. Action steps with `(layout)` failed replay due to comparing un-wireframed post-action screenshots against wireframed baselines.
  3. `(visual-full)` syntax was not recognized as a full-page directive.
- **Root Cause:**
  1. Setting `color: transparent !important;` forces `currentColor` to evaluate to `transparent`, hiding backgrounds. Setting `visibility: hidden !important;` suppresses box painting.
  2. `ExecuteActionsStep` captured post-step state without wireframe for layout steps.
  3. `VISUAL_FULL_PATTERN` only matched colon syntax `(visual: full)`.
- **Detection Gap ("What did we miss?"):** Unit tests in `VisualBaselineGateStepTest` used synthetic in-memory `BufferedImage` mocks rather than evaluating injected CSS in real browser execution.
- **Resolution:**
  Updated `neodymium-color-wireframe.js` with `-webkit-text-fill-color: transparent !important;` and `object-position: -99999px !important;`. Updated `ExecuteActionsStep` and `VisualBaselineGateStep` to wireframe layout steps during post-action capture. Added `(visual-full)` / `(layout-full)` aliases to `PlaybookStep`.
- **Safety Net Added:**
  Created `VisualAndLayoutIntegrationTest.java` in `src/test/java/org/neodymium/ai/integration/live/` verifying `(visual)`, `(visual-full)`, and `(layout)` in live headless Chrome.
