# [DEF-20261116-1117] Concurrent UI-Confirm Resurrection, [Error] Trace History Leak, and Dunder-Inside-Italic Rendering

- **Status:** Resolved
- **Opened:** 2026-11-16 10:56
- **Closed:** 2026-11-16 11:20
- **Component:** `aura-manager`
- **Scope:** Framework

## Symptom

1. `_check __init__ now_` rendered as `<em>check __init_</em> now_` in the chat Markdown renderer instead of `<em>check <strong>init</strong> now</em>`.
2. Internal exceptions caught in `AuraChatService.executePendingConfirmation` (emitting `thinking = "[Error] ..."`, `action = "error"`) were treated as executed tool calls in `AuraTestChatController.confirmPendingAction` (`!chatResp.thinking.isBlank()`) and saved with `error = false`, leaking internal error text into future LLM history.
3. Confirming, cancelling, or clearing a pending destructive confirmation via UI buttons while a follow-up chat turn was in flight did not stop the in-flight turn from re-issuing/merging a duplicate pending confirmation card for the already-handled target when `runChatWorkflow` finished.

## Root Cause

1. `findClosingUnderscore` in `dashboard-chat.js` checked `text[idx + 1] !== '_'` and fell through to `idx++`, so on the next loop iteration `idx` landed on the second `_` of a `__` pair and treated it as a single-underscore closer.
2. `AuraTestChatController.confirmPendingAction` inferred tool execution from non-blank `chatResp.thinking`, failing to distinguish `[Tool Call] ...` execution traces from pre-execution `[Error] ...` fallback entries.
3. `AuraChatService.runChatWorkflow` merged `issuedInCurrentTurn` into `pendingConfirmationsBySession` unconditionally without comparing against concurrent UI confirm/cancel/session-clear mutations performed after the turn began.

## Detection Gap ("What did we miss?")

- `DashboardChatJsTest.testMarkdownRendererAndSubmitGuardsViaNode` covered dot-suffixed tokens (`__see __init__.py here__`, `_check _tmp_.log now_`), missing plain double-underscore dunder tokens inside single-underscore spans without a `.ext` boundary.
- `AuraTestChatControllerTest.testGetMessagesTrimsSessionIdDeleteEvictsPendingAndConfirmPreservesExecutedToolHistory` tested `"[Tool Call] cancel_run"` and blank `thinking`, missing non-blank `"[Error] ..."` exception traces.
- `AuraChatServiceTest.testPendingConfirmationRemainsActionableWhileUnrelatedTurnIsInFlight` verified UI confirmation during an unrelated non-tool turn, missing concurrent UI confirmation/cancellation while the in-flight turn re-invokes the same destructive tool target.

## Resolution

1. **Dunder pair skip in `findClosingUnderscore` ([`dashboard-chat.js`](file:///home/robert/Development/internal/neodymium/neodymium/aura-manager/src/main/resources/static/dashboard-chat.js)):** Added an `idx += 2; continue;` skip whenever `text[idx] === '_' && text[idx + 1] === '_'`, matching `findClosingSingleAsterisk`.
2. **Tool trace marker check ([`AuraTestChatController.java`](file:///home/robert/Development/internal/neodymium/neodymium/aura-manager/src/main/java/com/xceptance/aura/test/controller/AuraTestChatController.java)):** Updated `confirmPendingAction` to check `chatResp.thinking != null && chatResp.thinking.contains("[Tool Call]")` before clearing `msg.error`.
3. **Monotonic resolution guard for in-flight turns ([`AuraChatService.java`](file:///home/robert/Development/internal/neodymium/neodymium/aura-manager/src/main/java/com/xceptance/aura/chat/service/AuraChatService.java)):** Added `confirmationMutationSeq`, `resolvedSeqBySession`, and `sessionClearedSeqBySession` updated atomically on confirm, cancel, and session clear, and checked `isResolvedSince(sessionKey, confirmKey, turnStartSeq)` in both `ConfirmationBarrierToolCallback.executeWithBarrier` and `mergeNewlyIssuedConfirmations`.

## Safety Net Added

- [`DashboardChatJsTest.java`](file:///home/robert/Development/internal/neodymium/neodymium/aura-manager/src/test/java/com/xceptance/aura/chat/DashboardChatJsTest.java): Added `_check __init__ now_` assertions to `testMarkdownRendererAndSubmitGuardsViaNode`.
- [`AuraTestChatControllerTest.java`](file:///home/robert/Development/internal/neodymium/neodymium/aura-manager/src/test/java/com/xceptance/aura/chat/controller/AuraTestChatControllerTest.java): Added `[Error] Transport error` assertion to `testGetMessagesTrimsSessionIdDeleteEvictsPendingAndConfirmPreservesExecutedToolHistory`.
- [`AuraChatServiceTest.java`](file:///home/robert/Development/internal/neodymium/neodymium/aura-manager/src/test/java/com/xceptance/aura/chat/service/AuraChatServiceTest.java): Added `testInFlightTurnReinvokingDestructiveToolDoesNotResurrectConcurrentlyHandledAction`.
