# [DEF-20261109-1145] Container Pie Chart Filtering Visual Artifacts, Boundary Shearing, and Donut Slice Desynchronization

- **Status:** `Resolved`
- **Opened:** 2026-11-09 11:45
- **Closed:** 2026-11-09 13:00
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In a unified widget group (e.g. "Latest test status information"), clicking a pie chart legend breakdown item (e.g. `Succeeded Fixed 1 (0.8%)`) caused multiple visual and data synchronization defects:
  1. **Disjointed Overlay Artifacts & Cracks:** Unselected base slices dimmed to `opacity: 0.38`, but healed sparkle (`#sparkle-pat-donut`) and AI stripe (`#ai-pat-donut`) overlay `<circle>` elements stayed at `opacity: 1.0` and drew harsh white diagonal stripes and sparkle patches cutting across dimmed slices.
  2. **Filter Badge Truncation:** The active filter indicator was not visible on the card because `.container-active-filter-strip` was nested inside `.widget-card-title` at the tail end of a non-wrapping flex badge line (`Linked Widgets`, `Latest Test Status`, `Identity`), getting clipped off-screen by `.widget-container-card { overflow: hidden }`.
  3. **Boundary Shearing on Donut Ring:** The outer stroke and drop shadow of the donut circle were flattened/sheared off on the top, left, and bottom edges instead of rendering as a smooth circle.
  4. **Donut Slices Desynchronized from Filtered Data:** Even when the center KPI showed `100% PASS` and summary showed `1 of 118 Tests`, the donut ring continued to display 84.7% green (`Passed`), 13.6% red (`Unknown Fail`), and a 0.8% blue sliver (`Succeeded Fixed`). Unselected slices remained visible in the donut ring contradicting the 100% KPI.
- **Root Cause:**
  1. In `dashboard-widgets.html`, base donut slice circles and their overlay circles (`healed`, `sparkle`, `ai`, `stripes`) were rendered as flat siblings without an enclosing `<g>` element. Interactive filter selectors only targeted base circles, leaving overlay patterns unaffected by opacity dimming. Furthermore, when dimmed, decorative pattern strokes remained rendered over low-opacity arcs.
  2. `.container-active-filter-strip` was positioned inside `.widget-card-title` alongside badge groups with `flex-nowrap`, causing horizontal overflow inside `.widget-container-card` which has `overflow: hidden`.
  3. In `dashboard-widgets.html`, the SVG declaration used `viewBox="0 0 36 36"` with center `(18, 18)`, radius `r="15.91549430918954"`, and `stroke-width="5.0"` (or `5.5` on hover/active). The outer radius reached `18 + 15.915 + 2.5 = 36.415` (and drop-shadow spread further), exceeding the coordinate space `[0, 36]`, clipping the outer circumference against SVG canvas boundaries.
  4. In `report-manager.js`, `recalculateStatusPieChart`, `recalculateAiUsagePieChart`, `recalculateTagPieChart`, `recalculateBugsPieChart`, and `recalculateFlakinessPieChart` computed slice stroke-dasharrays and offsets using unfiltered `variations` instead of container `effectiveVariations`. Slices with count 0 were not hidden, and the active filtered slice did not expand to fill the 100% donut ring.
- **Detection Gap ("What did we miss?"):**
  Previous tests for container filtering (`StaticResourceSyntaxTest.java`) verified facet exclusion logic and list filtering across distinct cards, but lacked assertions for:
  - SVG viewBox margin padding (`viewBox="-2 -2 40 40"` with `style="overflow: visible;"`) to accommodate stroke width and glow filters.
  - CSS suppression of decorative overlay circles on dimmed slices (`:not(.is-filter-active) circle:not(.pie-slice-base)`).
  - Proper intra-donut SVG layer encapsulation (`<g class="pie-slice-group">`).
  - Dedicated filter strip positioning outside badge headers.
  - Slices expanding to `100.00 0.00` and zero-count slices hiding (`display: none`) when filtered in DOM simulations.
- **Resolution:**
  1. **SVG ViewBox Boundary:** Updated SVG in `dashboard-widgets.html` from `viewBox="0 0 36 36"` to `viewBox="-2 -2 40 40" style="overflow: visible;"`, providing 2 units of padding in all directions to completely eliminate stroke clipping and drop shadow truncation.
  2. **Layer Grouping & Pattern Suppression:** Grouped all SVG slice layers (base, healed, sparkle, AI, stripes) into `<g class="pie-slice-group widget-interactive-filter" ...>` in `dashboard-widgets.html`. In `dashboard-styles.css`, added rule `.has-active-cross-filter g.pie-slice-group.widget-interactive-filter:not(.is-filter-active) circle:not(.pie-slice-base) { display: none !important; }` to completely remove decorative stripe/sparkle overlays from inactive dimmed slices.
  3. **Dedicated Container Filter Strip:** Extracted `.container-active-filter-strip` out of `.widget-card-title` into a dedicated container-wide bar beneath `.widget-container-header` with an "Active Filter:" label, flex wrapping, and clear button.
  4. **Data Synchronization in Pie Charts:** Updated `recalculateStatusPieChart`, `recalculateAiUsagePieChart`, `recalculateTagPieChart`, `recalculateCostsPieChart`, `recalculateBugsPieChart`, and `recalculateFlakinessPieChart` in `report-manager.js` to compute `effectiveVariations` against active container filters. The active slice expands to `100.00 0.00` (full donut), zero-count slices receive `display: none`, center rate reflects strictly filtered items (e.g. `100% PASS`), and summary text displays `1 of 118 Tests`.
  5. **Filter Clearing:** Updated `clearContainerCrossFilter` to restore original summaries, restore slice displays, and reset slice group opacities.
- **Safety Net Added:**
  Added automated regression test `StaticResourceSyntaxTest.testContainerPieChartSvgLayerGroupingAndOverlaySynchronization` validating:
  1. Grouped `<g class="pie-slice-group widget-interactive-filter">` and layer classes (`pie-slice-base`, `pie-slice-healed`, `pie-slice-sparkle`, `pie-slice-ai`, `pie-slice-stripes`) in `dashboard-widgets.html`.
  2. Expanded `viewBox="-2 -2 40 40" style="overflow: visible;"` in `dashboard-widgets.html`.
  3. Dedicated `.container-active-filter-strip` bar outside header title.
  4. CSS rules in `dashboard-styles.css` including overlay suppression on dimmed slices.
  5. Full Node.js VM DOM simulation of filter click on `Succeeded Fixed`, verifying:
     - Active slice opacity `1.0` with `is-filter-active` class.
     - Dimmed slices opacity `0.38`.
     - Active base slice expanding to `stroke-dasharray="100.00 0.00"`.
     - Zero-count slices set to `display: none`.
     - Center rate updating to `100% PASS`.
     - Summary count updating to `1 of 3 Tests`.
     - Complete restoration to original display, opacities, and counts upon filter clearing.
