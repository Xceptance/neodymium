# [DEF-20261023-1545] Area Filter Collision Between @Browsing and @Browsing (default)

- **Status:** `Resolved`
- **Opened:** 2026-10-23 15:45
- **Closed:** 2026-10-23 15:49
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** Selecting area tag "@Browsing" caused "@Browsing (default)" to also be selected/highlighted, and no test runs, variations, or area trend cards were filtered out.
- **Root Cause:** 
  1. `normalizeAreaFilterVal` in `report-manager.js` erroneously mapped `'BROWSING'` to `'BROWSING (DEFAULT)'` alongside `'GENERAL'` and `'DEFAULT'`, treating explicit `@Browsing` tags identically to untagged `@Browsing (default)`.
  2. Filtering and visual styling routines across `report-manager.js` used substring inclusion (`|| includes()`), causing `'BROWSING (DEFAULT)'.includes('BROWSING')` to evaluate to `true` and cross-matching both areas.
- **Detection Gap ("What did we miss?"):** Previous ratio filtering tests validated `@Checkout` against `@Browsing (default)` and lacked test cases with overlapping names such as `@Browsing` vs `@Browsing (default)`.
- **Resolution:**
  1. Removed `'BROWSING'` from default browsing aliases in `normalizeAreaFilterVal`, preserving it as a distinct normalized area key (`'BROWSING'`).
  2. Enforced strict normalized equality (`===`) across:
     - `applyBatchDirectoryFilters()` (`run-history-row`, `.area-trend-card`, `.tb-entry-row`)
     - `recalculateBatchLatestStatusPie()`
     - `syncBatchRatioBarsVisualState()` (Browser, Locale, Environment, and Tag cross-filtering)
     - `syncBatchRatioBarsVisualState()` (Area chip and bar active/dimmed styling)
- **Safety Net Added:** Extended `StaticResourceSyntaxTest.testBatchHistoryAreaRatioFiltering` in `aura-manager/src/test/java/com/xceptance/aura/report/staticassets/StaticResourceSyntaxTest.java` to assert distinct isolation between `@Browsing` and `@Browsing (default)` across rows, cards, variations, and ratio segment states.
