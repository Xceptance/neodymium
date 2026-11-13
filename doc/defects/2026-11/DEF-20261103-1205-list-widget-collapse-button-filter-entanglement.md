# [DEF-20261103-1205] List Widget Collapse Button Inadvertently Triggers Container Cross-Filtering

- **Status:** `Resolved`
- **Opened:** 2026-11-03 12:05
- **Closed:** 2026-11-03 12:08
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In united container widgets on the dashboard, clicking the chevron button to collapse or expand list item sub-items in a list widget inadvertently triggered cross-filtering across the entire dashboard container.
- **Root Cause:** The collapse chevron button was nested inside `div.widget-list-row`, which itself was designated as `.widget-interactive-filter` with an inline click handler that executed both `toggleWidgetListItemCollapse(this, event)` and `onContainerWidgetFilterClick(this, event)`. Clicking the collapse button therefore dispatched the container filter click handler.
- **Detection Gap ("What did we miss?"):** Existing test coverage in `StaticResourceSyntaxTest.java` verified that `recalculateContainerList` hid non-matching items and managed empty filter states, but did not assert event isolation between the collapse/expand trigger and the container cross-filter dispatcher.
- **Resolution:** Decoupled the collapse button and filter target in `dashboard-widgets.html`. The chevron button now strictly triggers `toggleWidgetListItemCollapse` with `event.stopPropagation()`, and the interactive filter is scoped to the item label chip (`.widget-list-label.widget-interactive-filter`). Updated `report-manager.js` to resolve the chevron directly when called from the collapse button element, and updated `dashboard-styles.css` with explicit pointer cursors and chip hover/active styles.
- **Safety Net Added:** Added `testUnitedContainerListWidgetCollapseDecoupledFromFilter` in `StaticResourceSyntaxTest.java` verifying that `widget-list-row` no longer carries `widget-interactive-filter`, the collapse chevron invokes only `toggleWidgetListItemCollapse`, the interactive filter is scoped to `.widget-list-label`, and `toggleWidgetListItemCollapse` resolves the chevron directly.
