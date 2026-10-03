# [DEF-20260927-01] Missing Failure Status on Optional Steps During Live StateMachine Execution

- **Status:** Resolved
- **Opened:** 2026-09-27
- **Closed:** 2026-09-27
- **Component:** `neodymium-core` (`ai-engine` / `StateMachineRunner`)
- **Scope:** `Framework`
- **Symptom:** When a step tagged with `(optional)` fails during live execution, the failure is bypassed without throwing an exception, but `metrics.getSoftFailedStepCount()` reports 0 instead of 1, and the recorded step status is incorrectly saved as `SUCCESS`.
- **Root Cause:** In `StateMachineRunner.java`, the exception handler for `playbookStep.isOptional()` logged a warning and popped try-catch scopes, but omitted setting `playbookStep.setStatus(PlaybookStepStatus.FAILED)`, `playbookStep.setFailed(true)`, and `playbookStep.setFailureReason(...)`. As a result, the subsequent `ExecuteActionsStep` post-step hook evaluated `!step.isFailed()` as true and promoted the step status to `PlaybookStepStatus.SUCCESS`.
- **Detection Gap ("What did we miss?"):** Existing optional integration tests (`mock.OptionalIntegrationTest` and legacy `live.OptionalIntegrationTest`) only verified that execution completed without throwing unhandled exceptions; neither asserted on step statuses or asserted via `.verifyMetrics().hasSoftFailedStepCount(1)`.
- **Resolution:** Updated `StateMachineRunner.java` in the `playbookStep.isOptional()` branch to explicitly mark `playbookStep` and any active sub-steps/parent steps as `FAILED`, `setFailed(true)`, and record the failure cause before continuing execution.
- **Safety Net Added:** Modernized `live.OptionalIntegrationTest` asserting `.verifyMetrics().hasStepCount(2).hasSoftFailedStepCount(1)` on bypassed failures (with `.onLive(m -> m.hasLlmCalls())`, `.onStrictReplay(m -> m.hasNoLlmCalls())`, and `.onHealing(m -> m.hasLlmCalls())`) and `.hasNoSoftFailures()` on passing optional steps across `FORCE_RECORDING`, `REPLAY_STRICT`, and `REPLAY_WITH_HEALING`.
