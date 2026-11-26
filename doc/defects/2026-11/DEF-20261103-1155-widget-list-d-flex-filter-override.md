# [DEF-20261103-1155] United Container List Widget d-flex Display Override and Label Matching Leakage

- **Status:** `Resolved`
- **Opened:** 2026-11-03 11:55
- **Closed:** 2026-11-03 11:57
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:**
  1. In united container widgets with a LIST presentation (e.g. status list), selecting the `Passed (Healed)` filter updated the group and section count badges to 5, but unhealed tests remained visible in the list.
  2. In the bug list widget, when zero bugs matched the active filter, the header showed `0 Bugs` and the note "No items match current filters" was rendered, yet the list of bug cards and the `MAJOR 1` group header remained visible alongside the empty state message.
- **Root Cause:**
  List presentation DOM elements (`.widget-list-group`, `.widget-list-subitem-row`, `.widget-list-subitems`) have Bootstrap's `.d-flex` class, which declares `display: flex !important;` in `dashboard-styles.css`. In `report-manager.js`, `recalculateContainerList` attempted to hide filtered elements using inline `element.style.display = 'none'`. Under CSS cascade rules (Origin & Importance), author stylesheet rules with `!important` take precedence over normal inline declarations without `!important`, causing the browser to ignore the hide command. Additionally, `recalculateContainerList` contained a loose fallback `mk.includes(subLabel)` that allowed non-matching variations sharing the same test method and dataset title to produce false-positive matches against healed variation keys.
- **Detection Gap ("What did we miss?"):**
  Existing test `StaticResourceSyntaxTest.testUnitedContainerListWidgetFilteringHidesNonMatchingItems` verified the presence of `sub.style.display = 'none';` and `grp.style.display = 'none';` as string literals in `report-manager.js`, but did not assert that `!important` or high-specificity class overrides were used to overcome Bootstrap's `.d-flex` rule.
- **Resolution:**
  1. Added `.widget-list-group.is-filtered-hidden`, `.widget-list-subitem-row.is-filtered-hidden`, `.widget-list-group.collapsed > .widget-list-subitems`, and `.widget-list-subitems.is-collapsed-hidden` with `display: none !important;` to `dashboard-styles.css` and `report-manager.css`.
  2. In `report-manager.js`, updated `recalculateContainerList` and `toggleWidgetListItemCollapse` to apply `.is-filtered-hidden` / `.is-collapsed-hidden` and `style.setProperty('display', 'none', 'important')` when hiding, and cleanly remove both when displaying.
  3. Eliminated loose `subLabel` substring matching in `recalculateContainerList` so variations and bugs match strictly on canonical identifiers.
  4. Added `data-subitem-healed` and `data-subitem-ai` attributes to `.widget-list-subitem-row` in `dashboard-widgets.html`.
- **Safety Net Added:**
  Updated `@Test` `StaticResourceSyntaxTest.testUnitedContainerListWidgetFilteringHidesNonMatchingItems` asserting that `is-filtered-hidden` class toggling and `display: none !important;` overrides are present in both JavaScript and CSS.
