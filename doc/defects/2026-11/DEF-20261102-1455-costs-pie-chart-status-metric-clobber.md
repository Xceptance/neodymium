# [DEF-20261102-1455] United Container Cross-Filtering Clobbers Non-Status Pie Charts (Costs, Tags, AI Usage)

- **Status:** `Resolved`
- **Opened:** 2026-11-02 14:52
- **Closed:** 2026-11-02 14:55
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** When a united container included a Costs Pie Chart (or other non-status pie chart) alongside other widgets:
  - Applying or clearing any cross-filter in the container caused the Costs Pie Chart center label to become `97% PASS` (or the suite-wide pass rate).
  - The donut chart SVG circles vanished into an empty grey ring track.
  - Slices reset to `Prompt Tokens (70%) 0 (0.0%)` and `Completion Tokens (30%) 0 (0.0%)`.
- **Root Cause:**
  1. In `report-manager.js`, `recalculateContainerPieChart` did not inspect `sec.getAttribute('data-metric')`, blindly assuming every pie chart in a container was a `STATUS` pie chart.
  2. It calculated the pass rate (`114 / 118 = 97%`) and unconditionally overwrote the center rate with `97%` and center sub-label with `PASS`, destroying the original `$X.XX COST` KPI.
  3. It looked up SVG circles and breakdown items using status slice keys (`Passed`, `Known Fail`, `Unknown Fail`). Because `Prompt Tokens (70%)` and `Completion Tokens (30%)` did not match status names, their counts were set to 0 and their SVG circle elements were hidden with `style.display = 'none'`.
  4. In `DashboardWidgetViewDto.serializeVariationsToJson`, per-test cost (`llmCost`) and tokens (`llmTokens`) were omitted from the serialized variation dataset.
- **Detection Gap ("What did we miss?"):** Previous cross-filtering tests verified container pie chart recalculation solely with `STATUS` metric widgets, lacking coverage for heterogeneous metric configurations (such as linking a Status Pie Chart with a Costs Pie Chart in the same container).
- **Resolution:**
  1. Updated `recalculateContainerPieChart` to inspect `data-metric` and dispatch to metric-specific recalculators:
     - `recalculateStatusPieChart` for `STATUS` widgets.
     - `recalculateCostsPieChart` for `COSTS` widgets: sums `v.c` (cost) and `v.tok` (tokens) across the active variation subset, maintains `$X.XX COST` center KPI, and recalculates prompt (70%) and completion (30%) slices.
     - `recalculateTagPieChart` for `TAG` widgets: recalculates discrete tag distribution and `ITEMS` center KPI.
     - `recalculateAiUsagePieChart` for `AI_USAGE` widgets: recalculates AI usage percentage and `AI` center KPI.
  2. Updated `DashboardWidgetViewDto.serializeVariationsToJson` to serialize `c` (cost) and `tok` (tokens) for each variation.
- **Safety Net Added:**
  - Added assertions in `WidgetAggregationServiceTest.java` verifying that `c` and `tok` are present in `containerVariationsJson`.
  - Added `testUnitedContainerMetricAwarePieChartDispatchingAndCostsPreservation` in `StaticResourceSyntaxTest.java` verifying metric-aware pie chart recalculation and preservation of the `COST` center label.
