# [DEF-20261012-2048] resetExecutionState Erases Recorded Status Breaking 0-Action Steps in REPLAY_STRICT

- **Status:** `Resolved`
- **Opened:** 2026-10-12 20:48
- **Closed:** 2026-10-12 21:13
- **Component:** `neodymium-core` (`ai-playbook` / `playbook-replay`)
- **Scope:** `Framework`
- **Symptom:** File-based playbooks containing recorded 0-action steps (such as conditional instructions where conditions evaluate to false, or assertions completed without browser interactions) pass in `FORCE_RECORDING` but fail in `REPLAY_STRICT` with: `ConclusiveFailureException: No recorded tool calls found for step '...' in REPLAY_STRICT mode. Companion JSON recording file is missing or step was not recorded.` (observed in `AutoTranslateCheckoutIntegrationTest#testCheckoutReplayGerman` and `testCheckoutReplayGermanWithJudge`).
- **Root Cause:** In `YamlPlaybookParser.java`, `resetExecutionState` unconditionally reset all deserialized steps from companion recording JSON files to `PlaybookStepStatus.PENDING` (introduced in DEF-20261008-1635 to clear stale statuses on abort). This wiped the recorded `SUCCESS` status, causing `ExecuteActionsStep.java`'s `isRecordedCompletedStep` check (`initialStepStatus != null && initialStepStatus != PlaybookStepStatus.PENDING`) to evaluate to `false` when replaying file-based companion recordings.
- **Detection Gap ("What did we miss?"):** `ExecuteActionsStepTest#testReplayStrictSucceedsWhenRecordedStepCompletedWithZeroActions` tested 0-action replay using manually constructed `PlaybookStep` instances with `status = SUCCESS`, bypassing `YamlPlaybookParser`. `YamlPlaybookParserTest#testJsonRecordedStepsStatusResetToPendingOnParse` asserted `status == PENDING` after parsing without verifying that the recorded completion state remained available for replay guards.
- **Resolution:** 
  1. Added `recordedStatus` and `recordedFailureReason` properties to `PlaybookStep.java` with getters and setters.
  2. In `YamlPlaybookParser.java#resetExecutionState`, preserved `step.getStatus()` into `step.setRecordedStatus(...)` and `failureReason` into `recordedFailureReason` before resetting `status` to `PENDING` for new execution runs.
  3. In `AiSession.java`, also copied `recordedStatus` and `recordedFailureReason` during programmatic step merging.
  4. In `ExecuteActionsStep.java`, updated `isRecordedCompletedStep`, `hasRecordedCalls`, `hasRecordedContent`, and failed step evaluation to check `recordedStatus` alongside `initialStepStatus`.
- **Safety Net Added:** 
  - `org.neodymium.ai.playbook.YamlPlaybookParserTest#testJsonRecordedStepsStatusResetToPendingOnParse` (asserting `s1.getRecordedStatus() == SUCCESS` and `s2.getRecordedStatus() == HEALED`)
  - `org.neodymium.ai.playbook.YamlPlaybookParserTest#testJsonRecordedStepsPreserveRecordedFailureReasonAcrossReset`
  - `org.neodymium.ai.pipeline.steps.ExecuteActionsStepTest#testReplayStrictSucceedsWhenStatusIsPendingWithRecordedStatusSuccess`
