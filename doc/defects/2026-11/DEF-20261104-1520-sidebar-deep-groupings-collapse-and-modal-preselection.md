# [DEF-20261104-1520] Sidebar Deep Groupings Omission, Collapsible State, Side Panel Scroll, and Modal Preselection

- **Status:** `Resolved`
- **Opened:** 2026-11-04 15:20
- **Closed:** 2026-11-04 15:22
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:**
  1. Sub-grouping links nested under 3rd-level dashboards were not rendered in the side panel navigation tree.
  2. The sidebar navigation lacked vertical scrolling and was clipped by the viewport when containing many custom dashboards or deep hierarchies.
  3. Groupings in the side panel were statically expanded with no way to collapse them.
  4. Both the "Create New Dashboard Page" modal and "Convert to Group" modal preselected the first grouping dimension pill (`batch` and `browser`) by default instead of requiring explicit user selection.
- **Root Cause:**
  1. `fragments/sidebar.html` hardcoded nested loop iterations down to level 3 only (`p` -> `sub` -> `child`), unconditionally rendering level 3 items as leaf nodes without evaluating `child.isGroupParent` or traversing `child.subPages`.
  2. `aside` in `report-manager.css` lacked `overflow-y: auto`, while placing overflow directly on `aside` would clip the absolutely positioned `.sidebar-toggle-btn` at `right: -12px`.
  3. `.sidebar-page-group-container` lacked collapsible toggle controls, default `.collapsed` CSS classes, and rotated chevron icons.
  4. `modals.html` and `report-manager.js` hardcoded `active` classes and fallback hidden input values on modal open instead of starting empty and validating selection on submit.
- **Detection Gap ("What did we miss?"):**
  Existing syntax tests in `StaticResourceSyntaxTest.java` only tested the presence of `th:if="${sub.isGroupParent}"` (2nd-level sub-groups) without asserting recursive fragment rendering, inner scrolling containers, or default unselected modal inputs.
- **Resolution:**
  1. Refactored `fragments/sidebar.html` to use a recursive Thymeleaf fragment `renderCustomPageNav(page, level)` that renders arbitrary depths with proper indentation, count badges, and delete actions.
  2. Preserved `overflow: visible` on `aside#mainSidebar` while wrapping inner navigation items in `.sidebar-scrollable-content` with `flex: 1; height: 100%; min-height: 0; overflow-y: auto; overflow-x: hidden; scrollbar-width: thin;`.
  3. Added `.sidebar-page-group-container.collapsed` by default with `.btn-sidebar-group-toggle` chevron controls and CSS rules to hide `.sidebar-sub-pages-list` when collapsed.
  4. Implemented `toggleSidebarGroup(btn, event)` and `initSidebarGroupExpansion()` in `report-manager.js` to keep groupings collapsed by default while auto-expanding only the active page's ancestor path.
  5. Cleared preselected grouping dimension pills and default hidden input values in `modals.html` and `report-manager.js`, enforcing validation on submit.
- **Safety Net Added:**
  Updated `StaticResourceSyntaxTest.testConvertToGroupDashboardUiArtifactsAndTemplates` to assert:
  - `sidebar.html` defines `th:if="${page.isGroupParent}"` (recursive), `.sidebar-scrollable-content`, `.btn-sidebar-group-toggle`, and `.sidebar-page-group-container.collapsed`.
  - `modals.html` initializes `createPageGroupByInput` and `convertToGroupGroupByInput` with empty values (`value=""`).
  - `report-manager.css` defines `.sidebar-scrollable-content`, `.btn-sidebar-group-toggle`, and collapsed sub-page hiding rules.
  - `report-manager.js` defines `toggleSidebarGroup` and `initSidebarGroupExpansion`.
