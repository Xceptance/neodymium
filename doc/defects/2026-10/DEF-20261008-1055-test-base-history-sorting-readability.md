# [DEF-20261008-1055] Unsorted Execution History and Mode Truncation in Test Base Variation Drawer

- **Status:** `Resolved`
- **Opened:** 2026-10-08 10:55
- **Closed:** 2026-10-08 11:00
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** Cross-batch test execution history in the Test Base variation drawer displays test executions in arbitrary order instead of chronological order by timestamp. Additionally, the MODE column is severely truncated (e.g. `LLM_REC`, `REPLAY_S`) and cells lack readability and breathing room.
- **Root Cause:** 
  1. `AuraReportDataService.getVariationHistory` assembled `historyList` by iterating over `varEntity.historyLinks` or fallback runs without sorting by timestamp, while `RunStorageSyncService` appended links in arrival order. Consequently, historical executions across different dates were displayed in arbitrary order.
  2. In `side-panel-step-list.html`, MODE was allocated only 8% width under `table-layout: fixed; width: 100%;`, and `report-manager.css` enforced `overflow: hidden; text-overflow: ellipsis; padding: 0.25rem 0.4rem;` across `.history-table`. This caused aggressive truncation of mode chips into `LLM_REC` and `REPLAY_S` and left virtually zero spacing between columns.
- **Detection Gap ("What did we miss?"):** Existing unit test `TestBaseVariationHistoryTest` only asserted single-execution retrievals or browser normalization, never verifying multi-execution timestamp sorting or table column overflow.
- **Resolution:** 
  1. Added `BY_TIMESTAMP_DESC` comparator to `TestBaseVariationHistoryDto` and sorted `historyList` descending by timestamp in `AuraReportDataService.getVariationHistory` across both fast and fallback retrieval paths.
  2. Maintained sorted chronological order when saving upgraded or synced `historyLinks` back to `TestBaseVariationEntity` in both `AuraReportDataService` and `RunStorageSyncService`.
  3. Rebalanced column percentages in `side-panel-step-list.html` and `side-panel-variation-history.html` (RUN ID: 25%, BATCH NAME: 15%, MODE: 18%, TIMESTAMP: 19%, STATUS: 23%) with `min-width: 760px` on the table to guarantee horizontal scrollability on narrow viewports.
  4. Updated `.history-table` styling in `report-manager.css` and `dashboard-styles.css` with comfortable padding (`0.5rem 0.65rem` / `8px 10px`), ensuring `.btn-fake-link` and `.tag-chip-mode` remain completely visible without truncation.
  5. Implemented `sortVariationHistoryTable(colIndex)` in `report-manager.js` to enable interactive header sorting across all columns.
- **Safety Net Added:** Unit tests `testGetVariationHistorySortedByTimestampDescending` and `testDtoTimestampComparatorWithIsoAndRecently` in `TestBaseVariationHistoryTest.java`.
