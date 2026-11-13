# [DEF-20261105-1943] Forbid String of Pearls Presentation for Tag Metric Dimension

- **Status:** `Resolved`
- **Opened:** 2026-11-05 19:43
- **Closed:** 2026-11-05 19:46
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** Selecting "Tag" as information source with "String of Pearls" presentation was permitted in the widget configurator UI and controller, but rendered a misleading widget displaying test pass rates instead of tag data due to an unhandled metric branch.
- **Root Cause:** `WidgetAggregationService.buildPerlenketteWidget` only supported scalar metrics (`STATUS`, `COSTS`, `BUGS`, `AI_USAGE`, `FLAKINESS`) and silently defaulted `TAG` to `STATUS`. The modal UI and DTO constructors lacked validation rules forbidding `WidgetPresentation.PERLENKETTE` for `WidgetMetric.TAG`.
- **Detection Gap ("What did we miss?"):** Unit tests in `WidgetAggregationServiceTest` and `AuraReportViewControllerTest` only tested valid combinations (`STATUS`, `COSTS`, `BUGS` with `PERLENKETTE`, and `TAG` with `RATIO_BAR` / `PIE_CHART`), never asserting mutual exclusion between `TAG` and `PERLENKETTE`.
- **Resolution:** 
  1. Disabled the `PERLENKETTE` radio tile in `widget-configurator-modal.html` and `report-manager.js` when `metric === 'TAG'`, automatically switching selection to `PIE_CHART`.
  2. Added CSS `:has(input:disabled)` visual dimming and pointer-events deactivation in `dashboard-styles.css`.
  3. Added backend normalization in `DashboardWidgetConfigDto` and `AuraReportViewController.saveWidget` converting any `TAG` + `PERLENKETTE` combination to `PIE_CHART`.
  4. Added defensive redirection in `WidgetAggregationService` routing `TAG` + `PERLENKETTE` to `buildPieChartWidget`.
- **Safety Net Added:** 
  - `DashboardWidgetConfigDtoTest.testForbidPerlenketteForTagMetricNormalization`
  - `AuraReportViewControllerTest.testSaveWidget_ForbidsPerlenketteForTagMetric_FallsBackToPieChart`
  - `WidgetAggregationServiceTest.testAggregateWidgetTagMetricPerlenketteFallbackToPieChart`
  - `StaticResourceSyntaxTest` syntax assertions on `widget-configurator-modal.html`, `report-manager.js`, and `dashboard-styles.css`
