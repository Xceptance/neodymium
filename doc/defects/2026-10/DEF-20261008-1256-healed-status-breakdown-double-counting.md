# [DEF-20261008-1256] Healed Tests Double-Counted in Area Breakdown List and Omitted from Donut Chart

- **Status:** `Resolved`
- **Opened:** 2026-10-08 12:56
- **Closed:** 2026-10-08 13:00
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In the Area Breakdown cards on the Overview tab, executions with self-healing were displayed as standalone sibling rows ("Passed Healed", "Failed Healed") alongside primary outcome statuses ("Successful", "Succeeded-Fixed", "Known Failed"). This caused the breakdown items to sum to more than the total executed tests (e.g. 2 executed tests appeared as 3 tests). Simultaneously, the SVG donut chart only rendered primary status slices, leaving healed counts completely unrepresented in the chart.
- **Root Cause:** 
  1. The HTML template and client-side JavaScript treated self-healing as an exclusive test status rather than an orthogonal execution modifier/tag.
  2. `AreaSummaryDto`, `TestClassSummaryDto`, and `report-manager.js` only aggregated binary `passedHealedCount` and `failedHealedCount` across mixed statuses rather than tracking granular per-status healing (`passHealedCount`, `fixedHealedCount`, `knownHealedCount`, `unknownHealedCount`).
- **Detection Gap ("What did we miss?"):** Existing tests (`RunStorageSyncServiceTest`) only asserted backend aggregation of aggregate `passedHealedCount`, not HTML template row semantics or chart SVG sub-slice generation.
- **Resolution:** 
  1. Extended `TestExecutionDto` with granular status predicates (`isPassHealed`, `isFixedHealed`, `isKnownHealed`, `isUnknownHealed`) while preserving backward-compatible `isPassedHealed` and `isFailedHealed`.
  2. Added per-status healed getters to `TestClassSummaryDto`, `AreaSummaryDto`, `RunReportDto`, `TestRunEntity`, and `BatchSummaryDto`.
  3. Replaced standalone sibling rows in `run-report.html` and `batch-overview.html` breakdown lists with compact inline badges (`badge-healed-inline`) adjacent to primary status counts.
  4. Updated `report-manager.js` to calculate granular per-status healing and render Combo 1 (luminous tinted sub-arcs + crisp tiled SVG sparkle print overlay pattern `<pattern id="sparkle-pat-...">`) within SVG donut charts across run reports and batch overview cards.
  5. Implemented luminous healed segments with CSS sparkle print overlay (`.seg-healed-print`, `.seg-pass-healed`, etc.) on all `status-stacked-bar` elements in run directory and batch history tables.
  6. Updated batch history trend chart and hover tooltip in `report-manager.js` to render inline `badge-healed-inline` indicators and distinct luminous gold-ringed trend nodes for runs containing healed tests.
  7. Styled `.badge-healed-inline`, `.seg-healed-print`, and luminous color variables in `report-manager.css` supporting light and dark themes.
- **Safety Net Added:** 
  - `AreaSummaryDtoTest.testGranularHealingPerStatusInExecutionsAndSummaries` and `AreaSummaryDtoTest.testTwoExecutionsOnePassedHealedOneSucceededFixedClean` asserting granular per-status healing aggregation and total execution arithmetic consistency.
  - Verification across targeted suites (`AreaSummaryDtoTest`, `TestExecutionDtoTest`, `RunStorageSyncServiceTest`) confirming 27 passing tests.
