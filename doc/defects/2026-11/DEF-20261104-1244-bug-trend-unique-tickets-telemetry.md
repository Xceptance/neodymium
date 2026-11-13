# [DEF-20261104-1244] Bug Metric Trend Tracks Failed Variations Instead of Unique Active Bugs

- **Status:** `Resolved`
- **Opened:** 2026-11-04 12:44
- **Closed:** 2026-11-04 12:55
- **Component:** `aura-manager`
- **Scope:** `Framework & UI`
- **Symptom:**
  When a dashboard is configured with Metric `BUGS`, a Card widget displays "1 Bug" and "1 Active Bug",
  while the adjacent Trend Chart (and sparkline delta) displays "2 bugs", "+2 bugs vs start",
  and an oscillating line reaching 2 on the Y-axis.
- **Root Cause:**
  1. Point widgets (`CARD`, `TEXT`, `PIE_CHART`, `RATIO_BAR`) use `resolveUniqueBugs(...)`,
     which deduplicates active bug entities by `bugTicket`, correctly reporting 1 unique bug.
  2. Trend widgets (`TREND_GRAPH`, `PERLENKETTE`, and sparklines) evaluated `BUGS` using
     `BatchSuiteEvolutionPointDto.getKnownTotalCount()` or `TestRunEntity.getFailedKnownCount()`,
     which count test executions/variations failing with status `failed-known`.
  3. When 1 bug ticket affects multiple variations (e.g. across multiple browsers or test methods),
     both variations fail as `failed-known`, causing the trend chart to plot 2 test variations
     while labeling the Y-axis and points as "2 bugs".
- **Detection Gap ("What did we miss?"):**
  Previous unit tests in `WidgetAggregationServiceTest` tested deduplication on point widgets
  and tested trend widgets in isolation with synthetic `failedKnownCount` numbers without
  supplying `TestBaseBugEntity` records, missing the semantic mismatch between point widgets and trends.
- **Resolution:**
  1. Implemented historical unique active bug resolution (`resolveUniqueActiveBugCountAtRun` and
     `isBugActiveAtRun`) in `WidgetAggregationService` to evaluate distinct active bug tickets at each
     run milestone based on bug creation/removal markers (`linkedRunId`, `removedRunId`, `createdAtMs`, `removedAtMs`).
  2. Updated `LATEST_APPROACH`, `BY_RUNS`, and time-bucket modes to plot unique active bug counts
     when bug entities are provided, with fallback to execution counts when bug lists are empty.
  3. Fixed pluralization across Y-axis ticks, point values, and deltas ("1 bug" vs "2 bugs").
- **Safety Net Added:**
  Added unit tests in `WidgetAggregationServiceTest` asserting that multiple test variations sharing
  a single bug ticket produce a constant trend of 1 bug across runs and suite evolution points.
