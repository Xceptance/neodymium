# [DEF-20261008-1510] Pie Chart Breakdown Text Misalignment & Zero-Count Clutter (Batch & Report Overview)

- **Status:** `Resolved`
- **Opened:** 2026-10-08 15:10
- **Closed:** 2026-10-08 15:24
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:**
  1. In the Batch Overview card (`batch-overview.html`), the "Last Execution" pie chart breakdown text was misaligned. For rows with healed tests (such as `Passed`), the count (`1`) shifted upwards and left-aligned directly above the `✨ 1 healed` badge, while rows without healed badges (such as `Fixed`) had their count pushed to the far right. In addition, the header text wrapped awkwardly across multiple fragmented lines.
  2. In the Run Report Overview fragment (`run-report.html`), the Area Execution Breakdown placed counts before the healed badges, creating an uneven jagged column across rows. Furthermore, statuses with 0 counts (which had no slice in the pie chart SVG) were still listed in the breakdown list.
- **Root Cause:**
  1. In `batch-overview.html`, breakdown items under `.pie-breakdown-list-sm` placed both the count and healed badge inside an unconstrained block `<div class="val">`, causing multi-line wrapping and vertical centering displacement within `.pie-breakdown-item`.
  2. In `run-report.html`, `.val-group` placed `<span class="val">` before the optional `<span class="badge-healed-inline">`, causing numbers to be placed at different horizontal positions across rows depending on whether a healed badge was present.
  3. `run-report.html` lacked `th:if="${area.<status>Count > 0}"` guards on breakdown rows, causing 0-count statuses not rendered in the pie SVG to clutter the breakdown list.
- **Detection Gap ("What did we miss?"):** Existing DOM tests asserted presence of status badges and numbers in the HTML output, but did not assert CSS flex-column alignment or check that only active non-zero pie segments are displayed in breakdown lists.
- **Resolution:**
  1. Updated `report-manager.css` to define `.pie-breakdown-list-sm .val-group` and `.pie-breakdown-item .val-group` with `display: flex; align-items: center; gap: 0.35rem; white-space: nowrap;`, and `.val` with `min-width: 14px; text-align: right; font-family: var(--font-mono); font-weight: 700;`.
  2. Updated `batch-overview.html` and `run-report.html` to place `<span class="badge-healed-inline ...">` before the right-aligned `<span class="val ...">`, guaranteeing uniform right-aligned tabular number columns.
  3. Added `th:if="${area.<status>Count > 0}"` guards to all breakdown items in `run-report.html` so statuses not present in the SVG chart are omitted, and added a fallback message when no executions exist.
  4. Restructured `last-run-header` into a two-line layout: Line 1 displays `Last Execution` and the pass rate badge; Line 2 displays the execution timestamp with a clock icon.
- **Safety Net Added:** Verified layout and visual rendering across responsive widths in both Light and Dark modes (`data-bs-theme="dark"` / `force-dark`), and verified compilation and template parsing.

