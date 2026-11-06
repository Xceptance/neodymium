# [DEF-20261029-1648] Status Pie Chart Healed and AI Sub-Arcs Clobber Base Status Colors

- **Status:** `Resolved`
- **Opened:** 2026-10-29 16:48
- **Closed:** 2026-10-29 16:51
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In the dashboard status pie/donut chart with healed/AI breakdown enabled, AI tests belonging to failed categories (Known Fail, Unknown Fail) were rendered as solid purple arcs rather than orange or red, and healed tests were rendered as green regardless of failure category.
- **Root Cause:** In `dashboard-widgets.html`, the sub-arc strokes for `s.aiCount > 0` and `s.healedCount > 0` were hardcoded to `var(--badge-ai-bg, #a78bfa)` (purple) and `var(--status-pass-healed, #34d399)` (green) respectively, overwriting the slice's base status color `s.color`.
- **Detection Gap ("What did we miss?"):** Prior unit tests verified that `s.aiDashArray` and `s.healedDashArray` were non-empty and that badge counts matched, but did not assert that the rendered SVG arc strokes preserved status-specific colors across failures.
- **Resolution:**
  1. Updated `WidgetPieSliceDto` to compute `healedColor` tailored to the status slice category (`--status-pass-healed`, `--status-fixed-healed`, `--status-known-healed`, `--status-unknown-healed`).
  2. Updated `dashboard-widgets.html` so AI sub-arcs use `th:attr="stroke=${s.color}"` and healed sub-arcs use `th:attr="stroke=${s.healedColor}"`, preserving status category identity beneath the pattern overlays.
- **Safety Net Added:** Added unit test `WidgetAggregationServiceTest.testStatusPieChart_HealedAndAiSubArcsPreserveCategoryStatusColors` asserting status-specific healed and AI colors for Known Fail and Unknown Fail slices.
