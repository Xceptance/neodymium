# [DEF-20261030-1108] Status Trend Y-Axis Value Truncation for Multi-Digit Test Volumes

- **Status:** `Resolved`
- **Opened:** 2026-10-30 11:08
- **Closed:** 2026-10-30 11:11
- **Component:** `aura-manager`
- **Scope:** `Framework & UI / Styles`
- **Symptom:** On the dashboard trend graph widgets ("Status Trend BY RUNS" and "Status Trend LATEST APPROACH"), the Y-axis top tick value appeared mathematically inverted or corrupted. For example, in a suite with 116 tests, the top label rendered as "16 Tests" above "58 Tests" and "0 Tests", because the leading "1" was clipped off. On cost trend graphs, currency labels like "$0.0099" were pressed tightly against the left SVG boundary.
- **Root Cause:**
  1. `padLeft` was fixed at `55.0` in `WidgetAggregationService.java` and `DashboardWidgetViewDto.java`, placing `text-anchor="end"` labels at `x = padLeft - 8 = 47.0`. For any label with 8 or more characters (e.g., "116 Tests" = 9 characters, ~54.2px wide in JetBrains Mono 10px monospace), the text started at `x = 47.0 - 54.2 = -7.2px`.
  2. Because the SVG viewport clips graphics where `x < 0`, the first character was completely clipped off by the left border of the SVG canvas, turning "116 Tests" into "16 Tests" and "118 Tests" into "18 Tests".
  3. `report-manager.js` contained an identical `xMin = 55` limitation for the Suite Evolution chart on the batch history page.
- **Detection Gap ("What did we miss?"):** Previous tests only tested single- and double-digit test volumes (e.g. `10 Tests` and `5 bugs`), asserting string contents on the DTO model rather than verifying whether multi-digit labels (100+ tests) fit within the non-negative viewBox bounds without clipping.
- **Resolution:**
  1. In `DashboardWidgetViewDto.java` and `WidgetAggregationService.java`, increased `trendPadLeft` to `70.0` (`graphWidth = 710.0`), expanding label clearance to 62px so up to 10 characters (e.g., "9999 Tests", "$0.0500") fit with comfortable padding.
  2. In `WidgetAggregationService.java`, added compact volume formatting for massive test suites (`count >= 10,000` formats as e.g. `15k Tests`).
  3. In `dashboard-widgets.html` and `batch-history.html`, added `style="overflow: visible;"` to trend `<svg>` elements as defense-in-depth against viewport edge clipping.
  4. In `report-manager.js`, updated `batchSuiteEvolutionSvg` `xMin` to `70` to match.
- **Safety Net Added:** Added unit tests `WidgetAggregationServiceTest.testStatusTrend_TripleDigitVolume_AccommodatesInPadLeftWithoutClipping` and `WidgetAggregationServiceTest.testStatusTrend_LargeSuiteVolume_FormatsCompactKiloTests` explicitly asserting that 3-digit and large volume test labels fit within the SVG coordinate space with non-negative starting bounds, and updated existing tests for `padLeft = 70.0`.
