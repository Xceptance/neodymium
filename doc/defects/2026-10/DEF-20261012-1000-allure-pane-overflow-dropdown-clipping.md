# [DEF-20261012-1000] Allure Pane Overflow Causes Dropdown Menu Clipping on Filter Deselection

- **Status:** `Resolved`
- **Opened:** 2026-10-12 10:00
- **Closed:** 2026-10-12 10:05
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In the Run Report view ("All Tests" tab), when multiple or all options in the Execution Filters dropdowns (Modes, Browsers, Locales) are deselected such that no test execution rows match, the dropdown menus are clipped at the bottom of the table container, preventing users from seeing or re-selecting options.
- **Root Cause:** In `report-manager.css`, `.allure-main-pane` was configured with `overflow-x: auto`. Per the CSS Overflow specification, having `overflow-x: auto` forces the computed `overflow-y` to `auto` as well. When all filter options are deselected, every `.area-group` and `.test-class-container` is hidden via `display: none`, causing the container height to collapse to just the header height (~120px). The absolute-positioned `.multiselect-menu` (height ~280px) extended below this collapsed boundary and was clipped by the container's forced vertical scroll context.
- **Detection Gap ("What did we miss?"):** Existing controller and DTO unit tests validated filter serialization and data model lists, but visual rendering and CSS overflow behavior when zero rows match in the interactive DOM were not covered by automated layout assertions.
- **Resolution:**
  1. Updated `.allure-main-pane` in `report-manager.css` from `overflow-x: auto` to `overflow: visible;` (horizontal table scrolling is already managed by the nested `.table-responsive` element).
  2. Added `#runReportSubTabAllTests { min-height: 480px; }` to guarantee sufficient vertical clearance for dropdown menus even when table contents collapse.
  3. Added an empty filter state banner (`#runReportFilterEmptyState`) with an intuitive "Reset All Filters" CTA button when no tests match the active filters.
  4. Updated `applyRunReportFilters()` in `report-manager.js` to dynamically toggle the empty state banner based on visible row count.
  5. Bumped cache-busting asset query string version to `v=1.1.1` in `index.html`.
- **Safety Net Added:** Verified filter toggle logic and empty state presence in `run-report.html` and `report-manager.js`. All unit and integration test suites in `aura-manager` continue to pass cleanly (`RunReportDtoTest`, `AuraReportViewControllerTest`).
