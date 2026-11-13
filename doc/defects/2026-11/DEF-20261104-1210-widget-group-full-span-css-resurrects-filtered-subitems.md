# [DEF-20261104-1210] Full-Span Panoramic CSS Rule Resurrects Filtered Subitem Rows via !important Collision

- **Status:** `Resolved`
- **Opened:** 2026-11-04 12:10
- **Closed:** 2026-11-04 12:15
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In a united widget group, when filtering by status "Succeeded Fixed", uncollapsing a bug list group caused filtered-out bug occurrences (e.g. `var_5c076a9ff3413dc7` with status `failed-known`) to remain visible on the screen alongside matching occurrences (`var_d389fad26d2e32ca`).
- **Root Cause:**
  1. In `report-manager.js`, `sub.style.setProperty('display', 'none', 'important')` was immediately followed by `sub.style.display = 'none'`. In standard DOM CSS implementations, assigning directly to `element.style.property` mutates the CSS declaration and resets its priority to `""` (stripping the inline `!important` modifier).
  2. When the user expanded the list group, `updateWidgetParentSectionSpan` applied `.widget-container-full-span` to the section. In `dashboard-styles.css` (line 5774), `.widget-container-full-span .widget-list-subitem-row` declared `display: flex !important;` in a `@media (min-width: 992px)` query.
  3. Because the full-span rule possessed identical specificity `(0, 2, 0)` with `!important` and appeared later in the stylesheet than line 2800 (`.widget-list-subitem-row.is-filtered-hidden { display: none !important; }`), it won the CSS cascade. Furthermore, because the inline style's `!important` had been stripped by the direct assignment, the stylesheet's `display: flex !important;` overrode the inline style as well, resurrecting the filtered-out subitem rows.
- **Detection Gap ("What did we miss?"):** Previous static assertions verified that `.is-filtered-hidden` contained `display: none !important`, but did not check for competing `display: flex !important` rules in subsequent full-span media queries or verify that inline priority was not demoted by redundant direct property assignment.
- **Resolution:**
  1. In `dashboard-styles.css` and `report-manager.css`, scoped the full-span rule to `:not(.is-filtered-hidden)`: `.widget-container-full-span .widget-list-subitem-row:not(.is-filtered-hidden) { display: flex !important; }`.
  2. In `dashboard-styles.css` and `report-manager.css`, added explicit full-span hiding rules: `.widget-container-full-span .widget-list-subitem-row.is-filtered-hidden { display: none !important; }`.
  3. In `report-manager.js`, removed redundant direct assignments (`sub.style.display = 'none';`, `grp.style.display = 'none';`, `subitems.style.display = 'none';`) that stripped inline `!important` priority.
  4. Bumped asset cache busters in `index.html`: `dashboard-styles.css?v=1.2.7`, `report-manager.css?v=1.1.15`, and `report-manager.js?v=1.1.7`.
- **Safety Net Added:** Added automated regression assertions in `StaticResourceSyntaxTest.java` verifying that full-span rules exclude `.is-filtered-hidden`, that elevated hiding selectors are declared, that inline priority is preserved, and that cache buster query parameters are updated.
