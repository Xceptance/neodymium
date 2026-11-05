# [DEF-20261104-1600] Interactive add/reorder corrupted executed-step history; append was not idempotent

- **Status:** `Resolved`
- **Opened:** 2026-11-04 15:54
- **Closed:** 2026-11-04 16:00
- **Component:** `neodymium-core` (`InteractiveConsoleListener`, `NeodymiumAiRunner`)
- **Scope:** `Framework`
- **Symptom:** With executed `@BeforeEach` steps present, an interactive in-block REORDER of main steps moved the wrong step in the executed-step list (`playbook.steps`), so the HUD showed a wrong order. An interactive ADD of a main step while the setup phase was still running left the new step twice in `playbook.steps` and `playbook.flatSteps` once the main phase was appended, which would later put a duplicated step into the recording.
- **Root Cause:** `handleAddStep` and `handleReorderStep` mirrored edits made to `playbook.mainSteps` into `playbook.steps` using the main-relative index. `playbook.steps` is offset by the executed setup steps (and holds no main steps before the main phase starts), so the index pointed at the wrong element or an unrelated region. `NeodymiumAiRunner.appendExecutedSteps` then appended the pending main steps (which already contained the mirrored step) without any de-duplication.
- **Detection Gap ("What did we miss?"):** The existing add/reorder tests used either no setup steps or a `playbook.steps` list without setup steps, so the setup-step offset was never exercised. No test combined an interactive edit with a subsequent `appendExecutedSteps`.
- **Resolution:** Edits are mirrored by identity, not by index (`InteractiveConsoleListener.mirrorIntoExecutedSteps`): the step is placed behind its predecessor in the main list (or in front of its successor), and nothing is changed while no main step is in the history yet. `appendExecutedSteps` now skips steps already present (identity) in both `playbook.steps` and `playbook.flatSteps`.
- **Safety Net Added:** `InteractiveConsoleListenerTest.testReorderMainStepsKeepsExecutedStepsAlignedWhenBeforeStepsPresent`, `InteractiveConsoleListenerTest.testAddMainStepDuringBeforePhaseDoesNotDuplicateWhenMainPhaseIsAppended`. Additionally `PlaybookRecorderTest.testRecordingContainsAllLifecyclePhaseStepsOnceInExecutionOrder` documents that recordings contain setup, test and teardown steps once each, in execution order.
