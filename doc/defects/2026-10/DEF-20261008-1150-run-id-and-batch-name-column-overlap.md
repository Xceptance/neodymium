# [DEF-20261008-1150] Run ID and Batch Name Column Overlap in Variation History Drawer

- **Status:** `Resolved`
- **Opened:** 2026-10-08 11:50
- **Closed:** 2026-10-08 11:55
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In the Test Base execution history drawer, the 21-character RUN ID label (e.g. `run_20261007_155956`) overflows into the BATCH NAME column, rendering text directly on top of `Unknown`. Simultaneously, the `"BATCH NAME"` header and sort icon collide with the `"MODE"` header.
- **Root Cause:** In `side-panel-step-list.html` and `side-panel-variation-history.html`, RUN ID was underallocated at 20% (~164px) while full run IDs plus icon require ~220px. BATCH NAME was underallocated at 11% (~90px), which is narrower than the header string `"BATCH NAME"` and sort icon (115px). Excess space was concentrated in MODE (27%).
- **Detection Gap ("What did we miss?"):** Visual table geometry on live run IDs (21 characters) was tested with earlier shorter run ID mock names in synthetic tests, missing cell overflow under `table-layout: fixed`.
- **Resolution:** Rebalanced drawer table column widths to RUN ID (28%, min-width: 225px), BATCH NAME (16%, min-width: 125px), MODE (20%, min-width: 165px), TIMESTAMP (17%, min-width: 150px), and STATUS (19%, min-width: 170px) with `min-width: 850px` on `.history-table`, and added CSS cell containment guardrails (`overflow: hidden; text-overflow: ellipsis;`).
- **Safety Net Added:** Verified via visual layout inspection and regression test suite execution in `TestBaseVariationHistoryTest.java`.
