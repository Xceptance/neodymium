# [DEF-20261102-1415] United Widget Container Interactive Cross-Filtering Disconnect

- **Status:** `Resolved`
- **Opened:** 2026-11-02 14:08
- **Closed:** 2026-11-02 14:18
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In united widget containers (e.g. combining a Status Donut Chart and a Browser Ratio Bar), clicking a filter item such as `Chrome_headless 79 (67%)` on the ratio bar added a filter chip in the container header and dimmed other browsers, but the paired Status Donut Chart on the left was not recalculated at all (remained at 118 items, 97% PASS, instead of filtering down to the 79 Chrome_headless tests). Reciprocally, clicking a status slice in the Donut Chart dimmed all ratio bar segments instead of recalculating the browser distribution of that status.
- **Root Cause:**
  1. `dashboard-widgets.html` hardcoded `data-filter-dim="status"` across all presentation templates (even ratio bars representing browsers or tags), preventing dimension-aware matching.
  2. `onContainerWidgetFilterClick` in `report-manager.js` only checked string equality on other interactive chips (`itemVal.includes(filterValLower)`), which never matched cross-dimensionally (e.g. "Chrome_headless" !== "Passed"), and only dimmed all items without recalculating pie slices, SVG arcs, center KPIs, or ratio bar segments.
  3. When custom dashboard widgets are active (`isCustomWidgetsConfigured == true`), `#batchLatestVariationsData` is omitted from the page, so no variation dataset was available in the DOM for custom widget cross-calculation.
- **Detection Gap ("What did we miss?"):** Previous unit tests verified widget aggregation DTO hierarchy without exercising interactive cross-calculation between united container components.
- **Resolution:**
  1. Extended `DashboardWidgetViewDto` with `containerVariationsJson` and a JSON serialization helper (`serializeVariationsToJson`) converting contributing test variations into a compact dataset.
  2. Enhanced `WidgetAggregationService` to pass scoped `containerVariations` when creating `DashboardWidgetViewDto.createContainerView`.
  3. Embedded the container dataset in `dashboard-widgets.html` via `<script type="application/json" class="widget-container-dataset">` and bound dynamic `data-filter-dim`, `data-metric`, and `data-tag-key` attributes on sections and chips.
  4. Implemented bidirectional recalculation engines in `report-manager.js` (`recalculateContainerPieChart`, `recalculateContainerRatioBar`, `recalculateContainerList`), dynamically recomputing SVG donut arcs, pass rate %, center labels, breakdown counts, and stacked bar widths in real-time.
  5. Updated `clearContainerCrossFilter` to restore the full unfiltered dataset across all container sections upon clicking `✕`.
- **Safety Net Added:** Added assertions in `WidgetAggregationServiceTest.java` verifying `containerVariationsJson` contains serialized variation telemetry, and in `StaticResourceSyntaxTest.java` verifying presence of `parseContainerDataset`, `matchesVariationFilter`, `recalculateContainerPieChart`, `recalculateContainerRatioBar`, and container template attributes.
