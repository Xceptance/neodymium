# [DEF-20261016-1230] Interactive Mode Step Edit Off-by-One Mutates Subsequent Step Instead of Current Step

- **Status:** `Resolved`
- **Opened:** 2026-10-16 12:30
- **Closed:** 2026-10-16 12:35
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In interactive console mode, editing the active paused step does not update the paused step. Instead, the paused step executes with its original instruction, and a new step with the edited instruction is executed immediately after.
- **Root Cause:** `InteractiveStateBuilder.java` serializes steps with 1-based indices (`obj.addProperty("index", stepIndex + 1)`), which `interactive_console.js` stores in `data-step-idx` and transmits in `EDIT` payloads. However, `InteractiveConsoleListener.java` compared `targetStepIdx` against `resolveStepIndex()`, which returns 0-based indices. Because `1 != 0`, `isCurrentStepEdit` evaluated to `false`. Execution fell into the pending step branch where `findStepByIndex(context, 1)` fetched index 1 of `flatSteps` (Step 2), overwriting the next step's instruction and leaving the active step unchanged while keeping the console paused.
- **Detection Gap ("What did we miss?"):** The original unit test `testEditNonCurrentStepDuringPauseKeepsPauseAndUpdatesTargetStep` constructed a synthetic JSON payload with a manual 0-based index `index: 1` instead of testing against the 1-based `index` serialized by `InteractiveStateBuilder` or simulating the Web UI `data-step-idx` contract. No test existed for editing the active step with the UI's published index.
- **Resolution:** Updated `interactive_console.js` to transmit normalized 0-based `index`, `block`, and explicit `isCurrentStep` flags for active step edits and AI suggestion fixes. Updated `InteractiveConsoleListener.java` to evaluate `isCurrentStepEdit` using explicit flags and normalized indices, with block-aware and 1-based fallback lookups in `findStepByIndex`.
- **Safety Net Added:** Added `@Test` methods `testEditCurrentStepUsingExplicitFlagAndZeroBasedIndex`, `testEditCurrentStepUsingOneBasedIndexAndActiveFlag`, and `testEditPendingStepUsingNormalizedZeroBasedIndex` in `InteractiveConsoleListenerTest.java`.
