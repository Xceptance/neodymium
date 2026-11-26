# [DEF-20261016-1535] Interactive Console Step Forward Reorder Reverted Due to Redundant Index Decrement

- **Status:** `Resolved`
- **Opened:** 2026-10-16 15:35
- **Closed:** 2026-10-16 15:41
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In the Interactive Console (simulation mode), moving a playbook step forward (e.g. from index 0 to index 1 or 2) within the same block appears to succeed initially with an `"accepted"` HTTP 200 response, but the reordering is immediately reverted back to the original order once the server enters the thinking phase or broadcasts subsequent state. Backward reordering (e.g. from index 2 to 0) or cross-block movements functioned normally.
- **Root Cause:** In `InteractiveConsoleServer.java#runSimulationInternal`, the `REORDER` handler contained a redundant forward decrement check:
  ```java
  int adjustedToIndex = toIndex;
  if (fromBlock.equals(toBlock) && adjustedToIndex > fromIndex) {
      adjustedToIndex--;
  }
  ```
  However, the frontend client (`interactive_console.js` via `handleDrop`, `reorderStepDirect`, and `moveStep`) already computes and sends `toIndex` as the target post-removal insertion slot in the array. Decrementing `adjustedToIndex` when `adjustedToIndex > fromIndex` introduced an off-by-one error (e.g., moving from index 0 to 1 resulted in `adjustedToIndex = 0`, placing the step back at its original index). Because the live simulation loop immediately set status to `"running"` and pushed `"AI is thinking..."`, the client's optimistic order was overwritten by the server's reverted block state.
- **Detection Gap ("What did we miss?"):** `InteractiveConsoleListener.java` (used during live execution) implemented reordering without the redundant decrement (`adjustedToIndex = toIndex` clamped to `[0, toList.size()]`), whereas `InteractiveConsoleServer.java` (used during static simulation mode) had legacy decrement logic. Existing unit tests for `InteractiveConsoleServer` verified HTTP CORS bindings and static file integrity, but did not test the simulation action loop or reordering mechanics.
- **Resolution:**
  1. Extracted and normalized simulation reordering logic into a package-private method `InteractiveConsoleServer.applyReorder(activeState, allSteps, response, currentStepIndex)`.
  2. Removed the redundant `adjustedToIndex > fromIndex` decrement in `InteractiveConsoleServer`, aligning it with `InteractiveConsoleListener` and the client's post-removal index contract.
  3. Replaced inline FQCNs (`java.util.UUID`) with top-level imports.
- **Safety Net Added:** Added comprehensive automated regression tests in `InteractiveConsoleServerTest.java`:
  - `testApplyReorderForwardWithinSameBlock`: Asserts moving steps forward within the same block preserves the new order and does not revert to the initial order.
  - `testApplyReorderBackwardWithinSameBlock`: Asserts moving steps backward updates positions and reindexes correctly.
  - `testApplyReorderCrossBlock`: Asserts moving steps between blocks (`before` -> `steps`) transfers elements and reconstructs sequential step indices across blocks.
  - `testApplyReorderBoundaryClamping`: Asserts negative and excessive indices are safely clamped to boundaries.
  - `testApplyReorderTracksRunningStepIndex`: Asserts active running step index is accurately tracked when steps before or after it are moved.
