# [DEF-20261030-1410] Empty Companion Playbook Recordings in Programmatic AI Tests

- **Status:** `Resolved`
- **Opened:** 2026-10-30 14:10
- **Closed:** 2026-10-30 14:45
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** Programmatic AI tests executed in recording modes (`LLM_RECORDING`, `FORCE_RECORDING`, `AUTO`, or `REPLAY_WITH_HEALING`) generated companion recording JSON files containing an empty array (`[ ]`) instead of recording executed steps, actions, tool calls, and baseline states (GitHub #492).
- **Root Cause:**
  1. In `NeodymiumAiRunner.java`, programmatic tests (`AiPlaybook.PROGRAMMATIC`) initialized `allSteps` as an empty list (`playbook.getAllSteps()`) and passed it to `PlaybookRecorder`. At serialization time, `PlaybookRecorder` serialized this initial empty list rather than dynamically referencing executed steps.
  2. In `AiSession.java`, step execution previously overwrote the cumulative transient collections across multiple programmatic calls, breaking step accumulation.
  3. In `AiSession.execute(...)`'s inner `finally` block, `playbook.deferSessionFinished` was removed unconditionally and `SessionFinishedEvent` was dispatched prematurely on the first `session.execute(...)` call, completing the session and persisting the empty list before subsequent steps or the enclosing test method completed.
- **Detection Gap ("What did we miss?"):** Prior programmatic test coverage in `AiSessionTest` tested standalone `session.execute(...)` calls without registering a `PlaybookRecorder` or running through the JUnit 5 extension lifecycle in `NeodymiumAiRunner`.
- **Resolution:**
  1. In `PlaybookRecorder.java` (finalized via PR #507 / DEF-20261105-0647), dynamically derived candidate steps to record from `ExecutionContext.getActiveContext().getTransientData().get("playbook.flatSteps")` and resolved top-level steps via `getRootStep()`, ensuring all programmatic and composite steps are captured.
  2. In `AiSession.java`, preserved outer runner lifecycle deferrals (`prevDefer`) and checked `KEY_MANAGED_LIFECYCLE` before firing `SessionFinishedEvent`, delegating final session completion to `NeodymiumAiRunner.afterEach`.
  3. In `AiSession.java`, ensured `beforeSteps`, `steps`, and `afterSteps` accumulate cumulatively across sequential `execute(...)` calls without clobbering previously stored steps.
- **Safety Net Added:**
  1. `@Test` `AiSessionTest.testProgrammaticBeforeAndAfterStepsAccumulateAcrossCalls` verifying cumulative preservation of setup and teardown steps across multiple programmatic calls.
  2. `@Test` `NeodymiumAiRunnerTest.testProgrammaticListenerRegistrationInRecordingMode` ensuring `PlaybookRecorder` and `TokenBudgetGuard` are registered on `session.getEventBus()` during recording modes (`LLM_RECORDING`).
  3. `@Test` `NeodymiumAiRunnerTest.testProgrammaticListenerOmittedInNonRecordingMode` ensuring `PlaybookRecorder` is omitted from `session.getEventBus()` in non-recording modes (`LLM_ONLY`).
  4. `@Test` `NeodymiumAiRunnerTest.testProgrammaticExecutionOnFailureDoesNotPersistCorruptRecording` ensuring that test execution failures abort persistence and void corrupt recordings.
  5. `@Test` `NeodymiumAiRunnerRecordReplayTest.testProgrammaticBeforeEachAndTestMethodAreRecordedInOrder` verifying end-to-end chronological recording of programmatic setup and test steps.
