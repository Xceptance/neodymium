# [DEF-20261103-1218] Status Pie Chart and List Widgets Missing Succeeded-Fixed Bucket Grouping

- **Status:** `Resolved`
- **Opened:** 2026-11-03 12:18
- **Closed:** 2026-11-03 12:25
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** When filtering tests by status on a dashboard page with linked widgets, tests with passing execution and an active linked bug (`succeeded-fixed`) caused the linked Bug Widget to display the bug under the `"Passed"` filter, while the Status Pie Chart and List widgets lumped the test into the generic `"Passed"` bucket rather than displaying it as a distinct status category.
- **Root Cause:** In `WidgetAggregationService.java` (`buildPieChartWidget` and `buildListWidget`) and `report-manager.js` (`normalizeStatusCategory`, `resolveVariationDimensionValue`, and `recalculateStatusPieChart`), tests with `succeeded-fixed` status were counted into internal `fixedClean`/`fixedHealed`/`fixedAi` accumulators, but then collapsed into `passTotal` under the single `"Passed"` slice. Furthermore, client-side cross-filter status matching did not isolate clean passes from fixed passes.
- **Detection Gap ("What did we miss?"):** Existing test `WidgetAggregationServiceTest.testStatusPieChartAggregation` asserted that a 3-test set (clean pass, succeeded-fixed, and unknown fail) produced only 2 slices, explicitly verifying the legacy behavior of collapsing fixed tests into the clean pass bucket rather than enforcing the 4-bucket grouping already modeled in trend layers.
- **Resolution:**
  1. In `WidgetAggregationService.java`, separated `passTotal` and `fixedTotal`, emitting a dedicated `"Succeeded Fixed"` slice (`var(--status-fixed, #3b82f6)`) while preserving overall center pass rate calculation `((passTotal + fixedTotal) * 100.0 / total)`.
  2. In `WidgetAggregationService.java`, added `"Succeeded Fixed"` grouping to `buildListWidget` and included Fixed slices in `buildTextWidget` total passing count.
  3. In `report-manager.js`, updated `normalizeStatusCategory`, `resolveVariationDimensionValue`, `matchesVariationFilter`, and `recalculateStatusPieChart` to recognize and process `"Succeeded Fixed"` as a distinct 4th bucket.
- **Safety Net Added:** Updated `WidgetAggregationServiceTest.testStatusPieChartAggregation` to assert 3 distinct slices, and added `testStatusPieChartFourBucketAggregation` verifying all 4 status buckets (`Passed`, `Succeeded Fixed`, `Known Fail`, `Unknown Fail`) plus `Ignored` in both pie chart and list presentations.
