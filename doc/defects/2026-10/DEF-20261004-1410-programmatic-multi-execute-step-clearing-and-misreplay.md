# [DEF-20261004-1410] Programmatic Multi-Execute Step Clearing and Positional Replay Misbinding

- **Status:** `Resolved`
- **Opened:** 2026-10-04 14:10
- **Closed:** 2026-10-04 14:10
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In programmatic tests invoking `session.execute(...)` multiple times (such as Step-by-Step Java Debugging or mixed Selenide/AI tests), only the steps from the final `execute()` call were persisted to the recording file on disk. When replayed in `REPLAY_*` modes, `AiSession.execute` blindly merged recorded actions by position index without checking instructions, corrupting steps (e.g. replaying step 1's actions in step 2) or failing due to step clearing.
- **Root Cause:** In `AiSession.execute(Playbook, SessionData)`, `sessionSteps.clear(); sessionSteps.addAll(playbookSteps);` was executed on every invocation. Because `PlaybookRecorder` held a reference to `sessionSteps` and flushed on each `SessionFinishedEvent`, previous steps were wiped before the final flush. Furthermore, in replay mode, `sessionSteps` was cleared after the first call, wiping subsequent recorded steps from memory, and the replay loop merged `parsed[i] <- sessionSteps[i]` purely by positional offset without verifying instruction equality.
- **Detection Gap ("What did we miss?"):** Programmatic tests previously focused on single `session.execute(...)` calls with multiple steps or text blocks, while multi-call programmatic tests in demo suites lacked companion replay assertions.
- **Resolution:**
  1. Updated `AiSession.execute` to accumulate executed steps (`sessionSteps.addAll(playbookSteps)`) across sequential `execute()` calls instead of wiping them with `clear()`.
  2. Preserved the full list of recorded steps on replay under `"playbook.recordedSteps"` and introduced a sequential `"playbook.replayCursor"`.
  3. Enforced strict normalized instruction matching between `parsed` and `recorded` steps (with variable-resolution fallback). If a mismatch occurs, execution terminates conclusively with a `ConclusiveFailureException` rather than executing mismatched actions.
  4. Enforced step count boundary verification in strict replay mode when executed calls exceed recorded steps.
- **Safety Net Added:** Added 4 permanent automated unit tests to `org.neodymium.ai.session.AiSessionTest`:
  - `testProgrammaticMultiExecuteAccumulatesStepsAcrossCalls`
  - `testProgrammaticMultiExecuteReplayAlignsInstructionsSequentially`
  - `testProgrammaticMultiExecuteReplayFailsOnInstructionMismatch`
  - `testProgrammaticMultiExecuteReplayFailsInStrictModeWhenStepsExceeded`
