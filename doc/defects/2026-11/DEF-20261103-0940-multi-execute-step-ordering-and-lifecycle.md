# [DEF-20261103-0940] Multi-Execute Lifecycle Deferral, Chronological Step Ordering, and Teardown Error Handling

- **Status:** `Resolved`
- **Opened:** 2026-11-03 09:40
- **Closed:** 2026-11-03 10:00
- **Component:** `neodymium-core` (`AiSession.java`, `NeodymiumAiRunner.java`, `StateMachineRunner.java`, `InteractiveStateBuilder.java`)
- **Scope:** `Framework`
- **Symptom:**
  1. Multiple calls to `session.execute(...)` within `@BeforeEach`, within a `@Test` method, or within `@AfterEach` either prematurely finalized the test session (`SessionFinishedEvent` fired during setup or mid-test), clobbered the cumulative step collection, or stripped runner lifecycle deferral flags.
  2. In hybrid tests combining programmatic `@BeforeEach` or `@AfterEach` steps with YAML playbooks in `@Test`, `@BeforeEach` steps were appended *after* the `@Test` steps in `playbook.flatSteps`. When a `@Test` step failed, `StateMachineRunner.logFinalStatsSummary` marked all subsequent steps in `flatSteps` as `SKIPPED`, erroneously mutating already-succeeded `@BeforeEach` steps to `SKIPPED`.
  3. When an exception was thrown and caught inside user test code (e.g. `try { session.execute(...) } catch (...) { ... }`), `ExecutionContext.KEY_LAST_EXECUTION_ERROR` persisted across subsequent successful step executions, causing `finalizeManagedSession` to mark a passing test as `FAILED`.
  4. Exceptions thrown during deferred after-playbook teardown execution in `NeodymiumAiRunner.afterEach()` were logged but not rethrown, preventing JUnit from reporting teardown failures.
  5. In interactive console runs, initializing `playbook.steps` as empty list caused `InteractiveStateBuilder` to render 0 pending test steps during setup pauses, and subsequent step accumulation duplicated `before` and `after` steps into `blocks.steps`.
- **Root Cause:**
  1. `NeodymiumAiRunner.executeBeforeEach` eagerly populated `"playbook.steps"` and `"playbook.flatSteps"` with the `@Test` YAML steps before `@BeforeEach` ran. Any programmatic `session.execute` calls in `@BeforeEach` appended to the list, placing them chronologically after the test steps.
  2. `AiSession.execute` unconditionally removed `"playbook.deferSessionFinished"` in its `finally` block, stripping outer deferral state set by `NeodymiumAiRunner` across multi-execute blocks.
  3. `AiSession.execute` did not clear `KEY_LAST_EXECUTION_ERROR` upon successful step completion, causing residual caught errors to poison the final session status evaluation in `finalizeManagedSession`.
  4. `StateMachineRunner.run()` only checked `playbook.deferSessionFinished` and ignored `playbook.managedLifecycle`, firing premature completion in standalone managed sessions.
  5. `NeodymiumAiRunner.afterEach` caught `runAfterPlaybook()` errors without rethrowing them after session finalization.
  6. `InteractiveStateBuilder` only checked `playbook.steps` and had no fallback to `playbook.mainSteps`, and did not filter out block-scoped `before`/`after` steps from the main steps array.
- **Detection Gap ("What did we miss?"):**
  Previous unit tests only validated single programmatic executions or single YAML playbooks per test lifecycle. No test exercised multiple programmatic executions in `@BeforeEach`, multiple executions in `@AfterEach`, hybrid multi-execute sequencing with error recovery, unmanaged sessions closed without execute calls, report failureReason retention upon test body failure, or `InteractiveStateBuilder` state generation with deferred main steps.
- **Resolution:**
  1. In `StateMachineRunner.java`: ensured `SessionFinishedEvent` is deferred if `playbook.deferSessionFinished` OR `playbook.managedLifecycle` is `true`. Also added unrun `playbook.mainSteps` to `targetSteps` in `markRemainingStepsAsSkipped` with HashSet lookup to avoid O(N²) quadratic overhead.
  2. In `AiSession.java`: preserved outer `playbook.deferSessionFinished` flags, cleared `KEY_LAST_EXECUTION_ERROR` on successful execution, provided `setManagedLifecycle(boolean)` and overloaded `finishSession(Throwable)`, implemented `close()` as a template method invoking `finishSession()` only when managed or executed inside `try ... finally { doClose(); }`, and provided a default non-abstract implementation for `doClose()` to safeguard backward compatibility for external subclasses.
  3. In `NeodymiumAiRunner.java`: initialized `"playbook.steps"` and `"playbook.flatSteps"` as empty lists in `executeBeforeEach`, appending executed steps chronologically at execution time via `appendExecutedSteps` in `runPlaybookForMethod`, `runBeforePlaybook`, `runMainTestPlaybook`, and `runAfterPlaybook`.
  4. In `NeodymiumAiRunner.afterEach`: tracked `afterPlaybookError` to ensure teardown failures fail the test and inform video recording and final stats, while ensuring caught test exceptions do not falsely mark passing tests as failed. When both test body and teardown fail, `afterPlaybookError` is attached as a suppressed exception on the primary failure. Preserved `KEY_LAST_EXECUTION_ERROR` when `context.getExecutionException()` is present to ensure `PreliminaryReportListener` retains test failure reasons and stack traces. Wrapped `session.close()` in afterEach to suppress close errors rather than swallowing teardown failures.
  5. In `InteractiveStateBuilder.java`: resolved `playbook.mainSteps` when `playbook.steps` is empty, merged executed setup steps ahead of pending `mainSteps` when both exist during setup pauses using Sets to avoid O(N²) complexity, and filtered out `before` and `after` block steps from `blocks.steps` to prevent duplicate step rendering. Added defensive null check for step entries in `stepsToSerialize`.
  6. In `AiSession.java`: added `clearLastExecutionError()` to allow standalone managed session users who catch and assert failures in test code to clear error state before `close()`.
  7. In `NeodymiumAiRunner.java`: added defensive check `!(currentSteps instanceof ArrayList)` in `appendExecutedSteps` to gracefully handle unmodifiable collections stored under `playbook.steps`.
