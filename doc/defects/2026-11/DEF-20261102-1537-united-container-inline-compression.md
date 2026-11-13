# [DEF-20261102-1537] United Widget Container Forces All Entries In-Line Compressing Multi-Widget Groups

- **Status:** `Resolved`
- **Opened:** 2026-11-02 15:31
- **Closed:** 2026-11-02 15:38
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** When multiple widgets (e.g. 3, 4, 5, or more widgets) were combined into a united container (`.widget-container-card`), all child sections were forced into a single horizontal row (`in-line`). In a standard 1200px dashboard layout, 5 widgets were compressed into narrow ~200px columns, causing pie chart breakdown labels to wrap awkwardly, ratio bars to squash, and charts to clip, while vertical height remained flat and underutilized.
- **Root Cause:** `.widget-container-unified-row` in `dashboard-styles.css` was styled with `display: flex; align-items: stretch;` with `flex-wrap: nowrap;`, and child section classes (`.widget-container-pie-section`, `.widget-container-ratios-section`) assumed a fixed 2-widget pair layout rather than an arbitrary multi-widget collection.
- **Detection Gap ("What did we miss?"):** `StaticResourceSyntaxTest.java` verified class name existence (`.widget-container-unified-row`, `.widget-container-pie-section`, `.widget-container-ratios-section`, `.widget-container-divider`) but lacked structural assertions checking responsive multi-row grid wrapping or minimum column bounds for multi-child containers.
- **Resolution:**
  1. Updated `.widget-container-unified-row` in `dashboard-styles.css` to use a responsive multi-row grid: `display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 440px), 1fr)); gap: 1.25rem; width: 100%;`.
  2. Styled each `.widget-container-section` as a dedicated sub-panel with `border: 1px solid var(--border, rgba(255, 255, 255, 0.08))`, `border-radius: var(--radius-md, 6px)`, `background: var(--bg-surface, rgba(255, 255, 255, 0.02))`, and `padding: 0.9rem 1.15rem`.
  3. Added `.widget-container-section.widget-container-full-span { grid-column: 1 / -1; }` and bound it in `dashboard-widgets.html` for presentations requiring full width (`TREND_GRAPH` and `PERLENKETTE`).
  4. Set `.widget-container-divider { display: none; }` in grid mode to avoid creating empty grid column cells while preserving individual sub-card framing.
- **Safety Net Added:** Added automated regression test `StaticResourceSyntaxTest.testUnitedContainerMultiRowGridLayoutAndFullSpanSupport()` asserting grid definition, auto-fit minmax configuration, sub-panel full-span styling, and dynamic HTML binding.
