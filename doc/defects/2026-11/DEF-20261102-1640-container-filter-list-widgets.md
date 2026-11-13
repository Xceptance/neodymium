# [DEF-20261102-1640] Container Cross-Filtering Takes No Effect on List Widgets

- **Status:** `Resolved`
- **Opened:** 2026-11-02 16:35
- **Closed:** 2026-11-02 16:48
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In united dashboard containers, applying cross-filters (such as clicking a Status Pie Chart segment like Passed, or a browser ratio chip) left LIST presentation widgets completely unfiltered; bug rows, test rows, and list group counts remained completely static.
- **Root Cause:** `recalculateContainerList` in `report-manager.js` only checked `data-subitem-label` against variation logical keys `v.k`. For BUGS list presentations, `data-subitem-label` contained ticket numbers (e.g. `BUG-4886`) while `v.k` contained test class and method names, causing a complete key mismatch. Furthermore, canonical variation IDs (`vid`), linked bug tickets, and severity were omitted from `containerVariationsJson`. Finally, list group header counts (`.widget-list-row .font-mono`) and section header summaries (`.widget-section-header .font-mono`) were never recalculated or restored, and `.widget-list-row` hardcoded `data-filter-dim='category'` instead of dynamically matching the underlying metric dimension.
- **Detection Gap ("What did we miss?"):** Prior static resource syntax tests verified cross-filter dispatch wiring for PIE_CHART, RATIO_BAR, and TEXT presentations, but lacked end-to-end assertions for list sub-item DOM attribute bindings (`data-variation-key`, `data-variation-id`), canonical variation ID matching (`matchedVids`), linked bug ticket evaluation (`matchedBugTickets`), and group header count caching (`data-original-val`).
- **Resolution:**
  1. Enhanced `BatchLatestVariationDto` with canonical `variationId` (`vid`) and linked `bugs` list, populating them during report creation in `AuraReportDataService.java`.
  2. Updated `serializeVariationsToJson` in `DashboardWidgetViewDto.java` to serialize `\"vid\"` and `\"bugs\"` in the embedded container dataset.
  3. Added `variationKey` and `variationId` to `WidgetListItemDto` and bound them as `data-variation-key` and `data-variation-id` on `.widget-list-subitem-row` in `dashboard-widgets.html`.
  4. Updated `.widget-list-row` in `dashboard-widgets.html` to dynamically assign `data-filter-dim` matching the widget metric (`status`, `severity`, `mode`, or `tagKey`).
  5. Enhanced `matchesVariationFilter` in `report-manager.js` to recognize `severity`, `bugs`, and `category` dimensions.
  6. Overhauled `recalculateContainerList` in `report-manager.js` to match sub-items against `matchedVids`, `matchedKeys`, and `matchedBugTickets`, update `.widget-list-row .font-mono` group counts, hide empty list groups, and update the section header summary count.
  7. Updated `clearContainerCrossFilter` to invoke `recalculateContainerList(childCard, allVariations, false)` to restore all original counts, groups, and subitems.
- **Safety Net Added:**
  - `com.xceptance.aura.report.staticassets.StaticResourceSyntaxTest#testUnitedContainerListWidgetCrossFilterDispatchingAndRecalculation`
  - `com.xceptance.aura.report.service.WidgetAggregationServiceTest#testStatusListPresentationAggregation` (verifying `subPass.getVariationKey()` and `getVariationId()`)
  - `com.xceptance.aura.report.service.WidgetAggregationServiceTest#testBugsListPresentationAggregation` (verifying `subCritical.getVariationId()` and `subMajor.getVariationId()`)
