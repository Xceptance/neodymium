# [DEF-20261103-1540] Interactive Main Steps Preservation, Lifecycle Step Accumulation, and Managed Session Cleanup

- **Status:** `Resolved`
- **Opened:** 2026-11-03 15:40
- **Closed:** 2026-11-03 16:00
- **Component:** `neodymium-core` (`InteractiveConsoleListener.java`, `AiSession.java`, `NeodymiumAiRunner.java`)
- **Scope:** `Framework`
- **Symptom:**
  1. Adding or reordering steps via the interactive console during pause in a test with deferred main steps caused pending main test steps to be wiped or ignored, since `playbook.steps` was inspected and mutated while pending test steps were residing in `playbook.mainSteps`.
  2. Calling programmatic step execution (`session.execute(...)`) clobbered existing `playbook.beforeSteps` or `playbook.afterSteps` in the session execution context because inline playbooks provide empty before/after step lists that overwrote the outer test lifecycle steps.
  3. Calling `session.setManagedLifecycle(false)` after executing steps still resulted in `SessionFinishedEvent` being fired on `close()` because `close()` checked `hasExecuted` (via `KEY_MANAGED_START_TIME`), violating the expectation that disabling managed lifecycle prevents `close()` from emitting session-level completion events.
  4. Multiple `@BeforeEach` or `@AfterEach` methods annotated with `@AiPlaybook` overwrote previous steps in `playbook.beforeSteps` or `playbook.afterSteps` instead of appending them.
- **Root Cause:**
  1. `InteractiveConsoleListener` methods (`handleAddStep`, `handleReorderStep`, `findStepByIndex`, `resolveStepBlock`, `resolveStepIndex`, `updateFlatSteps`) directly read and wrote `playbook.steps` without checking `playbook.mainSteps`, which holds deferred main steps prior to `runMainTestPlaybook()`.
  2. `AiSession.execute` unconditionally replaced `playbook.beforeSteps` and `playbook.afterSteps` with `new ArrayList<>(beforeSteps)` and `new ArrayList<>(afterSteps)` from the inline playbook, which are empty collections for programmatic calls.
  3. `AiSession.setManagedLifecycle(false)` did not remove `KEY_MANAGED_START_TIME`, and `AiSession.close()` checked `startTime != null` instead of strictly checking whether `isManagedLifecycle` is currently active.
  4. `NeodymiumAiRunner.runPlaybookForMethod` assigned `playbook.beforeSteps` and `playbook.afterSteps` directly using `put("playbook.beforeSteps", steps)` rather than appending to existing steps if already present.
- **Detection Gap ("What did we miss?"):**
  - Previous unit tests for `InteractiveConsoleListener` only tested adding/reordering steps when `playbook.steps` was already populated. No test verified interactive pause behavior when main steps were deferred in `playbook.mainSteps`.
  - Previous tests for `session.execute(...)` did not check if existing runner lifecycle steps were preserved across programmatic executions.
  - Previous tests for `setManagedLifecycle` tested setting it to `true` and closing, but never tested turning it off after execution to ensure `close()` does not emit `SessionFinishedEvent`.
  - Tests for `@AiPlaybook` on lifecycle methods only tested a single annotated `@BeforeEach` method rather than multiple annotated lifecycle methods on a single test class.
- **Resolution:**
  1. In `InteractiveConsoleListener.java`: updated step lookup, addition, reordering, block resolution, and flat steps regeneration to check and mutate `playbook.mainSteps` when present in context transient data.
  2. In `AiSession.java`: guarded `playbook.beforeSteps` and `playbook.afterSteps` updates so empty collections do not clobber existing lifecycle steps.
  3. In `AiSession.java`: updated `setManagedLifecycle(false)` to remove `KEY_MANAGED_START_TIME`, and changed `close()` to check `isManaged` directly.
  4. In `NeodymiumAiRunner.java`: updated `runPlaybookForMethod` to accumulate `beforeSteps` and `afterSteps` across multiple annotated lifecycle methods via `existing.addAll(steps)`.
- **Safety Net Added:**
  - `InteractiveConsoleListenerTest.testInteractiveAddStepPreservesPendingMainSteps`: asserts that adding a step interactively preserves pending main steps in `playbook.mainSteps`.
  - `AiSessionTest.testProgrammaticStepExecutionPreservesExistingBeforeAndAfterSteps`: asserts that programmatic `session.execute` preserves pre-existing before and after steps.
  - `AiSessionTest.testDisablingManagedLifecycleDoesNotEmitFinishedEventOnClose`: asserts that disabling managed lifecycle prevents `SessionFinishedEvent` emission during `session.close()`.
  - `NeodymiumAiRunnerTest.testMultipleBeforeEachPlaybookAnnotationsAccumulateSteps`: asserts that multiple `@BeforeEach` methods with `@AiPlaybook` accumulate steps in sequence.
