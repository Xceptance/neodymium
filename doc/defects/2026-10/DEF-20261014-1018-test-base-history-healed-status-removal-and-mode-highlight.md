# [DEF-20261014-1018] Test Base History Drawer Conflated Healed Status and Unhighlighted Execution Modes

- **Status:** `Resolved`
- **Opened:** 2026-10-14 10:18
- **Closed:** 2026-10-14 10:23
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:**
  1. The Test Base cross-batch variation history drawer rendered a secondary "HEALED" badge in the `STATUS / BUGS` column, conflating execution mode with pure test health.
  2. The `MODE` column displayed unhighlighted raw mode strings (such as `REPLAY_WITH_HEALING`, `LLM_RECORDING`) inside a generic unstyled tag chip instead of highlighted badges matching the main Test Base table.
- **Root Cause:**
  1. `side-panel-step-list.html` included a separate healed badge span within the status cell and rendered mode inside a generic unstyled `.tag-chip.tag-chip-mode`.
  2. `TestBaseVariationHistoryDto` lacked a `getDisplayMode()` resolver, and `AuraReportDataService.getStatusBadgeInfo` returned "HEALED" label for legacy "healed" raw status.
- **Detection Gap ("What did we miss?"):**
  - Existing tests in `TestBaseVariationHistoryTest` only asserted boolean flags (`isHealed`, `isPassedHealed`) without asserting `displayMode` resolution or HTML template badge decoupling.
- **Resolution:**
  - Added `getDisplayMode()` to `TestBaseVariationHistoryDto` resolving `AI`, `HEALED`, `PLAYBOOK`, and `JAVA`.
  - Normalized legacy `"healed"` status to `"badge-pass"` and `"PASSED"` in `AuraReportDataService.getStatusBadgeInfo`.
  - Removed the `HEALED` badge from the status column and highlighted `MODE` badges with `badge-mode-ai`, `badge-mode-playbook`, `badge-mode-healed`, and `badge-mode-java` in `side-panel-step-list.html`.
- **Safety Net Added:**
  - Added `TestBaseVariationHistoryTest.testTestBaseVariationHistoryDtoDisplayModeResolution` asserting `displayMode` resolution across `AI`, `HEALED`, `PLAYBOOK`, and `JAVA`.
  - Added `TestBaseVariationHistoryTest.testGetStatusBadgeInfoNormalizesHealedToPassed` verifying legacy "healed" status maps to `badge-pass` and `PASSED`.
  - Updated existing healed history tests in `TestBaseVariationHistoryTest` asserting `dto.getDisplayMode()` returns `HEALED` and `dto.getStatusLabel()` returns pure `PASSED`.
