# [DEF-20261023-1430] Test Class Appears as Test Variation in Batch History Overview

- **Status:** `Resolved`
- **Opened:** 2026-10-23 14:30
- **Closed:** 2026-10-23 14:35
- **Component:** `aura-manager` & `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In the "Known Test Base Variations" table on the Batch History Overview page, the test class name itself appears as a variation row (e.g. `Aura_my_test_yaml_Test (14 runs)` and `Aura_tests_examples_wikipedia_search_yml_Test (24 runs)` with status SKIPPED), artificially inflating variation counts and skewing batch history distribution statistics.
- **Root Cause:** When queue executions are cancelled prior to test start, `AuraQueueService.markRunningOrMissingExecutionsAsSkipped` generated fallback snapshots with `testName` set to `className` and no `testMethod`. When ingesting into `run.json`, `LocalRunJsonStorageService.buildRunJsonContent` populated `title` with `testName` (`className`). Unlike `getTestBaseData()` which filters by non-empty `testMethodName`, `AuraReportDataService.getBatchTestBaseClasses` and `getBatchLatestTestExecutionsSummary` ingested these fallback records directly from `report.getExecutions()`, registering variations where `testMethod = ""` and `dataSetLabel = className`.
- **Detection Gap ("What did we miss?"):** Existing unit tests in `TestBaseVariationHistoryTest` only supplied mock executions with valid, explicit `testMethod` strings, missing the cancelled queue fallback scenario where `testMethod` is blank and `title` equals `testClass`.
- **Resolution:**
  1. Added `isValidTestVariation` in `AuraReportDataService` enforcing that variations must have a non-blank `testMethodName` and that `dataSet` does not match `testClass`.
  2. Applied `isValidTestVariation` filtering in `getBatchTestBaseClasses`, `getBatchLatestTestExecutionsSummary`, and `getBatchLatestTestExecutions` to skip class container placeholders.
  3. Updated `LocalRunJsonStorageService` so `title` is not assigned `className` when `testName` matches `testClass`.
  4. Updated `AuraQueueService.markRunningOrMissingExecutionsAsSkipped` to explicitly record `testClass` in fallback snapshots.
  5. Added defensive rendering in `batch-history.html` preventing `cls.className` from being rendered as a variation title or dataset label.
- **Safety Net Added:** Added `@Test` `TestBaseVariationHistoryTest.testBatchTestBaseClassesAndSummaryExcludesClassPlaceholderExecutions` asserting that class placeholder executions with blank test methods are excluded from `getBatchTestBaseClasses` and `getBatchLatestTestExecutionsSummary`.
