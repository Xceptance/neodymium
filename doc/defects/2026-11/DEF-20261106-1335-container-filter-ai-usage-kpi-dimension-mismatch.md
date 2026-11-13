# [DEF-20261106-1335] United Container Cross-Filtering by AI Usage KPI Text Clobbers Variations to Zero

- **Status:** `Resolved`
- **Opened:** 2026-11-06 13:35
- **Closed:** 2026-11-06 13:40
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In united widget containers, clicking on AI Usage KPI breakdown texts (e.g. `AI-Driven`, `Healed`, or `Playbook`) caused all linked widgets in the container (Status Pie Chart, AI Usage KPI, AI Usage Ratio Bar) to drop to 0 tests (0% pass, 0% AI, 0 items). In contrast, clicking the AI Usage Ratio Bar worked. Additionally, ratio bar recalculation displayed `AI-Driven 0 (0%)` instead of the actual AI test volume due to a secondary dimension value mismatch between `'AI'` and `'AI-Driven'`.
- **Root Cause:**
  1. `dashboard-widgets.html` bound `data-filter-dim='type'` on `textBody` KPI cards instead of the framework standard `data-filter-dim='mode'`.
  2. `report-manager.js` `matchesVariationFilter` only handled `dim === 'mode'`, failing to match dimension `'type'` and returning `false` for all test variations.
  3. Facet exclusion in `matchesContainerFilters` compared `excludeDim` (`'mode'`) with `dim` (`'type'`). Because they differed, facet exclusion failed and the AI Usage KPI card filtered itself down to zero.
  4. In `textSmallTrendBody`, interactive filter attributes (`widget-interactive-filter`, `data-filter-dim`, and `onclick`) were missing from the AI_USAGE breakdown segments.
  5. `resolveVariationDimensionValue` returned raw `v.m` (`'AI'`) instead of `'AI-Driven'`, causing ratio bar recount lookups for `'AI-Driven'` in `resolveDimensionCount` to resolve to 0.
- **Detection Gap ("What did we miss?"):** `StaticResourceSyntaxTest.java` verified function definitions and `STATUS`/`COSTS`/`BUGS` metric wiring, but lacked assertions verifying that `AI_USAGE` interactive filters in `textBody` and `textSmallTrendBody` bind to `data-filter-dim='mode'` and that `matchesVariationFilter` and `resolveVariationDimensionValue` properly map `'AI-Driven'`.
- **Resolution:**
  1. Updated `dashboard-widgets.html`:
     - Changed `data-filter-dim='type'` to `data-filter-dim='mode'` on `textBody` AI_USAGE segment chips.
     - Added `widget-interactive-filter`, `data-filter-dim='mode'`, and `onclick="onContainerWidgetFilterClick(this, event)"` to `textSmallTrendBody` AI_USAGE breakdown segments.
  2. Updated `report-manager.js`:
     - Updated `recalculateContainerText` to query both `data-filter-dim="mode"` and legacy `"type"`.
     - Updated `matchesVariationFilter` to support `dim === 'mode' || dim === 'type'` and accurately match `AI-Driven`, `Healed`, `Playbook`, and `Classic` modes.
     - Normalized `'type'` to `'mode'` across `onContainerWidgetFilterClick`, `matchesContainerFilters`, `updateContainerCardsVisualState`, and `removeContainerSingleFilter`.
     - Updated `resolveVariationDimensionValue` for `mode` / `type` to classify variations into `'AI-Driven'`, `'Healed'`, `'Playbook'`, or `'Classic'`.
     - Updated `resolveDimensionCount` to equate `'ai-driven'` and `'ai'`.
- **Safety Net Added:** Added automated regression test `StaticResourceSyntaxTest.testUnitedContainerAiUsageKpiCrossFilteringSupport()` asserting HTML template filter dimensions, JavaScript mode matching, facet exclusion, and ratio bar dimension resolution via Node.js VM execution.
