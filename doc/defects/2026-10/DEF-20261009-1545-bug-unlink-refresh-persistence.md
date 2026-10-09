# [DEF-20261009-1545] Bug Link Removal Is Not Persistent Across Page Refresh, Batch History, or Run History, and Report Page Refresh Reopens Batch History

- **Status:** `Resolved`
- **Opened:** 2026-10-09 15:45
- **Closed:** 2026-10-09 16:08
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** 
  1. Unlinking a bug ticket in Aura Report UI updates the test row and metrics badge immediately in the client browser DOM, but upon page refresh (F5 or `/runs/refresh`) or navigation to Batch History (`/batch-history`) or Run History / Batch Overview (`/report`), the old status (`failed-known` or `succeeded-fixed`) returns with the bug ticket restored.
  2. In addition, when viewing a run report from batch history or batch overview and refreshing the browser page, the browser reloaded the `/batch-history` or `/batch-overview` view instead of staying on the run report (`/run-report?runId=...`).
  3. When attempting to unlink bugs from side panels, `runId` was sent as `#RUN_ID` placeholder or with a leading `#` prefix (e.g. `#run_2026...`), preventing the backend from resolving the run directory or execution.
- **Root Cause:** 
  1. `AuraReportDataService.removeBugFromExecution` modified in-memory caches and the DB `bugRepository`, but never persisted bug ticket removals to disk execution JSON files (`console-execution-*.json`) or `run.json`. When the cache was cleared on page refresh, `getRunReport` re-read raw executions from disk, reviving the unlinked bug.
  2. For tests where bugs originated directly in execution JSON or `@Bug` annotations, no prior `TestBaseBugEntity` existed in `bugRepository`. `removeBugFromExecution` searched for existing DB records, found none, and skipped saving any entity. As a result, `removedTickets` remained empty on disk reloads, failing to suppress the bug.
  3. `getVariationHistory` parsed raw `bugs` query parameters (`&bugs=...`) from `varEntity.getHistoryLinks()` without filtering against removed tickets, while `syncVariationLastStatus` inspected the un-sanitized history string and reinstated `failed-known` / `succeeded-fixed`.
  4. `batchHistory` in `AuraReportViewController` and `getBatchOverviewData` in `AuraReportDataService` synchronized AI and healed counts from `RunReportDto` to `TestRunEntity`, but omitted synchronizing `passedCount`, `succeededFixedCount`, `failedKnownCount`, `failedUnknownCount`, `ignoredCount`, `totalTests`, and recalculating the pass rate.
  5. In HTMX templates `batch-history.html`, `batch-details.html`, and `batch-overview.html`, navigation links targeting the run report omitted `hx-push-url="true"` and used incomplete target attributes, causing the browser address bar to remain at `/batch-history` or `/batch-overview`. Consequently, pressing browser refresh reloaded the previous page instead of the active run report.
  6. In `side-panel-step-list.html` and `report-manager.js`, the `runId` attribute was either unresolved (falling back to placeholder `#RUN_ID`) or preserved the display label's leading `#` prefix. In `AuraReportViewController` and `AuraReportDataService`, missing or hash-prefixed run IDs were not normalized against `HX-Current-URL`, `Referer`, or stripped of `#`.
  7. In multi-file test runs, `console-execution-*.json` files lack a top-level `id` field and the client passes composite keys (`TestClass#method#title#browser`). `LocalRunJsonStorageService.updateExecutionInRun` only checked `nodeId`, `nodeTestId`, `nodeDatasetId`, `fileNameNoExt`, or single-file runs (`jsonFiles.size() == 1`). Consequently, in multi-file runs, execution matching evaluated to `false`, disk JSON files were never modified, and subsequent calls to `buildRunJsonContent` re-read the unmodified execution files, overwriting `run.json` with the old bug tickets and status counts.
  8. `runReportCache` in `AuraReportDataService` was not invalidated during `removeBugFromExecution` and `reevaluateBatchRunsFrom`, causing subsequent requests within the cache TTL to serve stale report DTOs.
