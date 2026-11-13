# [DEF-20261104-0950] Trend Chart Y-Axis Scaling Squashes Tag Curves, Freezes on Legend Filtering, and Duplicates Legend Chips

- **Status:** `Resolved`
- **Opened:** 2026-11-04 09:50
- **Closed:** 2026-11-04 10:18
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:**
  1. In unstacked line mode (`Fill` unchecked / `isFilled() == false`) for `TAG` metric widgets, curves were severely compressed into the bottom 2% of the canvas baseline.
  2. The Y-axis top tick displayed the sum of all tests across the entire suite (e.g. 118 or 236 tests) rather than the maximal single tag count (maximal known Y-value) of the individual curves.
  3. Clicking a legend chip to isolate a tag/area layer hid the inactive curves but left the isolated curve flat at the bottom and left the Y-axis ticks frozen at the full suite peak rather than dynamically rescaling to the isolated layer's peak.
  4. The legend displayed duplicate chips for the same area dimension (e.g. `@International` and `International`), doubling the total counted test volume.
- **Root Cause:**
  1. In `AuraReportDataService.java` (`calculateEvolutionPoints`), tests were registered under `varAreaMap` with `@` (e.g. `@International`) and then traversed again in `cTags` without `@` (e.g. `International`), causing test runs to register twice and producing duplicate chips with double the test volume.
  2. In `WidgetAggregationService.java`, `baseScale` was uniformly calculated as `graphHeight / maxStackedSum` (the sum across all tags), squashing individual lines when individual tag counts were much smaller than the whole suite total.
  3. In `WidgetAggregationService.java`, Y-axis ticks always took the sum of all tags across the suite regardless of `config.isFilled()`.
  4. In `report-manager.js`, `toggleWidgetTrendLayerSelect` toggled visibility classes on inactive layers and adjusted dot positions, but never recomputed the SVG line path `d` or updated `.widget-trend-y-axis` tick labels to match the isolated layer's peak value.
  5. In `WidgetTrendLayerDto.java` and `dashboard-widgets.html`, layers lacked per-layer raw count telemetry (`peakCount`, `rawValuesCsv`, `data-raw-values`, `data-max-val`, `data-orig-d`) and the SVG element lacked layout metadata (`data-trend-width`, `data-trend-height`, `data-pad-left`, `data-baseline-y`).
- **Detection Gap ("What did we miss?"):** Previous tests in `WidgetAggregationServiceTest.java` asserted that multi-line trend layers were generated and that `points` had values, but did not assert that the top Y-axis tick matched the maximal single tag count in unstacked mode, nor did they assert layer peak telemetry attributes or dynamic client-side Y-axis rescaling.
- **Resolution:**
  1. Fixed `AuraReportDataService.java` to skip standard dimensions (`area`, `browser`, `locale`, `location`, `environment`, `env`) during `cTags` traversal, preventing double-counting and duplicate chips.
  2. Added `peakCount` and `rawValuesCsv` to `WidgetTrendLayerDto.java` with corresponding getters and constructors.
  3. In `WidgetAggregationService.java`, implemented dual scaling for `TAG` metric: `stackedScale` for filled area polygons and `standaloneScale` for unstacked lines.
  4. In `WidgetAggregationService.java`, scaled Y-axis ticks in unstacked mode (`config.isFilled() == false`) to `maxSingleTagCount` (the maximal known Y-value) instead of the suite sum. Added robust tag matching (`addDistinctTagValue`, `stripAt`, `resolveCountForTag`, `resolveTagColor`).
  5. In `dashboard-widgets.html`, added `class="widget-trend-svg"` and layout data attributes (`data-trend-width`, `data-trend-height`, `data-pad-left`, `data-pad-right`, `data-pad-top`, `data-pad-bottom`, `data-baseline-y`) to `<svg>`, and telemetry attributes (`data-raw-values`, `data-max-val`, `data-orig-d`) to lines and chips.
  6. In `report-manager.js`, implemented `updateTrendYAxis` and `restoreTrendYAxis`. Updated `toggleWidgetTrendLayerSelect` to dynamically recalculate the isolated curve path `d`, node dots, and Y-axis tick values to `isolatedMax`, and cleanly restore un-isolated multi-line view upon deselecting.
- **Safety Net Added:** Added automated regression test `testTrendGraph_TagMetric_UnstackedVsStacked_YAxisScaling` in `WidgetAggregationServiceTest.java` verifying unstacked vs stacked Y-axis scaling, peak count, and raw values CSV telemetry. Added tests in `StaticResourceSyntaxTest.java` verifying `updateTrendYAxis`, `restoreTrendYAxis`, `data-raw-values`, and `data-orig-d`.
