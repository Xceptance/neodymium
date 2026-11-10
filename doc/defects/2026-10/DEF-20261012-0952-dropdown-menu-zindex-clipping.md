# [DEF-20261012-0952] Run Report Multiselect Dropdown Menu Z-Index Stacking Clipping

- **Status:** `Resolved`
- **Opened:** 2026-10-12 09:52
- **Closed:** 2026-10-12 09:55
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** When opening execution filter dropdowns (e.g. Modes, Browsers, Locales) on the Run Report page, the bottom portion of the dropdown menu is partially cut off and rendered underneath the adjacent `.panel` header and execution table.
- **Root Cause:** 
  1. `.test-filters-toolbar` lacked `position: relative` and an elevated `z-index`, allowing the following `.panel` sibling in normal DOM painting order to render its background and border on top of the absolutely positioned `.multiselect-menu`.
  2. `.multiselect-menu` used a modest `z-index: 50` while `.multiselect-dropdown` lacked active z-index elevation when open.
  3. `index.html` lacked a cache-busting query parameter on `/css/report-manager.css`.
- **Detection Gap ("What did we miss?"):** Visual audits in automated unit/controller tests do not render CSS stacking contexts; visual regression testing on empty/short table states did not test open dropdown overflow over panel borders.
- **Resolution:**
  1. Added `position: relative; z-index: 1000;` to `.test-filters-toolbar` and `z-index: 1060;` with elevated box-shadow to `.multiselect-menu`.
  2. Added `.multiselect-dropdown.active, .multiselect-dropdown:focus-within { z-index: 1050; }` in `report-manager.css`.
  3. Added container sizing and overflow override for `#runReportSubTabAllTests > .panel { height: auto; flex: 0 0 auto; overflow: visible; }`.
  4. Updated `report-manager.js` click listener to toggle `.active` class on the parent `.multiselect-dropdown` when open/close.
  5. Added `?v=1.1.0` cache-buster to `report-manager.css` link in `index.html`.
- **Safety Net Added:** Verified via visual check, syntax validation, and `mvn test -pl aura-manager -Dtest=AuraReportViewControllerTest`.
