# [DEF-20261012-1540] Pressing Enter in Sub-steps Area Lacked Sub-step Propagation and Double-Enter Exit

- **Status:** `Resolved`
- **Opened:** 2026-10-12 15:37
- **Closed:** 2026-10-12 15:48
- **Component:** `aura-manager` / `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** Pressing `Enter` on a sub-step row immediately inserted an un-nested regular top-level step rather than continuing to add a new sub-step at the same hierarchical level. Additionally, there was no keyboard workflow to exit the sub-steps area by pressing `Enter` twice on an empty sub-step line.
- **Root Cause:** In `dashboard-editor.js`, `insertStepBelow()` unconditionally constructed `.step-row` elements without checking whether `targetRow` possessed `.step-row-substep` or propagating `data-manual-substep="true"`. Furthermore, `handleKeyDown()` under `event.key === 'Enter'` lacked detection for empty sub-step lines to de-escalate them back to the parent step level.
- **Detection Gap ("What did we miss?"):** Existing visual editor keyboard navigation tests only exercised `Enter` on top-level steps and `Backspace` at offset 0 for un-indenting, but lacked test coverage for Enter event propagation inside sub-step groups and double-Enter exit semantics.
- **Resolution:**
  1. Updated `insertStepBelow()` in `dashboard-editor.js` (both `aura-manager` and `neodymium-core`) so that newly inserted rows inherit `.step-row-substep` and `data-manual-substep="true"` when the preceding reference row is a sub-step.
  2. Updated `handleKeyDown()` under `event.key === 'Enter'` to detect if `currentRow.classList.contains('step-row-substep') && raw.trim() === ''`. When encountered (a second Enter pressed on an empty sub-step row), it removes `.step-row-substep` and `data-manual-substep="true"`, updates numbering via `updateAllLineNumbers()`, refreshes the toggle bar via `refreshAllSubstepToggleBars()`, recompiles the YAML playbook, and keeps caret focus in the now-top-level step line.
  3. Bumped asset cache versions to `?v=1.2.6` across `index.html` and `dashboard.html`.
  4. Added automated integration test `testPressEnterInSubstepsAreaAddsNewSubstepAndDoubleEnterExitsSubstepsArea` in `AuraManagerEditorUiTest`.
- **Safety Net Added:** `AuraManagerEditorUiTest.testPressEnterInSubstepsAreaAddsNewSubstepAndDoubleEnterExitsSubstepsArea` in `aura-manager`.
