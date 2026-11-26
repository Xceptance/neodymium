# [DEF-20261110-1538] Downstream Unreached StepFinishedEvent Clobbers Earlier Completed Steps

- **Status:** `Resolved`
- **Opened:** 2026-11-10 15:12
- **Closed:** 2026-11-10 15:38
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:**
  In playbook runs featuring multiple inclusions of identical sub-step fragments (e.g., `tests/stokke/dings.yaml` executing `search-for-product-and-add-to-cart.steps` twice), when a failure occurred in the second inclusion (e.g. step #14), earlier executed and successfully completed steps from the first inclusion (e.g. steps #7–#12) were retroactively rewritten in reports and interactive console execution artifacts (`console-execution-1.json`) to status `NOT_RUN` with reason `"Not executed because step #14 failed."`.
- **Root Cause:**
  When a step fails, `StateMachineRunner.markRemainingStepsAsSkipped()` iterates through all unexecuted downstream steps and dispatches `StepFinishedEvent(step, PlaybookStepStatus.NOT_RUN)`.
  In `PreliminaryReportListener.onEvent(StepFinishedEvent)`:
  1. Unexecuted downstream steps have never started and thus never had a report entry created via `StepStartedEvent`.
  2. The fallback lookup in `PreliminaryReportListener` searched `this.report.getSteps()` backwards solely by comparing natural language instruction strings (`pbInstr` / `pbRaw`).
  3. Because an identical fragment had executed and succeeded earlier in the run, the fallback lookup matched the earlier completed step entries.
  4. The listener had no check to ensure target entries were not already completed, so it matched completed entries and unconditionally updated their status to `NOT_RUN`, overwriting their `SUCCESS` status and assigning the downstream skip reason.
- **Detection Gap ("What did we miss?"):**
  Previous tests for `StepFinishedEvent` and skip propagation tested unique step names or single-include playbooks. No regression test covered a multi-include scenario where an unexecuted downstream step share identical instruction strings with an already completed upstream step.
- **Resolution:**
  1. In `PreliminaryReportListener.java`:
     - Added `isTerminalCompleted(final String status)` helper that recognizes `SUCCESS`, `PASSED`, `FAILED`, `HEALED`, `SKIPPED`, and `NOT_RUN` / `NOT RUN`.
     - Guarded `this.currentStep` matching so that steps already in a terminal state are not erroneously selected as `targetStep` by downstream events with identical instructions.
     - Guarded backward report entry lookups in `onEvent(StepFinishedEvent)` so they ignore entries that are already in a terminal completed state.
     - Added an unconditional early exit guard before modifying `targetStep`: if `targetStep.getStatus()` is already terminal completed, return immediately without modifying the entry.
- **Safety Net Added:**
  - Added `@Test` `PreliminaryReportListenerTest.testDownstreamUnreachedStepFinishedEventDoesNotClobberEarlierCompletedIdenticalSteps`: sets up multiple identically worded steps across two execution phases, dispatches a failure on phase 2 step 4 followed by unreached `NOT_RUN` `StepFinishedEvent`s on downstream steps (both matching earlier completed steps and matching the failing step itself), and asserts that all completed step statuses and reasons are preserved.
  - Added `@Test` `StateMachineRunnerTest.testMarkRemainingStepsAsSkippedPreservesCompletedSuccessfulSteps`: asserts that when a step fails mid-run, earlier completed steps retain `SUCCESS`, keep their recorded execution duration, and have no `notRunReason`.
