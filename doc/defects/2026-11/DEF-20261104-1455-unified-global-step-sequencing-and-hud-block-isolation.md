# [DEF-20261104-1455] Unified Global Step Sequencing, Interactive HUD Block Isolation, and Live Playbook Recording

- **Status:** `Resolved`
- **Opened:** 2026-11-04 14:55
- **Closed:** 2026-11-04 15:06
- **Component:** `neodymium-core` (`ExecuteActionsStep.java`, `PlaybookRecorder.java`, `InteractiveStateBuilder.java`, `InteractiveConsoleListener.java`, `NeodymiumAiRunner.java`, `ExecutionContext.java`, `AiSession.java`)
- **Scope:** `Framework`
- **Symptom:**
  1. In `ExecuteActionsStep.java`, setup step #0 and main step #0 both received global step index `0`, causing setup steps to be overwritten in `TestExecutionReport` and `HtmlReportGenerator` step maps.
  2. When interactive console actions (add, delete, reorder) occurred during test execution pauses, `PlaybookRecorder` serialized a stale $t=0$ snapshot, silently dropping all user edits and additions from the recorded companion JSON file.
  3. Steps added interactively during execution were rendered at the bottom of the HUD instead of at the requested relative index.
  4. In `InteractiveConsoleListener`, inserting or reordering steps in `mainSteps` left `playbook.steps` out of sync.
- **Root Cause:**
  1. `ExecuteActionsStep.resolveStepIndex()` checked the block-relative list `playbook.steps` before checking `flatSteps`, assigning duplicate index `0` to steps in different blocks.
  2. `NeodymiumAiRunner` passed a static `allSteps` snapshot to `PlaybookRecorder` at test initialization time instead of querying the live executed step list upon `SessionFinishedEvent`.
  3. `InteractiveConsoleListener` mutated `mainSteps` without synchronizing `playbook.steps` when both lists existed in transient data.
- **Detection Gap ("What did we miss?"):**
  Existing unit tests verified step execution and report generation in isolation, but lacked end-to-end tests validating that interactive console step additions are faithfully preserved in `PlaybookRecorder` output while maintaining sequential non-colliding step indices across setup and main blocks.
- **Resolution:**
  1. In `ExecuteActionsStep.java`: updated `resolveStepIndex()` to check `flatSteps` first by identity, establishing `flatSteps` as the sole canonical global index authority across all lifecycle phases.
  2. In `PlaybookRecorder.java`: updated `onEvent(SessionFinishedEvent)` to query the active `ExecutionContext`'s `flatSteps` list (filtering for top-level steps), ensuring all live interactive edits and runtime additions are serialized into the companion JSON.
  3. In `InteractiveConsoleListener.java`: ensured `handleAddAction` and `handleReorderStep` synchronize both `mainSteps` and `playbook.steps` when both are present in transient context.
  4. In `NeodymiumAiRunner.java`: set `KEY_ACTIVE_PHASE` (`"before"`, `"steps"`, `"after"`) across `interceptBeforeEachMethod`, `interceptTestMethod`, and `interceptAfterEachMethod` so dynamic programmatic steps inherit their enclosing lifecycle block.
- **Safety Net Added:**
  - `PlaybookLifecycleExecutionTest.testHybridTestChronologicalOrderingInStateBuilder`: verifies chronological serialization of hybrid YAML and programmatic steps.
  - `PlaybookLifecycleExecutionTest.testInteractiveStateBuilderResolvesMainStepsWhenStepsEmptyAndFiltersDuplicates`: verifies setup step filtering and relative block indexing.
  - `InteractiveConsoleListenerTest`: 54 tests validating all interactive actions, rewind, heal, and state building.
  - `NeodymiumAiRunnerTest`: 30 tests validating runner lifecycle, before/after playbooks, and companion replay.
