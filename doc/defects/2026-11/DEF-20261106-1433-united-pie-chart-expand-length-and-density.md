# [DEF-20261106-1433] Widget Group Pie Chart Leaves Wasted Space and Lacks Vertical Expansion

- **Status:** `Resolved`
- **Opened:** 2026-11-06 14:33
- **Closed:** 2026-11-06 14:48
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** Pie/donut charts inside united widget groups rendered at a small fixed size (130px), leaving substantial dead space in standard cards. When expanded to full width via `toggleWidgetSectionSpan`, the container expanded only horizontally while vertical height remained flat (~160px), the donut remained at 130px, and breakdown items remained compressed into a single 250px column, wasting over half the row width.
- **Root Cause:** `.widget-container-pie-section` enforced `130px !important` for `.pie-chart-wrapper-batch` and lacked specialized `.widget-container-full-span` rules to expand height (`min-height`), scale donut dimensions, and distribute legend slices into a multi-column responsive grid.
- **Detection Gap ("What did we miss?"):** `StaticResourceSyntaxTest.java` asserted class presence (`.widget-container-pie-section`, `.widget-container-full-span`) but lacked verification for expanded vertical dimensions (`min-height`), scaled donut dimensions, or multi-column breakdown grid behavior.
- **Resolution:**
  1. Enlarge unexpanded donut chart in `.widget-container-pie-section` from 130px to 160px with matching rate typography (`1.85rem`) and increased legend flex bounds (`max-width: 320px`).
  2. Introduce dedicated `.widget-container-pie-section.widget-container-full-span` styling providing vertical expansion (`min-height: 280px !important`), scaled donut chart (`220px !important`), prominent typography (`2.6rem`), and a responsive multi-column slice grid (`grid-template-columns: repeat(auto-fit, minmax(220px, 1fr))`) to use the full horizontal and vertical space.
- **Safety Net Added:** Added automated regression test `StaticResourceSyntaxTest.testUnitedPieChartExpandedAndDefaultDensityLayout()` verifying default and expanded dimensions, vertical height, and responsive grid layout.
