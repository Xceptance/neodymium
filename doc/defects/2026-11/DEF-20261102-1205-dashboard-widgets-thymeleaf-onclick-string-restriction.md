# [DEF-20261102-1205] Dashboard Trend Widget Legend Chip Fails with Thymeleaf String Event Restriction

- **Status:** `Resolved`
- **Opened:** 2026-11-02 12:05
- **Closed:** 2026-11-02 12:06
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** Opening the dashboard view (`/` or `/dashboard`) with trend widgets throws an HTTP 500 error: `org.thymeleaf.exceptions.TemplateProcessingException: Only variable expressions returning numbers or booleans are allowed in this context, any other datatypes are not trusted in the context of this expression, including Strings or any other object that could be rendered as a text literal... (template: "fragments/dashboard-widgets" - line 578, col 30)`.
- **Root Cause:** Line 578 in `aura-manager/src/main/resources/templates/fragments/dashboard-widgets.html` used `th:onclick` with a dynamic String concatenation expression (`toggleWidgetTrendLayerSelect('...' + ${w.id} ...)`). Thymeleaf 3.1+ enforces strict execution restrictions on DOM event attributes (`th:on*`), prohibiting non-numeric/non-boolean dynamic expressions to prevent script injection.
- **Detection Gap ("What did we miss?"):** `StaticResourceSyntaxTest.java` verified JavaScript files with Node.js and checked HTML files using raw text assertions, while prior Spring context integration tests in `AuraReportViewControllerSpringContextTest.java` only populated `PIE_CHART` and `TEXT` widgets, skipping the multi-layer trend chart loop where line 578 executes.
- **Resolution:** Replaced `th:onclick` on `.ratio-legend-chip` with standard HTML `onclick="toggleWidgetTrendLayerSelect(this)"`. Updated `toggleWidgetTrendLayerSelect` in `report-manager.js` to inspect `target.getAttribute('data-widget-id')` and `target.getAttribute('data-layer-name')` directly from the DOM element, while preserving backward compatibility with `(widgetId, layerName)` invocations.
- **Safety Net Added:** Added `@Test` `AuraReportViewControllerSpringContextTest.testRenderDashboardWidgetsWithMultiLayerTrendGraph` that renders `fragments/dashboard-widgets` with a multi-layer `TREND_GRAPH` widget through `SpringTemplateEngine`. Also updated `StaticResourceSyntaxTest.testTrendChartLegendSelectionSyntax` to enforce standard HTML `onclick` on trend legend chips and prohibit `th:onclick`.
