# [DEF-20261030-1410] Empty Companion Playbook Recordings in Programmatic AI Tests

- **Status:** `Resolved`
- **Opened:** 2026-10-30 14:10
- **Closed:** 2026-10-30 14:45
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** Programmatic AI tests executed in recording modes (`LLM_RECORDING`, `FORCE_RECORDING`, `AUTO`, or `REPLAY_WITH_HEALING`) generated companion recording JSON files containing an empty array (`[ ]`) instead of recording executed steps, actions, tool calls, and baseline states.
- **Root Cause:**
  1. In `NeodymiumAiRunner.java`, programmatic tests (`AiPlaybook.PROGRAMMATIC`) initialized `allSteps` as an empty list (`playbook.getAllSteps()`) and passed it to `PlaybookRecorder`, but never stored `allSteps` in `ExecutionContext.getTransientData()`.
  2. In `AiSession.java`, line 670 previously executed `this.executionContext.getTransientData().put("playbook.steps", new ArrayList<>(playbookSteps));`, which overwrote the cumulative `sessionSteps` list populated across multiple programmatic calls and broke step accumulation.
  3. `AiSession.execute(...)` did not synchronize executed or replayed/healed steps into the runner's `allSteps` list held by `PlaybookRecorder`.
  4. In `AiSession.execute(...)`'s inner `finally` block, `playbook.deferSessionFinished` was removed unconditionally and `SessionFinishedEvent` was dispatched prematurely on the first `session.execute(...)` call, completing the session and persisting the empty `allSteps` list before subsequent steps or the enclosing test method completed.
- **Detection Gap ("What did we miss?"):** Prior programmatic test coverage in `AiSessionTest` tested standalone `session.execute(...)` calls without registering a `PlaybookRecorder` or running through the JUnit 5 extension lifecycle in `NeodymiumAiRunner`. Furthermore, `testProgrammaticMultiExecuteAccumulatesStepsAcrossCalls` in `AiSessionTest` was broken by the transient data overwrite of `"playbook.steps"`.
- **Resolution:**
  1. In `NeodymiumAiRunner.java`, registered `allSteps` into transient data under key `"playbook.allSteps"`.
  2. In `AiSession.java`, removed the overwriting `put("playbook.steps", new ArrayList<>(playbookSteps))` so cumulative `sessionSteps` is preserved across multiple `execute(...)` calls.
  3. In `AiSession.java`, synchronized executed steps into `runnerAllSteps` (from `"playbook.allSteps"`), handling index alignment during replay/healing as well as appending during recording.
  4. In `AiSession.java`, inspected whether `playbook.deferSessionFinished` was already active prior to step execution (`deferralAlreadyActive`). If active, `AiSession` preserves outer runner deferral and skips premature `SessionFinishedEvent` dispatch, delegating final event dispatch and recording persistence to `NeodymiumAiRunner.executeTestLifecycle`.
- **Safety Net Added:**
  1. Fixed existing `@Test` `AiSessionTest.testProgrammaticMultiExecuteAccumulatesStepsAcrossCalls` verifying cumulative step preservation.
  2. Added `@Test` `AiSessionTest.testProgrammaticExecuteSynchronizesIntoRunnerAllSteps` verifying that multiple programmatic `session.execute(...)` calls accumulate steps into `playbook.allSteps`.
  3. Added `@Test` `AiSessionTest.testProgrammaticDeferralHonoredWhenRunnerDeferralActive` ensuring `SessionFinishedEvent` is deferred when runner deferral is active.
  4. Added `@Test` `NeodymiumAiRunnerTest.testProgrammaticSetupInitializesPlaybookAllSteps` ensuring `NeodymiumAiRunner` registers `playbook.allSteps` in transient data for programmatic tests.
  5. Added `@Test` `NeodymiumAiRunnerTest.testProgrammaticListenerRegistrationInRecordingMode` ensuring `PlaybookRecorder` and `TokenBudgetGuard` are registered on `session.getEventBus()` during recording modes (`LLM_RECORDING`).
  6. Added `@Test` `NeodymiumAiRunnerTest.testProgrammaticListenerOmittedInNonRecordingMode` ensuring `PlaybookRecorder` is omitted from `session.getEventBus()` in non-recording modes (`LLM_ONLY`).
  7. Added `@Test` `NeodymiumAiRunnerTest.testProgrammaticExecutionWritesNonEmptyCompanionRecording` ensuring a full programmatic test lifecycle writes a non-empty companion JSON recording file containing actual steps and actions.
  8. Added `@Test` `NeodymiumAiRunnerTest.testProgrammaticMultiExecuteAccumulatesAllStepsInRecordingFile` ensuring multiple sequential `session.execute(...)` calls accumulate cleanly into a single cohesive companion JSON recording file.
  9. Added `@Test` `NeodymiumAiRunnerTest.testProgrammaticExecutionOnFailureDoesNotPersistCorruptRecording` ensuring that test execution failures abort persistence and void corrupt recordings.
