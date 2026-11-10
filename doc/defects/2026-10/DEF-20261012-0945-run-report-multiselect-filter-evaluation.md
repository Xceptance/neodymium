# [DEF-20261012-0945] Run Report Multiselect Filter Evaluation & Browser Cache Invalidation

- **Status:** `Resolved`
- **Opened:** 2026-10-12 09:45
- **Closed:** 2026-10-12 09:48
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In the Run Report "All Tests" tab, unchecking items in the Modes, Browsers, or Locales multiselect dropdowns updates the button label (e.g., "1 Selected"), but the test execution rows in the table remain visible instead of being filtered out.
- **Root Cause:** 
  1. `index.html` referenced `/js/report-manager.js` without a cache-busting query parameter (`?v=1.1.0`), causing browsers to execute stale disk-cached JavaScript that still targeted the legacy `#browserFilter` and `#localeFilter` `<select>` elements (which were replaced by the multiselects and thus resolved to `'ALL'`).
  2. `currentGlobalStatusBadgeFilter` and related global state variables were declared with `let` at line 4501 while being referenced at line 1043 within `applyRunReportFilters()`, creating potential Temporal Dead Zone initialization exceptions.
  3. `applyRunReportFilters()` used non-strict substring matching (`selectedModes.some(...)`) and relied on CSS selector string matching `:not([style*="display: none"])` instead of direct DOM computed style checks.
- **Detection Gap ("What did we miss?"):** Unit tests verified DTO and Controller model attributes for multiselect filter lists, but client-side DOM filtering behavior and asset versioning in Thymeleaf templates were not verified in headless browser integration tests.
- **Resolution:** 
  1. Added version query parameter `?v=1.1.0` to `report-manager.js` script tag in `index.html`.
  2. Hoisted global filter state declarations (`currentGlobalStatusBadgeFilter`, `currentExecutionStatusFilter`, `currentExecutionModeFilter`, `currentHealedFilter`) to the top of `report-manager.js` (near line 25) and removed redundant duplicate declarations.
  3. Refactored `applyRunReportFilters()` to use exact matching for `selectedModes`, `selectedBrowsers`, and `selectedLocs`, and direct DOM property inspection (`Array.from(classBox.querySelectorAll('.test-row')).filter(r => r.style.display !== 'none')`) for container visibility.
  4. Added `applyRunReportFilters()` invocation to `bindGlobalListeners()` on run report view initialization.
- **Safety Net Added:** Verified via `RunReportDtoTest`, `AuraReportViewControllerTest`, and `node -c aura-manager/src/main/resources/static/js/report-manager.js`.
