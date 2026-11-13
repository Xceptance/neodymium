# [DEF-20261026-1102] Test Variation History Drawer Shrink Crushes Runs Overview Top Grid and Table Layout

- **Status:** `Resolved`
- **Opened:** 2026-10-26 11:02
- **Closed:** 2026-10-26 11:35
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** When clicking on an entry in the "Known Test Base Variations" table on `/batch-history`, opening `#testSidePagePanel` (920px width) shrunk the main overview container from ~1670px down to ~740px. The top overview `.batch-top-overview-panel` maintained a rigid 2-column flex/grid layout on wide viewports (1920px), crushing `.batch-latest-status-card` to ~430px, forcing `.contributing-runs-list` into ~200px width with horizontal scrollbars and squished progress bars, and forcing the status card height to stretch to match 5 stacked ratio cards (1120px tall) leaving an ~870px empty white void below the donut chart. Furthermore, table columns suffered text clipping (`LAST STATU`) due to rigid column percentages.
- **Root Cause:**
  1. `.batch-top-overview-panel` responsive collapse was gated exclusively by a viewport media query (`@media (max-width: 1100px)`), which did not trigger when a drawer shrunk the flex-child container on 1920px desktop viewports.
  2. `.batch-latest-status-card` had `height: 0; min-height: 100%;` to match the right column's height; when collapsing to 1 column or when shrunk, this caused massive vertical elongation to match the 5 stacked ratio cards.
  3. `.contributing-runs-list` lacked `overflow-x: hidden;`, causing horizontal scrollbar artifacts.
  4. The variations table `<colgroup>` allocated 38% to test name and only 18% to browser and 18% to status, clipping the status column headers in compact mode.
  5. `openTestSidePagePanel` and `initPanelResizer` lacked a minimum viewport boundary safeguard, allowing the side drawer to squeeze `#appMain` below readable widths.
- **Detection Gap ("What did we miss?"):** Previous static resource syntax tests verified that CSS files had media queries and JS had valid syntax, but did not test component-level container responsiveness or DOM behavior under drawer expansion.
- **Resolution:**
  1. Added `container-type: inline-size;` to `.content-body` and paired it with `@container (max-width: 1100px)` alongside `body:has(#testSidePagePanel.active) .batch-top-overview-panel` in `report-manager.css` to collapse the top overview into a single column whenever the side drawer is open or available width drops below 1100px.
  2. Set `height: auto !important; min-height: auto !important;` on `.batch-latest-status-card` in responsive mode to eliminate the empty vertical gap below the donut chart.
  3. Converted `.batch-ratios-stack-column` into an auto-fit grid (`grid-template-columns: repeat(auto-fit, minmax(210px, 1fr))`) when stacked.
  4. Added `overflow-x: hidden;` to `.contributing-runs-list`.
  5. Rebalanced variations table `<colgroup>` in `batch-history.html` to `30%`, `13%`, `25%`, `14%`, `18%`.
  6. Clamped side panel width in `report-manager.js` (`window.innerWidth - 420`) ensuring `#appMain` always retains at least 420px.
  7. Bumped CSS version to `?v=1.1.9` in `index.html`.
- **Safety Net Added:** `StaticResourceSyntaxTest.testBatchVariationDrawerResponsiveLayoutSafetyNet` verifying container queries, active drawer layout rules, horizontal overflow suppression, drawer width clamping, and table column width allocations.
