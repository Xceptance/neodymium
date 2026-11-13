# [DEF-20261102-1305] Trend Graph Dual-Mode Fill Toggle (Filled vs. Not Filled)

- **Status:** `Resolved`
- **Opened:** 2026-11-02 12:45
- **Closed:** 2026-11-02 13:05
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** Trend charts on the dashboard previously forced stacked cumulative area polygons for multi-series metrics (`STATUS`, `TAG`, `AI_USAGE`), which caused lower layers to vertically distort the baseline and contour shape of upper layers (e.g. variations in `Classic` test count causing constant `AI-Driven` tests to appear wavy). When unstacked lines were introduced to decouple the curves, users lost the option to view cumulative stacked volume/breakdown areas when desired.
- **Root Cause:** Dashboard trend widget configurations lacked an explicit fill style toggle (`filled` boolean), and the rendering pipeline in `WidgetAggregationService` and `dashboard-widgets.html` coupled line drawing directly with stacked area accumulation.
- **Detection Gap ("What did we miss?"):** The widget configurator and card header actions only provided toggles for data point dots (`showDots`), missing configuration support for visual area fill representation vs. independent line comparison.
- **Resolution:**
  1. Extended `DashboardWidgetConfigDto` and `DashboardWidgetViewDto` with a `filled` property (defaulting to `false` for independent lines).
  2. Enhanced `WidgetAggregationService` to pre-compute BOTH `svgAreaPath` (cumulative stacked area) and `svgLinePath` (independent zero-baseline line) for all multi-series trend layers, as well as gradient area and stroke paths for single-series metrics (`COSTS`, `BUGS`), enabling instant zero-latency client-side toggling.
  3. Added "Chart Fill Style" radio selector in `widget-configurator-modal.html` and a quick-toggle checkbox `Fill` in the card header actions in `dashboard-widgets.html`.
  4. Added `/widgets/toggle-filled` endpoint in `AuraReportViewController` to persist user preference.
  5. Implemented `toggleTrendFilled()` in `report-manager.js` to instantly toggle `.widget-trend-layer-area` vs. `.widget-trend-layer-line` without a page reload, and updated `toggleWidgetTrendLayerSelect()` to respect the active fill mode when restoring category layers upon deselecting a legend item.
- **Safety Net Added:** Added `@Test testTrendGraphSupportsBothFilledAndNotFilledVariants()` to `WidgetAggregationServiceTest`, asserting that both stacked area paths and standalone zero-baseline line paths are simultaneously evaluated and available on multi-series trend layers.