- **Detection Gap ("What did we miss?"):** 
  Existing test `AuraReportBugUnlinkTest` only asserted immediate in-memory return values of `getRunReport` without calling `dataService.clearCache()` to simulate page refresh / disk reloads, tested single-file runs with explicit `id` attributes rather than multi-file runs with composite execution keys without explicit `id`, and did not test unlinking bugs originating directly from disk JSON or count synchronization in batch history and overview views. Furthermore, template HTMX attributes and URL push-state navigation were not validated by automated controller integration tests.
- **Resolution:** 
  1. In `LocalRunJsonStorageService.updateExecutionInRun`, extended execution updates to persist changes into both class execution JSON files and nested `run.json` files on disk.
  2. In `LocalRunJsonStorageService.updateExecutionInRun`, enhanced execution file matching in multi-file runs to support composite execution keys (`buildExecutionKey(tClass, tMethod, tTitle, tBrowser)`, `execKeyNoMethod`, `fullKey`, `fullKeyTestId`, `fullKeyDatasetId`, `classMethodBrowser`, or component matching).
  3. In `LocalRunJsonStorageService.updateRunJsonFile`, automatically recalculate the summary block (`total`, `pass`, `fixed`, `known`, `unknown`, `ignored`, `passRate`) when metrics or executions change.
  4. In `AuraReportDataService.removeBugFromExecution`, persist tombstone bug entities when unlinking tickets not previously recorded in `bugRepository`, update disk files via `localRunJsonStorageService.updateExecutionInRun`, rebuild `run.json`, evict `runReportCache`, and sanitize the variation's `historyLinks`.
  5. In `AuraReportDataService.reevaluateBatchRunsFrom`, evict affected `runId`s from `runReportCache` before recalculating batch entity stats.
  6. In `AuraReportDataService.getVariationHistory`, filter history `bugsParam` against `removedTickets`.
  7. In `AuraReportViewController.batchHistory` and `AuraReportDataService.getBatchOverviewData`, synchronize all counts (`passedCount`, `succeededFixedCount`, `failedKnownCount`, `failedUnknownCount`, `ignoredCount`, `totalTests`) and recalculate pass rates on `TestRunEntity`.
  8. In `batch-history.html`, `batch-details.html`, and `batch-overview.html`, added `hx-push-url="true"` to run report links. In `run-report.html`, added `th:data-run-id` to the root container.
  9. In `report-manager.js`, implemented `resolveActiveRunId` prioritizing element data, container `data-run-id`, URL search parameters, and stripping `#`.
  10. In `AuraReportViewController`, implemented `resolveEffectiveRunId` falling back to `HX-Current-URL` or `Referer` when `#RUN_ID` placeholder is provided, and stripping leading `#`.
- **Safety Net Added:** 
  Permanent automated regression tests added:
  - In `AuraReportBugUnlinkTest`:
    - `testBugUnlinkPersistenceAcrossCacheEvictionAndDiskReload`: verifies persistence across `dataService.clearCache()` and batch overview count sync.
    - `testBugUnlinkPersistenceInMultiFileRunWithoutExplicitIdAndCompositeKey`: verifies multi-file runs lacking top-level `id` persist bug unlinking with composite keys across cache eviction and disk reloads.
    - `testUnlinkEmbeddedBugOriginatingDirectlyFromDiskJson`: verifies unlinking bugs embedded directly in execution JSON files on disk without prior DB entity.
    - `testUnlinkBugSanitizesVariationHistory`: verifies variation history links and variation `lastStatus` are sanitized upon bug unlinking.
  - In `AuraReportViewControllerTest`:
    - `testRemoveBugFromExecutionResolvesRunIdFromCurrentUrlWhenPlaceholderProvided`: asserts fallback resolution of `runId` from `HX-Current-URL`.
    - `testRemoveBugFromExecutionStripsHashPrefixFromRunId`: asserts stripping of `#` prefix from `runId`.