- **Safety Net Added:**
  - `AiSessionTest.testProgrammaticMultiExecuteAccumulatesStepsAcrossCalls`: verifies step accumulation across multiple calls.
  - `NeodymiumAiRunnerTest.testBeforeEachProgrammaticAndTestMethodProgrammaticBothReported`: verifies both `@BeforeEach` and `@Test` programmatic steps are reported in `PreliminaryReportListener` without premature completion.
  - `NeodymiumAiRunnerTest.testBeforeEachProgrammaticAndTestMethodYamlPlaybookBothReported`: verifies both `@BeforeEach` programmatic steps and test method YAML playbook steps are reported without duplication or premature completion.
  - `NeodymiumAiRunnerTest.testMultipleProgrammaticExecuteCallsInTestMethodBothReported`: verifies multiple sequential `session.execute(...)` calls within a test method all accumulate and report cleanly.
  - `NeodymiumAiRunnerTest.testMultipleProgrammaticExecuteCallsInBeforeEachBothReported`: verifies multiple sequential calls in `@BeforeEach` accumulate ahead of test method steps.
  - `NeodymiumAiRunnerTest.testMultipleProgrammaticExecuteCallsInAfterEachBothReported`: verifies multiple sequential calls in `@AfterEach` accumulate following test method steps.
  - `NeodymiumAiRunnerTest.testHybridBeforeEachAndTestYamlAndAfterEachChronologicalOrder`: verifies exact chronological ordering across `@BeforeEach`, YAML `@Test`, and `@AfterEach`.
  - `NeodymiumAiRunnerTest.testTryCatchRecoveredStepDoesNotFailOverallTest`: verifies caught step errors do not poison `overallSuccess` or `KEY_LAST_EXECUTION_ERROR`.
  - `PlaybookLifecycleExecutionTest.testInteractiveStateBuilderResolvesMainStepsWhenStepsEmptyAndFiltersDuplicates`: verifies `InteractiveStateBuilder` resolves `mainSteps` when `steps` is empty, preserves executed setup steps ahead of pending main steps in hybrid tests, and prevents duplicate step entries.
  - `AiSessionTest.testLegacyCustomSubclassCompatibilityWithoutOverridingDoClose`: verifies backward compatibility for external subclasses extending `AiSession` without implementing `doClose()`.
  - `AiSessionTest.testManagedLifecycleDefersSessionFinishedEventUntilClose`: verifies `setManagedLifecycle(true)` suppresses intermediate `SessionFinishedEvent` emissions and dispatches authoritatively upon `close()`.
  - `AiSessionTest.testManagedLifecyclePropagatesErrorToSessionFinishedEvent`: verifies `setManagedLifecycle(true)` propagates step errors to `SessionFinishedEvent` upon `close()`.
  - `AiSessionTest.testStandaloneUnmanagedSessionDispatchesFinishedEventOnFirstExecution`: verifies and locks down unmanaged session lifecycle contract where standalone calls finalize immediately.
  - `AiSessionTest.testUnmanagedSessionClosedWithoutExecutionDoesNotDispatchFinishedEvent`: verifies unmanaged sessions closed without execute calls do not dispatch false-positive finished events.
  - `NeodymiumAiRunnerTest.testTestBodyAssertionFailureSetsFailureReasonInReport`: verifies that when a test body fails with an assertion error, `PreliminaryReportListener` captures the failure reason and stack trace in the report.
  - `NeodymiumAiRunnerTest.testAfterPlaybookTeardownFailureInAfterEachIsRethrown`: verifies after-playbook teardown failures during `afterEach()` are rethrown to fail the test in JUnit.
  - `NeodymiumAiRunnerTest.testAfterPlaybookTeardownFailureAttachedAsSuppressedWhenTestBodyFails`: verifies teardown failure is attached as a suppressed exception when both the test body and teardown fail.
  - `NeodymiumAiRunnerTest.testForceRecordingVoidedWhenFailureRecordedWithNullContext`: verifies `FORCE_RECORDING` voids invalid recording files when failure occurs with `context == null`.
  - `NeodymiumAiRunnerTest.testAppendExecutedStepsHandlesUnmodifiableStepsList`: verifies unmodifiable lists under `playbook.steps` and `playbook.flatSteps` are safely handled without throwing `UnsupportedOperationException`.
  - `AiSessionTest.testManagedLifecycleClearLastExecutionErrorReportsSuccessOnClose`: verifies `clearLastExecutionError()` clears recorded failure state and reports `overallSuccess = true` on `close()`.
