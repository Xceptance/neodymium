# [DEF-20261103-1540] Container Area Filter Ineffective on Status and Bugs Widgets Due to Residual Tag Key Pollution

- **Status:** `Resolved`
- **Opened:** 2026-11-03 15:40
- **Closed:** 2026-11-03 15:48
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** Clicking a category slice in the "Area" ratio card inside a dashboard container filtered list widgets and other ratio bars (e.g. Browser ratio) down to the matching test variations (e.g. 6 tests), but left `Status Pie Chart` (118 tests), `AI usage breakdown` (118 tests), `Bugs Text` (1 bug), `Bugs Pie Chart` (1 bug), and `Bugs ratio` (1 bug) completely unaffected and showing global unfiltered numbers.
- **Root Cause:**
  1. In `widget-configurator-modal.html`, `#widgetTagKeySelect` was visually hidden via `style="display: none"` when choosing non-TAG metrics (such as `STATUS` or `BUGS`), but its `<select>` input element was not marked `disabled`. Submitting the configurator form serialized the active value of `#widgetTagKeySelect` (defaulting to `"Area"`), persisting a non-empty `tagKey` (`"Area"`) on non-TAG widgets.
  2. The server rendered this polluted `tagKey` onto HTML child cards via `data-tag-key="Area"` regardless of the widget metric.
  3. In `report-manager.js`, `updateContainerCardsVisualState` and `recalculateContainerRatioBar` determined `cardDim` by checking `childCard.getAttribute('data-tag-key')` *before* checking `metric`. This assigned `cardDim = "area"` to Status, AI Usage, and Bug widgets.
  4. Cross-filtering facet exclusion sets `excludeDimForCard = cardDim = "area"` so that a widget does not filter against its own dimension. However, because Status and Bug widgets erroneously reported `cardDim = "area"`, the client-side cross-filter evaluated them with `excludeDim = "area"`, skipping the active `"area"` filter entirely during `matchesContainerFilters(v, activeFilters, excludeDim)`.
- **Detection Gap ("What did we miss?"):** Previous container cross-filter unit and browser tests tested container filtering where Status widgets either had no `tagKey` set or where filters were applied along status dimensions, rather than simulating form serialization pollution where a non-TAG widget holds a residual `tagKey` matching the applied filter dimension.
- **Resolution:**
  1. **JavaScript Cross-Filter Guard (`report-manager.js`):** In `updateContainerCardsVisualState` and `recalculateContainerRatioBar`, decoupled `cardDim` so that `data-tag-key` is only evaluated when `metric === 'TAG'`. Non-TAG metrics strictly resolve to their canonical dimensions (`status`, `mode`, `costs`, `severity`, `flaky`, `variations`).
  2. **Configurator Modal Input Disabling (`widget-configurator-modal.html`):** Added `th:disabled` and dynamic `disabled` toggling to `#widgetTagKeySelect` in `onWidgetMetricChanged()` so disabled fields are not serialized or posted when non-TAG metrics are selected.
  3. **Backend DTO & Controller Sanitization (`DashboardWidgetConfigDto.java`, `DashboardWidgetViewDto.java`, `AuraReportViewController.java`):** Enforced that `tagKey` is blanked unless `metric == WidgetMetric.TAG` in both the constructor and `getTagKey()` getter, ensuring full backwards compatibility with already saved dashboard widgets in existing storage.
- **Safety Net Added:**
  - `StaticResourceSyntaxTest.testContainerCrossFilterDecouplesTagKeyFromNonTagMetrics`: Enforces decoupling in `report-manager.js`, modal disabling logic in `widget-configurator-modal.html`, and executes a Node.js VM runtime simulation verifying that Status cards with legacy polluted `data-tag-key="Area"` are correctly filtered down when an area filter is active.
  - `DashboardWidgetConfigDtoTest.testNonTagMetricsClearTagKey`: Verifies that `DashboardWidgetConfigDto` sanitizes and clears `tagKey` for non-TAG metrics (`STATUS`, `BUGS`).
  - `WidgetAggregationServiceTest.testContainerWidgetAggregationClearsTagKeyForNonTagChildren`: Verifies that container widget aggregation produces child view DTOs with empty `tagKey` for non-TAG child widgets.
