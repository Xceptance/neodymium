# [DEF-20261004-1718] Offline Replay Vector Healing Status Clobbered to SUCCESS and Selector Intel Omitted in Reports

- **Status:** `Resolved`
- **Opened:** 2026-10-04 16:58
- **Closed:** 2026-10-04 17:28
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** When a test passed using offline vector/attribute self-healing (such as in `VectorHealingSandboxLiveTest`), the HTML report displayed `✨ Healed: 0`, the step badge showed green `SUCCESS` instead of purple `HEALED`, and the action inspector table displayed only the dynamically healed selector with zero indication that healing occurred or what the original recorded selector was.
- **Root Cause:** 
  1. In `ExecuteActionsStep`, the pipeline step end-hook evaluated `Boolean.TRUE.equals(c.getTransientData().remove(ExecutionContext.KEY_IS_HEALED_STEP))`. Because offline replay healing in `PlaybookToolReplayer` marked `step.setStatus(PlaybookStepStatus.HEALED)` directly without setting `KEY_IS_HEALED_STEP` in transient data (which was previously only populated during online LLM retry healing), the `else` branch ran and unconditionally overwrote the step status to `PlaybookStepStatus.SUCCESS`.
  2. In `PlaybookToolReplayer.replayStep`, an `else` branch after the tool loop set `step.setStatus(PlaybookStepStatus.SUCCESS)` whenever `anyHealed` was false, which would clobber steps whose healing status was set without `anyHealed` being flagged.
  3. `ExecuteActionsStep` subsequently dispatched `StepFinishedEvent` with status `SUCCESS`, causing `MetricsCollector` and `PreliminaryReportListener` to record 0 healed steps.
  4. `PlaybookToolReplayer` dispatched `ActionExecutedEvent` containing only the runtime healed tool call without the canonical recorded tool call and without flagging the action event as healed.
  5. `HtmlReportGenerator` lacked UI markers for self-healed action entries and did not differentiate locator healing transitions from parameter templates.
- **Detection Gap ("What did we miss?"):** Existing unit tests in `ExecuteActionsStepTest` tested healing by manually injecting `KEY_IS_HEALED_STEP` into context transient data (modeling online LLM retry healing), but never verified offline replay healing where `PlaybookToolReplayer` assigns `step.setStatus(PlaybookStepStatus.HEALED)` without the transient flag.
- **Resolution:**
  1. Updated `ExecuteActionsStep` to preserve `PlaybookStepStatus.HEALED` in the end-hook if `step.getStatus() == PlaybookStepStatus.HEALED || Boolean.TRUE.equals(isHealed)`.
  2. Updated `PlaybookToolReplayer` to preserve `PlaybookStepStatus.HEALED` in `replayStep`, dispatch `ActionExecutedEvent` with both the canonical recorded action and resolved healed action, with `healed = true`, and set `KEY_IS_HEALED_STEP` on session execution context.
  3. Added `healed` property and constructors to `ActionExecutedEvent`, `healed` field and getter/setter to `TestExecutionReport.ReportActionEntry`, and `isHealed()` method to `ReportStepEntry`.
  4. Updated `PreliminaryReportListener` to populate `ReportActionEntry.setHealed(actionExecuted.isHealed())`.
  5. Updated `HtmlReportGenerator` to style healed actions with `.status-heal`, display `🧬 Healed from: <code><original-selector></code>` in the target column, and render purple `HEALED` status pills.
- **Safety Net Added:** 
  - `ExecuteActionsStepTest.testOfflineReplayHealedStepPreservedThroughPipelineAndDispatchesHealedEvent`
  - `PreliminaryReportListenerTest.testHealedActionEventWithCanonicalAndResolvedSelectors`
  - `PlaybookToolReplayTest.testOfflineReplaySimilarityHealingWithDomFeatureVector`

