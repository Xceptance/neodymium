# [DEF-20261014-0943] Inconsistent Test Base Column Alignment and Mixed Status/Mode Concerns

- **Status:** `Resolved`
- **Opened:** 2026-10-14 09:34
- **Closed:** 2026-10-14 09:48
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:**
  1. Variation tables in separate test class cards shifted columns horizontally based on text length of dataset/variation names, creating a jagged, unaligned visual experience across test classes.
  2. "HEALED" was displayed as a status or alongside status badges in the status column, obscuring whether the test passed or failed.
  3. Execution mode (AI, Playbook, Healed, Java) was not visible as a dedicated table dimension in the Test Base.
  4. Execution counts were cluttered inside the status column rather than beside the variation name.
  5. Visible table borders cluttered the variation listing within class cards.
- **Root Cause:**
  1. Lack of `table-layout: fixed` and uniform `<colgroup>` across class variation tables in `test-base.html`.
  2. `AuraReportDataService` overriding `lastStatus` to `"healed"` when `isHealed` was true, conflating execution mode with execution status.
  3. Lack of a dedicated `executionMode` property and resolution method on `TestBaseVariationEntity`.
  4. Disk synchronization in `RunStorageSyncService` previously set `executionMode` within an unordered scan loop, potentially overwriting latest execution mode with older executions.
- **Detection Gap ("What did we miss?"):**
  - Existing unit tests in `TestBaseVariationHistoryTest` specifically asserted `Assertions.assertEquals("healed", updatedVar.getLastStatus())`, reinforcing the conflation of healed state with status.
  - No automated tests asserted the presence of a dedicated execution mode column, fixed layout constraints, or chronological mode resolution from multiple historical executions.
- **Resolution:**
  - Added `executionMode` and `getDisplayMode()` (`AI`, `HEALED`, `PLAYBOOK`, `JAVA`) to `TestBaseVariationEntity`.
  - Updated `AuraReportDataService` and `RunStorageSyncService` to decouple healing from `lastStatus`, preserving pure status values (`passed-clean`, `failed-unknown`, `failed-known`, `succeeded-fixed`, `skipped`) and propagating mode.
  - Ensured both `RunStorageSyncService` and `AuraReportDataService` strictly derive execution mode from the latest chronological execution (`sortedLinks.get(0)`), falling back to reading the run JSON for legacy links missing `&mode=`.
  - Redesigned `test-base.html` with fixed `<colgroup>` (`38%`, `12%`, `18%`, `14%`, `18%`), moved run counts next to the variation name, added a dedicated `Execution Mode` column, and isolated pure status badges in `Last Status`.
  - Added CSS rules in `report-manager.css` for `#pageTestBase table.aura-table` enforcing `table-layout: fixed` and making table borders invisible (`border: none !important; border-bottom: none !important;`).
- **Safety Net Added:**
  - Updated `TestBaseVariationHistoryTest.testGetTestBaseDataDerivesHealedFromLatestExecution` asserting decoupled `passed-clean` status and `HEALED` displayMode.
  - Added `TestBaseVariationStatusSyncTest.testDisplayModeResolutionAndDecoupledStatus` verifying execution mode resolution across `AI`, `HEALED`, `PLAYBOOK`, and `JAVA` alongside pure statuses.
  - Added `TestBaseVariationHistoryTest.testGetTestBaseDataStrictlyResolvesLatestExecutionMode` testing unordered multi-run histories to assert strict chronological latest mode precedence.
