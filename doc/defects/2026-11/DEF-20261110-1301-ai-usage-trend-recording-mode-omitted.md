# [DEF-20261110-1301] AI Usage Trend Graph Omits RECORDING Mode Layer

- **Status:** `Resolved`
- **Opened:** 2026-11-10 12:45
- **Closed:** 2026-11-10 13:01
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In the "AI Usage Trend Graph (Latest Approach) BY RUNS" widget and time-bucketed trend graphs (`HOURLY`, `DAILY`, `MONTHLY`), tests executed under `RECORDING` mode (formerly `PLAYBOOK`) were not displayed as a trend layer or in the legend. Only `Classic`, `AI-Driven`, and `Healed` were rendered, while recording tests fell through into `Classic`.
- **Root Cause:**
  1. `TestRunEntity` lacked a database column mapping and accessors for `playbook_count`, causing run stats calculated during storage ingestion (`RunStorageSyncService`) or refresh (`AuraReportDataService`) to drop the count of playbook/recording tests.
  2. `WidgetAggregationService` had hardcoded `0` for `playbook` in `AiUsagePointBreakdown` across `BY_RUNS`, `HOURLY`, `DAILY`, and `MONTHLY` modes.
  3. In `AuraReportDataService.buildSuiteEvolution`, execution mode was checked with `"PLAYBOOK".equalsIgnoreCase(state.mode())`, whereas modern execution DTOs standardize playbook mode to `"RECORDING"` (`TestExecutionDto.getDisplayMode()`), causing recording tests to evaluate to `isPlaybook = false` and fall back into `pClean` (Classic).
  4. Trend graphs filter out layers where cumulative `sum == 0`, completely omitting the `"Recording"` layer and legend chip whenever `playbookCount == 0`.
- **Detection Gap ("What did we miss?"):** Existing unit tests in `WidgetAggregationServiceTest` and `BatchSuiteEvolutionDtoTest` primarily tested synthetic runs with only AI-driven, healed, or classic test distributions, or verified `getTotalAiUsageCount()` without asserting the presence of the `Recording` layer in multi-series trend layer aggregation for `BY_RUNS` and time-bucketed modes.
- **Resolution:**
  1. Added `@Column(name = "playbook_count") private Integer playbookCount = 0;` with safe accessor `getPlaybookCountSafe()` and mutators in `TestRunEntity.java`.
  2. Updated `RunStorageSyncService.java` to compute `calcPlaybook` when `calculatedMode` is `PLAYBOOK` and not healed, persisting it to `runEntity.setPlaybookCount()`.
  3. Updated `AuraReportDataService.java` to sync `playbookCount` during run refreshes and `recalculateRunEntityStats`, and expanded `buildSuiteEvolution` to match `"RECORDING".equalsIgnoreCase(state.mode()) || "PLAYBOOK".equalsIgnoreCase(state.mode())`.
  4. Updated `WidgetAggregationService.java` to resolve playbook counts via `resolveRunPlaybookCount` (inspecting `TestRunEntity.getPlaybookCountSafe()`, falling back to `AuraReportDataService.getRunReport()` and `latestVariations`), tracked `playbookCount` in `TimeBucket`, populated `playbookCount` in `AiUsagePointBreakdown`, renamed display layer to `"Recording"` (`#0284c7`), and included `"Recording"` and `"Classic"` in bead tooltips and latest-approach breakdowns.
- **Safety Net Added:**
  - `TestRunEntityTest.testPlaybookCount`: asserts default, null-safe fallback, and getter/setter for `playbookCount`.
  - `TestBaseVariationHistoryTest.testSuiteEvolution_RecordsPlaybookAndRecordingMode`: asserts suite evolution points count both `RECORDING` and `PLAYBOOK` modes as `playbookCount` and correctly adjusts `classicCount`.
  - `WidgetAggregationServiceTest.testAiUsageTrendGraphByRuns_IncludesRecordingLayer`: asserts `BY_RUNS` trend graph contains the 4th layer `"Recording"` with color `#0284c7`, raw values CSV, and paths.
  - `WidgetAggregationServiceTest.testAiUsageTrendGraphByRuns_FallbackToDataService`: asserts fallback resolution to `AuraReportDataService` when entity count is zero.
  - `WidgetAggregationServiceTest.testAiUsageTrendGraphHourly_IncludesRecordingLayer`: asserts `HOURLY` bucket aggregation tracks and renders the `"Recording"` layer.
