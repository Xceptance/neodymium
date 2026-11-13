# [DEF-20261103-0948] United Widget Container Single-Value Filter Restriction

- **Status:** `Resolved`
- **Opened:** 2026-11-03 09:48
- **Closed:** 2026-11-03 09:54
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In united widget containers (`.widget-container-card`), clicking a filter value (e.g. Status `Passed`) cleared all other active filters across the entire container. Users could not select multiple values in the same dimension (e.g. `Passed` and `Failed Known`, or `Chrome` and `Firefox`) or filter simultaneously across multiple distinct dimensions (e.g. Status `Passed` and Browser `Chrome`).
- **Root Cause:** `onContainerWidgetFilterClick` in `report-manager.js` stripped `.is-filter-active` from all container elements on every click, stored only a single scalar `(filterDim, filterVal)` in the DOM, and `.container-active-filter-strip` only rendered a single text label without multi-chip support. Additionally, `matchedVariations` evaluated only a single dimension and value rather than combining criteria.
- **Detection Gap ("What did we miss?"):** Prior unit tests verified single-filter dispatching for individual presentation types (`PIE_CHART`, `RATIO_BAR`, `TEXT`, `LIST`), but lacked assertions for multi-value state persistence, disjunctive same-dimension matching, conjunctive cross-dimension matching, and facet-excluded multi-dimension recalculation.
- **Resolution:**
  1. Implemented container-scoped filter state management via `getContainerActiveFilters(container)` and `getContainerActiveFiltersCount(activeFilters)`.
  2. Implemented `matchesContainerFilters(v, activeFilters, excludeDim)` evaluating disjunctive matching within each dimension and conjunctive matching across dimensions, while applying facet exclusion during widget recalculation.
  3. Overhauled `onContainerWidgetFilterClick(el, event)` to toggle values in/out of the multi-filter set and update active/dimmed opacity styling across all child widgets.
  4. Added `removeContainerSingleFilter(btn, event, optDim, optVal)` to allow removing individual filter values via `✕` chip buttons.
  5. Enhanced `renderContainerFilterStrip(container, activeFilters)` to dynamically render removable chips for every active filter value alongside a `Clear all` button.
  6. Updated `dashboard-widgets.html` and `dashboard-styles.css` with `.container-filter-chips` and `.btn-clear-all-container-filters` styling.
- **Safety Net Added:** Added `@Test testUnitedContainerMultiValueCrossFilteringSupport()` in `StaticResourceSyntaxTest.java`, asserting presence of `matchesContainerFilters`, `getContainerActiveFilters`, `removeContainerSingleFilter`, `renderContainerFilterStrip`, multi-value disjunctive matching logic, and container template wrappers.
