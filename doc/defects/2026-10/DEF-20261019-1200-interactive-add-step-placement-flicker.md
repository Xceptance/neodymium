# [DEF-20261019-1200] Interactive Add Step Optimistic Placement Flicker and End-of-Test Displacement

- **Status:** `Resolved`
- **Opened:** 2026-10-19 12:00
- **Closed:** 2026-10-19 12:15
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** When clicking the "+" button on an existing step in interactive console to add a new step directly after it, the new step intermittently appeared at the bottom/end of the test before moving to its correct position. In simulation mode or in tests with `before` blocks, steps were placed at the end or in the wrong block entirely.
- **Root Cause:**
  1. `applyState` in `interactive_console.js` reconciled pending/temporary steps during incoming SSE events using `newList.push(pendingStep)`, discarding the original relative insertion index and appending the step to the end of the list until server confirmation arrived.
  2. `renderStepCard` did not pass `blockName` to `addStepAfter`, leading to cross-block index collisions where `bKey = 'before'` matched step 1 of main steps. Lookup misses defaulted to `openAddStepOverlay('steps')` without `targetIndex`, which used `targetList.push(tempStep)`.
  3. `InteractiveConsoleServer.java` handled `"ADD"` in simulation mode via unconditional `array.add(newStep)`, ignoring the client-provided `index`.
- **Detection Gap ("What did we miss?"):** Existing unit tests for `InteractiveConsoleListener` verified that the backend test execution stack inserted at the specified index, but unit tests did not simulate intermediate SSE state events during inline editing in the frontend console, nor did they verify that simulation mode respected `index`.
- **Resolution:**
  1. In `interactive_console.js`:
     - Pass `blockName` from `renderStepCard` to `addStepAfter(event, ${step.index}, '${blockName}')`.
     - In `addStepAfter`, use `blockName` directly and compare indices with loose equality (`==`).
     - In `openAddStepOverlay`, record `targetBlock` and `targetInsertIndex` on `tempStep`.
     - In `applyState`, reconcile pending steps by splicing them into `newList` at `pendingStep.targetInsertIndex` (or `oldList.indexOf(pendingStep)`) rather than appending with `newList.push(pendingStep)`. Check both resolved `instruction` and `rawInstruction`.
     - In `saveEdit`, use `targetStep.targetInsertIndex` to guarantee the original insertion index is preserved.
  2. In `InteractiveConsoleServer.java`:
     - In `"ADD".equals(action)`, inspect `response.get("index")` and insert `newStep` at `targetIdx` in the JSON array.
- **Safety Net Added:**
  - `InteractiveConsoleServerTest.testInteractiveConsoleJsApplyStatePreservesPendingStepPosition`: asserts `applyState` splices pending steps into their original index rather than pushing to the end.
  - `InteractiveConsoleServerTest.testInteractiveConsoleJsAddStepAfterPassesBlockName`: asserts `renderStepCard` passes `blockName` to `addStepAfter`.
  - `InteractiveConsoleServerTest.testSimulationAddStepRespectsTargetIndex`: verifies simulation mode inserts at `targetIdx`.
