# [DEF-20261110-1405] Tag Perlenkette Collapses Parameterized and Data-Driven Tests under Latest Approach Mode

- **Status:** `Resolved`
- **Opened:** 2026-11-10 14:05
- **Closed:** 2026-11-10 14:10
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** On dashboards displaying a String of Pearls (`PERLENKETTE`) widget with `Metric: TAG` (e.g. Release) and `Trend Mode: LATEST_APPROACH`, milestone pearls collapsed multiple parameterized, data-driven, or YAML test cases of the same test class and method into a single test (e.g. displaying "1 test" instead of "2 tests"), while all other dashboard widgets and the contributing executions modal accurately reported 2 tests.
- **Root Cause:** In `WidgetAggregationService#populateTagPerlenketteBeads`, the deduplication map key (`execKey`) for `LATEST_APPROACH` mode was constructed using only `testClass#testMethod#browser#location`, omitting `exec.getTitle()` (and `dataService.buildLogicalKey`). When multiple parameterized, data-driven, or YAML test cases execute within the same test class and method, subsequent executions produced identical keys and overwrote earlier executions in `milestoneExecutions`, collapsing distinct test scenarios into 1.
- **Detection Gap ("What did we miss?"):** Previous tests in `WidgetAggregationServiceTest#testTagPerlenketteTrendMode_ByRunsVsLatestApproach` tested deduplication across distinct test classes (`TestClassA`, `TestClassB`, `TestClassC`), and never exercised parameterized or data-driven tests sharing the same class and method with different test scenario titles.
- **Resolution:** In `WidgetAggregationService#populateTagPerlenketteBeads`, constructed the deduplication key using `dataService.buildLogicalKey(r, exec)` when available, falling back to a composite key that explicitly includes `dataSet` / `exec.getTitle()`, ensuring distinct parameterized and data-driven test variations are preserved during milestone suite evolution deduplication.
- **Safety Net Added:** Added `WidgetAggregationServiceTest#testTagPerlenketteLatestApproach_PreservesParameterizedAndDataDrivenScenarios` explicitly asserting that parameterized and data-driven tests sharing the same class and method are both preserved and counted within their milestone bead under `LATEST_APPROACH`.
