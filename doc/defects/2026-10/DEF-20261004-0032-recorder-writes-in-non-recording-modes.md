# [DEF-20261004-0032] Recorder overwrote recordings in LLM_ONLY and LINTER_ONLY modes

- **Status:** `Resolved`
- **Opened:** 2026-10-04 00:10
- **Closed:** 2026-10-04 00:32
- **Component:** `neodymium-core`, `neodymium-e2e-tests`
- **Scope:** `Framework`
- **Symptom:** Running a test in `LLM_ONLY` or `LINTER_ONLY` rewrote the playbook recording file next to the test, and replaced a good recording with a different one. `LINTER_ONLY` runs also created action-less `PENDING` recordings; 13 of them were committed as `neodymium-e2e-tests/src/test/resources/PrelinterRuleMatrixLiveTest_*.json`.
- **Root Cause:** `NeodymiumAiRunner` registered `PlaybookRecorder` on the event bus for every execution mode, and `PlaybookRecorder.onEvent` wrote whenever the in-memory steps differed from the file. `LINTER_ONLY` sets `success=true` in `StateMachineRunner` and dispatches `SessionFinishedEvent`, so the recorder treated a run that never executed any browser action as a finished recording. `LLM_ONLY` is documented as "no recording", but nothing in the recorder consulted the mode.
- **Detection Gap ("What did we miss?"):** `PlaybookRecorderTest` only exercised the recording modes, so no test asserted that the other modes leave the file system alone. Related to P1-1 of the 2026-10-01 review (replay rewrites recordings): same family, "the recorder writes outside the recording modes".
- **Resolution:** Added `ExecutionMode.persistsRecording()` (`isRecording() || isReplay()`). `PlaybookRecorder.onEvent` returns early (debug log) when the mode does not persist, and `NeodymiumAiRunner` only registers the recorder for modes that do. Deleted the 13 junk `PrelinterRuleMatrixLiveTest_*.json` files. Writes in recording and replay modes are unchanged on purpose.
- **Safety Net Added:** `PlaybookRecorderTest.testNonPersistingModesNeverOverwriteExistingRecording`, `PlaybookRecorderTest.testNonPersistingModesNeverCreateRecording` and the guard `PlaybookRecorderTest.testPersistingModesStillWriteRecording`. The first two failed before the fix (2 failures) and pass now.
