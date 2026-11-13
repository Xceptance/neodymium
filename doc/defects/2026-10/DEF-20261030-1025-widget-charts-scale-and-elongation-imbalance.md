# [DEF-20261030-1025] Disproportionate Sizing of Non-Trend Widget Charts in Elongated Dashboard Rows

- **Status:** `Resolved`
- **Opened:** 2026-10-30 10:25
- **Closed:** 2026-10-30 10:28
- **Component:** `aura-manager`
- **Scope:** `UI / Styles`
- **Symptom:** When trend charts were enlarged to a 220px SVG container (~320px total card height) to accommodate milestone axes and legend labels, CSS grid row stretching expanded adjacent widget cards (Pie Chart, Ratio Bar, Perlenkette) to match this taller height. However, the inner diagrams remained miniature (e.g. 84px donut, 18px ratio bar), causing them to look dwarfed and lost amidst large empty whitespace.
- **Root Cause:** 
  1. An inline style override `style="width: 84px; height: 84px; flex-shrink: 0;"` in `dashboard-widgets.html` artificially restricted the donut chart to a miniature size, overriding the default 140px styling in `report-manager.css`.
  2. The center rate and breakdown list typography was scaled for small cards (`0.75rem` text and 8px dots) rather than adapting to the expanded card height.
  3. Ratio bars and Perlenkette bead diagrams lacked widget-specific height rules to scale proportionally with taller dashboard cards.
- **Detection Gap ("What did we miss?"):** Visual layout tests evaluated individual widget cards in isolation rather than reviewing multi-widget grid rows where mixed presentation types stretch to the tallest neighbor in the row.
- **Resolution:**
  1. Removed the hardcoded 84px inline style and introduced dedicated `.widget-pie-card` scaling rules in `dashboard-styles.css`, expanding the donut wrapper to 150px by 150px with `1.85rem` center typography and `0.85rem` breakdown labels.
  2. Balanced card body layout with `justify-content: space-around;` and generous vertical padding (`min-height: 240px;`).
  3. Scaled `.widget-ratio-card` stacked bar height to 32px with 10px legend dots.
  4. Enlarged `.widget-perlenkette-card` beads to 42px with 18px pulse centers and 0.82rem badge text.
- **Safety Net Added:** Verified via `StaticResourceSyntaxTest` and `WidgetAggregationServiceTest` (60/60 tests passing) ensuring all CSS rules and HTML fragments render cleanly with zero regressions.
