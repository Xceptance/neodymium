# Defect Record: DEF-20261009-1635-bug-unlink-method-mismatch

- **Status:** Resolved
- **Scope:** Framework
- **Component:** `AuraReportDataService`, `TestExecutionDto`, `LocalRunJsonStorageService`, `RunStorageSyncService`
- **Discovered:** 2026-10-09 16:35 CEST
- **Resolved:** 2026-10-09 16:47 CEST
- **Reporter:** Antigravity

### 1. Symptom
Unlinking a bug ticket in Aura Report appears successful dynamically, but refreshing the page (browser reload or POST /runs/refresh) causes the unlinked bug ticket to reappear on the execution and reverts the execution status from passed-clean / failed-unknown back to succeeded-fixed / failed-known.

### 2. Root Cause
1. `LocalRunJsonStorageService.extractTestMethod` treats `#executeTest` as empty string `""`, creating variations and bugs under `varId(testClass, "", dataSet, ...)`.
2. `TestExecutionDto` did not ignore `#executeTest`, setting `testMethod = "executeTest"`.
3. `removeBugFromExecution` unlinked against `varId(testClass, "executeTest", ...)`, failing to match the existing bug under `varId(testClass, "", ...)`, leaving the active bug in DB and the variation history link un-sanitized.
4. On refresh or `syncLocalRunStorage()`, the active bug in DB was re-discovered and resurrected back into `run.json` and the report.
5. In `LocalRunJsonStorageService.updateRunJsonFile`, key matching in `executionMetrics` only checked literal `entry.getKey().equalsIgnoreCase(rowId)` and lacked composite key resolution.

### 3. Detection Gap
Existing unit tests passed identical custom method names in both the fixture and the test assertions, never executing with Neodymium's standard default test method name `#executeTest` across both `TestExecutionDto` and `RunStorageSyncService`.

### 4. Resolution
1. **Normalized `TestExecutionDto`:** Uniformly ignore `"executeTest"` across all sources (`testMethod` argument, `junitTags`, `testFile`, and `#` split from `id`), ensuring consistent `testMethod = ""` alignment with `LocalRunJsonStorageService.extractTestMethod`.
2. **Dual Variation ID Handling in `AuraReportDataService`:** In `removeBugFromExecution`, compute both `varId` and `varIdNoMethod` (fallback without method), mark removed in DB across both, save tombstones for both if no match was found, sanitize history links on both variation entities, and re-evaluate runs for both IDs.
3. **Dual Variation Lookup in Report Generation:** In `getRunReport`, `resolveBugsForExecution`, `updateCachedReportBugsAndStats`, and `getVariationHistory`, consult both `varId` and `varIdNoMethod` for active and removed bugs.
4. **Enhanced `updateRunJsonFile`:** Added composite key matching (`execKey`, `execKeyNoMethod`, `fullKey`, `classMethodBrowser`, `mId`) across `executionMetrics` and `executions` array in `LocalRunJsonStorageService`.
5. **Storage Sync Dual Checking:** In `RunStorageSyncService.syncLocalRunsFromDisk`, query both `varId` and `varIdNoMethod` for active and removed bugs when syncing from disk.

### 5. Safety Net Verification
New test `testBugUnlinkPersistenceOverRefreshWithExecuteTestMethod` in `AuraReportBugUnlinkTest` asserting that an execution using `#executeTest` unlinks cleanly, tombstoning both method variations, sanitizing history links, updating `run.json`, and persisting across `dataService.clearCache()` and `runStorageSyncService.syncLocalRunStorage()`. Verified with all 9 tests passing in `AuraReportBugUnlinkTest` and all 10 tests passing in `AuraReportViewControllerTest`.
