# [DEF-20261023-1142] Batch History Contributing Runs List Fixed Max Height Void

- **Status:** `Resolved`
- **Opened:** 2026-10-23 11:42
- **Closed:** 2026-10-23 12:04
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In the Batch History Overview (`/batch-history`), the "Latest Test Status" card stretches vertically to match the height of the right-hand ratio cards column. However, `.contributing-runs-list` displayed only ~3 runs inside a fixed 155px container, leaving a large empty white space between the contributing runs list and the "Open Latest Report" button footer, while the horizontal breakdown legend occupied excessive lateral space next to the donut chart.
- **Root Cause:** A rigid `max-height: 155px;` was configured on `.contributing-runs-list` in `report-manager.css`, horizontal row flex on `.latest-status-chart-group` widened the chart block unnecessarily, and combining `calc()` with `fr` flexible length units (`calc(1fr + 225px)`) violated the W3C CSS Grid specification causing the browser parser to discard the declaration and fall back to single-column stacking.
- **Detection Gap ("What did we miss?"):** CSS syntax validation failed to flag that `calc()` cannot combine flexible fraction units (`fr`) with fixed `<length>` values in grid track sizing.
- **Resolution:**
  1. Updated `report-manager.css` to add `min-height: 0;` on `.latest-status-body` and `.contributing-runs-section`.
  2. Stacked the donut chart status breakdown legend (`Passed`, `Unknown Fail`, `Ignored`) vertically directly under the circular 38% PASS donut chart (`flex-direction: column; width: 175px;`).
  3. Rebalanced `.batch-top-overview-panel` with `grid-template-columns: minmax(0, 1.85fr) minmax(0, 1fr);` and reduced inter-container gap to `0.75rem` (and `.batch-ratios-stack-column` gap to `0.55rem`), providing ample space for the Latest Test Status card while compressing the ratio cards and tightening padding.
  4. Adjusted `.contributing-runs-list` to `max-height: 275px;` with `overflow-y: auto;`, completely filling the vertical space down to the divider line above the "Open Latest Report" button with 5 full run items while strictly preserving smooth scrolling.
  5. Bumped `report-manager.css` query parameter version in `index.html` to `?v=1.1.6` for browser asset cache busting.
- **Safety Net Added:** Verified flex constraints across desktop grid tracks and responsive viewports; stylesheet verified with no CSS syntax issues.
