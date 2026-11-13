# [DEF-20261027-1143] Batch Latest Status Pie Breakdown Horizontal Overflow and Layout Overlap

- **Status:** `Resolved`
- **Opened:** 2026-10-27 11:43
- **Closed:** 2026-10-27 11:46
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In the latest test status card on the batch history and batch overview dashboards, breakdown elements beneath the donut chart overflowed their 175px container when test runs contained healed or AI tests. The labels, badges (`✨ 83 healed`, `48 AI`), and test execution counts collided on a single line, clipping into the divider and overlapping the adjacent Contributing Runs section, while leaving massive unutilized empty space beneath the chart.
- **Root Cause:** In `report-manager.css`, `.latest-status-chart-group` was fixed to `width: 175px`, while `.pie-breakdown-item` used a single-line horizontal flex row (`display: flex; justify-content: space-between;`) grouping status dot, label, healed badge, AI badge, and count. Inline width required exceeded 240px. Additionally, no summary element existed to occupy the vertical space created when the card stretched to match the adjacent Contributing Runs list height.
- **Detection Gap ("What did we miss?"):** Existing test cases in `StaticResourceSyntaxTest` and `AuraReportViewControllerSpringContextTest` validated JavaScript syntax and Spring context instantiation, but did not assert multi-line breakdown wrapping or the presence of sub-badge containers and total execution tiles.
- **Resolution:**
  1. Restructured `.pie-breakdown-item` in `batch-history.html` and `batch-overview.html` into a multi-line card layout with `.pie-breakdown-main-row` (label, dot, and count) and an indented `.pie-breakdown-sub-row` (healed and AI badges).
  2. Added `.pie-breakdown-total-tile` to utilize the bottom vertical space by cleanly displaying total executions.
  3. Styled `.pie-breakdown-main-row`, `.pie-breakdown-sub-row`, and `.pie-breakdown-total-tile` in `report-manager.css` while preserving backward compatibility for single-line `.val-group` items. Increased chart group width to `185px`.
  4. Synchronized dynamic recalculation of `batchLatestTotalVal` and `batchLatestTotalTile` in `report-manager.js`.
- **Safety Net Added:** Added automated regression test assertions in `StaticResourceSyntaxTest.testAllStaticJavaScriptAssetsHaveValidSyntax` and template structure verification in `StaticResourceSyntaxTest.testBatchHistoryAndOverviewPieBreakdownMultiLineStructure`.
