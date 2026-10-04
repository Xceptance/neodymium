# [DEF-20261004-1330] REPLAY_STRICT persisted recordings to disk and LLM_RECORDING deleted pre-existing recordings upfront

- **Status:** `Resolved`
- **Opened:** 2026-10-04 12:55
- **Closed:** 2026-10-04 13:30
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** 
  1. `REPLAY_STRICT` executions modified recording JSON files on disk when step metadata (such as timestamps or durations) differed, violating the strict read-only replay contract.
  2. `LLM_RECORDING` deleted pre-existing recording files at test start (`beforeEach`), destroying baseline recordings if the subsequent live recording run failed.
- **Root Cause:**
  1. `ExecutionMode.persistsRecording()` previously returned `isRecording() || isReplay()`. Because `REPLAY_STRICT` has `isReplay() == true`, `PlaybookRecorder` was registered and wrote out file updates on session completion.
  2. In `NeodymiumAiRunner.java`, upfront deletion was conditioned on `this.mode.isRecording()`, which included both `FORCE_RECORDING` and `LLM_RECORDING`.
- **Detection Gap ("What did we miss?"):** DEF-20261004-0032 introduced `persistsRecording()` to protect `LLM_ONLY` and `LINTER_ONLY`, but assumed all replay modes needed persistence for healing. However, `REPLAY_STRICT` specifically does not support healing and must be completely read-only.
- **Resolution:**
  1. Updated `ExecutionMode.persistsRecording()` to return `isRecording() || this == REPLAY_WITH_HEALING`. Added `REPLAY_STRICT` to non-persisting mode suites.
  2. Scoped upfront deletion in `NeodymiumAiRunner.java` lines 1386 and 1625 strictly to `this.mode == ExecutionMode.FORCE_RECORDING`. In `LLM_RECORDING`, pre-existing recordings are preserved upfront and only overwritten upon successful execution.
- **Safety Net Added:**
  - `ExecutionModeTest.testExecutionModeClassification` (asserting `persistsRecording()` across all 6 modes).
  - `PlaybookRecorderTest.testNonPersistingModesNeverOverwriteExistingRecording` (including `REPLAY_STRICT`).
  - `NeodymiumAiRunnerTest.testForceRecordingDeletesUpfrontWhileLlmRecordingPreserves` (verifying upfront deletion behavior on disk).
