# [DEF-20261110-1205] Dashboard Widget Playbook Mode and Incomplete Unknown Fail UI Renaming

- **Status:** `Resolved`
- **Opened:** 2026-11-10 12:05
- **Closed:** 2026-11-10 12:18
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** After renaming execution mode `PLAYBOOK` to `RECORDING` on the run report table, dashboard widgets (pie slices, trend layers, ratio bars, text KPIs, list views, and pearl details) continued to display `Playbook`. Furthermore, status badges, KPI cards, area badges, and widget breakdowns continued displaying `Unknown Fail` / `UNKNOWN FAIL` instead of `Fail` / `FAIL`.
- **Root Cause:**
  1. `WidgetAggregationService` and `report-manager.js` hardcoded `"Playbook"` across AI usage breakdown slices, trend layers, and dimension aggregations, and `computeAiUsageBreakdown` only checked `"PLAYBOOK".equalsIgnoreCase(v.getMode())`, ignoring variations saved with `"RECORDING"`.
  2. `AuraReportDataService.getStatusBadgeInfo` returned `statusLabel = "UNKNOWN FAIL"`, and UI templates (`run-report.html`, `test-base.html`, `dashboard-widgets.html`, `custom-dashboard-page.html`, `widget-configurator-modal.html`) hardcoded `"Unknown Fail"` and `"UNKNOWN FAIL"` on KPI cards, badges, and breakdown legends.
- **Detection Gap ("What did we miss?"):** Previous unit tests only validated the execution DTO display mode (`TestExecutionDto.getDisplayMode()`) and the single donut breakdown label, but did not assert widget presentation slices, trend layer names, or template badge text for `Fail` and `Recording`.
- **Resolution:**
  1. Updated `WidgetAggregationService.java` to support `"RECORDING"` alongside `"PLAYBOOK"` and label all AI usage pie slices, ratio segments, trend layers, list items, and pearl details as `"Recording"`.
  2. Updated `WidgetAggregationService.java` to label status pie slices, ratio segments, list items, and pearl details as `"Fail"`.
  3. Updated `AuraReportDataService.java` to return `"FAIL"` for unknown failure status.
  4. Updated all HTML templates (`run-report.html`, `test-base.html`, `dashboard-widgets.html`, `custom-dashboard-page.html`, `widget-configurator-modal.html`) to display `"Fail"` / `"FAIL"` and `"Recording"`.
  5. Updated `report-manager.js` client-side normalization, breakdown maps, and interactive filters to produce and filter `"Fail"` and `"Recording"` while maintaining backward-compatible aliases.
- **Safety Net Added:**
  - Added `@Test` `WidgetAggregationServiceTest.testAiUsagePieChart_RecordingSlice` asserting that `"RECORDING"` variations produce `"Recording"` pie slices with color `#0284c7`.
  - Added `@Test` `WidgetAggregationServiceTest.testStatusPieChart_FailSlice` asserting that unknown failures produce `"Fail"` pie slices with color `var(--status-unknown-fail, #ef4444)`.
  - Updated assertions across `WidgetAggregationServiceTest` and `TestBaseVariationHistoryTest`.
