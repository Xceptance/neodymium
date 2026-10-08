# [DEF-20261008-1135] Execution Mode Label Overlap and Stale Failure Status in Test Base View

- **Status:** `Resolved`
- **Opened:** 2026-10-08 11:35
- **Closed:** 2026-10-08 11:45
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In the Test Base view, long execution mode labels such as `REPLAY_WITH_HEALING` (19 characters) clipped and overlapped with the adjacent TIMESTAMP column in the cross-batch variation history drawer. Additionally, the main Test Base variation row displayed a stale `UNKNOWN FAIL` status even though the latest chronological execution of that variation had passed successfully (`PASSED`).
- **Root Cause:**
  1. In `side-panel-step-list.html` and `side-panel-variation-history.html`, the MODE column was allocated 18% width with a 140px min-width. Coupled with large table cell padding, 19-character mode labels exceeded cell bounds and overlapped adjacent columns.
  2. In `AuraReportDataService.getTestBaseData()`, `curStatus` was read directly from `var.getLastStatus()` without reconciling against the newest chronological history link, leaving stale `failed-unknown` entity values untouched.
  3. In `RunStorageSyncService.syncTestRunsFromDisk()`, `varEntity.setLastStatus(effectiveStatus)` was set during run scanning in arbitrary directory iteration order (`Files.list(baseDir)`), allowing older runs scanned later to overwrite newer run statuses.
- **Detection Gap ("What did we miss?"):** Previous tests in `TestBaseVariationHistoryTest` only asserted single-run execution history and browser normalization, never verifying multi-run state transitions where an initial failed run is followed by a subsequent passing run.
- **Resolution:**
  1. Rebalanced drawer history table column widths in `side-panel-step-list.html` and `side-panel-variation-history.html`: RUN ID (20%, min-width 175px), BATCH NAME (11%, min-width 90px), MODE (27%, min-width 215px), TIMESTAMP (19%, min-width 155px), and STATUS (23%, min-width 185px) with table min-width `820px`.
  2. Refined `.tag-chip-mode` and table cell padding in `report-manager.css` (`padding: 0.65rem 0.65rem !important; font-size: 0.72rem; padding: 0.15rem 0.5rem;`) and `dashboard-styles.css`.
  3. Added `AuraReportDataService.sortHistoryLinksDescending` and updated `getTestBaseData()`, `syncVariationLastStatus()`, and `RunStorageSyncService.syncTestRunsFromDisk()` so that `effectiveLastStatus` and `isHealed` are derived directly from the newest chronological history link (`sortedLinks.get(0)`).
  4. Normalized `latestStatus` in `AuraReportDataService.getVariationHistory` to ensure consistent canonical status strings (`passed-clean`, `failed-unknown`, etc.) are persisted to `varEntity`.
- **Safety Net Added:** Added automated regression tests in `TestBaseVariationHistoryTest.java`: `testSortHistoryLinksDescending`, `testGetTestBaseDataUpdatesStaleStatusToLatestExecution`, and `testGetTestBaseDataDerivesHealedFromLatestExecution`.
