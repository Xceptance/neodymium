# [DEF-20261008-1545] Healed Execution Pattern Omitted from Batch History Trend and Area Trend Charts

- **Status:** `Resolved`
- **Opened:** 2026-10-08 15:45
- **Closed:** 2026-10-08 15:51
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In the Batch History Trend overview (`batch-history.html`), trend nodes show gold rings for runs with healed tests and hover tooltips show healed badges, but the stacked area trend chart (`#mainTrendSvg`) rendered all outcome areas as monolithic solid fills, completely omitting the self-healing sparkle pattern and distinct healed sub-layers. In addition, the chart legend did not display healed pattern indicators, and the included area trend graphs omitted healed sub-layers.
- **Root Cause:**
  1. `batch-history.html` omitted `<pattern id="trendSparklePattern">` in `#mainTrendSvg` `<defs>` and only defined 5 coarse status paths (`trendLayerIgnored`, `trendLayerUnknown`, `trendLayerKnown`, `trendLayerFixed`, `trendLayerPass`) without clean/healed sub-layers or pattern overlays.
  2. `report-manager.js` (`renderDynamicTrendChart` and `renderDynamicAreaTrendCharts`) parsed healed counts but stacked total counts directly (`y0` through `y5`), never computing clean vs healed boundaries (`y_clean` vs `y`) or generating paths for healed fills and pattern overlays.
  3. `report-manager.css` restricted `.seg-healed-print` to `.status-bar-seg`, preventing its reuse on chart legend indicator boxes.
  4. `AreaRunPointDto` and `AuraReportViewController.java` did not transport per-status healed counts for area trend graphs.
- **Detection Gap ("What did we miss?"):** Previous healing integration tests verified backend DTO aggregations, donut chart sub-slices, and table status bar segments, but did not assert SVG path definitions or layer partitioning in stacked area trend charts.
- **Resolution:**
  1. Added `<pattern id="trendSparklePattern">` to `#mainTrendSvg` and `<pattern th:id="'areaSparklePat-' + ${area.areaGroup}">` to area trend SVGs in `batch-history.html`.
  2. Defined clean and healed stacked layers (`trendLayerPass`, `trendLayerPassHealed`, `trendLayerPassHealedPattern`, `trendLayerFixed`, `trendLayerFixedHealed`, `trendLayerFixedHealedPattern`, etc.) with luminous base fills and sparkle pattern overlays.
  3. Updated `renderDynamicTrendChart` and `renderDynamicAreaTrendCharts` in `report-manager.js` to calculate clean vs healed elevations and render bezier paths for both clean and healed sub-layers.
  4. Updated `batch-history.html` legend and `report-manager.css` to display healed pattern legend items (`.legend-box.seg-healed-print`).
  5. Extended `AreaRunPointDto` with healed metrics (`passHealedCount`, `fixedHealedCount`, `knownHealedCount`, `unknownHealedCount`) and clean helper getters, and updated `AuraReportViewController.java` to populate them.
- **Safety Net Added:** 
  - `AreaRunPointDtoTest` asserting 8- and 12-parameter constructor semantics, field preservation, and clean metrics calculation.
  - `AuraReportViewControllerTest.testBatchHistoryPopulatesGranularHealingInAreaTrends` asserting that granular healed counts are properly transported into `BatchAreaTrendDto` and `AreaRunPointDto`.
