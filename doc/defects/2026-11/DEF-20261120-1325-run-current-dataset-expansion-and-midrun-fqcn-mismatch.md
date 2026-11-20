# [DEF-20261120-1325] Run Current Dataset Expansion and Mid-Run Test Class Accordion Split

- **Status:** `Resolved`
- **Opened:** 2026-11-20 13:20
- **Closed:** 2026-11-20 13:40
- **Component:** `aura-manager` & `neodymium-core`
- **Scope:** `Framework`
- **Symptom:**
  1. When triggering "Run Open Test" on a YAML test with multiple datasets (such as `wikipedia_search.yml` with 2 datasets), the sidebar runner progress displayed `1/1` during execution instead of reflecting the actual total test count (e.g. `2/2` or `datasets * browserProfiles`). The second test only appeared after the entire batch finished.
  2. During an active run in `/run-report`, tests from the same class were split into two separate class accordions: the completed test appeared under the simple class name (`Aura_tests_examples_wikipedia_search_yml_Test`), while the running test appeared under the FQCN (`com.xceptance.neodymium.aura.sandbox.Aura_tests_examples_wikipedia_search_yml_Test`). Once the run completed, they collapsed back into a single accordion.
- **Root Cause:**
  1. In `AuraTestQueueController.startExecution()`, `runCurrent` created a single `DatasetSelection` with `sel.id = null`, instead of looking up the active file in `fileService.getYamlFilesList()` and expanding all defined datasets. Additionally, `AuraTestQueueController.getStatusStream()` and `AuraGlobalModelAdvice` calculated `scheduledCount` directly as `lastReq.datasets.size()` without factoring in the effective browser profiles configured for each dataset or globally.
  2. In `AuraQueueService.ingestBatchExecutionFiles()`, when ingesting `console-execution-*.json`, line 1440 unconditionally performed `payload.put("testClass", className)`, clobbering the fully qualified class name in the JSON payload with the directory/runner name (`className` = simple class name). As a result, the live run buffer had the simple class name for completed tests, while uncompleted or disk-read tests used the FQCN. Furthermore, `AuraReportDataService.buildAreaSummaries()` grouped executions using raw string equality on `e.getTestClass()`, creating two distinct groups.
- **Detection Gap ("What did we miss?"):**
  1. Unit tests for `runCurrent` only asserted a single dataset request without mocking multi-dataset files from `AuraFileService`.
  2. Integration tests for `/run-report` and `buildAreaSummaries()` only tested test executions with uniform class names, missing the mid-run transition where `liveRunBuffer` ingested simple class names while concurrently running tests used FQCNs.
- **Resolution:**
  1. Updated `AuraTestQueueController.startExecution()` to look up `activeFile` in `fileService.getYamlFilesList()`, expanding all datasets into individual `DatasetSelection` items.
  2. Updated `AuraTestQueueController.getStatusStream()` and `AuraGlobalModelAdvice` to compute `scheduledCount` by summing the effective browser profiles per dataset.
  3. Modified `AuraQueueService.ingestBatchExecutionFiles()` to preserve any existing, non-blank `testClass` in `payload` before falling back to `className`.
  4. Introduced `groupByCanonicalClass()` in `AuraReportDataService.buildAreaSummaries()` to canonicalize class names using `extractSimpleClassName()` and prefer fully qualified names for display, preventing accordion splitting.
- **Safety Net Added:**
  - `AuraTestQueueControllerTest.testStartExecutionRunCurrentExpandsMultipleDatasetsFromYamlFile`
  - `AuraTestQueueControllerTest.testGetStatusStreamScheduledCountCalculatedWithProfilesAndDatasets`
  - `AuraReportSubAreaHierarchyTest.testBuildAreaSummariesMergesSimpleAndFullyQualifiedClassNames`
