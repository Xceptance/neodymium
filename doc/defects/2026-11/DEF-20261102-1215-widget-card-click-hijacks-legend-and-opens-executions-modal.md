# [DEF-20261102-1215] Widget Card Click Hijacks Legend Interaction and Unintentionally Opens Executions Modal

- **Status:** `Resolved`
- **Opened:** 2026-11-02 12:15
- **Closed:** 2026-11-02 12:17
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** Clicking a trend widget legend chip (or anywhere on the widget card body) unintentionally opens the "Contributing Test Executions" modal dialog instead of only toggling the non-filled line trend.
- **Root Cause:** All 7 widget presentation cards in `aura-manager/src/main/resources/templates/fragments/dashboard-widgets.html` bound `openWidgetExecutionsModal` at the root card container `div` (`class="... clickable-widget-card"`, `th:attr="onclick='openWidgetExecutionsModal(\'' + ${w.id} + '\')'"`, `title="Click to view contributing test executions"`). Clicks on inner interactive components like legend chips bubbled up to the card root handler, hijacking the click and opening the executions dialog.
- **Detection Gap ("What did we miss?"):** Prior tests only verified that modal HTML and controller endpoints returned HTTP 200, without testing DOM event bubbling or click isolation between child components and root card containers.
- **Resolution:** Removed card-level `onclick='openWidgetExecutionsModal(...)'`, `clickable-widget-card` class, and card-level `title="Click to view contributing test executions"` from all 7 widget card root containers in `dashboard-widgets.html`. Preserved the dedicated header action button `<button type="button" class="btn-icon-widget" th:attr="onclick='openWidgetExecutionsModal(\'' + ${w.id} + '\')'" title="View contributing test executions">` as the exclusive modal trigger across all widgets. Also added `event.stopPropagation()` to `.ratio-legend-chip` onclick handler as defense-in-depth.
- **Safety Net Added:** Added assertions in `AuraReportViewControllerSpringContextTest.testRenderDashboardWidgetsWithMultiLayerTrendGraph` and `StaticResourceSyntaxTest.testTrendChartLegendSelectionSyntax` verifying that widget cards do NOT contain `clickable-widget-card` or card-level `onclick="openWidgetExecutionsModal"`, and that only the header icon button triggers `openWidgetExecutionsModal`.
