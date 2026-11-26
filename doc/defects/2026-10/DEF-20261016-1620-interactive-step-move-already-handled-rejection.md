# [DEF-20261016-1620] Interactive Step Move Rejected with Spurious Already-Handled Status

- **Status:** `Resolved`
- **Opened:** 2026-10-16 16:20
- **Closed:** 2026-10-16 16:23
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** Submitting a request to move/reorder steps in the Interactive Console returns HTTP 200 with `{"status":"already-handled"}` and discards the move, even when no move has occurred yet in the test run, or when performing multiple step moves sequentially during a pause.
- **Root Cause:**
  1. `InteractiveConsoleEngine.submitAction(final JsonObject req)` unconditionally required `activePauseId != null && activePauseId.equals(incomingPauseId)` for all actions, including structural playbook mutations like `REORDER`.
  2. When `activePauseId` was `null` (during active execution, runner initialization, or pause-loop turnaround) or did not match, the engine rejected the request and returned `{"status":"already-handled"}`. This falsely reported an unhandled move as already handled and prevented legitimate playbook reordering.
  3. `InteractiveConsoleEngine` held pending actions in a single `AtomicReference<JsonObject> pendingAction` rather than an action queue, causing sequential in-pause reorders to collide with token invalidation in `waitForAction`'s finally block (`currentPauseId.set(null)`).
- **Detection Gap ("What did we miss?"):** Existing tests in `InteractiveConsoleEngineTest` and `InteractiveConsoleListenerTest` tested sequential asynchronous submission where `REORDER` was immediately followed by `RUN`, but did not assert submission of multiple consecutive `REORDER` actions with stale/null `pauseId` tokens or while `activePauseId` was cleared.
- **Resolution:**
  1. In `InteractiveConsoleEngine.java`: Distinguished structural playbook actions (`REORDER`, `MOVE_STEP`, `ADD`, `TOGGLE_BREAKPOINT`, etc.) from execution-advancing actions (`RUN`, `SKIP`, `AUTO`, `HEAL`, `ABORT`).
  2. Allowed structural actions to bypass strict single-use `pauseId` matching, returning `{"status":"accepted"}` rather than `{"status":"already-handled"}`.
  3. Backed pending action storage with a concurrent queue (`ConcurrentLinkedQueue<JsonObject> pendingActions`) so multiple sequential step reorders are preserved and processed in FIFO order without token invalidation dropouts.
  4. Preserved existing idempotent `already-handled` behavior for execution actions (`APPROVE`, `RUN`, `PAUSE`) with no active pause to protect test runner state transitions and maintain test contract compatibility.
- **Safety Net Added:** Added unit tests in `InteractiveConsoleEngineTest`:
  - `testReorderActionAcceptedEvenWhenPauseIdIsNull`: verifies `REORDER` returns `{"status":"accepted"}` when no pause token is active or when incoming `pauseId` is mismatched.
  - `testMultipleSequentialReordersAcceptedWithoutAlreadyHandledRejection`: verifies multiple consecutive `REORDER` actions are accepted and queued without token rejection, and consumed in FIFO order by `waitForAction`.
