# [DEF-20261020-1638] Interactive Console Cross-Block LLM Call Pollution in Teardown Steps

- **Status:** `Resolved`
- **Opened:** 2026-10-20 16:38
- **Closed:** 2026-10-21 10:05
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In interactive console mode, pending or running steps in the `after` teardown block display LLM calls from previous core block steps (specifically core step 0) until the teardown step generates its own LLM call.
- **Root Cause:**
  1. `InteractiveStateBuilder.serializeStep` had an unscoped fallback matching `report.getLlmCalls()` solely by `stepIndex` or `globalIndex` without verifying block parity against `expectedBlock`. For the first teardown step (`stepIndex == 0`), it falsely matched core step 0 (`call.getStepIndex() == 0`).
  2. `ReportLlmCallEntry` lacked a `block` attribute, preventing callers from distinguishing which lifecycle block (`before`, `steps`, `after`) an LLM invocation originated from.
  3. `PreliminaryReportListener` did not populate block metadata when receiving `LlmResponseReceivedEvent`.
  4. `InteractiveStateBuilder.serializeLlmCalls` omitted `block` in serialized JSON payloads, causing `interactive_console.js` to filter across block boundaries.
  5. `InteractiveConsoleListener.resolveStepIndex` checked `flatSteps` before `afterSteps`, potentially leaking global sequence indices instead of block-scoped indices.
- **Detection Gap ("What did we miss?"):** Existing interactive console unit tests tested core block execution and serialized states with pre-populated step calls, but lacked assertions verifying lifecycle block isolation between core `steps` and teardown `after` steps when an after step is pending/running without its own LLM calls yet.
- **Resolution:**
  1. Added `block` field with getter and setter to `ReportLlmCallEntry` in `TestExecutionReport.java`.
  2. Populated `block` on `callEntry` in `PreliminaryReportListener.java` from `currentParentStep` or `currentStep` (defaulting to `"steps"`).
  3. Scoped LLM call matching in `InteractiveStateBuilder.serializeStep` to require `expectedBlock.equalsIgnoreCase(call.getBlock())`.
  4. Included `block` property in serialized LLM call objects in `InteractiveStateBuilder.serializeLlmCalls`.
  5. Updated `buildLlmCallsHtml` in `interactive_console.js` to enforce block matching when falling back to `currentState.llmCalls`.
  6. Reordered `resolveStepIndex` in `InteractiveConsoleListener.java` to check `afterSteps` before `flatSteps`.
- **Safety Net Added:** Added unit test `InteractiveConsoleEngineTest#testTeardownAfterStepDoesNotDisplayPreviousCoreBlockLlmCalls` asserting that teardown after steps never absorb core block LLM calls and properly display their own calls once available.
