# [DEF-20261029-1658] Trend Widgets Legend Point Values Omission

- **Status:** `Resolved`
- **Opened:** 2026-10-29 16:58
- **Closed:** 2026-10-29 17:00
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In trend graph widgets (Status Trend, Bugs Trend, Costs Trend), legend chips below the SVG chart displayed point-in-time numerical quantities (e.g. `Passed Clean 83 passed`, `Failed Unknown AI 3 AI`, `Active Known Bugs 1 bugs`, `LLM Token Cost $0.0340`). Because trend charts depict continuous progression over time across a sequence of runs rather than a point-in-time snapshot, showing point values in the legend was misleading and inconsistent with the batch history trend chart design.
- **Root Cause:** `WidgetTrendLayerDto` instances constructed in `WidgetAggregationService.java` were populating `formattedValue` with single-point values from the latest run, and `dashboard-widgets.html` rendered `<span class="val" th:text="${layer.formattedValue}"></span>` next to the category title in the trend graph legend chips.
- **Detection Gap ("What did we miss?"):** Early widget integration tests explicitly asserted that `layer.getFormattedValue()` returned latest-run counts (e.g., `"1 bugs"`, `"$0.0340"`), codifying the point-in-time snapshot behavior instead of testing against the progress-oriented trend visualization specification.
- **Resolution:**
  1. In `aura-manager/src/main/resources/templates/fragments/dashboard-widgets.html`, removed `<span class="val">` from `.ratio-legend-chip` inside `.ratio-legend-list` for trend graph widgets, displaying only the category dot/pattern and layer name.
  2. In `aura-manager/src/main/java/com/xceptance/aura/report/service/WidgetAggregationService.java`, passed empty strings `""` for `formattedValue` across all trend layers (STATUS, BUGS, and COSTS).
  3. In `aura-manager/src/test/java/com/xceptance/aura/report/service/WidgetAggregationServiceTest.java`, updated test assertions to verify that all trend layers supply an empty formatted value.
- **Safety Net Added:** Verified via `WidgetAggregationServiceTest.testBugsTrendGraphRunsMode`, `WidgetAggregationServiceTest.testCostsTrendGraphDailySubDayGranularityFallback`, `WidgetAggregationServiceTest.testStatusTrend_ProportionalMaxTestsScaling_AvoidsFlatBrick`, and `WidgetAggregationServiceTest.testStatusTrend_ShowHealedAi_GeneratesPatternLayers`.
