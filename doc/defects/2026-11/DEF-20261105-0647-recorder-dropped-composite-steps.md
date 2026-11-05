# [DEF-20261105-0647] PlaybookRecorder dropped composite steps and their sub-steps from recordings

- **Status:** `Resolved`
- **Opened:** 2026-11-05 06:40
- **Closed:** 2026-11-05 06:52
- **Component:** `neodymium-core` (`PlaybookRecorder`)
- **Scope:** `Framework`
- **Symptom:** A YAML test containing a composite step (a folded `>-` block that the parser turns into a parent step with children) produced a recording that held only the plain steps. The composite and all of its sub-steps were missing, so the recording could not reproduce the test. This was a regression introduced on this branch: the same test recorded the composite correctly on `develop`.
- **Root Cause:** To make recordings include programmatic steps (steps executed through `session.execute(...)`), the recorder was changed to build its output from the executed-step list `playbook.flatSteps` instead of the parsed playbook. That list is flattened to leaf steps: a composite parent is not part of it, only its children are, and every child carries a parent reference. The recorder kept only steps with `getParent() == null`, which discards exactly those children, and the composite parent was never in the list to begin with.
- **Detection Gap ("What did we miss?"):** The recorder tests used flat steps only. The one test that included a step with a parent listed the parent in `flatSteps` itself, which never happens in a real run. No test exercised a recording of a playbook with a composite step through the real JUnit lifecycle.
- **Resolution:** `PlaybookRecorder` now maps every entry of `playbook.flatSteps` to its root step (`getRootStep()`) and records each root once (identity-based de-duplication, first-seen order). A composite is therefore written once, with its sub-steps nested in it, and plain steps are unchanged.
- **Safety Net Added:** `PlaybookRecorderTest.testRecordingKeepsCompositeStepWithSubStepsOnce` (unit level, composite absent from `flatSteps` as in a real run) and `NeodymiumAiRunnerRecordReplayTest.testCompositeStepRecordThenReplay` (full JUnit lifecycle: records a folded composite, then replays it in `REPLAY_STRICT`). `NeodymiumAiRunnerRecordReplayTest` also pins the recording order for programmatic and hybrid tests.
