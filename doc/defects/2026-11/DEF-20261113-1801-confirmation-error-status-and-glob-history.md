# [DEF-20261113-1801] Confirmation Error Misclassification, Stale Messages Dropdown Fallback, and Inline Glob/History Edge Cases

- **Status:** `Resolved`
- **Opened:** 2026-11-13 17:58
- **Closed:** 2026-11-13 18:01
- **Component:** `aura-manager`
- **Scope:** `Framework & AI/Prompt`
- **Symptom:**
  1. Clicking UI **Confirm** when the underlying MCP tool returned JSON `"status": "ERROR"` or `"NOT_FOUND"` (e.g., expired token or deleted file) produced `"Confirmed ..."` with `action = null`, storing a failed operation in session history with `error = false`. On exceptions, `executePendingConfirmation` also returned `null` for `pendingConfirmations`.
  2. Switching to or reloading a deleted session via `GET /api/chat/messages` updated `#hiddenSessionId` to the fallback session while leaving `#chatSessionSelect` pointing at the deleted ID, misdirecting subsequent rename/delete actions.
  3. Inline markdown rendered `"Match *.yaml and *important*"` as `<em>.yaml and *important</em>` and formatted `/__init__/` or `/_tmp_/` path segments as `<strong>`/`<em>`.
  4. Hallucinated `confirmationToken` arguments (e.g., `"confirmed"`) bypassed `priorConfirmationClaimer`, UI Confirm/Cancel left adjacent `AssistantMessage` turns in replay history, and `#chatInput` cleared newly typed follow-up text when an earlier `/api/chat` request succeeded.
- **Root Cause:**
  1. `AuraChatService.executePendingConfirmation` did not inspect tool JSON `status` for `"ERROR"`/`"NOT_FOUND"` and formatted every non-throwing call via `"Confirmed ..."`, while its `catch` block omitted remaining session confirmations.
  2. `AuraTestChatController.getMessages` omitted `oobSessionSelect = true` when `chatSessionService.getSession(sessionId)` fell back to another session.
  3. `dashboard-chat.js` blocked `/` and `\` only before `*`, missing trailing `.`, `/`, `\` after `*` (`*.yaml`) and path boundaries around `_` / `__`.
  4. `ConfirmationBarrierToolCallback.executeWithBarrier` skipped `priorConfirmationClaimer` whenever an arbitrary non-`REDACTED` `confirmationToken` string was supplied by the model, `buildHistoryMessages` did not coalesce adjacent or drop leading `AssistantMessage` entries, and `htmx:afterRequest` cleared `#chatInput` unconditionally.
- **Detection Gap ("What did we miss?"):**
  - `AuraChatServiceTest` tested only `"SUCCESS"`/`"CANCELLED"` for `executePendingConfirmation` and empty/same-turn tokens; `AuraTestChatControllerTest` did not verify `oobSessionSelect` on `GET /api/chat/messages` fallback; `DashboardChatJsTest` had no `*word*` emphasis after `*.yaml` on the same line and dispatched `htmx:afterRequest` without mutating `#chatInput` mid-flight.
- **Resolution:**
  - Added `isToolErrorStatus` and error prefixing (`"Could not execute ..."`) in `AuraChatService.executePendingConfirmation` / `formatDirectConfirmationOutcome`, and preserved `remaining` pending confirmations in the catch path.
  - Updated `ConfirmationBarrierToolCallback` with `stripConfirmationToken` so unverified LLM `confirmationToken` values are stripped and valid server-held tokens are still claimed via `priorConfirmationClaimer`.
  - Updated `AuraChatService.buildHistoryMessages` to coalesce consecutive `AssistantMessage` entries and strip leading `AssistantMessage` turns after windowing.
  - Updated `AuraTestChatController.getMessages` to emit `chatSessions` and `oobSessionSelect = true` whenever a non-blank `sessionId` falls back to a different session.
  - Updated `dashboard-chat.js` to block `.`, `/`, `\` after `*` and `/`, `\` around `_`/`__`, and to clear `#chatInput` on `/api/chat` success only when the text still matches `pendingSubmittedPrompt`.
- **Safety Net Added:**
  - `AuraChatServiceTest.testExecutePendingConfirmationPropagatesToolErrorsAndPreservesSiblings`
  - `AuraChatServiceTest.testHallucinatedTokenInNextTurnStillConsumesServerConfirmation`
  - `AuraChatServiceTest.testBuildHistoryMessagesCoalescesAdjacentAssistantMessagesAndDropsLeadingAssistant`
  - `AuraTestChatControllerTest.testGetMessagesResyncsDropdownWhenFallingBackFromMissingSession`
  - Extended `DashboardChatJsTest.testMarkdownRendererAndSubmitGuardsViaNode` and `DashboardChatJsTest.testChatInputPreservationConfirmationRecoveryAndErrorScopingViaNode`
