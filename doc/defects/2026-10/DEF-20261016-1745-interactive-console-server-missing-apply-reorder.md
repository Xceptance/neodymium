# [DEF-20261016-1745] InteractiveConsoleServer Missing applyReorder Method Causing Compilation Failure

- **Status:** `Resolved`
- **Opened:** 2026-10-16 17:45
- **Closed:** 2026-10-16 18:18
- **Component:** `neodymium-core`
- **Scope:** `Framework & Test/Harness`
- **Symptom:** `mvn test-compile` on `neodymium-core` failed with 9 `cannot find symbol` errors referencing `InteractiveConsoleServer.applyReorder(JsonObject, List<JsonObject>, JsonObject, int)` across five unit tests in `InteractiveConsoleServerTest.java`.
- **Root Cause:** Commit `885285eb2` added unit tests against `InteractiveConsoleServer.applyReorder(...)` but omitted the extraction of the reorder logic from `InteractiveConsoleServer.runSimulation()` into a package-private static method. Furthermore, the inlined simulation code retained an obsolete forward target decrement (`adjustedToIndex--`) that conflicted with 0-indexed list insertion math.
- **Detection Gap ("What did we miss?"):** Commit `885285eb2` was committed without running a full `mvn test-compile` on the parent repository or `neodymium-core`.
- **Resolution:**
  1. Extracted package-private static method `int applyReorder(final JsonObject activeState, final List<JsonObject> allSteps, final JsonObject response, final int currentStepIndex)` in `InteractiveConsoleServer.java`.
  2. Aligned the target index math with `InteractiveConsoleListener.handleReorderStep`, clamping `adjustedToIndex` to `[0, toArray.size()]` and inserting at `adjustedToIndex` without forward decrementing.
  3. Replaced the inlined `REORDER` simulation action handling in `InteractiveConsoleServer.runSimulation()` with a call to `applyReorder`.
- **Safety Net Added:** Verified via `mvn clean test-compile` across the entire reactor and verified all 16 tests in `InteractiveConsoleServerTest` pass, specifically:
  - `testApplyReorderForwardWithinSameBlock`
  - `testApplyReorderBackwardWithinSameBlock`
  - `testApplyReorderCrossBlock`
  - `testApplyReorderBoundaryClamping`
  - `testApplyReorderTracksRunningStepIndex`
