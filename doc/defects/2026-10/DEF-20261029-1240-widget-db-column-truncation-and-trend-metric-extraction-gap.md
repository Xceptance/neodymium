# [DEF-20261029-1240] Widget Database Column Truncation and Trend Metric Extraction Gap

- **Status:** `Resolved`
- **Opened:** 2026-10-29 12:40
- **Closed:** 2026-10-29 12:51 
- **Component:** `aura-manager`
- **Scope:** `Framework & SUT`
- **Symptom:** Saving or deleting dashboard widgets logged SQL error `Value too long for column "SETTING_VALUE CHARACTER VARYING(500)"`. In addition, configured trend graphs and perlenkette charts for Costs, Bugs, or Daily/Hourly/Monthly modes extracted 0 values or displayed overall run pass rates instead of metric-specific data.
- **Root Cause:** 
  1. `ReportSettingEntity.setting_value` was limited to `length = 500`, whereas widget layout JSON exceeds 900 bytes.
  2. `WidgetAggregationService.buildTrendGraphWidget` bypassed time bucketing for all metrics and bypassed metric extraction for Bugs and Costs, falling back to `r.getPassRate()`.
  3. `TimeBucket.addRun` omitted accumulating `totalCost` and `activeBugsCount`, and `buildPerlenketteWidget` had hardcoded `"$0.00"` for by-runs costs.
- **Detection Gap ("What did we miss?"):** Unit tests used short widget titles producing JSON $<500$ chars, and trend tests only verified `LATEST_APPROACH` status evolution rather than parameter combinations of `(COSTS, DAILY)` or `(BUGS, BY_RUNS)`.
- **Resolution:**
  1. Altered `ReportSettingEntity.setting_value` to `columnDefinition = "TEXT"` with automatic startup schema migration.
  2. Implemented metric-aware value extraction across all trend modes (BY_RUNS, LATEST_APPROACH, DAILY, HOURLY, MONTHLY) for STATUS, COSTS, and BUGS in both `buildTrendGraphWidget` and `buildPerlenketteWidget`.
  3. Updated `TimeBucket.addRun` to accumulate `totalCost` and `activeBugsCount`.
- **Safety Net Added:** Added test cases in `WidgetAggregationServiceTest` and `ReportDashboardSettingServiceTest` covering large payload persistence and trend metric extraction across time modes.
