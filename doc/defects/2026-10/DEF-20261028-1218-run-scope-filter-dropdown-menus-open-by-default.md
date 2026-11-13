# [DEF-20261028-1218] Run Scope Filter Dropdown Menus Render Open Simultaneously by Default

- **Status:** `Resolved`
- **Opened:** 2026-10-28 12:18
- **Closed:** 2026-10-28 12:20
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** On the `/batch-history` run history page, all run scope filter dropdown menus (Browsers, Area, Branch, Build, Custom, Environment, etc.) were rendered open and visible simultaneously on page load, obscuring the overview dashboard.
- **Root Cause:** In `report-manager.css`, `.run-scope-menu` unconditionally specified `display: flex;`. Because `.run-scope-menu` appeared after `.multiselect-menu` in the stylesheet, this rule overrode `.multiselect-menu`'s default `display: none;`, keeping all dropdown menus permanently visible even without `.active` or `.show` classes.
- **Detection Gap ("What did we miss?"):** Previous static asset tests verified the presence of `.btn-apply-scope-filter` and `.run-scope-menu-footer` rules, but did not assert that `.run-scope-menu` inherits the hidden `display: none;` default from `.multiselect-menu`.
- **Resolution:**
  - Removed unconditional `display: flex;` from `.run-scope-menu` in `report-manager.css` so it defaults to `display: none;` and only displays as `flex` when toggled open with `.active` or `.show`.
- **Safety Net Added:**
  - `StaticResourceSyntaxTest.testBatchRunScopeFilterDeselectAllAndExclusionSyntax`: Asserts that `.run-scope-menu` in `report-manager.css` does not declare `display: flex;` unconditionally.
