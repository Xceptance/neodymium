# [DEF-20261109-1000] Tag Metric Perlenkette Ineffective Trend Mode Selection and Aggregation Disconnect

- **Status:** `Resolved`
- **Opened:** 2026-11-09 09:55
- **Closed:** 2026-11-09 10:15
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:**
  1. In the Widget Configurator modal, the "Trend Time Grouping / Mode" selection was displayed for String of Pearls (`PERLENKETTE`) widgets alongside "Target Point / Time Scope", but toggling between `BY_RUNS` and `LATEST_APPROACH` had no effect on the aggregated beads or pass rates for `TAG` metric widgets.
  2. Irrelevant calendar bucket options (`Daily`, `Hourly`, `Monthly`) were shown for categorical milestone beads where time bucket grouping is invalid.
- **Root Cause:**
  1. In `WidgetAggregationService#populateTagPerlenketteBeads`, execution aggregation was hardcoded to deduplicate test cases by test key (`LATEST_APPROACH` suite evolution logic) unconditionally without reading `config.getTrendMode()`.
  2. The configurator modal did not tailor trend mode pills based on metric context: `TAG` widgets group data by categorical milestones rather than calendar buckets, making calendar-based time grouping (`Daily`, `Hourly`, `Monthly`) semantically inapplicable while leaving the essential distinction between total execution volume (`BY_RUNS`) vs. suite deduplication (`LATEST_APPROACH`) unexposed and uncontextualized.
- **Detection Gap ("What did we miss?"):**
  Previous tests for tag-based String of Pearls (`WidgetAggregationServiceTest#testTagPerlenketteChronologicalMilestonesAcrossRuns`) only asserted chronological ordering and bead generation under the default configuration, without testing the behavioral divergence between `BY_RUNS` (all CI execution volume) and `LATEST_APPROACH` (latest variation per test).
- **Resolution:**
  1. In `WidgetAggregationService#populateTagPerlenketteBeads`, integrated `config.getTrendMode()`:
     - `BY_RUNS`: Aggregates all test executions across all eligible runs in each milestone (measuring total CI run volume and execution pass rates).
     - `LATEST_APPROACH` (default): Evaluates the latest execution of each unique test variation within each milestone (measuring milestone suite evolution).
     - Populated bead tooltip details with `"Aggregation Mode": "All Run Executions (Volume)"` or `"Suite Evolution (Deduplicated)"` and added `"Total Executions"` when in `BY_RUNS` mode.
  2. In `widget-configurator-modal.html` and `report-manager.js`:
     - Dynamically contextualized the section header as `Milestone Aggregation Mode` when `metric == TAG`.
     - Renamed `By Runs` pill label to `All Run Executions (Volume)`.
     - Hid calendar bucket options (`Daily`, `Hourly`, `Monthly`) for `TAG` metric, automatically redirecting any selected calendar bucket to `LATEST_APPROACH`.
     - Preserved full `Trend Time Grouping / Mode` pills for non-`TAG` metrics while hiding `pointScope`.
- **Safety Net Added:**
  `WidgetAggregationServiceTest#testTagPerlenketteTrendMode_ByRunsVsLatestApproach` verifying that a multi-run milestone with re-run / retried tests evaluates to 3 unique tests (100% pass rate) under `LATEST_APPROACH`, but evaluates to 5 total executions (4 passed, 80% pass rate) under `BY_RUNS`.
