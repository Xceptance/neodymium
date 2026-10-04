# [DEF-20261004-1325] Leaked healed step flag caused subsequent successful replay steps to be marked HEALED

- **Status:** `Resolved`
- **Opened:** 2026-10-04 12:50
- **Closed:** 2026-10-04 13:25
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** During test execution in `REPLAY_WITH_HEALING` mode, if a step experienced healing (setting `ExecutionContext.KEY_IS_HEALED_STEP`), subsequent steps that replayed cleanly without any healing were also marked with status `PlaybookStepStatus.HEALED` instead of `SUCCESS`.
- **Root Cause:** In `ExecuteActionsStep.java`, the step end-hook inspected `(Boolean) c.getTransientData().get(ExecutionContext.KEY_IS_HEALED_STEP)` using `.get()` instead of `.remove()`. Additionally, when preparing a new step in `execute(...)`, `KEY_IS_HEALED_STEP` was not purged from `contextState.getTransientData()`. Consequently, once set to `true`, the flag persisted across all following steps in the session.
- **Detection Gap ("What did we miss?"):** Single-step unit tests in `ExecuteActionsStepTest` tested isolated step healing transitions without chaining multiple sequential steps where step 1 was healed and step 2 was a standard recorded replay.
- **Resolution:** In `ExecuteActionsStep.java`:
  1. Purged `contextState.getTransientData().remove(ExecutionContext.KEY_IS_HEALED_STEP)` at the beginning of each step execution.
  2. Changed the step end-hook to atomically consume and remove the flag: `final Object isHealed = c.getTransientData().remove(ExecutionContext.KEY_IS_HEALED_STEP);`.
- **Safety Net Added:** Added `@Test` `ExecuteActionsStepTest.testHealedStepFlagDoesNotLeakToSubsequentReplaySteps`, simulating a sequence where step 1 triggers healing and step 2 executes normally, asserting step 2 remains `PlaybookStepStatus.SUCCESS` and the transient flag is removed.
