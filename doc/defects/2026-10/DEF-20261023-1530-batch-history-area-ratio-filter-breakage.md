# [DEF-20261023-1530] Batch History Area Ratio Bar Filtering Breakage

- **Status:** `Resolved`
- **Opened:** 2026-10-23 15:30
- **Closed:** 2026-10-23 15:40
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** Clicking on segments or legend chips in the Area Ratio bar on the Batch History page failed to filter runs in the Run History table, failed to filter cards in the History Trends for Included Areas grid, and caused erratic variation filtering and ratio synchronization.
- **Root Cause:**
  1. In `report-manager.js` `applyBatchDirectoryFilters()`, empty string tokens from `(row.getAttribute('data-areas') || '').split(',')` produced `['']`. Because `String.prototype.includes('') === true`, any test run lacking area tags (`data-areas=""`) falsely matched any active area filter (`selectedAreas.some(a => rowAreas.some(ra => a.includes(ra)))`).
  2. The "History Trends for Included Areas" grid cards (`.area-trend-card` in `batch-history.html`) did not bind `th:data-area` and were completely omitted from `applyBatchDirectoryFilters()`.
  3. Formatting and prefix discrepancies between area representations (`@Checkout` in ratio bars vs `Checkout` in variation attributes or data-areas, and `@Browsing (default)` vs `General`/`Browsing`/`Default`) caused equality and substring checks in `filterFromRatioBar()` and `syncBatchRatioBarsVisualState()` to fail or desync active visual states.
- **Detection Gap ("What did we miss?"):** Existing static resource tests in `StaticResourceSyntaxTest.java` only validated `filterAllTestsByStatus` on run report sub-tabs and did not exercise the multi-dimensional ratio bar filtering engine (`filterFromRatioBar`) or area trend cards on batch history.
- **Resolution:**
  1. Bound `th:data-area="${area.areaName}"` to `.area-trend-card` in `batch-history.html`.
  2. Implemented `normalizeAreaFilterVal(str)` in `report-manager.js` to strip leading `@` and canonicalize browsing/default aliases (`GENERAL`, `BROWSING`, `DEFAULT` -> `BROWSING (DEFAULT)`).
  3. Added `.filter(Boolean)` and guarded `rowAreas.length > 0` in `applyBatchDirectoryFilters()`.
  4. Added `.area-trend-card` filtering to `applyBatchDirectoryFilters()` using `normalizeAreaFilterVal`.
  5. Updated `recalculateBatchLatestStatusPie()`, `syncBatchRatioBarsVisualState()` cross-filter loops (Browser, Locale, Environment, Area, and Tag), and area chip/bar visual state styling to use `normalizeAreaFilterVal`.
  6. Updated `filterFromRatioBar()` to use `normalizeAreaFilterVal` for single-click toggle/isolate, Ctrl+click exclusion, and legacy multiselect menu synchronization.
- **Safety Net Added:** Added `StaticResourceSyntaxTest.testBatchHistoryAreaRatioFiltering` in `aura-manager/src/test/java/com/xceptance/aura/report/staticassets/StaticResourceSyntaxTest.java`, exercising single-click filtering, empty-area row exclusion, area trend card filtering, toggle reset, prefix-less value normalization, clear button reset, and Ctrl+click exclusion/inclusion cycles.
