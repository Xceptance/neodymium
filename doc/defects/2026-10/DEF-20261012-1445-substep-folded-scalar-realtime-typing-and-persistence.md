# [DEF-20261012-1445] Missing Real-time Sub-step Folding Indicator Styling and Dynamic Sub-step Promotion in Test Editor

- **Status:** `Resolved`
- **Opened:** 2026-10-12 14:45
- **Closed:** 2026-10-12 15:00
- **Component:** `aura-manager` / `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** Typing folded block scalar indicators (`>-`, `- >-`, `  - `) in a test step line did not format the `>-` token with dedicated badge styling, did not dynamically promote the row to a sub-step with hierarchical line numbering (`parent.child`), and did not immediately generate or refresh the `.substeps-toggle-bar` grouping header card.
- **Root Cause:**
  1. `checkAndUpdateCommentRow()` in `dashboard-editor.js` exclusively inspected comment prefixes (`#`) and ignored folded scalar / sub-step indicator patterns (`>-`, `- >-`, `  - `).
  2. `formatStepToTokens()` lacked a replacement rule for escaped folded scalar indicator tokens (`&gt;-`), leaving raw indicator text unstyled.
  3. `dashboard-styles.css` lacked a dedicated CSS badge class (`.substep-folded-badge`) for styling folded scalar tokens.
  4. `serializeStepRows()` in `compilePlaybookToYaml()` did not strip `>-` or `- >-` prefixes when serializing manually converted sub-steps, leaving redundant prefixes in folded scalar output blocks.
  5. `AuraFileService.extractSectionStepsWithComments()` in `neodymium-core` dropped inline text immediately following `- >-` due to an unconditional `continue` statement without checking if step text was present on the same line.
- **Detection Gap ("What did we miss?"):**
  Existing UI tests verified static rendering of sub-step rows parsed from pre-existing files on disk (`testOpenFoldedScalarStepsRendersDiscreteRowsInVisualEditor`), but lacked interaction tests simulating active typing of `>-` or `- >-` into normal step lines, immediate CSS class toggling, hierarchical renumbering, dynamic toggle bar updates, and subsequent round-trip persistence.
- **Resolution:**
  1. Updated `checkAndUpdateCommentRow()` across `aura-manager` and `neodymium-core` (`dashboard-editor.js`) to detect `isSubstepPrefix = text.startsWith('>-') || text.startsWith('- >-') || text.startsWith('  - ')`, automatically adding `.step-row-substep`, setting `data-manual-substep="true"`, recomputing line numbers with `updateAllLineNumbers()`, and refreshing toggle headers via `refreshAllSubstepToggleBars()`.
  2. Implemented immediate reversion: removing the `>-` prefix immediately clears `.step-row-substep` from manual sub-steps, re-indexes sequential integer step numbering, and removes vacant toggle bars.
  3. Added `.substep-folded-badge` style rules in `dashboard-styles.css` (indigo monospace badge with rounded corners, subtle border, and dark mode palette).
  4. Updated `formatStepToTokens()` in `dashboard-editor.js` to render `<span class="substep-folded-badge">&gt;-</span>`.
  5. Updated `serializeStepRows()` in `compilePlaybookToYaml()` to recognize `>-` / `- >-` prefixes, strip the prefix from the step body, and emit clean folded scalar YAML (`  - >-\n    <cleaned step>\n`).
  6. Updated `AuraFileService.extractSectionStepsWithComments()` in `neodymium-core` to capture inline step text when present after `- >-`.
  7. Bumped asset query strings to `?v=1.2.4` across templates (`index.html`, `dashboard.html`, `neodymium-aura-dashboard.html`).
  8. Enforced scoped indicator hiding: added `.step-row-substep .substep-folded-badge { display: none !important; }` in `dashboard-styles.css` so that the `>-` badge is only hidden after the sub-steps element (`.step-row-substep` / `.substeps-toggle-bar`) is already displayed, while remaining visible when sub-steps are not active.
- **Safety Net Added:**
  - `com.xceptance.aura.test.ui.AuraManagerEditorUiTest#testTypeFoldedScalarIndicatorStylesRowAsSubstepImmediatelyAndPersistsOnSave`
    - Asserts row 2 begins as a normal step numbered 2 without toggle bar.
    - Types `>- Verify the cart header is displayed.` and asserts immediate application of `.step-row-substep`, hierarchical numbering `1.1`, and creation of `.substeps-toggle-bar` with `1 sub-step`.
    - Deletes `>- ` and asserts immediate reversion to normal step 2 and removal of `.substeps-toggle-bar`.
    - Re-types `>- Verify the cart header is displayed.`, saves file, and asserts disk YAML contains clean `- >-` folded scalar structure.
    - Reloads page and asserts row 2 retains `.step-row-substep`, `1.1` numbering, `.substeps-toggle-bar`, and verifies that the `>-` badge is cleanly hidden (`substep-folded-badge` not visible) once the sub-steps element is displayed.
