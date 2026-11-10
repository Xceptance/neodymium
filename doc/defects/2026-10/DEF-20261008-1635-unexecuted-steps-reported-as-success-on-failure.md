# [DEF-20261008-1635] Unexecuted Steps Displayed as Success on Intermediate Test Failure

- **Status:** `Resolved`
- **Opened:** 2026-10-08 16:35
- **Closed:** 2026-10-08 16:40
- **Component:** `neodymium-core`, `aura-manager`
- **Scope:** `Framework`
- **Symptom:** When a test fails on an intermediate step (e.g., step 2 of 5 in `run_20261008_162427`), all unexecuted subsequent steps were rendered in the execution console and report side-panel with a green `SUCCESS` badge, old recorded durations, and non-zero action counts instead of being marked as `SKIPPED`.
- **Root Cause:** 
  1. `YamlPlaybookParser` deserializes companion recording JSONs with persisted `status: "SUCCESS"` and old `durationMs`/`startTimeMs` from prior runs without resetting them to `PlaybookStepStatus.PENDING` for new executions.
  2. `StateMachineRunner` only marked the active failed step as `FAILED` on test failure and did not update remaining unexecuted steps in `playbook.steps`/`playbook.flatSteps` to `PlaybookStepStatus.SKIPPED`.
  3. `InteractiveStateBuilder` did not enforce `SKIPPED` status for unexecuted steps when serializing `console-execution-*.json` on failed runs.
  4. `report-manager.js` lacked failure propagation across steps, causing existing and loaded runs to display green `SUCCESS` badges on unexecuted steps with `PENDING` context levels.
- **Detection Gap ("What did we miss?"):** Existing runner tests verified that `currentStep` was marked `FAILED`, but did not assert the status of subsequent steps in the playbook list when a failure terminates execution.
- **Resolution:** 
  1. Reset execution fields (`status` to `PENDING`, `failed` to `false`, `startTimeMs` and `durationMs` to `null`) when deserializing playbook steps from recording JSONs in `YamlPlaybookParser`.
  2. In `StateMachineRunner`, iterate through all remaining unexecuted steps on failure and transition them to `PlaybookStepStatus.SKIPPED`.
  3. In `InteractiveStateBuilder`, ensure steps following a failure or left `PENDING` in a failed run serialize with `status: "skipped"` and omit duration/timestamps.
  4. In `report-manager.js`, propagate step failure state so that unexecuted steps after a failure render as `SKIPPED` with `step-ignored` styling and clear duration/action counts, correctly displaying historical runs like `run_20261008_162427`.
- **Safety Net Added:** 
  - `org.neodymium.ai.runner.StateMachineRunnerTest#testUnexecutedStepsMarkedSkippedOnStepFailure`
  - `org.neodymium.ai.playbook.YamlPlaybookParserTest#testJsonRecordedStepsStatusResetToPendingOnParse`
  - `org.neodymium.ai.event.InteractiveConsoleListenerTest#testUnexecutedStepsSerializedAsSkippedOnRunFailure`
