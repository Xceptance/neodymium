# [DEF-20261008-1215] Missing Browser in Execution Metrics Leading to Split TestBase Variations and Chrome Defaulting

- **Status:** `Resolved`
- **Opened:** 2026-10-08 12:15
- **Closed:** 2026-10-08 12:20
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In the TestBase variations table, tests executed with explicit browser profiles like `Chrome_1920x1080` were split into two separate variations:
  1. An obsolete variation named `Chrome` with all historical runs (e.g. 77 runs), even though the underlying test executions were run on `Chrome_1920x1080`.
  2. A separate variation named `Chrome_1920x1080` with only recent/skipped executions (e.g. 2 skipped runs) and an empty history drawer (`runs: null` / `historyLinks = null`).
- **Root Cause:**
  1. In `LocalRunJsonStorageService.buildRunJsonContent()`, the `metricObj` created for each test execution in `executionMetrics` omitted the `"browser"` property, leaving only `id`, `name`, `status`, `durationMs`, etc.
  2. When `RunStorageSyncService.syncTestRunsFromDisk()` deserialized `run.json` or imported legacy disk runs, `exec.path("browser").asText("Chrome")` defaulted the missing property to generic `"Chrome"`. This caused `generateVariationId(testClass, testMethod, dataSet, location, browser)` to calculate hash `var_4b10f63966bd9198` (`Chrome`) instead of the true profile `var_1af773b1700f0faa` (`Chrome_1920x1080`).
  3. When live executions were ingested via `AuraReportDataService.ingestExecution()`, the runtime execution object retained the full browser profile (`Chrome_1920x1080`), generating the canonical variation hash (`var_1af773b1700f0faa`), but leaving `historyLinks = null`.
  4. The in-memory deduplication in `AuraReportDataService.getTestBaseData()` only grouped variations matching identical `(testClass, testMethod, dataSet, location, browser)`, which prevented merging `Chrome` into `Chrome_1920x1080`.
- **Detection Gap ("What did we miss?"):**
  Unit tests in `RunStorageSyncServiceTest` mocked or populated `run.json` with pre-populated `"browser": "Chrome"` or tested variations with uniform browser strings across runs. No integration test verified that an execution recorded with a multi-token browser profile (e.g., `Chrome_1920x1080` in execution ID `#Chrome_1920x1080`) written through `LocalRunJsonStorageService` retained its browser property when synced back into `TestBaseVariationEntity` and reconciled in `AuraReportDataService.getTestBaseData()`.
- **Resolution:**
  1. Updated `LocalRunJsonStorageService.buildRunJsonContent()` to persist `"browser"` (`tBrowser`) in `metricObj` within `run.json`'s `executionMetrics`.
  2. Added `AuraReportDataService.extractBrowserFromIdOrKey(String)` to extract browser profile tokens from execution IDs (e.g. `test...#Chrome_1920x1080` or `.../Chrome_1920x1080/...`) when `"browser"` is missing or generic `"Chrome"`.
  3. Updated `RunStorageSyncService.syncTestRunsFromDisk()` and startup/refresh migration `upgradeLegacyHistoryLinks()` to extract and heal browser profiles from execution IDs, re-attributing executions to their true browser variation.
  4. Updated `AuraReportDataService.getRunReport()` to inject extracted browsers into raw `executionMetrics` before DTO deserialization.
  5. Enhanced `AuraReportDataService.getTestBaseData()` deduplication to reconcile generic `Chrome` variations against referenced execution history links, merge runs into the canonical entity ID, and purge obsolete duplicate records from `TestBaseVariationRepository`.
- **Safety Net Added:**
  - `TestBaseVariationHistoryTest.testExtractBrowserFromIdOrKey()` verifying robust profile parsing from hash fragments and query strings.
  - `TestBaseVariationHistoryTest.testGetTestBaseDataReconcilesBrowserFromHistoryLinkAndMergesDuplicates()` verifying that generic `Chrome` variations are reconciled to `Chrome_1920x1080`, execution counts merged, canonical IDs retained, and duplicate entities purged.
