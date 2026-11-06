# [DEF-20261102-1505] Costs Pie Chart Cross-Filtering Matches Zero Variations Due to Unmapped Dimension

- **Status:** `Resolved`
- **Opened:** 2026-11-02 15:05
- **Closed:** 2026-11-02 15:06
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In a united widget container containing a Costs pie chart, clicking any cost/token slice (e.g. "Prompt Tokens (70%)" or "Completion Tokens (30%)") creates an active filter strip, but resets all sibling widgets (status pie, ratio bars, and lists) to `0 Tests / 0 Items`.
- **Root Cause:** In `dashboard-widgets.html`, the `data-filter-dim` attribute on pie slices and breakdown items only checked for `STATUS`, `TAG`, and `AI_USAGE`, falling back to `'generic'` for `COSTS`. In `report-manager.js`, `matchesVariationFilter(v, filterDim, filterVal)` did not recognize `costs` or token labels, treating the filter value `"Prompt Tokens (70%)"` as a literal custom tag. Because no test variations have a custom tag equal to `"Prompt Tokens (70%)"`, 0 variations matched, causing all sibling widgets to recalculate against an empty dataset.
- **Detection Gap ("What did we miss?"):** Previous cross-filtering unit tests covered status categories, discrete browser codes, and environment tags, but did not assert client-side dimension attribute generation and variation filtering for `COSTS` metric slices.
- **Resolution:**
  1. Updated `dashboard-widgets.html` to map `w.metric.name() == 'COSTS'` to `data-filter-dim="costs"` across donut slices, pie breakdown list items, ratio bar segments, and ratio legend chips.
  2. Updated `matchesVariationFilter()` in `report-manager.js` to recognize `dim === 'costs'`, `dim === 'tokens'`, or values containing `'token'` / `'cost'`, matching test variations with `(Number(v.tok) > 0 || Number(v.c) > 0 || v.m === 'AI')`.
  3. Updated `resolveVariationDimensionValue()` and `recalculateContainerRatioBar()` to support `costs` dimension resolution.
- **Safety Net Added:** Added `@Test` `StaticResourceSyntaxTest.testUnitedContainerCostsPieCrossFilteringSupport()` asserting template attribute binding and JavaScript matcher logic for costs cross-filtering.
