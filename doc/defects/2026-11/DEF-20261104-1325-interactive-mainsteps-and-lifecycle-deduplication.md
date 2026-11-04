# [DEF-20261104-1325] Interactive Main Steps Preservation, Lifecycle Deduplication, and Symmetrical Queue Management

- **Status:** `Resolved`
- **Opened:** 2026-11-04 13:05
- **Closed:** 2026-11-04 13:25
- **Component:** `neodymium-core` (`NeodymiumAiRunner.java`, `InteractiveConsoleListener.java`, `InteractiveStateBuilder.java`, `AiSession.java`)
- **Scope:** `Framework`
- **Symptom:**
  1. During interactive console pauses while a test was actively executing, issuing an `ADD`, `DELETE`, or `REORDER` action on the `steps` block mutated setup steps instead of the intended test steps due to an off-by-$N$ index shift.
  2. Every `@BeforeEach` and `@AfterEach` method annotated with `@AiPlaybook` had its steps parsed and registered twice into `playbook.beforeSteps` and `playbook.afterSteps` in real test runs.
  3. `InteractiveConsoleListener.updateFlatSteps()` duplicated before-steps into `flatSteps`, corrupting global progress percentage calculations and rewind target offsets.
  4. In programmatic tests (no YAML on `@Test`), `containsKey("playbook.mainSteps")` routed user console actions to an empty list rather than `playbook.steps`, causing `IndexOutOfBoundsException` or silent failures.
  5. In hybrid tests executing dynamic programmatic steps alongside a YAML playbook, inline steps executed after a YAML step were prepended ahead of that YAML step in the serialized state JSON.
  6. Calling programmatic `session.execute(...)` threw `UnsupportedOperationException` if pre-configured `playbook.beforeSteps` or `playbook.afterSteps` held unmodifiable collections.
- **Root Cause:**
  1. `NeodymiumAiRunner.runMainTestPlaybook()` removed `"playbook.mainSteps"`, destroying the reference list and forcing `InteractiveConsoleListener` to fall back to `"playbook.steps"` containing accumulated setup steps.
  2. `NeodymiumAiRunner.postProcessTestInstance()` pre-parsed all `@BeforeEach` and `@AfterEach` methods into `beforeFlat` and `afterFlat`, while `runPlaybookForMethod()` also parsed and appended them when JUnit invoked the methods. Because `PlaybookStep` does not override `equals()` or `hashCode()`, instance-based checks could not detect duplicates.
  3. `updateFlatSteps()` flattened `beforeSteps`, `mainSteps`, `playbookSteps`, and `afterSteps` without identity filtering, adding the same step instances multiple times.
  4. `InteractiveConsoleListener` checked `containsKey("playbook.mainSteps")` rather than verifying `mainSteps != null && !mainSteps.isEmpty()`.
  5. `InteractiveStateBuilder` assumed any non-main step in `sessionSteps` was an upfront setup step and unconditionally prepended it ahead of `mainSteps`.
  6. `AiSession.execute()` called `existingBefore.addAll(beforeSteps)` and `existingAfter.addAll(afterSteps)` without defensive `try ... catch (UnsupportedOperationException)` wrapping.
- **Detection Gap ("What did we miss?"):**
  Existing unit tests only validated setup pauses before test steps ran, wrote test YAML files to mock resource managers *after* `postProcessTestInstance` had already silently failed, and lacked assertions for hybrid step execution order or identity-based step uniqueness.
- **Resolution:**
  1. In `NeodymiumAiRunner.java`: removed eager pre-parsing of `@BeforeEach` and `@AfterEach` methods (lines 1416–1445). Introduced symmetrical `"playbook.mainStepsToRun"` queue consumed by `runMainTestPlaybook()` while keeping `"playbook.mainSteps"` intact throughout the test lifecycle.
  2. In `InteractiveConsoleListener.java`: updated all step action handlers, index lookups, block resolutions, and rewind boundaries to check `(mainSteps != null && !mainSteps.isEmpty()) ? "playbook.mainSteps" : "playbook.steps"`. Added symmetrical sync for `"playbook.mainStepsToRun"` when mutating steps during setup pause. Rebuilt `updateFlatSteps()` using `Collections.newSetFromMap(new IdentityHashMap<>())` to guarantee 0 duplicate steps while preserving dynamic inline steps.
  3. In `InteractiveStateBuilder.java`: merged `sessionSteps` followed by `mainSteps` using an identity set to preserve exact chronological execution order across setup, YAML, and inline steps.
  4. In `AiSession.java`: wrapped `existingBefore.addAll` and `existingAfter.addAll` with defensive `UnsupportedOperationException` handling, and added a diagnostic warning when multiple `execute()` calls occur on unmanaged sessions.
- **Safety Net Added:**
  - `NeodymiumAiRunnerTest.testLifecyclePlaybookAnnotationsDoNotDuplicateStepsWhenFilesExistUpfront`: asserts that `@AiPlaybook` on `@BeforeEach` methods with pre-existing files registers steps exactly once in `playbook.beforeSteps`.
  - `InteractiveConsoleListenerTest.testInteractiveActionsDuringTestExecutionTargetMainSteps`: asserts that interactive additions during test execution pause target `playbook.mainSteps` with the exact relative index despite existing setup steps.
  - `InteractiveConsoleListenerTest.testInteractiveActionsInProgrammaticTestTargetPlaybookSteps`: asserts that interactive actions in programmatic tests manipulate `playbook.steps`.
  - `InteractiveConsoleListenerTest.testUpdateFlatStepsEliminatesDuplicatesWhileRetainingInlineSteps`: asserts identity deduplication in `flatSteps` across setup, main, and dynamic inline steps.
  - `PlaybookLifecycleExecutionTest.testHybridTestChronologicalOrderingInStateBuilder`: asserts that inline steps executed after a YAML step appear strictly after that YAML step in the serialized state JSON.
