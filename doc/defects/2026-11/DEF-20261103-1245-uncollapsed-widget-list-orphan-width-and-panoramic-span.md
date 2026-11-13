# [DEF-20261103-1245] Uncollapsed List Items Confined to 1-Column Track Leaving 66% Empty Whitespace

- **Status:** `Resolved`
- **Opened:** 2026-11-03 12:30
- **Closed:** 2026-11-03 12:45
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** When uncollapsing list groups (such as `Passed (113)`) in a `LIST` widget inside a unified container, the dense sub-item rows (test method names, classes, run identifiers, and multiple tag chips) were constrained to a single 440px grid track. Because the widget wrapped to a second row with no other sibling widgets, the remaining ~66% of the row width was completely blank whitespace, while the sub-items were cramped and stacked into thousands of vertical pixels.
- **Root Cause:** In `dashboard-styles.css`, `.widget-container-unified-row` configured `display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 440px), 1fr));`. Unlike `TREND_GRAPH` and `PERLENKETTE` which unconditionally received `widget-container-full-span` (`grid-column: 1 / -1`), `LIST` widgets were given `.widget-container-generic-section` which defaulted to a single column track regardless of whether sub-items were collapsed or uncollapsed, and `.widget-container-generic-section .widget-card-body` lacked a `max-height` scroll cap.
- **Detection Gap ("What did we miss?"):** Previous static resource syntax tests verified that `toggleWidgetListItemCollapse` toggled `.collapsed` and `.is-collapsed-hidden` without invoking filter handlers, but did not assert responsive grid adaptation, parent section spanning, or desktop panoramic row formatting when list groups are expanded.
- **Resolution:**
  1. Updated `toggleWidgetListItemCollapse(rowEl, event)` in `report-manager.js` to dynamically add `.widget-container-full-span` and `.is-uncollapsed-expanded` to the parent section when any group is expanded, and remove it once all groups are collapsed.
  2. Implemented `toggleWidgetSectionSpan(btn, event)` and added the `btn-widget-section-span` toggle button in `dashboard-widgets.html` (`widget-section-header`) for explicit manual full-span toggling.
  3. Added `max-height: 520px; overflow-y: auto;` to `.widget-list-subitems` in `dashboard-styles.css` to prevent vertical layout blowout.
  4. Configured responsive single-row panoramic tabular alignment for `.widget-container-full-span .widget-list-subitem-row` on screens $\ge 992\text{px}$.
- **Safety Net Added:** Added `@Test` `StaticResourceSyntaxTest.testUnitedContainerListWidgetAdaptiveFullSpanAndPanoramicRow` verifying HTML button bindings, JavaScript adaptive span management, scroll cap, and desktop panoramic flex styling.
