# [DEF-20261120-1100] Active Run Context Progress Bar Leaves Unfilled Section for AI-Driven Executions

- **Status:** `Resolved`
- **Opened:** 2026-11-20 11:00
- **Closed:** 2026-11-20 11:05
- **Component:** `aura-manager` / `neodymium-core`
- **Scope:** `Framework`
- **Linked Ticket:** GitHub Issue [#568](https://github.com/Xceptance/neodymium/issues/568)
- **Symptom:** When a test run finishes containing AI-driven test executions (`executionMode: AI` or `llmCalls > 0`), the progress bar track on the Active Run Context sidebar card (`#sidebarContextCard`) has an unfilled (grey) section (e.g. only 50% filled when 2 out of 2 tests passed), and the passed badge chip displays fewer passed tests than were actually executed and passed.
- **Root Cause:**
  1. In `TestRunEntity.java`, `getPassedCleanCount()` is defined as `Math.max(0, pass - getPassedHealedCountSafe() - getPassedAiCountSafe())`.
  2. When computing `passedCount` for the active run context and the `/api/status` endpoint in `AuraGlobalModelAdvice.java` (lines 380, 440) and `AuraTestQueueController.java` (lines 1832, 1903), the calculation manually summed breakdown categories:
     `activeEntity.getPassedCleanCount() + activeEntity.getFixedCleanCount() + activeEntity.getPassedHealedCountSafe() + activeEntity.getFixedHealedCountSafe()`.
     This computation completely omitted `getPassedAiCountSafe()` and `getFixedAiCountSafe()`.
  3. Because `passedAiCount` was subtracted when computing `getPassedCleanCount()`, omitting it from the sum resulted in `passedCount` dropping all AI-driven passed tests. For a 2-test run with 1 AI pass and 1 clean pass, `passedCount` evaluated to 1 instead of 2.
  4. In `AuraQueueService.java:1407`, real-time batch execution ingestion checked for status strings `passed-clean`, `passed-healed`, `fixed-clean`, `fixed-healed`, but omitted `succeeded-fixed`, `passed-ai`, and `fixed-ai`.
- **Detection Gap ("What did we miss?"):**
  Existing unit tests in `AuraGlobalModelAdviceTest` and `AuraTestQueueControllerTest` set `passedCount` on mock `TestRunEntity` instances without setting `passedAiCount`, which caused `getPassedCleanCount()` to equal the full pass count and masked the omission of the AI category.
- **Resolution:**
  1. Updated `AuraGlobalModelAdvice.java` (lines 380, 440) and `AuraTestQueueController.java` (lines 1832, 1903) to comprehensively compute `passedCount` including clean, healed, fixed, and AI-driven executions via:
     ```java
     final int activePass = (activeEntity.getPassedCount() != null)
         ? activeEntity.getPassedCount()
         : (activeEntity.getPassedCleanCount() + activeEntity.getPassedHealedCountSafe() + activeEntity.getPassedAiCountSafe());
     final int activeFixed = (activeEntity.getSucceededFixedCount() != null)
         ? activeEntity.getSucceededFixedCount()
         : (activeEntity.getFixedCleanCount() + activeEntity.getFixedHealedCountSafe() + activeEntity.getFixedAiCountSafe());
     passedCount = activePass + activeFixed;
     ```
  2. Updated `AuraQueueService.java:1407` to recognize `passed-ai`, `fixed-ai`, and `succeeded-fixed` statuses during real-time batch execution ingestion.
- **Safety Net Added:**
  - `AuraGlobalModelAdviceTest.testActiveRunContextIncludesAiDrivenPassedCount`: verifies that an active/completed run containing AI passes correctly populates `activeRunPassedCount = 2`, `activeRunScheduledCount = 2`, and `activeRunExecutedCount = 2`.
  - `AuraTestQueueControllerTest.testGetStatusStreamIncludesAiDrivenPassedCount`: verifies that `/api/status` returns `passed = 2` when AI passes are present.
