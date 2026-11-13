# [DEF-20261029-1215] Dashboard Widget Grid Class Mismatch and Tag Dimension Selector Discovery Gap

- **Status:** `Resolved`
- **Opened:** 2026-10-29 12:15
- **Closed:** 2026-10-29 12:20
- **Component:** `aura-manager`
- **Scope:** `Framework & SUT`
- **Symptom:** On the Run History page, added dashboard widgets render squeezed into narrow 1/12th grid columns (8.3% width) side-by-side. In addition, when opening the widget configurator to create a tag-based widget, selecting the "Tag" option fails to display or allow selecting available tag keys.
- **Root Cause:**
  1. Class name divergence: `dashboard-widgets.html` applied `widget-half-width` / `widget-full-width`, but `dashboard-styles.css` defined `.widget-card-half` / `.widget-card-full`. Unmatched grid items defaulted to `grid-column: span 1` in a 12-column CSS Grid.
  2. Tag dropdown discovery failure: `#widgetTagSelectorRow` had inline `style="display: none;"` and depended on an external script (`onWidgetMetricChanged`) without CSS `:has()` fallback. Browser caching of `report-manager.js?v=1.1.3` prevented execution. Additionally, `getWidgetConfigModal` supplied composite value strings (`"Key: Value"`) rather than distinct tag keys.
- **Detection Gap ("What did we miss?"):** HTML string rendering tests in `StaticResourceSyntaxTest` verified fragment compilation but did not assert CSS rule application against the rendered class names, nor did tests verify CSS `:has()` declarative fallbacks when JavaScript handlers fail to run.
- **Resolution:**
  1. Add dual class bindings (`.widget-card-half, .widget-half-width { grid-column: span 6; min-width: 0; }` and `.widget-card-full, .widget-full-width { grid-column: span 12; min-width: 0; }`).
  2. Implement declarative CSS `:has()` display rules (`#widgetConfigForm:has(input[name="metric"][value="TAG"]:checked) #widgetTagSelectorRow { display: block !important; }`) and embed inline script directly in the modal fragment to guarantee instant display regardless of client-side cache state.
  3. Enhance `widgetTagSelectorRow` with quick-select pill buttons for standard tags (`Browser`, `Locale`, `Area`, `Environment`, `Branch`, `Release`) and clean deduplicated tag dropdown populated from `getAllKnownTagMap().keySet()`.
  4. Provide an optional Tag Filter / Scope selector so users can also scope Status, Costs, or Bug widgets to a specific tag if desired.
  5. Bump cache-busting query strings in `index.html` to `dashboard-styles.css?v=1.2.6` and `report-manager.js?v=1.1.4`.
- **Safety Net Added:** Add regression test in `AuraReportViewControllerTest` and `StaticResourceSyntaxTest` asserting `.widget-half-width`, `.widget-full-width`, and clean tag key population.
